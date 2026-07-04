package com.xyj.focuspod.service.ai

import android.content.Context
import android.os.Handler
import android.util.Base64
import android.util.Log
import com.xyj.focuspod.model.GradeResult
import com.xyj.focuspod.model.Question
import com.xyj.focuspod.model.StudyStage
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

data class DoubaoMultimodalConfig(
    val apiKey: String,
    val model: String,
    val endpoint: String = DEFAULT_ENDPOINT
) {
    fun isReady(): Boolean = apiKey.isNotBlank() && model.isNotBlank()

    companion object {
        const val DEFAULT_ENDPOINT = "https://ark.cn-beijing.volces.com/api/v3/responses"
    }
}

class DoubaoMultimodalGradingAiService(
    private val context: Context,
    private val config: DoubaoMultimodalConfig,
    private val handler: Handler,
    private val client: OkHttpClient = OkHttpClient()
) : GradingAiService {
    private val gradingPrompt: String by lazy { loadPromptAsset() }

    override fun grade(
        stage: StudyStage,
        question: Question,
        imagePath: String,
        examRound: Int,
        callback: (Result<GradeResult>) -> Unit
    ) {
        if (!config.isReady()) {
            callback(Result.failure(IllegalStateException("豆包多模态批卷参数未配置。")))
            return
        }

        val requestBody = runCatching {
            buildRequestBody(stage, question, imagePath, examRound)
        }.getOrElse {
            callback(Result.failure(it))
            return
        }

        val request = Request.Builder()
            .url(config.endpoint)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handler.post { callback(Result.failure(e)) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val raw = it.body?.string().orEmpty()
                    if (!it.isSuccessful) {
                        handler.post {
                            callback(Result.failure(IllegalStateException("豆包批卷请求失败：${it.code} $raw")))
                        }
                        return
                    }

                    val result = runCatching {
                        parseGradeResult(raw, stage, question)
                    }
                    handler.post { callback(result) }
                }
            }
        })
    }

    private fun buildRequestBody(
        stage: StudyStage,
        question: Question,
        imagePath: String,
        examRound: Int
    ): JSONObject {
        val inputText = """
            当前阶段：${stage.name}
            考试轮次：$examRound
            题目标题：${question.title}
            题干：${question.questionMarkdown}
            标准答案：${question.answer}
            本题批卷要求：${question.gradingPrompt}
        """.trimIndent()

        val content = JSONArray()
            .put(
                JSONObject()
                    .put("type", "input_text")
                    .put("text", inputText)
            )
            .put(
                JSONObject()
                    .put("type", "input_image")
                    .put("image_url", imageDataUrl(imagePath))
                    .put("detail", "high")
            )

        return JSONObject()
            .put("model", config.model)
            .put("instructions", gradingPrompt)
            .put(
                "input",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("type", "message")
                        .put("content", content)
                )
            )
            .put("max_output_tokens", MAX_OUTPUT_TOKENS)
            .put("thinking", JSONObject().put("type", "disabled"))
            .put("stream", false)
            .put("text", JSONObject().put("format", gradeResultSchema()))
    }

    private fun gradeResultSchema(): JSONObject {
        return JSONObject()
            .put("type", "json_schema")
            .put("name", "grading_result")
            .put("strict", true)
            .put("schema", JSONObject()
                .put("type", "object")
                .put(
                    "properties",
                    JSONObject()
                        .put("score", JSONObject().put("type", "integer").put("minimum", 0).put("maximum", 100))
                        .put("feedback", JSONObject().put("type", "string"))
                        .put("reason", JSONObject().put("type", "string"))
                        .put("suggestion", JSONObject().put("type", "string"))
                )
                .put("required", JSONArray().put("score").put("feedback").put("reason").put("suggestion"))
                .put("additionalProperties", false)
            )
    }

    private fun parseGradeResult(raw: String, stage: StudyStage, question: Question): GradeResult {
        val root = JSONObject(raw)
        if (root.optString("status") == "incomplete") {
            val reason = root.optJSONObject("incomplete_details")?.optString("reason").orEmpty()
                .ifBlank { "unknown" }
            throw IllegalStateException("豆包批卷未完成：$reason")
        }

        val outputText = outputTextFromResponse(root)
            ?: throw IllegalStateException("豆包批卷响应缺少文本输出。")
        val jsonText = extractJsonText(outputText)
        Log.i(TAG, "Doubao grading result JSON=$jsonText")
        val resultJson = JSONObject(jsonText)
        return GradeResult(
            questionId = question.id,
            stage = stage,
            score = resultJson.optInt("score").coerceIn(0, 100),
            feedback = resultJson.optString("feedback", "已完成批改。"),
            reason = resultJson.optString("reason"),
            suggestion = resultJson.optString("suggestion")
        )
    }

    private fun outputTextFromResponse(root: JSONObject): String? {
        root.opt("output_text").takeIf { it is String }?.let {
            val text = it as String
            if (text.isNotBlank()) return text
        }

        val output = root.optJSONArray("output") ?: return null
        for (index in 0 until output.length()) {
            val item = output.optJSONObject(index) ?: continue
            if (item.optString("type") == "message") {
                outputTextFromMessage(item)?.let { return it }
            }
            if (item.optString("type") == "output_text") {
                item.opt("text").takeIf { it is String }?.let {
                    val text = it as String
                    if (text.isNotBlank()) return text
                }
            }
        }
        return null
    }

    private fun outputTextFromMessage(message: JSONObject): String? {
        val content = message.optJSONArray("content") ?: return null
        for (index in 0 until content.length()) {
            val item = content.optJSONObject(index) ?: continue
            if (item.optString("type") == "output_text" || item.has("text")) {
                item.opt("text").takeIf { it is String }?.let {
                    val text = it as String
                    if (text.isNotBlank()) return text
                }
            }
        }
        return null
    }

    private fun extractJsonText(text: String): String {
        val trimmed = text.trim()
        if (!trimmed.startsWith("```")) return trimmed

        return trimmed
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .substringBeforeLast("```")
            .trim()
    }

    private fun imageDataUrl(imagePath: String): String {
        val bytes = File(imagePath).readBytes()
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$encoded"
    }

    private fun loadPromptAsset(): String {
        return context.assets.open(GRADING_PROMPT_ASSET_PATH).bufferedReader().use { it.readText() }
    }

    private companion object {
        const val TAG = "DoubaoGradingAiService"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val GRADING_PROMPT_ASSET_PATH = "prompts/grading_multimodal_prompt.txt"
        const val MAX_OUTPUT_TOKENS = 1_600
    }
}

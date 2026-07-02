package com.xyj.focuspod.service.ai

import android.app.Application
import android.content.Context
import android.os.Handler
import android.util.Log
import com.bytedance.speech.speechengine.SpeechEngine
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import com.xyj.focuspod.model.Question
import com.xyj.focuspod.model.QuestionSource
import com.xyj.focuspod.model.StudyStage
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

class DoubaoDialogTutoringAiService(
    private val context: Context,
    private val application: Application,
    private val config: DoubaoDialogConfig,
    private val handler: Handler
) : TutoringAiService, SpeechEngine.SpeechListener {
    private var engine: SpeechEngine? = null
    private var listener: TutoringAiListener? = null
    private var sessionStarted = false
    private var pendingQuestionContext: PendingQuestionContext? = null

    override fun startSession(
        stage: StudyStage,
        question: Question,
        listener: TutoringAiListener
    ) {
        this.listener = listener
        pendingQuestionContext = PendingQuestionContext(stage, question)
        if (!config.isReady()) {
            listener.onError("豆包语音参数未配置，请补充 APP ID 和 Access Token。")
            return
        }

        val currentEngine = ensureEngine(listener) ?: return
        currentEngine.sendChecked(
            SpeechEngineDefines.DIRECTIVE_SYNC_STOP_ENGINE,
            "",
            "停止旧语音会话"
        ) ?: return

        val startPayload = JSONObject()
            .put("dialog", JSONObject().put("bot_name", config.botName))
            .toString()
        if (currentEngine.sendChecked(
                SpeechEngineDefines.DIRECTIVE_START_ENGINE,
                startPayload,
                "启动豆包语音会话"
            ) == null
        ) {
            return
        }

        sessionStarted = true
        listener.onSessionStarted()
        listener.onCaption("豆包语音已连接，可以直接提问。")
    }

    override fun stopSession() {
        val currentEngine = engine ?: return
        sessionStarted = false
        pendingQuestionContext = null
        currentEngine.sendDirective(SpeechEngineDefines.DIRECTIVE_SYNC_STOP_ENGINE, "")
        listener = null
    }

    override fun release() {
        val currentEngine = engine ?: return
        sessionStarted = false
        pendingQuestionContext = null
        listener = null
        currentEngine.destroyEngine()
        engine = null
    }

    override fun onSpeechMessage(type: Int, data: ByteArray, len: Int) {
        val message = String(data, 0, len.coerceAtMost(data.size), StandardCharsets.UTF_8)
        handler.post {
            when (type) {
                SpeechEngineDefines.MESSAGE_TYPE_ENGINE_START,
                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_SESSION_STARTED,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_SESSION_STARTED -> {
                    listener?.onCaption("语音会话已启动。")
                    sendPendingQuestionContext()
                }

                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_ASR_INFO,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_ASR_INFO -> {
                    listener?.onCaption("正在听你说话。")
                }

                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_ASR_RESPONSE,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_ASR_RESPONSE -> {
                    extractText(message)?.let {
                        listener?.onStudentSpeech(it)
                        listener?.onCaption("你说：$it")
                    }
                }

                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_ASR_ENDED,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_ASR_ENDED -> {
                    listener?.onCaption("已收到问题，正在组织引导。")
                }

                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_CHAT_RESPONSE,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_CHAT_RESPONSE,
                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_TTS_RESPONSE,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_TTS_RESPONSE,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_TTS_SUBTITLE -> {
                    extractText(message)?.let {
                        listener?.onAiResponse(it)
                        listener?.onCaption(it)
                    }
                }

                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_CHAT_ENDED,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_CHAT_ENDED,
                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_TTS_ENDED,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_TTS_ENDED -> {
                    listener?.onCaption("可以继续提问，完成后翻转手机开始批卷。")
                }

                SpeechEngineDefines.MESSAGE_TYPE_ENGINE_STOP,
                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_SESSION_FINISHED,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_SESSION_FINISHED -> {
                    sessionStarted = false
                    listener?.onCaption("语音会话已结束。")
                }

                SpeechEngineDefines.MESSAGE_TYPE_ENGINE_ERROR,
                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_SESSION_FAILED,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_SESSION_FAILED,
                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_CONNECTION_FAILED,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_CONNECTION_FAILED -> {
                    listener?.onError("豆包语音异常：${message.ifBlank { type.toString() }}")
                }
            }
        }
    }

    override fun onSpeechLogid(logid: String) {
        Log.d(TAG, "Doubao speech logid: $logid")
    }

    private fun ensureEngine(listener: TutoringAiListener): SpeechEngine? {
        if (engine != null) return engine

        if (!SpeechEngineGenerator.PrepareEnvironment(context.applicationContext, application)) {
            listener.onError("豆包语音环境初始化失败。")
            return null
        }

        val createdEngine = SpeechEngineGenerator.getInstance()
        createdEngine.createEngine()
        createdEngine.setContext(context.applicationContext)
        createdEngine.setListener(this)
        configureEngine(createdEngine)
        val initResult = createdEngine.initEngine()
        if (initResult != SpeechEngineDefines.ERR_NO_ERROR) {
            listener.onError("豆包语音引擎初始化失败：$initResult")
            createdEngine.destroyEngine()
            return null
        }

        engine = createdEngine
        return createdEngine
    }

    private fun configureEngine(engine: SpeechEngine) {
        engine.setOptionString(SpeechEngineDefines.PARAMS_KEY_ENGINE_NAME_STRING, SpeechEngineDefines.DIALOG_ENGINE)
        engine.setOptionString(SpeechEngineDefines.PARAMS_KEY_LOG_LEVEL_STRING, config.logLevel)
        engine.setOptionString(SpeechEngineDefines.PARAMS_KEY_DEBUG_PATH_STRING, config.debugPath)
        engine.setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_ID_STRING, config.appId)
        engine.setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_KEY_STRING, config.appKey)
        engine.setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_TOKEN_STRING, config.token)
        engine.setOptionString(SpeechEngineDefines.PARAMS_KEY_RESOURCE_ID_STRING, config.resourceId)
        engine.setOptionString(SpeechEngineDefines.PARAMS_KEY_UID_STRING, config.uid)
        engine.setOptionString(SpeechEngineDefines.PARAMS_KEY_DIALOG_ADDRESS_STRING, config.dialogAddress)
        engine.setOptionString(SpeechEngineDefines.PARAMS_KEY_DIALOG_URI_STRING, config.dialogUri)
        engine.setOptionString(SpeechEngineDefines.PARAMS_KEY_RECORDER_TYPE_STRING, SpeechEngineDefines.RECORDER_TYPE_RECORDER)
        engine.setOptionBoolean(SpeechEngineDefines.PARAMS_KEY_DIALOG_ENABLE_PLAYER_BOOL, true)
        engine.setOptionBoolean(SpeechEngineDefines.PARAMS_KEY_DIALOG_ENABLE_RECORDER_AUDIO_CALLBACK_BOOL, false)
        engine.setOptionBoolean(SpeechEngineDefines.PARAMS_KEY_DIALOG_ENABLE_PLAYER_AUDIO_CALLBACK_BOOL, false)
        engine.setOptionBoolean(SpeechEngineDefines.PARAMS_KEY_DIALOG_ENABLE_DECODER_AUDIO_CALLBACK_BOOL, false)
        engine.setOptionString(SpeechEngineDefines.PARAMS_KEY_DIALOG_RECORDER_PATH_STRING, config.recorderPath)
        engine.setOptionString(SpeechEngineDefines.PARAMS_KEY_DIALOG_PLAYER_PATH_STRING, config.playerPath)

        val aecFile = config.aecModelPath.takeIf { it.isNotBlank() }?.let(::File)
        val enableAec = aecFile?.exists() == true
        engine.setOptionBoolean(SpeechEngineDefines.PARAMS_KEY_ENABLE_AEC_BOOL, enableAec)
        if (enableAec) {
            engine.setOptionString(SpeechEngineDefines.PARAMS_KEY_AEC_MODEL_PATH_STRING, aecFile.absolutePath)
        }
    }

    private fun sendQuestionContext(engine: SpeechEngine, stage: StudyStage, question: Question) {
        val stageText = if (stage == StudyStage.EXAMPLE) "例题讲解" else "考试讲评"
        val sourceText = if (question.source == QuestionSource.MISTAKE_REVIEW) "错题复盘" else "原始例题"
        val content = buildString {
            append("你是小学生学习辅导老师。")
            append("当前阶段：").append(stageText).append("。")
            append("题目来源：").append(sourceText).append("。")
            append("题目标题：").append(question.title).append("。")
            append("题干：").append(question.questionMarkdown).append("。")
            append("考察知识点：").append(question.knowledgePoints.joinToString("、")).append("。")
            append("辅导要求：").append(question.tutoringPrompt).append("。")
            append("请用中文短句分步引导学生思考。")
            append("只能提示读题、找条件、列关系式、检查单位和过程。")
            append("不要直接给最终答案，也不要代写完整解题过程。")
        }
        val ragItem = JSONObject()
            .put("title", "当前辅导题目")
            .put("content", content)
        val payload = JSONObject()
            .put("external_rag", "[$ragItem]")
            .toString()
        engine.sendChecked(
            SpeechEngineDefines.DIRECTIVE_EVENT_CHAT_RAG_TEXT,
            payload,
            "发送题目上下文"
        )
    }

    private fun sendPendingQuestionContext() {
        val currentEngine = engine ?: return
        val context = pendingQuestionContext ?: return
        pendingQuestionContext = null
        sendQuestionContext(currentEngine, context.stage, context.question)
        sayHello(currentEngine, context.question)
    }

    private fun sayHello(engine: SpeechEngine, question: Question) {
        val content = "我们来看${question.title}。先读题，找出已知条件；你也可以直接说出卡住的地方。"
        val payload = JSONObject().put("content", content).toString()
        engine.sendChecked(
            SpeechEngineDefines.DIRECTIVE_EVENT_SAY_HELLO,
            payload,
            "播报辅导开场白"
        )
    }

    private fun SpeechEngine.sendChecked(
        directive: Int,
        payload: String,
        action: String
    ): Int? {
        val result = sendDirective(directive, payload)
        if (result != SpeechEngineDefines.ERR_NO_ERROR) {
            listener?.onError("$action 失败：$result")
            return null
        }
        return result
    }

    private fun extractText(raw: String): String? {
        if (raw.isBlank()) return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return raw
        return TEXT_KEYS.firstNotNullOfOrNull { key ->
            json.optString(key).takeIf { it.isNotBlank() }
        } ?: extractNestedText(json) ?: raw
    }

    private fun extractNestedText(json: JSONObject): String? {
        json.keys().forEach { key ->
            val value = json.opt(key)
            if (value is JSONObject) {
                val nested = TEXT_KEYS.firstNotNullOfOrNull { textKey ->
                    value.optString(textKey).takeIf { it.isNotBlank() }
                } ?: extractNestedText(value)
                if (!nested.isNullOrBlank()) return nested
            }
        }
        return null
    }

    companion object {
        private const val TAG = "DoubaoDialogTutoring"
        private val TEXT_KEYS = listOf(
            "content",
            "text",
            "utterance",
            "result",
            "sentence",
            "subtitle"
        )
    }
}

private data class PendingQuestionContext(
    val stage: StudyStage,
    val question: Question
)

data class DoubaoDialogConfig(
    val appId: String,
    val appKey: String,
    val token: String,
    val uid: String,
    val resourceId: String,
    val dialogAddress: String,
    val dialogUri: String,
    val botName: String,
    val aecModelPath: String,
    val debugPath: String,
    val recorderPath: String,
    val playerPath: String,
    val logLevel: String
) {
    fun isReady(): Boolean {
        return appId.isNotBlank() && token.isNotBlank()
    }
}

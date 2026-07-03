package com.xyj.focuspod.service.ai

import android.app.Application
import android.content.Context
import android.os.Handler
import android.util.Log
import com.bytedance.speech.speechengine.SpeechEngine
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import com.xyj.focuspod.model.Question
import com.xyj.focuspod.model.StudyStage
import com.xyj.focuspod.model.TutoringSpeaker
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
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
    private var pendingOpeningQuestion: Question? = null
    private var lastAiCaption: String = ""
    private val tutoringSystemRolePrompt: String by lazy { loadPromptAsset(TUTORING_SYSTEM_ROLE_PROMPT_ASSET_PATH) }
    private val tutoringSpeakingStyle: String by lazy { loadPromptAsset(TUTORING_SPEAKING_STYLE_ASSET_PATH) }
    private val tutoringOpeningPrompt: String by lazy { loadPromptAsset(TUTORING_OPENING_PROMPT_ASSET_PATH) }

    override fun startSession(
        stage: StudyStage,
        question: Question,
        listener: TutoringAiListener
    ) {
        this.listener = listener
        pendingOpeningQuestion = question
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

        val dialogPayload = JSONObject()
            .put("bot_name", config.botName)
            .put("SubtitleConfig", subtitleConfigJson())
        buildTutoringSystemRole(question).takeIf { it.isNotBlank() }?.let {
            dialogPayload.put("system_role", it)
        }
        tutoringSpeakingStyle.takeIf { it.isNotBlank() }?.let {
            dialogPayload.put("speaking_style", it)
        }

        val startPayload = JSONObject()
            .put(
                "dialog",
                dialogPayload
            )
            .put("SubtitleConfig", subtitleConfigJson())
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
    }

    override fun stopSession() {
        val currentEngine = engine ?: return
        sessionStarted = false
        pendingOpeningQuestion = null
        lastAiCaption = ""
        currentEngine.sendDirective(SpeechEngineDefines.DIRECTIVE_SYNC_STOP_ENGINE, "")
        listener = null
    }

    override fun release() {
        val currentEngine = engine ?: return
        sessionStarted = false
        pendingOpeningQuestion = null
        lastAiCaption = ""
        listener = null
        currentEngine.destroyEngine()
        engine = null
    }

    override fun onSpeechMessage(type: Int, data: ByteArray, len: Int) {
        val payloadLength = len.coerceIn(0, data.size)
        val subtitles = decodeSubtitlePayload(data, payloadLength)
        val message = decodeTextPayload(data, payloadLength)
        handler.post {
            logReadableMessage(type, message)
            when (type) {
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_TTS_SUBTITLE -> {
                    val parsedSubtitles = subtitles.ifEmpty {
                        message?.let(::parseSubtitleJson).orEmpty()
                    }
                    if (parsedSubtitles.isNotEmpty()) {
                        parsedSubtitles.forEach { listener?.onSubtitle(it) }
                    } else {
                        message?.let(::extractText)?.let {
                            emitAiCaption(it)
                        }
                    }
                }

                SpeechEngineDefines.MESSAGE_TYPE_ENGINE_START -> {
                    Unit
                }

                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_SESSION_STARTED,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_SESSION_STARTED -> {
                    sendPendingOpening()
                }

                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_ASR_INFO,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_ASR_INFO -> {
                    Unit
                }

                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_ASR_RESPONSE,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_ASR_RESPONSE -> {
                    message?.let(::extractText)?.let {
                        listener?.onStudentSpeech(it)
                    }
                }

                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_ASR_ENDED,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_ASR_ENDED -> {
                    Unit
                }

                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_CHAT_RESPONSE,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_CHAT_RESPONSE -> {
                    message?.let(::extractText)?.let {
                        emitAiCaption(it)
                    }
                }

                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_TTS_RESPONSE,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_TTS_RESPONSE -> {
                    message?.let(::extractText)?.let {
                        emitAiCaption(it)
                    }
                }

                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_TTS_SENTENCE_END,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_TTS_SENTENCE_END -> {
                    message?.let(::extractText)?.let {
                        emitAiCaption(it)
                    }
                }

                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_CHAT_ENDED,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_CHAT_ENDED,
                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_TTS_ENDED,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_TTS_ENDED -> {
                    Unit
                }

                SpeechEngineDefines.MESSAGE_TYPE_ENGINE_STOP,
                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_SESSION_FINISHED,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_SESSION_FINISHED -> {
                    sessionStarted = false
                }

                SpeechEngineDefines.MESSAGE_TYPE_ENGINE_ERROR,
                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_SESSION_FAILED,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_SESSION_FAILED,
                SpeechEngineDefines.MESSAGE_TYPE_DIALOG_CONNECTION_FAILED,
                SpeechEngineDefines.MESSAGE_TYPE_EVENT_CONNECTION_FAILED -> {
                    listener?.onError("豆包语音异常：${message?.takeIf { it.isNotBlank() } ?: type.toString()}")
                }
            }
        }
    }

    fun onRoomBinaryMessageReceived(uid: String, buffer: ByteBuffer) {
        val message = ByteArray(buffer.remaining())
        buffer.slice().get(message)
        val subtitles = decodeSubtitlePayload(message, message.size)
        if (subtitles.isEmpty()) return

        handler.post {
            subtitles.forEach { listener?.onSubtitle(it) }
        }
    }

    override fun onSpeechLogid(logid: String) {
        Log.d(TAG, "Doubao speech logid: $logid")
    }

    private fun emitAiCaption(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank() || cleanText == lastAiCaption) return
        lastAiCaption = cleanText
        listener?.onCaption(cleanText)
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

        val aecFile = resolveAecModelFile()
        val enableAec = aecFile?.exists() == true
        engine.setOptionBoolean(SpeechEngineDefines.PARAMS_KEY_ENABLE_AEC_BOOL, enableAec)
        if (enableAec) {
            engine.setOptionString(SpeechEngineDefines.PARAMS_KEY_AEC_MODEL_PATH_STRING, aecFile.absolutePath)
        }
    }

    private fun resolveAecModelFile(): File? {
        val configuredFile = config.aecModelPath
            .takeIf { it.isNotBlank() }
            ?.let(::File)
            ?.takeIf { it.exists() }
        if (configuredFile != null) return configuredFile

        val assetAecFile = File(context.filesDir, AEC_MODEL_FILE_PATH)
        if (assetAecFile.exists()) return assetAecFile

        return runCatching {
            assetAecFile.parentFile?.mkdirs()
            context.assets.open(AEC_MODEL_ASSET_PATH).use { input ->
                assetAecFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            assetAecFile
        }.onFailure {
            Log.w(TAG, "AEC model is unavailable, AEC will be disabled.", it)
        }.getOrNull()
    }

    private fun buildTutoringSystemRole(question: Question): String {
        return renderPrompt(tutoringSystemRolePrompt, promptValues(question))
    }

    private fun loadPromptAsset(assetPath: String): String {
        return runCatching {
            context.assets.open(assetPath).use { input ->
                input.bufferedReader(StandardCharsets.UTF_8).readText().trim()
            }
        }.getOrElse { error ->
            Log.w(TAG, "Prompt asset is unavailable: $assetPath", error)
            ""
        }
    }

    private fun renderPrompt(template: String, values: Map<String, String>): String {
        return values.entries.fold(template) { result, (key, value) ->
            result.replace("{{$key}}", value)
        }.trim()
    }

    private fun promptValues(question: Question): Map<String, String> {
        return mapOf(
            "question_title" to question.title,
            "question_markdown" to question.questionMarkdown,
            "knowledge_points" to question.knowledgePoints.joinToString("、"),
            "tutoring_prompt" to question.tutoringPrompt
        )
    }

    private fun sendPendingOpening() {
        val currentEngine = engine ?: return
        val question = pendingOpeningQuestion ?: return
        pendingOpeningQuestion = null
        sayHello(currentEngine, question)
    }

    private fun sayHello(engine: SpeechEngine, question: Question) {
        val content = renderPrompt(
            tutoringOpeningPrompt,
            mapOf("question_title" to question.title)
        )
        if (content.isBlank()) return
        val payload = JSONObject().put("content", content).toString()
        engine.sendChecked(
            SpeechEngineDefines.DIRECTIVE_EVENT_SAY_HELLO,
            payload,
            "播报辅导开场白"
        )
    }

    private fun subtitleConfigJson(): JSONObject {
        return JSONObject()
            .put("DisableRTSSubtitle", false)
            .put("SubtitleMode", SUBTITLE_MODE_FAST)
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

    private fun decodeTextPayload(data: ByteArray, length: Int): String? {
        if (length <= 0 || isKnownBinaryPayload(data, length)) return null

        val text = String(data, 0, length, StandardCharsets.UTF_8).trim()
        if (text.isBlank()) return null
        if (text.startsWith("OggS")) return null

        val replacementCount = text.count { it == '\uFFFD' }
        if (replacementCount > maxOf(1, text.length / 20)) return null

        val controlCount = text.count { it.isISOControl() && it != '\n' && it != '\r' && it != '\t' }
        if (controlCount > 0) return null

        return text
    }

    private fun logReadableMessage(type: Int, message: String?) {
        if (message.isNullOrBlank()) return
        if (type !in DIALOG_MESSAGE_TYPE_RANGE && type !in EVENT_MESSAGE_TYPE_RANGE) return
        Log.d(TAG, "Speech message type=$type payload=${message.take(MAX_LOG_PAYLOAD_LENGTH)}")
    }

    private fun decodeSubtitlePayload(data: ByteArray, length: Int): List<TutoringSubtitle> {
        val subtitleJson = unpackSubtitlePayload(data, length) ?: return emptyList()
        return parseSubtitleJson(subtitleJson)
    }

    private fun unpackSubtitlePayload(data: ByteArray, length: Int): String? {
        if (length < SUBTITLE_HEADER_SIZE) return null
        if (!SUBTITLE_MAGIC.indices.all { data[it] == SUBTITLE_MAGIC[it] }) return null

        val subtitleLength = ((data[4].toInt() and 0xff) shl 24) or
            ((data[5].toInt() and 0xff) shl 16) or
            ((data[6].toInt() and 0xff) shl 8) or
            (data[7].toInt() and 0xff)
        if (length - SUBTITLE_HEADER_SIZE != subtitleLength) return null
        if (subtitleLength <= 0) return ""

        return String(data, SUBTITLE_HEADER_SIZE, subtitleLength, StandardCharsets.UTF_8).trim()
    }

    private fun parseSubtitleJson(raw: String): List<TutoringSubtitle> {
        if (raw.isBlank()) return emptyList()
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
        val data = json.opt("data")
        val subtitleItems = when (data) {
            is JSONArray -> data.asJsonObjects()
            is JSONObject -> listOf(data)
            else -> emptyList()
        }
        return subtitleItems.mapNotNull(::parseSubtitleItem)
    }

    private fun parseSubtitleItem(json: JSONObject): TutoringSubtitle? {
        val text = json.optString("text").trim()
        if (text.isBlank()) return null

        val userId = json.optString("userId")
        return TutoringSubtitle(
            speaker = subtitleSpeaker(userId),
            text = text,
            definite = json.optBoolean("definite", false),
            paragraph = json.optBoolean("paragraph", false),
            sequence = json.optInt("sequence", -1),
            roundId = json.optInt("roundId", -1)
        )
    }

    private fun JSONArray.asJsonObjects(): List<JSONObject> {
        val objects = mutableListOf<JSONObject>()
        for (index in 0 until length()) {
            val item = opt(index)
            if (item is JSONObject) {
                objects += item
            }
        }
        return objects
    }

    private fun subtitleSpeaker(userId: String): TutoringSpeaker {
        val normalizedUserId = userId.trim()
        return if (normalizedUserId.isNotBlank() && normalizedUserId == config.uid) {
            TutoringSpeaker.STUDENT
        } else {
            TutoringSpeaker.AI
        }
    }

    private fun isKnownBinaryPayload(data: ByteArray, length: Int): Boolean {
        if (length >= OGG_MAGIC.size && OGG_MAGIC.indices.all { data[it] == OGG_MAGIC[it] }) {
            return true
        }
        if (length >= SUBTITLE_MAGIC.size && SUBTITLE_MAGIC.indices.all { data[it] == SUBTITLE_MAGIC[it] }) {
            return true
        }
        return (0 until length).any { data[it] == 0.toByte() }
    }

    private fun extractText(raw: String): String? {
        if (raw.isBlank()) return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return raw.trim()
        return TEXT_KEYS.firstNotNullOfOrNull { key ->
            json.optString(key).takeIf { isReadableSpeechText(it) }
        } ?: extractNestedText(json)
    }

    private fun extractNestedText(json: JSONObject): String? {
        json.keys().forEach { key ->
            val value = json.opt(key)
            val nested = when (value) {
                is JSONObject -> extractTextFromJson(value)
                is JSONArray -> extractTextFromArray(value)
                else -> null
            }
            if (!nested.isNullOrBlank()) return nested
        }
        return null
    }

    private fun extractTextFromJson(json: JSONObject): String? {
        return TEXT_KEYS.firstNotNullOfOrNull { textKey ->
            json.optString(textKey).takeIf { isReadableSpeechText(it) }
        } ?: extractNestedText(json)
    }

    private fun extractTextFromArray(array: JSONArray): String? {
        for (index in 0 until array.length()) {
            val nested = when (val value = array.opt(index)) {
                is JSONObject -> extractTextFromJson(value)
                is JSONArray -> extractTextFromArray(value)
                is String -> value.takeIf { isReadableSpeechText(it) }
                else -> null
            }
            if (!nested.isNullOrBlank()) return nested
        }
        return null
    }

    private fun isReadableSpeechText(text: String): Boolean {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return false
        return !cleanText.startsWith("{") && !cleanText.startsWith("[")
    }

    companion object {
        private const val TAG = "DoubaoDialogTutoring"
        private const val AEC_MODEL_ASSET_PATH = "doubao/aec.model"
        private const val AEC_MODEL_FILE_PATH = "doubao/aec.model"
        private const val TUTORING_SYSTEM_ROLE_PROMPT_ASSET_PATH = "prompts/tutoring_context_prompt.txt"
        private const val TUTORING_SPEAKING_STYLE_ASSET_PATH = "prompts/tutoring_speaking_style.txt"
        private const val TUTORING_OPENING_PROMPT_ASSET_PATH = "prompts/tutoring_opening_prompt.txt"
        private const val SUBTITLE_HEADER_SIZE = 8
        private const val SUBTITLE_MODE_FAST = 1
        private val OGG_MAGIC = byteArrayOf(0x4F, 0x67, 0x67, 0x53)
        private val SUBTITLE_MAGIC = byteArrayOf(0x73, 0x75, 0x62, 0x76)
        private val TEXT_KEYS = listOf(
            "content",
            "text",
            "utterance",
            "result",
            "sentence",
            "subtitle"
        )
        private const val MAX_LOG_PAYLOAD_LENGTH = 600
        private val DIALOG_MESSAGE_TYPE_RANGE = 3000..3018
        private val EVENT_MESSAGE_TYPE_RANGE = 3000..3028
    }
}

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

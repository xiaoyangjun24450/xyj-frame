package com.xyj.focuspod.app

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.xyj.focuspod.BuildConfig
import com.xyj.focuspod.data.AssetStudyPlanApi
import com.xyj.focuspod.flow.StudyFlow
import com.xyj.focuspod.service.ai.DoubaoDialogConfig
import com.xyj.focuspod.service.ai.DoubaoDialogTutoringAiService
import com.xyj.focuspod.service.ai.DoubaoMultimodalConfig
import com.xyj.focuspod.service.ai.DoubaoMultimodalGradingAiService
import com.xyj.focuspod.service.camera.Camera2CaptureService
import com.xyj.focuspod.service.device.UnconfiguredDoorControlService
import com.xyj.focuspod.service.sensor.AndroidPoseDetector
import com.xyj.focuspod.service.voice.AndroidTextToSpeechPromptService

class AppContainer(
    context: Context,
    application: Application
) {
    private val handler = Handler(Looper.getMainLooper())
    private val tutoringAiService = DoubaoDialogTutoringAiService(
        context = context.applicationContext,
        application = application,
        config = DoubaoDialogConfig(
            appId = BuildConfig.DOUBAO_APP_ID,
            appKey = BuildConfig.DOUBAO_APP_KEY,
            token = BuildConfig.DOUBAO_TOKEN,
            uid = BuildConfig.DOUBAO_UID,
            resourceId = BuildConfig.DOUBAO_RESOURCE_ID,
            dialogAddress = BuildConfig.DOUBAO_DIALOG_ADDRESS,
            dialogUri = BuildConfig.DOUBAO_DIALOG_URI,
            botName = BuildConfig.DOUBAO_BOT_NAME,
            aecModelPath = BuildConfig.DOUBAO_AEC_MODEL_PATH,
            debugPath = BuildConfig.DOUBAO_DEBUG_PATH,
            recorderPath = BuildConfig.DOUBAO_RECORDER_PATH,
            playerPath = BuildConfig.DOUBAO_PLAYER_PATH,
            logLevel = BuildConfig.DOUBAO_LOG_LEVEL
        ),
        handler = handler
    )
    private val cameraCaptureService = Camera2CaptureService(
        context = context.applicationContext,
        handler = handler
    )
    private val voicePromptService = AndroidTextToSpeechPromptService(context.applicationContext)
    private val gradingAiService = DoubaoMultimodalGradingAiService(
        context = context.applicationContext,
        config = DoubaoMultimodalConfig(
            apiKey = BuildConfig.DOUBAO_MULTIMODAL_API_KEY,
            model = BuildConfig.DOUBAO_MULTIMODAL_MODEL,
            endpoint = BuildConfig.DOUBAO_MULTIMODAL_ENDPOINT
        ),
        handler = handler
    )

    val studyFlow = StudyFlow(
        handler = handler,
        studyPlanApi = AssetStudyPlanApi(context.applicationContext, handler),
        poseDetector = AndroidPoseDetector(context.applicationContext, handler),
        cameraCaptureService = cameraCaptureService,
        tutoringAiService = tutoringAiService,
        gradingAiService = gradingAiService,
        doorControlService = UnconfiguredDoorControlService(handler),
        voicePromptService = voicePromptService
    )

}

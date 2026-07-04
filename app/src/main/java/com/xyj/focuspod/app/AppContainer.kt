package com.xyj.focuspod.app

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.xyj.focuspod.BuildConfig
import com.xyj.focuspod.flow.StudyFlow
import com.xyj.focuspod.mock.FakeCameraCaptureService
import com.xyj.focuspod.mock.FakeDoorControlService
import com.xyj.focuspod.mock.FakeGradingAiService
import com.xyj.focuspod.mock.MockStudyPlanApi
import com.xyj.focuspod.service.ai.DoubaoDialogConfig
import com.xyj.focuspod.service.ai.DoubaoDialogTutoringAiService
import com.xyj.focuspod.service.sensor.AndroidPoseDetector

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

    val studyFlow = StudyFlow(
        handler = handler,
        studyPlanApi = MockStudyPlanApi(context.applicationContext, handler),
        poseDetector = AndroidPoseDetector(context.applicationContext, handler),
        cameraCaptureService = FakeCameraCaptureService(handler),
        tutoringAiService = tutoringAiService,
        gradingAiService = FakeGradingAiService(handler),
        doorControlService = FakeDoorControlService(handler)
    )
}

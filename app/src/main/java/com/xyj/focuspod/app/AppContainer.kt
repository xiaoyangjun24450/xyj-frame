package com.xyj.focuspod.app

import android.os.Handler
import android.os.Looper
import com.xyj.focuspod.flow.StudyFlow
import com.xyj.focuspod.mock.FakeCameraCaptureService
import com.xyj.focuspod.mock.FakeDoorControlService
import com.xyj.focuspod.mock.FakeGradingAiService
import com.xyj.focuspod.mock.FakePoseDetector
import com.xyj.focuspod.mock.MockStudyPlanApi

class AppContainer {
    private val handler = Handler(Looper.getMainLooper())

    val studyFlow = StudyFlow(
        handler = handler,
        studyPlanApi = MockStudyPlanApi(handler),
        poseDetector = FakePoseDetector(handler),
        cameraCaptureService = FakeCameraCaptureService(handler),
        gradingAiService = FakeGradingAiService(handler),
        doorControlService = FakeDoorControlService(handler)
    )
}

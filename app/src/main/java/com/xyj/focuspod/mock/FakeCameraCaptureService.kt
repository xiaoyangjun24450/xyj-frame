package com.xyj.focuspod.mock

import android.os.Handler
import android.os.Looper
import com.xyj.focuspod.service.camera.CameraCaptureService

class FakeCameraCaptureService(
    private val handler: Handler = Handler(Looper.getMainLooper())
) : CameraCaptureService {

    override fun captureAnswer(callback: (Result<String>) -> Unit) {
        handler.postDelayed({
            callback(Result.success("mock://answer-photo.jpg"))
        }, MOCK_DELAY_LONG_MS)
    }
}

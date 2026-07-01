package com.xyj.focuspod.mock

import android.os.Handler
import android.os.Looper
import com.xyj.focuspod.service.sensor.PoseDetector

class FakePoseDetector(
    private val handler: Handler = Handler(Looper.getMainLooper())
) : PoseDetector {

    override fun waitForFlip(callback: () -> Unit) {
        handler.postDelayed(callback, MOCK_DELAY_LONG_MS)
    }

    override fun waitForFaceUp(callback: () -> Unit) {
        handler.postDelayed(callback, MOCK_DELAY_LONG_MS)
    }
}

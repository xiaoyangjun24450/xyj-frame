package com.xyj.focuspod.service.sensor

interface PoseDetector {
    fun waitForFlip(callback: () -> Unit)

    fun waitForFaceUp(callback: () -> Unit)

    fun cancel() = Unit

    fun release() {
        cancel()
    }
}

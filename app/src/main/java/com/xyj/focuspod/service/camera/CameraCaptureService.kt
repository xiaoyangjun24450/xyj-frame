package com.xyj.focuspod.service.camera

interface CameraCaptureService {
    fun captureAnswer(callback: (Result<String>) -> Unit)
}

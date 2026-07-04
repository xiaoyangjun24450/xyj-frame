package com.xyj.focuspod.service.camera

import android.view.TextureView

interface CameraCaptureService {
    fun bindPreview(textureView: TextureView) = Unit

    fun captureAnswer(callback: (Result<String>) -> Unit)

    fun release() = Unit
}

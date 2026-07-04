package com.xyj.focuspod.service.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.os.Handler
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class Camera2CaptureService(
    context: Context,
    private val handler: Handler
) : CameraCaptureService {
    private val appContext = context.applicationContext
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var textureView: TextureView? = null
    private var cameraId: String? = null
    private var previewSize: Size? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var pendingCaptureCallback: ((Result<String>) -> Unit)? = null
    private val isOpening = AtomicBoolean(false)
    private val isCaptureInFlight = AtomicBoolean(false)

    override fun bindPreview(textureView: TextureView) {
        if (this.textureView === textureView && cameraDevice != null) return
        this.textureView = textureView
        if (textureView.isAvailable) {
            openCamera()
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    openCamera()
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    closeCamera()
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
            }
        }
    }

    override fun captureAnswer(callback: (Result<String>) -> Unit) {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            callback(Result.failure(IllegalStateException("相机权限未授权。")))
            return
        }
        if (!isCaptureInFlight.compareAndSet(false, true)) {
            callback(Result.failure(IllegalStateException("正在拍照，请稍候。")))
            return
        }

        pendingCaptureCallback = callback
        val device = cameraDevice
        val session = captureSession
        val reader = imageReader
        if (device == null || session == null || reader == null) {
            openCamera()
            return
        }
        captureStillImage()
    }

    override fun release() {
        closeCamera()
        textureView = null
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingCaptureCallback?.invoke(Result.failure(IllegalStateException("相机权限未授权。")))
            pendingCaptureCallback = null
            isCaptureInFlight.set(false)
            return
        }
        if (cameraDevice != null || !isOpening.compareAndSet(false, true)) return

        val id = selectBackCameraId()
        if (id == null) {
            isOpening.set(false)
            pendingCaptureCallback?.invoke(Result.failure(IllegalStateException("未找到后置摄像头。")))
            pendingCaptureCallback = null
            isCaptureInFlight.set(false)
            return
        }

        cameraId = id
        previewSize = previewSizeFor(id)
        runCatching {
            cameraManager.openCamera(
                id,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        isOpening.set(false)
                        cameraDevice = camera
                        startPreviewSession()
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        isOpening.set(false)
                        camera.close()
                        cameraDevice = null
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        isOpening.set(false)
                        camera.close()
                        cameraDevice = null
                        pendingCaptureCallback?.invoke(Result.failure(IllegalStateException("打开相机失败：$error")))
                        pendingCaptureCallback = null
                        isCaptureInFlight.set(false)
                    }
                },
                handler
            )
        }.onFailure {
            isOpening.set(false)
            pendingCaptureCallback?.invoke(Result.failure(it))
            pendingCaptureCallback = null
            isCaptureInFlight.set(false)
        }
    }

    private fun startPreviewSession() {
        val device = cameraDevice ?: return
        val texture = textureView?.surfaceTexture ?: return
        val size = previewSize ?: Size(DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT)

        texture.setDefaultBufferSize(size.width, size.height)
        val previewSurface = Surface(texture)
        val reader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, MAX_CAPTURE_IMAGES)
        imageReader?.close()
        imageReader = reader
        reader.setOnImageAvailableListener({ availableReader ->
            val callback = pendingCaptureCallback
            pendingCaptureCallback = null
            val result = runCatching {
                val image = availableReader.acquireLatestImage()
                    ?: throw IllegalStateException("未获取到相机图像。")
                try {
                    val buffer = image.planes.first().buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val file = File(appContext.cacheDir, "grading-answer-${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { output -> output.write(bytes) }
                    file.absolutePath
                } finally {
                    image.close()
                }
            }
            isCaptureInFlight.set(false)
            callback?.invoke(result)
        }, handler)

        runCatching {
            val requestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(previewSurface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }
            device.createCaptureSession(
                listOf(previewSurface, reader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        runCatching {
                            session.setRepeatingRequest(requestBuilder.build(), null, handler)
                        }
                        if (pendingCaptureCallback != null && isCaptureInFlight.get()) {
                            captureStillImage()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        pendingCaptureCallback?.invoke(Result.failure(IllegalStateException("相机预览配置失败。")))
                        pendingCaptureCallback = null
                        isCaptureInFlight.set(false)
                    }
                },
                handler
            )
        }.onFailure {
            pendingCaptureCallback?.invoke(Result.failure(it))
            pendingCaptureCallback = null
            isCaptureInFlight.set(false)
        }
    }

    private fun captureStillImage() {
        val device = cameraDevice
        val session = captureSession
        val reader = imageReader
        if (device == null || session == null || reader == null) {
            pendingCaptureCallback?.invoke(Result.failure(IllegalStateException("相机尚未准备好。")))
            pendingCaptureCallback = null
            isCaptureInFlight.set(false)
            return
        }

        runCatching {
            val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(reader.surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation())
            }.build()
            session.capture(request, null, handler)
        }.onFailure {
            pendingCaptureCallback?.invoke(Result.failure(it))
            pendingCaptureCallback = null
            isCaptureInFlight.set(false)
        }
    }

    private fun selectBackCameraId(): String? {
        return cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }

    private fun previewSizeFor(cameraId: String): Size {
        val map = cameraManager.getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return map.bestJpegSize() ?: Size(DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT)
    }

    private fun StreamConfigurationMap?.bestJpegSize(): Size? {
        val sizes = this?.getOutputSizes(ImageFormat.JPEG).orEmpty()
        return sizes
            .filter { it.width <= MAX_CAPTURE_WIDTH && it.height <= MAX_CAPTURE_HEIGHT }
            .maxByOrNull { it.width * it.height }
            ?: sizes.maxByOrNull { it.width * it.height }
    }

    private fun jpegOrientation(): Int {
        val id = cameraId ?: return 0
        val sensorOrientation = cameraManager.getCameraCharacteristics(id)
            .get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        return sensorOrientation
    }

    private fun closeCamera() {
        runCatching { captureSession?.close() }
        runCatching { cameraDevice?.close() }
        runCatching { imageReader?.close() }
        captureSession = null
        cameraDevice = null
        imageReader = null
        isOpening.set(false)
        isCaptureInFlight.set(false)
    }

    private companion object {
        const val TAG = "Camera2CaptureService"
        const val DEFAULT_PREVIEW_WIDTH = 1280
        const val DEFAULT_PREVIEW_HEIGHT = 720
        const val MAX_CAPTURE_WIDTH = 1920
        const val MAX_CAPTURE_HEIGHT = 1080
        const val MAX_CAPTURE_IMAGES = 2
    }
}

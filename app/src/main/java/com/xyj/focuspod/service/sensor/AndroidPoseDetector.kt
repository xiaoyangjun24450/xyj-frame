package com.xyj.focuspod.service.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.util.Log

class AndroidPoseDetector(
    context: Context,
    private val handler: Handler,
    private val stabilityDetector: FlipStabilityDetector = FlipStabilityDetector()
) : PoseDetector, SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val poseSensor = accelerometer ?: gravitySensor

    private var targetPose: DevicePose? = null
    private var callback: (() -> Unit)? = null
    private var isListening = false
    private var filteredZAcceleration = 0f
    private var hasAccelerationSample = false

    override fun waitForFlip(callback: () -> Unit) {
        waitForPose(DevicePose.CAMERA_BACK_DOWN, callback)
    }

    override fun waitForFaceUp(callback: () -> Unit) {
        waitForPose(DevicePose.SCREEN_DOWN, callback)
    }

    override fun cancel() {
        if (isListening) {
            sensorManager.unregisterListener(this)
        }
        isListening = false
        callback = null
        targetPose = null
        hasAccelerationSample = false
        stabilityDetector.reset()
    }

    override fun release() {
        cancel()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val waitingForPose = targetPose ?: return
        val zAcceleration = zAccelerationFrom(event)
        val nowMs = System.currentTimeMillis()

        if (!stabilityDetector.update(zAcceleration, nowMs, waitingForPose)) return

        val readyCallback = callback ?: return
        cancel()
        handler.post(readyCallback)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun waitForPose(pose: DevicePose, callback: () -> Unit) {
        cancel()
        targetPose = pose
        this.callback = callback
        stabilityDetector.reset()

        val sensor = poseSensor
        if (sensor == null) {
            Log.w(TAG, "No gravity or accelerometer sensor found; triggering $pose without IMU data.")
            triggerWithoutSensor(callback)
            return
        }

        isListening = sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_UI,
            handler
        )
        if (!isListening) {
            Log.w(TAG, "Failed to register pose sensor; triggering $pose without IMU data.")
            triggerWithoutSensor(callback)
        }
    }

    private fun zAccelerationFrom(event: SensorEvent): Float {
        if (event.sensor.type == Sensor.TYPE_GRAVITY) {
            return -event.values[Z_AXIS]
        }

        filteredZAcceleration = if (!hasAccelerationSample) {
            hasAccelerationSample = true
            event.values[Z_AXIS]
        } else {
            LOW_PASS_ALPHA * filteredZAcceleration + (1f - LOW_PASS_ALPHA) * event.values[Z_AXIS]
        }
        return filteredZAcceleration
    }

    private fun triggerWithoutSensor(callback: () -> Unit) {
        cancel()
        handler.post(callback)
    }

    private companion object {
        const val TAG = "AndroidPoseDetector"
        const val Z_AXIS = 2
        const val LOW_PASS_ALPHA = 0.8f
    }
}

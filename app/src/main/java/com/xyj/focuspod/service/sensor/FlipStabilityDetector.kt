package com.xyj.focuspod.service.sensor

enum class DevicePose {
    CAMERA_BACK_DOWN,
    SCREEN_DOWN,
    UNKNOWN
}

class FlipStabilityDetector(
    private val stableDurationMs: Long = DEFAULT_STABLE_DURATION_MS,
    private val verticalGravityThreshold: Float = DEFAULT_VERTICAL_GRAVITY_THRESHOLD
) {
    private var candidatePose: DevicePose = DevicePose.UNKNOWN
    private var candidateSinceMs: Long = 0L

    fun reset() {
        candidatePose = DevicePose.UNKNOWN
        candidateSinceMs = 0L
    }

    fun detectPose(zAcceleration: Float): DevicePose {
        // Accelerometer Z is positive when the screen faces up, so the camera/back side faces down.
        return when {
            zAcceleration >= verticalGravityThreshold -> DevicePose.CAMERA_BACK_DOWN
            zAcceleration <= -verticalGravityThreshold -> DevicePose.SCREEN_DOWN
            else -> DevicePose.UNKNOWN
        }
    }

    fun update(zAcceleration: Float, timestampMs: Long, targetPose: DevicePose): Boolean {
        val pose = detectPose(zAcceleration)
        if (pose != targetPose) {
            reset()
            return false
        }

        if (candidatePose != pose) {
            candidatePose = pose
            candidateSinceMs = timestampMs
            return false
        }

        return timestampMs - candidateSinceMs >= stableDurationMs
    }

    companion object {
        const val DEFAULT_STABLE_DURATION_MS = 800L
        const val DEFAULT_VERTICAL_GRAVITY_THRESHOLD = 7.0f
    }
}

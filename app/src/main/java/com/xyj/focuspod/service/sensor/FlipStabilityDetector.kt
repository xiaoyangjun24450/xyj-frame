package com.xyj.focuspod.service.sensor

enum class DevicePose {
    CAMERA_BACK_DOWN,
    SCREEN_DOWN,
    UPRIGHT,
    UNKNOWN
}

class FlipStabilityDetector(
    private val stableDurationMs: Long = DEFAULT_STABLE_DURATION_MS,
    private val verticalGravityThreshold: Float = DEFAULT_VERTICAL_GRAVITY_THRESHOLD,
    private val uprightZThreshold: Float = DEFAULT_UPRIGHT_Z_THRESHOLD
) {
    private var candidatePose: DevicePose = DevicePose.UNKNOWN
    private var candidateSinceMs: Long = 0L

    fun reset() {
        candidatePose = DevicePose.UNKNOWN
        candidateSinceMs = 0L
    }

    fun detectPose(zAcceleration: Float): DevicePose {
        return when {
            kotlin.math.abs(zAcceleration) < uprightZThreshold -> DevicePose.UPRIGHT
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
        const val DEFAULT_STABLE_DURATION_MS = 3_000L
        const val DEFAULT_VERTICAL_GRAVITY_THRESHOLD = 7.0f
        const val DEFAULT_UPRIGHT_Z_THRESHOLD = 2.0f
    }
}

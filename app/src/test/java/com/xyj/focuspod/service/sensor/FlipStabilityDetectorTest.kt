package com.xyj.focuspod.service.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipStabilityDetectorTest {
    @Test
    fun detectPoseUsesGravityZDirection() {
        val detector = FlipStabilityDetector()

        assertEquals(DevicePose.CAMERA_BACK_DOWN, detector.detectPose(8.2f))
        assertEquals(DevicePose.SCREEN_DOWN, detector.detectPose(-8.2f))
        assertEquals(DevicePose.UNKNOWN, detector.detectPose(2.0f))
    }

    @Test
    fun updateTriggersOnlyAfterTargetPoseIsStable() {
        val detector = FlipStabilityDetector(stableDurationMs = 800L)

        assertFalse(detector.update(8.0f, timestampMs = 1_000L, targetPose = DevicePose.CAMERA_BACK_DOWN))
        assertFalse(detector.update(8.0f, timestampMs = 1_500L, targetPose = DevicePose.CAMERA_BACK_DOWN))
        assertTrue(detector.update(8.0f, timestampMs = 1_800L, targetPose = DevicePose.CAMERA_BACK_DOWN))
    }

    @Test
    fun updateResetsWhenPoseBecomesUnstable() {
        val detector = FlipStabilityDetector(stableDurationMs = 800L)

        assertFalse(detector.update(8.0f, timestampMs = 1_000L, targetPose = DevicePose.CAMERA_BACK_DOWN))
        assertFalse(detector.update(0.5f, timestampMs = 1_500L, targetPose = DevicePose.CAMERA_BACK_DOWN))
        assertFalse(detector.update(8.0f, timestampMs = 1_900L, targetPose = DevicePose.CAMERA_BACK_DOWN))
        assertFalse(detector.update(8.0f, timestampMs = 2_500L, targetPose = DevicePose.CAMERA_BACK_DOWN))
        assertTrue(detector.update(8.0f, timestampMs = 2_700L, targetPose = DevicePose.CAMERA_BACK_DOWN))
    }
}

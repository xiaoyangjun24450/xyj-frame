package com.xyj.focuspod.ui

import com.xyj.focuspod.model.DoorStatus
import com.xyj.focuspod.model.GradingStep
import com.xyj.focuspod.model.StudyStage
import com.xyj.focuspod.ui.component.doorStatusText
import com.xyj.focuspod.ui.component.gradingStepText
import com.xyj.focuspod.ui.component.stageText
import org.junit.Assert.assertEquals
import org.junit.Test

class UiTextTest {
    @Test
    fun stageTextShowsLearningStage() {
        assertEquals("例题讲解", stageText(StudyStage.EXAMPLE))
        assertEquals("考试", stageText(StudyStage.EXAM))
    }

    @Test
    fun gradingStepTextShowsCurrentProgress() {
        assertEquals("准备拍摄\n5 秒后自动拍照", gradingStepText(GradingStep.PREPARING, 5))
        assertEquals("正在拍摄", gradingStepText(GradingStep.CAPTURING))
        assertEquals("正在上传", gradingStepText(GradingStep.UPLOADING))
        assertEquals("正在批改", gradingStepText(GradingStep.GRADING))
        assertEquals("批改完成\n请把手机翻回正面", gradingStepText(GradingStep.WAITING_FLIP_BACK))
    }

    @Test
    fun doorStatusTextShowsDoorProgress() {
        assertEquals("正在开门", doorStatusText(DoorStatus.OPENING))
        assertEquals("开门成功", doorStatusText(DoorStatus.OPENED))
        assertEquals("开门失败", doorStatusText(DoorStatus.FAILED))
    }
}

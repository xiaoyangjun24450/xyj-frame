package com.xyj.focuspod.ui.component

import com.xyj.focuspod.model.DoorStatus
import com.xyj.focuspod.model.GradingStep
import com.xyj.focuspod.model.StudyStage

fun stageText(stage: StudyStage): String {
    return when (stage) {
        StudyStage.EXAMPLE -> "例题讲解"
        StudyStage.EXAM -> "考试"
    }
}

fun gradingStepText(step: GradingStep, countdownSeconds: Int = 0): String {
    return when (step) {
        GradingStep.PREPARING -> "准备拍摄\n${countdownSeconds.coerceAtLeast(0)} 秒后自动拍照"
        GradingStep.CAPTURING -> "正在拍摄"
        GradingStep.UPLOADING -> "正在上传"
        GradingStep.GRADING -> "正在批改"
        GradingStep.WAITING_FLIP_BACK -> "批改完成\n请把手机翻回正面"
    }
}

fun doorStatusText(status: DoorStatus): String {
    return when (status) {
        DoorStatus.OPENING -> "正在开门"
        DoorStatus.OPENED -> "开门成功"
        DoorStatus.FAILED -> "开门失败"
    }
}

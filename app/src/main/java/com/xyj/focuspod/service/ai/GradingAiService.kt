package com.xyj.focuspod.service.ai

import com.xyj.focuspod.model.GradeResult
import com.xyj.focuspod.model.Question
import com.xyj.focuspod.model.StudyStage

interface GradingAiService {
    fun grade(
        stage: StudyStage,
        question: Question,
        imagePath: String,
        examRound: Int,
        callback: (Result<GradeResult>) -> Unit
    )
}

package com.xyj.focuspod.mock

import android.os.Handler
import android.os.Looper
import com.xyj.focuspod.model.GradeResult
import com.xyj.focuspod.model.Question
import com.xyj.focuspod.model.StudyStage
import com.xyj.focuspod.service.ai.GradingAiService

class FakeGradingAiService(
    private val handler: Handler = Handler(Looper.getMainLooper())
) : GradingAiService {

    override fun grade(
        stage: StudyStage,
        question: Question,
        imagePath: String,
        examRound: Int,
        callback: (Result<GradeResult>) -> Unit
    ) {
        handler.postDelayed({
            val score = when (stage) {
                StudyStage.EXAMPLE -> 92
                StudyStage.EXAM -> if (examRound <= 1 && question.id == "exam-2") 58 else 92
            }
            callback(
                Result.success(
                    GradeResult(
                        questionId = question.id,
                        stage = stage,
                        score = score,
                        passed = score >= 80,
                        feedback = if (score >= 80) {
                            "思路清楚，关键步骤完整。请继续保持书写稳定。"
                        } else {
                            "本题关键关系式不够准确。系统会把它整理成新的例题，讲清后再考试。"
                        }
                    )
                )
            )
        }, MOCK_DELAY_LONG_MS)
    }
}

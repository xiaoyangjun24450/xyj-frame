package com.xyj.focuspod.flow

import com.xyj.focuspod.model.GradeResult

object ExamScoring {
    const val TOTAL_EXAM_SCORE = 100.0
    private const val FULL_SCORE_EPSILON = 0.01
    private const val PASS_SCORE_EPSILON = 0.0001

    fun questionMaxScore(examQuestionCount: Int): Double {
        return if (examQuestionCount <= 0) 0.0 else TOTAL_EXAM_SCORE / examQuestionCount
    }

    fun totalScore(results: List<GradeResult>): Double {
        return results.sumOf { if (isFullScore(it)) it.maxScore else it.score }
            .coerceIn(0.0, TOTAL_EXAM_SCORE)
    }

    fun mistakes(results: List<GradeResult>): List<GradeResult> {
        return results.filter { !isFullScore(it) }
    }

    fun isPassed(totalScore: Double, passScore: Int): Boolean {
        return totalScore + PASS_SCORE_EPSILON >= passScore
    }

    private fun isFullScore(result: GradeResult): Boolean {
        return result.score + FULL_SCORE_EPSILON >= result.maxScore
    }
}

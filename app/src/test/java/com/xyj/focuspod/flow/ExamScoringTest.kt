package com.xyj.focuspod.flow

import com.xyj.focuspod.model.GradeResult
import com.xyj.focuspod.model.StudyStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamScoringTest {
    @Test
    fun questionMaxScoreSplitsTotalScoreAcrossExamQuestions() {
        assertEquals(50.0, ExamScoring.questionMaxScore(2), 0.0001)
        assertEquals(25.0, ExamScoring.questionMaxScore(4), 0.0001)
    }

    @Test
    fun totalScoreSumsQuestionScoresInsteadOfAveraging() {
        val results = listOf(
            result(score = 50.0, maxScore = 50.0),
            result(score = 25.0, maxScore = 50.0)
        )

        assertEquals(75.0, ExamScoring.totalScore(results), 0.0001)
    }

    @Test
    fun mistakesAreQuestionsThatDidNotReceiveFullScore() {
        val fullScore = result(questionId = "full", score = 50.0, maxScore = 50.0)
        val partialScore = result(questionId = "partial", score = 49.0, maxScore = 50.0)

        val mistakes = ExamScoring.mistakes(listOf(fullScore, partialScore))

        assertEquals(listOf(partialScore), mistakes)
    }

    @Test
    fun roundedFullScoresAreTreatedAsFullScores() {
        val maxScore = ExamScoring.questionMaxScore(3)
        val results = listOf(
            result(questionId = "first", score = 33.33, maxScore = maxScore),
            result(questionId = "second", score = 33.33, maxScore = maxScore),
            result(questionId = "third", score = 33.33, maxScore = maxScore)
        )

        assertEquals(emptyList<GradeResult>(), ExamScoring.mistakes(results))
        assertEquals(100.0, ExamScoring.totalScore(results), 0.0001)
    }

    @Test
    fun passScoreIsAppliedOnlyToFinalExamTotal() {
        assertTrue(ExamScoring.isPassed(totalScore = 80.0, passScore = 80))
    }

    private fun result(
        questionId: String = "question",
        score: Double,
        maxScore: Double
    ): GradeResult {
        return GradeResult(
            questionId = questionId,
            stage = StudyStage.EXAM,
            score = score,
            feedback = "已批改",
            maxScore = maxScore
        )
    }
}

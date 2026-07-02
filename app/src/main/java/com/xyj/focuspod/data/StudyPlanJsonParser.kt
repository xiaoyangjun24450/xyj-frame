package com.xyj.focuspod.data

import com.xyj.focuspod.model.Question
import com.xyj.focuspod.model.StudyPlan
import org.json.JSONArray
import org.json.JSONObject

object StudyPlanJsonParser {
    fun parsePlans(json: String): List<StudyPlan> {
        val root = JSONObject(json)
        return root.getJSONArray("plans").toStudyPlans()
    }

    private fun JSONArray.toStudyPlans(): List<StudyPlan> {
        return List(length()) { index ->
            getJSONObject(index).toStudyPlan()
        }
    }

    private fun JSONObject.toStudyPlan(): StudyPlan {
        return StudyPlan(
            id = getString("id"),
            title = getString("title"),
            subject = getString("subject"),
            passScore = getInt("passScore"),
            exampleQuestions = getJSONArray("exampleQuestions").toQuestions(),
            examQuestions = getJSONArray("examQuestions").toQuestions(),
            estimatedMinutes = optInt("estimatedMinutes", 0)
        )
    }

    private fun JSONArray.toQuestions(): List<Question> {
        return List(length()) { index ->
            getJSONObject(index).toQuestion()
        }
    }

    private fun JSONObject.toQuestion(): Question {
        return Question(
            id = getString("id"),
            title = getString("title"),
            questionMarkdown = getString("questionMarkdown"),
            answer = getString("answer"),
            solutionMarkdown = getString("solutionMarkdown"),
            knowledgePoints = getJSONArray("knowledgePoints").toStringList(),
            tutoringPrompt = getString("tutoringPrompt"),
            gradingPrompt = getString("gradingPrompt")
        )
    }

    private fun JSONArray.toStringList(): List<String> {
        return List(length()) { index -> getString(index) }
    }
}

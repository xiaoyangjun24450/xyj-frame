package com.xyj.focuspod.model

enum class StudyPage {
    START,
    PLAN_SELECT,
    DEVICE_SELF_CHECK,
    TUTORING,
    EXAM,
    GRADING,
    GRADE_RESULT,
    EXAM_SCORE
}

enum class StudyStage {
    EXAMPLE,
    EXAM
}

data class Question(
    val id: String,
    val title: String,
    val questionMarkdown: String,
    val answer: String,
    val solutionMarkdown: String,
    val knowledgePoints: List<String>,
    val tutoringPrompt: String,
    val gradingPrompt: String,
    val source: QuestionSource = QuestionSource.ORIGINAL
)

enum class QuestionSource {
    ORIGINAL,
    MISTAKE_REVIEW
}

data class StudyPlan(
    val id: String,
    val title: String,
    val subject: String,
    val passScore: Int,
    val exampleQuestions: List<Question>,
    val examQuestions: List<Question>,
    val estimatedMinutes: Int = 0
)

data class GradeResult(
    val questionId: String,
    val stage: StudyStage,
    val score: Double,
    val feedback: String,
    val reason: String = "",
    val suggestion: String = "",
    val maxScore: Double = 100.0
)

enum class TutoringSpeaker {
    AI,
    STUDENT
}

data class TutoringMessage(
    val id: String,
    val speaker: TutoringSpeaker,
    val text: String
)

enum class DeviceCheckStatus {
    CHECKING,
    PASSED,
    FAILED
}

data class DeviceCheckItem(
    val name: String,
    val status: DeviceCheckStatus
)

enum class GradingStep {
    PREPARING,
    CAPTURING,
    UPLOADING,
    GRADING,
    WAITING_FLIP_BACK
}

enum class DoorStatus {
    OPENING,
    OPENED,
    FAILED
}

data class StudySessionState(
    val page: StudyPage = StudyPage.START,
    val stage: StudyStage = StudyStage.EXAMPLE,
    val selectedPlan: StudyPlan? = null,
    val currentQuestion: Question? = null,
    val remainingCount: Int = 0,
    val lastGradeResult: GradeResult? = null,
    val alertMessage: String? = null,
    val studentName: String = "学生 001",
    val networkOnline: Boolean = true,
    val availablePlans: List<StudyPlan> = emptyList(),
    val deviceChecks: List<DeviceCheckItem> = emptyList(),
    val voiceCaption: String = "",
    val tutoringMessages: List<TutoringMessage> = emptyList(),
    val gradingStep: GradingStep = GradingStep.PREPARING,
    val gradingCountdownSeconds: Int = 10,
    val resultCountdownSeconds: Int = 10,
    val doorStatus: DoorStatus = DoorStatus.OPENING,
    val totalExamScore: Double = 0.0,
    val examPassed: Boolean? = null,
    val completedQuestionCount: Int = 0,
    val mistakeCount: Int = 0
)

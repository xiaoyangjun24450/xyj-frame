package com.xyj.focuspod.model

enum class StudyPage {
    START,
    PLAN_SELECT,
    DEVICE_SELF_CHECK,
    TUTORING,
    EXAM,
    GRADING,
    GRADE_RESULT,
    DONE
}

enum class StudyStage {
    EXAMPLE,
    EXAM
}

data class Question(
    val id: String,
    val title: String,
    val prompt: String,
    val subject: String,
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
    val estimatedMinutes: Int,
    val passScore: Int,
    val exampleQuestions: List<Question>,
    val examQuestions: List<Question>
)

data class GradeResult(
    val questionId: String,
    val stage: StudyStage,
    val score: Int,
    val passed: Boolean,
    val feedback: String
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
    val gradingStep: GradingStep = GradingStep.CAPTURING,
    val resultCountdownSeconds: Int = 10,
    val doorStatus: DoorStatus = DoorStatus.OPENING,
    val totalExamScore: Int = 0,
    val completedQuestionCount: Int = 0,
    val mistakeCount: Int = 0
)

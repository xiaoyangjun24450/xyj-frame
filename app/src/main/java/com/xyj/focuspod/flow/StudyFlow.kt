package com.xyj.focuspod.flow

import android.os.Handler
import com.xyj.focuspod.mock.RESULT_COUNTDOWN_SECONDS
import com.xyj.focuspod.mock.START_DELAY_MS
import com.xyj.focuspod.model.DeviceCheckItem
import com.xyj.focuspod.model.DeviceCheckStatus
import com.xyj.focuspod.model.DoorStatus
import com.xyj.focuspod.model.GradeResult
import com.xyj.focuspod.model.GradingStep
import com.xyj.focuspod.model.Question
import com.xyj.focuspod.model.QuestionSource
import com.xyj.focuspod.model.StudyPage
import com.xyj.focuspod.model.StudyPlan
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.model.StudyStage
import com.xyj.focuspod.service.ai.GradingAiService
import com.xyj.focuspod.service.api.StudyPlanApi
import com.xyj.focuspod.service.camera.CameraCaptureService
import com.xyj.focuspod.service.device.DoorControlService
import com.xyj.focuspod.service.sensor.PoseDetector

class StudyFlow(
    private val handler: Handler,
    private val studyPlanApi: StudyPlanApi,
    private val poseDetector: PoseDetector,
    private val cameraCaptureService: CameraCaptureService,
    private val gradingAiService: GradingAiService,
    private val doorControlService: DoorControlService
) {
    private var state = StudySessionState()
    private val listeners = mutableSetOf<(StudySessionState) -> Unit>()
    private var activeStageQuestions: List<Question> = emptyList()
    private var exampleIndex = 0
    private var examIndex = 0
    private var examRound = 1
    private val examResults = mutableListOf<GradeResult>()
    private var completedAttemptCount = 0
    private var hasStartedCurrentPage = false

    fun observe(listener: (StudySessionState) -> Unit) {
        listeners += listener
        listener(state)
    }

    fun removeObserver(listener: (StudySessionState) -> Unit) {
        listeners -= listener
    }

    fun start() {
        hasStartedCurrentPage = false
        update(
            state.copy(
                page = StudyPage.START,
                voiceCaption = "正在启动，请稍候",
                alertMessage = null
            )
        )
        handler.postDelayed({
            loadPlans()
        }, START_DELAY_MS)
    }

    fun selectPlan(plan: StudyPlan) {
        activeStageQuestions = plan.exampleQuestions
        exampleIndex = 0
        examIndex = 0
        examRound = 1
        examResults.clear()
        completedAttemptCount = 0
        update(
            state.copy(
                page = StudyPage.DEVICE_SELF_CHECK,
                stage = StudyStage.EXAMPLE,
                selectedPlan = plan,
                currentQuestion = null,
                remainingCount = plan.exampleQuestions.size,
                lastGradeResult = null,
                alertMessage = null,
                deviceChecks = initialDeviceChecks(),
                voiceCaption = "请把手机放入手机仓并关闭仓门，系统开始自检。",
                completedQuestionCount = 0,
                mistakeCount = 0,
                totalExamScore = 0
            )
        )
        runDeviceSelfCheck()
    }

    fun enterCurrentPage() {
        if (hasStartedCurrentPage) return
        hasStartedCurrentPage = true
        when (state.page) {
            StudyPage.TUTORING,
            StudyPage.EXAM -> waitForFlipToGrade()
            StudyPage.GRADING -> runGrading()
            StudyPage.GRADE_RESULT -> runResultCountdown()
            StudyPage.DONE -> runDoorOpen()
            else -> Unit
        }
    }

    private fun loadPlans() {
        studyPlanApi.getPlans { result ->
            result.fold(
                onSuccess = { plans ->
                    update(
                        state.copy(
                            page = StudyPage.PLAN_SELECT,
                            availablePlans = plans,
                            voiceCaption = "请选择学习计划。",
                            alertMessage = if (plans.isEmpty()) "暂无学习计划，请联系老师。" else null
                        )
                    )
                },
                onFailure = {
                    update(state.copy(page = StudyPage.PLAN_SELECT, alertMessage = "学习计划加载失败，请稍后重试。"))
                }
            )
        }
    }

    private fun runDeviceSelfCheck() {
        val names = listOf("相机", "麦克风", "网络", "姿态传感器", "USB 手机仓")
        names.forEachIndexed { index, name ->
            handler.postDelayed({
                val checks = names.mapIndexed { itemIndex, itemName ->
                    DeviceCheckItem(
                        name = itemName,
                        status = if (itemIndex <= index) DeviceCheckStatus.PASSED else DeviceCheckStatus.CHECKING
                    )
                }
                update(
                    state.copy(
                        deviceChecks = checks,
                        voiceCaption = "$name 检查通过。"
                    )
                )
                if (index == names.lastIndex) {
                    handler.postDelayed({ enterExampleQuestion() }, 300L)
                }
            }, (index + 1) * 1_000L)
        }
    }

    private fun enterExampleQuestion() {
        val question = activeStageQuestions.getOrNull(exampleIndex)
        if (question == null) {
            enterExamQuestion()
            return
        }
        update(
            state.copy(
                page = StudyPage.TUTORING,
                stage = StudyStage.EXAMPLE,
                currentQuestion = question,
                remainingCount = activeStageQuestions.size - exampleIndex,
                lastGradeResult = null,
                alertMessage = null,
                voiceCaption = "正在讲解 ${question.title}。完成后请翻转手机开始批卷。"
            )
        )
    }

    private fun enterExamQuestion() {
        val plan = state.selectedPlan ?: return
        activeStageQuestions = plan.examQuestions
        val question = activeStageQuestions.getOrNull(examIndex)
        if (question == null) {
            finishExamRound()
            return
        }
        update(
            state.copy(
                page = StudyPage.EXAM,
                stage = StudyStage.EXAM,
                currentQuestion = question,
                remainingCount = activeStageQuestions.size - examIndex,
                lastGradeResult = null,
                alertMessage = null,
                voiceCaption = "考试开始。写完后请翻转手机提交本题。"
            )
        )
    }

    private fun waitForFlipToGrade() {
        val page = state.page
        update(state.copy(voiceCaption = "系统正在等待手机翻转。"))
        poseDetector.waitForFlip {
            if (state.page == page) {
                update(
                    state.copy(
                        page = StudyPage.GRADING,
                        gradingStep = GradingStep.CAPTURING,
                        voiceCaption = "检测到翻转，开始拍摄并批改。"
                    )
                )
            }
        }
    }

    private fun runGrading() {
        val question = state.currentQuestion ?: return
        update(state.copy(gradingStep = GradingStep.CAPTURING, voiceCaption = "正在拍摄纸面答案，请保持手机稳定。"))
        cameraCaptureService.captureAnswer { captureResult ->
            captureResult.fold(
                onSuccess = { imagePath ->
                    update(state.copy(gradingStep = GradingStep.UPLOADING, voiceCaption = "答案已拍摄，正在上传。"))
                    handler.postDelayed({
                        update(state.copy(gradingStep = GradingStep.GRADING, voiceCaption = "上传完成，AI 正在批改。"))
                        gradingAiService.grade(state.stage, question, imagePath, examRound) { gradeResult ->
                            gradeResult.fold(
                                onSuccess = { result -> waitForFaceUp(result) },
                                onFailure = { update(state.copy(alertMessage = "AI 批改失败，系统将自动重试。")) }
                            )
                        }
                    }, 900L)
                },
                onFailure = {
                    update(state.copy(alertMessage = "照片不清晰，请保持手机稳定，系统将自动重试。"))
                    handler.postDelayed({ runGrading() }, 1_000L)
                }
            )
        }
    }

    private fun waitForFaceUp(result: GradeResult) {
        update(
            state.copy(
                gradingStep = GradingStep.WAITING_FLIP_BACK,
                lastGradeResult = result,
                voiceCaption = "批改完成，请把手机翻回正面。"
            )
        )
        poseDetector.waitForFaceUp {
            update(
                state.copy(
                    page = StudyPage.GRADE_RESULT,
                    lastGradeResult = result,
                    resultCountdownSeconds = RESULT_COUNTDOWN_SECONDS,
                    completedQuestionCount = completedQuestionCountAfter(result),
                    voiceCaption = "本题结果已生成，10 秒后进入下一步。"
                )
            )
        }
    }

    private fun runResultCountdown() {
        for (second in RESULT_COUNTDOWN_SECONDS downTo 1) {
            handler.postDelayed({
                if (state.page == StudyPage.GRADE_RESULT) {
                    update(state.copy(resultCountdownSeconds = second))
                }
            }, (RESULT_COUNTDOWN_SECONDS - second) * 1_000L)
        }
        handler.postDelayed({
            if (state.page == StudyPage.GRADE_RESULT) {
                advanceAfterResult()
            }
        }, RESULT_COUNTDOWN_SECONDS * 1_000L)
    }

    private fun advanceAfterResult() {
        val result = state.lastGradeResult ?: return
        if (result.stage == StudyStage.EXAMPLE) {
            exampleIndex += 1
            enterExampleQuestion()
            return
        }

        examResults += result
        examIndex += 1
        if (examIndex < activeStageQuestions.size) {
            enterExamQuestion()
        } else {
            finishExamRound()
        }
    }

    private fun finishExamRound() {
        val plan = state.selectedPlan ?: return
        val averageScore = if (examResults.isEmpty()) 0 else examResults.sumOf { it.score } / examResults.size
        val mistakes = examResults.filter { !it.passed }
        if (averageScore >= plan.passScore && mistakes.isEmpty()) {
            update(
                state.copy(
                    page = StudyPage.DONE,
                    stage = StudyStage.EXAM,
                    currentQuestion = null,
                    remainingCount = 0,
                    totalExamScore = averageScore,
                    mistakeCount = 0,
                    doorStatus = DoorStatus.OPENING,
                    voiceCaption = "考试达标，学习完成，正在打开手机仓。"
                )
            )
            return
        }

        activeStageQuestions = mistakes.mapIndexed { index, result ->
            val original = plan.examQuestions.firstOrNull { it.id == result.questionId }
            Question(
                id = "review-${examRound}-${index + 1}",
                title = "错题讲解 ${index + 1}",
                prompt = original?.prompt ?: "请复盘本题的关键步骤。",
                subject = original?.subject ?: plan.subject,
                source = QuestionSource.MISTAKE_REVIEW
            )
        }
        exampleIndex = 0
        examIndex = 0
        examRound += 1
        examResults.clear()
        update(
            state.copy(
                stage = StudyStage.EXAMPLE,
                mistakeCount = activeStageQuestions.size,
                totalExamScore = averageScore,
                voiceCaption = "本轮考试未达标，错题已整理成新的例题。"
            )
        )
        handler.postDelayed({ enterExampleQuestion() }, 800L)
    }

    private fun runDoorOpen() {
        doorControlService.openDoor { result ->
            result.fold(
                onSuccess = {
                    update(
                        state.copy(
                            doorStatus = DoorStatus.OPENED,
                            voiceCaption = "手机仓已打开，本次学习完成。"
                        )
                    )
                },
                onFailure = {
                    update(
                        state.copy(
                            doorStatus = DoorStatus.FAILED,
                            alertMessage = "开门失败，请联系老师或工作人员。",
                            voiceCaption = "开门失败，请联系老师或工作人员。"
                        )
                    )
                }
            )
        }
    }

    private fun completedQuestionCountAfter(result: GradeResult): Int {
        completedAttemptCount += 1
        return completedAttemptCount
    }

    private fun initialDeviceChecks(): List<DeviceCheckItem> {
        return listOf("相机", "麦克风", "网络", "姿态传感器", "USB 手机仓").map {
            DeviceCheckItem(it, DeviceCheckStatus.CHECKING)
        }
    }

    private fun update(newState: StudySessionState) {
        val previousPage = state.page
        state = newState
        if (previousPage != newState.page) {
            hasStartedCurrentPage = false
        }
        listeners.forEach { it(state) }
    }
}

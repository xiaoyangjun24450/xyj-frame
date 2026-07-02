package com.xyj.focuspod.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.xyj.focuspod.flow.StudyFlow
import com.xyj.focuspod.model.StudyPage
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.ui.theme.FocusPodTheme

@Composable
fun FocusPodApp(state: StudySessionState, flow: StudyFlow) {
    FocusPodTheme {
        LaunchedEffect(state.page) {
            flow.enterCurrentPage()
        }

        when (state.page) {
            StudyPage.START -> StartScreen(state)
            StudyPage.PLAN_SELECT -> PlanSelectScreen(state, flow)
            StudyPage.DEVICE_SELF_CHECK -> DeviceSelfCheckScreen(state)
            StudyPage.TUTORING -> TutoringScreen(state)
            StudyPage.EXAM -> ExamScreen(state)
            StudyPage.GRADING -> GradingScreen(state)
            StudyPage.GRADE_RESULT -> GradeResultScreen(state)
            StudyPage.DONE -> DoneScreen(state)
        }
    }
}

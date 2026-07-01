package com.xyj.focuspod

import android.app.Activity
import android.os.Bundle
import com.xyj.focuspod.app.AppContainer
import com.xyj.focuspod.model.StudyPage
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.screen.deviceSelfCheckScreen
import com.xyj.focuspod.screen.doneScreen
import com.xyj.focuspod.screen.examScreen
import com.xyj.focuspod.screen.gradeResultScreen
import com.xyj.focuspod.screen.gradingScreen
import com.xyj.focuspod.screen.planSelectScreen
import com.xyj.focuspod.screen.startScreen
import com.xyj.focuspod.screen.tutoringScreen

class MainActivity : Activity() {
    private val appContainer = AppContainer()
    private val studyFlow = appContainer.studyFlow
    private val observer: (StudySessionState) -> Unit = { state ->
        render(state)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        studyFlow.observe(observer)
        studyFlow.start()
    }

    override fun onDestroy() {
        studyFlow.removeObserver(observer)
        super.onDestroy()
    }

    private fun render(state: StudySessionState) {
        val view = when (state.page) {
            StudyPage.START -> startScreen(this, state)
            StudyPage.PLAN_SELECT -> planSelectScreen(this, state, studyFlow)
            StudyPage.DEVICE_SELF_CHECK -> deviceSelfCheckScreen(this, state)
            StudyPage.TUTORING -> tutoringScreen(this, state)
            StudyPage.EXAM -> examScreen(this, state)
            StudyPage.GRADING -> gradingScreen(this, state)
            StudyPage.GRADE_RESULT -> gradeResultScreen(this, state)
            StudyPage.DONE -> doneScreen(this, state)
        }
        setContentView(view)
        view.post { studyFlow.enterCurrentPage() }
    }
}

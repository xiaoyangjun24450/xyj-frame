package com.xyj.focuspod

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.xyj.focuspod.app.AppContainer
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.screen.FocusPodApp

class MainActivity : ComponentActivity() {
    private val appContainer = AppContainer()
    private val studyFlow = appContainer.studyFlow
    private val currentState = mutableStateOf(StudySessionState())
    private val observer: (StudySessionState) -> Unit = { state ->
        currentState.value = state
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        studyFlow.observe(observer)
        setContent {
            FocusPodApp(
                state = currentState.value,
                flow = studyFlow
            )
        }
        studyFlow.start()
    }

    override fun onDestroy() {
        studyFlow.removeObserver(observer)
        super.onDestroy()
    }
}

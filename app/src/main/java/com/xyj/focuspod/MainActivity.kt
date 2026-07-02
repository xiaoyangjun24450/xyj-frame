package com.xyj.focuspod

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableStateOf
import com.xyj.focuspod.app.AppContainer
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.screen.FocusPodApp

class MainActivity : ComponentActivity() {
    private lateinit var appContainer: AppContainer
    private lateinit var studyFlow: com.xyj.focuspod.flow.StudyFlow
    private val currentState = mutableStateOf(StudySessionState())
    private val observer: (StudySessionState) -> Unit = { state ->
        currentState.value = state
    }
    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // StudyFlow surfaces SDK permission failures in the tutoring page if permission is denied.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContainer = AppContainer(this, application)
        studyFlow = appContainer.studyFlow
        enableEdgeToEdge()
        studyFlow.observe(observer)
        setContent {
            FocusPodApp(
                state = currentState.value,
                flow = studyFlow
            )
        }
        requestAudioPermissionIfNeeded()
        studyFlow.start()
    }

    override fun onDestroy() {
        studyFlow.release()
        studyFlow.removeObserver(observer)
        super.onDestroy()
    }

    private fun requestAudioPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

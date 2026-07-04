package com.xyj.focuspod

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
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
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // StudyFlow surfaces permission failures in the relevant page if permission is denied.
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
        requestRuntimePermissionsIfNeeded()
        studyFlow.start()
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }

    override fun onDestroy() {
        studyFlow.release()
        studyFlow.removeObserver(observer)
        super.onDestroy()
    }

    private fun requestRuntimePermissionsIfNeeded() {
        val missingPermissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        ).filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}

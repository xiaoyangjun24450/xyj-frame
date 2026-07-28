package com.xyj.focuspod.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xyj.focuspod.flow.StudyFlow
import com.xyj.focuspod.model.StudyPlan
import com.xyj.focuspod.model.StudySessionState
import com.xyj.focuspod.service.device.FocusPodUsbCommandSender
import com.xyj.focuspod.ui.component.AppScaffold
import com.xyj.focuspod.ui.component.PlanCard
import com.xyj.focuspod.ui.component.StatusChip
import com.xyj.focuspod.ui.theme.FocusPrimary
import com.xyj.focuspod.ui.theme.FocusSuccess
import com.xyj.focuspod.ui.theme.FocusWarning

@Composable
fun PlanSelectScreen(
    state: StudySessionState,
    flow: StudyFlow
) {
    val context = LocalContext.current
    val usbCommandSender = remember(context) { FocusPodUsbCommandSender(context) }
    var selectedPlan by remember(state.availablePlans) {
        mutableStateOf<StudyPlan?>(state.availablePlans.firstOrNull())
    }
    var usbStatus by remember { mutableStateOf<String?>(null) }

    DisposableEffect(usbCommandSender) {
        usbCommandSender.register()
        onDispose { usbCommandSender.unregister() }
    }

    AppScaffold(
        alertMessage = state.alertMessage,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    enabled = selectedPlan != null,
                    onClick = { selectedPlan?.let(flow::selectPlan) },
                    colors = ButtonDefaults.buttonColors(containerColor = FocusPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .height(56.dp)
                ) {
                    Text(text = "开始本计划", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "选择学习计划", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.studentName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                StatusChip(
                    text = if (state.networkOnline) "网络在线" else "网络离线",
                    color = if (state.networkOnline) FocusSuccess else FocusWarning,
                    textColor = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        usbCommandSender.sendCommand(0x01) { status ->
                            usbStatus = status
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FocusPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text(text = "发送 0x01")
                }
                Button(
                    onClick = {
                        usbCommandSender.sendCommand(0x00) { status ->
                            usbStatus = status
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FocusWarning),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text(text = "发送 0x00")
                }
            }
            usbStatus?.let { status ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(28.dp))

            if (state.availablePlans.isEmpty()) {
                Text(
                    text = "暂无学习计划，请联系老师",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.availablePlans.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        selected = selectedPlan == plan,
                        onClick = { selectedPlan = plan }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

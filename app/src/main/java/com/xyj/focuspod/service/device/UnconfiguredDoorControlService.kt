package com.xyj.focuspod.service.device

import android.os.Handler
import android.os.Looper

class UnconfiguredDoorControlService(
    private val handler: Handler = Handler(Looper.getMainLooper())
) : DoorControlService {

    override fun openDoor(callback: (Result<Unit>) -> Unit) {
        handler.post {
            callback(Result.failure(IllegalStateException("开门服务未配置。")))
        }
    }
}

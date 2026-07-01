package com.xyj.focuspod.mock

import android.os.Handler
import android.os.Looper
import com.xyj.focuspod.service.device.DoorControlService

class FakeDoorControlService(
    private val handler: Handler = Handler(Looper.getMainLooper())
) : DoorControlService {

    override fun openDoor(callback: (Result<Unit>) -> Unit) {
        handler.postDelayed({
            callback(Result.success(Unit))
        }, MOCK_DELAY_LONG_MS)
    }
}

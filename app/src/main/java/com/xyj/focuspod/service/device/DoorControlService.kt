package com.xyj.focuspod.service.device

interface DoorControlService {
    fun openDoor(callback: (Result<Unit>) -> Unit)
}

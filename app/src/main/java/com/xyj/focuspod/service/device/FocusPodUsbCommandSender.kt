package com.xyj.focuspod.service.device

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.Locale

class FocusPodUsbCommandSender(context: Context) {
    private val appContext = context.applicationContext
    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var receiver: BroadcastReceiver? = null
    private var pendingCommand: Int? = null
    private var pendingStatus: ((String) -> Unit)? = null

    fun register() {
        if (receiver != null) return

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != ACTION_USB_PERMISSION) return

                val device = intent.usbDeviceExtra()
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                val command = pendingCommand
                val onStatus = pendingStatus
                pendingCommand = null
                pendingStatus = null

                if (granted && device != null && command != null && onStatus != null) {
                    sendInBackground(device, command, onStatus)
                } else {
                    onStatus?.invoke("USB 授权未完成")
                }
            }
        }

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(receiver, filter)
        }
    }

    fun unregister() {
        receiver?.let(appContext::unregisterReceiver)
        receiver = null
        pendingCommand = null
        pendingStatus = null
    }

    fun sendCommand(command: Int, onStatus: (String) -> Unit) {
        val device = findFocusPodDevice()
        if (device == null) {
            onStatus("未找到 USB 设备 VID=0x5131 PID=0x2008")
            return
        }

        if (!usbManager.hasPermission(device)) {
            pendingCommand = command
            pendingStatus = onStatus
            usbManager.requestPermission(device, permissionIntent())
            onStatus("已请求 USB 授权")
            return
        }

        sendInBackground(device, command, onStatus)
    }

    private fun sendInBackground(device: UsbDevice, command: Int, onStatus: (String) -> Unit) {
        onStatus("正在发送 ${command.hexByte()}")
        Thread {
            val status = sendNow(device, command)
            mainHandler.post { onStatus(status) }
        }.start()
    }

    private fun sendNow(device: UsbDevice, command: Int): String {
        val connection = usbManager.openDevice(device)
            ?: return "USB 设备打开失败"

        return try {
            sendControlCommand(connection, command)
        } finally {
            connection.close()
        }
    }

    private fun sendControlCommand(
        connection: UsbDeviceConnection,
        command: Int
    ): String {
        val sent = connection.controlTransfer(
            UsbConstants.USB_DIR_OUT or UsbConstants.USB_TYPE_VENDOR or USB_RECIP_DEVICE,
            FOCUSPOD_VENDOR_SET_LED,
            command and 0xFF,
            0,
            ByteArray(0),
            0,
            USB_TIMEOUT_MS
        )
        return if (sent >= 0) {
            "已通过 EP0 发送 ${command.hexByte()}"
        } else {
            "EP0 发送失败 sent=$sent"
        }
    }

    private fun findFocusPodDevice(): UsbDevice? {
        return usbManager.deviceList.values.firstOrNull { device ->
            device.vendorId == FOCUSPOD_VENDOR_ID && device.productId == FOCUSPOD_PRODUCT_ID
        }
    }

    private fun permissionIntent(): PendingIntent {
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(appContext.packageName)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        return PendingIntent.getBroadcast(
            appContext,
            USB_PERMISSION_REQUEST_CODE,
            intent,
            flags
        )
    }

    private fun Intent.usbDeviceExtra(): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }

    private fun Int.hexByte(): String {
        return String.format(Locale.US, "0x%02X", this and 0xFF)
    }

    private companion object {
        private const val ACTION_USB_PERMISSION = "com.xyj.focuspod.USB_PERMISSION"
        private const val USB_PERMISSION_REQUEST_CODE = 552
        private const val USB_TIMEOUT_MS = 1000
        private const val USB_RECIP_DEVICE = 0x00
        private const val FOCUSPOD_VENDOR_ID = 0x5131
        private const val FOCUSPOD_PRODUCT_ID = 0x2008
        private const val FOCUSPOD_VENDOR_SET_LED = 0x01
    }
}

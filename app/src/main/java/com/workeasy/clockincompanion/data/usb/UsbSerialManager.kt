package com.workeasy.clockincompanion.data.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class UsbSerialManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val ioMutex = Mutex()
    private var serialPort: UsbSerialPort? = null

    fun hasCompatibleDevice(): Boolean = findDevice() != null

    fun findDevice(): UsbDevice? {
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        return drivers.firstOrNull()?.device
            ?: usbManager.deviceList.values.firstOrNull { device ->
                device.vendorId == CP2102_VENDOR_ID && device.productId == CP2102_PRODUCT_ID
            }
    }

    suspend fun open(): Unit = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            if (serialPort?.isOpen == true) return@withContext

            val device = findDevice()
                ?: error("No USB serial device found (expected CP2102)")

            if (!usbManager.hasPermission(device)) {
                requestPermission(device)
            }

            val connection = usbManager.openDevice(device)
                ?: error("Unable to open USB device connection")

            val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
                ?: error("No serial driver for device")
            val port = driver.ports.firstOrNull()
                ?: error("Device has no serial ports")

            port.open(connection)
            port.setParameters(
                BAUD_RATE,
                8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE,
            )
            serialPort = port
        }
    }

    suspend fun close(): Unit = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            runCatching { serialPort?.close() }
            serialPort = null
        }
    }

    /**
     * Writes [command] and reads until one complete AS608 ACK packet arrives or [timeoutMs].
     */
    suspend fun transact(command: ByteArray, timeoutMs: Long = 2_500L): ByteArray =
        withContext(Dispatchers.IO) {
            ioMutex.withLock {
                val port = serialPort ?: error("Serial port not open")
                // Drain any stale bytes before a new command.
                val drain = ByteArray(256)
                while (port.read(drain, 50) > 0) {
                    // discard
                }
                port.write(command, WRITE_TIMEOUT_MS)

                val buffer = ArrayList<Byte>(64)
                val deadline = System.currentTimeMillis() + timeoutMs
                val chunk = ByteArray(256)
                while (System.currentTimeMillis() < deadline) {
                    val remaining = (deadline - System.currentTimeMillis()).toInt().coerceAtLeast(1)
                    val n = port.read(chunk, remaining.coerceAtMost(200))
                    if (n > 0) {
                        for (i in 0 until n) buffer.add(chunk[i])
                        val arr = buffer.toByteArray()
                        val extracted = As608Protocol.extractAckPacket(arr)
                        if (extracted != null) {
                            return@withLock extracted.first
                        }
                    }
                }
                error("Timed out waiting for sensor response (${buffer.size} bytes)")
            }
        }

    private suspend fun requestPermission(device: UsbDevice) {
        suspendCancellableCoroutine { cont ->
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (intent.action != ACTION_USB_PERMISSION) return
                    ctx.unregisterReceiver(this)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (granted) {
                        cont.resume(Unit)
                    } else {
                        cont.cancel(IllegalStateException("USB permission denied"))
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, filter)
            }
            cont.invokeOnCancellation {
                runCatching { context.unregisterReceiver(receiver) }
            }
            val flags = PendingIntent.FLAG_MUTABLE
            val pi = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                flags,
            )
            usbManager.requestPermission(device, pi)
        }
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.workeasy.clockincompanion.USB_PERMISSION"
        const val BAUD_RATE = 57600
        private const val WRITE_TIMEOUT_MS = 1_000
        private const val CP2102_VENDOR_ID = 0x10C4
        private const val CP2102_PRODUCT_ID = 0xEA60
    }
}

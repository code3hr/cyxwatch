package com.cyxwatch.app.platform.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.system.OsConstants
import android.os.Build
import android.os.ParcelFileDescriptor
import com.cyxwatch.app.data.settings.VpnModeSettingsRepository
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.IOException

class CyxWatchVpnService : VpnService() {
    private val trafficStore by lazy { VpnModeTrafficStore.shared }
    private var workerThread: Thread? = null
    private var vpnDescriptor: ParcelFileDescriptor? = null
    private var vpnInputStream: FileInputStream? = null
    private val packetParser = VpnPacketParser
    private val vpnModeSettingsRepository by lazy { VpnModeSettingsRepository(this) }
    @Volatile private var packetForwarder: VpnModePacketForwarder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateForwardingState()
        if (workerThread == null) {
            startForeground(NOTIFICATION_ID, buildForegroundNotification())
            setVpnModeEnabledState(isEnabled = true)
            workerThread = Thread(
                {
                    try {
                        if (!openVpnInterface()) {
                            setVpnModeEnabledState(isEnabled = false)
                            stopSelf()
                            return@Thread
                        }
                        trafficStore.clear()
                        observePackets()
                    } finally {
                        closeVpnInterface()
                    }
                },
                WORKER_THREAD_NAME,
            )
            workerThread?.start()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        workerThread?.interrupt()
        workerThread = null
        closeVpnInterface()
        setVpnModeEnabledState(isEnabled = false)
        super.onDestroy()
    }

    override fun onRevoke() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        setVpnModeEnabledState(isEnabled = false)
        workerThread?.interrupt()
        workerThread = null
        closeVpnInterface()
        super.onRevoke()
    }

    private fun openVpnInterface(): Boolean {
        if (vpnDescriptor != null) return true

        val builder = Builder()
            .setSession(VPN_SESSION_NAME)
            .setMtu(VPN_MTU)
            .addAddress(VPN_TUN_IP, VPN_TUN_PREFIX)
            .addDnsServer(VPN_DNS_SERVER)
            .allowFamily(OsConstants.AF_INET)
            .allowFamily(OsConstants.AF_INET6)

        return try {
            vpnDescriptor = builder.establish()
            if (vpnDescriptor == null) {
                false
            } else {
                vpnInputStream = FileInputStream(vpnDescriptor!!.fileDescriptor)
                true
            }
        } catch (_: Exception) {
            closeVpnInterface()
            false
        }
    }

    private fun observePackets() {
        val stream = vpnInputStream ?: return
        val buffer = ByteArray(MAX_PACKET_BYTES)
        val packetProcessor = VpnModePacketProcessor(
            trafficStore = trafficStore,
            packetParser = packetParser,
        )

        while (!Thread.currentThread().isInterrupted) {
            val readBytes = try {
                stream.read(buffer)
            } catch (_: IOException) {
                break
            }

            if (readBytes <= 0) {
                continue
            }

            packetProcessor.processPacket(
                buffer = buffer,
                bytesRead = readBytes,
                forwardingPacketSink = packetForwarder?.let(::toForwardingPacketSink),
            )
        }
    }

    private fun closeVpnInterface() {
        trafficStore.setForwardingEnabled(enabled = false)
        vpnInputStream?.let { stream ->
            runCatching { stream.close() }
            vpnInputStream = null
        }
        trafficStore.clear()
        vpnDescriptor?.let { descriptor ->
            runCatching { descriptor.close() }
            vpnDescriptor = null
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("CyxWatch VPN visibility")
            .setContentText("Local network visibility is active.")
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "CyxWatch VPN Visibility",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun setVpnModeEnabledState(isEnabled: Boolean) {
        vpnModeSettingsRepository.setEnabled(
            isEnabled = isEnabled,
            changedAtEpochMs = System.currentTimeMillis(),
        )
    }

    private fun updateForwardingState() {
        val forwardingEnabledForSession = requestedForwardingEnabled() &&
            VpnModeCapabilities.FORWARDING_MODE_SUPPORTED
        trafficStore.setForwardingEnabled(enabled = forwardingEnabledForSession)
        packetForwarder = if (forwardingEnabledForSession) {
            NoopVpnModePacketForwarder()
        } else {
            null
        }
    }

    private fun toForwardingPacketSink(packetForwarder: VpnModePacketForwarder): (ByteArray, Int) -> Unit {
        return { packet, bytesRead ->
            kotlin.runCatching {
                packetForwarder.forwardPacket(packet, bytesRead)
            }
        }
    }

    private fun requestedForwardingEnabled(): Boolean {
        return vpnModeSettingsRepository.readState().isForwardingEnabled
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "cyxwatch_vpn_service"
        private const val NOTIFICATION_ID = 1001
        private const val WORKER_THREAD_NAME = "cyxwatch-vpn-service-worker"
        private const val MAX_PACKET_BYTES = 32 * 1024
        private const val VPN_TUN_IP = "10.255.255.1"
        private const val VPN_TUN_PREFIX = 32
        private const val VPN_DNS_SERVER = "1.1.1.1"
        private const val VPN_MTU = 1400
        private const val VPN_SESSION_NAME = "CyxWatch network visibility"
        fun isForwardingModeSupported(): Boolean = VpnModeCapabilities.FORWARDING_MODE_SUPPORTED

        fun start(context: Context) {
            val startIntent = Intent(context, CyxWatchVpnService::class.java)
            context.startForegroundService(startIntent)
        }

        fun stop(context: Context) {
            val stopIntent = Intent(context, CyxWatchVpnService::class.java)
            context.stopService(stopIntent)
        }
    }
}

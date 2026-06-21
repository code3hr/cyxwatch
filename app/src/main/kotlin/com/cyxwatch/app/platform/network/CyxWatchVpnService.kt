package com.cyxwatch.app.platform.network

import com.cyxwatch.app.BuildConfig
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.system.OsConstants
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.cyxwatch.app.runtime.RuntimeIntegrityGuard
import com.cyxwatch.app.data.settings.VpnModeSettingsRepository
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.IOException

class CyxWatchVpnService : VpnService() {
    companion object {
        private const val TAG = "CyxWatchVpnService"
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
            val integrityResult = RuntimeIntegrityGuard
                .create(context, isBuildDebuggable = BuildConfig.DEBUG)
                .canRunHighRiskAction("VPN service start")
            if (!integrityResult.isAllowed) {
                return
            }

            val startIntent = Intent(context, CyxWatchVpnService::class.java)
            context.startForegroundService(startIntent)
        }

        fun stop(context: Context) {
            val stopIntent = Intent(context, CyxWatchVpnService::class.java)
            context.stopService(stopIntent)
        }
    }

    private val trafficStore by lazy { VpnModeTrafficStore.shared }
    private val workerLifecycle = VpnServiceWorkerLifecycle()
    private var vpnDescriptor: ParcelFileDescriptor? = null
    private var vpnInputStream: FileInputStream? = null
    private val packetParser = VpnPacketParser
    private val runtimeIntegrityGuard by lazy { RuntimeIntegrityGuard.create(this, isBuildDebuggable = BuildConfig.DEBUG) }
    private val vpnModeSettingsRepository by lazy { VpnModeSettingsRepository(this) }
    @Volatile private var packetForwarder: VpnModePacketForwarder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val integrityResult = runtimeIntegrityGuard.canRunHighRiskAction("VPN service start")
        if (integrityResult.isBlocked) {
            if (!workerLifecycle.isActive()) {
                setVpnModeEnabledState(isEnabled = false)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            return START_NOT_STICKY
        }

        updateForwardingState()
        val workerThread = workerLifecycle.tryStart(::createWorkerThread)
        if (workerThread == null) {
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        setVpnModeEnabledState(isEnabled = true)
        workerThread.start()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        shutdownVpnService("onDestroy")
        super.onDestroy()
    }

    override fun onRevoke() {
        shutdownVpnService("onRevoke")
        super.onRevoke()
    }

    private fun openVpnInterface(): Boolean {
        if (vpnDescriptor != null && vpnInputStream != null) {
            return true
        }
        if (vpnDescriptor != null || vpnInputStream != null) {
            Log.w(TAG, "Detected inconsistent VPN interface state; resetting before re-open.")
            closeVpnInterface()
        }

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
        } catch (exception: SecurityException) {
            Log.w(TAG, "Security exception while creating VPN interface.", exception)
            closeVpnInterface()
            false
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to create VPN interface.", exception)
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
            } catch (exception: IOException) {
                if (!Thread.currentThread().isInterrupted) {
                    Log.w(TAG, "VPN stream read interrupted.", exception)
                }
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

        Log.d(TAG, "VPN packet loop stopped.")
    }

    private fun runVpnSession() {
        try {
            if (!openVpnInterface()) {
                Log.w(TAG, "Unable to open VPN interface; stopping service.")
                stopSelf()
                return
            }
            trafficStore.clear()
            observePackets()
        } catch (exception: Throwable) {
            Log.w(TAG, "Unexpected VPN worker error.", exception)
        } finally {
            closeVpnInterface()
            clearWorkerThreadForCurrent()
            setVpnModeEnabledState(isEnabled = false)
        }
    }

    private fun createWorkerThread(): Thread {
        return Thread(
            {
                runVpnSession()
                stopForeground(STOP_FOREGROUND_REMOVE)
            },
            WORKER_THREAD_NAME,
        )
    }

    private fun clearWorkerThreadForCurrent() {
        workerLifecycle.finishCurrentWorkerIfCurrent(Thread.currentThread())
    }

    private fun shutdownVpnService(reason: String) {
        Log.i(TAG, "Shutdown requested: $reason")
        stopForeground(STOP_FOREGROUND_REMOVE)
        val thread = workerLifecycle.stopActiveWorker()
        if (thread != null && thread !== Thread.currentThread()) {
            thread.interrupt()
            runCatching { thread.join(2_000L) }
        }
        closeVpnInterface()
        setVpnModeEnabledState(isEnabled = false)
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
}

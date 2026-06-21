package com.cyxwatch.app.platform.network

internal class VpnServiceWorkerLifecycle {
    private val lock = Any()
    private var workerThread: Thread? = null

    fun tryStart(createWorkerThread: () -> Thread): Thread? {
        return synchronized(lock) {
            if (workerThread?.isAlive == true) {
                null
            } else {
                val nextWorkerThread = createWorkerThread()
                workerThread = nextWorkerThread
                nextWorkerThread
            }
        }
    }

    fun finishCurrentWorkerIfCurrent(currentThread: Thread) {
        synchronized(lock) {
            if (workerThread === currentThread) {
                workerThread = null
            }
        }
    }

    fun stopActiveWorker(): Thread? {
        return synchronized(lock) {
            val activeWorkerThread = workerThread
            workerThread = null
            activeWorkerThread
        }
    }

    fun isActive(): Boolean = synchronized(lock) {
        workerThread?.isAlive == true
    }
}


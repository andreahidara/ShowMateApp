package com.andrea.showmateapp.util

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

object PerfTracer {

    private fun getFirebaseInstance(): FirebasePerformance? = runCatching {
        FirebasePerformance.getInstance()
    }.getOrNull()

    suspend fun <T> trace(name: String, block: suspend (Trace?) -> T): T {
        val t = getFirebaseInstance()?.newTrace(name)
        t?.start()
        return try {
            block(t)
        } finally {
            runCatching { t?.stop() }
        }
    }

    fun start(name: String): Trace? {
        val t = getFirebaseInstance()?.newTrace(name)
        t?.start()
        return t
    }

    fun Trace?.safePutMetric(name: String, value: Long) {
        this?.putMetric(name, value)
    }

    fun Trace?.safeStop() {
        runCatching { this?.stop() }
    }
}

package com.andrea.showmateapp.util

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

object PerfTracer {

    suspend fun <T> trace(name: String, block: suspend (Trace) -> T): T {
        val t = FirebasePerformance.getInstance().newTrace(name)
        t.start()
        return try {
            block(t)
        } finally {
            runCatching { t.stop() }
        }
    }

    fun start(name: String): Trace =
        FirebasePerformance.getInstance().newTrace(name).also { it.start() }
}

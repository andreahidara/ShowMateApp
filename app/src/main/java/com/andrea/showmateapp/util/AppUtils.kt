package com.andrea.showmateapp.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

object AppUtils {
    val Context.isDebug: Boolean
        get() = (0 != (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE))

    fun isInstalledFromPlayStore(context: Context): Boolean {
        return try {
            val packageName = context.packageName
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName == "com.android.vending"
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName) == "com.android.vending"
            }
        } catch (_: Exception) {
            false
        }
    }

    class CrashlyticsTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority < Log.WARN) return
            val crashlytics = FirebaseCrashlytics.getInstance()
            tag?.let { crashlytics.setCustomKey("timber_tag", it) }
            crashlytics.log(message)
            if (t != null && priority == Log.ERROR) crashlytics.recordException(t)
        }
    }
}

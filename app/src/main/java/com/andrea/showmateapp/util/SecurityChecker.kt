package com.andrea.showmateapp.util

import android.os.Build
import java.io.File

object SecurityChecker {

    fun isRooted(): Boolean = checkSuBinaries() || checkTestKeys() || checkDangerousPackages()

    fun isEmulator(): Boolean {
        val fp = Build.FINGERPRINT
        return fp.startsWith("generic") ||
            fp.startsWith("unknown") ||
            fp.contains("vbox") ||
            fp.contains("test-keys") ||
            Build.MODEL.contains("google_sdk", ignoreCase = true) ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for x86", ignoreCase = true) ||
            Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            Build.PRODUCT == "google_sdk" ||
            Build.HARDWARE == "goldfish" ||
            Build.HARDWARE == "ranchu"
    }

    fun isDeviceCompromised(): Boolean = isRooted() || isEmulator()

    private fun checkSuBinaries(): Boolean {
        val paths = listOf(
            "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su",
            "/data/local/su", "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkTestKeys(): Boolean = Build.TAGS?.contains("test-keys") == true

    private fun checkDangerousPackages(): Boolean {
        val knownRootApps = listOf(
            "/system/app/Superuser.apk",
            "/data/app/eu.chainfire.supersu",
            "/data/app/com.topjohnwu.magisk",
            "/data/adb/magisk"
        )
        return knownRootApps.any { File(it).exists() }
    }
}

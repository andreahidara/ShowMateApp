package com.andrea.showmateapp

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom test runner that replaces the Application class with HiltTestApplication,
 * enabling Hilt dependency injection in instrumented tests.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(classLoader: ClassLoader, className: String, context: Context): Application =
        super.newApplication(classLoader, HiltTestApplication::class.java.name, context)
}

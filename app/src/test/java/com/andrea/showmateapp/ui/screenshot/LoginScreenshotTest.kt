package com.andrea.showmateapp.ui.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.andrea.showmateapp.ui.screens.login.LoginScreenContent
import com.andrea.showmateapp.ui.screens.login.LoginUiState
import com.andrea.showmateapp.util.UiText
import org.junit.Rule
import org.junit.Test

/**
 * Screenshot tests for [LoginScreenContent] using Paparazzi.
 *
 * Run: ./gradlew recordPaparazziDebug   (record new golden images)
 *       ./gradlew verifyPaparazziDebug  (compare against golden images)
 */
class LoginScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoTitleBar"
    )

    private val noOp: (String) -> Unit = {}

    @Test
    fun login_initialState() {
        paparazzi.snapshot {
            LoginScreenContent(
                state = LoginUiState(),
                onEmailChanged = noOp,
                onPasswordChanged = noOp,
                onLoginClick = {},
                onTogglePasswordVisibility = {},
                onSendPasswordReset = noOp,
                onDismissResetDialog = {},
                onNavigateToSignUp = {}
            )
        }
    }

    @Test
    fun login_loadingState() {
        paparazzi.snapshot {
            LoginScreenContent(
                state = LoginUiState(
                    email = "user@example.com",
                    password = "password123",
                    isLoading = true
                ),
                onEmailChanged = noOp,
                onPasswordChanged = noOp,
                onLoginClick = {},
                onTogglePasswordVisibility = {},
                onSendPasswordReset = noOp,
                onDismissResetDialog = {},
                onNavigateToSignUp = {}
            )
        }
    }

    @Test
    fun login_errorState() {
        paparazzi.snapshot {
            LoginScreenContent(
                state = LoginUiState(
                    email = "user@example.com",
                    password = "wrong",
                    error = UiText.DynamicString("Credenciales incorrectas")
                ),
                onEmailChanged = noOp,
                onPasswordChanged = noOp,
                onLoginClick = {},
                onTogglePasswordVisibility = {},
                onSendPasswordReset = noOp,
                onDismissResetDialog = {},
                onNavigateToSignUp = {}
            )
        }
    }

    @Test
    fun login_filledState() {
        paparazzi.snapshot {
            LoginScreenContent(
                state = LoginUiState(
                    email = "andrea@showmate.com",
                    password = "mypassword"
                ),
                onEmailChanged = noOp,
                onPasswordChanged = noOp,
                onLoginClick = {},
                onTogglePasswordVisibility = {},
                onSendPasswordReset = noOp,
                onDismissResetDialog = {},
                onNavigateToSignUp = {}
            )
        }
    }

    @Test
    fun login_passwordVisible() {
        paparazzi.snapshot {
            LoginScreenContent(
                state = LoginUiState(
                    email = "andrea@showmate.com",
                    password = "mypassword",
                    isPasswordVisible = true
                ),
                onEmailChanged = noOp,
                onPasswordChanged = noOp,
                onLoginClick = {},
                onTogglePasswordVisibility = {},
                onSendPasswordReset = noOp,
                onDismissResetDialog = {},
                onNavigateToSignUp = {}
            )
        }
    }
}

package com.andrea.showmateapp.ui.screens.login

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.andrea.showmateapp.util.UiText
import org.junit.Rule
import org.junit.Test


class LoginScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        state: LoginUiState = LoginUiState(),
        onEmailChanged: (String) -> Unit = {},
        onPasswordChanged: (String) -> Unit = {},
        onLoginClick: () -> Unit = {},
        onTogglePasswordVisibility: () -> Unit = {},
        onSendPasswordReset: (String) -> Unit = {},
        onDismissResetDialog: () -> Unit = {},
        onNavigateToSignUp: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            LoginScreenContent(
                state = state,
                onEmailChanged = onEmailChanged,
                onPasswordChanged = onPasswordChanged,
                onLoginClick = onLoginClick,
                onTogglePasswordVisibility = onTogglePasswordVisibility,
                onSendPasswordReset = onSendPasswordReset,
                onDismissResetDialog = onDismissResetDialog,
                onNavigateToSignUp = onNavigateToSignUp
            )
        }
    }


    @Test
    fun givenInitialState_whenRendered_thenWelcomeTextIsDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Bienvenido de nuevo").assertIsDisplayed()
    }

    @Test
    fun givenInitialState_whenRendered_thenSubtitleIsDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Inicia sesión para continuar").assertIsDisplayed()
    }

    @Test
    fun givenInitialState_whenRendered_thenEmailFieldIsDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
    }

    @Test
    fun givenInitialState_whenRendered_thenPasswordFieldIsDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Contraseña").assertIsDisplayed()
    }

    @Test
    fun givenInitialState_whenRendered_thenLoginButtonIsDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Iniciar Sesión").assertIsDisplayed()
    }

    @Test
    fun givenInitialState_whenRendered_thenForgotPasswordLinkIsDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("¿Olvidaste tu contraseña?").assertIsDisplayed()
    }

    @Test
    fun givenInitialState_whenRendered_thenRegisterLinkIsDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Regístrate").assertIsDisplayed()
    }


    @Test
    fun givenLoginButton_whenClicked_thenOnLoginClickIsCalled() {
        var called = false
        setContent(onLoginClick = { called = true })

        composeTestRule.onNodeWithText("Iniciar Sesión").performClick()

        assert(called) { "onLoginClick should have been called" }
    }

    @Test
    fun givenRegisterLink_whenClicked_thenOnNavigateToSignUpIsCalled() {
        var called = false
        setContent(onNavigateToSignUp = { called = true })

        composeTestRule.onNodeWithText("Regístrate").performClick()

        assert(called) { "onNavigateToSignUp should have been called" }
    }


    @Test
    fun givenErrorState_whenRendered_thenErrorMessageIsDisplayed() {
        val errorText = "Credenciales incorrectas"
        setContent(state = LoginUiState(error = UiText.DynamicString(errorText)))

        composeTestRule.onNodeWithText(errorText).assertIsDisplayed()
    }

    @Test
    fun givenNoError_whenRendered_thenNoErrorBoxIsShown() {
        setContent(state = LoginUiState(error = null))

        // No error text should be present for any common error message
        composeTestRule.onNodeWithText("Credenciales incorrectas").assertDoesNotExist()
    }


    @Test
    fun givenIsLoadingTrue_whenRendered_thenProgressIndicatorIsVisible() {
        setContent(state = LoginUiState(isLoading = true))

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    @Test
    fun givenIsLoadingFalse_whenRendered_thenNoProgressIndicatorIsShown() {
        setContent(state = LoginUiState(isLoading = false))

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertDoesNotExist()
    }


    @Test
    fun givenLoginScreen_whenAccessibilityCheck_thenLogoHasContentDescription() {
        setContent()
        composeTestRule.onNodeWithContentDescription("ShowMate Logo").assertExists()
    }
}

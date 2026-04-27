package com.andrea.showmateapp.ui.screens.login

import com.andrea.showmateapp.data.repository.AuthRepository
import com.andrea.showmateapp.domain.repository.IUserRepository
import com.andrea.showmateapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val userRepository: IUserRepository = mockk(relaxed = true)
    private val interactionRepository: com.andrea.showmateapp.domain.repository.IInteractionRepository = mockk(relaxed = true)

    private fun viewModel() = LoginViewModel(authRepository, userRepository, interactionRepository)

    @Test
    fun `onEmailChanged updates email in state`() {
        val vm = viewModel()
        vm.onEmailChanged("test@example.com")
        assertEquals("test@example.com", vm.uiState.value.email)
    }

    @Test
    fun `onEmailChanged clears previous error`() {
        val vm = viewModel()
        // Trigger an error first
        vm.onLoginClick() // empty fields → error
        assertNotNull(vm.uiState.value.error)

        vm.onEmailChanged("new@email.com")
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `onPasswordChanged updates password in state`() {
        val vm = viewModel()
        vm.onPasswordChanged("secret123")
        assertEquals("secret123", vm.uiState.value.password)
    }

    @Test
    fun `onPasswordChanged clears previous error`() {
        val vm = viewModel()
        vm.onLoginClick()
        assertNotNull(vm.uiState.value.error)

        vm.onPasswordChanged("newpass")
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `togglePasswordVisibility toggles flag from false to true`() {
        val vm = viewModel()
        assertFalse(vm.uiState.value.isPasswordVisible)
        vm.togglePasswordVisibility()
        assertTrue(vm.uiState.value.isPasswordVisible)
    }

    @Test
    fun `togglePasswordVisibility toggles flag back to false`() {
        val vm = viewModel()
        vm.togglePasswordVisibility()
        vm.togglePasswordVisibility()
        assertFalse(vm.uiState.value.isPasswordVisible)
    }

    @Test
    fun `onLoginClick with empty email sets error state`() {
        val vm = viewModel()
        vm.onLoginClick()
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `onLoginClick with empty password sets error state`() {
        val vm = viewModel()
        vm.onEmailChanged("user@example.com")
        vm.onLoginClick()
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `onLoginClick with invalid email format sets error`() {
        val vm = viewModel()
        vm.onEmailChanged("not-an-email")
        vm.onPasswordChanged("password123")
        vm.onLoginClick()
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `onLoginClick with invalid email does not set isLoading`() {
        val vm = viewModel()
        vm.onEmailChanged("bad-email")
        vm.onPasswordChanged("pass")
        vm.onLoginClick()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onLoginClick with valid credentials sets isSuccess on success`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns Result.success(Unit)

        val vm = viewModel()
        vm.onEmailChanged("user@example.com")
        vm.onPasswordChanged("password123")
        vm.onLoginClick()

        assertTrue(vm.uiState.value.isSuccess)
    }

    @Test
    fun `onLoginClick with failed credentials sets error message`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns
            Result.failure(Exception("Contraseña incorrecta"))

        val vm = viewModel()
        vm.onEmailChanged("user@example.com")
        vm.onPasswordChanged("wrongpass")
        vm.onLoginClick()

        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isSuccess)
    }

    @Test
    fun `onLoginClick sets isLoading false after failure`() {
        coEvery { authRepository.login(any(), any()) } returns
            Result.failure(Exception("Error"))

        val vm = viewModel()
        vm.onEmailChanged("user@example.com")
        vm.onPasswordChanged("pass")
        vm.onLoginClick()

        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `sendPasswordReset with blank email sets resetError`() {
        val vm = viewModel()
        vm.sendPasswordReset("")
        assertNotNull(vm.uiState.value.resetError)
    }

    @Test
    fun `sendPasswordReset success sets resetEmailSent true`() = runTest {
        coEvery { authRepository.sendPasswordResetEmail(any()) } returns Result.success(Unit)

        val vm = viewModel()
        vm.sendPasswordReset("user@example.com")

        assertTrue(vm.uiState.value.resetEmailSent)
        assertNull(vm.uiState.value.resetError)
    }

    @Test
    fun `sendPasswordReset failure sets resetError`() = runTest {
        coEvery { authRepository.sendPasswordResetEmail(any()) } returns
            Result.failure(Exception("Email no encontrado"))

        val vm = viewModel()
        vm.sendPasswordReset("unknown@example.com")

        assertFalse(vm.uiState.value.resetEmailSent)
        assertNotNull(vm.uiState.value.resetError)
    }

    @Test
    fun `dismissResetDialog resets resetEmailSent and resetError`() = runTest {
        coEvery { authRepository.sendPasswordResetEmail(any()) } returns Result.success(Unit)

        val vm = viewModel()
        vm.sendPasswordReset("user@example.com")
        assertTrue(vm.uiState.value.resetEmailSent)

        vm.dismissResetDialog()
        assertFalse(vm.uiState.value.resetEmailSent)
        assertNull(vm.uiState.value.resetError)
    }
}

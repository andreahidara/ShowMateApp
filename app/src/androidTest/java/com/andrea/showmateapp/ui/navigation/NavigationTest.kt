package com.andrea.showmateapp.ui.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import com.andrea.showmateapp.ui.screens.login.LoginScreenContent
import com.andrea.showmateapp.ui.screens.login.LoginUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test


class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    private fun setUpNavGraph(startRoute: String = "login") {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController = navController, startDestination = startRoute) {
                composable("login") {
                    LoginScreenContent(
                        state = LoginUiState(),
                        onEmailChanged = {},
                        onPasswordChanged = {},
                        onLoginClick = {},
                        onTogglePasswordVisibility = {},
                        onSendPasswordReset = {},
                        onDismissResetDialog = {},
                        onNavigateToSignUp = { navController.navigate("signup") }
                    )
                }
                composable("signup") {
                    // Stub: just a node for destination verification
                }
                composable("onboarding") {
                    // Stub
                }
                composable("main") {
                    // Stub
                }
            }
        }
    }


    @Test
    fun givenLoginRoute_whenGraphStarts_thenCurrentDestinationIsLogin() {
        setUpNavGraph(startRoute = "login")

        composeTestRule.runOnIdle {
            assertEquals("login", navController.currentDestination?.route)
        }
    }


    @Test
    fun givenLoginDestination_whenRendered_thenLoginTitleIsDisplayed() {
        setUpNavGraph(startRoute = "login")

        composeTestRule.onNodeWithText("Bienvenido de nuevo").assertIsDisplayed()
    }

    @Test
    fun givenLoginDestination_whenRendered_thenLoginButtonIsDisplayed() {
        setUpNavGraph(startRoute = "login")

        composeTestRule.onNodeWithText("Iniciar Sesión").assertIsDisplayed()
    }


    @Test
    fun givenLoginScreen_whenRegisterClicked_thenNavigatesToSignup() {
        setUpNavGraph(startRoute = "login")

        composeTestRule.onNodeWithText("Regístrate").performClick()

        composeTestRule.runOnIdle {
            assertEquals("signup", navController.currentDestination?.route)
        }
    }


    @Test
    fun givenSignupDestination_whenBackPressed_thenReturnsToLogin() {
        setUpNavGraph(startRoute = "login")

        composeTestRule.onNodeWithText("Regístrate").performClick()
        composeTestRule.runOnIdle {
            assertEquals("signup", navController.currentDestination?.route)
        }

        composeTestRule.runOnIdle {
            navController.popBackStack()
        }

        composeTestRule.runOnIdle {
            assertEquals("login", navController.currentDestination?.route)
        }
    }


    @Test
    fun givenLoginScreen_whenNavigatingToMain_thenCurrentDestinationIsMain() {
        setUpNavGraph(startRoute = "login")

        composeTestRule.runOnIdle {
            navController.navigate("main")
        }

        composeTestRule.runOnIdle {
            assertEquals("main", navController.currentDestination?.route)
        }
    }

    @Test
    fun givenLoginScreen_whenNavigatingToOnboarding_thenCurrentDestinationIsOnboarding() {
        setUpNavGraph(startRoute = "login")

        composeTestRule.runOnIdle {
            navController.navigate("onboarding")
        }

        composeTestRule.runOnIdle {
            assertEquals("onboarding", navController.currentDestination?.route)
        }
    }
}

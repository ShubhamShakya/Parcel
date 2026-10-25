package com.parcel.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.parcel.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

class AuthFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun waitFor(tag: String, timeout: Long = 15_000) = compose.waitUntil(timeout) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun chooseUk() {
        compose.onNodeWithTag("auth_country").performClick()
        compose.onNodeWithTag("country_search").performTextInput("United Kingdom")
        compose.onNodeWithTag("country_GB").performClick()
    }
    @Test fun newNumberRegistersAndExistingNumberLogsInWithCountdownAndRestore() {
        val db = GlobalContext.get().get<ParcelDatabase>()
        val number = "9" + System.currentTimeMillis().toString().takeLast(9)
        runBlocking {
            db.shoppingDao().setPreference(PreferenceEntity("disclosure_v1", true))
            RoomAuthRepository(db).signOut()
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor("auth_phone")
            compose.onNodeWithTag("auth_phone").performTextInput("12345678")
            compose.onNodeWithTag("auth_request").assertIsNotEnabled()
            compose.onNodeWithTag("auth_phone").performTextReplacement("1".repeat(17))
            compose.onNodeWithTag("auth_request").assertIsNotEnabled()
            chooseUk()
            compose.onNodeWithTag("auth_phone").performTextReplacement(number)
            compose.onNodeWithTag("auth_request").performScrollTo().performClick()
            waitFor("auth_otp")
            compose.onNodeWithTag("auth_resend").assertDoesNotExist()
            val deadline = runBlocking { db.authDao().challenge()!!.resendAt }
            scenario.recreate(); waitFor("auth_otp")
            assertEquals(deadline, runBlocking { db.authDao().challenge()!!.resendAt })
            waitFor("auth_resend", 40_000)
            compose.onNodeWithTag("auth_resend").performScrollTo().performClick()
            waitFor("otp_countdown")
            compose.onNodeWithTag("auth_resend").assertDoesNotExist()
            compose.onNodeWithTag("auth_otp").performScrollTo().performTextInput("123")
            compose.onNodeWithTag("auth_verify").assertIsNotEnabled()
            compose.onNodeWithTag("auth_otp").performTextReplacement("aB9!")
            compose.onNodeWithTag("auth_verify").performScrollTo().performClick()
            waitFor("auth_first_name")
            compose.onNodeWithTag("auth_register").assertIsNotEnabled()
            compose.onNodeWithTag("auth_first_name").performTextInput("Mira")
            compose.onNodeWithTag("auth_last_name").performTextInput("Shah")
            compose.onNodeWithTag("auth_register").performScrollTo().performClick()
            waitFor("tab_about"); compose.onNodeWithTag("tab_about").performClick()
            compose.onNodeWithTag("profile_name").assertTextEquals("Mira Shah")
            compose.onNodeWithTag("profile_phone").assertTextEquals("+44$number")
            scenario.recreate(); waitFor("tab_about")
            compose.onNodeWithTag("tab_about").performClick()
            compose.onNodeWithTag("profile_name").assertTextEquals("Mira Shah")
            compose.onNodeWithTag("auth_sign_out").performScrollTo().performClick()
            waitFor("cancel_sign_out")
            compose.onNodeWithTag("cancel_sign_out").performClick()
            compose.onNodeWithTag("profile_name").assertTextEquals("Mira Shah")
            compose.onNodeWithTag("auth_sign_out").performClick()
            compose.onNodeWithTag("confirm_sign_out").performClick()
            waitFor("auth_phone"); chooseUk()
            compose.onNodeWithTag("auth_phone").performTextInput(number)
            compose.onNodeWithTag("auth_request").performScrollTo().performClick()
            waitFor("auth_otp")
            compose.onNodeWithTag("auth_otp").performTextInput("0000")
            compose.onNodeWithTag("auth_verify").performScrollTo().performClick()
            waitFor("tab_about")
            compose.onNodeWithTag("auth_first_name").assertDoesNotExist()
            compose.onNodeWithTag("tab_about").performClick()
            compose.onNodeWithTag("profile_name").assertTextEquals("Mira Shah")
        }
    }
}

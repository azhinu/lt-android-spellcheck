package azhinu.languagetool.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun primaryNavigationOpensEveryScreen() {
        composeRule.onNodeWithText("Server").assertIsDisplayed()

        composeRule.onNodeWithText("Check").performClick()
        composeRule.onNodeWithText("Text to check").assertIsDisplayed()

        composeRule.onNodeWithText("Dictionary").performClick()
        composeRule.onNodeWithText("Personal dictionary").assertIsDisplayed()

        composeRule.onNodeWithText("Logs").performClick()
        composeRule.onNodeWithText("Current run only").assertIsDisplayed()
    }

    @Test
    fun preferredLanguagesOpenDirectlyAndSearchByName() {
        composeRule.onNodeWithTag("preferred_languages").performScrollTo().performClick()
        composeRule.onNodeWithTag("preferred_language_search").performTextInput("russ")

        composeRule.onNodeWithText("Russian").assertIsDisplayed()
        composeRule.onNodeWithText("German").assertDoesNotExist()
    }

    @Test
    fun dictionaryActionsLiveOnlyInOverflowMenu() {
        composeRule.onNodeWithText("Dictionary").performClick()
        composeRule.onNodeWithText("Import").assertDoesNotExist()
        composeRule.onNodeWithText("Clear dictionary").assertDoesNotExist()

        composeRule.onNodeWithTag("dictionary_actions").performClick()

        composeRule.onNodeWithText("Import").assertIsDisplayed()
        composeRule.onNodeWithText("Export").assertIsDisplayed()
        composeRule.onNodeWithText("Clear dictionary").assertIsDisplayed()
    }

    @Test
    fun saveRejectsInvalidEndpointBeforeNetworkRequest() {
        val endpoint = composeRule.onNodeWithTag("endpoint")
        endpoint.performScrollTo()
        endpoint.performTextClearance()
        endpoint.performTextInput("ftp://server.example")
        composeRule.onNodeWithTag("save_endpoint").performClick()

        composeRule.onNodeWithText("Only HTTP and HTTPS are allowed").assertIsDisplayed()
    }
}

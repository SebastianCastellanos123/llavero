package io.github.sebastiancastellanos123.llavero.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityScreensTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lockScreenAsksImmediatelyAndAgainOnButton() {
        var requests = 0
        compose.setContent { LockScreen(onUnlockRequest = { requests++ }) }
        compose.waitForIdle()
        assertEquals(1, requests)

        compose.onNodeWithTag("unlock").performClick()
        assertEquals(2, requests)
    }

    @Test
    fun lockScreenShowsError() {
        compose.setContent { LockScreen(onUnlockRequest = {}, errorMessage = "Demasiados intentos") }
        compose.onNodeWithText("Demasiados intentos").assertIsDisplayed()
    }

    @Test
    fun settingsReportToggles() {
        var lock: Boolean? = null
        var screenshots: Boolean? = null
        compose.setContent {
            SettingsScreen(
                lockAvailable = true,
                lockEnabled = true,
                blockScreenshots = true,
                onLockChange = { lock = it },
                onBlockScreenshotsChange = { screenshots = it },
                onBack = {},
            )
        }
        compose.onNodeWithTag("lockSwitch").assertIsOn().performClick()
        compose.onNodeWithTag("screenshotSwitch").assertIsOn().performClick()
        assertEquals(false, lock)
        assertEquals(false, screenshots)
    }

    @Test
    fun lockSwitchDisabledWithoutScreenLock() {
        compose.setContent {
            SettingsScreen(
                lockAvailable = false,
                lockEnabled = true,
                blockScreenshots = true,
                onLockChange = {},
                onBlockScreenshotsChange = {},
                onBack = {},
            )
        }
        compose.onNodeWithTag("lockSwitch").assertIsOff().assertIsNotEnabled()
        compose.onNodeWithText("Configura un bloqueo de pantalla en el teléfono para usar esta opción.").assertIsDisplayed()
    }
}

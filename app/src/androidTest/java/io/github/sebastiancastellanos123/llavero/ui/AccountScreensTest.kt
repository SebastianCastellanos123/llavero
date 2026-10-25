package io.github.sebastiancastellanos123.llavero.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.sebastiancastellanos123.llavero.otp.OtpAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountScreensTest {
    @get:Rule
    val compose = createComposeRule()

    // Semilla del RFC 4226: en t=59 s el contador es 1 y el código es 287082.
    private val rfcAccount = OtpAccount("Banco", "ana@correo.com", "12345678901234567890".toByteArray())

    @Test
    fun listShowsFormattedCodeAndCopiesRawCode() {
        var copied: String? = null
        compose.setContent {
            AccountList(listOf(rfcAccount), nowSeconds = 59, onCopy = { copied = it }, onDelete = {}, contentPadding = PaddingValues())
        }
        compose.onNodeWithText("287 082").assertIsDisplayed()
        compose.onNodeWithText("Banco").assertIsDisplayed()
        compose.onNodeWithText("ana@correo.com").assertIsDisplayed()
        compose.onNodeWithTag("account").performClick()
        assertEquals("287082", copied)
    }

    @Test
    fun deleteAsksForConfirmation() {
        var deleted: OtpAccount? = null
        compose.setContent {
            AccountList(listOf(rfcAccount), nowSeconds = 59, onCopy = {}, onDelete = { deleted = it }, contentPadding = PaddingValues())
        }
        compose.onNodeWithContentDescription("Eliminar Banco").performClick()
        compose.onNodeWithText("Cancelar").performClick()
        assertNull(deleted)

        compose.onNodeWithContentDescription("Eliminar Banco").performClick()
        compose.onNodeWithText("Eliminar").performClick()
        assertEquals(rfcAccount, deleted)
    }

    @Test
    fun emptyListShowsHint() {
        compose.setContent {
            AccountList(emptyList(), nowSeconds = 0, onCopy = {}, onDelete = {}, contentPadding = PaddingValues())
        }
        compose.onNodeWithTag("empty").assertIsDisplayed()
    }

    @Test
    fun addFormShowsErrorsAndSavesValidAccount() {
        var saved: OtpAccount? = null
        compose.setContent { AddAccountScreen(onSave = { saved = it }, onBack = {}) }

        compose.onNodeWithTag("save").performClick()
        compose.onNodeWithText("Escribe el nombre de la cuenta").assertIsDisplayed()
        compose.onNodeWithText("Escribe la clave secreta").assertIsDisplayed()
        assertNull(saved)

        compose.onNodeWithTag("issuer").performTextInput("GitHub")
        compose.onNodeWithTag("accountName").performTextInput("sebastian")
        compose.onNodeWithTag("secret").performTextInput("JBSWY3DPEHPK3PXP")
        compose.onNodeWithTag("save").performClick()

        assertEquals("GitHub", saved?.issuer)
        assertEquals("sebastian", saved?.accountName)
    }
}

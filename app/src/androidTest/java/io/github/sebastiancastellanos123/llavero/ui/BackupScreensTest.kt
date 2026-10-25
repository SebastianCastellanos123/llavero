package io.github.sebastiancastellanos123.llavero.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private var confirmed: String? = null

    private fun exportDialog(busy: Boolean = false, error: String? = null) {
        compose.setContent {
            PasswordDialog(
                title = "Exportar respaldo",
                message = "Elige una contraseña",
                confirmLabel = "Cifrar y guardar",
                confirm = true,
                busy = busy,
                error = error,
                onConfirm = { confirmed = String(it) },
                onDismiss = {},
            )
        }
    }

    @Test
    fun rejectsShortPassword() {
        exportDialog()
        compose.onNodeWithTag("password").performTextInput("corta")
        compose.onNodeWithTag("repeat").performTextInput("corta")
        compose.onNodeWithTag("passwordConfirm").performClick()
        compose.onNodeWithText("Usa al menos 8 caracteres").assertIsDisplayed()
        assertNull(confirmed)
    }

    @Test
    fun rejectsMismatch() {
        exportDialog()
        compose.onNodeWithTag("password").performTextInput("caballo-bateria")
        compose.onNodeWithTag("repeat").performTextInput("caballo-bateri")
        compose.onNodeWithTag("passwordConfirm").performClick()
        compose.onNodeWithText("Las contraseñas no coinciden").assertIsDisplayed()
        assertNull(confirmed)
    }

    @Test
    fun confirmsMatchingPassword() {
        exportDialog()
        compose.onNodeWithTag("password").performTextInput("caballo-bateria")
        compose.onNodeWithTag("repeat").performTextInput("caballo-bateria")
        compose.onNodeWithTag("passwordConfirm").performClick()
        assertEquals("caballo-bateria", confirmed)
    }

    @Test
    fun showsProgressAndErrors() {
        exportDialog(busy = true, error = "Contraseña incorrecta o el archivo fue alterado")
        compose.onNodeWithText("Procesando…").assertIsDisplayed()
        compose.onNodeWithText("Contraseña incorrecta o el archivo fue alterado").assertIsDisplayed()
    }

    @Test
    fun settingsOffersExportAndImport() {
        var exported = false
        var imported = false
        compose.setContent {
            SettingsScreen(
                lockAvailable = true,
                lockEnabled = true,
                blockScreenshots = true,
                onLockChange = {},
                onBlockScreenshotsChange = {},
                onBack = {},
                onExport = { exported = true },
                onImport = { imported = true },
            )
        }
        compose.onNodeWithTag("export").performClick()
        compose.onNodeWithTag("import").performClick()
        assertEquals(true, exported)
        assertEquals(true, imported)
    }
}

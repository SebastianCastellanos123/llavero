package io.github.sebastiancastellanos123.llavero

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regresión: con androidx.fragment 1.2.5 (el que trae androidx.biometric),
 * FragmentActivity rechazaba los códigos de solicitud de ActivityResultRegistry
 * y la app se cerraba al abrir el selector de fotos, de archivos o el permiso de cámara.
 */
@RunWith(AndroidJUnit4::class)
class ActivityResultTest {
    @Test
    fun launchingForResultDoesNotCrash() {
        var error: Throwable? = null
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val launcher = activity.activityResultRegistry.register("prueba", ActivityResultContracts.StartActivityForResult()) {}
                error = runCatching { launcher.launch(Intent(activity, MainActivity::class.java)) }.exceptionOrNull()
                launcher.unregister()
            }
        }
        assertNull(error)
    }
}

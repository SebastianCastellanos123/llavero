package io.github.sebastiancastellanos123.llavero

import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.sebastiancastellanos123.llavero.security.SecurityViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureWindowTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun resetSettings() {
        SecurityViewModel.prefs(context).edit().clear().commit()
    }

    private fun windowIsSecure(): Boolean {
        var secure = false
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                secure = it.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
            }
        }
        return secure
    }

    @Test
    fun screenshotsBlockedByDefault() {
        assertTrue(windowIsSecure())
    }

    @Test
    fun screenshotsAllowedWhenUserDisablesProtection() {
        SecurityViewModel.prefs(context).edit().putBoolean("block_screenshots", false).commit()
        assertFalse(windowIsSecure())
    }
}

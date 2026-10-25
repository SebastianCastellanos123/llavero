package io.github.sebastiancastellanos123.llavero

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import io.github.sebastiancastellanos123.llavero.security.DeviceAuth
import io.github.sebastiancastellanos123.llavero.security.SecurityViewModel
import io.github.sebastiancastellanos123.llavero.ui.LlaveroRoot
import io.github.sebastiancastellanos123.llavero.ui.theme.LlaveroTheme

// FragmentActivity porque BiometricPrompt la necesita para mostrar su diálogo.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Antes de dibujar nada: así ni el primer cuadro queda expuesto en capturas o en recientes.
        setScreenSecure(SecurityViewModel.blockScreenshots(SecurityViewModel.prefs(this)))
        enableEdgeToEdge()
        setContent {
            LlaveroTheme {
                LlaveroRoot(
                    authenticator = { title, subtitle, onSuccess, onError ->
                        DeviceAuth.authenticate(this, title, subtitle, onSuccess, onError)
                    },
                    setScreenSecure = ::setScreenSecure,
                )
            }
        }
    }

    /** FLAG_SECURE: bloquea capturas, grabación de pantalla y la miniatura de apps recientes. */
    private fun setScreenSecure(secure: Boolean) {
        if (secure) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

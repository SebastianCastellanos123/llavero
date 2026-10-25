package io.github.sebastiancastellanos123.llavero.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Huella, rostro o el PIN/patrón/contraseña del teléfono, con el diálogo del sistema. */
object DeviceAuth {
    /**
     * Biometría fuerte o el bloqueo del teléfono. Android 9 y 10 no admiten esa
     * combinación con biometría "fuerte", así que ahí se acepta también la débil.
     */
    private val authenticators: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        }

    /** false si el teléfono no tiene bloqueo de pantalla: no hay con qué verificar al usuario. */
    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {},
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Cancelar no es un error que valga la pena mostrar.
                val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                if (!cancelled) onError(errString.toString())
            }
            // onAuthenticationFailed (huella no reconocida): el diálogo sigue abierto y deja reintentar.
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(info)
    }
}

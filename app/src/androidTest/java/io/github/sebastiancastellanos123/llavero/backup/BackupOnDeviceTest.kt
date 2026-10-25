package io.github.sebastiancastellanos123.llavero.backup

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.sebastiancastellanos123.llavero.otp.Base32
import io.github.sebastiancastellanos123.llavero.otp.OtpAccount
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** En el teléfono, con las iteraciones reales: confirma que Android trae PBKDF2-HMAC-SHA256 y cuánto tarda. */
@RunWith(AndroidJUnit4::class)
class BackupOnDeviceTest {
    @Test
    fun roundTripWithDefaultIterations() {
        val account = OtpAccount("GitHub", "sebastian", Base32.decode("JBSWY3DPEHPK3PXP"))
        val password = "caballo-bateria-grapa"

        val start = System.nanoTime()
        val data = BackupCodec.encrypt(listOf(account), password.toCharArray())
        val encryptMs = (System.nanoTime() - start) / 1_000_000
        val restored = BackupCodec.decrypt(data, password.toCharArray())

        Log.i("BackupOnDeviceTest", "Cifrar con ${BackupCodec.DEFAULT_ITERATIONS} iteraciones tardó $encryptMs ms")
        assertTrue(restored.single().isSameAs(account))
    }
}

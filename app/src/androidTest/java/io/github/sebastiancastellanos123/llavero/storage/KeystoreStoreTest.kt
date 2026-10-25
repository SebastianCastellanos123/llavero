package io.github.sebastiancastellanos123.llavero.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.sebastiancastellanos123.llavero.otp.Base32
import io.github.sebastiancastellanos123.llavero.otp.OtpAccount
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Lo mismo que EncryptedAccountStoreTest, pero con la clave real de Android Keystore. */
@RunWith(AndroidJUnit4::class)
class KeystoreStoreTest {
    private val alias = "llavero.test"
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val file = File(context.cacheDir, "keystore-test.bin")
    private fun store() = EncryptedAccountStore(file, AesGcmAead { KeystoreKey.getOrCreate(alias) })

    @After
    fun cleanUp() {
        file.delete()
        KeystoreKey.delete(alias)
    }

    @Test
    fun roundTripWithKeystoreKey() {
        val secret = Base32.decode("JBSWY3DPEHPK3PXP")
        store().save(listOf(OtpAccount("Banco", "ana", secret)))
        assertArrayEquals(secret, store().load().single().secret())
    }

    @Test
    fun deletingTheKeyMakesFileUnreadable() {
        store().save(listOf(OtpAccount("Banco", "ana", byteArrayOf(1, 2, 3))))
        KeystoreKey.delete(alias)
        assertThrows(CorruptStoreException::class.java) { store().load() }
    }
}

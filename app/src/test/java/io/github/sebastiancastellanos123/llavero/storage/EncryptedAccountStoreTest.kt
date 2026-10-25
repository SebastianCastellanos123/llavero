package io.github.sebastiancastellanos123.llavero.storage

import io.github.sebastiancastellanos123.llavero.otp.Base32
import io.github.sebastiancastellanos123.llavero.otp.OtpAccount
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class EncryptedAccountStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val secret = Base32.decode("JBSWY3DPEHPK3PXP")
    private val accounts = listOf(OtpAccount("Banco", "ana@correo.com", secret))

    private fun newKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val key = newKey()
    private val file: File by lazy { File(folder.root, "accounts.bin") }
    private fun store(withKey: SecretKey = key) = EncryptedAccountStore(file, AesGcmAead { withKey })

    @Test
    fun missingFileMeansNoAccounts() {
        assertTrue(store().load().isEmpty())
    }

    @Test
    fun saveThenLoad() {
        store().save(accounts)
        val loaded = store().load()
        assertEquals(1, loaded.size)
        assertEquals("ana@correo.com", loaded[0].accountName)
        assertArrayEquals(secret, loaded[0].secret())
    }

    @Test
    fun saveReplacesPreviousContent() {
        store().save(accounts)
        store().save(emptyList())
        assertTrue(store().load().isEmpty())
    }

    @Test
    fun fileContainsNoPlaintext() {
        store().save(accounts)
        val raw = file.readBytes()
        assertFalse(raw.containsSequence("ana@correo.com".toByteArray()))
        assertFalse(raw.containsSequence("Banco".toByteArray()))
        assertFalse(raw.containsSequence(secret))
    }

    @Test
    fun everySaveUsesFreshIv() {
        store().save(accounts)
        val first = file.readBytes()
        store().save(accounts)
        assertFalse(first.contentEquals(file.readBytes()))
    }

    @Test
    fun detectsTamperingInEveryByte() {
        store().save(accounts)
        val original = file.readBytes()
        for (i in original.indices) {
            val tampered = original.copyOf()
            tampered[i] = (tampered[i].toInt() xor 0x01).toByte()
            file.writeBytes(tampered)
            assertThrows("Byte $i alterado sin detectar", CorruptStoreException::class.java) { store().load() }
        }
    }

    @Test
    fun wrongKeyCannotRead() {
        store().save(accounts)
        assertThrows(CorruptStoreException::class.java) { store(withKey = newKey()).load() }
    }

    @Test
    fun rejectsTruncatedFile() {
        store().save(accounts)
        file.writeBytes(file.readBytes().copyOf(10))
        assertThrows(CorruptStoreException::class.java) { store().load() }
    }

    @Test
    fun leavesNoTemporaryFile() {
        store().save(accounts)
        assertEquals(listOf("accounts.bin"), folder.root.list()!!.toList())
    }

    private fun ByteArray.containsSequence(needle: ByteArray): Boolean =
        (0..size - needle.size).any { start -> needle.indices.all { this[start + it] == needle[it] } }
}

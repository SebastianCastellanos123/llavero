package io.github.sebastiancastellanos123.llavero.backup

import io.github.sebastiancastellanos123.llavero.otp.Base32
import io.github.sebastiancastellanos123.llavero.otp.HashAlgorithm
import io.github.sebastiancastellanos123.llavero.otp.OtpAccount
import io.github.sebastiancastellanos123.llavero.storage.AccountCodec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class BackupCodecTest {
    // El mínimo aceptado, para que las pruebas no tarden; la app usa 600 000.
    private val fast = 100_000
    private val password = "caballo-bateria-grapa".toCharArray()
    private val accounts = listOf(
        OtpAccount("GitHub", "sebastian", Base32.decode("JBSWY3DPEHPK3PXP")),
        OtpAccount(null, "sin emisor", byteArrayOf(9, 8, 7), HashAlgorithm.SHA512, 8, 60),
    )

    private fun backup() = BackupCodec.encrypt(accounts, password.copyOf(), fast)

    @Test
    fun pbkdf2MatchesRfc7914Vector() {
        // RFC 7914, sección 11: PBKDF2-HMAC-SHA256("passwd", "salt", 1, 64)
        val expected = (
            "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc" +
                "49ca9cccf179b645991664b39d77ef317c71b845b1e30bd509112041d3a19783"
            ).chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        assertArrayEquals(expected, BackupCodec.deriveKey("passwd".toCharArray(), "salt".toByteArray(), 1, 64))
    }

    @Test
    fun roundTrip() {
        val restored = BackupCodec.decrypt(backup(), password.copyOf())
        assertEquals(accounts.size, restored.size)
        accounts.zip(restored).forEach { (a, b) -> assertTrue(a.isSameAs(b)) }
        assertEquals(8, restored[1].digits)
    }

    @Test
    fun defaultUsesOwaspIterations() {
        val data = BackupCodec.encrypt(accounts, password.copyOf())
        assertEquals(600_000, ByteBuffer.wrap(data, 6, 4).int)
    }

    @Test
    fun wrongPasswordFails() {
        assertThrows(WrongPasswordException::class.java) {
            BackupCodec.decrypt(backup(), "caballo-bateria-grapA".toCharArray())
        }
    }

    @Test
    fun sameInputGivesDifferentFiles() {
        assertFalse(backup().contentEquals(backup()))
    }

    @Test
    fun fileContainsNoPlaintext() {
        val data = String(backup(), Charsets.ISO_8859_1)
        assertFalse(data.contains("GitHub"))
        assertFalse(data.contains("sebastian"))
    }

    @Test
    fun detectsTamperingInHeaderAndCiphertext() {
        val original = backup()
        // Cada byte del encabezado (salvo las iteraciones, que tienen su propia prueba) y una muestra del resto.
        val positions = (0 until 26).filterNot { it in 6..9 } + listOf(26, 30, 40, original.size / 2, original.size - 1)
        for (i in positions) {
            val tampered = original.copyOf()
            tampered[i] = (tampered[i].toInt() xor 0x01).toByte()
            val error = runCatching { BackupCodec.decrypt(tampered, password.copyOf()) }.exceptionOrNull()
            assertTrue("Byte $i alterado sin detectar", error is WrongPasswordException || error is InvalidBackupException)
        }
    }

    @Test
    fun rejectsAbsurdIterationsBeforeDerivingKey() {
        val data = backup()
        ByteBuffer.wrap(data).putInt(6, Int.MAX_VALUE)
        val start = System.nanoTime()
        assertThrows(InvalidBackupException::class.java) { BackupCodec.decrypt(data, password.copyOf()) }
        assertTrue("Debió rechazarse sin calcular", System.nanoTime() - start < 1_000_000_000)
    }

    @Test
    fun rejectsFilesThatAreNotBackups() {
        val notBackups = listOf(
            ByteArray(0),
            "hola".toByteArray(),
            ByteArray(200) { it.toByte() },
            AccountCodec.encode(accounts),
            ByteArray(BackupCodec.MAX_FILE_BYTES + 1),
        )
        for (data in notBackups) {
            assertThrows(InvalidBackupException::class.java) { BackupCodec.decrypt(data, password.copyOf()) }
        }
    }

    @Test
    fun rejectsNewerVersion() {
        val data = backup()
        data[4] = 2
        val error = assertThrows(InvalidBackupException::class.java) { BackupCodec.decrypt(data, password.copyOf()) }
        assertTrue(error.message!!.contains("más nueva"))
    }

    @Test
    fun rejectsShortPassword() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.encrypt(accounts, "corta".toCharArray(), fast)
        }
    }

    @Test
    fun emptyAccountListRoundTrips() {
        val data = BackupCodec.encrypt(emptyList(), password.copyOf(), fast)
        assertTrue(BackupCodec.decrypt(data, password.copyOf()).isEmpty())
    }
}

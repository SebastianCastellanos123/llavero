package io.github.sebastiancastellanos123.llavero.storage

import io.github.sebastiancastellanos123.llavero.otp.Base32
import io.github.sebastiancastellanos123.llavero.otp.HashAlgorithm
import io.github.sebastiancastellanos123.llavero.otp.OtpAccount
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class AccountCodecTest {
    private val accounts = listOf(
        OtpAccount("Banco", "ana@correo.com", Base32.decode("JBSWY3DPEHPK3PXP")),
        OtpAccount(null, "sin emisor", byteArrayOf(1, 2, 3)),
        OtpAccount("Bogotá ✓", "josé", ByteArray(64) { it.toByte() }, HashAlgorithm.SHA512, 8, 60),
    )

    @Test
    fun roundTripKeepsEveryField() {
        val decoded = AccountCodec.decode(AccountCodec.encode(accounts))
        assertEquals(accounts.size, decoded.size)
        for ((original, copy) in accounts.zip(decoded)) {
            assertEquals(original.issuer, copy.issuer)
            assertEquals(original.accountName, copy.accountName)
            assertEquals(original.algorithm, copy.algorithm)
            assertEquals(original.digits, copy.digits)
            assertEquals(original.period, copy.period)
            assertArrayEquals(original.secret(), copy.secret())
        }
    }

    @Test
    fun roundTripEmptyList() {
        assertTrue(AccountCodec.decode(AccountCodec.encode(emptyList())).isEmpty())
    }

    @Test
    fun rejectsTruncatedData() {
        val data = AccountCodec.encode(accounts)
        for (length in listOf(0, 3, 8, data.size / 2, data.size - 1)) {
            assertThrows(CorruptStoreException::class.java) { AccountCodec.decode(data.copyOf(length)) }
        }
    }

    @Test
    fun rejectsTrailingData() {
        val data = AccountCodec.encode(accounts) + byteArrayOf(0)
        assertThrows(CorruptStoreException::class.java) { AccountCodec.decode(data) }
    }

    @Test
    fun rejectsUnknownVersion() {
        val data = AccountCodec.encode(accounts)
        data[3] = 99
        assertThrows(CorruptStoreException::class.java) { AccountCodec.decode(data) }
    }

    @Test
    fun rejectsAbsurdCounts() {
        assertThrows(CorruptStoreException::class.java) { AccountCodec.decode(header(count = -1)) }
        assertThrows(CorruptStoreException::class.java) { AccountCodec.decode(header(count = Int.MAX_VALUE)) }
    }

    @Test
    fun rejectsInvalidAccountInsideFile() {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(1)
            out.writeInt(1)
            out.writeBoolean(false)
            out.writeUTF("ana")
            out.writeUTF("SHA1")
            out.writeInt(9) // dígitos no soportados
            out.writeInt(30)
            out.writeInt(3)
            out.write(byteArrayOf(1, 2, 3))
        }
        assertThrows(CorruptStoreException::class.java) { AccountCodec.decode(bytes.toByteArray()) }
    }

    private fun header(count: Int): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(1)
            out.writeInt(count)
        }
        return bytes.toByteArray()
    }
}

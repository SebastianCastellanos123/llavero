package io.github.sebastiancastellanos123.llavero.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OtpTest {
    private val seed20 = "12345678901234567890".toByteArray()
    private val seed32 = "12345678901234567890123456789012".toByteArray()
    private val seed64 = "1234567890123456789012345678901234567890123456789012345678901234".toByteArray()

    @Test
    fun hotp_matchesRfc4226AppendixD() {
        val expected = listOf(
            "755224", "287082", "359152", "969429", "338314",
            "254676", "287922", "162583", "399871", "520489",
        )
        expected.forEachIndexed { counter, code ->
            assertEquals(code, Otp.hotp(seed20, counter.toLong()))
        }
    }

    @Test
    fun totp_matchesRfc6238AppendixB() {
        // 8 dígitos, periodo 30 s
        data class Vector(val time: Long, val sha1: String, val sha256: String, val sha512: String)
        val vectors = listOf(
            Vector(59L, "94287082", "46119246", "90693936"),
            Vector(1111111109L, "07081804", "68084774", "25091201"),
            Vector(1111111111L, "14050471", "67062674", "99943326"),
            Vector(1234567890L, "89005924", "91819424", "93441116"),
            Vector(2000000000L, "69279037", "90698825", "38618901"),
            Vector(20000000000L, "65353130", "77737706", "47863826"),
        )
        for (v in vectors) {
            assertEquals(v.sha1, Otp.totp(seed20, v.time, digits = 8, algorithm = HashAlgorithm.SHA1))
            assertEquals(v.sha256, Otp.totp(seed32, v.time, digits = 8, algorithm = HashAlgorithm.SHA256))
            assertEquals(v.sha512, Otp.totp(seed64, v.time, digits = 8, algorithm = HashAlgorithm.SHA512))
        }
    }

    @Test
    fun totp_keepsLeadingZeros() {
        // En el RFC, t=1111111109 con SHA1 da "07081804"; con 6 dígitos queda "081804".
        assertEquals("081804", Otp.totp(seed20, 1111111109L))
    }

    @Test
    fun counterAt_changesExactlyAtPeriodBoundary() {
        assertEquals(0L, Otp.counterAt(29))
        assertEquals(1L, Otp.counterAt(30))
        assertEquals(2L, Otp.counterAt(60, period = 30))
        assertEquals(1L, Otp.counterAt(60, period = 60))
    }

    @Test
    fun secondsRemaining_countsDownToOne() {
        assertEquals(30, Otp.secondsRemaining(0))
        assertEquals(1, Otp.secondsRemaining(29))
        assertEquals(30, Otp.secondsRemaining(30))
        assertEquals(45, Otp.secondsRemaining(15, period = 60))
    }

    @Test
    fun rejectsInvalidParameters() {
        assertThrows(IllegalArgumentException::class.java) { Otp.hotp(ByteArray(0), 0) }
        assertThrows(IllegalArgumentException::class.java) { Otp.hotp(seed20, 0, digits = 5) }
        assertThrows(IllegalArgumentException::class.java) { Otp.hotp(seed20, 0, digits = 9) }
        assertThrows(IllegalArgumentException::class.java) { Otp.counterAt(0, period = 0) }
    }
}

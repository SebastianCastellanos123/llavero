package io.github.sebastiancastellanos123.llavero.otp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class Base32Test {
    // Vectores de prueba del RFC 4648, sección 10.
    private val rfcVectors = listOf(
        "" to "",
        "f" to "MY======",
        "fo" to "MZXQ====",
        "foo" to "MZXW6===",
        "foob" to "MZXW6YQ=",
        "fooba" to "MZXW6YTB",
        "foobar" to "MZXW6YTBOI======",
    )

    @Test
    fun encode_matchesRfc4648Vectors() {
        for ((plain, encoded) in rfcVectors) {
            assertEquals(encoded, Base32.encode(plain.toByteArray()))
        }
    }

    @Test
    fun decode_matchesRfc4648Vectors() {
        for ((plain, encoded) in rfcVectors) {
            assertArrayEquals(plain.toByteArray(), Base32.decode(encoded))
        }
    }

    @Test
    fun decode_acceptsHowServicesShowSecrets() {
        val expected = Base32.decode("JBSWY3DPEHPK3PXP")
        assertArrayEquals(expected, Base32.decode("jbsw y3dp ehpk 3pxp"))
        assertArrayEquals(expected, Base32.decode("JBSW-Y3DP-EHPK-3PXP"))
        assertArrayEquals("foobar".toByteArray(), Base32.decode("MZXW6YTBOI"))
    }

    @Test
    fun decode_rejectsCharactersOutsideAlphabet() {
        for (bad in listOf("MZXW6YT1", "MZXW6YT0", "MZXW6YT8", "MZ=W6YTB")) {
            assertThrows(IllegalArgumentException::class.java) { Base32.decode(bad) }
        }
    }

    @Test
    fun decode_errorMessageDoesNotLeakSecret() {
        val error = assertThrows(IllegalArgumentException::class.java) { Base32.decode("ABCDEFG!") }
        assertFalse(error.message!!.contains("!"))
    }

    @Test
    fun decode_rejectsImpossibleLengths() {
        for (bad in listOf("A", "ABC", "ABCDEF")) {
            assertThrows(IllegalArgumentException::class.java) { Base32.decode(bad) }
        }
    }

    @Test
    fun roundTrip_allByteValues() {
        val data = ByteArray(256) { it.toByte() }
        assertArrayEquals(data, Base32.decode(Base32.encode(data)))
    }
}

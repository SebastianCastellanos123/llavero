package io.github.sebastiancastellanos123.llavero.otp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpAuthUriTest {
    @Test
    fun parsesGoogleExample() {
        val account = OtpAuthUri.parse(
            "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example"
        )
        assertEquals("Example", account.issuer)
        assertEquals("alice@google.com", account.accountName)
        assertArrayEquals(Base32.decode("JBSWY3DPEHPK3PXP"), account.secret())
        assertEquals(HashAlgorithm.SHA1, account.algorithm)
        assertEquals(6, account.digits)
        assertEquals(30, account.period)
    }

    @Test
    fun readsOptionalParameters() {
        val account = OtpAuthUri.parse(
            "otpauth://totp/ACME%20Co:john.doe@email.com?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ" +
                "&issuer=ACME%20Co&algorithm=SHA256&digits=8&period=60"
        )
        assertEquals("ACME Co", account.issuer)
        assertEquals("john.doe@email.com", account.accountName)
        assertEquals(HashAlgorithm.SHA256, account.algorithm)
        assertEquals(8, account.digits)
        assertEquals(60, account.period)
    }

    @Test
    fun issuerParameterWinsOverLabelPrefix() {
        val account = OtpAuthUri.parse("otpauth://totp/Viejo:ana?secret=JBSWY3DPEHPK3PXP&issuer=Nuevo")
        assertEquals("Nuevo", account.issuer)
    }

    @Test
    fun issuerFromLabelWhenNoParameter() {
        val account = OtpAuthUri.parse("otpauth://totp/Banco:ana?secret=JBSWY3DPEHPK3PXP")
        assertEquals("Banco", account.issuer)
    }

    @Test
    fun noIssuerAtAll() {
        val account = OtpAuthUri.parse("otpauth://totp/ana?secret=JBSWY3DPEHPK3PXP")
        assertNull(account.issuer)
        assertEquals("ana", account.accountName)
    }

    @Test
    fun plusSignInLabelIsLiteral() {
        val account = OtpAuthUri.parse("otpauth://totp/Banco:ana+banco@correo.com?secret=JBSWY3DPEHPK3PXP")
        assertEquals("ana+banco@correo.com", account.accountName)
    }

    @Test
    fun decodesUtf8Labels() {
        val account = OtpAuthUri.parse("otpauth://totp/Bogot%C3%A1:jos%C3%A9?secret=JBSWY3DPEHPK3PXP")
        assertEquals("Bogotá", account.issuer)
        assertEquals("josé", account.accountName)
    }

    @Test
    fun schemeAndTypeAreCaseInsensitive() {
        val account = OtpAuthUri.parse("OTPAUTH://TOTP/ana?secret=JBSWY3DPEHPK3PXP")
        assertEquals("ana", account.accountName)
    }

    @Test
    fun rejectsInvalidUris() {
        val bad = listOf(
            "https://totp/ana?secret=JBSWY3DPEHPK3PXP",
            "otpauth://hotp/ana?secret=JBSWY3DPEHPK3PXP&counter=0",
            "otpauth://otro/ana?secret=JBSWY3DPEHPK3PXP",
            "otpauth://totp/ana",
            "otpauth://totp/?secret=JBSWY3DPEHPK3PXP",
            "otpauth://totp/ana?secret=",
            "otpauth://totp/ana?secret=NO-VALIDO-1",
            "otpauth://totp/ana?secret=JBSWY3DPEHPK3PXP&algorithm=MD5",
            "otpauth://totp/ana?secret=JBSWY3DPEHPK3PXP&digits=9",
            "otpauth://totp/ana?secret=JBSWY3DPEHPK3PXP&digits=seis",
            "otpauth://totp/ana?secret=JBSWY3DPEHPK3PXP&period=0",
            "otpauth://totp/ana%2?secret=JBSWY3DPEHPK3PXP",
            "otpauth://totp/ana%-1?secret=JBSWY3DPEHPK3PXP",
        )
        for (uri in bad) {
            assertThrows("Debería rechazar: $uri", IllegalArgumentException::class.java) {
                OtpAuthUri.parse(uri)
            }
        }
    }

    @Test
    fun rejectsRepeatedParameters() {
        // Dos "secret" son ambiguos: no se adivina cuál usar.
        assertThrows(IllegalArgumentException::class.java) {
            OtpAuthUri.parse("otpauth://totp/ana?secret=JBSWY3DPEHPK3PXP&secret=MZXW6YTBOI")
        }
    }

    @Test
    fun toStringNeverShowsSecret() {
        val account = OtpAuthUri.parse("otpauth://totp/Banco:ana?secret=JBSWY3DPEHPK3PXP")
        val text = account.toString()
        assertFalse(text.contains("JBSWY3DPEHPK3PXP"))
        assertTrue(text.contains("<oculto>"))
    }

    @Test
    fun secretCannotBeModifiedFromOutside() {
        val account = OtpAuthUri.parse("otpauth://totp/Banco:ana?secret=JBSWY3DPEHPK3PXP")
        val before = account.codeAt(1_700_000_000)
        account.secret().fill(0)
        assertEquals(before, account.codeAt(1_700_000_000))
    }

    @Test
    fun isSameAsComparesIssuerNameAndSecret() {
        val a = OtpAuthUri.parse("otpauth://totp/Banco:ana?secret=JBSWY3DPEHPK3PXP")
        assertTrue(a.isSameAs(OtpAuthUri.parse("otpauth://totp/Banco:ana?secret=jbsw y3dp ehpk 3pxp")))
        assertFalse(a.isSameAs(OtpAuthUri.parse("otpauth://totp/Banco:ana?secret=MZXW6YTBOI")))
        assertFalse(a.isSameAs(OtpAuthUri.parse("otpauth://totp/Otro:ana?secret=JBSWY3DPEHPK3PXP")))
        assertFalse(a.isSameAs(OtpAuthUri.parse("otpauth://totp/Banco:luis?secret=JBSWY3DPEHPK3PXP")))
    }
}

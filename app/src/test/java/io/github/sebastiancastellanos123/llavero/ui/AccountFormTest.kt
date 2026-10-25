package io.github.sebastiancastellanos123.llavero.ui

import io.github.sebastiancastellanos123.llavero.otp.Base32
import io.github.sebastiancastellanos123.llavero.otp.HashAlgorithm
import io.github.sebastiancastellanos123.llavero.otp.OtpAuthUri
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountFormTest {
    private val valid = AccountForm(issuer = " Banco ", accountName = " ana@correo.com ", secret = "jbsw y3dp ehpk 3pxp")

    @Test
    fun validFormBuildsTrimmedAccount() {
        val result = valid.validate()
        assertTrue(result is FormResult.Valid)
        val account = (result as FormResult.Valid).account
        assertEquals("Banco", account.issuer)
        assertEquals("ana@correo.com", account.accountName)
        assertArrayEquals(Base32.decode("JBSWY3DPEHPK3PXP"), account.secret())
        assertEquals(HashAlgorithm.SHA1, account.algorithm)
        assertEquals(6, account.digits)
        assertEquals(30, account.period)
    }

    @Test
    fun blankIssuerBecomesNull() {
        val account = (valid.copy(issuer = "   ").validate() as FormResult.Valid).account
        assertNull(account.issuer)
    }

    @Test
    fun keepsAdvancedOptions() {
        val account = (valid.copy(algorithm = HashAlgorithm.SHA256, digits = 8, period = "60").validate()
            as FormResult.Valid).account
        assertEquals(HashAlgorithm.SHA256, account.algorithm)
        assertEquals(8, account.digits)
        assertEquals(60, account.period)
    }

    @Test
    fun reportsEveryErrorAtOnce() {
        val errors = (AccountForm(accountName = " ", secret = "", period = "0").validate()
            as FormResult.Invalid).errors
        assertNotNull(errors.accountName)
        assertNotNull(errors.secret)
        assertNotNull(errors.period)
    }

    @Test
    fun rejectsInvalidSecret() {
        for (bad in listOf("CLAVE-CON-1", "ABC", "!!!!")) {
            val errors = (valid.copy(secret = bad).validate() as FormResult.Invalid).errors
            assertNotNull("Debería rechazar $bad", errors.secret)
        }
    }

    @Test
    fun rejectsOutOfRangePeriod() {
        for (bad in listOf("", "abc", "0", "-30", "301")) {
            val errors = (valid.copy(period = bad).validate() as FormResult.Invalid).errors
            assertNotNull("Debería rechazar $bad", errors.period)
        }
    }

    @Test
    fun rejectsTooLongNames() {
        val long = "a".repeat(257)
        assertNotNull((valid.copy(accountName = long).validate() as FormResult.Invalid).errors.accountName)
        assertNotNull((valid.copy(issuer = long).validate() as FormResult.Invalid).errors.accountName)
    }

    @Test
    fun toStringHidesSecret() {
        assertFalse(valid.toString().contains("jbsw"))
    }

    @Test
    fun formatsCodesInGroups() {
        assertEquals("123 456", formatCode("123456"))
        assertEquals("1234 567", formatCode("1234567"))
        assertEquals("1234 5678", formatCode("12345678"))
    }

    @Test
    fun formFromScannedAccountRoundTrips() {
        val scanned = OtpAuthUri.parse(
            "otpauth://totp/GitHub:sebastian?secret=JBSWY3DPEHPK3PXP&issuer=GitHub&algorithm=SHA256&digits=8&period=60"
        )
        val form = AccountForm.from(scanned)
        assertEquals("GitHub", form.issuer)
        assertEquals("sebastian", form.accountName)
        assertEquals("JBSWY3DPEHPK3PXP", form.secret)
        assertFalse(form.hasDefaultAdvancedOptions)

        val rebuilt = (form.validate() as FormResult.Valid).account
        assertTrue(rebuilt.isSameAs(scanned))
        assertEquals(scanned.codeAt(1_700_000_000), rebuilt.codeAt(1_700_000_000))
    }

    @Test
    fun defaultAdvancedOptionsDetected() {
        assertTrue(AccountForm.from(OtpAuthUri.parse("otpauth://totp/ana?secret=JBSWY3DPEHPK3PXP")).hasDefaultAdvancedOptions)
    }
}

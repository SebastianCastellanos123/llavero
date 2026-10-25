package io.github.sebastiancastellanos123.llavero.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrDecoderTest {
    private val uri = "otpauth://totp/GitHub:sebasti%C3%A1n?secret=JBSWY3DPEHPK3PXP&issuer=GitHub"

    private fun qr(text: String, size: Int = 300): BitMatrix =
        QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 2))

    private fun BitMatrix.toLuminance(rowStride: Int = width, inverted: Boolean = false): ByteArray {
        val data = ByteArray(rowStride * height) { 0x7F }
        for (y in 0 until height) for (x in 0 until width) {
            val black = get(x, y) != inverted
            data[y * rowStride + x] = if (black) 0 else 0xFF.toByte()
        }
        return data
    }

    private fun BitMatrix.toArgb(): IntArray =
        IntArray(width * height) { i -> if (get(i % width, i / width)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }

    @Test
    fun decodesCameraFrame() {
        val matrix = qr(uri)
        assertEquals(uri, QrDecoder.decodeLuminance(matrix.toLuminance(), matrix.width, matrix.width, matrix.height))
    }

    @Test
    fun handlesRowPaddingFromCamera() {
        val matrix = qr(uri)
        val stride = matrix.width + 64
        assertEquals(uri, QrDecoder.decodeLuminance(matrix.toLuminance(rowStride = stride), stride, matrix.width, matrix.height))
    }

    @Test
    fun decodesInvertedQr() {
        val matrix = qr(uri)
        assertEquals(uri, QrDecoder.decodeLuminance(matrix.toLuminance(inverted = true), matrix.width, matrix.width, matrix.height))
    }

    @Test
    fun decodesArgbImage() {
        val matrix = qr(uri)
        assertEquals(uri, QrDecoder.decodeArgb(matrix.toArgb(), matrix.width, matrix.height))
    }

    @Test
    fun returnsNullWithoutQr() {
        val blank = ByteArray(200 * 200) { 0xFF.toByte() }
        assertNull(QrDecoder.decodeLuminance(blank, 200, 200, 200))
        val noise = java.util.Random(42).let { r -> ByteArray(200 * 200) { r.nextInt().toByte() } }
        assertNull(QrDecoder.decodeLuminance(noise, 200, 200, 200))
    }
}

package io.github.sebastiancastellanos123.llavero.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/**
 * Lee códigos QR con ZXing core: Java puro, sin red ni Google Play Services,
 * así que se puede probar en la JVM y no obliga a pedir permiso INTERNET.
 */
object QrDecoder {
    private val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.CHARACTER_SET to "UTF-8",
    )

    /**
     * Para cuadros de la cámara: el plano Y (brillo) de una imagen YUV.
     * [rowStride] puede ser mayor que [width] porque algunas cámaras rellenan cada fila.
     */
    fun decodeLuminance(data: ByteArray, rowStride: Int, width: Int, height: Int): String? =
        decode(PlanarYUVLuminanceSource(data, rowStride, height, 0, 0, width, height, false))

    /** Para imágenes de la galería: píxeles ARGB. */
    fun decodeArgb(pixels: IntArray, width: Int, height: Int): String? =
        decode(RGBLuminanceSource(width, height, pixels))

    private fun decode(source: LuminanceSource): String? {
        val reader = QRCodeReader()
        // También se prueba invertido: algunos servicios con tema oscuro muestran el QR claro sobre negro.
        for (candidate in listOf(source, source.invert())) {
            try {
                return reader.decode(BinaryBitmap(HybridBinarizer(candidate)), hints).text
            } catch (e: ReaderException) {
                // No hay QR legible en esta versión de la imagen; se prueba la siguiente.
            } finally {
                reader.reset()
            }
        }
        return null
    }
}

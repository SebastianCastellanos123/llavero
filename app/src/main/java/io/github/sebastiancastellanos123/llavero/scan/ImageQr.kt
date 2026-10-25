package io.github.sebastiancastellanos123.llavero.scan

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri

/** Lado máximo al leer una imagen: suficiente para un QR y evita cargar fotos enormes en memoria. */
private const val MAX_IMAGE_SIDE = 2048

/**
 * Busca un QR en una imagen elegida con el selector de fotos del sistema.
 * El selector no necesita permisos de almacenamiento: la app solo ve la imagen elegida.
 */
fun decodeQrFromImage(context: Context, uri: Uri): String? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_IMAGE_SIDE) sampleSize *= 2

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: return null
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val width = bitmap.width
    val height = bitmap.height
    bitmap.recycle()
    return try {
        QrDecoder.decodeArgb(pixels, width, height)
    } finally {
        pixels.fill(0)
    }
}

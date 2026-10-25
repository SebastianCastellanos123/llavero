package io.github.sebastiancastellanos123.llavero.storage

import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Cifrado autenticado: si alguien altera los datos o el AAD, decrypt falla. */
interface Aead {
    /** Devuelve IV + texto cifrado + etiqueta. */
    fun encrypt(plaintext: ByteArray, aad: ByteArray): ByteArray

    /** @throws GeneralSecurityException si los datos fueron alterados o la clave no es la correcta. */
    fun decrypt(blob: ByteArray, aad: ByteArray): ByteArray
}

/**
 * AES-GCM con la clave que entregue [key]. En la app la clave vive en Android
 * Keystore; en las pruebas de JVM es una clave normal en memoria.
 */
class AesGcmAead(private val key: () -> SecretKey) : Aead {
    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }

    override fun encrypt(plaintext: ByteArray, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        // Sin parámetros, el proveedor genera un IV aleatorio. Android Keystore
        // incluso prohíbe pasar uno propio, lo que evita reutilizar un IV por error.
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(aad)
        val ciphertext = cipher.doFinal(plaintext)
        val iv = cipher.iv
        check(iv.size == IV_BYTES) { "IV inesperado de ${iv.size} bytes" }
        return iv + ciphertext
    }

    override fun decrypt(blob: ByteArray, aad: ByteArray): ByteArray {
        if (blob.size < IV_BYTES + TAG_BITS / 8) throw GeneralSecurityException("Datos cifrados demasiado cortos")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, blob, 0, IV_BYTES))
        cipher.updateAAD(aad)
        return cipher.doFinal(blob, IV_BYTES, blob.size - IV_BYTES)
    }
}

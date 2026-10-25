package io.github.sebastiancastellanos123.llavero.backup

import io.github.sebastiancastellanos123.llavero.otp.OtpAccount
import io.github.sebastiancastellanos123.llavero.storage.AccountCodec
import io.github.sebastiancastellanos123.llavero.storage.AesGcmAead
import io.github.sebastiancastellanos123.llavero.storage.CorruptStoreException
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class InvalidBackupException(message: String) : Exception(message)

/** AES-GCM no distingue entre contraseña equivocada y archivo alterado: las dos fallan igual. */
class WrongPasswordException : Exception("Contraseña incorrecta o el archivo fue alterado")

/**
 * Respaldo cifrado con contraseña, para pasar las cuentas a otro teléfono.
 * A diferencia del almacén local, no depende del Keystore: solo de la contraseña.
 *
 *     "LLVB" | versión (1) | KDF (1) | iteraciones (4) | sal (16) | IV (12) | cifrado + etiqueta (16)
 *     └────────────── encabezado: va como AAD ──────────────┘
 *
 * La clave sale de PBKDF2-HMAC-SHA256. El número de iteraciones queda en el
 * archivo, así que se puede subir en versiones futuras sin romper los respaldos viejos.
 */
object BackupCodec {
    /** Mínimo recomendado por OWASP (2023) para PBKDF2-HMAC-SHA256. */
    const val DEFAULT_ITERATIONS = 600_000
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_FILE_BYTES = 1_048_576

    // Al importar se aceptan solo valores razonables: un archivo malicioso con
    // dos mil millones de iteraciones dejaría la app pegada calculando.
    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 10_000_000

    private val MAGIC = byteArrayOf('L'.code.toByte(), 'L'.code.toByte(), 'V'.code.toByte(), 'B'.code.toByte())
    private const val VERSION: Byte = 1
    private const val KDF_PBKDF2_SHA256: Byte = 1
    private const val SALT_BYTES = 16
    private const val HEADER_BYTES = 4 + 1 + 1 + 4 + SALT_BYTES
    private const val MIN_CIPHERTEXT_BYTES = 12 + 16 // IV + etiqueta

    fun encrypt(
        accounts: List<OtpAccount>,
        password: CharArray,
        iterations: Int = DEFAULT_ITERATIONS,
        random: SecureRandom = SecureRandom(),
    ): ByteArray {
        require(password.size >= MIN_PASSWORD_LENGTH) { "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres" }
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Iteraciones fuera de rango" }

        // Sal nueva en cada respaldo: la misma contraseña nunca da la misma clave dos veces.
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC).put(VERSION).put(KDF_PBKDF2_SHA256).putInt(iterations).put(salt)
            .array()

        val key = deriveKey(password, salt, iterations)
        val plaintext = AccountCodec.encode(accounts)
        try {
            return header + aead(key).encrypt(plaintext, header)
        } finally {
            plaintext.fill(0)
            key.fill(0)
        }
    }

    fun decrypt(data: ByteArray, password: CharArray): List<OtpAccount> {
        if (data.size > MAX_FILE_BYTES || data.size < HEADER_BYTES + MIN_CIPHERTEXT_BYTES) {
            throw InvalidBackupException("El archivo no es un respaldo de Llavero")
        }
        val buffer = ByteBuffer.wrap(data)
        val magic = ByteArray(MAGIC.size).also { buffer.get(it) }
        if (!magic.contentEquals(MAGIC)) throw InvalidBackupException("El archivo no es un respaldo de Llavero")
        if (buffer.get() != VERSION) throw InvalidBackupException("Este respaldo se creó con una versión más nueva de Llavero")
        if (buffer.get() != KDF_PBKDF2_SHA256) throw InvalidBackupException("Formato de respaldo desconocido")
        val iterations = buffer.int
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) throw InvalidBackupException("Respaldo con parámetros no válidos")
        val salt = ByteArray(SALT_BYTES).also { buffer.get(it) }

        val header = data.copyOfRange(0, HEADER_BYTES)
        val key = deriveKey(password, salt, iterations)
        val plaintext = try {
            aead(key).decrypt(data.copyOfRange(HEADER_BYTES, data.size), header)
        } catch (e: GeneralSecurityException) {
            throw WrongPasswordException()
        } finally {
            key.fill(0)
        }
        try {
            return AccountCodec.decode(plaintext)
        } catch (e: CorruptStoreException) {
            // La etiqueta GCM fue válida pero el contenido no se entiende: no debería pasar.
            throw InvalidBackupException("El respaldo está dañado")
        } finally {
            plaintext.fill(0)
        }
    }

    internal fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int, lengthBytes: Int = 32): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, lengthBytes * 8)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun aead(key: ByteArray) = AesGcmAead { SecretKeySpec(key, "AES") }
}

package io.github.sebastiancastellanos123.llavero.otp

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class HashAlgorithm(val macName: String) {
    SHA1("HmacSHA1"),
    SHA256("HmacSHA256"),
    SHA512("HmacSHA512"),
}

/** HOTP (RFC 4226) y TOTP (RFC 6238). */
object Otp {
    const val DEFAULT_PERIOD = 30
    const val DEFAULT_DIGITS = 6
    val SUPPORTED_DIGITS = 6..8

    private val POWERS_OF_TEN = intArrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000)

    fun hotp(
        secret: ByteArray,
        counter: Long,
        digits: Int = DEFAULT_DIGITS,
        algorithm: HashAlgorithm = HashAlgorithm.SHA1,
    ): String {
        require(secret.isNotEmpty()) { "El secreto está vacío" }
        require(digits in SUPPORTED_DIGITS) { "Dígitos no soportados: $digits" }

        val mac = Mac.getInstance(algorithm.macName)
        mac.init(SecretKeySpec(secret, algorithm.macName))
        val hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array())

        // Truncamiento dinámico (RFC 4226, sección 5.3): el último nibble elige
        // dónde leer 31 bits del HMAC.
        val offset = hash.last().toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)

        return (binary % POWERS_OF_TEN[digits]).toString().padStart(digits, '0')
    }

    fun counterAt(epochSeconds: Long, period: Int = DEFAULT_PERIOD): Long {
        require(period > 0) { "Periodo inválido: $period" }
        return epochSeconds.floorDiv(period.toLong())
    }

    fun totp(
        secret: ByteArray,
        epochSeconds: Long,
        period: Int = DEFAULT_PERIOD,
        digits: Int = DEFAULT_DIGITS,
        algorithm: HashAlgorithm = HashAlgorithm.SHA1,
    ): String = hotp(secret, counterAt(epochSeconds, period), digits, algorithm)

    /** Segundos que le quedan al código actual; sirve para la cuenta regresiva. */
    fun secondsRemaining(epochSeconds: Long, period: Int = DEFAULT_PERIOD): Int {
        require(period > 0) { "Periodo inválido: $period" }
        return period - epochSeconds.mod(period.toLong()).toInt()
    }
}

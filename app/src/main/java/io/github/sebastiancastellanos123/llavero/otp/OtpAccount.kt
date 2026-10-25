package io.github.sebastiancastellanos123.llavero.otp

/**
 * Una cuenta TOTP. El secreto se copia al entrar y al salir para que nadie
 * pueda modificar el arreglo interno, y toString() nunca lo imprime: así no
 * termina en un log ni en un reporte de error.
 */
class OtpAccount(
    val issuer: String?,
    val accountName: String,
    secret: ByteArray,
    val algorithm: HashAlgorithm = HashAlgorithm.SHA1,
    val digits: Int = Otp.DEFAULT_DIGITS,
    val period: Int = Otp.DEFAULT_PERIOD,
) {
    private val secretBytes = secret.copyOf()

    init {
        require(accountName.isNotBlank()) { "Falta el nombre de la cuenta" }
        require(secretBytes.isNotEmpty()) { "El secreto está vacío" }
        require(digits in Otp.SUPPORTED_DIGITS) { "Dígitos no soportados: $digits" }
        require(period > 0) { "Periodo inválido: $period" }
    }

    fun secret(): ByteArray = secretBytes.copyOf()

    fun codeAt(epochSeconds: Long): String =
        Otp.totp(secretBytes, epochSeconds, period, digits, algorithm)

    override fun toString(): String =
        "OtpAccount(issuer=$issuer, accountName=$accountName, algorithm=$algorithm, " +
            "digits=$digits, period=$period, secret=<oculto>)"
}

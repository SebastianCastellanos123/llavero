package io.github.sebastiancastellanos123.llavero.ui

import io.github.sebastiancastellanos123.llavero.otp.Base32
import io.github.sebastiancastellanos123.llavero.otp.HashAlgorithm
import io.github.sebastiancastellanos123.llavero.otp.Otp
import io.github.sebastiancastellanos123.llavero.otp.OtpAccount

/** Lo que el usuario escribe en "Agregar cuenta", antes de validarlo. */
data class AccountForm(
    val issuer: String = "",
    val accountName: String = "",
    val secret: String = "",
    val algorithm: HashAlgorithm = HashAlgorithm.SHA1,
    val digits: Int = Otp.DEFAULT_DIGITS,
    val period: String = Otp.DEFAULT_PERIOD.toString(),
) {
    companion object {
        /** Formulario ya lleno con lo que traía un QR, para que el usuario lo revise antes de guardar. */
        fun from(account: OtpAccount): AccountForm {
            val secret = account.secret()
            try {
                return AccountForm(
                    issuer = account.issuer.orEmpty(),
                    accountName = account.accountName,
                    secret = Base32.encode(secret).trimEnd('='),
                    algorithm = account.algorithm,
                    digits = account.digits,
                    period = account.period.toString(),
                )
            } finally {
                secret.fill(0)
            }
        }
    }

    /** true si el usuario no necesita mirar las opciones avanzadas. */
    val hasDefaultAdvancedOptions: Boolean
        get() = algorithm == HashAlgorithm.SHA1 && digits == Otp.DEFAULT_DIGITS && period == Otp.DEFAULT_PERIOD.toString()

    // El secreto nunca aparece si el formulario termina en un log.
    override fun toString(): String =
        "AccountForm(issuer=$issuer, accountName=$accountName, secret=<oculto>, " +
            "algorithm=$algorithm, digits=$digits, period=$period)"
}

data class FormErrors(
    val accountName: String? = null,
    val secret: String? = null,
    val period: String? = null,
) {
    val isEmpty get() = accountName == null && secret == null && period == null
}

sealed interface FormResult {
    class Valid(val account: OtpAccount) : FormResult
    data class Invalid(val errors: FormErrors) : FormResult
}

fun AccountForm.validate(): FormResult {
    val name = accountName.trim()
    val nameError = when {
        name.isEmpty() -> "Escribe el nombre de la cuenta"
        name.length > OtpAccount.MAX_NAME_LENGTH -> "Máximo ${OtpAccount.MAX_NAME_LENGTH} caracteres"
        issuer.trim().length > OtpAccount.MAX_NAME_LENGTH -> "El emisor tiene más de ${OtpAccount.MAX_NAME_LENGTH} caracteres"
        else -> null
    }

    var secretBytes: ByteArray? = null
    val secretError = when {
        secret.isBlank() -> "Escribe la clave secreta"
        else -> try {
            secretBytes = Base32.decode(secret)
            if (secretBytes.isEmpty()) "La clave secreta está vacía" else null
        } catch (e: IllegalArgumentException) {
            "Clave no válida: solo letras A-Z y números 2-7"
        }
    }

    val periodValue = period.trim().toIntOrNull()
    val periodError = if (periodValue == null || periodValue !in 1..300) "Entre 1 y 300 segundos" else null

    val errors = FormErrors(nameError, secretError, periodError)
    if (!errors.isEmpty) {
        secretBytes?.fill(0)
        return FormResult.Invalid(errors)
    }
    try {
        return FormResult.Valid(
            OtpAccount(issuer.trim().ifEmpty { null }, name, secretBytes!!, algorithm, digits, periodValue!!)
        )
    } finally {
        secretBytes!!.fill(0)
    }
}

/** "123456" → "123 456"; "12345678" → "1234 5678". Más fácil de leer y dictar. */
fun formatCode(code: String): String = when (code.length) {
    6 -> "${code.substring(0, 3)} ${code.substring(3)}"
    7 -> "${code.substring(0, 4)} ${code.substring(4)}"
    8 -> "${code.substring(0, 4)} ${code.substring(4)}"
    else -> code
}

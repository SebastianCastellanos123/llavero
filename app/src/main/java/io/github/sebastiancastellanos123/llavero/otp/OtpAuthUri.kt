package io.github.sebastiancastellanos123.llavero.otp

import java.io.ByteArrayOutputStream

/**
 * Lee los enlaces `otpauth://totp/Emisor:cuenta?secret=...` que vienen en los
 * códigos QR (formato "Key Uri" de Google Authenticator).
 *
 * El QR llega de afuera, así que se trata como entrada no confiable: se rechaza
 * cualquier cosa ambigua (parámetros repetidos, escapes mal formados) en vez
 * de adivinar qué quiso decir.
 */
object OtpAuthUri {
    private const val SCHEME = "otpauth://"

    fun parse(uri: String): OtpAccount {
        require(uri.startsWith(SCHEME, ignoreCase = true)) { "No es un enlace otpauth://" }
        val rest = uri.substring(SCHEME.length).substringBefore('#')

        val type = rest.substringBefore('/', missingDelimiterValue = "")
        require(!type.equals("hotp", ignoreCase = true)) {
            "Las cuentas HOTP (por contador) no están soportadas"
        }
        require(type.equals("totp", ignoreCase = true)) { "Tipo de cuenta desconocido" }

        val pathAndQuery = rest.substringAfter('/')
        val label = percentDecode(pathAndQuery.substringBefore('?'))
        val params = parseQuery(pathAndQuery.substringAfter('?', missingDelimiterValue = ""))

        val labelIssuer = label.substringBefore(':', missingDelimiterValue = "").trim()
        val accountName = label.substringAfter(':').trim()

        val secret = params["secret"] ?: throw IllegalArgumentException("Falta el secreto")
        val issuer = params["issuer"]?.trim()?.ifEmpty { null } ?: labelIssuer.ifEmpty { null }

        val algorithm = params["algorithm"]?.let { name ->
            HashAlgorithm.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: throw IllegalArgumentException("Algoritmo no soportado")
        } ?: HashAlgorithm.SHA1

        val digits = params["digits"]?.let {
            it.toIntOrNull()?.takeIf { d -> d in Otp.SUPPORTED_DIGITS }
                ?: throw IllegalArgumentException("Dígitos no soportados")
        } ?: Otp.DEFAULT_DIGITS

        val period = params["period"]?.let {
            it.toIntOrNull()?.takeIf { p -> p > 0 }
                ?: throw IllegalArgumentException("Periodo inválido")
        } ?: Otp.DEFAULT_PERIOD

        return OtpAccount(issuer, accountName, Base32.decode(secret), algorithm, digits, period)
    }

    private fun parseQuery(query: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        if (query.isEmpty()) return params
        for (pair in query.split('&')) {
            if (pair.isEmpty()) continue
            val key = percentDecode(pair.substringBefore('=')).lowercase()
            val value = percentDecode(pair.substringAfter('=', missingDelimiterValue = ""))
            require(params.put(key, value) == null) { "Parámetro repetido: $key" }
        }
        return params
    }

    // A diferencia de URLDecoder, no convierte '+' en espacio: en una etiqueta
    // como "ana+banco@correo.com" el '+' es literal.
    private fun percentDecode(text: String): String {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream(bytes.size)
        var i = 0
        while (i < bytes.size) {
            if (bytes[i] == '%'.code.toByte()) {
                require(i + 2 < bytes.size) { "Escape % incompleto" }
                val high = Character.digit(bytes[i + 1].toInt(), 16)
                val low = Character.digit(bytes[i + 2].toInt(), 16)
                require(high >= 0 && low >= 0) { "Escape % inválido" }
                out.write((high shl 4) or low)
                i += 3
            } else {
                out.write(bytes[i].toInt())
                i++
            }
        }
        return out.toString(Charsets.UTF_8.name())
    }
}

package io.github.sebastiancastellanos123.llavero.otp

/**
 * Codificación Base32 (RFC 4648, sección 6). Es el formato en que los servicios
 * entregan el secreto TOTP, tanto en el código QR como para escribirlo a mano.
 */
object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    // Con relleno quitado, una entrada válida solo puede dejar estos restos de 8.
    private val VALID_REMAINDERS = setOf(0, 2, 4, 5, 7)

    fun encode(data: ByteArray): String {
        val out = StringBuilder((data.size + 4) / 5 * 8)
        var buffer = 0
        var bits = 0
        for (b in data) {
            buffer = ((buffer shl 8) or (b.toInt() and 0xFF)) and 0xFFF
            bits += 8
            while (bits >= 5) {
                out.append(ALPHABET[(buffer shr (bits - 5)) and 0x1F])
                bits -= 5
            }
        }
        if (bits > 0) out.append(ALPHABET[(buffer shl (5 - bits)) and 0x1F])
        while (out.length % 8 != 0) out.append('=')
        return out.toString()
    }

    /**
     * Acepta minúsculas, espacios, guiones y relleno opcional, porque así suelen
     * mostrar el secreto los servicios cuando no se puede escanear el QR.
     * El mensaje de error da la posición, nunca el carácter: no se filtra el secreto.
     */
    fun decode(input: String): ByteArray {
        val clean = input.filterNot { it.isWhitespace() || it == '-' }
            .uppercase()
            .trimEnd('=')
        require(clean.length % 8 in VALID_REMAINDERS) { "Longitud Base32 inválida" }

        val out = ByteArray(clean.length * 5 / 8)
        var buffer = 0
        var bits = 0
        var index = 0
        clean.forEachIndexed { position, c ->
            val value = ALPHABET.indexOf(c)
            require(value >= 0) { "Carácter Base32 inválido en la posición ${position + 1}" }
            buffer = ((buffer shl 5) or value) and 0xFFF
            bits += 5
            if (bits >= 8) {
                out[index++] = (buffer shr (bits - 8)).toByte()
                bits -= 8
            }
        }
        return out
    }
}

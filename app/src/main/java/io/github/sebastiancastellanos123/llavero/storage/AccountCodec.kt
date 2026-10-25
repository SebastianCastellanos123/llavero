package io.github.sebastiancastellanos123.llavero.storage

import io.github.sebastiancastellanos123.llavero.otp.HashAlgorithm
import io.github.sebastiancastellanos123.llavero.otp.OtpAccount
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class CorruptStoreException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Formato binario de la lista de cuentas (antes de cifrar). Lleva versión para
 * poder migrarlo más adelante, y al leer valida cada campo: un archivo dañado
 * da un error claro en vez de cuentas a medias.
 */
object AccountCodec {
    private const val FORMAT_VERSION = 1
    private const val MAX_ACCOUNTS = 10_000
    private const val MAX_SECRET_BYTES = 1024

    fun encode(accounts: List<OtpAccount>): ByteArray {
        val bytes = WipeableByteArrayOutputStream()
        try {
            DataOutputStream(bytes).use { out ->
                out.writeInt(FORMAT_VERSION)
                out.writeInt(accounts.size)
                for (account in accounts) {
                    out.writeBoolean(account.issuer != null)
                    account.issuer?.let(out::writeUTF)
                    out.writeUTF(account.accountName)
                    out.writeUTF(account.algorithm.name)
                    out.writeInt(account.digits)
                    out.writeInt(account.period)
                    val secret = account.secret()
                    try {
                        out.writeInt(secret.size)
                        out.write(secret)
                    } finally {
                        secret.fill(0)
                    }
                }
            }
            return bytes.toByteArray()
        } finally {
            bytes.wipe()
        }
    }

    fun decode(data: ByteArray): List<OtpAccount> {
        val input = DataInputStream(ByteArrayInputStream(data))
        try {
            val version = input.readInt()
            if (version != FORMAT_VERSION) throw CorruptStoreException("Versión de formato desconocida: $version")
            val count = input.readInt()
            if (count !in 0..MAX_ACCOUNTS) throw CorruptStoreException("Cantidad de cuentas inválida: $count")

            val accounts = ArrayList<OtpAccount>(count)
            repeat(count) {
                val issuer = if (input.readBoolean()) input.readUTF() else null
                val accountName = input.readUTF()
                val algorithmName = input.readUTF()
                val algorithm = HashAlgorithm.entries.firstOrNull { it.name == algorithmName }
                    ?: throw CorruptStoreException("Algoritmo desconocido")
                val digits = input.readInt()
                val period = input.readInt()
                val secretSize = input.readInt()
                if (secretSize !in 1..MAX_SECRET_BYTES) throw CorruptStoreException("Tamaño de secreto inválido")
                val secret = ByteArray(secretSize)
                try {
                    input.readFully(secret)
                    accounts += OtpAccount(issuer, accountName, secret, algorithm, digits, period)
                } finally {
                    secret.fill(0)
                }
            }
            if (input.read() != -1) throw CorruptStoreException("Datos sobrantes al final")
            return accounts
        } catch (e: IOException) {
            throw CorruptStoreException("Datos truncados o mal formados", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptStoreException("Cuenta inválida en el archivo", e)
        }
    }

    // ByteArrayOutputStream guarda su propio búfer con los secretos; este lo borra al terminar.
    private class WipeableByteArrayOutputStream : ByteArrayOutputStream() {
        fun wipe() = buf.fill(0)
    }
}

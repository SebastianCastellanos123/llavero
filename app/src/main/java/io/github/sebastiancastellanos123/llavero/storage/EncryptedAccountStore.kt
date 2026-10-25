package io.github.sebastiancastellanos123.llavero.storage

import android.content.Context
import io.github.sebastiancastellanos123.llavero.otp.OtpAccount
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.GeneralSecurityException

/**
 * Guarda las cuentas en un solo archivo cifrado:
 *
 *     "LLV" | versión (1 byte) | IV (12) | texto cifrado | etiqueta GCM (16)
 *
 * El encabezado va como AAD: no está cifrado, pero si alguien lo cambia el
 * descifrado falla igual que si tocara el contenido.
 */
class EncryptedAccountStore(private val file: File, private val aead: Aead) {

    companion object {
        private val HEADER = byteArrayOf('L'.code.toByte(), 'L'.code.toByte(), 'V'.code.toByte(), 1)
        private const val FILE_NAME = "accounts.bin"
        private const val KEY_ALIAS = "llavero.accounts"

        /**
         * noBackupFilesDir queda fuera de las copias de seguridad de Android; de
         * todos modos el archivo no sirve sin la clave, que no sale del Keystore.
         */
        fun create(context: Context) = EncryptedAccountStore(
            File(context.noBackupFilesDir, FILE_NAME),
            AesGcmAead { KeystoreKey.getOrCreate(KEY_ALIAS) },
        )
    }

    @Synchronized
    fun load(): List<OtpAccount> {
        if (!file.exists()) return emptyList()
        val bytes = file.readBytes()
        if (bytes.size < HEADER.size || !bytes.copyOfRange(0, HEADER.size).contentEquals(HEADER)) {
            throw CorruptStoreException("Encabezado de archivo inválido")
        }
        val plaintext = try {
            aead.decrypt(bytes.copyOfRange(HEADER.size, bytes.size), HEADER)
        } catch (e: GeneralSecurityException) {
            throw CorruptStoreException("No se pudo descifrar: el archivo fue alterado o la clave cambió", e)
        }
        try {
            return AccountCodec.decode(plaintext)
        } finally {
            plaintext.fill(0)
        }
    }

    @Synchronized
    fun save(accounts: List<OtpAccount>) {
        val plaintext = AccountCodec.encode(accounts)
        val blob = try {
            HEADER + aead.encrypt(plaintext, HEADER)
        } finally {
            plaintext.fill(0)
        }

        // Escritura atómica: si el teléfono se apaga a la mitad, queda el archivo
        // anterior completo y no uno a medias.
        val temp = File(file.parentFile, "${file.name}.tmp")
        FileOutputStream(temp).use { out ->
            out.write(blob)
            out.fd.sync()
        }
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}

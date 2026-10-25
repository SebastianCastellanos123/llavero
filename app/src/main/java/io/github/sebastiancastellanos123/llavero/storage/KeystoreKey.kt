package io.github.sebastiancastellanos123.llavero.storage

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Clave AES-256 dentro de Android Keystore. La app solo recibe una referencia:
 * el material de la clave nunca sale del hardware seguro, así que copiar el
 * archivo de cuentas a otro teléfono no sirve para descifrarlo.
 */
object KeystoreKey {
    private const val PROVIDER = "AndroidKeyStore"

    @Synchronized
    fun getOrCreate(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return generate(alias, useStrongBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
    }

    fun delete(alias: String) {
        KeyStore.getInstance(PROVIDER).apply { load(null) }.deleteEntry(alias)
    }

    private fun generate(alias: String, useStrongBox: Boolean): SecretKey {
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
        if (useStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        return try {
            generator.init(builder.build())
            generator.generateKey()
        } catch (e: ProviderException) {
            // StrongBoxUnavailableException hereda de ProviderException: el teléfono
            // no tiene chip StrongBox, así que se usa el Keystore normal (TEE).
            if (useStrongBox) generate(alias, useStrongBox = false) else throw e
        }
    }
}

@file:Suppress("DEPRECATION")
package com.andrea.showmateapp.util

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

object DatabaseKeyProvider {

    private const val PREFS_NAME = "db_key_store"
    private const val KEY_PASSPHRASE = "db_passphrase_v1"

    fun getOrCreatePassphrase(context: Context): ByteArray {
        // MasterKey almacenado en Android Keystore: la clave maestra nunca sale del hardware (TEE/StrongBox)
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        // EncryptedSharedPreferences cifra tanto claves como valores; protege la passphrase de SQLCipher
        // en caso de extracción física del almacenamiento del dispositivo (cifrado en reposo)
        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val stored = prefs.getString(KEY_PASSPHRASE, null)
        if (stored != null) return Base64.decode(stored, Base64.NO_WRAP)
        // 256 bits de entropía via SecureRandom: la passphrase se genera una sola vez y persiste;
        // SQLCipher la usa como clave AES-256 para cifrar toda la base de datos Room en disco
        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_PASSPHRASE, Base64.encodeToString(passphrase, Base64.NO_WRAP))
            .apply()
        return passphrase
    }
}

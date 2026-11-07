package com.neilyin.nnowork.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.neilyin.nnowork.data.JsonProvider
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class StoredAuthSession(
    val accessToken: String,
    val refreshToken: String,
    val email: String? = null,
)

/** Session 只以 Android Keystore AES-GCM 密文保存，不進 DataStore/備份 JSON。 */
internal class AuthSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val adapter = JsonProvider.adapter<StoredAuthSession>()

    fun read(): StoredAuthSession? {
        val encoded = preferences.getString(SESSION_KEY, null) ?: return null
        return runCatching {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            require(packed.size > IV_BYTES)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, packed, 0, IV_BYTES))
            val plain = cipher.doFinal(packed, IV_BYTES, packed.size - IV_BYTES)
            adapter.fromJson(plain.toString(Charsets.UTF_8))
        }.getOrElse {
            clear()
            null
        }
    }

    fun write(session: StoredAuthSession) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(adapter.toJson(session).toByteArray(Charsets.UTF_8))
        val packed = cipher.iv + encrypted
        preferences.edit().putString(SESSION_KEY, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    fun clear() {
        preferences.edit().remove(SESSION_KEY).apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFS_NAME = "nnowork_auth_secure"
        const val SESSION_KEY = "session_ciphertext"
        const val KEY_ALIAS = "nnowork_auth_session_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
    }
}

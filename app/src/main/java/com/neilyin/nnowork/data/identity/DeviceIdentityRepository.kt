package com.neilyin.nnowork.data.identity

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.neilyin.nnowork.data.datastore.appDataStore
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/**
 * 提供 App 永久固定的匿名裝置身份。
 *
 * 新安裝以隨機 UUID 建立身份；從尚未保存身份的舊版升級時，若啟動當下已有
 * 本機資料，則沿用舊版使用的 ANDROID_ID 雜湊，避免樹洞內容失聯。DataStore
 * 一旦已有 device hash 就只讀取、不再依系統識別碼重算。
 */
class DeviceIdentityRepository private constructor(
    private val context: Context,
    private val hadLegacyLocalDataAtStartup: Boolean,
    private val androidIdProvider: () -> String?,
    private val uuidProvider: () -> String,
) {
    private val deviceHashKey = stringPreferencesKey(DEVICE_HASH_KEY)

    /** 取得已保存的 device hash；首次呼叫才以相容規則建立並原子寫入。 */
    suspend fun deviceHash(): String {
        var resolvedHash: String? = null

        context.appDataStore.edit { preferences ->
            resolvedHash = preferences[deviceHashKey] ?: createInitialHash().also { newHash ->
                preferences[deviceHashKey] = newHash
            }
        }

        return checkNotNull(resolvedHash)
    }

    private fun createInitialHash(): String {
        // 全新安裝完全不讀取系統識別碼；ANDROID_ID 僅供舊資料的一次性遷移。
        val legacyAndroidId = if (hadLegacyLocalDataAtStartup) androidIdProvider() else null
        val randomUuid = if (legacyAndroidId.isNullOrBlank()) uuidProvider() else ""
        val seed = DeviceIdentityCodec.initialSeed(
            hadLegacyLocalData = hadLegacyLocalDataAtStartup,
            legacyAndroidId = legacyAndroidId,
            randomUuid = randomUuid,
        )
        return DeviceIdentityCodec.sha256Hex(seed)
    }

    companion object {
        private const val DEVICE_HASH_KEY = "device_hash"
        private const val DATASTORE_FILE = "datastore/nnowork_prefs.preferences_pb"
        private const val DATABASE_FILE = "nnowork.db"

        /**
         * 必須在 Application 啟動、其他元件建立新資料之前呼叫，才能正確區分
         * 舊版既有資料與本版全新安裝。
         */
        fun createAtAppStartup(context: Context): DeviceIdentityRepository {
            val appContext = context.applicationContext
            return DeviceIdentityRepository(
                context = appContext,
                hadLegacyLocalDataAtStartup = hasLegacyLocalData(appContext),
                androidIdProvider = {
                    Settings.Secure.getString(
                        appContext.contentResolver,
                        Settings.Secure.ANDROID_ID,
                    )
                },
                uuidProvider = { UUID.randomUUID().toString() },
            )
        }

        private fun hasLegacyLocalData(context: Context): Boolean {
            val preferencesFile = File(context.filesDir, DATASTORE_FILE)
            val databaseFile = context.getDatabasePath(DATABASE_FILE)
            return preferencesFile.exists() || databaseFile.exists()
        }
    }
}

/** 可獨立測試的身份選種與編碼規則。 */
internal object DeviceIdentityCodec {
    fun initialSeed(
        hadLegacyLocalData: Boolean,
        legacyAndroidId: String?,
        randomUuid: String,
    ): String = if (hadLegacyLocalData) {
        legacyAndroidId?.takeIf { it.isNotBlank() } ?: randomUuid
    } else {
        randomUuid
    }

    fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
}

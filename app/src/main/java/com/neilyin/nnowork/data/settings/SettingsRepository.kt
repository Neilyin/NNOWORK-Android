package com.neilyin.nnowork.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.neilyin.nnowork.data.JsonProvider
import com.neilyin.nnowork.data.datastore.appDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 讀寫 AppSettings（以 JSON 字串存於 DataStore）。
 */
class SettingsRepository(private val context: Context) {

    private val key = stringPreferencesKey("settings_json")
    private val adapter = JsonProvider.adapter<AppSettings>()

    val settings: Flow<AppSettings> = context.appDataStore.data.map { prefs ->
        prefs[key]?.let { runCatching { adapter.fromJson(it) }.getOrNull() } ?: AppSettings()
    }

    suspend fun current(): AppSettings = settings.first()

    /** 以函式轉換的方式原子更新設定。 */
    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.appDataStore.edit { prefs ->
            val currentValue = prefs[key]?.let { runCatching { adapter.fromJson(it) }.getOrNull() } ?: AppSettings()
            val updated = transform(currentValue)
            prefs[key] = adapter.toJson(updated)
        }
    }
}

package com.neilyin.nnowork.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * 全 App 共用的 Preferences DataStore（取代 iOS 的 UserDefaults / SwiftData 設定）。
 */
val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "nnowork_prefs")

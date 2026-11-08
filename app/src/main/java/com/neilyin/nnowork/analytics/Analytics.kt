package com.neilyin.nnowork.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Firebase Analytics 包裝。沒有 google-services.json 時自動停用，不會 crash。
 * 事件名稱對照 iOS（tab_switched / stamp_added / salary_settings_saved …）。
 */
object Analytics {
    private var fa: FirebaseAnalytics? = null

    fun init(context: Context) {
        runCatching {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                fa = FirebaseAnalytics.getInstance(context)
            }
        }
    }

    fun log(event: String, vararg params: Pair<String, String>) {
        val analytics = fa ?: return
        val bundle = Bundle().apply { params.forEach { putString(it.first, it.second) } }
        runCatching { analytics.logEvent(event, bundle) }
    }
}

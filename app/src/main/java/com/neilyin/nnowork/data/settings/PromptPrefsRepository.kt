package com.neilyin.nnowork.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.neilyin.nnowork.data.datastore.appDataStore
import com.neilyin.nnowork.di.ServiceLocator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 「提示類」旗標：更新 banner 已關閉的版本、評分里程碑是否已用過（iOS 用 UserDefaults）。
 * 與 AppSettings JSON 分開存，不進備份、不被重置清掉。
 */
class PromptPrefsRepository(private val context: Context) {

    private val dismissedUpdateKey = intPreferencesKey("update_banner_dismissed_version_code")
    /** 已問過「匯入國定假日」的「地區-資料版本」（例如 TW-2027）；換地區或資料更新會再問。iOS key 同名。 */
    private val holidayPromptedKey = stringPreferencesKey("niuma_holiday_prompted_region")
    private fun milestoneKey(name: String): Preferences.Key<Boolean> = booleanPreferencesKey(name)

    /** 使用者已關閉 banner 的 Play 版本碼；0 = 從未關閉。 */
    val dismissedUpdateVersionCode: Flow<Int> =
        context.appDataStore.data.map { it[dismissedUpdateKey] ?: 0 }

    suspend fun dismissUpdate(versionCode: Int) {
        context.appDataStore.edit { prefs ->
            val old = prefs[dismissedUpdateKey] ?: 0
            if (versionCode > old) prefs[dismissedUpdateKey] = versionCode
        }
    }

    suspend fun isReviewMomentUsed(moment: ReviewMoment): Boolean =
        context.appDataStore.data.first()[milestoneKey(moment.prefKey)] ?: false

    suspend fun markReviewMomentUsed(moment: ReviewMoment) {
        context.appDataStore.edit { it[milestoneKey(moment.prefKey)] = true }
    }

    val holidayPromptedTag: Flow<String> = context.appDataStore.data.map { it[holidayPromptedKey].orEmpty() }

    suspend fun markHolidayPrompted(tag: String) {
        context.appDataStore.edit { it[holidayPromptedKey] = tag }
    }

    companion object {
        val instance: PromptPrefsRepository by lazy { PromptPrefsRepository(ServiceLocator.applicationContext) }
    }
}

/** 評分的三個正向時刻（對應 iOS ReviewManager.Milestone；prefKey 與 iOS 旗標同名）。 */
enum class ReviewMoment(val prefKey: String) {
    /** 第一次設定薪水計時器 */
    TIMER_SETUP("review_milestone_timerSetup"),
    /** 第一次蓋章 */
    FIRST_STAMP("review_milestone_firstStamp"),
    /** 集滿一張卡 */
    CARD_COMPLETE("review_milestone_cardComplete"),
}

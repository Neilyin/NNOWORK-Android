package com.neilyin.nnowork.ui.theme

import com.neilyin.nnowork.data.backup.BackupAppSettings
import com.neilyin.nnowork.data.backup.BackupCodec
import com.neilyin.nnowork.data.backup.BackupPayload
import com.neilyin.nnowork.data.backup.BackupResignationPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 夜空金（id 10）是 Android 才有的主題。iOS（DataBackup.swift AppSettingsDTO）把 cardStyle 與
 * unlockedStyles 當成純 Int／[Int] 解碼、不驗證範圍，畫面端一律 `CardTheme(rawValue:) ?? .rpg`，
 * 所以備份裡的 10 不會讓 iOS 拒收或閃退：iOS 會顯示 RPG，但原值照樣保存，再匯回 Android 時仍是夜空金。
 * 這組測試鎖定 Android 端輸出的格式維持 iOS 可接受的形狀（整數、不加新欄位）。
 */
class NightThemeBackupCompatTest {
    private val codec = BackupCodec()

    private fun payload() = BackupPayload(
        schemaVersion = BackupPayload.CURRENT_SCHEMA_VERSION,
        exportDate = "2026-11-07T08:30:00Z",
        appSettings = listOf(
            BackupAppSettings(
                cardStyle = CardTheme.NIGHT.id,
                unlockedStyles = listOf(1, 2, 3, CardTheme.NIGHT.id),
                hasMigratedToLedger = true,
            ),
        ),
        resignationPlans = listOf(BackupResignationPlan()),
    )

    @Test
    fun nightThemeRoundTripsThroughTheSharedBackupFormat() {
        val decoded = codec.decodeAndValidate(codec.encode(payload()))
        val settings = decoded.appSettings.single()
        assertEquals(10, settings.cardStyle)
        assertEquals(listOf(1, 2, 3, 10), settings.unlockedStyles)
        assertEquals(CardTheme.NIGHT, CardTheme.fromId(settings.cardStyle))
    }

    @Test
    fun exportedThemeFieldsArePlainIntegersIosCanDecode() {
        val json = codec.encode(payload())
        // iOS: container.decode(Int.self, forKey: .cardStyle) / decode([Int].self, forKey: .unlockedStyles)
        assertTrue(json, Regex("\"cardStyle\"\\s*:\\s*10\\b").containsMatchIn(json))
        assertTrue(json, Regex("\"unlockedStyles\"\\s*:\\s*\\[\\s*1\\s*,\\s*2\\s*,\\s*3\\s*,\\s*10\\s*]").containsMatchIn(json))
    }

    @Test
    fun unknownThemeIdsFallBackToRpgLikeIos() {
        // 反方向的保護：iOS 未來新增 11 號主題時，Android 讀到也要像 iOS 一樣退回 RPG 而不是閃退。
        assertEquals(CardTheme.RPG, CardTheme.fromId(11))
    }
}

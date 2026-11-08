package com.neilyin.nnowork.data.plan

import java.util.Locale

/**
 * 依手機地區選用的國定假日（見紅休預選休假；對應 iOS NiuMaConfig.currentHolidayRegion）。
 *
 * 地區一律看手機系統的「國家／地區」，不看 App 語言。
 * 2026 年資料沿用 iOS（台灣、日本、中國大陸）；2027 年 9 個地區見 [Holidays2027]。
 * 不支援的地區沒有國定假日，只預選週末。
 */
object HolidayCalendar {

    /** 這份資料的版本：換資料（例如加了 2028）就改這裡，讓已回答過的使用者再被問一次。 */
    const val DATA_VERSION = "2027"

    private val TW_2026 = setOf(
        "2026-09-25", // 中秋節
        "2026-09-28", // 教師節
        "2026-10-09", // 雙十節補休
        "2026-10-26", // 光復節補休
        "2026-12-25", // 行憲紀念日
    )

    private val JP_2026 = setOf(
        "2026-08-11", // 山の日
        "2026-09-21", // 敬老の日
        "2026-09-22", // 休日
        "2026-09-23", // 秋分の日
        "2026-10-12", // スポーツの日
        "2026-11-03", // 文化の日
        "2026-11-23", // 勤労感謝の日
    )

    private val CN_2026 = setOf(
        "2026-01-01", "2026-01-02", "2026-01-03", // 元旦
        "2026-02-15", "2026-02-16", "2026-02-17", "2026-02-18", "2026-02-19",
        "2026-02-20", "2026-02-21", "2026-02-22", "2026-02-23", // 春節
        "2026-04-04", "2026-04-05", "2026-04-06", // 清明
        "2026-05-01", "2026-05-02", "2026-05-03", "2026-05-04", "2026-05-05", // 勞動節
        "2026-06-19", "2026-06-20", "2026-06-21", // 端午
        "2026-09-25", "2026-09-26", "2026-09-27", // 中秋
        "2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04",
        "2026-10-05", "2026-10-06", "2026-10-07", // 國慶
    )

    private val CN_MAKEUP_2026 = setOf(
        "2026-01-04", "2026-02-14", "2026-02-28", "2026-05-09", "2026-09-20", "2026-10-10",
    )

    private val holidaysByRegion: Map<String, Set<String>> = mapOf(
        "TW" to TW_2026 + Holidays2027.TW,
        "JP" to JP_2026 + Holidays2027.JP,
        "CN" to CN_2026 + Holidays2027.CN,
        "US" to Holidays2027.US,
        "HK" to Holidays2027.HK,
        "MY" to Holidays2027.MY,
        "SG" to Holidays2027.SG,
        "CA" to Holidays2027.CA,
        "AU" to Holidays2027.AU,
    )

    private val makeupByRegion: Map<String, Set<String>> = mapOf(
        "CN" to CN_MAKEUP_2026 + Holidays2027.CN_MAKEUP_WORKDAYS,
    )

    val supportedRegions: Set<String> get() = holidaysByRegion.keys

    /** 測試可覆寫；null = 讀手機地區。 */
    @Volatile
    var regionOverride: String? = null

    /** 目前地區碼（大寫）；不支援的地區回 null。 */
    fun currentRegion(): String? {
        val region = (regionOverride ?: systemCountry()).uppercase(Locale.ROOT)
        return region.takeIf { it in holidaysByRegion }
    }

    /**
     * 手機系統的國家／地區。不用 Locale.getDefault()：App 內切換語言（例如英文手機把 App 設成繁中）會改掉它，
     * 那是語言不是所在地。
     */
    private fun systemCountry(): String = try {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            // Android 13+ 的 App 語言會連 Resources.getSystem() 一起改，要問 LocaleManager 拿系統的
            com.neilyin.nnowork.di.ServiceLocator.applicationContext
                .getSystemService(android.app.LocaleManager::class.java)
                .systemLocales[0].country
        } else {
            android.content.res.Resources.getSystem().configuration.locales[0].country
        }
    } catch (_: Throwable) {
        Locale.getDefault().country
    }

    fun holidays(region: String? = currentRegion()): Set<String> = region?.let { holidaysByRegion[it] }.orEmpty()

    /** 調休補班（原為週末、要上班）；只有中國大陸有。 */
    fun makeupWorkdays(region: String? = currentRegion()): Set<String> = region?.let { makeupByRegion[it] }.orEmpty()

    fun hasMakeupWorkdays(region: String?): Boolean = makeupWorkdays(region).isNotEmpty()

    /** 地區名稱，用手機語言顯示（例如「台灣」「日本」「United States」）。 */
    fun displayName(region: String, inLocale: Locale = Locale.getDefault()): String {
        // 與 iOS 文案一致：中文稱「中國大陸」
        if (region == "CN" && inLocale.language == "zh") {
            val simplified = inLocale.script == "Hans" || inLocale.country == "CN" || inLocale.country == "SG"
            return if (simplified) "中国大陆" else "中國大陸"
        }
        return Locale("", region).getDisplayCountry(inLocale).ifBlank { region }
    }
}

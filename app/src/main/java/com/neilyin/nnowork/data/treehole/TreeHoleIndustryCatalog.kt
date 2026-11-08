package com.neilyin.nnowork.data.treehole

import androidx.annotation.StringRes
import com.neilyin.nnowork.R

/**
 * 樹洞行業的單一資料來源。
 *
 * [canonicalValue] 同時是本機 profile 與 Supabase API 的儲存值，既有資料已經使用這些
 * 繁體中文字串，因此只能往後追加，不可改名、翻譯後再送出，也不可移除。
 * UI 應使用 [labelRes] 顯示在地化名稱；伺服器若回傳不在清單的舊值，應保留原文顯示。
 */
data class TreeHoleIndustryDefinition(
    val canonicalValue: String,
    @StringRes val labelRes: Int,
    val iconKey: TreeHoleIndustryIconKey,
)

/** UI 可映射到各平台原生圖示的穩定語意鍵；不把 Android ImageVector 放進 data 層。 */
enum class TreeHoleIndustryIconKey {
    TECHNOLOGY,
    FINANCE,
    FOOD_BEVERAGE,
    RETAIL,
    EDUCATION,
    HEALTHCARE,
    MANUFACTURING,
    MEDIA,
    SERVICE,
    CIVIL_SERVICE,
    CONSTRUCTION,
    LOGISTICS,
    HOSPITALITY,
    BEAUTY,
    DESIGN,
    LEGAL,
    REAL_ESTATE,
    MARKETING_PR,
    INSURANCE,
    AGRICULTURE,
    AVIATION,
    SOCIAL_WORK,
    FREELANCE,
    OTHER,
}

object TreeHoleIndustryCatalog {
    val entries: List<TreeHoleIndustryDefinition> = listOf(
        TreeHoleIndustryDefinition("科技業", R.string.treehole_industry_technology, TreeHoleIndustryIconKey.TECHNOLOGY),
        TreeHoleIndustryDefinition("金融業", R.string.treehole_industry_finance, TreeHoleIndustryIconKey.FINANCE),
        TreeHoleIndustryDefinition("餐飲業", R.string.treehole_industry_food_beverage, TreeHoleIndustryIconKey.FOOD_BEVERAGE),
        TreeHoleIndustryDefinition("零售業", R.string.treehole_industry_retail, TreeHoleIndustryIconKey.RETAIL),
        TreeHoleIndustryDefinition("教育業", R.string.treehole_industry_education, TreeHoleIndustryIconKey.EDUCATION),
        TreeHoleIndustryDefinition("醫療業", R.string.treehole_industry_healthcare, TreeHoleIndustryIconKey.HEALTHCARE),
        TreeHoleIndustryDefinition("製造業", R.string.treehole_industry_manufacturing, TreeHoleIndustryIconKey.MANUFACTURING),
        TreeHoleIndustryDefinition("媒體業", R.string.treehole_industry_media, TreeHoleIndustryIconKey.MEDIA),
        TreeHoleIndustryDefinition("服務業", R.string.treehole_industry_service, TreeHoleIndustryIconKey.SERVICE),
        TreeHoleIndustryDefinition("公職", R.string.treehole_industry_civil_service, TreeHoleIndustryIconKey.CIVIL_SERVICE),
        TreeHoleIndustryDefinition("營建業", R.string.treehole_industry_construction, TreeHoleIndustryIconKey.CONSTRUCTION),
        TreeHoleIndustryDefinition("物流運輸", R.string.treehole_industry_logistics, TreeHoleIndustryIconKey.LOGISTICS),
        TreeHoleIndustryDefinition("旅宿觀光", R.string.treehole_industry_hospitality, TreeHoleIndustryIconKey.HOSPITALITY),
        TreeHoleIndustryDefinition("美容美髮", R.string.treehole_industry_beauty, TreeHoleIndustryIconKey.BEAUTY),
        TreeHoleIndustryDefinition("設計創意", R.string.treehole_industry_design, TreeHoleIndustryIconKey.DESIGN),
        TreeHoleIndustryDefinition("法律", R.string.treehole_industry_legal, TreeHoleIndustryIconKey.LEGAL),
        TreeHoleIndustryDefinition("不動產", R.string.treehole_industry_real_estate, TreeHoleIndustryIconKey.REAL_ESTATE),
        TreeHoleIndustryDefinition("行銷公關", R.string.treehole_industry_marketing_pr, TreeHoleIndustryIconKey.MARKETING_PR),
        TreeHoleIndustryDefinition("保險", R.string.treehole_industry_insurance, TreeHoleIndustryIconKey.INSURANCE),
        TreeHoleIndustryDefinition("農林漁牧", R.string.treehole_industry_agriculture, TreeHoleIndustryIconKey.AGRICULTURE),
        TreeHoleIndustryDefinition("航空", R.string.treehole_industry_aviation, TreeHoleIndustryIconKey.AVIATION),
        TreeHoleIndustryDefinition("社福社工", R.string.treehole_industry_social_work, TreeHoleIndustryIconKey.SOCIAL_WORK),
        TreeHoleIndustryDefinition("自由接案", R.string.treehole_industry_freelance, TreeHoleIndustryIconKey.FREELANCE),
        TreeHoleIndustryDefinition("其他", R.string.treehole_industry_other, TreeHoleIndustryIconKey.OTHER),
    )

    /** Canonical API/storage values in the same append-only order as iOS. */
    val values: List<String> = entries.map(TreeHoleIndustryDefinition::canonicalValue)

    private val byCanonicalValue = entries.associateBy(TreeHoleIndustryDefinition::canonicalValue)

    fun find(canonicalValue: String?): TreeHoleIndustryDefinition? =
        canonicalValue?.let(byCanonicalValue::get)

    /** Returns null for a legacy/unknown server value so UI can display that value verbatim. */
    fun stringResFor(canonicalValue: String?): Int? = find(canonicalValue)?.labelRes
}

package com.neilyin.nnowork.feature.manual

import androidx.annotation.StringRes
import com.neilyin.nnowork.R

/** 手冊每個區塊的小示意圖（對應 iOS ManualSectionData.demo）。 */
enum class ManualDemo { StampCard, Salary, TreeHole }

/** 區塊的標示色：對應語意 token，畫面端再轉成 NnoworkTheme.colors 的顏色。 */
enum class ManualTone { Warning, Info, Success, Special, Teal, Highlight, Accent, Muted }

enum class ManualIcon { StampCard, Plan, Salary, History, TreeHole, Coin, Account, Settings }

data class ManualSection(
    val id: String,
    val icon: ManualIcon,
    val tone: ManualTone,
    @StringRes val title: Int,
    @StringRes val bullets: List<Int>,
    val demo: ManualDemo? = null,
)

/** iOS UserManualView 的 8 個區塊（順序、條目數、示意圖位置皆相同）。 */
val ManualSections: List<ManualSection> = listOf(
    ManualSection(
        "stamp_card", ManualIcon.StampCard, ManualTone.Warning, R.string.manual_s1_title,
        listOf(R.string.manual_s1_b1, R.string.manual_s1_b2, R.string.manual_s1_b3, R.string.manual_s1_b4),
        ManualDemo.StampCard,
    ),
    ManualSection(
        "plan", ManualIcon.Plan, ManualTone.Info, R.string.manual_s2_title,
        listOf(R.string.manual_s2_b1, R.string.manual_s2_b2, R.string.manual_s2_b3),
    ),
    ManualSection(
        "salary", ManualIcon.Salary, ManualTone.Success, R.string.manual_s3_title,
        listOf(
            R.string.manual_s3_b1, R.string.manual_s3_b2, R.string.manual_s3_b3,
            R.string.manual_s3_b4, R.string.manual_s3_b5, R.string.manual_s3_b6,
        ),
        ManualDemo.Salary,
    ),
    ManualSection(
        "history", ManualIcon.History, ManualTone.Highlight, R.string.manual_s4_title,
        listOf(R.string.manual_s4_b1, R.string.manual_s4_b2),
    ),
    ManualSection(
        "treehole", ManualIcon.TreeHole, ManualTone.Teal, R.string.manual_s5_title,
        listOf(
            R.string.manual_s5_b1, R.string.manual_s5_b2, R.string.manual_s5_b3,
            R.string.manual_s5_b4, R.string.manual_s5_b5, R.string.manual_s5_b6,
            R.string.manual_s5_b7, R.string.manual_s5_b8, R.string.manual_s5_b9,
        ),
        ManualDemo.TreeHole,
    ),
    ManualSection(
        "coins", ManualIcon.Coin, ManualTone.Special, R.string.manual_s6_title,
        listOf(R.string.manual_s6_b1, R.string.manual_s6_b2, R.string.manual_s6_b3, R.string.manual_s6_b4),
    ),
    ManualSection(
        "account", ManualIcon.Account, ManualTone.Accent, R.string.manual_s7_title,
        listOf(
            R.string.manual_s7_b1, R.string.manual_s7_b2, R.string.manual_s7_b3,
            R.string.manual_s7_b4, R.string.manual_s7_b5,
        ),
    ),
    ManualSection(
        "settings", ManualIcon.Settings, ManualTone.Muted, R.string.manual_s8_title,
        listOf(R.string.manual_s8_b1, R.string.manual_s8_b2, R.string.manual_s8_b3),
    ),
)

/** 展開狀態的純函式：點一下切換該區塊（iOS 可多個同時展開）。 */
fun toggleExpanded(expanded: Set<String>, id: String): Set<String> =
    if (id in expanded) expanded - id else expanded + id

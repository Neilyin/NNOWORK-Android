package com.neilyin.nnowork.data.treehole

data class AnswerBadge(
    val level: Int,
    val minimumExp: Int,
    val name: String,
)

/** 解答者獎牌門檻的唯一資料層來源；後端只提供 EXP 數字。 */
object AnswerBadgePolicy {
    val badges: List<AnswerBadge> = listOf(
        AnswerBadge(level = 1, minimumExp = 0, name = "樹洞新人"),
        AnswerBadge(level = 2, minimumExp = 100, name = "熱心路人"),
        AnswerBadge(level = 3, minimumExp = 500, name = "樹洞前輩"),
        AnswerBadge(level = 4, minimumExp = 2_000, name = "職場軍師"),
        AnswerBadge(level = 5, minimumExp = 5_000, name = "解惑大師"),
    )

    fun badgeFor(exp: Int): AnswerBadge = badges.last { exp.coerceAtLeast(0) >= it.minimumExp }

    /** Feed 卡片與留言從第 2 級（100 EXP）才顯示。 */
    fun isVisibleOnCommunity(exp: Int): Boolean = exp >= 100

    /** 個人頁從第 1 級顯示進度；最高級回傳 null。 */
    fun expUntilNextBadge(exp: Int): Int? {
        val normalized = exp.coerceAtLeast(0)
        val next = badges.firstOrNull { normalized < it.minimumExp } ?: return null
        return next.minimumExp - normalized
    }
}

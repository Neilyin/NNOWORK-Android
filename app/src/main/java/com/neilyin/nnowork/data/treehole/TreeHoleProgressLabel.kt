package com.neilyin.nnowork.data.treehole

import java.util.Locale

/**
 * 貼文周目前綴（android開發.md §20.1，對照 iOS `TreeHoleProgressLabel`）。純文字工具，
 * UI 端接上即可：job_index null 或 1 不顯示；繁／簡中用國字「二周目」、日文「2周目」、英文「Run 2」。
 */
object TreeHoleProgressLabel {
    fun runPrefix(jobIndex: Int?, locale: Locale = Locale.getDefault()): String? {
        if (jobIndex == null || jobIndex < 2) return null
        return when (locale.language) {
            "zh" -> "${chineseNumeral(jobIndex)}周目"
            "ja" -> "${jobIndex}周目"
            else -> "Run $jobIndex"
        }
    }

    /** 在既有標籤前加周目；以「· 」開頭（接在行業名稱後）的標籤把周目插在點的後面。 */
    fun applyingRunPrefix(base: String, jobIndex: Int?, locale: Locale = Locale.getDefault()): String {
        val run = runPrefix(jobIndex, locale) ?: return base
        return if (base.startsWith("· ")) "· $run $base" else "$run · $base"
    }

    /** 1…9999 轉國字（二、十、十一、二十、一百零一）；超出範圍退回阿拉伯數字。 */
    fun chineseNumeral(n: Int): String {
        if (n <= 0 || n >= 10_000) return n.toString()
        val digits = listOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
        val units = listOf("", "十", "百", "千")
        val values = n.toString().map { it - '0' }
        val result = StringBuilder()
        var zeroPending = false
        values.forEachIndexed { index, digit ->
            if (digit == 0) {
                zeroPending = result.isNotEmpty()
                return@forEachIndexed
            }
            if (zeroPending) {
                result.append(digits[0])
                zeroPending = false
            }
            result.append(digits[digit]).append(units[values.size - 1 - index])
        }
        val text = result.toString()
        return if (text.startsWith("一十")) text.substring(1) else text
    }
}

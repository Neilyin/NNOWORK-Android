package com.neilyin.nnowork.feature.treehole

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.core.os.ConfigurationCompat
import com.neilyin.nnowork.R
import com.neilyin.nnowork.data.treehole.TreeHolePost
import com.neilyin.nnowork.data.treehole.TreeHoleProgressLabel
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import java.util.Locale

/** iOS `TreeHoleProgressLabel.Style`：同一份進度，三個地方三種寫法。 */
enum class TreeHoleProgressStyle(@StringRes val templateRes: Int) {
    /** 「第 N 張 · 第 M 個印章」：動態牆、某人的貼文列表、貼文詳情。 */
    COMPACT(R.string.treehole_progress_compact),
    /** 「第 N 張卡 · 第 M 個印章」：卡片堆疊（今日心聲）。 */
    CARD(R.string.treehole_progress_card),
    /** 「· 第 N 張卡 · 第 M 個印章」：分享前預覽，接在行業名稱後面。 */
    SHEET(R.string.treehole_progress_sheet),
}

/**
 * 集點進度標籤（§20.1）：周目 ≥ 2 時加「二周目」前綴；以「· 」開頭的寫法把周目插在點的後面。
 * 提問、沒有卡片資訊的貼文不顯示。
 */
object TreeHolePostProgressText {
    fun of(
        post: TreeHolePost,
        locale: Locale,
        base: (round: Int, stamps: Int) -> String,
    ): String? {
        if (post.isQuestion) return null
        return of(post.roundNumber, post.stampCount, post.displayRunNumber, locale, base)
    }

    fun of(
        roundNumber: Int?,
        stampCount: Int?,
        runNumber: Int?,
        locale: Locale,
        base: (round: Int, stamps: Int) -> String,
    ): String? {
        val round = roundNumber ?: return null
        val stamps = stampCount?.takeIf { it > 0 } ?: return null
        return TreeHoleProgressLabel.applyingRunPrefix(base(round, stamps), runNumber, locale)
    }
}

/** 依目前 App 語系產生進度文字；沒有可顯示的進度回 null。 */
@Composable
fun treeHoleProgressText(
    roundNumber: Int?,
    stampCount: Int?,
    runNumber: Int?,
    style: TreeHoleProgressStyle,
): String? {
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val template = stringResource(style.templateRes)
    return TreeHolePostProgressText.of(roundNumber, stampCount, runNumber, locale) { round, stamps ->
        String.format(locale, template, round, stamps)
    }
}

@Composable
fun TreeHolePostProgressLabel(
    post: TreeHolePost,
    modifier: Modifier = Modifier,
    style: TreeHoleProgressStyle = TreeHoleProgressStyle.COMPACT,
    fontSize: TextUnit = NnoworkTheme.type.caption,
) {
    if (post.isQuestion) return
    val text = treeHoleProgressText(post.roundNumber, post.stampCount, post.displayRunNumber, style) ?: return
    TreeHoleProgressRow(text, modifier, fontSize)
}

@Composable
fun TreeHoleProgressRow(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = NnoworkTheme.type.caption,
    color: Color = NnoworkTheme.colors.secondaryText,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs),
    ) {
        Icon(
            Icons.Filled.GridView,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(NnoworkSize.compactIcon),
        )
        Text(
            text,
            style = pixelTextStyle(fontSize, color = color),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

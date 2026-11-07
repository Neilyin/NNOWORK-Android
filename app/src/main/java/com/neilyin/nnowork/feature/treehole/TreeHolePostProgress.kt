package com.neilyin.nnowork.feature.treehole

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.os.ConfigurationCompat
import com.neilyin.nnowork.R
import com.neilyin.nnowork.data.treehole.TreeHolePost
import com.neilyin.nnowork.data.treehole.TreeHoleProgressLabel
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import java.util.Locale

/**
 * 貼文卡片的集點進度標籤（iOS `TreeHoleProgressLabel.text(style: .compact)`，§20.1）：
 * 「第 N 張 · 第 M 個印章」，周目 ≥ 2 時前面加「二周目 · 」。提問、沒有卡片資訊的貼文不顯示。
 */
object TreeHolePostProgressText {
    fun of(
        post: TreeHolePost,
        locale: Locale,
        base: (round: Int, stamps: Int) -> String,
    ): String? {
        if (post.isQuestion) return null
        val round = post.roundNumber ?: return null
        val stamps = post.stampCount?.takeIf { it > 0 } ?: return null
        return TreeHoleProgressLabel.applyingRunPrefix(base(round, stamps), post.displayRunNumber, locale)
    }
}

@Composable
fun TreeHolePostProgressLabel(post: TreeHolePost, modifier: Modifier = Modifier) {
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val template = stringResource(R.string.treehole_progress_compact)
    val text = TreeHolePostProgressText.of(post, locale) { round, stamps ->
        String.format(locale, template, round, stamps)
    } ?: return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs),
    ) {
        Icon(
            Icons.Filled.GridView,
            contentDescription = null,
            tint = NnoworkTheme.colors.secondaryText,
            modifier = Modifier.size(NnoworkSize.compactIcon),
        )
        Text(
            text,
            style = pixelTextStyle(NnoworkTheme.type.caption, color = NnoworkTheme.colors.secondaryText),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

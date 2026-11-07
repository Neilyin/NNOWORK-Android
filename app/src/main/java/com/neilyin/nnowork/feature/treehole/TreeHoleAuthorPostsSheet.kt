package com.neilyin.nnowork.feature.treehole

import com.neilyin.nnowork.ui.theme.NnoworkRadius
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import com.neilyin.nnowork.ui.theme.NnoworkSheetDefaults
import androidx.compose.foundation.background
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkLine
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neilyin.nnowork.data.treehole.TreeHolePost
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle

/** authorKey 是不可逆的公開識別碼；nickname 只供顯示與舊資料 fallback。 */
@Immutable
data class TreeHoleAuthorRef(
    val nickname: String,
    val industry: String?,
    val authorKey: String?,
)

@Immutable
data class TreeHoleAuthorPostsStrings(
    val title: String = "這個人的公開貼文",
    val close: String = "關閉作者貼文",
    val publicPosts: String = "公開分享",
    val reactions: String = "收到共鳴",
    val questions: String = "職場解惑",
    val loading: String = "正在載入公開貼文…",
    val empty: String = "還沒有公開的分享",
    val loadFailed: String = "無法載入這個人的貼文。",
    val retry: String = "重新載入",
    val openPost: String = "查看貼文與留言",
    val questionTag: String = "提問",
    val replyCountTemplate: String = "%d 人回答",
    val waitingForAnswer: String = "等你回答",
    val like: String = "按讚",
    val cheer: String = "加油",
    val cry: String = "難過",
    val fallbackIndustry: String = "職場倖存者",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeHoleAuthorPostsSheet(
    author: TreeHoleAuthorRef,
    posts: List<TreeHolePost>,
    loading: Boolean,
    errorMessage: String?,
    industryLabel: String,
    industryIcon: ImageVector,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onOpenPost: (TreeHolePost) -> Unit,
    onReact: (String, TreeHoleReactionType) -> Unit,
    modifier: Modifier = Modifier,
    strings: TreeHoleAuthorPostsStrings = TreeHoleAuthorPostsStrings(),
    badgeStrings: TreeHoleAnswerBadgeStrings = TreeHoleAnswerBadgeStrings(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val totalReactions = posts.sumOf { it.likesCount + it.cheerCount + it.cryCount }
    val questionCount = posts.count(TreeHolePost::isQuestion)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        shape = NnoworkSheetDefaults.shape,
        containerColor = NnoworkSheetDefaults.containerColor,
        scrimColor = NnoworkSheetDefaults.scrimColor,
        dragHandle = { NnoworkSheetDefaults.DragHandle() },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            contentPadding = PaddingValues(start = NnoworkSpacing.md, end = NnoworkSpacing.md, bottom = NnoworkSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        strings.title,
                        style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.weight(1f),
                    )
                    PixelButton(
                        onClick = onDismiss,
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(NnoworkSize.minTouchTarget),
                        contentPadding = PaddingValues(NnoworkSpacing.xs),
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = strings.close,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item(key = "profile") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pixelCard(backgroundColor = MaterialTheme.colorScheme.surfaceVariant)
                        .padding(NnoworkSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug),
                ) {
                    TreeHoleInitialsAvatar(nickname = author.nickname, size = 64.dp)
                    Text(
                        author.nickname,
                        style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            industryIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            industryLabel.ifBlank { strings.fallbackIndustry },
                            style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        AuthorStat(posts.size, strings.publicPosts)
                        AuthorStat(totalReactions, strings.reactions)
                        AuthorStat(questionCount, strings.questions)
                    }
                }
            }

            when {
                loading -> item(key = "loading") {
                    AuthorMessageCard(strings.loading, Icons.Filled.ChatBubble)
                }
                errorMessage != null -> item(key = "error") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(NnoworkRadius.field)).background(NnoworkTheme.colors.treeHole.errorSoft)
                            .border(NnoworkLine.hairline, NnoworkTheme.colors.treeHole.errorBorder, RoundedCornerShape(NnoworkRadius.field))
                            .padding(NnoworkSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
                    ) {
                        Text(
                            errorMessage,
                            style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.treeHole.errorText),
                        )
                        PixelButton(
                            onClick = onRetry,
                            backgroundColor = NnoworkTheme.colors.highlight,
                            modifier = Modifier.heightIn(min = NnoworkSize.minTouchTarget),
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = NnoworkTheme.colors.primaryText)
                            Spacer(Modifier.size(NnoworkSpacing.tight))
                            Text(strings.retry, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                        }
                    }
                }
                posts.isEmpty() -> item(key = "empty") {
                    AuthorMessageCard(strings.empty, Icons.Filled.QuestionAnswer)
                }
                else -> items(posts, key = TreeHolePost::id) { post ->
                    AuthorPostCard(
                        post = post,
                        onOpen = { onOpenPost(post) },
                        onReact = { type -> onReact(post.id, type) },
                        strings = strings,
                        badgeStrings = badgeStrings,
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthorStat(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value.coerceAtLeast(0).toString(),
            style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, MaterialTheme.colorScheme.onSurfaceVariant),
        )
        Text(label, style = pixelTextStyle(NnoworkTheme.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant))
    }
}

@Composable
private fun AuthorMessageCard(message: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard(backgroundColor = MaterialTheme.colorScheme.surfaceVariant)
            .padding(NnoworkSpacing.roomy),
        horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(message, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, MaterialTheme.colorScheme.onSurfaceVariant))
    }
}

@Composable
private fun AuthorPostCard(
    post: TreeHolePost,
    onOpen: () -> Unit,
    onReact: (TreeHoleReactionType) -> Unit,
    strings: TreeHoleAuthorPostsStrings,
    badgeStrings: TreeHoleAnswerBadgeStrings,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard(backgroundColor = MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Button, onClickLabel = strings.openPost, onClick = onOpen)
            .padding(NnoworkSpacing.cozy),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (post.isQuestion) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(NnoworkRadius.field)).background(NnoworkTheme.colors.treeHole.questionAccent)
                        .border(NnoworkLine.hairline, NnoworkTheme.colors.border, RoundedCornerShape(NnoworkRadius.field))
                        .padding(horizontal = NnoworkSpacing.tight, vertical = 2.dp),
                ) {
                    Text(strings.questionTag, style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            }
            TreeHoleAnswerBadgeMark(exp = post.authorExp, strings = badgeStrings)
        }
        Text(
            post.content,
            style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, MaterialTheme.colorScheme.onSurfaceVariant),
        )
        if (post.isQuestion) {
            Text(
                if (post.commentCount > 0) strings.replyCountTemplate.withCount(post.commentCount)
                else strings.waitingForAnswer,
                style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.treeHole.questionAccentDark),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AuthorReactionButton(Icons.Filled.ThumbUp, strings.like, post.likesCount) {
                onReact(TreeHoleReactionType.LIKE)
            }
            AuthorReactionButton(Icons.Filled.Campaign, strings.cheer, post.cheerCount) {
                onReact(TreeHoleReactionType.CHEER)
            }
            AuthorReactionButton(Icons.Filled.SentimentDissatisfied, strings.cry, post.cryCount) {
                onReact(TreeHoleReactionType.CRY)
            }
        }
    }
}

@Composable
private fun AuthorReactionButton(
    icon: ImageVector,
    label: String,
    count: Int,
    onClick: () -> Unit,
) {
    PixelButton(
        onClick = onClick,
        backgroundColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.heightIn(min = NnoworkSize.minTouchTarget),
        contentPadding = PaddingValues(horizontal = NnoworkSpacing.xs, vertical = 7.dp),
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(NnoworkSpacing.xxs))
        Text(count.coerceAtLeast(0).toString(), style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, MaterialTheme.colorScheme.onBackground))
    }
}

private fun String.withCount(count: Int): String = replace("%d", count.coerceAtLeast(0).toString())

package com.neilyin.nnowork.feature.treehole

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import com.neilyin.nnowork.data.treehole.TreeHolePost
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
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
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onBackground) },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        strings.title,
                        style = pixelTextStyle(20.sp, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.weight(1f),
                    )
                    PixelButton(
                        onClick = onDismiss,
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(8.dp),
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
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TreeHoleInitialsAvatar(nickname = author.nickname, size = 64.dp)
                    Text(
                        author.nickname,
                        style = pixelTextStyle(18.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurfaceVariant),
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
                            style = pixelTextStyle(12.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurfaceVariant),
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
                            .background(Color(0xFFFFE7E7))
                            .border(2.dp, Color(0xFF9A1F1F), RectangleShape)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            errorMessage,
                            style = pixelTextStyle(13.sp, FontWeight.Bold, Color(0xFF711515)),
                        )
                        PixelButton(
                            onClick = onRetry,
                            backgroundColor = PixelColors.yellow,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = PixelColors.black)
                            Spacer(Modifier.size(6.dp))
                            Text(strings.retry, style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.black))
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
            style = pixelTextStyle(18.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurfaceVariant),
        )
        Text(label, style = pixelTextStyle(10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
    }
}

@Composable
private fun AuthorMessageCard(message: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard(backgroundColor = MaterialTheme.colorScheme.surfaceVariant)
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(message, style = pixelTextStyle(13.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurfaceVariant))
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
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (post.isQuestion) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF6BB8FA))
                        .border(1.dp, PixelColors.black, RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(strings.questionTag, style = pixelTextStyle(10.sp, FontWeight.Bold, PixelColors.black))
                }
            }
            TreeHoleAnswerBadgeMark(exp = post.authorExp, strings = badgeStrings)
        }
        Text(
            post.content,
            style = pixelTextStyle(15.sp, FontWeight.Medium, MaterialTheme.colorScheme.onSurfaceVariant),
        )
        if (post.isQuestion) {
            Text(
                if (post.commentCount > 0) strings.replyCountTemplate.withCount(post.commentCount)
                else strings.waitingForAnswer,
                style = pixelTextStyle(11.sp, FontWeight.Bold, Color(0xFF0B5FA5)),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
        modifier = Modifier.heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(4.dp))
        Text(count.coerceAtLeast(0).toString(), style = pixelTextStyle(11.sp, FontWeight.Bold, MaterialTheme.colorScheme.onBackground))
    }
}

private fun String.withCount(count: Int): String = replace("%d", count.coerceAtLeast(0).toString())

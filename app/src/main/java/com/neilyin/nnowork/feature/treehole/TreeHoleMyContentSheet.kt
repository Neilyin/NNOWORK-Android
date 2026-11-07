package com.neilyin.nnowork.feature.treehole

import androidx.compose.foundation.background
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkLine
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neilyin.nnowork.data.treehole.MyTreeHoleComment
import com.neilyin.nnowork.data.treehole.TreeHolePost
import com.neilyin.nnowork.data.treehole.TreeHoleProfile
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TreeHoleMySection {
    POSTS,
    COMMENTS,
}

enum class TreeHoleMyPostStatusFilter {
    ALL,
    PENDING,
    APPROVED,
}

/**
 * Call-site owned presentation state for the optional industry migration prompt.
 * Passing `null` to [TreeHoleMyContentSheet] hides the prompt entirely.
 */
@Immutable
data class TreeHoleIndustrySyncUi(
    val title: String,
    val message: String,
    val actionLabel: String,
    val loading: Boolean = false,
    val enabled: Boolean = true,
)

/** All visible copy in [TreeHoleMyContentSheet] is caller-overridable for localization. */
@Immutable
data class TreeHoleMyContentStrings(
    val title: String = "我的樹洞",
    val close: String = "關閉我的樹洞",
    val editProfile: String = "編輯身份",
    val anonymousNickname: String = "匿名社畜",
    val profileFallback: String = "職場倖存者",
    val avatarSuffix: String = "的頭像",
    val companyTemplate: String = "公司：%s",
    val postsTabTemplate: String = "我的貼文（%d）",
    val commentsTabTemplate: String = "我的留言（%d）",
    val selectedState: String = "已選取",
    val notSelectedState: String = "未選取",
    val allFilterTemplate: String = "全部 %d",
    val pendingFilterTemplate: String = "審核中 %d",
    val approvedFilterTemplate: String = "已通過 %d",
    val approvedStatus: String = "已通過",
    val pendingStatus: String = "審核中",
    val rejectedStatus: String = "未通過",
    val questionTag: String = "提問",
    val deletePost: String = "刪除貼文",
    val deleteComment: String = "刪除留言",
    val deletePostTitle: String = "確定刪除這篇貼文？",
    val deletePostBody: String = "刪除後無法復原，相關內容也不會再顯示。",
    val deleteCommentTitle: String = "確定刪除這則留言？",
    val deleteCommentBody: String = "刪除後無法復原。",
    val confirmDelete: String = "確定刪除",
    val cancel: String = "取消",
    val emptyPosts: String = "你還沒有符合此狀態的貼文。",
    val emptyComments: String = "你還沒有留過言。",
    val repliedPostTemplate: String = "回覆貼文：%s",
    val loadingPosts: String = "正在載入我的貼文",
    val loadingComments: String = "正在載入我的留言",
    val industrySyncLoading: String = "同步中…",
    val justNow: String = "剛剛",
    val minutesAgoTemplate: String = "%d 分鐘前",
    val hoursAgoTemplate: String = "%d 小時前",
    val daysAgoTemplate: String = "%d 天前",
    val unknownTime: String = "時間未知",
    val createdAtDescriptionTemplate: String = "發佈時間：%s",
    val absoluteDatePattern: String = "yyyy/MM/dd HH:mm",
    val badge: TreeHoleAnswerBadgeStrings = TreeHoleAnswerBadgeStrings(),
)

/**
 * Large, scrollable sheet for the user's Tree Hole profile, posts and comments.
 *
 * Network and persistence operations remain call-site owned. Deletion callbacks are only invoked
 * after the built-in destructive-action confirmation dialog is accepted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeHoleMyContentSheet(
    profile: TreeHoleProfile,
    posts: List<TreeHolePost>,
    comments: List<MyTreeHoleComment>,
    answerExp: Int,
    helpfulAnswerCount: Int,
    loadingPosts: Boolean,
    loadingComments: Boolean,
    onEditProfile: () -> Unit,
    onDeletePost: (TreeHolePost) -> Unit,
    onDeleteComment: (MyTreeHoleComment) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    industryIcon: ImageVector = Icons.Filled.Work,
    industrySync: TreeHoleIndustrySyncUi? = null,
    onIndustrySync: (() -> Unit)? = null,
    strings: TreeHoleMyContentStrings = TreeHoleMyContentStrings(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedSection by rememberSaveable { mutableStateOf(TreeHoleMySection.POSTS) }
    var selectedPostFilter by rememberSaveable { mutableStateOf(TreeHoleMyPostStatusFilter.ALL) }
    var deleteTarget by remember { mutableStateOf<TreeHoleDeleteTarget?>(null) }

    val pendingPostCount = posts.count { it.statusCategory() == TreeHoleContentStatus.PENDING }
    val approvedPostCount = posts.count { it.statusCategory() == TreeHoleContentStatus.APPROVED }
    val visiblePosts = when (selectedPostFilter) {
        TreeHoleMyPostStatusFilter.ALL -> posts
        TreeHoleMyPostStatusFilter.PENDING -> posts.filter {
            it.statusCategory() == TreeHoleContentStatus.PENDING
        }
        TreeHoleMyPostStatusFilter.APPROVED -> posts.filter {
            it.statusCategory() == TreeHoleContentStatus.APPROVED
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(MY_SHEET_HEIGHT_FRACTION),
            contentPadding = PaddingValues(start = NnoworkSpacing.md, end = NnoworkSpacing.md, bottom = NnoworkSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
        ) {
            item(key = "header") {
                SheetHeader(
                    title = strings.title,
                    closeDescription = strings.close,
                    onDismiss = onDismiss,
                )
            }

            item(key = "profile") {
                TreeHoleProfileSummary(
                    profile = profile,
                    industryIcon = industryIcon,
                    onEditProfile = onEditProfile,
                    strings = strings,
                )
            }

            item(key = "answer-badge") {
                TreeHoleAnswerBadgeProgressCard(
                    exp = answerExp,
                    helpfulCount = helpfulAnswerCount,
                    strings = strings.badge,
                )
            }

            industrySync?.let { syncUi ->
                item(key = "industry-sync") {
                    TreeHoleIndustrySyncCard(
                        state = syncUi,
                        onSync = onIndustrySync,
                        loadingLabel = strings.industrySyncLoading,
                    )
                }
            }

            item(key = "section-tabs") {
                MySectionTabs(
                    selected = selectedSection,
                    postCount = posts.size,
                    commentCount = comments.size,
                    onSelected = { selectedSection = it },
                    strings = strings,
                )
            }

            if (selectedSection == TreeHoleMySection.POSTS) {
                item(key = "post-filters") {
                    MyPostStatusFilters(
                        selected = selectedPostFilter,
                        allCount = posts.size,
                        pendingCount = pendingPostCount,
                        approvedCount = approvedPostCount,
                        onSelected = { selectedPostFilter = it },
                        strings = strings,
                    )
                }

                if (loadingPosts) {
                    item(key = "posts-loading") {
                        TreeHoleMyLoadingState(strings.loadingPosts)
                    }
                }

                if (!loadingPosts && visiblePosts.isEmpty()) {
                    item(key = "posts-empty") {
                        TreeHoleMyEmptyState(strings.emptyPosts)
                    }
                }

                itemsIndexed(
                    items = visiblePosts,
                    key = { index, post -> "post:${post.id.ifBlank { index.toString() }}" },
                ) { _, post ->
                    TreeHoleMyPostCard(
                        post = post,
                        onDelete = { deleteTarget = TreeHoleDeleteTarget.Post(post) },
                        strings = strings,
                    )
                }
            } else {
                if (loadingComments) {
                    item(key = "comments-loading") {
                        TreeHoleMyLoadingState(strings.loadingComments)
                    }
                }

                if (!loadingComments && comments.isEmpty()) {
                    item(key = "comments-empty") {
                        TreeHoleMyEmptyState(strings.emptyComments)
                    }
                }

                itemsIndexed(
                    items = comments,
                    key = { index, comment -> "comment:${comment.id.ifBlank { index.toString() }}" },
                ) { _, comment ->
                    TreeHoleMyCommentCard(
                        comment = comment,
                        onDelete = { deleteTarget = TreeHoleDeleteTarget.Comment(comment) },
                        strings = strings,
                    )
                }
            }
        }
    }

    deleteTarget?.let { target ->
        TreeHoleDeleteConfirmationDialog(
            target = target,
            strings = strings,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                when (target) {
                    is TreeHoleDeleteTarget.Post -> onDeletePost(target.post)
                    is TreeHoleDeleteTarget.Comment -> onDeleteComment(target.comment)
                }
            },
        )
    }
}

@Composable
private fun SheetHeader(
    title: String,
    closeDescription: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.weight(1f),
        )
        TreeHoleMyIconAction(
            icon = Icons.Filled.Close,
            contentDescription = closeDescription,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onDismiss,
        )
    }
}

@Composable
private fun TreeHoleProfileSummary(
    profile: TreeHoleProfile,
    industryIcon: ImageVector,
    onEditProfile: () -> Unit,
    strings: TreeHoleMyContentStrings,
) {
    val nickname = profile.nickname.ifBlank { strings.anonymousNickname }
    val identityLine = listOf(profile.industry, profile.role)
        .filter(String::isNotBlank)
        .joinToString(" · ")
        .ifBlank { strings.profileFallback }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(NnoworkSpacing.cozy),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
        ) {
            TreeHoleInitialsAvatar(
                nickname = nickname,
                size = NnoworkSize.minTouchTarget,
                contentDescription = "$nickname${strings.avatarSuffix}",
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs),
            ) {
                Text(
                    text = nickname,
                    style = pixelTextStyle(
                        NnoworkTheme.type.bodyLarge,
                        FontWeight.Bold,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = industryIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = identityLine,
                        style = pixelTextStyle(
                            NnoworkTheme.type.small,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (profile.showCompany && profile.companyName.isNotBlank()) {
                    Text(
                        text = strings.companyTemplate.withText(profile.companyName),
                        style = pixelTextStyle(
                            NnoworkTheme.type.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        PixelButton(
            onClick = onEditProfile,
            backgroundColor = NnoworkTheme.colors.success,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MY_MIN_TOUCH_TARGET)
                .semantics { role = Role.Button },
            contentPadding = PaddingValues(horizontal = NnoworkSpacing.cozy, vertical = NnoworkSpacing.snug),
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                tint = NnoworkTheme.colors.primaryText,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                strings.editProfile,
                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
            )
        }
    }
}

@Composable
private fun TreeHoleIndustrySyncCard(
    state: TreeHoleIndustrySyncUi,
    onSync: (() -> Unit)?,
    loadingLabel: String,
) {
    val canSync = state.enabled && !state.loading && onSync != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard(
                backgroundColor = NnoworkTheme.colors.highlight.copy(alpha = 0.18f),
                borderColor = NnoworkTheme.colors.warning,
            )
            .padding(NnoworkSpacing.cozy),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Sync,
                contentDescription = null,
                tint = NnoworkTheme.colors.warning,
                modifier = Modifier.size(22.dp),
            )
            Text(
                state.title,
                style = pixelTextStyle(
                    NnoworkTheme.type.body,
                    FontWeight.Bold,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
        Text(
            state.message,
            style = pixelTextStyle(NnoworkTheme.type.small, color = MaterialTheme.colorScheme.onSurfaceVariant),
        )
        PixelButton(
            onClick = { onSync?.invoke() },
            enabled = canSync,
            backgroundColor = if (canSync) NnoworkTheme.colors.highlight else NnoworkTheme.colors.disabledBG,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MY_MIN_TOUCH_TARGET)
                .semantics {
                    role = Role.Button
                    if (state.loading) {
                        stateDescription = loadingLabel
                        liveRegion = LiveRegionMode.Polite
                    }
                },
            contentPadding = PaddingValues(horizontal = NnoworkSpacing.cozy, vertical = NnoworkSpacing.snug),
        ) {
            if (state.loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .semantics { contentDescription = loadingLabel },
                    color = NnoworkTheme.colors.primaryText,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(NnoworkSpacing.xs))
                Text(
                    loadingLabel,
                    style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
            } else {
                Text(
                    state.actionLabel,
                    style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
            }
        }
    }
}

@Composable
private fun MySectionTabs(
    selected: TreeHoleMySection,
    postCount: Int,
    commentCount: Int,
    onSelected: (TreeHoleMySection) -> Unit,
    strings: TreeHoleMyContentStrings,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
    ) {
        TreeHoleMyTab(
            label = strings.postsTabTemplate.withCount(postCount),
            selected = selected == TreeHoleMySection.POSTS,
            selectedState = strings.selectedState,
            notSelectedState = strings.notSelectedState,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(TreeHoleMySection.POSTS) },
        )
        TreeHoleMyTab(
            label = strings.commentsTabTemplate.withCount(commentCount),
            selected = selected == TreeHoleMySection.COMMENTS,
            selectedState = strings.selectedState,
            notSelectedState = strings.notSelectedState,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(TreeHoleMySection.COMMENTS) },
        )
    }
}

@Composable
private fun TreeHoleMyTab(
    label: String,
    selected: Boolean,
    selectedState: String,
    notSelectedState: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .heightIn(min = MY_MIN_TOUCH_TARGET)
            .background(if (selected) NnoworkTheme.colors.success else MaterialTheme.colorScheme.surfaceVariant)
            .border(NnoworkLine.control, MaterialTheme.colorScheme.outline, RectangleShape)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                stateDescription = if (selected) selectedState else notSelectedState
            }
            .padding(horizontal = NnoworkSpacing.xs, vertical = NnoworkSpacing.snug),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = pixelTextStyle(
                NnoworkTheme.type.small,
                FontWeight.Bold,
                if (selected) NnoworkTheme.colors.primaryText else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MyPostStatusFilters(
    selected: TreeHoleMyPostStatusFilter,
    allCount: Int,
    pendingCount: Int,
    approvedCount: Int,
    onSelected: (TreeHoleMyPostStatusFilter) -> Unit,
    strings: TreeHoleMyContentStrings,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
    ) {
        TreeHoleStatusFilterChip(
            label = strings.allFilterTemplate.withCount(allCount),
            selected = selected == TreeHoleMyPostStatusFilter.ALL,
            selectedState = strings.selectedState,
            notSelectedState = strings.notSelectedState,
            onClick = { onSelected(TreeHoleMyPostStatusFilter.ALL) },
        )
        TreeHoleStatusFilterChip(
            label = strings.pendingFilterTemplate.withCount(pendingCount),
            selected = selected == TreeHoleMyPostStatusFilter.PENDING,
            selectedState = strings.selectedState,
            notSelectedState = strings.notSelectedState,
            selectedColor = NnoworkTheme.colors.warning,
            onClick = { onSelected(TreeHoleMyPostStatusFilter.PENDING) },
        )
        TreeHoleStatusFilterChip(
            label = strings.approvedFilterTemplate.withCount(approvedCount),
            selected = selected == TreeHoleMyPostStatusFilter.APPROVED,
            selectedState = strings.selectedState,
            notSelectedState = strings.notSelectedState,
            selectedColor = NnoworkTheme.colors.success,
            onClick = { onSelected(TreeHoleMyPostStatusFilter.APPROVED) },
        )
    }
}

@Composable
private fun TreeHoleStatusFilterChip(
    label: String,
    selected: Boolean,
    selectedState: String,
    notSelectedState: String,
    selectedColor: Color = NnoworkTheme.colors.info,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = MY_MIN_TOUCH_TARGET, minHeight = MY_MIN_TOUCH_TARGET)
            .background(if (selected) selectedColor else MaterialTheme.colorScheme.surfaceVariant)
            .border(NnoworkLine.control, MaterialTheme.colorScheme.outline, RectangleShape)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                stateDescription = if (selected) selectedState else notSelectedState
            }
            .padding(horizontal = NnoworkSpacing.sm, vertical = NnoworkSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = pixelTextStyle(
                NnoworkTheme.type.small,
                FontWeight.Bold,
                if (selected) NnoworkTheme.colors.primaryText else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@Composable
private fun TreeHoleMyPostCard(
    post: TreeHolePost,
    onDelete: () -> Unit,
    strings: TreeHoleMyContentStrings,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(NnoworkSpacing.cozy),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (post.isQuestion) {
                TreeHoleMyQuestionTag(strings.questionTag)
            }
            TreeHoleMyStatusBadge(post.statusCategory(), strings)
            Spacer(Modifier.weight(1f))
            TreeHoleMyIconAction(
                icon = Icons.Filled.Delete,
                contentDescription = strings.deletePost,
                backgroundColor = NnoworkTheme.colors.danger,
                iconColor = NnoworkTheme.colors.textOnDark,
                onClick = onDelete,
            )
        }

        Text(
            text = post.content,
            style = pixelTextStyle(
                NnoworkTheme.type.body,
                FontWeight.Medium,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )

        TreeHoleCreatedAt(
            value = post.createdAt,
            strings = strings,
        )
    }
}

@Composable
private fun TreeHoleMyCommentCard(
    comment: MyTreeHoleComment,
    onDelete: () -> Unit,
    strings: TreeHoleMyContentStrings,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(NnoworkSpacing.cozy),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
        ) {
            TreeHoleMyStatusBadge(comment.status.statusCategory(), strings)
            Spacer(Modifier.weight(1f))
            TreeHoleMyIconAction(
                icon = Icons.Filled.Delete,
                contentDescription = strings.deleteComment,
                backgroundColor = NnoworkTheme.colors.danger,
                iconColor = NnoworkTheme.colors.textOnDark,
                onClick = onDelete,
            )
        }

        Text(
            text = comment.content,
            style = pixelTextStyle(
                NnoworkTheme.type.body,
                FontWeight.Bold,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )

        comment.postContent?.takeIf(String::isNotBlank)?.let { postContent ->
            Text(
                text = strings.repliedPostTemplate.withText(postContent),
                style = pixelTextStyle(NnoworkTheme.type.small, color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        TreeHoleCreatedAt(
            value = comment.createdAt,
            strings = strings,
        )
    }
}

@Composable
private fun TreeHoleCreatedAt(
    value: String,
    strings: TreeHoleMyContentStrings,
) {
    val displayValue = remember(value, strings) {
        formatTreeHoleCreatedAt(value, strings)
    }
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = strings.createdAtDescriptionTemplate.withText(displayValue)
        },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.AccessTime,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = displayValue,
            style = pixelTextStyle(
                NnoworkTheme.type.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            ),
        )
    }
}

@Composable
private fun TreeHoleMyQuestionTag(label: String) {
    Row(
        modifier = Modifier
            .background(NnoworkTheme.colors.treeHole.questionAccent)
            .border(NnoworkLine.hairline, MaterialTheme.colorScheme.outline, RectangleShape)
            .semantics(mergeDescendants = true) { contentDescription = label }
            .padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.QuestionAnswer,
            contentDescription = null,
            tint = NnoworkTheme.colors.primaryText,
            modifier = Modifier.size(13.dp),
        )
        Text(label, style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.primaryText))
    }
}

@Composable
private fun TreeHoleMyStatusBadge(
    status: TreeHoleContentStatus,
    strings: TreeHoleMyContentStrings,
) {
    val (label, color) = when (status) {
        TreeHoleContentStatus.APPROVED -> strings.approvedStatus to NnoworkTheme.colors.success
        TreeHoleContentStatus.PENDING -> strings.pendingStatus to NnoworkTheme.colors.warning
        TreeHoleContentStatus.REJECTED -> strings.rejectedStatus to NnoworkTheme.colors.danger
    }
    Box(
        modifier = Modifier
            .background(color)
            .border(NnoworkLine.hairline, MaterialTheme.colorScheme.outline, RectangleShape)
            .semantics(mergeDescendants = true) { contentDescription = label }
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(label, style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.primaryText))
    }
}

@Composable
private fun TreeHoleMyLoadingState(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                liveRegion = LiveRegionMode.Polite
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = NnoworkTheme.colors.success,
            strokeWidth = 3.dp,
        )
        Spacer(Modifier.width(NnoworkSpacing.snug))
        Text(
            label,
            style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
        )
    }
}

@Composable
private fun TreeHoleMyEmptyState(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .pixelCard()
            .padding(NnoworkSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = pixelTextStyle(NnoworkTheme.type.small, color = MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}

@Composable
private fun TreeHoleMyIconAction(
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = MY_MIN_TOUCH_TARGET, minHeight = MY_MIN_TOUCH_TARGET)
            .background(backgroundColor)
            .border(NnoworkLine.control, MaterialTheme.colorScheme.outline, RectangleShape)
            .clickable(
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun TreeHoleDeleteConfirmationDialog(
    target: TreeHoleDeleteTarget,
    strings: TreeHoleMyContentStrings,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isPost = target is TreeHoleDeleteTarget.Post
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (isPost) strings.deletePostTitle else strings.deleteCommentTitle,
                style = pixelTextStyle(
                    NnoworkTheme.type.subtitle,
                    FontWeight.Bold,
                    MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        text = {
            Text(
                text = if (isPost) strings.deletePostBody else strings.deleteCommentBody,
                style = pixelTextStyle(NnoworkTheme.type.small, color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        },
        confirmButton = {
            PixelButton(
                onClick = onConfirm,
                backgroundColor = NnoworkTheme.colors.danger,
                modifier = Modifier.heightIn(min = MY_MIN_TOUCH_TARGET),
                contentPadding = PaddingValues(horizontal = NnoworkSpacing.cozy, vertical = NnoworkSpacing.snug),
            ) {
                Text(
                    strings.confirmDelete,
                    style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.textOnDark),
                )
            }
        },
        dismissButton = {
            PixelButton(
                onClick = onDismiss,
                backgroundColor = NnoworkTheme.colors.disabledBG,
                modifier = Modifier.heightIn(min = MY_MIN_TOUCH_TARGET),
                contentPadding = PaddingValues(horizontal = NnoworkSpacing.cozy, vertical = NnoworkSpacing.snug),
            ) {
                Text(
                    strings.cancel,
                    style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
            }
        },
    )
}

/**
 * Tolerant formatter for API timestamps. It accepts ISO instants, offset/zoned timestamps,
 * database-style `yyyy-MM-dd HH:mm:ss[.fraction]+00`, and timezone-less ISO values.
 */
fun formatTreeHoleCreatedAt(
    value: String,
    strings: TreeHoleMyContentStrings = TreeHoleMyContentStrings(),
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    val instant = parseTreeHoleInstant(value, zoneId) ?: return strings.unknownTime
    val elapsedSeconds = Duration.between(instant, now).seconds

    if (elapsedSeconds in 0 until SECONDS_PER_MINUTE) return strings.justNow
    if (elapsedSeconds in SECONDS_PER_MINUTE until SECONDS_PER_HOUR) {
        return strings.minutesAgoTemplate.withCount((elapsedSeconds / SECONDS_PER_MINUTE).toInt())
    }
    if (elapsedSeconds in SECONDS_PER_HOUR until SECONDS_PER_DAY) {
        return strings.hoursAgoTemplate.withCount((elapsedSeconds / SECONDS_PER_HOUR).toInt())
    }
    if (elapsedSeconds in SECONDS_PER_DAY until DAYS_SHOWN_RELATIVELY * SECONDS_PER_DAY) {
        return strings.daysAgoTemplate.withCount((elapsedSeconds / SECONDS_PER_DAY).toInt())
    }

    return runCatching {
        DateTimeFormatter
            .ofPattern(strings.absoluteDatePattern, Locale.getDefault())
            .format(instant.atZone(zoneId))
    }.getOrElse {
        DateTimeFormatter.ISO_LOCAL_DATE.format(instant.atZone(zoneId))
    }
}

private fun parseTreeHoleInstant(value: String, zoneId: ZoneId): Instant? {
    val raw = value.trim()
    if (raw.isEmpty()) return null
    val normalized = raw
        .normalizeDateTimeSeparator()
        .normalizeShortOffset()

    val parsers: List<() -> Instant> = listOf(
        { Instant.parse(normalized) },
        { OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant() },
        { ZonedDateTime.parse(normalized, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant() },
        { LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(zoneId).toInstant() },
        { LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(zoneId).toInstant() },
    )
    parsers.forEach { parser ->
        runCatching(parser).getOrNull()?.let { return it }
    }
    return null
}

private fun String.normalizeDateTimeSeparator(): String =
    if (length > 10 && this[10] == ' ' && !contains('T')) replaceRange(10, 11, "T") else this

private fun String.normalizeShortOffset(): String = when {
    SHORT_HOUR_OFFSET.containsMatchIn(this) -> replace(SHORT_HOUR_OFFSET, "$1:00")
    COMPACT_HOUR_MINUTE_OFFSET.containsMatchIn(this) -> replace(COMPACT_HOUR_MINUTE_OFFSET, "$1:$2")
    else -> this
}

private fun TreeHolePost.statusCategory(): TreeHoleContentStatus = status.statusCategory()

private fun String?.statusCategory(): TreeHoleContentStatus = when (this?.trim()?.lowercase(Locale.ROOT)) {
    "approved" -> TreeHoleContentStatus.APPROVED
    "rejected" -> TreeHoleContentStatus.REJECTED
    else -> TreeHoleContentStatus.PENDING
}

private fun String.withCount(count: Int): String = replace("%d", count.coerceAtLeast(0).toString())

private fun String.withText(value: String): String = replace("%s", value)

private enum class TreeHoleContentStatus {
    APPROVED,
    PENDING,
    REJECTED,
}

private sealed interface TreeHoleDeleteTarget {
    data class Post(val post: TreeHolePost) : TreeHoleDeleteTarget
    data class Comment(val comment: MyTreeHoleComment) : TreeHoleDeleteTarget
}

private val SHORT_HOUR_OFFSET = Regex("([+-]\\d{2})$")
private val COMPACT_HOUR_MINUTE_OFFSET = Regex("([+-]\\d{2})(\\d{2})$")
private val MY_MIN_TOUCH_TARGET = NnoworkSize.minTouchTarget
private const val MY_SHEET_HEIGHT_FRACTION = 0.94f
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 60L * SECONDS_PER_MINUTE
private const val SECONDS_PER_DAY = 24L * SECONDS_PER_HOUR
private const val DAYS_SHOWN_RELATIVELY = 7L

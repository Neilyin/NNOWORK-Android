package com.neilyin.nnowork.feature.treehole

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neilyin.nnowork.data.treehole.TreeHolePost
import com.neilyin.nnowork.data.treehole.TreeHoleProfile
import com.neilyin.nnowork.data.treehole.TreeHoleRepository
import com.neilyin.nnowork.data.treehole.TreeHoleComment
import com.neilyin.nnowork.data.treehole.MyTreeHoleComment
import com.neilyin.nnowork.data.treehole.industries
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TreeHoleUiState(
    val profile: TreeHoleProfile? = null,
    val feedPosts: List<TreeHolePost> = emptyList(),
    val myPosts: List<TreeHolePost> = emptyList(),
    val myComments: List<MyTreeHoleComment> = emptyList(),
    val selectedPost: TreeHolePost? = null,
    val comments: List<TreeHoleComment> = emptyList(),
    val commentsLoading: Boolean = false,
    val loading: Boolean = false,
    val posting: Boolean = false,
    val syncingCoins: Boolean = false,
    val message: String? = null,
) {
    val hasProfile: Boolean get() = profile?.isComplete == true
}

class TreeHoleViewModel(
    private val repository: TreeHoleRepository = ServiceLocator.treeHoleRepository,
) : ViewModel() {
    private val localState = MutableStateFlow(TreeHoleUiState())

    val uiState: StateFlow<TreeHoleUiState> =
        combine(repository.profile, localState) { profile, state ->
            state.copy(profile = profile)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TreeHoleUiState())

    init {
        viewModelScope.launch {
            repository.profile.collect { profile ->
                if (profile?.isComplete == true) refreshAll()
            }
        }
    }

    fun saveProfile(profile: TreeHoleProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
            refreshAll()
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            localState.update { it.copy(loading = true, message = null) }
            runCatching {
                val feed = repository.fetchFeed()
                val mine = repository.fetchMyPosts()
                val comments = repository.fetchMyComments()
                localState.update { it.copy(feedPosts = feed, myPosts = mine, myComments = comments, loading = false) }
            }.onFailure { error ->
                localState.update { it.copy(loading = false, message = error.userMessage()) }
            }
        }
    }

    fun submit(content: String) {
        val trimmed = content.trim()
        if (trimmed.length < 8) {
            localState.update { it.copy(message = "至少寫 8 個字，讓大家知道你受了什麼委屈。") }
            return
        }
        viewModelScope.launch {
            localState.update { it.copy(posting = true, message = null) }
            runCatching {
                repository.submitPost(trimmed)
                val mine = repository.fetchMyPosts()
                val feed = repository.fetchFeed()
                val comments = repository.fetchMyComments()
                localState.update {
                    it.copy(
                        feedPosts = feed,
                        myPosts = mine,
                        myComments = comments,
                        posting = false,
                        message = "已送出，審核通過後會出現在樹洞。",
                    )
                }
            }.onFailure { error ->
                localState.update { it.copy(posting = false, message = error.userMessage()) }
            }
        }
    }

    fun delete(postId: String) {
        viewModelScope.launch {
            runCatching {
                repository.deleteMyPost(postId)
                repository.fetchMyPosts()
            }.onSuccess { mine ->
                localState.update { it.copy(myPosts = mine, message = "已刪除貼文。") }
            }.onFailure { error ->
                localState.update { it.copy(message = error.userMessage()) }
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            runCatching {
                repository.deleteMyComment(commentId)
                repository.fetchMyComments()
            }.onSuccess { comments ->
                localState.update { it.copy(myComments = comments, message = "已刪除留言。") }
            }.onFailure { error ->
                localState.update { it.copy(message = error.userMessage()) }
            }
        }
    }

    fun openPost(post: TreeHolePost) {
        viewModelScope.launch {
            localState.update { it.copy(selectedPost = post, commentsLoading = true, comments = emptyList(), message = null) }
            runCatching { repository.fetchComments(post.id) }
                .onSuccess { comments ->
                    localState.update { it.copy(comments = comments, commentsLoading = false) }
                }
                .onFailure { error ->
                    localState.update { it.copy(commentsLoading = false, message = error.userMessage()) }
                }
        }
    }

    fun closePost() {
        localState.update { it.copy(selectedPost = null, comments = emptyList(), commentsLoading = false) }
    }

    fun submitComment(content: String) {
        val post = localState.value.selectedPost ?: return
        val trimmed = content.trim()
        if (trimmed.length < 3) {
            localState.update { it.copy(message = "留言至少 3 個字。") }
            return
        }
        viewModelScope.launch {
            localState.update { it.copy(commentsLoading = true, message = null) }
            runCatching {
                repository.submitComment(post.id, trimmed)
                repository.fetchComments(post.id)
            }.onSuccess { comments ->
                localState.update { it.copy(comments = comments, commentsLoading = false, message = "留言已送出，審核通過後會顯示。") }
            }.onFailure { error ->
                localState.update { it.copy(commentsLoading = false, message = error.userMessage()) }
            }
        }
    }

    fun react(postId: String, reactionType: String) {
        viewModelScope.launch {
            runCatching {
                repository.reactPost(postId, reactionType)
                repository.fetchFeed()
            }.onSuccess { feed ->
                localState.update { it.copy(feedPosts = feed, message = "反應已更新。") }
            }.onFailure { error ->
                localState.update { it.copy(message = error.userMessage()) }
            }
        }
    }

    fun syncCoinsAndApprovals() {
        viewModelScope.launch {
            localState.update { it.copy(syncingCoins = true, message = null) }
            runCatching {
                val approvals = repository.checkNewApprovals()
                val balance = repository.syncCoinBalance()
                approvals to balance
            }.onSuccess { (approvals, balance) ->
                val approvalText = if (approvals > 0) "，有 $approvals 則新通過" else ""
                localState.update {
                    it.copy(syncingCoins = false, message = "金幣已同步：$balance 枚$approvalText。")
                }
            }.onFailure { error ->
                localState.update { it.copy(syncingCoins = false, message = error.userMessage()) }
            }
        }
    }

    private fun Throwable.userMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "網路或伺服器暫時不穩，稍後再試。"
}

@Composable
fun TreeHoleScreen(viewModel: TreeHoleViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TreeHoleContent(
        state = state,
        onSaveProfile = viewModel::saveProfile,
        onRefresh = viewModel::refreshAll,
        onSubmit = viewModel::submit,
        onDelete = viewModel::delete,
        onDeleteComment = viewModel::deleteComment,
        onOpenPost = viewModel::openPost,
        onClosePost = viewModel::closePost,
        onSubmitComment = viewModel::submitComment,
        onReact = viewModel::react,
        onSyncCoins = viewModel::syncCoinsAndApprovals,
    )
}

@Composable
private fun TreeHoleContent(
    state: TreeHoleUiState,
    onSaveProfile: (TreeHoleProfile) -> Unit,
    onRefresh: () -> Unit,
    onSubmit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onOpenPost: (TreeHolePost) -> Unit,
    onClosePost: () -> Unit,
    onSubmitComment: (String) -> Unit,
    onReact: (String, String) -> Unit,
    onSyncCoins: () -> Unit,
) {
    if (!state.hasProfile) {
        TreeHoleProfileSetup(
            initial = state.profile,
            message = state.message,
            onSave = onSaveProfile,
        )
    } else {
        TreeHoleMain(
            state = state,
            onSaveProfile = onSaveProfile,
            onRefresh = onRefresh,
            onSubmit = onSubmit,
            onDelete = onDelete,
            onDeleteComment = onDeleteComment,
            onOpenPost = onOpenPost,
            onClosePost = onClosePost,
            onSubmitComment = onSubmitComment,
            onReact = onReact,
            onSyncCoins = onSyncCoins,
        )
    }
}

@Composable
private fun TreeHoleProfileSetup(
    initial: TreeHoleProfile?,
    message: String?,
    onSave: (TreeHoleProfile) -> Unit,
) {
    var nickname by remember(initial) { mutableStateOf(initial?.nickname.orEmpty()) }
    var industry by remember(initial) { mutableStateOf(initial?.industry ?: industries.first()) }
    var company by remember(initial) { mutableStateOf(initial?.companyName.orEmpty()) }
    var showCompany by remember(initial) { mutableStateOf(initial?.showCompany ?: false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelColors.darkGray)
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("取暖樹洞", style = pixelTextStyle(30.sp, FontWeight.Bold, PixelColors.white))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pixelCard()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Spa, null, tint = PixelColors.green, modifier = Modifier.size(34.dp))
                Spacer(Modifier.size(10.dp))
                Column {
                    Text("先設定樹洞身份", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.black))
                    Text("只存在本機，用來發文與管理自己的貼文。", style = pixelTextStyle(12.sp, color = PixelColors.gray))
                }
            }
            PixelTextField("暱稱", nickname, "例如：快下班的小 N") { nickname = it }
            Text("產業", style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.gray))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                industries.forEach { item ->
                    PixelButton(
                        onClick = { industry = item },
                        backgroundColor = if (industry == item) PixelColors.green else PixelColors.gray,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                    ) {
                        Text(item, style = pixelTextStyle(12.sp, FontWeight.Bold, PixelColors.black))
                    }
                }
            }
            PixelTextField("公司名稱（選填）", company, "不想填也可以") { company = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = showCompany,
                    onCheckedChange = { showCompany = it },
                    colors = CheckboxDefaults.colors(checkedColor = PixelColors.green),
                )
                Text("發文時顯示公司名稱", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
            }
            message?.let { Text(it, style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.red)) }
            PixelButton(
                onClick = {
                    onSave(
                        TreeHoleProfile(
                            nickname = nickname.trim(),
                            industry = industry,
                            companyName = company.trim(),
                            showCompany = showCompany,
                        ),
                    )
                },
                enabled = nickname.isNotBlank() && industry.isNotBlank(),
                backgroundColor = if (nickname.isNotBlank()) PixelColors.yellow else PixelColors.gray,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("加入樹洞", style = pixelTextStyle(17.sp, FontWeight.Bold, PixelColors.black))
            }
        }
    }
}

@Composable
private fun ProfileEditDialog(
    initial: TreeHoleProfile?,
    onDismiss: () -> Unit,
    onSave: (TreeHoleProfile) -> Unit,
) {
    var nickname by remember(initial) { mutableStateOf(initial?.nickname.orEmpty()) }
    var industry by remember(initial) { mutableStateOf(initial?.industry ?: industries.first()) }
    var company by remember(initial) { mutableStateOf(initial?.companyName.orEmpty()) }
    var showCompany by remember(initial) { mutableStateOf(initial?.showCompany ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PixelColors.darkGray,
        title = { Text("編輯樹洞身份", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PixelTextField("暱稱", nickname, "例如：快下班的小 N") { nickname = it }
                Text("產業", style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.gray))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    industries.forEach { item ->
                        PixelButton(
                            onClick = { industry = item },
                            backgroundColor = if (industry == item) PixelColors.green else PixelColors.gray,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                        ) {
                            Text(item, style = pixelTextStyle(12.sp, FontWeight.Bold, PixelColors.black))
                        }
                    }
                }
                PixelTextField("公司名稱（選填）", company, "不想填也可以") { company = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showCompany,
                        onCheckedChange = { showCompany = it },
                        colors = CheckboxDefaults.colors(checkedColor = PixelColors.green),
                    )
                    Text("發文時顯示公司名稱", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.white))
                }
            }
        },
        confirmButton = {
            PixelButton(
                onClick = {
                    onSave(
                        TreeHoleProfile(
                            nickname = nickname.trim(),
                            industry = industry,
                            companyName = company.trim(),
                            showCompany = showCompany,
                        ),
                    )
                },
                enabled = nickname.isNotBlank() && industry.isNotBlank(),
                backgroundColor = if (nickname.isNotBlank()) PixelColors.yellow else PixelColors.gray,
            ) {
                Text("儲存", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
            }
        },
        dismissButton = {
            PixelButton(onClick = onDismiss, backgroundColor = PixelColors.gray) {
                Text("取消", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
            }
        },
    )
}

@Composable
private fun TreeHoleMain(
    state: TreeHoleUiState,
    onSaveProfile: (TreeHoleProfile) -> Unit,
    onRefresh: () -> Unit,
    onSubmit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onOpenPost: (TreeHolePost) -> Unit,
    onClosePost: () -> Unit,
    onSubmitComment: (String) -> Unit,
    onReact: (String, String) -> Unit,
    onSyncCoins: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedMineTab by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf("") }
    var editingProfile by remember { mutableStateOf(false) }

    LaunchedEffect(state.posting) {
        if (!state.posting && state.message?.startsWith("已送出") == true) draft = ""
    }

    state.selectedPost?.let { post ->
        CommentsDialog(
            post = post,
            comments = state.comments,
            loading = state.commentsLoading,
            message = state.message,
            onDismiss = onClosePost,
            onSubmit = onSubmitComment,
        )
    }

    if (editingProfile) {
        ProfileEditDialog(
            initial = state.profile,
            onDismiss = { editingProfile = false },
            onSave = {
                editingProfile = false
                onSaveProfile(it)
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelColors.darkGray)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("取暖樹洞", style = pixelTextStyle(30.sp, FontWeight.Bold, PixelColors.white), modifier = Modifier.weight(1f))
                PixelButton(onClick = onSyncCoins, backgroundColor = PixelColors.yellow, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)) {
                    Text(if (state.syncingCoins) "..." else "🪙", style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.black))
                }
                Spacer(Modifier.size(8.dp))
                PixelButton(onClick = { editingProfile = true }, backgroundColor = PixelColors.green, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)) {
                    Text("身份", style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.black))
                }
                Spacer(Modifier.size(8.dp))
                PixelButton(onClick = onRefresh, backgroundColor = PixelColors.blue, contentPadding = PaddingValues(10.dp)) {
                    Icon(Icons.Filled.Refresh, null, tint = PixelColors.white)
                }
            }
        }
        item {
            ComposerCard(
                draft = draft,
                posting = state.posting,
                onDraftChange = { draft = it.take(500) },
                onSubmit = { onSubmit(draft) },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentButton("大家的樹洞", selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
                SegmentButton("我的貼文", selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
            }
        }
        state.message?.let {
            item {
                Text(
                    it,
                    style = pixelTextStyle(13.sp, FontWeight.Bold, if (it.startsWith("已")) PixelColors.green else PixelColors.yellow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PixelColors.black.copy(alpha = 0.25f))
                        .padding(10.dp),
                )
            }
        }
        if (state.loading) {
            item {
                Text("載入中...", style = pixelTextStyle(16.sp, FontWeight.Bold, PixelColors.white), modifier = Modifier.padding(vertical = 30.dp))
            }
        } else {
            if (selectedTab == 1) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SegmentButton("我的貼文", selectedMineTab == 0, Modifier.weight(1f)) { selectedMineTab = 0 }
                        SegmentButton("我的留言", selectedMineTab == 1, Modifier.weight(1f)) { selectedMineTab = 1 }
                    }
                }
            }
            val posts = if (selectedTab == 0) state.feedPosts else if (selectedMineTab == 0) state.myPosts else emptyList()
            if (posts.isEmpty()) {
                if (selectedTab == 1 && selectedMineTab == 1) {
                    if (state.myComments.isEmpty()) {
                        item {
                            Text("你還沒有留過言。", style = pixelTextStyle(14.sp, color = PixelColors.gray), modifier = Modifier.padding(vertical = 30.dp))
                        }
                    } else {
                        items(state.myComments, key = { it.id }) { comment ->
                            MyCommentCard(comment = comment, onDelete = { onDeleteComment(comment.id) })
                        }
                    }
                } else {
                    item {
                        Text(
                            if (selectedTab == 0) "目前還沒有審核通過的貼文。" else "你還沒有發過貼文。",
                            style = pixelTextStyle(14.sp, color = PixelColors.gray),
                            modifier = Modifier.padding(vertical = 30.dp),
                        )
                    }
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        mine = selectedTab == 1,
                        onOpen = { onOpenPost(post) },
                        onReact = { reaction -> onReact(post.id, reaction) },
                        onDelete = { onDelete(post.id) },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ComposerCard(
    draft: String,
    posting: Boolean,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Add, null, tint = PixelColors.orange)
            Spacer(Modifier.size(8.dp))
            Text("丟一張紙條進樹洞", style = pixelTextStyle(18.sp, FontWeight.Bold, PixelColors.black))
        }
        BasicTextField(
            value = draft,
            onValueChange = onDraftChange,
            textStyle = pixelTextStyle(15.sp, FontWeight.Medium, PixelColors.black),
            cursorBrush = SolidColor(PixelColors.black),
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(PixelColors.white)
                .border(3.dp, PixelColors.black, RectangleShape)
                .padding(12.dp),
            decorationBox = { inner ->
                if (draft.isEmpty()) Text("今天公司又做了什麼荒唐事...", style = pixelTextStyle(14.sp, color = PixelColors.gray))
                inner()
            },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${draft.length} / 500", style = pixelTextStyle(12.sp, color = if (draft.length > 500) PixelColors.red else PixelColors.gray))
            Spacer(Modifier.weight(1f))
            PixelButton(
                onClick = onSubmit,
                enabled = draft.trim().length >= 8 && !posting,
                backgroundColor = if (draft.trim().length >= 8) PixelColors.yellow else PixelColors.gray,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Filled.Send, null, tint = PixelColors.black, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(if (posting) "送出中" else "送出", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
            }
        }
    }
}

@Composable
private fun SegmentButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    PixelButton(
        onClick = onClick,
        backgroundColor = if (selected) PixelColors.green else PixelColors.gray,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        Text(label, style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
    }
}

@Composable
private fun PostCard(
    post: TreeHolePost,
    mine: Boolean,
    onOpen: () -> Unit,
    onReact: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(post.nickname ?: "匿名社畜", style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.black), modifier = Modifier.weight(1f))
            post.status?.let { StatusBadge(it) }
            if (mine) {
                Spacer(Modifier.size(8.dp))
                PixelButton(onClick = onDelete, backgroundColor = PixelColors.red, contentPadding = PaddingValues(7.dp)) {
                    Icon(Icons.Filled.Delete, null, tint = PixelColors.white, modifier = Modifier.size(17.dp))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Work, null, tint = PixelColors.gray, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(5.dp))
            Text(
                listOfNotNull(post.industry, post.companyName).joinToString(" · ").ifBlank { "職場倖存者" },
                style = pixelTextStyle(12.sp, color = PixelColors.gray),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(post.content, style = pixelTextStyle(16.sp, FontWeight.Medium, PixelColors.black))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ReactionButton("👍", post.likesCount) { onReact("like") }
            ReactionButton("💪", post.cheerCount) { onReact("cheer") }
            ReactionButton("😭", post.cryCount) { onReact("cry") }
            Spacer(Modifier.weight(1f))
            PixelButton(onClick = onOpen, backgroundColor = PixelColors.gray, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)) {
                Icon(Icons.Filled.Favorite, null, tint = PixelColors.red, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(5.dp))
                Text("${post.commentCount}", style = pixelTextStyle(12.sp, FontWeight.Bold, PixelColors.black))
            }
        }
        post.latestComment?.let {
            Text("最新回覆：$it", style = pixelTextStyle(12.sp, color = PixelColors.gray), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MyCommentCard(comment: MyTreeHoleComment, onDelete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(comment.status)
            Spacer(Modifier.weight(1f))
            PixelButton(onClick = onDelete, backgroundColor = PixelColors.red, contentPadding = PaddingValues(7.dp)) {
                Icon(Icons.Filled.Delete, null, tint = PixelColors.white, modifier = Modifier.size(17.dp))
            }
        }
        Text(comment.content, style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.black))
        comment.postContent?.let {
            Text(
                "回覆貼文：$it",
                style = pixelTextStyle(12.sp, color = PixelColors.gray),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (label, color) = when (status) {
        "approved" -> "已通過" to PixelColors.green
        "rejected" -> "未通過" to PixelColors.red
        else -> "審核中" to PixelColors.orange
    }
    Box(
        modifier = Modifier
            .background(color)
            .border(2.dp, PixelColors.black, RectangleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, style = pixelTextStyle(11.sp, FontWeight.Bold, PixelColors.black))
    }
}

@Composable
private fun ReactionButton(icon: String, count: Int, onClick: () -> Unit) {
    PixelButton(
        onClick = onClick,
        backgroundColor = PixelColors.white,
        borderColor = PixelColors.gray,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text("$icon $count", style = pixelTextStyle(12.sp, FontWeight.Bold, PixelColors.black))
    }
}

@Composable
private fun CommentsDialog(
    post: TreeHolePost,
    comments: List<TreeHoleComment>,
    loading: Boolean,
    message: String?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var draft by remember(post.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PixelColors.darkGray,
        title = { Text("樹洞回聲", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(post.content, style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.white))
                if (loading) {
                    Text("載入中...", style = pixelTextStyle(13.sp, color = PixelColors.gray))
                } else if (comments.isEmpty()) {
                    Text("還沒有通過審核的留言。", style = pixelTextStyle(13.sp, color = PixelColors.gray))
                } else {
                    comments.take(6).forEach { comment ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PixelColors.white)
                                .border(2.dp, PixelColors.black, RectangleShape)
                                .padding(10.dp),
                        ) {
                            Text(comment.nickname, style = pixelTextStyle(12.sp, FontWeight.Bold, PixelColors.black))
                            Text(comment.content, style = pixelTextStyle(13.sp, color = PixelColors.black))
                        }
                    }
                }
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it.take(200) },
                    textStyle = pixelTextStyle(14.sp, FontWeight.Medium, PixelColors.black),
                    cursorBrush = SolidColor(PixelColors.black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp)
                        .background(PixelColors.white)
                        .border(3.dp, PixelColors.black, RectangleShape)
                        .padding(10.dp),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) Text("回一點溫暖...", style = pixelTextStyle(13.sp, color = PixelColors.gray))
                        inner()
                    },
                )
                message?.let { Text(it, style = pixelTextStyle(12.sp, FontWeight.Bold, PixelColors.yellow)) }
            }
        },
        confirmButton = {
            PixelButton(
                onClick = {
                    onSubmit(draft)
                    draft = ""
                },
                enabled = draft.trim().length >= 3 && !loading,
                backgroundColor = if (draft.trim().length >= 3) PixelColors.yellow else PixelColors.gray,
            ) {
                Text("送出留言", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
            }
        },
        dismissButton = {
            PixelButton(onClick = onDismiss, backgroundColor = PixelColors.gray) {
                Text("關閉", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
            }
        },
    )
}

@Composable
private fun PixelTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.gray))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = pixelTextStyle(16.sp, FontWeight.Medium, PixelColors.black),
            cursorBrush = SolidColor(PixelColors.black),
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelColors.white)
                .border(3.dp, PixelColors.black, RectangleShape)
                .padding(12.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, style = pixelTextStyle(15.sp, color = PixelColors.gray))
                inner()
            },
        )
    }
}

@Preview(name = "取暖樹洞", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TreeHolePreview() {
    TreeHoleContent(
        state = TreeHoleUiState(
            profile = TreeHoleProfile("快下班的小 N", "科技業", "血汗科技", false),
            feedPosts = listOf(
                TreeHolePost(
                    id = "1",
                    content = "今天會議開了兩小時，結論是明天再開一次。大家有沒有推薦的離職歌單？",
                    industry = "科技業",
                    nickname = "會議倖存者",
                    likesCount = 12,
                    cheerCount = 7,
                    cryCount = 3,
                    commentCount = 2,
                    latestComment = "先把履歷打開，呼吸會順一點。",
                ),
            ),
            myPosts = listOf(
                TreeHolePost(id = "2", content = "我只是想準時下班。", status = "pending", nickname = "快下班的小 N"),
            ),
            myComments = listOf(
                MyTreeHoleComment(id = "c1", content = "先把履歷打開，呼吸會順一點。", status = "approved", postContent = "今天又開了三小時會議。"),
            ),
        ),
        onSaveProfile = {},
        onRefresh = {},
        onSubmit = {},
        onDelete = {},
        onDeleteComment = {},
        onOpenPost = {},
        onClosePost = {},
        onSubmitComment = {},
        onReact = { _, _ -> },
        onSyncCoins = {},
    )
}

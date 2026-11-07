package com.neilyin.nnowork.feature.jobarchive

import com.neilyin.nnowork.data.jobarchive.JobTransitionException
import com.neilyin.nnowork.data.jobarchive.JobTransitionPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 換工作畫面要的資料層動作（正式版接 JobArchiveRepository，測試用假的）。 */
interface JobTransitionActions {
    /** 封存並開始新工作；回傳要取消提醒的待辦 id。失敗丟 [JobTransitionException]。 */
    suspend fun archiveAndStartNewJob(newCompanyName: String, maxStamps: Int): List<String>

    /** 使用者明確確認「不保存，直接開始」後才可呼叫；回傳要取消提醒的待辦 id。 */
    suspend fun startNewJobWithoutArchive(newCompanyName: String, maxStamps: Int): List<String>

    suspend fun hasUnfinishedUploadedArchive(): Boolean
}

enum class JobTransitionError { NETWORK, NOT_SIGNED_IN, SERVER_NOT_READY, SERVER, LOCAL_SAVE }

data class JobTransitionUiState(
    val companyName: String = "",
    val maxStamps: Int = JobTransitionPolicy.DEFAULT_MAX_STAMPS,
    val processing: Boolean = false,
    /** 「確定不保存這段紀錄？」破壞性確認對話框。 */
    val showDiscardConfirm: Boolean = false,
    val error: JobTransitionError? = null,
    val serverMessage: String? = null,
    val hasUnfinishedArchive: Boolean = false,
    /** 新工作已開始（封存或不保存皆算），畫面應關閉。 */
    val completed: Boolean = false,
) {
    val trimmedName: String get() = companyName.trim()
    val canProceed: Boolean get() = trimmedName.isNotEmpty() && !processing
}

/**
 * iOS `JobTransitionSheet` 的行為（不含畫面）：
 * - 已登入：「封存並開始新工作」→ 上傳成功才清除目前工作。
 * - 「不保存，直接開始」一定要先跳破壞性確認，按下確認才呼叫 [JobTransitionActions.startNewJobWithoutArchive]。
 * 純 Kotlin、操作是 suspend，ViewModel 用 viewModelScope 呼叫；單元測試直接 runBlocking。
 */
class JobTransitionController(
    private val actions: JobTransitionActions,
    private val cancelReminders: (List<String>) -> Unit,
) {
    private val _state = MutableStateFlow(JobTransitionUiState())
    val state: StateFlow<JobTransitionUiState> = _state.asStateFlow()

    suspend fun load() {
        val unfinished = runCatching { actions.hasUnfinishedUploadedArchive() }
            .onFailure { if (it is CancellationException) throw it }
            .getOrDefault(false)
        _state.update { it.copy(hasUnfinishedArchive = unfinished) }
    }

    fun updateCompanyName(name: String) = _state.update { it.copy(companyName = name) }

    fun selectMaxStamps(count: Int) {
        if (count in MAX_STAMP_OPTIONS) _state.update { it.copy(maxStamps = count) }
    }

    /** 只打開確認框，不動任何資料。 */
    fun requestDiscard() {
        if (!_state.value.canProceed) return
        _state.update { it.copy(showDiscardConfirm = true) }
    }

    /** 確認框按「取消」或點外面：什麼都不做。 */
    fun dismissDiscard() = _state.update { it.copy(showDiscardConfirm = false) }

    /** 確認框按「不保存，直接開始」。 */
    suspend fun confirmDiscard() {
        val current = _state.value
        if (!current.showDiscardConfirm || !current.canProceed) return
        _state.update { it.copy(showDiscardConfirm = false, processing = true, error = null) }
        run(JobTransitionError.LOCAL_SAVE) { actions.startNewJobWithoutArchive(current.trimmedName, current.maxStamps) }
    }

    suspend fun archive() {
        val current = _state.value
        if (!current.canProceed) return
        _state.update { it.copy(processing = true, error = null, serverMessage = null) }
        run(JobTransitionError.NETWORK) { actions.archiveAndStartNewJob(current.trimmedName, current.maxStamps) }
    }

    fun dismissError() = _state.update { it.copy(error = null, serverMessage = null) }

    private suspend fun run(fallback: JobTransitionError, block: suspend () -> List<String>) {
        try {
            val clearedTodoIds = block()
            runCatching { cancelReminders(clearedTodoIds) }
            _state.update { it.copy(processing = false, completed = true) }
        } catch (cancelled: CancellationException) {
            _state.update { it.copy(processing = false) }
            throw cancelled
        } catch (error: Exception) {
            val unfinished = runCatching { actions.hasUnfinishedUploadedArchive() }.getOrDefault(false)
            _state.update {
                it.copy(
                    processing = false,
                    error = errorKind(error, fallback),
                    serverMessage = (error as? JobTransitionException.Server)?.message,
                    hasUnfinishedArchive = unfinished,
                )
            }
        }
    }

    companion object {
        val MAX_STAMP_OPTIONS = listOf(10, 20, 30)

        /** [fallback]：iOS 對非預期錯誤的預設文案（封存＝網路失敗、不保存＝本機儲存失敗）。 */
        fun errorKind(error: Throwable, fallback: JobTransitionError): JobTransitionError = when (error) {
            is JobTransitionException.NotSignedIn -> JobTransitionError.NOT_SIGNED_IN
            is JobTransitionException.ServerNotReady -> JobTransitionError.SERVER_NOT_READY
            is JobTransitionException.Server -> JobTransitionError.SERVER
            is JobTransitionException.LocalSaveFailed -> JobTransitionError.LOCAL_SAVE
            is JobTransitionException.Network -> JobTransitionError.NETWORK
            else -> fallback
        }
    }
}

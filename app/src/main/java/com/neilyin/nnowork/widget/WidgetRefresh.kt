package com.neilyin.nnowork.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** 定時刷新桌面 Widget（薪水快照、語錄）。 */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        WidgetScheduler.refreshNow(applicationContext)
        return Result.success()
    }
}

object WidgetScheduler {
    private const val WORK_NAME = "widget_refresh"

    /** 依設定的刷新分鐘排程；WorkManager 週期下限為 15 分鐘。 */
    fun schedule(context: Context, intervalMinutes: Int) {
        val minutes = intervalMinutes.coerceAtLeast(15).toLong()
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(minutes, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    suspend fun refreshNow(context: Context) {
        runCatching { SalaryWidget().updateAll(context) }
        runCatching { QuoteWidget().updateAll(context) }
        runCatching { NiuMaWidget().updateAll(context) }
    }
}

/**
 * 牛馬 Widget 專用的刷新鏈：Widget 每次渲染後依狀態排下一次（上班中每分鐘、其餘在狀態邊界），
 * 只重繪 [NiuMaWidget]，不動其他 Widget。沒有任何牛馬 Widget 時 provideGlance 不會被呼叫，鏈自然中止。
 * 不使用 AlarmManager（省電、無需精確鬧鐘權限）；WorkManager 在 Doze 下可能延後，故倒數／計時交給 Chronometer。
 */
class NiuMaRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        runCatching { NiuMaWidget().updateAll(applicationContext) }
        return Result.success()
    }
}

object NiuMaRefresh {
    private const val TICK_WORK = "niuma_widget_tick"
    private const val CHANGED_WORK = "niuma_widget_changed"

    fun scheduleNext(context: Context, state: NiuMaState, now: Long) {
        val at = NiuMaWidgetState.nextRefreshAt(state, now)
        val manager = WorkManager.getInstance(context)
        if (at == null) {
            manager.cancelUniqueWork(TICK_WORK)
            return
        }
        val request = OneTimeWorkRequestBuilder<NiuMaRefreshWorker>()
            .setInitialDelay((at - now).coerceAtLeast(1_000L), TimeUnit.MILLISECONDS)
            .build()
        manager.enqueueUniqueWork(TICK_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    /** 薪資設定／手動計時變動後立刻重繪（由 PlanRepository.update 呼叫）。 */
    fun requestUpdate(context: Context) {
        runCatching {
            val request = OneTimeWorkRequestBuilder<NiuMaRefreshWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(CHANGED_WORK, ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun cancel(context: Context) {
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork(TICK_WORK)
            WorkManager.getInstance(context).cancelUniqueWork(CHANGED_WORK)
        }
    }
}

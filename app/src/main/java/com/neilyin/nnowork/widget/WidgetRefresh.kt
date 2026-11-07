package com.neilyin.nnowork.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
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
    }
}

package com.neilyin.nnowork.notify

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** 待辦提醒排程：以 WorkManager 唯一工作（todo id）排程／取消本機通知。 */
object ReminderScheduler {

    private fun workName(todoId: String) = "todo_reminder_$todoId"

    fun schedule(context: Context, todoId: String, title: String, triggerAtMillis: Long) {
        val delay = triggerAtMillis - System.currentTimeMillis()
        if (delay <= 0) return // 過去的時間不排程
        val request = OneTimeWorkRequestBuilder<TodoReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(TodoReminderWorker.KEY_TITLE, title).build())
            .addTag(workName(todoId))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(todoId), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, todoId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(todoId))
    }
}

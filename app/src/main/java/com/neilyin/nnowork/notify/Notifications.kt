package com.neilyin.nnowork.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** 通知頻道集中管理。 */
object Notifications {
    const val CHANNEL_TODO = "todo_reminders"
    const val CHANNEL_TREEHOLE = "treehole_push"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_TODO, "待辦提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "離職計畫待辦事項的提醒"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_TREEHOLE, "樹洞通知", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "樹洞貼文審核通過等通知"
            },
        )
    }
}

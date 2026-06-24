package com.neilyin.nnowork.di

import android.content.Context
import com.neilyin.nnowork.data.local.AppDatabase
import com.neilyin.nnowork.data.local.StampDao
import com.neilyin.nnowork.data.plan.PlanRepository
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.data.treehole.TreeHoleRepository

/**
 * 輕量級依賴提供者（取代 Hilt）。在 Application 初始化。
 */
object ServiceLocator {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val database: AppDatabase by lazy { AppDatabase.get(appContext) }
    val stampDao: StampDao by lazy { database.stampDao() }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val planRepository: PlanRepository by lazy { PlanRepository(appContext) }
    val treeHoleRepository: TreeHoleRepository by lazy {
        TreeHoleRepository(appContext, settingsRepository, stampDao)
    }
}

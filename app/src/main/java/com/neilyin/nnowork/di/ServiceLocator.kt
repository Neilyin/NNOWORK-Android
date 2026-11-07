package com.neilyin.nnowork.di

import android.content.Context
import com.neilyin.nnowork.data.auth.AuthRepository
import com.neilyin.nnowork.data.backup.BackupRepository
import com.neilyin.nnowork.data.coin.CoinLedgerDao
import com.neilyin.nnowork.data.coin.CoinLedgerRepository
import com.neilyin.nnowork.data.identity.DeviceIdentityRepository
import com.neilyin.nnowork.data.local.AppDatabase
import com.neilyin.nnowork.data.local.StampDao
import com.neilyin.nnowork.data.plan.PlanRepository
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.data.shop.ShopPurchaseRepository
import com.neilyin.nnowork.data.jobarchive.JobArchiveRepository
import com.neilyin.nnowork.data.treehole.TreeHoleApi
import com.neilyin.nnowork.data.treehole.TreeHoleRepository
import com.neilyin.nnowork.data.treehole.UserTokenCaller
import com.neilyin.nnowork.data.treehole.createTreeHoleApi

/**
 * 輕量級依賴提供者（取代 Hilt）。在 Application 初始化。
 */
object ServiceLocator {
    private lateinit var appContext: Context
    private lateinit var deviceIdentityRepositoryInstance: DeviceIdentityRepository

    fun init(context: Context) {
        appContext = context.applicationContext
        // 在任何新資料檔建立前快照舊版資料狀態，供 device hash 一次性遷移。
        deviceIdentityRepositoryInstance = DeviceIdentityRepository.createAtAppStartup(appContext)
    }

    val applicationContext: Context get() = appContext
    val deviceIdentityRepository: DeviceIdentityRepository get() = deviceIdentityRepositoryInstance

    val database: AppDatabase by lazy { AppDatabase.get(appContext) }
    val authRepository: AuthRepository by lazy { AuthRepository(appContext, deviceIdentityRepository) }
    val stampDao: StampDao by lazy { database.stampDao() }
    val coinLedgerDao: CoinLedgerDao by lazy { database.coinLedgerDao() }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val coinLedgerRepository: CoinLedgerRepository by lazy {
        CoinLedgerRepository(database, settingsRepository)
    }
    val planRepository: PlanRepository by lazy { PlanRepository(appContext) }
    val shopPurchaseRepository: ShopPurchaseRepository by lazy {
        ShopPurchaseRepository(
            database,
            deviceIdentityRepository,
            accessTokenProvider = { authRepository.currentAccessToken },
        )
    }
    val backupRepository: BackupRepository by lazy {
        BackupRepository(appContext, database, coinLedgerRepository)
    }
    /** 樹洞、獎勵、封存共用同一個 Retrofit/OkHttp。 */
    private val treeHoleApi: TreeHoleApi by lazy { createTreeHoleApi() }

    val jobArchiveRepository: JobArchiveRepository by lazy {
        JobArchiveRepository(
            context = appContext,
            database = database,
            api = { treeHoleApi },
            tokenCaller = UserTokenCaller(
                accessToken = { authRepository.currentAccessToken },
                refreshAccessToken = { stale -> authRepository.refreshAccessToken(stale) },
            ),
            currentOwnerId = { authRepository.currentUserId },
        )
    }

    val treeHoleRepository: TreeHoleRepository by lazy {
        TreeHoleRepository(
            appContext,
            settingsRepository,
            stampDao,
            deviceIdentityRepository,
            coinLedgerRepository,
            accessTokenProvider = { authRepository.currentAccessToken },
            claimDeviceData = { authRepository.claimDeviceData() },
            refreshAccessToken = { stale -> authRepository.refreshAccessToken(stale) },
            currentUserId = { authRepository.currentUserId },
            runNumberProvider = { jobArchiveRepository.currentRunNumber() },
            apiFactory = { treeHoleApi },
        )
    }
}

package com.neilyin.nnowork.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neilyin.nnowork.data.treehole.TreeHoleIndustryCatalog
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.feature.treehole.TreeHoleIndustryFilterSheet
import com.neilyin.nnowork.feature.treehole.TreeHoleIndustryOptionUi
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.NnoworkPageTitle
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.appWideTheme

/**
 * Debug-only：在目前主題下直接打開樹洞的「職業篩選」bottom sheet（正式元件），
 * 供截圖驗證 sheet 外觀；不需要先同意樹洞條款或連網。release 建置不包含。
 *
 * adb shell am start -n com.chi157.resignationpointscard/com.neilyin.nnowork.debug.DebugSheetActivity
 */
class DebugSheetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by ServiceLocator.settingsRepository.settings.collectAsState(initial = null)
            val current = settings ?: return@setContent
            val theme = CardTheme.fromId(current.cardStyle).appWideTheme(current.unlockedStyles)
            NnoworkTheme(cardTheme = theme) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(NnoworkTheme.colors.appBG)
                        .statusBarsPadding()
                        .padding(NnoworkSpacing.page),
                ) {
                    NnoworkPageTitle("取暖樹洞")
                }
                val options = TreeHoleIndustryCatalog.entries.take(8).map {
                    TreeHoleIndustryOptionUi(key = it.canonicalValue, label = stringResource(it.labelRes))
                }
                TreeHoleIndustryFilterSheet(
                    industries = options,
                    selectedIndustries = options.take(2).map { it.key }.toSet(),
                    myIndustry = options.first().key,
                    onApply = { finish() },
                    onDismiss = { finish() },
                )
            }
        }
    }
}

package com.neilyin.nnowork.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.neilyin.nnowork.feature.history.HistoryScreen
import com.neilyin.nnowork.feature.home.HomeScreen
import com.neilyin.nnowork.feature.plan.PlanScreen
import com.neilyin.nnowork.feature.settings.SettingsScreen
import com.neilyin.nnowork.feature.treehole.TreeHoleScreen

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("集點卡", Icons.Filled.GridView),
    TabItem("計畫", Icons.AutoMirrored.Filled.ListAlt),
    TabItem("紀錄", Icons.Filled.Book),
    TabItem("樹洞", Icons.Filled.Park),
    TabItem("設定", Icons.Filled.Settings),
)

@Composable
fun MainScaffold(
    initialSelected: Int = 0,
    onShowUpdateNotice: () -> Unit = {},
) {
    var selected by rememberSaveable { mutableIntStateOf(initialSelected.coerceIn(tabs.indices)) }
    var receiptRequest by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // 各主分頁自行處理頂部 system bar；Scaffold 只提供底部導覽的高度。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RectangleShape,
                ),
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = {
                            selected = index
                            com.neilyin.nnowork.analytics.Analytics.log("tab_switched", "tab" to tab.label)
                        },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                            indicatorColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selected) {
                0 -> HomeScreen(
                    onCardCompleted = {
                        receiptRequest++
                        selected = 2
                    },
                    onMasterCompleted = {
                        receiptRequest++
                        selected = 2
                    },
                )
                1 -> PlanScreen()
                2 -> HistoryScreen(receiptRequest = receiptRequest)
                3 -> TreeHoleScreen(onExit = { selected = 0 })
                4 -> SettingsScreen(onShowUpdateNotice = onShowUpdateNotice)
            }
        }
    }
}

package com.neilyin.nnowork.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.VerticalDivider
import com.neilyin.nnowork.ui.adaptive.LocalNnoworkWindowInfo
import com.neilyin.nnowork.ui.adaptive.NnoworkNavigationType
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.font.FontWeight
import com.neilyin.nnowork.ui.theme.NnoworkLine
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

    val useRail = LocalNnoworkWindowInfo.current.navigationType == NnoworkNavigationType.Rail
    val onSelect: (Int) -> Unit = { index ->
        selected = index
        com.neilyin.nnowork.analytics.Analytics.log("tab_switched", "tab" to tabs[index].label)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // 各主分頁自行處理頂部 system bar；Scaffold 只提供底部導覽的高度。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!useRail) {
                Column {
                    // 方向 B：底欄不加方框，上緣一條 divider；淺色主題白底、深色主題 darkGray（iOS tabBarBG）。
                    HorizontalDivider(thickness = NnoworkLine.hairline, color = NnoworkTheme.colors.divider)
                    NavigationBar(
                        containerColor = NnoworkTheme.colors.tabBarBG,
                        tonalElevation = 0.dp,
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = selected == index,
                                onClick = { onSelect(index) },
                                icon = { TabIcon(tab) },
                                label = { TabLabel(tab, selected == index) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NnoworkTheme.colors.tabSelected,
                                    unselectedIconColor = NnoworkTheme.colors.tabUnselected,
                                    selectedTextColor = NnoworkTheme.colors.tabSelected,
                                    unselectedTextColor = NnoworkTheme.colors.tabUnselected,
                                    indicatorColor = NnoworkTheme.colors.tabIndicator,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        // 分頁內容不論底欄或側欄都放在同一個位置（Row → Box），摺疊／展開重建後
        // rememberSaveable 的 key 路徑不變，輸入中的文字與開著的對話框才還原得回來。
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (useRail) {
                NnoworkNavigationRail(selected = selected, onSelect = onSelect)
                VerticalDivider(thickness = NnoworkLine.hairline, color = NnoworkTheme.colors.divider)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    // 側欄模式沒有底欄吃掉手勢列高度，內容自己讓開底部與右側 system bar。
                    .then(
                        if (useRail) {
                            Modifier.windowInsetsPadding(
                                WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.End),
                            )
                        } else {
                            Modifier
                        },
                    ),
            ) {
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
}

@Composable
private fun TabIcon(tab: TabItem) {
    Icon(
        tab.icon,
        contentDescription = tab.label,
        modifier = Modifier.size(NnoworkSize.icon),
    )
}

@Composable
private fun TabLabel(tab: TabItem, selected: Boolean) {
    Text(
        tab.label,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        ),
    )
}

/** medium／expanded 的左側導覽：沿用底欄的 tab 與配色，項目垂直置中（拇指在兩側都搆得到）。 */
@Composable
private fun NnoworkNavigationRail(selected: Int, onSelect: (Int) -> Unit) {
    NavigationRail(
        containerColor = NnoworkTheme.colors.tabBarBG,
        modifier = Modifier.fillMaxHeight(),
    ) {
        Spacer(Modifier.weight(1f))
        tabs.forEachIndexed { index, tab ->
            NavigationRailItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = { TabIcon(tab) },
                label = { TabLabel(tab, selected == index) },
                modifier = Modifier.padding(vertical = NnoworkSpacing.xxs),
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = NnoworkTheme.colors.tabSelected,
                    unselectedIconColor = NnoworkTheme.colors.tabUnselected,
                    selectedTextColor = NnoworkTheme.colors.tabSelected,
                    unselectedTextColor = NnoworkTheme.colors.tabUnselected,
                    indicatorColor = NnoworkTheme.colors.tabIndicator,
                ),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

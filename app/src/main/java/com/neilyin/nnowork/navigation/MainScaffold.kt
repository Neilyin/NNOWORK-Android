package com.neilyin.nnowork.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.neilyin.nnowork.feature.history.HistoryScreen
import com.neilyin.nnowork.feature.home.HomeScreen
import com.neilyin.nnowork.feature.plan.PlanScreen
import com.neilyin.nnowork.feature.settings.SettingsScreen
import com.neilyin.nnowork.feature.treehole.TreeHoleScreen
import com.neilyin.nnowork.ui.theme.PixelColors
import com.neilyin.nnowork.ui.theme.pixelTextStyle

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("集點卡", Icons.Filled.GridView),
    TabItem("計畫", Icons.Filled.ListAlt),
    TabItem("紀錄", Icons.Filled.Book),
    TabItem("樹洞", Icons.Filled.Park),
    TabItem("設定", Icons.Filled.Settings),
)

@Composable
fun MainScaffold() {
    var selected by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = PixelColors.darkGray,
        bottomBar = {
            NavigationBar(containerColor = PixelColors.darkGray) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, style = pixelTextStyle(10.sp)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PixelColors.white,
                            unselectedIconColor = PixelColors.gray,
                            selectedTextColor = PixelColors.white,
                            unselectedTextColor = PixelColors.gray,
                            indicatorColor = PixelColors.blue.copy(alpha = 0.3f),
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selected) {
                0 -> HomeScreen()
                1 -> PlanScreen()
                2 -> HistoryScreen()
                3 -> TreeHoleScreen()
                4 -> SettingsScreen()
            }
        }
    }
}

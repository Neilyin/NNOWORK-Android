package com.neilyin.nnowork.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color

/**
 * 薪水計時器桌面 Widget（Glance）。目前為基礎版本，
 * 完整即時收入計算與排班整合留待 Task #9 / #5。
 */
class SalaryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "💰 薪水計時器",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFF4D03F)),
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    "打開 App 設定薪資",
                    style = TextStyle(color = ColorProvider(Color(0xFFFDFEFE))),
                )
            }
        }
    }
}

class SalaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SalaryWidget()
}

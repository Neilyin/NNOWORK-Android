package com.neilyin.nnowork.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.neilyin.nnowork.data.plan.PlanRepository
import com.neilyin.nnowork.data.plan.SalaryCalculator
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.appWideTheme
import com.neilyin.nnowork.ui.theme.palette

/**
 * 薪水計時器桌面 Widget（Glance）。金額為快照，依系統與 WidgetRefreshWorker 定時刷新。
 */
class SalaryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val plan = PlanRepository(context).current()
        val settings = SettingsRepository(context).current()
        val palette = CardTheme.fromId(settings.cardStyle)
            .appWideTheme(settings.unlockedStyles)
            .palette
        val snapshot = SalaryCalculator.snapshot(plan, System.currentTimeMillis())
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(palette.chrome).padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!snapshot.configured) {
                        Text("💰 薪水計時器", style = TextStyle(color = ColorProvider(palette.accent), fontWeight = FontWeight.Bold))
                        Text("打開 App 設定薪資", style = TextStyle(color = ColorProvider(palette.onChrome)), modifier = GlanceModifier.padding(top = 6.dp))
                    } else {
                        Text("今日已賺", style = TextStyle(color = ColorProvider(palette.onChrome.copy(alpha = 0.72f))))
                        Text(
                            SalaryCalculator.formatMoney(plan, snapshot.todayEarned),
                            style = TextStyle(color = ColorProvider(palette.primary), fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier.padding(top = 2.dp),
                        )
                        LinearProgressIndicator(
                            progress = snapshot.progress,
                            modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
                            color = ColorProvider(palette.accent),
                            backgroundColor = ColorProvider(palette.onChrome.copy(alpha = 0.18f)),
                        )
                        Text(
                            "本期 ${SalaryCalculator.formatMoney(plan, snapshot.cycleEarned)}",
                            style = TextStyle(color = ColorProvider(palette.onChrome)),
                        )
                        if (snapshot.phase == SalaryCalculator.Phase.WORKING) {
                            Text(
                                "剩 ${SalaryCalculator.formatCountdown(snapshot.countdownMillis)}",
                                style = TextStyle(color = ColorProvider(palette.accent), fontWeight = FontWeight.Bold),
                                modifier = GlanceModifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

class SalaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SalaryWidget()
}

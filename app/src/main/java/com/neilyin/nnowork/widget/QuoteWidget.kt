package com.neilyin.nnowork.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.neilyin.nnowork.data.QuotesProvider
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.appWideTheme
import com.neilyin.nnowork.ui.theme.palette
import java.util.concurrent.TimeUnit

/** 離職語錄桌面 Widget（Glance）。語錄依目前刷新週期挑選，定時由 WidgetRefreshWorker 更新。 */
class QuoteWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = SettingsRepository(context).current()
        val palette = CardTheme.fromId(settings.cardStyle)
            .appWideTheme(settings.unlockedStyles)
            .palette
        val interval = settings.widgetRefreshInterval.coerceAtLeast(15).toLong()
        val seed = System.currentTimeMillis() / TimeUnit.MINUTES.toMillis(interval)
        val quote = QuotesProvider.quoteFor(context, seed)
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(palette.chrome).padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "今天又想離職了？",
                        style = TextStyle(color = ColorProvider(palette.accent), fontWeight = FontWeight.Bold),
                    )
                    Text(
                        quote,
                        style = TextStyle(color = ColorProvider(palette.onChrome), textAlign = TextAlign.Center),
                        modifier = GlanceModifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuoteWidget()
}

package com.neilyin.nnowork.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.SystemClock
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.neilyin.nnowork.MainActivity
import com.neilyin.nnowork.R
import com.neilyin.nnowork.data.plan.PlanRepository
import com.neilyin.nnowork.data.plan.ResignationPlan
import com.neilyin.nnowork.data.plan.SalaryCalculator
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.NnoworkColors
import com.neilyin.nnowork.ui.theme.appWideTheme
import com.neilyin.nnowork.ui.theme.nnoworkColors

/**
 * 牛馬／薪水計時器 Widget（對應 iOS NiuMaWidget；小 = systemSmall、寬 = systemMedium）。
 *
 * 另有舊的 [SalaryWidget]（對應 iOS 薪水 Widget）。兩者並存、互不取代：iOS 同樣有兩個獨立 Widget。
 *
 * 狀態選擇是純函式 [NiuMaWidgetState.select]；本檔只負責渲染與排程。
 * 倒數／手動計時用嵌入的 Chronometer 由系統每秒自走；金額為快照，上班中每分鐘由 [NiuMaRefresh] 重繪。
 */
class NiuMaWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SmallSize, WideSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val planRepo = PlanRepository(context)
        val settingsRepo = SettingsRepository(context)
        val initialPlan = planRepo.current()
        val initialSettings = settingsRepo.current()
        val openApp = Intent(context, MainActivity::class.java)

        // Glance session 存活期間（約 45 秒）再呼叫 update 不會重跑 provideGlance，只會重組。
        // 所以資料與「現在時間」都要在組合內讀：設定／計時變動走 Flow，定時重繪靠 glance state 的 tick。
        provideContent {
            val plan by planRepo.plan.collectAsState(initialPlan)
            val settings by settingsRepo.settings.collectAsState(initialSettings)
            val tick = currentState(TickKey)
            val now = remember(plan, settings, tick) { System.currentTimeMillis() }
            val state = remember(plan, now) { NiuMaWidgetState.select(plan, now) }
            LaunchedEffect(state, now) { NiuMaRefresh.scheduleNext(context, state, now) }
            val theme = CardTheme.fromId(settings.cardStyle).appWideTheme(settings.unlockedStyles)
            val tokens = NiuMaTokens.forTheme(theme)
            val night = (LocalContext.current.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            GlanceTheme {
                val wide = LocalSize.current.width >= WideThreshold
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(tokens.color { it.appBG })
                        .cornerRadius(20.dp)
                        .clickable(actionStartActivity(openApp))
                        .padding(if (wide) 16.dp else 14.dp),
                ) {
                    NiuMaContent(context, plan, state, tokens, if (night) tokens.nightColors else tokens.dayColors, wide, now)
                }
            }
        }
    }

    companion object {
        private val TickKey = longPreferencesKey("niuma_tick")

        /** 寫入新的 tick 再 update：session 還活著時也能逼它用新時間重組。 */
        suspend fun refreshAll(context: Context) {
            val widget = NiuMaWidget()
            val ids = GlanceAppWidgetManager(context).getGlanceIds(NiuMaWidget::class.java)
            for (id in ids) {
                runCatching {
                    updateAppWidgetState(context, id) { it[TickKey] = System.currentTimeMillis() }
                    widget.update(context, id)
                }
            }
        }

        val SmallSize = DpSize(110.dp, 110.dp)
        val WideSize = DpSize(250.dp, 110.dp)
        private val WideThreshold = 200.dp
    }
}

class NiuMaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NiuMaWidget()

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        NiuMaRefresh.cancel(context)
    }
}

/**
 * 日／夜兩組語意色。使用者主題若是淺色，夜間改用「夜空」；若是深色，白天改用「Notion」，
 * 確保跟隨系統日夜模式且兩邊對比都 ≥ 4.5:1（token 本身即已鎖定）。
 */
internal class NiuMaTokens(val dayColors: NnoworkColors, val nightColors: NnoworkColors) {
    fun color(pick: (NnoworkColors) -> Color) = ColorProvider(day = pick(dayColors), night = pick(nightColors))

    companion object {
        fun forTheme(theme: CardTheme): NiuMaTokens {
            val own = theme.nnoworkColors
            return if (theme.isLightTheme) {
                NiuMaTokens(own, CardTheme.NIGHT.nnoworkColors)
            } else {
                NiuMaTokens(CardTheme.NOTION.nnoworkColors, own)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun NiuMaContent(
    context: Context,
    plan: ResignationPlan,
    state: NiuMaState,
    tokens: NiuMaTokens,
    resolved: NnoworkColors,
    wide: Boolean,
    now: Long,
) {
    fun money(value: Double) = SalaryCalculator.formatMoney(plan, value)
    when (state) {
        NiuMaState.NotConfigured -> StatusBody(
            "🐂", context.getString(R.string.niuma_name), context.getString(R.string.niuma_not_configured), tokens,
        )
        is NiuMaState.OffWork -> StatusBody(
            "🎉", context.getString(R.string.niuma_off_work),
            context.getString(R.string.niuma_off_work_detail, money(state.dailyWage)), tokens,
        )
        is NiuMaState.BeforeWork -> StatusBody(
            "😴", context.getString(R.string.niuma_before_work), context.getString(R.string.niuma_before_work_detail), tokens,
        )
        is NiuMaState.RestDay -> StatusBody(
            if (state.paidAmount > 0) "🛋️" else "🏖️",
            context.getString(R.string.niuma_rest_day),
            if (state.paidAmount > 0) context.getString(R.string.niuma_rest_paid, money(state.paidAmount))
            else context.getString(R.string.niuma_rest_free),
            tokens,
        )
        is NiuMaState.Holiday -> StatusBody(
            "🎌", context.getString(R.string.niuma_holiday),
            context.getString(R.string.niuma_holiday_detail, money(state.dailyWage)), tokens,
        )
        is NiuMaState.Working -> WorkingBody(context, ::money, state, tokens, resolved, wide, now)
        is NiuMaState.Manual -> ManualBody(context, ::money, state, tokens, resolved, wide, now)
    }
}

@androidx.compose.runtime.Composable
private fun StatusBody(emoji: String, title: String, detail: String, tokens: NiuMaTokens) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = TextStyle(fontSize = 30.sp))
        Text(
            title,
            style = TextStyle(color = tokens.color { it.primaryText }, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.padding(top = 4.dp),
        )
        Text(
            detail,
            style = TextStyle(color = tokens.color { it.secondaryText }, fontSize = 11.sp, textAlign = TextAlign.Center),
            modifier = GlanceModifier.padding(top = 2.dp),
        )
    }
}

@androidx.compose.runtime.Composable
private fun WorkingBody(
    context: Context,
    money: (Double) -> String,
    state: NiuMaState.Working,
    tokens: NiuMaTokens,
    resolved: NnoworkColors,
    wide: Boolean,
    now: Long,
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                "🐂 ${context.getString(R.string.niuma_earned_today)}",
                style = TextStyle(color = tokens.color { it.secondaryText }, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (wide) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        context.getString(R.string.niuma_cycle_total),
                        style = TextStyle(color = tokens.color { it.secondaryText }, fontSize = 10.sp),
                    )
                    Text(
                        money(state.cycleEarned),
                        style = TextStyle(color = tokens.color { it.primaryText }, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
        Text(
            money(state.earned),
            style = TextStyle(
                color = tokens.color { it.success },
                fontSize = if (wide) 40.sp else 30.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        LinearProgressIndicator(
            progress = state.progress.coerceIn(0f, 1f),
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 6.dp),
            color = tokens.color { it.success },
            backgroundColor = tokens.color { it.divider },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                context.getString(R.string.niuma_until_off),
                style = TextStyle(color = tokens.color { it.secondaryText }, fontSize = 12.sp),
            )
            Spacer(GlanceModifier.width(6.dp))
            Chronometer(context, state.countdownMillis, countDown = true, color = resolved.accentForeground, sizeSp = if (wide) 19f else 17f)
        }
    }
}

@androidx.compose.runtime.Composable
private fun ManualBody(
    context: Context,
    money: (Double) -> String,
    state: NiuMaState.Manual,
    tokens: NiuMaTokens,
    resolved: NnoworkColors,
    wide: Boolean,
    now: Long,
) {
    val running = state.status == NiuMaState.ManualStatus.RUNNING
    val label = when (state.status) {
        NiuMaState.ManualStatus.RUNNING -> context.getString(R.string.niuma_running)
        NiuMaState.ManualStatus.PAUSED -> context.getString(R.string.niuma_paused)
        NiuMaState.ManualStatus.STOPPED -> context.getString(R.string.niuma_elapsed)
    }
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                "🐂 $label",
                style = TextStyle(
                    color = if (running) tokens.color { it.success } else tokens.color { it.secondaryText },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (wide) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        context.getString(R.string.niuma_cycle_total),
                        style = TextStyle(color = tokens.color { it.secondaryText }, fontSize = 10.sp),
                    )
                    Text(
                        money(state.cycleEarned),
                        style = TextStyle(color = tokens.color { it.primaryText }, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
        Spacer(GlanceModifier.padding(top = 4.dp))
        if (running) {
            Chronometer(context, state.elapsedMillis, countDown = false, color = resolved.primaryText, sizeSp = 22f)
        } else {
            Text(
                formatElapsed(state.elapsedMillis),
                style = TextStyle(color = tokens.color { it.primaryText }, fontSize = 22.sp, fontWeight = FontWeight.Bold),
            )
        }
        Text(
            money(state.earned),
            style = TextStyle(color = tokens.color { it.success }, fontSize = if (wide) 36.sp else 28.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.padding(top = 2.dp),
        )
    }
}

internal fun formatElapsed(millis: Long): String = SalaryCalculator.formatCountdown(millis)

/**
 * 以 AndroidRemoteViews 嵌入原生 Chronometer。[valueMillis] 為倒數剩餘（countDown）或已經過時間（正數計時），
 * 系統每秒自行更新文字，不需要 Widget 重繪。
 */
@androidx.compose.runtime.Composable
private fun Chronometer(
    context: Context,
    valueMillis: Long,
    countDown: Boolean,
    color: Color,
    sizeSp: Float,
) {
    val views = RemoteViews(context.packageName, R.layout.niuma_chronometer)
    val base = if (countDown) SystemClock.elapsedRealtime() + valueMillis else SystemClock.elapsedRealtime() - valueMillis
    views.setChronometerCountDown(R.id.niuma_chronometer, countDown)
    views.setChronometer(R.id.niuma_chronometer, base, null, true)
    views.setTextColor(R.id.niuma_chronometer, color.toArgb())
    views.setTextViewTextSize(R.id.niuma_chronometer, TypedValue.COMPLEX_UNIT_SP, sizeSp)
    AndroidRemoteViews(views)
}

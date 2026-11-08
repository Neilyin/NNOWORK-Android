package com.neilyin.nnowork.feature.jobarchive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.neilyin.nnowork.R
import com.neilyin.nnowork.data.backup.BackupDateCodec
import com.neilyin.nnowork.data.jobarchive.JobArchiveEntity
import com.neilyin.nnowork.data.jobarchive.JobArchivePayload
import com.neilyin.nnowork.data.jobarchive.JobArchivedStamp
import com.neilyin.nnowork.data.jobarchive.JobReceiptSnapshot
import com.neilyin.nnowork.data.jobarchive.JobSalarySnapshot
import com.neilyin.nnowork.ui.theme.NnoworkLine
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.PixelBadge
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelButtonVariant
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 過往工作的顯示格式（iOS JobArchiveFormat）。 */
object JobArchiveFormat {
    private fun dateFormat() = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)

    fun date(millis: Long): String = dateFormat().format(Date(millis))

    fun date(iso: String?): String? = iso?.let { runCatching { date(BackupDateCodec.parse(it)) }.getOrNull() }

    fun number(value: Double): String = NumberFormat.getIntegerInstance(Locale.TAIWAN).apply {
        maximumFractionDigits = 0
    }.format(value)

    fun clock(minutes: Int): String = String.format(Locale.ROOT, "%02d:%02d", (minutes / 60) % 24, minutes % 60)

    /** 動態回顧的順序（iOS 依封存快照的順序，即蓋章時間由舊到新；同一張卡自然連在一起）。 */
    fun timelineOrder(stamps: List<JobArchivedStamp>): List<JobArchivedStamp> =
        stamps.sortedBy { runCatching { BackupDateCodec.parse(it.date) }.getOrDefault(0L) }
}

@Composable
private fun periodText(startIso: String?, endIso: String): String {
    val end = JobArchiveFormat.date(endIso).orEmpty()
    val start = JobArchiveFormat.date(startIso) ?: return end
    return stringResource(R.string.jobarchive_period, start, end)
}

@Composable
private fun companyTitle(name: String): String = name.ifBlank { stringResource(R.string.jobarchive_company_fallback) }

/**
 * 「離職紀錄」上方的「過往工作 · N 份」區塊（iOS PastJobsSection）。
 * 只顯示目前登入帳號的封存（由呼叫端以 visibleArchives 過濾）；沒有封存時呼叫端不顯示。
 */
@Composable
fun PastJobsSection(
    archives: List<JobArchiveEntity>,
    onSelect: (JobArchiveEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Inventory2, null, tint = NnoworkTheme.colors.accentForeground, modifier = Modifier.size(NnoworkSize.compactIcon))
            Spacer(Modifier.size(NnoworkSpacing.xs))
            Text(
                stringResource(R.string.jobarchive_past_jobs),
                style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.jobarchive_past_jobs_count, archives.size),
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Medium, NnoworkTheme.colors.secondaryText),
            )
        }
        archives.forEach { archive ->
            PastJobRow(archive = archive, onClick = { onSelect(archive) })
        }
    }
}

@Composable
private fun PastJobRow(archive: JobArchiveEntity, onClick: () -> Unit) {
    val payload = remember(archive.payloadJson) { archive.payload }
    val isMaster = payload?.receipt?.kind == JobReceiptSnapshot.KIND_MASTER
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(NnoworkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isMaster) Icons.Filled.EmojiEvents else Icons.Filled.Business,
            null,
            tint = if (isMaster) NnoworkTheme.colors.special else NnoworkTheme.colors.info,
            modifier = Modifier.size(NnoworkSize.icon),
        )
        Spacer(Modifier.size(NnoworkSpacing.cozy))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs)) {
            Text(
                companyTitle(payload?.companyName ?: archive.companyName),
                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                payload?.let { periodText(it.startDate, it.endDate) } ?: JobArchiveFormat.date(archive.endDate),
                style = pixelTextStyle(NnoworkTheme.type.caption, color = NnoworkTheme.colors.secondaryText),
            )
        }
        Text(
            stringResource(R.string.jobarchive_stamp_count, payload?.stampCount ?: 0),
            style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText),
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = NnoworkTheme.colors.secondaryText)
    }
}

/** 單份工作的封存詳情（iOS JobArchiveDetailView，唯讀）。內容一律以封存快照為準。 */
@Composable
fun JobArchiveDetailScreen(archive: JobArchiveEntity, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val payload = remember(archive.payloadJson) { archive.payload }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = NnoworkSpacing.page, vertical = NnoworkSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.lg),
    ) {
        item(key = "nav") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelButton(
                    onClick = onBack,
                    variant = PixelButtonVariant.Tonal,
                    modifier = Modifier.size(NnoworkSize.minTouchTarget),
                    contentPadding = PaddingValues(NnoworkSpacing.xs),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.jobarchive_back), tint = NnoworkTheme.colors.primaryText)
                }
                Spacer(Modifier.size(NnoworkSpacing.sm))
                Text(
                    companyTitle(payload?.companyName ?: archive.companyName),
                    style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (payload == null) {
            item(key = "unreadable") {
                Text(stringResource(R.string.jobarchive_no_stamps), style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.secondaryText))
            }
            return@LazyColumn
        }
        item(key = "header") { DetailHeader(payload) }
        item(key = "stats") {
            Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.md)) {
                StatCard(Icons.Filled.CheckCircle, payload.stampCount.toString(), stringResource(R.string.jobarchive_total_stamps), NnoworkTheme.colors.info, Modifier.weight(1f))
                StatCard(Icons.Filled.ViewModule, payload.cardCount.toString(), stringResource(R.string.jobarchive_cards_used), NnoworkTheme.colors.warning, Modifier.weight(1f))
            }
        }
        if (payload.topReasons.isNotEmpty()) {
            item(key = "reasons") {
                DetailSection(Icons.Filled.LocalFireDepartment, stringResource(R.string.jobarchive_top_reasons)) {
                    payload.topReasons.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(thickness = NnoworkLine.hairline, color = NnoworkTheme.colors.divider)
                        Row(Modifier.padding(vertical = NnoworkSpacing.snug), verticalAlignment = Alignment.CenterVertically) {
                            PixelBadge(backgroundColor = if (index == 0) NnoworkTheme.colors.danger else NnoworkTheme.colors.celebrateYellow) {
                                Text("${index + 1}", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText, monospace = true))
                            }
                            Spacer(Modifier.size(NnoworkSpacing.sm))
                            Text(
                                item.reason,
                                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, NnoworkTheme.colors.primaryText),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(stringResource(R.string.jobarchive_times, item.count), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
                        }
                    }
                }
            }
        }
        item(key = "savings") {
            DetailSection(Icons.Filled.AttachMoney, stringResource(R.string.jobarchive_money_saved)) {
                Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug)) {
                    InfoRow(stringResource(R.string.jobarchive_freedom_fund), "$" + JobArchiveFormat.number(payload.savedAmount), highlight = true)
                    if (payload.targetAmount > 0) {
                        InfoRow(stringResource(R.string.jobarchive_target_amount), "$" + JobArchiveFormat.number(payload.targetAmount))
                        LinearProgressIndicator(
                            progress = { (payload.savedAmount / payload.targetAmount).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = NnoworkTheme.colors.success,
                            trackColor = NnoworkTheme.colors.disabledBG,
                            // 不畫 M3 尾端的停止點（iOS ProgressView 沒有）。
                            drawStopIndicator = {},
                        )
                    }
                }
            }
        }
        item(key = "salary") {
            DetailSection(Icons.Filled.Payments, stringResource(R.string.jobarchive_salary_then)) {
                SalaryContent(payload.salary)
            }
        }
        item(key = "receipt") {
            DetailSection(Icons.AutoMirrored.Filled.ReceiptLong, stringResource(R.string.jobarchive_receipt), inCard = false) {
                ArchivedReceiptCard(payload.receipt)
            }
        }
        if (payload.todos.isNotEmpty()) {
            item(key = "todos") {
                DetailSection(Icons.Filled.Checklist, stringResource(R.string.jobarchive_todos)) {
                    Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
                        payload.todos.forEach { todo ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    if (todo.isCompleted) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                    null,
                                    tint = if (todo.isCompleted) NnoworkTheme.colors.success else NnoworkTheme.colors.secondaryText,
                                    modifier = Modifier.size(NnoworkSize.compactIcon),
                                )
                                Spacer(Modifier.size(NnoworkSpacing.snug))
                                Text(
                                    todo.title,
                                    style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.primaryText).let {
                                        if (todo.isCompleted) it.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else it
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        item(key = "timeline") {
            DetailSection(Icons.Filled.History, stringResource(R.string.jobarchive_timeline)) {
                Timeline(payload.stamps)
            }
        }
    }
}

@Composable
private fun DetailHeader(payload: JobArchivePayload) {
    Column(
        modifier = Modifier.fillMaxWidth().pixelCard().padding(NnoworkSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
    ) {
        Text(stringResource(R.string.jobarchive_job_number, payload.jobIndex), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
        Text(companyTitle(payload.companyName), style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, null, tint = NnoworkTheme.colors.secondaryText, modifier = Modifier.size(NnoworkSize.compactIcon))
            Spacer(Modifier.size(NnoworkSpacing.tight))
            Text(periodText(payload.startDate, payload.endDate), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Medium, NnoworkTheme.colors.secondaryText))
        }
        PixelBadge(backgroundColor = NnoworkTheme.colors.accentSoft) {
            Text(stringResource(R.string.jobarchive_read_only), style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.pixelCard().padding(NnoworkSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(NnoworkSize.icon))
        Text(value, style = pixelTextStyle(NnoworkTheme.type.pageTitle, FontWeight.Bold, NnoworkTheme.colors.primaryText, monospace = true))
        Text(label, style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
    }
}

@Composable
private fun DetailSection(
    icon: ImageVector,
    title: String,
    inCard: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = NnoworkTheme.colors.accentForeground, modifier = Modifier.size(NnoworkSize.compactIcon))
            Spacer(Modifier.size(NnoworkSpacing.xs))
            Text(title, style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        }
        if (inCard) {
            Column(
                modifier = Modifier.fillMaxWidth().pixelCard().padding(NnoworkSpacing.md),
                content = content,
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, highlight: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Medium, NnoworkTheme.colors.secondaryText), modifier = Modifier.weight(1f))
        Text(
            value,
            style = pixelTextStyle(
                NnoworkTheme.type.body,
                FontWeight.Bold,
                if (highlight) NnoworkTheme.colors.success else NnoworkTheme.colors.primaryText,
                monospace = true,
            ),
        )
    }
}

@Composable
private fun SalaryContent(salary: JobSalarySnapshot?) {
    if (salary == null) {
        Text(stringResource(R.string.jobarchive_no_salary), style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
        return
    }
    val hourly = salary.payMode == 1
    Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug)) {
        InfoRow(
            stringResource(R.string.jobarchive_pay_type),
            if (hourly) {
                stringResource(R.string.jobarchive_hourly_mode) +
                    if (salary.hourlyManualMode) " · " + stringResource(R.string.jobarchive_manual_timer) else ""
            } else {
                stringResource(R.string.jobarchive_monthly_mode)
            },
        )
        InfoRow(
            stringResource(if (hourly) R.string.jobarchive_hourly_rate else R.string.jobarchive_monthly_salary),
            salary.currencySymbol + JobArchiveFormat.number(if (hourly) salary.hourlyRate else salary.monthlySalary),
            highlight = true,
        )
        if (!(hourly && salary.hourlyManualMode)) {
            InfoRow(stringResource(R.string.jobarchive_work_start), JobArchiveFormat.clock(salary.startMinutes))
            InfoRow(stringResource(R.string.jobarchive_work_end), JobArchiveFormat.clock(salary.endMinutes))
        }
    }
}

@Composable
private fun Timeline(stamps: List<JobArchivedStamp>) {
    if (stamps.isEmpty()) {
        Text(stringResource(R.string.jobarchive_no_stamps), style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
        return
    }
    val ordered = remember(stamps) { JobArchiveFormat.timelineOrder(stamps) }
    Column {
        ordered.forEachIndexed { index, stamp ->
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Column(
                    modifier = Modifier.width(NnoworkSpacing.sm).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(NnoworkSpacing.xxs))
                    Box(Modifier.size(NnoworkSpacing.sm).background(roundColor(stamp.roundNumber), CircleShape))
                    if (index < ordered.lastIndex) {
                        Box(Modifier.width(NnoworkSize.timelineConnector).weight(1f).background(NnoworkTheme.colors.divider))
                    }
                }
                Spacer(Modifier.size(NnoworkSpacing.sm))
                Column(
                    modifier = Modifier.padding(bottom = NnoworkSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight)) {
                        Text(JobArchiveFormat.date(stamp.date).orEmpty(), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                        Text(stringResource(R.string.jobarchive_card_number, stamp.roundNumber), style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Medium, NnoworkTheme.colors.secondaryText))
                        if (stamp.isEasterEgg) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = NnoworkTheme.colors.celebrateYellow, modifier = Modifier.size(NnoworkSpacing.sm))
                        }
                    }
                    Text(stamp.reason, style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, NnoworkTheme.colors.primaryText))
                }
            }
        }
    }
}

/** 依封存快照重畫的唯讀收據（版面比照離職收據／忍耐大師收據）。 */
@Composable
private fun ArchivedReceiptCard(receipt: JobReceiptSnapshot) {
    val master = receipt.kind == JobReceiptSnapshot.KIND_MASTER
    val company = companyTitle(receipt.companyName)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard(backgroundColor = NnoworkTheme.colors.elevatedSurface)
            .padding(horizontal = NnoworkSpacing.page, vertical = NnoworkSpacing.page),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.roomy),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
            Text(
                stringResource(if (master) R.string.jobarchive_receipt_master_title else R.string.jobarchive_receipt),
                style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, if (master) NnoworkTheme.colors.danger else NnoworkTheme.colors.onElevated),
                textAlign = TextAlign.Center,
            )
            Text(company, style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, NnoworkTheme.colors.secondaryText))
        }
        ReceiptDivider()
        Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
            ReceiptRow(
                null,
                stringResource(if (master) R.string.jobarchive_receipt_from else R.string.jobarchive_receipt_start_date),
                JobArchiveFormat.date(receipt.firstStampDate) ?: "—",
            )
            ReceiptRow(
                null,
                stringResource(if (master) R.string.jobarchive_receipt_to else R.string.jobarchive_receipt_end_date),
                JobArchiveFormat.date(receipt.completionDate).orEmpty(),
            )
        }
        ReceiptDivider()
        Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug)) {
            ReceiptRow(
                if (master) Icons.Filled.Schedule else Icons.Filled.CalendarMonth,
                stringResource(if (master) R.string.jobarchive_receipt_endured_for else R.string.jobarchive_receipt_days_endured),
                stringResource(R.string.jobarchive_receipt_days, receipt.totalDays),
            )
            ReceiptRow(
                Icons.Filled.Verified,
                stringResource(if (master) R.string.jobarchive_receipt_stamps_collected else R.string.jobarchive_receipt_stamp_total),
                stringResource(R.string.jobarchive_stamp_count, receipt.totalStamps),
            )
            ReceiptRow(
                Icons.Filled.AttachMoney,
                stringResource(if (master) R.string.jobarchive_money_saved else R.string.jobarchive_freedom_fund),
                "$" + JobArchiveFormat.number(receipt.savedAmount),
                highlight = true,
            )
        }
        ReceiptDivider()
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight)) {
            if (master) {
                Text(stringResource(R.string.jobarchive_receipt_still_holding), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Medium, NnoworkTheme.colors.secondaryText))
                Text(
                    stringResource(R.string.jobarchive_receipt_model_employee, company),
                    style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.special),
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(stringResource(R.string.jobarchive_receipt_thanks), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Medium, NnoworkTheme.colors.secondaryText))
                Text(stringResource(R.string.jobarchive_receipt_good_luck), style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.info))
            }
        }
        Text(
            stringResource(R.string.jobarchive_receipt_footer, JobArchiveFormat.date(receipt.issuedAt).orEmpty()),
            style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Medium, NnoworkTheme.colors.secondaryText),
        )
    }
}

@Composable
private fun ReceiptRow(icon: ImageVector?, label: String, value: String, highlight: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = if (highlight) NnoworkTheme.colors.success else NnoworkTheme.colors.secondaryText, modifier = Modifier.size(NnoworkSize.compactIcon))
            Spacer(Modifier.size(NnoworkSpacing.xs))
        }
        Text(label, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Medium, NnoworkTheme.colors.onElevated), modifier = Modifier.weight(1f))
        Text(
            value,
            style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, if (highlight) NnoworkTheme.colors.success else NnoworkTheme.colors.onElevated),
        )
    }
}

@Composable
private fun ReceiptDivider() {
    Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs), modifier = Modifier.fillMaxWidth()) {
        repeat(18) {
            Spacer(
                Modifier
                    .weight(1f)
                    .height(NnoworkLine.control)
                    .background(NnoworkTheme.colors.divider, RoundedCornerShape(NnoworkRadius.small)),
            )
        }
    }
}

@Composable
private fun roundColor(round: Int): Color = when (round) {
    1 -> NnoworkTheme.colors.info
    2 -> NnoworkTheme.colors.warning
    3 -> NnoworkTheme.colors.special
    else -> NnoworkTheme.colors.secondaryText
}

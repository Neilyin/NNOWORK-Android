package com.neilyin.nnowork.feature.treehole

import androidx.compose.foundation.background
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkLine
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neilyin.nnowork.R
import com.neilyin.nnowork.data.treehole.TreeHoleBlockPolicy
import com.neilyin.nnowork.data.treehole.TreeHoleReportReason
import com.neilyin.nnowork.data.treehole.TreeHoleReportTargetType
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.pixelTextStyle

@Immutable
data class TreeHoleSafetyTarget(
    val targetType: TreeHoleReportTargetType,
    val targetId: String,
    val authorKey: String?,
    val nickname: String,
    val preview: String,
) {
    val canBlock: Boolean
        get() = TreeHoleBlockPolicy.normalizeAuthorKey(authorKey) != null
}

@Composable
fun TreeHoleSafetyActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PixelButton(
        onClick = onClick,
        backgroundColor = NnoworkTheme.colors.elevatedSurface,
        modifier = modifier.size(NnoworkSize.minTouchTarget),
        contentPadding = PaddingValues(NnoworkSpacing.xs),
    ) {
        Icon(
            Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.treehole_safety_more_actions),
            tint = NnoworkTheme.colors.primaryText,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
fun TreeHoleContentActionsDialog(
    target: TreeHoleSafetyTarget,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                stringResource(R.string.treehole_safety_title),
                style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.textOnDark),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
                Text(
                    target.preview,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
                )
                PixelButton(
                    onClick = onReport,
                    backgroundColor = NnoworkTheme.colors.highlight,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Flag, contentDescription = null, tint = NnoworkTheme.colors.primaryText)
                    Spacer(Modifier.size(7.dp))
                    Text(
                        stringResource(
                            if (target.targetType == TreeHoleReportTargetType.POST) {
                                R.string.treehole_report_post
                            } else {
                                R.string.treehole_report_comment
                            },
                        ),
                        style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                    )
                }
                PixelButton(
                    onClick = onBlock,
                    enabled = target.canBlock,
                    backgroundColor = if (target.canBlock) NnoworkTheme.colors.danger else NnoworkTheme.colors.disabledBG,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Block, contentDescription = null, tint = NnoworkTheme.colors.textOnDark)
                    Spacer(Modifier.size(7.dp))
                    Text(
                        stringResource(
                            if (target.canBlock) R.string.treehole_block_author
                            else R.string.treehole_block_unavailable,
                        ),
                        style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.textOnDark),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            PixelButton(onClick = onDismiss, backgroundColor = NnoworkTheme.colors.disabledBG) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = NnoworkTheme.colors.primaryText)
                Spacer(Modifier.size(5.dp))
                Text(
                    stringResource(R.string.treehole_action_close),
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
            }
        },
    )
}

@Composable
fun TreeHoleReportDialog(
    target: TreeHoleSafetyTarget,
    submitting: Boolean,
    errorMessage: String?,
    onSubmit: (TreeHoleReportReason, String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember(target.targetId) { mutableStateOf(TreeHoleReportReason.OFFENSIVE) }
    var note by remember(target.targetId) { mutableStateOf("") }
    var alsoBlock by remember(target.targetId) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                stringResource(
                    if (target.targetType == TreeHoleReportTargetType.POST) {
                        R.string.treehole_report_post_title
                    } else {
                        R.string.treehole_report_comment_title
                    },
                ),
                style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.textOnDark),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug),
            ) {
                Text(
                    stringResource(R.string.treehole_report_reason),
                    style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.textOnDark),
                )
                TreeHoleReportReason.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = !submitting,
                                role = Role.RadioButton,
                                onClick = { reason = option },
                            )
                            .background(NnoworkTheme.colors.elevatedSurface)
                            .border(NnoworkLine.control, NnoworkTheme.colors.border, RectangleShape)
                            .padding(horizontal = NnoworkSpacing.xs, vertical = NnoworkSpacing.xxs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = reason == option, onClick = null)
                        Text(
                            reportReasonLabel(option),
                            style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                        )
                    }
                }
                Text(
                    stringResource(R.string.treehole_report_note),
                    style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.textOnDark),
                )
                BasicTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    enabled = !submitting,
                    textStyle = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.primaryText),
                    cursorBrush = SolidColor(NnoworkTheme.colors.primaryText),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(NnoworkTheme.colors.elevatedSurface)
                        .border(NnoworkLine.control, NnoworkTheme.colors.border, RectangleShape)
                        .padding(NnoworkSpacing.snug),
                    decorationBox = { inner ->
                        if (note.isEmpty()) {
                            Text(
                                stringResource(R.string.treehole_report_note_placeholder),
                                style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
                            )
                        }
                        inner()
                    },
                )
                Text(
                    stringResource(R.string.treehole_report_note_count, note.length),
                    style = pixelTextStyle(NnoworkTheme.type.caption, color = NnoworkTheme.colors.secondaryText),
                )
                if (target.canBlock) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !submitting) { alsoBlock = !alsoBlock },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = alsoBlock, onCheckedChange = null)
                        Text(
                            stringResource(R.string.treehole_report_also_block),
                            style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.textOnDark),
                        )
                    }
                }
                Text(
                    stringResource(R.string.treehole_report_review_24h),
                    style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.highlight),
                )
                errorMessage?.let {
                    Text(
                        it,
                        style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.danger),
                    )
                }
            }
        },
        confirmButton = {
            PixelButton(
                onClick = { onSubmit(reason, note, alsoBlock) },
                enabled = !submitting,
                backgroundColor = NnoworkTheme.colors.highlight,
            ) {
                Text(
                    stringResource(
                        if (submitting) R.string.treehole_report_submitting
                        else R.string.treehole_report_submit,
                    ),
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
            }
        },
        dismissButton = {
            PixelButton(onClick = onDismiss, enabled = !submitting, backgroundColor = NnoworkTheme.colors.disabledBG) {
                Text(
                    stringResource(R.string.treehole_report_cancel),
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
            }
        },
    )
}

@Composable
fun TreeHoleBlockConfirmationDialog(
    target: TreeHoleSafetyTarget,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                stringResource(R.string.treehole_block_confirm_title),
                style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.textOnDark),
            )
        },
        text = {
            Text(
                stringResource(R.string.treehole_block_confirm_body, target.nickname),
                style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
            )
        },
        confirmButton = {
            PixelButton(onClick = onConfirm, backgroundColor = NnoworkTheme.colors.danger) {
                Text(
                    stringResource(R.string.treehole_block_author),
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.textOnDark),
                )
            }
        },
        dismissButton = {
            PixelButton(onClick = onDismiss, backgroundColor = NnoworkTheme.colors.disabledBG) {
                Text(
                    stringResource(R.string.treehole_action_cancel),
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
            }
        },
    )
}

@Composable
private fun reportReasonLabel(reason: TreeHoleReportReason): String = stringResource(
    when (reason) {
        TreeHoleReportReason.OFFENSIVE -> R.string.treehole_report_reason_offensive
        TreeHoleReportReason.HARASSMENT -> R.string.treehole_report_reason_harassment
        TreeHoleReportReason.PRIVACY -> R.string.treehole_report_reason_privacy
        TreeHoleReportReason.SPAM -> R.string.treehole_report_reason_spam
        TreeHoleReportReason.ILLEGAL -> R.string.treehole_report_reason_illegal
        TreeHoleReportReason.OTHER -> R.string.treehole_report_reason_other
    },
)

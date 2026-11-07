package com.neilyin.nnowork.feature.treehole

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import com.neilyin.nnowork.R
import com.neilyin.nnowork.data.treehole.TreeHoleBlockPolicy
import com.neilyin.nnowork.data.treehole.TreeHoleReportReason
import com.neilyin.nnowork.data.treehole.TreeHoleReportTargetType
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
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
        backgroundColor = PixelColors.white,
        modifier = modifier.size(48.dp),
        contentPadding = PaddingValues(8.dp),
    ) {
        Icon(
            Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.treehole_safety_more_actions),
            tint = PixelColors.black,
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
                style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    target.preview,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = pixelTextStyle(13.sp, color = PixelColors.gray),
                )
                PixelButton(
                    onClick = onReport,
                    backgroundColor = PixelColors.yellow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Flag, contentDescription = null, tint = PixelColors.black)
                    Spacer(Modifier.size(7.dp))
                    Text(
                        stringResource(
                            if (target.targetType == TreeHoleReportTargetType.POST) {
                                R.string.treehole_report_post
                            } else {
                                R.string.treehole_report_comment
                            },
                        ),
                        style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black),
                    )
                }
                PixelButton(
                    onClick = onBlock,
                    enabled = target.canBlock,
                    backgroundColor = if (target.canBlock) PixelColors.red else PixelColors.gray,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Block, contentDescription = null, tint = PixelColors.white)
                    Spacer(Modifier.size(7.dp))
                    Text(
                        stringResource(
                            if (target.canBlock) R.string.treehole_block_author
                            else R.string.treehole_block_unavailable,
                        ),
                        style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.white),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            PixelButton(onClick = onDismiss, backgroundColor = PixelColors.gray) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = PixelColors.black)
                Spacer(Modifier.size(5.dp))
                Text(
                    stringResource(R.string.treehole_action_close),
                    style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black),
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
                style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.treehole_report_reason),
                    style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.white),
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
                            .background(PixelColors.white)
                            .border(2.dp, PixelColors.black, RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = reason == option, onClick = null)
                        Text(
                            reportReasonLabel(option),
                            style = pixelTextStyle(12.sp, FontWeight.Bold, PixelColors.black),
                        )
                    }
                }
                Text(
                    stringResource(R.string.treehole_report_note),
                    style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.white),
                )
                BasicTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    enabled = !submitting,
                    textStyle = pixelTextStyle(13.sp, color = PixelColors.black),
                    cursorBrush = SolidColor(PixelColors.black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(PixelColors.white)
                        .border(2.dp, PixelColors.black, RectangleShape)
                        .padding(10.dp),
                    decorationBox = { inner ->
                        if (note.isEmpty()) {
                            Text(
                                stringResource(R.string.treehole_report_note_placeholder),
                                style = pixelTextStyle(12.sp, color = PixelColors.gray),
                            )
                        }
                        inner()
                    },
                )
                Text(
                    stringResource(R.string.treehole_report_note_count, note.length),
                    style = pixelTextStyle(11.sp, color = PixelColors.gray),
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
                            style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.white),
                        )
                    }
                }
                Text(
                    stringResource(R.string.treehole_report_review_24h),
                    style = pixelTextStyle(12.sp, color = PixelColors.yellow),
                )
                errorMessage?.let {
                    Text(
                        it,
                        style = pixelTextStyle(12.sp, FontWeight.Bold, PixelColors.red),
                    )
                }
            }
        },
        confirmButton = {
            PixelButton(
                onClick = { onSubmit(reason, note, alsoBlock) },
                enabled = !submitting,
                backgroundColor = PixelColors.yellow,
            ) {
                Text(
                    stringResource(
                        if (submitting) R.string.treehole_report_submitting
                        else R.string.treehole_report_submit,
                    ),
                    style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black),
                )
            }
        },
        dismissButton = {
            PixelButton(onClick = onDismiss, enabled = !submitting, backgroundColor = PixelColors.gray) {
                Text(
                    stringResource(R.string.treehole_report_cancel),
                    style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black),
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
                style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white),
            )
        },
        text = {
            Text(
                stringResource(R.string.treehole_block_confirm_body, target.nickname),
                style = pixelTextStyle(13.sp, color = PixelColors.gray),
            )
        },
        confirmButton = {
            PixelButton(onClick = onConfirm, backgroundColor = PixelColors.red) {
                Text(
                    stringResource(R.string.treehole_block_author),
                    style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.white),
                )
            }
        },
        dismissButton = {
            PixelButton(onClick = onDismiss, backgroundColor = PixelColors.gray) {
                Text(
                    stringResource(R.string.treehole_action_cancel),
                    style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black),
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

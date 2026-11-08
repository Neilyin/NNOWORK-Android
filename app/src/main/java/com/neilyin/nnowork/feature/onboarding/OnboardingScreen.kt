package com.neilyin.nnowork.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.NnoworkCenteredPage
import com.neilyin.nnowork.ui.theme.NnoworkElevation
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTextField
import com.neilyin.nnowork.ui.theme.palette
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import kotlinx.coroutines.launch

/**
 * 最小可用引導：輸入公司名稱、選擇卡片格數與主題，完成後進入主畫面。
 * （完整版引導／教學留待後續 Task #8 補齊）
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    settingsRepository: SettingsRepository = ServiceLocator.settingsRepository,
) {
    var company by rememberSaveable { mutableStateOf("") }
    var maxStamps by rememberSaveable { mutableIntStateOf(10) }
    var styleId by rememberSaveable { mutableIntStateOf(1) }
    val scope = rememberCoroutineScope()

    NnoworkCenteredPage {
        Box(
            modifier = Modifier
                .size(86.dp)
                .pixelCard(
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    borderColor = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(NnoworkRadius.hero),
                    elevation = NnoworkElevation.hero,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(NnoworkSize.minTouchTarget),
            )
        }
        Spacer(Modifier.height(NnoworkSpacing.md))
        Text("離職集點卡", style = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.onBackground))
        Spacer(Modifier.height(NnoworkSpacing.xxs))
        Text(
            "把每個爆氣的日子，蓋成一枚印章",
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(NnoworkSpacing.xl))

        Text("讓你想離職的公司叫什麼？", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, MaterialTheme.colorScheme.onBackground), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(NnoworkSpacing.xs))
        NnoworkTextField(
            value = company,
            onValueChange = { company = it },
            label = "公司名稱",
            placeholder = "例如：血汗科技",
        )

        Spacer(Modifier.height(NnoworkSpacing.lg))
        Text("一張卡幾格？", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, MaterialTheme.colorScheme.onBackground), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(NnoworkSpacing.xs))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
            listOf(10, 20, 30).forEach { n ->
                PixelButton(
                    onClick = { maxStamps = n },
                    backgroundColor = if (maxStamps == n) NnoworkTheme.colors.success else NnoworkTheme.colors.disabledBG,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("$n 格", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            }
        }

        Spacer(Modifier.height(NnoworkSpacing.lg))
        Text("選個風格", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, MaterialTheme.colorScheme.onBackground), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(NnoworkSpacing.xs))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
            listOf(CardTheme.RPG, CardTheme.CYBERPUNK, CardTheme.VACATION).forEach { t ->
                PixelButton(
                    onClick = { styleId = t.id },
                    backgroundColor = if (styleId == t.id) t.primaryColor else NnoworkTheme.colors.disabledBG,
                    modifier = Modifier.weight(1f),
                ) {
                    val textColor = if (styleId == t.id) t.palette.onPrimary else NnoworkTheme.colors.primaryText
                    Text(
                        t.title,
                        style = MaterialTheme.typography.labelMedium.copy(color = textColor),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }

        Spacer(Modifier.height(NnoworkSpacing.xxl))
        PixelButton(
            onClick = {
                scope.launch {
                    settingsRepository.update {
                        it.copy(
                            companyName = company.trim(),
                            maxStamps = maxStamps,
                            cardStyle = styleId,
                            currentRound = 1,
                            isOnboardingFinished = true,
                            // 新用戶不需要看「這次更新了什麼」
                            lastSeenNoticeVersion = com.neilyin.nnowork.feature.update.UPDATE_NOTICE_VERSION,
                        )
                    }
                    onFinished()
                }
            },
            backgroundColor = NnoworkTheme.colors.highlight,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("開始集點！", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        }
    }
}

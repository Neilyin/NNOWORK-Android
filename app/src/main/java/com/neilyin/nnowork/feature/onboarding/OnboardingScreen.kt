package com.neilyin.nnowork.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
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
    var company by remember { mutableStateOf("") }
    var maxStamps by remember { mutableIntStateOf(10) }
    var styleId by remember { mutableIntStateOf(1) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelColors.darkGray)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🐂", style = pixelTextStyle(64.sp))
        Spacer(Modifier.height(8.dp))
        Text("離職集點卡", style = pixelTextStyle(28.sp, FontWeight.Bold, PixelColors.white))
        Spacer(Modifier.height(4.dp))
        Text("把每個爆氣的日子，蓋成一枚印章", style = pixelTextStyle(13.sp, color = PixelColors.gray), textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))

        Text("讓你想離職的公司叫什麼？", style = pixelTextStyle(16.sp, FontWeight.Bold, PixelColors.white), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelColors.white)
                .border(3.dp, PixelColors.black, RectangleShape)
                .padding(14.dp),
        ) {
            BasicTextField(
                value = company,
                onValueChange = { company = it },
                singleLine = true,
                textStyle = pixelTextStyle(18.sp, FontWeight.Medium, PixelColors.black),
                cursorBrush = SolidColor(PixelColors.black),
                modifier = Modifier.fillMaxWidth(),
            )
            if (company.isEmpty()) {
                Text("例如：血汗科技", style = pixelTextStyle(16.sp, color = PixelColors.gray))
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("一張卡幾格？", style = pixelTextStyle(16.sp, FontWeight.Bold, PixelColors.white), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10, 20, 30).forEach { n ->
                PixelButton(
                    onClick = { maxStamps = n },
                    backgroundColor = if (maxStamps == n) PixelColors.green else PixelColors.gray,
                ) {
                    Text("$n 格", style = pixelTextStyle(16.sp, FontWeight.Bold, PixelColors.black))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("選個風格", style = pixelTextStyle(16.sp, FontWeight.Bold, PixelColors.white), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(CardTheme.RPG, CardTheme.CYBERPUNK, CardTheme.VACATION).forEach { t ->
                PixelButton(
                    onClick = { styleId = t.id },
                    backgroundColor = if (styleId == t.id) t.primaryColor else PixelColors.gray,
                ) {
                    Text(t.title, style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.white))
                }
            }
        }

        Spacer(Modifier.height(40.dp))
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
                        )
                    }
                    onFinished()
                }
            },
            backgroundColor = PixelColors.yellow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("開始集點！", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.black))
        }
    }
}

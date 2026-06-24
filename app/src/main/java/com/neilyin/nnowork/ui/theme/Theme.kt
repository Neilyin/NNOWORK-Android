package com.neilyin.nnowork.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

private val PixelTypography = Typography().run {
    val mono = FontFamily.Monospace
    fun TextStyle.mono() = copy(fontFamily = mono)
    Typography(
        displayLarge = displayLarge.mono(),
        displayMedium = displayMedium.mono(),
        displaySmall = displaySmall.mono(),
        headlineLarge = headlineLarge.mono(),
        headlineMedium = headlineMedium.mono(),
        headlineSmall = headlineSmall.mono(),
        titleLarge = titleLarge.mono(),
        titleMedium = titleMedium.mono(),
        titleSmall = titleSmall.mono(),
        bodyLarge = bodyLarge.mono(),
        bodyMedium = bodyMedium.mono(),
        bodySmall = bodySmall.mono(),
        labelLarge = labelLarge.mono(),
        labelMedium = labelMedium.mono(),
        labelSmall = labelSmall.mono(),
    )
}

private val DarkColors = darkColorScheme(
    primary = PixelColors.blue,
    secondary = PixelColors.yellow,
    background = PixelColors.darkGray,
    surface = colorHex("#283541"),
)

private val LightColors = lightColorScheme(
    primary = PixelColors.blue,
    secondary = PixelColors.orange,
    background = PixelColors.notionBackground,
    surface = PixelColors.notionSurface,
)

@Composable
fun NnoworkTheme(
    isLight: Boolean = !isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isLight) LightColors else DarkColors,
        typography = PixelTypography,
        content = content,
    )
}

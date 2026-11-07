package com.neilyin.nnowork.ui.common

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * 鋪滿整個螢幕（含狀態列與手勢列）的對話框，對應 iOS fullScreenCover／全螢幕遮罩。
 *
 * 預設 Dialog 視窗排在狀態列下方，上方會露出深灰遮罩帶、底部內容被手勢列蓋住，也會讓底部導覽仍可點。
 * 這裡讓視窗鋪滿、拿掉系統遮罩，[background] 自己畫底色；內容自動讓開狀態列與手勢列。
 * [background] 用半透明 scrim 時就是蓋住整個 App（含底部導覽）的遮罩。
 */
@Composable
fun FullScreenDialog(
    onDismissRequest: () -> Unit,
    background: Brush,
    lightSystemBars: Boolean,
    dismissOnBackPress: Boolean = true,
    /** false：內容自己處理狀態列與手勢列（例如年度回顧的背景要鋪到系統列底下）。 */
    applySystemBarInsets: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                )
                @Suppress("DEPRECATION")
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                @Suppress("DEPRECATION")
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                window.setDimAmount(0f)
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightSystemBars
                    isAppearanceLightNavigationBars = lightSystemBars
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .then(if (applySystemBarInsets) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier),
            content = content,
        )
    }
}

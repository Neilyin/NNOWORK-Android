package com.neilyin.nnowork

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.neilyin.nnowork.navigation.AppRoot
import com.neilyin.nnowork.ui.theme.NnoworkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NnoworkTheme {
                AppRoot()
            }
        }
    }
}

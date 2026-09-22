package com.delta.tactics

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.delta.tactics.core.ui.theme.AppBackgroundLight
import com.delta.tactics.core.ui.theme.DeltaTacticsTheme
import com.delta.tactics.presentation.cipher.CipherRoomViewModel
import com.delta.tactics.presentation.home.HomeDashboardScreen

class MainActivity : ComponentActivity() {

    private val cipherViewModel: CipherRoomViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        setContent {
            DeltaTacticsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppBackgroundLight
                ) {
                    HomeDashboardScreen(viewModel = cipherViewModel)
                }
            }
        }
    }
}

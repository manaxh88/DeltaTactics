package com.delta.tactics

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.delta.tactics.core.ui.theme.AppBackgroundLight
import com.delta.tactics.core.ui.theme.DeltaTacticsTheme
import com.delta.tactics.data.repository.CardLoadoutRepository
import com.delta.tactics.data.repository.KeyRoomRepository
import com.delta.tactics.data.repository.TacticalNewsRepository
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

        // 冷启动或开屏静默同步各模块数据（带 1 小时缓存有效期校验）与图片预热
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                CardLoadoutRepository(applicationContext).fetchCardLoadoutData(force = false)
                val keyRepo = KeyRoomRepository(applicationContext)
                keyRepo.syncKeyRoomsFromWeb(force = false)
                keyRepo.preloadKeyRoomImages(applicationContext)
                TacticalNewsRepository(applicationContext).fetchArticles(page = 1, force = false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

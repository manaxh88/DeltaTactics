package com.delta.tactics.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.Toast
import com.delta.tactics.MainActivity
import com.delta.tactics.R
import com.delta.tactics.data.repository.CipherRoomRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 三角洲助手 · 桌面每日密码小组件 (4x2 经典全景卡片)
 * 支持桌面直接点击卡片复制密码、手动刷新、App 快速唤起与双端联动。
 */
class DailyPasswordWidgetProvider : AppWidgetProvider() {

    private val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val ACTION_COPY_CODE = "com.delta.tactics.action.COPY_CODE"
        const val ACTION_REFRESH_WIDGET = "com.delta.tactics.action.REFRESH_WIDGET"
        const val ACTION_UPDATE_DATA = "com.delta.tactics.action.UPDATE_DATA"

        const val EXTRA_MAP_NAME = "extra_map_name"
        const val EXTRA_CODE = "extra_code"

        /**
         * 全局刷新所有已添加的桌面小组件
         */
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, DailyPasswordWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return
                if (appWidgetIds.isNotEmpty()) {
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * 单个小组件视图与交互绑定
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_daily_password)
            val repository = CipherRoomRepository(context)
            val passwords = repository.loadCachedPasswordsIfValid() ?: repository.getDefaultDailyPasswords()

            // 1. 设置顶栏日期标签
            val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
            val dateStr = sdf.format(Date())
            views.setTextViewText(R.id.widget_update_time, "今日 $dateStr 已更新")

            // 2. 地图卡片与视图 ID 映射 (6 大战术地图)
            val cardIds = intArrayOf(
                R.id.widget_card_1,
                R.id.widget_card_2,
                R.id.widget_card_3,
                R.id.widget_card_4,
                R.id.widget_card_5,
                R.id.widget_card_6
            )
            val codeIds = intArrayOf(
                R.id.widget_code_1,
                R.id.widget_code_2,
                R.id.widget_code_3,
                R.id.widget_code_4,
                R.id.widget_code_5,
                R.id.widget_code_6
            )
            val nameIds = intArrayOf(
                R.id.widget_name_1,
                R.id.widget_name_2,
                R.id.widget_name_3,
                R.id.widget_name_4,
                R.id.widget_name_5,
                R.id.widget_name_6
            )

            for (i in cardIds.indices) {
                if (i in passwords.indices) {
                    val item = passwords[i]
                    views.setTextViewText(codeIds[i], item.code)
                    views.setTextViewText(nameIds[i], item.mapName)

                    // 绑定点击直接复制广播
                    val copyIntent = Intent(context, DailyPasswordWidgetProvider::class.java).apply {
                        action = ACTION_COPY_CODE
                        putExtra(EXTRA_MAP_NAME, item.mapName)
                        putExtra(EXTRA_CODE, item.code)
                        data = Uri.parse("delta://widget/copy/${appWidgetId}/$i")
                    }
                    val copyPendingIntent = PendingIntent.getBroadcast(
                        context,
                        appWidgetId * 10 + i,
                        copyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(cardIds[i], copyPendingIntent)
                }
            }

            // 3. 顶栏刷新按钮广播绑定
            val refreshIntent = Intent(context, DailyPasswordWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
                data = Uri.parse("delta://widget/refresh/${appWidgetId}")
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 100 + 1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            // 4. 顶栏 App 图标一键唤起主应用
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 100 + 2,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_app, appPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_COPY_CODE -> {
                val mapName = intent.getStringExtra(EXTRA_MAP_NAME) ?: "地图"
                val code = intent.getStringExtra(EXTRA_CODE) ?: ""
                if (code.isNotBlank()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    if (clipboard != null) {
                        val clip = ClipData.newPlainText("Delta Tactics Code", code)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制 [$mapName] 密码 $code", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            ACTION_REFRESH_WIDGET -> {
                Toast.makeText(context, "正在刷新每日密码...", Toast.LENGTH_SHORT).show()
                val pendingResult = goAsync()
                providerScope.launch {
                    try {
                        val repo = CipherRoomRepository(context)
                        val result = withContext(Dispatchers.IO) {
                            repo.syncDailyPasswordsFromWeb(force = true)
                        }
                        updateAllWidgets(context)
                        if (result.isSuccess) {
                            Toast.makeText(context, "每日密码已同步最新", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "网络同步失败，已显示本地最新密码", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_UPDATE_DATA -> {
                updateAllWidgets(context)
            }
        }
    }
}

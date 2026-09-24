package com.delta.tactics

import com.delta.tactics.presentation.widget.DailyPasswordWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DailyPasswordWidgetTest {

    @Test
    fun `widget layout and xml metadata files exist`() {
        val layoutFile = File("src/main/res/layout/widget_daily_password.xml")
        assertTrue("widget_daily_password.xml should exist", layoutFile.exists())
        val layoutContent = layoutFile.readText(Charsets.UTF_8)

        // 验证 6 个卡片与代码 TextView ID
        for (i in 1..6) {
            assertTrue("Should contain widget_card_$i", layoutContent.contains("widget_card_$i"))
            assertTrue("Should contain widget_code_$i", layoutContent.contains("widget_code_$i"))
            assertTrue("Should contain widget_name_$i", layoutContent.contains("widget_name_$i"))
        }

        assertTrue("Should contain refresh button ID", layoutContent.contains("widget_btn_refresh"))
        assertTrue("Should contain app button ID", layoutContent.contains("widget_btn_app"))
        assertTrue("Should contain update time ID", layoutContent.contains("widget_update_time"))

        // 验证 Provider 元数据 XML
        val xmlInfoFile = File("src/main/res/xml/widget_daily_password_info.xml")
        assertTrue("widget_daily_password_info.xml should exist", xmlInfoFile.exists())
        val xmlContent = xmlInfoFile.readText(Charsets.UTF_8)
        assertTrue("Should target 4 cells width", xmlContent.contains("targetCellWidth=\"4\""))
        assertTrue("Should target 2 cells height", xmlContent.contains("targetCellHeight=\"2\""))
        assertTrue("Should specify initialLayout", xmlContent.contains("widget_daily_password"))
    }

    @Test
    fun `widget provider action constants are valid`() {
        assertEquals("com.delta.tactics.action.COPY_CODE", DailyPasswordWidgetProvider.ACTION_COPY_CODE)
        assertEquals("com.delta.tactics.action.REFRESH_WIDGET", DailyPasswordWidgetProvider.ACTION_REFRESH_WIDGET)
        assertEquals("com.delta.tactics.action.UPDATE_DATA", DailyPasswordWidgetProvider.ACTION_UPDATE_DATA)
        assertEquals("extra_map_name", DailyPasswordWidgetProvider.EXTRA_MAP_NAME)
        assertEquals("extra_code", DailyPasswordWidgetProvider.EXTRA_CODE)
    }
}

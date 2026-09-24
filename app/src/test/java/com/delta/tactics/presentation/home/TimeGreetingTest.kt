package com.delta.tactics.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeGreetingTest {

    @Test
    fun testAll24HoursGreeting() {
        // 0..4 夜深了
        for (h in 0..4) {
            assertEquals("Hour $h should be 夜深了", "夜深了", getTimeGreeting(h))
        }

        // 5..8 早上好
        for (h in 5..8) {
            assertEquals("Hour $h should be 早上好", "早上好", getTimeGreeting(h))
        }

        // 9..11 上午好
        for (h in 9..11) {
            assertEquals("Hour $h should be 上午好", "上午好", getTimeGreeting(h))
        }

        // 12..13 中午好
        for (h in 12..13) {
            assertEquals("Hour $h should be 中午好", "中午好", getTimeGreeting(h))
        }

        // 14..17 下午好
        for (h in 14..17) {
            assertEquals("Hour $h should be 下午好", "下午好", getTimeGreeting(h))
        }

        // 18..23 晚上好
        for (h in 18..23) {
            assertEquals("Hour $h should be 晚上好", "晚上好", getTimeGreeting(h))
        }
    }
}

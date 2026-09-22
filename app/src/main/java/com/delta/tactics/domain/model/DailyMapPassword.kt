package com.delta.tactics.domain.model

data class DailyMapPassword(
    val mapId: String,
    val mapName: String,
    val code: String,
    val locationDesc: String,
    val refreshTime: String = "今日 00:00 已刷新"
)

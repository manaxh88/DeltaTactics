package com.delta.tactics.domain.model

data class CipherRoom(
    val id: String,
    val map: TacticalMap,
    val roomName: String,
    val code: String,
    val isDailyDynamic: Boolean = false,
    val dynamicHint: String? = null,
    val locationDesc: String,
    val lootHighlights: List<String>,
    val tacticalTips: String,
    val dangerLevel: Int = 3
)

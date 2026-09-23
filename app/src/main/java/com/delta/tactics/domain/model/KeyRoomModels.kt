package com.delta.tactics.domain.model

data class KeyRoomCardItem(
    val id: String,
    val name: String,
    val mapId: Int,
    val mapName: String,
    val imgUrl: String,
    val grade: Int,
    val price: Long,
    val changeToday: String,
    val lootDesc: String = ""
) {
    /**
     * 格式化市场估值价格展示 (例如: 417.7W 或 85.2K)
     */
    val formattedPrice: String
        get() {
            return when {
                price >= 10000 -> String.format("%.1fW 币", price / 10000.0)
                price > 0 -> "${price} 币"
                else -> "市价波动中"
            }
        }

    /**
     * 品质等级称号
     */
    val gradeLabel: String
        get() = when (grade) {
            6 -> "6级绝密金卡"
            5 -> "5级机密紫卡"
            4 -> "4级特勤蓝卡"
            3 -> "3级常规绿卡"
            else -> "${grade}级卡"
        }
}

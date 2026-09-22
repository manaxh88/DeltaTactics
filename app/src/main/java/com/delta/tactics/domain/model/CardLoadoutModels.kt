package com.delta.tactics.domain.model

import androidx.compose.ui.graphics.Color

/**
 * 卡战备根数据对象
 */
data class CardLoadoutData(
    val updateTime: String = "",
    val tiers: List<CardLoadoutTier> = emptyList()
)

/**
 * 战备门槛档位（如 11W、18W、55W、60W、78W）
 */
data class CardLoadoutTier(
    val id: String,
    val name: String,
    val maps: String,
    val thresholdValue: Long,
    val plans: List<CardLoadoutPlan>
)

/**
 * 具体配装流派方案（如 枪械优先、均衡套装、胸挂优先）
 */
data class CardLoadoutPlan(
    val name: String,
    val price: Long,
    val jz: Long,       // 战备值
    val cz: Long,       // 假账差额 (price - jz，负数表示立省)
    val data: List<CardLoadoutItem>
)

/**
 * 配装中的单个装备/配件项
 */
data class CardLoadoutItem(
    val id: Long = 0,
    val name: String = "",
    val grade: Int = 1,
    val price: Long = 0,
    val jz: Long = 0,       // 该配件对假账的贡献 (负数表示虚高战备，省钱)
    val type: String = "",  // 如 "枪1", "枪1-枪管", "枪1-瞄具", "防具", "背包", "胸挂"
    val pic: String = "",
    val bl: Int = 0,
    val jiazhang: Int = 0
)

/**
 * 装备配件树形节点（主武器与其下挂配件树）
 */
data class LoadoutItemTreeNode(
    val parent: CardLoadoutItem,
    val children: List<CardLoadoutItem> = emptyList()
)

/**
 * 工具方法：将扁平装备列表转换为父子树结构
 */
fun List<CardLoadoutItem>.toTreeNodes(): List<LoadoutItemTreeNode> {
    val result = mutableListOf<LoadoutItemTreeNode>()
    var currentParent: CardLoadoutItem? = null
    val currentChildren = mutableListOf<CardLoadoutItem>()

    for (item in this) {
        if (item.type.contains("-")) {
            currentChildren.add(item)
        } else {
            if (currentParent != null) {
                result.add(LoadoutItemTreeNode(currentParent, currentChildren.toList()))
                currentChildren.clear()
            }
            currentParent = item
        }
    }
    if (currentParent != null) {
        result.add(LoadoutItemTreeNode(currentParent, currentChildren.toList()))
    }
    return result
}

/**
 * 装备品质等级颜色
 */
fun getGradeBadgeColor(grade: Int): Color {
    return when (grade) {
        1 -> Color(0xFF94A3B8) // 白 / 灰
        2 -> Color(0xFF22C55E) // 绿
        3 -> Color(0xFF3B82F6) // 蓝
        4 -> Color(0xFFA855F7) // 紫
        5 -> Color(0xFFEAB308) // 金
        6 -> Color(0xFFEF4444) // 红
        else -> Color(0xFF94A3B8)
    }
}

package com.delta.tactics.domain.model

enum class CraftBenchType(val displayName: String) {
    ALL("全部"),
    ARMOR("防具台"),
    AMMO("弹药台"),
    MEDICAL("医疗台"),
    WEAPON("枪械台")
}

data class CraftRecipe(
    val id: String,
    val name: String,
    val benchType: CraftBenchType,
    val benchName: String,
    val durationHours: Int,
    val totalProfit: Long,
    val hourlyProfit: Long,
    val cost: Long,
    val revenue: Long,
    val materials: List<String>,
    val recommendedLevel: Int = 1,
    val imageUrl: String = "",
    val grade: Int = 0
)

data class BulletPack(
    val id: String,
    val packName: String,
    val recommendedBullet: String,
    val recommendedValue: Long,
    val bullets: List<BulletPackItem>,
    val imageUrl: String = ""
)

data class BulletPackItem(
    val rank: Int,
    val name: String,
    val caliber: String,
    val count: Int,
    val unitPrice: Long,
    val totalValue: Long,
    val isBest: Boolean,
    val imageUrl: String = "",
    val grade: Int = 0
)

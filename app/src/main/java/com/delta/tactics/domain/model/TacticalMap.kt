package com.delta.tactics.domain.model

enum class TacticalMap(
    val id: String,
    val displayName: String,
    val englishName: String,
    val shortTag: String
) {
    ALL("all", "全部", "All Maps", "ALL"),
    ZERO_DAM("zero_dam", "零号大坝", "Zero Dam", "大坝"),
    LONGBOW_VALLEY("longbow_valley", "长弓溪谷", "Longbow Valley", "溪谷"),
    BARKASH("barkash", "巴克什", "Barkash", "巴克什"),
    SPACE_CITY("space_city", "航天基地", "Space City", "航天"),
    TIDE_PRISON("tide_prison", "潮汐监狱", "Tide Prison", "监狱"),
    AZ3_NUCLEAR("az3_nuclear", "AZ3核电站", "AZ3 Nuclear", "核电站");

    companion object {
        fun fromId(id: String): TacticalMap {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ALL
        }
    }
}

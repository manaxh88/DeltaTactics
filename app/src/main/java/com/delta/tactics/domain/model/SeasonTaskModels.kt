package com.delta.tactics.domain.model

data class TaskReward(
    val name: String,
    val amount: Long,
    val isImportant: Boolean = false,
    val grade: Int = 0
)

data class SeasonTask(
    val questId: Long,
    val name: String,
    val desc: String,
    val acceptRequiredLevel: Int,
    val objectives: List<String>,
    val rewards: List<TaskReward>,
    val isCompleted: Boolean = false
)

data class SeasonTaskGroup(
    val groupId: Long,
    val name: String,
    val titleName: String,
    val questIds: List<Long>
)

data class SeasonPhase(
    val phase: Int,
    val stageName: String,
    val stageShortName: String,
    val main: SeasonTaskGroup,
    val sides: List<SeasonTaskGroup>
) {
    val allGroupIds: List<Long> get() = listOf(main.groupId) + sides.map { it.groupId }
    val allQuestIds: List<Long> get() = main.questIds + sides.flatMap { it.questIds }
}

data class SeasonData(
    val seasonCode: String,
    val themeName: String,
    val phases: List<SeasonPhase>,
    val quests: Map<Long, SeasonTask>
)

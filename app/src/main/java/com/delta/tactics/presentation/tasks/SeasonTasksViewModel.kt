package com.delta.tactics.presentation.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.delta.tactics.data.repository.SeasonTasksRepository
import com.delta.tactics.domain.model.SeasonData
import com.delta.tactics.domain.model.SeasonPhase
import com.delta.tactics.domain.model.SeasonTask
import com.delta.tactics.domain.model.SeasonTaskGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SeasonTasksUiState(
    val seasonData: SeasonData? = null,
    val selectedPhaseIndex: Int = 0, // 0 -> Phase 1, 1 -> Phase 2, etc.
    val selectedGroupId: Long? = null, // null means all groups in current phase
    val searchQuery: String = "",
    val filterUncompletedOnly: Boolean = false,
    val completedQuestIds: Set<Long> = emptySet(),
    val totalQuestsCount: Int = 0,
    val totalCompletedCount: Int = 0
) {
    val currentPhase: SeasonPhase?
        get() = seasonData?.phases?.getOrNull(selectedPhaseIndex)

    val currentPhaseGroups: List<SeasonTaskGroup>
        get() {
            val phase = currentPhase ?: return emptyList()
            return listOf(phase.main) + phase.sides
        }

    val overallProgressPercent: Float
        get() = if (totalQuestsCount > 0) totalCompletedCount.toFloat() / totalQuestsCount else 0f

    val currentPhaseProgress: Pair<Int, Int>
        get() {
            val phase = currentPhase ?: return 0 to 0
            val allIds = phase.allQuestIds
            val completed = allIds.count { completedQuestIds.contains(it) }
            return completed to allIds.size
        }

    val displayedTasks: List<SeasonTask>
        get() {
            val data = seasonData ?: return emptyList()
            var list: List<SeasonTask>

            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = data.quests.values.filter { task ->
                    task.name.lowercase().contains(q) ||
                            task.objectives.any { it.lowercase().contains(q) } ||
                            task.desc.lowercase().contains(q)
                }
            } else {
                val phase = currentPhase ?: return emptyList()
                val targetQuestIds = if (selectedGroupId != null) {
                    val group = (listOf(phase.main) + phase.sides).firstOrNull { it.groupId == selectedGroupId }
                    group?.questIds ?: emptyList()
                } else {
                    phase.allQuestIds
                }
                list = targetQuestIds.mapNotNull { data.quests[it] }
            }

            if (filterUncompletedOnly) {
                list = list.filter { !completedQuestIds.contains(it.questId) }
            }

            return list.map { it.copy(isCompleted = completedQuestIds.contains(it.questId)) }
        }
}

class SeasonTasksViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SeasonTasksRepository(application)

    private val _uiState = MutableStateFlow(SeasonTasksUiState())
    val uiState: StateFlow<SeasonTasksUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getSeasonData()
            val completed = repository.getCompletedQuestIds()
            _uiState.update {
                it.copy(
                    seasonData = data,
                    completedQuestIds = completed,
                    totalQuestsCount = data.quests.size,
                    totalCompletedCount = completed.size
                )
            }
        }
    }

    fun selectPhase(phaseIndex: Int) {
        _uiState.update {
            it.copy(
                selectedPhaseIndex = phaseIndex,
                selectedGroupId = null // Reset group filter on phase change
            )
        }
    }

    fun selectGroup(groupId: Long?) {
        _uiState.update { it.copy(selectedGroupId = groupId) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFilterUncompleted() {
        _uiState.update { it.copy(filterUncompletedOnly = !it.filterUncompletedOnly) }
    }

    fun toggleQuestCompleted(questId: Long) {
        val newState = repository.toggleQuestCompleted(questId)
        val updated = repository.getCompletedQuestIds()
        _uiState.update {
            it.copy(
                completedQuestIds = updated,
                totalCompletedCount = updated.size
            )
        }
    }

    fun resetAllProgress() {
        repository.clearAllCompleted()
        _uiState.update {
            it.copy(
                completedQuestIds = emptySet(),
                totalCompletedCount = 0
            )
        }
    }
}

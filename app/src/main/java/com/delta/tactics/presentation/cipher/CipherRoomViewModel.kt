package com.delta.tactics.presentation.cipher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delta.tactics.data.repository.CipherRoomRepository
import com.delta.tactics.domain.model.CipherRoom
import com.delta.tactics.domain.model.DailyMapPassword
import com.delta.tactics.domain.model.TacticalMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CipherUiState(
    val selectedMap: TacticalMap = TacticalMap.ALL,
    val searchQuery: String = "",
    val rooms: List<CipherRoom> = emptyList(),
    val totalCount: Int = 0
)

class CipherRoomViewModel(
    private val repository: CipherRoomRepository = CipherRoomRepository()
) : ViewModel() {

    private val _selectedMap = MutableStateFlow(TacticalMap.ALL)
    val selectedMap: StateFlow<TacticalMap> = _selectedMap.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val dailyPasswords: StateFlow<List<DailyMapPassword>> = repository.dailyPasswords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = repository.getDefaultDailyPasswords()
        )

    init {
        // 延迟 1.5 秒异步静默同步，避免冷启动与首帧渲染抢占 CPU 和线程池
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            syncDailyPasswords()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CipherUiState> = combine(_selectedMap, _searchQuery) { map, query ->
        Pair(map, query)
    }.flatMapLatest { (map, query) ->
        repository.getCipherRooms(map, query).combine(MutableStateFlow(map)) { rooms, currentMap ->
            CipherUiState(
                selectedMap = currentMap,
                searchQuery = _searchQuery.value,
                rooms = rooms,
                totalCount = rooms.size
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CipherUiState()
    )

    fun syncDailyPasswords(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.syncDailyPasswordsFromWeb()
            _isSyncing.value = false
            onComplete?.invoke(result.isSuccess)
        }
    }

    fun onMapSelected(map: TacticalMap) {
        _selectedMap.value = map
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }
}

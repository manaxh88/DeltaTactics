package com.delta.tactics.presentation.loadout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.delta.tactics.data.repository.CardLoadoutRepository
import com.delta.tactics.domain.model.CardLoadoutData
import com.delta.tactics.domain.model.CardLoadoutPlan
import com.delta.tactics.domain.model.CardLoadoutTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CardLoadoutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CardLoadoutRepository(application)

    private val _loadoutData = MutableStateFlow(CardLoadoutData())
    val loadoutData: StateFlow<CardLoadoutData> = _loadoutData.asStateFlow()

    private val _selectedTierIndex = MutableStateFlow(0)
    val selectedTierIndex: StateFlow<Int> = _selectedTierIndex.asStateFlow()

    private val _selectedPlanType = MutableStateFlow<String?>(null) // null = 全部
    val selectedPlanType: StateFlow<String?> = _selectedPlanType.asStateFlow()

    // 交互式测算器状态：当前已有战备输入
    private val _calculatorCurrentValue = MutableStateFlow("")
    val calculatorCurrentValue: StateFlow<String> = _calculatorCurrentValue.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val data = repository.getCardLoadoutData()
            _loadoutData.value = data
        }
    }

    fun selectTier(index: Int) {
        if (index in 0 until (_loadoutData.value.tiers.size)) {
            _selectedTierIndex.value = index
        }
    }

    fun selectPlanType(planType: String?) {
        _selectedPlanType.value = planType
    }

    fun setCalculatorCurrentValue(value: String) {
        // 只允许输入纯数字
        _calculatorCurrentValue.value = value.filter { it.isDigit() }
    }

    /**
     * 当前选中的档位
     */
    val currentTier: StateFlow<CardLoadoutTier?> = combine(
        _loadoutData,
        _selectedTierIndex
    ) { data, idx ->
        if (data.tiers.isNotEmpty() && idx in data.tiers.indices) {
            data.tiers[idx]
        } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * 当前档位下根据流派过滤的方案列表
     */
    val filteredPlans: StateFlow<List<CardLoadoutPlan>> = combine(
        currentTier,
        _selectedPlanType
    ) { tier, planType ->
        if (tier == null) emptyList()
        else if (planType == null) tier.plans
        else tier.plans.filter { it.name == planType }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

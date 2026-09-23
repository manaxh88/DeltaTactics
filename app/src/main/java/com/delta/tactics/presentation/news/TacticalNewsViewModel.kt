package com.delta.tactics.presentation.news

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.delta.tactics.data.repository.TacticalNewsRepository
import com.delta.tactics.domain.model.TacticalNewsDetail
import com.delta.tactics.domain.model.TacticalNewsItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TacticalNewsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: TacticalNewsRepository = TacticalNewsRepository(application)

    // 优先加载本地磁盘持久化的资讯列表，实现无白屏开屏秒显与离线支持
    private val _newsList = MutableStateFlow<List<TacticalNewsItem>>(repository.getDiskCachedNewsList())
    val newsList: StateFlow<List<TacticalNewsItem>> = _newsList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _selectedDetail = MutableStateFlow<TacticalNewsDetail?>(null)
    val selectedDetail: StateFlow<TacticalNewsDetail?> = _selectedDetail.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    private val _detailError = MutableStateFlow<String?>(null)
    val detailError: StateFlow<String?> = _detailError.asStateFlow()

    init {
        loadNews(force = false)
    }

    /**
     * 加载/刷新第一页资讯 (自动增量合并并持久化至本地磁盘)
     */
    fun loadNews(force: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.fetchArticles(page = 1, limit = 20, force = force)
            result.onSuccess { items ->
                if (items.isNotEmpty()) {
                    _newsList.value = items
                    _currentPage.value = 1
                    _hasMore.value = items.size >= 10
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * 加载更多资讯 (分页)
     */
    fun loadMore() {
        if (_isLoading.value || !_hasMore.value) return

        viewModelScope.launch {
            _isLoading.value = true
            val nextPage = _currentPage.value + 1
            val result = repository.fetchArticles(page = nextPage, limit = 10)
            result.onSuccess { newItems ->
                if (newItems.isNotEmpty()) {
                    val currentIds = _newsList.value.map { it.threadId }.toSet()
                    val distinctNew = newItems.filterNot { currentIds.contains(it.threadId) }
                    _newsList.value = _newsList.value + distinctNew
                    _currentPage.value = nextPage
                    _hasMore.value = newItems.size >= 10
                } else {
                    _hasMore.value = false
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * 选择并拉取文章详情 (优先直接命中本地磁盘持久化缓存，秒开老新闻)
     */
    fun selectArticle(threadId: Long) {
        viewModelScope.launch {
            _selectedDetail.value = null
            _detailError.value = null
            _isLoadingDetail.value = true

            val result = repository.fetchArticleDetail(threadId)
            result.onSuccess { detail ->
                _selectedDetail.value = detail
            }.onFailure { err ->
                _detailError.value = err.message ?: "加载文章失败"
            }
            _isLoadingDetail.value = false
        }
    }

    /**
     * 关闭/清空当前选中的文章
     */
    fun clearSelectedArticle() {
        _selectedDetail.value = null
        _detailError.value = null
        _isLoadingDetail.value = false
    }

    companion object {
        /**
         * 格式化阅读量与点赞量（如 1.2w, 9.8k）
         */
        fun formatCount(count: Int): String {
            return when {
                count >= 10_000 -> {
                    val w = count / 10_000.0
                    String.format(java.util.Locale.US, "%.1fw", w).replace(".0w", "w")
                }
                count >= 1_000 -> {
                    val k = count / 1_000.0
                    String.format(java.util.Locale.US, "%.1fk", k).replace(".0k", "k")
                }
                else -> count.toString()
            }
        }

        /**
         * 格式化发布时间（如 "2026-09-21 17:50:50" -> "09-21 17:50"）
         */
        fun formatDate(raw: String): String {
            if (raw.isBlank()) return ""
            return raw.removePrefix("2026-").removePrefix("2025-").removePrefix("2024-").trim()
        }
    }
}

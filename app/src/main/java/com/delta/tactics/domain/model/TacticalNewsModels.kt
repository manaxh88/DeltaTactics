package com.delta.tactics.domain.model

/**
 * 战术资讯条目 (对应 shushu.fan /api/articles 数据模型)
 */
data class TacticalNewsItem(
    val threadId: Long,
    val dataId: String,
    val title: String,
    val coverUrl: String,
    val author: String,
    val avatarUrl: String,
    val createdAt: String,
    val viewCount: Int,
    val likedCount: Int
)

/**
 * 战术资讯详情 (对应 shushu.fan /api/articles/{threadID} 数据模型)
 */
data class TacticalNewsDetail(
    val threadId: Long,
    val title: String,
    val authorName: String,
    val authorAvatar: String,
    val isOfficial: Boolean,
    val contentHtml: String
)

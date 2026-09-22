package com.delta.tactics

import com.delta.tactics.data.repository.TacticalNewsRepository
import com.delta.tactics.presentation.news.TacticalNewsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TacticalNewsRepositoryTest {

    private lateinit var repository: TacticalNewsRepository

    @Before
    fun setUp() {
        repository = TacticalNewsRepository()
    }

    @Test
    fun `normalizeUrl prefixes protocol-relative urls with https`() {
        assertEquals(
            "https://static.gametalk.qq.com/image/1.png",
            repository.normalizeUrl("//static.gametalk.qq.com/image/1.png")
        )
        assertEquals(
            "https://shushu.fan/logo.png",
            repository.normalizeUrl("https://shushu.fan/logo.png")
        )
        assertEquals(
            "https://example.com/img.jpg",
            repository.normalizeUrl("http://example.com/img.jpg")
        )
    }

    @Test
    fun `fallbackNewsList contains valid initial announcements from shushu fan`() {
        val list = repository.getFallbackNewsList()
        assertTrue("Fallback list should contain items", list.isNotEmpty())
        assertEquals(5, list.size)

        val first = list.first()
        assertEquals(18863L, first.threadId)
        assertTrue(first.title.contains("9月22日更新公告"))
        assertTrue(first.coverUrl.startsWith("https://"))
        assertTrue(first.viewCount > 0)
    }

    @Test
    fun `parseArticlesJson successfully parses shushu fan api format`() {
        val sampleJson = """
            {
                "success": true,
                "data": {
                    "items": [
                        {
                            "threadID": 18863,
                            "dataID": "11675625343806188297",
                            "title": "9月22日更新公告 | 洲年庆典开启！",
                            "cover": "//static.gametalk.qq.com/image/423/cover.png",
                            "author": "三角洲行动",
                            "avatar": "//static.gametalk.qq.com/image/423/avatar.jpg",
                            "createdAt": "2026-09-21 17:50:50",
                            "viewCount": 57607,
                            "likedCount": 490
                        }
                    ],
                    "hasMore": true
                }
            }
        """.trimIndent()

        val parsed = repository.parseArticlesJson(sampleJson)
        assertEquals(1, parsed.size)
        val item = parsed.first()
        assertEquals(18863L, item.threadId)
        assertEquals("9月22日更新公告 | 洲年庆典开启！", item.title)
        assertEquals("https://static.gametalk.qq.com/image/423/cover.png", item.coverUrl)
        assertEquals("https://static.gametalk.qq.com/image/423/avatar.jpg", item.avatarUrl)
        assertEquals(57607, item.viewCount)
        assertEquals(490, item.likedCount)
    }

    @Test
    fun `parseArticleDetailJson successfully parses article html and author`() {
        val sampleJson = """
            {
                "success": true,
                "data": {
                    "title": "9月22日更新公告 | 洲年庆典开启！",
                    "author": {
                        "nickname": "三角洲行动官方",
                        "avatar": "//static.gametalk.qq.com/image/avatar.png",
                        "isOfficial": true
                    },
                    "contentHtml": "<p><img src=\"https://static.gametalk.qq.com/image/poster.jpg\" /></p>"
                }
            }
        """.trimIndent()

        val detail = repository.parseArticleDetailJson(18863L, sampleJson)
        assertNotNull(detail)
        assertEquals(18863L, detail!!.threadId)
        assertEquals("9月22日更新公告 | 洲年庆典开启！", detail.title)
        assertEquals("三角洲行动官方", detail.authorName)
        assertEquals("https://static.gametalk.qq.com/image/avatar.png", detail.authorAvatar)
        assertTrue(detail.isOfficial)
        assertTrue(detail.contentHtml.contains("poster.jpg"))
    }

    @Test
    fun `formatCount properly formats numbers`() {
        assertEquals("500", TacticalNewsViewModel.formatCount(500))
        assertEquals("1.2k", TacticalNewsViewModel.formatCount(1234))
        assertEquals("5.8w", TacticalNewsViewModel.formatCount(57607))
        assertEquals("10w", TacticalNewsViewModel.formatCount(100000))
    }

    @Test
    fun `formatDate strips year prefix cleanly`() {
        assertEquals("09-21 17:50:50", TacticalNewsViewModel.formatDate("2026-09-21 17:50:50"))
        assertEquals("08-15 12:00", TacticalNewsViewModel.formatDate("2025-08-15 12:00"))
    }
}

package com.delta.tactics

import com.delta.tactics.core.cache.ItemImageDiskCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemImageDiskCacheTest {

    @Test
    fun `hashUrl generates deterministic sha256 hex string`() {
        val url = "https://playerhub.df.qq.com/playerhub/60004/object/key/p_%E9%9B%B6%E5%8F%B7%E5%A4%A7%E5%9D%9D%E9%87%91%E5%8D%A1.png"
        val hash1 = ItemImageDiskCache.hashUrl(url)
        val hash2 = ItemImageDiskCache.hashUrl(url)

        assertEquals("Hashes for identical URLs must match", hash1, hash2)
        assertEquals("SHA-256 hex string should be 64 characters long", 64, hash1.length)
        assertTrue("Hash should contain valid hex characters", hash1.matches(Regex("^[0-9a-f]{64}$")))
    }

    @Test
    fun `hashUrl differentiates distinct image urls`() {
        val url1 = "https://playerhub.df.qq.com/playerhub/60004/object/key/p_dam_gold.png"
        val url2 = "https://playerhub.df.qq.com/playerhub/60004/object/key/p_space_gold.png"

        assertNotEquals(ItemImageDiskCache.hashUrl(url1), ItemImageDiskCache.hashUrl(url2))
    }
}

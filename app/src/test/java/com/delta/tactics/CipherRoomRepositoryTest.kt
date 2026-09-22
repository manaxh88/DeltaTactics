package com.delta.tactics

import com.delta.tactics.data.repository.CipherRoomRepository
import com.delta.tactics.domain.model.TacticalMap
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CipherRoomRepositoryTest {

    private lateinit var repository: CipherRoomRepository

    @Before
    fun setUp() {
        repository = CipherRoomRepository()
    }

    @Test
    fun `dailyPasswords contains exactly 6 canonical map 4-digit codes matching shushu fan`() = runBlocking {
        val list = repository.dailyPasswords.first()
        assertEquals(6, list.size)

        val mapCodeMap = list.associate { it.mapName to it.code }
        assertEquals("1392", mapCodeMap["零号大坝"])
        assertEquals("5097", mapCodeMap["长弓溪谷"])
        assertEquals("3144", mapCodeMap["巴克什"])
        assertEquals("9646", mapCodeMap["航天基地"])
        assertEquals("4885", mapCodeMap["潮汐监狱"])
        assertEquals("2525", mapCodeMap["AZ3"])
    }

    @Test
    fun `getAllRoomsSync returns populated list of cipher rooms across 6 maps`() {
        val rooms = repository.getAllRoomsSync()
        assertTrue("Cipher room list should not be empty", rooms.isNotEmpty())
        assertEquals(6, rooms.size)
    }

    @Test
    fun `filter by ZERO_DAM returns only zero dam rooms`() = runBlocking {
        val zeroDamRooms = repository.getCipherRooms(TacticalMap.ZERO_DAM, "").first()
        assertTrue("Should contain zero dam rooms", zeroDamRooms.isNotEmpty())
        assertTrue(zeroDamRooms.all { it.map == TacticalMap.ZERO_DAM })
        assertEquals("1392", zeroDamRooms.first().code)
    }

    @Test
    fun `morse code dictionary conforms to standard delta force cipher`() {
        assertEquals("-----", repository.morseCodeDict['0'])
        assertEquals(".----", repository.morseCodeDict['1'])
        assertEquals(".....", repository.morseCodeDict['5'])
        assertEquals("----.", repository.morseCodeDict['9'])
    }
}

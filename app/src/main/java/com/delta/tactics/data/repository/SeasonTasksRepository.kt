package com.delta.tactics.data.repository

import android.content.Context
import com.delta.tactics.domain.model.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class SeasonTasksRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("season_tasks_prefs", Context.MODE_PRIVATE)
    private var cachedData: SeasonData? = null

    fun getSeasonData(): SeasonData {
        cachedData?.let { return it }
        val parsed = loadFromAssets()
        cachedData = parsed
        return parsed
    }

    private fun loadFromAssets(): SeasonData {
        val stringBuilder = java.lang.StringBuilder()
        context.assets.open("season_tasks.json").use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }

        val json = JSONObject(stringBuilder.toString())
        val seasonCode = json.optString("seasonCode", "S11")
        val themeName = json.optString("themeName", "赛季任务：群星")

        // Parse Quest Details
        val questDetailsJson = json.getJSONObject("questDetails")
        val questsMap = mutableMapOf<Long, SeasonTask>()
        val completedSet = getCompletedQuestIds()

        val keys = questDetailsJson.keys()
        while (keys.hasNext()) {
            val qidStr = keys.next()
            val qJson = questDetailsJson.getJSONObject(qidStr)
            val questId = qJson.getLong("questId")
            val name = qJson.getString("name")
            val desc = qJson.optString("desc", "")
            val acceptLevel = qJson.optInt("acceptRequiredLevel", 0)

            val objArray = qJson.optJSONArray("objectives")
            val objectives = mutableListOf<String>()
            if (objArray != null) {
                for (i in 0 until objArray.length()) {
                    objectives.add(objArray.getString(i))
                }
            }

            val rewArray = qJson.optJSONArray("rewards")
            val rewards = mutableListOf<TaskReward>()
            if (rewArray != null) {
                for (i in 0 until rewArray.length()) {
                    val rJson = rewArray.getJSONObject(i)
                    rewards.add(
                        TaskReward(
                            name = rJson.getString("name"),
                            amount = rJson.optLong("amount", 0),
                            isImportant = rJson.optBoolean("isImportant", false),
                            grade = rJson.optInt("grade", 0)
                        )
                    )
                }
            }

            questsMap[questId] = SeasonTask(
                questId = questId,
                name = name,
                desc = desc,
                acceptRequiredLevel = acceptLevel,
                objectives = objectives,
                rewards = rewards,
                isCompleted = completedSet.contains(questId)
            )
        }

        // Parse Phases
        val phasesArray = json.getJSONArray("phases")
        val phasesList = mutableListOf<SeasonPhase>()
        for (i in 0 until phasesArray.length()) {
            val pJson = phasesArray.getJSONObject(i)
            val phaseNum = pJson.getInt("phase")
            val stageName = pJson.getString("stageName")
            val stageShortName = pJson.getString("stageShortName")

            val mainJson = pJson.getJSONObject("main")
            val mainGroup = parseGroup(mainJson)

            val sidesArray = pJson.optJSONArray("sides")
            val sidesList = mutableListOf<SeasonTaskGroup>()
            if (sidesArray != null) {
                for (j in 0 until sidesArray.length()) {
                    sidesList.add(parseGroup(sidesArray.getJSONObject(j)))
                }
            }

            phasesList.add(
                SeasonPhase(
                    phase = phaseNum,
                    stageName = stageName,
                    stageShortName = stageShortName,
                    main = mainGroup,
                    sides = sidesList
                )
            )
        }

        return SeasonData(
            seasonCode = seasonCode,
            themeName = themeName,
            phases = phasesList,
            quests = questsMap
        )
    }

    private fun parseGroup(groupJson: JSONObject): SeasonTaskGroup {
        val groupId = groupJson.getLong("groupId")
        val name = groupJson.getString("name")
        val titleName = groupJson.getString("titleName")
        val questIdsArray = groupJson.getJSONArray("questIds")
        val questIds = mutableListOf<Long>()
        for (i in 0 until questIdsArray.length()) {
            questIds.add(questIdsArray.getLong(i))
        }
        return SeasonTaskGroup(
            groupId = groupId,
            name = name,
            titleName = titleName,
            questIds = questIds
        )
    }

    fun getCompletedQuestIds(): Set<Long> {
        val set = prefs.getStringSet("completed_quests", emptySet()) ?: emptySet()
        return set.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun toggleQuestCompleted(questId: Long): Boolean {
        val current = getCompletedQuestIds().toMutableSet()
        val newState = if (current.contains(questId)) {
            current.remove(questId)
            false
        } else {
            current.add(questId)
            true
        }
        prefs.edit().putStringSet("completed_quests", current.map { it.toString() }.toSet()).apply()

        // Update in cachedData if loaded
        cachedData?.quests?.get(questId)?.let { old ->
            val updated = old.copy(isCompleted = newState)
            (cachedData?.quests as? MutableMap<Long, SeasonTask>)?.put(questId, updated)
        }

        return newState
    }

    fun clearAllCompleted() {
        prefs.edit().remove("completed_quests").apply()
        cachedData?.let { data ->
            (data.quests as? MutableMap<Long, SeasonTask>)?.let { map ->
                map.keys.forEach { id ->
                    map[id]?.let { map[id] = it.copy(isCompleted = false) }
                }
            }
        }
    }
}

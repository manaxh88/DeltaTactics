package com.delta.tactics.data.repository

import android.content.Context
import com.delta.tactics.domain.model.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class CardLoadoutRepository(private val context: Context) {

    private var cachedData: CardLoadoutData? = null

    fun getCardLoadoutData(): CardLoadoutData {
        cachedData?.let { return it }
        val parsed = loadFromAssets()
        cachedData = parsed
        return parsed
    }

    private fun loadFromAssets(): CardLoadoutData {
        return try {
            val stringBuilder = java.lang.StringBuilder()
            context.assets.open("card_loadout.json").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }

            val json = JSONObject(stringBuilder.toString())
            val updateTime = json.optString("updateTime", "")
            val tiersArray = json.getJSONArray("tiers")
            val tiersList = mutableListOf<CardLoadoutTier>()

            for (t in 0 until tiersArray.length()) {
                val tierJson = tiersArray.getJSONObject(t)
                val id = tierJson.getString("id")
                val name = tierJson.getString("name")
                val maps = tierJson.getString("maps")
                val thresholdValue = tierJson.optLong("thresholdValue", 0L)

                val plansArray = tierJson.getJSONArray("plans")
                val plansList = mutableListOf<CardLoadoutPlan>()

                for (p in 0 until plansArray.length()) {
                    val planJson = plansArray.getJSONObject(p)
                    val planName = planJson.getString("name")
                    val price = planJson.getLong("price")
                    val jz = planJson.getLong("jz")
                    val cz = planJson.getLong("cz")

                    val dataArray = planJson.getJSONArray("data")
                    val itemsList = mutableListOf<CardLoadoutItem>()

                    for (d in 0 until dataArray.length()) {
                        val itemJson = dataArray.getJSONObject(d)
                        itemsList.add(
                            CardLoadoutItem(
                                id = itemJson.optLong("id", 0L),
                                name = itemJson.optString("name", ""),
                                grade = itemJson.optInt("grade", 1),
                                price = itemJson.optLong("price", 0L),
                                jz = itemJson.optLong("jz", 0L),
                                type = itemJson.optString("type", ""),
                                pic = itemJson.optString("pic", ""),
                                bl = itemJson.optInt("bl", 0),
                                jiazhang = itemJson.optInt("jiazhang", 0)
                            )
                        )
                    }

                    plansList.add(
                        CardLoadoutPlan(
                            name = planName,
                            price = price,
                            jz = jz,
                            cz = cz,
                            data = itemsList
                        )
                    )
                }

                tiersList.add(
                    CardLoadoutTier(
                        id = id,
                        name = name,
                        maps = maps,
                        thresholdValue = thresholdValue,
                        plans = plansList
                    )
                )
            }

            CardLoadoutData(
                updateTime = updateTime,
                tiers = tiersList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            CardLoadoutData()
        }
    }
}

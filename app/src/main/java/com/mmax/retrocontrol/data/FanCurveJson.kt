package com.mmax.retrocontrol.data

import org.json.JSONArray
import org.json.JSONObject

/** Portable JSON representation for one fan curve. */
object FanCurveJson {
    private const val FORMAT = "fan-curve"
    private const val VERSION = 1

    fun encode(
        profileId: String,
        profileName: String,
        points: List<FanCurvePoint>,
    ): String {
        val clean = FanCurveSerializer.sanitize(points)
        require(clean.size >= 2) { "A fan curve requires at least two points" }

        val entries = JSONArray()
        clean.forEach { point ->
            entries.put(
                JSONObject()
                    .put("temperatureC", point.tempC)
                    .put("speedPercent", point.speedPercent)
            )
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("curve", profileId)
            .put("name", profileName)
            .put("points", entries)
            .toString(2)
    }

    fun decode(json: String): List<FanCurvePoint> {
        val root = JSONObject(json)
        require(root.optString("format") == FORMAT) { "Unsupported fan curve format" }
        require(root.optInt("version", -1) == VERSION) { "Unsupported fan curve version" }

        val entries = root.getJSONArray("points")
        require(entries.length() >= 2) { "A fan curve requires at least two points" }
        val points = buildList {
            repeat(entries.length()) { index ->
                val entry = entries.getJSONObject(index)
                val tempC = entry.getInt("temperatureC")
                val speedPercent = entry.getInt("speedPercent")
                add(FanCurvePoint(tempC = tempC, speedPercent = speedPercent))
            }
        }
        val clean = FanCurveSerializer.sanitize(points)
        require(clean.size == points.size) { "Fan curve temperatures must be unique" }
        return clean
    }
}

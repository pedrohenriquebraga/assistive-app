package com.eitalab.objectdetection.src.services

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class DetectionHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getConfidencePercent(): Int = (confidence * 100).toInt()
}

class HistoryRepository private constructor(context: Context) {

    private val storage = StorageService(context.applicationContext)
    private val listeners = mutableListOf<() -> Unit>()

    companion object {
        private const val KEY_HISTORY = "detection_history_json"

        @Volatile
        private var INSTANCE: HistoryRepository? = null

        fun getInstance(context: Context): HistoryRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HistoryRepository(context).also { INSTANCE = it }
            }
        }
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it.invoke() }
    }

    fun addDetection(label: String, confidence: Float) {
        val currentList = getHistory().toMutableList()
        val newItem = DetectionHistoryItem(
            label = label,
            confidence = confidence,
            timestamp = System.currentTimeMillis()
        )
        currentList.add(0, newItem) // Mais recente primeiro

        // Limita a 100 itens para não sobrecarregar
        val trimmedList = if (currentList.size > 100) currentList.subList(0, 100) else currentList

        saveList(trimmedList)
        notifyListeners()
    }

    fun getHistory(): List<DetectionHistoryItem> {
        val jsonString = storage.getData(KEY_HISTORY, "[]")
        val result = mutableListOf<DetectionHistoryItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    DetectionHistoryItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        label = obj.optString("label", "Desconhecido"),
                        confidence = obj.optDouble("confidence", 0.0).toFloat(),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun clearHistory() {
        storage.saveData(KEY_HISTORY, "[]")
        notifyListeners()
    }

    private fun saveList(items: List<DetectionHistoryItem>) {
        val jsonArray = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("label", item.label)
                put("confidence", item.confidence.toDouble())
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }
        storage.saveData(KEY_HISTORY, jsonArray.toString())
    }

    fun getTotalCount(): Int = getHistory().size

    fun getLastDetection(): DetectionHistoryItem? = getHistory().firstOrNull()
}

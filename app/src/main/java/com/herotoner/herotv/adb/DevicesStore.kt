package com.herotoner.herotv.adb

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** قائمة التلفزيونات المحفوظة (عنوان/منفذ/اسم فقط، بلا أي مفاتيح أو رموز اقتران). */
class DevicesStore(context: Context) {

    private val prefs = context.getSharedPreferences("hero_devices", Context.MODE_PRIVATE)
    private val _devices = MutableStateFlow(load())
    val devices: StateFlow<List<SavedTv>> = _devices.asStateFlow()

    @Synchronized
    fun last(): SavedTv? {
        val id = prefs.getString(KEY_LAST, null) ?: return null
        return _devices.value.firstOrNull { it.id == id }
    }

    @Synchronized
    fun upsert(device: SavedTv) {
        val updated = _devices.value.filterNot { it.id == device.id } + device
        persist(updated)
    }

    @Synchronized
    fun setLast(id: String) {
        prefs.edit().putString(KEY_LAST, id).apply()
    }

    @Synchronized
    fun remove(id: String) {
        persist(_devices.value.filterNot { it.id == id })
        if (prefs.getString(KEY_LAST, null) == id) prefs.edit().remove(KEY_LAST).apply()
    }

    private fun persist(list: List<SavedTv>) {
        val array = JSONArray()
        list.forEach { d ->
            array.put(
                JSONObject()
                    .put("id", d.id)
                    .put("name", d.name)
                    .put("host", d.host)
                    .put("port", d.port)
                    .put("service", d.serviceName ?: JSONObject.NULL)
                    .put("last", d.lastConnectedAt)
            )
        }
        prefs.edit().putString(KEY_DEVICES, array.toString()).apply()
        _devices.value = list
    }

    private fun load(): List<SavedTv> {
        val raw = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                SavedTv(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    host = o.getString("host"),
                    port = o.getInt("port"),
                    serviceName = if (o.isNull("service")) null else o.getString("service"),
                    lastConnectedAt = o.optLong("last", 0L)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private companion object {
        const val KEY_DEVICES = "devices"
        const val KEY_LAST = "last_id"
    }
}

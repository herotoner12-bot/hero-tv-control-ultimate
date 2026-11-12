package com.herotoner.herotv.pad

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** ملفات تخصيص يد الألعاب (مواضع الأزرار ووظائفها) + ربط الألعاب بملف. */
class ProfilesStore(context: Context) {

    private val prefs = context.getSharedPreferences("hero_pad", Context.MODE_PRIVATE)

    private val _profiles = MutableStateFlow(loadProfiles())
    val profiles: StateFlow<List<PadProfile>> = _profiles.asStateFlow()

    private val _activeId = MutableStateFlow(prefs.getString(KEY_ACTIVE, null) ?: _profiles.value.first().id)
    val activeId: StateFlow<String> = _activeId.asStateFlow()

    private val _games = MutableStateFlow(prefs.getStringSet(KEY_GAMES, emptySet()).orEmpty().toSet())
    /** حزم وضع المستخدم عليها علامة «لعبة». */
    val games: StateFlow<Set<String>> = _games.asStateFlow()

    fun active(): PadProfile =
        _profiles.value.firstOrNull { it.id == _activeId.value } ?: _profiles.value.first()

    @Synchronized
    fun save(profile: PadProfile) {
        val list = _profiles.value
        val updated = if (list.any { it.id == profile.id }) list.map { if (it.id == profile.id) profile else it } else list + profile
        persist(updated)
    }

    @Synchronized
    fun create(name: String): PadProfile {
        val profile = PadProfile(id = UUID.randomUUID().toString(), name = name.trim().take(30).ifBlank { "ملف جديد" })
        save(profile)
        setActive(profile.id)
        return profile
    }

    @Synchronized
    fun delete(id: String) {
        if (_profiles.value.size <= 1) return
        val updated = _profiles.value.filterNot { it.id == id }
        persist(updated)
        if (_activeId.value == id) setActive(updated.first().id)
        // نفك ربط الألعاب بهذا الملف
        val editor = prefs.edit()
        _games.value.forEach { pkg -> if (prefs.getString(GAME_PROFILE_PREFIX + pkg, null) == id) editor.remove(GAME_PROFILE_PREFIX + pkg) }
        editor.apply()
    }

    @Synchronized
    fun setActive(id: String) {
        if (_profiles.value.none { it.id == id }) return
        prefs.edit().putString(KEY_ACTIVE, id).apply()
        _activeId.value = id
    }

    @Synchronized
    fun setGame(packageName: String, isGame: Boolean) {
        val updated = if (isGame) _games.value + packageName else _games.value - packageName
        prefs.edit().putStringSet(KEY_GAMES, updated).apply()
        _games.value = updated
    }

    fun profileForGame(packageName: String): String? = prefs.getString(GAME_PROFILE_PREFIX + packageName, null)

    @Synchronized
    fun bindGame(packageName: String, profileId: String) {
        prefs.edit().putString(GAME_PROFILE_PREFIX + packageName, profileId).apply()
    }

    private fun persist(list: List<PadProfile>) {
        val array = JSONArray()
        list.forEach { array.put(toJson(it)) }
        prefs.edit().putString(KEY_PROFILES, array.toString()).apply()
        _profiles.value = list
    }

    private fun loadProfiles(): List<PadProfile> {
        val raw = prefs.getString(KEY_PROFILES, null)
        if (raw != null) {
            try {
                val array = JSONArray(raw)
                val list = (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                // نتجاهل ونرجع للملف الافتراضي
            }
        }
        return listOf(PadProfile(id = DEFAULT_ID, name = "الافتراضي"))
    }

    private fun toJson(p: PadProfile): JSONObject {
        val actions = JSONObject()
        p.actions.forEach { (k, v) -> actions.put(k, v) }
        val positions = JSONObject()
        p.positions.forEach { (k, v) -> positions.put(k, JSONArray().put(v.x.toDouble()).put(v.y.toDouble())) }
        return JSONObject()
            .put("id", p.id)
            .put("name", p.name)
            .put("stick", p.stickMode.id)
            .put("hold", p.holdRepeat)
            .put("actions", actions)
            .put("positions", positions)
    }

    private fun fromJson(o: JSONObject): PadProfile {
        val actions = mutableMapOf<String, String>()
        o.optJSONObject("actions")?.let { a -> a.keys().forEach { k -> actions[k] = a.getString(k) } }
        val positions = mutableMapOf<String, Pos>()
        o.optJSONObject("positions")?.let { p ->
            p.keys().forEach { k ->
                val pair = p.getJSONArray(k)
                positions[k] = Pos(pair.getDouble(0).toFloat(), pair.getDouble(1).toFloat())
            }
        }
        return PadProfile(
            id = o.getString("id"),
            name = o.getString("name"),
            stickMode = StickMode.fromId(o.optString("stick")),
            holdRepeat = o.optBoolean("hold", true),
            actions = actions,
            positions = positions
        )
    }

    private companion object {
        const val KEY_PROFILES = "profiles"
        const val KEY_ACTIVE = "active"
        const val KEY_GAMES = "games"
        const val GAME_PROFILE_PREFIX = "game_profile_"
        const val DEFAULT_ID = "default"
    }
}

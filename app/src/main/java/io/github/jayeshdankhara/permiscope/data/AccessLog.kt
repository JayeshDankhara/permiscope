package io.github.jayeshdankhara.permiscope.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

enum class Sensor { CAMERA, MICROPHONE }

data class AccessEvent(
    val id: Long,
    val sensor: Sensor,
    val start: Long,
    val end: Long?,
    /** Best guess at the app responsible: the app in the foreground when access began. */
    val packageName: String?,
    val appLabel: String?,
    /** Screen was off or locked when access began. */
    val whileLocked: Boolean,
) {
    val durationMs: Long? get() = end?.let { it - start }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("sensor", sensor.name)
        .put("start", start)
        .put("end", end ?: JSONObject.NULL)
        .put("pkg", packageName ?: JSONObject.NULL)
        .put("label", appLabel ?: JSONObject.NULL)
        .put("locked", whileLocked)

    companion object {
        fun fromJson(o: JSONObject) = AccessEvent(
            id = o.getLong("id"),
            sensor = Sensor.valueOf(o.getString("sensor")),
            start = o.getLong("start"),
            end = if (o.isNull("end")) null else o.getLong("end"),
            packageName = if (o.isNull("pkg")) null else o.getString("pkg"),
            appLabel = if (o.isNull("label")) null else o.getString("label"),
            whileLocked = o.optBoolean("locked"),
        )
    }
}

/**
 * Camera and microphone access history, kept for [RETENTION_DAYS] days in a
 * private file. Shared between the monitor service and the UI in one process.
 */
object AccessLog {
    private const val FILE = "access_log.json"
    private const val RETENTION_DAYS = 30L

    private val _events = MutableStateFlow<List<AccessEvent>>(emptyList())
    val events: StateFlow<List<AccessEvent>> = _events.asStateFlow()

    private var loaded = false
    private lateinit var file: File

    @Synchronized
    fun init(context: Context) {
        if (loaded) return
        file = File(context.applicationContext.filesDir, FILE)
        _events.value = runCatching {
            val arr = JSONArray(file.readText())
            List(arr.length()) { AccessEvent.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
        loaded = true
        prune()
    }

    @Synchronized
    fun start(sensor: Sensor, packageName: String?, appLabel: String?, whileLocked: Boolean): AccessEvent {
        val now = System.currentTimeMillis()
        // Camera and microphone often start in the same millisecond; keep ids unique
        val id = maxOf(now, (_events.value.maxOfOrNull { it.id } ?: 0L) + 1)
        val event = AccessEvent(id, sensor, now, null, packageName, appLabel, whileLocked)
        _events.value = listOf(event) + _events.value
        save()
        return event
    }

    @Synchronized
    fun finish(id: Long) {
        val now = System.currentTimeMillis()
        _events.value = _events.value.map { if (it.id == id && it.end == null) it.copy(end = now) else it }
        save()
    }

    /** Close entries left open by a crash or reboot so they don't look "in use" forever. */
    @Synchronized
    fun closeDangling() {
        if (_events.value.none { it.end == null }) return
        _events.value = _events.value.map { if (it.end == null) it.copy(end = it.start) else it }
        save()
    }

    @Synchronized
    fun clear() {
        _events.value = emptyList()
        save()
    }

    private fun prune() {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(RETENTION_DAYS)
        val kept = _events.value.filter { it.start >= cutoff }
        if (kept.size != _events.value.size) {
            _events.value = kept
            save()
        }
    }

    private fun save() {
        if (!loaded) return
        val arr = JSONArray()
        _events.value.forEach { arr.put(it.toJson()) }
        val tmp = File(file.parentFile, "$FILE.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
    }
}

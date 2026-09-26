package com.personal.englishautotalk.record

import android.content.Context
import com.personal.englishautotalk.ai.Correction
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** 회차 기록. 기기 안 파일(한 줄에 한 회차 JSON)에만 저장한다. 음성 원본은 저장하지 않는다. */
data class SessionRecord(
    val startedAt: Long,
    val test: Boolean,
    val outcome: String,
    val topic: String?,
    val aiMode: Boolean,
    val lines: List<Pair<Boolean, String>>, // (사용자 발화 여부, 텍스트)
    val corrections: List<Correction>,
    val praise: String?,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("started_at", startedAt)
        .put("test", test)
        .put("outcome", outcome)
        .put("topic", topic ?: JSONObject.NULL)
        .put("ai_mode", aiMode)
        .put("lines", JSONArray().apply { lines.forEach { put(JSONObject().put("learner", it.first).put("text", it.second)) } })
        .put(
            "corrections",
            JSONArray().apply {
                corrections.forEach { put(JSONObject().put("said", it.said).put("better", it.better).put("note_ko", it.noteKo)) }
            },
        )
        .put("praise", praise ?: JSONObject.NULL)

    companion object {
        fun fromJson(o: JSONObject): SessionRecord {
            val lines = o.optJSONArray("lines") ?: JSONArray()
            val corr = o.optJSONArray("corrections") ?: JSONArray()
            return SessionRecord(
                startedAt = o.getLong("started_at"),
                test = o.optBoolean("test"),
                outcome = o.optString("outcome"),
                topic = if (o.isNull("topic")) null else o.optString("topic"),
                aiMode = o.optBoolean("ai_mode"),
                lines = (0 until lines.length()).map { lines.getJSONObject(it).let { l -> l.getBoolean("learner") to l.getString("text") } },
                corrections = (0 until corr.length()).map {
                    corr.getJSONObject(it).let { c -> Correction(c.getString("said"), c.getString("better"), c.getString("note_ko")) }
                },
                praise = if (o.isNull("praise")) null else o.optString("praise"),
            )
        }
    }
}

object SessionStore {
    private const val FILE = "sessions.jsonl"

    @Synchronized
    fun append(context: Context, record: SessionRecord) {
        File(context.filesDir, FILE).appendText(record.toJson().toString() + "\n")
    }

    fun recent(context: Context, limit: Int = 20): List<SessionRecord> {
        val file = File(context.filesDir, FILE)
        if (!file.exists()) return emptyList()
        return file.readLines().asReversed().asSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { runCatching { SessionRecord.fromJson(JSONObject(it)) }.getOrNull() }
            .take(limit)
            .toList()
    }

    @Synchronized
    fun clear(context: Context) {
        File(context.filesDir, FILE).delete()
    }
}

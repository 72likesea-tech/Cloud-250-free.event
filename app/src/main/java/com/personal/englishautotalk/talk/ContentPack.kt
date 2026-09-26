package com.personal.englishautotalk.talk

import android.content.Context
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime

/** assets/content_pack.json (docs/content/task1-persona-openers.md) 을 읽는다. */
class ContentPack(
    private val placeholder: String,
    private val firstAttempt: List<String>,
    private val retry: List<Pair<IntRange, String>>,
    private val weekend: List<String>,
    val debateQuestions: List<String>,
    val reprompts: List<String>,
    val goodbyeRetry: List<String>,
    val goodbyeTomorrow: List<String>,
    /** (영어 유도 문장, 화면에 보일 한국어 힌트) */
    val koreanNudges: List<Pair<String, String>>,
) {
    /** 날짜별로 돌아가며 30일 주기. */
    fun debateQuestion(date: LocalDate): String =
        debateQuestions[Math.floorMod(date.toEpochDay(), debateQuestions.size.toLong()).toInt()]

    /** 주말 → 주말용, 평일 14시 → 첫 시도용, 그 뒤 → 시간대가 맞는 재시도용. 같은 날·시각이면 같은 문장. */
    fun opening(now: ZonedDateTime, firstHour: Int = 14): String {
        val weekendDay = now.dayOfWeek == DayOfWeek.SATURDAY || now.dayOfWeek == DayOfWeek.SUNDAY
        val candidates = when {
            weekendDay -> weekend
            now.hour <= firstHour -> firstAttempt
            else -> retry.filter { now.hour in it.first }.map { it.second }.ifEmpty { retry.map { it.second } }
        }
        val seed = now.toLocalDate().toEpochDay() * 31 + now.hour
        val template = candidates[Math.floorMod(seed, candidates.size.toLong()).toInt()]
        return template.replace(placeholder, debateQuestion(now.toLocalDate()))
    }

    fun pick(list: List<String>, seed: Int): String = list[Math.floorMod(seed, list.size)]

    companion object {
        fun load(context: Context): ContentPack =
            parse(context.assets.open("content_pack.json").bufferedReader().use { it.readText() })

        fun parse(json: String): ContentPack {
            val root = JSONObject(json)
            val openers = root.getJSONObject("openers")
            val noAnswer = root.getJSONObject("no_answer")
            val retry = openers.getJSONArray("retry").let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    val hours = o.getJSONArray("hours")
                    (hours.getInt(0)..hours.getInt(1)) to o.getString("text")
                }
            }
            val debate = root.getJSONArray("debate_questions").let { arr ->
                (0 until arr.length()).map { arr.getJSONObject(it).getString("text") }
            }
            val nudges = root.getJSONArray("korean_or_stuck_nudges").let { arr ->
                (0 until arr.length()).map { arr.getJSONObject(it).let { o -> o.getString("en") to o.getString("hint_ko") } }
            }
            return ContentPack(
                placeholder = root.getString("placeholder"),
                firstAttempt = openers.getJSONArray("first_attempt").strings(),
                retry = retry,
                weekend = openers.getJSONArray("weekend").strings(),
                debateQuestions = debate,
                reprompts = noAnswer.getJSONArray("reprompt").strings(),
                goodbyeRetry = noAnswer.getJSONArray("goodbye_retry").strings(),
                goodbyeTomorrow = noAnswer.getJSONArray("goodbye_tomorrow").strings(),
                koreanNudges = nudges,
            )
        }

        private fun org.json.JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
    }
}

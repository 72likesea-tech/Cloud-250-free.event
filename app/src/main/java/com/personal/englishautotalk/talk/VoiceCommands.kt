package com.personal.englishautotalk.talk

/**
 * 사용자 음성 명령 (requirements.md ③):
 * - 한국어 도움 요청: "Help me in Korean" 또는 한국어로 말하기
 * - 내 말 끝 신호: "I'm all set" 또는 "Over to you" — 이 말이 나올 때까지 대답을 이어서 듣는다.
 */
object VoiceCommands {

    private val endSignal = Regex(
        """[\s,.!?]*(?:(?:i\s*'?\s*m|i\s+am)\s+all\s+set|over\s+to\s+you)[\s,.!?]*$""",
        RegexOption.IGNORE_CASE,
    )

    private val koreanHelp = Regex("""help\s+me\s+(?:in\s+)?korean?\b""", RegexOption.IGNORE_CASE)

    private val hangul = Regex("[가-힣ㄱ-ㆎ]")

    data class Segment(val text: String, val endOfTurn: Boolean)

    /** 인식된 한 구간에서 끝 신호를 떼어 낸다. 끝 신호는 구간 끝에 있을 때만 인정한다. */
    fun parseSegment(raw: String): Segment {
        val trimmed = raw.trim()
        val match = endSignal.find(trimmed) ?: return Segment(trimmed, endOfTurn = false)
        return Segment(trimmed.substring(0, match.range.first).trim(), endOfTurn = true)
    }

    /** "Help me in Korean"이라고 했거나, 한국어로 말했으면(한글 인식 또는 언어 감지) 한국어 도움 요청이다. */
    fun isKoreanHelpRequest(raw: String, detectedKorean: Boolean = false): Boolean =
        detectedKorean || koreanHelp.containsMatchIn(raw) || hangul.containsMatchIn(raw)
}

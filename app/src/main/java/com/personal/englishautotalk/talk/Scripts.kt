package com.personal.englishautotalk.talk

import java.time.LocalDate

/**
 * 2단계용 고정 문장. 3단계에서 Gemini 응답과 GPT 담당 콘텐츠 팩(assets JSON)으로 대체한다.
 * 주제 질문은 '의견과 토론'이라고 묻지 않고 그날의 구체적 토론 주제를 바로 제시한다.
 */
object Scripts {
    private val debateTopics = listOf(
        "should companies adopt a four-day work week?",
        "is working from home better than working in an office?",
        "should AI be allowed to help make hiring decisions?",
        "is it better to travel alone or with friends?",
        "should kids under sixteen be allowed on social media?",
        "is it worth paying more for eco-friendly products?",
        "should public transportation be free in big cities?",
    )

    fun debateTopic(date: LocalDate): String = debateTopics[date.dayOfYear % debateTopics.size]

    fun opening(topic: String) =
        "Hi there! It's time for a quick English chat. " +
            "What do you want to talk about today: work, travel, or this question: $topic"

    const val REPROMPT = "Are you there? No rush. Just say work, travel, or today's question."

    /** "Help me in Korean" 또는 한국어로 말했을 때(주제 고르기 단계). 3단계에서는 Gemini가 상황에 맞게 만든다. */
    const val KOREAN_HELP_TOPIC =
        "도와드릴게요. 오늘 이야기할 주제를 영어로 하나 골라 주세요. " +
            "일은 워크, 여행은 트래블, 아니면 오늘의 토론 질문이에요. " +
            "다 말하면 오버 투 유 라고 해 주세요."

    const val KOREAN_HELP_EXAMPLE = "예: Let's talk about travel. Over to you."

    const val BACK_TO_ENGLISH = "Okay, now try it in English. Take your time."

    const val GOODBYE_NO_ANSWER = "Okay, looks like you're busy right now. I'll try again in an hour!"

    fun stage2Reply(heard: String) =
        "Great, I heard you say: $heard. That's all for this test. Talk to you soon!"
}

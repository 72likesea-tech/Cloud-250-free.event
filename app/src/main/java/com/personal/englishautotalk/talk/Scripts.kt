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

    const val GOODBYE_NO_ANSWER = "Okay, looks like you're busy right now. I'll try again in an hour!"

    fun stage2Reply(heard: String) =
        "Great, I heard you say: $heard. That's all for this test. Talk to you soon!"
}

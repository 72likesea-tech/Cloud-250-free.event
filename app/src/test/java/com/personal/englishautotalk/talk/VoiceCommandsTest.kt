package com.personal.englishautotalk.talk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandsTest {

    @Test fun endSignalsAreStripped() {
        assertEquals(VoiceCommands.Segment("I went to Busan", true), VoiceCommands.parseSegment("I went to Busan. I'm all set"))
        assertEquals(VoiceCommands.Segment("I went to Busan", true), VoiceCommands.parseSegment("I went to Busan over to you"))
        assertEquals(VoiceCommands.Segment("travel", true), VoiceCommands.parseSegment("travel, Over to you."))
        assertEquals(VoiceCommands.Segment("work", true), VoiceCommands.parseSegment("work im all set"))
        assertEquals(VoiceCommands.Segment("work", true), VoiceCommands.parseSegment("work I am all set!"))
    }

    @Test fun endSignalAloneEndsTurnWithEmptyText() {
        assertEquals(VoiceCommands.Segment("", true), VoiceCommands.parseSegment("Over to you"))
        assertEquals(VoiceCommands.Segment("", true), VoiceCommands.parseSegment("I'm all set."))
    }

    @Test fun textWithoutSignalKeepsListening() {
        assertEquals(VoiceCommands.Segment("I like to travel", false), VoiceCommands.parseSegment("I like to travel"))
        // 끝 신호가 문장 중간에 있으면 인정하지 않는다.
        assertFalse(VoiceCommands.parseSegment("I'm all set for the trip next week").endOfTurn)
    }

    @Test fun koreanHelpRequests() {
        assertTrue(VoiceCommands.isKoreanHelpRequest("Help me in Korean"))
        assertTrue(VoiceCommands.isKoreanHelpRequest("um help me in korean please"))
        assertTrue(VoiceCommands.isKoreanHelpRequest("help me in Korea"))
        assertTrue(VoiceCommands.isKoreanHelpRequest("잘 모르겠어요"))
        assertTrue(VoiceCommands.isKoreanHelpRequest("anything", detectedKorean = true))
        assertFalse(VoiceCommands.isKoreanHelpRequest("I want to talk about Korean food"))
        assertFalse(VoiceCommands.isKoreanHelpRequest("Can you help me with work"))
    }
}

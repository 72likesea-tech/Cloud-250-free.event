package com.personal.englishautotalk.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiClientTest {

    private fun client(vararg results: GeminiClient.Result, slept: MutableList<Long>, calls: MutableList<String>): GeminiClient {
        val queue = ArrayDeque(results.toList())
        return GeminiClient("k", "gemini-3.5-flash-lite", sleeper = { slept += it }) { method, url, _ ->
            calls += "$method $url"
            queue.removeFirst()
        }
    }

    @Test fun serverErrorsRetryTwiceWithBackoff() {
        val slept = mutableListOf<Long>()
        val calls = mutableListOf<String>()
        val fail = GeminiClient.Result.Fail(ApiError.SERVER, 503)
        val r = client(fail, fail, fail, slept = slept, calls = calls).generate("{}")
        assertEquals(fail, r)
        assertEquals(3, calls.size)
        assertEquals(listOf(1_000L, 3_000L), slept)
        assertTrue(calls[0].endsWith("/models/gemini-3.5-flash-lite:generateContent"))
    }

    @Test fun quotaErrorIsNotRetried() {
        val slept = mutableListOf<Long>()
        val calls = mutableListOf<String>()
        val quota = GeminiClient.Result.Fail(ApiError.QUOTA, 429)
        assertEquals(quota, client(quota, slept = slept, calls = calls).generate("{}"))
        assertEquals(1, calls.size)
        assertTrue(slept.isEmpty())
    }

    @Test fun recoversAfterOneRetry() {
        val slept = mutableListOf<Long>()
        val calls = mutableListOf<String>()
        val ok = GeminiClient.Result.Ok("{}")
        assertEquals(ok, client(GeminiClient.Result.Fail(ApiError.NETWORK, 0), ok, slept = slept, calls = calls).generate("{}"))
        assertEquals(listOf(1_000L), slept)
    }

    @Test fun connectionTestUsesGetOnModel() {
        val calls = mutableListOf<String>()
        client(GeminiClient.Result.Ok("{}"), slept = mutableListOf(), calls = calls).testModel()
        assertTrue(calls.single().startsWith("GET ") && calls.single().endsWith("/models/gemini-3.5-flash-lite"))
    }
}

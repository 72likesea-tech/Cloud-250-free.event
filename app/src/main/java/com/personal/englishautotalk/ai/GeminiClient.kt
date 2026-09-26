package com.personal.englishautotalk.ai

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gemini REST 호출. 메인 스레드에서 부르지 않는다.
 * 재시도는 서버 오류·네트워크 오류에만, 최대 2회(1초, 3초 뒤). 429·4xx는 재시도하지 않는다.
 */
class GeminiClient(
    private val apiKey: String,
    private val model: String,
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) },
    /** 테스트에서 네트워크 대신 넣는 전송 함수 (method, url, body). */
    transport: ((String, String, String?) -> Result)? = null,
) {
    private val send: (String, String, String?) -> Result = transport ?: ::call

    sealed interface Result {
        data class Ok(val body: String) : Result
        data class Fail(val error: ApiError, val httpCode: Int) : Result
    }

    fun generate(requestBody: String): Result =
        withRetry { send("POST", "$BASE/models/$model:generateContent", requestBody) }

    /** 설정 화면의 연결 테스트: 이 키로 이 모델 정보를 읽을 수 있는지 확인한다(생성 한도를 쓰지 않음). */
    fun testModel(): Result = send("GET", "$BASE/models/$model", null)

    private fun withRetry(block: () -> Result): Result {
        var result = block()
        for (delay in RETRY_DELAYS_MS) {
            if (result !is Result.Fail || !result.error.retryable) return result
            sleeper(delay)
            result = block()
        }
        return result
    }

    private fun call(method: String, url: String, body: String?): Result {
        val conn = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            return Result.Fail(ApiError.NETWORK, 0)
        }
        return try {
            conn.requestMethod = method
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.setRequestProperty("x-goog-api-key", apiKey)
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code in 200..299) Result.Ok(text) else Result.Fail(GeminiProtocol.classify(code, text), code)
        } catch (e: IOException) {
            Result.Fail(ApiError.NETWORK, 0)
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val BASE = "https://generativelanguage.googleapis.com/v1beta"
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val READ_TIMEOUT_MS = 15_000
        private val RETRY_DELAYS_MS = listOf(1_000L, 3_000L)
    }
}

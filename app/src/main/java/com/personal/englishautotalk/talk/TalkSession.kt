package com.personal.englishautotalk.talk

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.personal.englishautotalk.schedule.AlarmScheduler
import com.personal.englishautotalk.schedule.SchedulePolicy
import com.personal.englishautotalk.schedule.ScheduleStore
import com.personal.englishautotalk.util.DeviceState
import com.personal.englishautotalk.util.EventLog
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale

enum class Phase { WAITING_TAP, PREPARING, SPEAKING, LISTENING, ENDED }

enum class Outcome(val label: String, val completesToday: Boolean) {
    ANSWERED("대화 완료", true),
    SKIPPED_TODAY("오늘은 그만", true),
    PASSED("패스 — 1시간 뒤 다시", false),
    NO_ANSWER("대답 없음 — 1시간 뒤 다시", false),
    NO_TAP("방해금지 중 30초 무반응 — 1시간 뒤 다시", false),
    INTERRUPTED("화면 이탈로 중단 — 1시간 뒤 다시", false),
    ERROR("오류로 종료 — 1시간 뒤 다시", false),
}

data class Line(val fromApp: Boolean, val text: String)

/**
 * 한 번의 말걸기 흐름: (방해금지면 터치 대기) → 첫 문장 음성 → 자동 듣기 →
 * 8초 무응답이면 1회 다시 말걸기 → 그래도 없으면 종료.
 * 모든 상태 변경은 메인 스레드에서 한다.
 */
class TalkSession(
    private val context: Context,
    val mode: TalkMode,
    private val test: Boolean,
    private val onEnded: (Outcome) -> Unit,
) {
    var phase by mutableStateOf(if (mode == TalkMode.DND) Phase.WAITING_TAP else Phase.PREPARING)
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var partial by mutableStateOf("")
        private set
    val lines = mutableStateListOf<Line>()

    private val handler = Handler(Looper.getMainLooper())
    private val topic = Scripts.debateTopic(LocalDate.now())
    private val afterSpeech = mutableMapOf<String, () -> Unit>()
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var recognizer: SpeechRecognizer? = null
    private var utteranceSeq = 0
    private var began = false
    private var reprompted = false
    private var heardSpeech = false
    private var listenStartedAt = 0L

    private val prefix get() = if (test) "[테스트] " else ""

    fun start() {
        if (DeviceState.isMediaMuted(context)) notice = "미디어 볼륨이 0이라 소리가 들리지 않습니다"
        tts = TextToSpeech(context) { status -> handler.post { onTtsInit(status) } }
        if (mode == TalkMode.DND) {
            vibrate()
            handler.postDelayed(tapTimeout, TAP_TIMEOUT_MS)
        }
    }

    private val tapTimeout = Runnable { finish(Outcome.NO_TAP) }

    fun onTapToStart() {
        if (phase != Phase.WAITING_TAP) return
        handler.removeCallbacks(tapTimeout)
        phase = Phase.PREPARING
        maybeBegin()
    }

    fun pass() = finish(Outcome.PASSED)

    fun skipToday() = finish(Outcome.SKIPPED_TODAY)

    fun interrupt() = finish(Outcome.INTERRUPTED)

    private fun onTtsInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            notice = "음성 출력(TTS)을 시작할 수 없습니다"
            finish(Outcome.ERROR)
            return
        }
        val lang = engine.setLanguage(Locale.US)
        if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
            notice = "영어 음성 데이터가 없습니다. 설정 → 텍스트 읽어주기에서 영어(미국)를 설치하세요"
            finish(Outcome.ERROR)
            return
        }
        // 미디어 스트림으로 내보내면 이어폰이 연결된 경우 이어폰으로 나온다.
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit
            override fun onDone(utteranceId: String) {
                handler.post { afterSpeech.remove(utteranceId)?.invoke() }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                handler.post {
                    notice = "음성 출력 오류"
                    afterSpeech.remove(utteranceId)?.invoke()
                }
            }
        })
        ttsReady = true
        maybeBegin()
    }

    private fun maybeBegin() {
        if (began || !ttsReady || phase != Phase.PREPARING) return
        began = true
        EventLog.add(context, "${prefix}첫 문장 재생 시작")
        say(Scripts.opening(topic)) { listen() }
    }

    private fun say(text: String, then: () -> Unit) {
        if (phase == Phase.ENDED) return
        phase = Phase.SPEAKING
        lines += Line(fromApp = true, text = text)
        val id = "u${++utteranceSeq}"
        afterSpeech[id] = then
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    private fun listen() {
        if (phase == Phase.ENDED) return
        if (!DeviceState.canRecordAudio(context)) {
            notice = "마이크 권한이 없어 대답을 들을 수 없습니다"
            finish(Outcome.ERROR)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            notice = "이 기기에서 음성 인식을 사용할 수 없습니다"
            finish(Outcome.ERROR)
            return
        }
        phase = Phase.LISTENING
        heardSpeech = false
        partial = ""
        listenStartedAt = SystemClock.elapsedRealtime()
        startRecognizer()
        handler.postDelayed(silenceTimeout, ANSWER_WAIT_MS)
    }

    private fun startRecognizer() {
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true),
        )
    }

    private val silenceTimeout = Runnable {
        if (phase == Phase.LISTENING && !heardSpeech) {
            recognizer?.cancel()
            onNoAnswer()
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() {
            heardSpeech = true
            handler.removeCallbacks(silenceTimeout)
        }
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.let { partial = it }
        }

        override fun onResults(results: Bundle?) {
            if (phase != Phase.LISTENING) return
            handler.removeCallbacks(silenceTimeout)
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (text.isNullOrBlank()) onNoAnswer() else onUserSaid(text)
        }

        override fun onError(error: Int) {
            if (phase != Phase.LISTENING) return
            val elapsed = SystemClock.elapsedRealtime() - listenStartedAt
            val silence = error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH
            if (silence && !heardSpeech && elapsed < ANSWER_WAIT_MS) {
                // 인식기 자체 무음 제한이 8초보다 짧을 수 있어, 8초가 찰 때까지 다시 듣는다.
                startRecognizer()
                return
            }
            handler.removeCallbacks(silenceTimeout)
            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                notice = "마이크 권한이 없어 대답을 들을 수 없습니다"
                finish(Outcome.ERROR)
                return
            }
            if (!silence) notice = "음성 인식 오류 (코드 $error)"
            onNoAnswer()
        }
    }

    private fun onUserSaid(text: String) {
        partial = ""
        lines += Line(fromApp = false, text = text)
        EventLog.add(context, "${prefix}사용자 대답 인식")
        say(Scripts.stage2Reply(text)) { finish(Outcome.ANSWERED) }
    }

    private fun onNoAnswer() {
        partial = ""
        if (!reprompted) {
            reprompted = true
            say(Scripts.REPROMPT) { listen() }
        } else {
            say(Scripts.GOODBYE_NO_ANSWER) { finish(Outcome.NO_ANSWER) }
        }
    }

    private fun vibrate() {
        val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400, 600, 400, 600), -1))
    }

    fun finish(outcome: Outcome) {
        if (phase == Phase.ENDED) return
        phase = Phase.ENDED
        handler.removeCallbacksAndMessages(null)
        afterSpeech.clear()
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        record(outcome)
        onEnded(outcome)
    }

    private fun record(outcome: Outcome) {
        EventLog.add(context, "${prefix}세션 종료: ${outcome.label}")
        if (test) return
        val now = ZonedDateTime.now()
        val store = ScheduleStore(context)
        if (outcome.completesToday) {
            store.completedDate = now.toLocalDate()
            AlarmScheduler.scheduleDaily(context, SchedulePolicy.nextFirstAttempt(now, store.completedDate))
        } else {
            // 알람 도착 시 이미 재시도를 예약했지만, '1시간 뒤'가 종료 시점 기준이 되도록 다시 맞춘다.
            AlarmScheduler.scheduleDaily(context, SchedulePolicy.retryAfter(now, store.completedDate))
        }
    }

    companion object {
        const val ANSWER_WAIT_MS = 8_000L
        const val TAP_TIMEOUT_MS = 30_000L
    }
}

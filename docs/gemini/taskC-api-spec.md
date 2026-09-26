# Gemini 작업 C — API 연동 사양서 (3단계 구현 기준)

- 작성: Claude (원래 Gemini 담당 작업). 확인 날짜 **2026-09-26**.
- 근거: `taskA-free-tier.md`, `taskB-android16-constraints.md`, `taskD-system-prompts.md`.
- 선택한 구성은 **(1) 텍스트 모델 + Android TTS/STT**.

## 1. 모델과 설정

| 항목 | 값 | 비고 |
| --- | --- | --- |
| 대화 모델 | 설정값, 기본 **[사용자 결정 대기]** `gemini-2.5-flash-lite` 또는 `gemini-3.5-flash-lite` | 2.5는 새 사용자 차단 가능성이 있음(작업 A) |
| 요약 모델 | 대화 모델과 같게 | 인터뷰 결정: 요약도 flash-lite |
| 엔드포인트 | `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent` | [공식-GH] |
| 인증 | 헤더 `x-goog-api-key: {key}` | 키는 Keystore에서 복호화. 로그 출력 금지 |
| 연결 테스트 | `GET https://generativelanguage.googleapis.com/v1beta/models/{model}` | 설정 화면의 **연결 테스트** 버튼. 200이면 ✅, 404면 "이 키로는 이 모델을 쓸 수 없음" + 다른 모델 제안 |
| 타임아웃 | 연결 5초, 응답 15초 | |
| 재시도 | 500/503/504·타임아웃만 **최대 2회**(1초, 3초 뒤) | 429·4xx는 재시도하지 않음 |

## 2. 턴 종류

| 턴 | 누가 만드나 | AI 호출 | 설명 |
| --- | --- | --- | --- |
| 오프닝 | **앱** (`content_pack.json`의 오프닝 + 토론 질문) | 없음 | 인터넷 없이도 나온다. 그래서 전날 밤에 미리 생성할 필요가 없다 |
| 재말걸기·작별 | 앱 (content pack) | 없음 | 8초 무응답 규칙 |
| 응답 턴 | Gemini | 1회/턴 | 사용자가 끝 신호를 말하면 전송 |
| 한국어 도움 | Gemini | 1회 | 같은 대화 프롬프트. 사용자 턴 텍스트를 `[HELP_KO]` 표식과 함께 보냄. 실패하면 content pack의 한국어 힌트 사용 |
| 마무리(wrap_up) | Gemini | 1회 | phase=`wrap_up`으로 보냄 |
| 종료 요약 | Gemini | 1회 | JSON 스키마 강제 |

## 3. 요청 형식

### 3-1. 응답 턴
```json
{
  "systemInstruction": {"parts": [{"text": "<taskD §1 대화 프롬프트, {{…}} 채움>"}]},
  "contents": [
    {"role": "model", "parts": [{"text": "Hi there! It's English time. Pick one for today: work, travel, or this question: Should companies adopt a four-day work week?"}]},
    {"role": "user",  "parts": [{"text": "uh four day work week. I think is good."}]}
  ],
  "generationConfig": {
    "temperature": 0.7,
    "maxOutputTokens": 150
  }
}
```
- **대화 이력:** 앱 오프닝부터 모든 턴을 `contents`에 누적한다. 최근 **20개 메시지**까지만 보내고, 앞부분은 버린다(10분 대화면 넘지 않음).
- `contents`의 첫 메시지가 `model` 역할이어도 된다. 호환 문제가 있으면 맨 앞에 `{"role":"user","parts":[{"text":"(start)"}]}`를 넣는다 [확인 필요 — 실제 키로 테스트].
- 사용자 텍스트에서 끝 신호("I'm all set", "Over to you")는 **앱이 미리 제거**한다.
- 한국어 도움 턴은 사용자 텍스트 앞에 `[HELP_KO] `를 붙인다. 시스템 프롬프트의 음성 명령 규칙을 따른다.
- 3.x 모델의 사고(thinking) 설정은 기본값을 쓴다. 지연이 크면 `thinkingConfig` 조정을 검토한다 [확인 필요].

### 3-2. 종료 요약
```json
{
  "systemInstruction": {"parts": [{"text": "<taskD §2 요약 프롬프트>"}]},
  "contents": [{"role": "user", "parts": [{"text": "Mia: Hi there! ...\nLearner: uh four day work week. I think is good.\nMia: Oh, you think it's a good idea? ...\n..."}]}],
  "generationConfig": {
    "temperature": 0.3,
    "maxOutputTokens": 600,
    "responseMimeType": "application/json",
    "responseSchema": "<taskD §2 스키마>"
  }
}
```
- 파싱에 실패하면 1회만 다시 요청한다. 그래도 실패하면 "요약을 만들지 못했어요"라고 표시하고 대화 기록만 저장한다.

### 3-3. 응답 읽기
- 텍스트: `candidates[0].content.parts[*].text`를 이어 붙인다.
- `finishReason`이 `MAX_TOKENS`면 마지막 문장까지만 자르고 TTS로 읽는다.
- `SAFETY` 등으로 막히면 기본 연습 문장으로 대체한다.
- 응답에 이모지나 마크다운이 섞여 있으면 TTS 전에 제거한다.

## 4. 오류별 앱 동작
작업 A §3 표를 그대로 따른다. 요약:
- **429 →** 재시도하지 않고 즉시 **기본 연습(AI 아님)**으로 전환. "오늘 무료 한도를 다 썼어요" 표시.
- **5xx/타임아웃 →** 최대 2회 재시도한 뒤 기본 연습으로 전환.
- **404 →** 모델 변경 안내.
- **403 →** 키 확인 안내.
- **오프라인 →** 기본 연습으로 전환.
- 모든 경우 사용자가 한 말은 기록에 저장한다. 유료 모델 자동 전환·키 교체·무제한 재시도는 없다.

## 5. 기본 연습 모드 (AI 없이)
- content pack을 쓴다.
  - 주제별 고정 후속 질문 → GPT 작업 2(콘텐츠 팩 확장)로 채울 예정.
  - 그 전에는 오프닝 + 토론 질문 + "Tell me more." / "Why do you think so?" 같은 범용 질문 순환.
- 화면 상단에 **"기본 연습 — AI 응답이 아닙니다"** 띠를 고정한다.
- 교정 요약은 만들지 않는다. 대화 기록만 저장한다.

## 6. 음성 설정
- TTS: Android 기본 엔진, 영어(미국), 속도 0.95. 한국어 도움은 한국어(ko-KR)로 읽는다.
- STT: Android SpeechRecognizer, en-US. Android 14 이상에서는 en-US/ko-KR 언어 감지를 요청한다.
- 오디오 출력은 USAGE_MEDIA(이어폰 연결 시 이어폰).

## 7. 보안·개인정보
- 키는 설정 화면에서 입력받아 **Android Keystore(AES-GCM)로 암호화**해 저장하고, 백업에서 제외한다(이미 `allowBackup=false`와 data-extraction 규칙 적용).
- 키를 화면에 다시 표시하지 않는다. 마지막 4자리만 표시한다.
- 로그에 키, 요청 본문, 응답 본문을 남기지 않는다. 이벤트 로그에는 상태 코드만 남긴다.
- 설정 화면에 안내 문구를 넣는다: "무료 Gemini는 대화 내용을 Google 제품 개선에 쓸 수 있어요. 개인정보·회사 기밀은 말하지 마세요."
- 권장: Cloud Console에서 키를 Generative Language API 전용으로 제한한다.

## 8. 3단계 구현 체크리스트
1. `GeminiClient` (HttpURLConnection, 추가 라이브러리 없음), 오류 분류
2. `KeyStore` 암호화 저장, 설정 화면(키 입력, 모델 선택, 연결 테스트)
3. `ConversationEngine`: phase, 턴 수, 경과 시간, 완료 기준(5분 또는 8왕복), 10분 마무리
4. `TalkSession` 연결: 오프닝(content pack) → 끝 신호 → Gemini 응답 → TTS 반복
5. 종료 요약 JSON → 결과 화면(교정 3개 + 칭찬) → Room에 기록 저장
6. 기본 연습 모드 + "AI 아님" 표시
7. 단위 테스트: 오류 분류, 요청 JSON 생성, 요약 JSON 파싱, 완료 기준

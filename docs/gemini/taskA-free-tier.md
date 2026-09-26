# Gemini 작업 A — 대화 엔진 선택과 무료 티어 확인표

- 작성: Claude (원래 Gemini 담당 작업), 확인 날짜 **2026-09-26**
- **확인 방법의 한계:** 이 환경에서는 ai.google.dev와 firebase.google.com을 직접 열 수 없었다.
  - **[공식-GH]** Google 공식 GitHub 저장소(`google-gemini/cookbook`, `googleapis/python-genai`, `google-gemini/gemini-skills`, 2026-09-23~25 커밋)를 직접 읽은 것
  - **[공식-검색요약]** 공식 페이지의 검색 결과 요약만 본 것 (원문 미확인)
  - **[2차]** 제3자 자료
- **API 키로 직접 호출하는 시험은 아직 못 했다.** 결론은 실제 키로 한 번 더 확인해야 한다.

## 결론 (먼저 읽기)

1. **`gemini-2.5-flash-lite`는 새 사용자에게 막혔을 가능성이 크다.**
   - 2026-09-18 Gemini API 변경 기록 인용 [2차, 공식 문구 인용]: *"we are limiting access to the 2.5 models to users who have actively used them in the past … For any new projects, use our latest models: 3.5 Flash-Lite or 3.8 Flash."*
   - 이전에 2.5를 써 본 프로젝트의 키라면 계속 쓸 수 있다. 처음 쓰는 키라면 404(`no longer available to new users`)가 날 수 있다.
   - 공식 이전 안내표 [공식-GH]: `gemini-2.5-flash-lite` → **`gemini-3.5-flash-lite`**.
   - 지원 종료일은 자료마다 다르다: "종료일 미정" / 2026-10-16 / 2026-10-20. **확인 불가.**
2. **무료 티어에서는 보낸 대화가 Google 제품 개선에 쓰이고, 사람이 검토할 수도 있다** [공식-검색요약, Gemini API Additional Terms].
   - 원문: "Do not submit sensitive, confidential, or personal information to the Unpaid Services."
   - 따라서 대화에서 개인정보나 회사 기밀을 말하지 않도록 앱에 안내 문구를 넣는다.
3. **추천: 1차 버전은 구성 (1) 텍스트 모델 + Android 음성 출력·인식으로 간다. 모델은 설정에서 바꿀 수 있게 한다.**
   - 앱의 **연결 테스트** 버튼으로 `gemini-2.5-flash-lite`가 사용자 키로 되는지 확인한다.
   - 안 되면 `gemini-3.5-flash-lite`로 바꾼다. 이 모델은 무료라서 월 0원 조건과 충돌하지 않는다.

## 1. 구성 비교

| 항목 | (1) 텍스트 모델 + Android TTS/STT | (2) Live API 실시간 음성 |
| --- | --- | --- |
| 모델 | `gemini-2.5-flash-lite`(기존 사용자만) / **`gemini-3.5-flash-lite`** / `gemini-3.1-flash-lite`(장기 안정판) [공식-GH] | `gemini-3.8-live`(기본, 2026-09 정식 출시) [공식-GH, 공식-검색요약] |
| 무료 한도 | 프로젝트 단위. 3.5-flash-lite 약 **15 RPM / 250K TPM / 500 RPD** [2차 — 공식 수치는 AI Studio `aistudio.google.com/rate-limit`에서 확인] | 동시 세션·일일 한도 **확인 불가** (2.5 Live는 동시 3세션이라는 2차 자료만 있음) |
| 세션 제약 | 없음 (요청 단위) | 음성 세션 15분, 연결 약 10분(재개 기능 필요) [공식-GH] |
| 키 보안 | 앱 안에 키 저장. Google 권장은 Firebase AI Logic + App Check [공식-검색요약] | 클라이언트는 **임시 토큰(ephemeral token)** 필수 권장 → 토큰 발급 서버가 필요 [공식-GH] |
| 무료 데이터 조건 | 제품 개선에 사용, 사람 검토 가능 | 같음 |
| 결제 없는 프로젝트 | 가능 | 가능하다는 2차 자료만 있음 |
| 구현 난이도 | 낮음 (이미 만든 음성 흐름 재사용) | 높음 (WebSocket 오디오 스트리밍, 토큰 서버) |

**추천: (1).** 이유 세 가지.
- 무료 한도 여유가 크다.
- 서버 없이 월 0원을 유지할 수 있다.
- "끝 신호까지 듣기", "한국어 도움" 같은 흐름을 앱이 직접 제어할 수 있다.

(2)는 토큰 발급 서버가 필요하고 무료 한도도 확인할 수 없어 이번 범위에서는 뺀다.

## 2. 하루 호출량 계산

전제는 하루 1회, 약 10왕복, 요약 1회다. 대화가 성립하지 않은 재시도에는 호출이 없다.

| 요청 | 횟수/일 | 1회당 토큰(추정) | 합계 |
| --- | --- | --- | --- |
| 대화 응답 (지시문 약 600 + 누적 대화 최대 약 1,500 입력 / 출력 약 60) | 10 | 평균 약 1,500 | 약 15,000 |
| 한국어 도움 (가끔) | 0~2 | 약 1,500 | 약 3,000 |
| 종료 요약 (JSON) | 1 | 입력 약 2,500 / 출력 약 300 | 약 2,800 |
| 오프닝 | 0 | 앱 내장 문장 (AI 호출 없음) | 0 |
| **합계** | **약 12~13회** | | **약 21,000 토큰** |

**무료 한도 대비 여유율** (3.5-flash-lite 2차 수치 기준)

| 한도 | 사용량 | 여유 |
| --- | --- | --- |
| RPD 500 | 13회 | **약 2.6% 사용, 97% 여유** |
| 분당 요청 | 대화는 사람이 말하는 속도라 분당 3~4회 이하 | RPM 15에 걸리지 않음 |
| TPM | 분당 약 6,000 토큰 이하 | 250K 대비 약 2% |

- 무료 한도가 **하루 20회**까지 줄어든 모델(3.x Flash 계열 [2차])을 쓰면 여유가 거의 없다.
- 그래서 대화 모델은 **Flash-Lite 계열**로 한다.

## 3. 한도 초과·오류 때 앱 동작 (유료 전환·키 교체·무제한 재시도 금지)

| 응답 | 의미 | 앱 동작 |
| --- | --- | --- |
| 429 RESOURCE_EXHAUSTED | 분당/일일 한도 초과 | 재시도하지 않는다. 즉시 **'기본 연습(AI 아님)'**으로 전환하고 "오늘 무료 한도를 다 썼어요" 표시. 일일 한도는 태평양 시간 자정에 초기화 [공식-검색요약] |
| 500 / 503 / 504 | 서버 일시 오류 | 1초, 3초 간격으로 **최대 2회**만 재시도한다. 그래도 실패하면 기본 연습으로 전환 |
| 404 NOT_FOUND | 모델 없음 / 2.5 새 사용자 차단 | 기본 연습으로 전환하고 "설정에서 모델을 gemini-3.5-flash-lite로 바꾸세요" 안내 |
| 403 PERMISSION_DENIED | 키 오류 또는 제한 불일치 | 기본 연습으로 전환하고 "API 키를 확인하세요" 안내 |
| 400 FAILED_PRECONDITION | 국가 또는 결제 조건 | 기본 연습으로 전환하고 상태 설명 |
| 네트워크 없음 | 오프라인 | 기본 연습으로 전환 |

어느 경우든 사용자가 한 말은 기록에 남기고, 화면에 "AI 아님" 표시를 붙인다.

## 4. 앱 키 보안 (공식 권장과 이 앱의 선택)

- **Google 권장** [공식-검색요약]: 모바일에서 직접 호출할 때는 Firebase AI Logic + App Check를 쓴다. 키를 앱에 넣지 않는 방식이다.
- **이 앱의 선택:** 개인용·결제 미연결·월 0원이므로 기기에서 키를 입력하고 **Android Keystore로 암호화 저장**, 백업 제외(작업지시서 기준).
  - 키가 새어도 결제가 없으므로 요금은 발생하지 않는다. 무료 한도를 다 써 버릴 수는 있다.
  - Cloud Console에서 키를 **Generative Language API 전용**으로 제한하기를 권장한다.
  - Android 앱 제한(패키지명+SHA-1)도 추가하면 좋다. 다만 헤더를 흉내 내면 뚫린다 [2차].
- Firebase AI Logic 전환은 서버 설정이 필요하므로 이번 범위에서 제외한다. 알려진 제약으로 기록한다.

## 5. 사용자가 확인·결정할 것

1. **모델 기본값:** `gemini-2.5-flash-lite`(인터뷰 결정, 새 사용자 차단 가능성) vs `gemini-3.5-flash-lite`(공식 후속 모델). 어느 쪽이든 설정에서 바꿀 수 있고, 앱의 연결 테스트로 확인한다.
2. **무료 데이터 조건 동의:** 대화 텍스트가 Google 제품 개선에 쓰이고 사람이 검토할 수 있다. 개인정보·회사 기밀을 말하지 않는 것을 전제로 한다.
3. **실제 한도 확인:** aistudio.google.com/rate-limit에서 이 프로젝트의 실제 RPM/RPD를 보고 알려 주면 이 문서의 2차 수치를 교체한다.

## 출처
- 공식-GH: https://github.com/google-gemini/gemini-skills (`skills/gemini-api-dev/SKILL.md`, `skills/gemini-live-api-dev/SKILL.md`), https://github.com/google-gemini/cookbook (`quickstarts/rest/System_instructions_REST.ipynb`), https://github.com/googleapis/python-genai
- 공식-검색요약: https://ai.google.dev/gemini-api/docs/changelog, https://ai.google.dev/gemini-api/docs/deprecations, https://ai.google.dev/gemini-api/docs/rate-limits, https://ai.google.dev/gemini-api/docs/pricing, https://ai.google.dev/gemini-api/terms, https://ai.google.dev/gemini-api/docs/live-session, https://ai.google.dev/gemini-api/docs/troubleshooting, https://firebase.google.com/docs/ai-logic
- 2차: https://github.com/ray-amjad/hyperwhisper-app/issues/1019, https://discuss.ai.google.dev/t/gemini-2-5-flash-lite-retirement-date-different-for-gemini-api-vs-vertex-ai/177897, https://dev.to/romeroyang/geminis-free-tier-measured-20-requests-a-day-and-google-no-longer-publishes-the-number-4gf2, https://www.cloudsek.com/blog/hardcoded-google-api-keys-in-top-android-apps-now-expose-gemini-ai

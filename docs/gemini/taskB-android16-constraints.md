# Gemini 작업 B — Android 16 자동 시작 제약 확인표

- 작성: Claude (원래 Gemini 담당 작업). 확인 날짜 **2026-09-26**.
- 대상: Galaxy S24 Ultra · One UI 8.5 · Android 16 · targetSdk 36 · 사이드로드 APK · 한국.
- 확인 방법:
  - developer.android.com은 원문을 직접 받아 확인했다.
  - source.android.com, samsung.com, dontkillmyapp.com은 이 환경에서 막혀 있어 검색 요약만 봤다. 이런 항목은 **확인 필요**로 표시한다.
- 분류: **자동** = 앱이 알아서 처리 / **사용자** = 사용자가 설정해야 함 / **불가** = 앱이 바꿀 수 없음.

## 확인표

| # | 항목 | 확인 내용 | 분류 | 앱 반영 |
| --- | --- | --- | --- | --- |
| 1 | 정확 알람 | `USE_EXACT_ALARM`은 **설치할 때 부여되고 사용자가 철회할 수 없다**. Play 제한은 스토어 정책일 뿐이라 사이드로드도 부여된다. 이 권한을 가진 앱은 대기 버킷이 WORKING_SET 이하로 유지된다. `SCHEDULE_EXACT_ALARM`(API 33 이상)은 Android 14부터 기본 거부다. 이 앱은 maxSdk 32로만 선언해서 해당 없다. `canScheduleExactAlarms()`가 true를 돌려주는지는 문구상 **확인 필요**. | 자동 | 이미 반영. 홈 화면 ✅/⚠️로 실측 |
| 2 | 전체화면 알림 권한 | targetSdk 34 이상에서는 통화·알람 앱으로 제한된다. 기본 권한 회수는 **Play Store가 설치할 때** 하는 조치다. AOSP 요약에 따르면 기본값은 켜짐이라 사이드로드는 켜져 있을 가능성이 높다(**확인 필요**). 권한이 없으면 잠금 상태에서도 **확장된 헤드업 알림이 60초만** 뜨고 화면은 열리지 않는다. 채널 IMPORTANCE_HIGH와 알림 권한이 필수다. | 자동(추정) / 꺼져 있으면 사용자 | 이미 반영: 상태 확인 + 설정 화면 이동 버튼 |
| 3 | 잠금·화면 꺼짐 상태에서 화면 열기 | 백그라운드 실행 제한의 예외에 "시스템이 보낸 PendingIntent(알림)"와 알람 시계가 있다. 전체화면 알림 경로로는 열린다. BroadcastReceiver에서 `startActivity`를 직접 부르면 막힌다. | 자동 | 이미 전체화면 알림 경로만 사용 |
| 3b | 화면 켜짐 + 잠금 해제 + 사용 중 | Android 13 이상에서는 전체화면 대신 **헤드업 알림**이 뜨고, 권한이 있으면 사용자가 닫을 때까지 유지된다. 탭하면 시작한다. | 불가 | 검수 안내서에 명시함 |
| 4 | 잠금화면 위 마이크·음성 | 잠금화면 위에 보이는(resumed) Activity는 전경 조건을 충족해 **녹음 권한을 쓸 수 있다**. 오디오 포커스(Android 15 이상)도 최상위 앱이면 된다. 화면이 가려지거나 꺼지면 인식을 계속할 수 없다. 백그라운드에서 마이크용 포그라운드 서비스를 시작하는 것은 금지다. 삼성·Google 엔진이 잠금 상태에서 동작하는지는 **확인 필요**. | 자동 (녹음 권한 허용은 사용자) | 이미 반영: 화면 이탈 시 중단 처리 |
| 5 | 방해금지(DND) | '알람만 허용' 모드에서는 CATEGORY_ALARM이 통과한다. '중요 알림만' 모드에서는 사용자가 알람을 허용했을 때만 통과한다. **'완전 무음' 모드는 알림도 막히고 통화 외 모든 소리가 음소거**된다. 정책 접근 권한 없이는 방해금지를 우회할 수 없다. | 중요 알림 모드: 사용자 / 완전 무음: 불가 | 완전 무음 감지 시 "소리가 나지 않으니 자막을 보세요" 안내 추가(0.2.2) |
| 6 | 개발자 인증 정책 | 2026-09-30부터 브라질·인도네시아·싱가포르·태국에서, 참여 스토어로 설치할 때만 시행한다. **한국은 2027년 전 세계 확대에 포함될 예정이며 날짜는 확인 필요**. 사이드로드는 2026-09 기준 미적용. **ADB 설치는 면제**. 무료 '제한 배포' 계정으로 등록된 기기 20대까지 설치할 수 있다. | 현재 영향 없음 | 알려진 제약에 기록. 2027년 전에 제한 배포 계정 등록을 검토 |
| 7 | 삼성 절전 | 약 3일 미사용이면 '절전 앱'이 되어 알람이 제한되고, 약 16일이면 '딥 절전'이 되어 백그라운드가 없다. **'절전 예외 앱' 등록이 필요**하다. 경로: 설정 → 배터리 → 백그라운드 사용 제한 → 절전 예외 앱. 앱 정보 → 배터리 → '제한 없음'도 권장한다. One UI 8.5 메뉴 이름은 **확인 필요**. | 사용자 | 이미 홈 화면에 안내. 매일 알람이 울리므로 미사용 판정 위험은 낮음 |
| 8 | 강제 중지 | 사용자가 강제 중지하면 모든 알람이 취소되고, 앱을 다시 열어야 복구된다(Android 15 이상). | 불가 | 이미 반영: 앱을 열면 예약 복원 |
| 9 | Android 16 고유 변경 | 정확 알람, 전체화면 알림, TTS/음성 인식, 부팅 수신에 직접 해당하는 변경은 없다. targetSdk 36에서는 예측형 뒤로 가기가 기본이라 `onBackPressed`를 쓰지 않는다(이 앱은 쓰지 않음). 재부팅하면 알람이 모두 사라지므로 부팅 수신으로 다시 등록한다(앱을 한 번 실행한 뒤에만 동작). | 자동 | 이미 반영 |

## 사용자가 직접 켜야 하는 설정 (앱 온보딩 화면 목록과 일치)

1. **알림 허용** (Android 13 이상 런타임 권한)
2. **마이크 허용**
3. **전체화면 알림 허용.** 사이드로드는 기본으로 켜져 있을 가능성이 높다. 홈 화면이 ⚠️면 설정 버튼을 눌러 켠다.
4. **배터리 최적화 제외** (앱 팝업에서 허용)
5. **삼성 절전 예외 앱 등록** + 앱 정보 → 배터리 → 제한 없음
6. **방해금지 예외:** '중요 알림만' 모드를 쓴다면 방해금지 설정 → 알람 허용(또는 앱 예외)에 추가한다. 완전 무음 모드에서는 소리가 나지 않는다.
7. 한국어·영어 **TTS 음성 데이터 설치** 확인

## 출처
- https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- https://developer.android.com/develop/background-work/services/alarms/schedule
- https://developer.android.com/reference/android/Manifest.permission#USE_EXACT_ALARM
- https://developer.android.com/about/versions/14/behavior-changes-14
- https://developer.android.com/reference/android/app/NotificationManager
- https://developer.android.com/develop/ui/views/notifications/time-sensitive
- https://developer.android.com/guide/components/activities/background-starts
- https://developer.android.com/about/versions/15/behavior-changes-15
- https://developer.android.com/about/versions/16/behavior-changes-all
- https://developer.android.com/about/versions/16/behavior-changes-16
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/media/optimize/audio-focus
- https://developer.android.com/developer-verification , /guides/faq , /guides/limited-distribution
- 검색 요약만 확인: https://source.android.com/docs/core/permissions/fsi-limits , https://www.samsung.com/sec/support/galaxy-battery/optimization/ , https://dontkillmyapp.com/samsung

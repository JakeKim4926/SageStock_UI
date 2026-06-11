# 기술부채 로그 (DEBT_LOG)

이번 작업 범위 밖이라 남겨둔 위험 요소를 기록한다. 즉시 해결이 아니라 추적이 목적이다.
해결한 항목은 `## 해결됨` 섹션으로 옮긴다.

## 열린 항목

### [2026-06-07] 하드코딩 — 코너 라운드 dp가 토큰화되지 않음
- 위치: `ui/theme/Dimens.kt` (radius 토큰 부재) · `ui/prediction/PredictionScreen.kt` 등 화면 전반
- 설명: `RoundedCornerShape(8.dp/12.dp/20.dp)`가 코드베이스 전반에 인라인으로 박혀 있음. `Dimens`에는 패딩 토큰(`screenPadding/cardPadding/gap`)만 있고 radius 항목이 없어, 개별 화면만 토큰화하면 오히려 불일치가 생김.
- 위험도: 낮음
- 후속: `Dimens`에 radius 토큰(예: `radiusSm/Md/Pill`) 추가 후 전 화면 `RoundedCornerShape` 일괄 치환

### [2026-06-11] Phase 7 부분 연동 — StockRepository만 Retrofit, 나머지 미연동
- 위치: `data/RetrofitStockRepository.kt`, `data/remote/*`, `di/NetworkModule.kt`, `di/AppModule.kt`
- 설명: Phase 7 중 **시세·지표·시그널(StockRepository)** 슬라이스만 Retrofit으로 구현하고 `BuildConfig.USE_MOCK=true`로 Mock 유지 중. 아래는 범위 밖으로 남김:
  - **예측**: 백엔드 `/predictions` 엔드포인트가 openapi.json에 없음(feature-spec B5 = 최종 단계). `getPredictions()`는 `MockStockRepository`에 위임. 백엔드 추가 시 그 메서드만 교체.
  - **Watchlist/Paper 서버 동기화(U1)**: 현재 Room 로컬. api-spec §3·§4는 서버 집계(holdings/account)·동기화를 요구하나 ViewModel/UI 변경이 따라와 "UI 무수정" 원칙과 충돌 → 별도 슬라이스.
  - **인증(U4)**: `AuthRepository`·토큰 인터셉터·401 refresh 재시도 미구현. `SessionManager`는 여전히 로컬 스텁.
- 위험도: 중간 (실서버 전환 시 한 번에 안 끝남)
- 후속: ① Watchlist/Paper RetrofitRepository + holdings/account 도메인 메서드 추가, ② AuthInterceptor + refresh, ③ 백엔드 `/predictions` 생기면 예측 교체

### [2026-06-11] openapi.json 계약 불일치 — base URL /v1 중복, 에러 바디 형태
- 위치: `app/build.gradle.kts` (`API_BASE_URL`), `data/remote/ApiCall.kt`, `data/remote/SageStockApi.kt`
- 설명: openapi.json의 `servers.url`이 `.../v1`인데 paths도 `/v1/...`로 시작 → 합치면 `/v1/v1/...` 중복. 현재는 baseUrl을 `https://api.example.com/`(placeholder)로 두고 경로에 `v1/`을 넣어 회피. 또 openapi는 에러 바디로 FastAPI 기본 `422 {detail}`만 문서화하고 api-spec §0의 `{code,message}`(400/401/404/409)는 미문서화 — `parseApiError`가 양쪽을 모두 수용하도록 방어 구현해 둠.
- 위험도: 낮음 (실서버 확정 시 `API_BASE_URL`·경로만 조정)
- 후속: 실서버 base path 확정 → `API_BASE_URL` 교체 및 경로 정리. 백엔드 에러 바디 형태 통일 합의.

## 해결됨

# SageStock UI — 컨벤션 & 개발 플랜

안드로이드 앱: **한국 주식 기술적 지표 뷰어**.
지표 계산과 XGBoost 예측은 **FastAPI 백엔드**가 담당하고, 안드로이드는 결과를 **차트/카드로 표시**한다.

대상 지표: RSI, 이동평균, 골든/데드크로스, 다이버전스, 볼린저밴드, 이격도, XGBoost 예측.

> 참고: `sagedash-plan` skill(MFC + WebView2 데스크톱)은 별개 프로젝트다. 이 앱과 무관.

---

## 확정 컨벤션

| 항목 | 결정 |
|------|------|
| **DI** | Hilt (KMP 안 함 → Koin 불필요) |
| **모듈 구조** | 단일 `app` 모듈 + 패키지 레이어 (`data` / `domain` / `ui`). 화면 많아지면 feature 모듈로 분리 |
| **에러/상태 타입** | `Result<T>` = Success / Error / Loading 하나로 통일 (`Try` 미사용) |

### 추가 스택
- UI: Jetpack Compose + Material 3
- 차트: Vico (Compose 네이티브)
- 네트워크: Retrofit + OkHttp + kotlinx.serialization
- 로컬: Room (관심목록)
- 테스트: MockK + Turbine

---

## 개발 플랜

초기엔 **Mock JSON으로 UI 먼저**, 마지막에 FastAPI로 교체.

- **Phase 0 — 셋업**: Compose+Hilt+Retrofit+Vico 골격, 패키지 레이어, `Result<T>`/UiState 정의. → 빈 앱 뜨고 DI 동작
- **Phase 1 — RSI E2E (Mock)**: 종목 검색 → 상세 → RSI 차트. Repository는 Mock. → 종목 선택 시 RSI 차트 표시
- **Phase 2 — 지표 확대**: 이동평균/볼린저/이격도 오버레이 + 골든·데드크로스/다이버전스 마커. → 차트형 지표 전부 표시
- **Phase 3 — 예측 + 관심목록**: XGBoost 예측 카드 + Room 관심목록. → 관심목록에서 예측 요약 표시
- **Phase 4 — FastAPI 연동**: Mock Repository → 실제 Retrofit 구현 교체 (인터페이스 동일, UI 무수정). → 실데이터 전체 흐름 동작

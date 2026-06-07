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

산출물의 정답지는 **`docs/SageStock_wireframes.html`**(9개 화면+빈상태·5탭 IA·4 플로우·토큰). 그 화면들을 **Mock JSON으로 UI 먼저** 만들고, 마지막에 FastAPI로 교체한다. 화면을 가로로 쌓지 않고 **지표 중심 수직 슬라이스**로 채운다. 운영용 산출물·검증기준·화면매핑은 `sagestock-plan` skill 참조.

- **Phase 0 — 셋업**: Compose+Hilt+Retrofit+Vico+Room 골격, 패키지 레이어, `Result<T>`/UiState, 디자인 토큰/테마. → 빈 앱 뜨고 DI 동작
- **Phase 1 — 앱 셸 & IA + 세션(01)**: 스플래시→자동로그인→로그인, 하단 5탭(홈·검색·시그널·가상매매·설정) Scaffold + 공용 상세 스택. 탭 내부는 스텁. → 와이어프레임 IA·로그인 플로우가 그대로 섬
- **Phase 2 — RSI E2E (Mock)**: 종목 검색(03) → 상세(04) → RSI 차트(05). Repository는 Mock. → 종목 선택 시 RSI 차트 표시
- **Phase 3 — 지표 확대 + 시그널(06)**: 이동평균/볼린저/이격도/스토캐스틱 + 골든·데드·다이버전스 마커, ⚙지표설정, 시그널 피드. → 차트형 지표·시그널 전부 표시
- **Phase 4 — 예측(07) + 홈(02) + 관심목록**: XGBoost 예측 카드 + 빈상태 3종, 홈 대시보드, Room 관심목록. → 관심·예측·장상태가 홈에 모임
- **Phase 5 — 가상매매(08·08b)**: 가상 매수/매도 입력 + 포트폴리오(Room). 실제 주문 아님. → 가상매매 플로우 완성
- **Phase 6 — 설정(09)**: 차트 기본값·등락 팔레트·로그아웃, 변경값 즉시 반영(Room+Theme). → 설정 플로우 완성
- **Phase 7 — FastAPI 연동**: Mock Repository → 실제 Retrofit 구현 교체 (인터페이스 동일, UI 무수정). → 실데이터 전체 흐름 동작

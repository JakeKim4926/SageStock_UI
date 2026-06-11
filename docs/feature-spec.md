# SageStock 백엔드 기능명세 (v0.2)

FastAPI 백엔드가 **무엇을, 어떻게** 수행하는지 정의한다. 인터페이스 계약(엔드포인트·스키마·에러코드)은
`docs/api-spec.md`가 정본이며, 이 문서는 그 뒤의 **내부 동작·계산 규칙·데이터 소스·재사용 자산**을 다룬다.

> 두 문서의 의존 방향: **이 문서 → api-spec.md** (단방향). 스키마는 복붙하지 않고 `api-spec §N`으로 링크한다.
> 산출물 흐름 정본: `docs/conventions-and-plan.md` (Mock-first → Phase 7 FastAPI 교체, 인터페이스 동일·UI 무수정)

## 결정 로그 (v0.2에서 확정)

| # | 항목 | 결정 |
|---|------|------|
| D1 | 예측 모델 | 백엔드는 **학습하지 않음**. 사전 학습된 `model_*.json`(+meta/thr)을 로드해 서빙. 모델 산출물은 운영자가 투입. |
| D2 | 예측 스코프 | 모델 유니버스(**KOSDAQ 중소형 급등**)만 `READY`. 그 외(KOSPI/US)는 `status=UNAVAILABLE`. |
| D3 | 예측 서빙 | **일배치 스코어링 + 캐시 서빙**(screen.py 구조). 요청 시 실시간 추론 안 함. |
| D4 | 예측 근거 필드 | MVP: `riseProbability`/`confidence`만 실산출. `expectedReturnPercent=0`, `reasons`/`riskFactors`=빈 배열. 근거 생성은 후속. |
| D5 | DB | **PostgreSQL** + SQLAlchemy + Alembic. |
| D6 | 시그널 임계값 | **표준 프리셋을 서버 기본 config**. 사용자 커스텀은 `/signals` 선택적 쿼리 파라미터 override(계획). |
| D7 | 가상매매 체결가 | 클라 **요청가 그대로 기록**. 서버는 예수금/보유수량만 검증. |
| D8 | 인증 토큰 | access 30분(api-spec), refresh 14일 **회전**, refresh는 DB 저장·로그아웃 시 무효화. 비번 해시 bcrypt. |

> 표준 기본값으로 채운 세부(시그널 임계값 수치, 캐시 TTL, 차트 시리즈 길이 등)는 각 절에 **"기본값(조정 가능)"**으로 명시.

## 재사용 원천 — `D:\Projects\SageStock_AI`

기존 연구·백테스트 프로젝트의 자산을 서빙 백엔드로 끌어온다. **그 프로젝트는 "서빙 API"가 아니라
"오프라인 학습 파이프라인 + 스크리너"**임에 주의 — 그대로 쓸 것/적응할 것/새로 짤 것이 갈린다.

| AI 프로젝트 파일 | 역할 | 백엔드 활용 |
|---|---|---|
| `SageStock.py` | 지표 계산 엔진(체이닝, OHLCV→지표) | 지표/차트 계산에 **거의 그대로** 이식 |
| `SageStockML.py` | 오프라인 1차→메타 XGBoost 학습·CV·백테스트 | 학습은 **오프라인 배치**로 유지, 서빙은 예측 부분만 래핑 |
| `screen.py` | 저장 모델로 현재 상장 KOSDAQ 매수후보 랭킹 | `/predictions` **일배치 스코어링**의 원형 |
| `supply.py` / `events.py` / `procurement*.py` | 수급·공시·조달청 피처 수집 | 예측 피처 파이프라인(수급 사용, 공시·조달청 현재 off) |
| `model_kosdaq_surge*.json` | 학습된 모델·메타·임계값 사이드카 | 서빙이 로드하는 산출물(D1) |
| `docs/01~04_*.md` | 라벨/청산·수급·백테스트·조달청 **연구 노트** | 각 기능 "왜(rationale)" 출처 |
| 데이터 소스 | `FinanceDataReader` + `pykrx` | 시세·일봉·수급 수집의 기반 |

---

## 1. 개요 & 범위

### 하는 것
- 인증/세션, 시세·지표 조회, 시그널 피드, XGBoost 예측, 관심종목·가상매매 동기화 (api-spec §1~4 전체)

### 안 하는 것 (api-spec §6 재확인)
- 웹소켓/SSE 실시간 푸시 — **폴링(REST)** 모델
- 비밀번호 재설정 — 후속 과제
- 최근 검색어·앱 설정(등락색 등) — 기기 로컬 전용, 서버 동기화 안 함

### 예측 스코프 (D2)
- 예측은 **사전 학습 모델이 커버하는 KOSDAQ 중소형 급등 유니버스**만 `status=READY`.
- KOSPI·US 등 모델 밖 종목은 `status=UNAVAILABLE`로 정직하게 응답(허위 예측 금지).
- KOSPI/US 모델 확장은 후속 과제(§9).

---

## 2. 시스템 아키텍처

```
[Android 클라]
   │  HTTPS 폴링 (REST, JWT)
   ▼
[FastAPI 서빙]
   ├─ 시세/지표 서비스  ── FinanceDataReader / pykrx  (+ 캐시)
   ├─ 시그널 서비스     ── 지표 기반 규칙 탐지 (표준 프리셋 config)
   ├─ 예측 서비스       ── 저장 모델 로드 → 일배치 스코어 캐시 조회 (실시간 추론 X)
   ├─ 인증 서비스       ── JWT access/refresh (PostgreSQL)
   └─ 동기화(관심·가상매매) ── PostgreSQL (계정 귀속)
                                  ▲
[오프라인 배치]  ① 학습(SageStockML, 백엔드 밖)  ② 일배치 예측 스코어링(screen.py 원형) ──┘
```

- 예측 서빙(D3): 일 1회(장 마감 후) 전 유니버스 스코어링 → 결과 캐시. API는 캐시 조회만. 전 종목 스캔·모델 로드 비용을 폴링 경로에서 제거.
- 시세 캐시 TTL **기본값(조정 가능)**: 장중 quote/snapshot 60초, 장 종료 시 그날 종가 고정. 일봉(`candles`)·지표는 종가 확정 후 일 1회 갱신.

---

## 3. 데이터 소싱

| 데이터 | 소스 (AI 프로젝트) | 비고 |
|---|---|---|
| 일봉 OHLCV | `fdr.DataReader(code)` | `SageStockML._read_prices`는 타임아웃 16스레드풀로 hang 방어 → 배치 수집에 재사용 |
| 종목 리스팅/시총 | `fdr.StockListing('KOSDAQ'/'KOSPI'/'KRX-DELISTING')` | 검색·유니버스. 일 1회 갱신 캐시 |
| 시장지수(국면) | `fdr.DataReader('KQ11')` | 예측 피처 `Mkt_Ret20/60`, `Mkt_Vol20` |
| 투자자별 수급 | `pykrx` (`.env`의 `KRX_ID/KRX_PW`) → `supply.py` | 외국인/기관 순매수(예측 피처) |

- `isDelayed`(api-spec §2 quote): fdr 일봉은 지연 → **KR 기본 `true`**. 실시간 시세 소스 도입은 후속(§9).
- `market/status`(api-spec §2): **신규**(AI 프로젝트엔 없음). 서버 UTC 기준 장 운영시간 판정.
  - 기본 운영시간(조정 가능): KR `09:00–15:30 KST`, US `09:30–16:00 ET`(+프리/애프터). 표시 변환은 클라.
  - 휴장일: KR은 `pykrx` 영업일 캘린더. US 휴장일 캘린더는 후속.

---

## 4. 도메인별 기능

각 기능: **① 기능 → ② api-spec 링크 → ③ 재사용 출처 → ④ 규칙/구현** 4단.

### 4.1 인증 / 세션
- ① 회원가입·로그인·토큰 재발급·로그아웃.
- ② api-spec §1, §0(인증)
- ③ 재사용 없음 — 신규 구현.
- ④ 규칙 (D8):
  - access 만료 30분(api-spec `accessExpiresIn: 1800`), refresh 14일.
  - refresh **회전**: 재발급 시 기존 refresh 무효화 후 새 토큰 발급. `refresh_tokens` 테이블에 저장.
  - 로그아웃(api-spec §1) 시 해당 refresh 무효화(`204`).
  - 비밀번호 해시 **bcrypt**. 검증 라이브러리 `passlib`, JWT `python-jose`(또는 `pyjwt`).
  - 에러: 자격 불일치 `401`, 이메일 중복 `409`, 검증 실패 `400` (api-spec §0 code 표 준수).

### 4.2 시세 · 지표
- ① 검색, 단건 메타, 스냅샷, quote, **지표셋(차트 시리즈)**.
- ② api-spec §2 (`/stocks/search`, `/stocks/{ticker}`, `/market/snapshots`, `/stocks/{ticker}/quote`, `/stocks/{ticker}/indicators`)
- ③ 재사용 출처 — `SageStock.py` 지표 산식 (그대로 이식):

  | api-spec 필드 | `SageStock.py` 산식 |
  |---|---|
  | `rsi14` / `rsiSeries` | `Make_RSI` — Wilder EMA(`com=period-1`), period=14 |
  | `ema5/20/60/120` | `Close.ewm(span=p)` 패턴 (현재 EMA20만 → **다기간 확장**) |
  | `bollingerUpper/Mid/Lower` | `Make_Bollinger_Bands` — MA20 ± 2σ |
  | `stochasticK/D` | `Make_Stochastic` — period=14, smooth=3 |
  | `disparitySeries` | `Make_Disparity_EMA` — `(Close/EMA20)*100` |
  | `candles` | fdr OHLCV 원본 |

- ④ 규칙/구현:
  - **피처값 vs 차트 시리즈 분리**: `SageStock.py`는 모델 입력용 *단일값 피처*(`BB_PercentB`, `BB_Bandwidth`, 정규화 `MACD`)를 만든다. 차트는 *원본 밴드 배열*(`bollingerUpper` 등)이 필요 → 표시용 시리즈 계산을 **별도 메서드**로 추가(원본 엔진 비파괴).
  - `ema5/60/120` 다기간 EMA 추가.
  - 반환 시리즈 길이 **기본값(조정 가능)**: 최근 **120 거래일**(`ema120` 워밍업 포함). 시리즈 인덱스는 `candles` 인덱스와 정렬(api-spec §2 주석).
  - `422 INSUFFICIENT_DATA` 트리거: `Drop_Na` 후 유효 봉 `< 60`(AI 코드 관례). `404 DATA_NOT_FOUND`: fdr 조회 결과 없음.
  - `crossMarkers`/`divergenceMarkers`는 §4.3 시그널 탐지 로직과 계산 공유.

### 4.3 시그널 (Signals)
- ① 골든/데드크로스·RSI 과매수도·볼린저돌파·다이버전스 탐지 피드.
- ② api-spec §2 (`GET /signals`), `SignalType` enum
- ③ 재사용 출처 — 지표 값은 `SageStock.py`. **탐지 규칙 자체는 신규.**
- ④ 규칙 (D6, **표준 프리셋 기본값 / config로 조정 가능**):

  | `SignalType` | 탐지 규칙(기본값) | `riskLevel`(기본) |
  |---|---|---|
  | `GOLDEN_CROSS` | EMA5가 EMA20을 상향 교차 | MEDIUM |
  | `DEAD_CROSS` | EMA5가 EMA20을 하향 교차 | MEDIUM |
  | `RSI_OVERSOLD` | RSI14 ≤ 30 | LOW |
  | `RSI_OVERBOUGHT` | RSI14 ≥ 70 | HIGH |
  | `BOLLINGER_BREAKOUT` | 종가가 상단(또는 하단) 밴드 이탈 | MEDIUM |
  | `BULLISH_DIVERGENCE` | 최근 20봉: 가격 저점↓ + RSI 저점↑ | MEDIUM |
  | `BEARISH_DIVERGENCE` | 최근 20봉: 가격 고점↑ + RSI 고점↓ | HIGH |

  - **사용자 커스텀**(D6): `/signals`에 선택적 쿼리 파라미터로 임계값 override(예: `rsiOversold`, `goldenFast`, `goldenSlow`, `divergenceWindow`). 미지정 시 기본 프리셋. 설정값은 기기 로컬에 보관(api-spec §6 로컬 전용 원칙 유지) → **이 쿼리 파라미터는 api-spec에 추가 필요(계획, §9)**.
  - `candleIndex`(api-spec §2): 탐지 봉의 `candles` 인덱스. 없으면 `-1`.
  - 탐지는 §4.2 지표 시리즈 위에서 수행(중복 계산 회피).

### 4.4 예측 (XGBoost)
- ① 종목별 상승확률·신뢰도·기대수익·근거 카드.
- ② api-spec §2 (`GET /predictions`), `Prediction`/`PredictionStatus`
- ③ 재사용 출처 — `SageStockML.py`(피처/모델 정의) + `screen.py`(서빙 원형):
  - 학습은 **백엔드 밖**(D1). 백엔드는 `model_kosdaq_surge.json`(+`_meta`,`_thr`)을 로드만.
  - 일배치 스코어링(D3) = `screen.py` 구조: 유니버스 일봉 수집 → `SageStock` 지표 → 시장국면/수급 피처 결합 → 최신 행에 **1차→메타** 적용 → 결과 캐시.
  - 라벨/피처 설계 근거: 연구노트 `01_라벨_청산_재설계.md`, `02_수급_거래대금_피처.md`.
  - api-spec 필드 매핑:

  | api-spec 필드 | 출처 |
  |---|---|
  | `riseProbability` | 1차 모델 `predict_proba[:,1]` |
  | `confidence` | 메타 모델 confidence |
  | `status` | `READY`(모델 커버·데이터 충분) / `INSUFFICIENT_DATA`(유효봉<60) / `UNAVAILABLE`(모델 밖, D2) / `PREPARING`(배치 미생성) |
  | `expectedReturnPercent` | **MVP 0**(D4) |
  | `reasons` / `riskFactors` | **MVP 빈 배열**(D4) |

- ④ 규칙/구현:
  - `riseProbability ≥ PRIMARY_THR`(0.5) 통과분만 메타 적용(screen.py와 동일). 임계값은 모델 사이드카 `_thr.json`(EV 튜닝값)에서 로드.
  - 배치 스케줄: 장 마감 후 일 1회. 스코어 캐시는 DB 또는 파일.
  - 후속(§9): `expectedReturnPercent`(백테스트 평균 매핑), `reasons`/`riskFactors`(SHAP 또는 규칙 문장), KOSPI/US 모델.

### 4.5 관심종목 (Watchlist)
- ① 계정별 관심종목 add/remove/조회(멱등).
- ② api-spec §3
- ③ 재사용 없음 — CRUD. 종목 메타는 §4.2 재사용.
- ④ `watchlist(user_id, ticker)` 유니크. PUT/DELETE 멱등(`204`). 응답의 종목 메타는 메타 캐시 테이블에서 조인.

### 4.6 가상매매 (Paper)
- ① 가상 매수/매도 기록, 보유현황 집계, 예수금.
- ② api-spec §4
- ③ 재사용 없음 — 규칙·집계 신규. `currentPrice`는 §4.2 quote 재사용.
- ④ 규칙 (D7):
  - 체결가 = **클라 요청가 그대로 기록**. 서버는 가격 대체/검증 안 함.
  - 매수: 예수금 `cash ≥ price×quantity` 검증 → 부족 시 `422 INSUFFICIENT_DATA`.
  - 매도: 보유수량 ≥ quantity 검증 → 부족 시 `422`.
  - 수량·가격 비정상(≤0 등) → `400 INVALID_REQUEST`.
  - `avgPrice`: 매수 가중평균(매도는 평단 유지, 수량만 차감). `id`·`timestamp`(epoch ms UTC)는 서버 부여.
  - 시드: `VIRTUAL_CASH_SEED = 10_000_000`(계정 생성 시 초기 `cash=seed`).

---

## 5. 데이터 모델 / DB (PostgreSQL, D5)

계정 귀속 영속 데이터만 DB. SQLAlchemy + Alembic 마이그레이션.

| 테이블 | 용도 |
|---|---|
| `users` | 인증(email·password_hash·name) |
| `refresh_tokens` | 세션(회전·무효화) |
| `watchlist` | (user_id, ticker) 관심종목 |
| `paper_trades` | 가상매매 기록(side·price·quantity·timestamp) |
| `paper_account` | 계정별 cash·seed |
| `stock_meta`(캐시) | ticker·name·market·exchange |
| `prediction_scores`(캐시) | 일배치 스코어(ticker·proba·confidence·status·as_of) |

- 시세·지표는 인메모리/파일 캐시(영속 불필요). 예측 스코어는 배치 산출물이라 캐시 테이블로 보관.

---

## 6. 비기능 요구사항
- 폴링 부하: 스냅샷/장상태 분리 폴링(api-spec §6) + 캐시 TTL(§2)로 fdr 호출 평탄화.
- 예측 배치: 전 유니버스 스코어링 소요시간 측정 후 마감 후 윈도우에 배치. 실패 시 직전 캐시 유지(`as_of` 노출).
- 에러 매핑: api-spec §0 HTTP/code 표 준수. `2xx`→`Result.Success`, 그 외→`Result.Error`.
- 로깅: 외부 소스(fdr/pykrx) 실패·타임아웃 기록.

---

## 7. 프로젝트 구조 (FastAPI)

```
app/
  main.py
  api/        # 라우터 (auth, stocks, market, signals, predictions, watchlist, paper)
  services/   # 비즈니스 로직 (지표·시그널·예측·가상매매)
  indicators/ # SageStock.py 이식 (모델 피처 + 차트 시리즈)
  ml/         # 모델 로드·스코어링 (screen.py 원형) + 일배치 스코어러
  data/       # fdr/pykrx 소싱 + 캐시
  models/     # SQLAlchemy 모델
  schemas/    # Pydantic (api-spec 스키마와 1:1)
  core/       # 설정·인증(JWT/bcrypt)·에러 매핑
```
- DB: PostgreSQL / SQLAlchemy / Alembic. 인증: python-jose + passlib(bcrypt).
- 배치: 일배치 스코어러는 별도 엔트리(스케줄러/크론)로 실행, 서빙과 분리.

---

## 8. 개발 단계 (백엔드 마일스톤)

클라 `conventions-and-plan.md` Phase 7(Mock→실 Retrofit 교체)과 맞물린다. 재사용 우선 순서:

> **AI(예측)는 가장 마지막에 개발한다.** 예측은 외부 학습 모델(`model_*.json`)·일배치 스코어러·캐시 등
> 의존이 가장 무겁고, 나머지 기능(시세·지표·시그널·동기화)과 독립적이다. 앞 단계가 모두 안정화된 뒤
> 착수해 위험을 끝으로 몰아둔다. 그 전까지 `/predictions`는 Mock 또는 `status=PREPARING`으로 응답해도
> 클라가 동작한다(`PredictionStatus` 폴백).

- **B0 셋업**: FastAPI 골격, 설정/에러 매핑(api-spec §0), PostgreSQL·Alembic·JWT 기초.
- **B1 시세·지표**: `SageStock.py` 이식 + 차트 시리즈 계산 → `/stocks/*`, `/market/*`. (클라 Phase 2~3)
- **B2 시그널**: §4.3 표준 프리셋 탐지 구현 → `/signals`. (클라 Phase 3)
- **B3 동기화**: 관심종목·가상매매 CRUD/집계 → `/watchlist`, `/paper/*`. (클라 Phase 4~5)
- **B4 운영**: 캐시·배치 스케줄·모니터링 기반.
- **B5 예측(AI) — 최종 단계**: 모델 로드 + 일배치 스코어러 + 캐시 서빙 → `/predictions`. (클라 Phase 4 카드)

각 단계 검증: 엔드포인트가 api-spec 스키마대로 응답하고, Mock 교체 시 클라 UI 무수정으로 동작.

---

## 9. UI 정합성 체크 (Phase 7 교체 시 충돌 지점)

UI(`app/.../domain` Repository·ViewModel) 실측 대조 결과, **UI가 필요로 하는 백엔드 데이터는
api-spec에 모두 존재**한다(누락 없음). 단, UI가 현재 **로컬에서 자체 계산**하는 항목이 있어
Mock→Retrofit 교체 시 정합성 결정이 필요하다.

| # | 항목 | 현재 UI 동작 | api-spec 설계 | Phase 7 결정 |
|---|------|-------------|--------------|-------------|
| U1 | 보유현황·예수금 | `PaperViewModel`이 `observeTrades()`로 **클라 집계**(평단·cash fold). `PaperRepository`에 조회 메서드 없음. `currentPrice`는 스냅샷에서. | `GET /paper/holdings`·`/paper/account` **서버 집계** | **서버를 정본으로**(아래 근거). PaperRepository에 메서드 추가. |
| U2 | 장 상태 | `HomeViewModel.marketStatus()` **클라 시각 스텁**(KST/ET). 조회 메서드 없음. | `GET /market/status` | 서버 값 사용 → StockRepository에 메서드 추가. |
| U3 | 단건 종목 메타 | `DetailViewModel`이 `search(ticker)`로 우회 | `GET /stocks/{ticker}` | `/stocks/{ticker}` 채택 권장(검색에 안 잡히는 종목 엣지 방어). |
| U4 | 인증 | UI에 **소비 계층(AuthRepository) 없음**(로그인 Mock) | api-spec §1 | Phase 7에서 AuthRepository 신규 + 토큰 인터셉터. |

- **U1 근거 / D7 충돌 해소**: D7에서 매수·매도 검증을 **서버가** 수행하므로 서버는 cash/holdings를
  이미 보유해야 한다. 클라가 별도 집계를 유지하면 두 출처가 어긋난다(특히 `currentPrice` 기준이
  스냅샷 vs quote로 갈림). → **서버 집계를 단일 정본**으로 하고, 클라 로컬 집계는 Mock 단계 한정으로 둔다.
  `Holding`의 파생값(`invested`/`value`/`profit`)은 api-spec대로 클라 계산 유지(표시 전용).

## 10. 후속 과제 (이번 명세 범위 밖)
- 예측 근거 생성: `expectedReturnPercent`(백테스트 평균), `reasons`/`riskFactors`(SHAP 또는 규칙 문장).
- KOSPI/US 예측 모델 확장(현재 KOSDAQ 급등 전용).
- 시그널 임계값 override 쿼리 파라미터 → **api-spec 추가 필요**.
- 실시간 시세 소스 도입(현재 fdr 지연).
- US 휴장일 캘린더.
- 비밀번호 재설정(api-spec 후속).
- 공시(DART)·조달청 피처 재활성(`USE_EVENTS`/`USE_PROCUREMENT` 현재 off).
</content>

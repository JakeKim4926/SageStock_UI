# [BE→FE] `GET /v1/stocks/{ticker}/indicators` interval·range 파라미터 추가 완료

develop 반영 완료 (커밋 `f4fb471`). 요청서(`docs/be-request-chart-interval-range.md`) 1·2·3 전부
반영했고, **응답 스키마는 기존 그대로**라 DTO·매퍼 변경 없음. 쿼리 파라미터만 추가됐다.

## 1. API 계약

```
GET /v1/stocks/{ticker}/indicators?interval={1d|1w|1mo}&range={1m|3m|6m|1y|max}
Authorization: Bearer <token>
```

| 파라미터 | 값 | 기본 | 의미 |
|---|---|---|---|
| `interval` | `1d` `1w` `1mo` | `1d` | 캔들 집계 단위(일/주/월봉) |
| `range` | `1m` `3m` `6m` `1y` `max` | `6m` | 마지막 캔들 기준 거슬러 볼 기간 |

- 둘 다 생략하면 `1d` + `6m` (기존 동작과 사실상 동일 → 마이그레이션 중 점진 적용 가능).
- 응답은 기존 `IndicatorSet` 그대로. **모든 배열(candles, rsiSeries, ema5/20/60/120, bollinger\*,
  stochasticK/D, disparitySeries)이 요청 interval로 집계된 캔들 인덱스에 정렬**되어 있음.
- `crossMarkers` / `divergenceMarkers` 의 index도 해당 interval 캔들 인덱스 기준.
- 지표는 일봉을 묶은 게 아니라 **간격 캔들 위에서 재계산**된 값이라, 주/월봉 오버레이를 그대로
  켜도 값이 맞다.

## 2. 에러 동작 (주의)

잘못된 파라미터 값은 **HTTP 422가 아니라 400**으로 온다 (이 백엔드 공통 envelope):

```json
{ "code": "INVALID_REQUEST", "message": "요청 파라미터가 올바르지 않습니다." }
```

예: `interval=2d`, `range=2y` 같은 미정의 값 → `400 INVALID_REQUEST`.
데이터 부족은 기존대로 `422 INSUFFICIENT_DATA` 유지.

## 3. 알아둘 한계 (데이터 길이)

- **월봉 장기 EMA(EMA120, 때때로 EMA60) 앞부분이 0/평탄선일 수 있음.** 월봉 EMA120은 원천 10년이
  필요한데 BE 원천을 8년으로 캡했고, 부족 구간은 0으로 패딩된다. 협의된 트레이드오프이니, 월봉에서
  EMA120이 처음 일부 구간 평탄하게 보여도 정상.
- **`interval=1d` + `range=max`는 최근 5년으로 캡**(페이로드 보호). 주/월봉 max는 전체 반환.

## 4. FE 할 일

1. 차트 요청에 `interval`/`range` 쿼리 파라미터 연결 (봉 세그먼트 ↔ interval, 기간 칩 ↔ range).
2. 클라 측 `takeLast(days)` 윈도잉 + `chunked` 주/월봉 집계 **제거** — 받은 시리즈를 그대로 그린다.
3. 주/월봉에서 막아둔 **EMA·볼린저 오버레이 다시 켜기**.
4. 파라미터 검증 에러 핸들링은 `400 INVALID_REQUEST` 기준으로 (422 아님).

## 5. 예시

```
GET /v1/stocks/005930/indicators?interval=1w&range=1y   → 주봉 ~52캔들, 지표 주봉 기준 재계산
GET /v1/stocks/005930/indicators?interval=1mo&range=max → 월봉 전체
GET /v1/stocks/AAPL/indicators                          → 일봉 6개월 (기본)
```

---

# [운영] Render 슬립 대응 — heartbeat · 워밍업

## 배경

Render 무료 웹 서비스는 **인바운드 요청이 15분간 없으면 슬립**, 다시 깨어나는 콜드스타트가
**30~50초**. 그래서 "로그인 동안 슬립 안 들어가게 주기적으로 heartbeat"를 검토했는데, 결론은
**heartbeat만으로는 절반만 해결**된다. 두 문제를 분리해야 한다.

- **문제 A — 세션 중 슬립**: 로그인해 쓰는 중 15분 쉬면 슬립 → 다음 액션이 느려짐. → heartbeat로 해결.
- **문제 B — 콜드스타트**: 첫 요청이 슬립 상태를 맞으면 느림. → heartbeat로 **못 푼다**(이미 깨어있는 걸
  유지할 뿐).

## 결론 (채택안)

BE 변경 없음. `/health`(순수 200, DB 미접근)가 이미 있으므로 **전부 Android(FE) 작업**이다.

1. **세션 heartbeat (문제 A)** — 앱이 **포그라운드 + 로그인 상태일 때만** `/health`를 **10분 주기**로
   ping. 백그라운드/로그아웃이면 중단.
   - 15분 컷이라 여유 두고 10분. 백그라운드에서 돌리지 말 것(배터리·무의미).
   - 실제 사용 중에만 깨어있어 무료 **월 750 instance-hours** 예산을 안 태움.

2. **앱 시작 워밍업 핑 (문제 B 완화)** — 스플래시/로그인 화면 진입 즉시 `/health` 1회 호출 →
   유저가 아이디·비번 입력하는 동안 인스턴스가 미리 스핀업. 콜드스타트 체감을 가린다.

3. **로그인 화면 "서버 깨우는 중" 로딩 상태** — 워밍업해도 첫 40초는 가끔 못 피하므로 UX로 덮는다.

## DB(Neon) 콜드스타트 — 별도 대응 불필요

- `/health`는 **순수 200이라 DB를 깨우지 않는다.** 즉 워밍업 핑은 Render 웹만 데우고 Neon은 못 데운다.
- Neon 무료도 ~5분 유휴 시 suspend가 있으나 **깨어나는 건 1초 안팎**(Render 콜드스타트보다 훨씬 빠름)
  이고, **로그인 자체가 DB를 조회**하므로 첫 인증 요청이 어차피 깨운다 → 추가 작업 없이 수용 가능.
- (옵션) 스플래시에서 DB까지 선제적으로 데우고 싶으면 BE에 `SELECT 1` 하는 readiness 엔드포인트
  추가가 필요. 현재는 불필요 판단.

## 하지 말 것 / 트레이드오프

- ❌ 외부 핑어(UptimeRobot/cron-job.org)로 24/7 깨우기 — 콜드스타트는 사라지지만 750시간 무료
  예산을 다 태워 "영구 무료" 전제가 깨진다.
- ✅ 콜드스타트를 **완전히 없애려면** Render 유료($7/mo, 슬립 없음)가 정공법. 무료 유지가 목적이면
  위 1+2+3 조합이 현실적 최선(콜드스타트를 "줄이고 가린다").

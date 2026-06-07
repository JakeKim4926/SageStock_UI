# 기술부채 로그 (DEBT_LOG)

이번 작업 범위 밖이라 남겨둔 위험 요소를 기록한다. 즉시 해결이 아니라 추적이 목적이다.
해결한 항목은 `## 해결됨` 섹션으로 옮긴다.

## 열린 항목

### [2026-06-07] 하드코딩 — 코너 라운드 dp가 토큰화되지 않음
- 위치: `ui/theme/Dimens.kt` (radius 토큰 부재) · `ui/prediction/PredictionScreen.kt` 등 화면 전반
- 설명: `RoundedCornerShape(8.dp/12.dp/20.dp)`가 코드베이스 전반에 인라인으로 박혀 있음. `Dimens`에는 패딩 토큰(`screenPadding/cardPadding/gap`)만 있고 radius 항목이 없어, 개별 화면만 토큰화하면 오히려 불일치가 생김.
- 위험도: 낮음
- 후속: `Dimens`에 radius 토큰(예: `radiusSm/Md/Pill`) 추가 후 전 화면 `RoundedCornerShape` 일괄 치환

## 해결됨

# SageStock

개인용 **한국·미국 주식 기술적 지표 뷰어** 안드로이드 앱. 회원가입 없이 단일 사용자가 로그인해 사용한다.

지표 계산과 XGBoost 예측은 **FastAPI 백엔드**가 담당하고, 안드로이드는 그 결과를 **차트·카드로 표시**하는 역할에 집중한다. 대상 지표는 RSI · 이동평균(EMA) · 골든/데드크로스 · 다이버전스 · 볼린저밴드 · 이격도 · XGBoost 예측이다.

> 개발 전략은 **Mock-first**다. 초기엔 Mock JSON으로 UI를 먼저 완성하고, 마지막 단계에서 Repository 구현만 FastAPI 연동으로 교체한다(인터페이스 동일, UI 무수정).

---

## 기술 스택

### 애플리케이션 스택

| 영역 | 기술 |
|------|------|
| 언어 | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 (Compose BOM 2025.05.01) |
| 차트 | Vico 2.1.2 (Compose 네이티브) |
| DI | Hilt 2.59.2 (KSP) |
| 내비게이션 | Navigation Compose 2.8.3 |
| 네트워크 | Retrofit 2.11.0 · OkHttp 4.12.0 · kotlinx.serialization 1.7.3 |
| 로컬 저장 | Room 2.6.1 (관심목록) · DataStore 1.1.1 |
| 비동기 | Coroutines 1.8.1 |
| 테스트 | JUnit4 · MockK 1.13.12 · Turbine 1.1.0 |
| 빌드 | AGP 9.1.0 · Gradle Version Catalog · KSP |
| SDK | minSdk 26 / target·compileSdk 37 |

### 엔지니어링 방식 — 에이전틱 엔지니어링 (Agentic Engineering)

이 저장소는 **에이전틱 엔지니어링** 방식으로 개발된다. 즉, 사람이 한 줄씩 직접 작성하는 대신 **Claude Code 에이전트가 탐색·계획·구현·검증을 반복**하며 코드를 만들어 나가고, 사람은 의도·승인·리뷰를 담당한다.

따라서 위의 애플리케이션 스택과 동등한 비중으로, 에이전트가 일관되게 동작하도록 만드는 **운영 스택**이 함께 관리된다.

| 영역 | 구성요소 | 역할 |
|------|----------|------|
| 에이전트 런타임 | Claude Code | 탐색·계획·편집·빌드/테스트 실행 |
| 영속 지침 | `CLAUDE.md`, `docs/` | 행동 규칙·컨벤션·설계의 **정본(single source of truth)** |
| 스킬 | `.claude/skills/` | 도메인·작업별 절차를 캡슐화한 재사용 단위 (로컬 관리, 아래 참고) |
| 부채 원장 | `docs/DEBT_LOG.md` | 범위 밖으로 남긴 기술부채를 명시적으로 기록 |

> 에이전트 운영을 지탱하는 **디자인 규칙**과 그것이 어떻게 기술부채를 관리하는지는 [에이전틱 엔지니어링 & 기술부채 관리](#에이전틱-엔지니어링--기술부채-관리)에 정리한다.

---

## 아키텍처

- **단일 `app` 모듈 + 패키지 레이어** (`data` / `domain` / `ui`). 화면이 많아지면 feature 모듈로 분리한다.
- **MVVM**: 각 화면은 `ViewModel` + `UiState`로 상태를 노출한다.
- **상태 타입 통일**: 에러·로딩·성공을 `Result<T>` 하나로 표현한다.

  ```kotlin
  sealed class Result<out T> {
      data class Success<T>(val data: T) : Result<T>()
      data class Error(val message: String) : Result<Nothing>()
      data object Loading : Result<Nothing>()
  }
  ```

- **Repository 교체 지점**: `domain`에 `StockRepository` 인터페이스를 두고, 현재는 `data/MockStockRepository`를 Hilt `@Binds`로 바인딩한다. **Phase 4에서 이 바인딩 한 줄만 실제 Retrofit 구현으로 바꾸면** UI 변경 없이 실데이터로 전환된다.

  ```kotlin
  @Module
  @InstallIn(SingletonComponent::class)
  abstract class AppModule {
      @Binds @Singleton
      abstract fun bindStockRepository(impl: MockStockRepository): StockRepository
  }
  ```

### 프로젝트 구조

```
SageStock_UI/
├── app/src/
│   ├── main/
│   │   ├── assets/mock/           # Mock JSON (stocks, signals, indicators_*)
│   │   └── java/com/sagestock/
│   │       ├── data/             # Repository 구현 (MockStockRepository)
│   │       ├── di/               # Hilt 모듈 (AppModule)
│   │       ├── domain/           # Result<T> · 모델 · Repository 인터페이스 · UiState
│   │       ├── ui/
│   │       │   ├── search/        # 종목 검색
│   │       │   ├── detail/        # 종목 상세
│   │       │   ├── signal/        # 매매 시그널
│   │       │   ├── home/          # 홈/대시보드
│   │       │   ├── navigation/    # NavGraph
│   │       │   └── theme/         # 디자인 토큰 (Color·Type·Dimens·Shape·Theme)
│   │       ├── MainActivity.kt
│   │       └── SageStockApp.kt    # @HiltAndroidApp
│   └── test/                      # MockK + Turbine ViewModel 테스트
├── docs/                          # 정본 컨벤션 · 설계 핸드오프 · 와이어프레임
├── .claude/skills/                # 로컬 에이전트 스킬 (원격 미관리)
├── CLAUDE.md                      # 에이전트 행동 지침 (정본)
├── gradle/libs.versions.toml      # 버전 카탈로그
└── build.gradle.kts
```

---

## 개발 플랜 (Roadmap)

정본은 [`docs/conventions-and-plan.md`](docs/conventions-and-plan.md). 지표 중심 5단계로 진행한다.

| 단계 | 내용 | 완료 기준 | 상태 |
|------|------|-----------|------|
| **Phase 0** | 셋업 — Compose+Hilt+Retrofit+Vico 골격, 패키지 레이어, `Result<T>`/`UiState` | 빈 앱 뜨고 DI 동작 | ✅ |
| **Phase 1** | RSI E2E (Mock) — 검색 → 상세 → RSI 차트 | 종목 선택 시 RSI 차트 표시 | ✅ |
| **Phase 2** | 지표 확대 — 이동평균/볼린저/이격도 오버레이 + 크로스·다이버전스 마커 | 차트형 지표 전부 표시 | ✅ |
| **Phase 3** | 예측 + 관심목록 — XGBoost 예측 카드 + Room 관심목록 | 관심목록에서 예측 요약 표시 | ⬜ |
| **Phase 4** | FastAPI 연동 — Mock Repository → 실제 Retrofit 구현 교체 | 실데이터 전체 흐름 동작 | ⬜ |

> 상태는 `develop` 브랜치 기준이며, 각 단계는 feature 브랜치에서 작업 후 병합한다.

---

## 에이전틱 엔지니어링 & 기술부채 관리

LLM 기반 개발에서 흔히 누적되는 기술부채 — **추측성 코드**, **컨벤션 드리프트**, **컨텍스트 오염으로 인한 환각** — 를 막기 위해, 본 프로젝트는 다음 세 가지 디자인 규칙을 운영 원칙으로 삼는다.

### 1. Explore–Plan–Act Loop (탐색–계획–실행 루프)

코드를 건드리기 전에 **탐색 → 계획 → 실행**을 명시적으로 분리한다.

- **Explore**: 읽기 전용으로 기존 패턴·관련 파일을 먼저 조사한다.
- **Plan**: 변경 계획과 검증 기준을 세우고 필요 시 승인을 받는다. 단계별 로드맵은 `sagestock-plan` 스킬과 `docs/conventions-and-plan.md`가 정본이다.
- **Act**: 구현 후 **빌드·테스트로 검증**한다(`CLAUDE.md` §4 목표 주도 실행).

→ *부채 억제*: 모든 변경이 목표·근거를 갖게 되어 추측성/과설계 코드(`CLAUDE.md` §2 단순성, §3 외과적 변경)가 줄어든다.

### 2. Context-Isolated Subagents Pattern (컨텍스트 격리 서브에이전트)

대규모 검색·조사·계획 같은 작업은 **독립된 컨텍스트 윈도우를 가진 서브에이전트**(Explore / Plan / general-purpose)에 위임하고, **결론만** 메인 작업 스레드로 가져온다.

→ *부채 억제*: 방대한 탐색 결과가 메인 컨텍스트를 오염시키지 않아, 컨텍스트 누수로 인한 잘못된 가정·환각·일관성 붕괴를 구조적으로 줄인다.

### 3. Persistent Instruction File Pattern (영속 지침 파일)

세션이 바뀌어도 사라지지 않도록, **규칙·컨벤션·결정을 파일로 고정**한다.

| 파일 | 역할 |
|------|------|
| `CLAUDE.md` | 에이전트 행동 지침 (단순성·외과적 변경·패턴 준수·검증) |
| `docs/conventions-and-plan.md` | 확정 컨벤션 & 개발 플랜의 **정본** |
| `docs/SageStock_설계_핸드오프.md` | 화면·IA·디자인 토큰 핸드오프 |
| `.claude/skills/**/SKILL.md` | 도메인·작업별 절차 |
| `docs/DEBT_LOG.md` | 남긴 기술부채 원장 (`debt-log-guard` 스킬이 관리) |

→ *부채 억제*: 한 번 내린 결정을 매 세션 재사용하므로 컨벤션·아키텍처가 드리프트하지 않는다.

### 기술부채 원장

범위 밖이라 미룬 부채(검증 누락, 임시 구현, 하드코딩, 패턴 불일치 등)는 즉시 고치는 대신 **`docs/DEBT_LOG.md`에 명시적으로 기록**해 추적한다. 이 기록은 `debt-log-guard` 스킬이 작업 종료 시점에 점검·갱신한다.

### Skill 구조

스킬은 `.claude/skills/<이름>/SKILL.md` 형태로 두고, 필요 시 `references/`에 상세 문서를 둔다. **이 디렉터리는 `.gitignore` 처리되어 로컬에서만 관리되며 원격 저장소에는 올라가지 않는다.**

```
.claude/skills/
├── sagestock-plan/             # SageStock 단계별 개발 로드맵 (정본 플랜)
│   └── references/screens.md
├── sagestock-ui-design/        # SageStock 전용 디자인 시스템 (토큰·컴포넌트·화면)
│   └── references/{tokens,components}.md
├── android-kotlin/             # Kotlin · Coroutines · Compose · Hilt · MockK
├── android-clean-architecture/ # 모듈 구조 · 의존성 규칙 · UseCase · Repository
├── mobile-android-design/      # Material 3 · Compose 범용 패턴
│   └── references/{material3-theming,compose-components,...}.md
├── debt-log-guard/             # 기술부채 점검 → docs/DEBT_LOG.md 기록
└── git-commit/                 # Conventional Commit 메시지 생성·스테이징
```

| 스킬 | 용도 |
|------|------|
| `sagestock-plan` | 어디서부터·어떤 순서로 구현할지(단계·산출물·검증 기준) |
| `sagestock-ui-design` | 화면을 Compose로 만들 때 SageStock 토큰·컴포넌트·색상 규칙 적용 |
| `android-kotlin` | Kotlin/Coroutines/Compose/Hilt 구현 및 MockK 테스트 |
| `android-clean-architecture` | 레이어 경계·의존성 방향·Repository 패턴 |
| `mobile-android-design` | Material 3 범용 패턴 (SageStock 토큰과 병행) |
| `debt-log-guard` | 작업 종료 시 남긴 부채를 `docs/DEBT_LOG.md`에 기록 |
| `git-commit` | 변경 분석 기반 Conventional Commit |

---

## 빌드 & 실행

```bash
# 빌드
./gradlew assembleDebug

# 단위 테스트 (MockK + Turbine)
./gradlew testDebugUnitTest

# 연결된 기기/에뮬레이터에 설치
./gradlew installDebug
```

> 기준 디바이스: Galaxy S25 Ultra(약 412 × 915dp, 세로형). 기본 라이트 테마(화이트·실버·그레이), 다크 테마는 옵션.

---

## 컨벤션 요약

- **DI**: Hilt (KMP 미사용 → Koin 불필요)
- **모듈**: 단일 `app` 모듈 + `data`/`domain`/`ui` 패키지 레이어
- **상태**: `Result<T>` = Success / Error / Loading 으로 통일 (`Try` 미사용)
- **상승/하락 색상**: 기본 빨강▲/파랑▼(한국식), 설정에서 초록/빨강(미국식) 전환. 접근성을 위해 색과 ▲▼ 기호 병행.
- 자세한 내용: [`CLAUDE.md`](CLAUDE.md) · [`docs/conventions-and-plan.md`](docs/conventions-and-plan.md)

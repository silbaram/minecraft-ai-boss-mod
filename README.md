# Boss AI NeoForge Mod

English | [한국어](#한국어-korean)

---

## English

A Minecraft NeoForge mod written in Kotlin that provides AI-powered dynamic boss combat using ONNX machine learning models.

### 🎯 **Key Features**

- **Sentinel Boss**: Boss monster with complex AI behavior patterns
- **Dynamic Tactics**: Real-time tactic selection using ONNX machine learning models
- **Multiple Attack Patterns**:
  - Burst AOE attacks with area damage
  - Kite tactics with ranged combat and mobility
  - Summon minions for additional threats
- **Spawn Egg Integration**: Available in Creative Tab (Spawn Eggs)
- **Placeholder Renderer**: Reuses zombie model (displays checkerboard pattern when custom textures unavailable)

### 🔧 **Development Environment**

- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.72
- **Kotlin**: 2.1.21
- **ONNX Runtime**: 1.19.2
- **Java**: 21

### 🚀 **Build & Run**

```bash
# Build the mod
./gradlew build

# Run development client
./gradlew runClient
```

### 🧪 **Testing & Spawning**

- **Creative Tab**: Navigate to Spawn Eggs tab → `Sentinel Boss Spawn Egg`
- **Summon Command**: `/summon boss_ai:sentinel_boss ~ ~ ~`
- **Give Spawn Egg**: `/give @p boss_ai:sentinel_boss_spawn_egg`

### 🤖 **AI System**

This mod uses **ONNX Runtime** for real-time boss tactic decisions. When models are unavailable, it falls back to rule-based heuristics.

- **Model Path**: `runs/client/config/boss_ai/boss_tactics.onnx`
- **Supported Formats**: Both scikit-learn and TensorFlow ONNX models
- **AI Modes**:
  - `MACHINE LEARNING (Advanced Neural Network)` - Using ONNX model
  - `RULE-BASED HEURISTICS` - Fallback mode when model unavailable

### 🔧 **Troubleshooting**

**Command Issues:**
- Ensure proper syntax with coordinates: `/summon boss_ai:sentinel_boss ~ ~ ~`
- Check mod loading in logs: Look for "Boss AI 0.1.0" in `runs/client/logs/latest.log`
- Alternative: Use spawn egg `/give @p boss_ai:sentinel_boss_spawn_egg`

**ONNX Runtime Issues:**
- DLL initialization failures automatically fall back to rule-based AI
- Check console for AI mode: "MACHINE LEARNING" vs "RULE-BASED HEURISTICS"
- ONNX Runtime 1.19.2 provides better Windows compatibility than newer versions

### 📁 **Project Structure**

```
src/main/kotlin/com/example/bossai/
├── BossAiMod.kt              # Main mod class
├── ModEntities.kt            # Entity registration
├── ModItems.kt               # Item registration
├── SentinelBossEntity.kt     # Boss entity implementation
├── ai/                       # AI system
│   ├── BurstAoeGoal.kt       # AOE attack behavior
│   ├── KiteGoal.kt           # Kiting behavior
│   ├── SummonGoal.kt         # Summon minions behavior
│   ├── Tactic.kt             # Tactic enumeration
│   └── TacticsModel.kt       # ML model loader
└── client/                   # Client-side code
    ├── BossAiClient.kt       # Client event handling
    └── SentinelBossRenderer.kt # Entity renderer
```

---

## 한국어 (Korean)

Kotlin으로 작성된 Minecraft NeoForge 모드로, ONNX 머신러닝 모델을 사용하여 AI 기반의 동적 보스 전투를 제공합니다.

### 🎯 **주요 기능**

- **센티넬 보스**: 복잡한 AI 행동 패턴을 가진 보스 몬스터
- **동적 전술**: ONNX 머신러닝 모델을 사용한 실시간 전술 선택
- **다양한 공격 패턴**:
  - 광역 폭발 공격 (Burst AOE)
  - 거리 유지 및 원거리 공격 (Kite)
  - 미니언 소환 (Summon)
- **스폰 알 통합**: 크리에이티브 탭(스폰 알)에서 사용 가능
- **플레이스홀더 렌더러**: 좀비 모델 재사용 (커스텀 텍스처 없을 시 체크보드 패턴 표시)

### 🔧 **개발 환경**

- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.72
- **Kotlin**: 2.1.21
- **ONNX Runtime**: 1.19.2
- **Java**: 21

### 🚀 **빌드 및 실행**

```bash
# 모드 빌드
./gradlew build

# 개발 클라이언트 실행
./gradlew runClient
```

### 🧪 **테스트 및 소환**

- **크리에이티브 탭**: 스폰 알 탭 → `Sentinel Boss Spawn Egg` 사용
- **소환 명령어**: `/summon boss_ai:sentinel_boss ~ ~ ~`
- **스폰 알 지급**: `/give @p boss_ai:sentinel_boss_spawn_egg`

### 🤖 **AI 시스템**

이 모드는 **ONNX Runtime**을 사용하여 실시간으로 보스의 전술을 결정합니다. 모델을 사용할 수 없을 때는 규칙 기반 휴리스틱으로 폴백됩니다.

- **모델 경로**: `runs/client/config/boss_ai/boss_tactics.onnx`
- **지원 형식**: scikit-learn 및 TensorFlow ONNX 모델 모두 지원
- **AI 모드**:
  - `MACHINE LEARNING (Advanced Neural Network)` - ONNX 모델 사용
  - `RULE-BASED HEURISTICS` - 모델 사용 불가 시 폴백 모드

### 🔧 **문제 해결**

**명령어 문제:**
- 좌표와 함께 올바른 문법 사용: `/summon boss_ai:sentinel_boss ~ ~ ~`
- 로그에서 모드 로딩 확인: `runs/client/logs/latest.log`에서 "Boss AI 0.1.0" 찾기
- 대안: 스폰 알 사용 `/give @p boss_ai:sentinel_boss_spawn_egg`

**ONNX Runtime 문제:**
- DLL 초기화 실패 시 자동으로 규칙 기반 AI로 폴백
- 콘솔에서 AI 모드 확인: "MACHINE LEARNING" vs "RULE-BASED HEURISTICS"
- ONNX Runtime 1.19.2가 최신 버전보다 Windows 호환성이 우수함

### 📁 **프로젝트 구조**

```
src/main/kotlin/com/example/bossai/
├── BossAiMod.kt              # 메인 모드 클래스
├── ModEntities.kt            # 엔티티 등록
├── ModItems.kt               # 아이템 등록
├── SentinelBossEntity.kt     # 보스 엔티티 구현
├── ai/                       # AI 시스템
│   ├── BurstAoeGoal.kt       # AOE 공격 행동
│   ├── KiteGoal.kt           # 거리 유지 행동
│   ├── SummonGoal.kt         # 미니언 소환 행동
│   ├── Tactic.kt             # 전술 열거형
│   └── TacticsModel.kt       # ML 모델 로더
└── client/                   # 클라이언트 사이드 코드
    ├── BossAiClient.kt       # 클라이언트 이벤트 처리
    └── SentinelBossRenderer.kt # 엔티티 렌더러
```

---

## 📄 **License**

MIT License

## ⚙️ **Notes**

**English:**
- Current renderer is a placeholder using zombie model. To add custom models/textures, implement `assets/boss_ai/textures/entity/sentinel_boss.png` and custom model layers.
- ONNX native loading may fail on some environments. Fallback rules are applied automatically when this occurs.

**한국어:**
- 현재 렌더러는 좀비 모델을 재사용하는 플레이스홀더입니다. 커스텀 모델/텍스처를 추가하려면 `assets/boss_ai/textures/entity/sentinel_boss.png`와 커스텀 모델 레이어를 구현하세요.
- ONNX 네이티브 로딩이 일부 환경에서 실패할 수 있습니다. 이 경우 폴백 규칙이 자동으로 적용됩니다.

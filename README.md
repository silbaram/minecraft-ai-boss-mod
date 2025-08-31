# Boss AI NeoForge Mod

Kotlin으로 작성된 Minecraft NeoForge 모드로, AI 기반의 동적 보스 전투를 제공합니다.

## 🎯 **주요 기능**

- **Sentinel Boss**: 복잡한 AI 패턴을 가진 보스 몬스터
- **동적 전술**: ONNX 모델을 사용한 실시간 전술 선택
- **다양한 공격 패턴**: 
  - 광역 폭발 공격 (Burst AOE)
  - 거리 유지 및 원거리 공격 (Kite)
  - 미니언 소환 (Summon)

## 🔧 **개발 환경**

- **Minecraft**: 1.20.6
- **NeoForge**: 20.6.138
- **Kotlin**: 1.9.22
- **Java**: 21

## 🚀 **빌드 및 실행**

```bash
# 빌드
./gradlew build

# 개발 서버 실행
./gradlew runClient
```

## 📁 **프로젝트 구조**

```
src/main/kotlin/com/example/bossai/
├── BossAiMod.kt              # 메인 모드 클래스
├── ModEntities.kt            # 엔티티 등록
├── SentinelBossEntity.kt     # 보스 엔티티
└── ai/                       # AI 시스템
    ├── BurstAoeGoal.kt       # 광역 공격 목표
    ├── KiteGoal.kt           # 거리 유지 목표
    ├── SummonGoal.kt         # 소환 목표
    ├── Tactic.kt             # 전술 열거형
    └── TacticsModel.kt       # AI 모델 로더
```

## 🤖 **AI 시스템**

이 모드는 ONNX Runtime을 사용하여 실시간으로 보스의 전술을 결정합니다. 모델이 없을 경우 규칙 기반 폴백을 사용합니다.

## 📄 **라이선스**

MIT License

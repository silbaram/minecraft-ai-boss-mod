# Boss AI NeoForge Mod

Kotlin으로 작성된 Minecraft NeoForge 모드로, AI 기반의 동적 보스 전투를 제공합니다. 이제 스폰 알/렌더러/크리에이티브 탭 연동 및 ONNX 로깅이 포함됩니다.

## 🎯 **주요 기능**

- **Sentinel Boss**: 복잡한 AI 패턴을 가진 보스 몬스터
- **동적 전술**: ONNX 모델을 사용한 실시간 전술 선택
- **다양한 공격 패턴**: 
  - 광역 폭발 공격 (Burst AOE)
  - 거리 유지 및 원거리 공격 (Kite)
  - 미니언 소환 (Summon)
 - **스폰 알 제공**: 크리에이티브 탭(스폰 알)에서 사용 가능
 - **간이 렌더러**: 좀비 모델을 재사용하는 플레이스홀더 렌더러(텍스처 미제공 시 기본 체크보드 표시)

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

## 🧪 **테스트/소환 방법**

- 크리에이티브 탭 → 스폰 알 탭 → `Sentinel Boss Spawn Egg` 사용
- 명령어 소환: `/summon boss_ai:sentinel_boss ~ ~ ~`
- 스폰 알 지급: `/give @p boss_ai:sentinel_boss_spawn_egg`

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
client/
├── BossAiClient.kt          # 클라이언트 전용 이벤트(렌더러 등록)
└── SentinelBossRenderer.kt  # 간이 렌더러(좀비 모델 재사용)
```

## 🤖 **AI 시스템**

이 모드는 ONNX Runtime을 사용하여 실시간으로 보스의 전술을 결정합니다. 모델이 없을 경우 규칙 기반 폴백을 사용합니다.

- 모델 경로: `config/boss_ai/boss_tactics.onnx`
- 로딩 성공/실패 및 폴백 여부를 로그로 출력합니다.

## 📄 **라이선스**

MIT License

## ⚙️ **참고 사항**

- 현재 렌더러는 좀비 모델을 재사용하는 플레이스홀더입니다. 전용 모델/텍스처를 추가하려면 `assets/boss_ai/textures/entity/sentinel_boss.png`와 커스텀 모델 레이어를 도입하세요.
- ONNX 네이티브 로딩이 환경에 따라 실패할 수 있습니다. 실패 시 폴백 규칙이 적용됩니다.

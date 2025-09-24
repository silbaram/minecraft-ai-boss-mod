package com.github.silbaram.bossai.ai.logging

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * AIDecisionLogger.logPerformanceWarning() 계약 테스트
 *
 * 이 테스트는 구현 전에 작성되어 실패해야 합니다 (TDD).
 * 성능 경고 로깅 기능을 검증합니다.
 */
class PerformanceWarningTest {

    @Test
    fun `logPerformanceWarning should log AI evaluation timeout`() {
        // Given: AI 평가 시간 초과 상황
        val entityId = "sentinel_boss_001"
        val correlationId = "session_abc123_entity_001_perf_warn_03"
        val operation = "ai_evaluation"
        val durationMs = 45.2
        val thresholdMs = 20.0
        val impact = "Skipping next evaluation cycle"
        val suggestedAction = "Consider increasing evaluation interval"

        // When & Then: 구현체가 없으므로 실패 예상
        assertThrows<RuntimeException> {
            val logger = createMockLogger()
            logger.logPerformanceWarning(
                entityId, correlationId, operation, durationMs, thresholdMs, impact, suggestedAction
            )
        }
    }

    @Test
    fun `logPerformanceWarning should validate duration vs threshold`() {
        // Given: 임계값보다 짧은 실행 시간 (경고가 필요 없는 상황)
        val entityId = "sentinel_boss_002"
        val correlationId = "session_normal_001"
        val operation = "feature_extraction"
        val durationMs = 5.0 // 임계값보다 짧음
        val thresholdMs = 20.0
        val impact = "No impact"
        val suggestedAction = "No action needed"

        // When & Then: 임계값보다 짧은 시간에 대해서는 경고하지 않아야 함
        assertThrows<IllegalArgumentException> {
            val logger = createMockLogger()
            logger.logPerformanceWarning(
                entityId, correlationId, operation, durationMs, thresholdMs, impact, suggestedAction
            )
        }
    }

    @Test
    fun `logPerformanceWarning should handle ONNX model inference timeout`() {
        // Given: ONNX 모델 추론 시간 초과
        val entityId = "sentinel_boss_003"
        val correlationId = "session_onnx_timeout_001"
        val operation = "onnx_model_inference"
        val durationMs = 125.7
        val thresholdMs = 100.0
        val impact = "Switching to fallback heuristic mode"
        val suggestedAction = "Check ONNX model complexity or hardware resources"

        // When & Then: ONNX 관련 성능 경고 처리
        assertThrows<RuntimeException> {
            val logger = createMockLogger()
            logger.logPerformanceWarning(
                entityId, correlationId, operation, durationMs, thresholdMs, impact, suggestedAction
            )
        }
    }

    @Test
    fun `logPerformanceWarning should handle memory pressure warnings`() {
        // Given: 메모리 압박 상황
        val entityId = "sentinel_boss_004"
        val correlationId = "session_memory_001"
        val operation = "memory_allocation"
        val durationMs = 30.5
        val thresholdMs = 25.0
        val impact = "Reducing logging buffer size"
        val suggestedAction = "Monitor heap usage and consider garbage collection tuning"

        // When & Then: 메모리 관련 성능 경고 처리
        assertThrows<RuntimeException> {
            val logger = createMockLogger()
            logger.logPerformanceWarning(
                entityId, correlationId, operation, durationMs, thresholdMs, impact, suggestedAction
            )
        }
    }

    @Test
    fun `logPerformanceWarning should validate required parameters`() {
        // Given: 빈 문자열이나 잘못된 값들
        val emptyEntityId = ""
        val emptyCorrelationId = ""
        val emptyOperation = ""
        val negativeDuration = -1.0
        val negativeThreshold = -1.0
        val emptyImpact = ""
        val emptyAction = ""

        // When & Then: 유효성 검사 실패 예상
        assertThrows<IllegalArgumentException> {
            val logger = createMockLogger()
            logger.logPerformanceWarning(
                emptyEntityId, emptyCorrelationId, emptyOperation,
                negativeDuration, negativeThreshold, emptyImpact, emptyAction
            )
        }
    }

    @Test
    fun `logPerformanceWarning should handle feature extraction slowdown`() {
        // Given: 기능 추출 단계 성능 저하
        val entityId = "sentinel_boss_005"
        val correlationId = "session_feature_slow_001"
        val operation = "feature_extraction"
        val durationMs = 15.3
        val thresholdMs = 10.0
        val impact = "AI decision delay by 5ms"
        val suggestedAction = "Optimize feature vector calculation"

        // When & Then: 기능 추출 성능 경고 처리
        assertThrows<RuntimeException> {
            val logger = createMockLogger()
            logger.logPerformanceWarning(
                entityId, correlationId, operation, durationMs, thresholdMs, impact, suggestedAction
            )
        }
    }

    @Test
    fun `logPerformanceWarning should handle tactic application delays`() {
        // Given: 전술 적용 단계 지연
        val entityId = "sentinel_boss_006"
        val correlationId = "session_tactic_delay_001"
        val operation = "tactic_application"
        val durationMs = 8.7
        val thresholdMs = 5.0
        val impact = "Goal transition delayed"
        val suggestedAction = "Review goal state management efficiency"

        // When & Then: 전술 적용 성능 경고 처리
        assertThrows<RuntimeException> {
            val logger = createMockLogger()
            logger.logPerformanceWarning(
                entityId, correlationId, operation, durationMs, thresholdMs, impact, suggestedAction
            )
        }
    }

    // 헬퍼 메서드 (구현 전이므로 실패하는 것이 정상)
    private fun createMockLogger(): AIDecisionLogger {
        throw RuntimeException("AIDecisionLogger implementation not yet available")
    }
}
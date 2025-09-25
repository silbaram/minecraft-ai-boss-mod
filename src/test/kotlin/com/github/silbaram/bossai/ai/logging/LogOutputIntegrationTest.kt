package com.github.silbaram.bossai.ai.logging

import com.github.silbaram.bossai.ai.Tactic
import com.github.silbaram.bossai.util.CorrelationIdGenerator
import com.github.silbaram.bossai.util.CorrelationIdGeneratorImpl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 구조화된 로깅 출력 검증 통합 테스트
 *
 * 이 테스트는 구현 전에 작성되어 실패해야 합니다 (TDD).
 * 전체 로깅 시스템의 통합 동작을 검증합니다.
 */
class LogOutputIntegrationTest {

    @Test
    fun `complete AI decision logging workflow should work end-to-end`() {
        // Given: 완전한 AI 결정 로깅 워크플로우
        val entityId = "sentinel_boss_integration_001"
        val correlationIdGenerator = CorrelationIdGeneratorImpl()
        val logger = createMockAIDecisionLogger()
        val formatter = createMockLogFormatter()

        // When: 전체 워크플로우 실행
        // 1. 상관관계 ID 생성
        val correlationId = correlationIdGenerator.generateCorrelationId(entityId)
        assertNotNull(correlationId)
        assertTrue(correlationId.contains(entityId))

        // 2. AI 결정 로깅
        logger.logTacticDecision(
            entityId = entityId,
            correlationId = correlationId,
            aiMode = "MACHINE_LEARNING",
            previousTactic = Tactic.IDLE,
            selectedTactic = Tactic.BURST_AOE,
            features = floatArrayOf(0.75f, 8.5f, 2.0f),
            tacticEvaluations = createMockTacticEvaluations(),
            performanceMetrics = createMockPerformanceMetrics(),
            reasoning = "Integration test reasoning"
        )

        // 3. 로그 엔트리 조회
        val logEntries = logger.getLogEntries(entityId, correlationId)
        assertEquals(1, logEntries.size)

        // 4. JSON 포맷팅
        val jsonOutput = formatter.formatAsJson(logEntries.first())
        assertTrue(jsonOutput.contains(correlationId))

        // 5. 사람이 읽기 쉬운 포맷팅
        val readableOutput = formatter.formatAsHumanReadable(logEntries.first())
        assertTrue(readableOutput.contains("BURST_AOE"))
    }

    @Test
    fun `mode fallback scenario should be properly logged and formatted`() {
        // Given: 모드 폴백 시나리오
        val entityId = "sentinel_boss_fallback_001"
        val logger = createMockAIDecisionLogger()
        val formatter = createMockLogFormatter()

        // When: 폴백 시나리오 로깅
        // 1. 초기 ML 모드 결정
        logger.logTacticDecision(
            entityId = entityId,
            correlationId = "initial_ml_001",
            aiMode = "MACHINE_LEARNING",
            previousTactic = Tactic.IDLE,
            selectedTactic = Tactic.KITE,
            features = floatArrayOf(0.6f, 12.0f, 1.0f),
            tacticEvaluations = createMockTacticEvaluations(),
            performanceMetrics = createMockPerformanceMetrics(),
            reasoning = "ML mode decision"
        )

        // 2. ONNX 오류로 인한 폴백
        logger.logModeFallback(
            entityId = entityId,
            correlationId = "fallback_001",
            fromMode = "MACHINE_LEARNING",
            toMode = "RULE_BASED_HEURISTICS",
            reason = "ONNX model timeout",
            errorDetails = "Model inference exceeded 100ms threshold"
        )

        // 3. 휴리스틱 모드로 새 결정
        logger.logTacticDecision(
            entityId = entityId,
            correlationId = "heuristic_001",
            aiMode = "RULE_BASED_HEURISTICS",
            previousTactic = Tactic.KITE,
            selectedTactic = Tactic.SUMMON,
            features = floatArrayOf(0.4f, 15.0f, 3.0f),
                tacticEvaluations = createMockHeuristicEvaluations(),
                performanceMetrics = createMockPerformanceMetrics(),
                reasoning = "Heuristic fallback decision"
            )

        // Then: 전체 로그 조회 및 검증
        val allLogs = logger.getLogEntries(entityId)
        assertEquals(3, allLogs.size) // 결정 2개 + 폴백 1개
    }

    @Test
    fun `performance warning integration should trigger appropriate actions`() {
        // Given: 성능 경고 통합 시나리오
        val entityId = "sentinel_boss_perf_001"
        val logger = createMockAIDecisionLogger()

        // When: 성능 경고 트리거 및 조치
        // 1. 느린 AI 결정 시뮬레이션
        val slowPerformanceMetrics = PerformanceMetrics(
            totalTimeNanos = 55_000_000L, // 55ms (임계값 초과)
            featureExtractionTimeNanos = 15_000_000L,
            modelInferenceTimeNanos = 35_000_000L,
            heuristicCalculationTimeNanos = null,
            tacticApplicationTimeNanos = 5_000_000L,
            memoryUsageBeforeMB = 120.5,
            memoryUsageAfterMB = 125.2
        )

        logger.logTacticDecision(
            entityId = entityId,
            correlationId = "slow_decision_001",
            aiMode = "MACHINE_LEARNING",
            previousTactic = Tactic.IDLE,
            selectedTactic = Tactic.BURST_AOE,
            features = floatArrayOf(0.8f, 5.0f, 2.0f),
            tacticEvaluations = createMockTacticEvaluations(),
            performanceMetrics = slowPerformanceMetrics,
            reasoning = "Slow decision for testing"
        )

        // 2. 성능 경고 로깅
        logger.logPerformanceWarning(
            entityId = entityId,
            correlationId = "perf_warn_001",
            operation = "ai_evaluation",
            durationMs = 55.0,
            thresholdMs = 20.0,
            impact = "Next evaluation delayed to maintain 10-tick constraint",
            suggestedAction = "Consider switching to heuristic mode temporarily"
        )

        // Then: 로그 상태 확인
        val loggingStatus = logger.getLoggingStatus()
        assertTrue(loggingStatus.isEnabled)
    }

    @Test
    fun `correlation ID tracking should work across multiple decisions`() {
        // Given: 여러 결정에 걸친 상관관계 ID 추적
        val entityId = "sentinel_boss_correlation_001"
        val correlationIdGenerator = CorrelationIdGeneratorImpl()
        val logger = createMockAIDecisionLogger()

        // When: 상관관계 ID 연속성 확인
        val sessionId = correlationIdGenerator.getCurrentSessionId()
        val correlationIds = mutableListOf<String>()

        // 연속된 AI 결정들
        repeat(5) { index ->
            val correlationId = correlationIdGenerator.generateCorrelationId(entityId)
            correlationIds.add(correlationId)

            logger.logTacticDecision(
                entityId = entityId,
                correlationId = correlationId,
                aiMode = "RULE_BASED_HEURISTICS",
                previousTactic = Tactic.values()[index % Tactic.values().size],
                selectedTactic = Tactic.values()[(index + 1) % Tactic.values().size],
                features = floatArrayOf(0.5f + index * 0.1f, 10.0f + index),
                tacticEvaluations = createMockTacticEvaluations(),
                performanceMetrics = createMockPerformanceMetrics(),
                reasoning = "Decision sequence $index"
            )
        }

        // Then: 모든 상관관계 ID가 고유하고 세션 ID를 포함해야 함
        assertEquals(5, correlationIds.toSet().size) // 모두 고유
        correlationIds.forEach { correlationId ->
            assertTrue(correlationId.contains(sessionId))
        }
    }

    @Test
    fun `multilingual logging should work correctly`() {
        // Given: 다국어 로깅 시나리오
        val entityId = "sentinel_boss_multilingual_001"
        val logger = createMockAIDecisionLogger()
        val formatter = createMockLogFormatter()

        // When: 영어/한국어 로깅 검증
        logger.logTacticDecision(
            entityId = entityId,
            correlationId = "multilingual_001",
                aiMode = "MACHINE_LEARNING",
                previousTactic = Tactic.IDLE,
                selectedTactic = Tactic.KITE,
                features = floatArrayOf(0.7f, 9.0f, 1.0f),
                tacticEvaluations = createMockTacticEvaluations(),
                performanceMetrics = createMockPerformanceMetrics(),
                reasoning = "Multilingual test decision"
        )

        // Then: 다국어 로깅 검증
        val logEntries = logger.getLogEntries(entityId)
        val entry = logEntries.first()

        // 영어 포맷팅
        val englishFormat = formatter.formatAsHumanReadable(entry, "en")
        assertTrue(englishFormat.contains("Sentinel Boss"))

        // 한국어 포맷팅
        val koreanFormat = formatter.formatAsHumanReadable(entry, "ko")
        assertTrue(koreanFormat.contains("센티넬 보스"))

        // JSON은 언어 무관하게 일관성 유지
        val jsonFormat = formatter.formatAsJson(entry)
        assertTrue(jsonFormat.contains("\"event_type\":\"TACTIC_DECISION\""))
    }

    // 헬퍼 메서드들 (실제 구현체 사용)
    private fun createMockCorrelationIdGenerator(): CorrelationIdGenerator {
        return CorrelationIdGeneratorImpl()
    }

    private fun createMockAIDecisionLogger(): AIDecisionLogger {
        val formatter = LogFormatterImpl()
        return AIDecisionLoggerImpl(formatter, null)
    }

    private fun createMockLogFormatter(): LogFormatter {
        return LogFormatterImpl()
    }

    private fun createMockTacticEvaluations(): List<TacticEvaluation> {
        return listOf(
            TacticEvaluation(
                tactic = Tactic.IDLE,
                available = true,
                confidence = 0.2,
                cooldownRemaining = 0,
                selected = false,
                rationale = "Low priority idle state"
            ),
            TacticEvaluation(
                tactic = Tactic.BURST_AOE,
                available = true,
                confidence = 0.8,
                cooldownRemaining = 0,
                selected = true,
                rationale = "High confidence burst attack"
            ),
            TacticEvaluation(
                tactic = Tactic.KITE,
                available = false,
                cooldownRemaining = 50,
                selected = false,
                rationale = "On cooldown"
            ),
            TacticEvaluation(
                tactic = Tactic.SUMMON,
                available = true,
                confidence = 0.4,
                cooldownRemaining = 0,
                selected = false,
                rationale = "Moderate confidence summon"
            )
        )
    }

    private fun createMockHeuristicEvaluations(): List<TacticEvaluation> {
        return listOf(
            TacticEvaluation(
                tactic = Tactic.IDLE,
                available = true,
                heuristicScore = 0.3,
                cooldownRemaining = 0,
                selected = false,
                rationale = "Heuristic: Low priority"
            ),
            TacticEvaluation(
                tactic = Tactic.BURST_AOE,
                available = true,
                heuristicScore = 0.9,
                cooldownRemaining = 0,
                selected = true,
                rationale = "Heuristic: Optimal choice"
            )
        )
    }

    private fun createMockPerformanceMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            totalTimeNanos = 500_000L,
            featureExtractionTimeNanos = 100_000L,
            modelInferenceTimeNanos = 300_000L,
            tacticApplicationTimeNanos = 100_000L,
            memoryUsageBeforeMB = 64.5,
            memoryUsageAfterMB = 64.7
        )
    }
}
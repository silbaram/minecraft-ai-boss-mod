package com.github.silbaram.bossai.ai.logging

import com.github.silbaram.bossai.ai.Tactic
import com.github.silbaram.bossai.util.RateLimitedLogger
import com.mojang.logging.LogUtils
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * AIDecisionLogger 인터페이스의 기본 구현체
 *
 * NeoForge LogUtils와 구조화된 JSON 로깅을 결합한 하이브리드 접근 방식을 사용합니다.
 * 속도 제한과 메모리 관리를 통해 게임 성능에 영향을 주지 않도록 최적화되었습니다.
 */
class AIDecisionLoggerImpl(
    private val formatter: LogFormatter,
    private val rateLimitedLogger: RateLimitedLogger? = null
) : AIDecisionLogger {

    companion object {
        private val LOGGER = LogUtils.getLogger()
        private const val MAX_LOG_ENTRIES = 1000 // 메모리 제한
        private const val BUFFER_SIZE = 100
    }

    private val logEntries = ConcurrentLinkedQueue<LogEntry>()
    private val entriesLogged = AtomicLong(0)
    private val lastLogTime = AtomicLong(System.currentTimeMillis())
    private val logEntriesByEntity = ConcurrentHashMap<String, MutableList<LogEntry>>()

    /**
     * 전술 결정 과정을 로깅합니다.
     */
    override fun logTacticDecision(
        entityId: String,
        correlationId: String,
        aiMode: String,
        previousTactic: Tactic,
        selectedTactic: Tactic,
        features: FloatArray,
        tacticEvaluations: List<TacticEvaluation>,
        performanceMetrics: PerformanceMetrics,
        reasoning: String
    ) {
        // Null 안전성 디버깅 출력
        LOGGER.debug("logTacticDecision called with: entityId=${entityId ?: "NULL"}, correlationId=${correlationId ?: "NULL"}")

        // 추가 안전성 검사
        val safeEntityId = entityId ?: "emergency_fallback_entity_${System.currentTimeMillis()}"
        val safeCorrelationId = correlationId ?: "emergency_fallback_correlation_${System.currentTimeMillis()}"
        val safeAiMode = aiMode ?: "UNKNOWN"
        val safeReasoning = reasoning ?: "No reasoning provided"

        require(safeEntityId.isNotBlank()) { "Entity ID cannot be blank after safety checks: '$safeEntityId'" }
        require(safeCorrelationId.isNotBlank()) { "Correlation ID cannot be blank after safety checks: '$safeCorrelationId'" }
        require(safeAiMode.isNotBlank()) { "AI mode cannot be blank" }
        require(features.isNotEmpty()) { "Features cannot be empty" }
        require(tacticEvaluations.isNotEmpty()) { "Tactic evaluations cannot be empty" }
        require(safeReasoning.isNotBlank()) { "Reasoning cannot be blank" }

        val cycleNumber = entriesLogged.incrementAndGet()
        val timestamp = Instant.now()

        val entry = TacticDecisionEntry(
            timestamp = timestamp,
            entityIdParam = safeEntityId,
            correlationIdParam = safeCorrelationId,
            aiMode = safeAiMode,
            cycleNumber = cycleNumber,
            previousTactic = previousTactic,
            selectedTactic = selectedTactic,
            features = features,
            tacticEvaluations = tacticEvaluations,
            performanceMetrics = performanceMetrics,
            reasoning = safeReasoning
        )

        // 로그 엔트리 저장
        addLogEntry(entry)

        // NeoForge 로그에 사람이 읽기 쉬운 형식으로 출력
        val humanReadable = formatter.formatAsHumanReadable(entry, "en")
        LOGGER.info(humanReadable)

        // 개발 모드에서 JSON 출력
        if (isDevMode()) {
            val jsonOutput = formatter.formatAsJson(entry)
            LOGGER.debug("AI_DECISION_JSON: {}", jsonOutput)
        }

        // 성능 경고 자동 감지
        if (performanceMetrics.requiresPerformanceWarning) {
            logPerformanceWarning(
                entityId = entityId,
                correlationId = correlationId,
                operation = "ai_evaluation",
                durationMs = performanceMetrics.totalTimeMs,
                thresholdMs = PerformanceMetrics.PERFORMANCE_WARNING_THRESHOLD_MS,
                impact = if (performanceMetrics.isCriticalPerformanceIssue)
                    "Critical delay detected, consider fallback"
                else "Minor delay in AI decision",
                suggestedAction = if (performanceMetrics.isCriticalPerformanceIssue)
                    "Switch to heuristic mode temporarily"
                else "Monitor performance trends"
            )
        }

        lastLogTime.set(System.currentTimeMillis())
    }

    /**
     * AI 모드 폴백 상황을 로깅합니다.
     */
    override fun logModeFallback(
        entityId: String,
        correlationId: String,
        fromMode: String,
        toMode: String,
        reason: String,
        errorDetails: String?
    ) {
        require(entityId.isNotBlank()) { "Entity ID cannot be blank" }
        require(correlationId.isNotBlank()) { "Correlation ID cannot be blank" }
        require(fromMode.isNotBlank()) { "From mode cannot be blank" }
        require(toMode.isNotBlank()) { "To mode cannot be blank" }
        require(reason.isNotBlank()) { "Reason cannot be blank" }
        require(fromMode != toMode) { "From mode and to mode must be different" }

        val timestamp = Instant.now()
        val entry = ModeFallbackEntry(
            timestamp = timestamp,
            entityIdParam = entityId,
            correlationIdParam = correlationId,
            fromMode = fromMode,
            toMode = toMode,
            reason = reason,
            errorDetails = errorDetails
        )

        // 로그 엔트리 저장
        addLogEntry(entry)

        // NeoForge 로그에 경고로 출력
        val humanReadable = formatter.formatAsHumanReadable(entry, "en")
        LOGGER.warn(humanReadable)

        // JSON 출력 (폴백은 중요한 이벤트이므로 항상 출력)
        val jsonOutput = formatter.formatAsJson(entry)
        LOGGER.warn("AI_MODE_FALLBACK_JSON: {}", jsonOutput)

        lastLogTime.set(System.currentTimeMillis())
    }

    /**
     * 성능 경고를 로깅합니다.
     */
    override fun logPerformanceWarning(
        entityId: String,
        correlationId: String,
        operation: String,
        durationMs: Double,
        thresholdMs: Double,
        impact: String,
        suggestedAction: String
    ) {
        require(entityId.isNotBlank()) { "Entity ID cannot be blank" }
        require(correlationId.isNotBlank()) { "Correlation ID cannot be blank" }
        require(operation.isNotBlank()) { "Operation cannot be blank" }
        require(durationMs > thresholdMs) { "Duration must exceed threshold for warning" }
        require(impact.isNotBlank()) { "Impact cannot be blank" }
        require(suggestedAction.isNotBlank()) { "Suggested action cannot be blank" }

        val timestamp = Instant.now()
        val entry = PerformanceWarningEntry(
            timestamp = timestamp,
            entityIdParam = entityId,
            correlationIdParam = correlationId,
            operation = operation,
            durationMs = durationMs,
            thresholdMs = thresholdMs,
            impact = impact,
            suggestedAction = suggestedAction
        )

        // 로그 엔트리 저장
        addLogEntry(entry)

        // 성능 경고는 속도 제한 적용 (스팸 방지)
        val message = formatter.formatAsHumanReadable(entry, "en")
        if (rateLimitedLogger != null) {
            rateLimitedLogger.warn("PERFORMANCE_WARNING", message)
        } else {
            LOGGER.warn(message)
        }

        // 심각한 성능 문제의 경우 JSON도 출력
        if (entry.isCritical) {
            val jsonOutput = formatter.formatAsJson(entry)
            LOGGER.warn("AI_PERFORMANCE_CRITICAL_JSON: {}", jsonOutput)
        }

        lastLogTime.set(System.currentTimeMillis())
    }

    /**
     * 특정 entity와 correlation ID로 로그를 필터링합니다.
     */
    override fun getLogEntries(entityId: String, correlationId: String?): List<LogEntry> {
        val entityLogs = logEntriesByEntity[entityId] ?: return emptyList()

        return if (correlationId != null) {
            entityLogs.filter { it.correlationId == correlationId }
        } else {
            entityLogs.toList()
        }
    }

    /**
     * 로깅 시스템의 현재 상태를 반환합니다.
     */
    override fun getLoggingStatus(): LoggingStatus {
        val currentSize = logEntries.size
        val bufferUtilization = currentSize.toDouble() / BUFFER_SIZE

        return LoggingStatus(
            isEnabled = true,
            currentLogLevel = if (isDevMode()) "DEBUG" else "INFO",
            totalEntriesLogged = entriesLogged.get(),
            lastLogTime = if (lastLogTime.get() > 0) Instant.ofEpochMilli(lastLogTime.get()) else null,
            bufferSize = BUFFER_SIZE,
            bufferUtilization = bufferUtilization.coerceAtMost(1.0)
        )
    }

    /**
     * 로그 엔트리를 내부 저장소에 추가합니다.
     */
    private fun addLogEntry(entry: LogEntry) {
        // 전체 로그 큐에 추가
        logEntries.offer(entry)

        // 엔티티별 로그에 추가
        logEntriesByEntity.computeIfAbsent(entry.entityId) { mutableListOf() }.add(entry)

        // 메모리 사용량 제한
        while (logEntries.size > MAX_LOG_ENTRIES) {
            val removed = logEntries.poll()
            removed?.let {
                logEntriesByEntity[it.entityId]?.remove(it)
            }
        }

        // 엔티티별 로그도 제한
        logEntriesByEntity.values.forEach { entityLogs ->
            while (entityLogs.size > MAX_LOG_ENTRIES / 10) { // 엔티티당 최대 100개
                entityLogs.removeFirstOrNull()
            }
        }
    }

    /**
     * 개발 모드 여부를 확인합니다.
     */
    private fun isDevMode(): Boolean {
        return System.getProperty("boss_ai.dev", "false").toBoolean() ||
               System.getProperty("boss_ai.debug.ai", "false").toBoolean()
    }
}

/**
 * AIDecisionLogger 인터페이스
 *
 * AI 의사결정 로깅을 위한 메인 인터페이스입니다.
 */
interface AIDecisionLogger {
    fun logTacticDecision(
        entityId: String,
        correlationId: String,
        aiMode: String,
        previousTactic: Tactic,
        selectedTactic: Tactic,
        features: FloatArray,
        tacticEvaluations: List<TacticEvaluation>,
        performanceMetrics: PerformanceMetrics,
        reasoning: String
    )

    fun logModeFallback(
        entityId: String,
        correlationId: String,
        fromMode: String,
        toMode: String,
        reason: String,
        errorDetails: String? = null
    )

    fun logPerformanceWarning(
        entityId: String,
        correlationId: String,
        operation: String,
        durationMs: Double,
        thresholdMs: Double,
        impact: String,
        suggestedAction: String
    )

    fun getLogEntries(entityId: String, correlationId: String? = null): List<LogEntry>

    fun getLoggingStatus(): LoggingStatus
}
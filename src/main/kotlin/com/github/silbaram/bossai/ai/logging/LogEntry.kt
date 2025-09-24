package com.github.silbaram.bossai.ai.logging

import com.github.silbaram.bossai.ai.Tactic
import java.time.Instant

/**
 * 로그 엔트리의 기본 sealed 클래스
 *
 * 모든 로그 엔트리의 공통 속성을 정의하고,
 * 타입 안전성을 보장하는 sealed 클래스 계층 구조의 루트입니다.
 */
sealed class LogEntry(
    /** 로그 생성 시간 */
    open val timestamp: Instant,
    /** 엔티티 고유 식별자 */
    entityId: String?,
    /** 상관관계 ID */
    correlationId: String?,
    /** 이벤트 타입 */
    eventType: String?
) {
    /** Null-safe 엔티티 ID */
    open val entityId: String = entityId ?: "emergency_fallback_entity_${System.currentTimeMillis()}"

    /** Null-safe 상관관계 ID */
    open val correlationId: String = correlationId ?: "emergency_fallback_correlation_${System.currentTimeMillis()}"

    /** Null-safe 이벤트 타입 */
    val eventType: String = eventType ?: "UNKNOWN_EVENT"

    init {
        require(this.entityId.isNotBlank()) { "Entity ID cannot be blank after null safety: '${this.entityId}'" }
        require(this.correlationId.isNotBlank()) { "Correlation ID cannot be blank after null safety: '${this.correlationId}'" }
        require(this.eventType.isNotBlank()) { "Event type cannot be blank after null safety: '${this.eventType}'" }
    }

    /**
     * 로그 엔트리의 고유 식별자를 생성합니다.
     */
    val id: String
        get() = "${eventType}_${entityId}_${timestamp.epochSecond}"

    /**
     * 이 로그 엔트리가 특정 시간 범위 내에 있는지 확인합니다.
     */
    fun isWithinTimeRange(startTime: Instant, endTime: Instant): Boolean {
        return timestamp.isAfter(startTime) && timestamp.isBefore(endTime)
    }

    /**
     * 이 로그 엔트리가 성능 관련 항목인지 확인합니다.
     */
    open val isPerformanceRelated: Boolean = false

    /**
     * 이 로그 엔트리가 오류 관련 항목인지 확인합니다.
     */
    open val isErrorRelated: Boolean = false
}

/**
 * 전술 결정 로그 엔트리
 *
 * AI 보스의 전술 선택 과정을 기록합니다.
 */
data class TacticDecisionEntry(
    override val timestamp: Instant,
    private val entityIdParam: String?,
    private val correlationIdParam: String?,
    /** 사용된 AI 모드 */
    val aiMode: String,
    /** 결정 사이클 번호 */
    val cycleNumber: Long,
    /** 이전 전술 */
    val previousTactic: Tactic,
    /** 선택된 전술 */
    val selectedTactic: Tactic,
    /** 의사결정에 사용된 기능 벡터 */
    val features: FloatArray,
    /** 각 전술의 평가 결과 */
    val tacticEvaluations: List<TacticEvaluation>,
    /** 성능 측정 데이터 */
    val performanceMetrics: PerformanceMetrics,
    /** 선택 이유 */
    val reasoning: String
) : LogEntry(timestamp, entityIdParam, correlationIdParam, "TACTIC_DECISION") {

    /** 전술이 실제로 변경되었는지 확인 */
    val isTacticChanged: Boolean
        get() = previousTactic != selectedTactic

    /** ML 모드에서 생성된 결정인지 확인 */
    val isMLDecision: Boolean
        get() = aiMode == "MACHINE_LEARNING"

    /** 휴리스틱 모드에서 생성된 결정인지 확인 */
    val isHeuristicDecision: Boolean
        get() = aiMode == "RULE_BASED_HEURISTICS"

    /** 성능 관련 로그인지 확인 (성능 경고가 필요한 경우) */
    override val isPerformanceRelated: Boolean
        get() = performanceMetrics.requiresPerformanceWarning

    /** 선택된 전술의 평가 결과 반환 */
    val selectedTacticEvaluation: TacticEvaluation?
        get() = tacticEvaluations.find { it.selected }

    /** 사용 가능한 전술들의 목록 */
    val availableTactics: List<TacticEvaluation>
        get() = tacticEvaluations.filter { it.available }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TacticDecisionEntry

        if (timestamp != other.timestamp) return false
        if (entityId != other.entityId) return false
        if (correlationId != other.correlationId) return false
        if (aiMode != other.aiMode) return false
        if (cycleNumber != other.cycleNumber) return false
        if (previousTactic != other.previousTactic) return false
        if (selectedTactic != other.selectedTactic) return false
        if (!features.contentEquals(other.features)) return false
        if (tacticEvaluations != other.tacticEvaluations) return false
        if (performanceMetrics != other.performanceMetrics) return false
        if (reasoning != other.reasoning) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + entityId.hashCode()
        result = 31 * result + correlationId.hashCode()
        result = 31 * result + aiMode.hashCode()
        result = 31 * result + cycleNumber.hashCode()
        result = 31 * result + previousTactic.hashCode()
        result = 31 * result + selectedTactic.hashCode()
        result = 31 * result + features.contentHashCode()
        result = 31 * result + tacticEvaluations.hashCode()
        result = 31 * result + performanceMetrics.hashCode()
        result = 31 * result + reasoning.hashCode()
        return result
    }
}

/**
 * 모드 폴백 로그 엔트리
 *
 * AI 모드 간 전환(주로 ML에서 휴리스틱으로의 폴백)을 기록합니다.
 */
data class ModeFallbackEntry(
    override val timestamp: Instant,
    private val entityIdParam: String?,
    private val correlationIdParam: String?,
    /** 원래 모드 */
    val fromMode: String,
    /** 전환된 모드 */
    val toMode: String,
    /** 폴백 이유 */
    val reason: String,
    /** 오류 세부사항 (선택적) */
    val errorDetails: String?
) : LogEntry(timestamp, entityIdParam, correlationIdParam, "MODE_FALLBACK") {

    /** ML에서 휴리스틱으로의 폴백인지 확인 */
    val isMLToHeuristicFallback: Boolean
        get() = fromMode == "MACHINE_LEARNING" && toMode == "RULE_BASED_HEURISTICS"

    /** 휴리스틱에서 ML로의 복원인지 확인 */
    val isHeuristicToMLRestore: Boolean
        get() = fromMode == "RULE_BASED_HEURISTICS" && toMode == "MACHINE_LEARNING"

    /** 오류 관련 폴백인지 확인 */
    override val isErrorRelated: Boolean
        get() = errorDetails != null || reason.lowercase().contains("error") || reason.lowercase().contains("exception")

    /** 성능 관련 폴백인지 확인 */
    override val isPerformanceRelated: Boolean
        get() = reason.lowercase().contains("performance") || reason.lowercase().contains("timeout")
}

/**
 * 성능 경고 로그 엔트리
 *
 * AI 시스템의 성능 문제를 기록합니다.
 */
data class PerformanceWarningEntry(
    override val timestamp: Instant,
    private val entityIdParam: String?,
    private val correlationIdParam: String?,
    /** 수행된 작업 */
    val operation: String,
    /** 실제 소요 시간 (밀리초) */
    val durationMs: Double,
    /** 임계값 (밀리초) */
    val thresholdMs: Double,
    /** 성능 문제의 영향 */
    val impact: String,
    /** 권장 조치 */
    val suggestedAction: String
) : LogEntry(timestamp, entityIdParam, correlationIdParam, "PERFORMANCE_WARNING") {

    init {
        require(durationMs > thresholdMs) { "Duration must exceed threshold for performance warning" }
        require(operation.isNotBlank()) { "Operation cannot be blank" }
        require(impact.isNotBlank()) { "Impact cannot be blank" }
        require(suggestedAction.isNotBlank()) { "Suggested action cannot be blank" }
    }

    /** 임계값 초과 정도 (배수) */
    val thresholdExceedMultiplier: Double
        get() = durationMs / thresholdMs

    /** 심각한 성능 문제인지 확인 (임계값의 2배 이상) */
    val isCritical: Boolean
        get() = thresholdExceedMultiplier >= 2.0

    /** 성능 관련 로그임을 명시 */
    override val isPerformanceRelated: Boolean = true

    /** 심각한 성능 문제의 경우 오류 관련으로도 분류 */
    override val isErrorRelated: Boolean
        get() = isCritical
}

/**
 * 로깅 시스템 상태
 *
 * 로깅 시스템의 현재 상태를 나타냅니다.
 */
data class LoggingStatus(
    /** 로깅 시스템 활성화 여부 */
    val isEnabled: Boolean,
    /** 현재 로그 레벨 */
    val currentLogLevel: String,
    /** 총 로그된 엔트리 수 */
    val totalEntriesLogged: Long,
    /** 마지막 로그 시간 */
    val lastLogTime: Instant?,
    /** 버퍼 크기 */
    val bufferSize: Int,
    /** 버퍼 사용률 (0.0 - 1.0) */
    val bufferUtilization: Double
) {
    init {
        require(bufferUtilization in 0.0..1.0) { "Buffer utilization must be between 0.0 and 1.0" }
        require(bufferSize >= 0) { "Buffer size must be non-negative" }
        require(totalEntriesLogged >= 0) { "Total entries logged must be non-negative" }
    }

    /** 버퍼가 거의 가득 찬 상태인지 확인 (80% 이상) */
    val isBufferNearFull: Boolean
        get() = bufferUtilization >= 0.8

    /** 버퍼가 임계 상태인지 확인 (95% 이상) */
    val isBufferCritical: Boolean
        get() = bufferUtilization >= 0.95

    /** 최근에 로그가 생성되었는지 확인 (1분 이내) */
    val isRecentlyActive: Boolean
        get() = lastLogTime?.let {
            Instant.now().epochSecond - it.epochSecond < 60
        } ?: false
}
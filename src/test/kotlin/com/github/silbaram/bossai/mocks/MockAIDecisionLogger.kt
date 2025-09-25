package com.github.silbaram.bossai.mocks

import com.github.silbaram.bossai.ai.Tactic
import com.github.silbaram.bossai.ai.logging.*

/**
 * Mock implementation of AIDecisionLogger for testing
 */
class MockAIDecisionLogger : AIDecisionLogger {
    private val tacticDecisionLogs = mutableListOf<TacticDecisionCall>()
    private val modeFallbackLogs = mutableListOf<ModeFallbackCall>()
    private val performanceWarningLogs = mutableListOf<PerformanceWarningCall>()

    data class TacticDecisionCall(
        val entityId: String,
        val correlationId: String,
        val aiMode: String,
        val previousTactic: Tactic,
        val selectedTactic: Tactic,
        val features: FloatArray,
        val tacticEvaluations: List<TacticEvaluation>,
        val performanceMetrics: PerformanceMetrics,
        val reasoning: String
    )

    data class ModeFallbackCall(
        val entityId: String,
        val correlationId: String,
        val fromMode: String,
        val toMode: String,
        val reason: String
    )

    data class PerformanceWarningCall(
        val entityId: String,
        val correlationId: String,
        val operation: String,
        val durationMs: Double,
        val thresholdMs: Double,
        val impact: String,
        val suggestedAction: String
    )

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
        // Validation
        require(entityId.isNotBlank()) { "Entity ID cannot be blank" }
        require(correlationId.isNotBlank()) { "Correlation ID cannot be blank" }
        require(aiMode.isNotBlank()) { "AI mode cannot be blank" }
        require(features.isNotEmpty()) { "Features array cannot be empty" }
        require(tacticEvaluations.isNotEmpty()) { "Tactic evaluations cannot be empty" }
        require(reasoning.isNotBlank()) { "Reasoning cannot be blank" }

        tacticDecisionLogs.add(
            TacticDecisionCall(
                entityId, correlationId, aiMode, previousTactic, selectedTactic,
                features, tacticEvaluations, performanceMetrics, reasoning
            )
        )
    }

    override fun logModeFallback(
        entityId: String,
        correlationId: String,
        fromMode: String,
        toMode: String,
        reason: String,
        errorDetails: String?
    ) {
        // Validation
        require(entityId.isNotBlank()) { "Entity ID cannot be blank" }
        require(correlationId.isNotBlank()) { "Correlation ID cannot be blank" }
        require(fromMode.isNotBlank()) { "From mode cannot be blank" }
        require(toMode.isNotBlank()) { "To mode cannot be blank" }
        require(reason.isNotBlank()) { "Reason cannot be blank" }
        require(fromMode != toMode) { "From mode and to mode must be different" }

        modeFallbackLogs.add(ModeFallbackCall(entityId, correlationId, fromMode, toMode, reason))
    }

    override fun logPerformanceWarning(
        entityId: String,
        correlationId: String,
        operation: String,
        durationMs: Double,
        thresholdMs: Double,
        impact: String,
        suggestedAction: String
    ) {
        // Validation
        require(entityId.isNotBlank()) { "Entity ID cannot be blank" }
        require(correlationId.isNotBlank()) { "Correlation ID cannot be blank" }
        require(operation.isNotBlank()) { "Operation cannot be blank" }
        require(durationMs > 0) { "Duration must be positive" }
        require(thresholdMs > 0) { "Threshold must be positive" }
        require(durationMs > thresholdMs) { "Duration must exceed threshold for performance warning" }
        require(impact.isNotBlank()) { "Impact cannot be blank" }
        require(suggestedAction.isNotBlank()) { "Suggested action cannot be blank" }

        performanceWarningLogs.add(
            PerformanceWarningCall(entityId, correlationId, operation, durationMs, thresholdMs, impact, suggestedAction)
        )
    }

    // Test helper methods
    fun getTacticDecisionLogs(): List<TacticDecisionCall> = tacticDecisionLogs.toList()
    fun getModeFallbackLogs(): List<ModeFallbackCall> = modeFallbackLogs.toList()
    fun getPerformanceWarningLogs(): List<PerformanceWarningCall> = performanceWarningLogs.toList()
    fun clear() {
        tacticDecisionLogs.clear()
        modeFallbackLogs.clear()
        performanceWarningLogs.clear()
    }

    // Implement missing interface methods
    override fun getLogEntries(entityId: String, correlationId: String?): List<LogEntry> = emptyList()
    override fun getLogEntriesForEntity(entityId: String, limit: Int): List<LogEntry> = emptyList()
    override fun getAllLogEntries(limit: Int): List<LogEntry> = emptyList()
    override fun clearLogEntries(olderThan: java.time.Instant?) {}
    override fun getLoggingStatus(): LoggingStatus =
        LoggingStatus(
            isEnabled = true,
            currentLogLevel = "INFO",
            totalEntriesLogged = 0L,
            lastLogTime = java.time.Instant.now(),
            bufferSize = 100,
            bufferUtilization = 0.0
        )
}
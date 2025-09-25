package com.github.silbaram.bossai.ai.logging

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.time.format.DateTimeFormatter

/**
 * LogFormatter 인터페이스의 기본 구현체
 *
 * JSON 직렬화와 사람이 읽기 쉬운 형식으로 로그 엔트리를 포맷팅합니다.
 * 다국어 지원(영어/한국어)을 제공합니다.
 */
class LogFormatterImpl : LogFormatter {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val dateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    /**
     * 로그 엔트리를 JSON 형식으로 포맷팅합니다.
     *
     * @param entry 로그 엔트리
     * @return JSON 형식의 문자열
     */
    override fun formatAsJson(entry: LogEntry): String {
        return when (entry) {
            is TacticDecisionEntry -> formatTacticDecisionAsJson(entry)
            is ModeFallbackEntry -> formatModeFallbackAsJson(entry)
            is PerformanceWarningEntry -> formatPerformanceWarningAsJson(entry)
        }
    }

    /**
     * 로그 엔트리를 사람이 읽기 쉬운 형식으로 포맷팅합니다.
     *
     * @param entry 로그 엔트리
     * @param locale 언어 설정 ("en" or "ko")
     * @return 포맷된 문자열
     */
    override fun formatAsHumanReadable(entry: LogEntry, locale: String): String {
        return when (entry) {
            is TacticDecisionEntry -> formatTacticDecisionAsHumanReadable(entry, locale)
            is ModeFallbackEntry -> formatModeFallbackAsHumanReadable(entry, locale)
            is PerformanceWarningEntry -> formatPerformanceWarningAsHumanReadable(entry, locale)
        }
    }

    private fun formatTacticDecisionAsJson(entry: TacticDecisionEntry): String {
        val jsonEntry = TacticDecisionJsonEntry(
            event_type = entry.eventType,
            timestamp = dateTimeFormatter.format(entry.timestamp),
            entity_id = entry.entityId,
            correlation_id = entry.correlationId,
            ai_mode = entry.aiMode,
            decision_data = DecisionDataJson(
                cycle_number = entry.cycleNumber,
                previous_tactic = entry.previousTactic.name,
                selected_tactic = entry.selectedTactic.name,
                feature_vector = entry.features.toList(),
                reasoning = entry.reasoning
            ),
            tactic_evaluations = entry.tacticEvaluations.map { eval ->
                TacticEvaluationJson(
                    tactic = eval.tactic.name,
                    available = eval.available,
                    confidence = eval.confidence,
                    heuristic_score = eval.heuristicScore,
                    cooldown_remaining = eval.cooldownRemaining,
                    selected = eval.selected,
                    rationale = eval.rationale
                )
            },
            performance = PerformanceJson(
                total_time_ms = entry.performanceMetrics.totalTimeMs,
                feature_extraction_time_ms = entry.performanceMetrics.featureExtractionTimeNanos / 1_000_000.0,
                model_inference_time_ms = entry.performanceMetrics.modelInferenceTimeNanos?.let { it / 1_000_000.0 },
                heuristic_calculation_time_ms = entry.performanceMetrics.heuristicCalculationTimeNanos?.let { it / 1_000_000.0 },
                tactic_application_time_ms = entry.performanceMetrics.tacticApplicationTimeNanos / 1_000_000.0,
                memory_delta_mb = entry.performanceMetrics.memoryDeltaMB
            )
        )
        return json.encodeToString(jsonEntry)
    }

    private fun formatModeFallbackAsJson(entry: ModeFallbackEntry): String {
        val jsonEntry = ModeFallbackJsonEntry(
            event_type = entry.eventType,
            timestamp = dateTimeFormatter.format(entry.timestamp),
            entity_id = entry.entityId,
            correlation_id = entry.correlationId,
            fallback_data = FallbackDataJson(
                from_mode = entry.fromMode,
                to_mode = entry.toMode,
                reason = entry.reason,
                error_details = entry.errorDetails
            )
        )
        return json.encodeToString(jsonEntry)
    }

    private fun formatPerformanceWarningAsJson(entry: PerformanceWarningEntry): String {
        val jsonEntry = PerformanceWarningJsonEntry(
            event_type = entry.eventType,
            timestamp = dateTimeFormatter.format(entry.timestamp),
            entity_id = entry.entityId,
            correlation_id = entry.correlationId,
            performance_data = PerformanceWarningDataJson(
                operation = entry.operation,
                duration_ms = entry.durationMs,
                threshold_ms = entry.thresholdMs,
                impact = entry.impact,
                suggested_action = entry.suggestedAction
            )
        )
        return json.encodeToString(jsonEntry)
    }


    private fun formatTacticDecisionAsHumanReadable(entry: TacticDecisionEntry, locale: String): String {
        return when (locale.lowercase()) {
            "ko" -> {
                val modeText = when (entry.aiMode) {
                    "MACHINE_LEARNING" -> "🤖ML"
                    "RULE_BASED_HEURISTICS" -> "🧠규칙"
                    else -> entry.aiMode
                }
                val hpPercent = String.format("%.1f", (entry.features.getOrNull(0)?.times(100) ?: 0.0))
                val distance = String.format("%.1f", (entry.features.getOrNull(1) ?: 0.0))
                val duration = String.format("%.1f", entry.performanceMetrics.totalTimeMs)

                val tacticEmoji = when (entry.selectedTactic.name) {
                    "IDLE" -> "😴"
                    "BURST_AOE" -> "💥"
                    "KITE" -> "🏃"
                    "SUMMON" -> "👥"
                    else -> "⚔️"
                }

                "🎯 센티넬 보스 | $tacticEmoji ${entry.previousTactic.name} → ${entry.selectedTactic.name} | " +
                        "❤️ $hpPercent% | 📏 ${distance}m | $modeText | ⏱️ ${duration}ms"
            }
            else -> { // "en" or default
                val modeText = when (entry.aiMode) {
                    "MACHINE_LEARNING" -> "🤖ML"
                    "RULE_BASED_HEURISTICS" -> "🧠Rules"
                    else -> entry.aiMode
                }
                val hpPercent = String.format("%.1f", (entry.features.getOrNull(0)?.times(100) ?: 0.0))
                val distance = String.format("%.1f", (entry.features.getOrNull(1) ?: 0.0))
                val duration = String.format("%.1f", entry.performanceMetrics.totalTimeMs)

                val tacticEmoji = when (entry.selectedTactic.name) {
                    "IDLE" -> "😴"
                    "BURST_AOE" -> "💥"
                    "KITE" -> "🏃"
                    "SUMMON" -> "👥"
                    else -> "⚔️"
                }

                "🎯 Sentinel Boss | $tacticEmoji ${entry.previousTactic.name} → ${entry.selectedTactic.name} | " +
                        "❤️ $hpPercent% | 📏 ${distance}m | $modeText | ⏱️ ${duration}ms"
            }
        }
    }

    private fun formatModeFallbackAsHumanReadable(entry: ModeFallbackEntry, locale: String): String {
        return when (locale.lowercase()) {
            "ko" -> {
                "⚠️ AI 모드 변경: ${entry.fromMode} → ${entry.toMode} | 이유: ${entry.reason}"
            }
            else -> { // "en" or default
                "⚠️ AI Mode Fallback: ${entry.fromMode} → ${entry.toMode} | Reason: ${entry.reason}"
            }
        }
    }

    private fun formatPerformanceWarningAsHumanReadable(entry: PerformanceWarningEntry, locale: String): String {
        return when (locale.lowercase()) {
            "ko" -> {
                "⚡ 성능 경고: ${entry.operation} | " +
                        "⏱️ ${String.format("%.1f", entry.durationMs)}ms (한계: ${String.format("%.1f", entry.thresholdMs)}ms) | " +
                        "영향: ${entry.impact}"
            }
            else -> { // "en" or default
                "⚡ Performance Warning: ${entry.operation} | " +
                        "⏱️ ${String.format("%.1f", entry.durationMs)}ms (threshold: ${String.format("%.1f", entry.thresholdMs)}ms) | " +
                        "Impact: ${entry.impact}"
            }
        }
    }

}

// JSON 직렬화를 위한 데이터 클래스들
@Serializable
private data class TacticDecisionJsonEntry(
    val event_type: String,
    val timestamp: String,
    val entity_id: String,
    val correlation_id: String,
    val ai_mode: String,
    val decision_data: DecisionDataJson,
    val tactic_evaluations: List<TacticEvaluationJson>,
    val performance: PerformanceJson
)

@Serializable
private data class DecisionDataJson(
    val cycle_number: Long,
    val previous_tactic: String,
    val selected_tactic: String,
    val feature_vector: List<Float>,
    val reasoning: String
)

@Serializable
private data class TacticEvaluationJson(
    val tactic: String,
    val available: Boolean,
    val confidence: Double?,
    val heuristic_score: Double?,
    val cooldown_remaining: Int,
    val selected: Boolean,
    val rationale: String
)

@Serializable
private data class PerformanceJson(
    val total_time_ms: Double,
    val feature_extraction_time_ms: Double,
    val model_inference_time_ms: Double?,
    val heuristic_calculation_time_ms: Double?,
    val tactic_application_time_ms: Double,
    val memory_delta_mb: Double
)

@Serializable
private data class ModeFallbackJsonEntry(
    val event_type: String,
    val timestamp: String,
    val entity_id: String,
    val correlation_id: String,
    val fallback_data: FallbackDataJson
)

@Serializable
private data class FallbackDataJson(
    val from_mode: String,
    val to_mode: String,
    val reason: String,
    val error_details: String?
)

@Serializable
private data class PerformanceWarningJsonEntry(
    val event_type: String,
    val timestamp: String,
    val entity_id: String,
    val correlation_id: String,
    val performance_data: PerformanceWarningDataJson
)

@Serializable
private data class PerformanceWarningDataJson(
    val operation: String,
    val duration_ms: Double,
    val threshold_ms: Double,
    val impact: String,
    val suggested_action: String
)


/**
 * LogFormatter 인터페이스
 */
interface LogFormatter {
    fun formatAsJson(entry: LogEntry): String
    fun formatAsHumanReadable(entry: LogEntry, locale: String = "en"): String
}
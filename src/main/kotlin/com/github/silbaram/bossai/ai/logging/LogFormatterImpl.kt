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

    private val localeDictionary = LocaleDictionary(LoggingConfig.loadConfig().defaultLocale)
    private val tacticDecisionHumanFormatter = TacticDecisionHumanReadableFormatter(localeDictionary)
    private val modeFallbackHumanFormatter = ModeFallbackHumanReadableFormatter(localeDictionary)
    private val performanceWarningHumanFormatter = PerformanceWarningHumanReadableFormatter(localeDictionary)

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
        val normalizedLocale = locale.lowercase()
        return when (entry) {
            is TacticDecisionEntry -> tacticDecisionHumanFormatter.format(entry, normalizedLocale)
            is ModeFallbackEntry -> modeFallbackHumanFormatter.format(entry, normalizedLocale)
            is PerformanceWarningEntry -> performanceWarningHumanFormatter.format(entry, normalizedLocale)
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

private interface HumanReadableEntryFormatter<in T : LogEntry> {
    fun format(entry: T, locale: String): String
}

private class TacticDecisionHumanReadableFormatter(
    private val dictionary: LocaleDictionary
) : HumanReadableEntryFormatter<TacticDecisionEntry> {

    private val tacticEmojis = mapOf(
        "IDLE" to "😴",
        "BURST_AOE" to "💥",
        "KITE" to "🏃",
        "SUMMON" to "👥"
    )

    override fun format(entry: TacticDecisionEntry, locale: String): String {
        val bossLabel = dictionary.bossLabel(locale)
        val emoji = tacticEmojis[entry.selectedTactic.name] ?: "⚔️"
        val tacticLabel = dictionary.tacticLabel(entry.selectedTactic.name, locale)
        val modeText = dictionary.modeLabel(entry.aiMode, locale)
        val hpPercent = formatNumber(entry.features.getOrNull(0)?.times(100f)?.toDouble() ?: 0.0)
        val distance = formatNumber(entry.features.getOrNull(1)?.toDouble() ?: 0.0)
        val duration = formatNumber(entry.performanceMetrics.totalTimeMs)

        return "$bossLabel | $emoji[$tacticLabel] ${entry.previousTactic.name} → ${entry.selectedTactic.name} | " +
                "❤️ $hpPercent% | 📏 ${distance}m | $modeText | ⏱️ ${duration}ms"
    }

    private fun formatNumber(value: Double): String = String.format("%.1f", value)
}

private class ModeFallbackHumanReadableFormatter(
    private val dictionary: LocaleDictionary
) : HumanReadableEntryFormatter<ModeFallbackEntry> {

    override fun format(entry: ModeFallbackEntry, locale: String): String {
        val prefix = dictionary.modeFallbackPrefix(locale)
        val reasonLabel = dictionary.modeFallbackReasonLabel(locale)
        return "$prefix: ${entry.fromMode} → ${entry.toMode} | $reasonLabel: ${entry.reason}"
    }
}

private class PerformanceWarningHumanReadableFormatter(
    private val dictionary: LocaleDictionary
) : HumanReadableEntryFormatter<PerformanceWarningEntry> {

    override fun format(entry: PerformanceWarningEntry, locale: String): String {
        val prefix = dictionary.performanceWarningPrefix(locale)
        val thresholdLabel = dictionary.performanceWarningThresholdLabel(locale)
        val impactLabel = dictionary.performanceWarningImpactLabel(locale)
        val duration = formatNumber(entry.durationMs)
        val threshold = formatNumber(entry.thresholdMs)

        return "$prefix: ${entry.operation} | ⏱️ ${duration}ms ($thresholdLabel: ${threshold}ms) | $impactLabel: ${entry.impact}"
    }

    private fun formatNumber(value: Double): String = String.format("%.1f", value)
}

private class LocaleDictionary(private val defaultLocale: String) {

    private val localeStrings: Map<String, LocaleStrings> = mapOf(
        "en" to LocaleStrings(
            bossLabel = "🎯 Sentinel Boss",
            machineLearningLabel = "🤖ML",
            heuristicLabel = "🧠Rules",
            tacticLabels = mapOf(
                "IDLE" to "Idle",
                "BURST_AOE" to "Burst AoE",
                "KITE" to "Kite",
                "SUMMON" to "Summon"
            ),
            modeFallbackPrefix = "⚠️ AI Mode Fallback",
            modeFallbackReasonLabel = "Reason",
            performanceWarningPrefix = "⚡ Performance Warning",
            performanceWarningThresholdLabel = "threshold",
            performanceWarningImpactLabel = "Impact"
        ),
        "ko" to LocaleStrings(
            bossLabel = "🎯 센티넬 보스",
            machineLearningLabel = "🤖ML",
            heuristicLabel = "🧠규칙",
            tacticLabels = mapOf(
                "IDLE" to "대기",
                "BURST_AOE" to "광역폭발",
                "KITE" to "카이팅",
                "SUMMON" to "소환"
            ),
            modeFallbackPrefix = "⚠️ AI 모드 변경",
            modeFallbackReasonLabel = "이유",
            performanceWarningPrefix = "⚡ 성능 경고",
            performanceWarningThresholdLabel = "한계",
            performanceWarningImpactLabel = "영향"
        )
    )

    fun bossLabel(locale: String): String = stringsFor(locale).bossLabel

    fun modeLabel(aiMode: String, locale: String): String {
        val strings = stringsFor(locale)
        return when (aiMode) {
            "MACHINE_LEARNING" -> strings.machineLearningLabel
            "RULE_BASED_HEURISTICS" -> strings.heuristicLabel
            else -> aiMode
        }
    }

    fun tacticLabel(tactic: String, locale: String): String = stringsFor(locale).tacticLabels[tactic] ?: tactic

    fun modeFallbackPrefix(locale: String): String = stringsFor(locale).modeFallbackPrefix

    fun modeFallbackReasonLabel(locale: String): String = stringsFor(locale).modeFallbackReasonLabel

    fun performanceWarningPrefix(locale: String): String = stringsFor(locale).performanceWarningPrefix

    fun performanceWarningThresholdLabel(locale: String): String = stringsFor(locale).performanceWarningThresholdLabel

    fun performanceWarningImpactLabel(locale: String): String = stringsFor(locale).performanceWarningImpactLabel

    private fun stringsFor(locale: String): LocaleStrings {
        val normalized = locale.lowercase()
        return localeStrings[normalized] ?: localeStrings[defaultLocale]!!
    }
}

private data class LocaleStrings(
    val bossLabel: String,
    val machineLearningLabel: String,
    val heuristicLabel: String,
    val tacticLabels: Map<String, String>,
    val modeFallbackPrefix: String,
    val modeFallbackReasonLabel: String,
    val performanceWarningPrefix: String,
    val performanceWarningThresholdLabel: String,
    val performanceWarningImpactLabel: String
)
package com.github.silbaram.bossai.mocks

import com.github.silbaram.bossai.ai.logging.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable

/**
 * Mock implementation of LogFormatter for testing
 */
class MockLogFormatter : LogFormatter {
    private val json = Json { prettyPrint = true }

    @Serializable
    data class MockLogEntry(
        val type: String,
        val content: String,
        val locale: String
    )

    override fun formatAsHumanReadable(entry: LogEntry, locale: String): String {
        val actualLocale = locale

        return when (entry) {
            is TacticDecisionEntry -> {
                if (actualLocale == "ko") {
                    "🎯 ${entry.entityId} | 💥 ${entry.previousTactic} → ${entry.selectedTactic} | ❤️ ${String.format("%.1f", entry.features[0] * 100)}% | 📏 ${String.format("%.1f", entry.features[1])}m | 🤖${entry.aiMode} | ⏱️ ${String.format("%.1f", entry.performanceMetrics.totalTimeMs)}ms"
                } else {
                    "🎯 ${entry.entityId} | 💥 ${entry.previousTactic} → ${entry.selectedTactic} | ❤️ ${String.format("%.1f", entry.features[0] * 100)}% | 📏 ${String.format("%.1f", entry.features[1])}m | 🤖${entry.aiMode} | ⏱️ ${String.format("%.1f", entry.performanceMetrics.totalTimeMs)}ms"
                }
            }
            is ModeFallbackEntry -> {
                if (actualLocale == "ko") {
                    "⚠️ ${entry.entityId} | 모드 전환: ${entry.fromMode} → ${entry.toMode} | 이유: ${entry.reason}"
                } else {
                    "⚠️ ${entry.entityId} | Mode fallback: ${entry.fromMode} → ${entry.toMode} | Reason: ${entry.reason}"
                }
            }
            is PerformanceWarningEntry -> {
                if (actualLocale == "ko") {
                    "🐌 ${entry.entityId} | 성능 경고: ${entry.operation} | 시간: ${String.format("%.1f", entry.durationMs)}ms > ${String.format("%.1f", entry.thresholdMs)}ms"
                } else {
                    "🐌 ${entry.entityId} | Performance warning: ${entry.operation} | Time: ${String.format("%.1f", entry.durationMs)}ms > ${String.format("%.1f", entry.thresholdMs)}ms"
                }
            }
            else -> "Unknown entry type: ${entry::class.simpleName}"
        }
    }

    override fun formatAsJson(entry: LogEntry): String {
        return try {
            when (entry) {
                is TacticDecisionEntry -> {
                    val mockEntry = MockLogEntry(
                        type = "TacticDecision",
                        content = "entityId=${entry.entityId}, tactic=${entry.previousTactic}->${entry.selectedTactic}, mode=${entry.aiMode}",
                        locale = "en"
                    )
                    json.encodeToString(mockEntry)
                }
                is ModeFallbackEntry -> {
                    val mockEntry = MockLogEntry(
                        type = "ModeFallback",
                        content = "entityId=${entry.entityId}, transition=${entry.fromMode}->${entry.toMode}",
                        locale = "en"
                    )
                    json.encodeToString(mockEntry)
                }
                is PerformanceWarningEntry -> {
                    val mockEntry = MockLogEntry(
                        type = "PerformanceWarning",
                        content = "entityId=${entry.entityId}, operation=${entry.operation}, duration=${entry.durationMs}ms",
                        locale = "en"
                    )
                    json.encodeToString(mockEntry)
                }
                else -> """{"type":"Unknown","content":"${entry::class.simpleName}","locale":"en"}"""
            }
        } catch (e: Exception) {
            """{"error":"Serialization failed: ${e.message}","type":"${entry::class.simpleName}"}"""
        }
    }
}
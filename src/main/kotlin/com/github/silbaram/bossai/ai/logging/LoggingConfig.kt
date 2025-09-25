package com.github.silbaram.bossai.ai.logging

import com.github.silbaram.bossai.ai.logging.AIDecisionLoggerImpl
import com.github.silbaram.bossai.util.RateLimitedLogger
import com.github.silbaram.bossai.util.CorrelationIdGeneratorImpl

/**
 * AI 로깅 시스템 설정 클래스
 *
 * 로깅 컴포넌트들의 초기화와 설정을 관리합니다.
 * 시스템 속성을 통해 로깅 동작을 제어할 수 있습니다.
 */
object LoggingConfig {

    /**
     * 로깅 시스템 설정
     */
    data class Config(
        /** 로깅 활성화 여부 */
        val enabled: Boolean = true,

        /** 속도 제한 간격 (밀리초) */
        val rateLimitIntervalMs: Long = 5000L,

        /** 간격당 최대 로그 수 */
        val maxLogsPerInterval: Int = 3,

        /** 개발 모드 활성화 여부 */
        val devMode: Boolean = false,

        /** JSON 로깅 활성화 여부 */
        val jsonLoggingEnabled: Boolean = false,

        /** 성능 경고 임계값 (밀리초) */
        val performanceWarningThresholdMs: Double = 50.0,

        /** 기본 로케일 */
        val defaultLocale: String = "en",

        /** 버퍼 크기 */
        val bufferSize: Int = 100,

        /** 최대 로그 엔트리 수 */
        val maxLogEntries: Int = 1000
    )

    /**
     * 시스템 속성에서 설정을 로드합니다.
     */
    fun loadConfig(): Config {
        return Config(
            enabled = System.getProperty("boss_ai.logging.enabled", "true").toBoolean(),
            rateLimitIntervalMs = System.getProperty("boss_ai.logging.rate_limit_interval", "5000").toLongOrNull() ?: 5000L,
            maxLogsPerInterval = System.getProperty("boss_ai.logging.max_logs_per_interval", "3").toIntOrNull() ?: 3,
            devMode = System.getProperty("boss_ai.dev", "false").toBoolean() ||
                     System.getProperty("boss_ai.debug.ai", "false").toBoolean(),
            jsonLoggingEnabled = System.getProperty("boss_ai.logging.json", "false").toBoolean(),
            performanceWarningThresholdMs = System.getProperty("boss_ai.logging.performance_threshold", "50.0").toDoubleOrNull() ?: 50.0,
            defaultLocale = System.getProperty("boss_ai.logging.locale", "en"),
            bufferSize = System.getProperty("boss_ai.logging.buffer_size", "100").toIntOrNull() ?: 100,
            maxLogEntries = System.getProperty("boss_ai.logging.max_entries", "1000").toIntOrNull() ?: 1000
        )
    }

    /**
     * 설정된 config에 따라 AIDecisionLogger 인스턴스를 생성합니다.
     */
    fun createAIDecisionLogger(config: Config = loadConfig()): AIDecisionLogger {
        val rateLimitedLogger = if (config.enabled) {
            RateLimitedLogger(
                rateLimitIntervalMs = config.rateLimitIntervalMs,
                maxLogsPerInterval = config.maxLogsPerInterval
            )
        } else {
            null
        }

        val formatter = LogFormatterImpl()

        return AIDecisionLoggerImpl(formatter, rateLimitedLogger)
    }

    /**
     * 설정 요약을 문자열로 반환합니다.
     */
    fun getConfigSummary(config: Config = loadConfig()): String {
        return buildString {
            appendLine("=== Boss AI Logging Configuration ===")
            appendLine("Enabled: ${config.enabled}")
            appendLine("Dev Mode: ${config.devMode}")
            appendLine("JSON Logging: ${config.jsonLoggingEnabled}")
            appendLine("Rate Limit: ${config.maxLogsPerInterval} logs per ${config.rateLimitIntervalMs}ms")
            appendLine("Performance Threshold: ${config.performanceWarningThresholdMs}ms")
            appendLine("Default Locale: ${config.defaultLocale}")
            appendLine("Buffer Size: ${config.bufferSize}")
            appendLine("Max Log Entries: ${config.maxLogEntries}")
            appendLine("=====================================")
        }
    }

    /**
     * 로깅 시스템 상태를 검증합니다.
     */
    fun validateConfiguration(config: Config = loadConfig()): List<String> {
        val issues = mutableListOf<String>()

        if (config.rateLimitIntervalMs < 1000) {
            issues.add("Rate limit interval too short (${config.rateLimitIntervalMs}ms < 1000ms)")
        }

        if (config.maxLogsPerInterval < 1) {
            issues.add("Max logs per interval too low (${config.maxLogsPerInterval} < 1)")
        }

        if (config.performanceWarningThresholdMs < 1.0) {
            issues.add("Performance warning threshold too low (${config.performanceWarningThresholdMs}ms < 1.0ms)")
        }

        if (config.bufferSize < 10) {
            issues.add("Buffer size too small (${config.bufferSize} < 10)")
        }

        if (config.maxLogEntries < 100) {
            issues.add("Max log entries too small (${config.maxLogEntries} < 100)")
        }

        if (config.defaultLocale !in listOf("en", "ko")) {
            issues.add("Unsupported default locale: ${config.defaultLocale}")
        }

        return issues
    }

    /**
     * 모든 시스템 속성을 표시합니다 (디버깅용).
     */
    fun debugSystemProperties(): String {
        val relevantProperties = System.getProperties().entries
            .filter { it.key.toString().startsWith("boss_ai.") }
            .sortedBy { it.key.toString() }

        return buildString {
            appendLine("=== Boss AI System Properties ===")
            if (relevantProperties.isEmpty()) {
                appendLine("No boss_ai.* properties found")
            } else {
                relevantProperties.forEach { (key, value) ->
                    appendLine("$key = $value")
                }
            }
            appendLine("=================================")
        }
    }

    /**
     * 기본 시스템 속성을 설정합니다 (테스트/개발용).
     */
    fun setupDefaultProperties() {
        val defaultProperties = mapOf(
            "boss_ai.logging.enabled" to "true",
            "boss_ai.logging.rate_limit_interval" to "5000",
            "boss_ai.logging.max_logs_per_interval" to "3",
            "boss_ai.logging.performance_threshold" to "50.0",
            "boss_ai.logging.locale" to "en",
            "boss_ai.logging.buffer_size" to "100",
            "boss_ai.logging.max_entries" to "1000"
        )

        defaultProperties.forEach { (key, value) ->
            if (System.getProperty(key) == null) {
                System.setProperty(key, value)
            }
        }
    }
}
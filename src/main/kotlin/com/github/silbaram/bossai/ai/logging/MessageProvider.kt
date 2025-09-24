package com.github.silbaram.bossai.ai.logging

/**
 * 다국어 메시지 제공자
 *
 * AI 로깅 시스템에서 사용되는 메시지들을 다국어로 제공합니다.
 * 현재 영어(en)와 한국어(ko)를 지원합니다.
 */
class MessageProvider {

    companion object {
        private val messages = mapOf(
            // 영어 메시지
            "en" to mapOf(
                "ai_decision" to "AI_DECISION",
                "mode_fallback" to "AI_MODE_FALLBACK",
                "performance_warning" to "AI_PERFORMANCE_WARNING",
                "switching_tactics" to "switching tactics",
                "tactic_change" to "전술 변경",
                "hp" to "HP",
                "distance" to "Distance",
                "blocks" to "blocks",
                "mode" to "Mode",
                "duration" to "Duration",
                "ms" to "ms",
                "reason" to "Reason",
                "operation" to "Operation",
                "threshold" to "threshold",
                "impact" to "Impact",
                "machine_learning" to "ML",
                "rule_based_heuristics" to "Heuristic",
                "from" to "from",
                "to" to "to",
                "tactic_idle" to "IDLE",
                "tactic_burst_aoe" to "BURST_AOE",
                "tactic_kite" to "KITE",
                "tactic_summon" to "SUMMON",
                "confidence" to "confidence",
                "cooldown" to "cooldown",
                "available" to "available",
                "selected" to "selected",
                "feature_vector" to "feature_vector",
                "reasoning" to "reasoning",
                "performance_metrics" to "performance_metrics",
                "total_time" to "total_time",
                "memory_delta" to "memory_delta",
                "error_details" to "error_details",
                "suggested_action" to "suggested_action"
            ),

            // 한국어 메시지
            "ko" to mapOf(
                "ai_decision" to "AI_결정",
                "mode_fallback" to "AI_모드_폴백",
                "performance_warning" to "AI_성능_경고",
                "switching_tactics" to "전술 변경",
                "tactic_change" to "전술 변경",
                "hp" to "체력",
                "distance" to "거리",
                "blocks" to "블록",
                "mode" to "모드",
                "duration" to "실행시간",
                "ms" to "ms",
                "reason" to "이유",
                "operation" to "작업",
                "threshold" to "임계값",
                "impact" to "영향",
                "machine_learning" to "머신러닝",
                "rule_based_heuristics" to "휴리스틱",
                "from" to "에서",
                "to" to "로",
                "tactic_idle" to "대기",
                "tactic_burst_aoe" to "폭발형_광역공격",
                "tactic_kite" to "기동_회피",
                "tactic_summon" to "소환",
                "confidence" to "신뢰도",
                "cooldown" to "쿨다운",
                "available" to "사용가능",
                "selected" to "선택됨",
                "feature_vector" to "기능_벡터",
                "reasoning" to "추론_과정",
                "performance_metrics" to "성능_메트릭",
                "total_time" to "총_시간",
                "memory_delta" to "메모리_변화",
                "error_details" to "오류_세부사항",
                "suggested_action" to "권장_조치"
            )
        )

        // 기본 로케일
        private const val DEFAULT_LOCALE = "en"
    }

    /**
     * 지정된 로케일의 메시지를 가져옵니다.
     *
     * @param key 메시지 키
     * @param locale 로케일 ("en" 또는 "ko")
     * @return 해당 로케일의 메시지, 없으면 키 자체 반환
     */
    fun getMessage(key: String, locale: String = DEFAULT_LOCALE): String {
        val normalizedLocale = locale.lowercase()
        return messages[normalizedLocale]?.get(key)
            ?: messages[DEFAULT_LOCALE]?.get(key)
            ?: key
    }

    /**
     * AI 모드를 로케일에 맞게 변환합니다.
     *
     * @param aiMode AI 모드 ("MACHINE_LEARNING" 또는 "RULE_BASED_HEURISTICS")
     * @param locale 로케일
     * @return 로케일에 맞는 AI 모드 표시 문자열
     */
    fun getAIModeDisplayName(aiMode: String, locale: String = DEFAULT_LOCALE): String {
        return when (aiMode.uppercase()) {
            "MACHINE_LEARNING" -> getMessage("machine_learning", locale)
            "RULE_BASED_HEURISTICS" -> getMessage("rule_based_heuristics", locale)
            else -> aiMode
        }
    }

    /**
     * 전술을 로케일에 맞게 변환합니다.
     *
     * @param tacticName 전술 이름 ("IDLE", "BURST_AOE", "KITE", "SUMMON")
     * @param locale 로케일
     * @return 로케일에 맞는 전술 표시 문자열
     */
    fun getTacticDisplayName(tacticName: String, locale: String = DEFAULT_LOCALE): String {
        val key = "tactic_${tacticName.lowercase()}"
        return getMessage(key, locale)
    }

    /**
     * 로그 레벨에 따른 접두사를 가져옵니다.
     *
     * @param level 로그 레벨 ("INFO", "WARN", "ERROR", "DEBUG")
     * @param locale 로케일
     * @return 로케일에 맞는 로그 레벨 접두사
     */
    fun getLogLevelPrefix(level: String, locale: String = DEFAULT_LOCALE): String {
        return when (locale.lowercase()) {
            "ko" -> when (level.uppercase()) {
                "INFO" -> "[정보]"
                "WARN" -> "[경고]"
                "ERROR" -> "[오류]"
                "DEBUG" -> "[디버그]"
                else -> "[$level]"
            }
            else -> "[$level]" // 영어는 기본 형식 사용
        }
    }

    /**
     * 포맷된 전술 변경 메시지를 생성합니다.
     *
     * @param entityId 엔티티 ID
     * @param previousTactic 이전 전술
     * @param selectedTactic 선택된 전술
     * @param locale 로케일
     * @return 포맷된 메시지
     */
    fun formatTacticChangeMessage(
        entityId: String,
        previousTactic: String,
        selectedTactic: String,
        locale: String = DEFAULT_LOCALE
    ): String {
        val prevTacticDisplay = getTacticDisplayName(previousTactic, locale)
        val selectedTacticDisplay = getTacticDisplayName(selectedTactic, locale)
        val switchingText = getMessage("switching_tactics", locale)

        return when (locale.lowercase()) {
            "ko" -> "$entityId $switchingText: $prevTacticDisplay -> $selectedTacticDisplay"
            else -> "$entityId $switchingText: $prevTacticDisplay -> $selectedTacticDisplay"
        }
    }

    /**
     * 포맷된 모드 폴백 메시지를 생성합니다.
     *
     * @param entityId 엔티티 ID
     * @param fromMode 원래 모드
     * @param toMode 전환된 모드
     * @param reason 이유
     * @param locale 로케일
     * @return 포맷된 메시지
     */
    fun formatModeFallbackMessage(
        entityId: String,
        fromMode: String,
        toMode: String,
        reason: String,
        locale: String = DEFAULT_LOCALE
    ): String {
        val fromModeDisplay = getAIModeDisplayName(fromMode, locale)
        val toModeDisplay = getAIModeDisplayName(toMode, locale)
        val reasonText = getMessage("reason", locale)

        return when (locale.lowercase()) {
            "ko" -> "$entityId | $fromModeDisplay -> $toModeDisplay | $reasonText: $reason"
            else -> "$entityId | $fromModeDisplay -> $toModeDisplay | $reasonText: $reason"
        }
    }

    /**
     * 포맷된 성능 경고 메시지를 생성합니다.
     *
     * @param entityId 엔티티 ID
     * @param operation 작업
     * @param durationMs 소요 시간
     * @param thresholdMs 임계값
     * @param impact 영향
     * @param locale 로케일
     * @return 포맷된 메시지
     */
    fun formatPerformanceWarningMessage(
        entityId: String,
        operation: String,
        durationMs: Double,
        thresholdMs: Double,
        impact: String,
        locale: String = DEFAULT_LOCALE
    ): String {
        val operationText = getMessage("operation", locale)
        val durationText = getMessage("duration", locale)
        val thresholdText = getMessage("threshold", locale)
        val impactText = getMessage("impact", locale)
        val msText = getMessage("ms", locale)

        return when (locale.lowercase()) {
            "ko" -> {
                "$entityId | $operationText: $operation | " +
                "$durationText: ${String.format("%.1f", durationMs)}$msText " +
                "($thresholdText: ${String.format("%.1f", thresholdMs)}$msText) | " +
                "$impactText: $impact"
            }
            else -> {
                "$entityId | $operationText: $operation | " +
                "$durationText: ${String.format("%.1f", durationMs)}$msText " +
                "($thresholdText: ${String.format("%.1f", thresholdMs)}$msText) | " +
                "$impactText: $impact"
            }
        }
    }

    /**
     * 지원되는 로케일 목록을 반환합니다.
     *
     * @return 지원되는 로케일 리스트
     */
    fun getSupportedLocales(): List<String> {
        return messages.keys.toList()
    }

    /**
     * 지정된 로케일이 지원되는지 확인합니다.
     *
     * @param locale 확인할 로케일
     * @return 지원 여부
     */
    fun isLocaleSupported(locale: String): Boolean {
        return messages.containsKey(locale.lowercase())
    }

    /**
     * 특정 로케일의 모든 메시지를 반환합니다.
     *
     * @param locale 로케일
     * @return 메시지 맵, 지원하지 않는 로케일의 경우 기본 로케일 반환
     */
    fun getAllMessages(locale: String = DEFAULT_LOCALE): Map<String, String> {
        val normalizedLocale = locale.lowercase()
        return messages[normalizedLocale] ?: messages[DEFAULT_LOCALE] ?: emptyMap()
    }

    /**
     * 퍼센트 값을 로케일에 맞게 포맷팅합니다.
     *
     * @param value 0.0-1.0 사이의 값
     * @param locale 로케일
     * @return 포맷된 퍼센트 문자열
     */
    fun formatPercentage(value: Float, locale: String = DEFAULT_LOCALE): String {
        val percentage = value * 100
        return when (locale.lowercase()) {
            "ko" -> "${String.format("%.1f", percentage)}%"
            else -> "${String.format("%.1f", percentage)}%"
        }
    }

    /**
     * 거리 값을 로케일에 맞게 포맷팅합니다.
     *
     * @param distance 거리 값
     * @param locale 로케일
     * @return 포맷된 거리 문자열
     */
    fun formatDistance(distance: Float, locale: String = DEFAULT_LOCALE): String {
        val blocksText = getMessage("blocks", locale)
        return "${String.format("%.1f", distance)} $blocksText"
    }

    /**
     * 시간 값을 로케일에 맞게 포맷팅합니다.
     *
     * @param timeMs 시간(밀리초)
     * @param locale 로케일
     * @return 포맷된 시간 문자열
     */
    fun formatTime(timeMs: Double, locale: String = DEFAULT_LOCALE): String {
        val msText = getMessage("ms", locale)
        return "${String.format("%.1f", timeMs)}$msText"
    }
}
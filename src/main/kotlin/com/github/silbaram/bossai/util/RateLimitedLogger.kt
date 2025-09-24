package com.github.silbaram.bossai.util

import com.mojang.logging.LogUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 속도 제한 로깅 유틸리티
 *
 * 로그 스팸을 방지하기 위해 동일한 메시지나 카테고리에 대해
 * 시간 기반 속도 제한을 적용합니다.
 */
class RateLimitedLogger(
    /** 속도 제한 간격 (밀리초) */
    private val rateLimitIntervalMs: Long = 5000L, // 기본 5초
    /** 같은 간격 내 최대 허용 로그 수 */
    private val maxLogsPerInterval: Int = 3
) {

    companion object {
        private val LOGGER = LogUtils.getLogger()
    }

    // 카테고리별 마지막 로그 시간 추적
    private val lastLogTimes = ConcurrentHashMap<String, AtomicLong>()
    // 카테고리별 현재 간격 내 로그 수 추적
    private val logCounts = ConcurrentHashMap<String, AtomicLong>()
    // 카테고리별 현재 간격 시작 시간
    private val intervalStartTimes = ConcurrentHashMap<String, AtomicLong>()

    /**
     * INFO 레벨 로그를 속도 제한과 함께 출력합니다.
     *
     * @param category 로그 카테고리 (예: "PERFORMANCE_WARNING", "AI_DECISION")
     * @param message 로그 메시지
     * @return 실제로 로깅되었는지 여부
     */
    fun info(category: String, message: String): Boolean {
        return logWithRateLimit(category, message, LogLevel.INFO)
    }

    /**
     * WARN 레벨 로그를 속도 제한과 함께 출력합니다.
     *
     * @param category 로그 카테고리
     * @param message 로그 메시지
     * @return 실제로 로깅되었는지 여부
     */
    fun warn(category: String, message: String): Boolean {
        return logWithRateLimit(category, message, LogLevel.WARN)
    }

    /**
     * ERROR 레벨 로그를 속도 제한과 함께 출력합니다.
     *
     * @param category 로그 카테고리
     * @param message 로그 메시지
     * @return 실제로 로깅되었는지 여부
     */
    fun error(category: String, message: String): Boolean {
        return logWithRateLimit(category, message, LogLevel.ERROR)
    }

    /**
     * DEBUG 레벨 로그를 속도 제한과 함께 출력합니다.
     *
     * @param category 로그 카테고리
     * @param message 로그 메시지
     * @return 실제로 로깅되었는지 여부
     */
    fun debug(category: String, message: String): Boolean {
        return logWithRateLimit(category, message, LogLevel.DEBUG)
    }

    /**
     * 메시지 해시를 기반으로 한 속도 제한 로깅
     *
     * 동일한 메시지가 반복되는 것을 방지합니다.
     *
     * @param messageHash 메시지의 해시값
     * @param message 로그 메시지
     * @param level 로그 레벨
     * @return 실제로 로깅되었는지 여부
     */
    fun logByMessageHash(messageHash: String, message: String, level: LogLevel = LogLevel.INFO): Boolean {
        return logWithRateLimit("MSG_HASH_$messageHash", message, level)
    }

    /**
     * 강제로 로깅합니다 (속도 제한 무시).
     *
     * 중요한 메시지나 오류의 경우 사용합니다.
     *
     * @param message 로그 메시지
     * @param level 로그 레벨
     */
    fun forceLog(message: String, level: LogLevel = LogLevel.INFO) {
        when (level) {
            LogLevel.DEBUG -> LOGGER.debug(message)
            LogLevel.INFO -> LOGGER.info(message)
            LogLevel.WARN -> LOGGER.warn(message)
            LogLevel.ERROR -> LOGGER.error(message)
        }
    }

    /**
     * 특정 카테고리의 속도 제한 상태를 리셋합니다.
     *
     * @param category 리셋할 카테고리
     */
    fun resetRateLimit(category: String) {
        lastLogTimes.remove(category)
        logCounts.remove(category)
        intervalStartTimes.remove(category)
    }

    /**
     * 모든 카테고리의 속도 제한 상태를 리셋합니다.
     */
    fun resetAllRateLimits() {
        lastLogTimes.clear()
        logCounts.clear()
        intervalStartTimes.clear()
    }

    /**
     * 특정 카테고리의 현재 로그 수를 반환합니다.
     *
     * @param category 카테고리
     * @return 현재 간격 내 로그 수
     */
    fun getCurrentLogCount(category: String): Long {
        val currentTime = System.currentTimeMillis()
        val intervalStart = intervalStartTimes[category]?.get() ?: 0L

        // 새로운 간격이 시작된 경우 카운트 리셋
        if (currentTime - intervalStart > rateLimitIntervalMs) {
            logCounts[category]?.set(0L)
            intervalStartTimes[category]?.set(currentTime)
            return 0L
        }

        return logCounts[category]?.get() ?: 0L
    }

    /**
     * 카테고리별 속도 제한 통계를 반환합니다.
     *
     * @return 카테고리별 통계 맵
     */
    fun getRateLimitStats(): Map<String, RateLimitStats> {
        val currentTime = System.currentTimeMillis()
        return lastLogTimes.keys.associateWith { category ->
            val lastLogTime = lastLogTimes[category]?.get() ?: 0L
            val currentCount = getCurrentLogCount(category)
            val intervalStart = intervalStartTimes[category]?.get() ?: 0L

            RateLimitStats(
                category = category,
                lastLogTime = lastLogTime,
                currentIntervalStart = intervalStart,
                currentLogCount = currentCount,
                maxLogsPerInterval = maxLogsPerInterval,
                rateLimitIntervalMs = rateLimitIntervalMs,
                isRateLimited = currentCount >= maxLogsPerInterval
            )
        }
    }

    /**
     * 속도 제한과 함께 로깅을 수행하는 내부 메서드
     */
    private fun logWithRateLimit(category: String, message: String, level: LogLevel): Boolean {
        val currentTime = System.currentTimeMillis()

        // 현재 간격 내 로그 수 확인 및 업데이트
        val intervalStart = intervalStartTimes.computeIfAbsent(category) { AtomicLong(currentTime) }
        val logCount = logCounts.computeIfAbsent(category) { AtomicLong(0) }

        // 새로운 간격 시작 확인
        if (currentTime - intervalStart.get() > rateLimitIntervalMs) {
            intervalStart.set(currentTime)
            logCount.set(0)
        }

        // 속도 제한 확인
        val currentCount = logCount.incrementAndGet()
        if (currentCount > maxLogsPerInterval) {
            // 속도 제한 초과 시 첫 번째 경고만 출력
            if (currentCount == maxLogsPerInterval + 1L) {
                val rateLimitMessage = "[RATE_LIMITED] Category '$category' exceeded $maxLogsPerInterval logs per ${rateLimitIntervalMs}ms. Suppressing further logs."
                when (level) {
                    LogLevel.DEBUG -> LOGGER.debug(rateLimitMessage)
                    LogLevel.INFO -> LOGGER.info(rateLimitMessage)
                    LogLevel.WARN -> LOGGER.warn(rateLimitMessage)
                    LogLevel.ERROR -> LOGGER.error(rateLimitMessage)
                }
            }
            return false
        }

        // 로그 출력
        lastLogTimes.computeIfAbsent(category) { AtomicLong() }.set(currentTime)
        when (level) {
            LogLevel.DEBUG -> LOGGER.debug(message)
            LogLevel.INFO -> LOGGER.info(message)
            LogLevel.WARN -> LOGGER.warn(message)
            LogLevel.ERROR -> LOGGER.error(message)
        }

        return true
    }
}

/**
 * 로그 레벨 열거형
 */
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

/**
 * 속도 제한 통계 데이터 클래스
 */
data class RateLimitStats(
    /** 카테고리 이름 */
    val category: String,
    /** 마지막 로그 시간 */
    val lastLogTime: Long,
    /** 현재 간격 시작 시간 */
    val currentIntervalStart: Long,
    /** 현재 간격 내 로그 수 */
    val currentLogCount: Long,
    /** 간격당 최대 허용 로그 수 */
    val maxLogsPerInterval: Int,
    /** 속도 제한 간격 (밀리초) */
    val rateLimitIntervalMs: Long,
    /** 현재 속도 제한 상태인지 여부 */
    val isRateLimited: Boolean
) {
    /** 다음 로그 가능 시간 */
    val nextLogAllowedTime: Long
        get() = currentIntervalStart + rateLimitIntervalMs

    /** 현재 간격 남은 시간 (밀리초) */
    val remainingIntervalTime: Long
        get() = (nextLogAllowedTime - System.currentTimeMillis()).coerceAtLeast(0L)

    /** 현재 간격에서 사용 가능한 로그 수 */
    val remainingLogsInInterval: Int
        get() = (maxLogsPerInterval - currentLogCount).toInt().coerceAtLeast(0)
}
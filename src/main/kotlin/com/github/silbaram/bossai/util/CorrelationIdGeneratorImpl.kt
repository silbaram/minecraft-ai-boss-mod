package com.github.silbaram.bossai.util

import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * CorrelationIdGenerator 인터페이스의 기본 구현체
 *
 * 이 클래스는 AI 보스의 각 의사결정을 추적할 수 있는 고유한 상관관계 ID를 생성합니다.
 * 생성된 ID는 다음 형식을 따릅니다:
 * "session_{sessionId}_entity_{entityId}_decision_{sequence}"
 */
class CorrelationIdGeneratorImpl : CorrelationIdGenerator {

    companion object {
        private val decisionSequence = AtomicLong(0)
    }

    private val sessionId: String = generateSessionId()

    /**
     * 새로운 상관관계 ID를 생성합니다.
     *
     * @param entityId 엔티티 ID
     * @return 고유한 상관관계 ID
     * @throws IllegalArgumentException 엔티티 ID가 비어있는 경우
     */
    override fun generateCorrelationId(entityId: String): String {
        require(entityId.isNotBlank()) { "Entity ID cannot be empty" }

        val sanitizedEntityId = sanitizeEntityId(entityId)
        val sequence = decisionSequence.incrementAndGet()

        return "session_${sessionId}_entity_${sanitizedEntityId}_decision_$sequence"
    }

    /**
     * 현재 세션 ID를 반환합니다.
     *
     * @return 세션 ID
     */
    override fun getCurrentSessionId(): String {
        return sessionId
    }

    /**
     * 세션 ID를 생성합니다.
     * UUID의 첫 8자리와 현재 시간의 마지막 6자리를 조합합니다.
     */
    private fun generateSessionId(): String {
        val uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
        val timestamp = Instant.now().epochSecond.toString().takeLast(6)
        return "${uuid}_$timestamp"
    }

    /**
     * 새로운 세션을 시작합니다.
     */
    override fun startNewSession(): String {
        val newSessionId = generateSessionId()
        return newSessionId
    }

    /**
     * 다음 의사결정 시퀀스 번호를 반환합니다.
     */
    override fun getNextDecisionSequence(): Long {
        return decisionSequence.incrementAndGet()
    }

    /**
     * 엔티티 ID에서 특수 문자를 제거하고 안전한 형식으로 변환합니다.
     */
    private fun sanitizeEntityId(entityId: String): String {
        return entityId
            .replace("[^a-zA-Z0-9_]".toRegex(), "_") // 특수 문자를 밑줄로 변경
            .replace("_{2,}".toRegex(), "_") // 연속된 밑줄을 하나로 통합
            .trim('_') // 앞뒤 밑줄 제거
            .take(32) // 최대 길이 제한
            .ifEmpty { "unknown" } // 빈 문자열 처리
    }
}

/**
 * CorrelationIdGenerator 인터페이스
 *
 * 상관관계 ID 생성을 위한 인터페이스입니다.
 */
interface CorrelationIdGenerator {
    /**
     * 새로운 상관관계 ID를 생성합니다.
     *
     * @param entityId 엔티티 ID
     * @return 고유한 상관관계 ID
     */
    fun generateCorrelationId(entityId: String): String

    /**
     * 현재 세션 ID를 반환합니다.
     */
    fun getCurrentSessionId(): String

    /**
     * 새로운 세션을 시작합니다.
     */
    fun startNewSession(): String

    /**
     * 다음 의사결정 시퀀스 번호를 반환합니다.
     */
    fun getNextDecisionSequence(): Long
}
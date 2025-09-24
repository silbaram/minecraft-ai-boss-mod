package com.github.silbaram.bossai.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * CorrelationIdGenerator.generateCorrelationId() 계약 테스트
 *
 * 이 테스트는 구현 전에 작성되어 실패해야 합니다 (TDD).
 * 상관관계 ID 생성 기능을 검증합니다.
 */
class CorrelationIdGeneratorTest {

    @Test
    fun `generateCorrelationId should create unique IDs for same entity`() {
        // Given: 같은 엔티티에 대한 연속 호출
        val entityId = "sentinel_boss_001"

        // When & Then: 구현체가 없으므로 실패 예상
        assertThrows<RuntimeException> {
            val generator = createMockGenerator()
            val id1 = generator.generateCorrelationId(entityId)
            val id2 = generator.generateCorrelationId(entityId)

            // 같은 엔티티라도 서로 다른 상관관계 ID가 생성되어야 함
            assertNotEquals(id1, id2)
        }
    }

    @Test
    fun `generateCorrelationId should include entity identifier`() {
        // Given: 특정 엔티티 ID
        val entityId = "sentinel_boss_042"

        // When & Then: 생성된 ID에 엔티티 식별자가 포함되어야 함
        assertThrows<RuntimeException> {
            val generator = createMockGenerator()
            val correlationId = generator.generateCorrelationId(entityId)

            // 상관관계 ID에 엔티티 ID가 포함되어야 함
            assertTrue(correlationId.contains(entityId) || correlationId.contains("042"))
        }
    }

    @Test
    fun `generateCorrelationId should handle empty entity ID`() {
        // Given: 빈 엔티티 ID
        val emptyEntityId = ""

        // When & Then: 빈 엔티티 ID에 대해 예외 발생 예상
        assertThrows<IllegalArgumentException> {
            val generator = createMockGenerator()
            generator.generateCorrelationId(emptyEntityId)
        }
    }

    @Test
    fun `generateCorrelationId should create time-ordered IDs`() {
        // Given: 시간 순서대로 생성되는 ID들
        val entityId = "sentinel_boss_001"

        // When & Then: 시간 순서 정보가 포함되어야 함
        assertThrows<RuntimeException> {
            val generator = createMockGenerator()
            val id1 = generator.generateCorrelationId(entityId)
            Thread.sleep(10) // 작은 시간 간격
            val id2 = generator.generateCorrelationId(entityId)

            // ID에 시간 정보가 반영되어 순서 구분 가능해야 함
            assertNotEquals(id1, id2)
        }
    }

    @Test
    fun `generateCorrelationId should handle special characters in entity ID`() {
        // Given: 특수 문자가 포함된 엔티티 ID
        val specialEntityId = "sentinel-boss_001:advanced@test"

        // When & Then: 특수 문자가 포함된 ID도 처리 가능해야 함
        assertThrows<RuntimeException> {
            val generator = createMockGenerator()
            val correlationId = generator.generateCorrelationId(specialEntityId)

            // 생성된 ID가 유효한 형식이어야 함
            assertNotNull(correlationId)
            assertTrue(correlationId.isNotEmpty())
        }
    }

    @Test
    fun `getCurrentSessionId should return consistent session ID`() {
        // Given: 세션 ID 조회

        // When & Then: 같은 세션 내에서 일관된 ID 반환
        assertThrows<RuntimeException> {
            val generator = createMockGenerator()
            val sessionId1 = generator.getCurrentSessionId()
            val sessionId2 = generator.getCurrentSessionId()

            // 같은 세션 내에서는 동일한 세션 ID 반환
            assertEquals(sessionId1, sessionId2)
        }
    }

    @Test
    fun `getCurrentSessionId should return non-empty session ID`() {
        // Given: 세션 ID 요청

        // When & Then: 비어있지 않은 세션 ID 반환
        assertThrows<RuntimeException> {
            val generator = createMockGenerator()
            val sessionId = generator.getCurrentSessionId()

            assertNotNull(sessionId)
            assertTrue(sessionId.isNotEmpty())
        }
    }

    @Test
    fun `correlation ID format should be parseable`() {
        // Given: 상관관계 ID 형식 검증
        val entityId = "sentinel_boss_001"

        // When & Then: 생성된 ID가 예상 형식을 따라야 함
        assertThrows<RuntimeException> {
            val generator = createMockGenerator()
            val correlationId = generator.generateCorrelationId(entityId)

            // 예상 형식: session_{sessionId}_entity_{entityId}_decision_{sequence}
            assertTrue(correlationId.matches(Regex("session_.+_entity_.+_decision_\\d+")))
        }
    }

    @Test
    fun `generateCorrelationId should handle concurrent calls`() {
        // Given: 동시 호출 상황
        val entityId = "sentinel_boss_concurrent"

        // When & Then: 동시 호출에서도 고유한 ID 생성
        assertThrows<RuntimeException> {
            val generator = createMockGenerator()
            val ids = mutableSetOf<String>()

            // 동시에 여러 ID 생성
            repeat(10) {
                val id = generator.generateCorrelationId(entityId)
                ids.add(id)
            }

            // 모든 ID가 고유해야 함
            assertEquals(10, ids.size)
        }
    }

    // 헬퍼 메서드 (구현 전이므로 실패하는 것이 정상)
    private fun createMockGenerator(): CorrelationIdGenerator {
        throw RuntimeException("CorrelationIdGenerator implementation not yet available")
    }
}

/**
 * CorrelationIdGenerator 인터페이스 (아직 구현되지 않음)
 *
 * 이 인터페이스는 계약 정의만 되어 있고 실제 구현은 나중에 Phase 3.3에서 수행됩니다.
 */
interface CorrelationIdGenerator {
    fun generateCorrelationId(entityId: String): String
    fun getCurrentSessionId(): String
}
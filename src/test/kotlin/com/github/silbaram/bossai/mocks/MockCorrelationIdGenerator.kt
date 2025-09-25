package com.github.silbaram.bossai.mocks

import com.github.silbaram.bossai.util.CorrelationIdGenerator
import java.util.concurrent.atomic.AtomicLong

/**
 * Mock implementation of CorrelationIdGenerator for testing
 */
class MockCorrelationIdGenerator : CorrelationIdGenerator {
    private val counter = AtomicLong(0)
    private val generatedIds = mutableListOf<String>()
    private val sessionId = "test_session_${System.currentTimeMillis()}"

    override fun generateCorrelationId(entityId: String): String {
        if (entityId.isBlank()) {
            throw IllegalArgumentException("Entity ID cannot be blank")
        }

        val id = "${entityId}_correlation_${counter.incrementAndGet()}_${System.currentTimeMillis()}"
        generatedIds.add(id)
        return id
    }

    override fun getCurrentSessionId(): String = sessionId

    fun getGeneratedIds(): List<String> = generatedIds.toList()
    fun clear() = generatedIds.clear()
}
package com.github.silbaram.bossai.ai.logging

/**
 * AI 결정 과정의 성능 측정 데이터 클래스
 *
 * AI 보스의 의사결정 과정에서 발생하는 성능 메트릭을 추적합니다.
 * 10-tick 제약 조건 준수와 성능 최적화를 위해 사용됩니다.
 */
data class PerformanceMetrics(
    /** 전체 평가 지속 시간 (나노초) */
    val totalTimeNanos: Long,

    /** 기능 추출 시간 (나노초) */
    val featureExtractionTimeNanos: Long,

    /** 모델 추론 시간 (나노초), ML 모드에서만 사용 */
    val modelInferenceTimeNanos: Long? = null,

    /** 휴리스틱 계산 시간 (나노초), 휴리스틱 모드에서만 사용 */
    val heuristicCalculationTimeNanos: Long? = null,

    /** 전술 적용 시간 (나노초) */
    val tacticApplicationTimeNanos: Long,

    /** 처리 전 메모리 사용량 (MB) */
    val memoryUsageBeforeMB: Double,

    /** 처리 후 메모리 사용량 (MB) */
    val memoryUsageAfterMB: Double,

    /** CPU 사용 시간 (나노초), 선택적 */
    val cpuTimeNanos: Long? = null
) {
    init {
        // 검증 규칙
        require(totalTimeNanos >= 0) { "Total time must be non-negative" }
        require(featureExtractionTimeNanos >= 0) { "Feature extraction time must be non-negative" }
        require(modelInferenceTimeNanos == null || modelInferenceTimeNanos >= 0) { "Model inference time must be non-negative" }
        require(heuristicCalculationTimeNanos == null || heuristicCalculationTimeNanos >= 0) { "Heuristic calculation time must be non-negative" }
        require(tacticApplicationTimeNanos >= 0) { "Tactic application time must be non-negative" }
        require(memoryUsageBeforeMB >= 0) { "Memory usage before must be non-negative" }
        require(memoryUsageAfterMB >= 0) { "Memory usage after must be non-negative" }
        require(cpuTimeNanos == null || cpuTimeNanos >= 0) { "CPU time must be non-negative" }

        // 제약 조건 검증
        // 성능 경고 임계값 확인 (예외 대신 경고만 발생)
        if (totalTimeMs >= 1000.0) {
            System.err.println("[WARNING] Total evaluation time exceeded 1000ms: ${totalTimeMs}ms")
        }
        if (memoryDeltaMB >= 50.0) {
            System.err.println("[WARNING] Memory usage increase exceeded 50MB: ${memoryDeltaMB}MB")
        }
    }

    /** 전체 시간을 밀리초로 변환 */
    val totalTimeMs: Double get() = totalTimeNanos / 1_000_000.0

    /** 기능 추출 시간을 밀리초로 변환 */
    val featureExtractionTimeMs: Double get() = featureExtractionTimeNanos / 1_000_000.0

    /** 모델 추론 시간을 밀리초로 변환 (ML 모드만) */
    val modelInferenceTimeMs: Double? get() = modelInferenceTimeNanos?.let { it / 1_000_000.0 }

    /** 휴리스틱 계산 시간을 밀리초로 변환 (휴리스틱 모드만) */
    val heuristicCalculationTimeMs: Double? get() = heuristicCalculationTimeNanos?.let { it / 1_000_000.0 }

    /** 전술 적용 시간을 밀리초로 변환 */
    val tacticApplicationTimeMs: Double get() = tacticApplicationTimeNanos / 1_000_000.0

    /** CPU 시간을 밀리초로 변환 */
    val cpuTimeMs: Double? get() = cpuTimeNanos?.let { it / 1_000_000.0 }

    /** 메모리 사용량 변화 (MB) */
    val memoryDeltaMB: Double get() = memoryUsageAfterMB - memoryUsageBeforeMB

    /** 이 메트릭이 ML 모드에서 생성되었는지 확인 */
    val isFromMLMode: Boolean get() = modelInferenceTimeNanos != null && heuristicCalculationTimeNanos == null

    /** 이 메트릭이 휴리스틱 모드에서 생성되었는지 확인 */
    val isFromHeuristicMode: Boolean get() = modelInferenceTimeNanos == null && heuristicCalculationTimeNanos != null

    /** 성능 경고가 필요한지 확인 (20ms 임계값) */
    val requiresPerformanceWarning: Boolean get() = totalTimeMs > 20.0

    /** 심각한 성능 문제인지 확인 (50ms 임계값) */
    val isCriticalPerformanceIssue: Boolean get() = totalTimeMs > 50.0

    /** 메모리 사용량이 경고 수준인지 확인 (5MB 임계값) */
    val hasMemoryWarning: Boolean get() = memoryDeltaMB > 5.0

    companion object {
        /** 10-tick 제약 조건 (밀리초) */
        const val TICK_CONSTRAINT_MS = 500.0

        /** 성능 경고 임계값 (밀리초) */
        const val PERFORMANCE_WARNING_THRESHOLD_MS = 20.0

        /** 심각한 성능 문제 임계값 (밀리초) */
        const val CRITICAL_PERFORMANCE_THRESHOLD_MS = 50.0

        /** 메모리 경고 임계값 (MB) */
        const val MEMORY_WARNING_THRESHOLD_MB = 5.0

        /** 최대 메모리 증가 허용량 (MB) */
        const val MAX_MEMORY_INCREASE_MB = 10.0

        /**
         * ML 모드용 PerformanceMetrics를 생성합니다.
         */
        fun forMLMode(
            totalTimeNanos: Long,
            featureExtractionTimeNanos: Long,
            modelInferenceTimeNanos: Long,
            tacticApplicationTimeNanos: Long,
            memoryUsageBeforeMB: Double,
            memoryUsageAfterMB: Double,
            cpuTimeNanos: Long? = null
        ): PerformanceMetrics {
            return PerformanceMetrics(
                totalTimeNanos = totalTimeNanos,
                featureExtractionTimeNanos = featureExtractionTimeNanos,
                modelInferenceTimeNanos = modelInferenceTimeNanos,
                heuristicCalculationTimeNanos = null,
                tacticApplicationTimeNanos = tacticApplicationTimeNanos,
                memoryUsageBeforeMB = memoryUsageBeforeMB,
                memoryUsageAfterMB = memoryUsageAfterMB,
                cpuTimeNanos = cpuTimeNanos
            )
        }

        /**
         * 휴리스틱 모드용 PerformanceMetrics를 생성합니다.
         */
        fun forHeuristicMode(
            totalTimeNanos: Long,
            featureExtractionTimeNanos: Long,
            heuristicCalculationTimeNanos: Long,
            tacticApplicationTimeNanos: Long,
            memoryUsageBeforeMB: Double,
            memoryUsageAfterMB: Double,
            cpuTimeNanos: Long? = null
        ): PerformanceMetrics {
            return PerformanceMetrics(
                totalTimeNanos = totalTimeNanos,
                featureExtractionTimeNanos = featureExtractionTimeNanos,
                modelInferenceTimeNanos = null,
                heuristicCalculationTimeNanos = heuristicCalculationTimeNanos,
                tacticApplicationTimeNanos = tacticApplicationTimeNanos,
                memoryUsageBeforeMB = memoryUsageBeforeMB,
                memoryUsageAfterMB = memoryUsageAfterMB,
                cpuTimeNanos = cpuTimeNanos
            )
        }

        /**
         * 빠른 테스트용 더미 PerformanceMetrics를 생성합니다.
         */
        fun dummy(totalTimeMs: Double = 10.0): PerformanceMetrics {
            val totalTimeNanos = (totalTimeMs * 1_000_000).toLong()
            return PerformanceMetrics(
                totalTimeNanos = totalTimeNanos,
                featureExtractionTimeNanos = totalTimeNanos / 4,
                modelInferenceTimeNanos = totalTimeNanos / 2,
                heuristicCalculationTimeNanos = null,
                tacticApplicationTimeNanos = totalTimeNanos / 4,
                memoryUsageBeforeMB = 100.0,
                memoryUsageAfterMB = 100.5,
                cpuTimeNanos = totalTimeNanos
            )
        }
    }
}
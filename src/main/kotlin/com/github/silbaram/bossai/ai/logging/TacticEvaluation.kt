package com.github.silbaram.bossai.ai.logging

import com.github.silbaram.bossai.ai.Tactic

/**
 * 전술 평가 결과 데이터 클래스
 *
 * AI 보스가 각 전술을 평가한 결과를 저장합니다.
 * ML 모드와 휴리스틱 모드에 따라 다른 필드가 사용됩니다.
 */
data class TacticEvaluation(
    /** 평가된 전술 유형 */
    val tactic: Tactic,

    /** 현재 사용 가능 여부 */
    val available: Boolean,

    /** ML 모델의 예측 신뢰도 (0.0-1.0), ML 모드에서만 사용 */
    val confidence: Double? = null,

    /** 휴리스틱 모드에서의 점수, 휴리스틱 모드에서만 사용 */
    val heuristicScore: Double? = null,

    /** 남은 쿨다운 시간 (틱 단위) */
    val cooldownRemaining: Int,

    /** 이 전술이 선택되었는지 여부 */
    val selected: Boolean,

    /** 선택/배제 이유 */
    val rationale: String
) {
    init {
        // 검증 규칙
        require(cooldownRemaining >= 0) { "Cooldown remaining must be non-negative" }
        require(confidence == null || confidence in 0.0..1.0) { "Confidence must be between 0.0 and 1.0" }
        require(rationale.isNotBlank()) { "Rationale cannot be blank" }
    }

    /**
     * 이 평가가 ML 모드에서 생성되었는지 확인합니다.
     */
    val isFromMLMode: Boolean
        get() = confidence != null && heuristicScore == null

    /**
     * 이 평가가 휴리스틱 모드에서 생성되었는지 확인합니다.
     */
    val isFromHeuristicMode: Boolean
        get() = confidence == null && heuristicScore != null

    /**
     * 전술의 효용성 점수를 반환합니다.
     * ML 모드에서는 confidence를, 휴리스틱 모드에서는 heuristicScore를 반환합니다.
     */
    val effectivenessScore: Double
        get() = confidence ?: heuristicScore ?: 0.0

    companion object {
        /**
         * ML 모드용 TacticEvaluation을 생성합니다.
         */
        fun forMLMode(
            tactic: Tactic,
            available: Boolean,
            confidence: Double,
            cooldownRemaining: Int,
            selected: Boolean,
            rationale: String
        ): TacticEvaluation {
            return TacticEvaluation(
                tactic = tactic,
                available = available,
                confidence = confidence,
                heuristicScore = null,
                cooldownRemaining = cooldownRemaining,
                selected = selected,
                rationale = rationale
            )
        }

        /**
         * 휴리스틱 모드용 TacticEvaluation을 생성합니다.
         */
        fun forHeuristicMode(
            tactic: Tactic,
            available: Boolean,
            heuristicScore: Double,
            cooldownRemaining: Int,
            selected: Boolean,
            rationale: String
        ): TacticEvaluation {
            return TacticEvaluation(
                tactic = tactic,
                available = available,
                confidence = null,
                heuristicScore = heuristicScore,
                cooldownRemaining = cooldownRemaining,
                selected = selected,
                rationale = rationale
            )
        }

        /**
         * 사용할 수 없는 전술(쿨다운 중)에 대한 TacticEvaluation을 생성합니다.
         */
        fun unavailable(
            tactic: Tactic,
            cooldownRemaining: Int,
            reason: String = "Tactic on cooldown"
        ): TacticEvaluation {
            return TacticEvaluation(
                tactic = tactic,
                available = false,
                confidence = null,
                heuristicScore = null,
                cooldownRemaining = cooldownRemaining,
                selected = false,
                rationale = reason
            )
        }
    }
}
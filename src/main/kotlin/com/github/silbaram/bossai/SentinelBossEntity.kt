package com.github.silbaram.bossai

import com.github.silbaram.bossai.ai.BurstAoeGoal
import com.github.silbaram.bossai.ai.KiteGoal
import com.github.silbaram.bossai.ai.SummonGoal
import com.github.silbaram.bossai.ai.Tactic
import com.github.silbaram.bossai.ai.TacticsModel
import com.github.silbaram.bossai.ai.logging.*
import com.github.silbaram.bossai.util.CorrelationIdGeneratorImpl
import com.github.silbaram.bossai.util.RateLimitedLogger
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.RandomStrollGoal
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import com.mojang.logging.LogUtils

/**
 * Custom boss entity implementing dynamic AI tactics.  Extends [Monster]
 * to leverage the existing combat behaviours and pathfinding.  Every 10
 * ticks the boss evaluates the current game state and selects a new
 * [Tactic] using a lightweight model【759699647327456†L118-L152】.  When a tactic
 * changes, a corresponding goal is attached to the [goalSelector].
 */
class SentinelBossEntity(type: EntityType<out SentinelBossEntity>, level: Level) : Monster(type, level) {
    private var tickCounter = 0
    private var currentTactic: Tactic = Tactic.IDLE
    private var currentCustomGoal: Goal? = null
    private val tacticsModel = TacticsModel()
    private val logger = LogUtils.getLogger()

    // AI 로깅 시스템 구성요소
    private val correlationIdGenerator = CorrelationIdGeneratorImpl()
    private val rateLimitedLogger = RateLimitedLogger()
    private val logFormatter = LogFormatterImpl()
    private val aiDecisionLogger = AIDecisionLoggerImpl(logFormatter, rateLimitedLogger)
    private var decisionCycleNumber = 0L
    
    // Tactic cooldown system
    private val tacticCooldowns = mutableMapOf<Tactic, Int>()
    private val tacticHistory = mutableListOf<Tactic>()
    private val maxHistorySize = 5
    
    // Cooldown durations (in ticks)
    private val cooldownDurations = mapOf(
        Tactic.BURST_AOE to 100,   // 5 seconds
        Tactic.KITE to 200,        // 10 seconds  
        Tactic.SUMMON to 300       // 15 seconds
    )

    override fun registerGoals() {
        // Basic survival and movement goals
        goalSelector.addGoal(0, FloatGoal(this))
        goalSelector.addGoal(2, MeleeAttackGoal(this, 1.0, false))
        goalSelector.addGoal(7, RandomStrollGoal(this, 1.0))
        goalSelector.addGoal(8, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        
        // Target selection goals
        targetSelector.addGoal(1, NearestAttackableTargetGoal(this, Player::class.java, true))
    }

    override fun tick() {
        super.tick()
        if (!level().isClientSide) {
            tickCounter++
            updateCooldowns()
            
            // Evaluate tactics once every half second (10 ticks)
            if (tickCounter % 10 == 0) {
                evaluateTactics()
            }
        }
    }

    /**
     * 전술 평가 및 로깅을 수행하는 메인 메서드
     */
    private fun evaluateTactics() {
        // 더욱 안전한 entityId 생성
        val rawUuid = try {
            uuid?.toString()
        } catch (e: Exception) {
            logger.warn("Failed to get UUID: ${e.message}")
            null
        }

        val safeUuid = rawUuid?.take(8) ?: "unknown_${hashCode().toString().take(8)}"
        val entityIdBase = "sentinel_boss_$safeUuid"

        // Null 안전성 보장
        val entityId: String = if (entityIdBase.isBlank()) {
            "fallback_entity_${System.currentTimeMillis()}_${this.hashCode()}"
        } else {
            entityIdBase
        }

        val correlationId: String = try {
            correlationIdGenerator.generateCorrelationId(entityId)
        } catch (e: Exception) {
            logger.warn("Failed to generate correlation ID: ${e.message}")
            "fallback_correlation_${System.currentTimeMillis()}_${entityId.hashCode()}"
        }

        // 디버그: 생성된 값들 확인 (개발 모드에서만)
        if (System.getProperty("boss_ai.dev", "false").toBoolean()) {
            logger.info("DEBUG: entityId='$entityId' (length=${entityId.length}), correlationId='$correlationId' (length=${correlationId.length}), safeUuid='$safeUuid'")
        }

        val startTime = System.nanoTime()
        val memoryBefore = getMemoryUsageMB()

        // 1. 기능 추출
        val featureExtractionStart = System.nanoTime()
        val features = extractFeatures()
        val featureExtractionTime = System.nanoTime() - featureExtractionStart

        // 2. 전술 평가
        val tacticEvaluationStart = System.nanoTime()
        val tacticEvaluations = evaluateAllTactics(features)
        val suggestedTactic = tacticsModel.selectTactic(features)
        val newTactic = selectViableTactic(suggestedTactic)
        val tacticEvaluationTime = System.nanoTime() - tacticEvaluationStart

        // 3. 전술 적용
        val tacticApplicationStart = System.nanoTime()
        val previousTactic = currentTactic
        var reasoning = "AI evaluation completed"

        if (newTactic != currentTactic) {
            currentTactic = newTactic
            applyTactic(newTactic)
            addTacticToHistory(newTactic)
            startCooldown(newTactic)
            reasoning = "Tactic changed due to ${getTacticChangeReason(features, newTactic)}"
        } else {
            reasoning = "Tactic maintained - ${getMaintainReason(features, currentTactic)}"
        }
        val tacticApplicationTime = System.nanoTime() - tacticApplicationStart

        val totalTime = System.nanoTime() - startTime
        val memoryAfter = getMemoryUsageMB()

        // 4. 성능 메트릭 생성
        val performanceMetrics = if (tacticsModel.isMLMode()) {
            PerformanceMetrics.forMLMode(
                totalTimeNanos = totalTime,
                featureExtractionTimeNanos = featureExtractionTime,
                modelInferenceTimeNanos = tacticEvaluationTime,
                tacticApplicationTimeNanos = tacticApplicationTime,
                memoryUsageBeforeMB = memoryBefore,
                memoryUsageAfterMB = memoryAfter
            )
        } else {
            PerformanceMetrics.forHeuristicMode(
                totalTimeNanos = totalTime,
                featureExtractionTimeNanos = featureExtractionTime,
                heuristicCalculationTimeNanos = tacticEvaluationTime,
                tacticApplicationTimeNanos = tacticApplicationTime,
                memoryUsageBeforeMB = memoryBefore,
                memoryUsageAfterMB = memoryAfter
            )
        }

        // 5. AI 결정 로깅
        decisionCycleNumber++

        // 디버그: 로깅 메서드 호출 직전의 파라미터 값들 확인 (개발 모드에서만)
        if (System.getProperty("boss_ai.dev", "false").toBoolean()) {
            logger.info("DEBUG: Before logging - entityId='$entityId' (length=${entityId.length}), correlationId='$correlationId' (length=${correlationId.length})")
        }

        aiDecisionLogger.logTacticDecision(
            entityId = entityId,
            correlationId = correlationId,
            aiMode = if (tacticsModel.isMLMode()) "MACHINE_LEARNING" else "RULE_BASED_HEURISTICS",
            previousTactic = previousTactic,
            selectedTactic = newTactic,
            features = features,
            tacticEvaluations = tacticEvaluations,
            performanceMetrics = performanceMetrics,
            reasoning = reasoning
        )
    }

    /**
     * 모든 전술에 대한 평가를 수행합니다
     */
    private fun evaluateAllTactics(features: FloatArray): List<TacticEvaluation> {
        return Tactic.values().map { tactic ->
            val isAvailable = !isOnCooldown(tactic) && !wasRecentlyUsed(tactic)
            val cooldownRemaining = tacticCooldowns[tactic] ?: 0

            if (tacticsModel.isMLMode()) {
                // ML 모드: confidence 기반 평가
                val confidence = tacticsModel.getTacticConfidence(tactic, features)
                TacticEvaluation.forMLMode(
                    tactic = tactic,
                    available = isAvailable,
                    confidence = confidence,
                    cooldownRemaining = cooldownRemaining,
                    selected = false, // 나중에 업데이트됨
                    rationale = if (isAvailable) "ML confidence: ${String.format("%.3f", confidence)}"
                               else "Unavailable: ${getUnavailableReason(tactic)}"
                )
            } else {
                // 휴리스틱 모드: rule-based 평가
                val heuristicScore = calculateHeuristicScore(tactic, features)
                TacticEvaluation.forHeuristicMode(
                    tactic = tactic,
                    available = isAvailable,
                    heuristicScore = heuristicScore,
                    cooldownRemaining = cooldownRemaining,
                    selected = false, // 나중에 업데이트됨
                    rationale = if (isAvailable) "Heuristic score: ${String.format("%.3f", heuristicScore)}"
                               else "Unavailable: ${getUnavailableReason(tactic)}"
                )
            }
        }.map { evaluation ->
            // 선택된 전술 표시 업데이트
            evaluation.copy(selected = evaluation.tactic == currentTactic)
        }
    }

    /**
     * 휴리스틱 점수를 계산합니다
     */
    private fun calculateHeuristicScore(tactic: Tactic, features: FloatArray): Double {
        val hpPct = features[0]
        val distance = features[1]
        val nearbyCount = features[2]

        return when (tactic) {
            Tactic.IDLE -> 0.1 // 기본 점수
            Tactic.BURST_AOE -> {
                if (nearbyCount >= 2 && distance < 8) 0.8 else 0.2
            }
            Tactic.KITE -> {
                if (hpPct < 0.3 && distance < 10) 0.9 else 0.3
            }
            Tactic.SUMMON -> {
                if (hpPct < 0.5 && nearbyCount >= 2) 0.7 else 0.1
            }
        }
    }

    /**
     * 전술을 사용할 수 없는 이유를 반환합니다
     */
    private fun getUnavailableReason(tactic: Tactic): String {
        return when {
            isOnCooldown(tactic) -> "On cooldown (${tacticCooldowns[tactic]} ticks remaining)"
            wasRecentlyUsed(tactic) -> "Recently used (preventing repetition)"
            else -> "Unknown restriction"
        }
    }

    /**
     * 전술 변경 이유를 반환합니다
     */
    private fun getTacticChangeReason(features: FloatArray, newTactic: Tactic): String {
        val hpPct = features[0]
        val distance = features[1]
        val nearbyCount = features[2]

        return when (newTactic) {
            Tactic.IDLE -> "no immediate threats detected"
            Tactic.BURST_AOE -> "multiple targets in range (count: ${nearbyCount.toInt()}, distance: ${String.format("%.1f", distance)})"
            Tactic.KITE -> "low health detected (${String.format("%.1f%%", hpPct * 100)}, distance: ${String.format("%.1f", distance)})"
            Tactic.SUMMON -> "tactical advantage needed (health: ${String.format("%.1f%%", hpPct * 100)}, enemies: ${nearbyCount.toInt()})"
        }
    }

    /**
     * 전술 유지 이유를 반환합니다
     */
    private fun getMaintainReason(features: FloatArray, currentTactic: Tactic): String {
        return when (currentTactic) {
            Tactic.IDLE -> "no tactical change needed"
            Tactic.BURST_AOE -> "continuing area attack"
            Tactic.KITE -> "maintaining evasive maneuvers"
            Tactic.SUMMON -> "summoning process ongoing"
        }
    }

    /**
     * 현재 메모리 사용량을 MB 단위로 반환합니다
     */
    private fun getMemoryUsageMB(): Double {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0)
    }

    /**
     * Apply a new tactic by removing the old custom goal and inserting a
     * tactic‑specific goal into the goal selector.  The priority is set
     * relatively low so that vanilla goals still execute unless overridden.
     */
    private fun applyTactic(tactic: Tactic) {
        // Remove previous custom goal if present
        currentCustomGoal?.let { goalSelector.removeGoal(it) }
        currentCustomGoal = when (tactic) {
            Tactic.IDLE -> null
            Tactic.BURST_AOE -> BurstAoeGoal(this)
            Tactic.KITE -> KiteGoal(this)
            Tactic.SUMMON -> SummonGoal(this)
        }
        currentCustomGoal?.let { goalSelector.addGoal(1, it) }
    }

    /**
     * Extract features used by the tactics model. Enhanced with cooldown and history information.
     */
    private fun extractFeatures(): FloatArray {
        val hpPct = health / maxHealth
        val nearestPlayer = level().getNearestPlayer(this, 32.0)
        val distance = nearestPlayer?.distanceTo(this) ?: 32f
        val nearbyCount = level().players().count { it.distanceTo(this) < 10.0f }
        
        // Add cooldown information as features
        val burstAoeOnCooldown = if (isOnCooldown(Tactic.BURST_AOE)) 1f else 0f
        val kiteOnCooldown = if (isOnCooldown(Tactic.KITE)) 1f else 0f
        val summonOnCooldown = if (isOnCooldown(Tactic.SUMMON)) 1f else 0f
        
        return floatArrayOf(hpPct, distance, nearbyCount.toFloat(), burstAoeOnCooldown, kiteOnCooldown, summonOnCooldown)
    }
    
    /**
     * Updates all tactic cooldowns by decrementing their remaining time.
     */
    private fun updateCooldowns() {
        tacticCooldowns.entries.removeAll { entry ->
            entry.setValue(entry.value - 1)
            entry.value <= 0
        }
    }
    
    /**
     * Starts a cooldown for the given tactic.
     */
    private fun startCooldown(tactic: Tactic) {
        cooldownDurations[tactic]?.let { duration ->
            tacticCooldowns[tactic] = duration
        }
    }
    
    /**
     * Checks if a tactic is currently on cooldown.
     */
    private fun isOnCooldown(tactic: Tactic): Boolean {
        return tacticCooldowns.containsKey(tactic) && tacticCooldowns[tactic]!! > 0
    }
    
    /**
     * Adds a tactic to the recent history and maintains the maximum history size.
     */
    private fun addTacticToHistory(tactic: Tactic) {
        tacticHistory.add(tactic)
        if (tacticHistory.size > maxHistorySize) {
            tacticHistory.removeAt(0)
        }
    }
    
    /**
     * Selects a viable tactic considering cooldowns and recent usage patterns.
     * Falls back to alternative tactics if the suggested one is not available.
     */
    private fun selectViableTactic(suggestedTactic: Tactic): Tactic {
        // Check if suggested tactic is available
        if (!isOnCooldown(suggestedTactic) && !wasRecentlyUsed(suggestedTactic)) {
            return suggestedTactic
        }
        
        // Find an alternative tactic that's not on cooldown
        val availableTactics = Tactic.values().filter { tactic ->
            tactic != Tactic.IDLE && !isOnCooldown(tactic) && !wasRecentlyUsed(tactic)
        }
        
        // Return a random available tactic, or IDLE if none are available
        return availableTactics.randomOrNull() ?: Tactic.IDLE
    }
    
    /**
     * Checks if a tactic was used recently to prevent repetitive behavior.
     */
    private fun wasRecentlyUsed(tactic: Tactic): Boolean {
        return tacticHistory.takeLast(2).contains(tactic)
    }

    companion object {
        /**
         * Creates the attribute set for this boss.  Without registering these
         * attributes the entity would default to very weak stats.  See
         * NeoForge docs for details on living entity attributes【228222573136550†L72-L99】.
         */
        fun createAttributes(): AttributeSupplier.Builder {
            return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0)
        }
    }
}
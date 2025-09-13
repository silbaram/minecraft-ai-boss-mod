package com.example.bossai

import com.example.bossai.ai.BurstAoeGoal
import com.example.bossai.ai.KiteGoal
import com.example.bossai.ai.SummonGoal
import com.example.bossai.ai.Tactic
import com.example.bossai.ai.TacticsModel
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
                val features = extractFeatures()
                val suggestedTactic = tacticsModel.selectTactic(features)
                val newTactic = selectViableTactic(suggestedTactic)
                
                if (newTactic != currentTactic) {
                    logger.info("SentinelBoss switching tactics: {} -> {} (HP: {:.1f}, Distance: {:.1f})", 
                        currentTactic, newTactic, health, features[1])
                    currentTactic = newTactic
                    applyTactic(newTactic)
                    addTacticToHistory(newTactic)
                    startCooldown(newTactic)
                }
            }
        }
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
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
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level

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

    override fun registerGoals() {
        // Basic vanilla goals: float in water, wander randomly, look at nearby players.
        goalSelector.addGoal(0, FloatGoal(this))
        goalSelector.addGoal(7, RandomStrollGoal(this, 1.0))
        goalSelector.addGoal(8, LookAtPlayerGoal(this, Player::class.java, 8.0f))
    }

    override fun tick() {
        super.tick()
        if (!level().isClientSide) {
            tickCounter++
            // Evaluate tactics once every half second (10 ticks)
            if (tickCounter % 10 == 0) {
                val features = extractFeatures()
                val newTactic = tacticsModel.selectTactic(features)
                if (newTactic != currentTactic) {
                    currentTactic = newTactic
                    applyTactic(newTactic)
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
     * Extract features used by the tactics model.  These include the boss's
     * health percentage, the distance to the nearest player and the number of
     * players within a 10‑block radius.  Additional features can be added
     * here (e.g. cooldowns, recent damage) for more nuanced behaviours.
     */
    private fun extractFeatures(): FloatArray {
        val hpPct = health / maxHealth
        val nearestPlayer = level().getNearestPlayer(this, 32.0)
        val distance = nearestPlayer?.distanceTo(this) ?: 32f
        val nearbyCount = level().players().count { it.distanceTo(this) < 10.0f }
        return floatArrayOf(hpPct, distance, nearbyCount.toFloat())
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
package com.github.silbaram.bossai.ai

import com.github.silbaram.bossai.SentinelBossEntity
import com.mojang.logging.LogUtils
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.level.Level
import kotlin.random.Random

/**
 * A goal that summons zombie minions near the boss with a proper cooldown system.
 * The goal has a 15-second cooldown (300 ticks) between summon attempts and 
 * limits the total number of active minions to prevent overwhelming players.
 */
class SummonGoal(private val boss: SentinelBossEntity) : Goal() {
    private var cooldownTicks = 0
    private var executionTicks = 0
    private var isExecuting = false
    private var summonsThisExecution = 0
    private var activationTime = 0L
    private val logger = LogUtils.getLogger()

    companion object {
        private const val MAX_ACTIVE_MINIONS = 6 // Maximum minions that can be alive at once
        private const val COOLDOWN_DURATION = 300 // 15 seconds (20 ticks per second)
        private const val EXECUTION_DURATION = 60 // 3 seconds to complete summoning
        private const val SUMMON_RADIUS = 6.0
        private const val MINION_DETECTION_RANGE = 16.0
    }

    private val maxActiveMinions = MAX_ACTIVE_MINIONS
    private val cooldownDuration = COOLDOWN_DURATION
    private val executionDuration = EXECUTION_DURATION

    override fun canUse(): Boolean {
        val hasTarget = boss.target != null
        val cooldownReady = cooldownTicks <= 0
        val activeMinionCount = getActiveMinionCount()
        val canSummon = cooldownReady && hasTarget && activeMinionCount < maxActiveMinions

        if (canSummon) {
            logger.info("[SUMMON] Goal activation: target={}, active_minions={}/{}, cooldown_ready={}",
                boss.target?.name?.string ?: "null",
                activeMinionCount, maxActiveMinions, cooldownReady)
        }

        return canSummon
    }

    override fun start() {
        isExecuting = true
        executionTicks = executionDuration
        cooldownTicks = cooldownDuration
        summonsThisExecution = 0
        activationTime = System.currentTimeMillis()

        logger.info("[SUMMON] Starting summoning ritual: duration={}ticks, cooldown={}ticks",
            executionDuration, cooldownDuration)
    }

    override fun tick() {
        if (isExecuting) {
            executionTicks--
            // Summon minions gradually during execution
            if (executionTicks % 20 == 0 && executionTicks > 0) { // Every second
                summonMinion()
            }

            // Log progress every 20 ticks during execution
            if (executionTicks % 20 == 0) {
                logger.debug("[SUMMON] Execution progress: remaining_ticks={}, minions_summoned={}",
                    executionTicks, summonsThisExecution)
            }
        } else {
            // Count down cooldown when not executing
            if (cooldownTicks > 0) {
                cooldownTicks--
            }
        }
    }

    override fun canContinueToUse(): Boolean {
        return isExecuting && executionTicks > 0
    }

    override fun stop() {
        super.stop()
        val duration = System.currentTimeMillis() - activationTime
        val finalMinionCount = getActiveMinionCount()

        logger.info("[SUMMON] Summoning ritual completed: duration={}ms, minions_summoned={}, total_active={}",
            duration, summonsThisExecution, finalMinionCount)

        isExecuting = false
        executionTicks = 0
        summonsThisExecution = 0
    }

    override fun isInterruptable(): Boolean = false

    private fun summonMinion() {
        val level: Level = boss.level()
        val offsetX = (Random.nextDouble() - 0.5) * SUMMON_RADIUS
        val offsetZ = (Random.nextDouble() - 0.5) * SUMMON_RADIUS
        val spawnX = boss.x + offsetX
        val spawnY = boss.y
        val spawnZ = boss.z + offsetZ

        try {
            val zombie = Zombie(EntityType.ZOMBIE, level)
            zombie.setPos(spawnX, spawnY, spawnZ)
            // Set the boss as the minion's target to make them assist in combat
            zombie.target = boss.target

            val success = level.addFreshEntity(zombie)
            if (success) {
                summonsThisExecution++
                logger.debug("[SUMMON] Minion summoned #{}: position=({:.1f},{:.1f},{:.1f}), target={}",
                    summonsThisExecution, spawnX, spawnY, spawnZ,
                    zombie.target?.name?.string ?: "none")
            } else {
                logger.warn("[SUMMON] Failed to spawn minion at position ({:.1f},{:.1f},{:.1f})",
                    spawnX, spawnY, spawnZ)
            }
        } catch (e: Exception) {
            logger.error("[SUMMON] Error summoning minion: {}", e.message)
        }
    }

    private fun getActiveMinionCount(): Int {
        // Count nearby zombies within 16 blocks (assumed to be our minions)
        val nearbyZombies = boss.level().getEntitiesOfClass(
            Zombie::class.java,
            boss.boundingBox.inflate(MINION_DETECTION_RANGE)
        ) { it.isAlive }

        val count = nearbyZombies.size
        logger.debug("[SUMMON] Active minion count: {} within {:.1f} blocks", count, MINION_DETECTION_RANGE)

        return count
    }
}
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
            logger.info("👥 [SUMMON] Activate — target {} | minions {}/{} | cooldown ready",
                boss.target?.name?.string ?: "null", activeMinionCount, maxActiveMinions)
        }
        return canSummon
    }

    override fun start() {
        isExecuting = true
        executionTicks = executionDuration
        cooldownTicks = cooldownDuration
        summonsThisExecution = 0
        activationTime = System.currentTimeMillis()

        logger.info("👥 [SUMMON] Start — duration {}t | cooldown {}t", executionDuration, cooldownDuration)
        
        // 즉시 첫 번째 소환 시도 (tick 지연 방지)
        try {
            summonMinion()
        } catch (e: Exception) {
            logger.warn("👥 [SUMMON] Immediate summon failed: {}", e.message)
        }
    }

    override fun tick() {
        logger.debug("👥 [SUMMON] tick() — executing: {}, remaining: {}t", isExecuting, executionTicks)
        
        if (isExecuting) {
            executionTicks--

            // 20틱마다 추가 소환 (첫 소환은 start()에서 이미 완료)
            if (executionTicks > 0 && executionTicks % 20 == 0) {
                logger.debug("👥 [SUMMON] Periodic summon — remaining {}t", executionTicks)
                summonMinion()
            }

            if (executionTicks <= 0) {
                logger.debug("👥 [SUMMON] Execution finished")
            }
        } else {
            if (cooldownTicks > 0) {
                cooldownTicks--
            }
        }
    }

    override fun canContinueToUse(): Boolean {
        // 더 강력한 지속 조건: 실행 중에는 중단되지 않도록 보장
        val shouldContinue = isExecuting && executionTicks > 0
        if (!shouldContinue && isExecuting) {
            logger.debug("👥 [SUMMON] canContinueToUse() returning false — will stop")
        }
        return shouldContinue
    }

    override fun stop() {
        super.stop()
        val duration = System.currentTimeMillis() - activationTime
        val finalMinionCount = getActiveMinionCount()

        logger.info("✅ [SUMMON] Done — {}ms | summoned {} | total active {}",
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
            zombie.target = boss.target

            val success = level.addFreshEntity(zombie)
            if (success) {
                summonsThisExecution++
                // 좌표를 미리 포맷해서 SLF4J 플레이스홀더와 혼용 방지
                val xStr = String.format("%.1f", spawnX)
                val yStr = String.format("%.1f", spawnY)
                val zStr = String.format("%.1f", spawnZ)
                logger.info("👥 [SUMMON] Spawned #{} at ({}, {}, {}) | target: {}",
                    summonsThisExecution, xStr, yStr, zStr,
                    zombie.target?.name?.string ?: "none")
            } else {
                val xStr = String.format("%.1f", spawnX)
                val yStr = String.format("%.1f", spawnY)
                val zStr = String.format("%.1f", spawnZ)
                logger.warn("👥 [SUMMON] Spawn failed at ({}, {}, {}) — addFreshEntity returned false",
                    xStr, yStr, zStr)
            }
        } catch (e: Exception) {
            logger.error("👥 [SUMMON] Spawn error: {}", e.message)
        }
    }

    private fun getActiveMinionCount(): Int {
        val nearbyZombies = boss.level().getEntitiesOfClass(
            Zombie::class.java,
            boss.boundingBox.inflate(MINION_DETECTION_RANGE)
        ) { it.isAlive }

        val count = nearbyZombies.size
        logger.trace("👥 [SUMMON] Active minions: {} within {:.1f}m", count, MINION_DETECTION_RANGE)
        return count
    }
}
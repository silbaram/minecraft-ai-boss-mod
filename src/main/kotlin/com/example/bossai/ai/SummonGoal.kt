package com.github.silbaram.bossai.ai

import com.github.silbaram.bossai.SentinelBossEntity
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
    private val maxActiveMinions = 6 // Maximum minions that can be alive at once
    private val cooldownDuration = 300 // 15 seconds (20 ticks per second)
    private val executionDuration = 60 // 3 seconds to complete summoning

    override fun canUse(): Boolean {
        return cooldownTicks <= 0 && boss.target != null && getActiveMinionCount() < maxActiveMinions
    }

    override fun start() {
        isExecuting = true
        executionTicks = executionDuration
        cooldownTicks = cooldownDuration
    }

    override fun tick() {
        if (isExecuting) {
            executionTicks--
            // Summon minions gradually during execution
            if (executionTicks % 20 == 0 && executionTicks > 0) { // Every second
                summonMinion()
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
        isExecuting = false
        executionTicks = 0
    }

    override fun isInterruptable(): Boolean = false

    private fun summonMinion() {
        val level: Level = boss.level()
        val offsetX = (Random.nextDouble() - 0.5) * 6.0
        val offsetZ = (Random.nextDouble() - 0.5) * 6.0
        val spawnX = boss.x + offsetX
        val spawnY = boss.y
        val spawnZ = boss.z + offsetZ
        
        val zombie = Zombie(EntityType.ZOMBIE, level)
        zombie.setPos(spawnX, spawnY, spawnZ)
        // Set the boss as the minion's target to make them assist in combat
        zombie.target = boss.target
        level.addFreshEntity(zombie)
    }

    private fun getActiveMinionCount(): Int {
        // Count nearby zombies within 16 blocks (assumed to be our minions)
        val nearbyZombies = boss.level().getEntitiesOfClass(
            Zombie::class.java,
            boss.boundingBox.inflate(16.0)
        ) { it.isAlive }
        return nearbyZombies.size
    }
}
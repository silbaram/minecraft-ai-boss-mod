package com.example.bossai.ai

import com.example.bossai.SentinelBossEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.level.Level
import kotlin.random.Random

/**
 * A goal that summons a few zombie minions near the boss.  This goal has a
 * cooldown so that it does not run every tick.  When triggered, it spawns
 * three zombies at random offsets around the boss and then completes.
 */
class SummonGoal(private val boss: SentinelBossEntity) : Goal() {
    private var executed = false

    override fun canUse(): Boolean {
        // Only summon if no minions have been summoned recently and the boss
        // has a target.
        return !executed && boss.target != null
    }

    override fun start() {
        executed = true
        val level: Level = boss.level()
        for (i in 0 until 3) {
            val offsetX = (Random.nextDouble() - 0.5) * 4.0
            val offsetZ = (Random.nextDouble() - 0.5) * 4.0
            val spawnX = boss.x + offsetX
            val spawnY = boss.y
            val spawnZ = boss.z + offsetZ
            val zombie = Zombie(EntityType.ZOMBIE, level)
            zombie.setPos(spawnX, spawnY, spawnZ)
            level.addFreshEntity(zombie)
        }
    }

    override fun canContinueToUse(): Boolean = false

    override fun isInterruptable(): Boolean = false
}
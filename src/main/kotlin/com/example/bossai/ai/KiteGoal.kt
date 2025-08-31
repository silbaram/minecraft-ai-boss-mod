package com.example.bossai.ai

import com.example.bossai.SentinelBossEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

/**
 * A simple goal that causes the boss to move away from the nearest player when
 * its health is low.  This goal uses the entity's navigation system to path
 * towards a point opposite the player's position.  It completes once the
 * boss has travelled a short distance or regained health.
 */
class KiteGoal(private val boss: SentinelBossEntity) : Goal() {
    private var cooldownTicks = 0

    override fun canUse(): Boolean {
        // Activate only when health is below 40% and a player is nearby.
        val hpPct = boss.health / boss.maxHealth
        val player = boss.level().getNearestPlayer(boss, 16.0)
        return hpPct < 0.4f && player != null
    }

    override fun start() {
        cooldownTicks = 40 // approximate two seconds of retreat
        moveAwayFromNearestPlayer()
    }

    override fun tick() {
        // Periodically recompute retreat direction.
        if (cooldownTicks % 10 == 0) {
            moveAwayFromNearestPlayer()
        }
        cooldownTicks--
    }

    override fun canContinueToUse(): Boolean {
        return cooldownTicks > 0 && boss.health / boss.maxHealth < 0.6f
    }

    private fun moveAwayFromNearestPlayer() {
        val player: Player = boss.level().getNearestPlayer(boss, 16.0) ?: return
        // Compute a vector pointing from the player to the boss and extend it.
        val dx = boss.x - player.x
        val dz = boss.z - player.z
        val dir = Vec3(dx, 0.0, dz).normalize().scale(8.0)
        val targetPos = boss.position().add(dir)
        boss.navigation.moveTo(targetPos.x, targetPos.y, targetPos.z, 1.2)
    }
}
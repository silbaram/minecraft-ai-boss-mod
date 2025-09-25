package com.github.silbaram.bossai.ai

import com.github.silbaram.bossai.SentinelBossEntity
import com.mojang.logging.LogUtils
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
    private val logger = LogUtils.getLogger()
    private var activationTime = 0L

    companion object {
        private const val LOW_HEALTH_THRESHOLD = 0.4f
        private const val RECOVERY_THRESHOLD = 0.6f
        private const val RETREAT_DURATION_TICKS = 40
        private const val RETREAT_DISTANCE = 8.0
        private const val MOVEMENT_SPEED = 1.2
        private const val PLAYER_DETECTION_RANGE = 16.0
    }

    override fun canUse(): Boolean {
        // Activate only when health is below 40% and a player is nearby.
        val hpPct = boss.health / boss.maxHealth
        val player = boss.level().getNearestPlayer(boss, PLAYER_DETECTION_RANGE)
        val canActivate = hpPct < LOW_HEALTH_THRESHOLD && player != null

        if (canActivate) {
            logger.info("[KITE] Goal activation: HP={:.1f}%, nearest_player={}, distance={:.1f}",
                hpPct * 100,
                player?.name?.string ?: "null",
                player?.let { boss.distanceTo(it) } ?: -1f)
        }

        return canActivate
    }

    override fun start() {
        cooldownTicks = RETREAT_DURATION_TICKS
        activationTime = System.currentTimeMillis()
        val hpPct = boss.health / boss.maxHealth

        logger.info("[KITE] Starting kite maneuver: HP={:.1f}%, duration={}ticks",
            hpPct * 100, RETREAT_DURATION_TICKS)

        moveAwayFromNearestPlayer()
    }

    override fun tick() {
        // Periodically recompute retreat direction.
        if (cooldownTicks % 10 == 0) {
            moveAwayFromNearestPlayer()
        }
        cooldownTicks--

        // Log progress every 20 ticks (1 second)
        if (cooldownTicks % 20 == 0) {
            val hpPct = boss.health / boss.maxHealth
            val nearestPlayer = boss.level().getNearestPlayer(boss, PLAYER_DETECTION_RANGE)
            logger.debug("[KITE] Retreat progress: HP={:.1f}%, remaining_ticks={}, distance_to_player={:.1f}",
                hpPct * 100, cooldownTicks, nearestPlayer?.let { boss.distanceTo(it) } ?: -1f)
        }
    }

    override fun canContinueToUse(): Boolean {
        val hpPct = boss.health / boss.maxHealth
        val shouldContinue = cooldownTicks > 0 && hpPct < RECOVERY_THRESHOLD

        if (!shouldContinue && cooldownTicks <= 0) {
            logger.debug("[KITE] Stopping due to timer expiration: HP={:.1f}%", hpPct * 100)
        } else if (!shouldContinue && hpPct >= RECOVERY_THRESHOLD) {
            logger.info("[KITE] Stopping due to health recovery: HP={:.1f}%", hpPct * 100)
        }

        return shouldContinue
    }

    private fun moveAwayFromNearestPlayer() {
        val player: Player = boss.level().getNearestPlayer(boss, PLAYER_DETECTION_RANGE) ?: return
        val currentDistance = boss.distanceTo(player)

        // Compute a vector pointing from the player to the boss and extend it.
        val dx = boss.x - player.x
        val dz = boss.z - player.z
        val dir = Vec3(dx, 0.0, dz).normalize().scale(RETREAT_DISTANCE)
        val targetPos = boss.position().add(dir)

        val success = boss.navigation.moveTo(targetPos.x, targetPos.y, targetPos.z, MOVEMENT_SPEED)

        logger.debug("[KITE] Retreat movement: from=({:.1f},{:.1f}) to=({:.1f},{:.1f}), current_distance={:.1f}, navigation_success={}",
            boss.x, boss.z, targetPos.x, targetPos.z, currentDistance, success)
    }

    override fun stop() {
        super.stop()
        val duration = System.currentTimeMillis() - activationTime
        val finalHp = boss.health / boss.maxHealth

        logger.info("[KITE] Kite maneuver completed: duration={}ms, final_HP={:.1f}%",
            duration, finalHp * 100)
    }
}
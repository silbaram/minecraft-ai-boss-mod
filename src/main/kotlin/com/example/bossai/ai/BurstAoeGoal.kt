package com.example.bossai.ai

import com.example.bossai.SentinelBossEntity
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.EntitySelector
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.InteractionHand

/**
 * A simple goal that performs a one‑time area of effect attack.  When executed
 * it damages all nearby players within a small radius.  The goal finishes
 * immediately after the attack and must be re‑added to the goal selector for
 * subsequent use.
 */
class BurstAoeGoal(private val boss: SentinelBossEntity) : Goal() {
    private var executed = false

    override fun canUse(): Boolean {
        // Only run if the boss has a target within 6 blocks.
        val target = boss.target
        return target != null && boss.distanceTo(target) < 6.0f
    }

    override fun start() {
        executed = true
        
        // Create an axis aligned bounding box around the boss
        val area: AABB = boss.boundingBox.inflate(4.0) // Slightly larger area
        val players: List<Player> = boss.level().getEntitiesOfClass(Player::class.java, area, EntitySelector.NO_SPECTATORS)
        
        // Deal damage to each player with scaling based on distance
        for (player in players) {
            val distance = boss.distanceTo(player).coerceAtLeast(1f)
            val baseDamage = 8.0f
            val scaledDamage = (baseDamage * (4f / distance)).coerceIn(4f, baseDamage)
            
            val source = player.damageSources().mobAttack(boss)
            player.hurt(source, scaledDamage)
            
            // Add knockback effect
            val dx = player.x - boss.x
            val dz = player.z - boss.z
            val length = kotlin.math.sqrt(dx * dx + dz * dz).coerceAtLeast(0.1)
            player.knockback(0.5, dx / length, dz / length)
        }
        
        // Play swing animation for visual feedback
        boss.swing(InteractionHand.MAIN_HAND)
    }

    override fun canContinueToUse(): Boolean = false

    override fun isInterruptable(): Boolean = false
}
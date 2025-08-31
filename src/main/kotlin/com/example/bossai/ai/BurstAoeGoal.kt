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
        // Mark that the attack has occurred.
        executed = true
        // Create an axis aligned bounding box around the boss.
        val area: AABB = boss.boundingBox.inflate(3.0)
        val players: List<Player> = boss.level().getEntitiesOfClass(Player::class.java, area, EntitySelector.NO_SPECTATORS)
        // Deal damage to each player.  Use a generic mob attack damage source
        // as an example.  In a real mod you may want to use custom damage
        // types or apply potion effects.  We leverage the player's DamageSources
        // API to obtain a mob attack type【759699647327456†L118-L154】.
        for (player in players) {
            val source = player.damageSources().mobAttack(boss)
            player.hurt(source, 6.0f)
        }
        // Play a swing animation to give visual feedback.
        boss.swing(InteractionHand.MAIN_HAND)
    }

    override fun canContinueToUse(): Boolean = false

    override fun isInterruptable(): Boolean = false
}
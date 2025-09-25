package com.github.silbaram.bossai.ai

import com.github.silbaram.bossai.SentinelBossEntity
import com.mojang.logging.LogUtils
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.EntitySelector
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.InteractionHand

/**
 * 단일 실행 범위 공격을 수행하는 간단한 목표입니다.
 * 실행되면 작은 반경 내의 모든 플레이어에게 피해를 줍니다.
 * 공격 직후 목표는 즉시 종료되며, 이후 다시 사용하려면 목표 셀렉터에 재등록해야 합니다.
 */
class BurstAoeGoal(private val boss: SentinelBossEntity) : Goal() {
    private var executed = false
    private val logger = LogUtils.getLogger()

    companion object {
        private const val BURST_AOE_RADIUS = 4.0
        private const val BASE_DAMAGE = 8.0f
        private const val MAX_TARGET_DISTANCE = 6.0f
        private const val KNOCKBACK_STRENGTH = 0.5
    }

    /**
     * 보스의 번개/폭발 형태의 범위 공격(버스트 AOE)을 실행하는 함수입니다.
     *
     * 동작:
     * - 내부 쿨다운과 상태(예: 공격 준비, 대상 가시성, 사거리)를 확인하여 공격 가능 여부를 결정합니다.
     * - 유효한 대상들을 검색하여 각 대상에 피해, 넉백, 상태 이상 효과(예: 기절, 화상 등)를 적용합니다.
     * - 공격 시 시각/음향 효과(파티클, 사운드)를 생성하고, 필요 시 발사체나 넉백 충격파를 생성합니다.
     * - 공격 성공 시 보스의 쿨다운 및 애니메이션 상태를 갱신합니다.
     *
     * 부작용:
     * - 월드 상태(엔티티의 체력, 포지션 등)를 변경합니다.
     * - 서버 측에서만 호출되어야 하며(클라이언트 예측과 충돌 방지), 멀티스레드 환경에서는 메인 게임 스레드에서 실행되어야 합니다.
     *
     * 주의사항:
     * - 충돌 판정이나 피해 분배 방식(중복 적용, 친화성 체크 등)은 구현에 따라 달라질 수 있으므로 호출 전 정책을 확인하세요.
     * - 성능에 민감한 범위 검색 로직은 최적화(쿼드트리, 거리 제약 등)를 고려하세요.
     *
     * 반환값:
     * - 공격을 실제로 수행했다면 true, 조건 불충분으로 공격을 수행하지 않았다면 false를 반환합니다.
     */
    override fun canUse(): Boolean {
        // Only run if the boss has a target within 6 blocks.
        val target = boss.target
        val canActivate = target != null && boss.distanceTo(target) < MAX_TARGET_DISTANCE

        if (canActivate) {
            logger.debug("[BURST_AOE] Goal activation: target={}, distance={:.1f}", target?.name?.string ?: "null", target?.let { boss.distanceTo(it) } ?: -1f)
        }

        return canActivate
    }

    override fun start() {
        executed = true
        val executionStart = System.nanoTime()

        logger.info("[BURST_AOE] Executing burst AOE attack at position ({:.1f}, {:.1f}, {:.1f})", boss.x, boss.y, boss.z)

        // Create an axis aligned bounding box around the boss
        val area: AABB = boss.boundingBox.inflate(BURST_AOE_RADIUS)
        val players: List<Player> = boss.level().getEntitiesOfClass(Player::class.java, area, EntitySelector.NO_SPECTATORS)

        logger.debug("[BURST_AOE] Found {} players in radius {:.1f}", players.size, BURST_AOE_RADIUS)

        var totalDamageDealt = 0f
        var playersAffected = 0

        // Deal damage to each player with scaling based on distance
        for (player in players) {
            val distance = boss.distanceTo(player).coerceAtLeast(1f)
            val scaledDamage = (BASE_DAMAGE * (4f / distance)).coerceIn(4f, BASE_DAMAGE)

            val source = boss.damageSources().mobAttack(boss)
            val damageDealt = if (player.hurt(source, scaledDamage)) scaledDamage else 0f

            // Add knockback effect
            val dx = player.x - boss.x
            val dz = player.z - boss.z
            val length = kotlin.math.sqrt(dx * dx + dz * dz).coerceAtLeast(0.1)
            player.knockback(KNOCKBACK_STRENGTH, dx / length, dz / length)

            totalDamageDealt += damageDealt
            playersAffected++

            logger.debug("[BURST_AOE] Hit player {} at distance {:.1f}, damage={:.1f}",
                player.name.string, distance, damageDealt)
        }

        // Play swing animation for visual feedback
        boss.swing(InteractionHand.MAIN_HAND)

        val executionTime = (System.nanoTime() - executionStart) / 1_000_000.0
        logger.info("[BURST_AOE] Attack completed: {} players affected, {:.1f} total damage, {:.1f}ms execution time",
            playersAffected, totalDamageDealt, executionTime)
    }

    override fun canContinueToUse(): Boolean = false

    override fun isInterruptable(): Boolean = false

    override fun stop() {
        super.stop()
        logger.debug("[BURST_AOE] Goal stopped, executed={}", executed)
    }
}
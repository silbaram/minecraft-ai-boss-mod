package com.github.silbaram.bossai.client

import com.github.silbaram.bossai.BossAiMod
import com.github.silbaram.bossai.ModEntities
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.EntityRenderersEvent

/**
 * 클라이언트 전용 등록(엔티티 렌더러 등).
 * KotlinForForge와 함께 사용 시 MOD 버스 사용
 */
@EventBusSubscriber(modid = BossAiMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object BossAiClient {
    @SubscribeEvent
    fun onRegisterRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerEntityRenderer(ModEntities.SENTINEL_BOSS.get(), ::SentinelBossRenderer)
    }
}


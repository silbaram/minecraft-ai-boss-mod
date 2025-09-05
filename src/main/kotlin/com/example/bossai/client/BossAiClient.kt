package com.example.bossai.client

import com.example.bossai.BossAiMod
import com.example.bossai.ModEntities
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.EntityRenderersEvent

/**
 * 클라이언트 전용 등록(엔티티 렌더러 등).
 */
@EventBusSubscriber(modid = BossAiMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object BossAiClient {
    @SubscribeEvent
    fun onRegisterRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerEntityRenderer(ModEntities.SENTINEL_BOSS.get(), ::SentinelBossRenderer)
    }
}


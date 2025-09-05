package com.example.bossai

import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent

@Mod(BossAiMod.MOD_ID)
class BossAiMod(modBus: IEventBus) {
    init {
        ModEntities.ENTITY_TYPES.register(modBus)
        ModItems.ITEMS.register(modBus)
        modBus.addListener(::onEntityAttributeCreation)
        NeoForge.EVENT_BUS.register(this)
    }

    private fun onEntityAttributeCreation(event: EntityAttributeCreationEvent) {
        event.put(
            ModEntities.SENTINEL_BOSS.get(),
            SentinelBossEntity.createAttributes().build()
        )
    }

    companion object {
        const val MOD_ID = "boss_ai"
    }
}

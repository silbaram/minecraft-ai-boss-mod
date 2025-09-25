package com.github.silbaram.bossai

import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(BossAiMod.MOD_ID)
class BossAiMod {
    init {
        ModEntities.ENTITY_TYPES.register(MOD_BUS)
        ModItems.ITEMS.register(MOD_BUS)
        MOD_BUS.addListener(::onEntityAttributeCreation)
        // No need to register this class since it has no @SubscribeEvent methods
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

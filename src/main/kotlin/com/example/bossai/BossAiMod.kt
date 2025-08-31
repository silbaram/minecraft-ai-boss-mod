package com.example.bossai

import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.common.NeoForge

/**
 * Main mod class for the Boss AI mod.  The `@Mod` annotation marks this
 * class as the mod entry point.  The mod ID must match the value in
 * neoforge.mods.toml and gradle.properties【840203598704741†L118-L126】.
 */
@Mod(BossAiMod.MOD_ID)
class BossAiMod(modBus: IEventBus) {
    init {
        // Register entity and other registries on the mod event bus.  See
        // Entities documentation for details on DeferredRegister usage【850544949770670†L90-L113】.
        ModEntities.ENTITY_TYPES.register(modBus)

        // Register attribute creation handler.  This uses a method on
        // ModEntities to populate attributes for our custom entity.
        modBus.addListener(::onEntityAttributeCreation)

        // Register ourselves with the global NeoForge bus if needed for
        // additional vanilla events.
        NeoForge.EVENT_BUS.register(this)
    }

    private fun onEntityAttributeCreation(event: EntityAttributeCreationEvent) {
        // Attach attributes to our SentinelBoss entity.  Without this, the
        // entity would default to very low health and damage values.
        event.put(ModEntities.SENTINEL_BOSS.get(),
            SentinelBossEntity.createAttributes().build())
    }

    companion object {
        /** The mod ID used throughout registration. */
        const val MOD_ID = "boss_ai"
    }
}
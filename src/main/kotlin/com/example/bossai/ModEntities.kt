package com.github.silbaram.bossai

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.registries.BuiltInRegistries
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.DeferredHolder
import java.util.function.Supplier

/**
 * Holds the registry objects for our custom entities.  Entities must be
 * registered via a [`DeferredRegister`][DeferredRegister] to ensure they
 * are available at the correct time in the mod lifecycle【850544949770670†L90-L113】.
 */
object ModEntities {
    // A DeferredRegister for all entity types belonging to this mod.  The
    // namespace (mod ID) is supplied via the mod ID constant.
    val ENTITY_TYPES: DeferredRegister<EntityType<*>> =
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, BossAiMod.MOD_ID)

    /**
     * Registration entry for our Sentinel Boss entity.  The `registerEntityType`
     * shorthand creates the EntityType and registers it under the given name
     * 【850544949770670†L174-L182】.
     */
    val SENTINEL_BOSS: DeferredHolder<EntityType<*>, EntityType<SentinelBossEntity>> =
        ENTITY_TYPES.register("sentinel_boss", Supplier {
            EntityType.Builder.of(::SentinelBossEntity, MobCategory.MONSTER)
                .sized(1.0f, 3.0f)
                .clientTrackingRange(8)
                .build("sentinel_boss")
        })
}
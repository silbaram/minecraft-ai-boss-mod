package com.github.silbaram.bossai

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.registries.BuiltInRegistries
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.DeferredHolder
import java.util.function.Supplier


/**
 * 커스텀 엔티티들의 레지스트리 객체를 보관합니다. 엔티티는 모드 라이프사이클의
 * 올바른 시점에 사용 가능하도록 [`DeferredRegister`][DeferredRegister]를 통해
 * 등록되어야 합니다.
 */
object ModEntities {
    // 이 모드에 속하는 모든 엔티티 타입을 위한 DeferredRegister입니다.
    // 네임스페이스(모드 ID)는 BossAiMod.MOD_ID 상수를 통해 제공됩니다.
    val ENTITY_TYPES: DeferredRegister<EntityType<*>> =
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, BossAiMod.MOD_ID)

    /**
     * Sentinel 보스 엔티티의 등록 항목입니다. `registerEntityType` 약식은
     * EntityType을 생성하고 주어진 이름으로 등록합니다.
     */
    val SENTINEL_BOSS: DeferredHolder<EntityType<*>, EntityType<SentinelBossEntity>> =
        ENTITY_TYPES.register("sentinel_boss", Supplier {
            EntityType.Builder.of(::SentinelBossEntity, MobCategory.MONSTER)
                .sized(1.0f, 3.0f)
                .clientTrackingRange(8)
                .build("sentinel_boss")
        })
}
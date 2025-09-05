package com.example.bossai

import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.SpawnEggItem
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.EntityType
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.minecraft.core.registries.BuiltInRegistries
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import java.util.function.Supplier

/**
 * 아이템 등록. 센티넬 보스 스폰 알을 추가합니다.
 */
object ModItems {
    val ITEMS: DeferredRegister<Item> =
        DeferredRegister.create(BuiltInRegistries.ITEM, BossAiMod.MOD_ID)

    val SENTINEL_BOSS_SPAWN_EGG: DeferredHolder<Item, Item> =
        ITEMS.register("sentinel_boss_spawn_egg", Supplier {
            SpawnEggItem(
                ModEntities.SENTINEL_BOSS.get() as EntityType<out Mob>,
                0x556677, // 바탕색
                0xCC9933, // 점색
                Item.Properties()
            )
        })
}

/**
 * 크리에이티브 탭에 스폰 알을 노출합니다.
 */
@EventBusSubscriber(modid = BossAiMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
object CreativeTabEvents {
    @SubscribeEvent
    fun onBuildCreativeTab(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.SENTINEL_BOSS_SPAWN_EGG.get())
        }
    }
}

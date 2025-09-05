package com.example.bossai.client

import com.example.bossai.SentinelBossEntity
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.resources.ResourceLocation

/**
 * 간단한 플레이스홀더 렌더러: 좀비 모델/레이어를 재사용합니다.
 * 추후 전용 모델/텍스처로 교체할 수 있습니다.
 */
class SentinelBossRenderer(ctx: EntityRendererProvider.Context) :
    MobRenderer<SentinelBossEntity, HumanoidModel<SentinelBossEntity>>(
        ctx, HumanoidModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.7f
    ) {

    private val texture = ResourceLocation.fromNamespaceAndPath("boss_ai", "textures/entity/sentinel_boss.png")

    override fun getTextureLocation(entity: SentinelBossEntity): ResourceLocation = texture
}

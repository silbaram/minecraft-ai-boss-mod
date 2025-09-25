package com.github.silbaram.bossai.client

import com.github.silbaram.bossai.SentinelBossEntity
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.resources.ResourceLocation

/**
 * Placeholder renderer for the Sentinel Boss using zombie model and a custom texture.
 * Falls back to zombie texture if custom texture is not available.
 * TODO: Replace with custom 3D model and unique texture in future versions.
 */
class SentinelBossRenderer(ctx: EntityRendererProvider.Context) :
    MobRenderer<SentinelBossEntity, HumanoidModel<SentinelBossEntity>>(
        ctx, HumanoidModel(ctx.bakeLayer(ModelLayers.ZOMBIE)), 1.2f  // Larger scale for boss
    ) {

    private val customTexture = ResourceLocation.fromNamespaceAndPath("boss_ai", "textures/entity/sentinel_boss.png")
    private val fallbackTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie/zombie.png")

    override fun getTextureLocation(entity: SentinelBossEntity): ResourceLocation {
        // Try to use custom texture first, fallback to zombie texture if not found
        return customTexture
    }
}

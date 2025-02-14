package io.github.lucunji.uusiaurinko.client.render.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import io.github.lucunji.uusiaurinko.client.render.ModRenderTypes;
import io.github.lucunji.uusiaurinko.client.render.entity.model.NewSunModel;
import io.github.lucunji.uusiaurinko.config.ClientConfigs;
import io.github.lucunji.uusiaurinko.entity.NewSunEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.layers.EndermanEyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

import static io.github.lucunji.uusiaurinko.UusiAurinko.MODID;

public class NewSunEntityRenderer extends EntityRenderer<NewSunEntity> {
    /* A scratch for setting sun's entity data:
    /data merge entity @e[type=uusi-aurinko:new_sun, limit=1] {}
    /data get entity @e[type=uusi-aurinko:new_sun, limit=1]
     */
    private static final ResourceLocation SUN_DARK_BLUE_TEXTURE = new ResourceLocation(MODID, "textures/entity/sun_dark_blue.png");

    private final NewSunModel model;

    public NewSunEntityRenderer(EntityRendererManager renderManager) {
        super(renderManager);
        this.model = new NewSunModel();
    }

    @Override
    public void render(NewSunEntity entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        matrixStackIn.push();

        float size = entityIn.getRenderingSize();
        float hitboxSize = entityIn.getBoundingBoxSize();
        matrixStackIn.translate(0, hitboxSize / 2D, 0);
        matrixStackIn.scale(size, size, size);

        float t = (entityIn.world.getGameTime() + partialTicks) / 2;
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(t));
        matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(t + 71));

        final NewSunEntity.SunState sunState = entityIn.getSunState();
        // render the sun itself
        RenderType renderType = ModRenderTypes.getSun(this.getEntityTexture(entityIn));

        packedLightIn = LightTexture.packLight(15, 15);

        model.render(matrixStackIn, bufferIn.getBuffer(renderType), packedLightIn, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

        // negate the size to use the feature of culling: only show the surfaces on the back
        matrixStackIn.scale(-1, 1, 1);

        // render the sun's halo
        renderType = ModRenderTypes.getHalo(sunState == NewSunEntity.SunState.FULL_DARK ? SUN_DARK_BLUE_TEXTURE : sunState.texture);
        final int haloIter = ClientConfigs.INSTANCE.SUN_HALO_ITERATIONS.get();

        packedLightIn = packedLightIn / 2;

        for (int i = 0; i < haloIter; i++) {
            matrixStackIn.push();
            // maps i ∈ [0, iters) to iit ∈ [0, 1)
            float iit = (float) i / haloIter;
            // maps i ∈ [0, iters) to scale ∈ [1, 2)
            float scale = 1 + iit;
            matrixStackIn.scale(scale, scale, scale);
            // alpha = (0.24 - 0.225 * iit) / (iters / 10)
            // maps i ∈ [0, iters) to ɑ ∈ [0.24/iters, 0.015/iters)
            // the integration of ɑ over the range [0, iter) is approximately constant
            float alpha = (2.4F - 2.25F * iit) / haloIter;

            model.render(matrixStackIn, bufferIn.getBuffer(renderType),
                    packedLightIn, OverlayTexture.NO_OVERLAY, 1, 1, 1, alpha);
            matrixStackIn.pop();
        }

        matrixStackIn.pop();
    }

    @Override
    public ResourceLocation getEntityTexture(NewSunEntity entity) {
        return entity.getSunState() == NewSunEntity.SunState.GROWING ? entity.getLastConsumedStone().texture : entity.getSunState().texture;
    }
}

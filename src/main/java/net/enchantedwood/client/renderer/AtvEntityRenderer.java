package net.enchantedwood.client.renderer;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.entity.custom.AtvEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class AtvEntityRenderer extends EntityRenderer<AtvEntity, AtvRenderState> {
    private static final Identifier TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/entity/atv/atv.png");
    private final AtvEntityModel model;

    public AtvEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.model = new AtvEntityModel(ctx.getPart(AtvEntityModel.MODEL_LAYER));
        this.shadowRadius = 0.8F;
    }

    @Override
    public AtvRenderState createRenderState() {
        return new AtvRenderState();
    }

    @Override
    public void updateRenderState(AtvEntity entity, AtvRenderState state, float tickProgress) {
        super.updateRenderState(entity, state, tickProgress);
        state.yaw = entity.getYaw(tickProgress);
        state.pitch = entity.getPitch(tickProgress);
        state.wheelRotation = entity.wheelRotation;
        state.attachmentType = entity.getAttachmentType();
        state.toolSpin = entity.toolSpin;
        state.hasDrill = entity.hasDrillBit();
        state.drillSpin = entity.drillSpin;
        state.headlightsActive = entity.areHeadlightsActive();
        state.lightmapCoordinates = 0x00F000F0;
    }

    @Override
    public void render(AtvRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        matrices.push();

        // Orient vehicle
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - state.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-state.pitch));
        matrices.scale(-1.0F, -1.0F, 1.0F);
        matrices.translate(0.0, -1.5, 0.0);

        RenderLayer layer = RenderLayers.entityCutoutNoCull(TEXTURE);
        queue.submitModel(this.model, state, matrices, layer, state.lightmapCoordinates, OverlayTexture.DEFAULT_UV, -1, null, 0, state.crumblingOverlay);

        matrices.pop();
        super.render(state, matrices, queue, cameraState);
    }
}

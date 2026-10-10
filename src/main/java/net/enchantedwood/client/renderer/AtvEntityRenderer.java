package net.enchantedwood.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.entity.custom.AtvEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class AtvEntityRenderer extends EntityRenderer<AtvEntity, AtvRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/entity/atv/atv.png");
    private final AtvEntityModel model;

    public AtvEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new AtvEntityModel(ctx.bakeLayer(AtvEntityModel.MODEL_LAYER));
        this.shadowRadius = 0.8F;
    }

    @Override
    public AtvRenderState createRenderState() {
        return new AtvRenderState();
    }

    @Override
    public void extractRenderState(AtvEntity entity, AtvRenderState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.yaw = entity.getViewYRot(tickProgress);
        state.pitch = entity.getViewXRot(tickProgress);
        state.wheelRotation = entity.wheelRotation;
        state.attachmentType = entity.getAttachmentType();
        state.toolSpin = entity.toolSpin;
        state.hasDrill = entity.hasDrillBit();
        state.drillSpin = entity.drillSpin;
        state.headlightsActive = entity.areHeadlightsActive();
        state.lightmapCoordinates = 0x00F000F0;
    }

    @Override
    public void submit(AtvRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        matrices.pushPose();

        // Orient vehicle
        matrices.rotateDegrees(Axis.YP, 180.0F - state.yaw);
        matrices.rotateDegrees(Axis.XP, -state.pitch);
        matrices.scale(-1.0F, -1.0F, 1.0F);
        matrices.translate(0.0, -1.5, 0.0);

        RenderType layer = RenderTypes.entityCutout(TEXTURE, false);
        queue.submitModel(this.model, state, matrices, layer, state.lightmapCoordinates, OverlayTexture.NO_OVERLAY, -1);
        if (state.crumblingOverlay != null) queue.submitCrumblingOverlay(this.model, state, matrices, layer, state.lightmapCoordinates, OverlayTexture.NO_OVERLAY, -1, state.crumblingOverlay);

        matrices.popPose();
        super.submit(state, matrices, queue, cameraState);
    }
}

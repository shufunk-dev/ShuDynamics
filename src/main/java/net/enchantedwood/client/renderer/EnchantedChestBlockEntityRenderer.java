package net.enchantedwood.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.EnchantedChestBlock;
import net.enchantedwood.block.entity.EnchantedChestBlockEntity;

@Environment(EnvType.CLIENT)
public class EnchantedChestBlockEntityRenderer implements BlockEntityRenderer<EnchantedChestBlockEntity, EnchantedChestRenderState> {
    private final ChestModel chestModel;

    public EnchantedChestBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.chestModel = new ChestModel(ctx.bakeLayer(ModelLayers.CHEST));
    }

    @Override
    public EnchantedChestRenderState createRenderState() {
        return new EnchantedChestRenderState();
    }

    @Override
    public void extractRenderState(EnchantedChestBlockEntity entity, EnchantedChestRenderState state, float tickDelta, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlayCommand) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlayCommand);
        BlockState blockState = entity.getBlockState();
        if (blockState.hasProperty(EnchantedChestBlock.FACING)) {
            state.facing = blockState.getValue(EnchantedChestBlock.FACING);
        } else {
            state.facing = Direction.NORTH;
        }
        state.gearTier = entity.getGearTier();
        if ((state.gearTier == null || state.gearTier == GearTier.NONE) && blockState.hasProperty(EnchantedChestBlock.GEAR_TIER)) {
            state.gearTier = blockState.getValue(EnchantedChestBlock.GEAR_TIER);
        }
        state.lidProgress = entity.getOpenNess(tickDelta);
    }

    @Override
    public void submit(EnchantedChestRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        matrices.pushPose();

        Direction direction = state.facing != null ? state.facing : Direction.NORTH;
        float rotation = direction.toYRot();
        matrices.translate(0.5D, 0.5D, 0.5D);
        matrices.rotateDegrees(Axis.YP, -rotation);
        matrices.translate(-0.5D, -0.5D, -0.5D);
        float progress = state.lidProgress;

        Identifier texture = getTextureForTier(state.gearTier);
        RenderType layer = RenderTypes.entityCutout(texture, false);

        queue.submitModel(this.chestModel, Float.valueOf(progress), matrices, layer, state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
        if (state.breakProgress != null) queue.submitCrumblingOverlay(this.chestModel, Float.valueOf(progress), matrices, layer, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, state.breakProgress);

        matrices.popPose();
    }

    private Identifier getTextureForTier(GearTier tier) {
        if (tier == null || tier == GearTier.NONE) {
            return Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/entity/chest/enchanted_chest_none.png");
        }
        if (tier == GearTier.IRON || tier == GearTier.ENCHANTED_IRON) {
            return Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/entity/chest/enchanted_chest_enchanted_iron.png");
        }
        if (tier == GearTier.ALUMINUM || tier == GearTier.STEEL) {
            return Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/entity/chest/enchanted_chest_enchanted_iron.png");
        }
        return Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/entity/chest/enchanted_chest_" + tier.getSerializedName() + ".png");
    }
}



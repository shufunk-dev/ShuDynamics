package net.enchantedwood.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.model.ChestBlockModel;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.EnchantedChestBlock;
import net.enchantedwood.block.entity.EnchantedChestBlockEntity;

@Environment(EnvType.CLIENT)
public class EnchantedChestBlockEntityRenderer implements BlockEntityRenderer<EnchantedChestBlockEntity, EnchantedChestRenderState> {
    private final ChestBlockModel chestModel;

    public EnchantedChestBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.chestModel = new ChestBlockModel(ctx.getLayerModelPart(EntityModelLayers.CHEST));
    }

    @Override
    public EnchantedChestRenderState createRenderState() {
        return new EnchantedChestRenderState();
    }

    @Override
    public void updateRenderState(EnchantedChestBlockEntity entity, EnchantedChestRenderState state, float tickDelta, Vec3d cameraPos, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlayCommand) {
        BlockEntityRenderState.updateBlockEntityRenderState(entity, state, crumblingOverlayCommand);
        BlockState blockState = entity.getCachedState();
        if (blockState.contains(EnchantedChestBlock.FACING)) {
            state.facing = blockState.get(EnchantedChestBlock.FACING);
        } else {
            state.facing = Direction.NORTH;
        }
        state.gearTier = entity.getGearTier();
        if ((state.gearTier == null || state.gearTier == GearTier.NONE) && blockState.contains(EnchantedChestBlock.GEAR_TIER)) {
            state.gearTier = blockState.get(EnchantedChestBlock.GEAR_TIER);
        }
        state.lidProgress = entity.getAnimationProgress(tickDelta);
    }

    @Override
    public void render(EnchantedChestRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        matrices.push();

        Direction direction = state.facing != null ? state.facing : Direction.NORTH;
        float rotation = direction.getPositiveHorizontalDegrees();
        matrices.translate(0.5D, 0.5D, 0.5D);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rotation));
        matrices.translate(-0.5D, -0.5D, -0.5D);
        float progress = state.lidProgress;

        Identifier texture = getTextureForTier(state.gearTier);
        RenderLayer layer = RenderLayers.entityCutoutNoCull(texture);

        queue.submitModel(this.chestModel, Float.valueOf(progress), matrices, layer, state.lightmapCoordinates, OverlayTexture.DEFAULT_UV, -1, null, 0, state.crumblingOverlay);

        matrices.pop();
    }

    private Identifier getTextureForTier(GearTier tier) {
        if (tier == null || tier == GearTier.NONE) {
            return Identifier.of(EnchantedWoodMod.MOD_ID, "textures/entity/chest/enchanted_chest_none.png");
        }
        if (tier == GearTier.IRON || tier == GearTier.ENCHANTED_IRON) {
            return Identifier.of(EnchantedWoodMod.MOD_ID, "textures/entity/chest/enchanted_chest_enchanted_iron.png");
        }
        if (tier == GearTier.ALUMINUM || tier == GearTier.STEEL) {
            return Identifier.of(EnchantedWoodMod.MOD_ID, "textures/entity/chest/enchanted_chest_enchanted_iron.png");
        }
        return Identifier.of(EnchantedWoodMod.MOD_ID, "textures/entity/chest/enchanted_chest_" + tier.asString() + ".png");
    }
}



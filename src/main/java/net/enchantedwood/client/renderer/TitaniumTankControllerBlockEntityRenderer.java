package net.enchantedwood.client.renderer;

import net.enchantedwood.block.entity.TitaniumTankControllerBlockEntity;
import net.enchantedwood.fluid.MoltenMetal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class TitaniumTankControllerBlockEntityRenderer implements BlockEntityRenderer<TitaniumTankControllerBlockEntity, TitaniumTankRenderState> {
    private final Sprite waterSprite;
    private final Sprite lavaSprite;

    public TitaniumTankControllerBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.waterSprite = ctx.spriteHolder().getSprite(TexturedRenderLayers.BLOCK_SPRITE_MAPPER.mapVanilla("water_still"));
        this.lavaSprite = ctx.spriteHolder().getSprite(TexturedRenderLayers.BLOCK_SPRITE_MAPPER.mapVanilla("lava_still"));
    }

    @Override
    public TitaniumTankRenderState createRenderState() {
        return new TitaniumTankRenderState();
    }

    @Override
    public void updateRenderState(TitaniumTankControllerBlockEntity entity, TitaniumTankRenderState state, float tickDelta, Vec3d cameraPos, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlayCommand) {
        BlockEntityRenderState.updateBlockEntityRenderState(entity, state, crumblingOverlayCommand);
        state.isFormed = entity.isFormed();
        state.lavaAmount = entity.getStoredFluidAmount();
        state.currentFluid = entity.getFluidType();
        state.minPos = entity.getMinPos();
    }

    @Override
    public boolean rendersOutsideBoundingBox() {
        return true;
    }

    @Override
    public void render(TitaniumTankRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        if (!state.isFormed || state.lavaAmount <= 0 || state.currentFluid == null || state.currentFluid == MoltenMetal.NONE) {
            return;
        }

        float fillFraction = (float) state.lavaAmount / 500_000.0f;
        fillFraction = Math.max(0.0f, Math.min(1.0f, fillFraction));
        float fluidHeight = Math.max(0.04f, 3.0f * fillFraction);
        float yBottom = -3.0f + 0.002f;
        float yTop = yBottom + fluidHeight;

        Sprite sprite = (state.currentFluid == MoltenMetal.LAVA) ? this.lavaSprite : this.waterSprite;
        int color = (state.currentFluid == MoltenMetal.LAVA) ? 0xFFFFFFFF : state.currentFluid.getColor();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = 250;

        queue.submitCustom(matrices, TexturedRenderLayers.getBlockTranslucentCull(), (entry, vertexConsumer) -> {
            renderFluidInterior(entry, vertexConsumer, sprite, r, g, b, a, yBottom, yTop);
        });
    }

    private void renderFluidInterior(MatrixStack.Entry entry, VertexConsumer vertexConsumer,
                                     Sprite sprite, int r, int g, int b, int a,
                                     float yBottom, float yTop) {
        float minU = sprite.getMinU();
        float maxU = sprite.getMaxU();
        float minV = sprite.getMinV();
        float maxV = sprite.getMaxV();

        // 1. Top Face & Underside (3x3 grid)
        for (int bx = 0; bx < 3; bx++) {
            float x0 = -1.0f + bx + (bx == 0 ? 0.002f : 0.0f);
            float x1 = -1.0f + bx + 1.0f - (bx == 2 ? 0.002f : 0.0f);

            for (int bz = 0; bz < 3; bz++) {
                float z0 = -1.0f + bz + (bz == 0 ? 0.002f : 0.0f);
                float z1 = -1.0f + bz + 1.0f - (bz == 2 ? 0.002f : 0.0f);

                // Top Face (Normal +Y: 0, 1, 0)
                vertex(entry, vertexConsumer, x0, yTop, z0, minU, minV, r, g, b, a, 0, 1, 0);
                vertex(entry, vertexConsumer, x0, yTop, z1, minU, maxV, r, g, b, a, 0, 1, 0);
                vertex(entry, vertexConsumer, x1, yTop, z1, maxU, maxV, r, g, b, a, 0, 1, 0);
                vertex(entry, vertexConsumer, x1, yTop, z0, maxU, minV, r, g, b, a, 0, 1, 0);

                // Underside (Normal -Y: 0, -1, 0)
                vertex(entry, vertexConsumer, x0, yTop, z0, minU, minV, r, g, b, a, 0, -1, 0);
                vertex(entry, vertexConsumer, x1, yTop, z0, maxU, minV, r, g, b, a, 0, -1, 0);
                vertex(entry, vertexConsumer, x1, yTop, z1, maxU, maxV, r, g, b, a, 0, -1, 0);
                vertex(entry, vertexConsumer, x0, yTop, z1, minU, maxV, r, g, b, a, 0, -1, 0);

                // Bottom Floor Face (Normal -Y: 0, -1, 0)
                vertex(entry, vertexConsumer, x0, yBottom, z0, minU, minV, r, g, b, a, 0, -1, 0);
                vertex(entry, vertexConsumer, x1, yBottom, z0, maxU, minV, r, g, b, a, 0, -1, 0);
                vertex(entry, vertexConsumer, x1, yBottom, z1, maxU, maxV, r, g, b, a, 0, -1, 0);
                vertex(entry, vertexConsumer, x0, yBottom, z1, minU, maxV, r, g, b, a, 0, -1, 0);
            }
        }

        // 2. Side Walls (North, South, West, East)
        for (int by = 0; by < 3; by++) {
            float segBottom = yBottom + by * 1.0f;
            if (segBottom >= yTop) break;
            float segTop = Math.min(yTop, segBottom + 1.0f);
            float segHeight = segTop - segBottom;
            float segMaxV = minV + (maxV - minV) * segHeight;

            // North Wall (Z = -1.0f)
            float northZ = -1.0f + 0.002f;
            for (int bx = 0; bx < 3; bx++) {
                float x0 = -1.0f + bx + (bx == 0 ? 0.002f : 0.0f);
                float x1 = -1.0f + bx + 1.0f - (bx == 2 ? 0.002f : 0.0f);

                // North Outside Face (Normal: 0, 0, -1)
                vertex(entry, vertexConsumer, x0, segBottom, northZ, minU, segMaxV, r, g, b, a, 0, 0, -1);
                vertex(entry, vertexConsumer, x0, segTop,    northZ, minU, minV,    r, g, b, a, 0, 0, -1);
                vertex(entry, vertexConsumer, x1, segTop,    northZ, maxU, minV,    r, g, b, a, 0, 0, -1);
                vertex(entry, vertexConsumer, x1, segBottom, northZ, maxU, segMaxV, r, g, b, a, 0, 0, -1);

                // North Inside Face (Normal: 0, 0, 1)
                vertex(entry, vertexConsumer, x1, segBottom, northZ, maxU, segMaxV, r, g, b, a, 0, 0, 1);
                vertex(entry, vertexConsumer, x1, segTop,    northZ, maxU, minV,    r, g, b, a, 0, 0, 1);
                vertex(entry, vertexConsumer, x0, segTop,    northZ, minU, minV,    r, g, b, a, 0, 0, 1);
                vertex(entry, vertexConsumer, x0, segBottom, northZ, minU, segMaxV, r, g, b, a, 0, 0, 1);
            }

            // South Wall (Z = +2.0f)
            float southZ = 2.0f - 0.002f;
            for (int bx = 0; bx < 3; bx++) {
                float x0 = -1.0f + bx + (bx == 0 ? 0.002f : 0.0f);
                float x1 = -1.0f + bx + 1.0f - (bx == 2 ? 0.002f : 0.0f);

                // South Outside Face (Normal: 0, 0, 1)
                vertex(entry, vertexConsumer, x1, segBottom, southZ, minU, segMaxV, r, g, b, a, 0, 0, 1);
                vertex(entry, vertexConsumer, x1, segTop,    southZ, minU, minV,    r, g, b, a, 0, 0, 1);
                vertex(entry, vertexConsumer, x0, segTop,    southZ, maxU, minV,    r, g, b, a, 0, 0, 1);
                vertex(entry, vertexConsumer, x0, segBottom, southZ, maxU, segMaxV, r, g, b, a, 0, 0, 1);

                // South Inside Face (Normal: 0, 0, -1)
                vertex(entry, vertexConsumer, x0, segBottom, southZ, maxU, segMaxV, r, g, b, a, 0, 0, -1);
                vertex(entry, vertexConsumer, x0, segTop,    southZ, maxU, minV,    r, g, b, a, 0, 0, -1);
                vertex(entry, vertexConsumer, x1, segTop,    southZ, minU, minV,    r, g, b, a, 0, 0, -1);
                vertex(entry, vertexConsumer, x1, segBottom, southZ, minU, segMaxV, r, g, b, a, 0, 0, -1);
            }

            // West Wall (X = -1.0f)
            float westX = -1.0f + 0.002f;
            for (int bz = 0; bz < 3; bz++) {
                float z0 = -1.0f + bz + (bz == 0 ? 0.002f : 0.0f);
                float z1 = -1.0f + bz + 1.0f - (bz == 2 ? 0.002f : 0.0f);

                // West Outside Face (Normal: -1, 0, 0)
                vertex(entry, vertexConsumer, westX, segBottom, z1, minU, segMaxV, r, g, b, a, -1, 0, 0);
                vertex(entry, vertexConsumer, westX, segTop,    z1, minU, minV,    r, g, b, a, -1, 0, 0);
                vertex(entry, vertexConsumer, westX, segTop,    z0, maxU, minV,    r, g, b, a, -1, 0, 0);
                vertex(entry, vertexConsumer, westX, segBottom, z0, maxU, segMaxV, r, g, b, a, -1, 0, 0);

                // West Inside Face (Normal: 1, 0, 0)
                vertex(entry, vertexConsumer, westX, segBottom, z0, maxU, segMaxV, r, g, b, a, 1, 0, 0);
                vertex(entry, vertexConsumer, westX, segTop,    z0, maxU, minV,    r, g, b, a, 1, 0, 0);
                vertex(entry, vertexConsumer, westX, segTop,    z1, minU, minV,    r, g, b, a, 1, 0, 0);
                vertex(entry, vertexConsumer, westX, segBottom, z1, minU, segMaxV, r, g, b, a, 1, 0, 0);
            }

            // East Wall (X = +2.0f)
            float eastX = 2.0f - 0.002f;
            for (int bz = 0; bz < 3; bz++) {
                float z0 = -1.0f + bz + (bz == 0 ? 0.002f : 0.0f);
                float z1 = -1.0f + bz + 1.0f - (bz == 2 ? 0.002f : 0.0f);

                // East Outside Face (Normal: 1, 0, 0)
                vertex(entry, vertexConsumer, eastX, segBottom, z0, minU, segMaxV, r, g, b, a, 1, 0, 0);
                vertex(entry, vertexConsumer, eastX, segTop,    z0, minU, minV,    r, g, b, a, 1, 0, 0);
                vertex(entry, vertexConsumer, eastX, segTop,    z1, maxU, minV,    r, g, b, a, 1, 0, 0);
                vertex(entry, vertexConsumer, eastX, segBottom, z1, maxU, segMaxV, r, g, b, a, 1, 0, 0);

                // East Inside Face (Normal: -1, 0, 0)
                vertex(entry, vertexConsumer, eastX, segBottom, z1, maxU, segMaxV, r, g, b, a, -1, 0, 0);
                vertex(entry, vertexConsumer, eastX, segTop,    z1, maxU, minV,    r, g, b, a, -1, 0, 0);
                vertex(entry, vertexConsumer, eastX, segTop,    z0, minU, minV,    r, g, b, a, -1, 0, 0);
                vertex(entry, vertexConsumer, eastX, segBottom, z0, minU, segMaxV, r, g, b, a, -1, 0, 0);
            }
        }
    }

    private static void vertex(MatrixStack.Entry entry, VertexConsumer vertexConsumer,
                               float x, float y, float z,
                               float u, float v,
                               int r, int g, int b, int a,
                               float nx, float ny, float nz) {
        vertexConsumer.vertex(entry, x, y, z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(entry, nx, ny, nz);
    }
}

package net.enchantedwood.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.enchantedwood.block.entity.TitaniumTankControllerBlockEntity;
import net.enchantedwood.fluid.MoltenMetal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class TitaniumTankControllerBlockEntityRenderer implements BlockEntityRenderer<TitaniumTankControllerBlockEntity, TitaniumTankRenderState> {
    private final TextureAtlasSprite waterSprite;
    private final TextureAtlasSprite lavaSprite;

    public TitaniumTankControllerBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.waterSprite = ctx.sprites().get(Sheets.BLOCKS_MAPPER.defaultNamespaceApply("water_still"));
        this.lavaSprite = ctx.sprites().get(Sheets.BLOCKS_MAPPER.defaultNamespaceApply("lava_still"));
    }

    @Override
    public TitaniumTankRenderState createRenderState() {
        return new TitaniumTankRenderState();
    }

    @Override
    public void extractRenderState(TitaniumTankControllerBlockEntity entity, TitaniumTankRenderState state, float tickDelta, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlayCommand) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlayCommand);
        state.isFormed = entity.isFormed();
        state.lavaAmount = entity.getStoredFluidAmount();
        state.currentFluid = entity.getFluidType();
        state.minPos = entity.getMinPos();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void submit(TitaniumTankRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (!state.isFormed || state.lavaAmount <= 0 || state.currentFluid == null || state.currentFluid == MoltenMetal.NONE) {
            return;
        }

        float fillFraction = (float) state.lavaAmount / 500_000.0f;
        fillFraction = Math.max(0.0f, Math.min(1.0f, fillFraction));
        float fluidHeight = Math.max(0.04f, 3.0f * fillFraction);
        float yBottom = -3.0f + 0.002f;
        float yTop = yBottom + fluidHeight;

        TextureAtlasSprite sprite = (state.currentFluid == MoltenMetal.LAVA) ? this.lavaSprite : this.waterSprite;
        int color = (state.currentFluid == MoltenMetal.LAVA) ? 0xFFFFFFFF : state.currentFluid.getColor();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = 250;

        queue.submitCustomGeometry(matrices, Sheets.translucentBlockItemSheet(), (entry, vertexConsumer) -> {
            renderFluidInterior(entry, vertexConsumer, sprite, r, g, b, a, yBottom, yTop);
        });
    }

    private void renderFluidInterior(PoseStack.Pose entry, VertexConsumer vertexConsumer,
                                     TextureAtlasSprite sprite, int r, int g, int b, int a,
                                     float yBottom, float yTop) {
        float minU = sprite.getU0();
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float maxV = sprite.getV1();

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

    private static void vertex(PoseStack.Pose entry, VertexConsumer vertexConsumer,
                               float x, float y, float z,
                               float u, float v,
                               int r, int g, int b, int a,
                               float nx, float ny, float nz) {
        vertexConsumer.addVertex(entry, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(entry, nx, ny, nz);
    }
}

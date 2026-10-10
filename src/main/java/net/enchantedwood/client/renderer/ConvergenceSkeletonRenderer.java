package net.enchantedwood.client.renderer;

import net.enchantedwood.EnchantedWoodMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class ConvergenceSkeletonRenderer extends SkeletonRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/entity/convergence_skeleton.png");

    public ConvergenceSkeletonRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(SkeletonRenderState state) {
        return TEXTURE;
    }
}

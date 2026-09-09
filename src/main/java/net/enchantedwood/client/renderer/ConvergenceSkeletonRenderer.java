package net.enchantedwood.client.renderer;

import net.enchantedwood.EnchantedWoodMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.SkeletonEntityRenderer;
import net.minecraft.client.render.entity.state.SkeletonEntityRenderState;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ConvergenceSkeletonRenderer extends SkeletonEntityRenderer {
    private static final Identifier TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/entity/convergence_skeleton.png");

    public ConvergenceSkeletonRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(SkeletonEntityRenderState state) {
        return TEXTURE;
    }
}

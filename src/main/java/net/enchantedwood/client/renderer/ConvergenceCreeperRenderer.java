package net.enchantedwood.client.renderer;

import net.enchantedwood.EnchantedWoodMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.CreeperEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.CreeperEntityRenderState;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ConvergenceCreeperRenderer extends CreeperEntityRenderer {
    private static final Identifier TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/entity/convergence_creeper.png");

    public ConvergenceCreeperRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(CreeperEntityRenderState state) {
        return TEXTURE;
    }
}

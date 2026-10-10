package net.enchantedwood.client.renderer;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.entity.custom.ResonanceColossusEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class ResonanceColossusRenderer extends MobRenderer<ResonanceColossusEntity, ResonanceColossusRenderState, ResonanceColossusModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/entity/resonance_colossus.png");

    public ResonanceColossusRenderer(EntityRendererProvider.Context context) {
        super(context, new ResonanceColossusModel(context.bakeLayer(ResonanceColossusModel.MODEL_LAYER)), 1.5F);
    }

    @Override
    public ResonanceColossusRenderState createRenderState() {
        return new ResonanceColossusRenderState();
    }

    @Override
    public void extractRenderState(ResonanceColossusEntity entity, ResonanceColossusRenderState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.ageInTicks = entity.tickCount + tickProgress;
    }

    @Override
    public Identifier getTextureLocation(ResonanceColossusRenderState state) {
        return TEXTURE;
    }
}

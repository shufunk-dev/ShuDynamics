package net.enchantedwood.client.renderer;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.entity.custom.ResonancePylonEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class ResonancePylonRenderer extends MobRenderer<ResonancePylonEntity, ResonancePylonRenderState, ResonancePylonModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/entity/resonance_pylon.png");

    public ResonancePylonRenderer(EntityRendererProvider.Context context) {
        super(context, new ResonancePylonModel(context.bakeLayer(ResonancePylonModel.MODEL_LAYER)), 0.6F);
    }

    @Override
    public ResonancePylonRenderState createRenderState() {
        return new ResonancePylonRenderState();
    }

    @Override
    public void extractRenderState(ResonancePylonEntity entity, ResonancePylonRenderState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.ageInTicks = entity.tickCount + tickProgress;
    }

    @Override
    public Identifier getTextureLocation(ResonancePylonRenderState state) {
        return TEXTURE;
    }
}

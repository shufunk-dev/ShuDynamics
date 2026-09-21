package net.enchantedwood.client.renderer;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.entity.custom.ResonancePylonEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ResonancePylonRenderer extends MobEntityRenderer<ResonancePylonEntity, ResonancePylonRenderState, ResonancePylonModel> {
    private static final Identifier TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/entity/resonance_pylon.png");

    public ResonancePylonRenderer(EntityRendererFactory.Context context) {
        super(context, new ResonancePylonModel(context.getPart(ResonancePylonModel.MODEL_LAYER)), 0.6F);
    }

    @Override
    public ResonancePylonRenderState createRenderState() {
        return new ResonancePylonRenderState();
    }

    @Override
    public void updateRenderState(ResonancePylonEntity entity, ResonancePylonRenderState state, float tickProgress) {
        super.updateRenderState(entity, state, tickProgress);
        state.ageInTicks = entity.age + tickProgress;
    }

    @Override
    public Identifier getTexture(ResonancePylonRenderState state) {
        return TEXTURE;
    }
}

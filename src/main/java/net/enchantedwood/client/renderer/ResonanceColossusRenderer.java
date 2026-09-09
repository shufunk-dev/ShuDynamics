package net.enchantedwood.client.renderer;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.entity.custom.ResonanceColossusEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ResonanceColossusRenderer extends MobEntityRenderer<ResonanceColossusEntity, ResonanceColossusRenderState, ResonanceColossusModel> {
    private static final Identifier TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/entity/resonance_colossus.png");

    public ResonanceColossusRenderer(EntityRendererFactory.Context context) {
        super(context, new ResonanceColossusModel(context.getPart(ResonanceColossusModel.MODEL_LAYER)), 1.5F);
    }

    @Override
    public ResonanceColossusRenderState createRenderState() {
        return new ResonanceColossusRenderState();
    }

    @Override
    public void updateRenderState(ResonanceColossusEntity entity, ResonanceColossusRenderState state, float tickProgress) {
        super.updateRenderState(entity, state, tickProgress);
        state.ageInTicks = entity.age + tickProgress;
    }

    @Override
    public Identifier getTexture(ResonanceColossusRenderState state) {
        return TEXTURE;
    }
}

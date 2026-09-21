package net.enchantedwood.client.renderer;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.entity.custom.AscendantColossusEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class AscendantColossusRenderer extends MobEntityRenderer<AscendantColossusEntity, ResonanceColossusRenderState, ResonanceColossusModel> {
    private static final Identifier TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/entity/ascendant_colossus.png");

    public AscendantColossusRenderer(EntityRendererFactory.Context context) {
        super(context, new ResonanceColossusModel(context.getPart(ResonanceColossusModel.MODEL_LAYER)), 1.8F);
    }

    @Override
    public ResonanceColossusRenderState createRenderState() {
        return new ResonanceColossusRenderState();
    }

    @Override
    public void updateRenderState(AscendantColossusEntity entity, ResonanceColossusRenderState state, float tickProgress) {
        super.updateRenderState(entity, state, tickProgress);
        state.ageInTicks = entity.age + tickProgress;
    }

    @Override
    public Identifier getTexture(ResonanceColossusRenderState state) {
        return TEXTURE;
    }
}

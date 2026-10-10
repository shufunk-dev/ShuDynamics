package net.enchantedwood.client.renderer;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.entity.custom.AscendantColossusEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class AscendantColossusRenderer extends MobRenderer<AscendantColossusEntity, ResonanceColossusRenderState, ResonanceColossusModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/entity/ascendant_colossus.png");

    public AscendantColossusRenderer(EntityRendererProvider.Context context) {
        super(context, new ResonanceColossusModel(context.bakeLayer(ResonanceColossusModel.MODEL_LAYER)), 1.8F);
    }

    @Override
    public ResonanceColossusRenderState createRenderState() {
        return new ResonanceColossusRenderState();
    }

    @Override
    public void extractRenderState(AscendantColossusEntity entity, ResonanceColossusRenderState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.ageInTicks = entity.tickCount + tickProgress;
    }

    @Override
    public Identifier getTextureLocation(ResonanceColossusRenderState state) {
        return TEXTURE;
    }
}

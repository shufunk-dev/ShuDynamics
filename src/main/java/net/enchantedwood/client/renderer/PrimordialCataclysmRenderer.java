package net.enchantedwood.client.renderer;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.entity.custom.PrimordialCataclysmEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class PrimordialCataclysmRenderer extends MobRenderer<PrimordialCataclysmEntity, ResonanceColossusRenderState, ResonanceColossusModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/entity/primordial_cataclysm.png");

    public PrimordialCataclysmRenderer(EntityRendererProvider.Context context) {
        super(context, new ResonanceColossusModel(context.bakeLayer(ResonanceColossusModel.MODEL_LAYER)), 2.2F);
    }

    @Override
    public ResonanceColossusRenderState createRenderState() {
        return new ResonanceColossusRenderState();
    }

    @Override
    public void extractRenderState(PrimordialCataclysmEntity entity, ResonanceColossusRenderState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.ageInTicks = entity.tickCount + tickProgress;
    }

    @Override
    public Identifier getTextureLocation(ResonanceColossusRenderState state) {
        return TEXTURE;
    }
}

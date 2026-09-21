package net.enchantedwood.client.renderer;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.entity.custom.PrimordialCataclysmEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class PrimordialCataclysmRenderer extends MobEntityRenderer<PrimordialCataclysmEntity, ResonanceColossusRenderState, ResonanceColossusModel> {
    private static final Identifier TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/entity/primordial_cataclysm.png");

    public PrimordialCataclysmRenderer(EntityRendererFactory.Context context) {
        super(context, new ResonanceColossusModel(context.getPart(ResonanceColossusModel.MODEL_LAYER)), 2.2F);
    }

    @Override
    public ResonanceColossusRenderState createRenderState() {
        return new ResonanceColossusRenderState();
    }

    @Override
    public void updateRenderState(PrimordialCataclysmEntity entity, ResonanceColossusRenderState state, float tickProgress) {
        super.updateRenderState(entity, state, tickProgress);
        state.ageInTicks = entity.age + tickProgress;
    }

    @Override
    public Identifier getTexture(ResonanceColossusRenderState state) {
        return TEXTURE;
    }
}

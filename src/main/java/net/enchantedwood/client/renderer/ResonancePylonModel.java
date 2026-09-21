package net.enchantedwood.client.renderer;

import net.enchantedwood.EnchantedWoodMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class ResonancePylonModel extends EntityModel<ResonancePylonRenderState> {
    public static final EntityModelLayer MODEL_LAYER = new EntityModelLayer(Identifier.of(EnchantedWoodMod.MOD_ID, "resonance_pylon"), "main");

    private final ModelPart base;
    private final ModelPart crystal;

    public ResonancePylonModel(ModelPart root) {
        super(root);
        this.base = root.getChild("base");
        this.crystal = root.getChild("crystal");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        // Base pedestal (12x4x12)
        root.addChild("base", ModelPartBuilder.create()
                .uv(0, 0).cuboid(-6.0F, 0.0F, -6.0F, 12.0F, 4.0F, 12.0F),
                ModelTransform.origin(0.0F, 20.0F, 0.0F));

        // Floating central crystal (8x24x8)
        root.addChild("crystal", ModelPartBuilder.create()
                .uv(0, 16).cuboid(-4.0F, -12.0F, -4.0F, 8.0F, 24.0F, 8.0F),
                ModelTransform.origin(0.0F, 8.0F, 0.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(ResonancePylonRenderState state) {
        super.setAngles(state);
        this.crystal.yaw = state.ageInTicks * 0.05F;
        this.crystal.originY = 8.0F + MathHelper.sin(state.ageInTicks * 0.1F) * 2.0F;
    }
}

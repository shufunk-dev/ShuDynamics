package net.enchantedwood.client.renderer;

import net.enchantedwood.EnchantedWoodMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class ResonancePylonModel extends EntityModel<ResonancePylonRenderState> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "resonance_pylon"), "main");

    private final ModelPart base;
    private final ModelPart crystal;

    public ResonancePylonModel(ModelPart root) {
        super(root);
        this.base = root.getChild("base");
        this.crystal = root.getChild("crystal");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition root = modelData.getRoot();

        // Base pedestal (12x4x12)
        root.addOrReplaceChild("base", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0F, 0.0F, -6.0F, 12.0F, 4.0F, 12.0F),
                PartPose.offset(0.0F, 20.0F, 0.0F));

        // Floating central crystal (8x24x8)
        root.addOrReplaceChild("crystal", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4.0F, -12.0F, -4.0F, 8.0F, 24.0F, 8.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        return LayerDefinition.create(modelData, 64, 64);
    }

    @Override
    public void setupAnim(ResonancePylonRenderState state) {
        super.setupAnim(state);
        this.crystal.yRot = state.ageInTicks * 0.05F;
        this.crystal.y = 8.0F + Mth.sin(state.ageInTicks * 0.1F) * 2.0F;
    }
}

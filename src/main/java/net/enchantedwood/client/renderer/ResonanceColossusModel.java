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
public class ResonanceColossusModel extends EntityModel<ResonanceColossusRenderState> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "resonance_colossus"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart leftCrystal;
    private final ModelPart rightCrystal;

    public ResonanceColossusModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.leftCrystal = root.getChild("left_crystal");
        this.rightCrystal = root.getChild("right_crystal");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition root = modelData.getRoot();

        // Head (12x12x12)
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0F, -12.0F, -6.0F, 12.0F, 12.0F, 12.0F)
                // Crown / Horns
                .texOffs(48, 0).addBox(-7.0F, -16.0F, -4.0F, 14.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, -30.0F, 0.0F));

        // Torso (24x26x14)
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-12.0F, 0.0F, -7.0F, 24.0F, 26.0F, 14.0F)
                // Exposed Singularity Chest Core (10x10x4)
                .texOffs(76, 24).addBox(-5.0F, 6.0F, -9.0F, 10.0F, 10.0F, 4.0F),
                PartPose.offset(0.0F, -30.0F, 0.0F));

        // Right Arm - Giant Slam Gauntlet (10x32x10)
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-10.0F, -2.0F, -5.0F, 10.0F, 32.0F, 10.0F),
                PartPose.offset(-12.0F, -28.0F, 0.0F));

        // Left Arm - Crystalline Blade Arm (8x30x8)
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(40, 64).addBox(0.0F, -2.0F, -4.0F, 8.0F, 30.0F, 8.0F),
                PartPose.offset(12.0F, -28.0F, 0.0F));

        // Right Leg (9x24x9)
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(72, 64).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 24.0F, 9.0F),
                PartPose.offset(-6.0F, -4.0F, 0.0F));

        // Left Leg (9x24x9)
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(72, 64).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 24.0F, 9.0F),
                PartPose.offset(6.0F, -4.0F, 0.0F));

        // Floating Left Shoulder Crystal (6x12x6)
        root.addOrReplaceChild("left_crystal", CubeListBuilder.create()
                .texOffs(0, 106).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 12.0F, 6.0F),
                PartPose.offset(16.0F, -36.0F, 0.0F));

        // Floating Right Shoulder Crystal (6x12x6)
        root.addOrReplaceChild("right_crystal", CubeListBuilder.create()
                .texOffs(24, 106).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 12.0F, 6.0F),
                PartPose.offset(-16.0F, -36.0F, 0.0F));

        return LayerDefinition.create(modelData, 128, 128);
    }

    @Override
    public void setupAnim(ResonanceColossusRenderState state) {
        super.setupAnim(state);

        // Walking leg animations
        this.rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.2F * state.walkAnimationSpeed;
        this.leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + (float) Math.PI) * 1.2F * state.walkAnimationSpeed;

        // Arm swing animations
        this.rightArm.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + (float) Math.PI) * 0.8F * state.walkAnimationSpeed;
        this.leftArm.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 0.8F * state.walkAnimationSpeed;

        // Floating telekinetic orbit crystals bobbing up and down
        this.leftCrystal.y = -36.0F + Mth.sin(state.ageInTicks * 0.08F) * 3.0F;
        this.rightCrystal.y = -36.0F + Mth.cos(state.ageInTicks * 0.08F) * 3.0F;
        this.leftCrystal.yRot = state.ageInTicks * 0.04F;
        this.rightCrystal.yRot = -state.ageInTicks * 0.04F;
    }
}

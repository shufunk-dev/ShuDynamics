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
public class ResonanceColossusModel extends EntityModel<ResonanceColossusRenderState> {
    public static final EntityModelLayer MODEL_LAYER = new EntityModelLayer(Identifier.of(EnchantedWoodMod.MOD_ID, "resonance_colossus"), "main");

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

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        // Head (12x12x12)
        root.addChild("head", ModelPartBuilder.create()
                .uv(0, 0).cuboid(-6.0F, -12.0F, -6.0F, 12.0F, 12.0F, 12.0F)
                // Crown / Horns
                .uv(48, 0).cuboid(-7.0F, -16.0F, -4.0F, 14.0F, 4.0F, 8.0F),
                ModelTransform.origin(0.0F, -30.0F, 0.0F));

        // Torso (24x26x14)
        root.addChild("body", ModelPartBuilder.create()
                .uv(0, 24).cuboid(-12.0F, 0.0F, -7.0F, 24.0F, 26.0F, 14.0F)
                // Exposed Singularity Chest Core (10x10x4)
                .uv(76, 24).cuboid(-5.0F, 6.0F, -9.0F, 10.0F, 10.0F, 4.0F),
                ModelTransform.origin(0.0F, -30.0F, 0.0F));

        // Right Arm - Giant Slam Gauntlet (10x32x10)
        root.addChild("right_arm", ModelPartBuilder.create()
                .uv(0, 64).cuboid(-10.0F, -2.0F, -5.0F, 10.0F, 32.0F, 10.0F),
                ModelTransform.origin(-12.0F, -28.0F, 0.0F));

        // Left Arm - Crystalline Blade Arm (8x30x8)
        root.addChild("left_arm", ModelPartBuilder.create()
                .uv(40, 64).cuboid(0.0F, -2.0F, -4.0F, 8.0F, 30.0F, 8.0F),
                ModelTransform.origin(12.0F, -28.0F, 0.0F));

        // Right Leg (9x24x9)
        root.addChild("right_leg", ModelPartBuilder.create()
                .uv(72, 64).cuboid(-4.5F, 0.0F, -4.5F, 9.0F, 24.0F, 9.0F),
                ModelTransform.origin(-6.0F, -4.0F, 0.0F));

        // Left Leg (9x24x9)
        root.addChild("left_leg", ModelPartBuilder.create()
                .uv(72, 64).cuboid(-4.5F, 0.0F, -4.5F, 9.0F, 24.0F, 9.0F),
                ModelTransform.origin(6.0F, -4.0F, 0.0F));

        // Floating Left Shoulder Crystal (6x12x6)
        root.addChild("left_crystal", ModelPartBuilder.create()
                .uv(0, 106).cuboid(-3.0F, -6.0F, -3.0F, 6.0F, 12.0F, 6.0F),
                ModelTransform.origin(16.0F, -36.0F, 0.0F));

        // Floating Right Shoulder Crystal (6x12x6)
        root.addChild("right_crystal", ModelPartBuilder.create()
                .uv(24, 106).cuboid(-3.0F, -6.0F, -3.0F, 6.0F, 12.0F, 6.0F),
                ModelTransform.origin(-16.0F, -36.0F, 0.0F));

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(ResonanceColossusRenderState state) {
        super.setAngles(state);

        // Walking leg animations
        if (state.summonTicks > 0 || state.deathOverloadTicks > 0) {
            this.rightLeg.pitch = 0.0F;
            this.leftLeg.pitch = 0.0F;
        } else {
            this.rightLeg.pitch = MathHelper.cos(state.limbSwingAnimationProgress * 0.6662F) * 1.2F * state.limbSwingAmplitude;
            this.leftLeg.pitch = MathHelper.cos(state.limbSwingAnimationProgress * 0.6662F + (float) Math.PI) * 1.2F * state.limbSwingAmplitude;
        }

        // Arm swing animations
        this.rightArm.pitch = MathHelper.cos(state.limbSwingAnimationProgress * 0.6662F + (float) Math.PI) * 0.8F * state.limbSwingAmplitude;
        this.leftArm.pitch = MathHelper.cos(state.limbSwingAnimationProgress * 0.6662F) * 0.8F * state.limbSwingAmplitude;

        // Reset base positions & angles
        this.body.originY = -30.0F;
        this.body.roll = 0.0F;
        this.head.pitch = 0.0F;
        this.head.yaw = 0.0F;
        this.rightArm.roll = 0.0F;
        this.leftArm.roll = 0.0F;

        // Floating telekinetic orbit crystals bobbing up and down
        this.leftCrystal.originY = -36.0F + MathHelper.sin(state.ageInTicks * 0.08F) * 3.0F;
        this.rightCrystal.originY = -36.0F + MathHelper.cos(state.ageInTicks * 0.08F) * 3.0F;
        this.leftCrystal.yaw = state.ageInTicks * 0.04F;
        this.rightCrystal.yaw = -state.ageInTicks * 0.04F;

        // --- Custom Entrance & Death Animations ---
        if (state.summonTicks > 0) {
            float sProgress = (70 - state.summonTicks) / 70.0F; // 0.0 to 1.0
            // Bowed head and crossed/downward arms, rising up from the ground
            this.head.pitch = (1.0F - sProgress) * 0.6F;
            this.rightArm.pitch = -(1.0F - sProgress) * 0.5F;
            this.leftArm.pitch = -(1.0F - sProgress) * 0.5F;
            this.body.originY = -30.0F + (1.0F - sProgress) * 16.0F;
            this.leftCrystal.originY = -36.0F + (1.0F - sProgress) * 12.0F;
            this.rightCrystal.originY = -36.0F + (1.0F - sProgress) * 12.0F;
        } else if (state.deathOverloadTicks > 0) {
            float dProgress = (80 - state.deathOverloadTicks) / 80.0F; // 0.0 to 1.0
            float jitter = MathHelper.sin(state.ageInTicks * 2.5F) * 0.12F * (1.0F + dProgress);
            // Head thrown back roaring into the sky
            this.head.pitch = -0.65F + jitter;
            this.head.yaw = jitter * 1.5F;
            this.body.roll = jitter;
            // Arms outstretched trembling violently
            this.rightArm.roll = 0.85F + jitter;
            this.leftArm.roll = -0.85F - jitter;
            this.rightArm.pitch = -0.3F;
            this.leftArm.pitch = -0.3F;
            // Crystals orbiting at hyper-speed and ascending
            this.leftCrystal.originY = -36.0F - dProgress * 16.0F;
            this.rightCrystal.originY = -36.0F - dProgress * 16.0F;
            this.leftCrystal.yaw = state.ageInTicks * 0.5F;
            this.rightCrystal.yaw = -state.ageInTicks * 0.5F;
        }
    }
}

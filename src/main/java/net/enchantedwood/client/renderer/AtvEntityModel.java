package net.enchantedwood.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.enchantedwood.EnchantedWoodMod;

@Environment(EnvType.CLIENT)
public class AtvEntityModel extends EntityModel<AtvRenderState> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "atv"), "main");

    private final ModelPart body;
    private final ModelPart frontLeftWheel;
    private final ModelPart frontRightWheel;
    private final ModelPart backLeftWheel;
    private final ModelPart backRightWheel;
    private final ModelPart drill;
    private final ModelPart treeSaw;
    private final ModelPart cropHarvester;
    private final ModelPart headlights;

    public AtvEntityModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.frontLeftWheel = root.getChild("front_left_wheel");
        this.frontRightWheel = root.getChild("front_right_wheel");
        this.backLeftWheel = root.getChild("back_left_wheel");
        this.backRightWheel = root.getChild("back_right_wheel");
        this.drill = root.getChild("drill");
        this.treeSaw = root.getChild("tree_saw");
        this.cropHarvester = root.getChild("crop_harvester");
        this.headlights = root.getChild("headlights");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition root = modelData.getRoot();

        // 1. Main Body / Chassis Frame
        root.addOrReplaceChild("body", CubeListBuilder.create()
                // Lower Chassis Base (16x4x24)
                .texOffs(0, 0).addBox(-8.0F, -6.0F, -12.0F, 16.0F, 4.0F, 24.0F)
                // Driver Seat (12x4x10)
                .texOffs(0, 28).addBox(-6.0F, -10.0F, -4.0F, 12.0F, 4.0F, 10.0F)
                // Front Hood / Engine Block (12x6x8)
                .texOffs(0, 42).addBox(-6.0F, -12.0F, -12.0F, 12.0F, 6.0F, 8.0F)
                // Handlebars Column (2x8x2)
                .texOffs(40, 42).addBox(-1.0F, -18.0F, -8.0F, 2.0F, 6.0F, 2.0F)
                // Handlebars Bar (16x2x2)
                .texOffs(0, 56).addBox(-8.0F, -19.0F, -8.0F, 16.0F, 2.0F, 2.0F)
                // Front Bull-Bar (14x6x2)
                .texOffs(36, 56).addBox(-7.0F, -8.0F, -14.0F, 14.0F, 6.0F, 2.0F)
                // Rear Cargo Rack (14x4x6)
                .texOffs(0, 60).addBox(-7.0F, -8.0F, 7.0F, 14.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // 2. 4 Heavy All-Terrain Rubber Wheels (4x8x8 each)
        CubeDeformation wheelDilation = new CubeDeformation(0.0F);

        root.addOrReplaceChild("front_left_wheel", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-2.0F, -4.0F, -4.0F, 4.0F, 8.0F, 8.0F, wheelDilation),
                PartPose.offsetAndRotation(9.0F, 20.0F, -8.0F, 0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("front_right_wheel", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-2.0F, -4.0F, -4.0F, 4.0F, 8.0F, 8.0F, wheelDilation),
                PartPose.offsetAndRotation(-9.0F, 20.0F, -8.0F, 0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("back_left_wheel", CubeListBuilder.create()
                .texOffs(56, 16).addBox(-2.0F, -4.0F, -4.0F, 4.0F, 8.0F, 8.0F, wheelDilation),
                PartPose.offsetAndRotation(9.0F, 20.0F, 8.0F, 0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("back_right_wheel", CubeListBuilder.create()
                .texOffs(56, 16).addBox(-2.0F, -4.0F, -4.0F, 4.0F, 8.0F, 8.0F, wheelDilation),
                PartPose.offsetAndRotation(-9.0F, 20.0F, 8.0F, 0.0F, 0.0F, 0.0F));

        // 3. Front Mining Drill Bit (Cone attached to front bumper)
        root.addOrReplaceChild("drill", CubeListBuilder.create()
                // Base drill cylinder (8x8x6)
                .texOffs(0, 68).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F)
                // Mid taper (6x6x4)
                .texOffs(28, 68).addBox(-3.0F, -3.0F, -10.0F, 6.0F, 6.0F, 4.0F)
                // Sharp cone tip (4x4x4)
                .texOffs(48, 68).addBox(-2.0F, -2.0F, -14.0F, 4.0F, 4.0F, 4.0F)
                // Center drill spindle (2x2x2)
                .texOffs(64, 68).addBox(-1.0F, -1.0F, -16.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, -14.0F, 0.0F, 0.0F, 0.0F));

        // 4. Front Lumberjack Tree Saw Blade (Large circular blade)
        root.addOrReplaceChild("tree_saw", CubeListBuilder.create()
                // Center Hub (4x4x4)
                .texOffs(0, 82).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 4.0F)
                // Circular Saw Blade (14x14x1)
                .texOffs(16, 82).addBox(-7.0F, -7.0F, -1.0F, 14.0F, 14.0F, 1.0F)
                // Cross cutting teeth (16x16x1)
                .texOffs(46, 82).addBox(-8.0F, -8.0F, -0.5F, 16.0F, 16.0F, 0.5F),
                PartPose.offsetAndRotation(0.0F, 16.0F, -15.0F, 0.0F, 0.0F, 0.0F));

        // 5. Front Agricultural Crop Harvester (Wide combine cylinder reel)
        root.addOrReplaceChild("crop_harvester", CubeListBuilder.create()
                // Central Reel Axle (22x2x2)
                .texOffs(0, 98).addBox(-11.0F, -1.0F, -1.0F, 22.0F, 2.0F, 2.0F)
                // Outer Left Wheel (1x8x8)
                .texOffs(48, 98).addBox(-11.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F)
                // Outer Right Wheel (1x8x8)
                .texOffs(48, 98).addBox(10.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F)
                // Combine Reel Slats (20x1x6)
                .texOffs(0, 102).addBox(-10.0F, -3.0F, -3.0F, 20.0F, 1.0F, 6.0F)
                .texOffs(0, 102).addBox(-10.0F, 2.0F, -3.0F, 20.0F, 1.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 18.0F, -15.0F, 0.0F, 0.0F, 0.0F));

        // 6. Dual Automotive Headlights
        root.addOrReplaceChild("headlights", CubeListBuilder.create()
                // Left Light (3x3x2)
                .texOffs(0, 110).addBox(-6.0F, -10.0F, -14.5F, 3.0F, 3.0F, 2.0F)
                // Right Light (3x3x2)
                .texOffs(0, 110).addBox(3.0F, -10.0F, -14.5F, 3.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(modelData, 128, 128);
    }

    @Override
    public void setupAnim(AtvRenderState state) {
        super.setupAnim(state);
        float rot = state.wheelRotation * 0.1f;
        this.frontLeftWheel.xRot = rot;
        this.frontRightWheel.xRot = rot;
        this.backLeftWheel.xRot = rot;
        this.backRightWheel.xRot = rot;

        this.drill.visible = (state.attachmentType == 1);
        this.treeSaw.visible = (state.attachmentType == 2);
        this.cropHarvester.visible = (state.attachmentType == 3);

        this.drill.zRot = (float) Math.toRadians(state.toolSpin);
        this.treeSaw.xRot = (float) Math.toRadians(state.toolSpin);
        this.cropHarvester.xRot = (float) Math.toRadians(state.toolSpin);
    }
}

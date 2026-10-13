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

        // 1. Detailed Chassis, Body Panels, Suspension & Cockpit
        root.addOrReplaceChild("body", CubeListBuilder.create()
                // Lower Tubular Frame Rails
                .texOffs(0, 0).addBox(-5.0F, -6.0F, -11.0F, 10.0F, 3.0F, 22.0F)
                // Heavy Steel Skid Plate
                .texOffs(0, 25).addBox(-4.0F, -3.0F, -10.0F, 8.0F, 1.0F, 20.0F)
                // Front & Rear Differential Gearboxes
                .texOffs(56, 25).addBox(-3.0F, -5.0F, -9.0F, 6.0F, 3.0F, 4.0F)
                .texOffs(56, 25).addBox(-3.0F, -5.0F, 5.0F, 6.0F, 3.0F, 4.0F)
                // Double Wishbone Suspension A-Arms
                .texOffs(56, 32).addBox(4.0F, -6.0F, -9.0F, 4.0F, 2.0F, 2.0F)
                .texOffs(56, 32).addBox(-8.0F, -6.0F, -9.0F, 4.0F, 2.0F, 2.0F)
                .texOffs(56, 32).addBox(4.0F, -6.0F, 7.0F, 4.0F, 2.0F, 2.0F)
                .texOffs(56, 32).addBox(-8.0F, -6.0F, 7.0F, 4.0F, 2.0F, 2.0F)
                // Coilover Shock Springs (Vibrant Yellow Springs)
                .texOffs(68, 32).addBox(3.5F, -8.0F, -8.5F, 2.0F, 3.0F, 2.0F)
                .texOffs(68, 32).addBox(-5.5F, -8.0F, -8.5F, 2.0F, 3.0F, 2.0F)
                .texOffs(68, 32).addBox(3.5F, -8.0F, 6.5F, 2.0F, 3.0F, 2.0F)
                .texOffs(68, 32).addBox(-5.5F, -8.0F, 6.5F, 2.0F, 3.0F, 2.0F)
                // High-Performance Engine Block & Transmission
                .texOffs(0, 46).addBox(-4.5F, -9.5F, -6.0F, 9.0F, 5.0F, 6.0F)
                // Dual Stainless Exhaust Mufflers & Tailpipes
                .texOffs(30, 46).addBox(2.5F, -8.5F, 7.0F, 2.0F, 2.0F, 6.0F)
                .texOffs(30, 46).addBox(-4.5F, -8.5F, 7.0F, 2.0F, 2.0F, 6.0F)
                // Diamond-Grip Nerf Bars / Floorboards
                .texOffs(46, 46).addBox(5.0F, -4.0F, -4.0F, 5.0F, 1.0F, 8.0F)
                .texOffs(46, 46).addBox(-10.0F, -4.0F, -4.0F, 5.0F, 1.0F, 8.0F)
                // Aerodynamic Front Cowl & Radiator Intake
                .texOffs(0, 57).addBox(-5.0F, -11.0F, -12.0F, 10.0F, 5.0F, 6.0F)
                // Aggressive Flared Front Fenders
                .texOffs(32, 57).addBox(4.0F, -10.0F, -13.0F, 7.0F, 2.0F, 9.0F)
                .texOffs(32, 57).addBox(-11.0F, -10.0F, -13.0F, 7.0F, 2.0F, 9.0F)
                // Wide Flared Rear Mudguard Fenders
                .texOffs(64, 57).addBox(4.0F, -9.5F, 3.5F, 7.0F, 2.0F, 10.0F)
                .texOffs(64, 57).addBox(-11.0F, -9.5F, 3.5F, 7.0F, 2.0F, 10.0F)
                // Sport Fuel Tank & Billet Gas Cap
                .texOffs(0, 68).addBox(-4.0F, -12.0F, -6.5F, 8.0F, 4.0F, 5.0F)
                .texOffs(26, 68).addBox(-1.5F, -13.0F, -5.5F, 3.0F, 1.0F, 3.0F)
                // Ergonomic Sport Saddle Seat & Raised Passenger Tail
                .texOffs(38, 68).addBox(-4.5F, -10.0F, -2.0F, 9.0F, 3.0F, 10.0F)
                .texOffs(76, 68).addBox(-4.0F, -11.0F, 5.5F, 8.0F, 2.0F, 4.0F)
                // Steering Column Riser & Crossbar Handlebars
                .texOffs(0, 77).addBox(-1.0F, -16.0F, -8.0F, 2.0F, 5.0F, 2.0F)
                .texOffs(8, 77).addBox(-8.0F, -17.0F, -8.5F, 16.0F, 2.0F, 2.0F)
                // Ergonomic Handguards (Bark Busters)
                .texOffs(44, 77).addBox(6.5F, -17.5F, -9.5F, 2.0F, 3.0F, 3.0F)
                .texOffs(44, 77).addBox(-8.5F, -17.5F, -9.5F, 2.0F, 3.0F, 3.0F)
                // Digital Instrument Gauge Cluster / HUD
                .texOffs(54, 77).addBox(-2.0F, -16.5F, -9.0F, 4.0F, 2.0F, 2.0F)
                // Heavy Tubular Front Bull-Bar & Winch
                .texOffs(66, 77).addBox(-7.0F, -9.0F, -14.0F, 14.0F, 6.0F, 2.0F)
                .texOffs(98, 77).addBox(-2.0F, -7.0F, -15.0F, 4.0F, 2.0F, 1.0F)
                // Rear Cargo Rack with Tie-Down Perimeter Rails
                .texOffs(0, 84).addBox(-6.5F, -9.5F, 7.0F, 13.0F, 2.0F, 7.0F)
                .texOffs(40, 84).addBox(-7.0F, -11.5F, 6.5F, 14.0F, 2.0F, 8.0F)
                // Integrated Rear LED Taillight Bar
                .texOffs(84, 84).addBox(-4.0F, -8.0F, 14.0F, 8.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // 2. 4 Deep-Dish All-Terrain Wheels with Knobby Lugs and Alloy Rims
        CubeListBuilder wheelBuilder = CubeListBuilder.create()
                // Knobby Rubber Main Tire
                .texOffs(24, 93).addBox(-2.5F, -4.5F, -4.5F, 5.0F, 9.0F, 9.0F)
                // Vertical Tread Lugs
                .texOffs(52, 93).addBox(-2.0F, -5.0F, -3.5F, 4.0F, 10.0F, 7.0F)
                // Horizontal Tread Lugs
                .texOffs(74, 93).addBox(-2.0F, -3.5F, -5.0F, 4.0F, 7.0F, 10.0F)
                // Deep-Dish Alloy Rim & Wheel Hub
                .texOffs(102, 93).addBox(-2.75F, -2.5F, -2.5F, 5.5F, 5.0F, 5.0F);

        root.addOrReplaceChild("front_left_wheel", wheelBuilder,
                PartPose.offsetAndRotation(9.5F, 19.5F, -8.0F, 0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("front_right_wheel", wheelBuilder,
                PartPose.offsetAndRotation(-9.5F, 19.5F, -8.0F, 0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("back_left_wheel", wheelBuilder,
                PartPose.offsetAndRotation(9.5F, 19.5F, 8.0F, 0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("back_right_wheel", wheelBuilder,
                PartPose.offsetAndRotation(-9.5F, 19.5F, 8.0F, 0.0F, 0.0F, 0.0F));

        // 3. Front Mining Drill Bit (Mounted cleanly on front bull-bar)
        root.addOrReplaceChild("drill", CubeListBuilder.create()
                .texOffs(0, 111).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F)
                .texOffs(28, 111).addBox(-3.0F, -3.0F, -10.0F, 6.0F, 6.0F, 4.0F)
                .texOffs(48, 111).addBox(-2.0F, -2.0F, -14.0F, 4.0F, 4.0F, 4.0F)
                .texOffs(64, 111).addBox(-1.0F, -1.0F, -16.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, -14.0F, 0.0F, 0.0F, 0.0F));

        // 4. Front Lumberjack Tree Saw Blade
        root.addOrReplaceChild("tree_saw", CubeListBuilder.create()
                .texOffs(0, 120).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 4.0F)
                .texOffs(16, 120).addBox(-7.0F, -7.0F, -1.0F, 14.0F, 14.0F, 1.0F)
                .texOffs(46, 120).addBox(-8.0F, -8.0F, -0.5F, 16.0F, 16.0F, 0.5F),
                PartPose.offsetAndRotation(0.0F, 16.0F, -15.0F, 0.0F, 0.0F, 0.0F));

        // 5. Front Agricultural Crop Harvester Reel
        root.addOrReplaceChild("crop_harvester", CubeListBuilder.create()
                .texOffs(46, 120).addBox(-11.0F, -1.0F, -1.0F, 22.0F, 2.0F, 2.0F)
                .texOffs(66, 120).addBox(-11.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F)
                .texOffs(66, 120).addBox(10.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F)
                .texOffs(46, 120).addBox(-10.0F, -3.0F, -3.0F, 20.0F, 1.0F, 6.0F)
                .texOffs(46, 120).addBox(-10.0F, 2.0F, -3.0F, 20.0F, 1.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 18.0F, -15.0F, 0.0F, 0.0F, 0.0F));

        // 6. Dual Projector Headlights & Auxiliary LED Bar
        root.addOrReplaceChild("headlights", CubeListBuilder.create()
                .texOffs(0, 93).addBox(-6.0F, -10.0F, -13.5F, 4.0F, 3.0F, 2.0F)
                .texOffs(0, 93).addBox(2.0F, -10.0F, -13.5F, 4.0F, 3.0F, 2.0F)
                .texOffs(12, 93).addBox(-2.0F, -11.0F, -13.5F, 4.0F, 2.0F, 2.0F),
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

package net.enchantedwood.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

@Environment(EnvType.CLIENT)
public class AtvEntityModel extends EntityModel<AtvRenderState> {
    public static final EntityModelLayer MODEL_LAYER = new EntityModelLayer(Identifier.of(EnchantedWoodMod.MOD_ID, "atv"), "main");

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

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        // 1. Detailed Chassis, Body Panels, Suspension & Cockpit
        root.addChild("body", ModelPartBuilder.create()
                // Lower Tubular Frame Rails
                .uv(0, 0).cuboid(-5.0F, -6.0F, -11.0F, 10.0F, 3.0F, 22.0F)
                // Heavy Steel Skid Plate
                .uv(0, 25).cuboid(-4.0F, -3.0F, -10.0F, 8.0F, 1.0F, 20.0F)
                // Front & Rear Differential Gearboxes
                .uv(56, 25).cuboid(-3.0F, -5.0F, -9.0F, 6.0F, 3.0F, 4.0F)
                .uv(56, 25).cuboid(-3.0F, -5.0F, 5.0F, 6.0F, 3.0F, 4.0F)
                // Double Wishbone Suspension A-Arms
                .uv(56, 32).cuboid(4.0F, -6.0F, -9.0F, 4.0F, 2.0F, 2.0F)
                .uv(56, 32).cuboid(-8.0F, -6.0F, -9.0F, 4.0F, 2.0F, 2.0F)
                .uv(56, 32).cuboid(4.0F, -6.0F, 7.0F, 4.0F, 2.0F, 2.0F)
                .uv(56, 32).cuboid(-8.0F, -6.0F, 7.0F, 4.0F, 2.0F, 2.0F)
                // Coilover Shock Springs (Vibrant Yellow Springs)
                .uv(68, 32).cuboid(3.5F, -8.0F, -8.5F, 2.0F, 3.0F, 2.0F)
                .uv(68, 32).cuboid(-5.5F, -8.0F, -8.5F, 2.0F, 3.0F, 2.0F)
                .uv(68, 32).cuboid(3.5F, -8.0F, 6.5F, 2.0F, 3.0F, 2.0F)
                .uv(68, 32).cuboid(-5.5F, -8.0F, 6.5F, 2.0F, 3.0F, 2.0F)
                // High-Performance Engine Block & Transmission
                .uv(0, 46).cuboid(-4.5F, -9.5F, -6.0F, 9.0F, 5.0F, 6.0F)
                // Dual Stainless Exhaust Mufflers & Tailpipes
                .uv(30, 46).cuboid(2.5F, -8.5F, 7.0F, 2.0F, 2.0F, 6.0F)
                .uv(30, 46).cuboid(-4.5F, -8.5F, 7.0F, 2.0F, 2.0F, 6.0F)
                // Diamond-Grip Nerf Bars / Floorboards
                .uv(46, 46).cuboid(5.0F, -4.0F, -4.0F, 5.0F, 1.0F, 8.0F)
                .uv(46, 46).cuboid(-10.0F, -4.0F, -4.0F, 5.0F, 1.0F, 8.0F)
                // Aerodynamic Front Cowl & Radiator Intake
                .uv(0, 57).cuboid(-5.0F, -11.0F, -12.0F, 10.0F, 5.0F, 6.0F)
                // Aggressive Flared Front Fenders
                .uv(32, 57).cuboid(4.0F, -10.0F, -13.0F, 7.0F, 2.0F, 9.0F)
                .uv(32, 57).cuboid(-11.0F, -10.0F, -13.0F, 7.0F, 2.0F, 9.0F)
                // Wide Flared Rear Mudguard Fenders
                .uv(64, 57).cuboid(4.0F, -9.5F, 3.5F, 7.0F, 2.0F, 10.0F)
                .uv(64, 57).cuboid(-11.0F, -9.5F, 3.5F, 7.0F, 2.0F, 10.0F)
                // Sport Fuel Tank & Billet Gas Cap
                .uv(0, 68).cuboid(-4.0F, -12.0F, -6.5F, 8.0F, 4.0F, 5.0F)
                .uv(26, 68).cuboid(-1.5F, -13.0F, -5.5F, 3.0F, 1.0F, 3.0F)
                // Ergonomic Sport Saddle Seat & Raised Passenger Tail
                .uv(38, 68).cuboid(-4.5F, -10.0F, -2.0F, 9.0F, 3.0F, 10.0F)
                .uv(76, 68).cuboid(-4.0F, -11.0F, 5.5F, 8.0F, 2.0F, 4.0F)
                // Steering Column Riser & Crossbar Handlebars
                .uv(0, 77).cuboid(-1.0F, -16.0F, -8.0F, 2.0F, 5.0F, 2.0F)
                .uv(8, 77).cuboid(-8.0F, -17.0F, -8.5F, 16.0F, 2.0F, 2.0F)
                // Ergonomic Handguards (Bark Busters)
                .uv(44, 77).cuboid(6.5F, -17.5F, -9.5F, 2.0F, 3.0F, 3.0F)
                .uv(44, 77).cuboid(-8.5F, -17.5F, -9.5F, 2.0F, 3.0F, 3.0F)
                // Digital Instrument Gauge Cluster / HUD
                .uv(54, 77).cuboid(-2.0F, -16.5F, -9.0F, 4.0F, 2.0F, 2.0F)
                // Heavy Tubular Front Bull-Bar & Winch
                .uv(66, 77).cuboid(-7.0F, -9.0F, -14.0F, 14.0F, 6.0F, 2.0F)
                .uv(98, 77).cuboid(-2.0F, -7.0F, -15.0F, 4.0F, 2.0F, 1.0F)
                // Rear Cargo Rack with Tie-Down Perimeter Rails
                .uv(0, 84).cuboid(-6.5F, -9.5F, 7.0F, 13.0F, 2.0F, 7.0F)
                .uv(40, 84).cuboid(-7.0F, -11.5F, 6.5F, 14.0F, 2.0F, 8.0F)
                // Integrated Rear LED Taillight Bar
                .uv(84, 84).cuboid(-4.0F, -8.0F, 14.0F, 8.0F, 2.0F, 1.0F),
                ModelTransform.of(0.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // 2. 4 Deep-Dish All-Terrain Wheels with Knobby Lugs and Alloy Rims
        ModelPartBuilder wheelBuilder = ModelPartBuilder.create()
                // Knobby Rubber Main Tire
                .uv(24, 93).cuboid(-2.5F, -4.5F, -4.5F, 5.0F, 9.0F, 9.0F)
                // Vertical Tread Lugs
                .uv(52, 93).cuboid(-2.0F, -5.0F, -3.5F, 4.0F, 10.0F, 7.0F)
                // Horizontal Tread Lugs
                .uv(74, 93).cuboid(-2.0F, -3.5F, -5.0F, 4.0F, 7.0F, 10.0F)
                // Deep-Dish Alloy Rim & Wheel Hub
                .uv(102, 93).cuboid(-2.75F, -2.5F, -2.5F, 5.5F, 5.0F, 5.0F);

        root.addChild("front_left_wheel", wheelBuilder,
                ModelTransform.of(9.5F, 19.5F, -8.0F, 0.0F, 0.0F, 0.0F));

        root.addChild("front_right_wheel", wheelBuilder,
                ModelTransform.of(-9.5F, 19.5F, -8.0F, 0.0F, 0.0F, 0.0F));

        root.addChild("back_left_wheel", wheelBuilder,
                ModelTransform.of(9.5F, 19.5F, 8.0F, 0.0F, 0.0F, 0.0F));

        root.addChild("back_right_wheel", wheelBuilder,
                ModelTransform.of(-9.5F, 19.5F, 8.0F, 0.0F, 0.0F, 0.0F));

        // 3. Front Mining Drill Bit (Mounted cleanly on front bull-bar)
        root.addChild("drill", ModelPartBuilder.create()
                .uv(0, 111).cuboid(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F)
                .uv(28, 111).cuboid(-3.0F, -3.0F, -10.0F, 6.0F, 6.0F, 4.0F)
                .uv(48, 111).cuboid(-2.0F, -2.0F, -14.0F, 4.0F, 4.0F, 4.0F)
                .uv(64, 111).cuboid(-1.0F, -1.0F, -16.0F, 2.0F, 2.0F, 2.0F),
                ModelTransform.of(0.0F, 16.0F, -14.0F, 0.0F, 0.0F, 0.0F));

        // 4. Front Lumberjack Tree Saw Blade
        root.addChild("tree_saw", ModelPartBuilder.create()
                .uv(0, 120).cuboid(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 4.0F)
                .uv(16, 120).cuboid(-7.0F, -7.0F, -1.0F, 14.0F, 14.0F, 1.0F)
                .uv(46, 120).cuboid(-8.0F, -8.0F, -0.5F, 16.0F, 16.0F, 0.5F),
                ModelTransform.of(0.0F, 16.0F, -15.0F, 0.0F, 0.0F, 0.0F));

        // 5. Front Agricultural Crop Harvester Reel
        root.addChild("crop_harvester", ModelPartBuilder.create()
                .uv(46, 120).cuboid(-11.0F, -1.0F, -1.0F, 22.0F, 2.0F, 2.0F)
                .uv(66, 120).cuboid(-11.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F)
                .uv(66, 120).cuboid(10.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F)
                .uv(46, 120).cuboid(-10.0F, -3.0F, -3.0F, 20.0F, 1.0F, 6.0F)
                .uv(46, 120).cuboid(-10.0F, 2.0F, -3.0F, 20.0F, 1.0F, 6.0F),
                ModelTransform.of(0.0F, 18.0F, -15.0F, 0.0F, 0.0F, 0.0F));

        // 6. Dual Projector Headlights & Auxiliary LED Bar
        root.addChild("headlights", ModelPartBuilder.create()
                .uv(0, 93).cuboid(-6.0F, -10.0F, -13.5F, 4.0F, 3.0F, 2.0F)
                .uv(0, 93).cuboid(2.0F, -10.0F, -13.5F, 4.0F, 3.0F, 2.0F)
                .uv(12, 93).cuboid(-2.0F, -11.0F, -13.5F, 4.0F, 2.0F, 2.0F),
                ModelTransform.of(0.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(AtvRenderState state) {
        super.setAngles(state);
        float rot = state.wheelRotation * 0.1f;
        this.frontLeftWheel.pitch = rot;
        this.frontRightWheel.pitch = rot;
        this.backLeftWheel.pitch = rot;
        this.backRightWheel.pitch = rot;

        this.drill.visible = (state.attachmentType == 1);
        this.treeSaw.visible = (state.attachmentType == 2);
        this.cropHarvester.visible = (state.attachmentType == 3);

        this.drill.roll = (float) Math.toRadians(state.toolSpin);
        this.treeSaw.pitch = (float) Math.toRadians(state.toolSpin);
        this.cropHarvester.pitch = (float) Math.toRadians(state.toolSpin);
    }
}

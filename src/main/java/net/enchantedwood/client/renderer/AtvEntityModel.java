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

        // 1. Main Body / Chassis Frame
        root.addChild("body", ModelPartBuilder.create()
                // Lower Chassis Base (16x4x24)
                .uv(0, 0).cuboid(-8.0F, -6.0F, -12.0F, 16.0F, 4.0F, 24.0F)
                // Driver Seat (12x4x10)
                .uv(0, 28).cuboid(-6.0F, -10.0F, -4.0F, 12.0F, 4.0F, 10.0F)
                // Front Hood / Engine Block (12x6x8)
                .uv(0, 42).cuboid(-6.0F, -12.0F, -12.0F, 12.0F, 6.0F, 8.0F)
                // Handlebars Column (2x8x2)
                .uv(40, 42).cuboid(-1.0F, -18.0F, -8.0F, 2.0F, 6.0F, 2.0F)
                // Handlebars Bar (16x2x2)
                .uv(0, 56).cuboid(-8.0F, -19.0F, -8.0F, 16.0F, 2.0F, 2.0F)
                // Front Bull-Bar (14x6x2)
                .uv(36, 56).cuboid(-7.0F, -8.0F, -14.0F, 14.0F, 6.0F, 2.0F)
                // Rear Cargo Rack (14x4x6)
                .uv(0, 60).cuboid(-7.0F, -8.0F, 7.0F, 14.0F, 2.0F, 6.0F),
                ModelTransform.of(0.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        // 2. 4 Heavy All-Terrain Rubber Wheels (4x8x8 each)
        Dilation wheelDilation = new Dilation(0.0F);

        root.addChild("front_left_wheel", ModelPartBuilder.create()
                .uv(56, 0).cuboid(-2.0F, -4.0F, -4.0F, 4.0F, 8.0F, 8.0F, wheelDilation),
                ModelTransform.of(9.0F, 20.0F, -8.0F, 0.0F, 0.0F, 0.0F));

        root.addChild("front_right_wheel", ModelPartBuilder.create()
                .uv(56, 0).cuboid(-2.0F, -4.0F, -4.0F, 4.0F, 8.0F, 8.0F, wheelDilation),
                ModelTransform.of(-9.0F, 20.0F, -8.0F, 0.0F, 0.0F, 0.0F));

        root.addChild("back_left_wheel", ModelPartBuilder.create()
                .uv(56, 16).cuboid(-2.0F, -4.0F, -4.0F, 4.0F, 8.0F, 8.0F, wheelDilation),
                ModelTransform.of(9.0F, 20.0F, 8.0F, 0.0F, 0.0F, 0.0F));

        root.addChild("back_right_wheel", ModelPartBuilder.create()
                .uv(56, 16).cuboid(-2.0F, -4.0F, -4.0F, 4.0F, 8.0F, 8.0F, wheelDilation),
                ModelTransform.of(-9.0F, 20.0F, 8.0F, 0.0F, 0.0F, 0.0F));

        // 3. Front Mining Drill Bit (Cone attached to front bumper)
        root.addChild("drill", ModelPartBuilder.create()
                // Base drill cylinder (8x8x6)
                .uv(0, 68).cuboid(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F)
                // Mid taper (6x6x4)
                .uv(28, 68).cuboid(-3.0F, -3.0F, -10.0F, 6.0F, 6.0F, 4.0F)
                // Sharp cone tip (4x4x4)
                .uv(48, 68).cuboid(-2.0F, -2.0F, -14.0F, 4.0F, 4.0F, 4.0F)
                // Center drill spindle (2x2x2)
                .uv(64, 68).cuboid(-1.0F, -1.0F, -16.0F, 2.0F, 2.0F, 2.0F),
                ModelTransform.of(0.0F, 16.0F, -14.0F, 0.0F, 0.0F, 0.0F));

        // 4. Front Lumberjack Tree Saw Blade (Large circular blade)
        root.addChild("tree_saw", ModelPartBuilder.create()
                // Center Hub (4x4x4)
                .uv(0, 82).cuboid(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 4.0F)
                // Circular Saw Blade (14x14x1)
                .uv(16, 82).cuboid(-7.0F, -7.0F, -1.0F, 14.0F, 14.0F, 1.0F)
                // Cross cutting teeth (16x16x1)
                .uv(46, 82).cuboid(-8.0F, -8.0F, -0.5F, 16.0F, 16.0F, 0.5F),
                ModelTransform.of(0.0F, 16.0F, -15.0F, 0.0F, 0.0F, 0.0F));

        // 5. Front Agricultural Crop Harvester (Wide combine cylinder reel)
        root.addChild("crop_harvester", ModelPartBuilder.create()
                // Central Reel Axle (22x2x2)
                .uv(0, 98).cuboid(-11.0F, -1.0F, -1.0F, 22.0F, 2.0F, 2.0F)
                // Outer Left Wheel (1x8x8)
                .uv(48, 98).cuboid(-11.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F)
                // Outer Right Wheel (1x8x8)
                .uv(48, 98).cuboid(10.0F, -4.0F, -4.0F, 1.0F, 8.0F, 8.0F)
                // Combine Reel Slats (20x1x6)
                .uv(0, 102).cuboid(-10.0F, -3.0F, -3.0F, 20.0F, 1.0F, 6.0F)
                .uv(0, 102).cuboid(-10.0F, 2.0F, -3.0F, 20.0F, 1.0F, 6.0F),
                ModelTransform.of(0.0F, 18.0F, -15.0F, 0.0F, 0.0F, 0.0F));

        // 6. Dual Automotive Headlights
        root.addChild("headlights", ModelPartBuilder.create()
                // Left Light (3x3x2)
                .uv(0, 110).cuboid(-6.0F, -10.0F, -14.5F, 3.0F, 3.0F, 2.0F)
                // Right Light (3x3x2)
                .uv(0, 110).cuboid(3.0F, -10.0F, -14.5F, 3.0F, 3.0F, 2.0F),
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

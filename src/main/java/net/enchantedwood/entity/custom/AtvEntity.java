package net.enchantedwood.entity.custom;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.entity.ModEntities;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.AtvScreenHandler;
import net.enchantedwood.item.custom.CropHarvesterItem;
import net.enchantedwood.item.custom.DrillBitItem;
import net.enchantedwood.item.custom.HeadlightsItem;
import net.enchantedwood.item.custom.TreeSawItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CocoaBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AtvEntity extends Entity implements NamedScreenHandlerFactory, Inventory {

    // 8 Core Slots: 0:Engine, 1:Tires, 2:Suspension, 3:Chassis, 4:Headlights, 5:Trunk, 6:Fuel/Battery, 7:Tool (Drill/Saw/Harvester)
    public static final int ENGINE_SLOT = 0;
    public static final int TIRE_SLOT = 1;
    public static final int SUSPENSION_SLOT = 2;
    public static final int CHASSIS_SLOT = 3;
    public static final int HEADLIGHT_SLOT = 4;
    public static final int TRUNK_SLOT = 5;
    public static final int FUEL_SLOT = 6;
    public static final int DRILL_SLOT = 7;
    public static final int TOOL_SLOT = 7;

    public static final int ATTACHMENT_NONE = 0;
    public static final int ATTACHMENT_DRILL = 1;
    public static final int ATTACHMENT_TREE_SAW = 2;
    public static final int ATTACHMENT_CROP_HARVESTER = 3;

    public static final int MODULE_SLOTS_COUNT = 8;
    public static final int MAX_TRUNK_SLOTS = 27;
    public static final int TOTAL_INVENTORY_SIZE = MODULE_SLOTS_COUNT + MAX_TRUNK_SLOTS;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(TOTAL_INVENTORY_SIZE, ItemStack.EMPTY);

    // Synced Data
    private static final TrackedData<Float> SPEED = DataTracker.registerData(AtvEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> FUEL_LEVEL = DataTracker.registerData(AtvEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MAX_FUEL = DataTracker.registerData(AtvEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ATTACHMENT_TYPE = DataTracker.registerData(AtvEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> HEADLIGHTS_ACTIVE = DataTracker.registerData(AtvEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    // Movement & Physics
    private float targetSpeed = 0.0f;
    private float currentSpeed = 0.0f;
    public float wheelRotation = 0.0f;
    public float toolSpin = 0.0f;
    public float drillSpin = 0.0f;
    private int fuelBurnTime = 0;
    private int toolCooldown = 0;
    private BlockPos dynamicLightPos = null;

    public AtvEntity(EntityType<? extends Entity> type, World world) {
        super(type, world);
        this.intersectionChecked = true;
    }

    public AtvEntity(World world, double x, double y, double z) {
        this(ModEntities.ATV, world);
        this.setPosition(x, y, z);
    }

    @Override
    public float getStepHeight() {
        // Tiered Step Height based on installed Tires & Suspension
        float step = 1.25f; // Base Rubber Tires climb dirt paths & 1-block steps

        ItemStack tires = inventory.get(TIRE_SLOT);
        if (tires.isOf(ModItems.STEEL_RIM_TIRE)) {
            step = 1.5f;
        } else if (tires.isOf(ModItems.TITANIUM_STUDDED_TIRE)) {
            step = 1.75f;
        }

        ItemStack suspension = inventory.get(SUSPENSION_SLOT);
        if (suspension.isOf(ModItems.STEEL_SUSPENSION)) {
            step += 0.1f;
        } else if (suspension.isOf(ModItems.TITANIUM_SUSPENSION)) {
            step += 0.25f; // Up to 2.0 blocks clearance with Titanium Tires + Suspension
        }

        return step;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(SPEED, 0.0f);
        builder.add(FUEL_LEVEL, 0);
        builder.add(MAX_FUEL, 1000);
        builder.add(ATTACHMENT_TYPE, ATTACHMENT_NONE);
        builder.add(HEADLIGHTS_ACTIVE, false);
    }

    public float getDisplaySpeed() {
        return this.dataTracker.get(SPEED);
    }

    public int getFuelLevel() {
        return this.dataTracker.get(FUEL_LEVEL);
    }

    public int getMaxFuel() {
        return this.dataTracker.get(MAX_FUEL);
    }

    public void setFuelLevel(int fuel, int max) {
        this.dataTracker.set(FUEL_LEVEL, Math.max(0, fuel));
        this.dataTracker.set(MAX_FUEL, Math.max(1, max));
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean canHit() {
        return !this.isRemoved();
    }

    @Override
    public boolean isCollidable(Entity other) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean collidesWith(Entity other) {
        return (other.isCollidable(this) || other.isPushable()) && !this.isConnectedThroughVehicle(other);
    }

    @Override
    protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3d(0.0, 0.78 * scale, -0.1 * scale);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (this.isInvulnerable()) return false;
        if (!this.isRemoved()) {
            ItemStack drop = new ItemStack(ModItems.ATV_ITEM);
            writeInventoryToItem(drop);
            this.dropStack(world, drop);
            this.discard();
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity first = this.getFirstPassenger();
        return first instanceof LivingEntity living ? living : null;
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        World world = this.getEntityWorld();
        ItemStack held = player.getStackInHand(hand);

        if (player.isSneaking()) {
            if (!world.isClient()) {
                // Only dismantle if holding a Wrench or Gear
                if (held.isOf(ModItems.WRENCH) || held.isOf(ModItems.COPPER_GEAR) || held.isOf(ModItems.STEEL_GEAR)) {
                    ItemStack atvDrop = new ItemStack(ModItems.ATV_ITEM);
                    writeInventoryToItem(atvDrop);
                    this.dropStack((ServerWorld) world, atvDrop);
                    this.discard();
                    return ActionResult.SUCCESS;
                }
                // Shift + Right-Click with empty hand or other items -> Open ATV Dashboard GUI!
                player.openHandledScreen(this);
            }
            return ActionResult.SUCCESS;
        }

        if (!world.isClient()) {
            net.enchantedwood.item.custom.AtvItem.triggerAnomaly2Unlock(player, world);
            if (!this.hasPassengers()) {
                player.startRiding(this);
            } else {
                player.openHandledScreen(this);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void onRemoved() {
        if (!this.getEntityWorld().isClient() && this.dynamicLightPos != null) {
            if (this.getEntityWorld().getBlockState(this.dynamicLightPos).isOf(net.minecraft.block.Blocks.LIGHT)) {
                this.getEntityWorld().removeBlock(this.dynamicLightPos, false);
            }
            this.dynamicLightPos = null;
        }
        super.onRemoved();
    }

    public void writeInventoryToItem(ItemStack stack) {
        NbtCompound tag = new NbtCompound();
        tag.putInt("FuelLevel", getFuelLevel());
        tag.putInt("MaxFuel", getMaxFuel());

        for (int i = 0; i < TOTAL_INVENTORY_SIZE; i++) {
            ItemStack s = this.inventory.get(i);
            if (!s.isEmpty()) {
                tag.putString("Slot_" + i, Registries.ITEM.getId(s.getItem()).toString());
                tag.putInt("Count_" + i, s.getCount());
                if (s.isDamageable() && s.getDamage() > 0) {
                    tag.putInt("Damage_" + i, s.getDamage());
                }
            }
        }

        stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(tag));
    }

    public void readInventoryFromItem(ItemStack stack) {
        net.minecraft.component.type.NbtComponent component = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        if (component != null) {
            NbtCompound tag = component.copyNbt();
            int fuel = tag.getInt("FuelLevel", 0);
            int max = tag.getInt("MaxFuel", 1000);
            setFuelLevel(fuel, max);

            this.inventory.clear();
            for (int i = 0; i < TOTAL_INVENTORY_SIZE; i++) {
                if (tag.contains("Slot_" + i)) {
                    String itemId = tag.getString("Slot_" + i).orElse("");
                    if (!itemId.isEmpty()) {
                        net.minecraft.util.Identifier id = net.minecraft.util.Identifier.of(itemId);
                        if (Registries.ITEM.containsId(id)) {
                            int count = tag.getInt("Count_" + i).orElse(1);
                            ItemStack restored = new ItemStack(Registries.ITEM.get(id), Math.max(1, count));
                            if (tag.contains("Damage_" + i)) {
                                restored.setDamage(tag.getInt("Damage_" + i).orElse(0));
                            }
                            this.inventory.set(i, restored);
                        }
                    }
                }
            }

            // Fallback / legacy Headlights & Trunk slot migration
            if (this.inventory.get(HEADLIGHT_SLOT).isEmpty() && tag.contains("Headlights")) {
                String lightId = tag.getString("Headlights").orElse("");
                if (!lightId.isEmpty()) {
                    net.minecraft.util.Identifier id = net.minecraft.util.Identifier.of(lightId);
                    if (Registries.ITEM.containsId(id)) {
                        this.inventory.set(HEADLIGHT_SLOT, new ItemStack(Registries.ITEM.get(id), 1));
                    }
                }
            }

            // If Slot_4 contained a trunk in an older save, move it to Slot_5 (TRUNK_SLOT)
            ItemStack slot4 = this.inventory.get(HEADLIGHT_SLOT);
            if (slot4.isOf(ModItems.SMALL_CARGO_TRUNK) || slot4.isOf(ModItems.MEDIUM_CARGO_TRUNK) || slot4.isOf(ModItems.LARGE_CARGO_TRUNK)) {
                this.inventory.set(TRUNK_SLOT, slot4);
                this.inventory.set(HEADLIGHT_SLOT, new ItemStack(ModItems.HALOGEN_HEADLIGHTS, 1));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity rider = this.getControllingPassenger();

        if (this.isLogicalSideForUpdatingMovement()) {
            // Player Driving on Client / Unoccupied Vehicle on Server
            if (rider instanceof PlayerEntity player) {
                handleRiderControl(player);
            } else {
                this.currentSpeed = MathHelper.lerp(0.1f, this.currentSpeed, 0.0f);
                this.targetSpeed = 0.0f;
            }

            applyMovementPhysics();
        } else {
            this.setVelocity(Vec3d.ZERO);
        }

        World world = this.getEntityWorld();
        if (world.isClient()) {
            this.wheelRotation += this.currentSpeed * 25.0f;
            if (getAttachmentType() != ATTACHMENT_NONE) {
                this.toolSpin += 30.0f;
                this.drillSpin = this.toolSpin;
            }
        } else {
            // Server side fuel, exhaust, tool execution, sync & gauges
            int att = ATTACHMENT_NONE;
            ItemStack tool = this.inventory.get(TOOL_SLOT);
            if (tool.getItem() instanceof DrillBitItem) att = ATTACHMENT_DRILL;
            else if (tool.getItem() instanceof TreeSawItem) att = ATTACHMENT_TREE_SAW;
            else if (tool.getItem() instanceof CropHarvesterItem) att = ATTACHMENT_CROP_HARVESTER;
            this.dataTracker.set(ATTACHMENT_TYPE, att);

            ItemStack lightsStack = this.inventory.get(HEADLIGHT_SLOT);
            boolean hasLights = lightsStack.getItem() instanceof HeadlightsItem;
            int lightLevel = 15;
            if (hasLights && lightsStack.getItem() instanceof HeadlightsItem hItem) {
                lightLevel = hItem.getTier().getLightLevel();
            }
            int skyLight = world.getLightLevel(net.minecraft.world.LightType.SKY, this.getBlockPos()) - world.getAmbientDarkness();
            boolean dark = hasLights && skyLight <= 7;
            this.dataTracker.set(HEADLIGHTS_ACTIVE, dark);

            if (dark && rider != null) {
                Vec3d forward = this.getRotationVector().normalize();
                BlockPos targetLight = BlockPos.ofFloored(this.getEntityPos().add(forward.multiply(2.5)).add(0, 1.0, 0));
                if (this.dynamicLightPos != null && !this.dynamicLightPos.equals(targetLight)) {
                    if (world.getBlockState(this.dynamicLightPos).isOf(net.minecraft.block.Blocks.LIGHT)) {
                        world.removeBlock(this.dynamicLightPos, false);
                    }
                    this.dynamicLightPos = null;
                }
                if (world.getBlockState(targetLight).isAir()) {
                    world.setBlockState(targetLight, net.minecraft.block.Blocks.LIGHT.getDefaultState().with(net.minecraft.block.LightBlock.LEVEL_15, lightLevel), net.minecraft.block.Block.NOTIFY_ALL);
                    this.dynamicLightPos = targetLight;
                }
            } else {
                if (this.dynamicLightPos != null) {
                    if (world.getBlockState(this.dynamicLightPos).isOf(net.minecraft.block.Blocks.LIGHT)) {
                        world.removeBlock(this.dynamicLightPos, false);
                    }
                    this.dynamicLightPos = null;
                }
            }

            processFuel();
            if (rider != null) {
                if (Math.abs(this.currentSpeed) > 0.05f) {
                    consumeFuel(1);
                    if (this.random.nextInt(3) == 0) {
                        Vec3d exhaustPos = this.getEntityPos().subtract(this.getRotationVector().multiply(0.9)).add(0, 0.4, 0);
                        ((ServerWorld) world).spawnParticles(ParticleTypes.SMOKE, exhaustPos.x, exhaustPos.y, exhaustPos.z, 2, 0.1, 0.1, 0.1, 0.02);
                    }
                }
                if (att == ATTACHMENT_DRILL) processDrilling((ServerWorld) world, rider);
                else if (att == ATTACHMENT_TREE_SAW) processTreeSawing((ServerWorld) world, rider);
                else if (att == ATTACHMENT_CROP_HARVESTER) processCropHarvesting((ServerWorld) world, rider);
            }
            this.dataTracker.set(SPEED, this.currentSpeed * 72.0f);
        }
    }

    private boolean getForwardInput(LivingEntity rider) {
        if (rider instanceof ServerPlayerEntity sp) {
            return sp.getPlayerInput().forward();
        }
        if (rider instanceof PlayerEntity p) {
            return p.forwardSpeed > 0.1f;
        }
        return false;
    }

    private boolean getBackwardInput(LivingEntity rider) {
        if (rider instanceof ServerPlayerEntity sp) {
            return sp.getPlayerInput().backward();
        }
        if (rider instanceof PlayerEntity p) {
            return p.forwardSpeed < -0.1f;
        }
        return false;
    }

    private boolean getLeftInput(LivingEntity rider) {
        if (rider instanceof ServerPlayerEntity sp) {
            return sp.getPlayerInput().left();
        }
        if (rider instanceof PlayerEntity p) {
            return p.sidewaysSpeed > 0.1f;
        }
        return false;
    }

    private boolean getRightInput(LivingEntity rider) {
        if (rider instanceof ServerPlayerEntity sp) {
            return sp.getPlayerInput().right();
        }
        if (rider instanceof PlayerEntity p) {
            return p.sidewaysSpeed < -0.1f;
        }
        return false;
    }

    private void handleRiderControl(PlayerEntity player) {
        float forward = 0.0f;
        float sideways = 0.0f;

        boolean pressForward = getForwardInput(player);
        boolean pressBackward = getBackwardInput(player);
        boolean pressLeft = getLeftInput(player);
        boolean pressRight = getRightInput(player);

        if (pressForward) forward += 1.0f;
        if (pressBackward) forward -= 1.0f;
        if (pressLeft) sideways += 1.0f;
        if (pressRight) sideways -= 1.0f;

        // 1. Steering sensitivity based on installed Tires
        float turnSpeed = 3.5f;
        ItemStack tires = inventory.get(TIRE_SLOT);
        if (tires.isOf(ModItems.STEEL_RIM_TIRE)) {
            turnSpeed = 4.2f;
        } else if (tires.isOf(ModItems.TITANIUM_STUDDED_TIRE)) {
            turnSpeed = 5.0f;
        }

        if (pressLeft) {
            this.setYaw(this.getYaw() - turnSpeed);
        }
        if (pressRight) {
            this.setYaw(this.getYaw() + turnSpeed);
        }
        if (forward != 0) {
            this.setYaw(MathHelper.lerpAngleDegrees(0.15f, this.getYaw(), player.getYaw()));
        }

        // 2. Engine max speed and acceleration calculation
        float maxForwardSpeed = 0.35f; // Base starter speed (~25 km/h)
        float accelRate = 0.04f;

        ItemStack engine = inventory.get(ENGINE_SLOT);
        if (engine.isOf(ModItems.COPPER_ATV_ENGINE)) {
            maxForwardSpeed = 0.40f;
            accelRate = 0.035f;
        } else if (engine.isOf(ModItems.ALUMINUM_ATV_ENGINE)) {
            maxForwardSpeed = 0.55f;
            accelRate = 0.05f;
        } else if (engine.isOf(ModItems.STEEL_ATV_ENGINE)) {
            maxForwardSpeed = 0.75f;
            accelRate = 0.04f;
        } else if (engine.isOf(ModItems.TITANIUM_ATV_ENGINE)) {
            maxForwardSpeed = 1.05f;
            accelRate = 0.07f;
        }

        // 3. Chassis weight & performance modifiers
        ItemStack chassis = inventory.get(CHASSIS_SLOT);
        if (chassis.isOf(ModItems.ALUMINUM_ATV_CHASSIS)) {
            accelRate *= 1.20f; // Lightweight aluminum accelerates 20% faster
        } else if (chassis.isOf(ModItems.TITANIUM_ATV_CHASSIS)) {
            maxForwardSpeed *= 1.10f; // Titanium gives +10% top speed & +25% acceleration
            accelRate *= 1.25f;
        }

        // 4. Asphalt speed bonus (+25%)
        BlockPos below = this.getBlockPos().down();
        if (this.getEntityWorld().getBlockState(below).isOf(ModBlocks.ASPHALT_BLOCK) || this.getEntityWorld().getBlockState(this.getBlockPos()).isOf(ModBlocks.ASPHALT_SLAB)) {
            maxForwardSpeed *= 1.25f;
        }

        boolean hasPower = getFuelLevel() > 0;

        if (hasPower && forward > 0) {
            this.targetSpeed = maxForwardSpeed * forward;
        } else if (hasPower && forward < 0) {
            this.targetSpeed = -maxForwardSpeed * 0.4f; // Reverse speed
        } else {
            this.targetSpeed = 0.0f;
        }

        // Smooth acceleration & braking
        this.currentSpeed = MathHelper.lerp(accelRate, this.currentSpeed, this.targetSpeed);
    }

    private void applyMovementPhysics() {
        Vec3d look = this.getRotationVector();
        Vec3d horizontalMovement = new Vec3d(look.x * this.currentSpeed, 0, look.z * this.currentSpeed);

        Vec3d velocity = this.getVelocity();
        double gravity = this.hasNoGravity() ? 0.0 : (this.isOnGround() ? 0.0 : -0.05);

        this.setVelocity(horizontalMovement.x, velocity.y + gravity, horizontalMovement.z);
        this.move(MovementType.SELF, this.getVelocity());

        // Apply ground friction
        this.setVelocity(this.getVelocity().multiply(0.85, 0.98, 0.85));
    }

    private void processFuel() {
        ItemStack fuelStack = inventory.get(FUEL_SLOT);
        if (!fuelStack.isEmpty() && getFuelLevel() <= getMaxFuel() - 200) {
            // 1. Refuel from Gasoline Canister (+1,000 Fuel)
            if (fuelStack.isOf(ModItems.GASOLINE_CANISTER)) {
                setFuelLevel(getFuelLevel() + 1000, 2000);
                fuelStack.decrement(1);
                this.dropOrStoreEmptyCanister();
            }
            // 2. Refuel from Biofuel Canister (+600 Fuel)
            else if (fuelStack.isOf(ModItems.BIOFUEL_CANISTER)) {
                setFuelLevel(getFuelLevel() + 600, 2000);
                fuelStack.decrement(1);
                this.dropOrStoreEmptyCanister();
            }
            // 3. Refuel from High-Octane Canister (+1,500 Fuel)
            else if (fuelStack.isOf(ModItems.HIGH_OCTANE_FUEL_CANISTER)) {
                setFuelLevel(getFuelLevel() + 1500, 3000);
                fuelStack.decrement(1);
                this.dropOrStoreEmptyCanister();
            }
            // 4. Solid Fuel (Coal / Charcoal) (+200 Fuel)
            else if (fuelStack.isOf(net.minecraft.item.Items.COAL) || fuelStack.isOf(net.minecraft.item.Items.CHARCOAL)) {
                setFuelLevel(getFuelLevel() + 200, 1000);
                fuelStack.decrement(1);
            }
            // 5. Battery Charge
            else if (fuelStack.getItem() instanceof net.enchantedwood.energy.ItemEnergyProvider itemProvider) {
                EnergyStorage storage = itemProvider.getEnergyStorage(fuelStack);
                if (storage != null && storage.getEnergy() >= 100) {
                    int extracted = storage.extractEnergy(100, false);
                    setFuelLevel(getFuelLevel() + extracted, 5000);
                }
            } else if (fuelStack.getItem() instanceof EnergyProvider provider) {
                EnergyStorage storage = provider.getEnergyStorage(null);
                if (storage != null && storage.getEnergy() >= 100) {
                    int extracted = storage.extractEnergy(100, false);
                    setFuelLevel(getFuelLevel() + extracted, 5000);
                }
            }
        }
    }

    private void dropOrStoreEmptyCanister() {
        ItemStack empty = new ItemStack(ModItems.EMPTY_GAS_CANISTER);
        if (!this.inventory.get(FUEL_SLOT).isEmpty()) {
            this.dropStack((ServerWorld) this.getEntityWorld(), empty);
        } else {
            this.inventory.set(FUEL_SLOT, empty);
        }
    }

    private void consumeFuel(int amount) {
        if (++this.fuelBurnTime % 4 == 0) {
            setFuelLevel(getFuelLevel() - amount, getMaxFuel());
        }
    }

    public ItemStack getDrillBit() {
        return this.inventory.get(DRILL_SLOT);
    }

    public int getAttachmentType() {
        if (this.getEntityWorld().isClient()) {
            return this.dataTracker.get(ATTACHMENT_TYPE);
        }
        ItemStack tool = this.inventory.get(TOOL_SLOT);
        if (tool.getItem() instanceof DrillBitItem) return ATTACHMENT_DRILL;
        if (tool.getItem() instanceof TreeSawItem) return ATTACHMENT_TREE_SAW;
        if (tool.getItem() instanceof CropHarvesterItem) return ATTACHMENT_CROP_HARVESTER;
        return ATTACHMENT_NONE;
    }

    public boolean hasDrillBit() {
        return getAttachmentType() == ATTACHMENT_DRILL;
    }

    public boolean areHeadlightsActive() {
        return this.dataTracker.get(HEADLIGHTS_ACTIVE);
    }

    public int getTrunkCapacity() {
        ItemStack trunk = this.inventory.get(TRUNK_SLOT);
        if (trunk.isOf(ModItems.LARGE_CARGO_TRUNK)) return 27;
        if (trunk.isOf(ModItems.MEDIUM_CARGO_TRUNK)) return 18;
        if (trunk.isOf(ModItems.SMALL_CARGO_TRUNK)) return 9;
        return 0;
    }

    public ItemStack insertIntoTrunk(ItemStack stack) {
        int cap = getTrunkCapacity();
        if (cap <= 0) return stack;

        // Try merge with existing stacks
        for (int i = 0; i < cap; i++) {
            int slot = MODULE_SLOTS_COUNT + i;
            ItemStack current = this.inventory.get(slot);
            if (ItemStack.areItemsAndComponentsEqual(current, stack)) {
                int space = current.getMaxCount() - current.getCount();
                int add = Math.min(space, stack.getCount());
                current.increment(add);
                stack.decrement(add);
                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
        }

        // Try empty slots
        for (int i = 0; i < cap; i++) {
            int slot = MODULE_SLOTS_COUNT + i;
            ItemStack current = this.inventory.get(slot);
            if (current.isEmpty()) {
                this.inventory.set(slot, stack.copy());
                return ItemStack.EMPTY;
            }
        }

        return stack;
    }

    private void processDrilling(ServerWorld world, LivingEntity rider) {
        if (this.toolCooldown > 0) {
            this.toolCooldown--;
            return;
        }

        ItemStack drillStack = this.inventory.get(TOOL_SLOT);
        if (!(drillStack.getItem() instanceof DrillBitItem drillBit)) {
            return;
        }

        if (this.currentSpeed < 0.05f && !getForwardInput(rider)) {
            return;
        }

        float yawRad = (float) Math.toRadians(this.getYaw());
        double dx = -Math.sin(yawRad);
        double dz = Math.cos(yawRad);
        Vec3d forwardVec = new Vec3d(dx, 0, dz).normalize();

        Vec3d drillCenter = this.getEntityPos().add(forwardVec.multiply(1.3)).add(0, 0.5, 0);
        BlockPos basePos = BlockPos.ofFloored(drillCenter);

        Vec3d rightVec = new Vec3d(-dz, 0, dx).normalize();

        List<BlockPos> targetPositions = new ArrayList<>();
        int y0 = (int) Math.floor(this.getY());
        int y1 = y0 + 1;

        BlockPos p1 = basePos.withY(y0);
        BlockPos p2 = basePos.withY(y1);
        BlockPos p3 = BlockPos.ofFloored(drillCenter.add(rightVec.multiply(0.55))).withY(y0);
        BlockPos p4 = BlockPos.ofFloored(drillCenter.add(rightVec.multiply(0.55))).withY(y1);
        BlockPos p5 = BlockPos.ofFloored(drillCenter.subtract(rightVec.multiply(0.55))).withY(y0);
        BlockPos p6 = BlockPos.ofFloored(drillCenter.subtract(rightVec.multiply(0.55))).withY(y1);

        for (BlockPos p : List.of(p1, p2, p3, p4, p5, p6)) {
            if (!targetPositions.contains(p)) {
                targetPositions.add(p);
            }
        }

        boolean drilledAny = false;

        for (BlockPos targetPos : targetPositions) {
            BlockState state = world.getBlockState(targetPos);
            if (state.isAir() || !state.getFluidState().isEmpty()) continue;
            if (!drillBit.canHarvest(state)) continue;

            List<ItemStack> drops = Block.getDroppedStacks(state, world, targetPos, world.getBlockEntity(targetPos), rider, drillStack);

            world.breakBlock(targetPos, false, rider);

            for (ItemStack drop : drops) {
                ItemStack remaining = insertIntoTrunk(drop);
                if (!remaining.isEmpty()) {
                    Block.dropStack(world, targetPos, remaining);
                }
            }

            world.playSound(null, targetPos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.5f, 1.2f);
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                    targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                    6, 0.2, 0.2, 0.2, 0.05);

            if (rider instanceof PlayerEntity player && !player.isCreative()) {
                int newDamage = drillStack.getDamage() + 1;
                if (newDamage >= drillStack.getMaxDamage()) {
                    this.inventory.set(TOOL_SLOT, ItemStack.EMPTY);
                    world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ITEM_BREAK.value(), SoundCategory.PLAYERS, 1.0f, 1.0f);
                    player.sendMessage(Text.literal("§c[ATV] Drill bit broke!"), true);
                    break;
                } else {
                    drillStack.setDamage(newDamage);
                }
            }

            drilledAny = true;
            break;
        }

        if (drilledAny) {
            int baseDelay = Math.max(1, (int) (6 / drillBit.getTier().getSpeedMultiplier()));
            this.toolCooldown = baseDelay;
        }
    }

    private void processTreeSawing(ServerWorld world, LivingEntity rider) {
        if (this.toolCooldown > 0) {
            this.toolCooldown--;
            return;
        }

        ItemStack toolStack = this.inventory.get(TOOL_SLOT);
        if (!(toolStack.getItem() instanceof TreeSawItem treeSaw)) {
            return;
        }

        if (this.currentSpeed < 0.05f && !getForwardInput(rider)) {
            return;
        }

        float yawRad = (float) Math.toRadians(this.getYaw());
        double dx = -Math.sin(yawRad);
        double dz = Math.cos(yawRad);
        Vec3d forwardVec = new Vec3d(dx, 0, dz).normalize();

        Vec3d sawCenter = this.getEntityPos().add(forwardVec.multiply(1.3)).add(0, 0.5, 0);
        BlockPos basePos = BlockPos.ofFloored(sawCenter);

        BlockPos startLog = null;
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos check = basePos.up(dy);
            if (treeSaw.canHarvest(world.getBlockState(check))) {
                startLog = check;
                break;
            }
        }

        if (startLog == null) return;

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> logsToBreak = new ArrayList<>();
        List<BlockPos> leavesToBreak = new ArrayList<>();

        queue.add(startLog);
        visited.add(startLog);

        int maxLogs = treeSaw.getTier().getMaxLogsPerTree();
        int maxCanopy = maxLogs * 4;

        while (!queue.isEmpty() && logsToBreak.size() < maxLogs) {
            BlockPos current = queue.poll();
            BlockState st = world.getBlockState(current);

            boolean isLog = st.isIn(BlockTags.LOGS) || st.isOf(net.minecraft.block.Blocks.BAMBOO)
                    || st.isOf(net.minecraft.block.Blocks.BAMBOO_SAPLING) || st.isOf(net.minecraft.block.Blocks.SUGAR_CANE)
                    || st.isOf(net.minecraft.block.Blocks.CACTUS);

            if (isLog) {
                logsToBreak.add(current);
            } else if (leavesToBreak.size() < maxCanopy) {
                leavesToBreak.add(current);
            }

            // Always expand neighbor branches and leaves upward and outward
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 3; oy++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        BlockPos neighbor = current.add(ox, oy, oz);
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            BlockState nState = world.getBlockState(neighbor);
                            if (treeSaw.canHarvest(nState)) {
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        if (logsToBreak.isEmpty()) return;

        int chopped = 0;
        for (BlockPos logPos : logsToBreak) {
            BlockState state = world.getBlockState(logPos);
            List<ItemStack> drops = Block.getDroppedStacks(state, world, logPos, world.getBlockEntity(logPos), rider, toolStack);
            world.breakBlock(logPos, false, rider);

            for (ItemStack drop : drops) {
                ItemStack rem = insertIntoTrunk(drop);
                if (!rem.isEmpty()) Block.dropStack(world, logPos, rem);
            }
            chopped++;
        }

        for (BlockPos leafPos : leavesToBreak) {
            BlockState state = world.getBlockState(leafPos);
            List<ItemStack> drops = Block.getDroppedStacks(state, world, leafPos, world.getBlockEntity(leafPos), rider, toolStack);
            world.breakBlock(leafPos, false, rider);

            for (ItemStack drop : drops) {
                ItemStack rem = insertIntoTrunk(drop);
                if (!rem.isEmpty()) Block.dropStack(world, leafPos, rem);
            }
        }

        world.playSound(null, startLog, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 0.8f, 0.9f);

        if (rider instanceof PlayerEntity player && !player.isCreative()) {
            int newDamage = toolStack.getDamage() + chopped;
            if (newDamage >= toolStack.getMaxDamage()) {
                this.inventory.set(TOOL_SLOT, ItemStack.EMPTY);
                world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ITEM_BREAK.value(), SoundCategory.PLAYERS, 1.0f, 1.0f);
                player.sendMessage(Text.literal("§c[ATV] Tree saw blade broke!"), true);
            } else {
                toolStack.setDamage(newDamage);
            }
        }

        this.toolCooldown = Math.max(2, (int) (12 / treeSaw.getTier().getSpeedMultiplier()));
    }

    private void processCropHarvesting(ServerWorld world, LivingEntity rider) {
        ItemStack toolStack = this.inventory.get(TOOL_SLOT);
        if (!(toolStack.getItem() instanceof CropHarvesterItem cropHarvester)) {
            return;
        }

        if (Math.abs(this.currentSpeed) < 0.03f && !getForwardInput(rider)) {
            return;
        }

        float yawRad = (float) Math.toRadians(this.getYaw());
        double dx = -Math.sin(yawRad);
        double dz = Math.cos(yawRad);
        Vec3d forwardVec = new Vec3d(dx, 0, dz).normalize();
        Vec3d rightVec = new Vec3d(-dz, 0, dx).normalize();

        int radius = cropHarvester.getTier().getRadius();
        Set<BlockPos> uniquePositions = new HashSet<>();

        // Sample along front bumper across full swath width in fine 0.5-block steps
        for (double offset = -radius; offset <= radius; offset += 0.5) {
            for (double fwd = 0.5; fwd <= 1.8; fwd += 0.6) {
                Vec3d samplePoint = this.getEntityPos().add(forwardVec.multiply(fwd)).add(rightVec.multiply(offset));
                BlockPos base = BlockPos.ofFloored(samplePoint);
                uniquePositions.add(base);
                uniquePositions.add(base.down());
                uniquePositions.add(base.up());
            }
        }

        int harvestedCount = 0;

        for (BlockPos p : uniquePositions) {
            BlockState state = world.getBlockState(p);
            if (cropHarvester.isMatureCrop(state)) {
                List<ItemStack> drops = Block.getDroppedStacks(state, world, p, world.getBlockEntity(p), rider, toolStack);

                if (state.getBlock() instanceof CropBlock crop) {
                    world.setBlockState(p, crop.withAge(0), Block.NOTIFY_ALL);
                } else if (state.getBlock() instanceof CocoaBlock) {
                    world.setBlockState(p, state.with(CocoaBlock.AGE, 0), Block.NOTIFY_ALL);
                } else if (state.getBlock() instanceof NetherWartBlock) {
                    world.setBlockState(p, state.with(NetherWartBlock.AGE, 0), Block.NOTIFY_ALL);
                } else {
                    world.breakBlock(p, false, rider);
                }

                for (ItemStack drop : drops) {
                    ItemStack rem = insertIntoTrunk(drop);
                    if (!rem.isEmpty()) Block.dropStack(world, p, rem);
                }

                world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                        p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.05);

                harvestedCount++;
            }
        }

        if (harvestedCount > 0) {
            world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS, 0.6f, 1.1f);
            if (rider instanceof PlayerEntity player && !player.isCreative()) {
                int newDamage = toolStack.getDamage() + harvestedCount;
                if (newDamage >= toolStack.getMaxDamage()) {
                    this.inventory.set(TOOL_SLOT, ItemStack.EMPTY);
                    world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ITEM_BREAK.value(), SoundCategory.PLAYERS, 1.0f, 1.0f);
                    player.sendMessage(Text.literal("§c[ATV] Crop harvester reel broke!"), true);
                } else {
                    toolStack.setDamage(newDamage);
                }
            }
        }
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new AtvScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("entity.enchantedwood.atv");
    }

    @Override
    protected void readCustomData(ReadView view) {
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        setFuelLevel(view.getInt("FuelLevel", 0), view.getInt("MaxFuel", 1000));
    }

    @Override
    protected void writeCustomData(WriteView view) {
        Inventories.writeData(view, this.inventory);
        view.putInt("FuelLevel", getFuelLevel());
        view.putInt("MaxFuel", getMaxFuel());
    }

    // Inventory Implementation
    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : inventory) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public void markDirty() {}

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return !this.isRemoved() && player.squaredDistanceTo(this) <= 64.0;
    }

    @Override
    public void clear() {
        inventory.clear();
    }
}

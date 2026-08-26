package net.enchantedwood.entity.custom;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.entity.ModEntities;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.AtvScreenHandler;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
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

public class AtvEntity extends AbstractBoatEntity implements NamedScreenHandlerFactory, Inventory {

    // 6 Module Slots: 0:Engine, 1:Tires, 2:Suspension, 3:Chassis, 4:Trunk, 5:Fuel/Battery
    public static final int ENGINE_SLOT = 0;
    public static final int TIRE_SLOT = 1;
    public static final int SUSPENSION_SLOT = 2;
    public static final int CHASSIS_SLOT = 3;
    public static final int TRUNK_SLOT = 4;
    public static final int FUEL_SLOT = 5;

    public static final int MODULE_SLOTS_COUNT = 6;
    public static final int MAX_TRUNK_SLOTS = 27;
    public static final int TOTAL_INVENTORY_SIZE = MODULE_SLOTS_COUNT + MAX_TRUNK_SLOTS;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(TOTAL_INVENTORY_SIZE, ItemStack.EMPTY);

    // Synced Data
    private static final TrackedData<Float> SPEED = DataTracker.registerData(AtvEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> FUEL_LEVEL = DataTracker.registerData(AtvEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MAX_FUEL = DataTracker.registerData(AtvEntity.class, TrackedDataHandlerRegistry.INTEGER);

    // Movement & Physics
    private float targetSpeed = 0.0f;
    private float currentSpeed = 0.0f;
    public float wheelRotation = 0.0f;
    private int fuelBurnTime = 0;
    private boolean pressingLeft;
    private boolean pressingRight;
    private boolean pressingForward;
    private boolean pressingBack;

    public AtvEntity(EntityType<? extends AbstractBoatEntity> type, World world) {
        super(type, world, () -> ModItems.ATV_ITEM);
        this.intersectionChecked = true;
    }

    public AtvEntity(World world, double x, double y, double z) {
        this(ModEntities.ATV, world);
        this.setPosition(x, y, z);
    }

    @Override
    protected double getPassengerAttachmentY(EntityDimensions dimensions) {
        return 0.45;
    }

    @Override
    public void setInputs(boolean left, boolean right, boolean forward, boolean back) {
        this.pressingLeft = left;
        this.pressingRight = right;
        this.pressingForward = forward;
        this.pressingBack = back;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(SPEED, 0.0f);
        builder.add(FUEL_LEVEL, 0);
        builder.add(MAX_FUEL, 1000);
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
        return new Vec3d(0.0, 0.45 * scale, 0.0);
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
        if (player.isSneaking()) {
            if (!world.isClient()) {
                ItemStack held = player.getStackInHand(hand);
                if (held.isEmpty() || held.isOf(ModItems.COPPER_GEAR) || held.isOf(ModItems.STEEL_GEAR)) {
                    ItemStack atvDrop = new ItemStack(ModItems.ATV_ITEM);
                    writeInventoryToItem(atvDrop);
                    this.dropStack((ServerWorld) world, atvDrop);
                    this.discard();
                    return ActionResult.SUCCESS;
                }
                player.openHandledScreen(this);
            }
            return ActionResult.SUCCESS;
        }

        if (!world.isClient()) {
            if (!this.hasPassengers()) {
                player.startRiding(this);
                return ActionResult.SUCCESS;
            } else {
                player.openHandledScreen(this);
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.SUCCESS;
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
                            this.inventory.set(i, new ItemStack(Registries.ITEM.get(id), Math.max(1, count)));
                        }
                    }
                }
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
        } else {
            // Server side fuel, exhaust & gauges
            processFuel();
            if (rider != null && Math.abs(this.currentSpeed) > 0.05f) {
                consumeFuel(1);
                if (this.random.nextInt(3) == 0) {
                    Vec3d exhaustPos = this.getEntityPos().subtract(this.getRotationVector().multiply(0.9)).add(0, 0.4, 0);
                    ((ServerWorld) world).spawnParticles(ParticleTypes.SMOKE, exhaustPos.x, exhaustPos.y, exhaustPos.z, 2, 0.1, 0.1, 0.1, 0.02);
                }
            }
            this.dataTracker.set(SPEED, this.currentSpeed * 72.0f);
        }
    }

    private void handleRiderControl(PlayerEntity player) {
        float forward = 0.0f;
        float sideways = 0.0f;

        if (this.pressingForward) forward += 1.0f;
        if (this.pressingBack) forward -= 1.0f;
        if (this.pressingLeft) sideways += 1.0f;
        if (this.pressingRight) sideways -= 1.0f;

        // Steering rotation: A/D keys turn, and smooth lerp towards camera view
        if (this.pressingLeft) {
            this.setYaw(this.getYaw() - 3.5f);
        }
        if (this.pressingRight) {
            this.setYaw(this.getYaw() + 3.5f);
        }
        if (forward != 0) {
            this.setYaw(MathHelper.lerpAngleDegrees(0.12f, this.getYaw(), player.getYaw()));
        }

        // Engine max speed and acceleration calculation
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

        // Check if riding on Asphalt for +25% speed bonus
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
            else if (fuelStack.getItem() instanceof EnergyProvider provider) {
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

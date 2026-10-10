package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.SuperComputerBlock;
import net.enchantedwood.block.custom.WaterPumpBlock;
import net.enchantedwood.block.custom.LavaPumpBlock;
import net.enchantedwood.block.custom.MagmaCrucibleBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.SuperComputerScreenHandler;
import net.enchantedwood.util.ItemTransportHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SuperComputerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int PATTERN_START = 0;
    public static final int PATTERN_SIZE = 9;
    public static final int UPGRADE_SLOT = 9;
    public static final int OUTPUT_START = 10;
    public static final int OUTPUT_SIZE = 4;
    public static final int PREVIEW_SLOT = 14;
    public static final int TOTAL_SLOTS = 15;

    public static final int ENERGY_CAPACITY = 100_000;
    public static final int BASE_CRAFT_TIME = 20; // 1 second per craft
    public static final int OVERCLOCKED_CRAFT_TIME = 2; // 10 crafts per second
    public static final int ENERGY_PER_CRAFT = 50; // 50 FE per craft

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 2_000, 2_000, 0);

    private int craftProgress = 0;
    private int maxCraftProgress = BASE_CRAFT_TIME;
    private boolean hasValidRecipe = false;

    protected final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergy() & 0xFFFF;
                case 1 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 4 -> craftProgress;
                case 5 -> maxCraftProgress;
                case 6 -> isNetworkOnline() ? 1 : 0;
                case 7 -> hasValidRecipe ? 1 : 0;
                case 8 -> isCasterOnline() ? 1 : 0;
                case 9 -> isCircuitFabricatorOnline() ? 1 : 0;
                case 10 -> isPressOnline() ? 1 : 0;
                case 11 -> isFurnaceOnline() ? 1 : 0;
                case 12 -> isWaterPumpOnline() ? 1 : 0;
                case 13 -> isLavaSourceOnline() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energyStorage.setEnergy((energyStorage.getEnergy() & 0xFFFF0000) | (value & 0xFFFF));
                case 1 -> energyStorage.setEnergy((energyStorage.getEnergy() & 0x0000FFFF) | ((value & 0xFFFF) << 16));
                case 4 -> craftProgress = value;
                case 5 -> maxCraftProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 14;
        }
    };

    private @Nullable BlockPos boundNetworkPos = null;
    private String boundDimension = "minecraft:overworld";

    private long lastScanTick = -100;
    private boolean cachedFurnaceOnline = false;
    private boolean cachedPressOnline = false;
    private boolean cachedFabricatorOnline = false;
    private boolean cachedCasterOnline = false;
    private boolean cachedNetworkOnline = false;
    private boolean cachedWaterPumpOnline = false;
    private boolean cachedLavaOnline = false;
    private List<EnchantedFurnaceBlockEntity> cachedFurnaces = java.util.Collections.emptyList();
    private List<HydraulicPressBlockEntity> cachedPresses = java.util.Collections.emptyList();
    private List<CircuitFabricatorBlockEntity> cachedFabricators = java.util.Collections.emptyList();
    private List<CastingPortBlockEntity> cachedCasters = java.util.Collections.emptyList();
    private List<TitaniumTankControllerBlockEntity> cachedTankControllers = java.util.Collections.emptyList();
    private List<WaterPumpBlockEntity> cachedWaterPumps = java.util.Collections.emptyList();
    private List<LavaPumpBlockEntity> cachedLavaPumps = java.util.Collections.emptyList();
    private List<MagmaCrucibleBlockEntity> cachedCrucibles = java.util.Collections.emptyList();
    private List<Container> cachedContainers = java.util.Collections.emptyList();
    private @Nullable EnchantedStorageTerminalBlockEntity cachedTerminal = null;
    private @Nullable EnchantedStorageControllerBlockEntity cachedPowerController = null;
    private @Nullable ActiveCraftJob activeJob = null;

    public SuperComputerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUPER_COMPUTER_BLOCK_ENTITY, pos, state);
    }

    public void bindNetwork(BlockPos pos, String dimension) {
        this.boundNetworkPos = pos;
        this.boundDimension = dimension != null ? dimension : "minecraft:overworld";
        this.lastScanTick = -100;
        setChanged();
    }

    public @Nullable BlockPos getBoundNetworkPos() {
        return this.boundNetworkPos;
    }

    public @Nullable BlockPos getEffectiveControllerPos() {
        if (this.boundNetworkPos != null) return this.boundNetworkPos;
        if (this.cachedPowerController != null && !this.cachedPowerController.isRemoved()) {
            return this.cachedPowerController.getBlockPos();
        }
        if (this.cachedTerminal != null && !this.cachedTerminal.isRemoved()) {
            return this.cachedTerminal.getBlockPos();
        }
        return null;
    }

    public @Nullable EnchantedStorageTerminalBlockEntity getNetworkTerminal() {
        if (this.cachedTerminal != null && !this.cachedTerminal.isRemoved()) {
            return this.cachedTerminal;
        }
        return null;
    }

    public boolean isNetworkOnline() {
        return this.cachedNetworkOnline;
    }

    public void updateMachineCache(boolean force) {
        if (this.level == null || this.level.isClientSide()) return;
        long currentTick = this.level.getGameTime();
        if (!force && (currentTick - this.lastScanTick < 40)) {
            return;
        }
        this.lastScanTick = currentTick;

        List<EnchantedFurnaceBlockEntity> foundFurnaces = new ArrayList<>();
        List<HydraulicPressBlockEntity> foundPresses = new ArrayList<>();
        List<CircuitFabricatorBlockEntity> foundFabricators = new ArrayList<>();
        List<CastingPortBlockEntity> foundCasters = new ArrayList<>();
        List<TitaniumTankControllerBlockEntity> foundTankControllers = new ArrayList<>();
        List<WaterPumpBlockEntity> foundWaterPumps = new ArrayList<>();
        List<LavaPumpBlockEntity> foundLavaPumps = new ArrayList<>();
        List<MagmaCrucibleBlockEntity> foundCrucibles = new ArrayList<>();
        List<Container> foundContainers = new ArrayList<>();
        final EnchantedStorageTerminalBlockEntity[] foundTerminal = new EnchantedStorageTerminalBlockEntity[1];
        final EnchantedStorageControllerBlockEntity[] foundController = new EnchantedStorageControllerBlockEntity[1];

        java.util.Set<BlockPos> visited = new java.util.HashSet<>();

        // 1. Direct check of boundNetworkPos if wrench-linked
        if (this.boundNetworkPos != null && this.level.hasChunk(this.boundNetworkPos.getX() >> 4, this.boundNetworkPos.getZ() >> 4)) {
            BlockEntity boundBe = this.level.getBlockEntity(this.boundNetworkPos);
            if (boundBe instanceof EnchantedStorageTerminalBlockEntity term) {
                foundTerminal[0] = term;
            } else if (boundBe instanceof EnchantedStorageControllerBlockEntity ctrl) {
                foundController[0] = ctrl;
            }
        }

        // 2. Scan around Super Computer (radius 48 X/Z, 32 Y)
        int minX = this.worldPosition.getX() - 48;
        int maxX = this.worldPosition.getX() + 48;
        int minY = Math.max(this.level.getMinY(), this.worldPosition.getY() - 32);
        int maxY = Math.min(this.level.getMaxY(), this.worldPosition.getY() + 32);
        int minZ = this.worldPosition.getZ() - 48;
        int maxZ = this.worldPosition.getZ() + 48;

        scanChunkArea(minX, maxX, minY, maxY, minZ, maxZ, visited,
                foundFurnaces, foundPresses, foundFabricators, foundCasters, foundTankControllers,
                foundWaterPumps, foundLavaPumps, foundCrucibles, foundContainers, foundTerminal, foundController);

        // 3. If bound to a controller or terminal located further away, scan around it too (radius 32 X/Z, 16 Y)
        BlockPos remotePos = foundController[0] != null ? foundController[0].getBlockPos() : (foundTerminal[0] != null ? foundTerminal[0].getBlockPos() : this.boundNetworkPos);
        if (remotePos != null && remotePos.distManhattan(this.worldPosition) > 20 && this.level.hasChunk(remotePos.getX() >> 4, remotePos.getZ() >> 4)) {
            int cMinX = remotePos.getX() - 32;
            int cMaxX = remotePos.getX() + 32;
            int cMinY = Math.max(this.level.getMinY(), remotePos.getY() - 16);
            int cMaxY = Math.min(this.level.getMaxY(), remotePos.getY() + 16);
            int cMinZ = remotePos.getZ() - 32;
            int cMaxZ = remotePos.getZ() + 32;

            scanChunkArea(cMinX, cMaxX, cMinY, cMaxY, cMinZ, cMaxZ, visited,
                    foundFurnaces, foundPresses, foundFabricators, foundCasters, foundTankControllers,
                    foundWaterPumps, foundLavaPumps, foundCrucibles, foundContainers, foundTerminal, foundController);
        }

        this.cachedFurnaces = foundFurnaces;
        this.cachedPresses = foundPresses;
        this.cachedFabricators = foundFabricators;
        this.cachedCasters = foundCasters;
        this.cachedTankControllers = foundTankControllers;
        this.cachedWaterPumps = foundWaterPumps;
        this.cachedLavaPumps = foundLavaPumps;
        this.cachedCrucibles = foundCrucibles;
        this.cachedContainers = foundContainers;
        this.cachedTerminal = foundTerminal[0];
        this.cachedPowerController = foundController[0];

        this.cachedFurnaceOnline = !foundFurnaces.isEmpty();
        this.cachedPressOnline = !foundPresses.isEmpty();
        this.cachedFabricatorOnline = !foundFabricators.isEmpty();
        this.cachedCasterOnline = !foundCasters.isEmpty();
        this.cachedWaterPumpOnline = !foundWaterPumps.isEmpty();
        this.cachedLavaOnline = !foundLavaPumps.isEmpty() || !foundCrucibles.isEmpty();

        boolean networkOn = false;
        if (foundController[0] != null && foundController[0].isOnline()) {
            networkOn = true;
        } else if (foundTerminal[0] != null && foundTerminal[0].isNetworkOnline()) {
            networkOn = true;
        }
        this.cachedNetworkOnline = networkOn;
    }

    private void scanChunkArea(int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                              java.util.Set<BlockPos> visited,
                              List<EnchantedFurnaceBlockEntity> furnaces,
                              List<HydraulicPressBlockEntity> presses,
                              List<CircuitFabricatorBlockEntity> fabricators,
                              List<CastingPortBlockEntity> casters,
                              List<TitaniumTankControllerBlockEntity> tankControllers,
                              List<WaterPumpBlockEntity> waterPumps,
                              List<LavaPumpBlockEntity> lavaPumps,
                              List<MagmaCrucibleBlockEntity> crucibles,
                              List<Container> containers,
                              EnchantedStorageTerminalBlockEntity[] foundTerminal,
                              EnchantedStorageControllerBlockEntity[] foundController) {
        if (this.level == null) return;

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                if (!this.level.hasChunk(cx, cz)) continue;
                net.minecraft.world.level.chunk.LevelChunk chunk = this.level.getChunkAt(new BlockPos(cx << 4, 0, cz << 4));
                if (chunk == null) continue;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be == null || be.isRemoved()) continue;
                    BlockPos bp = be.getBlockPos();
                    if (bp.getX() < minX || bp.getX() > maxX || bp.getY() < minY || bp.getY() > maxY || bp.getZ() < minZ || bp.getZ() > maxZ) {
                        continue;
                    }
                    if (!visited.add(bp)) continue;

                    if (be instanceof CastingPortBlockEntity port) {
                        casters.add(port);
                    } else if (be instanceof CircuitFabricatorBlockEntity fab) {
                        fabricators.add(fab);
                    } else if (be instanceof HydraulicPressBlockEntity press) {
                        presses.add(press);
                    } else if (be instanceof EnchantedFurnaceBlockEntity ef) {
                        furnaces.add(ef);
                    } else if (be instanceof WaterPumpBlockEntity wp) {
                        waterPumps.add(wp);
                    } else if (be instanceof LavaPumpBlockEntity lp) {
                        lavaPumps.add(lp);
                    } else if (be instanceof MagmaCrucibleBlockEntity mc) {
                        crucibles.add(mc);
                    } else if (be instanceof TitaniumTankControllerBlockEntity controller && controller.isFormed()) {
                        tankControllers.add(controller);
                    } else if (be instanceof TitaniumTankCasingBlockEntity casing) {
                        TitaniumTankControllerBlockEntity master = casing.getMaster();
                        if (master != null && master.isFormed() && visited.add(master.getBlockPos())) {
                            tankControllers.add(master);
                        }
                    } else if (be instanceof EnchantedStorageTerminalBlockEntity term) {
                        if (foundTerminal[0] == null) foundTerminal[0] = term;
                    } else if (be instanceof EnchantedStorageControllerBlockEntity ctrl) {
                        if (foundController[0] == null) foundController[0] = ctrl;
                    } else if (be instanceof Container inv && !(be instanceof SuperComputerBlockEntity) && !(be instanceof EnchantedStorageTerminalBlockEntity)) {
                        containers.add(inv);
                    }
                }
            }
        }
    }

    public java.util.Map<net.minecraft.world.item.Item, Integer> collectAvailableItems(@Nullable EnchantedStorageTerminalBlockEntity terminal,
                                                                                @Nullable Player player) {
        java.util.Map<net.minecraft.world.item.Item, Integer> available = new java.util.HashMap<>();

        // 1. Digital Storage Terminal crystals
        if (terminal != null && terminal.isNetworkOnline()) {
            for (EnchantedStorageTerminalBlockEntity.StoredItem item : terminal.getStoredItems()) {
                if (item.getCount() > 0 && !item.getSample().isEmpty()) {
                    int c = (int) Math.min(item.getCount(), (long) Integer.MAX_VALUE);
                    available.put(item.getSample().getItem(), available.getOrDefault(item.getSample().getItem(), 0) + c);
                }
            }
        }

        // 2. Player Inventory (slots 0..35: hotbar + main inventory)
        if (player != null) {
            Inventory pInv = player.getInventory();
            for (int i = 0; i < 36; i++) {
                ItemStack pStack = pInv.getItem(i);
                if (!pStack.isEmpty()) {
                    available.put(pStack.getItem(), available.getOrDefault(pStack.getItem(), 0) + pStack.getCount());
                }
            }
        }

        // 3. Directly adjacent inventories (Chests, Barrels, Enchanted Chests, etc.)
        if (this.level != null) {
            for (Direction dir : Direction.values()) {
                BlockEntity be = this.level.getBlockEntity(this.worldPosition.relative(dir));
                if (be instanceof Container adjInv && !(be instanceof SuperComputerBlockEntity) && !(be instanceof EnchantedStorageTerminalBlockEntity)) {
                    for (int s = 0; s < adjInv.getContainerSize(); s++) {
                        ItemStack stk = adjInv.getItem(s);
                        if (!stk.isEmpty()) {
                            available.put(stk.getItem(), available.getOrDefault(stk.getItem(), 0) + stk.getCount());
                        }
                    }
                }
            }
        }

        // 4. Scanned connected containers within network area
        for (Container container : this.cachedContainers) {
            if (container != null) {
                for (int s = 0; s < container.getContainerSize(); s++) {
                    ItemStack stk = container.getItem(s);
                    if (!stk.isEmpty()) {
                        available.put(stk.getItem(), available.getOrDefault(stk.getItem(), 0) + stk.getCount());
                    }
                }
            }
        }

        // 5. Water Pumps (Bucket in slot = empty buckets, Bucket out slot = water buckets)
        for (WaterPumpBlockEntity wp : this.cachedWaterPumps) {
            if (wp != null && !wp.isRemoved()) {
                ItemStack inStack = wp.getItem(WaterPumpBlockEntity.BUCKET_IN_SLOT);
                if (!inStack.isEmpty()) {
                    available.put(inStack.getItem(), available.getOrDefault(inStack.getItem(), 0) + inStack.getCount());
                }
                ItemStack outStack = wp.getItem(WaterPumpBlockEntity.BUCKET_OUT_SLOT);
                if (!outStack.isEmpty()) {
                    available.put(outStack.getItem(), available.getOrDefault(outStack.getItem(), 0) + outStack.getCount());
                }
            }
        }

        // 6. Lava Pumps (Bucket in slot = empty buckets, Bucket out slot = lava buckets)
        for (LavaPumpBlockEntity lp : this.cachedLavaPumps) {
            if (lp != null && !lp.isRemoved()) {
                ItemStack inStack = lp.getItem(LavaPumpBlockEntity.BUCKET_IN_SLOT);
                if (!inStack.isEmpty()) {
                    available.put(inStack.getItem(), available.getOrDefault(inStack.getItem(), 0) + inStack.getCount());
                }
                ItemStack outStack = lp.getItem(LavaPumpBlockEntity.BUCKET_OUT_SLOT);
                if (!outStack.isEmpty()) {
                    available.put(outStack.getItem(), available.getOrDefault(outStack.getItem(), 0) + outStack.getCount());
                }
            }
        }

        // 7. Magma Crucibles (Bucket input slot = empty buckets, Bucket output slot = lava buckets)
        for (MagmaCrucibleBlockEntity mc : this.cachedCrucibles) {
            if (mc != null && !mc.isRemoved()) {
                ItemStack inStack = mc.getItem(MagmaCrucibleBlockEntity.BUCKET_INPUT_SLOT);
                if (!inStack.isEmpty()) {
                    available.put(inStack.getItem(), available.getOrDefault(inStack.getItem(), 0) + inStack.getCount());
                }
                ItemStack outStack = mc.getItem(MagmaCrucibleBlockEntity.BUCKET_OUTPUT_SLOT);
                if (!outStack.isEmpty()) {
                    available.put(outStack.getItem(), available.getOrDefault(outStack.getItem(), 0) + outStack.getCount());
                }
            }
        }

        // NOTE: The 3x3 pattern matrix (slots 0..8) is purely a virtual recipe blueprint.
        // It NEVER contributes available items for manufacturing.

        return available;
    }

    public static class MetalCastInfo {
        public final net.enchantedwood.fluid.MoltenMetal metal;
        public final int costMb;

        public MetalCastInfo(net.enchantedwood.fluid.MoltenMetal metal, int costMb) {
            this.metal = metal;
            this.costMb = costMb;
        }
    }

    public static @Nullable MetalCastInfo getMetalCastInfo(net.minecraft.world.item.Item item) {
        if (item == null) return null;
        for (net.enchantedwood.fluid.MoltenMetal metal : net.enchantedwood.fluid.MoltenMetal.values()) {
            if (metal == net.enchantedwood.fluid.MoltenMetal.NONE || metal == net.enchantedwood.fluid.MoltenMetal.LAVA) continue;
            if (metal.getNuggetItem() == item) {
                return new MetalCastInfo(metal, 10);
            }
            if (metal.getIngotItem() == item) {
                return new MetalCastInfo(metal, 90);
            }
            if (metal.getBlockItem() == item) {
                return new MetalCastInfo(metal, 810);
            }
        }
        return null;
    }

    public static @Nullable ItemStack getDirectCastingPatternResult(List<ItemStack> patternStacks) {
        net.minecraft.world.item.Item firstItem = null;
        int count = 0;
        for (ItemStack s : patternStacks) {
            if (!s.isEmpty()) {
                if (firstItem == null) {
                    firstItem = s.getItem();
                } else if (firstItem != s.getItem()) {
                    return null;
                }
                count++;
            }
        }
        if (firstItem != null && getMetalCastInfo(firstItem) != null) {
            return new ItemStack(firstItem, Math.max(1, count));
        }
        return null;
    }

    public boolean isCasterOnline() {
        return this.cachedCasterOnline;
    }

    public List<CastingPortBlockEntity> getNearbyCastingPorts() {
        return this.cachedCasters;
    }

    public boolean isCircuitFabricatorOnline() {
        return this.cachedFabricatorOnline;
    }

    public List<CircuitFabricatorBlockEntity> getNearbyCircuitFabricators() {
        return this.cachedFabricators;
    }

    public boolean isPressOnline() {
        return this.cachedPressOnline;
    }

    public List<HydraulicPressBlockEntity> getNearbyHydraulicPresses() {
        return this.cachedPresses;
    }

    public boolean isFurnaceOnline() {
        return this.cachedFurnaceOnline;
    }

    public List<EnchantedFurnaceBlockEntity> getNearbyFurnaces() {
        return this.cachedFurnaces;
    }

    public boolean isWaterPumpOnline() {
        if (this.cachedWaterPumpOnline) return true;
        if (this.level != null) {
            BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
            for (int dx = -16; dx <= 16; dx++) {
                for (int dy = -8; dy <= 8; dy++) {
                    for (int dz = -16; dz <= 16; dz++) {
                        mut.set(this.worldPosition.getX() + dx, this.worldPosition.getY() + dy, this.worldPosition.getZ() + dz);
                        if (this.level.getFluidState(mut).is(net.minecraft.world.level.material.Fluids.WATER) || this.level.getBlockState(mut).is(net.minecraft.world.level.block.Blocks.WATER)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public List<WaterPumpBlockEntity> getNearbyWaterPumps() {
        return this.cachedWaterPumps;
    }

    public @Nullable WaterPumpBlockEntity getBestAvailableWaterPump() {
        for (WaterPumpBlockEntity wp : this.cachedWaterPumps) {
            if (wp != null && !wp.isRemoved()) return wp;
        }
        return null;
    }

    public boolean isLavaSourceOnline() {
        return this.cachedLavaOnline;
    }

    public boolean isLavaPumpOnline() {
        return !this.cachedLavaPumps.isEmpty();
    }

    public List<LavaPumpBlockEntity> getNearbyLavaPumps() {
        return this.cachedLavaPumps;
    }

    public @Nullable LavaPumpBlockEntity getBestAvailableLavaPump() {
        for (LavaPumpBlockEntity lp : this.cachedLavaPumps) {
            if (lp != null && !lp.isRemoved()) return lp;
        }
        return null;
    }

    public boolean isCrucibleOnline() {
        return !this.cachedCrucibles.isEmpty();
    }

    public List<MagmaCrucibleBlockEntity> getNearbyCrucibles() {
        return this.cachedCrucibles;
    }

    public @Nullable MagmaCrucibleBlockEntity getBestAvailableCrucible() {
        for (MagmaCrucibleBlockEntity mc : this.cachedCrucibles) {
            if (mc != null && !mc.isRemoved()) return mc;
        }
        return null;
    }

    public @Nullable HydraulicPressBlockEntity getBestAvailablePress() {
        if (this.activeJob != null) {
            for (HydraulicPressBlockEntity press : this.cachedPresses) {
                if (press != null && !press.isRemoved() && press.isExternalProcess()) return press;
            }
        }
        HydraulicPressBlockEntity bestIdle = null;
        int bestSpeed = -1;
        for (HydraulicPressBlockEntity press : this.cachedPresses) {
            if (press != null && !press.isRemoved()) {
                if (this.activeJob == null && press.isExternalProcess()) {
                    press.clearExternalProcess();
                }
                boolean isIdle = !press.isExternalProcess() && press.getItem(0).isEmpty();
                int speed = press.getProcessingSpeed(press.getActiveGearTier());
                if (isIdle && speed > bestSpeed) {
                    bestIdle = press;
                    bestSpeed = speed;
                }
            }
        }
        return bestIdle;
    }

    public @Nullable CircuitFabricatorBlockEntity getBestAvailableFabricator() {
        if (this.activeJob != null) {
            for (CircuitFabricatorBlockEntity fab : this.cachedFabricators) {
                if (fab != null && !fab.isRemoved() && fab.isExternalProcess()) return fab;
            }
        }
        CircuitFabricatorBlockEntity bestIdle = null;
        float bestSpeed = -1f;
        for (CircuitFabricatorBlockEntity fab : this.cachedFabricators) {
            if (fab != null && !fab.isRemoved()) {
                if (this.activeJob == null && fab.isExternalProcess()) {
                    fab.clearExternalProcess();
                }
                boolean isIdle = !fab.isExternalProcess()
                        && fab.getItem(CircuitFabricatorBlockEntity.SUBSTRATE_SLOT).isEmpty()
                        && fab.getItem(CircuitFabricatorBlockEntity.COMPONENT_SLOT_1).isEmpty();
                float speed = fab.getSpeedMultiplier();
                if (isIdle && speed > bestSpeed) {
                    bestIdle = fab;
                    bestSpeed = speed;
                }
            }
        }
        return bestIdle;
    }

    public @Nullable EnchantedFurnaceBlockEntity getBestAvailableFurnace() {
        if (this.activeJob != null) {
            for (EnchantedFurnaceBlockEntity ef : this.cachedFurnaces) {
                if (ef != null && !ef.isRemoved() && ef.isExternalProcess()) {
                    return ef;
                }
            }
        }
        for (EnchantedFurnaceBlockEntity ef : this.cachedFurnaces) {
            if (ef != null && !ef.isRemoved()) {
                if (this.activeJob == null && ef.isExternalProcess()) {
                    ef.clearExternalProcess();
                }
                if (ef.isIdle()) return ef;
            }
        }
        return null;
    }

    public @Nullable CastingPortBlockEntity getBestAvailableCaster() {
        for (CastingPortBlockEntity c : this.cachedCasters) {
            if (c != null && !c.isRemoved() && !c.isCasting() && c.getItem(0).isEmpty()) return c;
        }
        return null;
    }

    public int getStepDurationTicks(CraftStep step) {
        return switch (step.type) {
            case PRESS -> {
                HydraulicPressBlockEntity press = getBestAvailablePress();
                if (press != null) {
                    int speed = press.getProcessingSpeed(press.getActiveGearTier());
                    yield Math.max(8, 100 / Math.max(1, speed));
                }
                yield 100;
            }
            case FABRICATE -> {
                CircuitFabricatorBlockEntity fab = getBestAvailableFabricator();
                if (fab != null) {
                    float speed = fab.getSpeedMultiplier();
                    yield Math.max(10, (int) (120 / Math.max(1.0f, speed)));
                }
                yield 120;
            }
            case SMELT -> isFurnaceOnline() ? 60 : 120;
            case CAST -> 20;
            case PUMP_WATER -> 20;
            case PUMP_LAVA -> 30;
            case MELT_LAVA -> 40;
            case ASSEMBLE -> isOverclocked() ? 10 : 25;
        };
    }

    public record PressRecipeInfo(net.minecraft.world.item.Item input, int yield) {}

    public static @Nullable PressRecipeInfo getPressRecipeInfo(net.minecraft.world.item.Item targetItem) {
        if (targetItem == ModItems.SILICON_WAFER) return new PressRecipeInfo(ModItems.SILICON, 2);
        if (targetItem == ModItems.TUNGSTEN_PLATE) return new PressRecipeInfo(ModItems.TUNGSTEN_INGOT, 1);
        if (targetItem == ModItems.COBALT_PLATE) return new PressRecipeInfo(ModItems.COBALT_INGOT, 1);
        if (targetItem == ModItems.ARDITE_PLATE) return new PressRecipeInfo(ModItems.ARDITE_INGOT, 1);
        if (targetItem == ModItems.MANYULLYN_PLATE) return new PressRecipeInfo(ModItems.MANYULLYN_INGOT, 1);
        if (targetItem == ModItems.STEEL_NUGGET) return new PressRecipeInfo(ModItems.STEEL_INGOT, 9);
        return null;
    }

    public static @Nullable CircuitFabricatorBlockEntity.FabricatorRecipe getMatchingFabricatorRecipe(List<ItemStack> patternStacks) {
        List<ItemStack> nonNull = patternStacks.stream().filter(s -> !s.isEmpty()).toList();
        if (nonNull.size() != 4) return null;

        for (CircuitFabricatorBlockEntity.FabricatorRecipe recipe : CircuitFabricatorBlockEntity.getRecipes()) {
            boolean hasSubstrate = false;
            List<net.minecraft.world.item.Item> neededComponents = new ArrayList<>(recipe.components());

            for (ItemStack s : nonNull) {
                if (!hasSubstrate && s.is(recipe.substrate())) {
                    hasSubstrate = true;
                } else if (neededComponents.contains(s.getItem())) {
                    neededComponents.remove(s.getItem());
                }
            }

            if (hasSubstrate && neededComponents.isEmpty()) {
                return recipe;
            }
        }
        return null;
    }

    public static @Nullable ItemStack getDirectFabricatorPatternResult(List<ItemStack> patternStacks) {
        CircuitFabricatorBlockEntity.FabricatorRecipe r = getMatchingFabricatorRecipe(patternStacks);
        return r != null ? r.output().copy() : null;
    }

    public static @Nullable ItemStack getDirectPressPatternResult(List<ItemStack> patternStacks) {
        ItemStack single = null;
        for (ItemStack s : patternStacks) {
            if (!s.isEmpty()) {
                if (single != null) return null;
                single = s;
            }
        }
        if (single == null) return null;
        ItemStack plateRes = HydraulicPressBlockEntity.getPlateResult(single.getItem());
        if (!plateRes.isEmpty()) {
            return new ItemStack(plateRes.getItem(), plateRes.getCount() * single.getCount());
        }
        return null;
    }

    public static @Nullable ItemStack getDirectSmeltingPatternResult(ServerLevel world, List<ItemStack> patternStacks) {
        ItemStack single = null;
        for (ItemStack s : patternStacks) {
            if (!s.isEmpty()) {
                if (single != null) return null;
                single = s;
            }
        }
        if (single == null) return null;

        // 1. Check custom dust smelting
        net.minecraft.world.item.Item dustSmelt = EnchantedFurnaceBlockEntity.getDustSmeltingResult(single.getItem());
        if (dustSmelt != null) {
            return new ItemStack(dustSmelt, single.getCount());
        }

        // 2. Check vanilla smelting recipes
        Optional<RecipeHolder<SmeltingRecipe>> match = world.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(single), world);
        if (match.isPresent()) {
            if (isEquipmentRecycleRecipe(match.get().value(), world)) return null;
            ItemStack res = match.get().value().assemble(new SingleRecipeInput(single));
            if (!res.isEmpty()) {
                return new ItemStack(res.getItem(), res.getCount() * single.getCount());
            }
        }
        return null;
    }

    public static @Nullable ItemStack getDirectWaterPumpPatternResult(List<ItemStack> patternStacks) {
        ItemStack single = null;
        for (ItemStack s : patternStacks) {
            if (!s.isEmpty()) {
                if (single != null) return null;
                single = s;
            }
        }
        if (single == null) return null;

        if (single.is(Items.BUCKET) || single.is(Items.WATER_BUCKET)) {
            return new ItemStack(Items.WATER_BUCKET);
        }
        if (single.is(ModItems.COPPER_BUCKET) || single.is(ModItems.COPPER_WATER_BUCKET)) {
            return new ItemStack(ModItems.COPPER_WATER_BUCKET);
        }
        return null;
    }

    public static @Nullable ItemStack getDirectLavaPatternResult(List<ItemStack> patternStacks) {
        ItemStack single = null;
        ItemStack second = null;
        for (ItemStack s : patternStacks) {
            if (!s.isEmpty()) {
                if (single == null) {
                    single = s;
                } else if (second == null) {
                    second = s;
                } else {
                    return null;
                }
            }
        }
        if (single == null) return null;

        if (second == null) {
            if (single.is(Items.LAVA_BUCKET)) return new ItemStack(Items.LAVA_BUCKET);
            if (single.is(ModItems.COPPER_LAVA_BUCKET)) return new ItemStack(ModItems.COPPER_LAVA_BUCKET);
            return null;
        }

        ItemStack bucket = null;
        ItemStack other = null;
        if (single.is(Items.BUCKET) || single.is(ModItems.COPPER_BUCKET)) {
            bucket = single;
            other = second;
        } else if (second.is(Items.BUCKET) || second.is(ModItems.COPPER_BUCKET)) {
            bucket = second;
            other = single;
        }

        if (bucket != null && other != null) {
            boolean isCopper = bucket.is(ModItems.COPPER_BUCKET);
            net.minecraft.world.item.Item otherItem = other.getItem();
            if (otherItem == ModItems.FIRE_CRYSTAL || otherItem == Items.MAGMA_BLOCK || otherItem == Items.BASALT ||
                otherItem == Items.BLACKSTONE || otherItem == Items.COBBLESTONE || otherItem == Items.STONE ||
                otherItem == Items.NETHERRACK || otherItem == Items.LAVA_BUCKET) {
                return isCopper ? new ItemStack(ModItems.COPPER_LAVA_BUCKET) : new ItemStack(Items.LAVA_BUCKET);
            }
        }
        return null;
    }

    public static List<net.minecraft.world.item.Item> getDustSmeltingInputs(net.minecraft.world.item.Item targetItem) {
        if (targetItem == net.minecraft.world.item.Items.IRON_INGOT) return List.of(ModItems.IRON_DUST);
        if (targetItem == net.minecraft.world.item.Items.COPPER_INGOT) return List.of(ModItems.COPPER_DUST);
        if (targetItem == ModItems.TIN_INGOT) return List.of(ModItems.TIN_DUST, ModItems.RAW_TIN);
        if (targetItem == ModItems.BRONZE_INGOT) return List.of(ModItems.BRONZE_DUST);
        if (targetItem == ModItems.TITANIUM_INGOT) return List.of(ModItems.TITANIUM_DUST, ModItems.RAW_TITANIUM);
        if (targetItem == net.minecraft.world.item.Items.GOLD_INGOT) return List.of(ModItems.GOLD_DUST);
        if (targetItem == net.minecraft.world.item.Items.DIAMOND) return List.of(ModItems.DIAMOND_DUST);
        if (targetItem == net.minecraft.world.item.Items.NETHERITE_INGOT) return List.of(ModItems.NETHERITE_DUST);
        if (targetItem == net.minecraft.world.item.Items.EMERALD) return List.of(ModItems.EMERALD_DUST);
        if (targetItem == net.minecraft.world.item.Items.COAL) return List.of(ModItems.COAL_DUST);
        return java.util.Collections.emptyList();
    }

    public java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> getAvailableMoltenMetals() {
        if (this.level == null) return new java.util.EnumMap<>(net.enchantedwood.fluid.MoltenMetal.class);
        java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> amounts = new java.util.EnumMap<>(net.enchantedwood.fluid.MoltenMetal.class);
        java.util.Set<BlockPos> visitedControllers = new java.util.HashSet<>();

        for (TitaniumTankControllerBlockEntity controller : this.cachedTankControllers) {
            if (controller != null && !controller.isRemoved() && controller.isFormed() && visitedControllers.add(controller.getBlockPos())) {
                net.enchantedwood.fluid.MoltenMetal fluid = controller.getFluidType();
                if (fluid != null && fluid != net.enchantedwood.fluid.MoltenMetal.NONE && fluid != net.enchantedwood.fluid.MoltenMetal.LAVA) {
                    int amt = controller.getStoredFluidAmount();
                    if (amt > 0) {
                        amounts.put(fluid, amounts.getOrDefault(fluid, 0) + amt);
                    }
                }
            }
        }

        for (CastingPortBlockEntity port : this.cachedCasters) {
            if (port != null && !port.isRemoved()) {
                net.enchantedwood.fluid.MoltenMetal fluid = port.getFluidType();
                if (fluid != null && fluid != net.enchantedwood.fluid.MoltenMetal.NONE && fluid != net.enchantedwood.fluid.MoltenMetal.LAVA) {
                    int amt = port.getFluidAmount(fluid);
                    if (amt > 0) {
                        amounts.put(fluid, amounts.getOrDefault(fluid, 0) + amt);
                    }
                }
            }
        }

        return amounts;
    }

    private void consumeMoltenMetals(java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> requiredFluids) {
        if (this.level == null || requiredFluids == null || requiredFluids.isEmpty()) return;
        java.util.Set<BlockPos> visitedControllers = new java.util.HashSet<>();

        for (java.util.Map.Entry<net.enchantedwood.fluid.MoltenMetal, Integer> entry : requiredFluids.entrySet()) {
            net.enchantedwood.fluid.MoltenMetal metal = entry.getKey();
            int needed = entry.getValue();
            if (needed <= 0) continue;

            // 1. Draw from Casting Port buffers first
            for (CastingPortBlockEntity port : this.cachedCasters) {
                if (needed <= 0) break;
                if (port != null && !port.isRemoved() && port.getFluidType() == metal) {
                    int extracted = port.extractFluid(metal, needed, false);
                    needed -= extracted;
                }
            }

            // 2. Draw directly from Titanium Tanks
            for (TitaniumTankControllerBlockEntity controller : this.cachedTankControllers) {
                if (needed <= 0) break;
                if (controller != null && !controller.isRemoved() && controller.isFormed() && visitedControllers.add(controller.getBlockPos())) {
                    if (controller.getFluidType() == metal) {
                        int extracted = controller.extractFluidInternal(metal, needed, false);
                        needed -= extracted;
                    }
                }
            }
        }
    }

    public int getEnergy() {
        return this.energyStorage.getEnergy();
    }

    public int getMaxEnergy() {
        return this.energyStorage.getMaxEnergy();
    }

    public boolean isOverclocked() {
        return this.inventory.get(UPGRADE_SLOT).is(ModItems.BLAZE_OVERCLOCK_CORE);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, SuperComputerBlockEntity entity) {
        if (entity.activeJob != null) {
            entity.tickActiveJob(world, pos, state);
            return;
        }

        entity.updateMachineCache(false);
        boolean wasLit = state.getValue(SuperComputerBlock.LIT);

        // Determine current craft speed based on upgrade socket
        entity.maxCraftProgress = entity.isOverclocked() ? OVERCLOCKED_CRAFT_TIME : BASE_CRAFT_TIME;

        // Form 3x3 recipe input
        List<ItemStack> patternStacks = new ArrayList<>(9);
        boolean patternEmpty = true;
        for (int i = 0; i < 9; i++) {
            ItemStack s = entity.inventory.get(PATTERN_START + i);
            patternStacks.add(s);
            if (!s.isEmpty()) patternEmpty = false;
        }

        if (patternEmpty) {
            entity.hasValidRecipe = false;
            entity.craftProgress = 0;
            if (!entity.inventory.get(PREVIEW_SLOT).isEmpty()) {
                entity.inventory.set(PREVIEW_SLOT, ItemStack.EMPTY);
                entity.setChanged();
            }
            if (wasLit) world.setBlock(pos, state.setValue(SuperComputerBlock.LIT, false), 3);
            return;
        }

        CraftingInput recipeInput = CraftingInput.of(3, 3, patternStacks);
        Optional<RecipeHolder<CraftingRecipe>> match = world.recipeAccess().getRecipeFor(RecipeType.CRAFTING, recipeInput, world);

        ItemStack resultStack = ItemStack.EMPTY;
        if (match.isPresent()) {
            entity.hasValidRecipe = true;
            resultStack = match.get().value().assemble(recipeInput);
        } else {
            ItemStack directCast = getDirectCastingPatternResult(patternStacks);
            ItemStack directFab = getDirectFabricatorPatternResult(patternStacks);
            ItemStack directPress = getDirectPressPatternResult(patternStacks);
            ItemStack directSmelt = getDirectSmeltingPatternResult(world, patternStacks);
            ItemStack directWater = getDirectWaterPumpPatternResult(patternStacks);
            ItemStack directLava = getDirectLavaPatternResult(patternStacks);

            if (directCast != null) {
                entity.hasValidRecipe = true;
                resultStack = directCast;
            } else if (directFab != null) {
                entity.hasValidRecipe = true;
                resultStack = directFab;
            } else if (directPress != null) {
                entity.hasValidRecipe = true;
                resultStack = directPress;
            } else if (directSmelt != null) {
                entity.hasValidRecipe = true;
                resultStack = directSmelt;
            } else if (directWater != null) {
                entity.hasValidRecipe = true;
                resultStack = directWater;
            } else if (directLava != null) {
                entity.hasValidRecipe = true;
                resultStack = directLava;
            } else {
                entity.hasValidRecipe = false;
                entity.craftProgress = 0;
                if (!entity.inventory.get(PREVIEW_SLOT).isEmpty()) {
                    entity.inventory.set(PREVIEW_SLOT, ItemStack.EMPTY);
                    entity.setChanged();
                }
                if (wasLit) world.setBlock(pos, state.setValue(SuperComputerBlock.LIT, false), 3);
                return;
            }
        }

        if (resultStack.isEmpty()) {
            entity.craftProgress = 0;
            if (!entity.inventory.get(PREVIEW_SLOT).isEmpty()) {
                entity.inventory.set(PREVIEW_SLOT, ItemStack.EMPTY);
                entity.setChanged();
            }
            if (wasLit) world.setBlock(pos, state.setValue(SuperComputerBlock.LIT, false), 3);
            return;
        }

        // Keep preview slot updated with the crafted result (Display only, no auto-crafting)
        if (!ItemStack.isSameItemSameComponents(entity.inventory.get(PREVIEW_SLOT), resultStack)) {
            entity.inventory.set(PREVIEW_SLOT, resultStack.copy());
            entity.setChanged();
        }

        if (wasLit) {
            world.setBlock(pos, state.setValue(SuperComputerBlock.LIT, false), 3);
        }
    }

    private void tickActiveJob(ServerLevel world, BlockPos pos, BlockState state) {
        if (this.activeJob == null) return;
        ActiveCraftJob job = this.activeJob;
        CraftStep step = job.getCurrentStep();

        if (step == null) {
            // All steps completed!
            EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
            Player player = world.getPlayerByUUID(job.playerUuid);

            depositCraftedResult(terminal, player, job.finalResult.copy());
            for (ItemStack leftover : job.leftoverItems) {
                if (!leftover.isEmpty()) {
                    depositCraftedResult(terminal, player, leftover.copy());
                }
            }

            if (player != null) {
                sendFeedback(player, "§a⚡ Factory Completed: §f" + job.finalResult.getCount() + "x " + job.finalResult.getHoverName().getString());
            }

            this.craftProgress = 0;
            this.maxCraftProgress = 0;
            boolean hadCraftAll = job.craftAll;
            this.activeJob = null;

            for (HydraulicPressBlockEntity p : this.cachedPresses) {
                if (p != null && !p.isRemoved() && p.isExternalProcess()) p.clearExternalProcess();
            }
            for (CircuitFabricatorBlockEntity f : this.cachedFabricators) {
                if (f != null && !f.isRemoved() && f.isExternalProcess()) f.clearExternalProcess();
            }
            for (EnchantedFurnaceBlockEntity ef : this.cachedFurnaces) {
                if (ef != null && !ef.isRemoved() && ef.isExternalProcess()) {
                    ef.clearExternalProcess();
                    BlockPos fPos = ef.getBlockPos();
                    BlockState fState = world.getBlockState(fPos);
                    if (fState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) && fState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) {
                        world.setBlock(fPos, fState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false), 3);
                    }
                }
            }

            setChanged();

            if (hadCraftAll && player != null) {
                executeManualCraft(player, true);
            } else {
                world.setBlock(pos, state.setValue(SuperComputerBlock.LIT, false), 3);
            }
            return;
        }

        // Keep Super Computer LIT
        if (!state.getValue(SuperComputerBlock.LIT)) {
            world.setBlock(pos, state.setValue(SuperComputerBlock.LIT, true), 3);
        }

        // Determine power draw for current step
        int powerDraw = switch (step.type) {
            case PRESS -> 40;
            case FABRICATE -> 50;
            case SMELT -> 30;
            case CAST -> 25;
            case PUMP_WATER -> 20;
            case PUMP_LAVA -> 30;
            case MELT_LAVA -> 35;
            case ASSEMBLE -> 20;
        };

        // Extract power directly from the active physical workstation first
        boolean powered = false;
        switch (step.type) {
            case PRESS -> {
                HydraulicPressBlockEntity press = getBestAvailablePress();
                if (press != null && press.getEnergyStorage(null).getEnergy() >= powerDraw) {
                    press.getEnergyStorage(null).extractEnergy(powerDraw, false);
                    powered = true;
                }
            }
            case FABRICATE -> {
                CircuitFabricatorBlockEntity fab = getBestAvailableFabricator();
                if (fab != null && fab.getEnergyStorage(null).getEnergy() >= powerDraw) {
                    fab.getEnergyStorage(null).extractEnergy(powerDraw, false);
                    powered = true;
                }
            }
            case PUMP_WATER -> {
                WaterPumpBlockEntity pump = getBestAvailableWaterPump();
                if (pump != null && pump.getEnergyStorage(null).getEnergy() >= powerDraw) {
                    pump.getEnergyStorage(null).extractEnergy(powerDraw, false);
                    powered = true;
                }
            }
            case PUMP_LAVA -> {
                LavaPumpBlockEntity pump = getBestAvailableLavaPump();
                if (pump != null && pump.getEnergyStorage(null).getEnergy() >= powerDraw) {
                    pump.getEnergyStorage(null).extractEnergy(powerDraw, false);
                    powered = true;
                }
            }
            case MELT_LAVA -> {
                MagmaCrucibleBlockEntity crucible = getBestAvailableCrucible();
                if (crucible != null && crucible.getEnergyStorage(null).getEnergy() >= powerDraw) {
                    crucible.getEnergyStorage(null).extractEnergy(powerDraw, false);
                    powered = true;
                }
            }
            default -> {}
        }

        // If the workstation did not have enough power, pull from Super Computer or base network
        if (!powered) {
            if (this.energyStorage.getEnergy() >= powerDraw) {
                this.energyStorage.extractEnergy(powerDraw, false);
                powered = true;
            } else if (drawNetworkPower()) {
                this.energyStorage.extractEnergy(powerDraw, false);
                powered = true;
            }
        }

        if (!powered) {
            // Stalled due to lack of power!
            if (world.getGameTime() % 60 == 0) {
                Player p = world.getPlayerByUUID(job.playerUuid);
                if (p != null) sendFeedback(p, "§c[Super Computer] Factory paused: Insufficient Energy! (" + powerDraw + " FE/t needed)");
            }
            return;
        }

        int stepMax = getStepDurationTicks(step);
        job.currentStepMaxTicks = stepMax;

        // Real physical machine operation, animations, and in-world particles
        switch (step.type) {
            case PRESS -> {
                HydraulicPressBlockEntity press = getBestAvailablePress();
                if (press != null) {
                    ItemStack inputStack = step.inputItem != null ? new ItemStack(step.inputItem, 1) : ItemStack.EMPTY;
                    press.setExternalProcess(inputStack, job.currentStepTicks, stepMax);
                    BlockPos pPos = press.getBlockPos();
                    if (world.getGameTime() % 3 == 0) {
                        world.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, pPos.getX() + 0.5, pPos.getY() + 0.8, pPos.getZ() + 0.5, 4, 0.15, 0.15, 0.15, 0.02);
                    }
                }
            }
            case SMELT -> {
                EnchantedFurnaceBlockEntity furnace = getBestAvailableFurnace();
                if (furnace != null) {
                    BlockPos fPos = furnace.getBlockPos();
                    BlockState fState = world.getBlockState(fPos);
                    if (fState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) && !fState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) {
                        world.setBlock(fPos, fState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, true), 3);
                    }
                    ItemStack inStack = step.inputItem != null ? new ItemStack(step.inputItem, 1) : ItemStack.EMPTY;
                    furnace.setExternalProcess(inStack, job.currentStepTicks, stepMax);
                    if (world.getGameTime() % 4 == 0) {
                        world.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, fPos.getX() + 0.5, fPos.getY() + 0.5, fPos.getZ() + 0.5, 3, 0.15, 0.15, 0.15, 0.02);
                    }
                    if (world.getGameTime() % 20 == 0) {
                        world.playSound(null, fPos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.8f, 1.0f);
                    }
                }
            }
            case CAST -> {
                CastingPortBlockEntity caster = getBestAvailableCaster();
                if (caster != null) {
                    BlockPos cPos = caster.getBlockPos();
                    if (world.getGameTime() % 3 == 0) {
                        world.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, cPos.getX() + 0.5, cPos.getY() + 0.8, cPos.getZ() + 0.5, 3, 0.15, 0.15, 0.15, 0.02);
                    }
                    if (world.getGameTime() % 15 == 0) {
                        world.playSound(null, cPos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.3f);
                    }
                }
            }
            case FABRICATE -> {
                CircuitFabricatorBlockEntity fab = getBestAvailableFabricator();
                if (fab != null) {
                    ItemStack substrateStack = step.inputItem != null ? new ItemStack(step.inputItem, 1) : ItemStack.EMPTY;
                    fab.setExternalProcess(substrateStack, job.currentStepTicks, stepMax);
                    BlockPos bPos = fab.getBlockPos();
                    if (world.getGameTime() % 3 == 0) {
                        world.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT, bPos.getX() + 0.5, bPos.getY() + 0.8, bPos.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.05);
                    }
                    if (world.getGameTime() % 25 == 0) {
                        world.playSound(null, bPos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.8f);
                    }
                }
            }
            case PUMP_WATER -> {
                WaterPumpBlockEntity pump = getBestAvailableWaterPump();
                BlockPos wPos = pump != null ? pump.getBlockPos() : pos;
                if (world.getGameTime() % 4 == 0) {
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH, wPos.getX() + 0.5, wPos.getY() + 0.9, wPos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.05);
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE, wPos.getX() + 0.5, wPos.getY() + 0.8, wPos.getZ() + 0.5, 3, 0.15, 0.15, 0.15, 0.02);
                }
                if (world.getGameTime() % 15 == 0) {
                    world.playSound(null, wPos, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.6f, 1.2f);
                }
            }
            case PUMP_LAVA -> {
                LavaPumpBlockEntity pump = getBestAvailableLavaPump();
                BlockPos lPos = pump != null ? pump.getBlockPos() : pos;
                if (world.getGameTime() % 4 == 0) {
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, lPos.getX() + 0.5, lPos.getY() + 0.9, lPos.getZ() + 0.5, 3, 0.15, 0.15, 0.15, 0.02);
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, lPos.getX() + 0.5, lPos.getY() + 0.8, lPos.getZ() + 0.5, 2, 0.15, 0.15, 0.15, 0.02);
                }
                if (world.getGameTime() % 15 == 0) {
                    world.playSound(null, lPos, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.6f, 1.0f);
                }
            }
            case MELT_LAVA -> {
                MagmaCrucibleBlockEntity crucible = getBestAvailableCrucible();
                BlockPos cPos = crucible != null ? crucible.getBlockPos() : pos;
                if (world.getGameTime() % 3 == 0) {
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, cPos.getX() + 0.5, cPos.getY() + 0.9, cPos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.03);
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, cPos.getX() + 0.5, cPos.getY() + 0.9, cPos.getZ() + 0.5, 3, 0.15, 0.15, 0.15, 0.02);
                }
                if (world.getGameTime() % 15 == 0) {
                    world.playSound(null, cPos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.7f, 1.0f);
                }
            }
            case ASSEMBLE -> {
                if (world.getGameTime() % 3 == 0) {
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.05);
                }
            }
        }

        job.currentStepTicks++;

        // Sync progress to delegate & GUI
        this.maxCraftProgress = job.getTotalEstimatedTicks(this);
        this.craftProgress = job.getCompletedTicks(this);

        if (job.currentStepTicks >= stepMax) {
            // Current step completed! Trigger sound & particles at the operating machine
            switch (step.type) {
                case PRESS -> {
                    HydraulicPressBlockEntity press = getBestAvailablePress();
                    BlockPos sPos = press != null ? press.getBlockPos() : pos;
                    if (press != null) {
                        press.clearExternalProcess();
                    }
                    world.playSound(null, sPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 0.6f);
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, sPos.getX() + 0.5, sPos.getY() + 0.8, sPos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.1);
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, sPos.getX() + 0.5, sPos.getY() + 0.8, sPos.getZ() + 0.5, 6, 0.15, 0.15, 0.15, 0.05);
                }
                case SMELT -> {
                    EnchantedFurnaceBlockEntity furnace = getBestAvailableFurnace();
                    BlockPos sPos = furnace != null ? furnace.getBlockPos() : pos;
                    if (furnace != null) {
                        furnace.clearExternalProcess();
                        BlockState fState = world.getBlockState(sPos);
                        if (fState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) && fState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) {
                            world.setBlock(sPos, fState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false), 3);
                        }
                    }
                    world.playSound(null, sPos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.9f, 1.2f);
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, sPos.getX() + 0.5, sPos.getY() + 0.8, sPos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.05);
                }
                case CAST -> {
                    CastingPortBlockEntity caster = getBestAvailableCaster();
                    BlockPos sPos = caster != null ? caster.getBlockPos() : pos;
                    world.playSound(null, sPos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.2f);
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, sPos.getX() + 0.5, sPos.getY() + 0.8, sPos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.05);
                }
                case FABRICATE -> {
                    CircuitFabricatorBlockEntity fab = getBestAvailableFabricator();
                    BlockPos sPos = fab != null ? fab.getBlockPos() : pos;
                    if (fab != null) {
                        fab.clearExternalProcess();
                    }
                    world.playSound(null, sPos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8f, 1.6f);
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT, sPos.getX() + 0.5, sPos.getY() + 0.8, sPos.getZ() + 0.5, 15, 0.2, 0.2, 0.2, 0.1);
                }
                case PUMP_WATER -> {
                    WaterPumpBlockEntity pump = getBestAvailableWaterPump();
                    if (pump != null) {
                        pump.extractWater(1000, false);
                    }
                    BlockPos sPos = pump != null ? pump.getBlockPos() : pos;
                    world.playSound(null, sPos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.8f, 1.0f);
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH, sPos.getX() + 0.5, sPos.getY() + 0.9, sPos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.05);
                }
                case PUMP_LAVA -> {
                    LavaPumpBlockEntity pump = getBestAvailableLavaPump();
                    if (pump != null) {
                        pump.extractLava(1000, false);
                    }
                    BlockPos sPos = pump != null ? pump.getBlockPos() : pos;
                    world.playSound(null, sPos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 0.8f, 1.0f);
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, sPos.getX() + 0.5, sPos.getY() + 0.9, sPos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.05);
                }
                case MELT_LAVA -> {
                    MagmaCrucibleBlockEntity crucible = getBestAvailableCrucible();
                    if (crucible != null) {
                        crucible.extractLava(1000, false);
                    }
                    BlockPos sPos = crucible != null ? crucible.getBlockPos() : pos;
                    world.playSound(null, sPos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 0.8f, 1.0f);
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, sPos.getX() + 0.5, sPos.getY() + 0.9, sPos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.05);
                }
                case ASSEMBLE -> {
                    world.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.4f);
                }
            }

            job.currentStepIndex++;
            job.currentStepTicks = 0;
        }

        setChanged();
    }

    public void executeManualCraft(Player player, boolean craftAll) {
        if (!(this.level instanceof ServerLevel serverWorld)) return;
        updateMachineCache(true);

        if (this.activeJob != null) {
            sendFeedback(player, "§e[Super Computer] Factory is currently busy working on a job!");
            return;
        }

        List<ItemStack> patternStacks = new ArrayList<>(9);
        boolean patternEmpty = true;
        for (int i = 0; i < 9; i++) {
            ItemStack s = this.inventory.get(PATTERN_START + i);
            patternStacks.add(s);
            if (!s.isEmpty()) patternEmpty = false;
        }

        if (patternEmpty) {
            sendFeedback(player, "§c[Super Computer] Place items in the 3x3 matrix to program a recipe!");
            return;
        }

        CraftingInput recipeInput = CraftingInput.of(3, 3, patternStacks);
        Optional<RecipeHolder<CraftingRecipe>> match = serverWorld.recipeAccess().getRecipeFor(RecipeType.CRAFTING, recipeInput, serverWorld);
        if (match.isEmpty()) {
            ItemStack directCast = getDirectCastingPatternResult(patternStacks);
            if (directCast != null) {
                executeDirectCasting(serverWorld, player, directCast, craftAll);
                return;
            }
            ItemStack directFab = getDirectFabricatorPatternResult(patternStacks);
            if (directFab != null) {
                executeDirectFabrication(serverWorld, player, patternStacks, directFab, craftAll);
                return;
            }
            ItemStack directPress = getDirectPressPatternResult(patternStacks);
            if (directPress != null) {
                executeDirectPress(serverWorld, player, patternStacks, directPress, craftAll);
                return;
            }
            ItemStack directSmelt = getDirectSmeltingPatternResult(serverWorld, patternStacks);
            if (directSmelt != null) {
                executeDirectSmelting(serverWorld, player, patternStacks, directSmelt, craftAll);
                return;
            }
            ItemStack directWater = getDirectWaterPumpPatternResult(patternStacks);
            if (directWater != null) {
                executeDirectWaterPumping(serverWorld, player, patternStacks, directWater, craftAll);
                return;
            }
            ItemStack directLava = getDirectLavaPatternResult(patternStacks);
            if (directLava != null) {
                executeDirectLavaPumping(serverWorld, player, patternStacks, directLava, craftAll);
                return;
            }
            sendFeedback(player, "§c[Super Computer] No valid crafting recipe in the 3x3 grid!");
            return;
        }

        ItemStack resultStack = match.get().value().assemble(recipeInput);
        if (resultStack.isEmpty()) return;

        if (!canAcceptOutput(resultStack)) {
            sendFeedback(player, "§c[Super Computer] Output buffer & digital storage are full!");
            return;
        }

        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        CraftingPlanResult planResult = resolveCraftingPlan(serverWorld, terminal, player, match.get().value(), patternStacks);
        if (!planResult.success || planResult.plan == null) {
            if (!planResult.missingItems.isEmpty()) {
                StringBuilder sb = new StringBuilder("§cMissing: §e");
                boolean first = true;
                for (java.util.Map.Entry<String, Integer> entry : planResult.missingItems.entrySet()) {
                    if (!first) sb.append("§7, §e");
                    sb.append(entry.getValue()).append("x ").append(entry.getKey());
                    first = false;
                }
                sendFeedback(player, sb.toString());
            } else {
                sendFeedback(player, "§c[Super Computer] Missing required materials in storage or inventory!");
            }
            return;
        }

        CraftingPlan plan = planResult.plan;

        if (plan.hydraulicPressings > 0 && getBestAvailablePress() == null) {
            sendFeedback(player, "§c[Super Computer] All connected Hydraulic Presses are currently busy!");
            return;
        }
        if (plan.circuitFabrications > 0 && getBestAvailableFabricator() == null) {
            sendFeedback(player, "§c[Super Computer] All connected Circuit Fabricators are currently busy!");
            return;
        }
        if (plan.smeltingSteps > 0 && getBestAvailableFurnace() == null) {
            sendFeedback(player, "§c[Super Computer] All connected Enchanted Furnaces are currently busy!");
            return;
        }
        if (plan.moltenMetalUsedMb > 0 && getBestAvailableCaster() == null) {
            sendFeedback(player, "§c[Super Computer] All connected Casting Ports are currently busy!");
            return;
        }

        // Deduct raw ingredients and molten metals up-front (committed to the job)
        consumeIngredients(terminal, player, plan.rawIngredientsToConsume);
        if (!plan.moltenMetalsToConsume.isEmpty()) {
            consumeMoltenMetals(plan.moltenMetalsToConsume);
        }

        StringBuilder opBreakdown = new StringBuilder();
        if (plan.smeltingSteps > 0) opBreakdown.append(plan.smeltingSteps).append("x Smelt, ");
        if (plan.hydraulicPressings > 0) opBreakdown.append(plan.hydraulicPressings).append("x Press, ");
        if (plan.circuitFabrications > 0) opBreakdown.append(plan.circuitFabrications).append("x Fab, ");
        if (plan.moltenMetalUsedMb > 0) opBreakdown.append(plan.moltenMetalUsedMb).append("mB Cast, ");
        String opStr = opBreakdown.toString();
        if (opStr.endsWith(", ")) opStr = opStr.substring(0, opStr.length() - 2);

        this.activeJob = new ActiveCraftJob(player.getUUID(), plan.steps, resultStack.copy(), plan.leftoverSynthesized, craftAll, patternStacks);
        sendFeedback(player, "§6⚡ Factory Activated: §fManufacturing " + resultStack.getHoverName().getString() + " §7(" + (opStr.isEmpty() ? plan.steps.size() + " ops" : opStr) + ")");
        setChanged();
        serverWorld.setBlock(this.worldPosition, serverWorld.getBlockState(this.worldPosition).setValue(SuperComputerBlock.LIT, true), 3);
    }

    private void executeDirectCasting(ServerLevel serverWorld, Player player, ItemStack directCast, boolean craftAll) {
        updateMachineCache(true);
        if (!isCasterOnline()) {
            sendFeedback(player, "§e[Super Computer] Place a Casting Port within 16 blocks to enable metal casting!");
            return;
        }

        if (this.activeJob != null) {
            sendFeedback(player, "§e[Super Computer] Factory is currently busy working on a job!");
            return;
        }

        if (getBestAvailableCaster() == null) {
            sendFeedback(player, "§c[Super Computer] All connected Casting Ports are currently busy!");
            return;
        }

        MetalCastInfo castInfo = getMetalCastInfo(directCast.getItem());
        if (castInfo == null) {
            sendFeedback(player, "§c[Super Computer] Item cannot be cast from molten metal!");
            return;
        }

        int perBatchCost = castInfo.costMb * directCast.getCount();
        if (!canAcceptOutput(directCast)) {
            sendFeedback(player, "§c[Super Computer] Output buffer & digital storage are full!");
            return;
        }

        java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> avail = getAvailableMoltenMetals();
        if (avail.getOrDefault(castInfo.metal, 0) < perBatchCost) {
            sendFeedback(player, "§c[Super Computer] Missing Molten " + castInfo.metal.getDisplayName() + "! (Needs " + perBatchCost + " mB in tanks)");
            return;
        }

        // Deduct fluid up-front
        java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> toConsume = new java.util.EnumMap<>(net.enchantedwood.fluid.MoltenMetal.class);
        toConsume.put(castInfo.metal, perBatchCost);
        consumeMoltenMetals(toConsume);

        List<CraftStep> steps = List.of(new CraftStep(StepType.CAST, castInfo.metal, directCast.getItem(), perBatchCost));
        this.activeJob = new ActiveCraftJob(player.getUUID(), steps, directCast.copy(), List.of(), craftAll, List.of());
        sendFeedback(player, "§6⚡ Casting: §f" + directCast.getHoverName().getString() + " §6[" + perBatchCost + " mB Molten " + castInfo.metal.getDisplayName() + "]");
        setChanged();
        serverWorld.setBlock(this.worldPosition, serverWorld.getBlockState(this.worldPosition).setValue(SuperComputerBlock.LIT, true), 3);
    }

    private void executeDirectFabrication(ServerLevel serverWorld, Player player, List<ItemStack> patternStacks, ItemStack directFab, boolean craftAll) {
        updateMachineCache(true);
        if (!isCircuitFabricatorOnline()) {
            sendFeedback(player, "§e[Super Computer] Place a Circuit Fabricator within 32 blocks to enable chip fabrication!");
            return;
        }

        if (this.activeJob != null) {
            sendFeedback(player, "§e[Super Computer] Factory is currently busy working on a job!");
            return;
        }

        CircuitFabricatorBlockEntity.FabricatorRecipe recipe = getMatchingFabricatorRecipe(patternStacks);
        if (recipe == null) {
            sendFeedback(player, "§c[Super Computer] No valid circuit recipe in grid!");
            return;
        }

        if (!canAcceptOutput(directFab)) {
            sendFeedback(player, "§c[Super Computer] Output buffer & digital storage are full!");
            return;
        }

        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        CraftingPlanResult planResult = resolveDirectFabricatorPlan(serverWorld, terminal, player, recipe);
        if (!planResult.success || planResult.plan == null) {
            if (!planResult.missingItems.isEmpty()) {
                StringBuilder sb = new StringBuilder("§cMissing: §e");
                boolean first = true;
                for (java.util.Map.Entry<String, Integer> entry : planResult.missingItems.entrySet()) {
                    if (!first) sb.append("§7, §e");
                    sb.append(entry.getValue()).append("x ").append(entry.getKey());
                    first = false;
                }
                sendFeedback(player, sb.toString());
            } else {
                sendFeedback(player, "§c[Super Computer] Missing required components for chip fabrication!");
            }
            return;
        }

        CraftingPlan plan = planResult.plan;

        if (plan.smeltingSteps > 0 && getBestAvailableFurnace() == null) {
            sendFeedback(player, "§c[Super Computer] All connected Enchanted Furnaces are currently busy!");
            return;
        }
        if (plan.hydraulicPressings > 0 && getBestAvailablePress() == null) {
            sendFeedback(player, "§c[Super Computer] All connected Hydraulic Presses are currently busy!");
            return;
        }
        if (plan.circuitFabrications > 0 && getBestAvailableFabricator() == null) {
            sendFeedback(player, "§c[Super Computer] All connected Circuit Fabricators are currently busy!");
            return;
        }
        if (plan.moltenMetalUsedMb > 0 && getBestAvailableCaster() == null) {
            sendFeedback(player, "§c[Super Computer] All connected Casting Ports are currently busy!");
            return;
        }

        consumeIngredients(terminal, player, plan.rawIngredientsToConsume);
        if (!plan.moltenMetalsToConsume.isEmpty()) {
            consumeMoltenMetals(plan.moltenMetalsToConsume);
        }

        StringBuilder opBreakdown = new StringBuilder();
        if (plan.smeltingSteps > 0) opBreakdown.append(plan.smeltingSteps).append("x Smelt, ");
        if (plan.hydraulicPressings > 0) opBreakdown.append(plan.hydraulicPressings).append("x Press, ");
        if (plan.circuitFabrications > 0) opBreakdown.append(plan.circuitFabrications).append("x Fab, ");
        if (plan.moltenMetalUsedMb > 0) opBreakdown.append(plan.moltenMetalUsedMb).append("mB Cast, ");
        String opStr = opBreakdown.toString();
        if (opStr.endsWith(", ")) opStr = opStr.substring(0, opStr.length() - 2);

        this.activeJob = new ActiveCraftJob(player.getUUID(), plan.steps, directFab.copy(), plan.leftoverSynthesized, craftAll, patternStacks);
        sendFeedback(player, "§6⚡ Fabricating: §f" + directFab.getHoverName().getString() + " §7(" + (opStr.isEmpty() ? plan.steps.size() + " ops" : opStr) + ")");
        setChanged();
        serverWorld.setBlock(this.worldPosition, serverWorld.getBlockState(this.worldPosition).setValue(SuperComputerBlock.LIT, true), 3);
    }

    private void executeDirectPress(ServerLevel serverWorld, Player player, List<ItemStack> patternStacks, ItemStack directPress, boolean craftAll) {
        updateMachineCache(true);
        if (!isPressOnline()) {
            sendFeedback(player, "§e[Super Computer] Place a Hydraulic Press within 32 blocks to enable pressing!");
            return;
        }

        if (this.activeJob != null) {
            sendFeedback(player, "§e[Super Computer] Factory is currently busy working on a job!");
            return;
        }

        if (getBestAvailablePress() == null) {
            sendFeedback(player, "§c[Super Computer] All connected Hydraulic Presses are currently busy!");
            return;
        }

        ItemStack single = null;
        for (ItemStack s : patternStacks) {
            if (!s.isEmpty()) {
                if (single != null) return;
                single = s;
            }
        }
        if (single == null) return;

        net.minecraft.world.item.Item rawInputItem = single.getItem();
        ItemStack oneBatchResult = HydraulicPressBlockEntity.getPlateResult(rawInputItem);
        if (oneBatchResult.isEmpty()) return;

        if (!canAcceptOutput(oneBatchResult)) {
            sendFeedback(player, "§c[Super Computer] Output buffer & digital storage are full!");
            return;
        }

        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        java.util.Map<net.minecraft.world.item.Item, Integer> available = collectAvailableItems(terminal, player);
        int availCount = available.getOrDefault(rawInputItem, 0);

        if (availCount < 1) {
            sendFeedback(player, "§c[Super Computer] Missing: §e1x " + new ItemStack(rawInputItem).getHoverName().getString());
            return;
        }

        consumeIngredients(terminal, player, List.of(new ItemStack(rawInputItem, 1)));
        List<CraftStep> steps = List.of(new CraftStep(StepType.PRESS, rawInputItem, oneBatchResult.getItem()));
        this.activeJob = new ActiveCraftJob(player.getUUID(), steps, oneBatchResult.copy(), List.of(), craftAll, patternStacks);
        sendFeedback(player, "§6⚡ Pressing: §f" + oneBatchResult.getHoverName().getString());
        setChanged();
        serverWorld.setBlock(this.worldPosition, serverWorld.getBlockState(this.worldPosition).setValue(SuperComputerBlock.LIT, true), 3);
    }

    private void executeDirectSmelting(ServerLevel serverWorld, Player player, List<ItemStack> patternStacks, ItemStack directSmelt, boolean craftAll) {
        updateMachineCache(true);
        if (!isFurnaceOnline()) {
            sendFeedback(player, "§e[Super Computer] Place an Enchanted Furnace within 48 blocks to enable automated smelting!");
            return;
        }

        if (this.activeJob != null) {
            sendFeedback(player, "§e[Super Computer] Factory is currently busy working on a job!");
            return;
        }

        if (getBestAvailableFurnace() == null) {
            sendFeedback(player, "§c[Super Computer] All connected Enchanted Furnaces are currently busy!");
            return;
        }

        ItemStack single = null;
        for (ItemStack s : patternStacks) {
            if (!s.isEmpty()) {
                if (single != null) return;
                single = s;
            }
        }
        if (single == null) return;

        net.minecraft.world.item.Item rawInputItem = single.getItem();
        ItemStack oneBatchResult = directSmelt.copyWithCount(directSmelt.getCount() / Math.max(1, single.getCount()));
        if (oneBatchResult.isEmpty()) oneBatchResult = directSmelt.copyWithCount(1);

        if (!canAcceptOutput(oneBatchResult)) {
            sendFeedback(player, "§c[Super Computer] Output buffer & digital storage are full!");
            return;
        }

        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        java.util.Map<net.minecraft.world.item.Item, Integer> available = collectAvailableItems(terminal, player);
        int availCount = available.getOrDefault(rawInputItem, 0);

        if (availCount < 1) {
            sendFeedback(player, "§c[Super Computer] Missing: §e1x " + new ItemStack(rawInputItem).getHoverName().getString());
            return;
        }

        consumeIngredients(terminal, player, List.of(new ItemStack(rawInputItem, 1)));
        List<CraftStep> steps = List.of(new CraftStep(StepType.SMELT, rawInputItem, oneBatchResult.getItem()));
        this.activeJob = new ActiveCraftJob(player.getUUID(), steps, oneBatchResult.copy(), List.of(), craftAll, patternStacks);
        sendFeedback(player, "§6⚡ Smelting: §f" + oneBatchResult.getHoverName().getString());
        setChanged();
        serverWorld.setBlock(this.worldPosition, serverWorld.getBlockState(this.worldPosition).setValue(SuperComputerBlock.LIT, true), 3);
    }

    private void executeDirectWaterPumping(ServerLevel serverWorld, Player player, List<ItemStack> patternStacks, ItemStack directWater, boolean craftAll) {
        updateMachineCache(true);
        if (!isWaterPumpOnline()) {
            sendFeedback(player, "§e[Super Computer] Place an Electric Water Pump (or water source) within 48 blocks to enable automated water extraction!");
            return;
        }

        if (this.activeJob != null) {
            sendFeedback(player, "§e[Super Computer] Factory is currently busy working on a job!");
            return;
        }

        boolean isCopper = directWater.is(ModItems.COPPER_WATER_BUCKET);
        net.minecraft.world.item.Item emptyBucket = isCopper ? ModItems.COPPER_BUCKET : Items.BUCKET;

        if (!canAcceptOutput(directWater)) {
            sendFeedback(player, "§c[Super Computer] Output buffer & digital storage are full!");
            return;
        }

        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        java.util.Map<net.minecraft.world.item.Item, Integer> available = collectAvailableItems(terminal, player);
        int availBuckets = available.getOrDefault(emptyBucket, 0);

        if (availBuckets < 1) {
            sendFeedback(player, "§c[Super Computer] Missing: §e1x " + new ItemStack(emptyBucket).getHoverName().getString() + " §7(Place empty bucket in storage, Water Pump, or inventory)");
            return;
        }

        consumeIngredients(terminal, player, List.of(new ItemStack(emptyBucket, 1)));
        List<CraftStep> steps = List.of(new CraftStep(StepType.PUMP_WATER, emptyBucket, directWater.getItem()));
        this.activeJob = new ActiveCraftJob(player.getUUID(), steps, directWater.copy(), List.of(), craftAll, patternStacks);
        sendFeedback(player, "§6⚡ Pumping Water: §f" + directWater.getHoverName().getString());
        setChanged();
        serverWorld.setBlock(this.worldPosition, serverWorld.getBlockState(this.worldPosition).setValue(SuperComputerBlock.LIT, true), 3);
    }

    private void executeDirectLavaPumping(ServerLevel serverWorld, Player player, List<ItemStack> patternStacks, ItemStack directLava, boolean craftAll) {
        updateMachineCache(true);
        if (!isLavaSourceOnline()) {
            sendFeedback(player, "§e[Super Computer] Place a Lava Pump or Magma Crucible within 48 blocks to enable lava handling!");
            return;
        }

        if (this.activeJob != null) {
            sendFeedback(player, "§e[Super Computer] Factory is currently busy working on a job!");
            return;
        }

        boolean isCopper = directLava.is(ModItems.COPPER_LAVA_BUCKET);
        net.minecraft.world.item.Item emptyBucket = isCopper ? ModItems.COPPER_BUCKET : Items.BUCKET;

        if (!canAcceptOutput(directLava)) {
            sendFeedback(player, "§c[Super Computer] Output buffer & digital storage are full!");
            return;
        }

        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        java.util.Map<net.minecraft.world.item.Item, Integer> available = collectAvailableItems(terminal, player);
        int availBuckets = available.getOrDefault(emptyBucket, 0);

        if (availBuckets < 1) {
            sendFeedback(player, "§c[Super Computer] Missing: §e1x " + new ItemStack(emptyBucket).getHoverName().getString() + " §7(Place empty bucket in storage, Lava Pump, or inventory)");
            return;
        }

        StepType stepType = isLavaPumpOnline() ? StepType.PUMP_LAVA : StepType.MELT_LAVA;
        consumeIngredients(terminal, player, List.of(new ItemStack(emptyBucket, 1)));
        List<CraftStep> steps = List.of(new CraftStep(stepType, emptyBucket, directLava.getItem()));
        this.activeJob = new ActiveCraftJob(player.getUUID(), steps, directLava.copy(), List.of(), craftAll, patternStacks);
        sendFeedback(player, "§6⚡ Pumping Lava: §f" + directLava.getHoverName().getString());
        setChanged();
        serverWorld.setBlock(this.worldPosition, serverWorld.getBlockState(this.worldPosition).setValue(SuperComputerBlock.LIT, true), 3);
    }

    private void sendFeedback(Player player, String msg) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(serverPlayer, new net.enchantedwood.network.SuperComputerStatusPayload(msg));
        }
        player.sendSystemMessage(Component.literal(msg));
    }

    public void executeManualCraft(Player player) {
        executeManualCraft(player, false);
    }

    private boolean drawNetworkPower() {
        if (this.level == null) return false;
        if (this.cachedPowerController != null && !this.cachedPowerController.isRemoved() && this.cachedPowerController.isOnline()) {
            this.energyStorage.insertEnergy(1_000, false);
            return true;
        }
        return false;
    }

    public enum StepType {
        CAST,
        SMELT,
        PRESS,
        FABRICATE,
        PUMP_WATER,
        PUMP_LAVA,
        MELT_LAVA,
        ASSEMBLE
    }

    public static class CraftStep {
        public final StepType type;
        public final @Nullable net.minecraft.world.item.Item inputItem;
        public final @Nullable net.minecraft.world.item.Item outputItem;
        public final @Nullable net.enchantedwood.fluid.MoltenMetal moltenMetal;
        public final int amountMb;

        public CraftStep(StepType type, @Nullable net.minecraft.world.item.Item outputItem) {
            this(type, null, outputItem, null, 0);
        }

        public CraftStep(StepType type, @Nullable net.minecraft.world.item.Item inputItem, @Nullable net.minecraft.world.item.Item outputItem) {
            this(type, inputItem, outputItem, null, 0);
        }

        public CraftStep(StepType type, @Nullable net.enchantedwood.fluid.MoltenMetal moltenMetal, @Nullable net.minecraft.world.item.Item outputItem, int amountMb) {
            this(type, null, outputItem, moltenMetal, amountMb);
        }

        public CraftStep(StepType type, @Nullable net.minecraft.world.item.Item inputItem, @Nullable net.minecraft.world.item.Item outputItem,
                         @Nullable net.enchantedwood.fluid.MoltenMetal moltenMetal, int amountMb) {
            this.type = type;
            this.inputItem = inputItem;
            this.outputItem = outputItem;
            this.moltenMetal = moltenMetal;
            this.amountMb = amountMb;
        }
    }

    public static class CraftingPlan {
        public final List<ItemStack> rawIngredientsToConsume = new ArrayList<>();
        public final List<ItemStack> leftoverSynthesized = new ArrayList<>();
        public final java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> moltenMetalsToConsume = new java.util.EnumMap<>(net.enchantedwood.fluid.MoltenMetal.class);
        public final List<CraftStep> steps = new ArrayList<>();
        public int totalCraftingSteps = 1;
        public int moltenMetalUsedMb = 0;
        public int circuitFabrications = 0;
        public int hydraulicPressings = 0;
        public int smeltingSteps = 0;
    }

    public static class CraftingPlanResult {
        public final boolean success;
        public final @Nullable CraftingPlan plan;
        public final java.util.Map<String, Integer> missingItems;

        public CraftingPlanResult(boolean success, @Nullable CraftingPlan plan, java.util.Map<String, Integer> missingItems) {
            this.success = success;
            this.plan = plan;
            this.missingItems = missingItems;
        }
    }

    public static class ActiveCraftJob {
        public final java.util.UUID playerUuid;
        public final List<CraftStep> steps;
        public final ItemStack finalResult;
        public final List<ItemStack> leftoverItems;
        public final boolean craftAll;
        public final List<ItemStack> patternSnapshot;

        public int currentStepIndex = 0;
        public int currentStepTicks = 0;
        public int currentStepMaxTicks = 20;

        public ActiveCraftJob(java.util.UUID playerUuid, List<CraftStep> steps, ItemStack finalResult,
                              List<ItemStack> leftoverItems, boolean craftAll, List<ItemStack> patternSnapshot) {
            this.playerUuid = playerUuid;
            this.steps = new ArrayList<>(steps);
            this.finalResult = finalResult;
            this.leftoverItems = new ArrayList<>(leftoverItems);
            this.craftAll = craftAll;
            this.patternSnapshot = new ArrayList<>(patternSnapshot);
        }

        public @Nullable CraftStep getCurrentStep() {
            if (currentStepIndex >= 0 && currentStepIndex < steps.size()) {
                return steps.get(currentStepIndex);
            }
            return null;
        }

        public int getTotalEstimatedTicks(SuperComputerBlockEntity sc) {
            int total = 0;
            for (CraftStep s : steps) {
                total += sc.getStepDurationTicks(s);
            }
            return Math.max(10, total);
        }

        public int getCompletedTicks(SuperComputerBlockEntity sc) {
            int completed = 0;
            for (int i = 0; i < currentStepIndex && i < steps.size(); i++) {
                completed += sc.getStepDurationTicks(steps.get(i));
            }
            return completed + currentStepTicks;
        }
    }

    public static String getIngredientDisplayName(net.minecraft.world.item.crafting.Ingredient ing) {
        List<net.minecraft.world.item.Item> matching;
        try {
            matching = ing.items()
                    .map(net.minecraft.core.Holder::value)
                    .toList();
        } catch (Throwable t) {
            matching = java.util.Collections.emptyList();
        }

        if (matching.isEmpty()) return "Unknown Material";

        if (matching.size() == 1) {
            return matching.get(0).getName(matching.get(0).getDefaultInstance()).getString();
        }

        // Detect item families
        boolean allLeaves = matching.stream().allMatch(item -> item.getDescriptionId().contains("leaves"));
        if (allLeaves) return "Leaf Blocks (any type)";

        boolean allLogs = matching.stream().allMatch(item -> item.getDescriptionId().contains("log") || item.getDescriptionId().contains("wood") || item.getDescriptionId().contains("stem"));
        if (allLogs) return "Logs (any type)";

        boolean allPlanks = matching.stream().allMatch(item -> item.getDescriptionId().contains("planks"));
        if (allPlanks) return "Planks (any type)";

        boolean allFlowers = matching.stream().allMatch(item -> item.getDescriptionId().contains("flower") || item.getDescriptionId().contains("tulip") || item.getDescriptionId().contains("orchid") || item.getDescriptionId().contains("daisy") || item.getDescriptionId().contains("dandelion") || item.getDescriptionId().contains("poppy") || item.getDescriptionId().contains("allium") || item.getDescriptionId().contains("bluet") || item.getDescriptionId().contains("rose") || item.getDescriptionId().contains("lilac") || item.getDescriptionId().contains("peony"));
        if (allFlowers) return "Flowers (any type)";

        boolean allWool = matching.stream().allMatch(item -> item.getDescriptionId().contains("wool"));
        if (allWool) return "Wool (any type)";

        boolean allGlass = matching.stream().allMatch(item -> item.getDescriptionId().contains("glass"));
        if (allGlass) return "Glass (any type)";

        boolean allDyes = matching.stream().allMatch(item -> item.getDescriptionId().contains("dye"));
        if (allDyes) return "Dye (any type)";

        boolean allSaplings = matching.stream().allMatch(item -> item.getDescriptionId().contains("sapling"));
        if (allSaplings) return "Saplings (any type)";

        boolean allSand = matching.stream().allMatch(item -> item.getDescriptionId().contains("sand"));
        if (allSand) return "Sand (any type)";

        if (matching.size() > 2) {
            return matching.get(0).getName(matching.get(0).getDefaultInstance()).getString() + " (or equivalent)";
        }

        return matching.get(0).getName(matching.get(0).getDefaultInstance()).getString();
    }

    public CraftingPlanResult resolveCraftingPlan(ServerLevel world,
                                                 @Nullable EnchantedStorageTerminalBlockEntity terminal,
                                                 @Nullable Player player,
                                                 CraftingRecipe craftingRecipe,
                                                 List<ItemStack> patternStacks) {
        java.util.Map<String, Integer> missingItems = new java.util.LinkedHashMap<>();
        try {
            // Snapshot available items from Terminal, Player Inventory, Machine networks, and Room storage
            java.util.Map<net.minecraft.world.item.Item, Integer> available = collectAvailableItems(terminal, player);

            CraftingPlan plan = new CraftingPlan();
            java.util.Map<net.minecraft.world.item.Item, Integer> virtualBuffer = new java.util.HashMap<>();
            java.util.Set<net.minecraft.world.item.Item> activeRecursion = new java.util.HashSet<>();
            java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> availableMolten = isCasterOnline()
                    ? new java.util.EnumMap<>(getAvailableMoltenMetals())
                    : new java.util.EnumMap<>(net.enchantedwood.fluid.MoltenMetal.class);

            List<net.minecraft.world.item.crafting.Ingredient> ingredients;
            try {
                ingredients = craftingRecipe.placementInfo().ingredients();
            } catch (Throwable t) {
                ingredients = java.util.Collections.emptyList();
            }

            boolean allSatisfied = true;
            if (!ingredients.isEmpty()) {
                for (net.minecraft.world.item.crafting.Ingredient ing : ingredients) {
                    if (ing == null || ing.isEmpty()) continue;
                    if (!resolveIngredientRequirement(world, ing, available, availableMolten, virtualBuffer, plan, missingItems, activeRecursion, 0)) {
                        allSatisfied = false;
                    }
                }
            } else {
                for (ItemStack req : patternStacks) {
                    if (req.isEmpty()) continue;
                    if (!resolveItemRequirement(world, req.getItem(), available, availableMolten, virtualBuffer, plan, missingItems, activeRecursion, 0)) {
                        allSatisfied = false;
                    }
                }
            }

            if (!allSatisfied) {
                return new CraftingPlanResult(false, null, missingItems);
            }

            // Append root assembly step
            ItemStack rootResult = getSafeRecipeResult(craftingRecipe, world);
            if (rootResult.isEmpty()) {
                rootResult = craftingRecipe.assemble(CraftingInput.of(3, 3, patternStacks));
            }
            plan.steps.add(new CraftStep(StepType.ASSEMBLE, rootResult.isEmpty() ? null : rootResult.getItem()));

            // Record any leftover synthesized items
            for (java.util.Map.Entry<net.minecraft.world.item.Item, Integer> entry : virtualBuffer.entrySet()) {
                if (entry.getValue() > 0) {
                    plan.leftoverSynthesized.add(new ItemStack(entry.getKey(), entry.getValue()));
                }
            }

            // Record any recipe remainders from actually consumed raw ingredients (e.g. empty buckets, bowls, bottles)
            for (ItemStack consumed : plan.rawIngredientsToConsume) {
                if (!consumed.isEmpty()) {
                    ItemStack rem = consumed.getItem().getCraftingRemainder() != null ? consumed.getItem().getCraftingRemainder().create() : ItemStack.EMPTY;
                    if (!rem.isEmpty()) {
                        plan.leftoverSynthesized.add(rem.copyWithCount(consumed.getCount()));
                    }
                }
            }

            return new CraftingPlanResult(true, plan, missingItems);
        } catch (Throwable t) {
            return new CraftingPlanResult(false, null, missingItems);
        }
    }

    public CraftingPlanResult resolveDirectFabricatorPlan(ServerLevel world,
                                                         @Nullable EnchantedStorageTerminalBlockEntity terminal,
                                                         @Nullable Player player,
                                                         CircuitFabricatorBlockEntity.FabricatorRecipe recipe) {
        java.util.Map<String, Integer> missingItems = new java.util.LinkedHashMap<>();
        try {
            java.util.Map<net.minecraft.world.item.Item, Integer> available = collectAvailableItems(terminal, player);

            CraftingPlan plan = new CraftingPlan();
            java.util.Map<net.minecraft.world.item.Item, Integer> virtualBuffer = new java.util.HashMap<>();
            java.util.Set<net.minecraft.world.item.Item> activeRecursion = new java.util.HashSet<>();
            java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> availableMolten = isCasterOnline()
                    ? new java.util.EnumMap<>(getAvailableMoltenMetals())
                    : new java.util.EnumMap<>(net.enchantedwood.fluid.MoltenMetal.class);

            boolean allSatisfied = true;
            if (!resolveItemRequirement(world, recipe.substrate(), available, availableMolten, virtualBuffer, plan, missingItems, activeRecursion, 0)) {
                allSatisfied = false;
            }

            for (net.minecraft.world.item.Item comp : recipe.components()) {
                if (!resolveItemRequirement(world, comp, available, availableMolten, virtualBuffer, plan, missingItems, activeRecursion, 0)) {
                    allSatisfied = false;
                }
            }

            if (!allSatisfied) {
                return new CraftingPlanResult(false, null, missingItems);
            }

            plan.circuitFabrications++;
            plan.steps.add(new CraftStep(StepType.FABRICATE, recipe.substrate(), recipe.output().getItem()));

            for (java.util.Map.Entry<net.minecraft.world.item.Item, Integer> entry : virtualBuffer.entrySet()) {
                if (entry.getValue() > 0) {
                    plan.leftoverSynthesized.add(new ItemStack(entry.getKey(), entry.getValue()));
                }
            }

            return new CraftingPlanResult(true, plan, missingItems);
        } catch (Throwable t) {
            return new CraftingPlanResult(false, null, missingItems);
        }
    }

    private static final CraftingInput DUMMY_INPUT = CraftingInput.of(3, 3, java.util.Collections.nCopies(9, ItemStack.EMPTY));

    private ItemStack getSafeRecipeResult(CraftingRecipe recipe, ServerLevel world) {
        try {
            ItemStack res = recipe.assemble(DUMMY_INPUT);
            if (!res.isEmpty()) return res;
        } catch (Throwable ignored) {}
        try {
            return recipe.assemble(CraftingInput.EMPTY);
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }

    private boolean resolveItemRequirement(ServerLevel world, net.minecraft.world.item.Item targetItem,
                                           java.util.Map<net.minecraft.world.item.Item, Integer> available,
                                           java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> availableMolten,
                                           java.util.Map<net.minecraft.world.item.Item, Integer> virtualBuffer,
                                           CraftingPlan plan,
                                           java.util.Map<String, Integer> missingItems,
                                           java.util.Set<net.minecraft.world.item.Item> activeRecursion,
                                           int depth) {
        // 1. If item already exists in intermediate virtual buffer, consume 1
        int vCount = virtualBuffer.getOrDefault(targetItem, 0);
        if (vCount > 0) {
            virtualBuffer.put(targetItem, vCount - 1);
            return true;
        }

        // 2. If item exists in available storage / inventory, consume 1
        int count = available.getOrDefault(targetItem, 0);
        if (count > 0) {
            available.put(targetItem, count - 1);
            plan.rawIngredientsToConsume.add(new ItemStack(targetItem, 1));
            return true;
        }

        // 3. Check if targetItem can be cast on-demand via an online Casting Port (Molder)
        if (isCasterOnline()) {
            MetalCastInfo castInfo = getMetalCastInfo(targetItem);
            if (castInfo != null) {
                int fluidAvail = availableMolten.getOrDefault(castInfo.metal, 0);
                if (fluidAvail >= castInfo.costMb) {
                    availableMolten.put(castInfo.metal, fluidAvail - castInfo.costMb);
                    plan.moltenMetalsToConsume.put(castInfo.metal, plan.moltenMetalsToConsume.getOrDefault(castInfo.metal, 0) + castInfo.costMb);
                    plan.moltenMetalUsedMb += castInfo.costMb;
                    plan.steps.add(new CraftStep(StepType.CAST, castInfo.metal, targetItem, castInfo.costMb));
                    return true;
                }
            }
        }

        // 3b. Check if targetItem is a Water Bucket and can be filled via an online Water Pump or nearby water
        if (targetItem == Items.WATER_BUCKET || targetItem == ModItems.COPPER_WATER_BUCKET) {
            if (isWaterPumpOnline()) {
                net.minecraft.world.item.Item emptyBucket = (targetItem == ModItems.COPPER_WATER_BUCKET) ? ModItems.COPPER_BUCKET : Items.BUCKET;
                if (resolveItemRequirement(world, emptyBucket, available, availableMolten, virtualBuffer, plan, missingItems, activeRecursion, depth + 1)) {
                    plan.totalCraftingSteps++;
                    plan.steps.add(new CraftStep(StepType.PUMP_WATER, emptyBucket, targetItem));
                    return true;
                }
            } else {
                missingItems.put(targetItem.getName(targetItem.getDefaultInstance()).getString() + " §c(Place Water Pump or Water nearby)", 1);
                return false;
            }
            missingItems.put(targetItem.getName(targetItem.getDefaultInstance()).getString() + " §c(Requires empty bucket)", 1);
            return false;
        }

        // 3c. Check if targetItem is a Lava Bucket and can be pumped or melted via an online Lava Pump / Magma Crucible
        if ((targetItem == Items.LAVA_BUCKET || targetItem == ModItems.COPPER_LAVA_BUCKET) && isLavaSourceOnline()) {
            net.minecraft.world.item.Item emptyBucket = (targetItem == ModItems.COPPER_LAVA_BUCKET) ? ModItems.COPPER_BUCKET : Items.BUCKET;
            if (isLavaPumpOnline()) {
                LavaPumpBlockEntity pump = getBestAvailableLavaPump();
                if (pump != null && pump.getLavaAmount() >= 1000) {
                    if (resolveItemRequirement(world, emptyBucket, available, availableMolten, virtualBuffer, plan, missingItems, activeRecursion, depth + 1)) {
                        plan.totalCraftingSteps++;
                        plan.steps.add(new CraftStep(StepType.PUMP_LAVA, emptyBucket, targetItem));
                        return true;
                    }
                }
            } else if (isCrucibleOnline()) {
                MagmaCrucibleBlockEntity bestCrucible = getBestAvailableCrucible();
                if (bestCrucible != null && bestCrucible.getLavaAmount() >= 1000) {
                    if (resolveItemRequirement(world, emptyBucket, available, availableMolten, virtualBuffer, plan, missingItems, activeRecursion, depth + 1)) {
                        plan.totalCraftingSteps++;
                        plan.steps.add(new CraftStep(StepType.MELT_LAVA, emptyBucket, targetItem));
                        return true;
                    }
                } else {
                    // Try to resolve rock melting materials + empty bucket
                    net.minecraft.world.item.Item[] meltCandidates = new net.minecraft.world.item.Item[]{
                        ModItems.FIRE_CRYSTAL, Items.MAGMA_BLOCK, Items.BASALT, Items.BLACKSTONE,
                        Items.COBBLESTONE, Items.STONE, Items.NETHERRACK
                    };
                    int[] meltCounts = new int[]{ 1, 2, 4, 4, 10, 10, 10 };
                    for (int m = 0; m < meltCandidates.length; m++) {
                        net.minecraft.world.item.Item rock = meltCandidates[m];
                        int neededCount = meltCounts[m];

                        java.util.Map<net.minecraft.world.item.Item, Integer> backupAvailable = new java.util.HashMap<>(available);
                        java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupMolten = new java.util.EnumMap<>(availableMolten);
                        java.util.Map<net.minecraft.world.item.Item, Integer> backupVirtual = new java.util.HashMap<>(virtualBuffer);
                        List<ItemStack> backupPlan = new ArrayList<>(plan.rawIngredientsToConsume);
                        java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupPlanMolten = new java.util.EnumMap<>(plan.moltenMetalsToConsume);
                        List<CraftStep> backupStepsList = new ArrayList<>(plan.steps);
                        int backupSteps = plan.totalCraftingSteps;

                        boolean rockOk = true;
                        for (int k = 0; k < neededCount; k++) {
                            if (!resolveItemRequirement(world, rock, available, availableMolten, virtualBuffer, plan, new java.util.LinkedHashMap<>(), activeRecursion, depth + 1)) {
                                rockOk = false;
                                break;
                            }
                        }

                        if (rockOk && resolveItemRequirement(world, emptyBucket, available, availableMolten, virtualBuffer, plan, new java.util.LinkedHashMap<>(), activeRecursion, depth + 1)) {
                            plan.totalCraftingSteps++;
                            plan.steps.add(new CraftStep(StepType.MELT_LAVA, rock, targetItem));
                            return true;
                        }

                        // Rollback
                        available.clear(); available.putAll(backupAvailable);
                        availableMolten.clear(); availableMolten.putAll(backupMolten);
                        virtualBuffer.clear(); virtualBuffer.putAll(backupVirtual);
                        plan.rawIngredientsToConsume.clear(); plan.rawIngredientsToConsume.addAll(backupPlan);
                        plan.moltenMetalsToConsume.clear(); plan.moltenMetalsToConsume.putAll(backupPlanMolten);
                        plan.steps.clear(); plan.steps.addAll(backupStepsList);
                        plan.totalCraftingSteps = backupSteps;
                    }
                }
            }
            missingItems.put(targetItem.getName(targetItem.getDefaultInstance()).getString() + " §c(Requires empty bucket or lava source)", 1);
            return false;
        }

        // 4. Prevent infinite loops or deep recursion
        if (depth >= 8 || activeRecursion.contains(targetItem)) {
            missingItems.put(targetItem.getName(targetItem.getDefaultInstance()).getString(), missingItems.getOrDefault(targetItem.getName(targetItem.getDefaultInstance()).getString(), 0) + 1);
            return false;
        }

        activeRecursion.add(targetItem);
        java.util.Map<String, Integer> bestCandidateMissing = null;
        try {
            // 5. Check if targetItem can be pressed via an online Hydraulic Press
            if (isPressOnline()) {
                PressRecipeInfo pressInfo = getPressRecipeInfo(targetItem);
                if (pressInfo != null) {
                    java.util.Map<net.minecraft.world.item.Item, Integer> backupAvailable = new java.util.HashMap<>(available);
                    java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupMolten = new java.util.EnumMap<>(availableMolten);
                    java.util.Map<net.minecraft.world.item.Item, Integer> backupVirtual = new java.util.HashMap<>(virtualBuffer);
                    List<ItemStack> backupPlan = new ArrayList<>(plan.rawIngredientsToConsume);
                    java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupPlanMolten = new java.util.EnumMap<>(plan.moltenMetalsToConsume);
                    List<CraftStep> backupStepsList = new ArrayList<>(plan.steps);
                    int backupSteps = plan.totalCraftingSteps;
                    int backupMoltenUsed = plan.moltenMetalUsedMb;
                    int backupPressings = plan.hydraulicPressings;

                    java.util.Map<String, Integer> pressMissing = new java.util.LinkedHashMap<>();
                    if (resolveItemRequirement(world, pressInfo.input(), available, availableMolten, virtualBuffer, plan, pressMissing, activeRecursion, depth + 1)) {
                        plan.totalCraftingSteps++;
                        plan.hydraulicPressings++;
                        plan.steps.add(new CraftStep(StepType.PRESS, pressInfo.input(), targetItem));
                        if (pressInfo.yield() > 1) {
                            virtualBuffer.put(targetItem, virtualBuffer.getOrDefault(targetItem, 0) + (pressInfo.yield() - 1));
                        }
                        return true;
                    } else {
                        if (!pressMissing.isEmpty() && (bestCandidateMissing == null || pressMissing.size() < bestCandidateMissing.size())) {
                            bestCandidateMissing = pressMissing;
                        }
                        available.clear(); available.putAll(backupAvailable);
                        availableMolten.clear(); availableMolten.putAll(backupMolten);
                        virtualBuffer.clear(); virtualBuffer.putAll(backupVirtual);
                        plan.rawIngredientsToConsume.clear(); plan.rawIngredientsToConsume.addAll(backupPlan);
                        plan.moltenMetalsToConsume.clear(); plan.moltenMetalsToConsume.putAll(backupPlanMolten);
                        plan.steps.clear(); plan.steps.addAll(backupStepsList);
                        plan.totalCraftingSteps = backupSteps;
                        plan.moltenMetalUsedMb = backupMoltenUsed;
                        plan.hydraulicPressings = backupPressings;
                    }
                }
            }

            // 6. Check if targetItem can be fabricated via an online Circuit Fabricator
            if (isCircuitFabricatorOnline()) {
                for (CircuitFabricatorBlockEntity.FabricatorRecipe fabRecipe : CircuitFabricatorBlockEntity.getRecipes()) {
                    if (fabRecipe.output().is(targetItem)) {
                        java.util.Map<net.minecraft.world.item.Item, Integer> backupAvailable = new java.util.HashMap<>(available);
                        java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupMolten = new java.util.EnumMap<>(availableMolten);
                        java.util.Map<net.minecraft.world.item.Item, Integer> backupVirtual = new java.util.HashMap<>(virtualBuffer);
                        List<ItemStack> backupPlan = new ArrayList<>(plan.rawIngredientsToConsume);
                        java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupPlanMolten = new java.util.EnumMap<>(plan.moltenMetalsToConsume);
                        List<CraftStep> backupStepsList = new ArrayList<>(plan.steps);
                        int backupSteps = plan.totalCraftingSteps;
                        int backupMoltenUsed = plan.moltenMetalUsedMb;
                        int backupFabs = plan.circuitFabrications;

                        java.util.Map<String, Integer> fabMissing = new java.util.LinkedHashMap<>();
                        boolean success = resolveItemRequirement(world, fabRecipe.substrate(), available, availableMolten, virtualBuffer, plan, fabMissing, activeRecursion, depth + 1);
                        for (net.minecraft.world.item.Item comp : fabRecipe.components()) {
                            if (!resolveItemRequirement(world, comp, available, availableMolten, virtualBuffer, plan, fabMissing, activeRecursion, depth + 1)) {
                                success = false;
                            }
                        }

                        if (success) {
                            plan.totalCraftingSteps++;
                            plan.circuitFabrications++;
                            plan.steps.add(new CraftStep(StepType.FABRICATE, fabRecipe.substrate(), targetItem));
                            if (fabRecipe.output().getCount() > 1) {
                                virtualBuffer.put(targetItem, virtualBuffer.getOrDefault(targetItem, 0) + (fabRecipe.output().getCount() - 1));
                            }
                            return true;
                        } else {
                            if (!fabMissing.isEmpty() && (bestCandidateMissing == null || fabMissing.size() < bestCandidateMissing.size())) {
                                bestCandidateMissing = fabMissing;
                            }
                            available.clear(); available.putAll(backupAvailable);
                            availableMolten.clear(); availableMolten.putAll(backupMolten);
                            virtualBuffer.clear(); virtualBuffer.putAll(backupVirtual);
                            plan.rawIngredientsToConsume.clear(); plan.rawIngredientsToConsume.addAll(backupPlan);
                            plan.moltenMetalsToConsume.clear(); plan.moltenMetalsToConsume.putAll(backupPlanMolten);
                            plan.steps.clear(); plan.steps.addAll(backupStepsList);
                            plan.totalCraftingSteps = backupSteps;
                            plan.moltenMetalUsedMb = backupMoltenUsed;
                            plan.circuitFabrications = backupFabs;
                        }
                    }
                }
            }

            // 7. Search RecipeManager for a crafting recipe that produces targetItem from available materials
            for (RecipeHolder<?> entry : world.recipeAccess().getRecipes()) {
                if (!(entry.value() instanceof CraftingRecipe craftingRecipe)) continue;
                ItemStack result = getSafeRecipeResult(craftingRecipe, world);
                if (!result.isEmpty() && result.is(targetItem)) {
                    int yield = Math.max(1, result.getCount());
                    java.util.Map<net.minecraft.world.item.Item, Integer> backupAvailable = new java.util.HashMap<>(available);
                    java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupMolten = new java.util.EnumMap<>(availableMolten);
                    java.util.Map<net.minecraft.world.item.Item, Integer> backupVirtual = new java.util.HashMap<>(virtualBuffer);
                    List<ItemStack> backupPlan = new ArrayList<>(plan.rawIngredientsToConsume);
                    java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupPlanMolten = new java.util.EnumMap<>(plan.moltenMetalsToConsume);
                    List<CraftStep> backupStepsList = new ArrayList<>(plan.steps);
                    int backupSteps = plan.totalCraftingSteps;
                    int backupMoltenUsed = plan.moltenMetalUsedMb;

                    List<net.minecraft.world.item.crafting.Ingredient> ings;
                    try {
                        ings = craftingRecipe.placementInfo().ingredients();
                    } catch (Throwable t) {
                        continue;
                    }

                    if (ings.isEmpty()) continue;

                    java.util.Map<String, Integer> craftCandidateMissing = new java.util.LinkedHashMap<>();
                    boolean success = true;
                    for (net.minecraft.world.item.crafting.Ingredient ing : ings) {
                        if (ing == null || ing.isEmpty()) continue;

                        if (!resolveIngredientRequirement(world, ing, available, availableMolten, virtualBuffer, plan, craftCandidateMissing, activeRecursion, depth + 1)) {
                            success = false;
                        }
                    }

                    if (success) {
                        plan.totalCraftingSteps++;
                        plan.steps.add(new CraftStep(StepType.ASSEMBLE, null, targetItem));
                        if (yield > 1) {
                            virtualBuffer.put(targetItem, virtualBuffer.getOrDefault(targetItem, 0) + (yield - 1));
                        }
                        return true;
                    } else {
                        if (!craftCandidateMissing.isEmpty() && (bestCandidateMissing == null || craftCandidateMissing.size() < bestCandidateMissing.size())) {
                            bestCandidateMissing = craftCandidateMissing;
                        }
                        // Rollback and try the next recipe candidate
                        available.clear();
                        available.putAll(backupAvailable);
                        availableMolten.clear();
                        availableMolten.putAll(backupMolten);
                        virtualBuffer.clear();
                        virtualBuffer.putAll(backupVirtual);
                        plan.rawIngredientsToConsume.clear();
                        plan.rawIngredientsToConsume.addAll(backupPlan);
                        plan.moltenMetalsToConsume.clear();
                        plan.moltenMetalsToConsume.putAll(backupPlanMolten);
                        plan.steps.clear();
                        plan.steps.addAll(backupStepsList);
                        plan.totalCraftingSteps = backupSteps;
                        plan.moltenMetalUsedMb = backupMoltenUsed;
                    }
                }
            }

            // 8. Check if targetItem can be smelted via an online Enchanted Furnace / Smelter
            if (isFurnaceOnline()) {
                // A. Check custom dust smelting (e.g. Iron Ingot from Iron Dust, etc.)
                for (net.minecraft.world.item.Item dustCandidate : getDustSmeltingInputs(targetItem)) {
                    java.util.Map<net.minecraft.world.item.Item, Integer> backupAvailable = new java.util.HashMap<>(available);
                    java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupMolten = new java.util.EnumMap<>(availableMolten);
                    java.util.Map<net.minecraft.world.item.Item, Integer> backupVirtual = new java.util.HashMap<>(virtualBuffer);
                    List<ItemStack> backupPlan = new ArrayList<>(plan.rawIngredientsToConsume);
                    java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupPlanMolten = new java.util.EnumMap<>(plan.moltenMetalsToConsume);
                    List<CraftStep> backupStepsList = new ArrayList<>(plan.steps);
                    int backupSteps = plan.totalCraftingSteps;
                    int backupMoltenUsed = plan.moltenMetalUsedMb;
                    int backupSmelts = plan.smeltingSteps;

                    java.util.Map<String, Integer> dustMissing = new java.util.LinkedHashMap<>();
                    if (resolveItemRequirement(world, dustCandidate, available, availableMolten, virtualBuffer, plan, dustMissing, activeRecursion, depth + 1)) {
                        plan.totalCraftingSteps++;
                        plan.smeltingSteps++;
                        plan.steps.add(new CraftStep(StepType.SMELT, dustCandidate, targetItem));
                        return true;
                    } else {
                        if (!dustMissing.isEmpty() && (bestCandidateMissing == null || dustMissing.size() < bestCandidateMissing.size())) {
                            bestCandidateMissing = dustMissing;
                        }
                        available.clear(); available.putAll(backupAvailable);
                        availableMolten.clear(); availableMolten.putAll(backupMolten);
                        virtualBuffer.clear(); virtualBuffer.putAll(backupVirtual);
                        plan.rawIngredientsToConsume.clear(); plan.rawIngredientsToConsume.addAll(backupPlan);
                        plan.moltenMetalsToConsume.clear(); plan.moltenMetalsToConsume.putAll(backupPlanMolten);
                        plan.steps.clear(); plan.steps.addAll(backupStepsList);
                        plan.totalCraftingSteps = backupSteps;
                        plan.moltenMetalUsedMb = backupMoltenUsed;
                        plan.smeltingSteps = backupSmelts;
                    }
                }

                // B. Check standard smelting recipes (e.g. Glass from Sand, Stone from Cobblestone, Smooth Stone from Stone, Charcoal from Log, etc.)
                for (RecipeHolder<?> entry : world.recipeAccess().getRecipes()) {
                    if (!(entry.value() instanceof AbstractCookingRecipe cookingRecipe)) continue;
                    if (cookingRecipe.getType() != RecipeType.SMELTING && cookingRecipe.getType() != RecipeType.BLASTING) continue;
                    if (isEquipmentRecycleRecipe(cookingRecipe, world)) continue;

                    ItemStack smeltRes = ItemStack.EMPTY;
                    try {
                        smeltRes = cookingRecipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
                    } catch (Throwable ignored) {}

                    if (!smeltRes.isEmpty() && smeltRes.is(targetItem)) {
                        net.minecraft.world.item.crafting.Ingredient ing = cookingRecipe.input();
                        if (ing == null || ing.isEmpty()) continue;

                        java.util.Map<net.minecraft.world.item.Item, Integer> backupAvailable = new java.util.HashMap<>(available);
                        java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupMolten = new java.util.EnumMap<>(availableMolten);
                        java.util.Map<net.minecraft.world.item.Item, Integer> backupVirtual = new java.util.HashMap<>(virtualBuffer);
                        List<ItemStack> backupPlan = new ArrayList<>(plan.rawIngredientsToConsume);
                        java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupPlanMolten = new java.util.EnumMap<>(plan.moltenMetalsToConsume);
                        List<CraftStep> backupStepsList = new ArrayList<>(plan.steps);
                        int backupSteps = plan.totalCraftingSteps;
                        int backupMoltenUsed = plan.moltenMetalUsedMb;
                        int backupSmelts = plan.smeltingSteps;

                        java.util.Map<String, Integer> smeltMissing = new java.util.LinkedHashMap<>();
                        if (resolveIngredientRequirement(world, ing, available, availableMolten, virtualBuffer, plan, smeltMissing, activeRecursion, depth + 1)) {
                            plan.totalCraftingSteps++;
                            plan.smeltingSteps++;
                            int yield = Math.max(1, smeltRes.getCount());
                            net.minecraft.world.item.Item resolvedInput = null;
                            for (net.minecraft.world.item.Item opt : ing.items().map(net.minecraft.core.Holder::value).toList()) {
                                if (available.containsKey(opt) || virtualBuffer.containsKey(opt)) {
                                    resolvedInput = opt;
                                    break;
                                }
                            }
                            if (resolvedInput == null) {
                                resolvedInput = ing.items().findFirst().map(net.minecraft.core.Holder::value).orElse(null);
                            }
                            plan.steps.add(new CraftStep(StepType.SMELT, resolvedInput, targetItem));
                            if (yield > 1) {
                                virtualBuffer.put(targetItem, virtualBuffer.getOrDefault(targetItem, 0) + (yield - 1));
                            }
                            return true;
                        } else {
                            if (!smeltMissing.isEmpty() && (bestCandidateMissing == null || smeltMissing.size() < bestCandidateMissing.size())) {
                                bestCandidateMissing = smeltMissing;
                            }
                            available.clear(); available.putAll(backupAvailable);
                            availableMolten.clear(); availableMolten.putAll(backupMolten);
                            virtualBuffer.clear(); virtualBuffer.putAll(backupVirtual);
                            plan.rawIngredientsToConsume.clear(); plan.rawIngredientsToConsume.addAll(backupPlan);
                            plan.moltenMetalsToConsume.clear(); plan.moltenMetalsToConsume.putAll(backupPlanMolten);
                            plan.steps.clear(); plan.steps.addAll(backupStepsList);
                            plan.totalCraftingSteps = backupSteps;
                            plan.moltenMetalUsedMb = backupMoltenUsed;
                            plan.smeltingSteps = backupSmelts;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            missingItems.put(targetItem.getName(targetItem.getDefaultInstance()).getString(), missingItems.getOrDefault(targetItem.getName(targetItem.getDefaultInstance()).getString(), 0) + 1);
            return false;
        } finally {
            activeRecursion.remove(targetItem);
        }

        if (bestCandidateMissing != null && !bestCandidateMissing.isEmpty()) {
            for (java.util.Map.Entry<String, Integer> e : bestCandidateMissing.entrySet()) {
                missingItems.put(e.getKey(), missingItems.getOrDefault(e.getKey(), 0) + e.getValue());
            }
        } else {
            String name = targetItem.getName(targetItem.getDefaultInstance()).getString();
            missingItems.put(name, missingItems.getOrDefault(name, 0) + 1);
        }

        return false;
    }

    private boolean resolveIngredientRequirement(ServerLevel world, net.minecraft.world.item.crafting.Ingredient ing,
                                                 java.util.Map<net.minecraft.world.item.Item, Integer> available,
                                                 java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> availableMolten,
                                                 java.util.Map<net.minecraft.world.item.Item, Integer> virtualBuffer,
                                                 CraftingPlan plan,
                                                 java.util.Map<String, Integer> missingItems,
                                                 java.util.Set<net.minecraft.world.item.Item> activeRecursion,
                                                 int depth) {
        List<net.minecraft.world.item.Item> matchingItems;
        try {
            matchingItems = ing.items().map(net.minecraft.core.Holder::value).toList();
        } catch (Throwable t) {
            matchingItems = java.util.Collections.emptyList();
        }

        if (matchingItems.isEmpty()) {
            return false;
        }

        // Priority 1: Check if any matching item was already synthesized in virtualBuffer
        for (net.minecraft.world.item.Item opt : matchingItems) {
            int vCount = virtualBuffer.getOrDefault(opt, 0);
            if (vCount > 0) {
                virtualBuffer.put(opt, vCount - 1);
                return true;
            }
        }

        // Priority 2: Check if any matching item is physically in available storage / inventory
        for (net.minecraft.world.item.Item opt : matchingItems) {
            int pCount = available.getOrDefault(opt, 0);
            if (pCount > 0) {
                available.put(opt, pCount - 1);
                plan.rawIngredientsToConsume.add(new ItemStack(opt, 1));
                return true;
            }
        }

        // Priority 3: Check if any matching item can be cast directly from available molten metals via an online Casting Port (Molder)
        if (isCasterOnline()) {
            for (net.minecraft.world.item.Item opt : matchingItems) {
                MetalCastInfo castInfo = getMetalCastInfo(opt);
                if (castInfo != null) {
                    int fluidAvail = availableMolten.getOrDefault(castInfo.metal, 0);
                    if (fluidAvail >= castInfo.costMb) {
                        availableMolten.put(castInfo.metal, fluidAvail - castInfo.costMb);
                        plan.moltenMetalsToConsume.put(castInfo.metal, plan.moltenMetalsToConsume.getOrDefault(castInfo.metal, 0) + castInfo.costMb);
                        plan.moltenMetalUsedMb += castInfo.costMb;
                        plan.steps.add(new CraftStep(StepType.CAST, castInfo.metal, opt, castInfo.costMb));
                        return true;
                    }
                }
            }
        }

        // Priority 4: Try to synthesize one of the matching items recursively from available materials
        java.util.Map<String, Integer> bestSubMissing = null;
        for (net.minecraft.world.item.Item opt : matchingItems) {
            java.util.Map<String, Integer> candidateMissing = new java.util.LinkedHashMap<>();
            if (resolveItemRequirement(world, opt, available, availableMolten, virtualBuffer, plan, candidateMissing, activeRecursion, depth)) {
                return true;
            }
            if (!candidateMissing.isEmpty() && (bestSubMissing == null || candidateMissing.size() < bestSubMissing.size())) {
                bestSubMissing = candidateMissing;
            }
        }

        if (bestSubMissing != null && !bestSubMissing.isEmpty()) {
            for (java.util.Map.Entry<String, Integer> e : bestSubMissing.entrySet()) {
                missingItems.put(e.getKey(), missingItems.getOrDefault(e.getKey(), 0) + e.getValue());
            }
            return false;
        }

        // If not found and not synthesizable, record friendly group missing name
        String displayName = getIngredientDisplayName(ing);
        if (!isFurnaceOnline() && canBeSmelted(world, matchingItems)) {
            displayName += " §c(Furnace Offline)";
        } else if (!isPressOnline() && canBePressed(matchingItems)) {
            displayName += " §c(Press Offline)";
        } else if (!isCircuitFabricatorOnline() && canBeFabricated(matchingItems)) {
            displayName += " §c(Fabricator Offline)";
        } else if (!isWaterPumpOnline() && matchingItems.stream().anyMatch(i -> i == Items.WATER_BUCKET || i == ModItems.COPPER_WATER_BUCKET)) {
            displayName += " §c(Water Pump Offline)";
        } else if (!isLavaSourceOnline() && matchingItems.stream().anyMatch(i -> i == Items.LAVA_BUCKET || i == ModItems.COPPER_LAVA_BUCKET)) {
            displayName += " §c(Lava Pump / Crucible Offline)";
        }
        missingItems.put(displayName, missingItems.getOrDefault(displayName, 0) + 1);
        return false;
    }

    private static boolean isEquipmentRecycleRecipe(AbstractCookingRecipe cookingRecipe, ServerLevel world) {
        try {
            ItemStack res = cookingRecipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
            if (!res.isEmpty() && (res.is(Items.IRON_NUGGET) || res.is(Items.GOLD_NUGGET) || res.getItem().getDescriptionId().endsWith("_nugget"))) {
                return true;
            }
        } catch (Throwable ignored) {}

        net.minecraft.world.item.crafting.Ingredient ing = cookingRecipe.input();
        if (ing == null || ing.isEmpty()) return false;
        try {
            for (net.minecraft.world.item.Item item : ing.items().map(net.minecraft.core.Holder::value).toList()) {
                ItemStack stack = item.getDefaultInstance();
                if (stack.isDamageableItem() ||
                    stack.has(net.minecraft.core.component.DataComponents.MAX_DAMAGE) ||
                    stack.has(net.minecraft.core.component.DataComponents.TOOL) ||
                    stack.has(net.minecraft.core.component.DataComponents.EQUIPPABLE) ||
                    item == Items.IRON_HORSE_ARMOR ||
                    item == Items.GOLDEN_HORSE_ARMOR) {
                    return true;
                }
            }
        } catch (Throwable t) {
            return false;
        }
        return false;
    }

    private boolean canBeSmelted(ServerLevel world, List<net.minecraft.world.item.Item> items) {
        for (net.minecraft.world.item.Item item : items) {
            if (EnchantedFurnaceBlockEntity.getDustSmeltingResult(item) != null) return true;
            for (RecipeHolder<?> entry : world.recipeAccess().getRecipes()) {
                if (entry.value() instanceof AbstractCookingRecipe c && (c.getType() == RecipeType.SMELTING || c.getType() == RecipeType.BLASTING)) {
                    if (isEquipmentRecycleRecipe(c, world)) continue;
                    ItemStack res = c.assemble(new SingleRecipeInput(ItemStack.EMPTY));
                    if (!res.isEmpty() && res.is(item)) return true;
                }
            }
        }
        return false;
    }

    private boolean canBePressed(List<net.minecraft.world.item.Item> items) {
        for (net.minecraft.world.item.Item item : items) {
            if (getPressRecipeInfo(item) != null) return true;
        }
        return false;
    }

    private boolean canBeFabricated(List<net.minecraft.world.item.Item> items) {
        for (net.minecraft.world.item.Item item : items) {
            for (CircuitFabricatorBlockEntity.FabricatorRecipe recipe : CircuitFabricatorBlockEntity.getRecipes()) {
                if (recipe.output().is(item)) return true;
            }
        }
        return false;
    }

    private void consumeIngredients(@Nullable EnchantedStorageTerminalBlockEntity terminal, @Nullable Player player, List<ItemStack> required) {
        for (ItemStack req : required) {
            if (req.isEmpty()) continue;
            int needed = req.getCount();

            // 1. Deduct from Digital Storage Terminal if online
            if (terminal != null && terminal.isNetworkOnline()) {
                ItemStack extracted = terminal.extractItem(req, needed);
                if (!extracted.isEmpty()) {
                    needed -= extracted.getCount();
                }
            }

            // 2. Deduct from Player Inventory if still needed
            if (needed > 0 && player != null) {
                Inventory pInv = player.getInventory();
                for (int i = 0; i < 36; i++) {
                    ItemStack pStack = pInv.getItem(i);
                    if (!pStack.isEmpty() && ItemStack.isSameItemSameComponents(pStack, req)) {
                        int take = Math.min(needed, pStack.getCount());
                        pStack.shrink(take);
                        needed -= take;
                        if (pStack.isEmpty()) {
                            pInv.setItem(i, ItemStack.EMPTY);
                        }
                        pInv.setChanged();
                        if (needed <= 0) break;
                    }
                }
            }

            // 3. Deduct from Water Pumps (BUCKET_OUT_SLOT first, then BUCKET_IN_SLOT)
            if (needed > 0) {
                for (WaterPumpBlockEntity wp : this.cachedWaterPumps) {
                    if (wp != null && !wp.isRemoved()) {
                        for (int slot : new int[]{WaterPumpBlockEntity.BUCKET_OUT_SLOT, WaterPumpBlockEntity.BUCKET_IN_SLOT}) {
                            ItemStack wpStack = wp.getItem(slot);
                            if (!wpStack.isEmpty() && ItemStack.isSameItemSameComponents(wpStack, req)) {
                                int take = Math.min(needed, wpStack.getCount());
                                wpStack.shrink(take);
                                needed -= take;
                                if (wpStack.isEmpty()) {
                                    wp.setItem(slot, ItemStack.EMPTY);
                                }
                                wp.setChanged();
                                if (needed <= 0) break;
                            }
                        }
                        if (needed <= 0) break;
                    }
                }
            }

            // 4. Deduct from Lava Pumps (BUCKET_OUT_SLOT first, then BUCKET_IN_SLOT)
            if (needed > 0) {
                for (LavaPumpBlockEntity lp : this.cachedLavaPumps) {
                    if (lp != null && !lp.isRemoved()) {
                        for (int slot : new int[]{LavaPumpBlockEntity.BUCKET_OUT_SLOT, LavaPumpBlockEntity.BUCKET_IN_SLOT}) {
                            ItemStack lpStack = lp.getItem(slot);
                            if (!lpStack.isEmpty() && ItemStack.isSameItemSameComponents(lpStack, req)) {
                                int take = Math.min(needed, lpStack.getCount());
                                lpStack.shrink(take);
                                needed -= take;
                                if (lpStack.isEmpty()) {
                                    lp.setItem(slot, ItemStack.EMPTY);
                                }
                                lp.setChanged();
                                if (needed <= 0) break;
                            }
                        }
                        if (needed <= 0) break;
                    }
                }
            }

            // 5. Deduct from Magma Crucibles (BUCKET_OUTPUT_SLOT first, then BUCKET_INPUT_SLOT)
            if (needed > 0) {
                for (MagmaCrucibleBlockEntity mc : this.cachedCrucibles) {
                    if (mc != null && !mc.isRemoved()) {
                        for (int slot : new int[]{MagmaCrucibleBlockEntity.BUCKET_OUTPUT_SLOT, MagmaCrucibleBlockEntity.BUCKET_INPUT_SLOT}) {
                            ItemStack mcStack = mc.getItem(slot);
                            if (!mcStack.isEmpty() && ItemStack.isSameItemSameComponents(mcStack, req)) {
                                int take = Math.min(needed, mcStack.getCount());
                                mcStack.shrink(take);
                                needed -= take;
                                if (mcStack.isEmpty()) {
                                    mc.setItem(slot, ItemStack.EMPTY);
                                }
                                mc.setChanged();
                                if (needed <= 0) break;
                            }
                        }
                        if (needed <= 0) break;
                    }
                }
            }

            // 6. Deduct from adjacent inventories if still needed
            if (needed > 0 && this.level != null) {
                for (Direction dir : Direction.values()) {
                    BlockEntity be = this.level.getBlockEntity(this.worldPosition.relative(dir));
                    if (be instanceof Container adjInv && !(be instanceof SuperComputerBlockEntity) && !(be instanceof EnchantedStorageTerminalBlockEntity)) {
                        for (int s = 0; s < adjInv.getContainerSize(); s++) {
                            ItemStack pStack = adjInv.getItem(s);
                            if (!pStack.isEmpty() && ItemStack.isSameItemSameComponents(pStack, req)) {
                                int take = Math.min(needed, pStack.getCount());
                                pStack.shrink(take);
                                needed -= take;
                                if (pStack.isEmpty()) {
                                    adjInv.setItem(s, ItemStack.EMPTY);
                                }
                                adjInv.setChanged();
                                if (needed <= 0) break;
                            }
                        }
                        if (needed <= 0) break;
                    }
                }
            }

            // 7. Deduct from scanned room containers (cachedContainers)
            if (needed > 0) {
                for (Container container : this.cachedContainers) {
                    if (container != null && !(container instanceof SuperComputerBlockEntity) && !(container instanceof EnchantedStorageTerminalBlockEntity)) {
                        for (int s = 0; s < container.getContainerSize(); s++) {
                            ItemStack pStack = container.getItem(s);
                            if (!pStack.isEmpty() && ItemStack.isSameItemSameComponents(pStack, req)) {
                                int take = Math.min(needed, pStack.getCount());
                                pStack.shrink(take);
                                needed -= take;
                                if (pStack.isEmpty()) {
                                    container.setItem(s, ItemStack.EMPTY);
                                }
                                container.setChanged();
                                if (needed <= 0) break;
                            }
                        }
                        if (needed <= 0) break;
                    }
                }
            }
        }
    }

    private boolean canAcceptOutput(ItemStack result) {
        // 1. Check if 2x2 output buffer (slots 10..13) has space
        for (int i = 0; i < OUTPUT_SIZE; i++) {
            ItemStack out = this.inventory.get(OUTPUT_START + i);
            if (out.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize()) {
                return true;
            }
        }
        // 2. Also check if digital terminal has space
        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        if (terminal != null && terminal.isNetworkOnline() && terminal.getStoredItemCount() + result.getCount() <= terminal.getNetworkCapacity()) {
            return true;
        }
        return false;
    }

    private void depositCraftedResult(@Nullable EnchantedStorageTerminalBlockEntity terminal, @Nullable Player player, ItemStack result) {
        // 1. Deposit into 2x2 Output Buffer (slots 10..13) first
        for (int i = 0; i < OUTPUT_SIZE; i++) {
            ItemStack out = this.inventory.get(OUTPUT_START + i);
            if (!out.isEmpty() && ItemStack.isSameItemSameComponents(out, result)) {
                int space = out.getMaxStackSize() - out.getCount();
                if (space > 0) {
                    int move = Math.min(space, result.getCount());
                    out.grow(move);
                    result.shrink(move);
                    this.setChanged();
                    if (result.isEmpty()) return;
                }
            }
        }
        for (int i = 0; i < OUTPUT_SIZE; i++) {
            ItemStack out = this.inventory.get(OUTPUT_START + i);
            if (out.isEmpty()) {
                this.inventory.set(OUTPUT_START + i, result.copy());
                result.setCount(0);
                this.setChanged();
                return;
            }
        }

        // 2. If output buffer is full, give to player
        if (player != null && !result.isEmpty()) {
            player.getInventory().placeItemBackInInventory(result.copy(), net.minecraft.util.Prediction.SERVER_ONLY);
            result.setCount(0);
            return;
        }

        // 3. If digital storage network is connected and has space, insert there
        if (terminal != null && terminal.isNetworkOnline()) {
            ItemStack remainder = terminal.depositItem(result.copy());
            if (remainder.getCount() != result.getCount()) {
                result.setCount(remainder.getCount());
                if (result.isEmpty()) return;
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.super_computer");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new SuperComputerScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.inventory) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.inventory, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(this.inventory, slot);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.inventory.clear();
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        // Only output buffer slots (10..13) are extractable
        return new int[]{10, 11, 12, 13};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return false; // Items are programmed in GUI, not piped in
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot >= OUTPUT_START && slot < OUTPUT_START + OUTPUT_SIZE;
    }

    @Override
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.craftProgress = view.getIntOr("CraftProgress", 0);
        this.energyStorage.setEnergy(view.getIntOr("Energy", 0));
        if (view.contains("BoundX") && view.contains("BoundY") && view.contains("BoundZ")) {
            this.boundNetworkPos = new BlockPos(view.getIntOr("BoundX", 0), view.getIntOr("BoundY", 0), view.getIntOr("BoundZ", 0));
            this.boundDimension = view.getStringOr("BoundDim", "minecraft:overworld");
        } else {
            this.boundNetworkPos = null;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        view.putInt("CraftProgress", this.craftProgress);
        view.putInt("Energy", this.energyStorage.getEnergy());
        if (this.boundNetworkPos != null) {
            view.putInt("BoundX", this.boundNetworkPos.getX());
            view.putInt("BoundY", this.boundNetworkPos.getY());
            view.putInt("BoundZ", this.boundNetworkPos.getZ());
            view.putString("BoundDim", this.boundDimension != null ? this.boundDimension : "minecraft:overworld");
        }
    }
}

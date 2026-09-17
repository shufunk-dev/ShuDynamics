package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.SuperComputerBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.SuperComputerScreenHandler;
import net.enchantedwood.util.ItemTransportHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SuperComputerBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
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

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 2_000, 2_000, 0);

    private int craftProgress = 0;
    private int maxCraftProgress = BASE_CRAFT_TIME;
    private boolean hasValidRecipe = false;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
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
        public int size() {
            return 12;
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
    private List<BlockEntity> cachedFurnaces = java.util.Collections.emptyList();
    private List<HydraulicPressBlockEntity> cachedPresses = java.util.Collections.emptyList();
    private List<CircuitFabricatorBlockEntity> cachedFabricators = java.util.Collections.emptyList();
    private List<CastingPortBlockEntity> cachedCasters = java.util.Collections.emptyList();
    private List<TitaniumTankControllerBlockEntity> cachedTankControllers = java.util.Collections.emptyList();
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
        markDirty();
    }

    public @Nullable BlockPos getBoundNetworkPos() {
        return this.boundNetworkPos;
    }

    public @Nullable BlockPos getEffectiveControllerPos() {
        if (this.boundNetworkPos != null) return this.boundNetworkPos;
        if (this.cachedPowerController != null && !this.cachedPowerController.isRemoved()) {
            return this.cachedPowerController.getPos();
        }
        if (this.cachedTerminal != null && !this.cachedTerminal.isRemoved()) {
            return this.cachedTerminal.getPos();
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
        if (this.world == null || this.world.isClient()) return;
        long currentTick = this.world.getTime();
        if (!force && (currentTick - this.lastScanTick < 40)) {
            return;
        }
        this.lastScanTick = currentTick;

        List<BlockEntity> foundFurnaces = new ArrayList<>();
        List<HydraulicPressBlockEntity> foundPresses = new ArrayList<>();
        List<CircuitFabricatorBlockEntity> foundFabricators = new ArrayList<>();
        List<CastingPortBlockEntity> foundCasters = new ArrayList<>();
        List<TitaniumTankControllerBlockEntity> foundTankControllers = new ArrayList<>();
        final EnchantedStorageTerminalBlockEntity[] foundTerminal = new EnchantedStorageTerminalBlockEntity[1];
        final EnchantedStorageControllerBlockEntity[] foundController = new EnchantedStorageControllerBlockEntity[1];

        java.util.Set<BlockPos> visited = new java.util.HashSet<>();

        // 1. Direct check of boundNetworkPos if wrench-linked
        if (this.boundNetworkPos != null && this.world.isChunkLoaded(this.boundNetworkPos.getX() >> 4, this.boundNetworkPos.getZ() >> 4)) {
            BlockEntity boundBe = this.world.getBlockEntity(this.boundNetworkPos);
            if (boundBe instanceof EnchantedStorageTerminalBlockEntity term) {
                foundTerminal[0] = term;
            } else if (boundBe instanceof EnchantedStorageControllerBlockEntity ctrl) {
                foundController[0] = ctrl;
            }
        }

        // 2. Scan around Super Computer (radius 24 X/Z, 8 Y)
        int minX = this.pos.getX() - 24;
        int maxX = this.pos.getX() + 24;
        int minY = Math.max(this.world.getBottomY(), this.pos.getY() - 8);
        int maxY = Math.min(this.world.getTopYInclusive(), this.pos.getY() + 8);
        int minZ = this.pos.getZ() - 24;
        int maxZ = this.pos.getZ() + 24;

        scanChunkArea(minX, maxX, minY, maxY, minZ, maxZ, visited,
                foundFurnaces, foundPresses, foundFabricators, foundCasters, foundTankControllers, foundTerminal, foundController);

        // 3. If bound to a controller or terminal located further away, scan around it too (radius 16 X/Z, 6 Y)
        BlockPos remotePos = foundController[0] != null ? foundController[0].getPos() : (foundTerminal[0] != null ? foundTerminal[0].getPos() : this.boundNetworkPos);
        if (remotePos != null && remotePos.getManhattanDistance(this.pos) > 20 && this.world.isChunkLoaded(remotePos.getX() >> 4, remotePos.getZ() >> 4)) {
            int cMinX = remotePos.getX() - 16;
            int cMaxX = remotePos.getX() + 16;
            int cMinY = Math.max(this.world.getBottomY(), remotePos.getY() - 6);
            int cMaxY = Math.min(this.world.getTopYInclusive(), remotePos.getY() + 6);
            int cMinZ = remotePos.getZ() - 16;
            int cMaxZ = remotePos.getZ() + 16;

            scanChunkArea(cMinX, cMaxX, cMinY, cMaxY, cMinZ, cMaxZ, visited,
                    foundFurnaces, foundPresses, foundFabricators, foundCasters, foundTankControllers, foundTerminal, foundController);
        }

        this.cachedFurnaces = foundFurnaces;
        this.cachedPresses = foundPresses;
        this.cachedFabricators = foundFabricators;
        this.cachedCasters = foundCasters;
        this.cachedTankControllers = foundTankControllers;
        this.cachedTerminal = foundTerminal[0];
        this.cachedPowerController = foundController[0];

        this.cachedFurnaceOnline = !foundFurnaces.isEmpty();
        this.cachedPressOnline = !foundPresses.isEmpty();
        this.cachedFabricatorOnline = !foundFabricators.isEmpty();
        this.cachedCasterOnline = !foundCasters.isEmpty();

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
                              List<BlockEntity> furnaces,
                              List<HydraulicPressBlockEntity> presses,
                              List<CircuitFabricatorBlockEntity> fabricators,
                              List<CastingPortBlockEntity> casters,
                              List<TitaniumTankControllerBlockEntity> tankControllers,
                              EnchantedStorageTerminalBlockEntity[] foundTerminal,
                              EnchantedStorageControllerBlockEntity[] foundController) {
        if (this.world == null) return;

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                if (!this.world.isChunkLoaded(cx, cz)) continue;
                net.minecraft.world.chunk.WorldChunk chunk = this.world.getWorldChunk(new BlockPos(cx << 4, 0, cz << 4));
                if (chunk == null) continue;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be == null || be.isRemoved()) continue;
                    BlockPos bp = be.getPos();
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
                    } else if (be instanceof EnchantedFurnaceBlockEntity
                            || be instanceof DustSmelterBlockEntity
                            || be instanceof DustSmelterMk2BlockEntity
                            || be instanceof net.minecraft.block.entity.AbstractFurnaceBlockEntity) {
                        furnaces.add(be);
                    } else if (be instanceof TitaniumTankControllerBlockEntity controller && controller.isFormed()) {
                        tankControllers.add(controller);
                    } else if (be instanceof TitaniumTankCasingBlockEntity casing) {
                        TitaniumTankControllerBlockEntity master = casing.getMaster();
                        if (master != null && master.isFormed() && visited.add(master.getPos())) {
                            tankControllers.add(master);
                        }
                    } else if (be instanceof EnchantedStorageTerminalBlockEntity term) {
                        if (foundTerminal[0] == null) foundTerminal[0] = term;
                    } else if (be instanceof EnchantedStorageControllerBlockEntity ctrl) {
                        if (foundController[0] == null) foundController[0] = ctrl;
                    }
                }
            }
        }
    }

    public static class MetalCastInfo {
        public final net.enchantedwood.fluid.MoltenMetal metal;
        public final int costMb;

        public MetalCastInfo(net.enchantedwood.fluid.MoltenMetal metal, int costMb) {
            this.metal = metal;
            this.costMb = costMb;
        }
    }

    public static @Nullable MetalCastInfo getMetalCastInfo(net.minecraft.item.Item item) {
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
        net.minecraft.item.Item firstItem = null;
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

    public List<BlockEntity> getNearbyFurnaces() {
        return this.cachedFurnaces;
    }

    public @Nullable HydraulicPressBlockEntity getBestAvailablePress() {
        HydraulicPressBlockEntity bestIdle = null;
        int bestSpeed = -1;
        for (HydraulicPressBlockEntity press : this.cachedPresses) {
            if (press != null && !press.isRemoved()) {
                if (press.isExternalProcess()) return press;
                boolean isIdle = press.getStack(0).isEmpty();
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
        CircuitFabricatorBlockEntity bestIdle = null;
        float bestSpeed = -1f;
        for (CircuitFabricatorBlockEntity fab : this.cachedFabricators) {
            if (fab != null && !fab.isRemoved()) {
                if (fab.isExternalProcess()) return fab;
                boolean isIdle = fab.getStack(CircuitFabricatorBlockEntity.SUBSTRATE_SLOT).isEmpty()
                        && fab.getStack(CircuitFabricatorBlockEntity.COMPONENT_SLOT_1).isEmpty();
                float speed = fab.getSpeedMultiplier();
                if (isIdle && speed > bestSpeed) {
                    bestIdle = fab;
                    bestSpeed = speed;
                }
            }
        }
        return bestIdle;
    }

    public @Nullable BlockEntity getBestAvailableFurnace() {
        for (BlockEntity f : this.cachedFurnaces) {
            if (f != null && !f.isRemoved()) {
                if (f instanceof EnchantedFurnaceBlockEntity ef) {
                    if (ef.getStack(0).isEmpty()) return ef;
                } else if (f instanceof net.minecraft.block.entity.AbstractFurnaceBlockEntity af) {
                    if (af.getStack(0).isEmpty()) return af;
                }
            }
        }
        return null;
    }

    public @Nullable CastingPortBlockEntity getBestAvailableCaster() {
        for (CastingPortBlockEntity c : this.cachedCasters) {
            if (c != null && !c.isRemoved() && !c.isCasting()) return c;
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
            case ASSEMBLE -> isOverclocked() ? 10 : 25;
        };
    }

    public record PressRecipeInfo(net.minecraft.item.Item input, int yield) {}

    public static @Nullable PressRecipeInfo getPressRecipeInfo(net.minecraft.item.Item targetItem) {
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
            List<net.minecraft.item.Item> neededComponents = new ArrayList<>(recipe.components());

            for (ItemStack s : nonNull) {
                if (!hasSubstrate && s.isOf(recipe.substrate())) {
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

    public static @Nullable ItemStack getDirectSmeltingPatternResult(ServerWorld world, List<ItemStack> patternStacks) {
        ItemStack single = null;
        for (ItemStack s : patternStacks) {
            if (!s.isEmpty()) {
                if (single != null) return null;
                single = s;
            }
        }
        if (single == null) return null;

        // 1. Check custom dust smelting
        net.minecraft.item.Item dustSmelt = EnchantedFurnaceBlockEntity.getDustSmeltingResult(single.getItem());
        if (dustSmelt != null) {
            return new ItemStack(dustSmelt, single.getCount());
        }

        // 2. Check vanilla smelting recipes
        Optional<RecipeEntry<SmeltingRecipe>> match = world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(single), world);
        if (match.isPresent()) {
            ItemStack res = match.get().value().craft(new SingleStackRecipeInput(single), world.getRegistryManager());
            if (!res.isEmpty()) {
                return new ItemStack(res.getItem(), res.getCount() * single.getCount());
            }
        }
        return null;
    }

    public static List<net.minecraft.item.Item> getDustSmeltingInputs(net.minecraft.item.Item targetItem) {
        if (targetItem == net.minecraft.item.Items.IRON_INGOT) return List.of(ModItems.IRON_DUST);
        if (targetItem == net.minecraft.item.Items.COPPER_INGOT) return List.of(ModItems.COPPER_DUST);
        if (targetItem == ModItems.TIN_INGOT) return List.of(ModItems.TIN_DUST, ModItems.RAW_TIN);
        if (targetItem == ModItems.BRONZE_INGOT) return List.of(ModItems.BRONZE_DUST);
        if (targetItem == ModItems.TITANIUM_INGOT) return List.of(ModItems.TITANIUM_DUST, ModItems.RAW_TITANIUM);
        if (targetItem == net.minecraft.item.Items.GOLD_INGOT) return List.of(ModItems.GOLD_DUST);
        if (targetItem == net.minecraft.item.Items.DIAMOND) return List.of(ModItems.DIAMOND_DUST);
        if (targetItem == net.minecraft.item.Items.NETHERITE_INGOT) return List.of(ModItems.NETHERITE_DUST);
        if (targetItem == net.minecraft.item.Items.EMERALD) return List.of(ModItems.EMERALD_DUST);
        if (targetItem == net.minecraft.item.Items.COAL) return List.of(ModItems.COAL_DUST);
        return java.util.Collections.emptyList();
    }

    public java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> getAvailableMoltenMetals() {
        if (this.world == null) return new java.util.EnumMap<>(net.enchantedwood.fluid.MoltenMetal.class);
        java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> amounts = new java.util.EnumMap<>(net.enchantedwood.fluid.MoltenMetal.class);
        java.util.Set<BlockPos> visitedControllers = new java.util.HashSet<>();

        for (TitaniumTankControllerBlockEntity controller : this.cachedTankControllers) {
            if (controller != null && !controller.isRemoved() && controller.isFormed() && visitedControllers.add(controller.getPos())) {
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
        if (this.world == null || requiredFluids == null || requiredFluids.isEmpty()) return;
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
                if (controller != null && !controller.isRemoved() && controller.isFormed() && visitedControllers.add(controller.getPos())) {
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
        return this.inventory.get(UPGRADE_SLOT).isOf(ModItems.BLAZE_OVERCLOCK_CORE);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, SuperComputerBlockEntity entity) {
        if (entity.activeJob != null) {
            entity.tickActiveJob(world, pos, state);
            return;
        }

        entity.updateMachineCache(false);
        boolean wasLit = state.get(SuperComputerBlock.LIT);

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
                entity.markDirty();
            }
            if (wasLit) world.setBlockState(pos, state.with(SuperComputerBlock.LIT, false), 3);
            return;
        }

        CraftingRecipeInput recipeInput = CraftingRecipeInput.create(3, 3, patternStacks);
        Optional<RecipeEntry<CraftingRecipe>> match = world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, recipeInput, world);

        ItemStack resultStack = ItemStack.EMPTY;
        if (match.isPresent()) {
            entity.hasValidRecipe = true;
            resultStack = match.get().value().craft(recipeInput, world.getRegistryManager());
        } else {
            ItemStack directCast = getDirectCastingPatternResult(patternStacks);
            ItemStack directFab = getDirectFabricatorPatternResult(patternStacks);
            ItemStack directPress = getDirectPressPatternResult(patternStacks);
            ItemStack directSmelt = getDirectSmeltingPatternResult(world, patternStacks);

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
            } else {
                entity.hasValidRecipe = false;
                entity.craftProgress = 0;
                if (!entity.inventory.get(PREVIEW_SLOT).isEmpty()) {
                    entity.inventory.set(PREVIEW_SLOT, ItemStack.EMPTY);
                    entity.markDirty();
                }
                if (wasLit) world.setBlockState(pos, state.with(SuperComputerBlock.LIT, false), 3);
                return;
            }
        }

        if (resultStack.isEmpty()) {
            entity.craftProgress = 0;
            if (!entity.inventory.get(PREVIEW_SLOT).isEmpty()) {
                entity.inventory.set(PREVIEW_SLOT, ItemStack.EMPTY);
                entity.markDirty();
            }
            if (wasLit) world.setBlockState(pos, state.with(SuperComputerBlock.LIT, false), 3);
            return;
        }

        // Keep preview slot updated with the crafted result (Display only, no auto-crafting)
        if (!ItemStack.areItemsAndComponentsEqual(entity.inventory.get(PREVIEW_SLOT), resultStack)) {
            entity.inventory.set(PREVIEW_SLOT, resultStack.copy());
            entity.markDirty();
        }

        if (wasLit) {
            world.setBlockState(pos, state.with(SuperComputerBlock.LIT, false), 3);
        }
    }

    private void tickActiveJob(ServerWorld world, BlockPos pos, BlockState state) {
        if (this.activeJob == null) return;
        ActiveCraftJob job = this.activeJob;
        CraftStep step = job.getCurrentStep();

        if (step == null) {
            // All steps completed!
            EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
            PlayerEntity player = world.getPlayerByUuid(job.playerUuid);

            depositCraftedResult(terminal, player, job.finalResult.copy());
            for (ItemStack leftover : job.leftoverItems) {
                if (!leftover.isEmpty()) {
                    depositCraftedResult(terminal, player, leftover.copy());
                }
            }

            world.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.BLOCKS, 0.7f, 1.1f);
            if (player != null) {
                sendFeedback(player, "§a⚡ Factory Completed: §f" + job.finalResult.getCount() + "x " + job.finalResult.getName().getString());
            }

            this.craftProgress = 0;
            this.maxCraftProgress = 0;
            boolean hadCraftAll = job.craftAll;
            this.activeJob = null;

            HydraulicPressBlockEntity press = getBestAvailablePress();
            if (press != null) press.clearExternalProcess();
            CircuitFabricatorBlockEntity fab = getBestAvailableFabricator();
            if (fab != null) fab.clearExternalProcess();
            BlockEntity furnace = getBestAvailableFurnace();
            if (furnace != null) {
                if (furnace instanceof EnchantedFurnaceBlockEntity ef) {
                    ef.setStack(0, ItemStack.EMPTY);
                }
                BlockPos fPos = furnace.getPos();
                BlockState fState = world.getBlockState(fPos);
                if (fState.contains(net.minecraft.state.property.Properties.LIT) && fState.get(net.minecraft.state.property.Properties.LIT)) {
                    world.setBlockState(fPos, fState.with(net.minecraft.state.property.Properties.LIT, false), 3);
                }
            }

            markDirty();

            if (hadCraftAll && player != null) {
                executeManualCraft(player, true);
            } else {
                world.setBlockState(pos, state.with(SuperComputerBlock.LIT, false), 3);
            }
            return;
        }

        // Keep Super Computer LIT
        if (!state.get(SuperComputerBlock.LIT)) {
            world.setBlockState(pos, state.with(SuperComputerBlock.LIT, true), 3);
        }

        // Determine power draw for current step
        int powerDraw = switch (step.type) {
            case PRESS -> 40;
            case FABRICATE -> 50;
            case SMELT -> 30;
            case CAST -> 25;
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
            if (world.getTime() % 60 == 0) {
                PlayerEntity p = world.getPlayerByUuid(job.playerUuid);
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
                    BlockPos pPos = press.getPos();
                    if (world.getTime() % 3 == 0) {
                        world.spawnParticles(net.minecraft.particle.ParticleTypes.SMOKE, pPos.getX() + 0.5, pPos.getY() + 0.8, pPos.getZ() + 0.5, 4, 0.15, 0.15, 0.15, 0.02);
                    }
                }
            }
            case SMELT -> {
                BlockEntity furnace = getBestAvailableFurnace();
                if (furnace != null) {
                    BlockPos fPos = furnace.getPos();
                    BlockState fState = world.getBlockState(fPos);
                    if (fState.contains(net.minecraft.state.property.Properties.LIT) && !fState.get(net.minecraft.state.property.Properties.LIT)) {
                        world.setBlockState(fPos, fState.with(net.minecraft.state.property.Properties.LIT, true), 3);
                    }
                    if (furnace instanceof EnchantedFurnaceBlockEntity ef) {
                        ItemStack inStack = step.inputItem != null ? new ItemStack(step.inputItem, 1) : ItemStack.EMPTY;
                        ef.setExternalProcess(inStack, job.currentStepTicks, stepMax);
                    }
                    if (world.getTime() % 4 == 0) {
                        world.spawnParticles(net.minecraft.particle.ParticleTypes.FLAME, fPos.getX() + 0.5, fPos.getY() + 0.5, fPos.getZ() + 0.5, 3, 0.15, 0.15, 0.15, 0.02);
                    }
                    if (world.getTime() % 20 == 0) {
                        world.playSound(null, fPos, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS, 0.8f, 1.0f);
                    }
                }
            }
            case CAST -> {
                CastingPortBlockEntity caster = getBestAvailableCaster();
                if (caster != null) {
                    BlockPos cPos = caster.getPos();
                    if (world.getTime() % 3 == 0) {
                        world.spawnParticles(net.minecraft.particle.ParticleTypes.LAVA, cPos.getX() + 0.5, cPos.getY() + 0.8, cPos.getZ() + 0.5, 3, 0.15, 0.15, 0.15, 0.02);
                    }
                    if (world.getTime() % 15 == 0) {
                        world.playSound(null, cPos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.6f, 1.3f);
                    }
                }
            }
            case FABRICATE -> {
                CircuitFabricatorBlockEntity fab = getBestAvailableFabricator();
                if (fab != null) {
                    ItemStack substrateStack = step.inputItem != null ? new ItemStack(step.inputItem, 1) : ItemStack.EMPTY;
                    fab.setExternalProcess(substrateStack, job.currentStepTicks, stepMax);
                    BlockPos bPos = fab.getPos();
                    if (world.getTime() % 3 == 0) {
                        world.spawnParticles(net.minecraft.particle.ParticleTypes.ENCHANTED_HIT, bPos.getX() + 0.5, bPos.getY() + 0.8, bPos.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.05);
                    }
                    if (world.getTime() % 25 == 0) {
                        world.playSound(null, bPos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 0.6f, 1.8f);
                    }
                }
            }
            case ASSEMBLE -> {
                if (world.getTime() % 3 == 0) {
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.05);
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
                    BlockPos sPos = press != null ? press.getPos() : pos;
                    if (press != null) {
                        press.clearExternalProcess();
                    }
                    world.playSound(null, sPos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.8f, 0.6f);
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.CRIT, sPos.getX() + 0.5, sPos.getY() + 0.8, sPos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.1);
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.LARGE_SMOKE, sPos.getX() + 0.5, sPos.getY() + 0.8, sPos.getZ() + 0.5, 6, 0.15, 0.15, 0.15, 0.05);
                }
                case SMELT -> {
                    BlockEntity furnace = getBestAvailableFurnace();
                    BlockPos sPos = furnace != null ? furnace.getPos() : pos;
                    if (furnace instanceof EnchantedFurnaceBlockEntity ef) {
                        ef.clearExternalProcess();
                    }
                    world.playSound(null, sPos, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS, 0.9f, 1.2f);
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.FLAME, sPos.getX() + 0.5, sPos.getY() + 0.8, sPos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.05);
                    if (furnace != null) {
                        BlockState fState = world.getBlockState(sPos);
                        if (fState.contains(net.minecraft.state.property.Properties.LIT) && fState.get(net.minecraft.state.property.Properties.LIT)) {
                            world.setBlockState(sPos, fState.with(net.minecraft.state.property.Properties.LIT, false), 3);
                        }
                    }
                }
                case CAST -> {
                    CastingPortBlockEntity caster = getBestAvailableCaster();
                    BlockPos sPos = caster != null ? caster.getPos() : pos;
                    world.playSound(null, sPos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.8f, 1.2f);
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.SMOKE, sPos.getX() + 0.5, sPos.getY() + 0.8, sPos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.05);
                }
                case FABRICATE -> {
                    CircuitFabricatorBlockEntity fab = getBestAvailableFabricator();
                    BlockPos sPos = fab != null ? fab.getPos() : pos;
                    if (fab != null) {
                        fab.clearExternalProcess();
                    }
                    world.playSound(null, sPos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 0.8f, 1.6f);
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.ENCHANTED_HIT, sPos.getX() + 0.5, sPos.getY() + 0.8, sPos.getZ() + 0.5, 15, 0.2, 0.2, 0.2, 0.1);
                }
                case ASSEMBLE -> {
                    world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.5f, 1.4f);
                }
            }

            job.currentStepIndex++;
            job.currentStepTicks = 0;
        }

        markDirty();
    }

    public void executeManualCraft(PlayerEntity player, boolean craftAll) {
        if (!(this.world instanceof ServerWorld serverWorld)) return;
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

        CraftingRecipeInput recipeInput = CraftingRecipeInput.create(3, 3, patternStacks);
        Optional<RecipeEntry<CraftingRecipe>> match = serverWorld.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, recipeInput, serverWorld);
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
            sendFeedback(player, "§c[Super Computer] No valid crafting recipe in the 3x3 grid!");
            return;
        }

        ItemStack resultStack = match.get().value().craft(recipeInput, serverWorld.getRegistryManager());
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
            sendFeedback(player, "§c[Super Computer] All connected Furnaces are currently busy!");
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

        this.activeJob = new ActiveCraftJob(player.getUuid(), plan.steps, resultStack.copy(), plan.leftoverSynthesized, craftAll, patternStacks);
        sendFeedback(player, "§6⚡ Factory Activated: §fManufacturing " + resultStack.getName().getString() + " §7(" + plan.steps.size() + " operations queued)");
        markDirty();
        serverWorld.setBlockState(this.pos, serverWorld.getBlockState(this.pos).with(SuperComputerBlock.LIT, true), 3);
    }

    private void executeDirectCasting(ServerWorld serverWorld, PlayerEntity player, ItemStack directCast, boolean craftAll) {
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
        this.activeJob = new ActiveCraftJob(player.getUuid(), steps, directCast.copy(), List.of(), craftAll, List.of());
        sendFeedback(player, "§6⚡ Casting: §f" + directCast.getName().getString() + " §6[" + perBatchCost + " mB Molten " + castInfo.metal.getDisplayName() + "]");
        markDirty();
        serverWorld.setBlockState(this.pos, serverWorld.getBlockState(this.pos).with(SuperComputerBlock.LIT, true), 3);
    }

    private void executeDirectFabrication(ServerWorld serverWorld, PlayerEntity player, List<ItemStack> patternStacks, ItemStack directFab, boolean craftAll) {
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

        if (plan.hydraulicPressings > 0 && getBestAvailablePress() == null) {
            sendFeedback(player, "§c[Super Computer] All connected Hydraulic Presses are currently busy!");
            return;
        }
        if (plan.circuitFabrications > 0 && getBestAvailableFabricator() == null) {
            sendFeedback(player, "§c[Super Computer] All connected Circuit Fabricators are currently busy!");
            return;
        }

        consumeIngredients(terminal, player, plan.rawIngredientsToConsume);
        if (!plan.moltenMetalsToConsume.isEmpty()) {
            consumeMoltenMetals(plan.moltenMetalsToConsume);
        }

        this.activeJob = new ActiveCraftJob(player.getUuid(), plan.steps, directFab.copy(), plan.leftoverSynthesized, craftAll, patternStacks);
        sendFeedback(player, "§6⚡ Fabricating: §f" + directFab.getName().getString() + " §7(" + plan.steps.size() + " operations queued)");
        markDirty();
        serverWorld.setBlockState(this.pos, serverWorld.getBlockState(this.pos).with(SuperComputerBlock.LIT, true), 3);
    }

    private void executeDirectPress(ServerWorld serverWorld, PlayerEntity player, List<ItemStack> patternStacks, ItemStack directPress, boolean craftAll) {
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

        net.minecraft.item.Item rawInputItem = single.getItem();
        ItemStack oneBatchResult = HydraulicPressBlockEntity.getPlateResult(rawInputItem);
        if (oneBatchResult.isEmpty()) return;

        if (!canAcceptOutput(oneBatchResult)) {
            sendFeedback(player, "§c[Super Computer] Output buffer & digital storage are full!");
            return;
        }

        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        int availCount = 0;
        if (terminal != null && terminal.isNetworkOnline()) {
            for (EnchantedStorageTerminalBlockEntity.StoredItem si : terminal.getStoredItems()) {
                if (si.getCount() > 0 && si.getSample().isOf(rawInputItem)) {
                    availCount += (int) Math.min(si.getCount(), Integer.MAX_VALUE);
                }
            }
        }
        if (player != null) {
            PlayerInventory pInv = player.getInventory();
            for (int i = 0; i < 36; i++) {
                ItemStack ps = pInv.getStack(i);
                if (!ps.isEmpty() && ps.isOf(rawInputItem)) {
                    availCount += ps.getCount();
                }
            }
        }

        if (availCount < 1) {
            sendFeedback(player, "§c[Super Computer] Missing: §e1x " + new ItemStack(rawInputItem).getName().getString());
            return;
        }

        consumeIngredients(terminal, player, List.of(new ItemStack(rawInputItem, 1)));
        List<CraftStep> steps = List.of(new CraftStep(StepType.PRESS, rawInputItem, oneBatchResult.getItem()));
        this.activeJob = new ActiveCraftJob(player.getUuid(), steps, oneBatchResult.copy(), List.of(), craftAll, patternStacks);
        sendFeedback(player, "§6⚡ Pressing: §f" + oneBatchResult.getName().getString());
        markDirty();
        serverWorld.setBlockState(this.pos, serverWorld.getBlockState(this.pos).with(SuperComputerBlock.LIT, true), 3);
    }

    private void executeDirectSmelting(ServerWorld serverWorld, PlayerEntity player, List<ItemStack> patternStacks, ItemStack directSmelt, boolean craftAll) {
        if (!isFurnaceOnline()) {
            sendFeedback(player, "§e[Super Computer] Place an Enchanted Furnace within 32 blocks to enable automated smelting!");
            return;
        }

        if (this.activeJob != null) {
            sendFeedback(player, "§e[Super Computer] Factory is currently busy working on a job!");
            return;
        }

        if (getBestAvailableFurnace() == null) {
            sendFeedback(player, "§c[Super Computer] All connected Furnaces are currently busy!");
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

        net.minecraft.item.Item rawInputItem = single.getItem();
        ItemStack oneBatchResult = directSmelt.copyWithCount(directSmelt.getCount() / Math.max(1, single.getCount()));
        if (oneBatchResult.isEmpty()) oneBatchResult = directSmelt.copyWithCount(1);

        if (!canAcceptOutput(oneBatchResult)) {
            sendFeedback(player, "§c[Super Computer] Output buffer & digital storage are full!");
            return;
        }

        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        int availCount = 0;
        if (terminal != null && terminal.isNetworkOnline()) {
            for (EnchantedStorageTerminalBlockEntity.StoredItem si : terminal.getStoredItems()) {
                if (si.getCount() > 0 && si.getSample().isOf(rawInputItem)) {
                    availCount += (int) Math.min(si.getCount(), Integer.MAX_VALUE);
                }
            }
        }
        if (player != null) {
            PlayerInventory pInv = player.getInventory();
            for (int i = 0; i < 36; i++) {
                ItemStack ps = pInv.getStack(i);
                if (!ps.isEmpty() && ps.isOf(rawInputItem)) {
                    availCount += ps.getCount();
                }
            }
        }

        if (availCount < 1) {
            sendFeedback(player, "§c[Super Computer] Missing: §e1x " + new ItemStack(rawInputItem).getName().getString());
            return;
        }

        consumeIngredients(terminal, player, List.of(new ItemStack(rawInputItem, 1)));
        List<CraftStep> steps = List.of(new CraftStep(StepType.SMELT, rawInputItem, oneBatchResult.getItem()));
        this.activeJob = new ActiveCraftJob(player.getUuid(), steps, oneBatchResult.copy(), List.of(), craftAll, patternStacks);
        sendFeedback(player, "§6⚡ Smelting: §f" + oneBatchResult.getName().getString());
        markDirty();
        serverWorld.setBlockState(this.pos, serverWorld.getBlockState(this.pos).with(SuperComputerBlock.LIT, true), 3);
    }

    private void sendFeedback(PlayerEntity player, String msg) {
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(serverPlayer, new net.enchantedwood.network.SuperComputerStatusPayload(msg));
        }
        player.sendMessage(Text.literal(msg), false);
    }

    public void executeManualCraft(PlayerEntity player) {
        executeManualCraft(player, false);
    }

    private boolean drawNetworkPower() {
        if (this.world == null) return false;
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
        ASSEMBLE
    }

    public static class CraftStep {
        public final StepType type;
        public final @Nullable net.minecraft.item.Item inputItem;
        public final @Nullable net.minecraft.item.Item outputItem;
        public final @Nullable net.enchantedwood.fluid.MoltenMetal moltenMetal;
        public final int amountMb;

        public CraftStep(StepType type, @Nullable net.minecraft.item.Item outputItem) {
            this(type, null, outputItem, null, 0);
        }

        public CraftStep(StepType type, @Nullable net.minecraft.item.Item inputItem, @Nullable net.minecraft.item.Item outputItem) {
            this(type, inputItem, outputItem, null, 0);
        }

        public CraftStep(StepType type, @Nullable net.enchantedwood.fluid.MoltenMetal moltenMetal, @Nullable net.minecraft.item.Item outputItem, int amountMb) {
            this(type, null, outputItem, moltenMetal, amountMb);
        }

        public CraftStep(StepType type, @Nullable net.minecraft.item.Item inputItem, @Nullable net.minecraft.item.Item outputItem,
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

    public static String getIngredientDisplayName(net.minecraft.recipe.Ingredient ing) {
        List<net.minecraft.item.Item> matching;
        try {
            matching = ing.getMatchingItems()
                    .map(net.minecraft.registry.entry.RegistryEntry::value)
                    .toList();
        } catch (Throwable t) {
            matching = java.util.Collections.emptyList();
        }

        if (matching.isEmpty()) return "Unknown Material";

        if (matching.size() == 1) {
            return matching.get(0).getName().getString();
        }

        // Detect item families
        boolean allLeaves = matching.stream().allMatch(item -> item.getTranslationKey().contains("leaves"));
        if (allLeaves) return "Leaf Blocks (any type)";

        boolean allLogs = matching.stream().allMatch(item -> item.getTranslationKey().contains("log") || item.getTranslationKey().contains("wood") || item.getTranslationKey().contains("stem"));
        if (allLogs) return "Logs (any type)";

        boolean allPlanks = matching.stream().allMatch(item -> item.getTranslationKey().contains("planks"));
        if (allPlanks) return "Planks (any type)";

        boolean allFlowers = matching.stream().allMatch(item -> item.getTranslationKey().contains("flower") || item.getTranslationKey().contains("tulip") || item.getTranslationKey().contains("orchid") || item.getTranslationKey().contains("daisy") || item.getTranslationKey().contains("dandelion") || item.getTranslationKey().contains("poppy") || item.getTranslationKey().contains("allium") || item.getTranslationKey().contains("bluet") || item.getTranslationKey().contains("rose") || item.getTranslationKey().contains("lilac") || item.getTranslationKey().contains("peony"));
        if (allFlowers) return "Flowers (any type)";

        boolean allWool = matching.stream().allMatch(item -> item.getTranslationKey().contains("wool"));
        if (allWool) return "Wool (any type)";

        boolean allGlass = matching.stream().allMatch(item -> item.getTranslationKey().contains("glass"));
        if (allGlass) return "Glass (any type)";

        boolean allDyes = matching.stream().allMatch(item -> item.getTranslationKey().contains("dye"));
        if (allDyes) return "Dye (any type)";

        boolean allSaplings = matching.stream().allMatch(item -> item.getTranslationKey().contains("sapling"));
        if (allSaplings) return "Saplings (any type)";

        boolean allSand = matching.stream().allMatch(item -> item.getTranslationKey().contains("sand"));
        if (allSand) return "Sand (any type)";

        if (matching.size() > 2) {
            return matching.get(0).getName().getString() + " (or equivalent)";
        }

        return matching.get(0).getName().getString();
    }

    public CraftingPlanResult resolveCraftingPlan(ServerWorld world,
                                                 @Nullable EnchantedStorageTerminalBlockEntity terminal,
                                                 @Nullable PlayerEntity player,
                                                 CraftingRecipe craftingRecipe,
                                                 List<ItemStack> patternStacks) {
        java.util.Map<String, Integer> missingItems = new java.util.LinkedHashMap<>();
        try {
            // Snapshot available items from Terminal, Player Inventory, and Matrix
            java.util.Map<net.minecraft.item.Item, Integer> available = new java.util.HashMap<>();

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
                PlayerInventory pInv = player.getInventory();
                for (int i = 0; i < 36; i++) {
                    ItemStack pStack = pInv.getStack(i);
                    if (!pStack.isEmpty()) {
                        available.put(pStack.getItem(), available.getOrDefault(pStack.getItem(), 0) + pStack.getCount());
                    }
                }
            }

            CraftingPlan plan = new CraftingPlan();
            java.util.Map<net.minecraft.item.Item, Integer> virtualBuffer = new java.util.HashMap<>();
            java.util.Set<net.minecraft.item.Item> activeRecursion = new java.util.HashSet<>();
            java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> availableMolten = isCasterOnline()
                    ? new java.util.EnumMap<>(getAvailableMoltenMetals())
                    : new java.util.EnumMap<>(net.enchantedwood.fluid.MoltenMetal.class);

            List<net.minecraft.recipe.Ingredient> ingredients;
            try {
                ingredients = craftingRecipe.getIngredientPlacement().getIngredients();
            } catch (Throwable t) {
                ingredients = java.util.Collections.emptyList();
            }

            boolean allSatisfied = true;
            if (!ingredients.isEmpty()) {
                for (net.minecraft.recipe.Ingredient ing : ingredients) {
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
                rootResult = craftingRecipe.craft(CraftingRecipeInput.create(3, 3, patternStacks), world.getRegistryManager());
            }
            plan.steps.add(new CraftStep(StepType.ASSEMBLE, rootResult.isEmpty() ? null : rootResult.getItem()));

            // Record any leftover synthesized items
            for (java.util.Map.Entry<net.minecraft.item.Item, Integer> entry : virtualBuffer.entrySet()) {
                if (entry.getValue() > 0) {
                    plan.leftoverSynthesized.add(new ItemStack(entry.getKey(), entry.getValue()));
                }
            }

            return new CraftingPlanResult(true, plan, missingItems);
        } catch (Throwable t) {
            return new CraftingPlanResult(false, null, missingItems);
        }
    }

    public CraftingPlanResult resolveDirectFabricatorPlan(ServerWorld world,
                                                         @Nullable EnchantedStorageTerminalBlockEntity terminal,
                                                         @Nullable PlayerEntity player,
                                                         CircuitFabricatorBlockEntity.FabricatorRecipe recipe) {
        java.util.Map<String, Integer> missingItems = new java.util.LinkedHashMap<>();
        try {
            java.util.Map<net.minecraft.item.Item, Integer> available = new java.util.HashMap<>();

            if (terminal != null && terminal.isNetworkOnline()) {
                for (EnchantedStorageTerminalBlockEntity.StoredItem item : terminal.getStoredItems()) {
                    if (item.getCount() > 0 && !item.getSample().isEmpty()) {
                        int c = (int) Math.min(item.getCount(), (long) Integer.MAX_VALUE);
                        available.put(item.getSample().getItem(), available.getOrDefault(item.getSample().getItem(), 0) + c);
                    }
                }
            }

            if (player != null) {
                PlayerInventory pInv = player.getInventory();
                for (int i = 0; i < 36; i++) {
                    ItemStack pStack = pInv.getStack(i);
                    if (!pStack.isEmpty()) {
                        available.put(pStack.getItem(), available.getOrDefault(pStack.getItem(), 0) + pStack.getCount());
                    }
                }
            }

            CraftingPlan plan = new CraftingPlan();
            java.util.Map<net.minecraft.item.Item, Integer> virtualBuffer = new java.util.HashMap<>();
            java.util.Set<net.minecraft.item.Item> activeRecursion = new java.util.HashSet<>();
            java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> availableMolten = isCasterOnline()
                    ? new java.util.EnumMap<>(getAvailableMoltenMetals())
                    : new java.util.EnumMap<>(net.enchantedwood.fluid.MoltenMetal.class);

            boolean allSatisfied = true;
            if (!resolveItemRequirement(world, recipe.substrate(), available, availableMolten, virtualBuffer, plan, missingItems, activeRecursion, 0)) {
                allSatisfied = false;
            }

            for (net.minecraft.item.Item comp : recipe.components()) {
                if (!resolveItemRequirement(world, comp, available, availableMolten, virtualBuffer, plan, missingItems, activeRecursion, 0)) {
                    allSatisfied = false;
                }
            }

            if (!allSatisfied) {
                return new CraftingPlanResult(false, null, missingItems);
            }

            plan.circuitFabrications++;
            plan.steps.add(new CraftStep(StepType.FABRICATE, recipe.substrate(), recipe.output().getItem()));

            for (java.util.Map.Entry<net.minecraft.item.Item, Integer> entry : virtualBuffer.entrySet()) {
                if (entry.getValue() > 0) {
                    plan.leftoverSynthesized.add(new ItemStack(entry.getKey(), entry.getValue()));
                }
            }

            return new CraftingPlanResult(true, plan, missingItems);
        } catch (Throwable t) {
            return new CraftingPlanResult(false, null, missingItems);
        }
    }

    private static final CraftingRecipeInput DUMMY_INPUT = CraftingRecipeInput.create(3, 3, java.util.Collections.nCopies(9, ItemStack.EMPTY));

    private ItemStack getSafeRecipeResult(CraftingRecipe recipe, ServerWorld world) {
        try {
            ItemStack res = recipe.craft(DUMMY_INPUT, world.getRegistryManager());
            if (!res.isEmpty()) return res;
        } catch (Throwable ignored) {}
        try {
            return recipe.craft(CraftingRecipeInput.EMPTY, world.getRegistryManager());
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }

    private boolean resolveItemRequirement(ServerWorld world, net.minecraft.item.Item targetItem,
                                           java.util.Map<net.minecraft.item.Item, Integer> available,
                                           java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> availableMolten,
                                           java.util.Map<net.minecraft.item.Item, Integer> virtualBuffer,
                                           CraftingPlan plan,
                                           java.util.Map<String, Integer> missingItems,
                                           java.util.Set<net.minecraft.item.Item> activeRecursion,
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

        // 4. Prevent infinite loops or deep recursion
        if (depth >= 8 || activeRecursion.contains(targetItem)) {
            missingItems.put(targetItem.getName().getString(), missingItems.getOrDefault(targetItem.getName().getString(), 0) + 1);
            return false;
        }

        activeRecursion.add(targetItem);
        java.util.Map<String, Integer> bestCandidateMissing = null;
        try {
            // 5. Check if targetItem can be pressed via an online Hydraulic Press
            if (isPressOnline()) {
                PressRecipeInfo pressInfo = getPressRecipeInfo(targetItem);
                if (pressInfo != null) {
                    java.util.Map<net.minecraft.item.Item, Integer> backupAvailable = new java.util.HashMap<>(available);
                    java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupMolten = new java.util.EnumMap<>(availableMolten);
                    java.util.Map<net.minecraft.item.Item, Integer> backupVirtual = new java.util.HashMap<>(virtualBuffer);
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
                    if (fabRecipe.output().isOf(targetItem)) {
                        java.util.Map<net.minecraft.item.Item, Integer> backupAvailable = new java.util.HashMap<>(available);
                        java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupMolten = new java.util.EnumMap<>(availableMolten);
                        java.util.Map<net.minecraft.item.Item, Integer> backupVirtual = new java.util.HashMap<>(virtualBuffer);
                        List<ItemStack> backupPlan = new ArrayList<>(plan.rawIngredientsToConsume);
                        java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupPlanMolten = new java.util.EnumMap<>(plan.moltenMetalsToConsume);
                        List<CraftStep> backupStepsList = new ArrayList<>(plan.steps);
                        int backupSteps = plan.totalCraftingSteps;
                        int backupMoltenUsed = plan.moltenMetalUsedMb;
                        int backupFabs = plan.circuitFabrications;

                        java.util.Map<String, Integer> fabMissing = new java.util.LinkedHashMap<>();
                        boolean success = resolveItemRequirement(world, fabRecipe.substrate(), available, availableMolten, virtualBuffer, plan, fabMissing, activeRecursion, depth + 1);
                        for (net.minecraft.item.Item comp : fabRecipe.components()) {
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

            // 7. Check if targetItem can be smelted via an online Enchanted Furnace / Smelter
            if (isFurnaceOnline()) {
                // A. Check custom dust smelting (e.g. Iron Ingot from Iron Dust, etc.)
                for (net.minecraft.item.Item dustCandidate : getDustSmeltingInputs(targetItem)) {
                    java.util.Map<net.minecraft.item.Item, Integer> backupAvailable = new java.util.HashMap<>(available);
                    java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupMolten = new java.util.EnumMap<>(availableMolten);
                    java.util.Map<net.minecraft.item.Item, Integer> backupVirtual = new java.util.HashMap<>(virtualBuffer);
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
                for (RecipeEntry<?> entry : world.getRecipeManager().values()) {
                    if (!(entry.value() instanceof AbstractCookingRecipe cookingRecipe)) continue;
                    if (cookingRecipe.getType() != RecipeType.SMELTING && cookingRecipe.getType() != RecipeType.BLASTING) continue;

                    ItemStack smeltRes = ItemStack.EMPTY;
                    try {
                        smeltRes = cookingRecipe.craft(new SingleStackRecipeInput(ItemStack.EMPTY), world.getRegistryManager());
                    } catch (Throwable ignored) {}

                    if (!smeltRes.isEmpty() && smeltRes.isOf(targetItem)) {
                        net.minecraft.recipe.Ingredient ing = cookingRecipe.ingredient();
                        if (ing == null || ing.isEmpty()) continue;

                        java.util.Map<net.minecraft.item.Item, Integer> backupAvailable = new java.util.HashMap<>(available);
                        java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupMolten = new java.util.EnumMap<>(availableMolten);
                        java.util.Map<net.minecraft.item.Item, Integer> backupVirtual = new java.util.HashMap<>(virtualBuffer);
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
                            net.minecraft.item.Item resolvedInput = null;
                            for (net.minecraft.item.Item opt : ing.getMatchingItems().map(net.minecraft.registry.entry.RegistryEntry::value).toList()) {
                                if (available.containsKey(opt) || virtualBuffer.containsKey(opt)) {
                                    resolvedInput = opt;
                                    break;
                                }
                            }
                            if (resolvedInput == null) {
                                resolvedInput = ing.getMatchingItems().findFirst().map(net.minecraft.registry.entry.RegistryEntry::value).orElse(null);
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

            // 8. Search RecipeManager for a crafting recipe that produces targetItem from available materials
            for (RecipeEntry<?> entry : world.getRecipeManager().values()) {
                if (!(entry.value() instanceof CraftingRecipe craftingRecipe)) continue;
                ItemStack result = getSafeRecipeResult(craftingRecipe, world);
                if (!result.isEmpty() && result.isOf(targetItem)) {
                    int yield = Math.max(1, result.getCount());
                    java.util.Map<net.minecraft.item.Item, Integer> backupAvailable = new java.util.HashMap<>(available);
                    java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupMolten = new java.util.EnumMap<>(availableMolten);
                    java.util.Map<net.minecraft.item.Item, Integer> backupVirtual = new java.util.HashMap<>(virtualBuffer);
                    List<ItemStack> backupPlan = new ArrayList<>(plan.rawIngredientsToConsume);
                    java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> backupPlanMolten = new java.util.EnumMap<>(plan.moltenMetalsToConsume);
                    List<CraftStep> backupStepsList = new ArrayList<>(plan.steps);
                    int backupSteps = plan.totalCraftingSteps;
                    int backupMoltenUsed = plan.moltenMetalUsedMb;

                    List<net.minecraft.recipe.Ingredient> ings;
                    try {
                        ings = craftingRecipe.getIngredientPlacement().getIngredients();
                    } catch (Throwable t) {
                        continue;
                    }

                    if (ings.isEmpty()) continue;

                    java.util.Map<String, Integer> craftCandidateMissing = new java.util.LinkedHashMap<>();
                    boolean success = true;
                    for (net.minecraft.recipe.Ingredient ing : ings) {
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
        } catch (Throwable t) {
            missingItems.put(targetItem.getName().getString(), missingItems.getOrDefault(targetItem.getName().getString(), 0) + 1);
            return false;
        } finally {
            activeRecursion.remove(targetItem);
        }

        if (bestCandidateMissing != null && !bestCandidateMissing.isEmpty()) {
            for (java.util.Map.Entry<String, Integer> e : bestCandidateMissing.entrySet()) {
                missingItems.put(e.getKey(), missingItems.getOrDefault(e.getKey(), 0) + e.getValue());
            }
        } else {
            String name = targetItem.getName().getString();
            missingItems.put(name, missingItems.getOrDefault(name, 0) + 1);
        }

        return false;
    }

    private boolean resolveIngredientRequirement(ServerWorld world, net.minecraft.recipe.Ingredient ing,
                                                 java.util.Map<net.minecraft.item.Item, Integer> available,
                                                 java.util.Map<net.enchantedwood.fluid.MoltenMetal, Integer> availableMolten,
                                                 java.util.Map<net.minecraft.item.Item, Integer> virtualBuffer,
                                                 CraftingPlan plan,
                                                 java.util.Map<String, Integer> missingItems,
                                                 java.util.Set<net.minecraft.item.Item> activeRecursion,
                                                 int depth) {
        List<net.minecraft.item.Item> matchingItems;
        try {
            matchingItems = ing.getMatchingItems().map(net.minecraft.registry.entry.RegistryEntry::value).toList();
        } catch (Throwable t) {
            matchingItems = java.util.Collections.emptyList();
        }

        if (matchingItems.isEmpty()) {
            return false;
        }

        // Priority 1: Check if any matching item was already synthesized in virtualBuffer
        for (net.minecraft.item.Item opt : matchingItems) {
            int vCount = virtualBuffer.getOrDefault(opt, 0);
            if (vCount > 0) {
                virtualBuffer.put(opt, vCount - 1);
                return true;
            }
        }

        // Priority 2: Check if any matching item is physically in available storage / inventory
        for (net.minecraft.item.Item opt : matchingItems) {
            int pCount = available.getOrDefault(opt, 0);
            if (pCount > 0) {
                available.put(opt, pCount - 1);
                plan.rawIngredientsToConsume.add(new ItemStack(opt, 1));
                return true;
            }
        }

        // Priority 3: Check if any matching item can be cast directly from available molten metals via an online Casting Port (Molder)
        if (isCasterOnline()) {
            for (net.minecraft.item.Item opt : matchingItems) {
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
        for (net.minecraft.item.Item opt : matchingItems) {
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
        }
        missingItems.put(displayName, missingItems.getOrDefault(displayName, 0) + 1);
        return false;
    }

    private boolean canBeSmelted(ServerWorld world, List<net.minecraft.item.Item> items) {
        for (net.minecraft.item.Item item : items) {
            if (EnchantedFurnaceBlockEntity.getDustSmeltingResult(item) != null) return true;
            for (RecipeEntry<?> entry : world.getRecipeManager().values()) {
                if (entry.value() instanceof AbstractCookingRecipe c && (c.getType() == RecipeType.SMELTING || c.getType() == RecipeType.BLASTING)) {
                    ItemStack res = c.craft(new SingleStackRecipeInput(ItemStack.EMPTY), world.getRegistryManager());
                    if (!res.isEmpty() && res.isOf(item)) return true;
                }
            }
        }
        return false;
    }

    private boolean canBePressed(List<net.minecraft.item.Item> items) {
        for (net.minecraft.item.Item item : items) {
            if (getPressRecipeInfo(item) != null) return true;
        }
        return false;
    }

    private boolean canBeFabricated(List<net.minecraft.item.Item> items) {
        for (net.minecraft.item.Item item : items) {
            for (CircuitFabricatorBlockEntity.FabricatorRecipe recipe : CircuitFabricatorBlockEntity.getRecipes()) {
                if (recipe.output().isOf(item)) return true;
            }
        }
        return false;
    }

    private void consumeIngredients(@Nullable EnchantedStorageTerminalBlockEntity terminal, @Nullable PlayerEntity player, List<ItemStack> required) {
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
                PlayerInventory pInv = player.getInventory();
                for (int i = 0; i < 36; i++) {
                    ItemStack pStack = pInv.getStack(i);
                    if (!pStack.isEmpty() && ItemStack.areItemsAndComponentsEqual(pStack, req)) {
                        int take = Math.min(needed, pStack.getCount());
                        pStack.decrement(take);
                        needed -= take;
                        if (pStack.isEmpty()) {
                            pInv.setStack(i, ItemStack.EMPTY);
                        }
                        pInv.markDirty();
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
            if (ItemStack.areItemsAndComponentsEqual(out, result) && out.getCount() + result.getCount() <= out.getMaxCount()) {
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

    private void depositCraftedResult(@Nullable EnchantedStorageTerminalBlockEntity terminal, @Nullable PlayerEntity player, ItemStack result) {
        // 1. Deposit into 2x2 Output Buffer (slots 10..13) first
        for (int i = 0; i < OUTPUT_SIZE; i++) {
            ItemStack out = this.inventory.get(OUTPUT_START + i);
            if (!out.isEmpty() && ItemStack.areItemsAndComponentsEqual(out, result)) {
                int space = out.getMaxCount() - out.getCount();
                if (space > 0) {
                    int move = Math.min(space, result.getCount());
                    out.increment(move);
                    result.decrement(move);
                    this.markDirty();
                    if (result.isEmpty()) return;
                }
            }
        }
        for (int i = 0; i < OUTPUT_SIZE; i++) {
            ItemStack out = this.inventory.get(OUTPUT_START + i);
            if (out.isEmpty()) {
                this.inventory.set(OUTPUT_START + i, result.copy());
                result.setCount(0);
                this.markDirty();
                return;
            }
        }

        // 2. If output buffer is full, give to player
        if (player != null && !result.isEmpty()) {
            player.getInventory().offerOrDrop(result.copy());
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
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.super_computer");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new SuperComputerScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public int size() {
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
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(this.inventory, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(this.inventory, slot);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public void clear() {
        this.inventory.clear();
        markDirty();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        // Only output buffer slots (10..13) are extractable
        return new int[]{10, 11, 12, 13};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return false; // Items are programmed in GUI, not piped in
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot >= OUTPUT_START && slot < OUTPUT_START + OUTPUT_SIZE;
    }

    @Override
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.craftProgress = view.getInt("CraftProgress", 0);
        this.energyStorage.setEnergy(view.getInt("Energy", 0));
        if (view.contains("BoundX") && view.contains("BoundY") && view.contains("BoundZ")) {
            this.boundNetworkPos = new BlockPos(view.getInt("BoundX", 0), view.getInt("BoundY", 0), view.getInt("BoundZ", 0));
            this.boundDimension = view.getString("BoundDim", "minecraft:overworld");
        } else {
            this.boundNetworkPos = null;
        }
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
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

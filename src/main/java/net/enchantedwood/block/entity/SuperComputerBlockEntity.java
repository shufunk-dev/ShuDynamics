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
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
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
            return 8;
        }
    };

    public SuperComputerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUPER_COMPUTER_BLOCK_ENTITY, pos, state);
    }

    public @Nullable EnchantedStorageTerminalBlockEntity getNetworkTerminal() {
        if (this.world == null) return null;
        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.pos.getX() + dx, this.pos.getY() + dy, this.pos.getZ() + dz);
                    BlockEntity be = this.world.getBlockEntity(mut);
                    if (be instanceof EnchantedStorageTerminalBlockEntity terminal) {
                        return terminal;
                    }
                }
            }
        }
        return null;
    }

    public boolean isNetworkOnline() {
        if (this.world == null) return false;
        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        if (terminal == null) return false;
        return terminal.isNetworkOnline();
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

        if (match.isEmpty()) {
            entity.hasValidRecipe = false;
            entity.craftProgress = 0;
            if (!entity.inventory.get(PREVIEW_SLOT).isEmpty()) {
                entity.inventory.set(PREVIEW_SLOT, ItemStack.EMPTY);
                entity.markDirty();
            }
            if (wasLit) world.setBlockState(pos, state.with(SuperComputerBlock.LIT, false), 3);
            return;
        }

        entity.hasValidRecipe = true;
        RecipeEntry<CraftingRecipe> recipeEntry = match.get();
        ItemStack resultStack = recipeEntry.value().craft(recipeInput, world.getRegistryManager());

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

    public void executeManualCraft(PlayerEntity player, boolean craftAll) {
        if (!(this.world instanceof ServerWorld serverWorld)) return;

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
            sendFeedback(player, "§c[Super Computer] No valid crafting recipe in the 3x3 grid!");
            return;
        }

        ItemStack resultStack = match.get().value().craft(recipeInput, serverWorld.getRegistryManager());
        if (resultStack.isEmpty()) return;

        EnchantedStorageTerminalBlockEntity terminal = getNetworkTerminal();
        int maxBatches = craftAll ? 64 : 1;
        int craftedBatches = 0;
        int totalCraftingStepsSum = 0;

        for (int b = 0; b < maxBatches; b++) {
            if (!canAcceptOutput(resultStack)) {
                if (craftedBatches == 0) {
                    sendFeedback(player, "§c[Super Computer] Output buffer & digital storage are full!");
                }
                break;
            }

            CraftingPlanResult planResult = resolveCraftingPlan(serverWorld, terminal, player, patternStacks);
            if (!planResult.success || planResult.plan == null) {
                if (craftedBatches == 0) {
                    if (!planResult.missingItems.isEmpty()) {
                        StringBuilder sb = new StringBuilder("§cMissing: §e");
                        boolean first = true;
                        for (java.util.Map.Entry<net.minecraft.item.Item, Integer> entry : planResult.missingItems.entrySet()) {
                            if (!first) sb.append("§7, §e");
                            sb.append(entry.getValue()).append("x ").append(entry.getKey().getName().getString());
                            first = false;
                        }
                        sendFeedback(player, sb.toString());
                    } else {
                        sendFeedback(player, "§c[Super Computer] Missing required materials in storage or inventory!");
                    }
                }
                break;
            }

            CraftingPlan plan = planResult.plan;

            int totalEnergy = ENERGY_PER_CRAFT * plan.totalCraftingSteps;
            if (this.energyStorage.getEnergy() < totalEnergy && !drawNetworkPower()) {
                if (craftedBatches == 0) {
                    sendFeedback(player, "§c[Super Computer] Insufficient energy! (Needs " + totalEnergy + " FE)");
                }
                break;
            }

            // Deduct raw ingredients
            consumeIngredients(terminal, player, plan.rawIngredientsToConsume);

            // Deduct energy
            if (this.energyStorage.getEnergy() >= totalEnergy) {
                this.energyStorage.extractEnergy(totalEnergy, false);
            } else {
                this.energyStorage.extractEnergy(this.energyStorage.getEnergy(), false);
            }

            // Deposit result and any leftover synthesized materials
            depositCraftedResult(terminal, player, resultStack.copy());
            for (ItemStack leftover : plan.leftoverSynthesized) {
                if (!leftover.isEmpty()) {
                    depositCraftedResult(terminal, player, leftover);
                }
            }

            craftedBatches++;
            totalCraftingStepsSum += plan.totalCraftingSteps;
        }

        if (craftedBatches > 0) {
            markDirty();
            serverWorld.playSound(null, this.pos, net.minecraft.sound.SoundEvents.BLOCK_ANVIL_USE, net.minecraft.sound.SoundCategory.BLOCKS, 0.6f, 1.2f);
            int totalYield = resultStack.getCount() * craftedBatches;
            sendFeedback(player, "§a⚡ Crafted: §f" + totalYield + "x " + resultStack.getName().getString() + (totalCraftingStepsSum > craftedBatches ? " §7(" + totalCraftingStepsSum + " steps synthesized)" : ""));
        }
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
        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.pos.getX() + dx, this.pos.getY() + dy, this.pos.getZ() + dz);
                    BlockEntity be = this.world.getBlockEntity(mut);
                    if (be instanceof EnchantedStorageControllerBlockEntity controller && controller.isOnline()) {
                        this.energyStorage.insertEnergy(1_000, false);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static class CraftingPlan {
        public final List<ItemStack> rawIngredientsToConsume = new ArrayList<>();
        public final List<ItemStack> leftoverSynthesized = new ArrayList<>();
        public int totalCraftingSteps = 1;
    }

    public static class CraftingPlanResult {
        public final boolean success;
        public final @Nullable CraftingPlan plan;
        public final java.util.Map<net.minecraft.item.Item, Integer> missingItems;

        public CraftingPlanResult(boolean success, @Nullable CraftingPlan plan, java.util.Map<net.minecraft.item.Item, Integer> missingItems) {
            this.success = success;
            this.plan = plan;
            this.missingItems = missingItems;
        }
    }

    public CraftingPlanResult resolveCraftingPlan(ServerWorld world,
                                                 @Nullable EnchantedStorageTerminalBlockEntity terminal,
                                                 @Nullable PlayerEntity player,
                                                 List<ItemStack> patternStacks) {
        java.util.Map<net.minecraft.item.Item, Integer> missingItems = new java.util.LinkedHashMap<>();
        try {
            // Snapshot available items from Terminal, Player Inventory, and Matrix
            java.util.Map<net.minecraft.item.Item, Integer> available = new java.util.HashMap<>();

            // 1. Digital Storage Terminal crystals
            if (terminal != null && terminal.isNetworkOnline()) {
                for (int slot = 0; slot < EnchantedStorageTerminalBlockEntity.STORAGE_SLOTS; slot++) {
                    ItemStack stack = terminal.getStack(slot);
                    if (!stack.isEmpty()) {
                        available.put(stack.getItem(), available.getOrDefault(stack.getItem(), 0) + stack.getCount());
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

            boolean allSatisfied = true;
            for (ItemStack req : patternStacks) {
                if (req.isEmpty()) continue;
                if (!resolveItemRequirement(world, req.getItem(), available, virtualBuffer, plan, missingItems, activeRecursion, 0)) {
                    allSatisfied = false;
                }
            }

            if (!allSatisfied) {
                return new CraftingPlanResult(false, null, missingItems);
            }

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
                                           java.util.Map<net.minecraft.item.Item, Integer> virtualBuffer,
                                           CraftingPlan plan,
                                           java.util.Map<net.minecraft.item.Item, Integer> missingItems,
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

        // 3. Prevent infinite loops or deep recursion
        if (depth >= 8 || activeRecursion.contains(targetItem)) {
            missingItems.put(targetItem, missingItems.getOrDefault(targetItem, 0) + 1);
            return false;
        }

        // 4. Search RecipeManager for a crafting recipe that produces targetItem
        activeRecursion.add(targetItem);
        try {
            for (RecipeEntry<?> entry : world.getRecipeManager().values()) {
                if (!(entry.value() instanceof CraftingRecipe craftingRecipe)) continue;
                ItemStack result = getSafeRecipeResult(craftingRecipe, world);
                if (!result.isEmpty() && result.isOf(targetItem)) {
                    int yield = Math.max(1, result.getCount());
                    java.util.Map<net.minecraft.item.Item, Integer> backupAvailable = new java.util.HashMap<>(available);
                    java.util.Map<net.minecraft.item.Item, Integer> backupVirtual = new java.util.HashMap<>(virtualBuffer);
                    List<ItemStack> backupPlan = new ArrayList<>(plan.rawIngredientsToConsume);
                    java.util.Map<net.minecraft.item.Item, Integer> subMissing = new java.util.LinkedHashMap<>();
                    int backupSteps = plan.totalCraftingSteps;

                    boolean success = true;
                    List<net.minecraft.recipe.Ingredient> ings;
                    try {
                        ings = craftingRecipe.getIngredientPlacement().getIngredients();
                    } catch (Throwable t) {
                        continue;
                    }

                    if (ings.isEmpty()) continue;

                    for (net.minecraft.recipe.Ingredient ing : ings) {
                        if (ing == null || ing.isEmpty()) continue;

                        if (!resolveIngredientRequirement(world, ing, available, virtualBuffer, plan, subMissing, activeRecursion, depth + 1)) {
                            success = false;
                        }
                    }

                    if (success) {
                        plan.totalCraftingSteps++;
                        if (yield > 1) {
                            virtualBuffer.put(targetItem, virtualBuffer.getOrDefault(targetItem, 0) + (yield - 1));
                        }
                        return true;
                    } else {
                        available.clear();
                        available.putAll(backupAvailable);
                        virtualBuffer.clear();
                        virtualBuffer.putAll(backupVirtual);
                        plan.rawIngredientsToConsume.clear();
                        plan.rawIngredientsToConsume.addAll(backupPlan);
                        plan.totalCraftingSteps = backupSteps;

                        if (!subMissing.isEmpty()) {
                            for (java.util.Map.Entry<net.minecraft.item.Item, Integer> me : subMissing.entrySet()) {
                                missingItems.put(me.getKey(), missingItems.getOrDefault(me.getKey(), 0) + me.getValue());
                            }
                            return false;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            return false;
        } finally {
            activeRecursion.remove(targetItem);
        }

        missingItems.put(targetItem, missingItems.getOrDefault(targetItem, 0) + 1);
        return false;
    }

    private boolean resolveIngredientRequirement(ServerWorld world, net.minecraft.recipe.Ingredient ing,
                                                 java.util.Map<net.minecraft.item.Item, Integer> available,
                                                 java.util.Map<net.minecraft.item.Item, Integer> virtualBuffer,
                                                 CraftingPlan plan,
                                                 java.util.Map<net.minecraft.item.Item, Integer> missingItems,
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

        // Priority 3: Try to synthesize one of the matching items recursively
        for (net.minecraft.item.Item opt : matchingItems) {
            if (resolveItemRequirement(world, opt, available, virtualBuffer, plan, missingItems, activeRecursion, depth)) {
                return true;
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
                for (int slot = 0; slot < EnchantedStorageTerminalBlockEntity.STORAGE_SLOTS; slot++) {
                    ItemStack termStack = terminal.getStack(slot);
                    if (!termStack.isEmpty() && ItemStack.areItemsAndComponentsEqual(termStack, req)) {
                        int take = Math.min(needed, termStack.getCount());
                        termStack.decrement(take);
                        needed -= take;
                        if (termStack.isEmpty()) {
                            terminal.setStack(slot, ItemStack.EMPTY);
                        }
                        terminal.markDirty();
                        if (needed <= 0) break;
                    }
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
        if (terminal != null && terminal.isNetworkOnline() && terminal.getStoredItemCount() + result.getCount() <= terminal.getNetworkCapacity()) {
            ItemStack toInsert = result.copy();
            for (int slot = 0; slot < EnchantedStorageTerminalBlockEntity.STORAGE_SLOTS; slot++) {
                ItemStack termStack = terminal.getStack(slot);
                if (!termStack.isEmpty() && ItemStack.areItemsAndComponentsEqual(termStack, toInsert)) {
                    int space = termStack.getMaxCount() - termStack.getCount();
                    if (space > 0) {
                        int move = Math.min(space, toInsert.getCount());
                        termStack.increment(move);
                        toInsert.decrement(move);
                        terminal.markDirty();
                        if (toInsert.isEmpty()) {
                            result.setCount(0);
                            return;
                        }
                    }
                }
            }
            if (!toInsert.isEmpty()) {
                for (int slot = 0; slot < EnchantedStorageTerminalBlockEntity.STORAGE_SLOTS; slot++) {
                    ItemStack termStack = terminal.getStack(slot);
                    if (termStack.isEmpty()) {
                        terminal.setStack(slot, toInsert.copy());
                        terminal.markDirty();
                        result.setCount(0);
                        return;
                    }
                }
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
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        view.putInt("CraftProgress", this.craftProgress);
        view.putInt("Energy", this.energyStorage.getEnergy());
    }
}

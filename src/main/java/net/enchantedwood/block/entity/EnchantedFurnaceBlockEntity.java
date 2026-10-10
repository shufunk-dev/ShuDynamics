package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class EnchantedFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
    private @Nullable BlockPos boundNetworkPos = null;
    private String boundDimension = "minecraft:overworld";

    public EnchantedFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANTED_FURNACE_BLOCK_ENTITY, pos, state, RecipeType.SMELTING);
    }

    public void bindNetwork(BlockPos pos, String dimension) {
        this.boundNetworkPos = pos;
        this.boundDimension = dimension != null ? dimension : "minecraft:overworld";
        setChanged();
    }

    public @Nullable BlockPos getBoundNetworkPos() {
        return this.boundNetworkPos;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.enchantedwood.enchanted_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new FurnaceMenu(syncId, playerInventory, this, this.dataAccess);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
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
        if (this.boundNetworkPos != null) {
            view.putInt("BoundX", this.boundNetworkPos.getX());
            view.putInt("BoundY", this.boundNetworkPos.getY());
            view.putInt("BoundZ", this.boundNetworkPos.getZ());
            view.putString("BoundDim", this.boundDimension != null ? this.boundDimension : "minecraft:overworld");
        }
    }

    public static Item getDustSmeltingResult(Item item) {
        if (item == ModItems.IRON_DUST) return Items.IRON_INGOT;
        if (item == ModItems.COPPER_DUST) return Items.COPPER_INGOT;
        if (item == ModItems.TIN_DUST) return ModItems.TIN_INGOT;
        if (item == ModItems.BRONZE_DUST) return ModItems.BRONZE_INGOT;
        if (item == ModItems.TITANIUM_DUST) return ModItems.TITANIUM_INGOT;
        if (item == ModItems.GOLD_DUST) return Items.GOLD_INGOT;
        if (item == ModItems.DIAMOND_DUST) return Items.DIAMOND;
        if (item == ModItems.NETHERITE_DUST) return Items.NETHERITE_INGOT;
        if (item == ModItems.EMERALD_DUST) return Items.EMERALD;
        if (item == ModItems.COAL_DUST) return Items.COAL;
        if (item == ModItems.RAW_TIN) return ModItems.TIN_INGOT;
        if (item == ModItems.RAW_TITANIUM || item == ModBlocks.TITANIUM_ORE.asItem() || item == ModBlocks.DEEPSLATE_TITANIUM_ORE.asItem()) return ModItems.TITANIUM_INGOT;
        return null;
    }

    private static int getFuelBurnTime(ServerLevel world, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (world != null) {
            int ticks = getFuelBurnTime(stack);
            if (ticks > 0) return ticks;
        }
        Item item = stack.getItem();
        if (item == ModItems.ENCHANTED_DUST) return 8000;
        if (item == ModItems.ENCHANTED_COAL) return 10000;
        if (item == ModBlocks.ENCHANTED_COAL_BLOCK.asItem()) return 90000;
        if (item == ModItems.COKE_COAL) return 3200;
        if (item == ModBlocks.COKE_COAL_BLOCK.asItem()) return 28800;
        if (item == ModItems.COPPER_LAVA_BUCKET) return 20000;
        if (item == ModItems.ENCHANTED_LAVA_BUCKET || item == ModItems.ENCHANTED_COPPER_LAVA_BUCKET) return 60000;
        if (item == Items.LAVA_BUCKET) return 20000;
        if (item == Items.COAL || item == Items.CHARCOAL) return 1600;
        if (item == Items.COAL_BLOCK) return 16000;
        if (item == Items.BLAZE_ROD) return 2400;
        return 0;
    }

    private boolean isExternalProcess = false;
    private int externalOperationTicks = 0;

    public boolean isExternalProcess() {
        return this.isExternalProcess;
    }

    public boolean isIdle() {
        if (this.isExternalProcess) return false;
        if (!getItem(0).isEmpty()) return false;
        if (this.dataAccess.get(2) > 0) return false;
        return true;
    }

    public void setExternalProcess(@Nullable ItemStack input, int progressTicks, int maxTicks) {
        this.isExternalProcess = true;
        this.externalOperationTicks = 5;
        if (input != null && !input.isEmpty()) {
            if (getItem(0).isEmpty() || !getItem(0).is(input.getItem())) {
                setItem(0, input.copy());
            }
        }
        int total = Math.max(1, maxTicks);
        int cook = Math.min(total, progressTicks);
        this.dataAccess.set(0, 200); // burnTime > 0 so flames render in GUI
        this.dataAccess.set(1, 200); // total burnTime
        this.dataAccess.set(2, cook); // cookTime
        this.dataAccess.set(3, total); // cookTotal
        setChanged();
    }

    public void clearExternalProcess() {
        this.isExternalProcess = false;
        this.externalOperationTicks = 0;
        setItem(0, ItemStack.EMPTY);
        this.dataAccess.set(0, 0);
        this.dataAccess.set(2, 0);
        setChanged();
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, EnchantedFurnaceBlockEntity furnace) {
        if (furnace.isExternalProcess) {
            if (furnace.externalOperationTicks > 0) {
                furnace.externalOperationTicks--;
            } else {
                furnace.clearExternalProcess();
            }
            return;
        }

        ItemStack input = furnace.getItem(0);
        Item dustResult = !input.isEmpty() ? getDustSmeltingResult(input.getItem()) : null;

        if (dustResult != null) {
            ItemStack output = furnace.getItem(2);
            boolean canOutput = output.isEmpty() || (output.is(dustResult) && output.getCount() < output.getMaxStackSize());

            if (canOutput) {
                int burnTime = furnace.dataAccess.get(0);
                int cookTime = furnace.dataAccess.get(2);
                int cookTotal = 200;
                furnace.dataAccess.set(3, cookTotal);

                // Ignite fuel if furnace isn't lit
                if (burnTime <= 0) {
                    ItemStack fuel = furnace.getItem(1);
                    int fuelBurn = getFuelBurnTime(world, fuel);
                    if (fuelBurn > 0) {
                        furnace.dataAccess.set(0, fuelBurn);
                        furnace.dataAccess.set(1, fuelBurn);
                        ItemStack remainder = getItemRemainder(fuel);
                        fuel.shrink(1);
                        if (fuel.isEmpty() && !remainder.isEmpty()) {
                            furnace.setItem(1, remainder.copy());
                        }
                        setChanged(world, pos, state);
                    }
                }

                // If lit, cook at 3x speed!
                burnTime = furnace.dataAccess.get(0);
                if (burnTime > 0) {
                    furnace.dataAccess.set(0, Math.max(0, burnTime - 1));
                    cookTime += 3; // 3x speed!
                    if (cookTime >= cookTotal) {
                        cookTime = 0;
                        input.shrink(1);
                        if (output.isEmpty()) {
                            furnace.setItem(2, new ItemStack(dustResult, 1));
                        } else {
                            output.grow(1);
                        }
                    }
                    furnace.dataAccess.set(2, cookTime);
                    setChanged(world, pos, state);
                }
                return;
            }
        }

        // Default vanilla smelting tick
        AbstractFurnaceBlockEntity.serverTick(world, pos, state, furnace);

        // 3x Smelting Speed for vanilla recipes
        int litTimeRemaining = furnace.dataAccess.get(0);
        int cookingTimeSpent = furnace.dataAccess.get(2);
        int cookingTotalTime = furnace.dataAccess.get(3);

        if (litTimeRemaining > 0 && cookingTimeSpent > 0 && cookingTimeSpent < cookingTotalTime) {
            int newCookTime = Math.min(cookingTotalTime - 1, cookingTimeSpent + 2);
            furnace.dataAccess.set(2, newCookTime);
        }
    }

    public static int getFuelBurnTime(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        net.minecraft.world.item.Item item = stack.getItem();
        if (item == net.enchantedwood.item.ModItems.ENCHANTED_DUST) return 8000;
        if (item == net.enchantedwood.item.ModItems.ENCHANTED_COAL) return 10000;
        if (item == net.enchantedwood.block.ModBlocks.ENCHANTED_COAL_BLOCK.asItem()) return 90000;
        if (item == net.enchantedwood.item.ModItems.COKE_COAL) return 3200;
        if (item == net.enchantedwood.block.ModBlocks.COKE_COAL_BLOCK.asItem()) return 28800;
        if (item == net.enchantedwood.item.ModItems.COPPER_LAVA_BUCKET) return 20000;
        if (item == net.enchantedwood.item.ModItems.ENCHANTED_LAVA_BUCKET || item == net.enchantedwood.item.ModItems.ENCHANTED_COPPER_LAVA_BUCKET) return 60000;
        if (item == net.minecraft.world.item.Items.LAVA_BUCKET) return 20000;
        if (item == net.minecraft.world.item.Items.COAL || item == net.minecraft.world.item.Items.CHARCOAL) return 1600;
        if (item == net.minecraft.world.item.Items.COAL_BLOCK) return 16000;
        if (item == net.minecraft.world.item.Items.BLAZE_ROD) return 2400;
        return net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.getFromItem(
                stack,
                net.minecraft.core.component.DataComponents.COOKING_FUEL,
                net.minecraft.world.item.component.CookingFuel::burnTime,
                null,
                0
        );
    }

    public static ItemStack getItemRemainder(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        if (stack.is(net.minecraft.world.item.Items.LAVA_BUCKET)) return new ItemStack(net.minecraft.world.item.Items.BUCKET);
        if (stack.is(net.enchantedwood.item.ModItems.COPPER_LAVA_BUCKET)) return new ItemStack(net.enchantedwood.item.ModItems.COPPER_BUCKET);
        if (stack.is(net.enchantedwood.item.ModItems.ENCHANTED_LAVA_BUCKET)) return new ItemStack(net.minecraft.world.item.Items.BUCKET);
        if (stack.is(net.enchantedwood.item.ModItems.ENCHANTED_COPPER_LAVA_BUCKET)) return new ItemStack(net.enchantedwood.item.ModItems.COPPER_BUCKET);
        var rem = stack.getItem().getCraftingRemainder();
        return rem != null ? rem.create() : ItemStack.EMPTY;
    }

}
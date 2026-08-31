package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.item.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class EnchantedFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
    public EnchantedFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANTED_FURNACE_BLOCK_ENTITY, pos, state, RecipeType.SMELTING);
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("container.enchantedwood.enchanted_furnace");
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new FurnaceScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    private static Item getDustSmeltingResult(Item item) {
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

    private static int getFuelBurnTime(ServerWorld world, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (world != null) {
            int ticks = world.getFuelRegistry().getFuelTicks(stack);
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

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, EnchantedFurnaceBlockEntity furnace) {
        ItemStack input = furnace.getStack(0);
        Item dustResult = !input.isEmpty() ? getDustSmeltingResult(input.getItem()) : null;

        if (dustResult != null) {
            ItemStack output = furnace.getStack(2);
            boolean canOutput = output.isEmpty() || (output.isOf(dustResult) && output.getCount() < output.getMaxCount());

            if (canOutput) {
                int burnTime = furnace.propertyDelegate.get(0);
                int cookTime = furnace.propertyDelegate.get(2);
                int cookTotal = 200;
                furnace.propertyDelegate.set(3, cookTotal);

                // Ignite fuel if furnace isn't lit
                if (burnTime <= 0) {
                    ItemStack fuel = furnace.getStack(1);
                    int fuelBurn = getFuelBurnTime(world, fuel);
                    if (fuelBurn > 0) {
                        furnace.propertyDelegate.set(0, fuelBurn);
                        furnace.propertyDelegate.set(1, fuelBurn);
                        ItemStack remainder = fuel.getRecipeRemainder();
                        fuel.decrement(1);
                        if (fuel.isEmpty() && !remainder.isEmpty()) {
                            furnace.setStack(1, remainder.copy());
                        }
                        markDirty(world, pos, state);
                    }
                }

                // If lit, cook at 3x speed!
                burnTime = furnace.propertyDelegate.get(0);
                if (burnTime > 0) {
                    furnace.propertyDelegate.set(0, Math.max(0, burnTime - 1));
                    cookTime += 3; // 3x speed!
                    if (cookTime >= cookTotal) {
                        cookTime = 0;
                        input.decrement(1);
                        if (output.isEmpty()) {
                            furnace.setStack(2, new ItemStack(dustResult, 1));
                        } else {
                            output.increment(1);
                        }
                    }
                    furnace.propertyDelegate.set(2, cookTime);
                    markDirty(world, pos, state);
                }
                return;
            }
        }

        // Default vanilla smelting tick
        AbstractFurnaceBlockEntity.tick(world, pos, state, furnace);

        // 3x Smelting Speed for vanilla recipes
        int litTimeRemaining = furnace.propertyDelegate.get(0);
        int cookingTimeSpent = furnace.propertyDelegate.get(2);
        int cookingTotalTime = furnace.propertyDelegate.get(3);

        if (litTimeRemaining > 0 && cookingTimeSpent > 0 && cookingTimeSpent < cookingTotalTime) {
            int newCookTime = Math.min(cookingTotalTime - 1, cookingTimeSpent + 2);
            furnace.propertyDelegate.set(2, newCookTime);
        }
    }
}

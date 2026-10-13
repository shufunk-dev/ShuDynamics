package net.enchantedwood.mixin;

import net.enchantedwood.block.ModBlocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreenHandler.class)
public abstract class CraftingScreenHandlerMixin {

    @Inject(method = "updateResult", at = @At("TAIL"))
    private static void enforceAnomalyKeystoneCraftingLocks(
            ScreenHandler handler,
            ServerWorld world,
            PlayerEntity player,
            RecipeInputInventory craftingInventory,
            CraftingResultInventory resultInventory,
            RecipeEntry<CraftingRecipe> recipe,
            CallbackInfo ci
    ) {
        if (player == null) return;
        ItemStack result = resultInventory.getStack(0);
        if (result.isEmpty()) return;

        // 1. Keystone #2: Kinetic Anchor (Locked until ATV is constructed and driven)
        if (result.isOf(ModBlocks.KINETIC_ANCHOR.asItem())) {
            boolean hasUnlockedAtv = player.getCommandTags().contains("unlocked_kinetic_anchor")
                    || player.getInventory().contains(new ItemStack(net.enchantedwood.item.ModItems.ATV_ITEM));
            if (!hasUnlockedAtv) {
                resultInventory.setStack(0, ItemStack.EMPTY);
                player.sendMessage(
                        Text.literal("§c🔒 Anomaly Keystone #2 is locked! Build and ride an ATV first."),
                        true
                );
            }
        }
        // 2. Keystone #1: Atmospheric Anchor (Locked until Scuba Gear ocean depth exploration)
        else if (result.isOf(ModBlocks.ATMOSPHERIC_ANCHOR.asItem())) {
            if (!player.getCommandTags().contains("unlocked_atmospheric_anchor")) {
                resultInventory.setStack(0, ItemStack.EMPTY);
                player.sendMessage(
                        Text.literal("§c🔒 Anomaly Keystone #1 is locked! Explore ocean depths in Scuba Gear first."),
                        true
                );
            }
        }
    }
}

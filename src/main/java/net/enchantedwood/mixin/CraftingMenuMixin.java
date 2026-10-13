package net.enchantedwood.mixin;

import net.enchantedwood.block.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
    private static void enforceAnomalyKeystoneCraftingLocks(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer craftSlots,
            ResultContainer resultSlots,
            RecipeHolder<CraftingRecipe> recipe,
            CallbackInfo ci
    ) {
        if (player == null) return;
        ItemStack result = resultSlots.getItem(0);
        if (result.isEmpty()) return;

        // 1. Keystone #1: Atmospheric Anchor (Locked until Scuba Gear ocean depth exploration)
        if (result.is(ModBlocks.ATMOSPHERIC_ANCHOR.asItem())) {
            if (!player.entityTags().contains("unlocked_atmospheric_anchor")) {
                resultSlots.setItem(0, ItemStack.EMPTY);
                player.sendOverlayMessage(
                        Component.literal("§c🔒 Anomaly Keystone #1 is locked! Explore ocean depths in Scuba Gear first.")
                );
            }
        }
        // 2. Keystone #2: Kinetic Anchor (Locked until ATV is constructed and driven)
        else if (result.is(ModBlocks.KINETIC_ANCHOR.asItem())) {
            boolean hasUnlockedAtv = player.entityTags().contains("unlocked_kinetic_anchor")
                    || player.getInventory().contains(new ItemStack(net.enchantedwood.item.ModItems.ATV_ITEM));
            if (!hasUnlockedAtv) {
                resultSlots.setItem(0, ItemStack.EMPTY);
                player.sendOverlayMessage(
                        Component.literal("§c🔒 Anomaly Keystone #2 is locked! Build and ride an ATV first.")
                );
            }
        }
        // 3. Keystone #3: Thermal Anchor (Locked until forging Fire Crystal and bringing it to the Nether)
        else if (result.is(ModBlocks.THERMAL_ANCHOR.asItem())) {
            if (!player.entityTags().contains("unlocked_thermal_anchor")) {
                resultSlots.setItem(0, ItemStack.EMPTY);
                player.sendOverlayMessage(
                        Component.literal("§c🔒 Anomaly Keystone #3 is locked! Forge a Fire Crystal and bring it into the Nether first.")
                );
            }
        }
        // 4. Keystone #4: Metallurgical Anchor (Locked until smelting Manyullyn Alloy)
        else if (result.is(ModBlocks.METALLURGICAL_ANCHOR.asItem())) {
            boolean hasUnlockedManyullyn = player.entityTags().contains("unlocked_metallurgical_anchor")
                    || player.getInventory().contains(new ItemStack(net.enchantedwood.item.ModItems.MANYULLYN_INGOT));
            if (!hasUnlockedManyullyn) {
                resultSlots.setItem(0, ItemStack.EMPTY);
                player.sendOverlayMessage(
                        Component.literal("§c🔒 Anomaly Keystone #4 is locked! Smelt Cobalt and Ardite into Manyullyn Alloy first.")
                );
            }
        }
        // 5. Keystone #5: Plasma Anchor (Locked until witnessing Solar Plasma Wave or acquiring a Nether Star)
        else if (result.is(ModBlocks.PLASMA_ANCHOR.asItem())) {
            boolean hasUnlockedPlasma = player.entityTags().contains("unlocked_plasma_anchor")
                    || player.entityTags().contains("sd_anomaly_plasma")
                    || player.getInventory().contains(new ItemStack(Items.NETHER_STAR));
            if (!hasUnlockedPlasma) {
                resultSlots.setItem(0, ItemStack.EMPTY);
                player.sendOverlayMessage(
                        Component.literal("§c🔒 Anomaly Keystone #5 is locked! Harness energized Solar Plasma in the Nether first.")
                );
            }
        }
        // Keystone #6: Dimensional Singularity is deliberately unlocked/unrestricted as per specifications
    }
}

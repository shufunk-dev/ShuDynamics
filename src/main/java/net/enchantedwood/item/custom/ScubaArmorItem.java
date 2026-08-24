package net.enchantedwood.item.custom;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.item.ModItems;

import java.util.List;
import java.util.function.Consumer;

public class ScubaArmorItem extends Item {
    private final EquipmentSlot expectedSlot;

    public ScubaArmorItem(EquipmentSlot slot, Settings settings) {
        super(settings);
        this.expectedSlot = slot;
    }

    public EquipmentSlot getExpectedSlot() {
        return this.expectedSlot;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        if (entity instanceof ServerPlayerEntity player) {
            // Check for full suit unlock event
            checkAndTriggerAnomalyUnlock(player, world);

            if (slot == this.expectedSlot) {
                boolean inWater = player.isSubmergedIn(FluidTags.WATER) || player.isTouchingWater();

                switch (this.expectedSlot) {
                    case HEAD -> {
                        // Diving Mask: Clear underwater sight & fast mining
                        if (inWater) {
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 40, 0, false, false, true));
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 220, 0, false, false, false));
                        }
                    }
                    case CHEST -> {
                        // Scuba Tank: Infinite underwater breathing
                        if (inWater || player.getAir() < player.getMaxAir()) {
                            player.setAir(player.getMaxAir());
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 40, 0, false, false, true));
                        }
                    }
                    case LEGS -> {
                        // Wetsuit: Dolphin's Grace streamlined swimming
                        if (inWater) {
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 40, 0, false, false, false));
                        }
                    }
                    case FEET -> {
                        // Diving Flippers: Ocean speed boost
                        if (inWater) {
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 1, false, false, false));
                        }
                    }
                    default -> {}
                }
            }
        }
        super.inventoryTick(stack, world, entity, slot);
    }

    private void checkAndTriggerAnomalyUnlock(ServerPlayerEntity player, ServerWorld world) {
        ItemStack head = player.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack legs = player.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack feet = player.getEquippedStack(EquipmentSlot.FEET);

        boolean wearingFullSet = head.isOf(ModItems.DIVING_MASK)
                && chest.isOf(ModItems.SCUBA_CHESTPLATE)
                && legs.isOf(ModItems.WETSUIT_LEGGINGS)
                && feet.isOf(ModItems.DIVING_FLIPPERS);

        // Also check if player has all 4 pieces across their inventory
        boolean hasAllPieces = wearingFullSet || (player.getInventory().contains(new ItemStack(ModItems.DIVING_MASK))
                && player.getInventory().contains(new ItemStack(ModItems.SCUBA_CHESTPLATE))
                && player.getInventory().contains(new ItemStack(ModItems.WETSUIT_LEGGINGS))
                && player.getInventory().contains(new ItemStack(ModItems.DIVING_FLIPPERS)));

        if (hasAllPieces) {
            if (player.addCommandTag("unlocked_atmospheric_anchor")) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.8f, 1.4f);

                player.sendMessage(Text.literal(""), false);
                player.sendMessage(Text.literal("§5✦ §d§l[DIMENSIONAL RESONANCE DETECTED] §5✦"), false);
                player.sendMessage(Text.literal("§fBy mastering deep-sea atmospheric pressure, you have unlocked the blueprint for:"), false);
                player.sendMessage(Text.literal("§b⚙ §e§lAnomaly Keystone #1: §bAtmospheric Anchor"), false);
                player.sendMessage(Text.literal("§8(Craft with Rubber, Oxygen Canister, Infused Heartwood & Crying Obsidian)"), false);
                player.sendMessage(Text.literal(""), false);

                player.sendMessage(Text.literal("§a✔ Anomaly Keystone #1 Unlocked: Atmospheric Anchor"), true);

                try {
                    player.unlockRecipes(List.of(RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(EnchantedWoodMod.MOD_ID, "atmospheric_anchor"))));
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        switch (this.expectedSlot) {
            case HEAD -> {
                textConsumer.accept(Text.literal("§3✦ Conduit Vision: §7Clear sight & fast underwater mining"));
            }
            case CHEST -> {
                textConsumer.accept(Text.literal("§b✦ Pressurized Oxygen: §7Infinite underwater breathing"));
            }
            case LEGS -> {
                textConsumer.accept(Text.literal("§a✦ Streamlined Polymer: §7Grants Dolphin's Grace swimming"));
            }
            case FEET -> {
                textConsumer.accept(Text.literal("§6✦ Hydrodynamic Flippers: §7High speed water propulsion"));
            }
            default -> {}
        }
        textConsumer.accept(Text.literal("§8Crafted with vulcanized rubber and life support"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}

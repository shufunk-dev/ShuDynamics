package net.enchantedwood.item.custom;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.List;
import java.util.function.Consumer;

public class ScubaArmorItem extends Item {
    private final EquipmentSlot expectedSlot;

    public ScubaArmorItem(EquipmentSlot slot, Properties settings) {
        super(settings);
        this.expectedSlot = slot;
    }

    public EquipmentSlot getExpectedSlot() {
        return this.expectedSlot;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        if (entity instanceof ServerPlayer player) {
            // Check for full suit unlock event
            checkAndTriggerAnomalyUnlock(player, world);

            if (slot == this.expectedSlot) {
                boolean inWater = player.isEyeInFluid(FluidTags.WATER) || player.isInWater();

                switch (this.expectedSlot) {
                    case HEAD -> {
                        // Diving Mask: Clear underwater sight & fast mining
                        if (inWater) {
                            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 40, 0, false, false, true));
                            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, false));
                        }
                    }
                    case CHEST -> {
                        // Scuba Tank: Infinite underwater breathing
                        if (inWater || player.getAirSupply() < player.getMaxAirSupply()) {
                            player.setAirSupply(player.getMaxAirSupply());
                            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, false, false, true));
                        }
                    }
                    case LEGS -> {
                        // Wetsuit: Dolphin's Grace streamlined swimming
                        if (inWater) {
                            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, false, false, false));
                        }
                    }
                    case FEET -> {
                        // Diving Flippers: Ocean speed boost
                        if (inWater) {
                            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1, false, false, false));
                        }
                    }
                    default -> {}
                }
            }
        }
        super.inventoryTick(stack, world, entity, slot);
    }

    private void checkAndTriggerAnomalyUnlock(ServerPlayer player, ServerLevel world) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);

        boolean wearingFullSet = head.is(ModItems.DIVING_MASK)
                && chest.is(ModItems.SCUBA_CHESTPLATE)
                && legs.is(ModItems.WETSUIT_LEGGINGS)
                && feet.is(ModItems.DIVING_FLIPPERS);

        // Also check if player has all 4 pieces across their inventory
        boolean hasAllPieces = wearingFullSet || (player.getInventory().contains(new ItemStack(ModItems.DIVING_MASK))
                && player.getInventory().contains(new ItemStack(ModItems.SCUBA_CHESTPLATE))
                && player.getInventory().contains(new ItemStack(ModItems.WETSUIT_LEGGINGS))
                && player.getInventory().contains(new ItemStack(ModItems.DIVING_FLIPPERS)));

        if (hasAllPieces) {
            if (player.addTag("unlocked_atmospheric_anchor")) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 0.8f, 1.4f);

                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("§5✦ §d§l[DIMENSIONAL RESONANCE DETECTED] §5✦"));
                player.sendSystemMessage(Component.literal("§fBy mastering deep-sea atmospheric pressure, you have unlocked the blueprint for:"));
                player.sendSystemMessage(Component.literal("§b⚙ §e§lAnomaly Keystone #1: §bAtmospheric Anchor"));
                player.sendSystemMessage(Component.literal("§8(Craft with Rubber, Oxygen Canister, Infused Heartwood & Crying Obsidian)"));
                player.sendSystemMessage(Component.literal(""));

                player.sendSystemMessage(Component.literal("§a✔ Anomaly Keystone #1 Unlocked: Atmospheric Anchor"));

                try {
                    player.awardRecipesByKey(List.of(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "atmospheric_anchor"))));
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        switch (this.expectedSlot) {
            case HEAD -> {
                textConsumer.accept(Component.literal("§3✦ Conduit Vision: §7Clear sight & fast underwater mining"));
            }
            case CHEST -> {
                textConsumer.accept(Component.literal("§b✦ Pressurized Oxygen: §7Infinite underwater breathing"));
            }
            case LEGS -> {
                textConsumer.accept(Component.literal("§a✦ Streamlined Polymer: §7Grants Dolphin's Grace swimming"));
            }
            case FEET -> {
                textConsumer.accept(Component.literal("§6✦ Hydrodynamic Flippers: §7High speed water propulsion"));
            }
            default -> {}
        }
        textConsumer.accept(Component.literal("§8Crafted with vulcanized rubber and life support"));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}

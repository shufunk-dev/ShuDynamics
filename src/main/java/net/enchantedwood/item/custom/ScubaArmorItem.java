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
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

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
        if (slot == this.expectedSlot && entity instanceof PlayerEntity player) {
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
        super.inventoryTick(stack, world, entity, slot);
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

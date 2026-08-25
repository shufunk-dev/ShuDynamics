package net.enchantedwood.item.custom;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.function.Consumer;

public class SnorkelItem extends Item {
    private final EquipmentSlot expectedSlot;

    public SnorkelItem(EquipmentSlot slot, Settings settings) {
        super(settings);
        this.expectedSlot = slot;
    }

    public EquipmentSlot getExpectedSlot() {
        return this.expectedSlot;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        if (entity instanceof ServerPlayerEntity player && slot == EquipmentSlot.HEAD) {
            boolean inWater = player.isSubmergedIn(FluidTags.WATER) || player.isTouchingWater();

            if (inWater) {
                BlockPos eyePos = BlockPos.ofFloored(player.getEyePos());
                BlockPos abovePos = eyePos.up();

                // Surface check: if the space directly above head has air or is not water fluid, snorkel can breathe surface air
                boolean surfaceAccessible = !world.getFluidState(abovePos).isIn(FluidTags.WATER)
                        || !player.isSubmergedIn(FluidTags.WATER)
                        || world.getBlockState(abovePos).isAir();

                if (surfaceAccessible) {
                    if (player.getAir() < player.getMaxAir()) {
                        player.setAir(player.getMaxAir());
                    }
                } else if (player.isSubmergedIn(FluidTags.WATER) && player.getAir() > 0) {
                    // Diving below surface: slows air depletion rate to 1/3 (triples dive breath duration)
                    if (world.getTime() % 3 != 0 && player.getAir() < player.getMaxAir()) {
                        player.setAir(player.getAir() + 1);
                    }
                }
            }
        }
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§3✦ Surface Snorkel: §7Breathe freely when near the water surface"));
        textConsumer.accept(Text.literal("§b✦ Extended Lungs: §7Triples underwater dive breath duration"));
        textConsumer.accept(Text.literal("§8Compatible with Wetsuit Leggings & Flippers"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}

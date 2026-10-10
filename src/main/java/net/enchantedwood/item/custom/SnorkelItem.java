package net.enchantedwood.item.custom;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class SnorkelItem extends Item {
    private final EquipmentSlot expectedSlot;

    public SnorkelItem(EquipmentSlot slot, Properties settings) {
        super(settings);
        this.expectedSlot = slot;
    }

    public EquipmentSlot getExpectedSlot() {
        return this.expectedSlot;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        if (entity instanceof ServerPlayer player && slot == EquipmentSlot.HEAD) {
            boolean inWater = player.isEyeInFluid(FluidTags.WATER) || player.isInWater();

            if (inWater) {
                BlockPos eyePos = BlockPos.containing(player.getEyePosition());
                BlockPos abovePos = eyePos.above();

                // Surface check: if the space directly above head has air or is not water fluid, snorkel can breathe surface air
                boolean surfaceAccessible = !world.getFluidState(abovePos).is(FluidTags.WATER)
                        || !player.isEyeInFluid(FluidTags.WATER)
                        || world.getBlockState(abovePos).isAir();

                if (surfaceAccessible) {
                    if (player.getAirSupply() < player.getMaxAirSupply()) {
                        player.setAirSupply(player.getMaxAirSupply());
                    }
                } else if (player.isEyeInFluid(FluidTags.WATER) && player.getAirSupply() > 0) {
                    // Diving below surface: slows air depletion rate to 1/3 (triples dive breath duration)
                    if (world.getGameTime() % 3 != 0 && player.getAirSupply() < player.getMaxAirSupply()) {
                        player.setAirSupply(player.getAirSupply() + 1);
                    }
                }
            }
        }
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§3✦ Surface Snorkel: §7Breathe freely when near the water surface"));
        textConsumer.accept(Component.literal("§b✦ Extended Lungs: §7Triples underwater dive breath duration"));
        textConsumer.accept(Component.literal("§8Compatible with Wetsuit Leggings & Flippers"));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}

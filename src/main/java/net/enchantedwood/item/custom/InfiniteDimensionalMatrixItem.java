package net.enchantedwood.item.custom;

import net.enchantedwood.energy.EnergyStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class InfiniteDimensionalMatrixItem extends BatteryItem {
    public static final int INFINITE_CAPACITY = 10_000_000;

    public InfiniteDimensionalMatrixItem(Properties settings) {
        super(settings.stacksTo(1), INFINITE_CAPACITY, INFINITE_CAPACITY, INFINITE_CAPACITY);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false; // Endless energy, no depletion bar needed
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(ItemStack stack) {
        return new EnergyStorage() {
            @Override
            public int getEnergy() {
                return INFINITE_CAPACITY;
            }

            @Override
            public int getMaxEnergy() {
                return INFINITE_CAPACITY;
            }

            @Override
            public int insertEnergy(int maxReceiveAmount, boolean simulate) {
                return maxReceiveAmount; // Absorb any incoming energy harmlessly
            }

            @Override
            public int extractEnergy(int maxExtractAmount, boolean simulate) {
                // Unlimited extraction — never depletes!
                return maxExtractAmount;
            }

            @Override
            public boolean canExtract() {
                return true;
            }

            @Override
            public boolean canInsert() {
                return false;
            }

            @Override
            public int getTransferRate() {
                return 100_000; // Superluminal flux transfer: up to 100,000 FE/t
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§5✦ Apex Relic of The Cataclysm ✦"));
        textConsumer.accept(Component.literal("§b✦ Infinite Dimensional Matrix ✦"));
        textConsumer.accept(Component.literal("§7Boundless crystalline lattice pulsing with unlimited dimensional flux."));
        textConsumer.accept(Component.literal("§e⚡ Energy Output: §a∞ Infinite FE"));
        textConsumer.accept(Component.literal("§d✦ Machine & Grid Power:"));
        textConsumer.accept(Component.literal("§8 • Place in any Battery Block discharge slot to power your base endlessly."));
        textConsumer.accept(Component.literal("§8 • Powers Road Pavers, Centrifuges & Synthesizers with zero generators."));
        textConsumer.accept(Component.literal("§d✦ Modular Suit Power:"));
        textConsumer.accept(Component.literal("§8 • Slot into any Modular Power Armor piece for limitless chassis energy."));
        textConsumer.accept(Component.literal("§8 • Equip in all 4 suit pieces (Requires 4 Cataclysm kills!) for complete God-mode power."));
    }
}

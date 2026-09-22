package net.enchantedwood.item.custom;

import net.enchantedwood.energy.EnergyStorage;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class InfiniteDimensionalMatrixItem extends BatteryItem {
    public static final int INFINITE_CAPACITY = 10_000_000;

    public InfiniteDimensionalMatrixItem(Settings settings) {
        super(settings.maxCount(1), INFINITE_CAPACITY, INFINITE_CAPACITY, INFINITE_CAPACITY);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
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
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§5✦ Apex Relic of The Cataclysm ✦"));
        textConsumer.accept(Text.literal("§b✦ Infinite Dimensional Matrix ✦"));
        textConsumer.accept(Text.literal("§7Boundless crystalline lattice pulsing with unlimited dimensional flux."));
        textConsumer.accept(Text.literal("§e⚡ Energy Output: §a∞ Infinite FE"));
        textConsumer.accept(Text.literal("§d✦ Machine & Grid Power:"));
        textConsumer.accept(Text.literal("§8 • Place in any Battery Block discharge slot to power your base endlessly."));
        textConsumer.accept(Text.literal("§8 • Powers Road Pavers, Centrifuges & Synthesizers with zero generators."));
        textConsumer.accept(Text.literal("§d✦ Modular Suit Power:"));
        textConsumer.accept(Text.literal("§8 • Slot into any Modular Power Armor piece for limitless chassis energy."));
        textConsumer.accept(Text.literal("§8 • Equip in all 4 suit pieces (Requires 4 Cataclysm kills!) for complete God-mode power."));
    }
}

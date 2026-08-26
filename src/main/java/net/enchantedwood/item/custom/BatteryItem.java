package net.enchantedwood.item.custom;

import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.ItemEnergyProvider;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class BatteryItem extends Item implements ItemEnergyProvider, EnergyProvider {
    private final int capacity;
    private final int maxReceive;
    private final int maxExtract;

    public BatteryItem(Settings settings, int capacity, int maxReceive, int maxExtract) {
        super(settings.maxCount(1));
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    public static int getStoredEnergy(ItemStack stack) {
        if (stack.contains(DataComponentTypes.CUSTOM_DATA)) {
            NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (nbtComponent != null) {
                return nbtComponent.copyNbt().getInt("Energy", 0);
            }
        }
        return 0;
    }

    public static void setStoredEnergy(ItemStack stack, int energy) {
        NbtCompound nbt = stack.contains(DataComponentTypes.CUSTOM_DATA) && stack.get(DataComponentTypes.CUSTOM_DATA) != null
                ? stack.get(DataComponentTypes.CUSTOM_DATA).copyNbt()
                : new NbtCompound();
        nbt.putInt("Energy", Math.max(0, energy));
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        int energy = getStoredEnergy(stack);
        return Math.round((float) energy * 13.0f / (float) this.capacity);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        float f = Math.max(0.0f, (float) getStoredEnergy(stack) / (float) this.capacity);
        return MathHelper.hsvToRgb(f / 3.0f, 1.0f, 1.0f);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(ItemStack stack) {
        return new EnergyStorage() {
            @Override
            public int getEnergy() {
                return getStoredEnergy(stack);
            }

            @Override
            public int getMaxEnergy() {
                return capacity;
            }

            @Override
            public int insertEnergy(int maxReceiveAmount, boolean simulate) {
                int stored = getStoredEnergy(stack);
                int toInsert = Math.min(capacity - stored, Math.min(maxReceive, maxReceiveAmount));
                if (!simulate && toInsert > 0) {
                    setStoredEnergy(stack, stored + toInsert);
                }
                return toInsert;
            }

            @Override
            public int extractEnergy(int maxExtractAmount, boolean simulate) {
                int stored = getStoredEnergy(stack);
                int toExtract = Math.min(stored, Math.min(maxExtract, maxExtractAmount));
                if (!simulate && toExtract > 0) {
                    setStoredEnergy(stack, stored - toExtract);
                }
                return toExtract;
            }

            @Override
            public boolean canExtract() {
                return true;
            }

            @Override
            public boolean canInsert() {
                return true;
            }

            @Override
            public int getTransferRate() {
                return maxExtract;
            }
        };
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return null;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        int energy = getStoredEnergy(stack);
        int percent = (int) (((long) energy * 100) / this.capacity);
        textConsumer.accept(Text.literal(String.format("§b⚡ Energy: §f%,d / %,d FE §7(%d%%)", energy, this.capacity, percent)));
        textConsumer.accept(Text.literal("§8Right-click any Battery Block or Generator to recharge."));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}

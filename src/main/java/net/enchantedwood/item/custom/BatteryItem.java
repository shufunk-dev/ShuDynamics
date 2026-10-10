package net.enchantedwood.item.custom;

import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.ItemEnergyProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class BatteryItem extends Item implements ItemEnergyProvider, EnergyProvider {
    private final int capacity;
    private final int maxReceive;
    private final int maxExtract;

    public BatteryItem(Properties settings, int capacity, int maxReceive, int maxExtract) {
        super(settings.stacksTo(1));
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    public static int getStoredEnergy(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData nbtComponent = stack.get(DataComponents.CUSTOM_DATA);
            if (nbtComponent != null) {
                return nbtComponent.copyTag().getIntOr("Energy", 0);
            }
        }
        return 0;
    }

    public static void setStoredEnergy(ItemStack stack, int energy) {
        CompoundTag nbt = stack.has(DataComponents.CUSTOM_DATA) && stack.get(DataComponents.CUSTOM_DATA) != null
                ? stack.get(DataComponents.CUSTOM_DATA).copyTag()
                : new CompoundTag();
        nbt.putInt("Energy", Math.max(0, energy));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int energy = getStoredEnergy(stack);
        return Math.round((float) energy * 13.0f / (float) this.capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = Math.max(0.0f, (float) getStoredEnergy(stack) / (float) this.capacity);
        return Mth.hsvToRgb(f / 3.0f, 1.0f, 1.0f);
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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        int energy = getStoredEnergy(stack);
        int percent = (int) (((long) energy * 100) / this.capacity);
        textConsumer.accept(Component.literal(String.format("§b⚡ Energy: §f%,d / %,d FE §7(%d%%)", energy, this.capacity, percent)));
        textConsumer.accept(Component.literal("§8Right-click any Battery Block or Generator to recharge."));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}

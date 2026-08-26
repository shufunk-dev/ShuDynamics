package net.enchantedwood.energy;

import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface ItemEnergyProvider {
    @Nullable
    EnergyStorage getEnergyStorage(ItemStack stack);
}

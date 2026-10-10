package net.enchantedwood.gas;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public interface GasProvider {
    @Nullable
    GasStorage getGasStorage(@Nullable Direction side);
}

package net.enchantedwood.gas;

import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface GasProvider {
    @Nullable
    GasStorage getGasStorage(@Nullable Direction side);
}

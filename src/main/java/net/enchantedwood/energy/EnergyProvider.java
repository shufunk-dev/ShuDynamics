package net.enchantedwood.energy;

import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Interface implemented by BlockEntities that provide or receive energy on one or more sides.
 */
public interface EnergyProvider {
    /**
     * @param side the direction from which energy is being accessed, or null for internal/all.
     * @return the EnergyStorage instance, or null if energy is not supported on that side.
     */
    @Nullable
    EnergyStorage getEnergyStorage(@Nullable Direction side);
}

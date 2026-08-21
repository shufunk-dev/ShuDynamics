package net.enchantedwood.energy;

import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Clean energy storage interface (FE/RF compatible).
 */
public interface EnergyStorage {
    /**
     * @return current energy stored in FE.
     */
    int getEnergy();

    /**
     * @return maximum energy capacity in FE.
     */
    int getMaxEnergy();

    /**
     * Inserts energy into the storage.
     * @param amount amount of FE to insert.
     * @param simulate if true, the insertion is only simulated without mutating state.
     * @return amount of FE successfully inserted.
     */
    int insertEnergy(int amount, boolean simulate);

    /**
     * Extracts energy from the storage.
     * @param amount amount of FE to extract.
     * @param simulate if true, the extraction is only simulated without mutating state.
     * @return amount of FE successfully extracted.
     */
    int extractEnergy(int amount, boolean simulate);

    /**
     * @return true if energy can be extracted from this storage.
     */
    boolean canExtract();

    /**
     * @return true if energy can be inserted into this storage.
     */
    boolean canInsert();

    /**
     * @return maximum transfer rate in FE/t for I/O operations.
     */
    int getTransferRate();
}

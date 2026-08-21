package net.enchantedwood.gas;

public interface GasStorage {
    GasType getGasType();
    int getAmount();
    int getCapacity();
    int insertGas(GasType type, int amount, boolean simulate);
    int extractGas(GasType type, int amount, boolean simulate);
    boolean canExtract(GasType type);
    boolean canInsert(GasType type);
}

package net.enchantedwood.fluid;

public interface LavaProvider {
    int getLavaAmount();
    int getMaxLava();
    int insertLava(int amount, boolean simulate);
    int extractLava(int amount, boolean simulate);

    default boolean canInsertLava() {
        return getLavaAmount() < getMaxLava();
    }

    default boolean canExtractLava() {
        return getLavaAmount() > 0;
    }
}

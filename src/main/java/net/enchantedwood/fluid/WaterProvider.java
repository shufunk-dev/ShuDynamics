package net.enchantedwood.fluid;

public interface WaterProvider {
    int getWaterAmount();
    int getMaxWater();
    int insertWater(int amount, boolean simulate);
    int extractWater(int amount, boolean simulate);

    default boolean canInsertWater() {
        return getWaterAmount() < getMaxWater();
    }

    default boolean canExtractWater() {
        return getWaterAmount() > 0;
    }
}

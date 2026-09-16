package net.enchantedwood.fluid;

import java.util.List;

public interface MoltenMetalProvider {
    MoltenMetal getFluidType();
    int getFluidAmount(MoltenMetal metal);
    int getMaxFluid();
    int insertFluid(MoltenMetal metal, int amount, boolean simulate);
    int extractFluid(MoltenMetal metal, int amount, boolean simulate);

    default boolean canInsertFluid(MoltenMetal metal) {
        return getFluidAmount(metal) < getMaxFluid();
    }

    default boolean canExtractFluid(MoltenMetal metal) {
        return getFluidAmount(metal) > 0;
    }

    /**
     * Checks if this provider is specifically dedicated to this metal (e.g. already contains it or has a filter).
     */
    default boolean isDedicatedTo(MoltenMetal metal) {
        return getFluidAmount(metal) > 0;
    }

    /**
     * For multi-fluid chambers like the Induction Smelter, returns all currently present molten metals.
     */
    default List<MoltenMetal> getContainedFluids() {
        MoltenMetal type = getFluidType();
        if (type != null && type != MoltenMetal.NONE && getFluidAmount(type) > 0) {
            return List.of(type);
        }
        return List.of();
    }
}

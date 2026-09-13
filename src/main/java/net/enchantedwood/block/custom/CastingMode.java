package net.enchantedwood.block.custom;

import net.minecraft.util.StringIdentifiable;

public enum CastingMode implements StringIdentifiable {
    INGOT("ingot", "Ingot", 90),
    BLOCK("block", "Block", 810),
    NUGGET("nugget", "Nugget", 10);

    private final String id;
    private final String displayName;
    private final int fluidCostMb;

    CastingMode(String id, String displayName, int fluidCostMb) {
        this.id = id;
        this.displayName = displayName;
        this.fluidCostMb = fluidCostMb;
    }

    @Override
    public String asString() {
        return this.id;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getFluidCostMb() {
        return this.fluidCostMb;
    }

    public CastingMode next() {
        return switch (this) {
            case INGOT -> BLOCK;
            case BLOCK -> NUGGET;
            case NUGGET -> INGOT;
        };
    }
}

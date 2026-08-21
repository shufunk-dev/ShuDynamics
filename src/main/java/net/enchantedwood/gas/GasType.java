package net.enchantedwood.gas;

import net.minecraft.util.StringIdentifiable;

public enum GasType implements StringIdentifiable {
    NONE("none", 0x7E7E7E),
    OXYGEN("oxygen", 0x4AA0E8),
    HYDROGEN("hydrogen", 0xE87E3A);

    private final String name;
    private final int color;

    GasType(String name, int color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String asString() {
        return this.name;
    }

    public int getColor() {
        return this.color;
    }

    public String getDisplayName() {
        return switch (this) {
            case OXYGEN -> "Oxygen (O₂)";
            case HYDROGEN -> "Hydrogen (H₂)";
            default -> "None";
        };
    }
}

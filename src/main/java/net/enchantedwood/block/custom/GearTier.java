package net.enchantedwood.block.custom;

import net.minecraft.util.StringIdentifiable;

public enum GearTier implements StringIdentifiable {
    NONE("none", 2),
    IRON("iron", 2),
    ENCHANTED_IRON("enchanted_iron", 3),
    COPPER("copper", 3),
    BRONZE("bronze", 3),
    ALUMINUM("aluminum", 4),
    STEEL("steel", 4),
    GOLD("gold", 4),
    TITANIUM("titanium", 5),
    DIAMOND("diamond", 5),
    NETHERITE("netherite", 6),
    BLAZE_OVERCLOCK("blaze_overclock", 8);

    private final String name;
    private final int baseOreYield;

    GearTier(String name, int baseOreYield) {
        this.name = name;
        this.baseOreYield = baseOreYield;
    }

    public int getBaseOreYield() {
        return this.baseOreYield;
    }

    public int getMultiplier() {
        return this.baseOreYield;
    }

    @Override
    public String asString() {
        return this.name;
    }

    public static GearTier fromString(String name) {
        for (GearTier tier : values()) {
            if (tier.name.equalsIgnoreCase(name)) {
                return tier;
            }
        }
        return NONE;
    }
}

package net.enchantedwood.block.custom;

import net.minecraft.util.StringIdentifiable;

public enum PipeSide implements StringIdentifiable {
    NONE("none"),
    CONNECTED("connected"),
    DISCONNECTED("disconnected");

    private final String name;

    PipeSide(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return this.name;
    }

    public boolean isConnected() {
        return this == CONNECTED;
    }

    public boolean isDisconnected() {
        return this == DISCONNECTED;
    }
}

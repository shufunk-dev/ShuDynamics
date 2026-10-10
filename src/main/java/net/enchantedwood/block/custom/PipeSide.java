package net.enchantedwood.block.custom;

import net.minecraft.util.StringRepresentable;

public enum PipeSide implements StringRepresentable {
    NONE("none"),
    CONNECTED("connected"),
    DISCONNECTED("disconnected");

    private final String name;

    PipeSide(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public boolean isConnected() {
        return this == CONNECTED;
    }

    public boolean isDisconnected() {
        return this == DISCONNECTED;
    }
}

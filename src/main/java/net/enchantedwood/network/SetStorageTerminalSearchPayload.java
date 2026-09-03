package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SetStorageTerminalSearchPayload(String query) implements CustomPayload {
    public static final Id<SetStorageTerminalSearchPayload> ID = new Id<>(Identifier.of(EnchantedWoodMod.MOD_ID, "set_storage_terminal_search"));
    public static final PacketCodec<RegistryByteBuf, SetStorageTerminalSearchPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, SetStorageTerminalSearchPayload::query,
            SetStorageTerminalSearchPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

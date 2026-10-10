package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetStorageTerminalSearchPayload(String query) implements CustomPacketPayload {
    public static final Type<SetStorageTerminalSearchPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "set_storage_terminal_search"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetStorageTerminalSearchPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetStorageTerminalSearchPayload::query,
            SetStorageTerminalSearchPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

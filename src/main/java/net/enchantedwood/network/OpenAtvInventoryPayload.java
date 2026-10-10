package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenAtvInventoryPayload() implements CustomPacketPayload {
    public static final Type<OpenAtvInventoryPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "open_atv_inventory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAtvInventoryPayload> CODEC = StreamCodec.unit(new OpenAtvInventoryPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

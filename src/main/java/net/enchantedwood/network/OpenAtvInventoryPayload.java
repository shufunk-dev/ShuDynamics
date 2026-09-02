package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenAtvInventoryPayload() implements CustomPayload {
    public static final Id<OpenAtvInventoryPayload> ID = new Id<>(Identifier.of(EnchantedWoodMod.MOD_ID, "open_atv_inventory"));
    public static final PacketCodec<RegistryByteBuf, OpenAtvInventoryPayload> CODEC = PacketCodec.unit(new OpenAtvInventoryPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

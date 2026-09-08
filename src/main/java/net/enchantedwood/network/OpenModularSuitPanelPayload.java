package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenModularSuitPanelPayload() implements CustomPayload {
    public static final Id<OpenModularSuitPanelPayload> ID = new Id<>(Identifier.of(EnchantedWoodMod.MOD_ID, "open_modular_suit_panel"));
    public static final PacketCodec<RegistryByteBuf, OpenModularSuitPanelPayload> CODEC = PacketCodec.unit(new OpenModularSuitPanelPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

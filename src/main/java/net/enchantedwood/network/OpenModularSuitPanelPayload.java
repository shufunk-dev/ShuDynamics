package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenModularSuitPanelPayload() implements CustomPacketPayload {
    public static final Type<OpenModularSuitPanelPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "open_modular_suit_panel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenModularSuitPanelPayload> CODEC = StreamCodec.unit(new OpenModularSuitPanelPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

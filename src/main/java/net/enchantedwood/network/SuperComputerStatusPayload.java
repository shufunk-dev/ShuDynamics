package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SuperComputerStatusPayload(String message) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SuperComputerStatusPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "super_computer_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SuperComputerStatusPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    SuperComputerStatusPayload::message,
                    SuperComputerStatusPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

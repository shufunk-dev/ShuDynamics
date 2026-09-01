package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SuperComputerStatusPayload(String message) implements CustomPayload {
    public static final CustomPayload.Id<SuperComputerStatusPayload> ID =
            new CustomPayload.Id<>(Identifier.of(EnchantedWoodMod.MOD_ID, "super_computer_status"));

    public static final PacketCodec<RegistryByteBuf, SuperComputerStatusPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING,
                    SuperComputerStatusPayload::message,
                    SuperComputerStatusPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ToggleCastingPortModePayload(BlockPos pos) implements CustomPayload {
    public static final CustomPayload.Id<ToggleCastingPortModePayload> ID =
            new CustomPayload.Id<>(Identifier.of(EnchantedWoodMod.MOD_ID, "toggle_casting_port_mode"));

    public static final PacketCodec<RegistryByteBuf, ToggleCastingPortModePayload> CODEC =
            PacketCodec.tuple(
                    BlockPos.PACKET_CODEC,
                    ToggleCastingPortModePayload::pos,
                    ToggleCastingPortModePayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

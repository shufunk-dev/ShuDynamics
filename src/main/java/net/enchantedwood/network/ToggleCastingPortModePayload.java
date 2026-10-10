package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleCastingPortModePayload(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleCastingPortModePayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "toggle_casting_port_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleCastingPortModePayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ToggleCastingPortModePayload::pos,
                    ToggleCastingPortModePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

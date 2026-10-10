package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LaserQuarryActionPayload(int actionId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LaserQuarryActionPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "laser_quarry_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LaserQuarryActionPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    LaserQuarryActionPayload::actionId,
                    LaserQuarryActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

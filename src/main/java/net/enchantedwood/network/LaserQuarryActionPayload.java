package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record LaserQuarryActionPayload(int actionId) implements CustomPayload {
    public static final CustomPayload.Id<LaserQuarryActionPayload> ID =
            new CustomPayload.Id<>(Identifier.of(EnchantedWoodMod.MOD_ID, "laser_quarry_action"));

    public static final PacketCodec<RegistryByteBuf, LaserQuarryActionPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER,
                    LaserQuarryActionPayload::actionId,
                    LaserQuarryActionPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

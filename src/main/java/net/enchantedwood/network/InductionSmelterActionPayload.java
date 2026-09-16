package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record InductionSmelterActionPayload(BlockPos pos, int action) implements CustomPayload {
    public static final CustomPayload.Id<InductionSmelterActionPayload> ID =
            new CustomPayload.Id<>(Identifier.of(EnchantedWoodMod.MOD_ID, "induction_smelter_action"));

    public static final PacketCodec<RegistryByteBuf, InductionSmelterActionPayload> CODEC =
            PacketCodec.tuple(
                    BlockPos.PACKET_CODEC,
                    InductionSmelterActionPayload::pos,
                    PacketCodecs.VAR_INT,
                    InductionSmelterActionPayload::action,
                    InductionSmelterActionPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

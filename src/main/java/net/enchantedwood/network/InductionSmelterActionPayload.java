package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record InductionSmelterActionPayload(BlockPos pos, int action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<InductionSmelterActionPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "induction_smelter_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InductionSmelterActionPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    InductionSmelterActionPayload::pos,
                    ByteBufCodecs.VAR_INT,
                    InductionSmelterActionPayload::action,
                    InductionSmelterActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

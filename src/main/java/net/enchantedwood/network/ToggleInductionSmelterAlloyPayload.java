package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleInductionSmelterAlloyPayload(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleInductionSmelterAlloyPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "toggle_induction_smelter_alloy"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleInductionSmelterAlloyPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ToggleInductionSmelterAlloyPayload::pos,
                    ToggleInductionSmelterAlloyPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

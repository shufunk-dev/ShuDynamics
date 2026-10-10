package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public record SetSuperComputerRecipePayload(List<ItemStack> pattern) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetSuperComputerRecipePayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "set_super_computer_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetSuperComputerRecipePayload> CODEC =
            StreamCodec.composite(
                    ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SetSuperComputerRecipePayload::pattern,
                    SetSuperComputerRecipePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

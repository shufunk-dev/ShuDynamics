package net.enchantedwood.network;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record SetSuperComputerRecipePayload(List<ItemStack> pattern) implements CustomPayload {
    public static final CustomPayload.Id<SetSuperComputerRecipePayload> ID =
            new CustomPayload.Id<>(Identifier.of(EnchantedWoodMod.MOD_ID, "set_super_computer_recipe"));

    public static final PacketCodec<RegistryByteBuf, SetSuperComputerRecipePayload> CODEC =
            PacketCodec.tuple(
                    ItemStack.OPTIONAL_PACKET_CODEC.collect(PacketCodecs.toList()),
                    SetSuperComputerRecipePayload::pattern,
                    SetSuperComputerRecipePayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

package net.enchantedwood.network;

import net.enchantedwood.screen.SuperComputerScreenHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

public class ModMessages {
    public static void registerC2SPackets() {
        PayloadTypeRegistry.playC2S().register(SetSuperComputerRecipePayload.ID, SetSuperComputerRecipePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SetSuperComputerRecipePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ScreenHandler handler = context.player().currentScreenHandler;
                if (handler instanceof SuperComputerScreenHandler superHandler) {
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = (i < payload.pattern().size()) ? payload.pattern().get(i) : ItemStack.EMPTY;
                        superHandler.getSlot(i).setStack(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
                        superHandler.getSlot(i).markDirty();
                    }
                    superHandler.sendContentUpdates();
                }
            });
        });
    }
}

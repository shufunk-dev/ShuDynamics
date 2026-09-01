package net.enchantedwood.network;

import net.enchantedwood.screen.SuperComputerScreen;
import net.enchantedwood.screen.SuperComputerScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

public class ModMessages {
    public static void registerPackets() {
        PayloadTypeRegistry.playC2S().register(SetSuperComputerRecipePayload.ID, SetSuperComputerRecipePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SuperComputerStatusPayload.ID, SuperComputerStatusPayload.CODEC);

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

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(SuperComputerStatusPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                SuperComputerScreen.setLastStatus(payload.message());
            });
        });
    }
}

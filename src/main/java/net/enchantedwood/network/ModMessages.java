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
        PayloadTypeRegistry.playC2S().register(LaserQuarryActionPayload.ID, LaserQuarryActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OpenAtvInventoryPayload.ID, OpenAtvInventoryPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(OpenAtvInventoryPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().getVehicle() instanceof net.enchantedwood.entity.custom.AtvEntity atv) {
                    context.player().openHandledScreen(atv);
                }
            });
        });

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

        ServerPlayNetworking.registerGlobalReceiver(LaserQuarryActionPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ScreenHandler handler = context.player().currentScreenHandler;
                if (handler instanceof net.enchantedwood.screen.LaserQuarryScreenHandler quarryHandler) {
                    if (context.player().getEntityWorld().getBlockEntity(quarryHandler.blockPos) instanceof net.enchantedwood.block.entity.LaserQuarryBlockEntity quarry) {
                        quarry.handleAction(payload.actionId());
                    }
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

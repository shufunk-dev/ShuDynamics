package net.enchantedwood.network;

import net.enchantedwood.screen.SuperComputerScreen;
import net.enchantedwood.screen.SuperComputerScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ModMessages {
    public static void registerPackets() {
        PayloadTypeRegistry.serverboundPlay().register(SetSuperComputerRecipePayload.ID, SetSuperComputerRecipePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SuperComputerStatusPayload.ID, SuperComputerStatusPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LaserQuarryActionPayload.ID, LaserQuarryActionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OpenAtvInventoryPayload.ID, OpenAtvInventoryPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetStorageTerminalSearchPayload.ID, SetStorageTerminalSearchPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OpenModularSuitPanelPayload.ID, OpenModularSuitPanelPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ToggleInductionSmelterAlloyPayload.ID, ToggleInductionSmelterAlloyPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(InductionSmelterActionPayload.ID, InductionSmelterActionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ToggleCastingPortModePayload.ID, ToggleCastingPortModePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ToggleInductionSmelterAlloyPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                net.enchantedwood.block.entity.InductionSmelterBlockEntity smelter = null;
                if (context.player().containerMenu instanceof net.enchantedwood.screen.InductionSmelterScreenHandler handler
                        && handler.getInventory() instanceof net.enchantedwood.block.entity.InductionSmelterBlockEntity smelterBe) {
                    smelter = smelterBe;
                } else if (payload.pos() != null && context.player().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(payload.pos())) <= 64.0) {
                    BlockEntity be = context.player().level().getBlockEntity(payload.pos());
                    if (be instanceof net.enchantedwood.block.entity.InductionSmelterBlockEntity smelterBe) {
                        smelter = smelterBe;
                    }
                }
                if (smelter != null) {
                    smelter.setAlloyingEnabled(!smelter.isAlloyingEnabled());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(InductionSmelterActionPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                net.enchantedwood.block.entity.InductionSmelterBlockEntity smelter = null;
                if (context.player().containerMenu instanceof net.enchantedwood.screen.InductionSmelterScreenHandler handler
                        && handler.getInventory() instanceof net.enchantedwood.block.entity.InductionSmelterBlockEntity smelterBe) {
                    smelter = smelterBe;
                } else if (payload.pos() != null && context.player().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(payload.pos())) <= 64.0) {
                    BlockEntity be = context.player().level().getBlockEntity(payload.pos());
                    if (be instanceof net.enchantedwood.block.entity.InductionSmelterBlockEntity smelterBe) {
                        smelter = smelterBe;
                    }
                }
                if (smelter != null) {
                    switch (payload.action()) {
                        case 0 -> smelter.setAlloyingEnabled(!smelter.isAlloyingEnabled());
                        case 1 -> smelter.purgeHoldingTanks();
                        case 2 -> smelter.toggleEjectHoldingTanks();
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ToggleCastingPortModePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                net.enchantedwood.block.entity.CastingPortBlockEntity port = null;
                if (context.player().containerMenu instanceof net.enchantedwood.screen.CastingPortScreenHandler handler
                        && handler.getInventory() instanceof net.enchantedwood.block.entity.CastingPortBlockEntity portBe) {
                    port = portBe;
                } else if (payload.pos() != null && context.player().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(payload.pos())) <= 64.0) {
                    BlockEntity be = context.player().level().getBlockEntity(payload.pos());
                    if (be instanceof net.enchantedwood.block.entity.CastingPortBlockEntity portBe) {
                        port = portBe;
                    }
                }
                if (port != null) {
                    port.cycleMode();
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(OpenModularSuitPanelPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                context.player().openMenu(new net.minecraft.world.SimpleMenuProvider(
                        (syncId, inv, p) -> new net.enchantedwood.screen.ModularSuitScreenHandler(syncId, inv),
                        net.minecraft.network.chat.Component.literal("Modular Suit Access Panel")
                ));
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SetStorageTerminalSearchPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                AbstractContainerMenu handler = context.player().containerMenu;
                if (handler instanceof net.enchantedwood.screen.EnchantedStorageTerminalScreenHandler terminalHandler) {
                    terminalHandler.setSearchFilter(payload.query());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(OpenAtvInventoryPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().getVehicle() instanceof net.enchantedwood.entity.custom.AtvEntity atv) {
                    context.player().openMenu(atv);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SetSuperComputerRecipePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                AbstractContainerMenu handler = context.player().containerMenu;
                if (handler instanceof SuperComputerScreenHandler superHandler) {
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = (i < payload.pattern().size()) ? payload.pattern().get(i) : ItemStack.EMPTY;
                        superHandler.getSlot(i).setByPlayer(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
                        superHandler.getSlot(i).setChanged();
                    }
                    superHandler.broadcastChanges();
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(LaserQuarryActionPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                AbstractContainerMenu handler = context.player().containerMenu;
                if (handler instanceof net.enchantedwood.screen.LaserQuarryScreenHandler quarryHandler) {
                    if (context.player().level().getBlockEntity(quarryHandler.blockPos) instanceof net.enchantedwood.block.entity.LaserQuarryBlockEntity quarry) {
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

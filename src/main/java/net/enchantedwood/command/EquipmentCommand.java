package net.enchantedwood.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.enchantedwood.event.PlayerEquipmentState;
import net.enchantedwood.screen.EquipmentScreenHandler;
import net.enchantedwood.screen.PlayerEquipmentInventory;

public class EquipmentCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("equipment")
                .requires(source -> true)
                .executes(context -> openEquipmentGui(context.getSource()))
                .then(CommandManager.literal("open").executes(context -> openEquipmentGui(context.getSource())))
                .then(CommandManager.literal("status").executes(context -> showStatus(context.getSource())))
                .then(CommandManager.literal("unequip")
                    .then(CommandManager.literal("cape").executes(context -> unequipCape(context.getSource())))
                    .then(CommandManager.literal("heart").executes(context -> unequipHeart(context.getSource())))
                    .then(CommandManager.literal("all").executes(context -> unequipAll(context.getSource())))
                )
        );
    }

    private static int openEquipmentGui(ServerCommandSource source) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            player.openHandledScreen(new NamedScreenHandlerFactory() {
                @Override
                public Text getDisplayName() {
                    return Text.literal("Player Equipment");
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity playerEntity) {
                    return new EquipmentScreenHandler(syncId, playerInventory, new PlayerEquipmentInventory((ServerPlayerEntity) playerEntity));
                }
            });
        }
        return 1;
    }

    private static int showStatus(ServerCommandSource source) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ItemStack cape = PlayerEquipmentState.getEquippedCape(player);
            ItemStack heart = PlayerEquipmentState.getEquippedHeart(player);

            String capeText = cape.isEmpty() ? "§7None" : "§a" + cape.getName().getString();
            String heartText = heart.isEmpty() ? "§7None" : "§e" + heart.getName().getString();

            player.sendMessage(Text.literal("§b--- Equipment Status ---"), false);
            player.sendMessage(Text.literal("§6Back Slot (Cape): " + capeText), false);
            player.sendMessage(Text.literal("§6Heart Slot: " + heartText), false);
        }
        return 1;
    }

    private static int unequipCape(ServerCommandSource source) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ItemStack cape = PlayerEquipmentState.unequipCape(player);
            if (!cape.isEmpty()) {
                if (!player.getInventory().insertStack(cape)) {
                    player.dropItem(cape, false);
                }
                player.sendMessage(Text.literal("§aUnequipped Enchanted Cape from Back Slot!"), true);
            } else {
                player.sendMessage(Text.literal("§cNo cape is currently equipped in your Back Slot."), true);
            }
        }
        return 1;
    }

    private static int unequipHeart(ServerCommandSource source) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ItemStack heart = PlayerEquipmentState.unequipHeart(player);
            if (!heart.isEmpty()) {
                if (!player.getInventory().insertStack(heart)) {
                    player.dropItem(heart, false);
                }
                player.sendMessage(Text.literal("§eUnequipped Heart Locket from Heart Container Slot!"), true);
            } else {
                player.sendMessage(Text.literal("§cNo Heart Locket is currently equipped."), true);
            }
        }
        return 1;
    }

    private static int unequipAll(ServerCommandSource source) {
        unequipCape(source);
        unequipHeart(source);
        return 1;
    }
}

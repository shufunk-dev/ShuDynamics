package net.enchantedwood.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.enchantedwood.event.PlayerEquipmentState;
import net.enchantedwood.screen.EquipmentScreenHandler;
import net.enchantedwood.screen.PlayerEquipmentInventory;

public class EquipmentCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("equipment")
                .requires(source -> true)
                .executes(context -> openEquipmentGui(context.getSource()))
                .then(Commands.literal("open").executes(context -> openEquipmentGui(context.getSource())))
                .then(Commands.literal("status").executes(context -> showStatus(context.getSource())))
                .then(Commands.literal("unequip")
                    .then(Commands.literal("cape").executes(context -> unequipCape(context.getSource())))
                    .then(Commands.literal("heart").executes(context -> unequipHeart(context.getSource())))
                    .then(Commands.literal("all").executes(context -> unequipAll(context.getSource())))
                )
        );
    }

    private static int openEquipmentGui(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Player Equipment");
                }

                @Override
                public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player playerEntity) {
                    return new EquipmentScreenHandler(syncId, playerInventory, new PlayerEquipmentInventory((ServerPlayer) playerEntity));
                }
            });
        }
        return 1;
    }

    private static int showStatus(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            ItemStack cape = PlayerEquipmentState.getEquippedCape(player);
            ItemStack heart = PlayerEquipmentState.getEquippedHeart(player);

            String capeText = cape.isEmpty() ? "§7None" : "§a" + cape.getHoverName().getString();
            String heartText = heart.isEmpty() ? "§7None" : "§e" + heart.getHoverName().getString();

            player.sendSystemMessage(Component.literal("§b--- Equipment Status ---"));
            player.sendSystemMessage(Component.literal("§6Back Slot (Cape): " + capeText));
            player.sendSystemMessage(Component.literal("§6Heart Slot: " + heartText));
        }
        return 1;
    }

    private static int unequipCape(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            ItemStack cape = PlayerEquipmentState.unequipCape(player);
            if (!cape.isEmpty()) {
                if (!player.getInventory().add(cape)) {
                    player.drop(cape, false, net.minecraft.util.Prediction.SERVER_ONLY);
                }
                player.sendSystemMessage(Component.literal("§aUnequipped Enchanted Cape from Back Slot!"));
            } else {
                player.sendOverlayMessage(Component.literal("§cNo cape is currently equipped in your Back Slot."));
            }
        }
        return 1;
    }

    private static int unequipHeart(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            ItemStack heart = PlayerEquipmentState.unequipHeart(player);
            if (!heart.isEmpty()) {
                if (!player.getInventory().add(heart)) {
                    player.drop(heart, false, net.minecraft.util.Prediction.SERVER_ONLY);
                }
                player.sendOverlayMessage(Component.literal("§eUnequipped Heart Locket from Heart Container Slot!"));
            } else {
                player.sendOverlayMessage(Component.literal("§cNo Heart Locket is currently equipped."));
            }
        }
        return 1;
    }

    private static int unequipAll(CommandSourceStack source) {
        unequipCape(source);
        unequipHeart(source);
        return 1;
    }
}

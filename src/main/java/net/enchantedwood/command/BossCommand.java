package net.enchantedwood.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.enchantedwood.entity.custom.ResonanceColossusEntity;
import net.enchantedwood.entity.custom.AscendantColossusEntity;
import net.enchantedwood.entity.custom.PrimordialCataclysmEntity;

public class BossCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                Commands.literal("boss")
                    .requires(source -> true)
                    .then(Commands.literal("despawn").executes(context -> despawnBosses(context.getSource())))
                    .then(Commands.literal("reset").executes(context -> despawnBosses(context.getSource())))
            );
        });
    }

    private static int despawnBosses(CommandSourceStack source) {
        ServerLevel world = source.getLevel();
        int count = 0;
        for (var entity : world.getAllEntities()) {
            if (entity instanceof ResonanceColossusEntity ||
                entity instanceof AscendantColossusEntity ||
                entity instanceof PrimordialCataclysmEntity) {
                entity.discard();
                count++;
            }
        }
        int finalCount = count;
        source.sendSuccess(() -> Component.literal("§e✦ Dismissed " + finalCount + " active ShuDynamics boss(es)!"), true);
        return count;
    }
}

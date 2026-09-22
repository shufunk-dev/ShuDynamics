package net.enchantedwood.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.enchantedwood.entity.custom.ResonanceColossusEntity;
import net.enchantedwood.entity.custom.AscendantColossusEntity;
import net.enchantedwood.entity.custom.PrimordialCataclysmEntity;

public class BossCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                CommandManager.literal("boss")
                    .requires(source -> true)
                    .then(CommandManager.literal("despawn").executes(context -> despawnBosses(context.getSource())))
                    .then(CommandManager.literal("reset").executes(context -> despawnBosses(context.getSource())))
            );
        });
    }

    private static int despawnBosses(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        int count = 0;
        for (var entity : world.iterateEntities()) {
            if (entity instanceof ResonanceColossusEntity ||
                entity instanceof AscendantColossusEntity ||
                entity instanceof PrimordialCataclysmEntity) {
                entity.discard();
                count++;
            }
        }
        int finalCount = count;
        source.sendFeedback(() -> Text.literal("§e✦ Dismissed " + finalCount + " active ShuDynamics boss(es)!"), true);
        return count;
    }
}

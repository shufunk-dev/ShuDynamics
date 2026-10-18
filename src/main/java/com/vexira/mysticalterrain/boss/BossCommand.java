package com.vexira.mysticalterrain.boss;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.vexira.mysticalterrain.feature.LairPlacer;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class BossCommand {
   private static final SuggestionProvider<CommandSourceStack> BOSSES = (ctx, builder) -> SharedSuggestionProvider.suggest(BossEntities.BOSSES.keySet(), builder);
   private static final SuggestionProvider<CommandSourceStack> MOVES = (ctx, builder) -> {
      MysticalBoss boss = nearest((CommandSourceStack)ctx.getSource());
      List<String> moves = boss == null ? List.of() : boss.debugMoves();
      return SharedSuggestionProvider.suggest(moves, builder);
   };

   private BossCommand() {
   }

   static void register() {
      CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> {
         dispatcher.register(
            Commands.literal("mysticalterrain")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                  .then(
                     Commands.literal("lair")
                        .then(Commands.argument("boss", StringArgumentType.word()).suggests(BOSSES).executes(BossCommand::lair))
                  )
                  .then(
                     Commands.literal("summon")
                        .then(Commands.argument("boss", StringArgumentType.word()).suggests(BOSSES).executes(BossCommand::summon))
                  )
                  .then(Commands.literal("debug").executes(BossCommand::debug))
                  .then(Commands.literal("skipintro").executes(BossCommand::skipIntro))
                  .then(
                     Commands.literal("phase")
                        .then(Commands.argument("phase", IntegerArgumentType.integer(0, 9)).executes(BossCommand::phase))
                  )
                  .then(
                     Commands.literal("cast")
                        .then(Commands.argument("move", StringArgumentType.word()).suggests(MOVES).executes(BossCommand::cast))
                  )
                  .then(Commands.literal("despawn").executes(context -> despawnBosses(context.getSource())))
                  .then(Commands.literal("reset").executes(context -> despawnBosses(context.getSource())))
            );

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
         if (entity instanceof MysticalBoss) {
            entity.discard();
            count++;
         }
      }
      int finalCount = count;
      source.sendSuccess(() -> Component.literal("§a✔ Dismissed " + finalCount + " active boss(es)!"), true);
      return count;
   }

   private static int lair(CommandContext<CommandSourceStack> ctx) {
      String boss = StringArgumentType.getString(ctx, "boss");
      BlockPos at = BlockPos.containing(((CommandSourceStack)ctx.getSource()).getPosition());
      if (BossEntities.BOSSES.containsKey(boss) && LairPlacer.place(((CommandSourceStack)ctx.getSource()).getLevel(), boss, at)) {
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Built the lair of " + boss + " at " + at.toShortString()), true);
         return 1;
      } else {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("No such boss lair: " + boss));
         return 0;
      }
   }

   private static int summon(CommandContext<CommandSourceStack> ctx) {
      String boss = StringArgumentType.getString(ctx, "boss");
      EntityType<? extends MysticalBoss> type = BossEntities.BOSSES.get(boss);
      if (type == null) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("No such boss: " + boss));
         return 0;
      }

      ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
      MysticalBoss mob = (MysticalBoss)type.create(level, EntitySpawnReason.COMMAND);
      if (mob == null) {
         return 0;
      }

      Vec3 pos = ((CommandSourceStack)ctx.getSource())
         .getPosition()
         .add(((CommandSourceStack)ctx.getSource()).getRotation().y == 0.0F ? 0.0 : 0.0, 0.0, 0.0);
      Vec3 look = Vec3.directionFromRotation(0.0F, ((CommandSourceStack)ctx.getSource()).getRotation().y).scale(8.0);
      mob.snapTo(pos.x + look.x, pos.y, pos.z + look.z, ((CommandSourceStack)ctx.getSource()).getRotation().y + 180.0F, 0.0F);
      mob.beginIntro(BlockPos.containing(mob.position()));
      level.addFreshEntity(mob);
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Summoned " + boss), true);
      return 1;
   }

   private static @Nullable MysticalBoss nearest(CommandSourceStack source) {
      Vec3 at = source.getPosition();
      MysticalBoss nearest = null;
      double best = Double.MAX_VALUE;

      for (MysticalBoss b : source.getLevel().getEntitiesOfClass(MysticalBoss.class, new AABB(at, at).inflate(64.0), e -> e.isBoss() && e.isAlive())) {
         double d = b.distanceToSqr(at);
         if (d < best) {
            best = d;
            nearest = b;
         }
      }

      return nearest;
   }

   private static @Nullable MysticalBoss nearestOrFail(CommandSourceStack source) {
      MysticalBoss boss = nearest(source);
      if (boss == null) {
         source.sendFailure(Component.literal("No boss within 64 blocks"));
      }

      return boss;
   }

   private static int debug(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack source = (CommandSourceStack)ctx.getSource();
      MysticalBoss nearest = nearestOrFail(source);
      if (nearest == null) {
         return 0;
      }

      for (String line : nearest.debugLines()) {
         source.sendSuccess(() -> Component.literal(line), false);
      }

      return 1;
   }

   private static int skipIntro(CommandContext<CommandSourceStack> ctx) {
      MysticalBoss boss = nearestOrFail((CommandSourceStack)ctx.getSource());
      if (boss == null) {
         return 0;
      } else if (!boss.inIntro()) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal(boss.bossId() + " is not in its entrance"));
         return 0;
      } else {
         boss.debugSkipIntro();
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal(boss.bossId() + ": entrance skipped"), true);
         return 1;
      }
   }

   private static int phase(CommandContext<CommandSourceStack> ctx) {
      int want = IntegerArgumentType.getInteger(ctx, "phase");
      MysticalBoss boss = nearestOrFail((CommandSourceStack)ctx.getSource());
      if (boss == null) {
         return 0;
      } else if (boss.inIntro()) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal(boss.bossId() + " is still in its entrance (/mysticalterrain skipintro)"));
         return 0;
      } else {
         int now = boss.debugSetPhase(want);
         if (now != want) {
            ((CommandSourceStack)ctx.getSource())
               .sendFailure(Component.literal(boss.bossId() + " has phases 0-" + boss.bossState().thresholds().size() + "; it is in phase " + now));
            return 0;
         } else {
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal(boss.bossId() + ": phase " + now), true);
            return 1;
         }
      }
   }

   private static int cast(CommandContext<CommandSourceStack> ctx) {
      String move = StringArgumentType.getString(ctx, "move");
      MysticalBoss boss = nearestOrFail((CommandSourceStack)ctx.getSource());
      if (boss == null) {
         return 0;
      } else if (boss.inIntro()) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal(boss.bossId() + " is still in its entrance (/mysticalterrain skipintro)"));
         return 0;
      } else if (!boss.debugCast(move)) {
         ((CommandSourceStack)ctx.getSource())
            .sendFailure(Component.literal(boss.bossId() + " has no move " + move + " (" + String.join(", ", boss.debugMoves()) + ")"));
         return 0;
      } else {
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal(boss.bossId() + " casts " + move + " (phase " + boss.phase() + ")"), true);
         return 1;
      }
   }
}

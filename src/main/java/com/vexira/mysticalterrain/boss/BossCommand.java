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
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

public final class BossCommand {
   private static final SuggestionProvider<ServerCommandSource> BOSSES = (ctx, builder) -> CommandSource.suggestMatching(BossEntities.BOSSES.keySet(), builder);
   private static final SuggestionProvider<ServerCommandSource> MOVES = (ctx, builder) -> {
      MysticalBoss boss = nearest((ServerCommandSource)ctx.getSource());
      List<String> moves = boss == null ? List.of() : boss.debugMoves();
      return CommandSource.suggestMatching(moves, builder);
   };

   private BossCommand() {
   }

   static void register() {
      CommandRegistrationCallback.EVENT
         .register(
            (dispatcher, registries, environment) -> dispatcher.register(
               CommandManager.literal("mysticalterrain")
                  .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                  .then(
                     CommandManager.literal("lair")
                        .then(CommandManager.argument("boss", StringArgumentType.word()).suggests(BOSSES).executes(BossCommand::lair))
                  )
                  .then(
                     CommandManager.literal("summon")
                        .then(CommandManager.argument("boss", StringArgumentType.word()).suggests(BOSSES).executes(BossCommand::summon))
                  )
                  .then(CommandManager.literal("debug").executes(BossCommand::debug))
                  .then(CommandManager.literal("skipintro").executes(BossCommand::skipIntro))
                  .then(
                     CommandManager.literal("phase")
                        .then(CommandManager.argument("phase", IntegerArgumentType.integer(0, 9)).executes(BossCommand::phase))
                  )
                  .then(
                     CommandManager.literal("cast")
                        .then(CommandManager.argument("move", StringArgumentType.word()).suggests(MOVES).executes(BossCommand::cast))
                  )
            )
         );
   }

   private static int lair(CommandContext<ServerCommandSource> ctx) {
      String boss = StringArgumentType.getString(ctx, "boss");
      BlockPos at = BlockPos.ofFloored(((ServerCommandSource)ctx.getSource()).getPosition());
      if (BossEntities.BOSSES.containsKey(boss) && LairPlacer.place(((ServerCommandSource)ctx.getSource()).getWorld(), boss, at)) {
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal("Built the lair of " + boss + " at " + at.toShortString()), true);
         return 1;
      } else {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("No such boss lair: " + boss));
         return 0;
      }
   }

   private static int summon(CommandContext<ServerCommandSource> ctx) {
      String boss = StringArgumentType.getString(ctx, "boss");
      EntityType<? extends MysticalBoss> type = BossEntities.BOSSES.get(boss);
      if (type == null) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal("No such boss: " + boss));
         return 0;
      }

      ServerWorld level = ((ServerCommandSource)ctx.getSource()).getWorld();
      MysticalBoss mob = (MysticalBoss)type.create(level, SpawnReason.COMMAND);
      if (mob == null) {
         return 0;
      }

      Vec3d pos = ((ServerCommandSource)ctx.getSource())
         .getPosition()
         .add(((ServerCommandSource)ctx.getSource()).getRotation().y == 0.0F ? 0.0 : 0.0, 0.0, 0.0);
      Vec3d look = Vec3d.fromPolar(0.0F, ((ServerCommandSource)ctx.getSource()).getRotation().y).multiply(8.0);
      mob.refreshPositionAndAngles(pos.x + look.x, pos.y, pos.z + look.z, ((ServerCommandSource)ctx.getSource()).getRotation().y + 180.0F, 0.0F);
      mob.beginIntro(BlockPos.ofFloored(mob.getEntityPos()));
      level.spawnEntity(mob);
      ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal("Summoned " + boss), true);
      return 1;
   }

   private static @Nullable MysticalBoss nearest(ServerCommandSource source) {
      Vec3d at = source.getPosition();
      MysticalBoss nearest = null;
      double best = Double.MAX_VALUE;

      for (MysticalBoss b : source.getWorld().getEntitiesByClass(MysticalBoss.class, new Box(at, at).expand(64.0), e -> e.isBoss() && e.isAlive())) {
         double d = b.squaredDistanceTo(at);
         if (d < best) {
            best = d;
            nearest = b;
         }
      }

      return nearest;
   }

   private static @Nullable MysticalBoss nearestOrFail(ServerCommandSource source) {
      MysticalBoss boss = nearest(source);
      if (boss == null) {
         source.sendError(Text.literal("No boss within 64 blocks"));
      }

      return boss;
   }

   private static int debug(CommandContext<ServerCommandSource> ctx) {
      ServerCommandSource source = (ServerCommandSource)ctx.getSource();
      MysticalBoss nearest = nearestOrFail(source);
      if (nearest == null) {
         return 0;
      }

      for (String line : nearest.debugLines()) {
         source.sendFeedback(() -> Text.literal(line), false);
      }

      return 1;
   }

   private static int skipIntro(CommandContext<ServerCommandSource> ctx) {
      MysticalBoss boss = nearestOrFail((ServerCommandSource)ctx.getSource());
      if (boss == null) {
         return 0;
      } else if (!boss.inIntro()) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal(boss.bossId() + " is not in its entrance"));
         return 0;
      } else {
         boss.debugSkipIntro();
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal(boss.bossId() + ": entrance skipped"), true);
         return 1;
      }
   }

   private static int phase(CommandContext<ServerCommandSource> ctx) {
      int want = IntegerArgumentType.getInteger(ctx, "phase");
      MysticalBoss boss = nearestOrFail((ServerCommandSource)ctx.getSource());
      if (boss == null) {
         return 0;
      } else if (boss.inIntro()) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal(boss.bossId() + " is still in its entrance (/mysticalterrain skipintro)"));
         return 0;
      } else {
         int now = boss.debugSetPhase(want);
         if (now != want) {
            ((ServerCommandSource)ctx.getSource())
               .sendError(Text.literal(boss.bossId() + " has phases 0-" + boss.bossState().thresholds().size() + "; it is in phase " + now));
            return 0;
         } else {
            ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal(boss.bossId() + ": phase " + now), true);
            return 1;
         }
      }
   }

   private static int cast(CommandContext<ServerCommandSource> ctx) {
      String move = StringArgumentType.getString(ctx, "move");
      MysticalBoss boss = nearestOrFail((ServerCommandSource)ctx.getSource());
      if (boss == null) {
         return 0;
      } else if (boss.inIntro()) {
         ((ServerCommandSource)ctx.getSource()).sendError(Text.literal(boss.bossId() + " is still in its entrance (/mysticalterrain skipintro)"));
         return 0;
      } else if (!boss.debugCast(move)) {
         ((ServerCommandSource)ctx.getSource())
            .sendError(Text.literal(boss.bossId() + " has no move " + move + " (" + String.join(", ", boss.debugMoves()) + ")"));
         return 0;
      } else {
         ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal(boss.bossId() + " casts " + move + " (phase " + boss.phase() + ")"), true);
         return 1;
      }
   }
}

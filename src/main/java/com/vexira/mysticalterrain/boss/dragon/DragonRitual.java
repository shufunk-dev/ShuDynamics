package com.vexira.mysticalterrain.boss.dragon;

import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.MysticalBosses;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.mixin.EnderDragonFightAccessor;
import com.vexira.mysticalterrain.mixin.EnderDragonFightInvoker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature.EndSpike;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class DragonRitual {
   private static final int[][] OFFSETS = new int[][]{{9, 0}, {-9, 0}, {0, 9}, {0, -9}};
   private static final int RISE_AT = 80;
   private static final int DONE_AT = 260;
   private static final int DARK_UNTIL = 70;
   private static final int REBUILD_FROM = 100;
   private static final int REBUILD_EVERY = 12;
   private static boolean allowSpawn;
   private static int ticks = -1;
   private static @Nullable Vec3 focus;
   private static final List<EndSpike> REBUILD = new ArrayList<>();

   private DragonRitual() {
   }

   public static boolean allowsSpawn() {
      return allowSpawn;
   }

   public static void setAllowSpawn(boolean allow) {
      allowSpawn = allow;
   }

   public static void tick(ServerLevel level) {
      EnderDragonFight fight = level.getDragonFight();
      if (fight != null) {
         if (level.getGameTime() % 100L == 0L && ticks < 0) {
            placePedestals(level, fight);
         }

         if (ticks >= 0) {
            tickSummon(level, fight);
         }
      }
   }

   public static boolean canBegin(ServerLevel level) {
      return level.getDragonFight() != null && ticks < 0 && level.getDragons().isEmpty();
   }

   public static void onEyePlaced(ServerLevel level, Player player) {
      EnderDragonFight fight = level.getDragonFight();
      if (fight != null) {
         List<BlockPos> pedestals = pedestals(level, fight);
         long filled = pedestals.stream().filter(px -> (Boolean)level.getBlockState(px).getValue(VoidPedestalBlock.EYE)).count();
         if (pedestals.size() == 4 && filled == 4L) {
            begin(level, fight);
         } else {
            for (ServerPlayer p : level.getPlayers(px -> px.distanceToSqr(player) < 4096.0)) {
               p.sendSystemMessage(Component.translatable("message.mysticalterrain.void_stirs", new Object[]{filled, 4}));
            }
         }
      }
   }

   private static BlockPos portal(ServerLevel level, EnderDragonFight fight) {
      BlockPos portal = ((EnderDragonFightAccessor)fight).mysticalterrain$portalLocation();
      if (portal == null) {
         BlockPos origin = ((EnderDragonFightAccessor)fight).mysticalterrain$origin();
         portal = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, origin != null ? origin : BlockPos.ZERO);
      }

      return portal;
   }

   public static List<BlockPos> pedestals(ServerLevel level, EnderDragonFight fight) {
      BlockPos portal = portal(level, fight);
      List<BlockPos> out = new ArrayList<>();

      for (int[] o : OFFSETS) {
         BlockPos found = findInColumn(level, portal.offset(o[0], 0, o[1]));
         if (found != null) {
            out.add(found);
         }
      }

      return out;
   }

   private static @Nullable BlockPos findInColumn(ServerLevel level, BlockPos around) {
      for (int dy = -8; dy <= 12; dy++) {
         BlockPos p = around.above(dy);
         if (level.getBlockState(p).is(MysticalBosses.VOID_PEDESTAL)) {
            return p;
         }
      }

      return null;
   }

   private static void placePedestals(ServerLevel level, EnderDragonFight fight) {
      BlockPos portal = portal(level, fight);

      for (int[] o : OFFSETS) {
         BlockPos column = portal.offset(o[0], 0, o[1]);
         if (level.isLoaded(column) && findInColumn(level, column) == null) {
            BlockPos top = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (top.getY() > level.getMinY() + 8 && Math.abs(top.getY() - portal.getY()) <= 10) {
               for (int dx = -1; dx <= 1; dx++) {
                  for (int dz = -1; dz <= 1; dz++) {
                     BlockPos b = top.offset(dx, -1, dz);
                     if (!level.getBlockState(b).isAir()) {
                        level.setBlockAndUpdate(b, dx == 0 && dz == 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState());
                     }
                  }
               }

               level.setBlockAndUpdate(top, MysticalBosses.VOID_PEDESTAL.defaultBlockState());
            }
         }
      }
   }

   private static void begin(ServerLevel level, EnderDragonFight fight) {
      ticks = 0;
      REBUILD.clear();
      Vec3 at = Vec3.atBottomCenterOf(portal(level, fight)).add(0.0, 24.0, 0.0);
      focus = at;
      level.playSound(null, at.x, at.y, at.z, MysticalBosses.RITUAL_ACTIVATE, SoundSource.HOSTILE, 10.0F, 0.7F);
      Vfx.explosion(level, at, DragonFx.V, 3.0);
      Vfx.screen(level, at, 160.0, 0.6, 20, DragonFx.V.glow(), 0.35F);

      for (ServerPlayer p : level.getPlayers(px -> px.distanceToSqr(at) < 32400.0)) {
         p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0, false, false));
         p.sendSystemMessage(Component.translatable("message.mysticalterrain.dragon_ritual"));
      }
   }

   private static void tickSummon(ServerLevel level, EnderDragonFight fight) {
      ticks++;
      Vec3 at = focus != null ? focus : Vec3.atBottomCenterOf(portal(level, fight)).add(0.0, 24.0, 0.0);
      List<BlockPos> pedestals = pedestals(level, fight);
      EnderDragon dragon = level.getDragons().isEmpty() ? null : (EnderDragon)level.getDragons().get(0);
      Vec3 to = dragon != null && ticks > 80 ? dragon.position().add(0.0, 2.0, 0.0) : at;
      if (ticks % 2 == 0) {
         for (BlockPos p : pedestals) {
            Vec3 from = Vec3.atBottomCenterOf(p).add(0.0, 1.1, 0.0);
            Vec3 d = to.subtract(from);
            double len = d.length();

            for (double s = ticks % 8 / 8.0 * 1.5; s < len; s += 1.5) {
               Vec3 q = from.add(d.scale(s / len));
               level.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0.0);
            }

            if (ticks % 6 == 0) {
               Vfx.beam(level, from, to, DragonFx.V, 0.45);
               Vfx.flare(level, from, DragonFx.V.glow(), 2.0, 8);
            }
         }
      }

      if (ticks < 80) {
         double r = 1.0 + ticks / 20.0;
         if (ticks % 4 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 12, r, r, r, 0.08);
            Vfx.smoke(level, at, DragonFx.V.smoke(), 6, r, 2.0);
         }

         if (ticks % 3 == 0) {
            Vfx.gather(level, at, DragonFx.V, 4.0 + r, 14);
            Vfx.flare(level, at, DragonFx.V.core(), 1.0 + ticks / 30.0, 6);
         }

         if (ticks % 20 == 0) {
            Vfx.ring(level, at.add(0.0, -24.0, 0.0), DragonFx.V.glow(), 18.0, 24);
            BossSounds.play(level, at, "boss.ender_dragon.channel.start", 6.0F, 0.5F + ticks / 160.0F);
         }
      }

      if (ticks == 70) {
         for (BlockPos p : pedestals) {
            Vec3 base = Vec3.atBottomCenterOf(p).add(0.0, 1.0, 0.0);
            Vfx.pillar(level, Vec3.atBottomCenterOf(p), DragonFx.V, 30.0, 1.6);
            Vfx.lightning(level, base.add(0.0, 40.0, 0.0), base, DragonFx.V, 4);
            Vfx.flash(level, base, 64.0, 16777215, 0.3F);
         }
      }

      if (ticks == 80) {
         EnderDragonFightAccessor access = (EnderDragonFightAccessor)fight;
         if (access.mysticalterrain$dragonKilled()) {
            access.mysticalterrain$spawnExitPortal(false);
            access.mysticalterrain$setDragonKilled(false);
         }

         allowSpawn = true;
         EnderDragon spawned = access.mysticalterrain$createNewDragon();
         allowSpawn = false;
         if (spawned != null) {
            ((DragonReworkHolder)spawned).mysticalterrain$rework().beginSummon(Vec3.atBottomCenterOf(portal(level, fight)).add(0.0, 28.0, 0.0));
            level.playSound(null, at.x, at.y, at.z, MysticalBosses.DRAGON_SUMMON, SoundSource.HOSTILE, 12.0F, 1.0F);
            Vfx.explosion(level, at, DragonFx.V, 6.0);
            Vfx.screen(level, at, 160.0, 1.4, 30, DragonFx.V.glow(), 0.55F);
         }

         access.mysticalterrain$dragonEvent().setDarkenScreen(false);
         REBUILD.clear();
         REBUILD.addAll(DragonFx.emptySpikes(level));
      }

      tickRebuild(level, fight, pedestals);
      if (ticks >= 260) {
         ticks = -1;
         REBUILD.clear();
         ((EnderDragonFightAccessor)fight).mysticalterrain$dragonEvent().setDarkenScreen(false);

         for (BlockPos p : pedestals) {
            BlockState state = level.getBlockState(p);
            level.setBlock(p, (BlockState)state.setValue(VoidPedestalBlock.EYE, false), 3);
            Vec3 top = Vec3.atBottomCenterOf(p).add(0.0, 1.0, 0.0);
            level.sendParticles(ParticleTypes.END_ROD, top.x, top.y, top.z, 20, 0.3, 0.3, 0.3, 0.1);
            Vfx.smoke(level, top, DragonFx.V.smoke(), 6, 0.4, 1.2);
            Vfx.impact(level, top, DragonFx.V, 0.8);
         }
      }
   }

   private static void tickRebuild(ServerLevel level, EnderDragonFight fight, List<BlockPos> pedestals) {
      if (!REBUILD.isEmpty() && ticks >= 88) {
         boolean rebuilt = false;

         for (int i = 0; i < REBUILD.size(); i++) {
            EndSpike spike = REBUILD.get(i);
            if (spike != null) {
               int due = 100 + i * 12;
               Vec3 top = DragonFx.spikeTop(spike).add(0.0, 1.0, 0.0);
               if (ticks >= due - 12 && ticks < due && ticks % 3 == 0 && !pedestals.isEmpty()) {
                  Vec3 from = Vec3.atBottomCenterOf((Vec3i)pedestals.get(i % pedestals.size())).add(0.0, 1.1, 0.0);
                  Vfx.beam(level, from, top, DragonFx.V, 0.35, 4);
                  Vfx.gather(level, top, DragonFx.V, 3.0, 8);
               }

               if (ticks == due) {
                  if (DragonFx.emptySpikes(level).contains(spike)) {
                     DragonFx.rebuildCrystal(level, spike);
                     rebuilt = true;
                  }

                  REBUILD.set(i, null);
               }
            }
         }

         if (rebuilt) {
            ((EnderDragonFightInvoker)fight).mysticalterrain$updateCrystalCount();
         }
      }
   }
}

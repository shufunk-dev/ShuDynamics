package com.vexira.mysticalterrain.boss.dragon;

import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.MysticalBosses;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.mixin.EndDragonFightAccessor;
import com.vexira.mysticalterrain.mixin.EndDragonFightInvoker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.gen.feature.EndPortalFeature;
import net.minecraft.world.gen.feature.EndSpikeFeature.Spike;
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
   private static @Nullable Vec3d focus;
   private static final List<Spike> REBUILD = new ArrayList<>();

   private DragonRitual() {
   }

   public static boolean allowsSpawn() {
      return allowSpawn;
   }

   public static void setAllowSpawn(boolean allow) {
      allowSpawn = allow;
   }

   public static void tick(ServerWorld level) {
      EnderDragonFight fight = level.getEnderDragonFight();
      if (fight != null) {
         if (level.getTime() % 100L == 0L && ticks < 0) {
            placePedestals(level, fight);
         }

         if (ticks >= 0) {
            tickSummon(level, fight);
         }
      }
   }

   public static boolean canBegin(ServerWorld level) {
      return level.getEnderDragonFight() != null && ticks < 0 && level.getAliveEnderDragons().isEmpty();
   }

   public static void onEyePlaced(ServerWorld level, PlayerEntity player) {
      EnderDragonFight fight = level.getEnderDragonFight();
      if (fight != null) {
         List<BlockPos> pedestals = pedestals(level, fight);
         long filled = pedestals.stream().filter(px -> (Boolean)level.getBlockState(px).get(VoidPedestalBlock.EYE)).count();
         if (pedestals.size() == 4 && filled == 4L) {
            begin(level, fight);
         } else {
            for (ServerPlayerEntity p : level.getPlayers(px -> px.squaredDistanceTo(player) < 4096.0)) {
               p.sendMessage(Text.translatable("message.mysticalterrain.void_stirs", new Object[]{filled, 4}), false);
            }
         }
      }
   }

   private static BlockPos portal(ServerWorld level, EnderDragonFight fight) {
      BlockPos portal = ((EndDragonFightAccessor)fight).mysticalterrain$portalLocation();
      if (portal == null) {
         portal = level.getTopPosition(Type.MOTION_BLOCKING_NO_LEAVES, EndPortalFeature.offsetOrigin(((EndDragonFightAccessor)fight).mysticalterrain$origin()));
      }

      return portal;
   }

   public static List<BlockPos> pedestals(ServerWorld level, EnderDragonFight fight) {
      BlockPos portal = portal(level, fight);
      List<BlockPos> out = new ArrayList<>();

      for (int[] o : OFFSETS) {
         BlockPos found = findInColumn(level, portal.add(o[0], 0, o[1]));
         if (found != null) {
            out.add(found);
         }
      }

      return out;
   }

   private static @Nullable BlockPos findInColumn(ServerWorld level, BlockPos around) {
      for (int dy = -8; dy <= 12; dy++) {
         BlockPos p = around.up(dy);
         if (level.getBlockState(p).isOf(MysticalBosses.VOID_PEDESTAL)) {
            return p;
         }
      }

      return null;
   }

   private static void placePedestals(ServerWorld level, EnderDragonFight fight) {
      BlockPos portal = portal(level, fight);

      for (int[] o : OFFSETS) {
         BlockPos column = portal.add(o[0], 0, o[1]);
         if (level.isPosLoaded(column) && findInColumn(level, column) == null) {
            BlockPos top = level.getTopPosition(Type.MOTION_BLOCKING_NO_LEAVES, column);
            if (top.getY() > level.getBottomY() + 8 && Math.abs(top.getY() - portal.getY()) <= 10) {
               for (int dx = -1; dx <= 1; dx++) {
                  for (int dz = -1; dz <= 1; dz++) {
                     BlockPos b = top.add(dx, -1, dz);
                     if (!level.getBlockState(b).isAir()) {
                        level.setBlockState(b, dx == 0 && dz == 0 ? Blocks.CRYING_OBSIDIAN.getDefaultState() : Blocks.OBSIDIAN.getDefaultState());
                     }
                  }
               }

               level.setBlockState(top, MysticalBosses.VOID_PEDESTAL.getDefaultState());
            }
         }
      }
   }

   private static void begin(ServerWorld level, EnderDragonFight fight) {
      ticks = 0;
      REBUILD.clear();
      Vec3d at = Vec3d.ofBottomCenter(portal(level, fight)).add(0.0, 24.0, 0.0);
      focus = at;
      level.playSound(null, at.x, at.y, at.z, MysticalBosses.RITUAL_ACTIVATE, SoundCategory.HOSTILE, 10.0F, 0.7F);
      Vfx.explosion(level, at, DragonFx.V, 3.0);
      Vfx.screen(level, at, 160.0, 0.6, 20, DragonFx.V.glow(), 0.35F);

      for (ServerPlayerEntity p : level.getPlayers(px -> px.squaredDistanceTo(at) < 32400.0)) {
         p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 70, 0, false, false));
         p.sendMessage(Text.translatable("message.mysticalterrain.dragon_ritual"), false);
      }
   }

   private static void tickSummon(ServerWorld level, EnderDragonFight fight) {
      ticks++;
      Vec3d at = focus != null ? focus : Vec3d.ofBottomCenter(portal(level, fight)).add(0.0, 24.0, 0.0);
      List<BlockPos> pedestals = pedestals(level, fight);
      EnderDragonEntity dragon = level.getAliveEnderDragons().isEmpty() ? null : (EnderDragonEntity)level.getAliveEnderDragons().get(0);
      Vec3d to = dragon != null && ticks > 80 ? dragon.getEntityPos().add(0.0, 2.0, 0.0) : at;
      if (ticks % 2 == 0) {
         for (BlockPos p : pedestals) {
            Vec3d from = Vec3d.ofBottomCenter(p).add(0.0, 1.1, 0.0);
            Vec3d d = to.subtract(from);
            double len = d.length();

            for (double s = ticks % 8 / 8.0 * 1.5; s < len; s += 1.5) {
               Vec3d q = from.add(d.multiply(s / len));
               level.spawnParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0.0);
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
            level.spawnParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 12, r, r, r, 0.08);
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
            Vec3d base = Vec3d.ofBottomCenter(p).add(0.0, 1.0, 0.0);
            Vfx.pillar(level, Vec3d.ofBottomCenter(p), DragonFx.V, 30.0, 1.6);
            Vfx.lightning(level, base.add(0.0, 40.0, 0.0), base, DragonFx.V, 4);
            Vfx.flash(level, base, 64.0, 16777215, 0.3F);
         }
      }

      if (ticks == 80) {
         EndDragonFightAccessor access = (EndDragonFightAccessor)fight;
         if (access.mysticalterrain$dragonKilled()) {
            access.mysticalterrain$spawnExitPortal(false);
            access.mysticalterrain$setDragonKilled(false);
         }

         allowSpawn = true;
         EnderDragonEntity spawned = access.mysticalterrain$createNewDragon();
         allowSpawn = false;
         if (spawned != null) {
            ((DragonReworkHolder)spawned).mysticalterrain$rework().beginSummon(Vec3d.ofBottomCenter(portal(level, fight)).add(0.0, 28.0, 0.0));
            level.playSound(null, at.x, at.y, at.z, MysticalBosses.DRAGON_SUMMON, SoundCategory.HOSTILE, 12.0F, 1.0F);
            Vfx.explosion(level, at, DragonFx.V, 6.0);
            Vfx.screen(level, at, 160.0, 1.4, 30, DragonFx.V.glow(), 0.55F);
         }

         access.mysticalterrain$dragonEvent().setDarkenSky(false);
         REBUILD.clear();
         REBUILD.addAll(DragonFx.emptySpikes(level));
      }

      tickRebuild(level, fight, pedestals);
      if (ticks >= 260) {
         ticks = -1;
         REBUILD.clear();
         ((EndDragonFightAccessor)fight).mysticalterrain$dragonEvent().setDarkenSky(false);

         for (BlockPos p : pedestals) {
            BlockState state = level.getBlockState(p);
            level.setBlockState(p, (BlockState)state.with(VoidPedestalBlock.EYE, false), 3);
            Vec3d top = Vec3d.ofBottomCenter(p).add(0.0, 1.0, 0.0);
            level.spawnParticles(ParticleTypes.END_ROD, top.x, top.y, top.z, 20, 0.3, 0.3, 0.3, 0.1);
            Vfx.smoke(level, top, DragonFx.V.smoke(), 6, 0.4, 1.2);
            Vfx.impact(level, top, DragonFx.V, 0.8);
         }
      }
   }

   private static void tickRebuild(ServerWorld level, EnderDragonFight fight, List<BlockPos> pedestals) {
      if (!REBUILD.isEmpty() && ticks >= 88) {
         boolean rebuilt = false;

         for (int i = 0; i < REBUILD.size(); i++) {
            Spike spike = REBUILD.get(i);
            if (spike != null) {
               int due = 100 + i * 12;
               Vec3d top = DragonFx.spikeTop(spike).add(0.0, 1.0, 0.0);
               if (ticks >= due - 12 && ticks < due && ticks % 3 == 0 && !pedestals.isEmpty()) {
                  Vec3d from = Vec3d.ofBottomCenter((Vec3i)pedestals.get(i % pedestals.size())).add(0.0, 1.1, 0.0);
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
            ((EndDragonFightInvoker)fight).mysticalterrain$updateCrystalCount();
         }
      }
   }
}

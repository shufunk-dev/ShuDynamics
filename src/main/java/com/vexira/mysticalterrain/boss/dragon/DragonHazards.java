package com.vexira.mysticalterrain.boss.dragon;

import com.vexira.mysticalterrain.boss.vfx.Vfx;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;

final class DragonHazards {
   private final DragonRework rework;
   private List<EndCrystal> crystals = List.of();
   private final List<DragonHazards.Pulse> pulses = new ArrayList<>();
   private final List<DragonHazards.Rift> rifts = new ArrayList<>();
   private final List<DragonHazards.Meteor> meteors = new ArrayList<>();
   private final List<DragonHazards.Shroud> shrouds = new ArrayList<>();
   private final Set<UUID> warned = new HashSet<>();
   private int nextRifts = 440;
   private boolean edge;
   private int edgeTicks;

   DragonHazards(DragonRework rework) {
      this.rework = rework;
   }

   List<EndCrystal> crystals() {
      return this.crystals;
   }

   void refreshCrystals(ServerLevel level) {
      this.crystals = DragonFx.spikeCrystals(level);
   }

   boolean edgeOn() {
      return this.edge;
   }

   void tick(ServerLevel level, Vec3 portal, boolean active) {
      EnderDragon dragon = this.rework.dragon();
      int now = dragon.tickCount;
      if (now % 20 == 0) {
         this.refreshCrystals(level);
      }

      if (active && this.rework.fightPhase() == 0) {
         this.startPulses(level, now);
      }

      this.tickPulses(level, now);
      if (active && this.rework.fightPhase() >= 2 && --this.nextRifts <= 0) {
         this.openRifts(level, portal);
      }

      this.tickRifts(level, now);
      this.tickMeteors(level);
      if (this.edge) {
         this.tickEdge(level, portal);
      }

      this.tickShrouds(level, now);
   }

   void clear() {
      for (DragonHazards.Shroud s : this.shrouds) {
         s.crystal().setPermanentlyInvulnerable(false);
      }

      this.shrouds.clear();
      this.pulses.clear();
      this.rifts.clear();
      this.meteors.clear();
      this.edge = false;
   }

   private void startPulses(ServerLevel level, int now) {
      for (int i = 0; i < this.crystals.size(); i++) {
         EndCrystal c = this.crystals.get(i);
         if (!c.isRemoved() && (now + i * 16) % 160 == 0) {
            Vfx.gather(level, c.position().add(0.0, 1.0, 0.0), DragonFx.V, 6.0, 16);
            Vfx.telegraphCircle(level, c.position(), 6.0, 20, DragonFx.V);
            this.pulses.add(new DragonHazards.Pulse(c, now + 20));
         }
      }
   }

   private void tickPulses(ServerLevel level, int now) {
      Iterator<DragonHazards.Pulse> it = this.pulses.iterator();

      while (it.hasNext()) {
         DragonHazards.Pulse p = it.next();
         if (now >= p.at()) {
            it.remove();
            if (!p.crystal().isRemoved()) {
               Vec3 at = p.crystal().position();
               Vfx.nova(level, at.add(0.0, 1.0, 0.0), DragonFx.V, 6.0);

               for (ServerPlayer player : DragonFx.players(level, at, 6.0)) {
                  DragonFx.hurt(level, this.rework.dragon(), player, this.rework.dmg(4.0F), true);
                  Vec3 out = DragonFx.away(at, player.position());
                  DragonFx.launch(player, new Vec3(out.x, 0.35, out.z));
               }
            }
         }
      }
   }

   void openRifts(ServerLevel level, Vec3 portal) {
      RandomSource r = this.rework.dragon().getRandom();
      this.nextRifts = 400 + r.nextInt(81);
      List<ServerPlayer> engaged = this.rework.engaged();
      int n = Math.min(5, 2 + Math.max(0, engaged.size() - 1));
      List<Vec3> spots = new ArrayList<>();

      for (int i = 0; i < n * 4 && spots.size() < n; i++) {
         Vec3 base;
         if (!engaged.isEmpty()) {
            ServerPlayer p = engaged.get(i % engaged.size());
            if (i < engaged.size()) {
               base = DragonRework.predict(p, 30.0);
            } else {
               double a = r.nextDouble() * Math.PI * 2.0;
               double d = 6.0 + r.nextDouble() * 8.0;
               base = p.position().add(Math.cos(a) * d, 0.0, Math.sin(a) * d);
            }
         } else {
            double a = r.nextDouble() * Math.PI * 2.0;
            double d = 8.0 + r.nextDouble() * 22.0;
            base = portal.add(Math.cos(a) * d, 0.0, Math.sin(a) * d);
         }

         Vec3 g = DragonFx.groundBelow(level, base.add(0.0, 3.0, 0.0));
         if (g != null && !(DragonFx.flatDist(g, portal) > 120.0)) {
            boolean crowded = false;

            for (Vec3 s : spots) {
               if (s.distanceTo(g) < 8.0) {
                  crowded = true;
                  break;
               }
            }

            if (!crowded) {
               spots.add(g);
            }
         }
      }

      int now = this.rework.dragon().tickCount;

      for (Vec3 g : spots) {
         Vfx.telegraphCircle(level, g, 3.0, 30, DragonFx.V);
         this.rifts.add(new DragonHazards.Rift(g, now + 30, now + 170));
      }
   }

   private void tickRifts(ServerLevel level, int now) {
      Iterator<DragonHazards.Rift> it = this.rifts.iterator();

      while (it.hasNext()) {
         DragonHazards.Rift rift = it.next();
         if (!rift.opened && now >= rift.open) {
            rift.opened = true;
            Vfx.vortex(level, rift.center.add(0.0, 0.1, 0.0), 3.5, 5.0, DragonFx.V, 140);
            DragonFx.sound(level, rift.center, "void_fissure.hit", 3.0F, 0.7F);
         }

         if (rift.opened) {
            boolean burn = (now - rift.open) % 20 == 0;

            for (ServerPlayer p : DragonFx.players(level, rift.center, 5.0)) {
               double dy = p.getY() - rift.center.y;
               if (!(DragonFx.flatDist(p.position(), rift.center) > 3.5) && !(dy < -1.0) && !(dy > 5.0)) {
                  Vec3 in = DragonFx.flat(rift.center.subtract(p.position()));
                  if (in.lengthSqr() > 0.04) {
                     DragonFx.nudge(p, in.normalize().scale(0.05));
                  }

                  if (burn) {
                     DragonFx.hurt(level, this.rework.dragon(), p, this.rework.dmg(2.0F), true);
                  }
               }
            }
         }

         if (now >= rift.close) {
            Vfx.nova(level, rift.center.add(0.0, 0.5, 0.0), DragonFx.V, 3.5);
            it.remove();
         }
      }
   }

   void startEdge() {
      if (!this.edge) {
         this.edge = true;
         this.edgeTicks = 0;
         this.warned.clear();
      }
   }

   void stopEdge() {
      this.edge = false;
   }

   double edgeRadius() {
      return this.edgeTicks >= 1200 ? 30.0 : 50.0 - 20.0 * this.edgeTicks / 1200.0;
   }

   private void tickEdge(ServerLevel level, Vec3 portal) {
      this.edgeTicks++;
      double radius = this.edgeRadius();
      if (this.edgeTicks % 10 == 0) {
         int offset = this.edgeTicks / 10 % 2;

         for (int i = offset; i < 48; i += 2) {
            double a = i / 48.0 * Math.PI * 2.0;
            double x = portal.x + Math.cos(a) * radius;
            double z = portal.z + Math.sin(a) * radius;
            Vec3 g = DragonFx.ground(level, x, z);
            double y = g != null ? g.y : portal.y;
            Vfx.smoke(level, new Vec3(x, y + 0.6, z), 657930, 3, 0.5, 2.4);
            if (i % 3 == 0) {
               level.sendParticles(ParticleTypes.END_ROD, x, y + 0.8, z, 2, 0.3, 1.2, 0.3, 0.02);
            }
         }
      }

      if (this.edgeTicks % 20 == 0) {
         EnderDragon dragon = this.rework.dragon();

         for (ServerPlayer p : level.players()) {
            if (p.isAlive() && !p.isSpectator() && !p.isCreative()) {
               double d = DragonFx.flatDist(p.position(), portal);
               if (!(d <= radius) && !(d > 200.0)) {
                  DragonFx.hurt(level, dragon, p, this.rework.dmg(3.0F), true);
                  p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false), dragon);
                  if (this.warned.add(p.getUUID())) {
                     p.sendOverlayMessage(Component.translatableWithFallback("message.mysticalterrain.dragon.edge", "The void closes in"));
                  }
               }
            }
         }
      }
   }

   void meteor(ServerLevel level, Vec3 at, boolean big) {
      RandomSource r = this.rework.dragon().getRandom();
      Vec3 from = at.add((r.nextDouble() * 2.0 - 1.0) * 6.0, 30.0, (r.nextDouble() * 2.0 - 1.0) * 6.0);
      Vfx.telegraphCircle(level, at, big ? 5.0 : 3.5, 20, DragonFx.V);
      this.meteors.add(new DragonHazards.Meteor(from, at, big));
   }

   private void tickMeteors(ServerLevel level) {
      Iterator<DragonHazards.Meteor> it = this.meteors.iterator();

      while (it.hasNext()) {
         DragonHazards.Meteor m = it.next();
         m.age++;
         Vec3 velocity = m.to.subtract(m.from).scale(0.05);
         Vec3 at = m.from.add(velocity.scale(m.age));
         Vfx.meteorTrail(level, at, velocity, DragonFx.V);
         if (m.age % 3 == 0) {
            Vfx.flare(level, at, 16777215, m.big ? 2.4 : 1.6, 4);
         }

         if (m.age >= 20) {
            it.remove();
            this.impact(level, m);
         }
      }
   }

   private void impact(ServerLevel level, DragonHazards.Meteor m) {
      double radius = m.big ? 5.0 : 3.5;
      float base = m.big ? 18.0F : 12.0F;
      EnderDragon dragon = this.rework.dragon();
      Vfx.heavyImpact(level, m.to, DragonFx.V, m.big ? 1.8 : 1.2);

      for (ServerPlayer p : DragonFx.players(level, m.to, radius + 2.0)) {
         double d = DragonFx.flatDist(p.position(), m.to);
         if (!(d > radius) && !(Math.abs(p.getY() - m.to.y) > 4.0)) {
            DragonFx.hurt(level, dragon, p, this.rework.dmg(base) * (float)(1.0 - 0.5 * d / radius), false);
            Vec3 out = DragonFx.away(m.to, p.position());
            DragonFx.launch(p, new Vec3(out.x * 0.8, 0.4, out.z * 0.8));
         }
      }

      if (this.rework.fightPhase() >= 2) {
         DragonFx.pool(level, dragon, m.to, m.big ? 3.5F : 2.5F, 100);
      }

      DragonFx.sound(level, m.to, m.big ? "slam.hit" : "fireball.hit", m.big ? 6.0F : 3.0F, 0.85F + dragon.getRandom().nextFloat() * 0.3F);
   }

   void shroud(ServerLevel level, EndCrystal crystal, int ticks) {
      crystal.setPermanentlyInvulnerable(true);
      Vfx.shieldBubble(level, crystal.position().add(0.0, 1.0, 0.0), 1.8, DragonFx.V, ticks);
      this.shrouds.add(new DragonHazards.Shroud(crystal, this.rework.dragon().tickCount + ticks));
   }

   private void tickShrouds(ServerLevel level, int now) {
      Iterator<DragonHazards.Shroud> it = this.shrouds.iterator();

      while (it.hasNext()) {
         DragonHazards.Shroud s = it.next();
         if (now >= s.until() || s.crystal().isRemoved()) {
            s.crystal().setPermanentlyInvulnerable(false);
            if (!s.crystal().isRemoved()) {
               Vfx.shards(level, s.crystal().position().add(0.0, 1.0, 0.0), 16777215, 12, 0.4);
            }

            it.remove();
         }
      }
   }

   private static final class Meteor {
      final Vec3 from;
      final Vec3 to;
      final boolean big;
      int age;

      Meteor(Vec3 from, Vec3 to, boolean big) {
         this.from = from;
         this.to = to;
         this.big = big;
      }
   }

   private record Pulse(EndCrystal crystal, int at) {
   }

   private static final class Rift {
      final Vec3 center;
      final int open;
      final int close;
      boolean opened;

      Rift(Vec3 center, int open, int close) {
         this.center = center;
         this.open = open;
         this.close = close;
      }
   }

   private record Shroud(EndCrystal crystal, int until) {
   }
}

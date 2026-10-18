package com.vexira.mysticalterrain.gear.arsenal;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.MysticalBoss;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.boss.vfx.VfxOptions;
import com.vexira.mysticalterrain.boss.vfx.VfxParticles;
import com.vexira.mysticalterrain.gear.Active;
import com.vexira.mysticalterrain.gear.ArmorKit;
import com.vexira.mysticalterrain.gear.Charge;
import com.vexira.mysticalterrain.gear.GearKit;
import com.vexira.mysticalterrain.gear.GearState;
import com.vexira.mysticalterrain.gear.GearUse;
import com.vexira.mysticalterrain.gear.MysticArmor;
import com.vexira.mysticalterrain.gear.MysticWeapon;
import com.vexira.mysticalterrain.gear.WeaponKit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AllowDamage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import org.jspecify.annotations.Nullable;

public final class AstraelArsenal extends Arsenal {
   private static final Vfx.Palette STAR = Vfx.Palette.STAR;
   private static final Vfx.Palette HOLY = Vfx.Palette.HOLY;
   private static final Vfx.Palette VOID = Vfx.Palette.VOID;
   private static final int PALE = 16774870;
   private static final int WHITE = 16777215;
   private static final String SPEAR = "starspear";
   private static final String CHAKRAM = "halo_chakram";
   private static final String SWORD = "nova_greatsword";
   private static final Set<String> WEAPONS = Set.of("starspear", "halo_chakram", "nova_greatsword");
   private static final String ZENITH = "halo_chakram.zenith";
   private static final String WARD = "set.astrael.ward";
   private static final int BRAND_TICKS = 160;
   private static final int TASK_CAP = 24;
   private static final List<AstraelArsenal.Job> TASKS = new ArrayList<>();
   private static final Map<LivingEntity, long[]> BRANDS = new WeakHashMap<>();
   private static final Map<UUID, Long> NO_FALL = new HashMap<>();
   private static final Map<UUID, Integer> ORBIT = new HashMap<>();
   private static final Map<UUID, Integer> CORE = new HashMap<>();
   private static final Map<UUID, AstraelArsenal.Orrery> ORRERY = new HashMap<>();
   private static final Set<UUID> GLIDERS = new HashSet<>();
   private static final Set<UUID> WARDED = new HashSet<>();
   private static boolean hooked;

   public AstraelArsenal() {
      super("astrael", "celestial_aegis", Vfx.Palette.STAR);
   }

   @Override
   protected void weapons() {
      this.weapon("starspear", 10.0F, -2.7F)
         .primary(Active.of(100, AstraelArsenal::dawnLance).empowered(40, 1.25F))
         .alternate(240, AstraelArsenal::cometDive)
         .charge(
            Charge.of(3, 10).pose(ItemUseAnimation.TRIDENT).onLevel(AstraelArsenal::lanceLevel).cast(Active.of(360, AstraelArsenal::meteorLance).empowered(60, 1.25F))
         )
         .passive(AstraelArsenal::starbrand)
         .resonancePerHit(10.0F);
      this.weapon("halo_chakram", 6.0F, -1.8F)
         .primary(Active.of(80, AstraelArsenal::haloRicochet).empowered(45, 1.0F))
         .alternate(180, AstraelArsenal::starstep)
         .charge(Charge.of(3, 8).pose(ItemUseAnimation.BOW).onLevel(AstraelArsenal::orreryLevel).cast(320, AstraelArsenal::orreryOfBlades))
         .passive(AstraelArsenal::orbitCut)
         .resonancePerHit(6.0F);
      this.weapon("nova_greatsword", 13.0F, -3.1F)
         .primary(Active.of(160, AstraelArsenal::zenithCleave).empowered(50))
         .alternate(280, AstraelArsenal::gravityWell)
         .charge(Charge.of(3, 14).pose(ItemUseAnimation.BLOCK).onLevel(AstraelArsenal::novaLevel).cast(Active.of(440, AstraelArsenal::supernova).empowered(70)))
         .passive(AstraelArsenal::starfireCore)
         .resonancePerHit(15.0F);
   }

   @Override
   protected void armor(ArmorKit set) {
      set.twoPiece(AstraelArsenal::starlitGrace);
      set.fourPiece(AstraelArsenal::aegisTick);
      set.onHurt(AstraelArsenal::aegisHurt);
      set.active(600, AstraelArsenal::constellation);
   }

   static boolean dawnLance(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float power = u.power();
      boolean emp = u.empowered();
      Vec3 start = p.position();
      Vec3 eye = GearKit.eye(p);
      Vec3 look = p.getLookAngle();
      Vec3 tip = GearKit.tip(p);
      if (emp) {
         Vfx.runeCircle(level, feet(level, start), 1.6, STAR, 16, 0.08F, Vfx.Rune.ASTRAL);
      }

      GearKit.dash(p, 1.1, 0.1);
      double reach = reach(level, p, eye, look, emp ? 14.0 : 9.0);
      Vec3 end = eye.add(look.scale(reach));

      for (LivingEntity e : GearKit.alongLine(level, p, eye, look, reach, 1.0)) {
         Vec3 c = centre(e);
         smite(level, p, e, 13.0F * power);
         e.push(look.x * 0.4, 0.1, look.z * 0.4);
         e.syncVelocity = true;
         brand(level, p, e, 1, "starspear");
         u.resonate(4.0F);
         Vfx.impact(level, c, STAR, 0.5);
         Vfx.glint(level, c, 16777215, 0.9);
         if (emp) {
            fallingStar(level, p, ground(level, e.position()), 12, 2.0, 8.0F * power);
         }
      }

      Vfx.dashTrail(level, start.add(0.0, 1.0, 0.0), start.add(GearKit.flatLook(p).scale(4.0)).add(0.0, 1.0, 0.0), STAR);
      Vfx.beam(level, tip, end, STAR, 0.5, 6);
      Vfx.beam(level, tip, end, HOLY, 0.18, 4);
      Vfx.streaks(level, tip, look, STAR, 12);
      Vfx.glint(level, end, 16774870, 1.6);
      Vfx.flare(level, end, STAR.glow(), 1.8, 6);
      GearKit.muzzle(level, p, STAR, 1.2);
      GearKit.kick(level, p, 0.2);
      return true;
   }

   static boolean cometDive(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      if (!p.isFallFlying() && !p.isInWater() && !p.isPassenger()) {
         Vec3 from = p.position();
         Vec3 below = groundOrNull(level, from);
         Vec3 aim = groundOrNull(level, GearKit.lookPoint(level, p, 16.0));
         if (aim == null) {
            aim = below;
         }

         if (aim == null) {
            return false;
         }

         double h = new Vec3(aim.x - from.x, 0.0, aim.z - from.z).length();
         Vec3 target = h < 3.0 && below != null ? below : aim;
         Vec3 dir = h < 3.0 ? Vec3.ZERO : new Vec3(aim.x - from.x, 0.0, aim.z - from.z).scale(1.0 / h);
         double hs = Math.min(0.9, h * 0.06);
         p.setDeltaMovement(dir.x * hs, 1.15, dir.z * hs);
         p.syncVelocity = true;
         p.resetFallDistance();
         NO_FALL.put(p.getUUID(), level.getGameTime() + 60L);
         Vfx.pillar(level, from, STAR, 4.0, 0.8);
         Vfx.streaks(level, from, new Vec3(0.0, 1.0, 0.0), STAR, 10);
         Vfx.ring(level, from, STAR.glow(), 2.2, 10);
         Vfx.debris(level, from, Vfx.Debris.FEATHER, 16774870, 10, 0.3);
         Vfx.telegraphCircle(level, target, 4.5, 22, STAR);
         run(level, p, new AstraelArsenal.Dive(p, target, u.power()));
         return true;
      } else {
         return false;
      }
   }

   static void lanceLevel(ServerLevel level, Player p, WeaponKit kit, int c, boolean max) {
      Vec3 tip = GearKit.tip(p);
      Vfx.gather(level, tip, STAR, 1.2, 6 + 2 * c);
      Vfx.glint(level, tip, 16774870, 0.6 + 0.3 * c);
      Vfx.ring(level, p.position(), STAR.glow(), 0.8 + 0.5 * c, 8);
      if (max) {
         Vfx.chargeUp(level, tip, STAR, 10);
         Vfx.runeCircle(level, feet(level, p.position()), 1.4, STAR, 20, 0.08F, Vfx.Rune.ASTRAL);
      }
   }

   static boolean meteorLance(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      int c = Mth.clamp(u.charge(), 1, 3);
      Vec3 start = GearKit.tip(p);
      Vec3 end = GearKit.lookPoint(level, p, 40.0);
      Vec3 dir = end.subtract(start);
      double length = dir.length();
      dir = length < 0.001 ? p.getLookAngle() : dir.scale(1.0 / length);
      GearKit.muzzle(level, p, STAR, 1.6);
      Vfx.streaks(level, start, dir, STAR, 14);
      GearKit.kick(level, p, 0.25);
      run(level, p, new AstraelArsenal.Javelin(p, GearKit.eye(p), start, end, dir, length, c, u.power(), u.empowered()));
      return true;
   }

   static void starbrand(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      GearKit.hitSpark(level, t, STAR);
      if (a instanceof Player p) {
         brand(level, p, t, 1, "starspear");
      }
   }

   static boolean haloRicochet(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float power = u.power();
      Vec3 look = p.getLookAngle();
      AstraelArsenal.Refund refund = new AstraelArsenal.Refund();
      LivingEntity target = GearKit.lookTarget(level, p, 24.0);
      run(level, p, new AstraelArsenal.Halo(p, power, look, target, target != null ? 0 : 12, false, refund));
      if (u.empowered()) {
         for (int side = -1; side <= 1; side += 2) {
            run(level, p, new AstraelArsenal.Halo(p, power, look.yRot(side * 25 * (float) (Math.PI / 180.0)), null, 8, true, refund));
         }

         Vfx.runeCircle(level, feet(level, p.position()), 1.4, STAR, 12, 0.1F, Vfx.Rune.ASTRAL);
      }

      GearKit.muzzle(level, p, STAR, 0.9);
      return true;
   }

   static boolean starstep(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      if (p.isPassenger()) {
         return false;
      }

      Vec3 from = p.position();
      LivingEntity t = GearKit.lookTarget(level, p, 16.0);
      Vec3 want;
      if (t != null) {
         want = t.position().add(Vec3.directionFromRotation(0.0F, t.getYRot()).scale(-(1.8 + t.getBbWidth() / 2.0F)));
      } else {
         Vec3 at = GearKit.lookPoint(level, p, 12.0).subtract(p.getLookAngle().scale(0.8));
         Vec3 g = groundOrNull(level, at);
         want = g != null && at.y - g.y <= 2.2 ? g : at.subtract(0.0, 1.0, 0.0);
      }

      Vec3 dest = fit(level, p, from, want);
      if (dest == null) {
         return false;
      }

      float power = u.power();
      p.teleportTo(dest.x, dest.y, dest.z);
      p.resetFallDistance();
      if (t != null) {
         float yaw = (float)(Mth.atan2(t.getZ() - dest.z, t.getX() - dest.x) * 180.0F / (float)Math.PI) - 90.0F;
         p.forceSetRotation(yaw, false, 0.0F, true);
      }

      Vec3 a = from.add(0.0, 1.0, 0.0);
      Vec3 b = dest.add(0.0, 1.0, 0.0);
      Vfx.dashTrail(level, a, b, STAR);
      Vfx.debris(level, a, Vfx.Debris.FEATHER, 16774870, 8, 0.25);
      Vfx.wisps(level, a, STAR, 3, 0.8, 12);
      Vfx.flare(level, b, STAR.glow(), 2.0, 6);
      Vfx.glint(level, dest.add(0.0, 1.6, 0.0), 16777215, 1.4);
      Vec3 g = feet(level, from);
      Vfx.telegraphCircle(level, g, 2.5, 10, STAR);
      later(level, p, 10, () -> {
         for (LivingEntity e : around(level, p, from, 2.5, 2.5, 2.5)) {
            zap(level, p, e, 12.0F * power);
            brand(level, p, e, 1, "halo_chakram");
         }

         Vfx.nova(level, from.add(0.0, 0.8, 0.0), STAR, 2.5);
         Vfx.heavyImpact(level, g, STAR, 0.5);
         play(level, from, "weapon.halo_chakram.burst", 1.4F, 1.1F);
      });
      u.state().cooldown("halo_chakram.zenith", 60);
      run(level, p, (l, age) -> {
         if (!lost(p, l) && age < 60 && GearState.of(p).remaining("halo_chakram.zenith") > 1) {
            if (age % 5 == 0) {
               Vfx.glint(l, GearKit.tip(p), 16774870, 0.6);
            }

            return false;
         } else {
            return true;
         }
      });
      return true;
   }

   static void orreryLevel(ServerLevel level, Player p, WeaponKit kit, int c, boolean max) {
      Vec3 base = p.position();
      Vfx.ring(level, base.add(0.0, 1.1, 0.0), STAR.glow(), 0.8 + 0.45 * c, 8);
      double a0 = Math.toRadians(p.getYRot() + 90.0F);
      int n = 1 + c;

      for (int k = 0; k < n; k++) {
         double a = a0 + (Math.PI * 2) * k / n;
         Vfx.glint(level, base.add(Math.cos(a) * 2.2, 1.1, Math.sin(a) * 2.2), 16774870, 0.5 + 0.1 * c);
      }

      if (max) {
         Vfx.wisps(level, base.add(0.0, 1.1, 0.0), STAR, 4, 2.4, 20);
         Vfx.runeCircle(level, feet(level, base), 2.4, STAR, 20, 0.1F, Vfx.Rune.ASTRAL);
      }
   }

   static boolean orreryOfBlades(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      AstraelArsenal.Orrery o = new AstraelArsenal.Orrery(p, Mth.clamp(u.charge(), 1, 3), Math.toRadians(p.getYRot() + 90.0F));
      if (!run(level, p, o)) {
         return false;
      }

      AstraelArsenal.Orrery old = ORRERY.put(p.getUUID(), o);
      if (old != null) {
         old.cancelled = true;
      }

      Vfx.auraRing(level, p.position(), STAR, 1.4);
      return true;
   }

   static void orbitCut(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      GearKit.hitSpark(level, t, STAR);
      if (a instanceof Player p) {
         GearState st = GearState.of(p);
         if (st.remaining("halo_chakram.zenith") > 1) {
            Vec3 c = centre(t);
            st.cooldown("halo_chakram.zenith", 1);
            zap(level, p, t, 8.0F);
            brand(level, p, t, 2, "halo_chakram");
            Vfx.slashArc(level, p.position().add(0.0, 1.2, 0.0), p.getYRot(), p.getXRot(), -90.0F, 2.6, STAR, 200.0F);
            Vfx.glint(level, c, 16777215, 1.6);
            Vfx.flare(level, c, STAR.core(), 1.8, 6);
            GearKit.kick(level, p, 0.2);
            GearKit.procSound(level, t, "halo_chakram");
         }

         int n = ORBIT.merge(p.getUUID(), 1, Integer::sum);
         if (n >= 3) {
            ORBIT.put(p.getUUID(), 0);
            shard(level, p, t);
         }
      }
   }

   private static void shard(ServerLevel level, Player p, LivingEntity t) {
      Vec3 from = centre(t);
      List<LivingEntity> near = new ArrayList<>(GearKit.enemies(level, p, from, 6.0));
      near.remove(t);
      near.removeIf(ex -> !t.hasLineOfSight(ex));
      near.sort(Comparator.comparingDouble(ex -> ex.distanceToSqr(t)));
      if (near.isEmpty()) {
         zap(level, p, t, 4.0F);
         Vfx.slashArc(level, from, p.getYRot() + 180.0F, 0.0F, 1.0, STAR, 360.0F);
         Vfx.glint(level, from, 16777215, 0.9);
      } else {
         Vec3 a = from;

         for (int k = 0; k < Math.min(2, near.size()); k++) {
            LivingEntity e = near.get(k);
            Vec3 b = centre(e);
            zap(level, p, e, 5.0F);
            brand(level, p, e, 1, "halo_chakram");
            Vfx.dashTrail(level, a, b, STAR);
            Vfx.glint(level, b, 16777215, 0.9);
            Vfx.slashArc(level, b, yawOf(b.subtract(a)), 0.0F, 1.0, STAR, 360.0F);
            a = b;
         }
      }

      GearKit.procSound(level, t, "halo_chakram");
   }

   static boolean zenithCleave(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float power = u.power();
      boolean emp = u.empowered();
      Vec3 at = p.position();
      Vec3 flat = GearKit.flatLook(p);
      double cos = Math.cos(Math.toRadians(emp ? 70.0 : 45.0));

      for (LivingEntity e : around(level, p, at, 5.5, 3.0, 3.0)) {
         Vec3 to = new Vec3(e.getX() - at.x, 0.0, e.getZ() - at.z);
         if (!(to.length() > 1.0) || !(to.normalize().dot(flat) < cos)) {
            smite(level, p, e, 16.0F * power);
            brand(level, p, e, 1, "nova_greatsword");
            e.push(flat.x * 0.6, 0.2, flat.z * 0.6);
            e.syncVelocity = true;
            u.resonate(3.0F);
         }
      }

      Vec3 pivot = at.add(0.0, 1.3, 0.0);
      float pitch = Mth.clamp(p.getXRot(), -10.0F, 30.0F);
      Vfx.slashArc(level, pivot, p.getYRot(), pitch, 90.0F, 4.5, STAR, 200.0F);
      if (emp) {
         Vfx.slashArc(level, pivot, p.getYRot(), pitch, 60.0F, 4.5, STAR, 240.0F);
      }

      Vec3 slam = ground(level, at.add(flat.scale(2.5)));
      Vfx.heavyImpact(level, slam, STAR, 1.0);
      Vfx.debris(level, slam, Vfx.Debris.FEATHER, 16774870, 6, 0.3);
      GearKit.kick(level, p, 0.35);
      Vec3 f0 = ground(level, at.add(flat.scale(1.2)));
      List<Vec3> dirs = emp ? List.of(flat.yRot(0.43633232F), flat, flat.yRot(-0.43633232F)) : List.of(flat);

      for (Vec3 d : dirs) {
         Vfx.telegraphLine(level, f0, d, 9.0, 1.3, 8, STAR);
         Vfx.groundCrack(level, f0.add(d.scale(4.5)), 4.5, STAR);
      }

      later(level, p, 8, () -> {
         for (Vec3 dx : dirs) {
            fissure(level, p, f0, dx, power);
         }

         play(level, f0.add(flat.scale(4.5)), "weapon.nova_greatsword.fissure", 1.5F, 1.0F);
      });
      return true;
   }

   private static void fissure(ServerLevel level, Player p, Vec3 f0, Vec3 dir, float power) {
      for (double s = 1.5; s <= 9.01; s += 1.5) {
         Vfx.pillar(level, ground(level, f0.add(dir.scale(s))), STAR, 3.5, 0.7);
      }

      for (LivingEntity e : around(level, p, f0.add(dir.scale(4.5)), 5.8, 2.5, 2.5)) {
         double qx = e.getX() - f0.x;
         double qz = e.getZ() - f0.z;
         double along = qx * dir.x + qz * dir.z;
         double side = Math.abs(qx * dir.z - qz * dir.x);
         if (!(along < -0.5) && !(along > 9.5) && !(side > 1.3 + e.getBbWidth() / 2.0F)) {
            zap(level, p, e, 10.0F * power);
            e.push(0.0, 0.6, 0.0);
            e.syncVelocity = true;
         }
      }
   }

   static boolean gravityWell(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float power = u.power();
      Vec3 w = ground(level, GearKit.lookPoint(level, p, 18.0));
      Vec3 top = w.add(0.0, 1.0, 0.0);
      Vec3 from = GearKit.tip(p);
      Vec3 v = top.subtract(from).scale(0.16666666666666666);
      GearKit.muzzle(level, p, VOID, 1.2);
      run(level, p, (l, age) -> {
         if (gone(p, l)) {
            return true;
         }

         if (age < 6) {
            Vec3 at = from.add(v.scale(age + 1));
            Vfx.meteorTrail(l, at, v, VOID);
            Vfx.trail(l, at, VOID, 0.6);
            return false;
         }

         if (age == 6) {
            Vfx.vortex(l, w, 4.5, 3.5, VOID, 30);
            Vfx.runeCircle(l, w, 4.5, VOID, 30, -0.08F, Vfx.Rune.ASTRAL);
            Vfx.telegraphCircle(l, w, 4.5, 30, VOID);
            Vfx.flare(l, top, VOID.glow(), 2.4, 8);
         }

         if (age <= 35) {
            pull(l, p, w, 7.0, 0.16, 0.04);
            if (age % 5 == 0) {
               Vfx.gather(l, top, VOID, 3.0, 6);
            }

            return false;
         } else {
            for (LivingEntity e : around(l, p, w, 4.5, 2.0, 4.5)) {
               zap(l, p, e, 18.0F * power);
               e.push(0.0, 0.8, 0.0);
               e.syncVelocity = true;
               brand(l, p, e, 1, "nova_greatsword");
               GearState.of(p).addResonance("nova_greatsword", 4.0F);
            }

            Vfx.nova(l, w.add(0.0, 0.5, 0.0), STAR, 5.0);
            Vfx.explosion(l, top, VOID, 2.5);
            Vfx.heavyImpact(l, w, STAR, 1.0);
            Vfx.glint(l, w.add(0.0, 1.2, 0.0), 16777215, 2.0);
            play(l, w, "weapon.nova_greatsword.collapse", 1.6F, 1.0F);
            return true;
         }
      });
      return true;
   }

   private static double novaRadius(int c) {
      return 3.5 + 1.5 * c;
   }

   static void novaLevel(ServerLevel level, Player p, WeaponKit kit, int c, boolean max) {
      Vec3 g = feet(level, p.position());
      Vec3 core = p.position().add(0.0, 1.2, 0.0);
      Vfx.telegraphCircle(level, g, novaRadius(c), 14, STAR);
      Vfx.flare(level, core, STAR.core(), 0.8 + 0.4 * c, 6);
      Vfx.gather(level, core, STAR, 1.5 + 0.5 * c, 6 + 2 * c);
      if (max) {
         Vfx.chargeUp(level, core, STAR, 14);
         Vfx.runeCircle(level, g, novaRadius(3), STAR, 20, 0.06F, Vfx.Rune.SOLAR);
      }
   }

   static boolean supernova(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      int c = Mth.clamp(u.charge(), 1, 3);
      float power = u.power();
      double r = novaRadius(c);
      Vec3 at = p.position();
      Vec3 g = feet(level, at);

      for (LivingEntity e : around(level, p, at, r, 4.0, 4.0)) {
         zap(level, p, e, (12 + 8 * c) * power);
         GearKit.knockAway(e, at, 1.0 + 0.3 * c, 0.45);
         e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), p);
         brand(level, p, e, 1, "nova_greatsword");
         u.resonate(3.0F);
      }

      Vfx.nova(level, at.add(0.0, 1.0, 0.0), STAR, r);
      Vfx.pillar(level, at, STAR, 6 + 2 * c, 1.2);
      Vfx.shockwave(level, g, STAR, r);
      Vfx.heavyImpact(level, g, STAR, 0.8 + 0.4 * c);
      Vfx.debris(level, at.add(0.0, 1.0, 0.0), Vfx.Debris.FEATHER, HOLY.glow(), 12, 0.5);
      if (c >= 3) {
         Vfx.impactFrame(level, at, 10.0, 16774870, 0.7F);
         Vfx.runeCircle(level, g, 5.0, STAR, 60, 0.04F, Vfx.Rune.SOLAR);
         zone(level, p, g, 5.0, 2.5, 60, 10, ex -> zap(level, p, ex, 3.0F));
         p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0));
      }

      if (u.empowered()) {
         collapse(level, p, at, c, power);
      }

      return true;
   }

   private static void collapse(ServerLevel level, Player p, Vec3 c, int charge, float power) {
      run(level, p, (l, age) -> {
         if (gone(p, l)) {
            return true;
         }

         if (age == 16) {
            Vfx.vortex(l, c, 5.0, 3.0, VOID, 16);
         }

         if (age < 32) {
            if (age >= 16) {
               pull(l, p, c, 10.0, 0.2, 0.05);
            }

            return false;
         } else {
            for (LivingEntity e : around(l, p, c, 5.0, 3.0, 3.0)) {
               zap(l, p, e, (12 + 8 * charge) * 0.6F * power);
               GearKit.knockAway(e, c, 1.2, 0.4);
            }

            Vfx.nova(l, c.add(0.0, 1.0, 0.0), VOID, 5.0);
            Vfx.heavyImpact(l, c, VOID, 1.5);
            Vfx.explosion(l, c.add(0.0, 1.0, 0.0), STAR, 3.0);
            play(l, c, "weapon.nova_greatsword.collapse", 1.6F, 0.9F);
            return true;
         }
      });
   }

   static void starfireCore(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      GearKit.hitSpark(level, t, STAR);
      if (a instanceof Player p) {
         Vec3 c = centre(t);
         int n = consume(t);
         if (n > 0 && t.isAlive()) {
            zap(level, p, t, 4 * n);
            Vfx.gather(level, c, VOID, 1.6, 6 + 2 * n);
            Vfx.impact(level, c, VOID, 0.4 + 0.2 * n);
            Vfx.glint(level, c, 16767231, 0.8 + 0.3 * n);
            GearState.of(p).addResonance("nova_greatsword", 5 * n);
            GearKit.procSound(level, t, "nova_greatsword");
         }

         int k = CORE.merge(p.getUUID(), 1, Integer::sum);
         if (k >= 4) {
            CORE.put(p.getUUID(), 0);
            if (t.isAlive()) {
               zap(level, p, t, 7.0F);
            }

            for (LivingEntity e : around(level, p, c, 2.5, 2.5, 2.5)) {
               if (e != t) {
                  zap(level, p, e, 7.0F);
               }
            }

            Vfx.nova(level, c, STAR, 2.5);
            Vfx.flare(level, c, STAR.core(), 2.0, 6);
            Vfx.sparks(level, c, STAR.glow(), 10, 0.35, 0.18);
            GearKit.procSound(level, t, "nova_greatsword");
         }
      }
   }

   static void starlitGrace(ServerLevel level, Player p, int pieces) {
      if (level.getGameTime() % 20L == 0L) {
         GearKit.keep(p, MobEffects.JUMP_BOOST, 0);
         if (level.isDarkOutside() && level.canSeeSky(p.blockPosition().above())) {
            keepTicking(p, MobEffects.REGENERATION, 0);
            RandomSource r = level.getRandom();
            Vfx.glint(level, p.position().add(r.nextDouble() * 1.2 - 0.6, 2.3, r.nextDouble() * 1.2 - 0.6), 16774870, 0.5);
         }
      }

      UUID id = p.getUUID();
      boolean was = GLIDERS.contains(id);
      double dy = p.getKnownMovement().y;
      boolean glide = p.isShiftKeyDown()
         && !p.onGround()
         && !p.isFallFlying()
         && !p.isInWater()
         && !p.isPassenger()
         && !p.getAbilities().flying
         && dy < (was ? 0.02 : -0.08);
      if (glide) {
         MobEffectInstance fall = p.getEffect(MobEffects.SLOW_FALLING);
         if (fall == null || fall.isAmbient() && fall.getDuration() < 50) {
            GearKit.keep(p, MobEffects.SLOW_FALLING, 0);
         }

         GLIDERS.add(id);
         if (level.getGameTime() % 3L == 0L) {
            Vfx.trail(level, p.position().add(0.0, 0.3, 0.0), STAR, 0.35);
            Vfx.debris(level, p.position().add(0.0, 0.8, 0.0), Vfx.Debris.FEATHER, 16774870, 1, 0.05);
         }
      } else if (was) {
         GLIDERS.remove(id);
         dropGlide(p);
      }
   }

   static void aegisTick(ServerLevel level, Player p, int pieces) {
      UUID id = p.getUUID();
      long now = level.getGameTime();
      if (!WARDED.contains(id)) {
         if (now % 20L == 0L && GearState.of(p).ready("set.astrael.ward") && !p.hasEffect(MobEffects.ABSORPTION)) {
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, -1, 1, true, false, true));
            WARDED.add(id);
            Vfx.ward(level, p.position(), 1.3, STAR, 20);
            Vfx.auraRing(level, p.position(), STAR, 1.1);
            Vfx.glint(level, p.position().add(0.0, 2.1, 0.0), 16777215, 1.2);
            play(level, p.position(), "bolt.star.launch", 0.6F, 1.3F);
         }
      } else {
         if (p.getEffect(MobEffects.ABSORPTION) == null || p.getAbsorptionAmount() <= 0.0F) {
            WARDED.remove(id);
            removeWard(p);
            GearState.of(p).cooldown("set.astrael.ward", 400);
         } else if (now % 40L == 0L) {
            Vfx.wisps(level, p.position().add(0.0, 1.0, 0.0), STAR, 2, 0.9, 40);
         }
      }
   }

   static void aegisHurt(ServerLevel level, Player p, @Nullable LivingEntity attacker, DamageSource source, float taken, int pieces) {
      UUID id = p.getUUID();
      if (pieces >= 4 && WARDED.contains(id) && !(p.getAbsorptionAmount() > 0.0F)) {
         WARDED.remove(id);
         removeWard(p);
         GearState.of(p).cooldown("set.astrael.ward", 400);
         Vec3 at = p.position();
         Vec3 mid = at.add(0.0, 1.0, 0.0);
         String weapon = held(p);

         for (LivingEntity e : around(level, p, at, 4.0, 2.5, 3.0)) {
            zap(level, p, e, 8.0F);
            GearKit.knockAway(e, at, 1.2, 0.4);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), p);
            brand(level, p, e, 1, weapon);
         }

         Vfx.shards(level, mid, 16774870, 16, 0.5);
         Vfx.nova(level, mid, STAR, 4.0);
         Vfx.debris(level, mid, Vfx.Debris.FEATHER, 16774870, 10, 0.4);
         Vfx.flash(level, at, 6.0, 16774870, 0.35F);
         play(level, at, "bolt.star.impact", 1.4F, 1.1F);
         if (attacker != null && attacker.isAlive() && !(attacker instanceof Player)) {
            fallingStar(level, p, ground(level, attacker.position()), 10, 2.0, 12.0F);
         }
      }
   }

   static boolean constellation(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      List<LivingEntity> pool = new ArrayList<>(GearKit.enemies(level, p, p.position(), 20.0));
      pool.removeIf(e -> !p.hasLineOfSight(e));
      if (pool.isEmpty()) {
         return false;
      }

      pool.sort(Comparator.<LivingEntity>comparingInt(e -> -brands(e)).thenComparingDouble(e -> e.distanceToSqr(p)));
      List<LivingEntity> stars = new ArrayList<>(pool.subList(0, Math.min(6, pool.size())));
      stars.sort(Comparator.comparingDouble(e -> Math.atan2(e.getZ() - p.getZ(), e.getX() - p.getX())));

      for (LivingEntity t : stars) {
         t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 22, 3), p);
         t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0), p);
         Vfx.glint(level, t.getEyePosition().add(0.0, 0.6, 0.0), 16774870, 1.2);
         Vfx.telegraphCircle(level, ground(level, t.position()), 1.5, 22, STAR);
      }

      Vfx.runeCircle(level, feet(level, p.position()), 2.6, STAR, 30, 0.06F, Vfx.Rune.ASTRAL);
      Vfx.auraRing(level, p.position(), STAR, 1.4);
      List<Vec3> web = web(p, stars);

      for (int k = 0; k + 1 < web.size(); k++) {
         Vfx.beam(level, web.get(k), web.get(k + 1), STAR, 0.1, 22);
      }

      run(level, p, (l, age) -> {
         if (lost(p, l)) {
            return true;
         }

         if (age < 22) {
            return false;
         }

         ignite(l, p, stars);
         return true;
      });
      return true;
   }

   private static List<Vec3> web(Player p, List<LivingEntity> stars) {
      List<Vec3> pts = new ArrayList<>();
      pts.add(GearKit.eye(p).add(0.0, -0.4, 0.0));

      for (LivingEntity t : stars) {
         pts.add(centre(t));
      }

      if (stars.size() >= 3) {
         pts.add(centre(stars.get(0)));
      }

      return pts;
   }

   private static void ignite(ServerLevel level, Player p, List<LivingEntity> chosen) {
      List<LivingEntity> stars = new ArrayList<>();

      for (LivingEntity t : chosen) {
         if (valid(t, level)) {
            stars.add(t);
         }
      }

      for (int k = 0; k < stars.size(); k++) {
         LivingEntity t = stars.get(k);
         int n = consume(t);
         Vec3 eye = t.getEyePosition();
         zap(level, p, t, 16 + 5 * n);
         Vfx.lightning(level, eye.add(0.0, 14.0, 0.0), eye, HOLY, 2);
         Vfx.heavyImpact(level, ground(level, t.position()), STAR, 0.7);
         Vfx.debris(level, centre(t), Vfx.Debris.FEATHER, HOLY.glow(), 6, 0.3);
         play(level, t.position(), "bolt.star.impact", 1.2F, 0.9F + 0.05F * k);
      }

      List<Vec3> web = web(p, stars);
      Set<LivingEntity> struck = new HashSet<>(stars);

      for (int k = 0; k + 1 < web.size(); k++) {
         Vec3 a = web.get(k);
         Vec3 b = web.get(k + 1);
         Vfx.beam(level, a, b, STAR, 0.6, 8);
         Vfx.beam(level, a, b, HOLY, 0.22, 6);
         Vec3 d = b.subtract(a);
         double len = d.length();
         if (!(len < 0.1)) {
            for (LivingEntity e : GearKit.alongLine(level, p, a, d.scale(1.0 / len), len, 1.0)) {
               if (struck.add(e)) {
                  zap(level, p, e, 10.0F);
               }
            }
         }
      }

      Vfx.nova(level, p.position().add(0.0, 1.2, 0.0), STAR, 3.0);
      String w = heldWeapon(p);
      if (w != null) {
         GearState.of(p).addResonance(w, 20.0F);
      }
   }

   static int brands(LivingEntity e) {
      long[] b = BRANDS.get(e);
      return b != null && b[1] >= e.level().getGameTime() ? (int)b[0] : 0;
   }

   static void brand(ServerLevel level, Player p, LivingEntity e, int n, String weapon) {
      if (n > 0 && e.isAlive() && !(e instanceof Player) && !(e instanceof ArmorStand)) {
         int stacks = brands(e) + n;
         if (stacks >= 3) {
            BRANDS.remove(e);
            brandBurst(level, p, e, weapon);
         } else {
            BRANDS.put(e, new long[]{stacks, level.getGameTime() + 160L});
            Vfx.glint(level, markAt(e), 16774870, 0.4 + 0.2 * stacks);
         }
      }
   }

   static int consume(LivingEntity e) {
      int n = brands(e);
      BRANDS.remove(e);
      return n;
   }

   private static void brandBurst(ServerLevel level, Player p, LivingEntity e, String weapon) {
      Vec3 c = centre(e);
      zap(level, p, e, 10.0F);

      for (LivingEntity o : around(level, p, c, 2.5, 2.5, 2.5)) {
         if (o != e) {
            zap(level, p, o, 5.0F);
         }
      }

      GearState.of(p).addResonance(weapon, 15.0F);
      GearKit.procSound(level, e, weapon);
      Vfx.nova(level, c, STAR, 2.5);
      Vfx.flare(level, c, HOLY.glow(), 2.2, 8);
      Vfx.debris(level, c, Vfx.Debris.FEATHER, 16774870, 6, 0.3);
      Vfx.glint(level, c.add(0.0, 0.6, 0.0), 16777215, 1.6);
   }

   private static Vec3 markAt(LivingEntity e) {
      return new Vec3(e.getX(), Math.max(e.getEyeY() + 0.55, e.getY() + e.getBbHeight() + 0.25), e.getZ());
   }

   private static void brandMarks(ServerLevel level) {
      long now = level.getGameTime();
      Iterator<Entry<LivingEntity, long[]>> it = BRANDS.entrySet().iterator();

      while (it.hasNext()) {
         Entry<LivingEntity, long[]> en = it.next();
         LivingEntity e = en.getKey();
         if (e == null || !e.isAlive() || e.isRemoved() || en.getValue()[1] < now) {
            it.remove();
         } else if (e.level() == level) {
            int stacks = (int)en.getValue()[0];
            Vfx.glint(level, markAt(e), 16774870, 0.3 + 0.18 * stacks);
            if (stacks >= 2) {
               Vfx.ring(level, e.position().add(0.0, 0.08, 0.0), STAR.glow(), e.getBbWidth() * 0.8 + 0.3, 10);
            }
         }
      }
   }

   static void fallingStar(ServerLevel level, Player p, Vec3 at, int delay, double r, float dmg) {
      Vfx.telegraphCircle(level, at, r, delay, STAR);
      Vec3 top = at.add(1.5, 14.0, -1.5);
      Vec3 vel = at.subtract(top).scale(0.16666666666666666);
      String weapon = held(p);
      run(level, p, (l, age) -> {
         if (gone(p, l)) {
            return true;
         }

         if (age < delay) {
            int fall = age - (delay - 6);
            if (fall >= 0) {
               Vfx.meteorTrail(l, top.add(vel.scale(fall)), vel, STAR);
            }

            return false;
         } else {
            for (LivingEntity e : around(l, p, at, r, 1.0, 6.0)) {
               zap(l, p, e, dmg);
               brand(l, p, e, 1, weapon);
            }

            Vfx.heavyImpact(l, at, STAR, 0.5);
            Vfx.glint(l, at.add(0.0, 0.4, 0.0), 16777215, 1.6);
            play(l, at, "weapon.starspear.impact", 1.2F, 1.1F + l.getRandom().nextFloat() * 0.15F);
            return true;
         }
      });
   }

   private static void zone(ServerLevel level, Player p, Vec3 c, double r, double dy, int life, int every, Consumer<LivingEntity> fx) {
      run(level, p, (l, age) -> {
         if (!gone(p, l) && age <= life) {
            if (age > 0 && age % every == 0) {
               for (LivingEntity e : around(l, p, c, r, dy, dy)) {
                  fx.accept(e);
               }
            }

            return false;
         } else {
            return true;
         }
      });
   }

   private static void pull(ServerLevel level, Player p, Vec3 c, double radius, double strength, double bossStrength) {
      for (LivingEntity e : around(level, p, c, radius, 3.0, 4.0)) {
         Vec3 to = new Vec3(c.x - e.getX(), 0.0, c.z - e.getZ());
         double d = to.length();
         double s = heavy(e) ? bossStrength : strength;
         if (!(d < 0.6) && !(s <= 0.0)) {
            e.setDeltaMovement(e.getDeltaMovement().add(to.scale(Math.min(s, d * 0.5) / d)));
            e.syncVelocity = true;
         }
      }
   }

   private static boolean heavy(LivingEntity e) {
      return e instanceof MysticalBoss b && b.isBoss() || e instanceof EnderDragon;
   }

   private static void smite(ServerLevel level, Player p, LivingEntity e, float dmg) {
      int frames = e.getInvulnerableTime();
      e.setInvulnerableTime(0);
      GearKit.strike(level, p, e, dmg);
      e.setInvulnerableTime(frames);
   }

   private static void zap(ServerLevel level, Player p, LivingEntity e, float dmg) {
      int frames = e.getInvulnerableTime();
      e.setInvulnerableTime(0);
      GearKit.magic(level, p, e, dmg);
      e.setInvulnerableTime(frames);
   }

   private static List<LivingEntity> around(ServerLevel level, Player p, Vec3 c, double r, double down, double up) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, c.add(0.0, (up - down) / 2.0, 0.0), r + Math.max(up, down) + 3.0)) {
         double dx = e.getX() - c.x;
         double dz = e.getZ() - c.z;
         double reach = r + e.getBbWidth() / 2.0F;
         if (dx * dx + dz * dz <= reach * reach && e.getY() + e.getBbHeight() >= c.y - down && e.getY() <= c.y + up) {
            out.add(e);
         }
      }

      return out;
   }

   private static Vec3 centre(LivingEntity e) {
      return e.getBoundingBox().getCenter();
   }

   private static boolean valid(LivingEntity e, ServerLevel level) {
      return e.isAlive() && !e.isRemoved() && e.level() == level;
   }

   private static boolean lost(Player p, ServerLevel level) {
      return !p.isAlive() || p.isRemoved() || p.level() != level;
   }

   private static boolean gone(Player p, ServerLevel level) {
      return p.isRemoved() || p.level() != level;
   }

   private static float yawOf(Vec3 d) {
      return (float)(Mth.atan2(-d.x, d.z) * 180.0F / (float)Math.PI);
   }

   private static float pitchOf(Vec3 d) {
      double len = d.length();
      return len < 1.0E-6 ? 0.0F : (float)(-Math.asin(d.y / len) * 180.0F / (float)Math.PI);
   }

   private static @Nullable Vec3 groundOrNull(ServerLevel level, Vec3 at) {
      MutableBlockPos b = BlockPos.containing(at.x, at.y + 0.5, at.z).mutable();

      for (int i = 0; i < 3 && !level.getBlockState(b).getCollisionShape(level, b).isEmpty(); i++) {
         b.move(Direction.UP);
      }

      for (int i = 0; i < 24; i++) {
         b.move(Direction.DOWN);
         BlockState s = level.getBlockState(b);
         VoxelShape shape = s.getCollisionShape(level, b);
         if (!shape.isEmpty()) {
            return new Vec3(at.x, b.getY() + shape.max(Axis.Y), at.z);
         }

         if (!s.getFluidState().isEmpty()) {
            return new Vec3(at.x, b.getY() + s.getFluidState().getHeight(level, b), at.z);
         }
      }

      return null;
   }

   private static Vec3 ground(ServerLevel level, Vec3 at) {
      Vec3 g = groundOrNull(level, at);
      return g != null ? g : at;
   }

   private static Vec3 feet(ServerLevel level, Vec3 at) {
      Vec3 g = groundOrNull(level, at);
      return g != null && at.y - g.y <= 3.0 ? g : at;
   }

   private static double reach(ServerLevel level, Player p, Vec3 from, Vec3 dir, double max) {
      BlockHitResult hit = level.clip(new ClipContext(from, from.add(dir.scale(max)), Block.COLLIDER, Fluid.NONE, p));
      return hit.getType() == Type.MISS ? max : Math.max(0.5, from.distanceTo(hit.getLocation()));
   }

   private static boolean clear(ServerLevel level, Player p, Vec3 a, Vec3 b) {
      return level.clip(new ClipContext(a, b, Block.COLLIDER, Fluid.NONE, p)).getType() == Type.MISS;
   }

   private static @Nullable Vec3 fit(ServerLevel level, Player p, Vec3 from, Vec3 want) {
      List<Vec3> tries = new ArrayList<>();
      tries.add(want);
      tries.add(want.add(0.0, 1.0, 0.0));
      Vec3 back = from.subtract(want);
      double len = back.length();
      if (len > 0.001) {
         back = back.scale(1.0 / len);

         for (int k = 1; k <= 4 && k < len; k++) {
            tries.add(want.add(back.scale(k)));
         }
      }

      for (Vec3 at : tries) {
         if (level.noCollision(p, p.getBoundingBox().move(at.subtract(from)))) {
            return at;
         }
      }

      return null;
   }

   private static void shorten(Player p, String key, int ticks) {
      GearState st = GearState.of(p);
      int left = st.remaining(key);
      if (left > 0) {
         st.cooldown(key, Math.max(1, left - ticks));
      }
   }

   private static @Nullable String heldWeapon(Player p) {
      return p.getMainHandItem().getItem() instanceof MysticWeapon w && WEAPONS.contains(w.gearId()) ? w.gearId() : null;
   }

   private static String held(Player p) {
      String w = heldWeapon(p);
      return w != null ? w : "starspear";
   }

   private static void play(ServerLevel level, Vec3 at, String key, float volume, float pitch) {
      SoundEvent e = BossSounds.event(key);
      if (e != null) {
         level.playSound(null, at.x, at.y, at.z, e, SoundSource.PLAYERS, volume, pitch);
      }
   }

   private static void keepTicking(Player p, Holder<MobEffect> effect, int amplifier) {
      MobEffectInstance cur = p.getEffect(effect);
      if (cur == null || cur.isAmbient() && cur.getAmplifier() == amplifier && cur.getDuration() < 40) {
         p.addEffect(new MobEffectInstance(effect, 120, amplifier, true, false, true));
      }
   }

   private static void dropGlide(Player p) {
      MobEffectInstance fall = p.getEffect(MobEffects.SLOW_FALLING);
      if (fall != null && fall.isAmbient() && !fall.isVisible() && fall.getDuration() <= 70) {
         p.removeEffect(MobEffects.SLOW_FALLING);
      }
   }

   private static boolean ourWard(MobEffectInstance e) {
      return e.isAmbient() && !e.isVisible() && e.getAmplifier() == 1 && e.isInfiniteDuration();
   }

   private static void removeWard(Player p) {
      MobEffectInstance abs = p.getEffect(MobEffects.ABSORPTION);
      if (abs != null && ourWard(abs)) {
         p.removeEffect(MobEffects.ABSORPTION);
      }
   }

   private static void tidy(Player p) {
      UUID id = p.getUUID();
      MobEffectInstance abs = p.getEffect(MobEffects.ABSORPTION);
      boolean ward = WARDED.contains(id) || abs != null && ourWard(abs);
      boolean glide = GLIDERS.contains(id);
      if (ward || glide) {
         int pieces = MysticArmor.pieces(p, "astrael");
         if (ward && pieces < 4) {
            if (WARDED.remove(id)) {
               GearState.of(p).cooldown("set.astrael.ward", 400);
            }

            removeWard(p);
         } else if (ward) {
            WARDED.add(id);
         }

         if (glide && pieces < 2) {
            GLIDERS.remove(id);
            dropGlide(p);
         }
      }
   }

   private static void hook() {
      if (!hooked) {
         hooked = true;
         ServerTickEvents.END_LEVEL_TICK.register(AstraelArsenal::worldTick);
         ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> reset());
         ServerLivingEntityEvents.ALLOW_DAMAGE
            .register(
               (AllowDamage)(entity, source, amount) -> !(
                  entity instanceof Player p
                     && source.is(DamageTypeTags.IS_FALL)
                     && NO_FALL.getOrDefault(p.getUUID(), Long.MIN_VALUE) >= p.level().getGameTime()
               )
            );
      }
   }

   private static void reset() {
      TASKS.clear();
      BRANDS.clear();
      NO_FALL.clear();
      ORBIT.clear();
      CORE.clear();
      ORRERY.clear();
      GLIDERS.clear();
      WARDED.clear();
   }

   private static boolean run(ServerLevel level, Player owner, AstraelArsenal.Task task) {
      UUID id = owner.getUUID();
      int live = 0;

      for (AstraelArsenal.Job j : TASKS) {
         if (!j.done && j.owner.equals(id)) {
            live++;
         }
      }

      if (live >= 24) {
         return false;
      }

      TASKS.add(new AstraelArsenal.Job(level, id, task));
      return true;
   }

   private static void later(ServerLevel level, Player p, int delay, Runnable r) {
      run(level, p, (l, age) -> {
         if (gone(p, l)) {
            return true;
         } else if (age >= delay) {
            r.run();
            return true;
         } else {
            return false;
         }
      });
   }

   private static void worldTick(ServerLevel level) {
      if (!TASKS.isEmpty()) {
         for (AstraelArsenal.Job j : new ArrayList<>(TASKS)) {
            if (j.level == level && !j.done) {
               try {
                  j.done = j.task.tick(level, j.age++);
               } catch (RuntimeException ex) {
                  j.done = true;
                  MysticalTerrain.LOGGER.error("Astrael gear effect failed", ex);
               }
            }
         }

         TASKS.removeIf(j -> j.done);
      }

      if (!BRANDS.isEmpty() && level.getGameTime() % 10L == 0L) {
         brandMarks(level);
      }

      for (ServerPlayer p : level.players()) {
         tidy(p);
      }
   }

   static {
      hook();
   }

   private static final class Dive implements AstraelArsenal.Task {
      private final Player p;
      private final Vec3 target;
      private final float power;
      private int diveAt = -1;
      private boolean rose;
      private Vec3 last;
      private int still;

      Dive(Player p, Vec3 target, float power) {
         this.p = p;
         this.target = target;
         this.power = power;
         this.last = p.position();
      }

      @Override
      public boolean tick(ServerLevel level, int age) {
         if (AstraelArsenal.lost(this.p, level)) {
            return true;
         }

         Vec3 at = this.p.position();
         if (this.diveAt < 0) {
            Vfx.trail(level, at.add(0.0, 1.0, 0.0), AstraelArsenal.STAR, 0.6);
            double vy = this.p.getKnownMovement().y;
            this.rose |= vy > 0.3;
            if (age >= 7 || this.rose && vy < 0.15) {
               this.diveAt = age;
               this.last = at;
               Vfx.flare(level, at.add(0.0, 1.0, 0.0), AstraelArsenal.STAR.core(), 2.2, 6);
            }

            return false;
         } else {
            Vec3 v = this.target.add(0.0, 0.2, 0.0).subtract(at).normalize().scale(1.7);
            this.p.setDeltaMovement(v);
            this.p.syncVelocity = true;
            this.p.resetFallDistance();
            Vfx.meteorTrail(level, at.add(0.0, 1.0, 0.0), v, AstraelArsenal.STAR);
            this.still = at.distanceTo(this.last) < 0.2 ? this.still + 1 : 0;
            this.last = at;
            boolean landed = age > this.diveAt + 1 && this.p.onGround();
            if (!landed && !(at.distanceTo(this.target) < 1.4) && this.still < 3 && age < 40) {
               return false;
            }

            this.land(level);
            return true;
         }
      }

      private void land(ServerLevel level) {
         Player p = this.p;
         Vec3 at = p.position();
         Vec3 g = AstraelArsenal.feet(level, at);

         for (LivingEntity e : AstraelArsenal.around(level, p, at, 4.5, 3.0, 3.0)) {
            AstraelArsenal.smite(level, p, e, 18.0F * this.power);
            AstraelArsenal.brand(level, p, e, 1, "starspear");
            GearKit.knockAway(e, at, 0.6, 0.75);
            GearState.of(p).addResonance("starspear", 5.0F);
         }

         Vfx.heavyImpact(level, g, AstraelArsenal.STAR, 1.0);
         Vfx.groundCrack(level, g, 3.5, AstraelArsenal.STAR);
         Vfx.nova(level, g.add(0.0, 0.3, 0.0), AstraelArsenal.STAR, 4.5);
         Vfx.debris(level, g, Vfx.Debris.FEATHER, AstraelArsenal.HOLY.glow(), 14, 0.4);
         Vfx.glint(level, g.add(0.0, 1.2, 0.0), 16777215, 2.2);
         Vfx.runeCircle(level, g, 3.0, AstraelArsenal.STAR, 60, 0.05F, Vfx.Rune.SOLAR);
         AstraelArsenal.zone(level, p, g, 3.0, 2.0, 60, 10, ex -> AstraelArsenal.zap(level, p, ex, 3.0F));
         p.setDeltaMovement(0.0, 0.05, 0.0);
         p.syncVelocity = true;
         AstraelArsenal.NO_FALL.put(p.getUUID(), level.getGameTime() + (p.onGround() ? 10 : 40));
         AstraelArsenal.play(level, at, "weapon.starspear.impact", 1.6F, 0.95F);
      }
   }

   private static final class Halo implements AstraelArsenal.Task {
      private static final int MAX_HITS = 5;
      private final Player p;
      private final float power;
      private final int straight;
      private final boolean seek;
      private final AstraelArsenal.Refund refund;
      private final Set<LivingEntity> hit = new HashSet<>();
      private @Nullable LivingEntity target;
      private Vec3 head;
      private Vec3 dir;
      private boolean back;
      private int backTicks;

      Halo(Player p, float power, Vec3 dir, @Nullable LivingEntity target, int straight, boolean seek, AstraelArsenal.Refund refund) {
         this.p = p;
         this.power = power;
         this.dir = dir.normalize();
         this.target = target;
         this.straight = straight;
         this.seek = seek;
         this.refund = refund;
         this.head = GearKit.tip(p);
      }

      @Override
      public boolean tick(ServerLevel level, int age) {
         if (AstraelArsenal.lost(this.p, level)) {
            return true;
         }

         if (this.back) {
            if (this.home(level)) {
               return true;
            }
         } else {
            this.out(level, age);
         }

         Vfx.trail(level, this.head, AstraelArsenal.STAR, 0.55);
         if (age % 2 == 0) {
            Vfx.glint(level, this.head, 16774870, 0.7);
         }

         if (age % 3 == 0) {
            Vfx.emit(
               level,
               VfxOptions.of(VfxParticles.Kind.RING, AstraelArsenal.STAR.glow(), 0.8, 4)
                  .facing(AstraelArsenal.yawOf(this.dir), AstraelArsenal.pitchOf(this.dir))
                  .spin(0.5F)
                  .build(),
               this.head,
               Vec3.ZERO
            );
         }

         return false;
      }

      private void out(ServerLevel level, int age) {
         if (this.target != null && !AstraelArsenal.valid(this.target, level)) {
            this.target = this.next(level, this.head, 8.0);
            if (this.target == null) {
               this.back = true;
               return;
            }
         }

         if (this.target == null && age >= this.straight) {
            this.target = this.seek ? this.next(level, this.head, 10.0) : null;
            if (this.target == null) {
               this.back = true;
               return;
            }
         }

         if (this.target != null) {
            Vec3 to = AstraelArsenal.centre(this.target).subtract(this.head);
            if (to.lengthSqr() > 1.0E-4) {
               this.dir = to.normalize();
            }
         }

         Vec3 prev = this.head;
         Vec3 step = prev.add(this.dir.scale(1.5));
         BlockHitResult wall = level.clip(new ClipContext(prev, step, Block.COLLIDER, Fluid.NONE, this.p));
         boolean blocked = wall.getType() != Type.MISS;
         this.head = blocked ? wall.getLocation().subtract(this.dir.scale(0.2)) : step;
         this.touch(level, prev);
         if (blocked) {
            Vfx.sparks(level, this.head, AstraelArsenal.STAR.core(), 5, 0.25, 0.14);
            this.back = true;
         }

         if (age >= 50) {
            this.back = true;
         }
      }

      private void touch(ServerLevel level, Vec3 prev) {
         for (LivingEntity e : GearKit.enemies(level, this.p, this.head, 6.0)) {
            if (this.hit.size() >= 5) {
               break;
            }

            if (!this.hit.contains(e)) {
               AABB box = e.getBoundingBox().inflate(0.5);
               Vec3 c = AstraelArsenal.centre(e);
               if (!(c.distanceTo(this.head) > 1.3) || box.contains(this.head) || !box.clip(prev, this.head).isEmpty()) {
                  this.hit.add(e);
                  this.refund.hits++;
                  AstraelArsenal.smite(level, this.p, e, 9.0F * this.power);
                  AstraelArsenal.brand(level, this.p, e, 1, "halo_chakram");
                  GearState.of(this.p).addResonance("halo_chakram", 3.0F);
                  Vfx.slashArc(level, c, AstraelArsenal.yawOf(this.dir), 0.0F, 1.3, AstraelArsenal.STAR, 360.0F);
                  Vfx.glint(level, c, 16777215, 1.1);
                  Vfx.sparks(level, c, AstraelArsenal.STAR.core(), 6, 0.3, 0.16);
                  AstraelArsenal.play(level, c, "weapon.halo_chakram.ricochet", 1.0F, 1.0F + 0.08F * this.hit.size());
                  this.target = this.hit.size() >= 5 ? null : this.next(level, c, 8.0);
                  this.back = this.target == null;
               }
            }
         }
      }

      private @Nullable LivingEntity next(ServerLevel level, Vec3 from, double radius) {
         LivingEntity best = null;
         double bestD = Double.MAX_VALUE;

         for (LivingEntity e : GearKit.enemies(level, this.p, from, radius)) {
            if (!this.hit.contains(e)) {
               Vec3 c = AstraelArsenal.centre(e);
               double d = c.distanceToSqr(from);
               if (d < bestD && AstraelArsenal.clear(level, this.p, this.head, c)) {
                  best = e;
                  bestD = d;
               }
            }
         }

         return best;
      }

      private boolean home(ServerLevel level) {
         Vec3 hand = GearKit.tip(this.p);
         Vec3 to = hand.subtract(this.head);
         if (!(to.length() <= 2.0) && ++this.backTicks <= 30) {
            this.dir = to.normalize();
            this.head = this.head.add(this.dir.scale(2.0));
            return false;
         }

         Vfx.flare(level, hand, AstraelArsenal.STAR.glow(), 1.3, 5);
         Vfx.ring(level, this.p.position().add(0.0, 1.1, 0.0), AstraelArsenal.STAR.glow(), 1.0, 6);
         int total = Math.min(40, 10 * this.refund.hits);
         if (total > this.refund.given) {
            AstraelArsenal.shorten(this.p, "halo_chakram.primary", total - this.refund.given);
            this.refund.given = total;
         }

         return true;
      }
   }

   private static final class Javelin implements AstraelArsenal.Task {
      private final Player p;
      private final Vec3 eye;
      private final Vec3 end;
      private final Vec3 dir;
      private final double length;
      private final int c;
      private final float power;
      private final boolean empowered;
      private final Set<LivingEntity> hit = new HashSet<>();
      private Vec3 head;
      private double flown;

      Javelin(Player p, Vec3 eye, Vec3 start, Vec3 end, Vec3 dir, double length, int c, float power, boolean empowered) {
         this.p = p;
         this.eye = eye;
         this.head = start;
         this.end = end;
         this.dir = dir;
         this.length = length;
         this.c = c;
         this.power = power;
         this.empowered = empowered;
      }

      @Override
      public boolean tick(ServerLevel level, int age) {
         if (AstraelArsenal.gone(this.p, level)) {
            return true;
         }

         double step = Math.max(0.0, Math.min(3.0, this.length - this.flown));
         Vec3 from = this.flown == 0.0 ? this.eye : this.head;
         Vec3 next = this.head.add(this.dir.scale(step));
         this.flown += step;
         Vfx.meteorTrail(level, next, this.dir.scale(3.0), AstraelArsenal.STAR);
         Vec3 seg = next.subtract(from);
         double segLen = seg.length();
         if (segLen > 0.001) {
            for (LivingEntity e : GearKit.alongLine(level, this.p, from, seg.scale(1.0 / segLen), segLen, 1.0)) {
               if (this.hit.add(e)) {
                  Vec3 ec = AstraelArsenal.centre(e);
                  AstraelArsenal.zap(level, this.p, e, (10 + 4 * this.c) * this.power);
                  AstraelArsenal.brand(level, this.p, e, 1, "starspear");
                  Vfx.glint(level, ec, 16777215, 1.0);
                  Vfx.impact(level, ec, AstraelArsenal.STAR, 0.4);
               }
            }
         }

         this.head = next;
         if (!(this.flown >= this.length - 0.001) && age < 20) {
            return false;
         }

         this.detonate(level);
         return true;
      }

      private void detonate(ServerLevel level) {
         Player p = this.p;
         Vec3 at = this.end;
         Vec3 g = AstraelArsenal.feet(level, at);
         double r = 2.5 + 0.5 * this.c;

         for (LivingEntity e : AstraelArsenal.around(level, p, at, r, r, r)) {
            AstraelArsenal.zap(level, p, e, (12 + 8 * this.c) * this.power);
            GearKit.knockAway(e, at, 1.0, 0.4);
            AstraelArsenal.brand(level, p, e, 1, "starspear");
            GearState.of(p).addResonance("starspear", 3.0F);
         }

         Vfx.explosion(level, at, AstraelArsenal.STAR, r);
         Vfx.heavyImpact(level, g, AstraelArsenal.STAR, 0.6 + 0.3 * this.c);
         Vfx.glint(level, at, 16777215, 2.4);
         AstraelArsenal.play(level, at, "weapon.starspear.detonate", 1.6F, 1.0F);
         if (this.c >= 3) {
            Vfx.runeCircle(level, g, 3.0, AstraelArsenal.STAR, 80, -0.05F, Vfx.Rune.ASTRAL);
            Vfx.wisps(level, g.add(0.0, 0.5, 0.0), AstraelArsenal.STAR, 4, 2.4, 80);
            AstraelArsenal.zone(level, p, g, 3.0, 2.5, 80, 20, e -> {
               AstraelArsenal.brand(level, p, e, 1, "starspear");
               e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 0), p);
            });
         }

         if (this.empowered) {
            RandomSource rnd = level.getRandom();

            for (int k = 0; k < 5; k++) {
               double a = rnd.nextDouble() * Math.PI * 2.0;
               double d = 0.5 + rnd.nextDouble() * 3.5;
               AstraelArsenal.fallingStar(
                  level, p, AstraelArsenal.ground(level, at.add(Math.cos(a) * d, 0.0, Math.sin(a) * d)), 8 + 4 * k, 2.0, 8.0F * this.power
               );
            }
         }
      }
   }

   private static final class Job {
      private final ServerLevel level;
      private final UUID owner;
      private final AstraelArsenal.Task task;
      private int age;
      private boolean done;

      Job(ServerLevel level, UUID owner, AstraelArsenal.Task task) {
         this.level = level;
         this.owner = owner;
         this.task = task;
      }
   }

   private static final class Orrery implements AstraelArsenal.Task {
      private static final int LIFE = 120;
      private final Player p;
      private final int c;
      private final int n;
      private final double r;
      private final double a0;
      private boolean cancelled;
      private int lastParry = -100;

      Orrery(Player p, int c, double a0) {
         this.p = p;
         this.c = c;
         this.n = 1 + c;
         this.r = c >= 3 ? 2.6 : 2.2;
         this.a0 = a0;
      }

      private Vec3 blade(int age, int k) {
         double a = this.a0 + age * 0.3 + (Math.PI * 2) * k / this.n;
         return this.p.position().add(Math.cos(a) * this.r, 1.1 + 0.15 * Math.sin(3.0 * a), Math.sin(a) * this.r);
      }

      @Override
      public boolean tick(ServerLevel level, int age) {
         if (this.cancelled) {
            return true;
         }

         if (!AstraelArsenal.lost(this.p, level) && age < 120) {
            for (int k = 0; k < this.n; k++) {
               Vec3 b = this.blade(age, k);
               if ((k + age) % 2 == 0) {
                  Vfx.trail(level, b, AstraelArsenal.STAR, 0.45);
               }

               if (age % 4 == 0) {
                  Vfx.glint(level, b, 16774870, 0.6);
               }
            }

            if (age % 10 == 9) {
               for (LivingEntity e : AstraelArsenal.around(level, this.p, this.p.position(), this.r + 1.0, 2.2, 2.2)) {
                  AstraelArsenal.zap(level, this.p, e, 1.5F * this.n);
                  Vfx.sparks(level, AstraelArsenal.centre(e), AstraelArsenal.STAR.core(), 3, 0.25, 0.14);
                  if (age % 20 == 19) {
                     AstraelArsenal.brand(level, this.p, e, 1, "halo_chakram");
                  }
               }
            }

            AABB guard = this.p.getBoundingBox().inflate(this.r + 0.8);

            for (Projectile pr : level.getEntitiesOfClass(Projectile.class, guard, x -> x.isAlive() && !(x.getOwner() instanceof Player))) {
               Vec3 at = pr.position();
               pr.discard();
               Vfx.glint(level, at, 16777215, 1.2);
               Vfx.sparks(level, at, AstraelArsenal.STAR.glow(), 6, 0.3, 0.16);
               Vfx.ring(level, at, AstraelArsenal.STAR.glow(), 0.8, 5);
               if (age - this.lastParry >= 4) {
                  this.lastParry = age;
                  AstraelArsenal.play(level, at, "weapon.halo_chakram.ricochet", 1.0F, 1.3F);
               }
            }

            return false;
         } else {
            if (age >= 120 && !AstraelArsenal.lost(this.p, level)) {
               this.finish(level, age);
            }

            AstraelArsenal.ORRERY.remove(this.p.getUUID(), this);
            return true;
         }
      }

      private void finish(ServerLevel level, int age) {
         Vec3 at = this.p.position();
         Vec3 mid = at.add(0.0, 1.1, 0.0);
         float dmg = 6.0F * (this.c >= 3 ? 1.5F : 1.0F);

         for (LivingEntity e : AstraelArsenal.around(level, this.p, at, 5.0, 2.5, 2.5)) {
            AstraelArsenal.zap(level, this.p, e, dmg);
            GearKit.knockAway(e, at, 0.8, 0.3);
         }

         Vfx.nova(level, mid, AstraelArsenal.STAR, 5.0);

         for (int k = 0; k < this.n; k++) {
            Vec3 b = this.blade(age, k);
            Vec3 out = new Vec3(b.x - mid.x, 0.0, b.z - mid.z);
            Vfx.streaks(level, b, out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize(), AstraelArsenal.STAR, 6);
         }

         AstraelArsenal.play(level, mid, "weapon.halo_chakram.burst", 1.3F, 1.0F);
      }
   }

   private static final class Refund {
      private int hits;
      private int given;
   }

   @FunctionalInterface
   private interface Task {
      boolean tick(ServerLevel var1, int var2);
   }
}

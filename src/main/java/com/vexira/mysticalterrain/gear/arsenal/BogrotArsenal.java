package com.vexira.mysticalterrain.gear.arsenal;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.BossFx;
import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.MysticalBoss;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.gear.Active;
import com.vexira.mysticalterrain.gear.ArmorKit;
import com.vexira.mysticalterrain.gear.Charge;
import com.vexira.mysticalterrain.gear.GearKit;
import com.vexira.mysticalterrain.gear.GearState;
import com.vexira.mysticalterrain.gear.GearUse;
import com.vexira.mysticalterrain.gear.MysticWeapon;
import com.vexira.mysticalterrain.gear.WeaponKit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class BogrotArsenal extends Arsenal {
   private static final Vfx.Palette P = Vfx.Palette.MIRE;
   private static final Vfx.Palette TOX = Vfx.Palette.TOXIC;
   private static final Vfx.Palette HEX = new Vfx.Palette(15917311, 10771711, 2364462).withDebris(Vfx.Debris.LEAF);
   private static final Vfx.Palette WISP = new Vfx.Palette(16776144, 13168704, 2765848).withDebris(Vfx.Debris.ASH);
   private static final Vfx.Palette LEECH = new Vfx.Palette(16769746, 12855872, 2757136).withDebris(Vfx.Debris.LEAF);
   private static final Vfx.Debris LEAF = Vfx.Debris.LEAF;
   private static final Vfx.Rune GLYPH = Vfx.Rune.GLYPH;
   private static final int MAX_ROT = 5;
   private static final double GLOB_G = 0.06;
   private static final String HOOK_WISP = "bogrot.hook_wisp";
   private static final String LEECH_HEAL = "bogrot.leech_heal";
   private static final String SPLASH = "bogrot.splash";
   private static final String SHROUD_KEY = "bogrot.shroud";
   private static final Identifier ABSORB = MysticalTerrain.id("bogrot_absorption");
   private static final Map<LivingEntity, BogrotArsenal.Rot> ROT = new WeakHashMap<>();
   private static final Map<LivingEntity, BogrotArsenal.Hex> HEXED = new WeakHashMap<>();
   private static final Map<LivingEntity, BogrotArsenal.Dot> BLEED = new WeakHashMap<>();
   private static final Map<LivingEntity, BogrotArsenal.Dot> LATCH = new WeakHashMap<>();
   private static final Map<LivingEntity, Map<UUID, BogrotArsenal.Hits>> LEECH_COUNT = new WeakHashMap<>();
   private static final Map<LivingEntity, Long> ATTACKER_ROT = new WeakHashMap<>();
   private static final Map<LivingEntity, Long> THORNS = new WeakHashMap<>();
   private static final Map<UUID, BogrotArsenal.Shroud> SHROUDS = new HashMap<>();
   private static final Map<UUID, Long> DRINK = new HashMap<>();
   private static final Set<UUID> CAPPED = new HashSet<>();
   private static final List<BogrotArsenal.Pool> POOLS = new ArrayList<>();
   private static final List<BogrotArsenal.Wisp> WISPS = new ArrayList<>();
   private static final List<BogrotArsenal.Wisp> NEW_WISPS = new ArrayList<>();
   private static final List<BogrotArsenal.Glob> GLOBS = new ArrayList<>();
   private static final List<BogrotArsenal.Glob> NEW_GLOBS = new ArrayList<>();
   private static final List<BogrotArsenal.Task> TASKS = new ArrayList<>();
   private static final List<BogrotArsenal.Task> PENDING = new ArrayList<>();
   private static boolean hooked;
   private static long clock;

   public BogrotArsenal() {
      super("bogrot", "mireroot_armor", Vfx.Palette.MIRE);
      hook();
   }

   private static void hook() {
      if (!hooked) {
         hooked = true;
         ServerTickEvents.END_SERVER_TICK.register(BogrotArsenal::tick);
         ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> reset());
         ServerLivingEntityEvents.AFTER_DEATH.register(BogrotArsenal::died);
      }
   }

   @Override
   protected void weapons() {
      this.weapon("hags_crook", 5.0F, -2.6F)
         .primary(Active.of(100, BogrotArsenal::witchfire).empowered(40, 1.4F))
         .alternate(Active.of(240, BogrotArsenal::drownedHands).empowered(50, 1.4F))
         .charge(Charge.of(3, 14).pose(ItemUseAnimation.TRIDENT).onLevel(BogrotArsenal::hexLevel).cast(Active.of(360, BogrotArsenal::cronesHex).empowered(60, 1.3F)))
         .passive(BogrotArsenal::cronesHook)
         .resonancePerHit(8.0F);
      this.weapon("leechblade", 8.0F, -2.4F)
         .primary(Active.of(120, BogrotArsenal::raspingCleave).empowered(40, 1.4F))
         .alternate(Active.of(280, BogrotArsenal::exsanguinate).empowered(60, 1.5F))
         .charge(Charge.of(3, 8).pose(ItemUseAnimation.SPEAR).onLevel(BogrotArsenal::lungeLevel).cast(200, BogrotArsenal::leechLunge))
         .passive(BogrotArsenal::leech)
         .resonancePerHit(10.0F);
      this.weapon("bogspitter", 3.0F, -2.4F)
         .primary(Active.of(80, BogrotArsenal::bileVolley).empowered(30, 1.3F))
         .alternate(Active.of(240, BogrotArsenal::marshfire).empowered(50, 1.4F))
         .charge(
            Charge.of(3, 12).pose(ItemUseAnimation.BOW).onLevel(BogrotArsenal::gourdLevel).cast(Active.of(360, BogrotArsenal::festeringRings).empowered(60, 1.3F))
         )
         .passive(BogrotArsenal::bileSplash)
         .resonancePerHit(6.0F);
   }

   @Override
   protected void armor(ArmorKit set) {
      set.twoPiece(BogrotArsenal::bogBlooded);
      set.fourPiece(BogrotArsenal::shroudTick);
      set.onHurt(BogrotArsenal::mirerootHurt);
      set.active(560, BogrotArsenal::embraceOfTheMire);
   }

   static boolean witchfire(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      LivingEntity first = GearKit.lookTarget(level, p, 28.0);
      List<LivingEntity> list = new ArrayList<>();
      if (first != null) {
         list.add(first);
      }

      List<LivingEntity> rest = new ArrayList<>();

      for (LivingEntity e : u.enemies(p.position(), 24.0)) {
         if (e != first && p.hasLineOfSight(e)) {
            rest.add(e);
         }
      }

      rest.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
      list.addAll(rest);
      if (list.isEmpty()) {
         return false;
      }

      boolean emp = u.empowered();
      int n = emp ? 5 : 3;
      float pw = u.power();
      Vec3 look = p.getLookAngle();
      Vec3 f = GearKit.flatLook(p);
      Vec3 side = new Vec3(-f.z, 0.0, f.x);
      Vec3 from = GearKit.tip(p);

      for (int i = 0; i < n; i++) {
         Vec3 vel = look.scale(0.35).add(side.scale((i - (n - 1) / 2.0) * 0.22)).add(0.0, 0.25, 0.0);
         BogrotArsenal.Wisp w = wisp(level, p, from, vel, list.get(i % list.size()));
         if (w == null) {
            break;
         }

         w.onHit = witchHit(7.0F * pw, emp, pw);
      }

      GearKit.muzzle(level, p, WISP, 1.0);
      Vfx.wisps(level, from, WISP, n, 0.5, 8);
      Vfx.ring(level, p.position(), WISP.glow(), 1.2, 10);
      if (emp) {
         GearKit.empowerFlourish(level, p, WISP);
      }

      return true;
   }

   private static BogrotArsenal.OnWisp witchHit(float damage, boolean fork, float pw) {
      return (w, e, at) -> {
         ServerLevel level = w.level;
         Player p = w.owner;
         hit(level, p, e, damage);
         rot(level, p, e, 1);
         resonate(p, "hags_crook", 3.0F);
         yank(e);
         Vfx.impact(level, at, WISP, 0.55);
         Vfx.flare(level, at, WISP.glow(), 1.4, 6);
         Vfx.embers(level, at, WISP.glow(), 5, 0.35);
         soundAt(level, at, 0.7F, 1.2F + level.getRandom().nextFloat() * 0.2F, "weapon.hags_crook.wisp", "weapon.hags_crook.proc");
         if (fork) {
            List<LivingEntity> others = new ArrayList<>();

            for (LivingEntity o : GearKit.enemies(level, p, at, 8.0)) {
               if (o != e) {
                  others.add(o);
               }
            }

            others.sort(Comparator.comparingDouble(ox -> ox.distanceToSqr(at)));

            for (int k = 0; k < Math.min(2, others.size()); k++) {
               LivingEntity o = others.get(k);
               Vec3 to = aimPoint(o).subtract(at);
               Vec3 vel = (to.lengthSqr() > 1.0E-4 ? to.normalize() : new Vec3(0.0, 1.0, 0.0)).scale(0.5).add(0.0, 0.12, 0.0);
               BogrotArsenal.Wisp s = wisp(level, p, at, vel, o);
               if (s == null) {
                  break;
               }

               s.size = 0.28;
               s.speed = 0.8;
               s.maxSpeed = 0.8;
               s.accel = 0.0;
               s.homing = 0.25;
               s.homingDelay = 0;
               s.life = 30;
               s.radius = 0.5;
               s.onHit = (w2, e2, at2) -> {
                  hit(level, p, e2, 3.0F * pw);
                  rot(level, p, e2, 1);
                  Vfx.impact(level, at2, WISP, 0.35);
               };
            }
         }
      };
   }

   private static void yank(LivingEntity e) {
      if (live(e) && !e.onGround() && !heavy(e)) {
         Vec3 v = e.getDeltaMovement();
         e.setDeltaMovement(v.x * 0.3, -1.2, v.z * 0.3);
         e.syncVelocity = true;
         e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1));

         for (int i = 1; i <= 6; i++) {
            schedule(i, () -> {
               if (live(e) && !e.onGround()) {
                  Vec3 m = e.getDeltaMovement();
                  e.setDeltaMovement(m.x * 0.6, Math.min(m.y, -0.9), m.z * 0.6);
                  e.syncVelocity = true;
               }
            });
         }
      }
   }

   static boolean drownedHands(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      LivingEntity t = GearKit.lookTarget(level, p, 28.0);
      Vec3 spot;
      if (t != null) {
         spot = ground(level, t.position());
      } else {
         Vec3 from = GearKit.eye(p);
         Vec3 look = p.getLookAngle();
         BlockHitResult bh = level.clip(new ClipContext(from, from.add(look.scale(24.0)), Block.COLLIDER, Fluid.NONE, p));
         if (bh.getType() == Type.MISS) {
            Vec3 f = floor(level, bh.getLocation(), 0.0, 6);
            if (f == null) {
               return false;
            }

            spot = f;
         } else {
            spot = ground(level, bh.getLocation().subtract(look.scale(0.1)));
         }
      }

      Vec3 S = spot;
      boolean emp = u.empowered();
      float pw = u.power();
      double R = emp ? 4.5 : 3.2;
      int H = emp ? 8 : 5;
      List<Vec3> hands = new ArrayList<>();
      double phase = level.getRandom().nextDouble() * Math.PI * 2.0;

      for (int i = 0; i < H; i++) {
         double a = phase + i * Math.PI * 2.0 / H;
         hands.add(ground(level, S.add(Math.cos(a) * 0.55 * R, 0.0, Math.sin(a) * 0.55 * R)));
      }

      hands.add(S);
      Vfx.telegraphCircle(level, S, R, 12, P);
      Vfx.groundCrack(level, S, R, P);
      Vfx.runeCircle(level, S, R + 0.4, HEX, 44, -0.05F, GLYPH);
      level.sendParticles(mud(), S.x, S.y + 0.2, S.z, 20, R / 2.0, 0.1, R / 2.0, 0.15);
      level.sendParticles(ParticleTypes.BUBBLE_POP, S.x, S.y + 0.2, S.z, 10, R / 2.0, 0.1, R / 2.0, 0.02);
      GearKit.muzzle(level, p, P, 0.9);
      Vfx.beam(level, GearKit.tip(p), S.add(0.0, 0.5, 0.0), HEX, 0.2, 6);
      List<LivingEntity> seized = new ArrayList<>();
      later(level, p, 12, () -> {
         for (Vec3 q : hands) {
            Vfx.pillar(level, q, P, 2.2, 0.45);
            float yaw = q == S ? level.getRandom().nextFloat() * 360.0F : yawTo(q, S);
            Vfx.claw(level, q.add(0.0, 1.2, 0.0), yaw, -70.0F, 1.6, P);
            level.sendParticles(mud(), q.x, q.y + 0.3, q.z, 6, 0.25, 0.3, 0.25, 0.12);
         }

         for (LivingEntity e : flatIn(level, p, S, R, 2.5)) {
            hit(level, p, e, 12.0F * pw);
            rot(level, p, e, 2);
            resonate(p, "hags_crook", 2.0F);
            seized.add(e);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, heavy(e) ? 1 : 5), p);
         }

         GearKit.kick(level, p, 0.2);
      });
      every(level, p, 13, 1, 27, i -> {
         for (LivingEntity e : seized) {
            if (live(e) && !heavy(e)) {
               Vec3 d = S.subtract(e.position());
               Vec3 v = e.getDeltaMovement();
               e.setDeltaMovement(d.x * 0.06, Math.min(0.0, v.y) - 0.04, d.z * 0.06);
               e.syncVelocity = true;
               e.fallDistance = 0.0;
               if (i % 4 == 0) {
                  Vfx.gather(level, e.position().add(0.0, 0.4, 0.0), P, 0.9, 3);
               }

               if (i % 8 == 0) {
                  level.sendParticles(mud(), e.getX(), e.getY() + 0.2, e.getZ(), 4, 0.3, 0.1, 0.3, 0.1);
               }
            }
         }
      });
      later(level, p, 40, () -> {
         for (LivingEntity e : flatIn(level, p, S, R, 2.5)) {
            hit(level, p, e, 10.0F * pw);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2), p);
            if (!heavy(e)) {
               e.setDeltaMovement(0.0, -1.0, 0.0);
               e.syncVelocity = true;
            }
         }

         Vfx.heavyImpact(level, S, P, 0.9);
         Vfx.shockwave(level, S, P, R);
         Vfx.debris(level, S.add(0.0, 0.3, 0.0), LEAF, P.glow(), 14, 0.4);
         level.sendParticles(mud(), S.x, S.y + 0.3, S.z, 30, R / 2.0, 0.3, R / 2.0, 0.25);
         level.sendParticles(ParticleTypes.SPLASH, S.x, S.y + 0.3, S.z, 20, R / 2.0, 0.2, R / 2.0, 0.2);
         soundAt(level, S, 1.2F, 0.9F, "weapon.hags_crook.slam", "weapon.hags_crook.alt");
         if (emp) {
            pool(level, p, S, 3.0, 100, BogrotArsenal.Kind.BLACK);
         }
      });
      return true;
   }

   static void hexLevel(ServerLevel level, Player p, WeaponKit kit, int c, boolean max) {
      Vec3 S = aim(level, p, 24.0);
      Vec3 at = GearKit.tip(p);
      Vfx.runeCircle(level, S, 2.0 + 1.5 * c, HEX, 18, 0.04F + 0.02F * c, GLYPH);
      Vfx.gather(level, at, HEX, 1.3, 6);
      Vfx.flare(level, at, HEX.glow(), 0.9 + 0.35 * c, 6);
      Vfx.ring(level, p.position(), HEX.glow(), 0.8 + 0.4 * c, 8);
      if (max) {
         Vfx.chargeUp(level, at, HEX, 10);
         Vfx.wisps(level, at, HEX, 3, 0.7, 24);
      }
   }

   static boolean cronesHex(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      int L = Math.max(1, Math.min(3, u.charge()));
      float pw = u.power();
      boolean emp = u.empowered();
      Vec3 S = aim(level, p, 24.0);
      double R = 2.0 + 1.5 * L;
      List<LivingEntity> victims = flatIn(level, p, S, R, 3.0);
      if (victims.isEmpty() && L < 3) {
         return false;
      }

      int marks = 0;

      for (LivingEntity e : victims) {
         hit(level, p, e, (5 + 2 * L) * pw);
         hex(level, p, e, 100 + 40 * L, emp, L >= 3);
         rot(level, p, e, L - 1);
         resonate(p, "hags_crook", 4.0F);
         if (marks++ < 6) {
            Vfx.runeCircle(level, ground(level, e.position()), 0.9, HEX, 30, 0.1F, GLYPH);
         }
      }

      Vfx.runeCircle(level, S, R, HEX, 30, 0.12F, GLYPH);
      Vfx.nova(level, S.add(0.0, 0.4, 0.0), HEX, R);
      Vfx.pillar(level, S, HEX, 3 + L, 0.5 + 0.1 * L);
      Vfx.beam(level, GearKit.tip(p), S.add(0.0, 1.0, 0.0), HEX, 0.3, 8);
      Vfx.flare(level, S.add(0.0, 1.0, 0.0), HEX.glow(), 2.0 + 0.5 * L, 8);
      if (L >= 3) {
         Vfx.radialFlash(level, S, 14.0, HEX.glow(), 0.35F);
      }

      if (!victims.isEmpty()) {
         soundAt(level, S, 1.0F, 1.0F, "weapon.hags_crook.hex", "weapon.hags_crook.alt");
      }

      if (emp) {
         GearKit.empowerFlourish(level, p, HEX);
      }

      if (L >= 3) {
         every(level, p, 1, 1, 20, i -> {
            for (LivingEntity ex : victims) {
               if (live(ex) && !heavy(ex)) {
                  Vec3 d = flat(S.subtract(ex.position()));
                  if (d.lengthSqr() > 0.64) {
                     Vec3 v = d.normalize().scale(0.12);
                     ex.setDeltaMovement(v.x, ex.getDeltaMovement().y, v.z);
                     ex.syncVelocity = true;
                  }
               }
            }
         });
         every(level, p, 10, 10, 6, i -> {
            Vfx.ring(level, S.add(0.0, 0.05, 0.0), HEX.glow(), R, 10);

            for (LivingEntity ex : flatIn(level, p, S, R, 3.0)) {
               if (!hexed(ex)) {
                  hex(level, p, ex, 60, false, false);
                  Vfx.flare(level, center(ex), HEX.glow(), 1.0, 5);
               }
            }
         });
      }

      return true;
   }

   static void cronesHook(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof Player p) {
         rot(level, p, t, 1);
         if (valid(t) && !heavy(t) && a.distanceTo(t) > 1.8) {
            Vec3 in = flat(a.position().subtract(t.position()));
            if (in.lengthSqr() > 1.0E-4) {
               in = in.normalize().scale(0.35);
               t.push(in.x, 0.1, in.z);
               t.syncVelocity = true;
            }
         }

         GearKit.hitSpark(level, t, P);
         Vfx.beam(level, center(t), a.getEyePosition().add(0.0, -0.4, 0.0).add(a.getLookAngle().scale(0.8)), P, 0.14);
         Vfx.ring(level, ground(level, t.position()).add(0.0, 0.05, 0.0), P.glow(), 0.8, 8);
         GearState state = GearState.of(p);
         if (hexed(t) && state.ready("bogrot.hook_wisp")) {
            state.cooldown("bogrot.hook_wisp", 20);
            LivingEntity to = nearest(level, p, t.position(), 10.0, t);
            if (to == null) {
               to = t;
            }

            Vec3 from = a.position().add(0.0, 2.4, 0.0);
            Vec3 dir = aimPoint(to).subtract(from);
            Vec3 vel = new Vec3(0.0, 0.25, 0.0).add(dir.lengthSqr() > 1.0E-4 ? dir.normalize().scale(0.25) : Vec3.ZERO);
            BogrotArsenal.Wisp w = wisp(level, p, from, vel, to);
            if (w != null) {
               w.homingDelay = 2;
               w.life = 50;
               w.onHit = (ww, e, at) -> {
                  hit(level, p, e, 4.0F);
                  rot(level, p, e, 1);
                  Vfx.impact(level, at, WISP, 0.45);
                  Vfx.flare(level, at, WISP.glow(), 1.1, 5);
                  soundAt(level, at, 0.6F, 1.3F + level.getRandom().nextFloat() * 0.2F, "weapon.hags_crook.wisp", "weapon.hags_crook.proc");
               };
               Vfx.flare(level, from, WISP.glow(), 1.0, 6);
               GearKit.procSound(level, t, "hags_crook");
            }
         }
      }
   }

   static boolean raspingCleave(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      boolean emp = u.empowered();
      float pw = u.power();
      cleave(level, p, 11.0F * pw, emp, false);
      if (emp) {
         GearKit.empowerFlourish(level, p, LEECH);
         later(level, p, 6, () -> {
            cleave(level, p, 7.7F * pw, true, true);
            GearKit.sound(level, p, 0.9F, 1.15F, "weapon.leechblade.use");
         });
      }

      GearKit.kick(level, p, 0.2);
      return true;
   }

   private static void cleave(ServerLevel level, Player p, float damage, boolean emp, boolean backhand) {
      Vec3 c = GearKit.eye(p).add(0.0, -0.4, 0.0);
      float yaw = p.getYRot();
      float pitch = p.getXRot() * 0.5F;
      if (backhand) {
         Vfx.slashArc(level, c, yaw, pitch, -20.0F, 4.8, LEECH, -200.0F);
      } else {
         Vfx.slashArc(level, c, yaw, pitch, 20.0F, 4.8, LEECH, 200.0F);
         Vfx.slashArc(level, c, yaw, pitch, 26.0F, 4.2, P, 160.0F);
      }

      Vec3 chest = chest(p);
      int n = 0;

      for (LivingEntity e : arc(level, p, 4.8, 100.0)) {
         hitStrike(level, p, e, damage);
         bleed(p, e, 60);
         rot(level, p, e, 1);
         if (!backhand) {
            resonate(p, "leechblade", 2.0F);
         }

         Vec3 at = center(e);
         Vfx.impact(level, at, LEECH, 0.45);
         Vfx.sparks(level, at, LEECH.glow(), 6, 0.25, 0.16);
         Vfx.beam(level, at, chest, LEECH, 0.16, 6);
         n++;
      }

      if (n > 0) {
         float per = emp ? 2.0F : 1.0F;
         p.heal(Math.min(emp ? 8.0F : 4.0F, per * n));
         Vfx.gather(level, chest, LEECH, 1.2, 6);
      }
   }

   static boolean exsanguinate(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      boolean emp = u.empowered();
      float pw = u.power();
      List<LivingEntity> all = new ArrayList<>();

      for (LivingEntity e : u.enemies(p.position(), 9.0)) {
         if (p.hasLineOfSight(e)) {
            all.add(e);
         }
      }

      if (all.isEmpty()) {
         return false;
      }

      all.sort(Comparator.<LivingEntity>comparingInt(e -> wounded(e) ? 0 : 1).thenComparingDouble(e -> e.distanceToSqr(p)));
      List<LivingEntity> victims = new ArrayList<>(all.subList(0, Math.min(6, all.size())));
      Vec3 chest = chest(p);

      for (LivingEntity e : victims) {
         Vfx.beam(level, center(e), chest, LEECH, 0.28, 30);
         e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, heavy(e) ? 0 : 2), p);
      }

      Vfx.runeCircle(level, ground(level, p.position()), 2.2, LEECH, 32, 0.08F, GLYPH);
      Vfx.flare(level, chest, LEECH.glow(), 1.6, 8);
      int step = emp ? 3 : 5;
      int drains = 30 / step;
      every(level, p, step, step, drains, i -> {
         Vec3 to = chest(p);

         for (LivingEntity e : victims) {
            if (live(e) && !(e.distanceTo(p) > 12.0F)) {
               Vec3 at = center(e);
               hit(level, p, e, 1.5F * pw);
               Vec3 dir = to.subtract(at);
               Vfx.streaks(level, at, dir.lengthSqr() > 1.0E-4 ? dir.normalize() : new Vec3(0.0, 1.0, 0.0), LEECH, 3);
               Vfx.sparks(level, at, LEECH.glow(), 3, 0.2, 0.14);
               Vfx.beam(level, at, to, LEECH, 0.12);
            }
         }

         if (i == drains - 1) {
            drink(level, p, victims, emp, pw);
         }
      });
      if (emp) {
         every(level, p, 1, 1, 30, i -> {
            for (LivingEntity e : victims) {
               if (live(e) && !heavy(e) && !(e.distanceTo(p) > 12.0F)) {
                  Vec3 d = flat(p.position().subtract(e.position()));
                  if (d.lengthSqr() > 2.25) {
                     d = d.normalize().scale(0.08);
                     e.push(d.x, 0.0, d.z);
                     e.syncVelocity = true;
                  }
               }
            }
         });
      }

      return true;
   }

   private static void drink(ServerLevel level, Player p, List<LivingEntity> victims, boolean emp, float pw) {
      float drunk = 0.0F;
      int n = 0;

      for (LivingEntity e : victims) {
         if (live(e) && !(e.distanceTo(p) > 12.0F)) {
            int s = 0;
            BogrotArsenal.Rot r = ROT.get(e);
            if (r != null) {
               s = r.stacks;
               r.stacks = 0;
            }

            hit(level, p, e, (5 + 3 * s) * pw);
            drunk += 1 + s;
            resonate(p, "leechblade", 2.0F);
            Vfx.impact(level, center(e), LEECH, 0.7);
            Vfx.debris(level, center(e), LEAF, TOX.glow(), 4, 0.3);
            n++;
         }
      }

      Vec3 chest = chest(p);
      Vfx.nova(level, chest, LEECH, 2.5);
      Vfx.auraRing(level, p.position(), LEECH, 1.4);
      Vfx.flare(level, chest, LEECH.glow(), 2.4, 8);
      soundAt(level, p.position(), 1.1F, 1.0F, "weapon.leechblade.drink", "weapon.leechblade.alt");
      if (n != 0) {
         float heal = Math.min(drunk, emp ? 18.0F : 12.0F);
         p.heal(heal);
         float over = Math.min(6.0F, drunk - heal);
         if (over > 0.0F) {
            DRINK.put(p.getUUID(), clock + 400L);
            absorbCap(p);
            float cur = p.getAbsorptionAmount();
            p.setAbsorptionAmount(Math.max(cur, Math.min(6.0F, cur + over)));
         }
      }
   }

   static void lungeLevel(ServerLevel level, Player p, WeaponKit kit, int c, boolean max) {
      Vec3 at = GearKit.tip(p);
      Vfx.gather(level, at, LEECH, 1.0, 5);
      Vfx.ring(level, p.position(), LEECH.glow(), 0.6 + 0.4 * c, 8);
      Vfx.flare(level, at, LEECH.glow(), 0.8 + 0.3 * c, 5);
      if (max) {
         Vfx.glint(level, at, LEECH.core(), 1.2);
         Vfx.sparks(level, at, LEECH.glow(), 8, 0.25, 0.16);
      }
   }

   static boolean leechLunge(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      int L = Math.max(1, Math.min(3, u.charge()));
      float pw = u.power();
      Vec3 dir = GearKit.flatLook(p);
      Vec3 start = p.position();
      float yaw = p.getYRot();
      int T = 6 + 2 * L;
      double v = (5.0 + 3 * L) / T;
      GearKit.dash(p, 1.1 + 0.3 * L, 0.12);
      Set<LivingEntity> struck = new HashSet<>();
      List<LivingEntity> order = new ArrayList<>();
      boolean[] stopped = new boolean[]{false};
      every(level, p, 1, 1, T, i -> {
         p.resetFallDistance();
         if (!stopped[0]) {
            Vec3 m = i > 0 && p instanceof ServerPlayer sp ? sp.getKnownMovement() : p.getDeltaMovement();
            if (i > 0 && p.horizontalCollision) {
               stopped[0] = true;
               p.setDeltaMovement(0.0, Math.min(0.0, m.y), 0.0);
            } else {
               double k = i >= T - 2 ? 0.45 : 1.0;
               p.setDeltaMovement(dir.x * v * k, m.y, dir.z * v * k);
            }

            p.syncVelocity = true;
         }

         Vec3 body = p.position().add(0.0, 0.9, 0.0);

         for (LivingEntity e : GearKit.enemies(level, p, body, 5.0)) {
            if (!struck.contains(e) && !(boxDist(e, body) > 1.8)) {
               struck.add(e);
               order.add(e);
               hitStrike(level, p, e, (8 + 4 * L) * pw);
               bleed(p, e, 60);
               rot(level, p, e, 1);
               p.heal(1.0F);
               resonate(p, "leechblade", 2.0F);
               Vec3 at = center(e);
               Vfx.claw(level, at, yaw, 0.0F, 1.6, LEECH);
               Vfx.impact(level, at, LEECH, 0.5);
            }
         }

         if (i % 2 == 0) {
            Vec3 at = p.position().add(0.0, 1.0, 0.0);
            Vfx.trail(level, at, LEECH, 0.6);
            Vfx.streaks(level, at, dir, LEECH, 4);
         }
      });
      later(level, p, T + 1, () -> {
         p.resetFallDistance();
         Vfx.dashTrail(level, start.add(0.0, 1.0, 0.0), p.position().add(0.0, 1.0, 0.0), LEECH);
         LivingEntity last = null;

         for (int k = order.size() - 1; k >= 0; k--) {
            if (live(order.get(k))) {
               last = order.get(k);
               break;
            }
         }

         if (last != null) {
            latch(p, last, 100);
            Vec3 at = center(last);
            Vfx.flare(level, at, LEECH.glow(), 1.4, 6);
            soundAt(level, at, 0.9F, 1.2F, "weapon.leechblade.proc");
         }
      });
      later(level, p, T + 6, p::resetFallDistance);
      return true;
   }

   static void leech(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof Player p) {
         GearState state = GearState.of(p);
         if (state.ready("bogrot.leech_heal")) {
            state.cooldown("bogrot.leech_heal", 10);
            p.heal(Math.min(3.5F, 1.0F + 0.5F * rotStacks(t)));
         }

         Vec3 at = center(t);
         if (valid(t)) {
            BogrotArsenal.Hits h = LEECH_COUNT.computeIfAbsent(t, k -> new HashMap<>()).computeIfAbsent(p.getUUID(), k -> new BogrotArsenal.Hits());
            if (clock - h.last > 80L) {
               h.count = 0;
            }

            h.count++;
            h.last = clock;
            if (h.count >= 3) {
               h.count = 0;
               rot(level, p, t, 1);
               bleed(p, t, 60);
               GearKit.procSound(level, t, "leechblade");
               Vfx.slashArc(level, at, a.getYRot(), 0.0F, 45.0F, 1.4, LEECH, 90.0F);
               Vfx.sparks(level, at, LEECH.glow(), 8, 0.3, 0.2);
            }
         }

         GearKit.hitSpark(level, t, LEECH);
         Vfx.beam(level, at, chest(a), LEECH, 0.12);
      }
   }

   static boolean bileVolley(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      boolean emp = u.empowered();
      float pw = u.power();
      Vec3 from = GearKit.tip(p);
      LivingEntity t = GearKit.lookTarget(level, p, 28.0);
      Vec3 a0 = t != null ? t.position() : aim(level, p, 28.0);
      int n = Mth.clamp((int)Math.round(Math.hypot(a0.x - from.x, a0.z - from.z)), 10, 24);
      Vec3 A = a0;
      if (t != null) {
         Vec3 lead = flat(t.getDeltaMovement()).scale(n);
         if (lead.lengthSqr() > 36.0) {
            lead = lead.normalize().scale(6.0);
         }

         A = ground(level, t.position().add(lead));
      }

      Vec3 f = flat(A.subtract(from));
      f = f.lengthSqr() < 0.001 ? GearKit.flatLook(p) : f.normalize();
      Vec3 perp = new Vec3(-f.z, 0.0, f.x);
      int count = emp ? 5 : 3;

      for (int k = 0; k < count; k++) {
         Vec3 land = ground(level, A.add(perp.scale((k - (count - 1) / 2.0) * 2.2)));
         Vfx.telegraphCircle(level, land, 2.0, n, TOX);
         Vec3 vel = land.subtract(from).scale(1.0 / n).add(0.0, 0.03 * (n + 1), 0.0);
         glob(level, p, from, vel, n, land, at -> globLand(level, p, at, emp, pw));
      }

      GearKit.muzzle(level, p, TOX, 1.0);
      Vfx.smoke(level, from, TOX.smoke(), 3, 0.3, 0.9);
      Vfx.sparks(level, from, TOX.core(), 6, 0.3, 0.16);
      if (emp) {
         GearKit.empowerFlourish(level, p, TOX);
      }

      return true;
   }

   private static void globLand(ServerLevel level, Player p, Vec3 at, boolean emp, float pw) {
      for (LivingEntity e : flatIn(level, p, at, 2.0, 2.0)) {
         hit(level, p, e, 7.0F * pw);
         rot(level, p, e, 1);
         e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), p);
         if (!heavy(e)) {
            GearKit.knockAway(e, at, 0.3, 0.2);
         }

         resonate(p, "bogspitter", 2.0F);
      }

      pool(level, p, at, emp ? 2.4 : 2.0, emp ? 120 : 80, BogrotArsenal.Kind.BILE);
      Vec3 up = at.add(0.0, 0.3, 0.0);
      Vfx.explosion(level, up, TOX, 2.0);
      Vfx.debris(level, up, LEAF, TOX.glow(), 6, 0.3);
      soundAt(level, at, 0.9F, 0.9F + level.getRandom().nextFloat() * 0.2F, "weapon.bogspitter.splat", "weapon.bogspitter.proc");
   }

   static boolean marshfire(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      boolean emp = u.empowered();
      float pw = u.power();
      float yaw = p.getYRot();
      float half = emp ? 50.0F : 35.0F;
      double range = emp ? 13.0 : 10.0;
      Vec3 feet = p.position();
      Vec3 dir = GearKit.flatLook(p);
      Vec3 eye = GearKit.eye(p);
      Vec3 look = p.getLookAngle();
      Vfx.telegraphCone(level, ground(level, feet), yaw, half, range, 16, WISP);

      for (int k = 0; k < 6; k++) {
         Vfx.smoke(level, eye.add(look.scale(1.2 + 1.5 * k)), TOX.smoke(), 4, 0.5 + 0.3 * k, 1.8);
      }

      Vfx.streaks(level, eye, look, TOX, 8);

      for (LivingEntity e : cone(level, p, feet, dir, half, range)) {
         e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), p);
         e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 0), p);
      }

      every(level, p, 2, 3, 5, i -> {
         for (int k = 0; k < 2; k++) {
            Vfx.smoke(level, conePoint(level, feet, dir, half, range).add(0.0, 0.8, 0.0), TOX.smoke(), 3, 0.6, 1.6);
         }
      });
      later(level, p, 16, () -> {
         for (LivingEntity e : cone(level, p, feet, dir, half, range)) {
            hit(level, p, e, 16.0F * pw);
            e.igniteForSeconds(4.0F);
            rot(level, p, e, 1);
            if (!heavy(e)) {
               GearKit.knockAway(e, feet, 0.6, 0.25);
            }

            resonate(p, "bogspitter", 1.0F);
         }

         Vfx.flare(level, GearKit.tip(p), WISP.glow(), 2.0, 6);
         GearKit.kick(level, p, 0.35);
         soundAt(level, feet.add(dir.scale(4.0)), 1.3F, 1.0F, "weapon.bogspitter.ignite", "weapon.bogspitter.alt");
         int blasts = 0;
         double cos = Math.cos(Math.toRadians(half));

         for (BogrotArsenal.Pool q : new ArrayList<>(POOLS)) {
            if (q.level == level && q.owner.equals(p.getUUID()) && !(Math.abs(q.at.y - feet.y) >= 3.0)) {
               Vec3 dx = flat(q.at.subtract(feet));
               double len = dx.length();
               if (!(len > range + q.r) && (!(len > q.r) || !(dx.scale(1.0 / len).dot(dir) < cos))) {
                  POOLS.remove(q);
                  Vfx.pillar(level, q.at, WISP, 3.0, 0.9);
                  Vfx.explosion(level, q.at.add(0.0, 0.4, 0.0), WISP, q.r + 0.5);

                  for (LivingEntity e : near(level, p, q.at, q.r + 0.5)) {
                     hit(level, p, e, 8.0F * pw);
                     e.igniteForSeconds(3.0F);
                  }

                  if (blasts++ < 3) {
                     soundAt(level, q.at, 1.1F, 1.2F, "weapon.bogspitter.ignite", "weapon.bogspitter.alt");
                  }
               }
            }
         }
      });

      for (int j = 0; j < 5; j++) {
         double d = range * 0.2 * (j + 1);
         later(level, p, 16 + j, () -> Vfx.explosion(level, ground(level, feet.add(dir.scale(d))).add(0.0, 0.6, 0.0), WISP, 1.8));
      }

      if (emp) {
         every(level, p, 21, 5, 12, i -> {
            Vec3 lit = feet;

            for (int k = 0; k < 3; k++) {
               lit = conePoint(level, feet, dir, half, range).add(0.0, 0.3, 0.0);
               Vfx.embers(level, lit, WISP.glow(), 3, 0.5);
            }

            if (i % 2 == 1) {
               Vfx.flare(level, lit, WISP.glow(), 1.4, 6);

               for (LivingEntity e : cone(level, p, feet, dir, half, range)) {
                  hit(level, p, e, 1.5F * pw);
                  e.igniteForSeconds(2.0F);
               }
            }
         });
      }

      return true;
   }

   static void gourdLevel(ServerLevel level, Player p, WeaponKit kit, int c, boolean max) {
      Vec3 S = aim(level, p, 22.0);
      Vec3 at = GearKit.tip(p);
      Vfx.ring(level, S.add(0.0, 0.05, 0.0), TOX.glow(), 3.5 + 3 * (c - 1), 12);
      Vfx.gather(level, at, TOX, 1.2, 6);
      Vfx.flare(level, at, TOX.glow(), 0.9 + 0.3 * c, 6);
      if (max) {
         Vfx.chargeUp(level, at, TOX, 10);
         Vfx.runeCircle(level, S, 9.5, TOX, 14, 0.05F, GLYPH);
      }
   }

   static boolean festeringRings(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      int L = Math.max(1, Math.min(3, u.charge()));
      boolean emp = u.empowered();
      float pw = u.power();
      Vec3 S = aim(level, p, 22.0);
      int K = L + (emp ? 1 : 0);
      Set<LivingEntity> struck = new HashSet<>();

      for (int k = 0; k < K; k++) {
         int ring = k;
         double r0 = k == 0 ? 0.0 : 3.5 + 3 * (k - 1);
         double r1 = 3.5 + 3 * k;
         int when = 8 * (k + 1);
         List<Vec3> qs = new ArrayList<>();
         if (k == 0) {
            Vfx.telegraphCircle(level, S, 3.5, when, TOX);
         } else {
            int count = 3 + 3 * k;
            double rad = r0 + 1.5;
            double phase = k * 0.35;

            for (int i = 0; i < count; i++) {
               double a = phase + i * Math.PI * 2.0 / count;
               Vec3 q = ground(level, S.add(Math.cos(a) * rad, 0.0, Math.sin(a) * rad));
               qs.add(q);
               Vfx.telegraphCircle(level, q, 1.6, when, TOX);
            }

            Vfx.ring(level, S.add(0.0, 0.05, 0.0), TOX.glow(), r1, when);
         }

         later(level, p, when, () -> erupt(level, p, S, ring, r0, r1, qs, L, pw, struck));
      }

      if (L >= 3) {
         later(level, p, 8 * K + 2, () -> {
            pool(level, p, S, 3.5, 120, BogrotArsenal.Kind.BILE);

            for (LivingEntity e : struck) {
               bloomNow(level, p, e);
            }
         });
      }

      return true;
   }

   private static void erupt(
      ServerLevel level, Player p, Vec3 S, int ring, double r0, double r1, List<Vec3> qs, int L, float pw, Set<LivingEntity> struck
   ) {
      for (LivingEntity e : GearKit.enemies(level, p, S, r1 + 4.0)) {
         if (!(Math.abs(e.getY() - S.y) >= 3.0)) {
            double d = flatDist(e, S);
            double w = e.getBbWidth() / 2.0F;
            if (!(d + w < r0) && !(d - w >= r1)) {
               hit(level, p, e, (12 + 3 * L) * pw);
               rot(level, p, e, 1);
               e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), p);
               if (!heavy(e)) {
                  e.push(0.0, 0.55, 0.0);
                  e.syncVelocity = true;
               }

               resonate(p, "bogspitter", 2.0F);
               struck.add(e);
            }
         }
      }

      if (ring == 0) {
         Vfx.heavyImpact(level, S, TOX, 0.9);
         Vfx.groundCrack(level, S, 3.5, TOX);
      } else {
         for (int i = 0; i < qs.size(); i++) {
            Vec3 q = qs.get(i);
            Vfx.pillar(level, q, TOX, 3.0, 0.8);
            if (i % 2 == 0) {
               Vfx.debris(level, q, LEAF, TOX.glow(), 3, 0.3);
            }
         }

         Vfx.shockwave(level, S, TOX, r1);
      }

      soundAt(level, S, 1.2F, 0.85F + 0.1F * ring, "weapon.bogspitter.erupt", "weapon.bogspitter.use");
   }

   static void bileSplash(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof Player p) {
         rot(level, p, t, 1);
         GearKit.hitSpark(level, t, TOX);
         GearState state = GearState.of(p);
         if (valid(t) && state.ready("bogrot.splash")) {
            state.cooldown("bogrot.splash", 20);
            pool(level, p, ground(level, t.position()), 1.2, 40, BogrotArsenal.Kind.BILE);
            Vec3 at = center(t);
            Vfx.impact(level, at, TOX, 0.4);
            Vfx.debris(level, at, LEAF, TOX.glow(), 4, 0.25);
            GearKit.procSound(level, t, "bogspitter");
         }
      }
   }

   static void bogBlooded(ServerLevel level, Player p, int pieces) {
      if (p.hasEffect(MobEffects.POISON)) {
         p.removeEffect(MobEffects.POISON);
      }

      if (clock % 20L == 0L) {
         GearKit.keep(p, MobEffects.WATER_BREATHING, 0);
         BlockPos at = p.blockPosition();
         boolean mending = p.isInWater() || level.isRainingAt(at) || level.isRainingAt(at.above()) || inOwnPool(p);
         if (mending) {
            if (clock % 40L == 0L && p.getHealth() < p.getMaxHealth()) {
               p.heal(1.0F);
            }

            if (clock % 60L == 0L) {
               Vfx.auraRing(level, p.position(), P, 0.8);
               Vfx.debris(level, p.position().add(0.0, 1.8, 0.0), LEAF, P.glow(), 3, 0.1);
            }
         }
      }
   }

   static void shroudTick(ServerLevel level, Player p, int pieces) {
      BogrotArsenal.Shroud sh = SHROUDS.get(p.getUUID());
      if (sh != null) {
         sh.seen = clock;
         if (clock % 10L == 0L) {
            Vfx.debris(level, top(p), LEAF, P.glow(), 2, 0.1);
         }

         if (clock >= sh.until || sh.given > 0.0F && p.getAbsorptionAmount() <= sh.before) {
            endShroud(level, p, sh, true);
         }
      }
   }

   static void mirerootHurt(ServerLevel level, Player p, @Nullable LivingEntity attacker, DamageSource source, float taken, int pieces) {
      if (pieces >= 4) {
         if (attacker != null && source.getDirectEntity() == attacker && valid(attacker)) {
            Long last = ATTACKER_ROT.get(attacker);
            if (last == null || clock - last >= 10L) {
               ATTACKER_ROT.put(attacker, clock);
               rot(level, p, attacker, 2);
               GearKit.proc(level, attacker, TOX);
            }

            if (SHROUDS.containsKey(p.getUUID())) {
               Long lt = THORNS.get(attacker);
               if (lt == null || clock - lt >= 10L) {
                  THORNS.put(attacker, clock);
                  hit(level, p, attacker, 3.0F);
                  rot(level, p, attacker, 1);
                  Vfx.sparks(level, attacker.getEyePosition(), P.glow(), 6, 0.3, 0.18);
               }
            }
         }

         GearState state = GearState.of(p);
         if (p.isAlive() && !SHROUDS.containsKey(p.getUUID()) && p.getHealth() < 0.4F * p.getMaxHealth() && state.ready("bogrot.shroud")) {
            state.cooldown("bogrot.shroud", 600);
            BogrotArsenal.Shroud sh = new BogrotArsenal.Shroud();
            sh.before = p.getAbsorptionAmount();
            sh.until = clock + 160L;
            sh.seen = clock;
            sh.given = Math.max(0.0F, 8.0F - sh.before);
            SHROUDS.put(p.getUUID(), sh);
            absorbCap(p);
            p.setAbsorptionAmount(sh.before + sh.given);
            Vfx.shieldBubble(level, p.position().add(0.0, 1.0, 0.0), 1.3, P, 160);
            Vfx.debris(level, top(p), LEAF, P.glow(), 16, 0.3);
            Vfx.auraRing(level, p.position(), P, 1.4);
            GearKit.sound(level, p, 1.0F, 1.2F, "boss.bogrot.willow_shroud.hit", "armor.mireroot_armor.ability");
         }
      }
   }

   private static void endShroud(ServerLevel level, Player p, BogrotArsenal.Shroud sh, boolean whip) {
      SHROUDS.remove(p.getUUID());
      if (sh.given > 0.0F) {
         p.setAbsorptionAmount(Math.min(p.getAbsorptionAmount(), sh.before));
      }

      absorbCap(p);
      if (whip && p.isAlive()) {
         Vec3 feet = p.position();

         for (LivingEntity e : near(level, p, feet, 4.0)) {
            hit(level, p, e, 8.0F);
            rot(level, p, e, 1);
            if (!heavy(e)) {
               GearKit.knockAway(e, feet, 1.2, 0.4);
            }
         }

         Vec3 chest = chest(p);
         Vfx.nova(level, chest, P, 4.0);
         Vfx.debris(level, chest, LEAF, P.glow(), 24, 0.5);
         GearKit.sound(level, p, 1.0F, 1.0F, "weapon.hags_crook.slam", "weapon.hags_crook.alt");
      }
   }

   static boolean embraceOfTheMire(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 g = ground(level, p.position());
      Vfx.runeCircle(level, g, 10.0, HEX, 36, -0.06F, GLYPH);
      Vfx.vortex(level, g, 5.0, 4.0, TOX, 30);
      Vfx.telegraphCircle(level, g, 4.0, 30, LEECH);
      Vfx.shieldBubble(level, p.position().add(0.0, 1.0, 0.0), 1.4, P, 30);
      p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0, false, true, true));
      Set<LivingEntity> slowed = new HashSet<>();
      every(level, p, 1, 1, 28, i -> {
         Vec3 c = p.position();
         double speed = (0.06 + 0.08 * i / 27.0) * 1.6;

         for (LivingEntity e : GearKit.enemies(level, p, c, 10.0)) {
            if (heavy(e)) {
               if (slowed.add(e)) {
                  e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1), p);
               }
            } else {
               Vec3 d = flat(c.subtract(e.position()));
               if (!(d.lengthSqr() < 2.25)) {
                  d = d.normalize().scale(speed);
                  e.push(d.x, 0.0, d.z);
                  e.syncVelocity = true;
               }
            }
         }

         if (i % 4 == 0) {
            Vfx.gather(level, chest(p), TOX, 3.0, 6);
         }
      });
      later(level, p, 30, () -> chomp(level, p));
      later(level, p, 40, () -> exhale(level, p));
      return true;
   }

   private static void chomp(ServerLevel level, Player p) {
      Vec3 feet = p.position();
      Vec3 g = ground(level, feet);
      Vec3 chest = chest(p);
      float yaw = p.getYRot();
      int n = 0;

      for (LivingEntity e : near(level, p, feet, 4.0)) {
         hit(level, p, e, 24.0F);
         rot(level, p, e, 2);
         e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), p);
         Vfx.impact(level, center(e), LEECH, 0.6);
         n++;
      }

      if (n > 0) {
         p.heal(Math.min(12, 3 * n));
      }

      Vfx.slashArc(level, chest, yaw, 0.0F, 90.0F, 4.0, LEECH, 180.0F);
      Vfx.slashArc(level, chest, yaw + 180.0F, 0.0F, 90.0F, 4.0, LEECH, 180.0F);

      for (int i = 0; i < 6; i++) {
         double a = Math.toRadians(i * 60);
         Vec3 q = ground(level, feet.add(Math.cos(a) * 3.0, 0.0, Math.sin(a) * 3.0));
         Vfx.claw(level, q.add(0.0, 1.0, 0.0), yawTo(q, feet), -60.0F, 1.8, P);
      }

      Vfx.heavyImpact(level, g, P, 1.4);
      if (n > 0) {
         Vfx.impactFrame(level, g, 16.0, TOX.glow(), 0.6F);
      }

      soundAt(level, feet, 1.4F, 1.0F, "boss.bogrot.rot_mothers_embrace.hit", "weapon.leechblade.drink", "weapon.leechblade.alt");
      Vfx.telegraphCircle(level, g, 8.0, 10, TOX);
   }

   private static void exhale(ServerLevel level, Player p) {
      Vec3 feet = p.position();
      Vec3 g = ground(level, feet);
      Vec3 chest = chest(p);

      for (LivingEntity e : near(level, p, feet, 8.0)) {
         double d = Math.min(8.0, boxDist(e, feet));
         hit(level, p, e, (float)(12.0 * (0.5 + 0.5 * (1.0 - d / 8.0))));
         rot(level, p, e, 2);
         e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), p);
         if (!heavy(e)) {
            GearKit.knockAway(e, feet, 1.2, 0.35);
         }
      }

      Vfx.nova(level, chest, TOX, 8.0);
      Vfx.radialFlash(level, g, 16.0, TOX.glow(), 0.45F);
      Vfx.debris(level, chest, LEAF, TOX.glow(), 20, 0.5);
      Vfx.groundCrack(level, g, 6.0, P);
      pool(level, p, g, 3.0, 100, BogrotArsenal.Kind.BLACK);
      soundAt(level, feet, 1.3F, 0.9F, "weapon.bogspitter.erupt", "weapon.bogspitter.use");
   }

   private static void rot(ServerLevel level, Player p, LivingEntity e, int n) {
      if (n > 0 && valid(e)) {
         BogrotArsenal.Rot r = ROT.computeIfAbsent(e, k -> new BogrotArsenal.Rot());
         if (r.spentUntil <= clock) {
            r.stacks += n;
            r.owner = p.getUUID();
            r.lastGain = clock;
            r.lastDecay = clock;
            boolean bogrot = isBogrot(e);
            if (hexed(e) ? r.stacks < 3 : bogrot || r.stacks < 5) {
               r.stacks = Math.min(r.stacks, bogrot ? 3 : 4);
            } else {
               bloom(level, p, e, r);
            }
         }
      }
   }

   private static int rotStacks(LivingEntity e) {
      BogrotArsenal.Rot r = ROT.get(e);
      return r == null ? 0 : r.stacks;
   }

   private static void bloomNow(ServerLevel level, Player p, LivingEntity e) {
      BogrotArsenal.Rot r = ROT.get(e);
      if (r != null && live(e) && r.stacks >= 3 && r.spentUntil <= clock && (!isBogrot(e) || hexed(e))) {
         bloom(level, p, e, r);
      }
   }

   private static void bloom(ServerLevel level, Player p, LivingEntity e, BogrotArsenal.Rot r) {
      r.stacks = 0;
      r.spentUntil = clock + 60L;
      Vec3 c = center(e);
      Vec3 feet = e.position();
      boolean heavy = heavy(e);
      hit(level, p, e, 10.0F);
      if (live(e)) {
         e.addEffect(new MobEffectInstance(MobEffects.POISON, heavy ? 40 : 60, heavy ? 0 : 1), p);
      }

      for (LivingEntity o : near(level, p, c, 3.0)) {
         if (o != e) {
            hit(level, p, o, 5.0F);
            rot(level, p, o, 1);
         }
      }

      Vec3 g = ground(level, feet);
      pool(level, p, g, 1.8, 60, BogrotArsenal.Kind.BILE);
      Vfx.nova(level, c, TOX, 2.5);
      Vfx.debris(level, c, LEAF, TOX.glow(), 14, 0.35);
      Vfx.flare(level, c, TOX.core(), 2.0, 8);
      Vfx.groundCrack(level, g, 1.8, TOX);
      soundAt(level, c, 1.1F, 0.95F + level.getRandom().nextFloat() * 0.1F, "weapon.hags_crook.bloom", "weapon.hags_crook.proc");

      for (ItemStack held : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
         if (held.getItem() instanceof MysticWeapon w && w.kit().arsenal() instanceof BogrotArsenal && w.kit().usesResonance()) {
            GearState.of(p).addResonance(w.gearId(), 5.0F);
            break;
         }
      }
   }

   private static void hex(ServerLevel level, Player p, LivingEntity e, int ticks, boolean jumps, boolean strong) {
      if (valid(e)) {
         BogrotArsenal.Hex h = HEXED.get(e);
         if (h == null || h.until <= clock) {
            h = new BogrotArsenal.Hex();
            HEXED.put(e, h);
         }

         h.until = Math.max(h.until, clock + ticks);
         h.owner = p.getUUID();
         h.jumps |= jumps;
         boolean heavy = heavy(e);
         e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0), p);
         e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, strong && !heavy ? 1 : 0), p);
         e.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0), p);
      }
   }

   private static boolean hexed(LivingEntity e) {
      BogrotArsenal.Hex h = HEXED.get(e);
      return h != null && h.until > clock;
   }

   private static float hexMul(LivingEntity e) {
      return hexed(e) ? 1.2F : 1.0F;
   }

   private static void jumpHex(ServerLevel level, @Nullable UUID ownerId, LivingEntity dead, Vec3 at, int left) {
      ServerPlayer o = owner(level, ownerId);
      if (o != null) {
         LivingEntity next = null;
         double best = Double.MAX_VALUE;

         for (LivingEntity c : GearKit.enemies(level, o, at, 8.0)) {
            double d = c.distanceToSqr(at);
            if (c != dead && !hexed(c) && d < best) {
               best = d;
               next = c;
            }
         }

         if (next != null) {
            hex(level, o, next, left, true, false);
            Vec3 to = center(next);
            Vfx.beam(level, at, to, HEX, 0.3, 8);
            Vfx.nova(level, to, HEX, 1.4);
            Vfx.runeCircle(level, ground(level, next.position()), 1.4, HEX, 30, 0.1F, GLYPH);
            soundAt(level, to, 0.9F, 1.2F, "weapon.hags_crook.hex", "weapon.hags_crook.alt");
         }
      }
   }

   private static void bleed(Player p, LivingEntity e, int ticks) {
      if (valid(e)) {
         BogrotArsenal.Dot d = BLEED.computeIfAbsent(e, k -> new BogrotArsenal.Dot());
         d.until = Math.max(d.until, clock + ticks);
         d.owner = p.getUUID();
      }
   }

   private static void latch(Player p, LivingEntity e, int ticks) {
      if (valid(e)) {
         BogrotArsenal.Dot d = LATCH.computeIfAbsent(e, k -> new BogrotArsenal.Dot());
         d.until = clock + ticks;
         d.owner = p.getUUID();
      }
   }

   private static boolean wounded(LivingEntity e) {
      BogrotArsenal.Dot b = BLEED.get(e);
      return rotStacks(e) > 0 || b != null && b.until > clock || LATCH.containsKey(e);
   }

   private static void burst(ServerLevel level, @Nullable UUID ownerId, Vec3 at) {
      ServerPlayer o = owner(level, ownerId);
      if (o != null) {
         for (LivingEntity e : near(level, o, at, 2.0)) {
            hit(level, o, e, 4.0F);
            rot(level, o, e, 1);
         }
      }

      Vfx.explosion(level, at, LEECH, 1.5);
      soundAt(level, at, 0.9F, 0.95F + level.getRandom().nextFloat() * 0.1F, "weapon.leechblade.proc");
   }

   private static void pool(ServerLevel level, Player owner, Vec3 at, double r, int ticks, BogrotArsenal.Kind kind) {
      UUID id = owner.getUUID();
      int mine = 0;
      BogrotArsenal.Pool oldest = null;

      for (BogrotArsenal.Pool q : POOLS) {
         if (q.owner.equals(id)) {
            mine++;
            if (oldest == null) {
               oldest = q;
            }
         }
      }

      if (mine >= 8) {
         POOLS.remove(oldest);
      }

      if (POOLS.size() >= 40) {
         POOLS.remove(0);
      }

      POOLS.add(new BogrotArsenal.Pool(level, id, at, r, clock + ticks, kind));
      Vfx.groundCrack(level, at, r, kind == BogrotArsenal.Kind.BILE ? TOX : P);
   }

   private static boolean inOwnPool(Player p) {
      for (BogrotArsenal.Pool q : POOLS) {
         if (q.level == p.level() && q.owner.equals(p.getUUID()) && Math.abs(p.getY() - q.at.y) < 1.5 && edgeDist(p, q.at) < q.r) {
            return true;
         }
      }

      return false;
   }

   private static void schedule(int ticks, Runnable r) {
      PENDING.add(new BogrotArsenal.Task(clock + Math.max(1, ticks), r));
   }

   private static void later(ServerLevel level, Player p, int ticks, Runnable r) {
      schedule(ticks, () -> {
         if (alive(p, level)) {
            r.run();
         }
      });
   }

   private static void every(ServerLevel level, Player p, int first, int period, int count, IntConsumer step) {
      for (int i = 0; i < count; i++) {
         int n = i;
         later(level, p, first + period * i, () -> step.accept(n));
      }
   }

   private static BogrotArsenal.@Nullable Wisp wisp(ServerLevel level, Player owner, Vec3 from, Vec3 vel, @Nullable LivingEntity target) {
      if (WISPS.size() + NEW_WISPS.size() >= 48) {
         return null;
      }

      BogrotArsenal.Wisp w = new BogrotArsenal.Wisp(level, owner, from, vel, target);
      NEW_WISPS.add(w);
      return w;
   }

   private static void glob(ServerLevel level, Player owner, Vec3 from, Vec3 vel, int life, Vec3 landing, Consumer<Vec3> onLand) {
      if (GLOBS.size() + NEW_GLOBS.size() < 32) {
         NEW_GLOBS.add(new BogrotArsenal.Glob(level, owner, from, vel, life, landing, onLand));
      }
   }

   private static void tick(MinecraftServer server) {
      if (!PENDING.isEmpty()) {
         TASKS.addAll(PENDING);
         PENDING.clear();
      }

      if (!TASKS.isEmpty()) {
         List<BogrotArsenal.Task> due = new ArrayList<>();
         TASKS.removeIf(tx -> tx.at() <= clock && due.add(tx));

         for (BogrotArsenal.Task t : due) {
            try {
               t.run().run();
            } catch (RuntimeException ex) {
               MysticalTerrain.LOGGER.error("Bogrot gear task failed", ex);
            }
         }
      }

      if (!WISPS.isEmpty()) {
         WISPS.removeIf(w -> {
            try {
               return stepWisp(w);
            } catch (RuntimeException ex) {
               MysticalTerrain.LOGGER.error("Bogrot gear wisp failed", ex);
               return true;
            }
         });
      }

      if (!NEW_WISPS.isEmpty()) {
         WISPS.addAll(NEW_WISPS);
         NEW_WISPS.clear();
      }

      if (!GLOBS.isEmpty()) {
         GLOBS.removeIf(g -> {
            try {
               return stepGlob(g);
            } catch (RuntimeException ex) {
               MysticalTerrain.LOGGER.error("Bogrot gear glob failed", ex);
               return true;
            }
         });
      }

      if (!NEW_GLOBS.isEmpty()) {
         GLOBS.addAll(NEW_GLOBS);
         NEW_GLOBS.clear();
      }

      try {
         tickPools();
         tickRot();
         tickDots();
         tickHex();
         if (clock % 5L == 0L) {
            previews(server);
         }

         tickPlayers(server);
      } catch (RuntimeException ex) {
         MysticalTerrain.LOGGER.error("Bogrot gear tick failed", ex);
      }

      clock++;
   }

   private static boolean stepWisp(BogrotArsenal.Wisp w) {
      if (!alive(w.owner, w.level)) {
         return true;
      }

      w.age++;
      if (w.target != null && !live(w.target)) {
         w.target = nearest(w.level, w.owner, w.pos, 10.0, null);
      }

      if (w.age > w.homingDelay && w.target != null) {
         Vec3 to = aimPoint(w.target).subtract(w.pos);
         if (to.lengthSqr() > 1.0E-4) {
            w.vel = w.vel.lerp(to.normalize().scale(w.speed), w.homing);
         }

         w.speed = Math.min(w.maxSpeed, w.speed + w.accel);
      }

      Vec3 next = w.pos.add(w.vel);
      BlockHitResult block = w.level.clip(new ClipContext(w.pos, next, Block.COLLIDER, Fluid.NONE, w.owner));
      boolean wall = block.getType() != Type.MISS;
      Vec3 end = wall ? block.getLocation() : next;
      LivingEntity struck = null;
      Vec3 at = end;
      double bestD = Double.MAX_VALUE;

      for (LivingEntity e : GearKit.enemies(w.level, w.owner, w.pos.add(end).scale(0.5), w.pos.distanceTo(end) / 2.0 + w.radius + 5.0)) {
         AABB box = e.getBoundingBox().inflate(w.radius * 0.5);
         Vec3 hitAt = box.contains(w.pos) ? w.pos : (Vec3)box.clip(w.pos, end).orElse(null);
         if (hitAt != null && hitAt.distanceToSqr(w.pos) < bestD) {
            bestD = hitAt.distanceToSqr(w.pos);
            struck = e;
            at = hitAt;
         }
      }

      if (struck != null) {
         w.onHit.hit(w, struck, at);
         return true;
      }

      if (wall) {
         fizzle(w, end);
         return true;
      }

      w.pos = next;
      Vfx.trail(w.level, w.pos, w.pal, w.size);
      if (w.age % 3 == 0) {
         Vfx.embers(w.level, w.pos, w.pal.glow(), 1, 0.12);
      }

      if (w.age >= w.life) {
         fizzle(w, w.pos);
         return true;
      } else {
         return false;
      }
   }

   private static void fizzle(BogrotArsenal.Wisp w, Vec3 at) {
      Vfx.flare(w.level, at, w.pal.glow(), 0.9, 5);
      Vfx.smoke(w.level, at, w.pal.smoke(), 2, 0.2, 0.6);
   }

   private static boolean stepGlob(BogrotArsenal.Glob g) {
      if (!alive(g.owner, g.level)) {
         return true;
      }

      g.age++;
      g.vel = g.vel.add(0.0, -0.06, 0.0);
      g.pos = g.pos.add(g.vel);
      Vfx.meteorTrail(g.level, g.pos, g.vel, TOX);
      if (g.age % 3 == 0) {
         Vfx.embers(g.level, g.pos, TOX.glow(), 1, 0.1);
      }

      if (g.age >= g.life) {
         g.onLand.accept(g.landing);
         return true;
      } else {
         return false;
      }
   }

   private static void tickPools() {
      if (!POOLS.isEmpty()) {
         boolean ten = clock % 10L == 0L;
         boolean twenty = clock % 20L == 0L;

         for (BogrotArsenal.Pool q : new ArrayList<>(POOLS)) {
            if (POOLS.contains(q)) {
               ServerPlayer owner = owner(q.level, q.owner);
               Vfx.Palette pal = q.pal();
               if (clock < q.until && owner != null) {
                  if (twenty) {
                     Vfx.aura(q.level, q.at, pal, q.r);
                     Vfx.smoke(q.level, q.at, pal.smoke(), 2, q.r * 0.6, 0.9);
                     if (q.kind == BogrotArsenal.Kind.BLACK) {
                        Vfx.ring(q.level, q.at.add(0.0, 0.05, 0.0), P.glow(), q.r, 20);
                     }
                  }

                  if (ten) {
                     for (LivingEntity e : flatIn(q.level, owner, q.at, q.r, 1.5)) {
                        hit(q.level, owner, e, 1.0F);
                        if (q.kind == BogrotArsenal.Kind.BILE) {
                           e.addEffect(new MobEffectInstance(MobEffects.POISON, 30, 0), owner);
                        } else if (!heavy(e)) {
                           e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 1), owner);
                        }

                        if (twenty) {
                           rot(q.level, owner, e, 1);
                        }
                     }
                  }
               } else {
                  POOLS.remove(q);
                  Vfx.smoke(q.level, q.at, pal.smoke(), 5, q.r * 0.7, 1.1);
               }
            }
         }
      }
   }

   private static void tickRot() {
      if (clock % 20L == 0L && !ROT.isEmpty()) {
         Map<ServerLevel, Integer> tells = new HashMap<>();

         for (LivingEntity e : new ArrayList<>(ROT.keySet())) {
            BogrotArsenal.Rot r = ROT.get(e);
            if (r != null) {
               if (live(e) && e.level() instanceof ServerLevel level) {
                  if (r.stacks > 0 && clock - r.lastGain >= 100L && clock - r.lastDecay >= 40L) {
                     r.stacks--;
                     r.lastDecay = clock;
                  }

                  if (r.stacks <= 0) {
                     if (r.spentUntil <= clock) {
                        ROT.remove(e);
                     }
                  } else {
                     int stacks = r.stacks;
                     ServerPlayer o = owner(level, r.owner);
                     if (o != null) {
                        hit(level, o, e, 0.5F * stacks);
                     }

                     if (live(e) && tells.merge(level, 1, Integer::sum) <= 24) {
                        Vfx.wisps(level, top(e).add(0.0, 0.5, 0.0), TOX, stacks, 0.45 + 0.1 * e.getBbWidth(), 22);
                        if (stacks >= 4) {
                           Vfx.smoke(level, center(e), TOX.smoke(), 1, 0.3, 0.8);
                        }
                     }
                  }
               } else {
                  ROT.remove(e);
               }
            }
         }
      }
   }

   private static void tickDots() {
      boolean ten = clock % 10L == 0L;
      if (ten && !BLEED.isEmpty()) {
         for (LivingEntity e : new ArrayList<>(BLEED.keySet())) {
            BogrotArsenal.Dot d = BLEED.get(e);
            if (d != null) {
               if (live(e) && clock < d.until && e.level() instanceof ServerLevel level) {
                  Vfx.sparks(level, center(e), LEECH.glow(), 2, 0.1, 0.12);
                  if (clock % 20L == 0L) {
                     ServerPlayer o = owner(level, d.owner);
                     if (o != null) {
                        hit(level, o, e, 1.5F);
                     }
                  }
               } else {
                  BLEED.remove(e);
               }
            }
         }
      }

      if (clock % 5L == 0L && !LATCH.isEmpty()) {
         for (LivingEntity e : new ArrayList<>(LATCH.keySet())) {
            BogrotArsenal.Dot d = LATCH.get(e);
            if (d != null) {
               if (live(e) && e.level() instanceof ServerLevel level) {
                  Vec3 var13 = center(e);
                  if (clock >= d.until) {
                     LATCH.remove(e);
                     burst(level, d.owner, var13);
                  } else {
                     Vfx.sparks(level, var13, LEECH.glow(), 2, 0.12, 0.12);
                     if (ten) {
                        ServerPlayer o = owner(level, d.owner);
                        if (o != null) {
                           hit(level, o, e, 1.2F);
                           o.heal(0.5F);
                           if (o.distanceTo(e) < 16.0F) {
                              Vfx.beam(level, var13, chest(o), LEECH, 0.1, 4);
                           }
                        }
                     }
                  }
               } else {
                  LATCH.remove(e);
               }
            }
         }
      }
   }

   private static void tickHex() {
      if (clock % 10L == 0L && !HEXED.isEmpty()) {
         for (LivingEntity e : new ArrayList<>(HEXED.keySet())) {
            BogrotArsenal.Hex h = HEXED.get(e);
            if (h != null) {
               if (live(e) && clock < h.until && e.level() instanceof ServerLevel level) {
                  Vfx.ring(level, e.position().add(0.0, 0.05, 0.0), HEX.glow(), 0.6 + 0.3 * e.getBbWidth(), 10);
                  if (clock % 20L == 0L) {
                     Vfx.embers(level, top(e), HEX.glow(), 2, 0.3);
                  }
               } else {
                  HEXED.remove(e);
               }
            }
         }
      }
   }

   private static void previews(MinecraftServer server) {
      for (ServerPlayer p : server.getPlayerList().getPlayers()) {
         if (p.isAlive() && p.isUsingItem() && p.getUseItem().getItem() instanceof MysticWeapon w) {
            ServerLevel level = p.level();
            if ("hags_crook".equals(w.gearId())) {
               int c = Math.max(1, Math.min(3, p.getTicksUsingItem() / 14));
               Vfx.ring(level, aim(level, p, 24.0).add(0.0, 0.05, 0.0), HEX.glow(), 2.0 + 1.5 * c, 6);
            } else if ("bogspitter".equals(w.gearId())) {
               int c = Math.max(1, Math.min(3, p.getTicksUsingItem() / 12));
               Vfx.ring(level, aim(level, p, 22.0).add(0.0, 0.05, 0.0), TOX.glow(), 3.5 + 3 * (c - 1), 6);
            }
         }
      }
   }

   private static void tickPlayers(MinecraftServer server) {
      if (!SHROUDS.isEmpty() || !DRINK.isEmpty() || !CAPPED.isEmpty()) {
         Set<UUID> ids = new HashSet<>(CAPPED);
         ids.addAll(SHROUDS.keySet());
         ids.addAll(DRINK.keySet());

         for (UUID id : ids) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) {
               SHROUDS.remove(id);
               DRINK.remove(id);
               CAPPED.remove(id);
            } else {
               Long drink = DRINK.get(id);
               if (drink != null && drink <= clock) {
                  DRINK.remove(id);
               }

               BogrotArsenal.Shroud sh = SHROUDS.get(id);
               if (sh != null && (!p.isAlive() || clock - sh.seen > 2L)) {
                  endShroud(p.level(), p, sh, false);
               }

               absorbCap(p);
            }
         }
      }
   }

   private static void absorbCap(Player p) {
      AttributeInstance max = p.getAttribute(Attributes.MAX_ABSORPTION);
      if (max != null) {
         UUID id = p.getUUID();
         Long drink = DRINK.get(id);
         double want = (SHROUDS.containsKey(id) ? 8 : 0) + (drink != null && drink > clock ? 6 : 0);
         AttributeModifier cur = max.getModifier(ABSORB);
         if (want <= 0.0) {
            if (cur != null) {
               max.removeModifier(ABSORB);
            }

            CAPPED.remove(id);
         } else if (cur == null || cur.amount() != want) {
            max.addOrUpdateTransientModifier(new AttributeModifier(ABSORB, want, Operation.ADD_VALUE));
            CAPPED.add(id);
         }
      }
   }

   private static void died(LivingEntity e, DamageSource source) {
      if (e.level() instanceof ServerLevel level) {
         Vec3 var7 = center(e);
         BogrotArsenal.Hex h = HEXED.remove(e);
         if (h != null && h.jumps && h.until > clock) {
            int left = (int)Math.max(60L, h.until - clock);
            UUID owner = h.owner;
            schedule(1, () -> jumpHex(level, owner, e, var7, left));
         }

         BogrotArsenal.Dot l = LATCH.remove(e);
         if (l != null) {
            UUID owner = l.owner;
            schedule(1, () -> burst(level, owner, var7));
         }

         ROT.remove(e);
         BLEED.remove(e);
         LEECH_COUNT.remove(e);
      }
   }

   private static void reset() {
      TASKS.clear();
      PENDING.clear();
      WISPS.clear();
      NEW_WISPS.clear();
      GLOBS.clear();
      NEW_GLOBS.clear();
      POOLS.clear();
      ROT.clear();
      HEXED.clear();
      BLEED.clear();
      LATCH.clear();
      LEECH_COUNT.clear();
      ATTACKER_ROT.clear();
      THORNS.clear();
      SHROUDS.clear();
      DRINK.clear();
      CAPPED.clear();
      clock = 0L;
   }

   private static boolean alive(Player p, ServerLevel level) {
      return p.isAlive() && !p.isRemoved() && p.level() == level;
   }

   private static boolean live(LivingEntity e) {
      return e.isAlive() && !e.isRemoved();
   }

   private static boolean valid(LivingEntity e) {
      return live(e) && !(e instanceof Player) && !(e instanceof ArmorStand);
   }

   private static boolean heavy(LivingEntity e) {
      return e instanceof MysticalBoss b && b.isBoss() || e instanceof EnderDragon || e instanceof WitherBoss;
   }

   private static boolean isBogrot(LivingEntity e) {
      return e instanceof MysticalBoss && "bogrot".equals(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath());
   }

   private static @Nullable ServerPlayer owner(ServerLevel level, @Nullable UUID id) {
      if (id == null) {
         return null;
      }

      ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
      return p != null && p.isAlive() && p.level() == level ? p : null;
   }

   private static void resonate(Player p, String weapon, float amount) {
      GearState.of(p).addResonance(weapon, amount);
   }

   private static void hit(ServerLevel level, Player p, LivingEntity e, float base) {
      if (live(e) && !(base <= 0.0F)) {
         e.setInvulnerableTime(0);
         e.hurtServer(level, p.damageSources().source(DamageTypes.MAGIC, p), base * hexMul(e));
      }
   }

   private static void hitStrike(ServerLevel level, Player p, LivingEntity e, float base) {
      if (live(e) && !(base <= 0.0F)) {
         e.setInvulnerableTime(0);
         GearKit.strike(level, p, e, base * hexMul(e));
      }
   }

   private static BlockParticleOption mud() {
      return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState());
   }

   private static Vec3 center(LivingEntity e) {
      return e.getBoundingBox().getCenter();
   }

   private static Vec3 top(LivingEntity e) {
      return e.position().add(0.0, e.getBbHeight() + 0.2, 0.0);
   }

   private static Vec3 chest(LivingEntity e) {
      return e.position().add(0.0, e.getBbHeight() * 0.65, 0.0);
   }

   private static Vec3 aimPoint(LivingEntity e) {
      return center(e).add(0.0, e.getBbHeight() * 0.2, 0.0);
   }

   private static Vec3 flat(Vec3 v) {
      return new Vec3(v.x, 0.0, v.z);
   }

   private static double flatDist(LivingEntity e, Vec3 at) {
      double dx = e.getX() - at.x;
      double dz = e.getZ() - at.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static double edgeDist(LivingEntity e, Vec3 at) {
      return Math.max(0.0, flatDist(e, at) - e.getBbWidth() / 2.0F);
   }

   private static double boxDist(LivingEntity e, Vec3 at) {
      AABB b = e.getBoundingBox();
      double dx = Math.max(Math.max(b.minX - at.x, 0.0), at.x - b.maxX);
      double dy = Math.max(Math.max(b.minY - at.y, 0.0), at.y - b.maxY);
      double dz = Math.max(Math.max(b.minZ - at.z, 0.0), at.z - b.maxZ);
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private static float yawTo(Vec3 from, Vec3 to) {
      return (float)(Mth.atan2(to.z - from.z, to.x - from.x) * 180.0F / (float)Math.PI) - 90.0F;
   }

   private static List<LivingEntity> near(ServerLevel level, Player p, Vec3 at, double r) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, at, r + 5.0)) {
         if (boxDist(e, at) <= r) {
            out.add(e);
         }
      }

      return out;
   }

   private static List<LivingEntity> flatIn(ServerLevel level, Player p, Vec3 at, double r, double dy) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, at, r + 4.0)) {
         if (Math.abs(e.getY() - at.y) < dy && edgeDist(e, at) <= r) {
            out.add(e);
         }
      }

      return out;
   }

   private static @Nullable LivingEntity nearest(ServerLevel level, Player p, Vec3 at, double r, @Nullable LivingEntity except) {
      LivingEntity best = null;
      double bestD = Double.MAX_VALUE;

      for (LivingEntity e : GearKit.enemies(level, p, at, r)) {
         double d = e.distanceToSqr(at);
         if (e != except && d < bestD) {
            bestD = d;
            best = e;
         }
      }

      return best;
   }

   private static List<LivingEntity> sector(ServerLevel level, Player p, Vec3 feet, Vec3 dir, double half, double range, double dy) {
      double cos = Math.cos(Math.toRadians(half));
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, feet, range + 4.0)) {
         if (!(Math.abs(e.getY() - feet.y) >= dy)) {
            Vec3 d = flat(e.position().subtract(feet));
            double len = d.length();
            if (!(len - e.getBbWidth() / 2.0F > range) && (!(len > 0.8) || !(d.scale(1.0 / len).dot(dir) < cos))) {
               out.add(e);
            }
         }
      }

      return out;
   }

   private static List<LivingEntity> cone(ServerLevel level, Player p, Vec3 feet, Vec3 dir, double half, double range) {
      return sector(level, p, feet, dir, half, range, 3.0);
   }

   private static List<LivingEntity> arc(ServerLevel level, Player p, double reach, double half) {
      return sector(level, p, p.position(), GearKit.flatLook(p), half, reach, 2.5);
   }

   private static Vec3 conePoint(ServerLevel level, Vec3 feet, Vec3 dir, double half, double range) {
      double a = (level.getRandom().nextDouble() * 2.0 - 1.0) * half;
      double d = 1.5 + level.getRandom().nextDouble() * (range - 1.5);
      return ground(level, feet.add(dir.yRot((float)Math.toRadians(a)).scale(d)));
   }

   private static Vec3 aim(ServerLevel level, Player p, double range) {
      return ground(level, GearKit.lookPoint(level, p, range).subtract(p.getLookAngle().scale(0.1)));
   }

   private static Vec3 ground(ServerLevel level, Vec3 at) {
      Vec3 g = floor(level, at, 1.5, 8);
      if (g != null) {
         return g;
      }

      Vec3 b = BossFx.ground(level, at.x, at.y + 1.5, at.z);
      return b != null ? b : at;
   }

   private static @Nullable Vec3 floor(ServerLevel level, Vec3 at, double up, int depth) {
      MutableBlockPos q = BlockPos.containing(at.x, at.y + up, at.z).mutable();
      boolean air = false;
      boolean wet = false;
      int steps = depth + (int)Math.ceil(up) + 1;

      for (int i = 0; i < steps; i++) {
         BlockState s = level.getBlockState(q);
         FluidState f = s.getFluidState();
         if (!f.isEmpty()) {
            if (air) {
               return new Vec3(at.x, q.getY() + f.getHeight(level, q), at.z);
            }

            wet = true;
         } else {
            VoxelShape shape = s.getCollisionShape(level, q);
            if (shape.isEmpty()) {
               air = true;
            } else if (air || wet) {
               return new Vec3(at.x, q.getY() + shape.max(Axis.Y), at.z);
            }
         }

         q.move(0, -1, 0);
      }

      return null;
   }

   private static void soundAt(ServerLevel level, Vec3 at, float volume, float pitch, String... keys) {
      for (String key : keys) {
         SoundEvent e = BossSounds.event(key);
         if (e != null) {
            level.playSound(null, at.x, at.y, at.z, e, SoundSource.PLAYERS, volume, pitch);
            return;
         }
      }
   }

   private static final class Dot {
      long until;
      @Nullable UUID owner;
   }

   private static final class Glob {
      final ServerLevel level;
      final Player owner;
      Vec3 pos;
      Vec3 vel;
      int age;
      final int life;
      final Vec3 landing;
      final Consumer<Vec3> onLand;

      Glob(ServerLevel level, Player owner, Vec3 pos, Vec3 vel, int life, Vec3 landing, Consumer<Vec3> onLand) {
         this.level = level;
         this.owner = owner;
         this.pos = pos;
         this.vel = vel;
         this.life = life;
         this.landing = landing;
         this.onLand = onLand;
      }
   }

   private static final class Hex {
      long until;
      @Nullable UUID owner;
      boolean jumps;
   }

   private static final class Hits {
      int count;
      long last;
   }

   private enum Kind {
      BILE,
      BLACK;
   }

   @FunctionalInterface
   private interface OnWisp {
      void hit(BogrotArsenal.Wisp var1, LivingEntity var2, Vec3 var3);
   }

   private static final class Pool {
      final ServerLevel level;
      final UUID owner;
      final Vec3 at;
      final double r;
      final long until;
      final BogrotArsenal.Kind kind;

      Pool(ServerLevel level, UUID owner, Vec3 at, double r, long until, BogrotArsenal.Kind kind) {
         this.level = level;
         this.owner = owner;
         this.at = at;
         this.r = r;
         this.until = until;
         this.kind = kind;
      }

      Vfx.Palette pal() {
         return this.kind == BogrotArsenal.Kind.BILE ? BogrotArsenal.TOX : BogrotArsenal.P;
      }
   }

   private static final class Rot {
      int stacks;
      @Nullable UUID owner;
      long lastGain;
      long lastDecay;
      long spentUntil;
   }

   private static final class Shroud {
      long until;
      long seen;
      float before;
      float given;
   }

   private record Task(long at, Runnable run) {
   }

   private static final class Wisp {
      final ServerLevel level;
      final Player owner;
      Vec3 pos;
      Vec3 vel;
      int age;
      int life = 70;
      int homingDelay = 4;
      double speed = 0.55;
      double maxSpeed = 0.95;
      double accel = 0.03;
      double homing = 0.16;
      double radius = 0.7;
      double size = 0.45;
      @Nullable LivingEntity target;
      Vfx.Palette pal = BogrotArsenal.WISP;
      BogrotArsenal.OnWisp onHit = (w, e, at) -> {};

      Wisp(ServerLevel level, Player owner, Vec3 pos, Vec3 vel, @Nullable LivingEntity target) {
         this.level = level;
         this.owner = owner;
         this.pos = pos;
         this.vel = vel;
         this.target = target;
      }
   }
}

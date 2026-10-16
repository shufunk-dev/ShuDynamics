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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.jspecify.annotations.Nullable;

public final class SekhmarArsenal extends Arsenal {
   private static final Vfx.Palette P = Vfx.Palette.SAND;
   private static final Vfx.Palette SUN = new Vfx.Palette(16777200, 16762944, 13150320).withDebris(Vfx.Debris.FEATHER);
   private static final Vfx.Palette SOUL = new Vfx.Palette(14745599, 4249840, 1714746).withDebris(Vfx.Debris.ASH);
   private static final int GOLD = 15251008;
   private static final int LINEN = 15260864;
   private static final int LAPIS = 2771120;
   private static final Identifier ANKH = MysticalTerrain.id("pharaoh_ankh");
   private static final String KHOPESH = "pharaohs_khopesh";
   private static final String CROOK = "crook_of_sekhmar";
   private static final String FLAIL = "scarab_flail";
   private static final int SEAL_LIFE = 200;
   private static final int MAX_JOBS = 40;
   private static final int MAX_KA = 12;
   private static final int MAX_SCARABS = 16;
   private static final int MAX_LATCHES = 5;
   private static final List<SekhmarArsenal.Job> JOBS = new ArrayList<>();
   private static final Map<LivingEntity, SekhmarArsenal.Seal> SEALS = new WeakHashMap<>();
   private static final Set<LivingEntity> BOUND = Collections.newSetFromMap(new WeakHashMap<>());
   private static final Map<ServerWorld, List<SekhmarArsenal.Sunlit>> ZONES = new HashMap<>();
   private static final List<SekhmarArsenal.Ka> KAS = new ArrayList<>();
   private static final List<SekhmarArsenal.Scarab> SCARABS = new ArrayList<>();
   private static final Map<LivingEntity, List<SekhmarArsenal.Latch>> LATCHED = new WeakHashMap<>();
   private static final Map<LivingEntity, Long> CURSED = new WeakHashMap<>();
   private static final Map<UUID, Long> FAVOUR = new HashMap<>();
   private static final Map<UUID, Long> SUN_RES = new HashMap<>();
   private static final Map<UUID, Long> WARD = new HashMap<>();
   private static final Map<UUID, long[]> HEALED = new HashMap<>();
   private static final Map<UUID, Integer> BROOD = new HashMap<>();
   private static @Nullable Set<Block> sandstone;
   private static boolean hooked;

   public SekhmarArsenal() {
      super("sekhmar", "pharaoh_regalia", Vfx.Palette.SAND);
      hook();
   }

   private static void hook() {
      if (!hooked) {
         hooked = true;
         ServerTickEvents.END_WORLD_TICK.register(SekhmarArsenal::tick);
         ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> reset());
         ServerLivingEntityEvents.AFTER_DEATH.register(SekhmarArsenal::onDeath);
      }
   }

   @Override
   protected void weapons() {
      this.weapon("pharaohs_khopesh", 9.0F, -2.4F)
         .primary(Active.of(100, SekhmarArsenal::scouringCrescent).empowered(40, 1.25F))
         .alternate(Active.of(180, SekhmarArsenal::reapingHook).empowered(50, 1.25F))
         .charge(
            Charge.of(3, 10)
               .pose(UseAction.TRIDENT)
               .cast(Active.of(280, SekhmarArsenal::withheldVerdict).empowered(60, 1.3F))
               .onLevel(SekhmarArsenal::verdictLevel)
         )
         .passive(SekhmarArsenal::curseOfTheSands)
         .resonancePerHit(10.0F);
      this.weapon("crook_of_sekhmar", 5.0F, -2.6F)
         .primary(Active.of(80, SekhmarArsenal::rayOfRa).empowered(30, 1.0F))
         .alternate(Active.of(240, SekhmarArsenal::pillarsOfRa).empowered(50, 1.25F))
         .charge(Charge.of(4, 8).pose(UseAction.TRIDENT).cast(Active.of(360, SekhmarArsenal::sunfall).empowered(60, 1.3F)).onLevel(SekhmarArsenal::sunLevel))
         .passive(SekhmarArsenal::brandOfRa)
         .resonancePerHit(8.0F);
      this.weapon("scarab_flail", 10.0F, -2.9F)
         .primary(Active.of(120, SekhmarArsenal::flailOfDominion).empowered(50, 1.3F))
         .alternate(Active.of(240, SekhmarArsenal::scarabTide).empowered(40, 1.0F))
         .charge(
            Charge.of(3, 12).pose(UseAction.TRIDENT).cast(Active.of(320, SekhmarArsenal::khepriDawn).empowered(60, 1.3F)).onLevel(SekhmarArsenal::dawnLevel)
         )
         .passive(SekhmarArsenal::khepriBrood)
         .resonancePerHit(14.0F);
   }

   @Override
   protected void armor(ArmorKit set) {
      set.twoPiece(SekhmarArsenal::sunBlessed);
      set.fourPiece(SekhmarArsenal::crestGlow);
      set.onHurt(SekhmarArsenal::curseOfTheTomb);
      set.allowDamage(SekhmarArsenal::ankhWard);
      set.allowDeath(SekhmarArsenal::ankh);
      set.active(600, SekhmarArsenal::wrathOfRa);
   }

   static boolean scouringCrescent(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      boolean emp = u.empowered();
      float pw = u.power();
      Vec3d dir = GearKit.flatLook(p);
      float yaw = p.getYaw();
      Vec3d chest = p.getEntityPos().add(0.0, 1.2, 0.0);
      Vec3d from = chest.add(dir.multiply(0.8));
      Vfx.slashArc(level, chest, yaw, 0.0F, 35.0F, 3.0, P, 160.0F);
      Vfx.slashArc(level, chest, yaw, 0.0F, -35.0F, 3.0, P, -160.0F);
      Vfx.streaks(level, chest, dir, P, 8);
      Vfx.smoke(level, p.getEntityPos().add(dir), P.smoke(), 4, 0.6, 1.2);
      int n = emp ? 11 : 8;
      Vfx.Palette pal = emp ? SUN : P;
      Set<LivingEntity> struck = new HashSet<>();
      Vec3d[] last = new Vec3d[]{chest};
      run(level, p, (l, age) -> {
         int k = age + 1;
         Vec3d prev = last[0];
         Vec3d head = from.add(dir.multiply(1.5 * k));
         BlockHitResult wall = clip(l, prev, head);
         boolean blocked = wall.getType() == Type.BLOCK && clip(l, prev.add(0.0, -0.9, 0.0), head.add(0.0, -0.9, 0.0)).getType() == Type.BLOCK;
         if (blocked) {
            head = wall.getPos();
         }

         double len = prev.distanceTo(head);
         if (len > 0.05) {
            for (LivingEntity e : GearKit.alongLine(l, p, prev, dir, len, 1.4)) {
               if (Math.abs(chest(e).y - head.y) < 1.8 && struck.add(e)) {
                  hit(l, p, e, 13.0F * pw, false);
                  seal(l, p, e, 1, 3);
                  if (!boss(e)) {
                     e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0), p);
                  }

                  if (emp) {
                     e.setOnFireFor(3.0F);
                  }

                  GearKit.hitSpark(l, e, P);
                  Vfx.sparks(l, chest(e), P.core(), 6, 0.35, 0.18);
                  u.resonate(3.0F);
               }
            }
         }

         Vfx.slashArc(l, head.subtract(dir.multiply(1.1)), yaw, 0.0F, k % 2 == 0 ? 35.0F : -35.0F, 1.6, pal, k % 2 == 0 ? 150.0F : -150.0F);
         Vfx.smoke(l, ground(l, head), P.smoke(), 3, 0.6, 1.3);
         if (k % 2 == 0) {
            Vfx.streaks(l, head, dir, P, 3);
         }

         if (emp) {
            Vfx.embers(l, head, SUN.glow(), 3, 0.4);
         }

         last[0] = head;
         if (!blocked && k < n) {
            return false;
         }

         crescentEnd(l, p, head, emp, pw);
         return true;
      });
      return true;
   }

   private static void crescentEnd(ServerWorld level, PlayerEntity p, Vec3d head, boolean emp, float pw) {
      Vfx.impact(level, head, P, 0.8);
      Vfx.smoke(level, head, P.smoke(), 8, 1.0, 1.6);
      if (emp) {
         Vfx.explosion(level, head, SUN, 3.0);

         for (LivingEntity e : within(level, p, head, 3.0)) {
            hit(level, p, e, 10.0F * pw, true);
            seal(level, p, e, 1, 3);
            e.setOnFireFor(3.0F);
         }

         sunlit(level, p, ground(level, head), 2.5, 80);
         sfx(level, head, "weapon.crook_of_sekhmar.impact", 0.8F, 1.2F);
      }
   }

   static boolean reapingHook(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      LivingEntity t = GearKit.lookTarget(level, p, 14.0);
      if (t == null) {
         return false;
      }

      boolean emp = u.empowered();
      float pw = u.power();
      List<LivingEntity> hooked = new ArrayList<>();
      hooked.add(t);
      if (emp) {
         List<LivingEntity> more = new ArrayList<>(within(level, p, t.getEntityPos(), 7.0));
         more.remove(t);
         more.removeIf(e -> !t.canSee(e));
         more.sort(byDistance(t));

         for (int i = 0; i < Math.min(2, more.size()); i++) {
            hooked.add(more.get(i));
         }
      }

      Vec3d prev = GearKit.tip(p);
      List<Vec3d> chain = new ArrayList<>();
      chain.add(prev);

      for (LivingEntity h : hooked) {
         Vec3d c = chest(h);
         Vfx.beam(level, prev, c, P, 0.22, 8);
         Vfx.claw(level, c, yawToward(p.getEntityPos(), h.getEntityPos()), 0.0F, 1.2, P);
         Vfx.glint(level, c, 15251008, 0.9);
         hit(level, p, h, 6.0F * pw, false);
         seal(level, p, h, 1, 3);
         chain.add(c);
         prev = c;
      }

      if (emp && chain.size() > 2) {
         Vfx.chainLightning(level, chain, P);
      }

      Vec3d look = GearKit.flatLook(p);
      Vec3d stop = p.getEntityPos().add(look.multiply(1.8));

      for (LivingEntity h : hooked) {
         if (h == t && big(h)) {
            Vec3d d = flat(h.getEntityPos().subtract(p.getEntityPos()));
            double dist = d.length() - (h.getWidth() / 2.0F + 1.2);
            if (dist > 0.3) {
               Vec3d dn = d.normalize();
               double v = Math.min(2.4, 0.1 + dist * 0.14);
               p.setVelocity(dn.x * v, 0.28, dn.z * v);
               p.knockedBack = true;
               Vfx.dashTrail(level, p.getEntityPos(), p.getEntityPos().add(dn.multiply(dist)), P);
            }

            p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 12, 1, true, false, true));
         } else if (!big(h) && h.isAlive()) {
            Vec3d d = stop.subtract(h.getEntityPos());
            h.setVelocity(d.x * 0.15, 0.3 + Math.max(0.0, d.y) * 0.12, d.z * 0.15);
            h.knockedBack = true;
            Vfx.streaks(level, chest(h), d.normalize(), P, 8);
            Vfx.dashTrail(level, h.getEntityPos(), stop, P);
         }
      }

      later(level, p, 6, () -> {
         float yaw = p.getYaw();
         Vec3d chest = p.getEntityPos().add(0.0, 1.2, 0.0);
         Vec3d fl = GearKit.flatLook(p);
         Vfx.slashArc(level, chest, yaw, 0.0F, -25.0F, 3.4, P, 200.0F);
         Vfx.slashArc(level, chest, yaw, 0.0F, -25.0F, 2.2, SUN, 140.0F);
         Vfx.sparks(level, chest.add(fl.multiply(2.0)), 15251008, 10, 0.4, 0.2);
         Set<LivingEntity> reaped = new LinkedHashSet<>();

         for (LivingEntity e : within(level, p, p.getEntityPos(), 3.8)) {
            if (inFront(p, e, fl, 60.0)) {
               reaped.add(e);
            }
         }

         for (LivingEntity hx : hooked) {
            if (hx.isAlive() && hx.distanceTo(p) <= 4.8) {
               reaped.add(hx);
            }
         }

         for (LivingEntity e : reaped) {
            hit(level, p, e, 14.0F * pw, false);
            seal(level, p, e, 1, 3);
            shove(e, p.getEntityPos(), 0.35, 0.15);
            u.resonate(2.0F);
         }

         sfx(level, p.getEntityPos(), "weapon.pharaohs_khopesh.use", 0.8F, 1.2F);
      });
      return true;
   }

   static void verdictLevel(ServerWorld level, PlayerEntity p, WeaponKit kit, int lvl, boolean max) {
      Vec3d tip = GearKit.tip(p);
      Vfx.ring(level, p.getEntityPos(), P.glow(), Math.max(0.8, 2.6 - 0.5 * lvl), 8);
      Vfx.flare(level, tip, max ? SUN.core() : P.glow(), 0.9 + 0.35 * lvl, 6);
      Vfx.sparks(level, tip, P.core(), 3 + 2 * lvl, 0.18, 0.14);
      if (lvl == 1) {
         Vfx.runeCircle(level, ground(level, p.getEntityPos()), 1.5, P, 22, 0.05F, Vfx.Rune.GLYPH);
      }

      if (max) {
         Vfx.gather(level, tip, SUN, 1.4, 10);
         Vfx.glint(level, tip, SUN.core(), 1.3);
         Vec3d right = right(GearKit.flatLook(p));
         Vfx.glint(level, p.getEntityPos().add(right.multiply(0.9)).add(0.0, 2.1, 0.0), 15251008, 0.8);
         Vfx.glint(level, p.getEntityPos().add(right.multiply(-0.9)).add(0.0, 2.1, 0.0), SOUL.core(), 0.8);
      }
   }

   static boolean withheldVerdict(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      int c = MathHelper.clamp(u.charge(), 1, 3);
      float pw = u.power();
      boolean emp = u.empowered();
      double r = 2.4 + 0.6 * c;
      Vec3d d = flat(GearKit.lookPoint(level, p, 6.0).subtract(p.getEntityPos()));
      double dist = Math.min(5.0, d.length());
      Vec3d dn = d.lengthSquared() > 1.0E-4 ? d.normalize() : GearKit.flatLook(p);
      Vec3d dest = ground(level, p.getEntityPos().add(dn.multiply(dist)));
      if (dist < 0.5) {
         p.setVelocity(0.0, 0.35, 0.0);
      } else {
         double v = Math.min(1.0, dist * 0.2);
         p.setVelocity(dn.x * v, 0.42, dn.z * v);
      }

      p.knockedBack = true;
      Vfx.dashTrail(level, p.getEntityPos(), p.getEntityPos().add(dn.multiply(dist * 0.5)).add(0.0, 1.2, 0.0), P);
      Vfx.telegraphCircle(level, dest, r, 7, SUN);
      later(level, p, 7, () -> verdict(level, p, u, c, pw, emp, r));
      return true;
   }

   private static void verdict(ServerWorld level, PlayerEntity p, GearUse u, int c, float pw, boolean emp, double r) {
      Vec3d look = GearKit.flatLook(p);
      Vec3d at = ground(level, p.getEntityPos().add(look.multiply(1.4)));
      if (!p.isOnGround()) {
         p.setVelocity(0.0, -0.8, 0.0);
         p.knockedBack = true;
      }

      Vfx.slashArc(level, p.getEntityPos().add(0.0, 1.6, 0.0), p.getYaw(), 20.0F, 90.0F, 3.4, SUN, 150.0F);
      Vfx.heavyImpact(level, at, P, 0.8 + 0.3 * c);
      Vfx.groundCrack(level, at, r, P);
      GearKit.kick(level, p, 0.3 + 0.1 * c);
      sfx(level, at, "weapon.pharaohs_khopesh.use", 1.0F, 0.75F);
      List<LivingEntity> judged = cylinder(level, p, at, r, 2.5);
      if (emp) {
         List<Vec3d> chain = new ArrayList<>();
         chain.add(chest(p));
         List<LivingEntity> sealed = new ArrayList<>(within(level, p, p.getEntityPos(), 10.0));
         sealed.sort(byDistance(p));

         for (LivingEntity e : sealed) {
            if (!judged.contains(e) && seals(level, e) > 0) {
               int n = breakSeals(level, p, e);
               hit(level, p, e, 8 * n * pw, true);
               if (chain.size() < 7) {
                  chain.add(chest(e));
               }
            }
         }

         if (chain.size() > 1) {
            Vfx.chainLightning(level, chain, SOUL);
         }
      }

      for (LivingEntity e : judged) {
         int n = breakSeals(level, p, e);
         float dmg = (14 + 6 * c) * pw;
         hit(level, p, e, dmg, false);
         if (n > 0) {
            hit(level, p, e, 8 * n * pw, true);
         }

         shove(e, at, 0.4, 0.3);
         u.resonate(3.0F);
         if (n == 3) {
            if (boss(e)) {
               hit(level, p, e, 0.2F * dmg, true);
            } else if (e.isAlive() && e.getHealth() <= e.getMaxHealth() * 0.3F) {
               execute(level, p, e);
            }
         }
      }

      if (c == 3) {
         fissure(level, p, at, look, pw);
      }
   }

   private static void execute(ServerWorld level, PlayerEntity p, LivingEntity e) {
      Vec3d c = chest(e);
      e.timeUntilRegen = 0;
      GearKit.magic(level, p, e, e.getHealth() + e.getAbsorptionAmount() + 20.0F);
      Vfx.impactFrame(level, c, 10.0, 16774336, 0.45F);
      Vfx.pillar(level, e.getEntityPos(), SOUL, 4.0, 0.9);
      Vfx.debris(level, c, Vfx.Debris.ASH, 1714746, 12, 0.4);
   }

   private static void fissure(ServerWorld level, PlayerEntity p, Vec3d at, Vec3d dir, float pw) {
      Vfx.telegraphLine(level, at, dir, 8.0, 1.2, 4, P);
      Set<LivingEntity> split = new HashSet<>();

      for (int k = 1; k <= 5; k++) {
         Vec3d pt = ground(level, at.add(dir.multiply(1.6 * k)));
         later(level, p, 4 + 2 * (k - 1), () -> {
            Vfx.pillar(level, pt, P, 2.2, 0.7);
            Vfx.shards(level, pt.add(0.0, 0.5, 0.0), P.core(), 4, 0.4);

            for (LivingEntity e : cylinder(level, p, pt, 1.3, 2.5)) {
               if (split.add(e)) {
                  hit(level, p, e, 10.0F * pw, false);
                  seal(level, p, e, 1, 3);
                  if (!boss(e)) {
                     e.addVelocity(0.0, 0.55, 0.0);
                     e.knockedBack = true;
                  }
               }
            }
         });
      }
   }

   static void curseOfTheSands(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof PlayerEntity p) {
         boolean wasBound = entombed(level, t);
         seal(level, p, t, 1, 3);
         if (wasBound) {
            hit(level, p, t, 3.0F, true);
            Vfx.flare(level, chest(t), SOUL.glow(), 1.0, 5);
            GearKit.procSound(level, t, "pharaohs_khopesh");
         }
      }

      GearKit.hitSpark(level, t, P);
   }

   static boolean rayOfRa(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float pw = u.power();
      Vec3d look = p.getRotationVector();
      List<Vec3d> dirs = u.empowered() ? List.of(look.rotateY((float) (-Math.PI / 15)), look, look.rotateY((float) (Math.PI / 15))) : List.of(look);
      Vec3d from = GearKit.tip(p);
      GearKit.muzzle(level, p, SUN, 1.2);
      Vfx.ring(level, p.getEntityPos(), SUN.glow(), 1.2, 8);
      Set<LivingEntity> struck = new HashSet<>();

      for (Vec3d dir : dirs) {
         BlockHitResult bh = clip(level, from, from.add(dir.multiply(24.0)));
         Vec3d end = bh.getPos();
         double len = end.distanceTo(from);
         if (len > 0.1) {
            for (LivingEntity e : GearKit.alongLine(level, p, from, dir, len, 0.9)) {
               if (struck.add(e)) {
                  hit(level, p, e, 12.0F * pw, true);
                  e.setOnFireFor(3.0F);
                  seal(level, p, e, 1, 3);
                  Vfx.impact(level, chest(e), SUN, 0.6);
                  u.resonate(4.0F);
               }
            }
         }

         Vfx.beam(level, from, end, SUN, 0.55, 8);
         Vfx.beam(level, from, end, P, 1.1, 4);
         Vfx.flare(level, end, SUN.core(), 2.0, 8);
         Vfx.glint(level, end, SUN.core(), 1.2);
         Vfx.streaks(level, from, dir, SUN, 6);
         if (bh.getType() == Type.BLOCK) {
            Vfx.groundCrack(level, end, 1.1, SUN);
            Vfx.embers(level, end, SUN.glow(), 6, 0.5);
         }
      }

      return true;
   }

   static boolean pillarsOfRa(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      boolean emp = u.empowered();
      float pw = u.power();
      float yaw = p.getYaw();
      Vec3d at = ground(level, aim(level, p, 24.0));
      List<Vec3d> inner = new ArrayList<>();
      List<Vec3d> outer = new ArrayList<>();

      for (int i = 0; i < 3; i++) {
         inner.add(ground(level, at.add(dirOf(yaw + 120 * i).multiply(2.0))));
         if (emp) {
            outer.add(ground(level, at.add(dirOf(yaw + 60.0F + 120 * i).multiply(4.5))));
         }
      }

      Vfx.runeCircle(level, at, emp ? 5.0 : 3.2, SUN, emp ? 22 : 16, 0.08F, Vfx.Rune.SOLAR);
      Vfx.chargeUp(level, at.add(0.0, 4.5, 0.0), SUN, 12);

      for (Vec3d pt : inner) {
         Vfx.telegraphCircle(level, pt, 1.5, 12, SUN);
      }

      for (Vec3d pt : outer) {
         Vfx.telegraphCircle(level, pt, 1.5, 18, SUN);
      }

      Set<LivingEntity> struck = new HashSet<>();
      later(level, p, 12, () -> {
         for (Vec3d ptx : inner) {
            sunPillar(level, p, u, ptx, pw, struck);
         }

         sunlit(level, p, at, emp ? 5.0 : 3.5, emp ? 200 : 120);
      });
      if (emp) {
         later(level, p, 18, () -> {
            for (Vec3d ptx : outer) {
               sunPillar(level, p, u, ptx, pw, struck);
            }
         });
      }

      return true;
   }

   private static void sunPillar(ServerWorld level, PlayerEntity p, GearUse u, Vec3d pt, float pw, Set<LivingEntity> struck) {
      int h = ceiling(level, pt, 12);
      Vfx.beam(level, pt.add(0.0, h, 0.0), pt, SUN, 1.5, 10);
      Vfx.pillar(level, pt, SUN, h, 1.5);
      Vfx.heavyImpact(level, pt, SUN, 0.6);

      for (LivingEntity e : column(level, p, pt, 1.5, h)) {
         if (struck.add(e)) {
            hit(level, p, e, 14.0F * pw, true);
            e.setOnFireFor(4.0F);
            seal(level, p, e, 1, 3);
            if (!boss(e)) {
               e.addVelocity(0.0, 0.55, 0.0);
               e.knockedBack = true;
            }

            u.resonate(4.0F);
         }
      }

      sfx(level, pt, "weapon.crook_of_sekhmar.pillar", 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
   }

   private static Vec3d sunPos(PlayerEntity p) {
      return p.getEyePos().add(0.0, 0.9, 0.0).add(p.getRotationVector().multiply(0.3));
   }

   static void sunLevel(ServerWorld level, PlayerEntity p, WeaponKit kit, int lvl, boolean max) {
      Vec3d sun = sunPos(p);
      Vfx.flare(level, sun, SUN.glow(), 0.8 + 0.45 * lvl, 8);
      Vfx.gather(level, sun, SUN, 1.2 + 0.3 * lvl, 6);
      Vfx.ring(level, p.getEntityPos(), SUN.glow(), 1.0 + 0.6 * lvl, 8);
      if (lvl == 1) {
         Vfx.runeCircle(level, ground(level, p.getEntityPos()), 2.2, SUN, 26, 0.05F, Vfx.Rune.SOLAR);
      }

      if (max) {
         Vfx.glint(level, sun, SUN.core(), 1.6);
         Vfx.nova(level, sun, SUN, 0.9);
      }
   }

   static boolean sunfall(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      int c = MathHelper.clamp(u.charge(), 1, 4);
      float pw = u.power();
      boolean emp = u.empowered();
      Vec3d target = ground(level, aim(level, p, 28.0));
      double radius = 2.5 + 0.9 * c;
      Vec3d from = sunPos(p);
      double dist = from.distanceTo(target);
      int ticks = MathHelper.clamp((int)Math.round(dist / 1.6), 8, 18);
      Vec3d apex = from.add(target).multiply(0.5).add(0.0, 2.5 + dist * 0.12, 0.0);
      Vfx.telegraphCircle(level, target, radius, ticks, SUN);
      Set<LivingEntity> struck = new HashSet<>();
      sunArc(level, p, from, apex, target, ticks, c, 1.0, at -> sunImpact(level, p, u, at, radius, c, pw, 1.0, struck, true));
      if (emp) {
         Vec3d throwDir = flat(target.subtract(from));
         Vec3d perp = right(throwDir.lengthSquared() > 1.0E-4 ? throwDir.normalize() : GearKit.flatLook(p));
         Vec3d half = bezier(from, apex, target, 0.5);
         int legs = ticks - ticks / 2 + 3;

         for (int s = -1; s <= 1; s += 2) {
            Vec3d side = ground(level, target.add(perp.multiply(s * radius * 0.9)));
            Vfx.telegraphCircle(level, side, radius * 0.7, ticks + 3, SUN);
            Vec3d ctrl = half.add(side).multiply(0.5).add(0.0, 1.2, 0.0);
            later(
               level,
               p,
               ticks / 2,
               () -> sunArc(level, p, half, ctrl, side, legs, c, 0.7, at -> sunImpact(level, p, u, at, radius * 0.7, c, pw * 0.6F, 0.7, struck, false))
            );
         }
      }

      return true;
   }

   private static void sunArc(ServerWorld level, PlayerEntity p, Vec3d a, Vec3d ctrl, Vec3d b, int ticks, int c, double scale, Consumer<Vec3d> land) {
      Vec3d[] last = new Vec3d[]{a};
      run(level, p, (l, age) -> {
         int t = age + 1;
         Vec3d prev = last[0];
         Vec3d pos = bezier(a, ctrl, b, Math.min(1.0, (double)t / ticks));
         BlockHitResult bh = clip(l, prev, pos);
         boolean early = bh.getType() == Type.BLOCK && t < ticks;
         if (early) {
            pos = bh.getPos();
         }

         Vec3d vel = pos.subtract(prev);
         Vfx.meteorTrail(l, pos, vel, SUN);
         Vfx.flare(l, pos, SUN.core(), (1.1 + 0.25 * c) * scale, 3);
         if (t % 3 == 0) {
            Vfx.embers(l, pos, SUN.glow(), 2, 0.3);
         }

         last[0] = pos;
         if (!early && t < ticks) {
            return false;
         }

         land.accept(early ? ground(l, pos.subtract(vel.normalize().multiply(0.3))) : b);
         return true;
      });
   }

   private static void sunImpact(
      ServerWorld level, PlayerEntity p, GearUse u, Vec3d at, double r, int c, float pw, double scale, Set<LivingEntity> struck, boolean main
   ) {
      Vfx.nova(level, at.add(0.0, 0.5, 0.0), SUN, r);
      Vfx.heavyImpact(level, at, SUN, (0.8 + 0.3 * c) * (main ? 1.0 : 0.7));
      Vfx.groundCrack(level, at, r, SUN);
      Vfx.pillar(level, at, SUN, (3 + c) * scale, 1.2 * scale);
      sfx(level, at, "weapon.crook_of_sekhmar.impact", (float)((1.2 + 0.1 * c) * (main ? 1.0 : 0.8)), 1.05F - 0.05F * c + (main ? 0.0F : 0.15F));

      for (LivingEntity e : cylinder(level, p, at, r, 3.0)) {
         if (struck.add(e)) {
            double d = Math.sqrt(flatDistSqr(e.getEntityPos(), at));
            float f = (float)(1.0 - 0.4 * Math.min(1.0, d / r));
            hit(level, p, e, (10 + 6 * c) * pw * f, true);
            e.setOnFireFor(5.0F);
            seal(level, p, e, c == 4 ? 2 : 1, 3);
            shove(e, at, 0.8 + 0.1 * c, 0.45);
            u.resonate(4.0F);
         }
      }

      sunlit(level, p, at, (1.5 + 0.8 * c) * scale, c == 4 ? 160 : 100);
      if (c == 4 && main) {
         Vfx.telegraphCircle(level, at, r + 3.0, 8, SUN);
         later(level, p, 8, () -> {
            Vfx.shockwave(level, at, SUN, r + 3.0);

            for (LivingEntity ex : cylinder(level, p, at, r + 3.0, 1.5)) {
               if (Math.sqrt(flatDistSqr(ex.getEntityPos(), at)) > r - 0.5) {
                  hit(level, p, ex, 8.0F * pw, true);
                  seal(level, p, ex, 1, 3);
               }
            }
         });
      }
   }

   static void brandOfRa(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof PlayerEntity p) {
         boolean burning = t.getFireTicks() > 0;
         t.setOnFireFor(3.0F);
         if (burning) {
            seal(level, p, t, 1, 3);
            Vfx.flare(level, chest(t), SUN.glow(), 1.1, 5);
            GearKit.procSound(level, t, "crook_of_sekhmar");
         }
      }

      GearKit.hitSpark(level, t, SUN);
   }

   static boolean flailOfDominion(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float pw = u.power();
      boolean emp = u.empowered();
      int[] flung = new int[]{0};
      lash(level, p, u, 0, pw, emp, flung);
      later(level, p, 5, () -> lash(level, p, u, 1, pw, emp, flung));
      later(level, p, 10, () -> lash(level, p, u, 2, pw, emp, flung));
      return true;
   }

   private static void lash(ServerWorld level, PlayerEntity p, GearUse u, int k, float pw, boolean emp, int[] flung) {
      float yaw = p.getYaw();
      Vec3d look = GearKit.flatLook(p);
      Vec3d side = right(look);
      Vec3d base = p.getEntityPos();
      float roll = k == 0 ? 45.0F : (k == 1 ? -45.0F : 90.0F);
      Vfx.slashArc(level, base.add(0.0, 1.3, 0.0), yaw, 8.0F, roll, 5.0, P, k == 2 ? 120.0F : 150.0F);

      for (int s = -1; s <= 1; s++) {
         Vfx.glint(level, base.add(look.multiply(4.6)).add(side.multiply(0.6 * s)).add(0.0, 1.0, 0.0), 15251008, 0.5);
      }

      Vfx.sparks(level, base.add(look.multiply(3.5)).add(0.0, 1.0, 0.0), P.core(), 8, 0.4, 0.2);
      List<LivingEntity> hits = new ArrayList<>(cone(level, p, 5.0, 40.0));
      if (k < 2) {
         for (LivingEntity e : hits) {
            hit(level, p, e, 8.0F * pw, false);
            shove(e, base, 0.2, 0.05);
            u.resonate(2.0F);
         }

         if (k == 1) {
            sfx(level, base, "weapon.scarab_flail.lash", 0.9F, 1.0F);
         }
      } else {
         Vec3d at = ground(level, base.add(look.multiply(3.2)));

         for (LivingEntity e : cylinder(level, p, at, 2.2, 2.5)) {
            if (!hits.contains(e)) {
               hits.add(e);
            }
         }

         for (LivingEntity e : hits) {
            hit(level, p, e, 12.0F * pw, false);
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), p);
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0), p);
            seal(level, p, e, 1, 3);
            u.resonate(3.0F);
         }

         Vfx.heavyImpact(level, at, P, 0.7);
         Vfx.groundCrack(level, at, 2.2, P);
         GearKit.kick(level, p, 0.3);
         sfx(level, base, "weapon.scarab_flail.lash", 1.1F, 0.85F);
      }

      if (emp && flung[0] < 3 && scarabCount(p.getUuid()) < 16) {
         LivingEntity near = null;
         double best = Double.MAX_VALUE;

         for (LivingEntity e : hits) {
            double d = e.squaredDistanceTo(p);
            if (e.isAlive() && d < best) {
               best = d;
               near = e;
            }
         }

         if (near != null) {
            Vfx.dashTrail(level, GearKit.tip(p), chest(near), P);
            latch(level, p, near, 60, pw, false);
            flung[0]++;
         }
      }
   }

   static boolean scarabTide(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      boolean emp = u.empowered();
      int n = Math.min(emp ? 10 : 6, 16 - scarabCount(p.getUuid()));
      if (n <= 0) {
         return false;
      }

      Vec3d fl = GearKit.flatLook(p);
      Vec3d spawn = ground(level, p.getEntityPos().add(fl.multiply(0.6)));

      for (int i = 0; i < n; i++) {
         double a = n == 1 ? 0.0 : -60.0 + 120.0 * i / (n - 1);
         Vec3d v = fl.rotateY((float)(a * (float) (Math.PI / 180.0))).multiply(0.5);
         SCARABS.add(new SekhmarArsenal.Scarab(level, p.getUuid(), spawn, v, 0.3, 0.25, 100, u.power(), emp));
      }

      Vec3d chest = p.getEntityPos().add(0.0, 1.2, 0.0);
      Vfx.shockwave(level, p.getEntityPos(), P, 3.0);
      Vfx.streaks(level, chest, fl, P, 10);
      Vfx.smoke(level, p.getEntityPos(), P.smoke(), 8, 1.0, 1.4);
      Vfx.sparks(level, chest, 15251008, 10, 0.4, 0.18);
      return true;
   }

   private static Vec3d headTop(PlayerEntity p) {
      return p.getEntityPos().add(0.0, p.getHeight() + 0.6, 0.0);
   }

   static void dawnLevel(ServerWorld level, PlayerEntity p, WeaponKit kit, int lvl, boolean max) {
      Vec3d top = headTop(p);
      Vfx.slashArc(level, top, p.getYaw() + 120 * lvl, 0.0F, 0.0F, 1.3 + 0.3 * lvl, P, 340.0F);
      Vfx.flare(level, top, SUN.glow(), 0.8 + 0.4 * lvl, 6);
      Vfx.ring(level, p.getEntityPos(), P.glow(), 1.0 + 0.5 * lvl, 8);
      if (max) {
         Vfx.glint(level, top, SUN.core(), 1.4);
         Vfx.gather(level, top, SUN, 1.6, 10);
      }
   }

   static boolean khepriDawn(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      int c = MathHelper.clamp(u.charge(), 1, 3);
      float pw = u.power();
      boolean emp = u.empowered();
      Vec3d dir = GearKit.flatLook(p);
      Vec3d start = ground(level, p.getEntityPos().add(dir.multiply(1.8)));
      Vfx.slashArc(level, p.getEntityPos().add(0.0, 1.5, 0.0), p.getYaw(), 15.0F, 90.0F, 3.0, P, 140.0F);
      Vfx.heavyImpact(level, start, SUN, 0.8);

      for (LivingEntity e : within(level, p, start, 2.5)) {
         hit(level, p, e, (12 + 4 * c) * pw, false);
         shove(e, start, 0.5, 0.35);
      }

      GearKit.kick(level, p, 0.3);
      Set<LivingEntity> rolled = new HashSet<>();
      double length = 10 + 4 * c;
      later(level, p, 2, () -> rollSun(level, p, u, start, dir, 1.1 + 0.2 * c, length, c, pw, 1.0, rolled, true));
      if (emp) {
         for (int s = -1; s <= 1; s += 2) {
            Vec3d d2 = dir.rotateY(s * 25 * (float) (Math.PI / 180.0));
            Vec3d st = ground(level, p.getEntityPos().add(d2.multiply(1.8)));
            later(level, p, 2, () -> rollSun(level, p, u, st, d2, 0.9, length, c, pw * 0.7F, 0.7, rolled, false));
         }
      }

      return true;
   }

   private static void rollSun(
      ServerWorld level,
      PlayerEntity p,
      GearUse u,
      Vec3d start,
      Vec3d dir,
      double rb,
      double length,
      int c,
      float pw,
      double scale,
      Set<LivingEntity> rolled,
      boolean main
   ) {
      int steps = Math.max(1, (int)Math.round(length / 0.7));
      Vec3d perp = right(dir);
      double[] floor = new double[]{start.y};
      Vec3d[] at = new Vec3d[]{start.add(0.0, rb * 0.8, 0.0)};
      if (main) {
         sfx(level, at[0], "weapon.scarab_flail.roll", 1.1F, 1.0F);
      }

      run(level, p, (l, age) -> {
         Vec3d pos = at[0];
         Vec3d next = pos.add(dir.multiply(0.7));
         Vec3d g = surface(l, next.x, floor[0], next.z, 1.25, 3.0);
         if (g == null) {
            sunBurst(l, p, pos, new Vec3d(pos.x, floor[0], pos.z), c, pw, scale, main);
            return true;
         }

         floor[0] = g.y;
         pos = new Vec3d(next.x, g.y + rb * 0.8, next.z);
         at[0] = pos;
         Vfx.flare(l, pos, SUN.core(), (1.5 + 0.3 * c) * scale, 3);
         Vfx.meteorTrail(l, pos, dir.multiply(0.7), SUN);
         if (age % 3 == 0) {
            Vfx.groundCrack(l, g, 0.9 * scale, SUN);
         } else if (age % 3 == 1) {
            sunTrack(l, p, g, 1.4 * scale, 80);
         }

         boolean stop = false;

         for (LivingEntity e : within(l, p, pos, rb + 0.6)) {
            if (!(Math.abs(e.getY() - g.y) >= rb + 1.0)) {
               if (rolled.add(e)) {
                  hit(l, p, e, (10 + 4 * c) * pw, true);
                  e.setOnFireFor(4.0F);
                  seal(l, p, e, 1, 3);
                  if (!boss(e)) {
                     Vec3d v = e.getEntityPos().subtract(pos);
                     double side = v.x * perp.x + v.z * perp.z >= 0.0 ? 1.0 : -1.0;
                     e.addVelocity(perp.x * side * 0.6, 0.6, perp.z * side * 0.6);
                     e.knockedBack = true;
                  }

                  Vfx.impact(l, chest(e), SUN, 0.8);
                  u.resonate(3.0F);
               }

               if (boss(e)) {
                  stop = true;
               }
            }
         }

         if (!stop && age + 1 < steps) {
            return false;
         }

         sunBurst(l, p, pos, g, c, pw, scale, main);
         return true;
      });
   }

   private static void sunBurst(ServerWorld level, PlayerEntity p, Vec3d pos, Vec3d floor, int c, float pw, double scale, boolean main) {
      double r = (2.5 + 0.5 * c) * scale;
      Vfx.nova(level, pos, SUN, r);
      Vfx.heavyImpact(level, floor, SUN, (0.8 + 0.3 * c) * (main ? 1.0 : 0.7));

      for (LivingEntity e : within(level, p, pos, r)) {
         hit(level, p, e, (12 + 4 * c) * pw, true);
         seal(level, p, e, 1, 3);
         shove(e, pos, 1.0, 0.5);
      }

      sfx(level, pos, "weapon.scarab_flail.burst", main ? 1.3F : 1.0F, main ? 1.0F : 1.15F);
   }

   static void khepriBrood(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof PlayerEntity p) {
         int n = BROOD.merge(p.getUuid(), 1, Integer::sum);
         if (n >= 3) {
            BROOD.put(p.getUuid(), 0);
            if (t.isAlive() && scarabCount(p.getUuid()) < 16) {
               Vfx.dashTrail(level, p.getEyePos(), chest(t), P);
               latch(level, p, t, 60, 1.0F, false);
               return;
            }
         }
      }

      GearKit.hitSpark(level, t, P);
   }

   static void sunBlessed(ServerWorld level, PlayerEntity p, int pieces) {
      long now = level.getTime();
      if (now % 10L == 0L) {
         p.removeStatusEffect(StatusEffects.BLINDNESS);
         p.removeStatusEffect(StatusEffects.DARKNESS);
      }

      if (now % 20L == 0L && (onSand(level, p) || favour(p))) {
         GearKit.keep(p, StatusEffects.SPEED, 0);
      }

      if (now % 40L == 0L && inOwnSun(p)) {
         p.heal(1.0F);
         Vfx.auraRing(level, p.getEntityPos(), SUN, 0.9);
      }
   }

   static void crestGlow(ServerWorld level, PlayerEntity p, int pieces) {
      if (level.getTime() % 40L == 0L) {
         Vfx.embers(level, p.getEntityPos().add(0.0, 2.5, 0.0), SUN.glow(), 1, 0.3);
      }
   }

   static void curseOfTheTomb(ServerWorld level, PlayerEntity p, @Nullable LivingEntity attacker, DamageSource source, float taken, int pieces) {
      if (pieces >= 4 && attacker != null && !(attacker instanceof PlayerEntity) && attacker.isAlive()) {
         boolean melee = source.getSource() == attacker;
         if (melee || !(attacker.squaredDistanceTo(p) > 256.0)) {
            long now = level.getTime();
            Long last = CURSED.get(attacker);
            if (last == null || now - last >= (melee ? 20 : 40)) {
               CURSED.put(attacker, now);
               seal(level, p, attacker, 1, 3);
               Vec3d c = chest(p);
               Vec3d to = chest(attacker).subtract(c);
               Vfx.streaks(level, c, to.lengthSquared() > 1.0E-4 ? to.normalize() : GearKit.flatLook(p), SOUL, 5);
               GearKit.proc(level, attacker, SOUL);
               sfx(level, attacker.getEntityPos(), "weapon.pharaohs_khopesh.proc", 0.7F, 0.9F);
            }
         }
      }
   }

   static boolean ankhWard(ServerWorld level, PlayerEntity p, DamageSource source, float amount, int pieces) {
      if (pieces >= 4 && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         Long until = WARD.get(p.getUuid());
         if (until != null && until > level.getTime()) {
            Vfx.flare(level, p.getEntityPos().add(0.0, 1.0, 0.0), SUN.glow(), 1.4, 4);
            return false;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   static boolean ankh(ServerWorld level, PlayerEntity p, DamageSource source, float amount, int pieces) {
      if (pieces >= 4 && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         ItemStack chest = p.getEquippedStack(EquipmentSlot.CHEST);
         if (p.getItemCooldownManager().isCoolingDown(chest)) {
            return true;
         }

         long now = level.getTime();
         p.setHealth(p.getMaxHealth() * 0.4F);
         p.getItemCooldownManager().set(chest, 6000);
         p.getItemCooldownManager().set(ANKH, 6000);
         WARD.put(p.getUuid(), now + 30L);
         p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1, true, false, true));
         Vec3d at = p.getEntityPos();

         for (LivingEntity e : within(level, p, at, 6.0)) {
            e.timeUntilRegen = 0;
            GearKit.magic(level, p, e, 12.0F);
            seal(level, p, e, 2, 3);
            shove(e, at, 1.2, 0.5);
         }

         level.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1.0, p.getZ(), 60, 0.5, 0.8, 0.5, 0.4);
         Vfx.pillar(level, at, SUN, 8.0, 1.2);
         Vfx.nova(level, at.add(0.0, 1.0, 0.0), SUN, 6.0);
         Vfx.ward(level, at, 1.4, SUN, 30);
         Vfx.runeCircle(level, at, 3.0, SUN, 40, 0.1F, Vfx.Rune.SOLAR);
         Vfx.ring(level, at.add(0.0, 0.1, 0.0), 2771120, 2.2, 14);
         Vfx.impactFrame(level, at, 12.0, 16774336, 0.6F);
         sfx(level, at, "weapon.crook_of_sekhmar.nova", 1.4F, 1.2F);
         sfx(level, at, "weapon.crook_of_sekhmar.ka", 1.0F, 0.8F);
         return false;
      } else {
         return true;
      }
   }

   static boolean wrathOfRa(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float yaw0 = p.getYaw();
      Vec3d feet = p.getEntityPos();
      p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 70, 0, true, false, true));
      Vfx.runeCircle(level, feet, 8.0, SUN, 70, 0.05F, Vfx.Rune.SOLAR);
      Vfx.chargeUp(level, feet.add(0.0, 3.2, 0.0), SUN, 10);
      Vec3d back = feet.add(0.0, 1.6, 0.0);
      Vfx.slashArc(level, back, yaw0 + 180.0F + 70.0F, -10.0F, 60.0F, 2.8, SUN, 110.0F);
      Vfx.slashArc(level, back, yaw0 + 180.0F - 70.0F, -10.0F, -60.0F, 2.8, SUN, -110.0F);
      Vfx.telegraphCircle(level, feet, 7.0, 60, SUN);
      run(level, p, (l, age) -> {
         int t = age + 1;
         if (t < 60) {
            if (t >= 10) {
               sunWheel(l, p, yaw0, t);
            }

            return false;
         } else {
            Vec3d f = p.getEntityPos();
            Vfx.nova(l, f.add(0.0, 1.0, 0.0), SUN, 7.0);
            Vfx.heavyImpact(l, f, SUN, 2.0);
            Vfx.pillar(l, f, SUN, 7.0, 1.6);

            for (LivingEntity e : within(l, p, f, 7.0)) {
               hit(l, p, e, 16.0F, true);
               seal(l, p, e, 1, 3);
               shove(e, f, 1.0, 0.4);
            }

            sunlit(l, p, ground(l, f), 6.0, 160);
            sfx(l, f, "weapon.crook_of_sekhmar.nova", 1.8F, 0.9F);
            return true;
         }
      });
      return true;
   }

   private static void sunWheel(ServerWorld level, PlayerEntity p, float yaw0, int t) {
      double angle = yaw0 + 180.0 * (t - 10) / 50.0;
      Vec3d from = p.getEntityPos().add(0.0, 1.1, 0.0);
      Vec3d[] ends = new Vec3d[4];

      for (int k = 0; k < 4; k++) {
         ends[k] = clip(level, from, from.add(dirOf(angle + 90 * k).multiply(9.0))).getPos();
         Vfx.beam(level, from, ends[k], SUN, 0.5);
      }

      if ((t - 10) % 10 == 0) {
         Vfx.flare(level, from, SUN.core(), 1.6, 10);

         for (LivingEntity e : GearKit.enemies(level, p, from, 10.5)) {
            if (!(Math.abs(e.getY() - p.getY()) >= 2.0)) {
               double reach = 1.0 + e.getWidth() / 2.0F;

               for (Vec3d end : ends) {
                  if (segDistFlat(e.getEntityPos(), from, end) <= reach) {
                     hit(level, p, e, 7.0F, true);
                     e.setOnFireFor(3.0F);
                     seal(level, p, e, 1, 3);
                     Vfx.impact(level, chest(e), SUN, 0.5);
                     break;
                  }
               }
            }
         }

         for (Vec3d end : ends) {
            Vfx.groundCrack(level, ground(level, end), 1.0, SUN);
         }

         if (t == 10) {
            sfx(level, from, "weapon.crook_of_sekhmar.wheel", 1.3F, 1.0F);
         }
      }
   }

   private static Set<Block> sandstone() {
      if (sandstone == null) {
         sandstone = Set.of(
            Blocks.SANDSTONE,
            Blocks.CUT_SANDSTONE,
            Blocks.CHISELED_SANDSTONE,
            Blocks.SMOOTH_SANDSTONE,
            Blocks.SANDSTONE_STAIRS,
            Blocks.SANDSTONE_SLAB,
            Blocks.SANDSTONE_WALL,
            Blocks.CUT_SANDSTONE_SLAB,
            Blocks.SMOOTH_SANDSTONE_STAIRS,
            Blocks.SMOOTH_SANDSTONE_SLAB,
            Blocks.RED_SANDSTONE,
            Blocks.CUT_RED_SANDSTONE,
            Blocks.CHISELED_RED_SANDSTONE,
            Blocks.SMOOTH_RED_SANDSTONE,
            Blocks.RED_SANDSTONE_STAIRS,
            Blocks.RED_SANDSTONE_SLAB,
            Blocks.RED_SANDSTONE_WALL,
            Blocks.CUT_RED_SANDSTONE_SLAB,
            Blocks.SMOOTH_RED_SANDSTONE_STAIRS,
            Blocks.SMOOTH_RED_SANDSTONE_SLAB
         );
      }

      return sandstone;
   }

   private static boolean onSand(ServerWorld level, PlayerEntity p) {
      BlockState s = level.getBlockState(BlockPos.ofFloored(p.getX(), p.getY() - 0.2, p.getZ()));
      return s.isIn(BlockTags.SAND) || sandstone().contains(s.getBlock());
   }

   private static SekhmarArsenal.@Nullable Seal entry(ServerWorld level, LivingEntity e) {
      SekhmarArsenal.Seal s = SEALS.get(e);
      if (s == null) {
         return null;
      }

      long now = level.getTime();
      if (s.stacks > 0 && now >= s.expires) {
         s.stacks = 0;
         s.entombedUntil = 0L;
      }

      if (s.stacks <= 0 && now >= s.immuneUntil) {
         SEALS.remove(e);
         BOUND.remove(e);
         return null;
      } else {
         return s;
      }
   }

   static int seals(ServerWorld level, LivingEntity e) {
      SekhmarArsenal.Seal s = entry(level, e);
      return s == null ? 0 : s.stacks;
   }

   static boolean entombed(ServerWorld level, LivingEntity e) {
      SekhmarArsenal.Seal s = entry(level, e);
      return s != null && s.stacks > 0 && s.entombedUntil > level.getTime();
   }

   static int seal(ServerWorld level, @Nullable PlayerEntity p, LivingEntity e, int n, int cap) {
      if (n > 0 && e.isAlive() && !e.isRemoved() && !(e instanceof PlayerEntity)) {
         long now = level.getTime();
         SekhmarArsenal.Seal s = entry(level, e);
         if (s == null) {
            s = new SekhmarArsenal.Seal();
            SEALS.put(e, s);
         }

         int before = s.stacks;
         int limit = Math.min(3, cap);
         int after = before >= limit ? before : Math.min(limit, before + n);
         s.stacks = after;
         s.expires = now + 200L;
         if (p != null) {
            s.owner = p.getUuid();
         }

         if (after > before) {
            double w = e.getWidth();
            double h = e.getHeight();
            Vfx.ring(level, e.getEntityPos().add(0.0, h * (0.2 + 0.28 * (after - 1)), 0.0), 15251008, 0.35 + 0.55 * w, 12);
            Vfx.sparks(level, chest(e), P.core(), 2 + after, 0.15, 0.12);
            if (after == 3) {
               Vfx.glint(level, top(e).add(0.0, 0.35, 0.0), SOUL.core(), 0.8);
               if (now >= s.immuneUntil) {
                  entomb(level, p, e, s, now);
               }
            }
         }

         return after;
      } else {
         return seals(level, e);
      }
   }

   private static void entomb(ServerWorld level, @Nullable PlayerEntity p, LivingEntity e, SekhmarArsenal.Seal s, long now) {
      boolean boss = boss(e);
      if (boss) {
         e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 0), p);
      } else {
         e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 3), p);
         e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30, 0), p);
         BOUND.add(e);
      }

      s.entombedUntil = now + 30L;
      s.immuneUntil = now + (boss ? 240 : 120);
      double w = e.getWidth();
      double h = e.getHeight();
      Vec3d feet = e.getEntityPos();
      Vfx.vortex(level, feet, Math.max(0.6, w * 0.7), h + 0.2, P, 30);

      for (double f : new double[]{0.25, 0.5, 0.8}) {
         Vfx.ring(level, feet.add(0.0, h * f, 0.0), 15260864, 0.25 + 0.6 * w, 30);
      }

      Vfx.shards(level, chest(e), 15260864, 8, 0.2);
      Vfx.glint(level, top(e), SOUL.core(), 1.0);
      sfx(level, chest(e), "weapon.pharaohs_khopesh.seal", 1.0F, 1.0F);
   }

   static int breakSeals(ServerWorld level, PlayerEntity p, LivingEntity e) {
      SekhmarArsenal.Seal s = entry(level, e);
      if (s != null && s.stacks > 0) {
         int n = s.stacks;
         s.stacks = 0;
         s.entombedUntil = 0L;
         BOUND.remove(e);
         Vec3d c = chest(e);
         Vfx.nova(level, c, SOUL, 1.0 + 0.4 * n);
         Vfx.shards(level, c, 15260864, 6 + 4 * n, 0.5);
         Vfx.glint(level, c, SOUL.core(), 1.0 + 0.2 * n);
         sfx(level, c, "weapon.pharaohs_khopesh.break", 0.8F + 0.2F * n, 1.0F);
         ka(level, p, c, n, 4 * n);
         return n;
      } else {
         return 0;
      }
   }

   private static void tickSeals(ServerWorld level, long now) {
      if (!BOUND.isEmpty()) {
         for (LivingEntity e : new ArrayList<>(BOUND)) {
            if (e.getEntityWorld() == level) {
               SekhmarArsenal.Seal s = SEALS.get(e);
               if (s != null && now < s.entombedUntil && e.isAlive()) {
                  Vec3d v = e.getVelocity();
                  e.setVelocity(0.0, Math.min(0.0, v.y), 0.0);
                  e.knockedBack = true;
                  if (now % 5L == 0L) {
                     Vfx.smoke(level, e.getEntityPos(), P.smoke(), 1, 0.3, 0.8);
                  }
               } else {
                  BOUND.remove(e);
               }
            }
         }
      }

      if (now % 20L == 0L && !SEALS.isEmpty()) {
         for (LivingEntity e : new ArrayList<>(SEALS.keySet())) {
            if (e.getEntityWorld() == level) {
               if (!e.isAlive()) {
                  SEALS.remove(e);
               } else {
                  int n = seals(level, e);
                  if (n > 0) {
                     Vfx.glint(level, top(e).add(0.0, 0.3, 0.0), n == 3 ? SOUL.core() : 15251008, 0.2 + 0.08 * n);
                  }
               }
            }
         }
      }
   }

   private static void ka(ServerWorld level, PlayerEntity p, Vec3d from, float heal, float res) {
      int n = 0;

      for (SekhmarArsenal.Ka k : KAS) {
         if (!k.done && k.owner.equals(p.getUuid())) {
            n++;
         }
      }

      if (n < 12) {
         KAS.add(new SekhmarArsenal.Ka(level, p.getUuid(), from, heal, res));
         Vfx.flare(level, from, SOUL.glow(), 0.9, 5);
      }
   }

   private static void tickKa(ServerWorld level) {
      if (!KAS.isEmpty()) {
         for (SekhmarArsenal.Ka k : new ArrayList<>(KAS)) {
            if (k.level == level && !k.done) {
               PlayerEntity p = level.getPlayerByUuid(k.owner);
               if (p != null && p.isAlive()) {
                  Vec3d chest = p.getEntityPos().add(0.0, p.getHeight() * 0.55, 0.0);
                  Vec3d d = chest.subtract(k.pos);
                  double len = d.length();
                  k.age++;
                  if (!(len < 0.8) && k.age < 40) {
                     Vec3d step = d.multiply(0.25);
                     if (step.length() < 0.6) {
                        step = d.multiply(Math.min(len, 0.6) / len);
                     }

                     k.pos = k.pos.add(step);
                     Vfx.trail(level, k.pos, SOUL, 0.35);
                     if (k.age % 3 == 0) {
                        Vfx.glint(level, k.pos, SOUL.core(), 0.3);
                     }
                  } else {
                     kaArrives(level, p, k, chest);
                     k.done = true;
                  }
               } else {
                  k.done = true;
               }
            }
         }

         KAS.removeIf(kx -> kx.done);
      }
   }

   private static void kaArrives(ServerWorld level, PlayerEntity p, SekhmarArsenal.Ka k, Vec3d chest) {
      healCapped(level, p, k.heal);
      if (p.getMainHandStack().getItem() instanceof MysticWeapon w && ours(w.gearId())) {
         GearState.of(p).addResonance(w.gearId(), k.res);
      }

      Vfx.flare(level, chest, SOUL.glow(), 1.3, 6);
      Vfx.sparks(level, chest, SOUL.core(), 6, 0.2, 0.14);
      Vfx.ring(level, p.getEntityPos(), SOUL.glow(), 0.9, 8);
      sfx(level, chest, "weapon.crook_of_sekhmar.ka", 0.6F, 1.1F + level.getRandom().nextFloat() * 0.2F);
   }

   private static void healCapped(ServerWorld level, PlayerEntity p, float amount) {
      long sec = level.getTime() / 20L;
      long[] h = HEALED.computeIfAbsent(p.getUuid(), k -> new long[2]);
      if (h[0] != sec) {
         h[0] = sec;
         h[1] = 0L;
      }

      float give = Math.min(amount, 6.0F - (float)h[1] / 100.0F);
      if (!(give <= 0.0F)) {
         h[1] += Math.round(give * 100.0F);
         p.heal(give);
      }
   }

   static void sunlit(ServerWorld level, PlayerEntity p, Vec3d c, double r, int ticks) {
      addZone(level, new SekhmarArsenal.Sunlit(p.getUuid(), c, r, level.getTime() + ticks, false), 6);
      Vfx.runeCircle(level, c, r, SUN, ticks, 0.02F, Vfx.Rune.SOLAR);
      Vfx.ring(level, c, SUN.glow(), r, 12);
   }

   static void sunTrack(ServerWorld level, PlayerEntity p, Vec3d c, double r, int ticks) {
      addZone(level, new SekhmarArsenal.Sunlit(p.getUuid(), c, r, level.getTime() + ticks, true), 16);
      Vfx.groundCrack(level, c, r, SUN);
   }

   private static void addZone(ServerWorld level, SekhmarArsenal.Sunlit z, int cap) {
      List<SekhmarArsenal.Sunlit> list = ZONES.computeIfAbsent(level, k -> new ArrayList<>());
      int n = 0;
      SekhmarArsenal.Sunlit oldest = null;

      for (SekhmarArsenal.Sunlit o : list) {
         if (o.track == z.track && o.owner.equals(z.owner)) {
            n++;
            if (oldest == null) {
               oldest = o;
            }
         }
      }

      if (n >= cap && oldest != null) {
         list.remove(oldest);
      }

      list.add(z);
   }

   static boolean favour(PlayerEntity p) {
      Long t = FAVOUR.get(p.getUuid());
      return t != null && t > p.getEntityWorld().getTime();
   }

   static boolean inOwnSun(PlayerEntity p) {
      return favour(p);
   }

   private static boolean inside(SekhmarArsenal.Sunlit z, Vec3d at) {
      return flatDistSqr(at, z.c) <= z.r * z.r && Math.abs(at.y - z.c.y) < 2.5;
   }

   private static void tickZones(ServerWorld level, long now) {
      List<SekhmarArsenal.Sunlit> list = ZONES.get(level);
      if (list != null && !list.isEmpty()) {
         for (SekhmarArsenal.Sunlit z : new ArrayList<>(list)) {
            if (now >= z.until) {
               list.remove(z);
            } else {
               int a = z.age++;
               if (a % 10 == 0) {
                  pulse(level, z, a / 10, now);
               }

               if (a % 20 == 10) {
                  motes(level, z);
               }
            }
         }
      }
   }

   private static void pulse(ServerWorld level, SekhmarArsenal.Sunlit z, int index, long now) {
      PlayerEntity owner = level.getPlayerByUuid(z.owner);

      for (LivingEntity e : zoneFoes(level, owner, z)) {
         e.setOnFireFor(2.0F);
         if (owner != null && index % 3 == 2) {
            seal(level, owner, e, 1, 2);
         }
      }

      if (owner != null && owner.isAlive() && inside(z, owner.getEntityPos())) {
         FAVOUR.put(owner.getUuid(), now + 15L);
         Long last = SUN_RES.get(owner.getUuid());
         if ((last == null || now - last >= 10L) && owner.getMainHandStack().getItem() instanceof MysticWeapon w && "crook_of_sekhmar".equals(w.gearId())) {
            SUN_RES.put(owner.getUuid(), now);
            GearState.of(owner).addResonance("crook_of_sekhmar", 1.0F);
         }
      }
   }

   private static List<LivingEntity> zoneFoes(ServerWorld level, @Nullable PlayerEntity owner, SekhmarArsenal.Sunlit z) {
      List<LivingEntity> raw = owner != null
         ? GearKit.enemies(level, owner, z.c, z.r + 3.0)
         : level.getEntitiesByClass(
            LivingEntity.class,
            new Box(z.c, z.c).expand(z.r + 1.0, 3.0, z.r + 1.0),
            ex -> ex.isAlive() && !(ex instanceof PlayerEntity) && !(ex instanceof ArmorStandEntity) && !(ex instanceof TameableEntity t && t.isTamed())
         );
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : raw) {
         if (inside(z, e.getEntityPos())) {
            out.add(e);
         }
      }

      return out;
   }

   private static void motes(ServerWorld level, SekhmarArsenal.Sunlit z) {
      Random rnd = level.getRandom();
      if (z.track) {
         Vfx.embers(level, randomIn(z, rnd), SUN.glow(), 2, 0.25);
      } else {
         int n = 2 + rnd.nextInt(3);

         for (int i = 0; i < n; i++) {
            Vfx.embers(level, randomIn(z, rnd), SUN.glow(), 2, 0.25);
         }

         Vfx.glint(level, randomIn(z, rnd).add(0.0, 0.4, 0.0), SUN.core(), 0.35);
      }
   }

   private static Vec3d randomIn(SekhmarArsenal.Sunlit z, Random rnd) {
      double a = rnd.nextDouble() * Math.PI * 2.0;
      double r = z.r * Math.sqrt(rnd.nextDouble());
      return z.c.add(Math.cos(a) * r, 0.1, Math.sin(a) * r);
   }

   private static int scarabCount(UUID owner) {
      int n = 0;

      for (SekhmarArsenal.Scarab s : SCARABS) {
         if (!s.dead && s.owner.equals(owner)) {
            n++;
         }
      }

      for (List<SekhmarArsenal.Latch> list : LATCHED.values()) {
         for (SekhmarArsenal.Latch l : list) {
            if (l.owner.equals(owner)) {
               n++;
            }
         }
      }

      return n;
   }

   private static int latchCount(LivingEntity e) {
      List<SekhmarArsenal.Latch> list = LATCHED.get(e);
      return list == null ? 0 : list.size();
   }

   static void latch(ServerWorld level, PlayerEntity p, LivingEntity e, int ticks, float power, boolean bursts) {
      if (e.isAlive()) {
         Vec3d c = chest(e);
         if (latchCount(e) < 5) {
            LATCHED.computeIfAbsent(e, k -> new ArrayList<>()).add(new SekhmarArsenal.Latch(p.getUuid(), level.getTime() + ticks, power, bursts));
            seal(level, p, e, 1, 3);
            Vfx.sparks(level, c, 15251008, 6, 0.3, 0.16);
            Vfx.ring(level, e.getEntityPos(), P.glow(), 0.5 + 0.5 * e.getWidth(), 8);
            GearKit.procSound(level, e, "scarab_flail");
         } else {
            for (LivingEntity o : within(level, p, c, 1.2)) {
               hit(level, p, o, 3.0F * power, true);
            }

            Vfx.impact(level, c, P, 0.4);
         }
      }
   }

   private static void tickScarabs(ServerWorld level) {
      if (!SCARABS.isEmpty()) {
         for (SekhmarArsenal.Scarab s : new ArrayList<>(SCARABS)) {
            if (s.level == level && !s.dead) {
               PlayerEntity owner = level.getPlayerByUuid(s.owner);
               s.age++;
               if (owner == null || !owner.isAlive()) {
                  s.dead = true;
                  Vfx.smoke(level, s.pos, P.smoke(), 3, 0.3, 0.8);
               } else if (s.age >= s.life) {
                  s.dead = true;
                  Vfx.smoke(level, s.pos, P.smoke(), 3, 0.3, 0.8);
                  if (s.bursts) {
                     for (LivingEntity e : within(level, owner, s.pos, 1.5)) {
                        hit(level, owner, e, 4.0F * s.power, true);
                     }

                     Vfx.impact(level, s.pos.add(0.0, 0.3, 0.0), P, 0.5);
                  }
               } else {
                  if (s.age % 10 == 1 || s.target != null && !s.target.isAlive()) {
                     s.target = scarabTarget(level, owner, s);
                  }

                  Vec3d at = s.pos.add(0.0, 0.15 + s.air, 0.0);
                  LivingEntity touched = contact(level, owner, s, at);
                  if (touched != null) {
                     s.dead = true;
                     latch(level, owner, touched, 60, s.power, s.bursts);
                  } else {
                     Vec3d hv = s.vel;
                     if (s.target != null) {
                        Vec3d to = flat(s.target.getEntityPos().subtract(s.pos));
                        if (to.lengthSquared() > 1.0E-4) {
                           hv = hv.lerp(to.normalize().multiply(0.5), 0.3);
                        }
                     }

                     Vec3d next = s.pos.add(hv.x, 0.0, hv.z);
                     Vec3d g = surface(level, next.x, s.pos.y, next.z, 1.3, 3.0);
                     if (g == null) {
                        hv = new Vec3d(-hv.x, 0.0, -hv.z);
                     } else {
                        s.pos = g;
                     }

                     s.vel = hv;
                     if (s.air > 0.0 || s.vy > 0.0) {
                        s.air = Math.max(0.0, s.air + s.vy);
                        s.vy = s.air > 0.0 ? s.vy - 0.08 : 0.0;
                     }

                     at = s.pos.add(0.0, 0.15 + s.air, 0.0);
                     if (s.age % 2 == 0) {
                        Vfx.trail(level, at, P, 0.25);
                        Vfx.glint(level, at.add(0.0, 0.15, 0.0), 15251008, 0.3);
                     }

                     if (s.age % 6 == 0) {
                        Vfx.smoke(level, s.pos, P.smoke(), 1, 0.1, 0.5);
                     }
                  }
               }
            }
         }

         SCARABS.removeIf(sx -> sx.dead);
      }
   }

   private static @Nullable LivingEntity scarabTarget(ServerWorld level, PlayerEntity owner, SekhmarArsenal.Scarab s) {
      LivingEntity best = null;
      double bestD = Double.MAX_VALUE;

      for (LivingEntity e : GearKit.enemies(level, owner, s.pos, 16.0)) {
         if (latchCount(e) < 5) {
            int hunting = 0;

            for (SekhmarArsenal.Scarab o : SCARABS) {
               if (o != s && !o.dead && o.target == e) {
                  hunting++;
               }
            }

            if (hunting < 3) {
               double d = e.squaredDistanceTo(s.pos);
               if (d < bestD) {
                  bestD = d;
                  best = e;
               }
            }
         }
      }

      return best;
   }

   private static @Nullable LivingEntity contact(ServerWorld level, PlayerEntity owner, SekhmarArsenal.Scarab s, Vec3d at) {
      if (s.target != null && touches(s.target, at)) {
         return s.target;
      }

      for (LivingEntity e : GearKit.enemies(level, owner, at, 3.0)) {
         if (touches(e, at)) {
            return e;
         }
      }

      return null;
   }

   private static boolean touches(LivingEntity e, Vec3d at) {
      if (!e.isAlive()) {
         return false;
      }

      double reach = e.getWidth() / 2.0F + 0.5;
      Box box = e.getBoundingBox();
      return flatDistSqr(e.getEntityPos(), at) <= reach * reach && at.y >= box.minY - 0.5 && at.y <= box.maxY;
   }

   private static void tickLatches(ServerWorld level, long now) {
      if (!LATCHED.isEmpty()) {
         for (LivingEntity host : new ArrayList<>(LATCHED.keySet())) {
            if (host.getEntityWorld() == level) {
               List<SekhmarArsenal.Latch> list = LATCHED.get(host);
               if (list != null) {
                  if (host.isAlive() && !host.isRemoved()) {
                     List<SekhmarArsenal.Latch> expired = new ArrayList<>();
                     list.removeIf(lx -> {
                        if (now >= lx.until) {
                           expired.add(lx);
                           return true;
                        } else {
                           return false;
                        }
                     });
                     if (list.isEmpty()) {
                        LATCHED.remove(host);
                     } else {
                        if (now % 4L == 0L) {
                           double w = host.getWidth() * 0.5 + 0.05;
                           double h = host.getHeight();

                           for (int i = 0; i < list.size(); i++) {
                              double a = Math.toRadians(i * 72 + now * 9L);
                              double f = 0.3 + 0.5 * ((i * 0.37 + now * 0.013) % 1.0);
                              Vfx.glint(level, host.getEntityPos().add(Math.cos(a) * w, h * f, Math.sin(a) * w), 15251008, 0.3);
                           }
                        }

                        if (now % 10L == 0L) {
                           devour(level, host, new ArrayList<>(list));
                        }
                     }

                     for (SekhmarArsenal.Latch l : expired) {
                        dropOff(level, host, l);
                     }
                  } else {
                     LATCHED.remove(host);
                  }
               }
            }
         }
      }
   }

   private static void devour(ServerWorld level, LivingEntity host, List<SekhmarArsenal.Latch> list) {
      Map<UUID, Float> bites = new LinkedHashMap<>();

      for (SekhmarArsenal.Latch l : list) {
         bites.merge(l.owner, 1.5F * l.power, Float::sum);
      }

      for (Entry<UUID, Float> en : bites.entrySet()) {
         PlayerEntity owner = level.getPlayerByUuid(en.getKey());
         if (owner != null && host.isAlive()) {
            hit(level, owner, host, en.getValue(), true);
            GearState.of(owner).addResonance("scarab_flail", 1.0F);
         }
      }

      Vfx.sparks(level, chest(host), P.core(), 2 + list.size(), 0.25, 0.14);
   }

   private static void dropOff(ServerWorld level, LivingEntity host, SekhmarArsenal.Latch l) {
      Vfx.smoke(level, host.getEntityPos(), P.smoke(), 3, 0.3, 0.8);
      if (l.bursts) {
         PlayerEntity owner = level.getPlayerByUuid(l.owner);
         if (owner != null) {
            for (LivingEntity e : within(level, owner, chest(host), 1.5)) {
               hit(level, owner, e, 4.0F * l.power, true);
            }
         }

         Vfx.impact(level, chest(host), P, 0.5);
      }
   }

   private static void run(ServerWorld level, PlayerEntity p, SekhmarArsenal.Task t) {
      int n = 0;

      for (SekhmarArsenal.Job j : JOBS) {
         if (!j.done && j.owner == p) {
            n++;
         }
      }

      if (n < 40) {
         JOBS.add(new SekhmarArsenal.Job(level, p, t));
      }
   }

   private static void later(ServerWorld level, PlayerEntity p, int delay, Runnable r) {
      if (delay <= 0) {
         r.run();
      } else {
         run(level, p, (l, age) -> {
            if (age < delay - 1) {
               return false;
            }

            r.run();
            return true;
         });
      }
   }

   private static void tickJobs(ServerWorld level) {
      if (!JOBS.isEmpty()) {
         for (SekhmarArsenal.Job j : new ArrayList<>(JOBS)) {
            if (!j.done && j.level == level) {
               if (j.owner.isAlive() && !j.owner.isRemoved() && j.owner.getEntityWorld() == level) {
                  try {
                     if (j.task.tick(level, j.age++)) {
                        j.done = true;
                     }
                  } catch (RuntimeException ex) {
                     j.done = true;
                     MysticalTerrain.LOGGER.error("Sekhmar gear task failed", ex);
                  }
               } else {
                  j.done = true;
               }
            }
         }

         JOBS.removeIf(jx -> jx.done);
      }
   }

   private static void tick(ServerWorld level) {
      long now = level.getTime();
      tickJobs(level);
      guard("seals", () -> tickSeals(level, now));
      guard("ka", () -> tickKa(level));
      guard("scarabs", () -> tickScarabs(level));
      guard("latches", () -> tickLatches(level, now));
      guard("zones", () -> tickZones(level, now));
      if (now % 2L == 0L) {
         guard("charging", () -> tickCharging(level, now));
      }

      if (now % 200L == 0L) {
         FAVOUR.values().removeIf(t -> t < now);
         WARD.values().removeIf(t -> t < now);
         SUN_RES.values().removeIf(t -> now - t > 200L);
         HEALED.values().removeIf(h -> now / 20L - h[0] > 10L);
      }
   }

   private static void guard(String what, Runnable r) {
      try {
         r.run();
      } catch (RuntimeException ex) {
         MysticalTerrain.LOGGER.error("Sekhmar gear " + what + " failed", ex);
      }
   }

   private static void tickCharging(ServerWorld level, long now) {
      for (ServerPlayerEntity p : level.getPlayers()) {
         if (p.isAlive() && p.isUsingItem() && p.getActiveItem().getItem() instanceof MysticWeapon w) {
            int held = p.getItemUseTime();
            switch (w.gearId()) {
               case "pharaohs_khopesh":
                  if (now % 4L == 0L) {
                     Vec3d tip = GearKit.tip(p);
                     Vfx.smoke(level, tip, P.smoke(), 1, 0.15, 0.6);
                     Vfx.embers(level, tip, SOUL.glow(), 1, 0.25);
                  }
                  break;
               case "crook_of_sekhmar":
                  int nx = Math.min(4, held / 8);
                  Vec3d sun = sunPos(p);
                  Vfx.flare(level, sun, SUN.core(), 0.5 + 0.25 * nx, 3);
                  if (now % 4L == 0L) {
                     Vfx.embers(level, sun, SUN.glow(), 2, 0.4);
                  }

                  if (now % 10L == 0L) {
                     Vfx.ring(level, ground(level, aim(level, p, 28.0)), SUN.glow(), 2.5 + 0.9 * Math.max(1, nx), 10);
                  }
                  break;
               case "scarab_flail":
                  int n = Math.min(3, held / 12);
                  Vec3d top = headTop(p);
                  double a = held * 16 * (float) (Math.PI / 180.0);
                  Vec3d off = new Vec3d(Math.cos(a), 0.0, Math.sin(a));
                  Vfx.sparks(level, top.add(off.multiply(1.4)), 15251008, 1, 0.1, 0.15);
                  Vfx.flare(level, top, SUN.core(), 0.4 + 0.25 * n, 3);
                  if (now % 6L == 0L) {
                     Vfx.streaks(level, top.add(off.multiply(1.4)), new Vec3d(-off.z, 0.0, off.x), P, 2);
                  }
            }
         }
      }
   }

   private static void onDeath(LivingEntity dead, DamageSource source) {
      if (dead.getEntityWorld() instanceof ServerWorld level) {
         guard("death", () -> {
            Vec3d c = chest(dead);
            long now = level.getTime();
            SekhmarArsenal.Seal s = SEALS.remove(dead);
            BOUND.remove(dead);
            if (s != null && s.stacks > 0 && now < s.expires && s.owner != null) {
               PlayerEntity owner = level.getPlayerByUuid(s.owner);
               if (owner != null && owner.isAlive() && owner.squaredDistanceTo(dead) < 1024.0) {
                  int n = s.stacks;
                  ka(level, owner, c, 1.0F + 0.5F * n, 5.0F);
                  LivingEntity next = null;
                  double best = Double.MAX_VALUE;

                  for (LivingEntity e : GearKit.enemies(level, owner, dead.getEntityPos(), 8.0)) {
                     double d = e.squaredDistanceTo(dead);
                     if (e != dead && seals(level, e) < 3 && d < best) {
                        best = d;
                        next = e;
                     }
                  }

                  if (next != null) {
                     Vfx.chainLightning(level, List.of(c, chest(next)), SOUL);
                     seal(level, owner, next, n, 3);
                  }
               }
            }

            List<SekhmarArsenal.Latch> latches = LATCHED.remove(dead);
            if (latches != null) {
               for (SekhmarArsenal.Latch l : latches) {
                  PlayerEntity owner = level.getPlayerByUuid(l.owner);
                  LivingEntity next = null;
                  if (owner != null) {
                     double best = Double.MAX_VALUE;

                     for (LivingEntity e : within(level, owner, dead.getEntityPos(), 6.0)) {
                        double d = e.squaredDistanceTo(dead);
                        if (e != dead && latchCount(e) < 5 && d < best) {
                           best = d;
                           next = e;
                        }
                     }
                  }

                  if (next != null) {
                     LATCHED.computeIfAbsent(next, k -> new ArrayList<>()).add(l);
                     Vfx.dashTrail(level, c, chest(next), P);
                  } else {
                     Vfx.smoke(level, dead.getEntityPos(), P.smoke(), 3, 0.3, 0.8);
                  }
               }
            }
         });
      }
   }

   private static void reset() {
      JOBS.clear();
      SEALS.clear();
      BOUND.clear();
      ZONES.clear();
      KAS.clear();
      SCARABS.clear();
      LATCHED.clear();
      CURSED.clear();
      FAVOUR.clear();
      SUN_RES.clear();
      WARD.clear();
      HEALED.clear();
      BROOD.clear();
   }

   private static void hit(ServerWorld level, PlayerEntity p, LivingEntity e, float dmg, boolean magic) {
      if (e.isAlive() && !(dmg <= 0.0F)) {
         float amp = (1.0F + 0.08F * seals(level, e)) * (favour(p) ? 1.15F : 1.0F);
         e.timeUntilRegen = 0;
         if (magic) {
            GearKit.magic(level, p, e, dmg * amp);
         } else {
            GearKit.strike(level, p, e, dmg * amp);
         }
      }
   }

   private static void shove(LivingEntity e, Vec3d from, double strength, double lift) {
      if (!boss(e)) {
         GearKit.knockAway(e, from, strength, lift);
      }
   }

   private static boolean boss(LivingEntity e) {
      return e instanceof MysticalBoss b ? b.isBoss() : e instanceof EnderDragonEntity || e instanceof WitherEntity;
   }

   private static boolean big(LivingEntity e) {
      return boss(e) || e.getWidth() * e.getHeight() > 4.0;
   }

   private static boolean ours(String id) {
      return "pharaohs_khopesh".equals(id) || "crook_of_sekhmar".equals(id) || "scarab_flail".equals(id);
   }

   private static Vec3d chest(Entity e) {
      return e.getBoundingBox().getCenter();
   }

   private static Vec3d top(LivingEntity e) {
      return e.getEyePos().add(0.0, 0.3, 0.0);
   }

   private static Vec3d flat(Vec3d v) {
      return new Vec3d(v.x, 0.0, v.z);
   }

   private static double flatDistSqr(Vec3d a, Vec3d b) {
      double dx = a.x - b.x;
      double dz = a.z - b.z;
      return dx * dx + dz * dz;
   }

   private static Vec3d right(Vec3d f) {
      return new Vec3d(-f.z, 0.0, f.x);
   }

   private static Vec3d dirOf(double yaw) {
      double r = Math.toRadians(yaw);
      return new Vec3d(-Math.sin(r), 0.0, Math.cos(r));
   }

   private static float yawToward(Vec3d from, Vec3d to) {
      return (float)(MathHelper.atan2(to.z - from.z, to.x - from.x) * 180.0F / (float)Math.PI) - 90.0F;
   }

   private static Vec3d bezier(Vec3d a, Vec3d c, Vec3d b, double t) {
      double u = 1.0 - t;
      return a.multiply(u * u).add(c.multiply(2.0 * u * t)).add(b.multiply(t * t));
   }

   private static Comparator<LivingEntity> byDistance(Entity from) {
      return Comparator.comparingDouble(e -> e.squaredDistanceTo(from));
   }

   private static boolean inFront(PlayerEntity p, LivingEntity e, Vec3d look, double halfDeg) {
      Vec3d v = flat(chest(e).subtract(p.getEntityPos()));
      return v.lengthSquared() < 0.64 || v.normalize().dotProduct(look) >= Math.cos(Math.toRadians(halfDeg));
   }

   private static double segDistFlat(Vec3d pt, Vec3d a, Vec3d b) {
      double abx = b.x - a.x;
      double abz = b.z - a.z;
      double apx = pt.x - a.x;
      double apz = pt.z - a.z;
      double len = abx * abx + abz * abz;
      double t = len < 1.0E-6 ? 0.0 : MathHelper.clamp((apx * abx + apz * abz) / len, 0.0, 1.0);
      double dx = apx - abx * t;
      double dz = apz - abz * t;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static BlockHitResult clip(ServerWorld level, Vec3d from, Vec3d to) {
      return level.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, ShapeContext.absent()));
   }

   private static Vec3d aim(ServerWorld level, PlayerEntity p, double range) {
      return GearKit.lookPoint(level, p, range).subtract(p.getRotationVector().multiply(0.25));
   }

   private static @Nullable Vec3d surface(ServerWorld level, double x, double y, double z, double up, double down) {
      Mutable m = new Mutable();
      int top = MathHelper.floor(y + up);
      int bottom = MathHelper.floor(y - down);
      m.set(MathHelper.floor(x), top, MathHelper.floor(z));
      if (!level.getBlockState(m).getCollisionShape(level, m).isEmpty()) {
         return null;
      }

      for (int yy = top - 1; yy >= bottom; yy--) {
         m.setY(yy);
         VoxelShape s = level.getBlockState(m).getCollisionShape(level, m);
         if (!s.isEmpty()) {
            return new Vec3d(x, yy + s.getMax(Axis.Y), z);
         }
      }

      return null;
   }

   private static Vec3d ground(ServerWorld level, Vec3d at) {
      Vec3d s = surface(level, at.x, at.y, at.z, 1.5, 12.0);
      if (s != null) {
         return s;
      }

      Vec3d g = BossFx.ground(level, at.x, at.y + 1.5, at.z);
      return g != null ? g : at;
   }

   private static int ceiling(ServerWorld level, Vec3d at, int max) {
      Mutable m = new Mutable();

      int h;
      for (h = 0; h < max; h++) {
         m.set(MathHelper.floor(at.x), MathHelper.floor(at.y + 0.05) + h, MathHelper.floor(at.z));
         if (!level.getBlockState(m).getCollisionShape(level, m).isEmpty()) {
            break;
         }
      }

      return Math.max(2, h);
   }

   private static List<LivingEntity> within(ServerWorld level, PlayerEntity p, Vec3d at, double r) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, at, r + 4.0)) {
         if (e.getBoundingBox().squaredMagnitude(at) <= r * r) {
            out.add(e);
         }
      }

      return out;
   }

   private static List<LivingEntity> cylinder(ServerWorld level, PlayerEntity p, Vec3d c, double r, double dy) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, c, r + dy + 2.0)) {
         double reach = r + e.getWidth() / 2.0F;
         if (flatDistSqr(e.getEntityPos(), c) <= reach * reach && Math.abs(e.getY() - c.y) < dy) {
            out.add(e);
         }
      }

      return out;
   }

   private static List<LivingEntity> column(ServerWorld level, PlayerEntity p, Vec3d base, double r, double h) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, base.add(0.0, h / 2.0, 0.0), Math.max(r, h / 2.0) + 2.0)) {
         double reach = r + e.getWidth() / 2.0F;
         if (flatDistSqr(e.getEntityPos(), base) <= reach * reach && e.getY() >= base.y - 1.0 && e.getY() <= base.y + h) {
            out.add(e);
         }
      }

      return out;
   }

   private static List<LivingEntity> cone(ServerWorld level, PlayerEntity p, double range, double halfDeg) {
      List<LivingEntity> out = new ArrayList<>();
      Vec3d o = p.getEntityPos();
      Vec3d look = GearKit.flatLook(p);
      double cos = Math.cos(Math.toRadians(halfDeg));

      for (LivingEntity e : GearKit.enemies(level, p, o.add(0.0, 1.0, 0.0), range + 3.0)) {
         Vec3d v = flat(e.getEntityPos().subtract(o));
         if (!(v.length() - e.getWidth() / 2.0F > range)
            && !(Math.abs(e.getY() - o.y) > 3.0)
            && (v.lengthSquared() < 0.64 || v.normalize().dotProduct(look) >= cos)) {
            out.add(e);
         }
      }

      return out;
   }

   private static void sfx(ServerWorld level, Vec3d at, String key, float volume, float pitch) {
      SoundEvent e = BossSounds.event(key);
      if (e != null) {
         level.playSound(null, at.x, at.y, at.z, e, SoundCategory.PLAYERS, volume, pitch * (0.95F + level.getRandom().nextFloat() * 0.1F));
      }
   }

   private static final class Job {
      final ServerWorld level;
      final PlayerEntity owner;
      final SekhmarArsenal.Task task;
      int age;
      boolean done;

      Job(ServerWorld level, PlayerEntity owner, SekhmarArsenal.Task task) {
         this.level = level;
         this.owner = owner;
         this.task = task;
      }
   }

   private static final class Ka {
      final ServerWorld level;
      final UUID owner;
      final float heal;
      final float res;
      Vec3d pos;
      int age;
      boolean done;

      Ka(ServerWorld level, UUID owner, Vec3d pos, float heal, float res) {
         this.level = level;
         this.owner = owner;
         this.pos = pos;
         this.heal = heal;
         this.res = res;
      }
   }

   private static final class Latch {
      final UUID owner;
      final long until;
      final float power;
      final boolean bursts;

      Latch(UUID owner, long until, float power, boolean bursts) {
         this.owner = owner;
         this.until = until;
         this.power = power;
         this.bursts = bursts;
      }
   }

   private static final class Scarab {
      final ServerWorld level;
      final UUID owner;
      final int life;
      final float power;
      final boolean bursts;
      Vec3d pos;
      Vec3d vel;
      double air;
      double vy;
      int age;
      boolean dead;
      @Nullable LivingEntity target;

      Scarab(ServerWorld level, UUID owner, Vec3d pos, Vec3d vel, double air, double vy, int life, float power, boolean bursts) {
         this.level = level;
         this.owner = owner;
         this.pos = pos;
         this.vel = vel;
         this.air = air;
         this.vy = vy;
         this.life = life;
         this.power = power;
         this.bursts = bursts;
      }
   }

   private static final class Seal {
      int stacks;
      long expires;
      long entombedUntil;
      long immuneUntil;
      @Nullable UUID owner;
   }

   private static final class Sunlit {
      final UUID owner;
      final Vec3d c;
      final double r;
      final long until;
      final boolean track;
      int age;

      Sunlit(UUID owner, Vec3d c, double r, long until, boolean track) {
         this.owner = owner;
         this.c = c;
         this.r = r;
         this.until = until;
         this.track = track;
      }
   }

   @FunctionalInterface
   private interface Task {
      boolean tick(ServerWorld var1, int var2);
   }
}

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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Disconnect;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
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

public final class ArchonArsenal extends Arsenal {
   private static final Vfx.Palette P = Vfx.Palette.ARCANE;
   private static final int CORE = P.core();
   private static final int GLOW = P.glow();
   private static final Vec3d UP = new Vec3d(0.0, 1.0, 0.0);
   private static final Vec3d DOWN = new Vec3d(0.0, -1.0, 0.0);
   private static final String SET = "archon";
   private static final String DEFLECT = "archon.deflect";
   private static final String SEED = "archon.seed";
   private static final int MAX_STONES = 5;
   private static final int MAX_BLADES = 3;
   private static final int TASK_CAP = 24;
   private static final int SHOT_CAP = 40;
   private static final int[][] STAR = new int[][]{{0, 2}, {2, 4}, {4, 1}, {1, 3}, {3, 0}};
   private static final String[] EDGE = new String[]{"weapon.runeblade.edge", "weapon.runeblade.use"};
   private static final String[] VERDICT = new String[]{"weapon.runeblade.verdict", "weapon.runeblade.release", "weapon.runeblade.use"};
   private static final String[] SHATTER = new String[]{"weapon.archon_focus.shatter", "weapon.archon_focus.alt"};
   private static final String[] WARD = new String[]{"weapon.archon_focus.deflect", "weapon.archon_focus.alt"};
   private static final String[] CRUSH = new String[]{"weapon.sigil_hammer.crush", "weapon.sigil_hammer.use"};
   private static final String[] GLYPH = new String[]{"weapon.sigil_hammer.glyph", "weapon.sigil_hammer.proc"};
   private static final String[] FIELD = new String[]{"weapon.sigil_hammer.release", "weapon.sigil_hammer.use"};
   private static final String[] LATTICE = new String[]{"weapon.sigil_hammer.lattice", "armor.runeplate_armor.ability"};
   private static final String[] UNMAKING = new String[]{"weapon.sigil_hammer.unmaking", "weapon.sigil_hammer.use"};
   private static final String[] LAUNCH = new String[]{"bolt.rune.launch"};
   private static final String[] IMPACT = new String[]{"bolt.rune.impact"};
   private static final List<ArchonArsenal.Job> TASKS = new ArrayList<>();
   private static final List<ArchonArsenal.Shot> SHOTS = new ArrayList<>();
   private static final List<ArchonArsenal.Shot> NEW_SHOTS = new ArrayList<>();
   private static final Map<UUID, ArchonArsenal.Runic> RUNIC = new HashMap<>();
   private static boolean hooked;
   private static int seq;

   public ArchonArsenal() {
      super("archon", "runeplate_armor", Vfx.Palette.ARCANE);
      hook();
   }

   @Override
   protected void weapons() {
      this.weapon("runeblade", 9.0F, -2.4F)
         .primary(Active.of(120, ArchonArsenal::runebladeCleave).empowered(40, 1.3F))
         .alternate(Active.of(240, ArchonArsenal::falseEdge).empowered(50, 1.3F))
         .charge(
            Charge.of(3, 12)
               .pose(UseAction.TRIDENT)
               .cast(Active.of(360, ArchonArsenal::sovereignsVerdict).empowered(60, 1.25F))
               .onLevel(ArchonArsenal::verdictLevel)
         )
         .passive(ArchonArsenal::glyphEdge)
         .resonancePerHit(10.0F);
      this.weapon("archon_focus", 4.0F, -2.4F)
         .primary(Active.of(80, ArchonArsenal::runeMissiles).empowered(30, 1.0F))
         .alternate(Active.of(280, ArchonArsenal::runePrison).empowered(50, 1.3F))
         .charge(Charge.of(4, 10).pose(UseAction.BOW).cast(Active.of(320, ArchonArsenal::heartBeam).empowered(60, 1.25F)).onLevel(ArchonArsenal::beamLevel))
         .passive(ArchonArsenal::heartspark)
         .resonancePerHit(6.0F);
      this.weapon("sigil_hammer", 11.0F, -3.1F)
         .primary(Active.of(160, ArchonArsenal::sigilSlam).empowered(60, 1.3F))
         .alternate(Active.of(280, ArchonArsenal::gravitySigil).empowered(50, 1.3F))
         .charge(Charge.of(3, 12).pose(UseAction.TRIDENT).perLevel(0.35F).onLevel(ArchonArsenal::crushLevel))
         .passive(ArchonArsenal::leyDischarge)
         .resonancePerHit(12.0F);
   }

   @Override
   protected void armor(ArmorKit set) {
      set.twoPiece(ArchonArsenal::wardShimmer);
      set.fourPiece(ArchonArsenal::leyConduit);
      set.allowDamage(ArchonArsenal::spellward);
      set.onHurt(ArchonArsenal::runeplateHurt);
      set.active(600, ArchonArsenal::pentagramLattice);
   }

   static boolean runebladeCleave(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float pw = u.power();
      long now = level.getTime();
      ArchonArsenal.Runic r = runic(p, level);
      r.lastCombat = now;
      Vec3d origin = p.getEntityPos().add(0.0, 1.25, 0.0);
      float yaw = p.getYaw();
      float pitch = MathHelper.clamp(p.getPitch(), -30.0F, 30.0F) * 0.5F;
      Vec3d f = GearKit.flatLook(p);
      Vfx.slashArc(level, origin, yaw, pitch, 0.0F, 5.0, P, 210.0F);
      Vfx.slashArc(level, origin.add(0.0, -0.15, 0.0), yaw, pitch, 10.0F, 3.6, P, 180.0F);
      Vfx.glint(level, origin.add(f.rotateY((float) (Math.PI * 7.0 / 12.0)).multiply(5.0)), CORE, 1.1);
      Vfx.streaks(level, origin.add(f.multiply(2.5)), f.rotateY((float) (Math.PI / 2)), P, 8);
      GearKit.kick(level, p, 0.2);

      for (LivingEntity e : sweep(level, p, 5.0, 105.0)) {
         hit(level, p, e, 13.0F * pw, true);
         shove(e, p.getEntityPos(), 0.5, 0.2);
         u.resonate(2.0F);
         Vec3d c = center(e);
         GearKit.hitSpark(level, e, P);
         Vfx.shards(level, c, CORE, 5, 0.35);
         if (r.blades < 3) {
            Vfx.dashTrail(level, c, bladeSlot(p, r.blades, now), P);
         }

         addBlade(r, now);
      }

      if (u.empowered()) {
         later(level, p, 6, () -> {
            Vec3d o = p.getEntityPos().add(0.0, 1.25, 0.0);
            float y2 = p.getYaw();
            float p2 = MathHelper.clamp(p.getPitch(), -30.0F, 30.0F) * 0.5F;
            Vfx.slashArc(level, o, y2, p2, -35.0F, 5.5, P, -200.0F);

            for (LivingEntity ex : sweep(level, p, 5.5, 100.0)) {
               hit(level, p, ex, 10.0F * pw, true);
               Vfx.impact(level, center(ex), P, 0.6);
            }

            ArchonArsenal.Runic rr = runic(p, level);
            rr.blades = 3;
            rr.bladeUntil = level.getTime() + 200L;
            Vfx.wisps(level, o, P, 3, 1.2, 12);
            GearKit.kick(level, p, 0.25);
            sfx(level, o, 1.0F, 1.2F, EDGE);
         });
      }

      return true;
   }

   static boolean falseEdge(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float pw = u.power();
      boolean emp = u.empowered();
      Vec3d start = p.getEntityPos();
      Vec3d dest = leyStep(level, p);
      if (dest == null) {
         dest = slide(level, p);
      }

      if (dest == null) {
         return false;
      }

      Vec3d flat = new Vec3d(dest.x - start.x, 0.0, dest.z - start.z);
      double len = flat.length();
      if (len < 1.5) {
         return false;
      }

      p.requestTeleport(dest.x, dest.y, dest.z);
      p.onLanding();
      p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, emp ? 30 : 10, emp ? 1 : 0, true, false, true));
      Vec3d a = start.add(0.0, 1.0, 0.0);
      Vec3d b = dest.add(0.0, 1.0, 0.0);
      Vfx.shieldBubble(level, a, 0.8, P, 10);
      Vfx.flare(level, a, GLOW, 2.2, 12);
      Vfx.runeCircle(level, ground(level, start), 1.2, P, 12, 0.1F, Vfx.Rune.ARCANE);
      Vfx.dashTrail(level, a, b, P);
      Vfx.telegraphLine(level, ground(level, start), flat.multiply(1.0 / len), len, 1.6, 10, P);
      runic(p, level).lastCombat = level.getTime();
      later(level, p, 10, () -> edgeCut(level, p, u, a, b, 16.0F * pw, pw, true));
      if (emp) {
         later(level, p, 16, () -> edgeCut(level, p, u, b, a, 10.0F * pw, pw, false));
      }

      return true;
   }

   private static void edgeCut(ServerWorld level, PlayerEntity p, GearUse u, Vec3d a, Vec3d b, float dmg, float pw, boolean first) {
      long now = level.getTime();
      List<LivingEntity> hits = new ArrayList<>(onSegment(level, p, a, b, 1.4));
      hits.sort(Comparator.comparingDouble(e -> e.getEntityPos().squaredDistanceTo(a)));
      ArchonArsenal.Runic r = RUNIC.get(p.getUuid());
      int blades = r != null ? r.blades : 0;

      for (int i = 0; i < hits.size(); i++) {
         LivingEntity e = hits.get(i);
         Vec3d c = center(e);
         hit(level, p, e, dmg, false);
         if (first) {
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 1), p);
            u.resonate(3.0F);
            if (i < 3 && blades > 0) {
               hit(level, p, e, 4 * blades * pw, false);

               for (int k = 0; k < blades; k++) {
                  Vfx.dashTrail(level, bladeSlot(p, k, now), c, P);
               }
            }
         }

         Vfx.impact(level, c, P, 0.7);
         Vfx.glint(level, c, CORE, 0.9);
      }

      Vec3d mid = a.add(b).multiply(0.5);
      Vec3d dir = b.subtract(a).normalize();
      Vfx.beam(level, a, b, P, first ? 0.45 : 0.35, first ? 6 : 5);
      Vfx.lightning(level, a.add(0.0, 0.2, 0.0), b.add(0.0, 0.2, 0.0), P, 2);
      Vfx.streaks(level, a, dir, P, first ? 14 : 8);
      sfx(level, mid, 1.2F, first ? 1.0F : 1.25F, EDGE);
   }

   private static @Nullable Vec3d leyStep(ServerWorld level, PlayerEntity p) {
      Vec3d f = GearKit.flatLook(p);
      Vec3d eye = GearKit.eye(p);
      ArchonArsenal.Stone best = null;
      double bestCos = Math.cos(Math.toRadians(25.0));

      for (ArchonArsenal.Stone s : stones(p)) {
         Vec3d to = new Vec3d(s.ground.x - p.getX(), 0.0, s.ground.z - p.getZ());
         double d = to.length();
         if (!(d < 3.0) && !(d > 16.0)) {
            double cos = to.multiply(1.0 / d).dotProduct(f);
            if (cos >= bestCos && los(level, p, eye, s.pos)) {
               best = s;
               bestCos = cos;
            }
         }
      }

      if (best == null) {
         return null;
      } else {
         Vec3d back = new Vec3d(p.getX() - best.ground.x, 0.0, p.getZ() - best.ground.z).normalize();
         Vec3d land = ground(level, best.ground.add(back));
         if (free(level, p, land)) {
            return land;
         } else {
            return free(level, p, best.ground) ? best.ground : null;
         }
      }
   }

   private static @Nullable Vec3d slide(ServerWorld level, PlayerEntity p) {
      Vec3d step = GearKit.flatLook(p).multiply(0.5);
      boolean grounded = p.isOnGround();
      Vec3d cur = p.getEntityPos();
      Vec3d last = null;

      for (int i = 0; i < 16; i++) {
         Vec3d n = cur.add(step);
         Vec3d cand = free(level, p, n) ? n : (free(level, p, n.add(0.0, 1.0, 0.0)) ? n.add(0.0, 1.0, 0.0) : null);
         if (cand == null || !los(level, p, cur.add(0.0, 1.2, 0.0), cand.add(0.0, 1.2, 0.0))) {
            break;
         }

         Vec3d g = floor(level, cand, 0.4, 2);
         if (g != null && cand.y - g.y <= 2.05 && free(level, p, g)) {
            cand = g;
         } else if (grounded) {
            break;
         }

         cur = cand;
         last = cand;
      }

      return last;
   }

   static void verdictLevel(ServerWorld level, PlayerEntity p, WeaponKit w, int lvl, boolean max) {
      ArchonArsenal.Runic r = RUNIC.get(p.getUuid());
      int n = Math.min(6, lvl + (r != null ? r.blades : 0));
      Vec3d slot = haloSlot(p, n, Math.min(lvl - 1, n - 1));
      Vec3d rt = raisedTip(p);
      Vfx.dashTrail(level, rt, slot, P);
      Vfx.glint(level, slot, CORE, 0.9);
      Vfx.ring(level, p.getEntityPos(), GLOW, 1.0 + 0.4 * lvl, 10);
      Vfx.flare(level, rt, max ? CORE : GLOW, 0.9 + 0.35 * lvl, 6);
      if (max) {
         Vfx.runeCircle(level, ground(level, p.getEntityPos()), 2.4, P, 36, 0.07F, Vfx.Rune.ARCANE);
         Vfx.chargeUp(level, GearKit.eye(p).add(0.0, 0.8, 0.0), P, 14);
      }
   }

   static boolean sovereignsVerdict(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      int c = Math.max(1, u.charge());
      boolean emp = u.empowered();
      ArchonArsenal.Runic r = runic(p, level);
      int stored = r.blades;
      r.blades = 0;
      r.lastCombat = level.getTime();
      int n = Math.min(8, c + stored + (emp ? 2 : 0));
      LivingEntity target = GearKit.lookTarget(level, p, 40.0);
      Vec3d aim = target != null ? center(target) : GearKit.lookPoint(level, p, 40.0).subtract(p.getRotationVector().multiply(0.2));
      Vfx.flare(level, raisedTip(p), CORE, 2.0, 6);
      GearKit.kick(level, p, 0.15);
      run(level, p, new ArchonArsenal.Verdict(p, u, u.power(), n, c >= 3 || emp, target, aim));
      return true;
   }

   static void glyphEdge(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof PlayerEntity p) {
         long now = level.getTime();
         ArchonArsenal.Runic r = runic(p, level);
         r.lastCombat = now;
         Vec3d c = center(t);
         if (r.blades > 0 && t.isAlive()) {
            hit(level, p, t, 1.5F * r.blades, false);
            int k = Math.floorMod(r.bladeTurn++, r.blades);
            Vfx.dashTrail(level, bladeSlot(p, k, now), c, P);
            Vfx.glint(level, c, CORE, 0.6);
         }

         if (++r.edgeHits % 2 == 0) {
            if (r.blades < 3) {
               Vec3d slot = bladeSlot(p, r.blades, now);
               Vfx.flare(level, slot, GLOW, 1.2, 6);
               Vfx.sparks(level, slot, CORE, 4, 0.2, 0.14);
               GearKit.procSound(level, t, "runeblade");
            }

            addBlade(r, now);
         }
      }
   }

   static boolean runeMissiles(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float pw = u.power();
      LivingEntity target = GearKit.lookTarget(level, p, 32.0);
      Vec3d aim = target == null ? GearKit.lookPoint(level, p, 32.0) : null;
      Vec3d tip = GearKit.tip(p);
      volley(level, p, u, target, aim, pw, 1);
      relay(level, p, u, target, aim, pw);
      GearKit.muzzle(level, p, P, 1.1);
      Vfx.gather(level, tip, P, 1.2, 8);
      Vfx.ring(level, p.getEntityPos(), GLOW, 1.4, 10);
      runic(p, level).lastCombat = level.getTime();
      if (u.empowered()) {
         later(level, p, 4, () -> {
            volley(level, p, u, target, aim, pw, -1);
            relay(level, p, u, target, aim, pw);
            GearKit.muzzle(level, p, P, 1.0);
         });
      }

      return true;
   }

   private static void volley(ServerWorld level, PlayerEntity p, GearUse u, @Nullable LivingEntity target, @Nullable Vec3d aim, float pw, int side) {
      Vec3d tip = GearKit.tip(p);
      Vec3d look = p.getRotationVector();
      LivingEntity live = target != null && target.isAlive() ? target : null;

      for (int k = -1; k <= 1; k++) {
         Vec3d dir = look.rotateY(k * 28 * (float) (Math.PI / 180.0)).add(0.0, 0.25 * side, 0.0).normalize();
         ArchonArsenal.Shot s = shot(level, p, tip, dir.multiply(0.55), 70, 0.5, 0);
         if (s == null) {
            return;
         }

         s.maxSpeed = 1.1;
         s.accel = 0.06;
         s.homingFrom = 4;
         s.homing = 0.18;
         s.target = live;
         s.aimAt = live != null ? center(live) : aim;
         s.onHit = (sh, e, at) -> missileHit(level, p, u, e, at, 7.0F * pw);
         Vfx.glint(level, tip.add(dir.multiply(0.4)), CORE, 0.6);
      }
   }

   private static void relay(ServerWorld level, PlayerEntity p, GearUse u, @Nullable LivingEntity target, @Nullable Vec3d aim, float pw) {
      LivingEntity live = target != null && target.isAlive() ? target : null;
      Vec3d goal = live != null ? center(live) : aim;
      if (goal != null) {
         for (ArchonArsenal.Stone st : stonesNear(p, goal, 20.0)) {
            Vec3d d = goal.subtract(st.pos);
            Vec3d dir = d.lengthSquared() > 1.0E-4 ? d.normalize() : UP;
            ArchonArsenal.Shot s = shot(level, p, st.pos.add(dir.multiply(0.4)), dir.multiply(0.9), 60, 0.5, 0);
            if (s == null) {
               return;
            }

            s.homing = 0.2;
            s.target = live;
            s.aimAt = goal;
            s.onHit = (sh, e, at) -> missileHit(level, p, u, e, at, 5.0F * pw);
            Vfx.flare(level, st.pos, GLOW, 1.4, 6);
            Vfx.beam(level, st.pos, st.pos.add(dir.multiply(0.9)), P, 0.2);
            sfx(level, st.pos, 0.6F, 1.2F, LAUNCH);
         }
      }
   }

   private static void missileHit(ServerWorld level, PlayerEntity p, GearUse u, @Nullable LivingEntity e, Vec3d at, float dmg) {
      if (e != null) {
         hit(level, p, e, dmg, false);
         u.resonate(2.0F);
      }

      Vfx.impact(level, at, P, 0.45);
      Vfx.ring(level, at, GLOW, 0.8, 8);
      sfx(level, at, 0.7F, 1.0F, IMPACT);
   }

   static boolean runePrison(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float pw = u.power();
      LivingEntity t = GearKit.lookTarget(level, p, 24.0);
      if (t == null) {
         return false;
      }

      List<LivingEntity> caged = new ArrayList<>();
      caged.add(t);
      if (u.empowered()) {
         List<LivingEntity> near = new ArrayList<>(GearKit.enemies(level, p, t.getEntityPos(), 6.0));
         near.remove(t);
         near.sort(Comparator.comparingDouble(ex -> ex.squaredDistanceTo(t)));

         for (int i = 0; i < Math.min(2, near.size()); i++) {
            caged.add(near.get(i));
         }
      }

      List<Vec3d> chain = new ArrayList<>();
      chain.add(GearKit.tip(p));

      for (LivingEntity e : caged) {
         chain.add(center(e));
      }

      Vfx.chainLightning(level, chain, P);

      for (int i = 0; i < caged.size(); i++) {
         prison(level, p, u, caged.get(i), pw, i == 0);
      }

      runic(p, level).lastCombat = level.getTime();
      return true;
   }

   private static void prison(ServerWorld level, PlayerEntity p, GearUse u, LivingEntity t, float pw, boolean plant) {
      boolean boss = isBoss(t);
      int d = boss ? 20 : 50;
      float bbw = t.getWidth();
      Vfx.ward(level, ground(level, t.getEntityPos()), 0.8 + bbw / 2.0F, P, d);
      Vfx.glint(level, top(t), CORE, 1.0);
      hit(level, p, t, 8.0F * pw, false);
      u.resonate(4.0F);
      if (boss) {
         t.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, d, 2), p);
      } else {
         t.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, d, 5), p);
         t.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, d, 0), p);
      }

      run(level, p, (l, age) -> {
         if (gone(p, l)) {
            return true;
         }

         if (age < d && t.isAlive() && !t.isRemoved()) {
            if (!boss) {
               Vec3d v = t.getVelocity();
               t.setVelocity(0.0, t.hasNoGravity() ? 0.0 : Math.min(0.0, v.y), 0.0);
               t.knockedBack = true;
               if (t instanceof MobEntity m) {
                  m.getNavigation().stop();
               }
            }

            if (age % 5 == 0) {
               Vfx.ring(l, t.getEntityPos(), GLOW, 0.9 + bbw / 2.0F, 6);
            }

            return false;
         } else {
            shatter(l, p, t, pw, plant);
            return true;
         }
      });
   }

   private static void shatter(ServerWorld level, PlayerEntity p, LivingEntity t, float pw, boolean plant) {
      Vec3d c = center(t);
      if (t.isAlive() && !t.isRemoved()) {
         hit(level, p, t, 12.0F * pw, false);
      }

      for (LivingEntity e : foes(level, p, c, 3.0)) {
         if (e != t) {
            hit(level, p, e, 6.0F * pw, false);
         }
      }

      Vfx.shards(level, c, CORE, 22, 0.7);
      Vfx.nova(level, c, P, 3.0);
      Vfx.glint(level, c, CORE, 1.4);
      sfx(level, c, 1.2F, 1.0F, SHATTER);
      if (plant) {
         stone(level, p, ground(level, t.getEntityPos()), false);
      }
   }

   static void beamLevel(ServerWorld level, PlayerEntity p, WeaponKit w, int lvl, boolean max) {
      Vec3d tip = GearKit.tip(p);
      Vfx.gather(level, tip, P, 1.0 + 0.3 * lvl, 6 + 2 * lvl);
      Vfx.ring(level, p.getEntityPos(), GLOW, 2.6 - 0.4 * lvl, 8);
      Vfx.flare(level, tip, max ? CORE : GLOW, 0.8 + 0.3 * lvl, 6);
      if (lvl >= 2) {
         Random rnd = level.getRandom();
         Vec3d off = new Vec3d(rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5);
         off = off.lengthSquared() > 1.0E-4 ? off.normalize().multiply(1.2) : new Vec3d(0.0, 1.2, 0.0);
         Vfx.lightning(level, tip.add(off), tip, P, 1);
      }

      if (max) {
         Vfx.chargeUp(level, tip, P, 12);
         Vfx.runeCircle(level, ground(level, p.getEntityPos()), 1.6, P, 30, 0.06F, Vfx.Rune.GLYPH);
      }
   }

   static boolean heartBeam(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      int c = Math.max(1, u.charge());
      boolean emp = u.empowered();
      double w = 0.4 + 0.1 * c + (emp ? 0.3 : 0.0);
      GearKit.kick(level, p, 0.15);
      Vfx.flare(level, GearKit.tip(p), CORE, 3.0, 8);
      runic(p, level).lastCombat = level.getTime();
      run(level, p, new ArchonArsenal.Channel(p, u, u.power(), 20 + 10 * c, w, emp));
      return true;
   }

   static void heartspark(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof PlayerEntity p) {
         ArchonArsenal.Runic r = runic(p, level);
         r.lastCombat = level.getTime();
         if (++r.sparkHits % 3 == 0) {
            List<LivingEntity> chain = new ArrayList<>();
            chain.add(t);
            LivingEntity prev = t;

            for (int i = 0; i < 3; i++) {
               LivingEntity next = null;
               double best = 25.0;

               for (LivingEntity e : GearKit.enemies(level, p, prev.getEntityPos(), 7.0)) {
                  double d = e.squaredDistanceTo(prev);
                  if (!chain.contains(e) && d <= best) {
                     best = d;
                     next = e;
                  }
               }

               if (next == null) {
                  break;
               }

               chain.add(next);
               prev = next;
            }

            List<Vec3d> pts = new ArrayList<>();
            pts.add(chest(p));

            for (LivingEntity e : chain) {
               Vec3d c = center(e);
               hit(level, p, e, 5.0F, false);
               pts.add(c);
               Vfx.glint(level, c, CORE, 0.7);
            }

            Vfx.chainLightning(level, pts, P);
            GearState.of(p).addResonance("archon_focus", 6.0F);
            GearKit.procSound(level, t, "archon_focus");
         }
      }
   }

   static boolean sigilSlam(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      int c = u.charge();
      float pw = u.power();
      double r = 3.5 + 0.75 * c;
      Vec3d at = ground(level, p.getEntityPos().add(GearKit.flatLook(p).multiply(2.5)));
      Vfx.slashArc(level, p.getEntityPos().add(0.0, 1.4, 0.0), p.getYaw(), 25.0F, 90.0F, 3.0, P, 140.0F);

      for (LivingEntity e : disc(level, p, at, r, -2.5, 2.5)) {
         hit(level, p, e, 16.0F * pw, true);
         shove(e, at, 0.3, 0.45 + 0.1 * c);
         u.resonate(3.0F);
         Vfx.impact(level, center(e), P, 0.6);
      }

      Vfx.heavyImpact(level, at, P, 1.0 + 0.25 * c);
      Vfx.runeCircle(level, at, r, P, 30, 0.1F, Vfx.Rune.GLYPH);
      Vfx.groundCrack(level, at, r, P);
      Vfx.shockwave(level, at, P, r);
      GearKit.kick(level, p, 0.35 + 0.1 * c);
      runic(p, level).lastCombat = level.getTime();
      ArchonArsenal.Stone[] made = new ArchonArsenal.Stone[1];
      later(level, p, 4, () -> {
         made[0] = stone(level, p, at, false);
         Vfx.pillar(level, at, P, 3 + c, 0.6);
      });
      if (c >= 3) {
         Vfx.telegraphCircle(level, at, r + 2.5, 6, P);
         later(level, p, 6, () -> {
            for (LivingEntity ex : disc(level, p, at, r + 2.5, -2.5, 3.0)) {
               if (flatDist(ex, at) >= r - 0.5) {
                  hit(level, p, ex, 8.0F * pw, false);
                  shove(ex, at, 0.6, 0.3);
               }
            }

            Vfx.shockwave(level, at, P, r + 2.5);

            for (int k = 0; k < 10; k++) {
               double a = k * Math.PI * 2.0 / 10.0;
               Vfx.pillar(level, ground(level, at.add(Math.cos(a) * (r + 2.0), 0.0, Math.sin(a) * (r + 2.0))), P, 1.4, 0.35);
            }
         });
      }

      if (u.empowered()) {
         Vfx.telegraphCircle(level, at, 5.5, 12, P);
         later(level, p, 12, () -> {
            Vec3d from = made[0] != null ? made[0].pos : at.add(0.0, 1.2, 0.0);
            int n = 0;

            for (LivingEntity ex : disc(level, p, at, 5.5, -2.5, 4.0)) {
               hit(level, p, ex, 12.0F * pw, false);
               if (n++ < 5) {
                  Vfx.lightning(level, from, center(ex), P, 2);
               }
            }

            Vfx.nova(level, at.add(0.0, 0.5, 0.0), P, 5.5);
            Vfx.runeCircle(level, at, 5.5, P, 16, -0.12F, Vfx.Rune.ARCANE);
            sfx(level, at, 1.0F, 1.15F, FIELD);
         });
      }

      return true;
   }

   static void crushLevel(ServerWorld level, PlayerEntity p, WeaponKit w, int lvl, boolean max) {
      Vec3d rt = raisedTip(p);
      Vfx.ring(level, p.getEntityPos(), GLOW, 3.4 - 0.6 * lvl, 8);
      Vfx.flare(level, rt, max ? CORE : GLOW, 0.9 + 0.4 * lvl, 6);
      Vfx.shards(level, rt, CORE, 2 + 2 * lvl, 0.2);
      if (lvl >= 2) {
         Vfx.lightning(level, rt.add(0.0, 2.5, 0.0), rt, P, lvl - 1);
      }

      if (max) {
         Vfx.runeCircle(level, ground(level, p.getEntityPos()), 2.0, P, 30, 0.08F, Vfx.Rune.GLYPH);
         Vfx.gather(level, rt, P, 1.6, 10);
         Vfx.glint(level, rt, CORE, 1.2);
      }
   }

   static boolean gravitySigil(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float pw = u.power();
      boolean emp = u.empowered();
      double r = 4.5;
      Vec3d at = ground(level, GearKit.lookPoint(level, p, 20.0).subtract(p.getRotationVector().multiply(0.2)));
      Vec3d tip = GearKit.tip(p);
      Vfx.beam(level, tip, at.add(0.0, 0.3, 0.0), P, 0.25, 5);
      Vfx.dashTrail(level, tip, at.add(0.0, 1.0, 0.0), P);
      Vfx.runeCircle(level, at, r, P, 34, -0.1F, Vfx.Rune.ARCANE);
      Vfx.telegraphCircle(level, at, r, 14, P);

      for (LivingEntity e : disc(level, p, at, r, 0.0, 14.0)) {
         if (!e.isOnGround() && !isBoss(e)) {
            Vec3d v = e.getVelocity();
            e.setVelocity(v.x * 0.3, -1.4, v.z * 0.3);
            e.knockedBack = true;
            Vfx.streaks(level, center(e), DOWN, P, 6);
         }
      }

      if (emp) {
         for (LivingEntity e : disc(level, p, at, 8.0, -2.0, 6.0)) {
            if (!isBoss(e)) {
               Vec3d to = new Vec3d(at.x - e.getX(), 0.0, at.z - e.getZ());
               if (to.lengthSquared() > 1.0E-4) {
                  to = to.normalize().multiply(0.6);
                  e.addVelocity(to.x, 0.1, to.z);
                  e.knockedBack = true;
               }
            }
         }

         Vfx.vortex(level, at, 4.5, 3.0, P, 14);
      }

      runic(p, level).lastCombat = level.getTime();
      run(level, p, new ArchonArsenal.Gravity(p, u, at, r, pw, emp));
      return true;
   }

   static void leyDischarge(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof PlayerEntity p) {
         long now = level.getTime();
         runic(p, level).lastCombat = now;
         Vec3d c = center(t);

         for (LivingEntity e : foes(level, p, c, 2.0)) {
            if (e != t) {
               hit(level, p, e, 3.0F, false);
            }
         }

         Vfx.shards(level, c, CORE, 5, 0.35);
         Vfx.ring(level, t.getEntityPos(), GLOW, 2.0, 8);
         ArchonArsenal.Stone best = null;
         double bestD = Double.MAX_VALUE;

         for (ArchonArsenal.Stone st : stones(p)) {
            double d = boxDist(t.getBoundingBox(), st.pos);
            if (d <= 5.0 && d < bestD && now - st.lastArc >= 20L) {
               best = st;
               bestD = d;
            }
         }

         if (best != null && t.isAlive()) {
            best.lastArc = now;
            Vfx.lightning(level, best.pos, c, P, 2);
            hit(level, p, t, 6.0F, false);
            GearKit.procSound(level, t, "sigil_hammer");
         }
      }
   }

   private static void wardShimmer(ServerWorld level, PlayerEntity p, int pieces) {
      if (level.getTime() % 40L == 0L && GearState.of(p).ready("archon.deflect")) {
         Vfx.ring(level, p.getEntityPos(), GLOW, 0.7, 20);
      }
   }

   private static boolean spellward(ServerWorld level, PlayerEntity p, DamageSource source, float amount, int pieces) {
      if (pieces >= 2 && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
         GearState state = GearState.of(p);
         if (!state.ready("archon.deflect")) {
            return true;
         }

         state.cooldown("archon.deflect", 160);
         Vec3d chest = chest(p);
         Entity direct = source.getSource();
         Vec3d from = direct != null ? direct.getEntityPos() : source.getPosition();
         Vec3d dir = from != null && from.squaredDistanceTo(chest) > 1.0E-4 ? from.subtract(chest).normalize() : p.getRotationVector();
         if (direct instanceof ProjectileEntity pr) {
            pr.discard();
         }

         Vec3d flash = chest.add(dir.multiply(0.9));
         Vfx.shieldBubble(level, chest, 1.1, P, 10);
         Vfx.flare(level, flash, CORE, 1.6, 6);
         Vfx.shards(level, flash, CORE, 6, 0.3);
         if (source.getAttacker() instanceof LivingEntity shooter
            && shooter != p
            && !(shooter instanceof PlayerEntity)
            && shooter.isAlive()
            && shooter.squaredDistanceTo(p) <= 1600.0) {
            Vec3d to = center(shooter).subtract(flash);
            ArchonArsenal.Shot s = shot(level, p, flash, (to.lengthSquared() > 1.0E-4 ? to.normalize() : dir).multiply(1.0), 60, 0.5, 0);
            if (s != null) {
               s.homing = 0.25;
               s.target = shooter;
               s.aimAt = center(shooter);
               s.onHit = (sh, e, at) -> {
                  if (e != null) {
                     hit(level, p, e, 8.0F, false);
                  }

                  Vfx.impact(level, at, P, 0.5);
                  sfx(level, at, 0.8F, 1.0F, IMPACT);
               };
               sfx(level, flash, 0.7F, 1.1F, LAUNCH);
            }
         }

         sfx(level, chest, 1.0F, 1.0F, WARD);
         return false;
      } else {
         return true;
      }
   }

   private static void runeplateHurt(ServerWorld level, PlayerEntity p, @Nullable LivingEntity attacker, DamageSource source, float taken, int pieces) {
      if (attacker != null) {
         runic(p, level).lastHurt = level.getTime();
      }
   }

   private static void leyConduit(ServerWorld level, PlayerEntity p, int pieces) {
      long now = level.getTime();
      if (now % 20L == 0L) {
         ArchonArsenal.Stone near = nearestStone(p, p.getEntityPos(), 10.0);
         if (near != null) {
            GearKit.keep(p, StatusEffects.RESISTANCE, 0);
            StatusEffectInstance regen = p.getStatusEffect(StatusEffects.REGENERATION);
            if (regen == null || regen.getAmplifier() == 0 && regen.getDuration() <= 20) {
               p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 70, 0, true, false, true));
            }

            if (now % 40L == 0L) {
               Vfx.auraRing(level, p.getEntityPos(), P, 0.9);
            }
         }

         if (now % 60L == 0L) {
            turrets(level, p);
         }

         ArchonArsenal.Runic r = RUNIC.get(p.getUuid());
         boolean fighting = r != null && (now - r.lastHurt < 100L || now - r.lastCombat < 100L);
         GearState state = GearState.of(p);
         if (fighting && near == null && state.ready("archon.seed")) {
            stone(level, p, ground(level, p.getEntityPos().add(right(p).multiply(1.5))), false);
            state.cooldown("archon.seed", 200);
         }
      }
   }

   private static void turrets(ServerWorld level, PlayerEntity p) {
      int n = 0;

      for (ArchonArsenal.Stone st : stonesNear(p, p.getEntityPos(), 12.0)) {
         if (n >= 3) {
            return;
         }

         LivingEntity e = nearestFoe(level, p, st.pos, 12.0);
         if (e != null) {
            Vec3d d = center(e).subtract(st.pos);
            Vec3d dir = d.lengthSquared() > 1.0E-4 ? d.normalize() : UP;
            ArchonArsenal.Shot s = shot(level, p, st.pos.add(dir.multiply(0.4)), dir.multiply(0.9), 50, 0.5, 0);
            if (s == null) {
               return;
            }

            s.homing = 0.2;
            s.target = e;
            s.aimAt = center(e);
            s.onHit = (sh, hitE, at) -> {
               if (hitE != null) {
                  hit(level, p, hitE, 5.0F, false);
               }

               Vfx.impact(level, at, P, 0.4);
               sfx(level, at, 0.6F, 1.1F, IMPACT);
            };
            Vfx.flare(level, st.pos, GLOW, 1.2, 5);
            sfx(level, st.pos, 0.6F, 1.0F, LAUNCH);
            n++;
         }
      }
   }

   static boolean pentagramLattice(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vec3d c = ground(level, p.getEntityPos());
      float yaw = p.getYaw();
      Vec3d[] v = new Vec3d[5];

      for (int k = 0; k < 5; k++) {
         v[k] = ground(level, c.add(yawDir(yaw + 72 * k).multiply(6.0)));
      }

      boolean[] taken = new boolean[5];
      int fed = 0;

      for (ArchonArsenal.Stone st : stonesNear(p, p.getEntityPos(), 16.0)) {
         if (fed >= 5) {
            break;
         }

         int best = -1;
         double bestD = 0.0;

         for (int k = 0; k < 5; k++) {
            double d = st.pos.squaredDistanceTo(v[k]);
            if (!taken[k] && (best < 0 || d < bestD)) {
               best = k;
               bestD = d;
            }
         }

         taken[best] = true;
         Vfx.dashTrail(level, st.pos, v[best].add(0.0, 1.2, 0.0), P);
         Vfx.beam(level, v[best], v[best].add(0.0, 2.2, 0.0), P, 0.25, 84);
         consume(p, st);
         fed++;
      }

      float amp = 1.0F + 0.15F * fed;
      Vfx.runeCircle(level, c, 6.6, P, 84, 0.03F, Vfx.Rune.ARCANE);
      Vfx.runeCircle(level, c, 2.6, P, 84, -0.06F, Vfx.Rune.GLYPH);

      for (Vec3d q : v) {
         Vfx.pillar(level, q, P, 4.0, 0.6);
         Vfx.glint(level, q.add(0.0, 1.2, 0.0), CORE, 0.9);
      }

      for (int[] e : STAR) {
         Vec3d a = v[e[0]];
         Vec3d d = v[e[1]].subtract(a);
         double len = d.horizontalLength();
         Vfx.telegraphLine(level, a, len > 0.001 ? new Vec3d(d.x / len, 0.0, d.z / len) : new Vec3d(0.0, 0.0, 1.0), len, 1.2, 20, P);
      }

      Vfx.chargeUp(level, chest(p), P, 20);
      p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 24, 1, true, false, true));
      run(level, p, new ArchonArsenal.Lattice(p, c, v, amp));
      return true;
   }

   private static ArchonArsenal.Stone stone(ServerWorld level, PlayerEntity p, Vec3d ground, boolean planted) {
      ArchonArsenal.Runic r = runic(p, level);

      while (r.stones.size() >= 5) {
         fizzle(level, r.stones.remove(0));
      }

      int life = MysticArmor.pieces(p, "archon") >= 2 ? 400 : 300;
      ArchonArsenal.Stone s = new ArchonArsenal.Stone(ground, planted, level.getTime(), life, Math.floorMod(seq++ * 7, 20));
      r.stones.add(s);
      Vfx.pillar(level, ground, P, 2.8, 0.45);
      Vfx.shards(level, s.pos, CORE, 8, 0.35);
      Vfx.ring(level, ground, GLOW, 1.4, 12);
      stoneLook(level, s, 22);
      sfx(level, s.pos, 1.0F, 1.0F + level.getRandom().nextFloat() * 0.1F, GLYPH);
      return s;
   }

   private static ArchonArsenal.@Nullable Stone nearestStone(PlayerEntity p, Vec3d at, double r) {
      ArchonArsenal.Stone best = null;
      double bestD = r * r;

      for (ArchonArsenal.Stone s : stones(p)) {
         double d = s.pos.squaredDistanceTo(at);
         if (d <= bestD) {
            best = s;
            bestD = d;
         }
      }

      return best;
   }

   private static List<ArchonArsenal.Stone> stonesNear(PlayerEntity p, Vec3d at, double r) {
      List<ArchonArsenal.Stone> out = new ArrayList<>();

      for (ArchonArsenal.Stone s : stones(p)) {
         if (s.pos.squaredDistanceTo(at) <= r * r) {
            out.add(s);
         }
      }

      out.sort(Comparator.comparingDouble(sx -> sx.pos.squaredDistanceTo(at)));
      return out;
   }

   private static void consume(PlayerEntity p, ArchonArsenal.Stone s) {
      ArchonArsenal.Runic r = RUNIC.get(p.getUuid());
      if (r != null) {
         r.stones.remove(s);
      }
   }

   private static List<ArchonArsenal.Stone> stones(PlayerEntity p) {
      ArchonArsenal.Runic r = RUNIC.get(p.getUuid());
      return r != null && r.level == p.getEntityWorld() ? r.stones : List.of();
   }

   private static void fizzle(ServerWorld level, ArchonArsenal.Stone s) {
      Vfx.shards(level, s.pos, CORE, 6, 0.25);
      Vfx.flare(level, s.pos, GLOW, 1.0, 5);
   }

   private static void stoneLook(ServerWorld level, ArchonArsenal.Stone s, int life) {
      if (s.planted) {
         Vfx.beam(level, s.ground, s.ground.add(0.0, 2.3, 0.0), P, 0.22, life);
         Vfx.beam(level, s.ground.add(-0.45, 1.7, 0.0), s.ground.add(0.45, 1.7, 0.0), P, 0.12, life);
      } else {
         Vfx.beam(level, s.pos.add(0.0, -0.55, 0.0), s.pos.add(0.0, 0.55, 0.0), P, 0.3, life);
         Vfx.emit(level, VfxOptions.of(VfxParticles.Kind.RUNE, GLOW, 0.9, life + 2).upright().spin(0.04F).fade(3).build(), s.pos, Vec3d.ZERO);
      }
   }

   private static void tickStones(ServerWorld level, ArchonArsenal.Runic r, long now) {
      Iterator<ArchonArsenal.Stone> it = r.stones.iterator();

      while (it.hasNext()) {
         ArchonArsenal.Stone s = it.next();
         long left = s.born + s.life - now;
         if (left <= 0L) {
            fizzle(level, s);
            it.remove();
         } else {
            long ph = now + s.phase;
            if (ph % 20L == 0L) {
               stoneLook(level, s, (int)Math.min(22L, left + 1L));
            }

            if (ph % (left <= 40L ? 5 : 10) == 0L) {
               Vfx.glint(level, s.pos.add(0.0, 0.65, 0.0), CORE, 0.45);
               if (ph % 10L == 0L) {
                  Vfx.sparks(level, s.pos, GLOW, 1, 0.04, 0.1);
               }
            }

            if (ph % 40L == 0L && left > 20L) {
               Vfx.runeCircle(level, s.ground, 1.1, P, (int)Math.min(42L, left), 0.04F, Vfx.Rune.GLYPH);
            }
         }
      }
   }

   private static void leyLines(ServerWorld level, PlayerEntity p, ArchonArsenal.Runic r) {
      List<ArchonArsenal.Seg> segs = new ArrayList<>();
      List<ArchonArsenal.Stone> st = r.stones;

      for (int i = 0; i < st.size(); i++) {
         for (int j = i + 1; j < st.size(); j++) {
            double d = st.get(i).pos.distanceTo(st.get(j).pos);
            if (d <= 10.0) {
               segs.add(new ArchonArsenal.Seg(st.get(i).pos, st.get(j).pos, d));
            }
         }
      }

      if (MysticArmor.pieces(p, "archon") >= 4) {
         Vec3d ch = p.getEntityPos().add(0.0, 1.1, 0.0);

         for (ArchonArsenal.Stone s : st) {
            double d = ch.distanceTo(s.pos);
            if (d <= 10.0) {
               segs.add(new ArchonArsenal.Seg(ch, s.pos, d));
            }
         }
      }

      if (!segs.isEmpty()) {
         segs.sort(Comparator.comparingDouble(ArchonArsenal.Seg::len));
         Set<LivingEntity> burned = new HashSet<>();

         for (int i = 0; i < Math.min(10, segs.size()); i++) {
            ArchonArsenal.Seg s = segs.get(i);
            Vfx.beam(level, s.a(), s.b(), P, 0.1, 12);
            Vfx.sparks(level, s.a().add(s.b()).multiply(0.5), GLOW, 1, 0.05, 0.1);

            for (LivingEntity e : onSegment(level, p, s.a(), s.b(), 0.8)) {
               if (burned.add(e)) {
                  pulseHit(level, p, e, 3.0F);
                  e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 15, 0), p);
                  Vfx.glint(level, center(e), CORE, 0.5);
               }
            }
         }
      }
   }

   private static Vec3d bladeSlot(PlayerEntity p, int k, long now) {
      double a = now * 0.16 + k * 2.094;
      return p.getEntityPos().add(Math.cos(a) * 1.25, 1.2, Math.sin(a) * 1.25);
   }

   private static void addBlade(ArchonArsenal.Runic r, long now) {
      r.blades = Math.min(3, r.blades + 1);
      r.bladeUntil = now + 200L;
   }

   private static void tickBlades(ServerWorld level, PlayerEntity p, ArchonArsenal.Runic r, long now, boolean halo) {
      if (r.blades > 0) {
         if (now < r.bladeUntil) {
            if (!halo && now % 3L == 0L) {
               for (int k = 0; k < r.blades; k++) {
                  Vec3d s = bladeSlot(p, k, now);
                  Vfx.beam(level, s.add(0.0, 0.45, 0.0), s.add(0.0, -0.55, 0.0), P, 0.16);
                  if (now % 9L == 0L) {
                     Vfx.glint(level, s.add(0.0, 0.45, 0.0), CORE, 0.35);
                  }
               }
            }
         } else {
            for (int k = 0; k < r.blades; k++) {
               Vfx.flare(level, bladeSlot(p, k, now), GLOW, 0.8, 5);
            }

            r.blades = 0;
         }
      }
   }

   private static Vec3d haloSlot(PlayerEntity p, int n, int k) {
      double o = k - (n - 1) / 2.0;
      return GearKit.eye(p).subtract(GearKit.flatLook(p).multiply(0.9)).add(right(p).multiply(o * 0.75)).add(0.0, 0.9 - 0.12 * o * o, 0.0);
   }

   private static void holdVisuals(ServerWorld level, ServerPlayerEntity p) {
      if (p.isUsingItem() && p.getActiveItem().getItem() instanceof MysticWeapon w) {
         int var8 = p.getItemUseTime();
         if ("runeblade".equals(w.gearId())) {
            ArchonArsenal.Runic r = RUNIC.get(p.getUuid());
            int n = Math.min(6, Math.min(3, var8 / 12) + (r != null ? r.blades : 0));

            for (int k = 0; k < n; k++) {
               Vec3d s = haloSlot(p, n, k);
               Vfx.beam(level, s.add(0.0, 0.5, 0.0), s.add(0.0, -0.6, 0.0), P, 0.16);
            }
         } else if ("archon_focus".equals(w.gearId()) && var8 >= 40) {
            Vec3d end = GearKit.lookPoint(level, p, 28.0);
            Vfx.beam(level, GearKit.tip(p), end, P, 0.05);
            Vfx.glint(level, end, CORE, 0.4);
         }
      }
   }

   private static ArchonArsenal.@Nullable Shot shot(ServerWorld level, PlayerEntity owner, Vec3d from, Vec3d vel, int life, double radius, int kind) {
      int live = 0;

      for (ArchonArsenal.Shot s : SHOTS) {
         if (s.owner == owner) {
            live++;
         }
      }

      for (ArchonArsenal.Shot s : NEW_SHOTS) {
         if (s.owner == owner) {
            live++;
         }
      }

      if (live >= 40) {
         return null;
      }

      ArchonArsenal.Shot s = new ArchonArsenal.Shot(level, owner, from, vel, life, radius, kind);
      NEW_SHOTS.add(s);
      return s;
   }

   private static boolean step(ArchonArsenal.Shot s) {
      if (gone(s.owner, s.level)) {
         return true;
      }

      s.age++;
      if (s.target != null) {
         if (s.target.isAlive() && !s.target.isRemoved()) {
            s.aimAt = center(s.target);
         } else {
            s.target = null;
         }
      }

      s.speed = Math.min(s.maxSpeed, s.speed + s.accel);
      Vec3d dir = s.vel.lengthSquared() > 1.0E-8 ? s.vel.normalize() : UP;
      if (s.homing > 0.0 && s.age >= s.homingFrom && s.aimAt != null) {
         Vec3d to = s.aimAt.subtract(s.pos);
         if (to.lengthSquared() > 1.0E-4) {
            Vec3d bent = dir.lerp(to.normalize(), s.homing);
            if (bent.lengthSquared() > 1.0E-6) {
               dir = bent.normalize();
            }
         }
      }

      s.vel = dir.multiply(s.speed);
      Vec3d next = s.pos.add(s.vel);
      BlockHitResult block = s.level.raycast(new RaycastContext(s.pos, next, ShapeType.COLLIDER, FluidHandling.NONE, s.owner));
      boolean wall = block.getType() != Type.MISS;
      Vec3d end = wall ? block.getPos() : next;
      LivingEntity best = null;
      Vec3d bestAt = null;
      double bestD = Double.MAX_VALUE;
      Vec3d mid = s.pos.add(end).multiply(0.5);

      for (LivingEntity e : GearKit.enemies(s.level, s.owner, mid, s.radius + end.distanceTo(s.pos) / 2.0 + 5.0)) {
         Box box = e.getBoundingBox().expand(s.radius);
         Vec3d at = box.contains(s.pos) ? s.pos : (Vec3d)box.raycast(s.pos, end).orElse(null);
         if (at != null) {
            double d = at.squaredDistanceTo(s.pos);
            if (d < bestD) {
               bestD = d;
               best = e;
               bestAt = at;
            }
         }
      }

      if (best != null) {
         if (s.onHit != null) {
            s.onHit.at(s, best, bestAt);
         }

         return true;
      } else if (wall) {
         if (s.onHit != null) {
            s.onHit.at(s, null, end);
         }

         return true;
      } else {
         s.pos = next;
         if (s.kind == 1) {
            Vfx.meteorTrail(s.level, s.pos, s.vel, P);
            Vfx.beam(s.level, s.pos, s.pos.subtract(dir), P, 0.18);
         } else {
            Vfx.trail(s.level, s.pos, P, 0.35);
            if (s.age % 3 == 0) {
               Vfx.glint(s.level, s.pos, CORE, 0.3);
            }
         }

         if (s.age >= s.life) {
            if (s.onHit != null) {
               s.onHit.at(s, null, s.pos);
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private static void hook() {
      if (!hooked) {
         hooked = true;
         ServerTickEvents.END_WORLD_TICK.register(ArchonArsenal::worldTick);
         ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> reset());
         ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, server) -> RUNIC.remove(handler.player.getUuid()));
      }
   }

   private static void reset() {
      TASKS.clear();
      SHOTS.clear();
      NEW_SHOTS.clear();
      RUNIC.clear();
   }

   private static void run(ServerWorld level, PlayerEntity owner, ArchonArsenal.Task task) {
      UUID id = owner.getUuid();
      int live = 0;

      for (ArchonArsenal.Job j : TASKS) {
         if (!j.done && j.owner.equals(id)) {
            live++;
         }
      }

      if (live < 24) {
         TASKS.add(new ArchonArsenal.Job(level, id, level.getTime(), task));
      }
   }

   private static void later(ServerWorld level, PlayerEntity p, int delay, Runnable r) {
      run(level, p, (l, t) -> {
         if (gone(p, l)) {
            return true;
         } else if (t >= delay) {
            r.run();
            return true;
         } else {
            return false;
         }
      });
   }

   private static void worldTick(ServerWorld level) {
      long now = level.getTime();
      if (!TASKS.isEmpty()) {
         for (ArchonArsenal.Job j : new ArrayList<>(TASKS)) {
            if (j.level == level && !j.done) {
               try {
                  j.done = j.task.tick(level, (int)(now - j.start));
               } catch (RuntimeException ex) {
                  j.done = true;
                  MysticalTerrain.LOGGER.error("Archon gear effect failed", ex);
               }
            }
         }

         TASKS.removeIf(j -> j.done);
      }

      if (!NEW_SHOTS.isEmpty()) {
         SHOTS.addAll(NEW_SHOTS);
         NEW_SHOTS.clear();
      }

      if (!SHOTS.isEmpty()) {
         SHOTS.removeIf(s -> {
            if (s.level != level) {
               return false;
            }

            try {
               return step(s);
            } catch (RuntimeException ex) {
               MysticalTerrain.LOGGER.error("Archon gear shot failed", ex);
               return true;
            }
         });
      }

      for (ServerPlayerEntity p : level.getPlayers()) {
         try {
            tickPlayer(level, p, now);
         } catch (RuntimeException ex) {
            MysticalTerrain.LOGGER.error("Archon gear state failed", ex);
            RUNIC.remove(p.getUuid());
         }
      }
   }

   private static void tickPlayer(ServerWorld level, ServerPlayerEntity p, long now) {
      if (now % 2L == 0L) {
         holdVisuals(level, p);
      }

      ArchonArsenal.Runic r = RUNIC.get(p.getUuid());
      if (r != null) {
         if (r.level != level) {
            r.stones.clear();
            r.level = level;
         }

         if (!p.isAlive()) {
            r.stones.clear();
            r.blades = 0;
         } else {
            tickStones(level, r, now);
            tickBlades(level, p, r, now, p.isUsingItem() && isGear(p.getActiveItem(), "runeblade"));
            if (!r.stones.isEmpty() && (now + (p.getId() & 7)) % 10L == 0L) {
               leyLines(level, p, r);
            }
         }

         if (r.stones.isEmpty() && r.blades == 0 && now - Math.max(r.touched, Math.max(r.lastCombat, r.lastHurt)) > 400L) {
            RUNIC.remove(p.getUuid());
         }
      }
   }

   private static ArchonArsenal.Runic runic(PlayerEntity p, ServerWorld level) {
      ArchonArsenal.Runic r = RUNIC.computeIfAbsent(p.getUuid(), id -> new ArchonArsenal.Runic(level, level.getTime()));
      if (r.level != level) {
         r.stones.clear();
         r.level = level;
      }

      return r;
   }

   private static boolean gone(PlayerEntity p, ServerWorld level) {
      return !p.isAlive() || p.isRemoved() || p.getEntityWorld() != level;
   }

   private static void hit(ServerWorld level, PlayerEntity p, LivingEntity e, float dmg, boolean melee) {
      if (e.isAlive() && !(dmg <= 0.0F)) {
         e.timeUntilRegen = 0;
         if (melee) {
            GearKit.strike(level, p, e, dmg);
         } else {
            GearKit.magic(level, p, e, dmg);
         }
      }
   }

   private static void pulseHit(ServerWorld level, PlayerEntity p, LivingEntity e, float dmg) {
      if (e.isAlive() && !(dmg <= 0.0F)) {
         int iv = e.timeUntilRegen;
         e.timeUntilRegen = 0;
         e.damage(level, level.getDamageSources().create(DamageTypes.MAGIC, p), dmg);
         e.timeUntilRegen = iv;
      }
   }

   private static void shove(LivingEntity e, Vec3d from, double strength, double lift) {
      if (e.isAlive() && !isBoss(e)) {
         GearKit.knockAway(e, from, strength, lift);
      }
   }

   private static boolean isBoss(LivingEntity e) {
      return e instanceof MysticalBoss b
         ? b.isBoss()
         : e instanceof EnderDragonEntity || e instanceof WitherEntity || e instanceof WardenEntity || e instanceof ElderGuardianEntity;
   }

   private static boolean isGear(ItemStack stack, String id) {
      return stack.getItem() instanceof MysticWeapon w && id.equals(w.gearId());
   }

   private static boolean holdsFocus(PlayerEntity p) {
      return isGear(p.getMainHandStack(), "archon_focus") || isGear(p.getOffHandStack(), "archon_focus");
   }

   private static List<LivingEntity> foes(ServerWorld level, PlayerEntity p, Vec3d at, double r) {
      return GearKit.enemies(level, p, at, r + 5.0).stream().filter(e -> boxDist(e.getBoundingBox(), at) <= r).toList();
   }

   private static List<LivingEntity> disc(ServerWorld level, PlayerEntity p, Vec3d at, double r, double lo, double hi) {
      double half = (hi - lo) / 2.0 + 5.0;
      double q = Math.max(half / 0.75, Math.sqrt((r + 3.0) * (r + 3.0) + half * half));
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, at.add(0.0, (lo + hi) / 2.0, 0.0), q)) {
         Box b = e.getBoundingBox();
         if (!(b.maxY < at.y + lo) && !(b.minY > at.y + hi)) {
            double dx = Math.max(Math.max(b.minX - at.x, 0.0), at.x - b.maxX);
            double dz = Math.max(Math.max(b.minZ - at.z, 0.0), at.z - b.maxZ);
            if (dx * dx + dz * dz <= r * r) {
               out.add(e);
            }
         }
      }

      return out;
   }

   private static List<LivingEntity> onSegment(ServerWorld level, PlayerEntity p, Vec3d a, Vec3d b, double width) {
      Vec3d mid = a.add(b).multiply(0.5);
      double half = a.distanceTo(b) / 2.0;
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, mid, (half + width + 4.0) / 0.75)) {
         if (entDist(e, a, b) <= width + e.getWidth() / 2.0F) {
            out.add(e);
         }
      }

      return out;
   }

   private static List<LivingEntity> sweep(ServerWorld level, PlayerEntity p, double reach, double halfAngle) {
      Vec3d f = GearKit.flatLook(p);
      double cos = Math.cos(Math.toRadians(halfAngle));
      double y = p.getY() + 1.25;
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, p.getEntityPos(), reach + 4.0)) {
         Box b = e.getBoundingBox();
         if (!(b.minY > y + 2.5) && !(b.maxY < y - 2.5)) {
            Vec3d to = new Vec3d(e.getX() - p.getX(), 0.0, e.getZ() - p.getZ());
            double d = to.length();
            if (!(d - e.getWidth() / 2.0F > reach) && (!(d > 1.0) || !(to.multiply(1.0 / d).dotProduct(f) < cos))) {
               out.add(e);
            }
         }
      }

      return out;
   }

   private static @Nullable LivingEntity nearestFoe(ServerWorld level, PlayerEntity p, Vec3d at, double r) {
      LivingEntity best = null;
      double bestD = r * r;

      for (LivingEntity e : GearKit.enemies(level, p, at, r)) {
         Vec3d c = center(e);
         double d = c.squaredDistanceTo(at);
         if (d <= bestD && los(level, p, at, c)) {
            best = e;
            bestD = d;
         }
      }

      return best;
   }

   private static double boxDist(Box b, Vec3d at) {
      double dx = Math.max(Math.max(b.minX - at.x, 0.0), at.x - b.maxX);
      double dy = Math.max(Math.max(b.minY - at.y, 0.0), at.y - b.maxY);
      double dz = Math.max(Math.max(b.minZ - at.z, 0.0), at.z - b.maxZ);
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private static double flatDist(LivingEntity e, Vec3d at) {
      double dx = e.getX() - at.x;
      double dz = e.getZ() - at.z;
      return Math.max(0.0, Math.sqrt(dx * dx + dz * dz) - e.getWidth() / 2.0F);
   }

   private static double segDist(Vec3d q, Vec3d a, Vec3d b) {
      Vec3d ab = b.subtract(a);
      double len2 = ab.lengthSquared();
      double t = len2 < 1.0E-8 ? 0.0 : MathHelper.clamp(q.subtract(a).dotProduct(ab) / len2, 0.0, 1.0);
      return q.distanceTo(a.add(ab.multiply(t)));
   }

   private static double entDist(LivingEntity e, Vec3d a, Vec3d b) {
      double h = e.getHeight();
      Vec3d f = e.getEntityPos();
      double d = Math.min(segDist(f.add(0.0, Math.min(0.3, h / 2.0), 0.0), a, b), segDist(center(e), a, b));
      return Math.min(d, segDist(f.add(0.0, Math.max(h - 0.3, h / 2.0), 0.0), a, b));
   }

   private static boolean inPolygonXZ(Vec3d q, Vec3d[] poly) {
      boolean in = false;
      int i = 0;

      for (int j = poly.length - 1; i < poly.length; j = i++) {
         Vec3d a = poly[i];
         Vec3d b = poly[j];
         if (a.z > q.z != b.z > q.z && q.x < (b.x - a.x) * (q.z - a.z) / (b.z - a.z) + a.x) {
            in = !in;
         }
      }

      return in;
   }

   private static boolean los(ServerWorld level, PlayerEntity p, Vec3d from, Vec3d to) {
      return level.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, p)).getType() == Type.MISS;
   }

   private static Vec3d clip(ServerWorld level, PlayerEntity p, Vec3d from, Vec3d dir, double range) {
      return level.raycast(new RaycastContext(from, from.add(dir.multiply(range)), ShapeType.COLLIDER, FluidHandling.NONE, p)).getPos();
   }

   private static boolean free(ServerWorld level, PlayerEntity p, Vec3d at) {
      return level.isSpaceEmpty(p, p.getBoundingBox().offset(at.subtract(p.getEntityPos())));
   }

   private static Vec3d center(LivingEntity e) {
      return e.getBoundingBox().getCenter();
   }

   private static Vec3d top(LivingEntity e) {
      return e.getEntityPos().add(0.0, e.getHeight() + 0.3, 0.0);
   }

   private static Vec3d chest(PlayerEntity p) {
      return p.getEntityPos().add(0.0, 1.1, 0.0);
   }

   private static Vec3d raisedTip(PlayerEntity p) {
      return GearKit.eye(p).add(0.0, 0.9, 0.0).add(GearKit.flatLook(p).multiply(-0.2));
   }

   private static Vec3d right(PlayerEntity p) {
      return GearKit.flatLook(p).rotateY((float) (-Math.PI / 2));
   }

   private static Vec3d yawDir(float yaw) {
      return new Vec3d(-MathHelper.sin(yaw * (float) (Math.PI / 180.0)), 0.0, MathHelper.cos(yaw * (float) (Math.PI / 180.0)));
   }

   private static Vec3d ground(ServerWorld level, Vec3d at) {
      Vec3d g = floor(level, at, 1.5, 10);
      return g != null ? g : at;
   }

   private static @Nullable Vec3d floor(ServerWorld level, Vec3d at, double up, int depth) {
      Mutable q = BlockPos.ofFloored(at.x, at.y + up, at.z).mutableCopy();
      int steps = depth + (int)Math.ceil(up) + 1;

      for (int i = 0; i < steps; i++) {
         BlockState s = level.getBlockState(q);
         VoxelShape shape = s.getCollisionShape(level, q);
         if (!shape.isEmpty()) {
            return new Vec3d(at.x, q.getY() + shape.getMax(Axis.Y), at.z);
         }

         FluidState f = s.getFluidState();
         if (!f.isEmpty()) {
            return new Vec3d(at.x, q.getY() + f.getHeight(level, q), at.z);
         }

         q.move(0, -1, 0);
      }

      return null;
   }

   private static void sfx(ServerWorld level, Vec3d at, float volume, float pitch, String... keys) {
      for (String key : keys) {
         SoundEvent e = BossSounds.event(key);
         if (e != null) {
            level.playSound(null, at.x, at.y, at.z, e, SoundCategory.PLAYERS, volume, pitch * (0.95F + level.getRandom().nextFloat() * 0.1F));
            return;
         }
      }
   }

   private static final class Channel implements ArchonArsenal.Task {
      private final PlayerEntity p;
      private final GearUse u;
      private final float pw;
      private final int len;
      private final double w;
      private final boolean emp;
      private final Map<ArchonArsenal.Stone, LivingEntity> refracted = new HashMap<>();

      Channel(PlayerEntity p, GearUse u, float pw, int len, double w, boolean emp) {
         this.p = p;
         this.u = u;
         this.pw = pw;
         this.len = len;
         this.w = w;
         this.emp = emp;
      }

      @Override
      public boolean tick(ServerWorld level, int t) {
         PlayerEntity p = this.p;
         if (ArchonArsenal.gone(p, level)) {
            return true;
         }

         if (ArchonArsenal.holdsFocus(p) && (!p.isUsingItem() || ArchonArsenal.isGear(p.getActiveItem(), "archon_focus"))) {
            Vec3d from = GearKit.tip(p);
            Vec3d look = p.getRotationVector();
            BlockHitResult bh = level.raycast(new RaycastContext(from, from.add(look.multiply(28.0)), ShapeType.COLLIDER, FluidHandling.NONE, p));
            Vec3d end = bh.getPos();
            boolean wall = bh.getType() != Type.MISS;
            Vfx.beam(level, from, end, ArchonArsenal.P, this.w);
            if (t % 3 == 0) {
               Vfx.impact(level, end, ArchonArsenal.P, 0.4);
            }

            if (wall && t % 10 == 0) {
               Vfx.groundCrack(level, end, 1.0, ArchonArsenal.P);
            }

            if (t % 2 == 0) {
               Vfx.glint(level, from, ArchonArsenal.CORE, 0.6);
            }

            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 5, 1, true, false, false));
            boolean pulse = t % 4 == 0;
            Set<LivingEntity> ticked = new HashSet<>();
            List<LivingEntity> onBeam = !pulse && !this.emp ? List.of() : ArchonArsenal.onSegment(level, p, from, end, this.w + 0.6);
            if (pulse) {
               for (LivingEntity e : onBeam) {
                  this.burn(level, e, 3.0F * this.pw);
                  ticked.add(e);
               }
            }

            for (ArchonArsenal.Stone s : ArchonArsenal.stones(p)) {
               if (ArchonArsenal.segDist(s.pos, from, end) > 1.5) {
                  this.refracted.remove(s);
               } else {
                  LivingEntity e = this.refracted.get(s);
                  if (e == null || !e.isAlive() || e.isRemoved() || t % 10 == 0) {
                     e = ArchonArsenal.nearestFoe(level, p, s.pos, 12.0);
                     if (e == null) {
                        this.refracted.remove(s);
                        continue;
                     }

                     this.refracted.put(s, e);
                  }

                  Vfx.beam(level, s.pos, ArchonArsenal.center(e), ArchonArsenal.P, 0.25);
                  if (t % 2 == 0) {
                     Vfx.glint(level, s.pos, ArchonArsenal.CORE, 0.9);
                  }

                  if (pulse && ticked.add(e)) {
                     this.burn(level, e, 3.0F * this.pw);
                  }
               }
            }

            if (this.emp && !onBeam.isEmpty()) {
               LivingEntity first = null;
               double best = Double.MAX_VALUE;

               for (LivingEntity e : onBeam) {
                  double along = ArchonArsenal.center(e).subtract(from).dotProduct(look);
                  if (along < best) {
                     best = along;
                     first = e;
                  }
               }

               Vec3d split = from.add(look.multiply(Math.max(0.0, best)));
               Set<LivingEntity> prismed = new HashSet<>();

               for (int k = -1; k <= 1; k++) {
                  Vec3d dir = Vec3d.fromPolar(p.getPitch(), p.getYaw() + 15 * k);
                  Vec3d pe = ArchonArsenal.clip(level, p, split, dir, 10.0);
                  Vfx.beam(level, split, pe, ArchonArsenal.P, 0.25);
                  if (pulse) {
                     for (LivingEntity e : ArchonArsenal.onSegment(level, p, split, pe, 0.85)) {
                        if (e != first && prismed.add(e)) {
                           ArchonArsenal.pulseHit(level, p, e, 2.0F * this.pw);
                        }
                     }
                  }
               }

               if (t % 2 == 0) {
                  Vfx.glint(level, split, ArchonArsenal.CORE, 0.8);
               }
            }

            if (t >= this.len) {
               Vfx.flare(level, GearKit.tip(p), ArchonArsenal.GLOW, 1.6, 6);
               return true;
            } else {
               return false;
            }
         } else {
            Vfx.flare(level, GearKit.tip(p), ArchonArsenal.GLOW, 1.6, 6);
            return true;
         }
      }

      private void burn(ServerWorld level, LivingEntity e, float dmg) {
         ArchonArsenal.pulseHit(level, this.p, e, dmg);
         this.u.resonate(1.0F);
         Vfx.sparks(level, ArchonArsenal.center(e), ArchonArsenal.CORE, 3, 0.2, 0.14);
      }
   }

   private static final class Gravity implements ArchonArsenal.Task {
      private final PlayerEntity p;
      private final GearUse u;
      private final Vec3d at;
      private final double r;
      private final float pw;
      private final boolean emp;
      private List<LivingEntity> held = List.of();
      private boolean lifted;
      private int crushedAt = -1;

      Gravity(PlayerEntity p, GearUse u, Vec3d at, double r, float pw, boolean emp) {
         this.p = p;
         this.u = u;
         this.at = at;
         this.r = r;
         this.pw = pw;
         this.emp = emp;
      }

      @Override
      public boolean tick(ServerWorld level, int t) {
         if (ArchonArsenal.gone(this.p, level)) {
            return true;
         }

         if (!this.lifted) {
            if (t >= 14) {
               this.lift(level);
            }

            return false;
         } else if (this.crushedAt < 0) {
            if (t >= 28) {
               this.crush(level);
               this.crushedAt = t;
               return false;
            }

            for (LivingEntity e : this.held) {
               if (e.isAlive() && !e.isRemoved()) {
                  Vec3d c = ArchonArsenal.center(e);
                  if (!ArchonArsenal.isBoss(e)) {
                     e.setVelocity(0.0, Math.max(0.0, e.getVelocity().y) * 0.5 + 0.03, 0.0);
                     e.knockedBack = true;
                  }

                  if (t % 2 == 0) {
                     Vfx.beam(level, this.at.add(0.0, 0.2, 0.0), c, ArchonArsenal.P, 0.12);
                  }

                  if (t % 4 == 0) {
                     Vfx.ring(level, e.getEntityPos(), ArchonArsenal.GLOW, 0.8, 5);
                     Vfx.glint(level, c, ArchonArsenal.CORE, 0.5);
                  }
               }
            }

            return false;
         } else {
            if (t == this.crushedAt + 1) {
               for (LivingEntity e : this.held) {
                  if (e.isAlive()) {
                     Vfx.meteorTrail(level, ArchonArsenal.center(e), new Vec3d(0.0, -2.8, 0.0), ArchonArsenal.P);
                  }
               }
            }

            if (t < this.crushedAt + 2) {
               return false;
            }

            for (LivingEntity e : this.held) {
               Vfx.impact(level, e.getEntityPos(), ArchonArsenal.P, 0.8);
            }

            return true;
         }
      }

      private void lift(ServerWorld level) {
         this.lifted = true;
         this.held = ArchonArsenal.disc(level, this.p, this.at, this.r, -1.0, 6.0);

         for (LivingEntity e : this.held) {
            if (ArchonArsenal.isBoss(e)) {
               e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1), this.p);
            } else {
               e.setVelocity(0.0, 1.05, 0.0);
               e.knockedBack = true;
            }

            ArchonArsenal.hit(level, this.p, e, 6.0F * this.pw, false);
            Vfx.streaks(level, e.getEntityPos(), ArchonArsenal.UP, ArchonArsenal.P, 5);
            Vfx.ring(level, e.getEntityPos(), ArchonArsenal.GLOW, 0.9, 12);
         }

         Vfx.pillar(level, this.at, ArchonArsenal.P, 5.0, this.r * 0.8);
         Vfx.vortex(level, this.at, this.r, 5.0, ArchonArsenal.P, 16);
         ArchonArsenal.sfx(level, this.at, 0.8F, 0.8F, ArchonArsenal.GLYPH);
      }

      private void crush(ServerWorld level) {
         for (LivingEntity e : this.held) {
            if (e.isAlive() && !e.isRemoved()) {
               if (!ArchonArsenal.isBoss(e)) {
                  e.setVelocity(0.0, -2.8, 0.0);
                  e.knockedBack = true;
               }

               ArchonArsenal.hit(level, this.p, e, 14.0F * this.pw, false);
               e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 2), this.p);
               if (this.emp) {
                  e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 1), this.p);
               }

               this.u.resonate(2.0F);
               Vfx.meteorTrail(level, ArchonArsenal.center(e), new Vec3d(0.0, -2.8, 0.0), ArchonArsenal.P);
            }
         }

         Vfx.heavyImpact(level, this.at, ArchonArsenal.P, 1.3);
         Vfx.groundCrack(level, this.at, this.r, ArchonArsenal.P);
         ArchonArsenal.sfx(level, this.at, 1.4F, 1.0F, ArchonArsenal.CRUSH);
      }
   }

   private static final class Job {
      final ServerWorld level;
      final UUID owner;
      final long start;
      final ArchonArsenal.Task task;
      boolean done;

      Job(ServerWorld level, UUID owner, long start, ArchonArsenal.Task task) {
         this.level = level;
         this.owner = owner;
         this.start = start;
         this.task = task;
      }
   }

   private static final class Lattice implements ArchonArsenal.Task {
      private final PlayerEntity p;
      private final Vec3d c;
      private final Vec3d[] v;
      private final float amp;
      private boolean ignited;
      private int pulses;

      Lattice(PlayerEntity p, Vec3d c, Vec3d[] v, float amp) {
         this.p = p;
         this.c = c;
         this.v = v;
         this.amp = amp;
      }

      @Override
      public boolean tick(ServerWorld level, int t) {
         if (ArchonArsenal.gone(this.p, level)) {
            return true;
         }

         if (!this.ignited) {
            if (t >= 20) {
               this.ignite(level);
            }

            return false;
         } else {
            if (t >= 30 + 10 * this.pulses) {
               this.pulse(level);
               if (++this.pulses >= 6) {
                  this.unmake(level);
                  return true;
               }
            }

            return false;
         }
      }

      private void ignite(ServerWorld level) {
         this.ignited = true;

         for (int[] e : ArchonArsenal.STAR) {
            Vfx.beam(level, this.v[e[0]].add(0.0, 1.0, 0.0), this.v[e[1]].add(0.0, 1.0, 0.0), ArchonArsenal.P, 0.35, 60);
         }

         Vfx.nova(level, this.c.add(0.0, 0.5, 0.0), ArchonArsenal.P, 6.5);
         Vfx.radialFlash(level, this.c, 16.0, ArchonArsenal.GLOW, 0.5F);

         for (LivingEntity e : ArchonArsenal.disc(level, this.p, this.c, 6.5, -1.0, 4.0)) {
            if (ArchonArsenal.inPolygonXZ(e.getEntityPos(), this.v)) {
               ArchonArsenal.hit(level, this.p, e, 14.0F * this.amp, false);
               e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), this.p);
            }
         }

         ArchonArsenal.sfx(level, this.c, 1.6F, 1.0F, ArchonArsenal.LATTICE);
      }

      private void pulse(ServerWorld level) {
         Set<LivingEntity> burned = new HashSet<>();

         for (int[] e : ArchonArsenal.STAR) {
            for (LivingEntity x : ArchonArsenal.onSegment(level, this.p, this.v[e[0]].add(0.0, 1.0, 0.0), this.v[e[1]].add(0.0, 1.0, 0.0), 1.0)) {
               if (burned.add(x)) {
                  ArchonArsenal.pulseHit(level, this.p, x, 3.0F * this.amp);
                  Vfx.glint(level, ArchonArsenal.center(x), ArchonArsenal.CORE, 0.5);
               }
            }
         }

         int[] edge = ArchonArsenal.STAR[level.getRandom().nextInt(ArchonArsenal.STAR.length)];
         Vfx.lightning(level, this.v[edge[0]].add(0.0, 1.2, 0.0), this.v[edge[1]].add(0.0, 1.2, 0.0), ArchonArsenal.P, 1);
      }

      private void unmake(ServerWorld level) {
         for (Vec3d q : this.v) {
            Vfx.heavyImpact(level, q, ArchonArsenal.P, 1.0);

            for (LivingEntity e : ArchonArsenal.disc(level, this.p, q, 3.0, -1.0, 4.0)) {
               ArchonArsenal.hit(level, this.p, e, 10.0F * this.amp, false);
               ArchonArsenal.shove(e, q, 0.7, 0.35);
            }
         }

         Vfx.heavyImpact(level, this.c, ArchonArsenal.P, 1.6);
         Vfx.pillar(level, this.c, ArchonArsenal.P, 8.0, 1.6);
         ArchonArsenal.sfx(level, this.c, 2.0F, 0.9F, ArchonArsenal.UNMAKING);
      }
   }

   @FunctionalInterface
   private interface OnShot {
      void at(ArchonArsenal.Shot var1, @Nullable LivingEntity var2, Vec3d var3);
   }

   private static final class Runic {
      ServerWorld level;
      final List<ArchonArsenal.Stone> stones = new ArrayList<>();
      int blades;
      long bladeUntil;
      int bladeTurn;
      int edgeHits;
      int sparkHits;
      long lastCombat = -2305843009213693952L;
      long lastHurt = -2305843009213693952L;
      final long touched;

      Runic(ServerWorld level, long now) {
         this.level = level;
         this.touched = now;
      }
   }

   private record Seg(Vec3d a, Vec3d b, double len) {
   }

   private static final class Shot {
      final ServerWorld level;
      final PlayerEntity owner;
      final int kind;
      final int life;
      Vec3d pos;
      Vec3d vel;
      double speed;
      double maxSpeed;
      double accel;
      double radius;
      double homing;
      int homingFrom;
      int age;
      @Nullable LivingEntity target;
      @Nullable Vec3d aimAt;
      ArchonArsenal.@Nullable OnShot onHit;

      Shot(ServerWorld level, PlayerEntity owner, Vec3d pos, Vec3d vel, int life, double radius, int kind) {
         this.level = level;
         this.owner = owner;
         this.pos = pos;
         this.vel = vel;
         this.life = life;
         this.radius = radius;
         this.kind = kind;
         this.speed = vel.length();
         this.maxSpeed = this.speed;
      }
   }

   private static final class Stone {
      final Vec3d ground;
      final Vec3d pos;
      final boolean planted;
      final long born;
      final int life;
      final int phase;
      long lastArc = -2305843009213693952L;

      Stone(Vec3d ground, boolean planted, long born, int life, int phase) {
         this.ground = ground;
         this.pos = ground.add(0.0, planted ? 1.1 : 1.2, 0.0);
         this.planted = planted;
         this.born = born;
         this.life = life;
         this.phase = phase;
      }
   }

   @FunctionalInterface
   private interface Task {
      boolean tick(ServerWorld var1, int var2);
   }

   private static final class Verdict implements ArchonArsenal.Task {
      private final PlayerEntity p;
      private final GearUse u;
      private final float pw;
      private final int n;
      private final boolean finale;
      private final @Nullable LivingEntity target;
      private final Vec3d aim;
      private final List<Vec3d> stuck = new ArrayList<>();
      private int launched;
      private int landed;
      private int doneAt = -1;
      private @Nullable Vec3d centre;

      Verdict(PlayerEntity p, GearUse u, float pw, int n, boolean finale, @Nullable LivingEntity target, Vec3d aim) {
         this.p = p;
         this.u = u;
         this.pw = pw;
         this.n = n;
         this.finale = finale;
         this.target = target;
         this.aim = aim;
      }

      @Override
      public boolean tick(ServerWorld level, int t) {
         if (ArchonArsenal.gone(this.p, level)) {
            return true;
         }

         if (this.launched < this.n) {
            if (t >= 3 * this.launched) {
               this.launch(level, this.launched++);
            }

            if (t % 2 == 0) {
               for (int k = this.launched; k < this.n; k++) {
                  Vec3d s = ArchonArsenal.haloSlot(this.p, this.n, k);
                  Vfx.beam(level, s.add(0.0, 0.5, 0.0), s.add(0.0, -0.6, 0.0), ArchonArsenal.P, 0.16);
               }
            }
         }

         if (this.launched >= this.n && this.doneAt < 0 && (this.landed >= this.n || t > 3 * this.n + 45)) {
            if (!this.finale) {
               return true;
            }

            this.doneAt = t;
         }

         if (this.doneAt >= 0) {
            if (this.centre == null && t >= this.doneAt + 4) {
               Vec3d at = this.target != null && this.target.isAlive() ? this.target.getEntityPos() : this.aim;
               this.centre = ArchonArsenal.ground(level, at);
               Vfx.telegraphCircle(level, this.centre, 4.0, 8, ArchonArsenal.P);
               Vfx.runeCircle(level, this.centre, 4.0, ArchonArsenal.P, 16, 0.14F, Vfx.Rune.ARCANE);
            } else if (this.centre != null && t >= this.doneAt + 12) {
               this.collapse(level, this.centre);
               return true;
            }
         }

         return false;
      }

      private void launch(ServerWorld level, int k) {
         Vec3d from = ArchonArsenal.haloSlot(this.p, this.n, k);
         Vec3d to = this.target != null && this.target.isAlive() ? ArchonArsenal.center(this.target) : this.aim;
         Vec3d d = to.subtract(from);
         Vec3d dir = d.lengthSquared() > 1.0E-4 ? d.normalize() : this.p.getRotationVector();
         ArchonArsenal.Shot s = ArchonArsenal.shot(level, this.p, from, dir.multiply(1.6), 40, 0.6, 1);
         if (s == null) {
            this.landed++;
         } else {
            s.onHit = (sh, e, x) -> this.land(level, x);
            Vfx.flare(level, from, ArchonArsenal.GLOW, 0.9, 4);
            ArchonArsenal.sfx(level, from, 0.7F, 1.05F + 0.05F * k, ArchonArsenal.LAUNCH);
         }
      }

      private void land(ServerWorld level, Vec3d x) {
         for (LivingEntity e : ArchonArsenal.foes(level, this.p, x, 1.8)) {
            ArchonArsenal.hit(level, this.p, e, 7.0F * this.pw, false);
            this.u.resonate(1.0F);
         }

         Vfx.impact(level, x, ArchonArsenal.P, 0.7);
         Vfx.shards(level, x, ArchonArsenal.CORE, 6, 0.4);
         Vfx.beam(level, x.add(0.0, 1.0, 0.0), x.add(0.0, -0.1, 0.0), ArchonArsenal.P, 0.14, 20);
         ArchonArsenal.sfx(level, x, 0.8F, 1.0F, ArchonArsenal.IMPACT);
         this.stuck.add(x);
         this.landed++;
      }

      private void collapse(ServerWorld level, Vec3d c) {
         Vec3d focus = c.add(0.0, 1.5, 0.0);

         for (Vec3d x : this.stuck) {
            Vfx.beam(level, x, focus, ArchonArsenal.P, 0.3, 6);
         }

         Vfx.heavyImpact(level, c, ArchonArsenal.P, 1.6);
         Vfx.pillar(level, c, ArchonArsenal.P, 6.0, 1.4);

         for (LivingEntity e : ArchonArsenal.disc(level, this.p, c, 4.0, -1.0, 4.0)) {
            ArchonArsenal.hit(level, this.p, e, 14.0F * this.pw, false);
            ArchonArsenal.shove(e, c, 0.8, 0.4);
         }

         ArchonArsenal.sfx(level, c, 1.6F, 1.0F, ArchonArsenal.VERDICT);
         ArchonArsenal.stone(level, this.p, c, true);
      }
   }
}

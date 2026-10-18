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
import com.vexira.mysticalterrain.gear.WeaponKit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class HollowWardenArsenal extends Arsenal {
   private static final Vfx.Palette SHADOW = Vfx.Palette.SHADOW;
   private static final Vfx.Palette SOUL = new Vfx.Palette(15138815, 6086911, 1318956).withDebris(Vfx.Debris.ASH);
   private static final Vfx.Palette VIOLET = new Vfx.Palette(16181503, 10514687, 1708080).withDebris(Vfx.Debris.ASH);
   private static final Identifier WARDS = MysticalTerrain.id("hollow_soul_wards");
   private static final int SENTENCE_MAX = 5;
   private static final int SENTENCE_TICKS = 120;
   private static final double KEY_STAB = 2.2;
   private static final double KEY_REACH = 6.0;
   private static final double[] SPREAD = new double[]{-1.0, 1.0, 0.0, -1.0, 1.0};
   private static final List<HollowWardenArsenal.Task> TASKS = new ArrayList<>();
   private static final List<HollowWardenArsenal.Mote> MOTES = new ArrayList<>();
   private static final Map<LivingEntity, HollowWardenArsenal.Mark> MARKS = new WeakHashMap<>();
   private static final Map<UUID, HollowWardenArsenal.Warden> WARDENS = new HashMap<>();
   private static final Map<UUID, HollowWardenArsenal.Keeper> KEEPERS = new HashMap<>();
   private static boolean hooked;

   public HollowWardenArsenal() {
      super("hollow_warden", "hollow_shroud", Vfx.Palette.SHADOW);
      hook();
   }

   @Override
   protected void weapons() {
      this.weapon("soulreaper_scythe", 10.0F, -2.9F)
         .primary(Active.of(120, HollowWardenArsenal::reap).empowered(40, 1.4F))
         .alternate(Active.of(240, HollowWardenArsenal::gallowsHook).empowered(50, 1.4F))
         .charge(Charge.of(3, 14).onLevel(HollowWardenArsenal::verdictLevel).cast(Active.of(400, HollowWardenArsenal::headsmansVerdict).empowered(60, 1.3F)))
         .passive(HollowWardenArsenal::soulDrink)
         .resonancePerHit(12.0F);
      this.weapon("shadowstep_dagger", 6.0F, -1.6F)
         .primary(Active.of(100, HollowWardenArsenal::shadowstep).empowered(30, 1.3F))
         .alternate(Active.of(180, HollowWardenArsenal::turnTheKey).empowered(40, 1.4F))
         .charge(Charge.of(2, 10).onLevel(HollowWardenArsenal::passageLevel).cast(Active.of(280, HollowWardenArsenal::phantomPassage).empowered(50, 1.3F)))
         .passive(HollowWardenArsenal::backstab)
         .resonancePerHit(8.0F);
      this.weapon("lantern_of_souls", 4.0F, -2.4F)
         .primary(Active.of(80, HollowWardenArsenal::soulVolley).empowered(30, 1.3F))
         .alternate(Active.of(280, HollowWardenArsenal::penance).empowered(50, 1.4F))
         .charge(Charge.of(3, 12).onLevel(HollowWardenArsenal::pyreLevel).cast(Active.of(440, HollowWardenArsenal::soulPyre).empowered(60, 1.3F)))
         .passive(HollowWardenArsenal::soulBrand)
         .resonancePerHit(6.0F);
   }

   @Override
   protected void armor(ArmorKit set) {
      set.twoPiece(HollowWardenArsenal::gloomsight);
      set.fourPiece(HollowWardenArsenal::keeperTick);
      set.onHurt(HollowWardenArsenal::shroudHurt);
      set.active(560, HollowWardenArsenal::lifeSentence);
   }

   private static synchronized void hook() {
      if (!hooked) {
         hooked = true;
         ServerTickEvents.END_SERVER_TICK.register(HollowWardenArsenal::tickAll);
         ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> clearAll());
         ServerLivingEntityEvents.ALLOW_DAMAGE.register(HollowWardenArsenal::allowDamage);
         ServerLivingEntityEvents.AFTER_DEATH.register(HollowWardenArsenal::died);
      }
   }

   private static long now(ServerLevel level) {
      return level.getServer().getTickCount();
   }

   private static long now(Entity e) {
      return e.level() instanceof ServerLevel level ? now(level) : 0L;
   }

   static void later(ServerLevel level, int delay, Runnable run) {
      TASKS.add(new HollowWardenArsenal.Task(now(level) + Math.max(1, delay), run));
   }

   static void soon(ServerLevel level, int delay, Runnable run) {
      if (delay <= 0) {
         run.run();
      } else {
         later(level, delay, run);
      }
   }

   static void every(ServerLevel level, int delay, int period, int count, IntConsumer step) {
      for (int i = 0; i < count; i++) {
         int k = i;
         later(level, delay + i * period, () -> step.accept(k));
      }
   }

   static void track(ServerLevel level, int delay, IntPredicate step) {
      int[] t = new int[]{0};
      Runnable[] self = new Runnable[1];
      self[0] = () -> {
         if (step.test(t[0]++)) {
            later(level, 1, self[0]);
         }
      };
      later(level, delay, self[0]);
   }

   private static void clearAll() {
      TASKS.clear();
      MOTES.clear();
      MARKS.clear();
      WARDENS.clear();
      KEEPERS.clear();
   }

   private static void tickAll(MinecraftServer server) {
      long now = server.getTickCount();
      if (!TASKS.isEmpty()) {
         List<HollowWardenArsenal.Task> due = new ArrayList<>();
         Iterator<HollowWardenArsenal.Task> it = TASKS.iterator();

         while (it.hasNext()) {
            HollowWardenArsenal.Task t = it.next();
            if (t.due() <= now) {
               due.add(t);
               it.remove();
            }
         }

         for (HollowWardenArsenal.Task t : due) {
            try {
               t.run().run();
            } catch (RuntimeException e) {
               MysticalTerrain.LOGGER.error("Hollow Warden gear effect failed", e);
            }
         }
      }

      if (!MOTES.isEmpty()) {
         List<HollowWardenArsenal.Mote> flying = new ArrayList<>(MOTES);
         MOTES.clear();

         for (HollowWardenArsenal.Mote m : flying) {
            try {
               if (m.tick()) {
                  MOTES.add(m);
               }
            } catch (RuntimeException e) {
               MysticalTerrain.LOGGER.error("Hollow Warden soul mote failed", e);
            }
         }
      }

      upkeepMarks(now);
      upkeepPlayers(server, now);
   }

   private static void upkeepMarks(long now) {
      if (!MARKS.isEmpty()) {
         List<LivingEntity> ended = new ArrayList<>();
         List<LivingEntity> refresh = new ArrayList<>();
         List<LivingEntity> burning = new ArrayList<>();
         Iterator<Entry<LivingEntity, HollowWardenArsenal.Mark>> it = MARKS.entrySet().iterator();

         while (it.hasNext()) {
            Entry<LivingEntity, HollowWardenArsenal.Mark> en = it.next();
            LivingEntity t = en.getKey();
            HollowWardenArsenal.Mark m = en.getValue();
            if (t != null && t.isAlive() && !t.isRemoved()) {
               if (m.stacks > 0 && now >= m.expires) {
                  m.stacks = 0;
               }

               if (m.condemnedUntil > 0L) {
                  if (now >= m.condemnedUntil) {
                     ended.add(t);
                  } else if (now > m.condemnedAt && (now - m.condemnedAt) % 10L == 0L) {
                     refresh.add(t);
                  }
               }

               if (m.burnUntil >= now && now >= m.burnNext) {
                  burning.add(t);
               }

               if (m.idle(now)) {
                  it.remove();
               }
            } else {
               it.remove();
            }
         }

         for (LivingEntity t : ended) {
            HollowWardenArsenal.Mark m = MARKS.get(t);
            if (m != null && t.level() instanceof ServerLevel level) {
               m.condemnedUntil = 0L;
               m.paroledUntil = now + (heavy(t) ? 160 : 60);
               Vfx.shards(level, center(t), SOUL.core(), 5, 0.25);
               Vfx.smoke(level, center(t), SOUL.smoke(), 3, 0.3, 0.8);
            }
         }

         if (!refresh.isEmpty()) {
            refresh.sort(Comparator.comparingDouble(tx -> {
               HollowWardenArsenal.Mark mx = MARKS.get(tx);
               Player by = mx == null ? null : mx.by;
               return by != null && by.level() == tx.level() ? by.distanceToSqr(tx) : 1.0E9;
            }));
            Map<Player, Integer> per = new HashMap<>();

            for (LivingEntity t : refresh) {
               HollowWardenArsenal.Mark m = MARKS.get(t);
               if (m != null && t.level() instanceof ServerLevel level && per.merge(m.by, 1, Integer::sum) <= 8) {
                  lockChains(level, t, m);
               }
            }
         }

         for (LivingEntity t : burning) {
            HollowWardenArsenal.Mark m = MARKS.get(t);
            if (m != null && t.level() instanceof ServerLevel level && t.isAlive()) {
               m.burnNext = now + 20L;
               Player by = m.burnBy;
               if (by != null && alive(by, level)) {
                  soft(level, by, t, amp(t));
                  Vfx.embers(level, center(t), SOUL.glow(), 3, 0.3);
               }
            }
         }
      }
   }

   private static void upkeepPlayers(MinecraftServer server, long now) {
      Iterator<Entry<UUID, HollowWardenArsenal.Warden>> it = WARDENS.entrySet().iterator();

      while (it.hasNext()) {
         Entry<UUID, HollowWardenArsenal.Warden> en = it.next();
         HollowWardenArsenal.Warden w = en.getValue();
         ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
         if (p == null) {
            it.remove();
         } else {
            if (w.invisUntil != 0L && now >= w.invisUntil) {
               w.invisUntil = 0L;
               if (!w.gloomed) {
                  dropShroud(p);
               }
            }

            if (w.gloomed && now - w.gloomSeen > 2L) {
               w.gloomed = false;
               w.sneaking = false;
               if (w.invisUntil <= now) {
                  dropShroud(p);
               }
            }
         }
      }

      Iterator<Entry<UUID, HollowWardenArsenal.Keeper>> keeperIt = KEEPERS.entrySet().iterator();

      while (keeperIt.hasNext()) {
         Entry<UUID, HollowWardenArsenal.Keeper> en = keeperIt.next();
         if (now - en.getValue().seen > 2L) {
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p != null) {
               wardCap(p, 0);
            }

            keeperIt.remove();
         }
      }
   }

   private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
      if (entity instanceof Player p && !WARDENS.isEmpty() && p.level() instanceof ServerLevel level) {
         HollowWardenArsenal.Warden w = WARDENS.get(p.getUUID());
         if (w != null && w.passageUntil > now(level) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
         }
      }

      return true;
   }

   private static void died(LivingEntity e, DamageSource source) {
      if (e.level() instanceof ServerLevel level) {
         if (e instanceof Player p) {
            KEEPERS.remove(p.getUUID());
         } else {
            HollowWardenArsenal.Mark m = MARKS.remove(e);
            if (m != null && m.condemnedUntil > now(level)) {
               Player by = m.by;
               Vec3 c = center(e);
               if (by != null && alive(by, level) && by.position().distanceTo(c) <= 32.0) {
                  Vfx.shards(level, c, SOUL.core(), 8, 0.35);
                  Vfx.glint(level, top(e), SOUL.core(), 1.1);
                  freeSoul(level, by, c);
               }
            }
         }
      }
   }

   private static HollowWardenArsenal.Mark mark(LivingEntity t) {
      return MARKS.computeIfAbsent(t, k -> new HollowWardenArsenal.Mark());
   }

   private static boolean condemned(LivingEntity t) {
      HollowWardenArsenal.Mark m = MARKS.get(t);
      return m != null && m.condemnedUntil > now(t);
   }

   private static float amp(LivingEntity t) {
      return condemned(t) ? 1.25F : 1.0F;
   }

   static int sentence(ServerLevel level, @Nullable Player by, LivingEntity t, int n) {
      if (n > 0 && foe(t) && t != by) {
         long now = now(level);
         HollowWardenArsenal.Mark m = mark(t);
         if (m.condemnedUntil <= now && m.paroledUntil <= now) {
            m.stacks = Math.min(5, m.stacks + n);
            m.expires = now + 120L;
            if (by != null) {
               m.by = by;
            }

            if (m.stacks >= 5) {
               condemn(level, by, t);
               return 5;
            }

            Vfx.ring(level, under(level, t), SHADOW.glow(), 0.45 + 0.12 * m.stacks + t.getBbWidth() * 0.5, 10);
            if (m.stacks >= 3) {
               Vfx.sparks(level, center(t), SOUL.glow(), m.stacks, 0.2, 0.3);
            }

            return m.stacks;
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }

   static void condemn(ServerLevel level, @Nullable Player by, LivingEntity t) {
      if (foe(t)) {
         long now = now(level);
         HollowWardenArsenal.Mark m = mark(t);
         boolean heavy = heavy(t);
         int d = heavy ? 60 : 100;
         m.stacks = 0;
         m.condemnedAt = now;
         m.condemnedUntil = now + d;
         m.paroledUntil = 0L;
         m.turn = 0;
         if (by != null) {
            m.by = by;
         }

         t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, d, heavy ? 0 : 1), by);
         Vec3 g = under(level, t);
         Vec3 c = center(t);
         m.circle = g;
         Vfx.runeCircle(level, g, 1.0 + 0.3 * t.getBbWidth(), SHADOW, d, 0.04F, Vfx.Rune.GLYPH);
         Vfx.emit(
            level, VfxOptions.of(VfxParticles.Kind.RUNE, SOUL.core(), 1.5, 8).variant(Vfx.Rune.GLYPH.ordinal()).face().curve(4).build(), top(t), Vec3.ZERO
         );
         Vfx.shards(level, c, SHADOW.core(), 8, 0.3);
         Vfx.flare(level, c, SOUL.glow(), 1.4 + t.getBbWidth(), 6);
         lockChains(level, t, m);
         soundAt(level, c, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.1F, "weapon.lantern_of_souls.condemn", "weapon.lantern_of_souls.proc");
      }
   }

   private static void lockChains(ServerLevel level, LivingEntity t, HollowWardenArsenal.Mark m) {
      Vec3 c = center(t);
      Vec3 g = under(level, t);
      double r = 1.6 + t.getBbWidth() * 0.5;
      double base = m.turn++ * 40.0;

      for (int j = 0; j < 3; j++) {
         double a = Math.toRadians(base + j * 120);
         Vfx.beam(level, c, ground(level, g.add(Math.cos(a) * r, 0.0, Math.sin(a) * r)), SOUL, 0.12, 12);
      }

      Vfx.emit(level, VfxOptions.of(VfxParticles.Kind.RUNE, SOUL.glow(), 0.9, 12).variant(Vfx.Rune.GLYPH.ordinal()).face().curve(3).build(), top(t), Vec3.ZERO);
      if (flatDist(g, m.circle) > 1.2) {
         Vfx.ring(level, g, SHADOW.glow(), 0.8 + 0.3 * t.getBbWidth(), 12);
      }
   }

   static boolean reapSoul(ServerLevel level, Player p, LivingEntity t, GearUse u) {
      long now = now(level);
      HollowWardenArsenal.Mark m = MARKS.get(t);
      if (m != null && m.condemnedUntil > now && t.isAlive()) {
         boolean heavy = heavy(t);
         m.condemnedUntil = 0L;
         m.paroledUntil = now + (heavy ? 160 : 60);
         m.stacks = 0;
         float bonus = Math.min(heavy ? 8.0F : 24.0F, 0.2F * (t.getMaxHealth() - t.getHealth()));
         Vec3 c = center(t);
         rehit(t);
         hex(u, t, 12.0F + bonus);
         Vfx.slashArc(level, c.add(0.0, 1.2, 0.0), p.getYRot(), 0.0F, 90.0F, 2.2, SOUL, 140.0F);
         Vfx.nova(level, c, SOUL, 1.6);
         Vfx.pillar(level, under(level, t), SOUL, 3.0, 0.6);
         Vfx.glint(level, top(t), SOUL.core(), 1.2);
         if (!t.isAlive()) {
            Vfx.impactFrame(level, c, 12.0, SOUL.core(), 0.45F);
         }

         soundAt(level, c, 1.2F, 0.95F + level.getRandom().nextFloat() * 0.1F, "weapon.soulreaper_scythe.reap", "weapon.soulreaper_scythe.proc");
         freeSoul(level, p, c);
         return true;
      } else {
         return false;
      }
   }

   private static void freeSoul(ServerLevel level, Player p, Vec3 from) {
      Vfx.dashTrail(level, from, chest(p), SOUL);
      Vfx.wisps(level, chest(p), SOUL, 1, 0.8, 30);
      p.heal(3.0F);
      HollowWardenArsenal.Keeper k = KEEPERS.get(p.getUUID());
      if (k != null && now(level) - k.seen <= 2L) {
         addWard(level, p, k);
      }
   }

   static boolean reap(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float yaw = p.getYRot();
      float pitch = p.getXRot() * 0.3F;
      Vec3 look = GearKit.flatLook(p);
      Vec3 origin = GearKit.eye(p).add(0.0, -0.35, 0.0);
      int n = 0;

      for (LivingEntity e : reapTargets(u, p, look)) {
         hit(u, e, 12.0F);
         sentence(level, p, e, 1);
         u.resonate(2.0F);
         if (e.isAlive() && !heavy(e)) {
            Vec3 v = cap(p.position().add(look.scale(1.8)).subtract(e.position()).scale(0.35), 0.9);
            e.setDeltaMovement(v.x, v.y + 0.1, v.z);
            e.syncVelocity = true;
         }

         Vec3 c = center(e);
         Vfx.impact(level, c, SHADOW, 0.5);
         Vfx.beam(level, c, chest(p), SOUL, 0.12);
         n++;
      }

      if (n > 0) {
         p.heal(Math.min(4, n));
         Vfx.wisps(level, chest(p), SOUL, Math.min(n, 4), 0.9, 30);
      }

      Vfx.slashArc(level, origin, yaw, pitch, -15.0F, 5.5, SHADOW, 220.0F);
      later(level, 2, () -> {
         if (alive(p, level)) {
            Vfx.slashArc(level, origin, yaw, pitch, -10.0F, 5.0, SOUL, 200.0F);
         }
      });
      Vfx.streaks(level, chest(p), look, SOUL, 6);
      GearKit.kick(level, p, 0.2);
      if (u.empowered()) {
         GearKit.empowerFlourish(level, p, SHADOW);
         later(level, 6, () -> returnCut(u));
      }

      return true;
   }

   private static void returnCut(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      if (alive(p, level)) {
         Vfx.slashArc(level, GearKit.eye(p).add(0.0, -0.35, 0.0), p.getYRot(), p.getXRot() * 0.3F, 15.0F, 5.5, SHADOW, -220.0F);
         Vfx.nova(level, chest(p), SOUL, 3.0);

         for (LivingEntity e : reapTargets(u, p, GearKit.flatLook(p))) {
            reapSoul(level, p, e, u);
            if (e.isAlive()) {
               rehit(e);
               hit(u, e, 12.0F);
               sentence(level, p, e, 1);
               Vfx.impact(level, center(e), SOUL, 0.5);
            }
         }

         soundAt(level, chest(p), 1.0F, 1.15F, "weapon.soulreaper_scythe.use");
         GearKit.kick(level, p, 0.2);
      }
   }

   private static List<LivingEntity> reapTargets(GearUse u, Player p, Vec3 look) {
      List<LivingEntity> out = new ArrayList<>();
      double cos = Math.cos(Math.toRadians(110.0));

      for (LivingEntity e : u.enemies(p.position(), 8.0)) {
         Vec3 to = e.position().subtract(p.position());
         double d = Math.sqrt(to.x * to.x + to.z * to.z);
         if (!(d > 5.5 + e.getBbWidth() / 2.0F)
            && !(e.getY() < p.getY() - 1.5)
            && !(e.getY() > p.getY() + 3.0)
            && (!(d > 0.8) || !((to.x * look.x + to.z * look.z) / d < cos))) {
            out.add(e);
         }
      }

      return out;
   }

   static boolean gallowsHook(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      LivingEntity e = GearKit.lookTarget(level, p, 16.0);
      if (e == null) {
         return false;
      }

      List<LivingEntity> bitten = new ArrayList<>();
      bitten.add(e);
      if (u.empowered()) {
         GearKit.empowerFlourish(level, p, SHADOW);

         for (LivingEntity x : hostiles(u, p, center(e), 6.0)) {
            if (bitten.size() >= 3) {
               break;
            }

            if (x != e) {
               bitten.add(x);
            }
         }

         List<Vec3> pts = new ArrayList<>();
         pts.add(GearKit.tip(p));

         for (LivingEntity x : bitten) {
            pts.add(center(x));
         }

         Vfx.chainLightning(level, pts, SOUL);
      }

      Vfx.beam(level, GearKit.tip(p), center(e), SOUL, 0.3, 4);
      Vfx.sparks(level, GearKit.tip(p), SOUL.glow(), 6, 0.3, 0.3);
      later(level, 2, () -> {
         if (alive(p, level)) {
            for (LivingEntity xxx : bitten) {
               if (xxx.isAlive()) {
                  hex(u, xxx, 6.0F);
                  sentence(level, p, xxx, 2);
                  Vfx.impact(level, center(xxx), SOUL, 0.7);
                  Vfx.glint(level, center(xxx), SOUL.core(), 1.0);
               }
            }

            LivingEntity main = null;

            for (LivingEntity xx : bitten) {
               if (xx.isAlive()) {
                  main = xx;
                  break;
               }
            }

            if (main != null) {
               haul(level, p, u, main, bitten);
            }
         }
      });
      return true;
   }

   private static void haul(ServerLevel level, Player p, GearUse u, LivingEntity main, List<LivingEntity> bitten) {
      Vec3 start = main.position();
      boolean[] self = new boolean[]{heavy(main)};
      double[] left = new double[]{-1.0};
      int[] pulled = new int[]{0};
      Vec3[] prev = new Vec3[]{p.position()};
      IntPredicate step = t -> {
         if (alive(p, level) && main.isAlive()) {
            Vec3 dest = p.position().add(GearKit.flatLook(p).scale(1.6));

            for (LivingEntity x : bitten) {
               if (x.isAlive() && !heavy(x) && (!self[0] || x != main)) {
                  Vec3 v = cap(dest.subtract(x.position()).scale(0.25), 1.6);
                  x.setDeltaMovement(v.x, v.y + (t == 0 ? 0.25 : 0.0), v.z);
                  x.syncVelocity = true;
                  Vfx.beam(level, GearKit.tip(p), center(x), SOUL, 0.25);
               }
            }

            if (self[0]) {
               Vec3 d = main.position().subtract(p.position());
               double gap = Math.max(0.0, d.length() - main.getBbWidth() / 2.0F);
               if (left[0] < 0.0) {
                  left[0] = Math.max(0.0, gap - 2.0);
               }

               if (!(gap < 2.5) && !(left[0] <= 0.05) && pulled[0] < 10) {
                  double s = Math.min(1.6, left[0]);
                  left[0] -= s;
                  Vec3 v = d.normalize().scale(s);
                  p.setDeltaMovement(v.x, v.y + (pulled[0] == 0 ? 0.25 : 0.0), v.z);
                  p.syncVelocity = true;
                  p.resetFallDistance();
                  Vfx.dashTrail(level, prev[0].add(0.0, 1.0, 0.0), p.position().add(0.0, 1.0, 0.0), SOUL);
                  Vfx.beam(level, GearKit.tip(p), center(main), SOUL, 0.25);
                  prev[0] = p.position();
                  pulled[0]++;
                  return true;
               }

               p.setDeltaMovement(p.getDeltaMovement().scale(0.15));
               p.syncVelocity = true;
               cushion(level, p);
               if (pulled[0] < 10 || gap < 6.0) {
                  gallowsSlam(level, p, u, main);
               }

               return false;
            } else if (!(flatDist(main.position(), p.position()) < 2.2) && t < 6) {
               if (t == 3 && main.position().distanceTo(start) < 1.0) {
                  self[0] = true;
               }

               return true;
            } else {
               gallowsSlam(level, p, u, main);
               return false;
            }
         } else {
            return false;
         }
      };
      if (step.test(0)) {
         track(level, 1, t -> step.test(t + 1));
      }
   }

   private static void gallowsSlam(ServerLevel level, Player p, GearUse u, LivingEntity main) {
      Vec3 g = under(level, main);
      List<LivingEntity> victims = new ArrayList<>();
      victims.add(main);
      if (u.empowered()) {
         for (LivingEntity x : inCircle(u, g, 3.0)) {
            if (x != main) {
               victims.add(x);
            }
         }
      }

      for (LivingEntity x : victims) {
         if (x.isAlive()) {
            rehit(x);
            hit(u, x, 12.0F);
            if (x.isAlive()) {
               if (heavy(x)) {
                  slow(x, p, 0, 30);
               } else {
                  Vec3 v = x.getDeltaMovement();
                  x.setDeltaMovement(v.x * 0.2, -1.2, v.z * 0.2);
                  x.syncVelocity = true;
                  slow(x, p, 1, 40);
               }
            }
         }
      }

      Vfx.slashArc(level, GearKit.eye(p).add(0.0, -0.3, 0.0), p.getYRot(), 20.0F, 90.0F, 3.5, SHADOW, 160.0F);
      if (u.empowered()) {
         Vfx.heavyImpact(level, g, SHADOW, 1.2);
         Vfx.runeCircle(level, g, 2.5, SOUL, 30, 0.08F, Vfx.Rune.GLYPH);
         Vfx.shockwave(level, g, SHADOW, 3.0);
      } else {
         Vfx.heavyImpact(level, g, SHADOW, 0.8);
         Vfx.groundCrack(level, g, 2.2, SHADOW);
      }

      soundAt(level, g, 1.3F, 0.9F, "weapon.soulreaper_scythe.slam", "weapon.soulreaper_scythe.alt");
      GearKit.kick(level, p, 0.3);
   }

   static void verdictLevel(ServerLevel level, Player p, WeaponKit kit, int c, boolean max) {
      Vec3 feet = p.position();
      Vec3 look = GearKit.flatLook(p);
      Vec3 g = ground(level, feet);
      Vfx.telegraphLine(level, ground(level, feet.add(look)), look, 6 + 2 * c, 2.4, 8, SHADOW);
      Vfx.gather(level, GearKit.tip(p), SOUL, 1.4, 6);
      Vfx.flare(level, GearKit.tip(p), SOUL.glow(), 0.8 + 0.3 * c, 6);
      Vfx.ring(level, g, SHADOW.glow(), 1.0 + 0.4 * c, 10);
      if (max) {
         Vfx.chargeUp(level, GearKit.tip(p), SOUL, 10);
         Vfx.runeCircle(level, g, 2.2, SHADOW, 40, 0.05F, Vfx.Rune.ASTRAL);
         Vfx.shake(level, feet, 6.0, 0.15, 6);
      }
   }

   static boolean headsmansVerdict(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      int lv = Mth.clamp(u.charge(), 1, 3);
      float yaw = p.getYRot();
      Vec3 feet = p.position();
      double len = 6 + 2 * lv;
      float[] offsets = u.empowered() ? new float[]{0.0F, 20.0F, -20.0F} : new float[]{0.0F};
      List<Vec3> starts = new ArrayList<>();
      List<Vec3> dirs = new ArrayList<>();

      for (float o : offsets) {
         Vec3 d = Vec3.directionFromRotation(0.0F, yaw + o);
         starts.add(ground(level, feet.add(d)));
         dirs.add(d);
      }

      Vfx.slashArc(level, GearKit.eye(p).add(0.0, -0.2, 0.0), yaw, 0.0F, 90.0F, 4.0, SHADOW, 170.0F);
      if (u.empowered()) {
         GearKit.empowerFlourish(level, p, SHADOW);
      }

      bladeFall(level, p, u, starts, dirs, yaw, offsets, len, lv, false);
      if (lv >= 3) {
         later(level, 10, () -> {
            if (alive(p, level)) {
               bladeFall(level, p, u, starts, dirs, yaw, offsets, len, lv, true);
            }
         });
      }

      return true;
   }

   private static void bladeFall(
      ServerLevel level, Player p, GearUse u, List<Vec3> starts, List<Vec3> dirs, float yaw, float[] offsets, double len, int lv, boolean again
   ) {
      Vfx.Palette pal = again ? VIOLET : SOUL;
      double s = again ? 0.7 : 1.0;

      for (int i = 0; i < starts.size(); i++) {
         Vec3 st = starts.get(i);
         Vec3 d = dirs.get(i);
         Vfx.telegraphLine(level, st, d, len, 2.4, 4, again ? VIOLET : SHADOW);
         Vfx.slashArc(level, st.add(d.scale(len / 2.0)).add(0.0, 2.2, 0.0), yaw + offsets[i], 0.0F, 90.0F, len / 2.0 + 0.5, pal, 150.0F);
      }

      later(level, 4, () -> {
         if (alive(p, level)) {
            Set<LivingEntity> struck = new HashSet<>();
            Vec3 first = null;

            for (int ix = 0; ix < starts.size(); ix++) {
               Vec3 stx = starts.get(ix);
               Vec3 dx = dirs.get(ix);

               for (double k = 0.0; k <= len + 0.01; k += 2.0) {
                  Vec3 q = ground(level, stx.add(dx.scale(k)));
                  Vfx.beam(level, q.add(0.0, 7.0 * s, 0.0), q, pal, 0.7 * s, 4);
                  Vfx.streaks(level, q.add(0.0, 3.0, 0.0), new Vec3(0.0, -1.0, 0.0), pal, 4);
               }

               for (double k = 1.5; k <= len; k += 3.0) {
                  Vfx.groundCrack(level, ground(level, stx.add(dx.scale(k))), 1.4 * s, SHADOW);
               }

               Vec3 mid = ground(level, stx.add(dx.scale(len / 2.0)));
               double power = (0.6 + 0.3 * lv) * s;
               Vfx.heavyImpact(level, mid, SHADOW, ix == 0 ? power : Math.min(1.2, power));
               if (ix == 0) {
                  first = mid;
                  if (lv >= 3 && !again) {
                     Vfx.impactFrame(level, mid, 16.0, SOUL.core(), 0.6F);
                  }
               }

               for (LivingEntity e : lineFoes(u, stx, dx, len, 1.2)) {
                  if (struck.add(e)) {
                     boolean reaped = reapSoul(level, p, e, u);
                     if (e.isAlive()) {
                        if (reaped || again) {
                           rehit(e);
                        }

                        hit(u, e, again ? 16.0F : 14 + 6 * lv);
                        if (!again) {
                           sentence(level, p, e, 2);
                        }

                        if (e.isAlive()) {
                           if (heavy(e)) {
                              slow(e, p, 0, 30);
                           } else {
                              Vec3 v = e.getDeltaMovement();
                              e.setDeltaMovement(v.x * 0.3, -0.6, v.z * 0.3);
                              e.syncVelocity = true;
                              slow(e, p, 2, 30);
                           }
                        }
                     }
                  }
               }
            }

            if (first != null) {
               soundAt(level, first, 1.4F, again ? 1.0F : 0.85F, "weapon.soulreaper_scythe.slam", "weapon.soulreaper_scythe.release");
            }

            GearKit.kick(level, p, 0.25 + 0.08 * lv);
         }
      });
   }

   static void soulDrink(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (!(t instanceof ArmorStand)) {
         a.heal(condemned(t) ? 2.5F : 1.0F);
         Vfx.beam(level, center(t), a.position().add(0.0, 1.2, 0.0), SOUL, 0.12);
         GearKit.hitSpark(level, t, SHADOW);
         if (a instanceof Player p && foe(t)) {
            long now = now(level);
            HollowWardenArsenal.Mark m = mark(t);
            if (now - m.drinkLast > 80L) {
               m.drinkHits = 0;
            }

            m.drinkLast = now;
            if (++m.drinkHits % 2 == 0) {
               sentence(level, p, t, 1);
               GearKit.proc(level, a, SOUL);
               GearKit.procSound(level, t, "soulreaper_scythe");
            }
         }
      }
   }

   static boolean shadowstep(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      LivingEntity e = markTarget(u, p, 18.0);
      if (e == null) {
         return false;
      }

      Vec3 from = p.position();
      if (!stepBehind(level, p, e)) {
         return false;
      }

      long now = now(level);
      stepOut(level, p, from);
      stepCut(level, p, u, e);
      p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, true, false, true));
      HollowWardenArsenal.Warden w = warden(p);
      w.invisUntil = now + 30L;
      w.shroudUntil = now + 60L;
      if (u.empowered()) {
         GearKit.empowerFlourish(level, p, SHADOW);
         Set<LivingEntity> cut = new HashSet<>();
         cut.add(e);
         LivingEntity[] last = new LivingEntity[]{e};

         for (int k = 1; k <= 2; k++) {
            later(level, 6 * k, () -> {
               if (alive(p, level)) {
                  LivingEntity next = null;

                  for (LivingEntity x : hostiles(u, p, center(last[0]), 8.0)) {
                     if (!cut.contains(x)) {
                        next = x;
                        break;
                     }
                  }

                  if (next != null) {
                     Vec3 at = p.position();
                     if (stepBehind(level, p, next)) {
                        cut.add(next);
                        last[0] = next;
                        stepOut(level, p, at);
                        stepCut(level, p, u, next);
                     }
                  }
               }
            });
         }
      }

      return true;
   }

   private static @Nullable LivingEntity markTarget(GearUse u, Player p, double range) {
      Vec3 eye = GearKit.eye(p);
      Vec3 look = p.getLookAngle();
      Vec3 end = eye.add(look.scale(range));
      LivingEntity direct = null;
      double directD = Double.MAX_VALUE;
      LivingEntity near = null;
      double nearScore = Double.MAX_VALUE;

      for (LivingEntity e : u.enemies(eye.add(look.scale(range / 2.0)), range / 2.0 + 3.0)) {
         AABB box = e.getBoundingBox().inflate(0.2);
         Optional<Vec3> through = box.contains(eye) ? Optional.of(eye) : box.clip(eye, end);
         if (through.isPresent()) {
            double d = through.get().distanceToSqr(eye);
            if (d < directD && p.hasLineOfSight(e)) {
               direct = e;
               directD = d;
            }
         } else {
            double along = center(e).subtract(eye).dot(look);
            if (hostile(e, p) && !(along < 0.5) && !(along > range)) {
               double miss = Math.sqrt(box.distanceToSqr(eye.add(look.scale(along))));
               double score = miss / along;
               if (miss <= 0.5 + 0.18 * along && score < nearScore && p.hasLineOfSight(e)) {
                  near = e;
                  nearScore = score;
               }
            }
         }
      }

      return direct != null ? direct : near;
   }

   private static boolean stepBehind(ServerLevel level, Player p, LivingEntity e) {
      Vec3 base = e.position();
      Vec3 c = center(e);
      double back = Math.max(1.2, e.getBbWidth() / 2.0F + 0.8);
      Vec3 behind = Vec3.directionFromRotation(0.0F, e.getYRot()).scale(-1.0);
      Vec3 far = flat(base.subtract(p.position()));
      if (far.lengthSqr() < 1.0E-6) {
         far = behind;
      }

      Vec3[] dirs = new Vec3[]{
         behind, far, behind.yRot(0.8F), behind.yRot(-0.8F), far.yRot(1.0F), far.yRot(-1.0F), behind.yRot(1.57F), behind.yRot(-1.57F)
      };

      for (Vec3 d : dirs) {
         for (double dy : new double[]{0.0, 0.5, 1.0}) {
            Vec3 at = base.add(d.x * back, dy, d.z * back);
            if (level.noCollision(p, p.getBoundingBox().move(at.subtract(p.position())))
               && level.clip(new ClipContext(c, at.add(0.0, 1.0, 0.0), Block.COLLIDER, Fluid.NONE, p)).getType() == Type.MISS) {
               double dx = c.x - at.x;
               double dz = c.z - at.z;
               float yaw = (float)(Mth.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F;
               float pitch = (float)Mth.clamp(
                  -Mth.atan2(c.y - (at.y + p.getEyeHeight()), Math.sqrt(dx * dx + dz * dz)) * 180.0F / (float)Math.PI, -60.0, 60.0
               );
               p.teleportTo(level, at.x, at.y, at.z, Set.of(), yaw, pitch, false);
               cushion(level, p);
               return true;
            }
         }
      }

      return false;
   }

   private static void stepOut(ServerLevel level, Player p, Vec3 from) {
      Vfx.dashTrail(level, from.add(0.0, 1.0, 0.0), p.position().add(0.0, 1.0, 0.0), SHADOW);
      Vfx.smoke(level, from.add(0.0, 0.6, 0.0), SHADOW.smoke(), 8, 0.5, 1.4);
      Vfx.flare(level, from.add(0.0, 1.0, 0.0), SHADOW.glow(), 1.6, 6);
      Vfx.flare(level, chest(p), SOUL.glow(), 1.4, 6);
      Vfx.ring(level, ground(level, p.position()), SHADOW.glow(), 1.2, 8);
   }

   private static void stepCut(ServerLevel level, Player p, GearUse u, LivingEntity e) {
      hit(u, e, 7.0F);
      backstabBurst(level, p, e, u.power());
      Vfx.slashArc(level, chest(p), p.getYRot(), p.getXRot(), -35.0F, 2.6, SHADOW, 120.0F);
      Vfx.glint(level, center(e), SHADOW.core(), 1.0);
   }

   static boolean turnTheKey(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      LivingEntity e = keyTarget(u, p);
      if (e == null) {
         return false;
      }

      double gap = gap(p, e);
      if (gap > 2.2 && !lunge(level, p, e) && gap > 3.0) {
         return false;
      }

      hit(u, e, 6.0F);
      Vfx.slashArc(level, chest(p), p.getYRot(), p.getXRot(), 0.0F, 2.2, SHADOW, 60.0F);
      Vfx.streaks(level, chest(p), p.getLookAngle(), SHADOW, 4);
      if (u.empowered()) {
         GearKit.empowerFlourish(level, p, SHADOW);
      }

      later(level, 5, () -> {
         if (alive(p, level) && e.isAlive()) {
            HollowWardenArsenal.Mark m = MARKS.get(e);
            boolean doomed = condemned(e);
            int n = doomed ? 5 : (m == null ? 0 : m.stacks);
            if (m != null) {
               m.stacks = 0;
            }

            Vec3 c = center(e);
            Vec3 g = under(level, e);
            rehit(e);
            if (n >= 1) {
               hex(u, e, 4 * n);
            } else {
               hex(u, e, 3.0F);
               sentence(level, p, e, 2);
            }

            if (doomed) {
               reapSoul(level, p, e, u);
            }

            Vfx.runeCircle(level, g, 1.0 + 0.2 * n, SOUL, 24, 0.12F, Vfx.Rune.GLYPH);
            Vfx.glint(level, c, SOUL.core(), 0.8 + 0.15 * n);
            Vfx.shards(level, c, SHADOW.core(), 4 + 2 * n, 0.4);
            if (n >= 3) {
               Vfx.nova(level, c, SHADOW, 1.5);
            }

            soundAt(level, c, 1.1F, 1.0F + 0.04F * n, "weapon.shadowstep_dagger.unlock", "weapon.shadowstep_dagger.proc");
            if (n >= 3) {
               u.state().cooldown("shadowstep_dagger.primary", 1);
               p.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1));
               Vfx.flare(level, chest(p), SHADOW.glow(), 1.2, 5);
            }

            if (u.empowered()) {
               float burst = n == 0 ? 3.0F : 2 * n;

               for (LivingEntity x : u.enemies(e.position(), 5.0)) {
                  if (x != e && !(x.position().distanceTo(e.position()) > 3.5 + x.getBbWidth() / 2.0F)) {
                     hex(u, x, burst);
                     sentence(level, p, x, 2);
                  }
               }

               Vfx.shockwave(level, g, SOUL, 3.5);
            }
         }
      });
      return true;
   }

   private static @Nullable LivingEntity keyTarget(GearUse u, Player p) {
      LivingEntity e = markTarget(u, p, 7.5);
      if (e != null && gap(p, e) <= 6.0) {
         return e;
      }

      Vec3 eye = GearKit.eye(p);
      Vec3 look = p.getLookAngle();
      LivingEntity best = null;
      double bestGap = Double.MAX_VALUE;

      for (LivingEntity x : u.enemies(p.position(), 9.0)) {
         double g = gap(p, x);
         HollowWardenArsenal.Mark m = MARKS.get(x);
         boolean quarry = hostile(x, p) || m != null && m.stacks > 0 || condemned(x);
         Vec3 to = center(x).subtract(eye);
         if (!(g > (quarry ? 6.0 : 3.0)) && !(g >= bestGap) && !(to.lengthSqr() < 1.0E-4) && !(to.normalize().dot(look) < 0.5) && p.hasLineOfSight(x)) {
            best = x;
            bestGap = g;
         }
      }

      return best;
   }

   private static double gap(Player p, LivingEntity e) {
      return !(e.getY() > p.getEyeY() + 1.5) && !(e.getY() + e.getBbHeight() < p.getY() - 1.5)
         ? Math.max(0.0, flatDist(p.position(), e.position()) - e.getBbWidth() / 2.0F)
         : Double.MAX_VALUE;
   }

   private static boolean lunge(ServerLevel level, Player p, LivingEntity e) {
      Vec3 from = p.position();
      Vec3 dir = flat(e.position().subtract(from));
      if (dir.lengthSqr() < 1.0E-6) {
         return false;
      }

      double gap = gap(p, e);

      for (double stand : new double[]{0.9, 1.4, 1.9}) {
         double go = gap - stand;
         if (go < 0.3) {
            break;
         }

         for (double y : new double[]{e.getY(), from.y}) {
            Vec3 at = new Vec3(from.x + dir.x * go, y, from.z + dir.z * go);
            if (level.noCollision(p, p.getBoundingBox().move(at.subtract(from)))
               && level.clip(new ClipContext(chest(p), at.add(0.0, 1.2, 0.0), Block.COLLIDER, Fluid.NONE, p)).getType() == Type.MISS) {
               p.teleportTo(level, at.x, at.y, at.z, Relative.ROTATION, 0.0F, 0.0F, false);
               cushion(level, p);
               Vfx.dashTrail(level, from.add(0.0, 1.0, 0.0), at.add(0.0, 1.0, 0.0), SHADOW);
               Vfx.smoke(level, from.add(0.0, 0.6, 0.0), SHADOW.smoke(), 6, 0.4, 1.2);
               return true;
            }
         }
      }

      return false;
   }

   static void passageLevel(ServerLevel level, Player p, WeaponKit kit, int c, boolean max) {
      Vec3 feet = p.position();
      Vfx.telegraphLine(level, ground(level, feet), GearKit.flatLook(p), 6 + 3 * c, 1.6, 6, SHADOW);
      Vfx.smoke(level, feet.add(0.0, 0.3, 0.0), SHADOW.smoke(), 4, 0.4, 1.0);
      Vfx.flare(level, GearKit.tip(p), SHADOW.glow(), 0.7 + 0.3 * c, 6);
      if (max) {
         Vfx.chargeUp(level, chest(p), SHADOW, 8);
      }
   }

   static boolean phantomPassage(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      int lv = Mth.clamp(u.charge(), 1, 2);
      Vec3 dir = GearKit.flatLook(p);
      double reach = 6 + 3 * lv;
      reach = Math.min(
         clearance(level, p, p.position().add(0.0, 0.7, 0.0), dir, reach), clearance(level, p, GearKit.eye(p).add(0.0, -0.1, 0.0), dir, reach)
      );
      if (reach < 1.5) {
         return false;
      }

      double per = reach / 5.0;
      float yaw = p.getYRot();
      float dmg = 9 + 4 * lv;
      Vec3 start = p.position();
      warden(p).passageUntil = now(level) + 8L;
      Set<LivingEntity> cut = new HashSet<>();
      Vec3[] prev = new Vec3[]{start};
      Vfx.smoke(level, chest(p), SHADOW.smoke(), 8, 0.5, 1.3);
      Vfx.flare(level, chest(p), SHADOW.glow(), 1.6, 5);
      if (u.empowered()) {
         GearKit.empowerFlourish(level, p, SHADOW);
      }

      IntPredicate step = t -> {
         if (!alive(p, level)) {
            return false;
         }

         p.resetFallDistance();
         if (t < 5) {
            p.setDeltaMovement(dir.x * per, p.onGround() ? 0.05 : 0.0, dir.z * per);
            p.syncVelocity = true;
         } else if (t == 5) {
            p.setDeltaMovement(dir.x * 0.15, 0.0, dir.z * 0.15);
            p.syncVelocity = true;
         }

         Vec3 at = p.position();
         AABB box = p.getBoundingBox().inflate(1.0).expandTowards(prev[0].subtract(at));

         for (LivingEntity e : u.enemies(at, 6.0)) {
            if (e.getBoundingBox().intersects(box) && cut.add(e)) {
               hit(u, e, dmg);
               sentence(level, p, e, 1);
               if (e.isAlive() && !heavy(e)) {
                  e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0), p);
               }

               Vfx.slashArc(level, center(e), yaw + 90.0F, 0.0F, 0.0F, 1.8, SHADOW, 140.0F);
               Vfx.impact(level, center(e), SHADOW, 0.5);
            }
         }

         if (at.distanceToSqr(prev[0]) > 0.01) {
            Vfx.dashTrail(level, prev[0].add(0.0, 1.0, 0.0), at.add(0.0, 1.0, 0.0), SHADOW);
            Vfx.streaks(level, at.add(0.0, 1.0, 0.0), dir, SHADOW, 3);
         }

         prev[0] = at;
         if (t >= 7) {
            passageEnd(level, p, u, start, at, dir, lv, dmg);
            return false;
         } else {
            return true;
         }
      };
      if (step.test(0)) {
         track(level, 1, t -> step.test(t + 1));
      }

      return true;
   }

   private static void passageEnd(ServerLevel level, Player p, GearUse u, Vec3 start, Vec3 end, Vec3 dir, int lv, float dmg) {
      Vec3 path = flat(end.subtract(start));
      double len = flatDist(start, end);
      if (len < 0.5 || path.lengthSqr() < 1.0E-6) {
         path = dir;
         len = 0.5;
      }

      Vec3 pd = path;
      double length = len;
      cushion(level, p);
      Vfx.telegraphLine(level, ground(level, start), pd, length, 1.6, 8, SHADOW);
      if (lv >= 2) {
         for (int k = 0; k < 2; k++) {
            later(level, 2 + 4 * k, () -> phantom(level, p, u, start, end, dmg));
         }
      }

      later(level, 8, () -> {
         if (alive(p, level)) {
            for (double kx = 0.0; kx <= length + 0.01; kx += 1.5) {
               Vec3 q = ground(level, start.add(pd.scale(kx)));
               Vfx.pillar(level, q, SHADOW, 1.6, 0.4);
               Vfx.smoke(level, q, SHADOW.smoke(), 2, 0.4, 1.0);
            }

            Vec3 a = start.add(0.0, 0.5, 0.0);
            Vec3 b = end.add(0.0, 0.5, 0.0);

            for (LivingEntity e : segFoes(u, a, b, 1.5)) {
               rehit(e);
               hex(u, e, 6.0F);
               sentence(level, p, e, 1);
            }

            soundAt(level, start.lerp(end, 0.5), 1.1F, 1.0F, "weapon.shadowstep_dagger.trail", "weapon.shadowstep_dagger.use");
            if (u.empowered()) {
               Vec3 ca = start.add(0.0, 0.3, 0.0);
               Vec3 cb = end.add(0.0, 0.3, 0.0);
               Vfx.beam(level, ca, cb, SOUL, 0.3, 60);
               Vfx.telegraphLine(level, ground(level, start), pd, length, 1.6, 2, SOUL);
               every(level, 10, 10, 6, kxx -> {
                  if (alive(p, level)) {
                     for (LivingEntity e : segFoes(u, ca, cb, 1.5)) {
                        softHex(u, e, 3.0F);
                        slow(e, p, 0, 20);
                     }

                     Vfx.sparks(level, ca.lerp(cb, level.getRandom().nextDouble()), SOUL.glow(), 3, 0.15, 0.25);
                  }
               });
            }
         }
      });
   }

   private static void phantom(ServerLevel level, Player p, GearUse u, Vec3 start, Vec3 end, float dmg) {
      if (alive(p, level)) {
         Vec3 a = start.add(0.0, 1.0, 0.0);
         Vec3 b = end.add(0.0, 1.0, 0.0);
         Vfx.dashTrail(level, a, b, SOUL);

         for (int j = 0; j < 3; j++) {
            double f = (2 * j + 1) / 6.0;
            soon(level, j, () -> Vfx.flare(level, a.lerp(b, f), SOUL.glow(), 1.2, 4));
         }

         for (LivingEntity e : segFoes(u, a, b, 1.2)) {
            rehit(e);
            hit(u, e, 0.5F * dmg);
            if (e.isAlive() && !heavy(e)) {
               e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0), p);
            }
         }
      }
   }

   static void backstab(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof Player p && foe(t)) {
         long now = now(level);
         HollowWardenArsenal.Warden w = WARDENS.get(p.getUUID());
         boolean shroud = w != null && w.shroudUntil > now;
         Vec3 facing = Vec3.directionFromRotation(0.0F, t.getYRot());
         Vec3 to = a.position().subtract(t.position());
         double d = Math.sqrt(to.x * to.x + to.z * to.z);
         boolean behind = d > 0.001 && (facing.x * to.x + facing.z * to.z) / d < -0.35;
         if (!shroud && !behind) {
            GearKit.hitSpark(level, t, SHADOW);
         } else if (now - mark(t).backstabLast >= 10L) {
            if (shroud) {
               w.shroudUntil = 0L;
            }

            backstabBurst(level, p, t, 1.0F);
         }
      }
   }

   private static void backstabBurst(ServerLevel level, Player p, LivingEntity t, float power) {
      if (t.isAlive()) {
         mark(t).backstabLast = now(level);
         rehit(t);
         GearKit.magic(level, p, t, 6.0F * power * amp(t));
         sentence(level, p, t, 1);
         GearState.of(p).addResonance("shadowstep_dagger", 4.0F);
         if (t.isAlive() && !heavy(t)) {
            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0), p);
         }

         Vec3 c = center(t);
         Vfx.slashArc(level, c, p.getYRot(), p.getXRot(), 45.0F, 1.6, SHADOW, 100.0F);
         Vfx.slashArc(level, c, p.getYRot(), p.getXRot(), -45.0F, 1.6, SHADOW, 100.0F);
         Vfx.glint(level, c, SHADOW.core(), 0.9);
         GearKit.procSound(level, t, "shadowstep_dagger");
      }
   }

   static boolean soulVolley(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      LivingEntity target = GearKit.lookTarget(level, p, 32.0);
      Vec3 aim = target == null ? aimPoint(level, p, 24.0) : null;
      int count = u.empowered() ? 5 : 3;
      int pierce = u.empowered() ? 1 : 0;
      GearKit.muzzle(level, p, SOUL, 0.8);
      if (u.empowered()) {
         GearKit.empowerFlourish(level, p, SOUL);
      }

      for (int i = 0; i < count; i++) {
         int k = i;
         soon(level, 3 * i, () -> {
            if (alive(p, level)) {
               Vec3 look = p.getLookAngle();
               Vec3 side = look.cross(new Vec3(0.0, 1.0, 0.0));
               side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
               double sp = SPREAD[k];
               Vec3 vel = look.scale(0.35).add(side.scale(sp * 0.35)).add(0.0, sp == 0.0 ? 0.45 : 0.25, 0.0);
               Vec3 from = GearKit.tip(p);
               MOTES.add(new HollowWardenArsenal.Mote(u, from, vel, target, aim, pierce));
               Vfx.flare(level, from, SOUL.glow(), 1.0, 5);
               soundAt(level, from, 0.6F, 1.2F + 0.1F * k, "bolt.shadow.launch");
            }
         });
      }

      return true;
   }

   static boolean penance(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 g = ground(level, aimPoint(level, p, 28.0));
      double r = u.empowered() ? 3.6 : 2.8;
      Vfx.telegraphCircle(level, g, r, 10, SOUL);
      Vfx.runeCircle(level, g, r, SHADOW, 80, 0.03F, Vfx.Rune.GLYPH);
      Vfx.chargeUp(level, g.add(0.0, 4.0, 0.0), SOUL, 10);
      GearKit.muzzle(level, p, SOUL, 0.8);
      if (u.empowered()) {
         GearKit.empowerFlourish(level, p, SOUL);
      }

      later(level, 10, () -> {
         if (alive(p, level)) {
            cell(level, p, u, g, r);
         }
      });
      return true;
   }

   private static void cell(ServerLevel level, Player p, GearUse u, Vec3 g, double r) {
      Vfx.ward(level, g, r, SOUL, 60);

      for (int i = 0; i < 8; i++) {
         double a = i * Math.PI / 4.0;
         Vec3 rim = g.add(Math.cos(a) * r, 0.0, Math.sin(a) * r);
         Vfx.beam(level, rim.add(0.0, 3.5, 0.0), rim, SHADOW, 0.2, 60);
      }

      Vfx.heavyImpact(level, g, SHADOW, 0.7);
      soundAt(level, g, 1.3F, 0.9F, "weapon.lantern_of_souls.cell", "weapon.lantern_of_souls.alt");
      List<LivingEntity> prisoners = new ArrayList<>();

      for (LivingEntity e : inCircle(u, g, r)) {
         hex(u, e, 10.0F);
         sentence(level, p, e, 2);
         u.resonate(4.0F);
         if (e.isAlive()) {
            prisoners.add(e);
            slow(e, p, heavy(e) ? 1 : 2, heavy(e) ? 60 : 30);
         }
      }

      track(level, 1, t -> {
         if (!alive(p, level)) {
            return false;
         }

         for (LivingEntity exxx : prisoners) {
            if (exxx.isAlive() && exxx.level() == level && !heavy(exxx)) {
               double d = flatDist(exxx.position(), g);
               if (d > r - 0.4 && d < r + 8.0) {
                  Vec3 back = flat(g.subtract(exxx.position())).scale(0.25);
                  exxx.setDeltaMovement(back.x, exxx.getDeltaMovement().y, back.z);
                  exxx.syncVelocity = true;
               }

               if (t % 10 == 0) {
                  slow(exxx, p, 2, 30);
               }
            }
         }

         if (u.empowered() && t % 20 == 19) {
            int drained = 0;

            for (LivingEntity ex : prisoners) {
               if (ex.isAlive() && ex.level() == level) {
                  softHex(u, ex, 2.0F);
                  Vfx.beam(level, center(ex), chest(p), SOUL, 0.15);
                  drained++;
               }
            }

            if (drained > 0) {
               p.heal(Math.min(3, drained));
            }
         }

         if (t < 59) {
            return true;
         }

         Vfx.nova(level, g.add(0.0, 1.0, 0.0), SOUL, r);
         Vfx.shards(level, g.add(0.0, 1.0, 0.0), SOUL.core(), 12, 0.5);

         for (LivingEntity exx : inCircle(u, g, r)) {
            rehit(exx);
            hex(u, exx, 8.0F);
            sentence(level, p, exx, 1);
         }

         soundAt(level, g, 1.1F, 1.3F, "weapon.lantern_of_souls.cell", "weapon.lantern_of_souls.alt");
         return false;
      });
   }

   static void pyreLevel(ServerLevel level, Player p, WeaponKit kit, int c, boolean max) {
      Vfx.gather(level, GearKit.tip(p), SOUL, 2.0, 8);
      Vfx.wisps(level, chest(p), SOUL, c + 1, 1.4, 20);
      Vfx.flare(level, GearKit.tip(p), SOUL.glow(), 0.8 + 0.3 * c, 6);
      if (max) {
         Vfx.chargeUp(level, GearKit.tip(p), SOUL, 10);
         Vfx.runeCircle(level, ground(level, p.position()), 2.0, SOUL, 30, 0.06F, Vfx.Rune.ARCANE);
      }
   }

   static boolean soulPyre(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      int lv = Mth.clamp(u.charge(), 1, 3);
      int n = 2 + lv + (u.empowered() ? 2 : 0);
      Vec3 c = ground(level, aimPoint(level, p, 28.0));
      float yaw = p.getYRot();
      List<Vec3> spots = new ArrayList<>();

      for (LivingEntity e : hostiles(u, p, c, 8.0)) {
         if (spots.size() >= n) {
            break;
         }

         Vec3 q = under(level, e);
         if (far(spots, q, 1.0)) {
            spots.add(q);
         }
      }

      for (int pass = 0; pass < 2 && spots.size() < n; pass++) {
         double radius = pass == 0 ? 3.0 : 5.5;
         int k = Math.max(1, n - spots.size());

         for (int j = 0; j < 2 * k && spots.size() < n; j++) {
            float a = yaw + j % k * 360.0F / k + (j >= k ? 180.0F / k : 0.0F);
            Vec3 q = ground(level, c.add(Vec3.directionFromRotation(0.0F, a).scale(radius)));
            if (far(spots, q, 2.5)) {
               spots.add(q);
            }
         }
      }

      if (spots.isEmpty()) {
         spots.add(c);
      }

      if (u.empowered()) {
         GearKit.empowerFlourish(level, p, SOUL);
      }

      int zone = u.empowered() ? 120 : 80;
      float dmg = 12 + 3 * lv;

      for (int i = 0; i < spots.size(); i++) {
         Vec3 q = spots.get(i);
         int k = i;
         soon(level, 3 * i, () -> {
            if (alive(p, level)) {
               Vfx.telegraphCircle(level, q, 1.8, 12, SOUL);
               Vfx.beam(level, GearKit.tip(p), q.add(0.0, 1.0, 0.0), SOUL, 0.2, 3);
               later(level, 12, () -> pyre(level, p, u, q, k, dmg, zone));
            }
         });
      }

      if (lv >= 3 && spots.size() >= 2) {
         later(level, 3 * (spots.size() - 1) + 16, () -> {
            if (alive(p, level)) {
               List<Vec3> pts = new ArrayList<>();

               for (int ixx = 0; ixx < Math.min(8, spots.size()); ixx++) {
                  pts.add(spots.get(ixx).add(0.0, 1.2, 0.0));
               }

               Vfx.chainLightning(level, pts, SOUL);
               Set<LivingEntity> struck = new HashSet<>();

               for (int ix = 0; ix + 1 < pts.size(); ix++) {
                  for (LivingEntity e : segFoes(u, pts.get(ix), pts.get(ix + 1), 1.2)) {
                     if (struck.add(e)) {
                        rehit(e);
                        hex(u, e, 6.0F);
                        sentence(level, p, e, 1);
                     }
                  }
               }

               soundAt(level, c, 1.2F, 1.0F, "weapon.lantern_of_souls.condemn", "weapon.lantern_of_souls.proc");
            }
         });
      }

      return true;
   }

   private static void pyre(ServerLevel level, Player p, GearUse u, Vec3 q, int i, float dmg, int zone) {
      if (alive(p, level)) {
         Vfx.pillar(level, q, SOUL, 6.0, 1.1);
         Vfx.nova(level, q.add(0.0, 0.5, 0.0), SOUL, 1.8);
         Vfx.embers(level, q.add(0.0, 0.5, 0.0), SOUL.glow(), 10, 1.2);
         Vfx.groundCrack(level, q, 1.8, SOUL);

         for (LivingEntity e : inCircle(u, q, 1.8)) {
            rehit(e);
            hex(u, e, dmg);
            sentence(level, p, e, 2);
            u.resonate(2.0F);
         }

         soundAt(level, q, 0.9F, 0.9F + 0.05F * i, "weapon.lantern_of_souls.pyre", "bolt.shadow.impact");
         every(level, 10, 10, zone / 10, z -> {
            if (alive(p, level)) {
               Vfx.embers(level, q.add(0.0, 0.2, 0.0), SOUL.glow(), 5, 1.4);
               Vfx.smoke(level, q, SOUL.smoke(), 2, 1.2, 0.8);
               if (z % 2 == 1) {
                  for (LivingEntity ex : inCircle(u, q, 1.8)) {
                     softHex(u, ex, 3.0F);
                     slow(ex, p, 0, 30);
                  }
               }
            }
         });
      }
   }

   static void soulBrand(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (!(t instanceof ArmorStand)) {
         GearKit.hitSpark(level, t, SOUL);
         Vfx.embers(level, center(t), SOUL.glow(), 4, 0.3);
         if (a instanceof Player p && foe(t)) {
            sentence(level, p, t, 1);
            long now = now(level);
            HollowWardenArsenal.Mark m = mark(t);
            boolean running = m.burnUntil >= now;
            m.burnUntil = now + 60L;
            m.burnBy = p;
            if (!running) {
               m.burnNext = now + 20L;
               GearKit.procSound(level, t, "lantern_of_souls");
            }
         }
      }
   }

   static void gloomsight(ServerLevel level, Player p, int pieces) {
      long now = now(level);
      HollowWardenArsenal.Warden w = warden(p);
      w.gloomSeen = now;
      if (level.getGameTime() % 20L == 0L) {
         p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
      }

      if (p.hasEffect(MobEffects.DARKNESS)) {
         p.removeEffect(MobEffects.DARKNESS);
      }

      if (p.hasEffect(MobEffects.BLINDNESS)) {
         p.removeEffect(MobEffects.BLINDNESS);
      }

      boolean sneak = p.isShiftKeyDown() && !p.isSpectator();
      if (sneak) {
         if (!w.sneaking) {
            Vfx.smoke(level, chest(p), SHADOW.smoke(), 6, 0.4, 1.2);
            Vfx.flare(level, chest(p), SHADOW.glow(), 0.8, 4);
         }

         MobEffectInstance inv = p.getEffect(MobEffects.INVISIBILITY);
         if (inv == null || inv.getDuration() < 40 && !inv.isVisible()) {
            GearKit.keep(p, MobEffects.INVISIBILITY, 0);
         }

         w.gloomed = true;
         if (now % 20L == 0L) {
            Vfx.smoke(level, p.position().add(0.0, 0.2, 0.0), SHADOW.smoke(), 1, 0.3, 0.8);
         }
      } else if (w.gloomed) {
         w.gloomed = false;
         if (w.invisUntil <= now) {
            dropShroud(p);
         }
      }

      w.sneaking = sneak;
   }

   static void keeperTick(ServerLevel level, Player p, int pieces) {
      long now = now(level);
      HollowWardenArsenal.Keeper k = KEEPERS.computeIfAbsent(p.getUUID(), id -> new HollowWardenArsenal.Keeper());
      k.seen = now;
      if (!k.souls.isEmpty()) {
         boolean changed = false;
         Iterator<Long> it = k.souls.iterator();

         while (it.hasNext()) {
            if (it.next() <= now) {
               int left = k.souls.size() - 1;
               float abs = p.getAbsorptionAmount();
               if (abs > 4 * left) {
                  p.setAbsorptionAmount(Math.max(4 * left, abs - 4.0F));
               }

               it.remove();
               changed = true;
            }
         }

         if (changed) {
            wardCap(p, k.souls.size());
            Vfx.smoke(level, chest(p), SOUL.smoke(), 3, 0.3, 0.8);
         }

         if (now % 40L == 0L && !k.souls.isEmpty()) {
            Vfx.wisps(level, chest(p), SOUL, k.souls.size(), 0.9, 42);
         }
      }
   }

   static void shroudHurt(ServerLevel level, Player p, @Nullable LivingEntity attacker, DamageSource source, float taken, int pieces) {
      if (pieces >= 4) {
         long now = now(level);
         boolean foeHit = attacker != null && attacker != p && foe(attacker);
         if (foeHit) {
            HollowWardenArsenal.Mark m = mark(attacker);
            if (now - m.keeperLast >= 20L) {
               m.keeperLast = now;
               sentence(level, p, attacker, 1);
               Vfx.beam(level, chest(p), center(attacker), SOUL, 0.12);
            }
         }

         HollowWardenArsenal.Keeper k = KEEPERS.get(p.getUUID());
         if (k != null && !k.souls.isEmpty()) {
            int keep = Math.max(0, (int)Math.ceil(p.getAbsorptionAmount() / 4.0F - 0.01F));

            int burst;
            for (burst = 0; k.souls.size() > keep; burst++) {
               int soonest = 0;

               for (int i = 1; i < k.souls.size(); i++) {
                  if (k.souls.get(i) < k.souls.get(soonest)) {
                     soonest = i;
                  }
               }

               k.souls.remove(soonest);
            }

            if (burst != 0) {
               wardCap(p, k.souls.size());
               Vec3 c = chest(p);
               Vfx.nova(level, c, SOUL, 2.0);
               Vfx.shards(level, c, SOUL.core(), 8 * burst, 0.4);
               if (foeHit && attacker.isAlive() && attacker.distanceTo(p) <= 6.0F) {
                  for (int i = 0; i < burst && attacker.isAlive(); i++) {
                     rehit(attacker);
                     GearKit.magic(level, p, attacker, 5.0F * amp(attacker));
                     sentence(level, p, attacker, 1);
                  }

                  Vfx.beam(level, c, center(attacker), SOUL, 0.2);
                  Vfx.impact(level, center(attacker), SOUL, 0.5);
               }

               GearKit.sound(level, p, 0.8F, 1.3F, "weapon.lantern_of_souls.proc");
            }
         }
      }
   }

   static boolean lifeSentence(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      List<LivingEntity> chained = new ArrayList<>();

      for (LivingEntity e : hostiles(u, p, p.position(), 12.0)) {
         if (chained.size() >= 6) {
            break;
         }

         if (p.hasLineOfSight(e)) {
            chained.add(e);
         }
      }

      if (chained.isEmpty()) {
         return false;
      }

      Vec3 feet = p.position();
      Vfx.runeCircle(level, ground(level, feet), 3.0, SOUL, 66, 0.05F, Vfx.Rune.GLYPH);
      Vfx.auraRing(level, feet, SHADOW, 1.6);
      Vfx.radialFlash(level, feet, 10.0, SHADOW.glow(), 0.35F);
      p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 70, 0));
      double[] leash = new double[chained.size()];

      for (int i = 0; i < chained.size(); i++) {
         LivingEntity e = chained.get(i);
         Vfx.beam(level, chest(p), center(e), SOUL, 0.3, 3);
         Vfx.impact(level, center(e), SOUL, 0.6);
         hex(u, e, 4.0F);
         slow(e, p, heavy(e) ? 0 : 1, 60);
         leash[i] = Math.max(2.5, p.distanceTo(e));
      }

      track(level, 1, t -> {
         int tick = t + 1;
         if (!alive(p, level)) {
            return false;
         }

         Vec3 me = chest(p);
         Vec3 dest = p.position().add(GearKit.flatLook(p).scale(1.5));

         for (int ix = 0; ix < chained.size(); ix++) {
            LivingEntity exxx = chained.get(ix);
            if (exxx.isAlive() && exxx.level() == level) {
               Vec3 c = center(exxx);
               if (tick <= 60 && tick % 2 == 0) {
                  Vfx.beam(level, me, c, SOUL, 0.3, 3);
               }

               boolean light = !heavy(exxx);
               if (tick < 60 && light && p.distanceTo(exxx) > leash[ix]) {
                  Vec3 back = flat(p.position().subtract(exxx.position())).scale(0.3);
                  exxx.setDeltaMovement(back.x, exxx.getDeltaMovement().y, back.z);
                  exxx.syncVelocity = true;
               }

               if (tick % 20 == 0 && tick <= 60) {
                  softHex(u, exxx, 2.0F);
                  Vfx.sparks(level, me.lerp(c, 0.5), SOUL.glow(), 4, 0.2, 0.3);
               }

               if (light && exxx.isAlive() && tick >= 60 && tick < 66 && flatDist(exxx.position(), dest) > 1.5) {
                  Vec3 v = cap(dest.subtract(exxx.position()).scale(0.22), tick == 60 ? 1.8 : 1.2);
                  exxx.setDeltaMovement(v.x, v.y + (tick == 60 ? 0.3 : 0.0), v.z);
                  exxx.syncVelocity = true;
                  if (tick == 60) {
                     Vfx.streaks(level, c, me.subtract(c).normalize(), SOUL, 6);
                  }
               }
            }
         }

         if (tick == 60) {
            soundAt(level, p.position(), 1.3F, 0.9F, "weapon.lantern_of_souls.condemn", "weapon.lantern_of_souls.alt");
         }

         if (tick < 66) {
            return true;
         }

         Vec3 f = p.position();
         Vec3 g = ground(level, f);
         Vfx.nova(level, chest(p), SHADOW, 5.0);
         Vfx.heavyImpact(level, g, SHADOW, 1.6);
         Vfx.runeCircle(level, g, 4.5, SOUL, 30, 0.1F, Vfx.Rune.GLYPH);

         for (LivingEntity exx : inCircle(u, g, 4.5)) {
            rehit(exx);
            hex(u, exx, 14.0F);
            if (exx.isAlive() && !heavy(exx)) {
               GearKit.knockAway(exx, f, 0.4, 0.2);
            }
         }

         for (LivingEntity ex : chained) {
            if (ex.isAlive() && ex.level() == level) {
               condemn(level, p, ex);
            }
         }

         soundAt(level, f, 1.4F, 0.8F, "weapon.soulreaper_scythe.slam", "weapon.soulreaper_scythe.alt");
         return false;
      });
      return true;
   }

   private static void addWard(ServerLevel level, Player p, HollowWardenArsenal.Keeper k) {
      long exp = now(level) + 600L;
      if (k.souls.size() < 3) {
         k.souls.add(exp);
      } else {
         int oldest = 0;

         for (int i = 1; i < k.souls.size(); i++) {
            if (k.souls.get(i) < k.souls.get(oldest)) {
               oldest = i;
            }
         }

         k.souls.set(oldest, exp);
      }

      wardCap(p, k.souls.size());
      p.setAbsorptionAmount(Math.max(p.getAbsorptionAmount(), 4.0F * k.souls.size()));
      Vfx.flare(level, chest(p), SOUL.glow(), 1.4, 6);
      Vfx.ring(level, p.position(), SOUL.glow(), 1.2, 10);
   }

   private static void wardCap(Player p, int souls) {
      AttributeInstance max = p.getAttribute(Attributes.MAX_ABSORPTION);
      if (max != null) {
         if (souls <= 0) {
            max.removeModifier(WARDS);
         } else {
            AttributeModifier cur = max.getModifier(WARDS);
            if (cur == null || cur.amount() != 4.0 * souls) {
               max.addOrUpdateTransientModifier(new AttributeModifier(WARDS, 4.0 * souls, Operation.ADD_VALUE));
            }
         }
      }
   }

   private static void dropShroud(Player p) {
      MobEffectInstance inv = p.getEffect(MobEffects.INVISIBILITY);
      if (inv != null && inv.getDuration() <= 70 && !inv.isVisible()) {
         p.removeEffect(MobEffects.INVISIBILITY);
      }
   }

   private static HollowWardenArsenal.Warden warden(Player p) {
      return WARDENS.computeIfAbsent(p.getUUID(), id -> new HollowWardenArsenal.Warden());
   }

   private static boolean alive(Player p, ServerLevel level) {
      return p.isAlive() && !p.isRemoved() && p.level() == level;
   }

   private static boolean heavy(LivingEntity e) {
      return e instanceof MysticalBoss b && b.isBoss() || e instanceof EnderDragon || e instanceof WitherBoss;
   }

   private static boolean foe(LivingEntity e) {
      return e.isAlive() && !(e instanceof Player) && !(e instanceof ArmorStand);
   }

   private static boolean hostile(LivingEntity e, Player p) {
      return e instanceof Enemy || e instanceof MysticalBoss || e instanceof EnderDragon || e instanceof Mob m && m.getTarget() == p;
   }

   private static List<LivingEntity> hostiles(GearUse u, Player p, Vec3 at, double r) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : u.enemies(at, r)) {
         if (hostile(e, p)) {
            out.add(e);
         }
      }

      out.sort(Comparator.comparingDouble(ex -> ex.distanceToSqr(at)));
      return out;
   }

   private static void rehit(LivingEntity e) {
      e.setInvulnerableTime(0);
   }

   private static void hit(GearUse u, LivingEntity e, float base) {
      if (e.isAlive()) {
         u.strike(e, base * amp(e));
      }
   }

   private static void hex(GearUse u, LivingEntity e, float base) {
      if (e.isAlive()) {
         u.magic(e, base * amp(e));
      }
   }

   private static void softHex(GearUse u, LivingEntity e, float base) {
      soft(u.level(), u.player(), e, base * u.power() * amp(e));
   }

   private static void soft(ServerLevel level, Player p, LivingEntity e, float dmg) {
      if (e.isAlive()) {
         int inv = e.getInvulnerableTime();
         Vec3 v = e.getDeltaMovement();
         e.setInvulnerableTime(0);
         GearKit.magic(level, p, e, dmg);
         if (e.isAlive()) {
            e.setInvulnerableTime(inv);
            e.setDeltaMovement(v);
         }
      }
   }

   private static void slow(LivingEntity e, Player p, int amplifier, int ticks) {
      if (e.isAlive()) {
         e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, amplifier), p);
      }
   }

   private static Vec3 ground(ServerLevel level, Vec3 v) {
      MutableBlockPos q = BlockPos.containing(v.x, v.y + 1.5, v.z).mutable();
      boolean open = empty(level, q);

      for (int i = 0; i < 16; i++) {
         q.move(Direction.DOWN);
         BlockState state = level.getBlockState(q);
         VoxelShape shape = state.getCollisionShape(level, q);
         if (!shape.isEmpty()) {
            if (open) {
               return new Vec3(v.x, q.getY() + shape.max(Axis.Y), v.z);
            }
         } else {
            open = true;
         }
      }

      return v;
   }

   private static boolean empty(ServerLevel level, BlockPos pos) {
      return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
   }

   private static Vec3 under(ServerLevel level, LivingEntity e) {
      Vec3 g = ground(level, e.position());
      return g.y < e.getY() - 2.5 ? e.position() : g;
   }

   private static Vec3 aimPoint(ServerLevel level, Player p, double range) {
      return GearKit.lookPoint(level, p, range).subtract(p.getLookAngle().scale(0.3));
   }

   private static double clearance(ServerLevel level, Player p, Vec3 from, Vec3 dir, double dist) {
      BlockHitResult hr = level.clip(new ClipContext(from, from.add(dir.scale(dist)), Block.COLLIDER, Fluid.NONE, p));
      return hr.getType() == Type.MISS ? dist : Math.max(0.0, hr.getLocation().distanceTo(from) - 0.6);
   }

   private static void cushion(ServerLevel level, Player p) {
      track(level, 1, t -> {
         if (!alive(p, level)) {
            return false;
         }

         p.resetFallDistance();
         return t < 40 && (t < 3 || !p.onGround());
      });
   }

   private static Vec3 chest(Player p) {
      return p.position().add(0.0, 1.2, 0.0);
   }

   private static Vec3 center(LivingEntity e) {
      return e.getBoundingBox().getCenter();
   }

   private static Vec3 top(LivingEntity e) {
      return e.position().add(0.0, e.getBbHeight() + 0.6, 0.0);
   }

   private static Vec3 flat(Vec3 v) {
      Vec3 f = new Vec3(v.x, 0.0, v.z);
      return f.lengthSqr() < 1.0E-6 ? Vec3.ZERO : f.normalize();
   }

   private static double flatDist(Vec3 a, Vec3 b) {
      double dx = a.x - b.x;
      double dz = a.z - b.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static Vec3 cap(Vec3 v, double max) {
      double l = v.length();
      return l > max ? v.scale(max / l) : v;
   }

   private static boolean far(List<Vec3> spots, Vec3 q, double min) {
      for (Vec3 s : spots) {
         if (flatDist(s, q) < min) {
            return false;
         }
      }

      return true;
   }

   private static List<LivingEntity> inCircle(GearUse u, Vec3 at, double r) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : u.enemies(at, r + 3.0)) {
         if (flatDist(e.position(), at) <= r + e.getBbWidth() / 2.0F && e.getY() > at.y - 1.5 && e.getY() < at.y + 3.5) {
            out.add(e);
         }
      }

      return out;
   }

   private static List<LivingEntity> lineFoes(GearUse u, Vec3 start, Vec3 dir, double len, double half) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : u.enemies(start.add(dir.scale(len / 2.0)), len / 2.0 + half + 3.0)) {
         Vec3 to = e.position().subtract(start);
         double along = to.x * dir.x + to.z * dir.z;
         if (!(along < -0.5) && !(along > len + 0.5)) {
            double px = to.x - dir.x * along;
            double pz = to.z - dir.z * along;
            if (!(Math.sqrt(px * px + pz * pz) > half + e.getBbWidth() / 2.0F) && !(e.getY() > start.y + 4.0) && !(e.getY() + e.getBbHeight() < start.y - 1.5)) {
               out.add(e);
            }
         }
      }

      return out;
   }

   private static List<LivingEntity> segFoes(GearUse u, Vec3 a, Vec3 b, double w) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : u.enemies(a.lerp(b, 0.5), a.distanceTo(b) / 2.0 + w + 3.0)) {
         AABB box = e.getBoundingBox().inflate(w);
         if (box.contains(a) || box.clip(a, b).isPresent()) {
            out.add(e);
         }
      }

      return out;
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

   private static final class Keeper {
      final List<Long> souls = new ArrayList<>();
      long seen;
   }

   private static final class Mark {
      int stacks;
      long expires;
      @Nullable Player by;
      long condemnedAt;
      long condemnedUntil;
      long paroledUntil;
      Vec3 circle = Vec3.ZERO;
      int turn;
      int drinkHits;
      long drinkLast = -1000L;
      long backstabLast = -1000L;
      long burnUntil = -1000L;
      long burnNext;
      @Nullable Player burnBy;
      long keeperLast = -1000L;

      boolean idle(long now) {
         return this.stacks == 0
            && this.condemnedUntil == 0L
            && this.paroledUntil < now
            && this.burnUntil < now
            && now - this.drinkLast > 80L
            && now - this.backstabLast > 10L
            && now - this.keeperLast > 20L;
      }
   }

   private static final class Mote {
      final ServerLevel level;
      final Player owner;
      final GearUse u;
      final Set<LivingEntity> struck = new HashSet<>();
      Vec3 pos;
      Vec3 vel;
      @Nullable LivingEntity target;
      @Nullable Vec3 aim;
      int age;
      int pierce;
      double travelled;

      Mote(GearUse u, Vec3 pos, Vec3 vel, @Nullable LivingEntity target, @Nullable Vec3 aim, int pierce) {
         this.level = u.level();
         this.owner = u.player();
         this.u = u;
         this.pos = pos;
         this.vel = vel;
         this.target = target;
         this.aim = aim;
         this.pierce = pierce;
      }

      boolean tick() {
         if (!HollowWardenArsenal.alive(this.owner, this.level)) {
            return false;
         }

         this.age++;
         double speed = Math.min(1.1, 0.5 + 0.08 * this.age);
         LivingEntity t = this.target;
         if (t != null && (!t.isAlive() || t.level() != this.level)) {
            t = null;
            this.target = null;
         }

         Vec3 goal = t != null ? HollowWardenArsenal.center(t) : this.aim;
         if (this.age > 4 && goal != null) {
            Vec3 want = goal.subtract(this.pos);
            if (t == null && want.lengthSqr() < 2.25) {
               this.aim = null;
            } else if (want.lengthSqr() > 1.0E-4) {
               this.vel = this.vel.lerp(want.normalize().scale(speed), 0.2);
            }
         }

         if (this.vel.lengthSqr() > 1.0E-6) {
            this.vel = this.vel.normalize().scale(speed);
         }

         Vec3 next = this.pos.add(this.vel);
         BlockHitResult block = this.level.clip(new ClipContext(this.pos, next, Block.COLLIDER, Fluid.NONE, this.owner));
         boolean wall = block.getType() != Type.MISS;
         Vec3 end = wall ? block.getLocation() : next;

         for (LivingEntity e : this.u.enemies(this.pos.lerp(end, 0.5), 3.5)) {
            if (!this.struck.contains(e)) {
               AABB box = e.getBoundingBox().inflate(0.6);
               if (box.contains(this.pos) || !box.clip(this.pos, end).isEmpty()) {
                  this.struck.add(e);
                  Vec3 at = box.clip(this.pos, end).orElse(HollowWardenArsenal.center(e));
                  HollowWardenArsenal.rehit(e);
                  HollowWardenArsenal.hex(this.u, e, 7.0F);
                  HollowWardenArsenal.sentence(this.level, this.owner, e, 1);
                  this.u.resonate(3.0F);
                  Vfx.impact(this.level, at, HollowWardenArsenal.SOUL, 0.5);
                  Vfx.glint(this.level, at, HollowWardenArsenal.SOUL.core(), 0.6);
                  HollowWardenArsenal.soundAt(this.level, at, 0.7F, 1.1F, "bolt.shadow.impact");
                  if (e == this.target) {
                     this.target = null;
                     this.aim = null;
                  }

                  if (this.pierce-- <= 0) {
                     return false;
                  }
               }
            }
         }

         if (wall) {
            Vfx.impact(this.level, end, HollowWardenArsenal.SOUL, 0.35);
            Vfx.embers(this.level, end, HollowWardenArsenal.SOUL.glow(), 4, 0.2);
            return false;
         }

         this.pos = next;
         this.travelled += speed;
         Vfx.trail(this.level, this.pos, HollowWardenArsenal.SOUL, 0.35);
         if (this.age % 2 == 0) {
            Vfx.embers(this.level, this.pos, HollowWardenArsenal.SOUL.glow(), 1, 0.05);
         }

         if (this.age < 40 && !(this.travelled > 36.0)) {
            return true;
         }

         Vfx.flare(this.level, this.pos, HollowWardenArsenal.SOUL.glow(), 0.8, 5);
         return false;
      }
   }

   private record Task(long due, Runnable run) {
   }

   private static final class Warden {
      long shroudUntil;
      long invisUntil;
      long passageUntil;
      long gloomSeen;
      boolean gloomed;
      boolean sneaking;
   }
}

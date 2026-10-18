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
import com.vexira.mysticalterrain.gear.MysticArmor;
import com.vexira.mysticalterrain.gear.MysticWeapon;
import com.vexira.mysticalterrain.gear.WeaponKit;
import java.util.ArrayList;
import java.util.Collections;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
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

public final class MycelordArsenal extends Arsenal {
   private static final Vfx.Palette SPORE = Vfx.Palette.SPORE;
   private static final Vfx.Palette ROT = new Vfx.Palette(15917311, 12602088, 2759214).withDebris(Vfx.Debris.ASH);
   private static final int ROT_GLOW = 12602088;
   private static final int ROT_SMOKE = 2759214;
   private static final int SHELF = 11039818;
   private static final int MAX_STACKS = 5;
   private static final String BLADE = "sporeblade";
   private static final String MAUL = "mycelial_maul";
   private static final String CASTER = "sporecaster";
   private static final String SET_KEY = "set.mycelord";
   private static final String SHELL_KEY = "mycelord.shell";
   private static final double COS_75 = Math.cos(Math.toRadians(75.0));
   private static final List<MycelordArsenal.Task> TASKS = new ArrayList<>();
   private static final List<MycelordArsenal.Live> LIVE = new ArrayList<>();
   private static boolean hooked;
   private static final Map<LivingEntity, MycelordArsenal.Infest> INFEST = new WeakHashMap<>();
   private static final List<MycelordArsenal.Burst> BURSTS = new ArrayList<>();
   private static final Map<UUID, Long> HEART = new HashMap<>();
   private static final Map<UUID, Long> HEALED = new HashMap<>();
   private static final Map<LivingEntity, Long> BREATHED = new WeakHashMap<>();
   private static @Nullable ServerLevel burstSoundLevel;
   private static long burstSoundTick = -1L;
   private static final List<MycelordArsenal.Patch> PATCHES = new ArrayList<>();
   private static final List<MycelordArsenal.Mine> MINES = new ArrayList<>();
   private static final Map<UUID, MycelordArsenal.Ring> RINGS = new HashMap<>();

   public MycelordArsenal() {
      super("mycelord", "mycelium_carapace", Vfx.Palette.SPORE);
      hook();
   }

   @Override
   protected void weapons() {
      this.weapon("sporeblade", 8.0F, -2.4F)
         .primary(Active.of(120, MycelordArsenal::gillstorm).empowered(40))
         .alternate(Active.of(200, MycelordArsenal::rhizomeDive).empowered(40))
         .charge(Charge.of(3, 10).perLevel(0.25F).pose(ItemUseAnimation.TRIDENT).onLevel(MycelordArsenal::rotdrawLevel))
         .passive(MycelordArsenal::infestation)
         .resonancePerHit(10.0F);
      this.weapon("mycelial_maul", 12.0F, -3.2F)
         .primary(Active.of(160, MycelordArsenal::capCrash).empowered(50))
         .alternate(Active.of(240, MycelordArsenal::puffballVault).empowered(50))
         .charge(
            Charge.of(3, 14)
               .pose(ItemUseAnimation.TRIDENT)
               .onLevel(MycelordArsenal::maelstromLevel)
               .cast(Active.of(320, MycelordArsenal::maelstromCrush).empowered(60))
         )
         .passive(MycelordArsenal::sporeshock)
         .resonancePerHit(14.0F);
      this.weapon("sporecaster", 4.0F, -2.4F)
         .primary(Active.of(80, MycelordArsenal::puffballBarrage).empowered(30))
         .alternate(Active.of(200, MycelordArsenal::gillTorrent).empowered(50))
         .charge(Charge.of(4, 10).pose(ItemUseAnimation.BOW).onLevel(MycelordArsenal::sporefallLevel).cast(Active.of(400, MycelordArsenal::sporefall).empowered(60)))
         .passive(MycelordArsenal::harvest)
         .resonancePerHit(8.0F);
   }

   @Override
   protected void armor(ArmorKit set) {
      set.toughness(3.75).knockback(0.1);
      set.twoPiece((level, p, pieces) -> {
         if (level.getGameTime() % 10L == 0L) {
            if (p.hasEffect(MobEffects.POISON)) {
               p.removeEffect(MobEffects.POISON);
            }

            if (p.hasEffect(MobEffects.WITHER)) {
               p.removeEffect(MobEffects.WITHER);
            }

            if (p.hasEffect(MobEffects.NAUSEA)) {
               p.removeEffect(MobEffects.NAUSEA);
            }
         }
      });
      set.fourPiece((level, p, pieces) -> HEART.put(p.getUUID(), level.getGameTime()));
      set.onHurt(MycelordArsenal::carapaceHurt);
      set.active(500, MycelordArsenal::deathcapBloom);
   }

   private static void hook() {
      if (!hooked) {
         hooked = true;
         ServerTickEvents.END_LEVEL_TICK.register(MycelordArsenal::tickWorld);
         ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> {
            TASKS.clear();
            LIVE.clear();
            INFEST.clear();
            BURSTS.clear();
            PATCHES.clear();
            MINES.clear();
            RINGS.clear();
            HEART.clear();
            HEALED.clear();
            BREATHED.clear();
            burstSoundLevel = null;
         });
      }
   }

   static void later(ServerLevel level, int ticks, Runnable run) {
      TASKS.add(new MycelordArsenal.Task(level, level.getGameTime() + Math.max(1, ticks), run));
   }

   static void every(ServerLevel level, MycelordArsenal.Ticker ticker) {
      LIVE.add(new MycelordArsenal.Live(level, ticker));
   }

   private static void tickWorld(ServerLevel level) {
      long now = level.getGameTime();
      if (!TASKS.isEmpty()) {
         List<MycelordArsenal.Task> due = new ArrayList<>();
         Iterator<MycelordArsenal.Task> it = TASKS.iterator();

         while (it.hasNext()) {
            MycelordArsenal.Task t = it.next();
            if (t.level() == level && t.due() <= now) {
               due.add(t);
               it.remove();
            }
         }

         for (MycelordArsenal.Task t : due) {
            try {
               t.run().run();
            } catch (RuntimeException e) {
               MysticalTerrain.LOGGER.error("Mycelord gear task failed", e);
            }
         }
      }

      if (!LIVE.isEmpty()) {
         for (MycelordArsenal.Live l : new ArrayList<>(LIVE)) {
            if (l.level == level) {
               boolean keep;
               try {
                  keep = l.ticker.tick(++l.age);
               } catch (RuntimeException e) {
                  MysticalTerrain.LOGGER.error("Mycelord gear effect failed", e);
                  keep = false;
               }

               if (!keep) {
                  LIVE.remove(l);
               }
            }
         }
      }

      try {
         tickHazards(level, now);
         tickBursts(level, now);
         tickInfestation(level, now);
      } catch (RuntimeException e) {
         MysticalTerrain.LOGGER.error("Mycelord gear upkeep failed", e);
      }
   }

   static boolean alive(Player p, ServerLevel level) {
      return p.isAlive() && !p.isRemoved() && p.level() == level;
   }

   static boolean holding(Player p, String id) {
      return p.getMainHandItem().getItem() instanceof MysticWeapon w && w.gearId().equals(id)
         || p.getOffhandItem().getItem() instanceof MysticWeapon o && o.gearId().equals(id);
   }

   static boolean infestable(LivingEntity e) {
      return e.isAlive() && !e.isRemoved() && !(e instanceof Player) && !(e instanceof ArmorStand);
   }

   static int stacks(LivingEntity e) {
      MycelordArsenal.Infest f = INFEST.get(e);
      return f == null ? 0 : f.stacks;
   }

   static void infest(ServerLevel level, Player by, LivingEntity e, int n) {
      infest(level, by, e, n, true);
   }

   static void infest(ServerLevel level, Player by, LivingEntity e, int n, boolean mayBurst) {
      if (n > 0 && infestable(e)) {
         long now = level.getGameTime();
         MycelordArsenal.Infest f = INFEST.computeIfAbsent(e, k -> new MycelordArsenal.Infest());
         f.stacks = Math.min(5, f.stacks + n);
         f.last = now;
         if (mayBurst && f.stacks >= 5 && !f.queued) {
            f.queued = true;
            BURSTS.add(new MycelordArsenal.Burst(level, by, e, Math.max(now + 3L, f.lock)));
         }
      }
   }

   static int consume(LivingEntity e) {
      MycelordArsenal.Infest f = INFEST.get(e);
      if (f == null) {
         return 0;
      }

      int n = f.stacks;
      f.stacks = 0;
      f.queued = false;
      return n;
   }

   static void sporeburst(ServerLevel level, Player by, LivingEntity e) {
      long now = level.getGameTime();
      MycelordArsenal.Infest f = INFEST.get(e);
      if (f != null) {
         f.stacks = 0;
         f.queued = false;
         f.lock = now + 20L;
      }

      Vec3 c = centre(e);
      if (e.isAlive() && !e.isRemoved()) {
         zap(level, by, e, 8.0F);
         e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), by);
         if (movable(e)) {
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 1), by);
         }
      }

      List<LivingEntity> others = near(level, by, c, 4.0);
      others.remove(e);
      others.sort(Comparator.comparingDouble(o -> centre(o).distanceToSqr(c)));

      for (int i = 0; i < Math.min(3, others.size()); i++) {
         LivingEntity o = others.get(i);
         infest(level, by, o, 2);
         Vfx.beam(level, c, centre(o), SPORE, 0.1, 6);
      }

      Vfx.nova(level, c, SPORE, 2.2);
      Vfx.smoke(level, c, SPORE.smoke(), 6, 0.8, 1.4);
      Vfx.embers(level, c, SPORE.glow(), 14, 2.0);
      Vfx.glint(level, c, SPORE.core(), 0.9);
      if (burstSoundLevel != level || burstSoundTick != now) {
         burstSoundLevel = level;
         burstSoundTick = now;
         BossSounds.play(level, c, "weapon.sporeblade.burst", 0.8F, 1.05F + level.getRandom().nextFloat() * 0.15F);
      }

      GearState state = GearState.of(by);
      if (by.getMainHandItem().getItem() instanceof MysticWeapon w
         && (w.gearId().equals("sporeblade") || w.gearId().equals("mycelial_maul") || w.gearId().equals("sporecaster"))) {
         state.addResonance(w.gearId(), 6.0F);
      }

      if (wearsHeart(by, now)) {
         Long healed = HEALED.get(by.getUUID());
         if (healed == null || now - healed >= 10L) {
            HEALED.put(by.getUUID(), now);
            by.heal(2.0F);
            Vfx.flare(level, chest(by), SPORE.glow(), 0.9, 5);
         }

         int left = state.remaining("set.mycelord");
         if (left > 0) {
            state.cooldown("set.mycelord", Math.max(1, left - 20));
         }
      }
   }

   static boolean wearsHeart(Player p, long now) {
      Long stamp = HEART.get(p.getUUID());
      return stamp != null && now - stamp <= 2L && MysticArmor.pieces(p, "mycelord") >= 4;
   }

   private static void tickBursts(ServerLevel level, long now) {
      if (!BURSTS.isEmpty()) {
         List<MycelordArsenal.Burst> due = new ArrayList<>();
         Iterator<MycelordArsenal.Burst> it = BURSTS.iterator();

         while (it.hasNext() && due.size() < 8) {
            MycelordArsenal.Burst b = it.next();
            if (b.level() == level && b.due() <= now) {
               due.add(b);
               it.remove();
            }
         }

         for (MycelordArsenal.Burst b : due) {
            MycelordArsenal.Infest f = INFEST.get(b.e());
            if (f != null && f.queued) {
               if (!alive(b.by(), level)) {
                  f.queued = false;
               } else {
                  sporeburst(level, b.by(), b.e());
               }
            }
         }
      }
   }

   private static void tickInfestation(ServerLevel level, long now) {
      if (!INFEST.isEmpty()) {
         boolean show = now % 20L == 0L;
         int shown = 0;
         Iterator<Entry<LivingEntity, MycelordArsenal.Infest>> it = INFEST.entrySet().iterator();

         while (it.hasNext()) {
            Entry<LivingEntity, MycelordArsenal.Infest> en = it.next();
            LivingEntity e = en.getKey();
            MycelordArsenal.Infest f = en.getValue();
            if (e == null) {
               it.remove();
            } else if (e.isAlive() && !e.isRemoved()) {
               if (e.level() == level) {
                  long idle = now - f.last;
                  if (!f.queued && f.stacks > 0 && idle >= 100L && (idle - 100L) % 40L == 0L) {
                     f.stacks--;
                  }

                  if (f.stacks <= 0 && !f.queued) {
                     it.remove();
                  } else if (show && shown < 32 && f.stacks > 0) {
                     shown++;
                     Vfx.wisps(level, centre(e), SPORE, f.stacks, 0.45 + 0.5 * e.getBbWidth(), 22);
                     if (f.stacks == 4) {
                        Vfx.glint(level, e.position().add(0.0, e.getBbHeight() + 0.4, 0.0), SPORE.core(), 0.6);
                     }
                  }
               }
            } else if (!f.queued) {
               it.remove();
            }
         }
      }
   }

   static void blight(ServerLevel level, Player p, Vec3 at, double r, int life) {
      Vfx.telegraphCircle(level, at, r, 8, SPORE);
      Vfx.groundCrack(level, at, r, SPORE);
      MycelordArsenal.Patch oldest = null;
      int owned = 0;

      for (MycelordArsenal.Patch h : PATCHES) {
         if (h.owner().equals(p.getUUID())) {
            owned++;
            if (oldest == null) {
               oldest = h;
            }
         }
      }

      if (owned >= 6) {
         PATCHES.remove(oldest);
      }

      long now = level.getGameTime();
      PATCHES.add(new MycelordArsenal.Patch(level, p.getUUID(), at, r, now, now + life));
   }

   static void mine(ServerLevel level, Player p, Vec3 at, int arm, int life, float damage) {
      MycelordArsenal.Mine oldest = null;
      int owned = 0;

      for (MycelordArsenal.Mine m : MINES) {
         if (m.owner.equals(p.getUUID())) {
            owned++;
            if (oldest == null) {
               oldest = m;
            }
         }
      }

      if (owned >= 12) {
         MINES.remove(oldest);
      }

      long now = level.getGameTime();
      MINES.add(new MycelordArsenal.Mine(level, p.getUUID(), at, now, now + arm, now + life, damage));
      Vfx.glint(level, at.add(0.0, 0.3, 0.0), SPORE.glow(), 0.5);
      Vfx.smoke(level, at.add(0.0, 0.2, 0.0), SPORE.smoke(), 1, 0.2, 0.7);
   }

   private static @Nullable Player owner(ServerLevel level, UUID id) {
      Player p = level.getPlayerByUUID(id);
      return p != null && alive(p, level) ? p : null;
   }

   private static void tickHazards(ServerLevel level, long now) {
      if (!PATCHES.isEmpty()) {
         PATCHES.removeIf(h -> h.level() == level && !tickPatch(h, now));
      }

      if (!MINES.isEmpty()) {
         MINES.removeIf(m -> m.level == level && !tickMine(m, now));
      }

      if (!RINGS.isEmpty()) {
         RINGS.values().removeIf(r -> r.level() == level && !tickRing(r, now));
      }
   }

   private static boolean tickPatch(MycelordArsenal.Patch h, long now) {
      if (now >= h.until()) {
         return false;
      }

      ServerLevel level = h.level();
      Player p = owner(level, h.owner());
      if (p == null) {
         return false;
      }

      long age = now - h.born();
      Vec3 at = h.at();
      if (age > 0L && age % 20L == 0L) {
         for (LivingEntity e : GearKit.enemies(level, p, at, h.r() + 3.0)) {
            if (flatDist(e.position(), at) <= h.r() + e.getBbWidth() / 2.0F && Math.abs(e.getY() - at.y) < 2.0) {
               zap(level, p, e, 3.0F);
               if (movable(e)) {
                  e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 0), p);
               }

               infest(level, p, e, 1);
            }
         }
      }

      if (age % 10L == 0L) {
         Vfx.smoke(level, at, SPORE.smoke(), 3, h.r() * 0.6, 0.6);
         Vfx.embers(level, at, SPORE.glow(), 4, h.r() * 0.5);
      }

      if (age > 0L && age % 60L == 0L) {
         Vfx.groundCrack(level, at, h.r(), SPORE);
      }

      return true;
   }

   private static boolean tickMine(MycelordArsenal.Mine m, long now) {
      ServerLevel level = m.level;
      Player p = owner(level, m.owner);
      if (p == null) {
         return false;
      }

      if (m.fuse < 0L) {
         boolean trip = now >= m.expireAt;
         if (!trip && now >= m.armAt) {
            for (LivingEntity e : GearKit.enemies(level, p, m.at, 5.6)) {
               if (gap(e, m.at) <= 1.6 && Math.abs(e.getY() - m.at.y) < 2.0) {
                  trip = true;
                  break;
               }
            }
         }

         if (trip) {
            m.fuse = now;
            Vfx.telegraphCircle(level, m.at, 2.5, 6, SPORE);
            Vfx.flare(level, m.at.add(0.0, 0.3, 0.0), SPORE.core(), 1.0, 6);
         } else if ((now - m.born) % 20L == 0L) {
            Vfx.glint(level, m.at.add(0.0, 0.3, 0.0), SPORE.glow(), 0.5);
            Vfx.wisps(level, m.at, SPORE, 1, 0.35, 22);
         }

         return true;
      } else {
         if (now - m.fuse < 6L) {
            return true;
         }

         for (LivingEntity e : near(level, p, m.at, 2.5)) {
            zap(level, p, e, m.damage);
            e.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0), p);
            infest(level, p, e, 1);
         }

         Vfx.explosion(level, m.at, SPORE, 1.2);
         Vfx.embers(level, m.at, SPORE.glow(), 10, 1.4);
         chips(level, m.at.add(0.0, 0.2, 0.0), 6, 0.4);
         BossSounds.play(level, m.at, "weapon.mycelial_maul.mine", 1.0F, 1.1F);
         return false;
      }
   }

   private static boolean tickRing(MycelordArsenal.Ring r, long now) {
      if (now >= r.until()) {
         return false;
      }

      ServerLevel level = r.level();
      Player p = owner(level, r.owner());
      if (p == null) {
         return false;
      }

      long age = now - r.born();
      Vec3 at = r.at();
      double reach = r.r() + 0.3;
      if (age % 20L == 0L) {
         AABB box = new AABB(at.x - reach, at.y - 2.0, at.z - reach, at.x + reach, at.y + 2.0, at.z + reach);

         for (Player ally : level.getEntitiesOfClass(Player.class, box, pl -> pl.isAlive() && !pl.isSpectator())) {
            if (flatDist(ally.position(), at) <= reach) {
               ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 1), p);
               ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 0), p);
            }
         }

         for (LivingEntity e : GearKit.enemies(level, p, at, r.r() + 4.0)) {
            if (flatDist(e.position(), at) <= r.r() + e.getBbWidth() / 2.0F && Math.abs(e.getY() - at.y) <= 2.0) {
               shove(e, at, 0.6, 0.15);
               infest(level, p, e, 1);
            }
         }
      }

      if (age % 40L == 0L) {
         Vfx.auraRing(level, at, SPORE, r.r());
      }

      return true;
   }

   static boolean movable(LivingEntity e) {
      return !(e instanceof MysticalBoss b && b.isBoss()) && !(e instanceof EnderDragon) && !(e instanceof WitherBoss);
   }

   static void zap(ServerLevel level, Player p, LivingEntity e, float damage) {
      e.setInvulnerableTime(0);
      GearKit.magic(level, p, e, damage);
   }

   static void cut(ServerLevel level, Player p, LivingEntity e, float damage) {
      e.setInvulnerableTime(0);
      GearKit.strike(level, p, e, damage);
   }

   static void shove(LivingEntity e, Vec3 from, double out, double up) {
      if (movable(e)) {
         Vec3 away = flat(e.position().subtract(from));
         away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, e.getYRot()).scale(-1.0) : away.normalize();
         e.push(away.x * out, up, away.z * out);
         e.syncVelocity = true;
      }
   }

   static void launch(LivingEntity e, Vec3 from, double out, double up) {
      if (movable(e)) {
         Vec3 away = flat(e.position().subtract(from));
         away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, e.getYRot()).scale(-1.0) : away.normalize();
         Vec3 v = e.getDeltaMovement();
         e.setDeltaMovement(v.x * 0.3 + away.x * out, Math.max(v.y, 0.0) + up, v.z * 0.3 + away.z * out);
         e.syncVelocity = true;
      }
   }

   static void toss(LivingEntity e, Vec3 dir, double along, double up) {
      if (movable(e)) {
         Vec3 v = e.getDeltaMovement();
         e.setDeltaMovement(v.x * 0.3 + dir.x * along, Math.max(v.y, 0.0) + up, v.z * 0.3 + dir.z * along);
         e.syncVelocity = true;
      }
   }

   static void nudge(LivingEntity e, Vec3 dv) {
      if (movable(e)) {
         e.setDeltaMovement(e.getDeltaMovement().add(dv));
         e.syncVelocity = true;
      }
   }

   static List<LivingEntity> near(ServerLevel level, Player p, Vec3 c, double r) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, c, r + 4.0)) {
         if (gap(e, c) <= r) {
            out.add(e);
         }
      }

      return out;
   }

   static double gap(LivingEntity e, Vec3 c) {
      AABB b = e.getBoundingBox();
      double dx = Math.max(0.0, Math.max(b.minX - c.x, c.x - b.maxX));
      double dy = Math.max(0.0, Math.max(b.minY - c.y, c.y - b.maxY));
      double dz = Math.max(0.0, Math.max(b.minZ - c.z, c.z - b.maxZ));
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   static List<LivingEntity> strip(ServerLevel level, Player p, Vec3 from, Vec3 dir, double length, double width) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, from.add(dir.scale(length / 2.0)), length / 2.0 + width + 4.0)) {
         double ox = e.getX() - from.x;
         double oz = e.getZ() - from.z;
         double along = ox * dir.x + oz * dir.z;
         double side = Math.abs(ox * dir.z - oz * dir.x);
         AABB b = e.getBoundingBox();
         if (along > -width * 0.5 && along < length + width * 0.5 && side < width + e.getBbWidth() / 2.0F && b.maxY > from.y - 2.0 && b.minY < from.y + 3.0) {
            out.add(e);
         }
      }

      return out;
   }

   static @Nullable Vec3 floor(ServerLevel level, double x, double y, double z, int up, int down) {
      MutableBlockPos pos = BlockPos.containing(x, y, z).mutable();
      VoxelShape here = level.getBlockState(pos).getCollisionShape(level, pos);
      if (!here.isEmpty() && pos.getY() + here.max(Axis.Y) > y + 0.01) {
         int climbed = 0;

         do {
            pos.move(0, 1, 0);
         } while (++climbed <= up && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty());

         if (climbed > up) {
            return null;
         }
      }

      for (int i = 0; i <= down + up; i++) {
         BlockState state = level.getBlockState(pos);
         VoxelShape shape = state.getCollisionShape(level, pos);
         if (!shape.isEmpty()) {
            return new Vec3(x, pos.getY() + shape.max(Axis.Y), z);
         }

         if (!state.getFluidState().isEmpty()) {
            return new Vec3(x, pos.getY() + 0.9, z);
         }

         pos.move(0, -1, 0);
      }

      return null;
   }

   static Vec3 ground(ServerLevel level, Vec3 at) {
      Vec3 g = floor(level, at.x, at.y + 0.01, at.z, 3, 8);
      return g != null ? g : at;
   }

   static Vec3 chest(Player p) {
      return p.position().add(0.0, 1.1, 0.0);
   }

   static Vec3 centre(LivingEntity e) {
      return e.getBoundingBox().getCenter();
   }

   static Vec3 flat(Vec3 v) {
      return new Vec3(v.x, 0.0, v.z);
   }

   static double flatDist(Vec3 a, Vec3 b) {
      double dx = a.x - b.x;
      double dz = a.z - b.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   static float yawOf(Vec3 d) {
      return (float)(Mth.atan2(-d.x, d.z) * 180.0F / (float)Math.PI);
   }

   static Vec3 ahead(double yaw, double r) {
      double rad = Math.toRadians(yaw);
      return new Vec3(-Math.sin(rad) * r, 0.0, Math.cos(rad) * r);
   }

   static void chips(ServerLevel level, Vec3 at, int count, double speed) {
      Vfx.debris(level, at, Vfx.Debris.LEAF, 11039818, count, speed);
   }

   static boolean gillstorm(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      int charge = u.charge();
      int slashes = 3 + charge;
      float power = u.power();
      boolean empowered = u.empowered();
      boolean[] stopped = new boolean[]{false};

      for (int k = 0; k < slashes; k++) {
         int n = k;
         boolean last = k == slashes - 1;
         if (k == 0) {
            gillSlash(level, p, 0, last, power, empowered, charge);
         } else {
            later(level, 4 * k, () -> {
               if (!stopped[0] && alive(p, level) && holding(p, "sporeblade")) {
                  gillSlash(level, p, n, last, power, empowered, charge);
               } else {
                  stopped[0] = true;
               }
            });
         }
      }

      return true;
   }

   static void gillSlash(ServerLevel level, Player p, int k, boolean last, float power, boolean empowered, int charge) {
      Vec3 chest = chest(p);
      Vec3 dir = GearKit.flatLook(p);
      Vec3 look = p.getLookAngle();
      float yaw = p.getYRot();
      float pitch = Mth.clamp(p.getXRot(), -30.0F, 30.0F);

      for (LivingEntity e : near(level, p, chest, 4.5)) {
         Vec3 to = flat(centre(e).subtract(chest));
         if (!(gap(e, chest) > 1.2) || !(to.lengthSqr() > 1.0E-4) || !(to.normalize().dot(dir) < COS_75)) {
            cut(level, p, e, 4.0F * power);
            shove(e, p.position(), 0.3, 0.05);
            infest(level, p, e, 1);
            GearState.of(p).addResonance("sporeblade", 1.0F);
         }
      }

      if (last) {
         Vfx.slashArc(level, chest, yaw, 0.0F, 0.0F, 5.0, SPORE, 220.0F);
      } else if (k % 2 == 0) {
         Vfx.slashArc(level, chest, yaw, pitch, 40.0F, 4.5, SPORE, 170.0F);
      } else {
         Vfx.slashArc(level, chest, yaw, pitch, -40.0F, 4.5, SPORE, -170.0F);
      }

      Vec3 shed = chest.add(look.scale(2.0));
      Vfx.embers(level, shed, SPORE.glow(), 6, 1.2);
      Vfx.smoke(level, shed, SPORE.smoke(), 2, 0.6, 0.9);
      if (k > 0) {
         GearKit.sound(level, p, 0.6F, 1.05F + 0.05F * k, "weapon.sporeblade.use");
      }

      GearKit.kick(level, p, last && charge >= 3 ? 0.3 : 0.12);
      if (last) {
         double length = 8 + 2 * charge;
         boolean big = charge >= 3;
         int per = empowered ? 2 : 1;
         float[] fan = empowered ? new float[]{-20.0F, 0.0F, 20.0F} : new float[]{0.0F};

         for (float a : fan) {
            Vec3 d = dir.yRot((float)Math.toRadians(a));
            MycelordArsenal.Pod c = new MycelordArsenal.Pod(level, p, chest.add(dir.scale(1.2)), d.scale(1.1));
            c.pierce = true;
            c.crescent = true;
            c.radius = big ? 1.8 : 1.2;
            c.reach = big ? 2.4 : 1.6;
            c.life = (int)Math.ceil(length / 1.1);
            c.onHit = (e, at) -> {
               zap(level, p, e, 6.0F * power);
               infest(level, p, e, per);
               GearKit.hitSpark(level, e, SPORE);
            };
            c.onBurst = at -> {
               Vfx.impact(level, at, SPORE, 0.5);
               Vfx.smoke(level, at, SPORE.smoke(), 4, 0.8, 1.2);
            };
            c.fly();
         }
      }
   }

   static void rotdrawLevel(ServerLevel level, Player p, WeaponKit kit, int charge, boolean max) {
      Vec3 tip = GearKit.tip(p);
      Vfx.gather(level, tip, SPORE, 1.0 + 0.3 * charge, 6 + 2 * charge);
      Vfx.wisps(level, chest(p), SPORE, charge + 1, 0.9, 12);
      Vfx.telegraphLine(level, p.position(), GearKit.flatLook(p), 8 + 2 * charge, 1.6, 8, SPORE);
      if (max) {
         Vfx.flare(level, tip, SPORE.core(), 1.6, 6);
         Vfx.glint(level, tip, SPORE.core(), 1.1);
         Vfx.ring(level, p.position(), SPORE.glow(), 1.8, 10);
         GearKit.kick(level, p, 0.15);
      }
   }

   static boolean rhizomeDive(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 origin = p.position();
      LivingEntity target = GearKit.lookTarget(level, p, 12.0);
      Vec3 want;
      if (target != null) {
         Vec3 away = flat(target.position().subtract(origin));
         away = away.lengthSqr() < 1.0E-4 ? GearKit.flatLook(p) : away.normalize();
         want = target.position().add(away.scale(Math.max(1.8, target.getBbWidth() / 2.0F + 1.0)));
      } else {
         Vec3 lp = GearKit.lookPoint(level, p, 14.0);
         Vec3 back = flat(origin.subtract(lp));
         want = back.lengthSqr() < 1.0E-4 ? lp : lp.add(back.normalize().scale(0.6));
      }

      Vec3 off = flat(want.subtract(origin));
      double dist = off.length();
      if (dist > 14.0) {
         want = new Vec3(origin.x + off.x / dist * 14.0, want.y, origin.z + off.z / dist * 14.0);
         dist = 14.0;
      }

      Vec3 toward = dist < 0.001 ? Vec3.ZERO : flat(origin.subtract(want)).normalize();
      Vec3 dest = null;

      for (double pull : new double[]{0.0, 2.0, 4.0}) {
         if (pull > 0.0 && pull >= dist) {
            break;
         }

         Vec3 c = want.add(toward.scale(pull));
         Vec3 g = floor(level, c.x, c.y + 0.5, c.z, 3, 8);
         if (g != null && BossFx.safeGround(level, g, 3.0) && level.noCollision(p, p.getBoundingBox().move(g.subtract(origin)))) {
            dest = g;
            break;
         }
      }

      if (dest == null) {
         return false;
      }

      float power = u.power();
      boolean empowered = u.empowered();
      Vec3 path = flat(dest.subtract(origin));
      double length = path.length();
      Vec3 dir = length < 0.001 ? GearKit.flatLook(p) : path.normalize();
      Vfx.groundCrack(level, origin, 1.6, SPORE);
      chips(level, origin.add(0.0, 0.2, 0.0), 10, 0.3);
      Vfx.smoke(level, origin.add(0.0, 0.3, 0.0), SPORE.smoke(), 8, 0.6, 1.2);
      Vfx.telegraphLine(level, origin, dir, Math.max(1.0, length), 1.4, 4, SPORE);
      p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 6, 3, false, false, true));
      p.setDeltaMovement(Vec3.ZERO);
      p.syncVelocity = true;
      Vec3 to = dest;
      later(level, 4, () -> emerge(level, p, origin, to, dir, length, target, power, empowered));
      return true;
   }

   static void emerge(
      ServerLevel level, Player p, Vec3 origin, Vec3 dest, Vec3 dir, double length, @Nullable LivingEntity target, float power, boolean empowered
   ) {
      if (alive(p, level)) {
         Vec3 at = dest;
         if (level.noCollision(p, p.getBoundingBox().move(dest.subtract(p.position())))) {
            float yaw = p.getYRot();
            if (target != null && target.isAlive() && target.level() == level) {
               Vec3 face = flat(target.position().subtract(dest));
               if (face.lengthSqr() > 1.0E-4) {
                  yaw = yawOf(face);
               }
            }

            p.teleportTo(level, dest.x, dest.y, dest.z, Set.of(), yaw, p.getXRot(), false);
         } else {
            at = p.position();
         }

         p.resetFallDistance();
         p.setDeltaMovement(0.0, 0.45, 0.0);
         p.syncVelocity = true;

         for (LivingEntity e : near(level, p, at, 3.5)) {
            cut(level, p, e, 12.0F * power);
            launch(e, at, 0.3, 0.8);
            infest(level, p, e, 2);
            GearState.of(p).addResonance("sporeblade", 3.0F);
         }

         float spin = p.getYRot();

         for (int i = 0; i < 6; i++) {
            Vfx.pillar(level, ground(level, at.add(ahead(spin + 60 * i, 2.5))), SPORE, 1.6, 0.4);
         }

         Vfx.heavyImpact(level, at, SPORE, 0.8);
         Vfx.groundCrack(level, at, 3.5, SPORE);
         chips(level, at.add(0.0, 0.3, 0.0), 14, 0.45);
         Vfx.dashTrail(level, origin.add(0.0, 0.2, 0.0), at.add(0.0, 0.2, 0.0), SPORE);
         GearKit.kick(level, p, 0.3);
         GearKit.sound(level, p, 1.1F, 1.0F, "weapon.sporeblade.emerge", "weapon.sporeblade.alt");
         if (empowered && !(length < 1.0)) {
            Vfx.telegraphLine(level, origin, dir, length, 1.4, 8, SPORE);
            later(level, 8, () -> {
               if (alive(p, level)) {
                  for (double d = 0.75; d < length; d += 1.5) {
                     Vfx.pillar(level, ground(level, origin.add(dir.scale(d))), SPORE, 1.4, 0.45);
                  }

                  Vec3 line = dest.subtract(origin);
                  Vec3 lineDir = line.lengthSqr() < 1.0E-4 ? dir : line.normalize();

                  for (LivingEntity e : GearKit.alongLine(level, p, origin.add(0.0, 0.5, 0.0), lineDir, line.length() + 0.5, 1.4)) {
                     zap(level, p, e, 8.0F * power);
                     launch(e, e.position(), 0.0, 0.6);
                     infest(level, p, e, 1);
                  }

                  BossSounds.play(level, origin.add(dest).scale(0.5), "weapon.sporeblade.impale", 1.0F, 1.0F);
               }
            });
         }
      }
   }

   static void infestation(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof Player p && infestable(t)) {
         infest(level, p, t, 1);
         GearKit.hitSpark(level, t, SPORE);
         if (stacks(t) >= 3) {
            GearKit.procSound(level, t, "sporeblade");
         }
      }
   }

   static boolean capCrash(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float power = u.power();
      boolean empowered = u.empowered();
      Vec3 dir = GearKit.flatLook(p);
      Vec3 impact = ground(level, p.position().add(dir.scale(2.2)));
      Set<Integer> hit = new HashSet<>();

      for (LivingEntity e : near(level, p, impact, 2.8)) {
         cut(level, p, e, 16.0F * power);
         toss(e, dir, 0.4, 0.6);
         infest(level, p, e, 2);
         u.resonate(2.0F);
         hit.add(e.getId());
      }

      Vfx.heavyImpact(level, impact, SPORE, 1.0);
      chips(level, impact.add(0.0, 0.2, 0.0), 14, 0.5);
      Vfx.smoke(level, impact.add(0.0, 0.3, 0.0), SPORE.smoke(), 6, 1.2, 1.4);
      GearKit.kick(level, p, 0.35);
      fissure(level, p, impact, dir, 8.0, power, hit, true);
      if (empowered) {
         fissure(level, p, impact, dir.yRot((float)Math.toRadians(25.0)), 7.0, power, hit, false);
         fissure(level, p, impact, dir.yRot((float)Math.toRadians(-25.0)), 7.0, power, hit, false);
      }

      later(level, 6, () -> {
         if (alive(p, level)) {
            blight(level, p, ground(level, impact.add(dir.scale(8.5))), empowered ? 3.5 : 2.5, empowered ? 160 : 100);
         }
      });
      return true;
   }

   static void fissure(ServerLevel level, Player p, Vec3 from, Vec3 dir, double length, float power, Set<Integer> hit, boolean voice) {
      Vfx.telegraphLine(level, from, dir, length, 2.4, 6, SPORE);
      Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
      double seg = length / 3.0;

      for (int s = 0; s < 3; s++) {
         int n = s;
         later(level, 2 * (s + 1), () -> {
            if (alive(p, level)) {
               Vec3 pt = ground(level, from.add(dir.scale(seg * (n + 1))));
               Vfx.groundCrack(level, pt, 1.6, SPORE);
               Vfx.pillar(level, ground(level, pt.add(side.scale(0.6))), SPORE, 1.8, 0.5);
               Vfx.pillar(level, ground(level, pt.subtract(side.scale(0.6))), SPORE, 1.8, 0.5);
               chips(level, pt.add(0.0, 0.2, 0.0), 4, 0.35);
               Vec3 start = from.add(dir.scale(seg * n));

               for (LivingEntity e : strip(level, p, new Vec3(start.x, pt.y, start.z), dir, seg, 1.4)) {
                  if (hit.add(e.getId())) {
                     zap(level, p, e, 10.0F * power);
                     launch(e, e.position(), 0.0, 0.7);
                     infest(level, p, e, 1);
                  }
               }

               if (voice && n == 0) {
                  BossSounds.play(level, pt, "weapon.mycelial_maul.fissure", 0.9F, 0.8F);
               }
            }
         });
      }
   }

   static boolean puffballVault(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float power = u.power();
      boolean empowered = u.empowered();
      Vec3 feet = p.position();
      Vec3 off = flat(GearKit.lookPoint(level, p, 14.0).subtract(feet));
      double dist = Math.min(12.0, off.length());
      Vec3 dir = off.lengthSqr() < 1.0E-4 ? GearKit.flatLook(p) : off.normalize();
      Vfx.explosion(level, feet, SPORE, 1.4);
      Vfx.smoke(level, feet, SPORE.smoke(), 8, 1.2, 1.4);
      Vfx.embers(level, feet, SPORE.glow(), 12, 1.6);
      chips(level, feet.add(0.0, 0.2, 0.0), 8, 0.4);

      for (LivingEntity e : near(level, p, feet, 3.0)) {
         zap(level, p, e, 6.0F * power);
         launch(e, feet, 0.8, 0.25);
         infest(level, p, e, 1);
      }

      double speed = Mth.clamp(dist / 12.0, 0.4, 1.2);
      p.setDeltaMovement(dir.x * speed, 0.95, dir.z * speed);
      p.syncVelocity = true;
      p.resetFallDistance();
      every(level, age -> {
         if (!alive(p, level)) {
            return false;
         }

         p.resetFallDistance();
         if (age % 2 == 0) {
            Vfx.trail(level, p.position().add(0.0, 0.5, 0.0), SPORE, 0.6);
            Vfx.smoke(level, p.position().add(0.0, 0.3, 0.0), SPORE.smoke(), 1, 0.2, 0.9);
         }

         if ((age <= 4 || !p.onGround() && !p.isInWater()) && age < 40) {
            return true;
         }

         vaultLand(level, p, power, empowered);
         return false;
      });
      return true;
   }

   static void vaultLand(ServerLevel level, Player p, float power, boolean empowered) {
      Vec3 at = p.onGround() ? p.position() : ground(level, p.position());

      for (LivingEntity e : near(level, p, at, 5.0)) {
         double d = Math.min(5.0, gap(e, at));
         cut(level, p, e, (float)(20.0 * (1.0 - 0.5 * d / 5.0)) * power);
         launch(e, at, 0.2, 0.6);
         if (movable(e)) {
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), p);
         }

         infest(level, p, e, 2);
         GearState.of(p).addResonance("mycelial_maul", 3.0F);
      }

      Vfx.heavyImpact(level, at, SPORE, 1.2);
      Vfx.groundCrack(level, at, 5.0, SPORE);
      Vfx.shockwave(level, at, SPORE, 5.0);
      chips(level, at.add(0.0, 0.3, 0.0), 16, 0.55);

      for (int i = 0; i < 8; i++) {
         Vfx.smoke(level, at.add(ahead(45 * i, 2.5)).add(0.0, 0.3, 0.0), SPORE.smoke(), 2, 0.4, 1.2);
      }

      GearKit.kick(level, p, 0.4);
      GearKit.sound(level, p, 1.3F, 0.8F, "weapon.mycelial_maul.land", "weapon.mycelial_maul.use");
      int count = empowered ? 6 : 4;
      double r = empowered ? 3.5 : 3.2;
      double first = empowered ? 0.0 : 45.0;
      float yaw = p.getYRot();

      for (int i = 0; i < count; i++) {
         Vec3 c = at.add(ahead(yaw + first + 360.0 / count * i, r));
         Vec3 g = floor(level, c.x, c.y + 0.5, c.z, 3, 6);
         if (g != null) {
            mine(level, p, g, 10, 100, 8.0F);
         }
      }

      if (empowered) {
         blight(level, p, at, 4.0, 160);
      }
   }

   static void maelstromLevel(ServerLevel level, Player p, WeaponKit kit, int charge, boolean max) {
      Vec3 feet = p.position();
      Vec3 tip = GearKit.tip(p);
      Vfx.vortex(level, feet, 3.0 + 1.5 * charge, 2.5, SPORE, 16);
      Vfx.gather(level, tip, SPORE, 1.2, 8);
      double r = 5 + 2 * charge;

      for (LivingEntity e : near(level, p, feet, r)) {
         if (flatDist(e.position(), feet) > 2.0) {
            Vec3 in = flat(feet.subtract(e.position())).normalize();
            nudge(e, new Vec3(in.x * 0.55, 0.12, in.z * 0.55));
         }

         zap(level, p, e, 2.0F);
         infest(level, p, e, 1);
      }

      if (charge == 1) {
         GearKit.sound(level, p, 1.0F, 0.8F, "weapon.mycelial_maul.vortex");
      }

      if (max) {
         Vfx.flare(level, tip, SPORE.core(), 1.8, 6);
         Vfx.glint(level, tip, SPORE.core(), 1.1);
         GearKit.kick(level, p, 0.15);
      }
   }

   static boolean maelstromCrush(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float power = u.power();
      int l = Math.max(1, u.charge());
      Vec3 feet = p.onGround() ? p.position() : ground(level, p.position());
      double r = 3.5 + l;

      for (LivingEntity e : near(level, p, feet, r)) {
         cut(level, p, e, (12 + 5 * l) * power);
         launch(e, feet, 0.3, 0.5 + 0.15 * l);
         infest(level, p, e, l);
         u.resonate(2.0F);
      }

      Vfx.heavyImpact(level, feet, SPORE, 0.7 + 0.3 * l);
      Vfx.shockwave(level, feet, SPORE, r);
      Vfx.groundCrack(level, feet, r, SPORE);
      chips(level, feet.add(0.0, 0.3, 0.0), 8 + 4 * l, 0.5);
      GearKit.kick(level, p, 0.25 + 0.1 * l);
      if (l >= 3) {
         Vfx.impactFrame(level, chest(p), 18.0, 4253896, 0.35F);
         double ring = r + 1.5;
         float yaw = p.getYRot();
         Vfx.telegraphCircle(level, feet, ring + 1.5, 6, SPORE);
         later(level, 6, () -> rootquake(level, p, feet, ring, yaw, power));
      }

      if (u.empowered()) {
         Vfx.vortex(level, feet, 6.0, 3.0, SPORE, 60);
         every(level, age -> {
            if (alive(p, level) && age <= 60) {
               for (LivingEntity exx : near(level, p, feet, 7.0)) {
                  Vec3 in = flat(feet.subtract(exx.position()));
                  if (in.lengthSqr() > 0.25) {
                     nudge(exx, in.normalize().scale(0.06));
                  }
               }

               if (age % 20 == 0) {
                  for (LivingEntity ex : near(level, p, feet, 6.0)) {
                     zap(level, p, ex, 3.0F);
                     infest(level, p, ex, 1);
                  }

                  Vfx.ring(level, feet, SPORE.glow(), 6.0, 12);
                  Vfx.embers(level, feet.add(0.0, 1.0, 0.0), SPORE.glow(), 8, 3.0);
               }

               return true;
            } else {
               return false;
            }
         });
      }

      return true;
   }

   static void rootquake(ServerLevel level, Player p, Vec3 centre, double radius, float yaw, float power) {
      if (alive(p, level)) {
         Set<Integer> hit = new HashSet<>();

         for (int i = 0; i < 8; i++) {
            Vec3 pt = ground(level, centre.add(ahead(yaw + 45 * i, radius)));
            Vfx.pillar(level, pt, SPORE, 2.2, 0.6);
            Vfx.groundCrack(level, pt, 1.2, SPORE);

            for (LivingEntity e : near(level, p, pt, 1.5)) {
               if (hit.add(e.getId())) {
                  zap(level, p, e, 10.0F * power);
                  launch(e, e.position(), 0.0, 0.8);
               }
            }
         }

         chips(level, centre.add(0.0, 0.3, 0.0), 10, 0.6);
         BossSounds.play(level, centre, "weapon.mycelial_maul.fissure", 1.0F, 0.7F);
      }
   }

   static void sporeshock(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof Player p && infestable(t)) {
         boolean rooted = stacks(t) >= 3;
         Vec3 c = centre(t);
         infest(level, p, t, 1);

         for (LivingEntity o : near(level, p, c, 2.5)) {
            if (o != t) {
               infest(level, p, o, 1);
            }
         }

         if (rooted && movable(t)) {
            t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 3), p);
            Vfx.groundCrack(level, t.position(), 1.0, SPORE);
         }

         Vfx.ring(level, t.position(), SPORE.glow(), 2.5, 10);
         Vfx.embers(level, c, SPORE.glow(), 8, 1.6);
         Vfx.smoke(level, t.position().add(0.0, 0.4, 0.0), SPORE.smoke(), 4, 1.0, 1.0);
         GearKit.procSound(level, t, "mycelial_maul");
      }
   }

   static boolean puffballBarrage(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float power = u.power();
      boolean empowered = u.empowered();
      Vec3 aim = GearKit.lookPoint(level, p, 24.0).subtract(p.getLookAngle().scale(0.3));
      Vec3 g = floor(level, aim.x, aim.y, aim.z, 2, 20);
      Vec3 centre = g != null ? g : aim;
      int count = empowered ? 5 : 3;
      double r = empowered ? 2.4 : 1.8;
      int spacing = empowered ? 1 : 2;
      float yaw = p.getYRot();

      for (int k = 0; k < count; k++) {
         Vec3 c = centre.add(ahead(yaw + 360.0 / count * k, r));
         Vec3 cg = floor(level, c.x, c.y + 0.5, c.z, 3, 6);
         Vec3 target = cg != null ? cg : c;
         if (k == 0) {
            lobPuffball(level, p, target, power, empowered);
         } else {
            later(level, spacing * k, () -> {
               if (alive(p, level)) {
                  lobPuffball(level, p, target, power, empowered);
               }
            });
         }
      }

      GearKit.muzzle(level, p, SPORE, 1.0);
      Vfx.smoke(level, GearKit.tip(p), SPORE.smoke(), 3, 0.3, 0.8);
      return true;
   }

   static void lobPuffball(ServerLevel level, Player p, Vec3 target, float power, boolean mine) {
      Vec3 from = GearKit.tip(p);
      int flight = 12;
      double g = 0.06;
      Vec3 v = target.subtract(from).scale(1.0 / flight).add(0.0, g * (flight + 1) / 2.0, 0.0);
      MycelordArsenal.Pod pod = new MycelordArsenal.Pod(level, p, from, v);
      pod.gravity = g;
      pod.radius = 0.5;
      pod.life = 40;
      pod.onBurst = at -> podBurst(level, p, at, power, mine);
      pod.fly();
      Vfx.flare(level, from, SPORE.glow(), 0.8, 4);
   }

   static void podBurst(ServerLevel level, Player p, Vec3 at, float power, boolean mine) {
      for (LivingEntity e : near(level, p, at, 2.5)) {
         zap(level, p, e, 6.0F * power);
         e.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0), p);
         infest(level, p, e, 1);
         GearState.of(p).addResonance("sporecaster", 2.0F);
      }

      Vfx.explosion(level, at, SPORE, 1.1);
      Vfx.embers(level, at, SPORE.glow(), 10, 1.4);
      Vfx.smoke(level, at, SPORE.smoke(), 4, 0.8, 1.2);
      BossSounds.play(level, at, "weapon.sporecaster.pod", 1.0F, 1.1F + level.getRandom().nextFloat() * 0.2F);
      if (mine) {
         Vec3 g = floor(level, at.x, at.y + 0.3, at.z, 2, 4);
         if (g != null) {
            mine(level, p, g, 10, 60, 6.0F);
         }
      }
   }

   static boolean gillTorrent(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float power = u.power();
      boolean empowered = u.empowered();
      int pulses = empowered ? 7 : 5;
      double half = empowered ? 24.0 : 18.0;
      double range = empowered ? 13.0 : 10.0;
      p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25 + (pulses - 5) * 5, 1, false, false, true));
      boolean[] stopped = new boolean[]{false};

      for (int k = 0; k < pulses; k++) {
         int n = k;
         boolean last = k == pulses - 1;
         if (k == 0) {
            torrentPulse(level, p, 0, last, power, empowered, half, range);
         } else {
            later(level, 5 * k, () -> {
               if (!stopped[0] && alive(p, level) && holding(p, "sporecaster")) {
                  torrentPulse(level, p, n, last, power, empowered, half, range);
               } else {
                  stopped[0] = true;
               }
            });
         }
      }

      return true;
   }

   static void torrentPulse(ServerLevel level, Player p, int k, boolean last, float power, boolean empowered, double half, double range) {
      Vec3 eye = GearKit.eye(p);
      Vec3 look = p.getLookAngle();
      double cos = Math.cos(Math.toRadians(half));
      Vec3 end = eye.add(look.scale(range));

      for (LivingEntity e : near(level, p, eye, range)) {
         Vec3 to = centre(e).subtract(eye);
         boolean inside = to.lengthSqr() < 1.0E-4 || to.normalize().dot(look) > cos || e.getBoundingBox().inflate(0.5).clip(eye, end).isPresent();
         if (inside && p.hasLineOfSight(e)) {
            zap(level, p, e, 5.0F * power);
            if (empowered && last && stacks(e) >= 3) {
               sporeburst(level, p, e);
            } else {
               infest(level, p, e, 1);
            }

            nudge(e, flat(look).scale(0.15));
         }
      }

      Vfx.streaks(level, eye.add(look.scale(0.8)), look, SPORE, 8);

      for (double f : new double[]{0.25, 0.5, 0.75}) {
         double d = range * f;
         Vfx.smoke(level, eye.add(look.scale(d)), SPORE.smoke(), 2, 0.3 * d, 0.9);
      }

      Vfx.embers(level, eye.add(look.scale(range * 0.4)), SPORE.glow(), 8, 1.5);
      GearKit.muzzle(level, p, SPORE, 0.7);
      if (k == 0) {
         Vfx.telegraphCone(level, p.position(), p.getYRot(), (float)half, range, 4, SPORE);
      }

      p.setDeltaMovement(p.getDeltaMovement().subtract(look.scale(0.08)));
      p.syncVelocity = true;
   }

   static Vec3 fallZone(ServerLevel level, Player p) {
      Vec3 aim = GearKit.lookPoint(level, p, 32.0).subtract(p.getLookAngle().scale(0.3));
      Vec3 g = floor(level, aim.x, aim.y, aim.z, 2, 24);
      return g != null ? g : aim;
   }

   static void sporefallLevel(ServerLevel level, Player p, WeaponKit kit, int charge, boolean max) {
      Vec3 zone = fallZone(level, p);
      Vec3 tip = GearKit.tip(p);
      Vfx.telegraphCircle(level, zone, 3.0 + 1.5 * charge, 12, ROT);
      if (charge == 1) {
         Vfx.runeCircle(level, zone, 4.0, ROT, 60, 0.04F, Vfx.Rune.GLYPH);
      }

      Vfx.gather(level, tip, ROT, 1.2, 8);
      if (max) {
         Vfx.chargeUp(level, tip, ROT, 8);
         Vfx.glint(level, tip, ROT.core(), 1.1);
      }
   }

   static boolean sporefall(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float power = u.power();
      boolean empowered = u.empowered();
      int l = Math.max(1, u.charge());
      Vec3 zone = fallZone(level, p);
      double r = 3.0 + 1.5 * l;
      Vec3 tip = GearKit.tip(p);
      GearKit.muzzle(level, p, ROT, 1.2);
      Vfx.beam(level, tip, tip.add(0.0, 12.0, 0.0), ROT, 0.3, 6);
      Vfx.streaks(level, tip, new Vec3(0.0, 1.0, 0.0), ROT, 8);
      Vfx.runeCircle(level, zone, r, ROT, 8 * (l - 1) + 24, 0.05F, Vfx.Rune.GLYPH);
      Set<Integer> used = new HashSet<>();

      for (int w = 0; w < l; w++) {
         if (w == 0) {
            sporeWave(level, p, zone, r, power, empowered, used);
         } else {
            later(level, 8 * w, () -> {
               if (alive(p, level)) {
                  sporeWave(level, p, zone, r, power, empowered, used);
               }
            });
         }
      }

      if (l >= 4) {
         later(level, 18, () -> {
            if (alive(p, level)) {
               Vfx.telegraphCircle(level, zone, 5.0, 24, ROT);
            }
         });
         later(level, 26, () -> every(level, age -> crownSpore(level, p, zone, power, age)));
      }

      return true;
   }

   static void sporeWave(ServerLevel level, Player p, Vec3 zone, double r, float power, boolean empowered, Set<Integer> used) {
      List<LivingEntity> marks = new ArrayList<>();
      if (empowered) {
         for (LivingEntity e : near(level, p, zone, r + 4.0)) {
            if (stacks(e) > 0 && !used.contains(e.getId())) {
               marks.add(e);
            }
         }

         Collections.shuffle(marks);
      }

      for (int q = 0; q < 4; q++) {
         Vec3 m;
         if (!marks.isEmpty()) {
            LivingEntity e = marks.remove(marks.size() - 1);
            used.add(e.getId());
            m = ground(level, e.position());
         } else {
            double ang = 90 * q + level.getRandom().nextDouble() * 90.0;
            double rr = r * (0.3 + 0.7 * level.getRandom().nextDouble());
            m = ground(level, zone.add(ahead(ang, rr)));
         }

         meteor(level, p, m, power, q == 0);
      }
   }

   static void meteor(ServerLevel level, Player p, Vec3 m, float power, boolean voice) {
      Vfx.telegraphCircle(level, m, 2.2, 14, ROT);
      Vfx.meteorTrail(level, m.add(0.0, 12.0, 0.0), new Vec3(0.0, -0.86, 0.0), ROT);
      every(level, age -> {
         if (!alive(p, level)) {
            return false;
         }

         if (age < 14) {
            if (age % 2 == 0) {
               Vfx.meteorTrail(level, m.add(0.0, 12.0 - 0.86 * age, 0.0), new Vec3(0.0, -0.86, 0.0), ROT);
            }

            return true;
         } else {
            for (LivingEntity e : near(level, p, m, 2.2)) {
               zap(level, p, e, 7.0F * power);
               infest(level, p, e, 1);
            }

            Vfx.heavyImpact(level, m, ROT, 0.5);
            Vfx.smoke(level, m.add(0.0, 0.3, 0.0), 2759214, 3, 0.8, 1.2);
            if (voice) {
               BossSounds.play(level, m, "weapon.sporecaster.meteor", 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            }

            return false;
         }
      });
   }

   static boolean crownSpore(ServerLevel level, Player p, Vec3 zone, float power, int age) {
      if (!alive(p, level)) {
         return false;
      }

      if (age < 16) {
         Vec3 head = zone.add(0.0, 16 - age, 0.0);
         Vfx.meteorTrail(level, head, new Vec3(0.0, -1.0, 0.0), ROT);
         if (age % 2 == 0) {
            Vfx.flare(level, head, 12602088, 2.2, 4);
         }

         return true;
      } else {
         for (LivingEntity e : near(level, p, zone, 5.0)) {
            zap(level, p, e, 18.0F * power);
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0), p);
            infest(level, p, e, 2);
         }

         Vfx.heavyImpact(level, zone, ROT, 1.6);
         Vfx.nova(level, zone, ROT, 5.0);
         Vfx.impactFrame(level, zone, 20.0, 12602088, 0.3F);
         Vfx.debris(level, zone.add(0.0, 0.5, 0.0), Vfx.Debris.ASH, 12602088, 18, 0.5);
         BossSounds.play(level, zone, "weapon.sporecaster.crown", 1.6F, 0.9F);
         return false;
      }
   }

   static void harvest(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof Player p) {
         int n = consume(t);
         if (n > 0) {
            Vec3 c = centre(t);
            if (t.isAlive()) {
               zap(level, p, t, 3 * n);
            }

            p.heal(n);
            GearState.of(p).addResonance("sporecaster", 5 * n);
            Vfx.gather(level, c, SPORE, 1.2, 4 + 2 * n);
            Vfx.beam(level, c, chest(p), SPORE, 0.12, 6);
            Vfx.flare(level, chest(p), SPORE.glow(), 0.8, 6);
            GearKit.procSound(level, t, "sporecaster");
         }
      }
   }

   static void carapaceHurt(ServerLevel level, Player p, @Nullable LivingEntity attacker, DamageSource source, float taken, int pieces) {
      long now = level.getGameTime();
      if (attacker != null && infestable(attacker)) {
         Long last = BREATHED.get(attacker);
         if (last == null || now - last >= 20L) {
            BREATHED.put(attacker, now);
            infest(level, p, attacker, 1);
            Vfx.smoke(level, chest(p), SPORE.smoke(), 4, 0.5, 1.2);
            Vfx.embers(level, centre(attacker), SPORE.glow(), 6, 0.8);
         }
      }

      if (pieces >= 4 && taken > 0.0F && p.isAlive() && p.getHealth() < p.getMaxHealth() * 0.3F) {
         GearState state = GearState.of(p);
         if (state.ready("mycelord.shell")) {
            state.cooldown("mycelord.shell", 1200);
            shelfShell(level, p);
         }
      }
   }

   static void shelfShell(ServerLevel level, Player p) {
      p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 1));
      p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 160, 0));
      Vfx.shieldBubble(level, chest(p), 1.4, SPORE, 22);
      chips(level, chest(p), 16, 0.4);
      Vfx.auraRing(level, p.position(), SPORE, 1.6);
      GearKit.sound(level, p, 1.2F, 0.9F, "weapon.sporecaster.shell");
      every(level, age -> {
         if (!alive(p, level)) {
            return false;
         }

         if (age % 20 == 0) {
            Vec3 feet = p.position();

            for (LivingEntity e : near(level, p, feet, 4.0)) {
               zap(level, p, e, 4.0F);
               infest(level, p, e, 1);
            }

            Vfx.ring(level, feet, SPORE.glow(), 4.0, 12);
            Vfx.smoke(level, feet.add(0.0, 0.4, 0.0), SPORE.smoke(), 4, 1.5, 1.2);
            if (age < 160 && p.getAbsorptionAmount() > 0.0F) {
               Vfx.shieldBubble(level, chest(p), 1.4, SPORE, 22);
            }
         }

         return age < 160;
      });
   }

   static boolean deathcapBloom(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 feet = p.position();
      Vec3 chest = chest(p);
      p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 16, 1, false, false, true));
      Vfx.chargeUp(level, chest, ROT, 14);
      Vfx.telegraphCircle(level, feet, 10.0, 14, ROT);
      Vfx.runeCircle(level, feet, 4.0, ROT, 30, 0.06F, Vfx.Rune.GLYPH);
      Vfx.vortex(level, feet, 3.0, 3.0, ROT, 14);
      Vfx.gather(level, chest, ROT, 3.0, 16);
      later(level, 14, () -> bloom(level, p));
      later(level, 16, () -> detonate(level, p));
      return true;
   }

   static void bloom(ServerLevel level, Player p) {
      if (alive(p, level)) {
         Vec3 chest = chest(p);
         Vfx.nova(level, chest, ROT, 10.0);
         Vfx.explosion(level, chest, ROT, 2.5);
         Vfx.radialFlash(level, chest, 16.0, 12602088, 0.35F);
         Vfx.debris(level, chest, Vfx.Debris.ASH, 12602088, 24, 0.5);
         Vfx.smoke(level, chest, 2759214, 10, 3.0, 2.2);

         for (LivingEntity e : near(level, p, chest, 10.0)) {
            zap(level, p, e, 12.0F);
            infest(level, p, e, 2, false);
            launch(e, p.position(), 1.0, 0.4);
         }

         GearKit.sound(level, p, 1.8F, 0.85F, "weapon.sporecaster.bloom");
         GearKit.kick(level, p, 0.35);
      }
   }

   static void detonate(ServerLevel level, Player p) {
      if (alive(p, level)) {
         Vec3 chest = chest(p);
         List<LivingEntity> rotting = new ArrayList<>();

         for (LivingEntity e : near(level, p, chest, 16.0)) {
            if (stacks(e) > 0) {
               rotting.add(e);
            }
         }

         rotting.sort(Comparator.comparingDouble(e -> centre(e).distanceToSqr(chest)));
         int beams = 0;

         for (int i = 0; i < Math.min(16, rotting.size()); i++) {
            LivingEntity e = rotting.get(i);
            Vec3 c = centre(e);
            int n = consume(e);
            zap(level, p, e, 4 * n);
            if (n >= 5) {
               sporeburst(level, p, e);
            }

            if (beams++ < 10) {
               Vfx.beam(level, chest, c, SPORE, 0.12, 8);
            }

            Vfx.nova(level, c, SPORE, 1.2 + 0.2 * n);
         }

         Vec3 at = ground(level, p.position());
         Vfx.ward(level, at, 3.0, SPORE, 160);
         Vfx.runeCircle(level, at, 3.0, SPORE, 160, 0.03F, Vfx.Rune.GLYPH);
         long now = level.getGameTime();
         RINGS.put(p.getUUID(), new MycelordArsenal.Ring(level, p.getUUID(), at, 3.0, now, now + 160L));
      }
   }

   private record Burst(ServerLevel level, Player by, LivingEntity e, long due) {
   }

   private static final class Infest {
      private int stacks;
      private long last;
      private long lock;
      private boolean queued;
   }

   private static final class Live {
      private final ServerLevel level;
      private final MycelordArsenal.Ticker ticker;
      private int age;

      private Live(ServerLevel level, MycelordArsenal.Ticker ticker) {
         this.level = level;
         this.ticker = ticker;
      }
   }

   private static final class Mine {
      private final ServerLevel level;
      private final UUID owner;
      private final Vec3 at;
      private final long born;
      private final long armAt;
      private final long expireAt;
      private final float damage;
      private long fuse = -1L;

      private Mine(ServerLevel level, UUID owner, Vec3 at, long born, long armAt, long expireAt, float damage) {
         this.level = level;
         this.owner = owner;
         this.at = at;
         this.born = born;
         this.armAt = armAt;
         this.expireAt = expireAt;
         this.damage = damage;
      }
   }

   private record Patch(ServerLevel level, UUID owner, Vec3 at, double r, long born, long until) {
   }

   private static final class Pod implements MycelordArsenal.Ticker {
      private final ServerLevel level;
      private final Player owner;
      private Vec3 pos;
      private Vec3 vel;
      private double gravity;
      private double radius = 0.5;
      private int life = 40;
      private boolean pierce;
      private boolean crescent;
      private double reach = 1.6;
      private final Set<Integer> hit = new HashSet<>();
      private BiConsumer<LivingEntity, Vec3> onHit = (e, at) -> {};
      private Consumer<Vec3> onBurst = at -> {};

      private Pod(ServerLevel level, Player owner, Vec3 pos, Vec3 vel) {
         this.level = level;
         this.owner = owner;
         this.pos = pos;
         this.vel = vel;
      }

      private void fly() {
         this.draw(0);
         MycelordArsenal.every(this.level, this);
      }

      @Override
      public boolean tick(int age) {
         if (!MycelordArsenal.alive(this.owner, this.level)) {
            return false;
         }

         if (age > this.life) {
            this.onBurst.accept(this.pos);
            return false;
         }

         this.vel = this.vel.add(0.0, -this.gravity, 0.0);
         Vec3 from = this.pos;
         Vec3 to = from.add(this.vel);
         if (!this.level.isLoaded(BlockPos.containing(to))) {
            return false;
         }

         Vec3 wall = null;
         BlockHitResult bh = this.level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, this.owner));
         if (bh.getType() != Type.MISS) {
            wall = bh.getLocation();
            to = wall;
         }

         Vec3 a = from;
         Vec3 b = to;
         List<LivingEntity> found = new ArrayList<>();

         for (LivingEntity e : GearKit.enemies(this.level, this.owner, a.add(b).scale(0.5), a.distanceTo(b) / 2.0 + this.radius + 4.0)) {
            AABB box = e.getBoundingBox().inflate(this.radius);
            if (!this.hit.contains(e.getId()) && (box.contains(a) || box.clip(a, b).isPresent())) {
               found.add(e);
            }
         }

         found.sort(Comparator.comparingDouble(ex -> ex.getBoundingBox().getCenter().distanceToSqr(a)));

         for (LivingEntity e : found) {
            this.hit.add(e.getId());
            AABB box = e.getBoundingBox().inflate(this.radius);
            Vec3 at = box.contains(a) ? a : box.clip(a, b).orElse(a);
            if (!this.pierce) {
               this.pos = at;
               this.onBurst.accept(at);
               return false;
            }

            this.onHit.accept(e, at);
         }

         if (wall != null) {
            Vec3 back = this.vel.lengthSqr() > 1.0E-6 ? this.vel.normalize().scale(0.15) : Vec3.ZERO;
            this.pos = wall.subtract(back);
            this.onBurst.accept(this.pos);
            return false;
         } else {
            this.pos = b;
            this.draw(age);
            return true;
         }
      }

      private void draw(int age) {
         if (this.crescent) {
            if (age % 2 == 0 && this.vel.lengthSqr() > 1.0E-6) {
               Vec3 dir = this.vel.normalize();
               Vfx.slashArc(
                  this.level, this.pos.subtract(dir.scale(this.reach * 0.7)), MycelordArsenal.yawOf(dir), 0.0F, this.reach, MycelordArsenal.SPORE, 150.0F
               );
               Vfx.embers(this.level, this.pos, MycelordArsenal.SPORE.glow(), 3, 0.6);
            }
         } else {
            Vfx.trail(this.level, this.pos, MycelordArsenal.SPORE, 0.45);
            if (age % 3 == 0) {
               Vfx.smoke(this.level, this.pos, MycelordArsenal.SPORE.smoke(), 1, 0.05, 0.7);
            }
         }
      }
   }

   private record Ring(ServerLevel level, UUID owner, Vec3 at, double r, long born, long until) {
   }

   private record Task(ServerLevel level, long due, Runnable run) {
   }

   @FunctionalInterface
   private interface Ticker {
      boolean tick(int var1);
   }
}

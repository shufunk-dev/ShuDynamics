package com.vexira.mysticalterrain.boss.entity;

import com.vexira.mysticalterrain.boss.BossAction;
import com.vexira.mysticalterrain.boss.BossFx;
import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.MysticalBoss;
import com.vexira.mysticalterrain.boss.RealmMob;
import com.vexira.mysticalterrain.boss.RealmMobs;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import java.util.Map.Entry;
import org.jspecify.annotations.Nullable;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

public class BogrotBoss extends MysticalBoss {
   private static final BossSounds SOUNDS = BossSounds.of("bogrot");
   private static final Vfx.Palette P = Vfx.Palette.MIRE;
   private static final Vfx.Palette TOX = Vfx.Palette.TOXIC;
   private static final Vfx.Palette HEX = new Vfx.Palette(15917311, 10771711, 2364462).withDebris(Vfx.Debris.LEAF);
   private static final Vfx.Palette WISP = new Vfx.Palette(16776144, 13168704, 2765848).withDebris(Vfx.Debris.ASH);
   private static final BlockParticleOption MUD = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState());
   private static final BlockParticleOption ROOTS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState());
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(BogrotBoss.class, EntityDataSerializers.INT);
   private static final int BILE = 0;
   private static final int BLACK = 1;
   private static final int G_BILE = 0;
   private static final int G_WISP = 1;
   private static final int G_HEX = 2;
   private static final int G_MARSH = 3;
   private static final int G_RETCH = 4;
   private static final float[] LASH_YAW = new float[]{-18.0F, 18.0F, -6.0F, 6.0F};
   private static final float[] RETCH_YAW = new float[]{0.0F, -2.5F, 2.5F};
   public static final float WIDTH = 2.6F;
   public static final float HEIGHT = 4.8F;
   public static final boolean FIRE_IMMUNE = false;
   private final List<BogrotBoss.Glob> globs = new ArrayList<>();
   private final List<BogrotBoss.Pool> pools = new ArrayList<>();
   private final Map<UUID, int[]> rotStacks = new HashMap<>();
   private final Map<UUID, Integer> hexedUntil = new HashMap<>();
   private final Set<Integer> trampled = new HashSet<>();
   private final Vec3[] lashDirs = new Vec3[4];
   private final Vec3[] retchAims = new Vec3[3];
   private final List<Vec3> geysers = new ArrayList<>();
   private final List<Vec3> rippleMid = new ArrayList<>();
   private final List<Vec3> rippleOuter = new ArrayList<>();
   private final List<List<Vec3>> hands = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
   private Vec3 rippleCenter = Vec3.ZERO;
   private Vec3 lungeFrom = Vec3.ZERO;
   private Vec3 diveAt = Vec3.ZERO;
   private Vec3 strangleAt = Vec3.ZERO;
   private boolean submerged;
   private boolean pouncing;
   private @Nullable LivingEntity grabbed;
   private float grabDamage;
   private boolean wasWarded;
   private float committedYaw;
   private float bloomTimer = 300.0F;
   private float tideTimer = 400.0F;
   private float wispTimer = 160.0F;
   private int wispRobin;
   private int lonely;
   private int minionCheck = -100;
   private int thralls;
   private int leeches;

   public static Builder attributes() {
      return MysticalBoss.bossAttributes(980.0, 16.0, 0.22, 16.0);
   }

   public BogrotBoss(EntityType<? extends Monster> type, Level level) {
      super(type, level, "bogrot", BossBarColor.GREEN);
   }

   @Override
   protected boolean v2() {
      return true;
   }

   @Override
   public float defense() {
      return 0.42F;
   }

   @Override
   public int bossLevel() {
      return 64;
   }

   @Override
   protected float[] phaseThresholds() {
      return new float[]{0.75F, 0.5F, 0.25F};
   }

   @Override
   public float poiseMax() {
      return this.getMaxHealth() * (this.phase() >= 2 ? 0.1F : 0.14F);
   }

   @Override
   public int enrageTicks() {
      return 7200;
   }

   @Override
   protected double runSpeed() {
      return 1.55;
   }

   @Override
   protected double preferredDistance() {
      return 5.0;
   }

   @Override
   protected boolean canDodge() {
      return true;
   }

   @Override
   protected boolean preferBlink() {
      return true;
   }

   @Override
   protected boolean immune() {
      return this.submerged;
   }

   @Override
   public int introTicks() {
      return 140;
   }

   @Override
   public int deathTicks() {
      return 150;
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FORM, 0);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      super.registerControllers(controllers);
      RawAnimation[] forms = new RawAnimation[4];

      for (int i = 0; i < 4; i++) {
         forms[i] = this.loopAnim("form_" + i);
      }

      controllers.add(
         new AnimationController("form", 6, test -> test.setAndContinue(forms[Mth.clamp((Integer)this.entityData.get(FORM), 0, 3)]))
            .additiveAnimations()
      );
   }

   @Override
   protected void readAdditionalSaveData(ValueInput view) {
      super.readAdditionalSaveData(view);
      this.entityData.set(FORM, Math.min(3, this.phase()));
   }

   private Vec3 facing() {
      return Vec3.directionFromRotation(0.0F, this.getYRot());
   }

   private static Vec3 dirOf(float yaw) {
      return Vec3.directionFromRotation(0.0F, yaw);
   }

   private Vec3 front(double dist, double up) {
      return this.position().add(this.facing().scale(dist)).add(0.0, up, 0.0);
   }

   private Vec3 side(double right, double fwd, double up) {
      return this.position().add(dirOf(this.getYRot() + 90.0F).scale(right)).add(this.facing().scale(fwd)).add(0.0, up, 0.0);
   }

   private Vec3 mouth() {
      return this.front(1.4, 3.25);
   }

   private Vec3 eyes() {
      return this.front(1.45, 3.65);
   }

   private Vec3 maw() {
      return this.front(0.8, 2.7);
   }

   private Vec3 vineRoot() {
      return this.front(0.2, 3.4);
   }

   private Vec3 lantern(boolean left) {
      return this.side(left ? -0.75 : 0.75, 0.65, 4.7);
   }

   private static Vec3 mid(LivingEntity e) {
      return e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
   }

   private static Vec3 onGround(ServerLevel level, Vec3 p) {
      Vec3 g = BossFx.ground(level, p.x, p.y, p.z);
      return g != null ? g : p;
   }

   private static double flatDist(Vec3 a, Vec3 b) {
      double dx = b.x - a.x;
      double dz = b.z - a.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static Vec3 flat(Vec3 from, Vec3 to) {
      Vec3 d = new Vec3(to.x - from.x, 0.0, to.z - from.z);
      return d.lengthSqr() < 1.0E-6 ? Vec3.ZERO : d.normalize();
   }

   private Vec3 away(Vec3 from, LivingEntity e) {
      Vec3 d = flat(from, e.position());
      return d.lengthSqr() < 1.0E-6 ? this.facing() : d;
   }

   private static float yawTo(Vec3 from, Vec3 to) {
      return (float)(Mth.atan2(to.z - from.z, to.x - from.x) * 180.0F / (float)Math.PI) - 90.0F;
   }

   private void faceTo(Vec3 at) {
      if (!(flatDist(this.position(), at) < 0.01)) {
         float yaw = yawTo(this.position(), at);
         this.setYRot(yaw);
         this.setYBodyRot(yaw);
         this.setYHeadRot(yaw);
      }
   }

   private void commitYaw(int t, int from, int to) {
      if (t == from) {
         this.committedYaw = this.getYRot();
      } else if (t > from && t <= to) {
         this.setYRot(this.committedYaw);
         this.setYBodyRot(this.committedYaw);
         this.setYHeadRot(this.committedYaw);
      }
   }

   private float frenzy(boolean melee) {
      return this.phase() == 3 ? (melee ? 1.3F : 0.8F) : 1.0F;
   }

   private void afflict(LivingEntity e, Holder<MobEffect> effect, int ticks, int amplifier) {
      e.addEffect(new MobEffectInstance(effect, ticks, amplifier), this);
   }

   private List<ServerPlayer> nearest(int max) {
      List<ServerPlayer> list = new ArrayList<>(this.engaged());
      list.sort((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this)));
      return list.size() > max ? list.subList(0, max) : list;
   }

   private boolean hexed(LivingEntity e) {
      Integer until = this.hexedUntil.get(e.getUUID());
      return until != null && until > this.tickCount;
   }

   private float hit(LivingEntity e, float base) {
      return this.dmg(base) * (this.hexed(e) ? 1.2F : 1.0F);
   }

   private boolean smite(ServerLevel level, LivingEntity e, float amount, Vec3 push, double up) {
      if (!e.hurtServer(level, this.damageSources().mobAttack(this), amount)) {
         return false;
      }

      if (push.lengthSqr() > 1.0E-6 || up > 0.0) {
         e.push(push.x, up, push.z);
         e.syncVelocity = true;
      }

      return true;
   }

   private boolean deal(ServerLevel level, LivingEntity e, float base, Vec3 push, double up) {
      return this.smite(level, e, this.hit(e, base), push, up);
   }

   private void guardBreak(ServerLevel level, LivingEntity e, int ticks) {
      if (e.isBlocking()) {
         ItemStack s = e.getItemBlockingWith();
         if (s != null && e instanceof Player player) {
            player.getCooldowns().addCooldown(s, ticks);
         }

         e.stopUsingItem();
         Vfx.shards(level, e.getEyePosition(), 16777215, 12, 0.5);
         Vfx.glint(level, e.getEyePosition(), P.core(), 1.6);
      }
   }

   private List<LivingEntity> near(ServerLevel level, AABB box) {
      return level.getEntitiesOfClass(
         LivingEntity.class,
         box,
         e -> e != this && e.isAlive() && !(e instanceof MysticalBoss) && !(e instanceof Player pl && (pl.isCreative() || pl.isSpectator()))
      );
   }

   private List<LivingEntity> arc(ServerLevel level, float yaw, double reach, float halfAngle) {
      Vec3 o = this.position();
      Vec3 f = dirOf(yaw);
      double cos = Math.cos(Math.toRadians(halfAngle));
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : this.near(level, new AABB(o, o).inflate(reach + 1.0, 0.0, reach + 1.0).expandTowards(0.0, 5.5, 0.0).expandTowards(0.0, -2.0, 0.0))) {
         double dx = e.getX() - o.x;
         double dz = e.getZ() - o.z;
         double d = Math.sqrt(dx * dx + dz * dz);
         if (!(d > reach + e.getBbWidth() * 0.5) && (d < 1.0 || (dx * f.x + dz * f.z) / d >= cos)) {
            out.add(e);
         }
      }

      return out;
   }

   private List<LivingEntity> line(ServerLevel level, Vec3 from, Vec3 dir, double length, double halfWidth) {
      Vec3 d = new Vec3(dir.x, 0.0, dir.z);
      d = d.lengthSqr() < 1.0E-6 ? this.facing() : d.normalize();
      Vec3 to = from.add(d.scale(length));
      AABB box = new AABB(from, to).inflate(halfWidth + 1.0, 0.0, halfWidth + 1.0).expandTowards(0.0, 5.0, 0.0).expandTowards(0.0, -2.0, 0.0);
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : this.near(level, box)) {
         double rx = e.getX() - from.x;
         double rz = e.getZ() - from.z;
         double s = rx * d.x + rz * d.z;
         double off = Math.abs(rx * d.z - rz * d.x);
         if (s >= -0.5 && s <= length + 0.3 && off <= halfWidth + e.getBbWidth() * 0.5) {
            out.add(e);
         }
      }

      return out;
   }

   private List<LivingEntity> ringHit(ServerLevel level, Vec3 c, double r0, double r1) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : this.near(level, new AABB(c, c).inflate(r1 + 1.0, 2.5, r1 + 1.0))) {
         double d = flatDist(c, e.position());
         if (d >= r0 && d <= r1 && Math.abs(e.getY() - c.y) < 2.5) {
            out.add(e);
         }
      }

      return out;
   }

   private List<LivingEntity> column(ServerLevel level, Vec3 base, double r, double height) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : this.near(level, new AABB(base, base).inflate(r + 1.0, 0.0, r + 1.0).expandTowards(0.0, height, 0.0).expandTowards(0.0, -1.0, 0.0))) {
         if (flatDist(base, e.position()) <= r + 0.3) {
            out.add(e);
         }
      }

      return out;
   }

   private List<LivingEntity> swept(ServerLevel level, Vec3 a, Vec3 b, double r) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : this.near(level, new AABB(a, b).inflate(r))) {
         if (sweepT(a, b, e.getBoundingBox(), r) >= 0.0) {
            out.add(e);
         }
      }

      return out;
   }

   private static double sweepT(Vec3 a, Vec3 b, AABB box, double r) {
      double r2 = r * r;
      if (box.distanceToSqr(a) <= r2) {
         return 0.0;
      }

      double lo = 0.0;
      double hi = 1.0;

      for (int i = 0; i < 24; i++) {
         double m1 = lo + (hi - lo) / 3.0;
         double m2 = hi - (hi - lo) / 3.0;
         if (box.distanceToSqr(a.lerp(b, m1)) < box.distanceToSqr(a.lerp(b, m2))) {
            hi = m2;
         } else {
            lo = m1;
         }
      }

      double best = (lo + hi) / 2.0;
      if (box.distanceToSqr(a.lerp(b, best)) > r2) {
         return -1.0;
      }

      lo = 0.0;
      hi = best;

      for (int i = 0; i < 16; i++) {
         double m = (lo + hi) / 2.0;
         if (box.distanceToSqr(a.lerp(b, m)) <= r2) {
            hi = m;
         } else {
            lo = m;
         }
      }

      return hi;
   }

   private List<LivingEntity> pull(ServerLevel level, double radius, double strength, int t, Vfx.Palette pal) {
      Vec3 c = this.position();
      if (t % 2 == 0) {
         Vfx.gather(level, c.add(0.0, 0.8, 0.0), pal, radius, (int)Math.min(30.0, radius * 3.0));
      }

      List<LivingEntity> list = new ArrayList<>();

      for (LivingEntity e : this.near(level, new AABB(c, c).inflate(radius, 4.0, radius))) {
         if (!(flatDist(c, e.position()) > radius)) {
            list.add(e);
            Vec3 to = c.subtract(e.position());
            if (to.lengthSqr() > 1.0) {
               Vec3 v = to.normalize().scale(strength);
               e.push(v.x, Math.max(0.05, v.y), v.z);
               e.syncVelocity = true;
            }
         }
      }

      return list;
   }

   private void rot(ServerLevel level, LivingEntity e, int n) {
      if (e instanceof ServerPlayer sp && n > 0 && sp.isAlive() && !sp.isCreative() && !sp.isSpectator()) {
         int[] s = this.rotStacks.computeIfAbsent(sp.getUUID(), k -> new int[2]);
         s[0] += n;
         s[1] = this.tickCount;
         if (s[0] >= (this.phase() >= 3 ? 4 : 5)) {
            s[0] = 0;
            Vfx.flare(level, sp.getEyePosition(), TOX.core(), 1.6, 12);
            Vfx.gather(level, mid(sp), TOX, 1.2, 6);
            this.schedule(11, () -> this.bloom(level, sp));
         }
      }
   }

   private void bloom(ServerLevel level, ServerPlayer sp) {
      if (this.isAlive() && sp.isAlive() && sp.level() == level) {
         DamageSource src = this.damageSources().indirectMagic(this, this);
         sp.hurtServer(level, src, this.dmg(14.0F));
         this.afflict(sp, MobEffects.POISON, 100, 1);
         this.afflict(sp, MobEffects.SLOWNESS, 20, 3);

         for (LivingEntity o : this.ringHit(level, sp.position(), 0.0, 3.0)) {
            if (o != sp && o instanceof Player && o.hurtServer(level, src, this.dmg(8.0F))) {
               this.afflict(o, MobEffects.POISON, 60, 0);
            }
         }

         Vec3 eye = sp.getEyePosition();
         Vfx.nova(level, sp.position().add(0.0, 1.0, 0.0), TOX, 2.5);
         Vfx.debris(level, eye, Vfx.Debris.LEAF, TOX.glow(), 14, 0.35);
         Vfx.flare(level, eye, TOX.core(), 2.0, 8);
      }
   }

   private void tickRot(ServerLevel level) {
      if (this.tickCount % 10 == 0 && !this.hexedUntil.isEmpty()) {
         Iterator<Entry<UUID, Integer>> it = this.hexedUntil.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, Integer> en = it.next();
            Player pl = level.getPlayerByUUID(en.getKey());
            if (pl != null && pl.isAlive() && en.getValue() > this.tickCount) {
               Vfx.ring(level, pl.position(), HEX.glow(), 1.0, 10);
            } else {
               it.remove();
            }
         }
      }

      if (this.tickCount % 20 == 0 && !this.rotStacks.isEmpty()) {
         Iterator<Entry<UUID, int[]>> it = this.rotStacks.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, int[]> en = it.next();
            Player pl = level.getPlayerByUUID(en.getKey());
            int[] s = en.getValue();
            if (pl != null && pl.isAlive()) {
               if (this.tickCount - s[1] >= 80) {
                  s[0]--;
                  s[1] = this.tickCount;
               }

               if (pl.isInWater()) {
                  s[0]--;
               }

               if (s[0] <= 0) {
                  it.remove();
               } else {
                  Vfx.wisps(level, pl.getEyePosition().add(0.0, 0.6, 0.0), TOX, s[0], 0.7, 22);
               }
            } else {
               it.remove();
            }
         }
      }
   }

   private void pool(ServerLevel level, Vec3 at, double r, int ticks, int kind) {
      int life = this.enraged() ? ticks * 3 / 2 : ticks;
      if (this.pools.size() >= 10) {
         this.pools.remove(0);
      }

      this.pools.add(new BogrotBoss.Pool(at, r, kind, this.tickCount + life));
      Vfx.groundCrack(level, at, r, kind == 0 ? TOX : P);
   }

   private void tickPools(ServerLevel level) {
      if (!this.pools.isEmpty()) {
         boolean fx = this.tickCount % 20 == 0;
         boolean sting = this.tickCount % 10 == 0;
         Iterator<BogrotBoss.Pool> it = this.pools.iterator();

         while (it.hasNext()) {
            BogrotBoss.Pool p = it.next();
            Vfx.Palette pal = p.kind() == 0 ? TOX : P;
            if (this.tickCount >= p.end()) {
               Vfx.smoke(level, p.at(), pal.smoke(), 6, p.r() * 0.7, 1.2);
               it.remove();
            } else {
               if (fx) {
                  Vfx.aura(level, p.at(), pal, p.r());
                  Vfx.smoke(level, p.at(), pal.smoke(), 3, p.r() * 0.6, 1.0);
               }

               if (sting) {
                  for (ServerPlayer pl : this.engaged()) {
                     if (!(flatDist(p.at(), pl.position()) >= p.r()) && !(Math.abs(pl.getY() - p.at().y) > 2.0)) {
                        DamageSource src = this.damageSources().indirectMagic(this, this);
                        if (p.kind() == 0) {
                           pl.hurtServer(level, src, this.dmg(2.0F));
                           this.afflict(pl, MobEffects.POISON, 40, 0);
                           if (this.tickCount % 40 == 0) {
                              this.rot(level, pl, 1);
                           }
                        } else {
                           pl.hurtServer(level, src, this.dmg(1.5F));
                           this.afflict(pl, MobEffects.SLOWNESS, 30, 1);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void mend(ServerLevel level) {
      if (this.tickCount % 20 == 0 && !(this.getHealth() >= this.getMaxHealth())) {
         boolean in = this.isInWater();
         boolean black = false;

         for (BogrotBoss.Pool p : this.pools) {
            if (flatDist(this.position(), p.at()) < p.r() && Math.abs(this.getY() - p.at().y) < 3.0) {
               in = true;
               black |= p.kind() == 1;
            }
         }

         if (in) {
            this.heal(this.getMaxHealth() * (this.phase() == 3 ? 0.0035F : 0.0025F) * (black ? 2 : 1));
            Vfx.auraRing(level, this.position(), P, 1.6);
         }
      }
   }

   private void addGlob(BogrotBoss.Glob g) {
      g.born = this.tickCount;
      if (this.globs.size() >= 24) {
         this.globs.remove(0);
      }

      this.globs.add(g);
   }

   private static Vfx.Palette palOf(BogrotBoss.Glob g) {
      return switch (g.kind) {
         case 0, 4 -> TOX;
         default -> WISP;
         case 2 -> HEX;
      };
   }

   private void tickGlobs(ServerLevel level) {
      if (!this.globs.isEmpty()) {
         for (BogrotBoss.Glob g : new ArrayList<>(this.globs)) {
            if (g.born != this.tickCount) {
               g.age++;
               if (g.homing > 0.0 && g.target != null && g.target.isAlive() && g.target.level() == level) {
                  Vec3 to = g.target.getEyePosition().subtract(g.pos);
                  if (to.lengthSqr() > 1.0E-4) {
                     g.vel = g.vel.lerp(to.normalize().scale(g.speed), g.homing);
                  }
               }

               Vec3 from = g.pos;
               g.vel = g.vel.add(0.0, -g.gravity, 0.0);
               g.pos = g.pos.add(g.vel);
               switch (g.kind) {
                  case 0:
                     Vfx.meteorTrail(level, g.pos, g.vel, TOX);
                     break;
                  case 1:
                  case 3:
                  default:
                     Vfx.trail(level, g.pos, WISP, 0.55);
                     break;
                  case 2:
                     Vfx.trail(level, g.pos, HEX, 0.6);
                     if (g.age % 3 == 0) {
                        Vfx.streaks(level, g.pos, g.vel.scale(-1.0), HEX, 3);
                     }
                     break;
                  case 4:
                     Vfx.trail(level, g.pos, TOX, 0.5);
               }

               if (g.landing == null) {
                  BlockPos bp = BlockPos.containing(g.pos);
                  if (!level.isLoaded(bp)) {
                     this.globs.remove(g);
                  } else {
                     BlockHitResult wall = level.clip(new ClipContext(from, g.pos, Block.COLLIDER, Fluid.NONE, this));
                     boolean blocked = wall.getType() != Type.MISS;
                     Vec3 end = blocked ? wall.getLocation() : g.pos;
                     List<LivingEntity> hit = this.swept(level, from, end, g.radius);
                     if (hit.isEmpty()) {
                        if (blocked) {
                           this.globs.remove(g);
                           g.pos = end;
                           Vfx.impact(level, g.pos, palOf(g), 0.4);
                           Vfx.sparks(level, g.pos, palOf(g).glow(), 6, 0.25, 0.16);
                        } else if (g.age >= g.life) {
                           this.globs.remove(g);
                           Vfx.smoke(level, g.pos, palOf(g).smoke(), 3, 0.3, 0.8);
                        }
                     } else {
                        this.globs.remove(g);
                        double first = 1.0;

                        for (LivingEntity e : hit) {
                           first = Math.min(first, sweepT(from, end, e.getBoundingBox(), g.radius));
                        }

                        g.pos = from.lerp(end, first);

                        for (LivingEntity e : hit) {
                           this.globHit(level, g, e);
                        }
                     }
                  }
               } else if (g.age >= g.life) {
                  this.globs.remove(g);
                  this.bileLand(level, g.landing);
               }
            }
         }
      }
   }

   private void globHit(ServerLevel level, BogrotBoss.Glob g, LivingEntity e) {
      e.setInvulnerableTime(0);
      switch (g.kind) {
         case 1:
            if (e.isFallFlying() || !e.onGround()) {
               e.stopFallFlying();
               Vec3 v = e.getDeltaMovement();
               e.setDeltaMovement(v.x * 0.3, -1.4, v.z * 0.3);
               e.syncVelocity = true;
               this.afflict(e, MobEffects.SLOWNESS, 40, 1);
            }

            if (this.deal(level, e, 10.0F, Vec3.ZERO, 0.0)) {
               this.afflict(e, MobEffects.POISON, 40, 0);
            }

            Vfx.impact(level, g.pos, WISP, 0.8);
            break;
         case 2:
            this.deal(level, e, 8.0F, Vec3.ZERO, 0.0);
            this.afflict(e, MobEffects.WEAKNESS, 160, 0);
            this.afflict(e, MobEffects.SLOWNESS, 160, 0);
            this.rot(level, e, 2);
            this.hexedUntil.put(e.getUUID(), this.tickCount + 160);
            if (e instanceof ServerPlayer sp) {
               this.markTarget(sp, 160);
            }

            Vfx.nova(level, mid(e), HEX, 2.0);
            Vfx.runeCircle(level, onGround(level, e.position()), 1.6, HEX, 40, 0.08F, Vfx.Rune.GLYPH);
            Vfx.flare(level, e.getEyePosition(), HEX.glow(), 2.4, 8);
            break;
         case 3:
            if (this.deal(level, e, 14.0F, Vec3.ZERO, 0.3)) {
               this.afflict(e, MobEffects.POISON, 40, 0);
               this.rot(level, e, 1);
            }

            Vfx.impact(level, g.pos, WISP, 1.0);
            break;
         case 4:
            if (this.deal(level, e, 12.0F, Vec3.ZERO, 0.0)) {
               this.afflict(e, MobEffects.POISON, 60, 0);
            }

            Vfx.impact(level, g.pos, TOX, 0.6);
            BossSounds.play(level, g.pos, "bolt.spore.impact", 1.6F, 1.0F);
      }
   }

   private void bileLand(ServerLevel level, Vec3 at) {
      for (LivingEntity e : this.ringHit(level, at, 0.0, 2.4)) {
         if (this.deal(level, e, 16.0F, this.away(at, e).scale(0.5), 0.3)) {
            this.afflict(e, MobEffects.POISON, 80, 0);
            this.rot(level, e, 1);
         }
      }

      this.pool(level, at, this.phase() >= 2 ? 2.6 : 2.2, 120, 0);
      Vfx.explosion(level, at, TOX, 2.2);
   }

   private void releaseGrab(ServerLevel level, boolean dropped) {
      LivingEntity e = this.grabbed;
      this.grabbed = null;
      if (e != null && dropped && e.isAlive()) {
         Vec3 a = this.away(this.position(), e).scale(0.5);
         e.setDeltaMovement(a.x, -0.5, a.z);
         e.syncVelocity = true;
         Vfx.impact(level, mid(e), P, 0.6);
         Vfx.debris(level, mid(e), Vfx.Debris.LEAF, P.glow(), 10, 0.3);
      }
   }

   private void grasp(ServerLevel level) {
      Vec3 from = this.front(1.0, 0.0);
      LivingEntity best = null;
      double bestD = Double.MAX_VALUE;

      for (LivingEntity e : this.line(level, from, this.facing(), 9.0, 1.0)) {
         double d = e.distanceToSqr(this);
         if (d < bestD) {
            best = e;
            bestD = d;
         }
      }

      Vfx.claw(level, this.front(1.5, 2.6), this.getYRot(), 0.0F, 7.0, P);
      if (best != null) {
         this.guardBreak(level, best, 40);
         this.deal(level, best, 8.0F, Vec3.ZERO, 0.0);
         if (best.isAlive()) {
            this.grabbed = best;
            this.grabDamage = 0.0F;
            Vfx.dashTrail(level, this.front(1.0, 2.6), mid(best), P);
         }
      }
   }

   private void holdGrab(ServerLevel level, int t) {
      LivingEntity e = this.grabbed;
      if (e != null) {
         Vec3 holdAt = this.front(2.6, 3.2);
         if (e.isAlive()
            && (t <= 18 || !(e.position().distanceTo(holdAt) > 5.0))
            && !(e instanceof Player pl && (pl.isCreative() || pl.isSpectator()))) {
            Vec3 v = holdAt.subtract(e.position()).scale(0.55);
            if (v.lengthSqr() > 4.0) {
               v = v.normalize().scale(2.0);
            }

            e.setDeltaMovement(v);
            e.syncVelocity = true;
            e.fallDistance = 0.0;
            if (t % 10 == 0) {
               this.afflict(e, MobEffects.POISON, 40, 0);
               this.rot(level, e, 1);
            }

            if (t % 4 == 0) {
               Vfx.gather(level, holdAt, P, 1.0, 4);
            }
         } else {
            this.releaseGrab(level, false);
         }
      }
   }

   private void graspSlam(ServerLevel level) {
      Vec3 g = onGround(level, this.front(3.0, 0.0));
      LivingEntity e = this.grabbed;
      this.grabbed = null;
      if (e != null && e.isAlive()) {
         this.guardBreak(level, e, 40);
         e.setDeltaMovement(0.0, -1.5, 0.0);
         e.syncVelocity = true;
         if (this.deal(level, e, 30.0F, this.facing().scale(1.2), 0.0)) {
            this.afflict(e, MobEffects.SLOWNESS, 40, 2);
            this.afflict(e, MobEffects.NAUSEA, 60, 0);
            this.rot(level, e, 2);
         }
      } else {
         for (LivingEntity v : this.ringHit(level, g, 0.0, 2.5)) {
            this.deal(level, v, 14.0F, Vec3.ZERO, 0.0);
         }
      }

      Vfx.heavyImpact(level, g, P, 1.2);
      Vfx.groundCrack(level, g, 2.5, P);
      Vfx.debris(level, g, Vfx.Debris.LEAF, P.glow(), 16, 0.4);
   }

   private void tickGrab(ServerLevel level) {
      BossAction cur = this.currentMove();
      if (this.grabbed != null && (cur == null || !cur.anim().equals("hag_grasp"))) {
         this.releaseGrab(level, false);
      }
   }

   private void countMinions(ServerLevel level) {
      if (this.tickCount - this.minionCheck >= 10) {
         this.minionCheck = this.tickCount;
         int t = 0;
         int l = 0;

         for (RealmMob m : level.getEntitiesOfClass(RealmMob.class, this.getBoundingBox().inflate(32.0), LivingEntity::isAlive)) {
            if (m.bossId().equals("bog_thrall")) {
               t++;
            } else if (m.bossId().equals("bog_leech")) {
               l++;
            }
         }

         this.thralls = t;
         this.leeches = l;
      }
   }

   private int thrallCount() {
      if (this.level() instanceof ServerLevel level) {
         this.countMinions(level);
      }

      return this.thralls;
   }

   @Override
   protected void defineActions(List<BossAction> out) {
      this.cronesMoves(out);
      this.huntingMoves(out);
      this.rangedMoves(out);
      this.areaMoves(out);
      this.utilityMoves(out);
      this.vineMoves(out);
      this.ultimates(out);
   }

   private void cronesMoves(List<BossAction> out) {
      out.add(
         BossAction.of("willow_lash", 1.2, 0.5)
            .range(0.0, 6.0)
            .cooldown(50)
            .weight(14)
            .tags("melee")
            .advance(0.3, 0.5, 0.1)
            .combo("twin_rake", 0.3F)
            .utility((b, t, c) -> this.frenzy(true))
            .tick((b, level, target, t) -> {
               if (t >= 2 && t <= 8 && t % 2 == 0) {
                  Vfx.gather(level, this.side(1.1, 0.4, 2.8), P, 1.2, 4);
               }

               this.commitYaw(t, 4, 10);
               if (t == 4) {
                  Vfx.telegraphCone(level, this.position(), this.getYRot(), 65.0F, 5.5, 6, P);
               }
            })
            .hit((b, level, target, i) -> {
               float yaw = this.getYRot();
               Vec3 left = dirOf(yaw - 90.0F);

               for (LivingEntity e : this.arc(level, yaw, 5.5, 65.0F)) {
                  if (this.deal(level, e, 16.0F, left.scale(0.8), 0.25)) {
                     this.afflict(e, MobEffects.POISON, 60, 0);
                     this.rot(level, e, 1);
                  }

                  Vfx.impact(level, mid(e), P, 0.5);
               }

               Vfx.slashArc(level, this.front(0.4, 3.0), yaw, 10.0F, 25.0F, 5.5, P, 200.0F);
            })
      );
      out.add(
         BossAction.of("twin_rake", 1.5, 0.45, 0.95)
            .range(0.0, 5.5)
            .cooldown(70)
            .weight(12)
            .tags("melee")
            .advance(0.25, 0.45, 0.16)
            .advance(0.75, 0.95, 0.14)
            .combo("rending_feint", 0.25F)
            .combo("rending_verdict", 0.35F)
            .utility((b, t, c) -> this.frenzy(true))
            .tick((b, level, target, t) -> {
               this.commitYaw(t, 3, 9);
               this.commitYaw(t, 13, 19);
               if (t == 3) {
                  Vfx.telegraphCone(level, this.position(), this.getYRot() - 12.0F, 55.0F, 5.0, 6, P);
               } else if (t == 13) {
                  Vfx.telegraphCone(level, this.position(), this.getYRot() + 12.0F, 55.0F, 5.0, 6, P);
               }
            })
            .hit((b, level, target, i) -> {
               float s = i == 0 ? -1.0F : 1.0F;
               float yaw = this.getYRot() + 12.0F * s;
               Vec3 fwd = this.facing();

               for (LivingEntity e : this.arc(level, yaw, 5.0, 55.0F)) {
                  if (this.deal(level, e, 18.0F, fwd.scale(0.5), 0.2)) {
                     this.rot(level, e, 1);
                     if (i == 1) {
                        this.afflict(e, MobEffects.POISON, 40, 1);
                     }
                  }

                  Vfx.impact(level, mid(e), P, 0.5);
               }

               Vfx.claw(level, this.front(0.5, 3.0), yaw, 15.0F, 5.0, P);
               Vfx.slashArc(level, this.front(0.5, 3.0), this.getYRot(), 15.0F, -45.0F * s, 5.0, P, -150.0F * s);
            })
      );
      out.add(
         BossAction.of("rending_verdict", 1.1, 0.4)
            .range(0.0, 5.5)
            .cooldown(140)
            .weight(6)
            .tags("melee guard_break")
            .advance(0.15, 0.35, 0.12)
            .combo("weeping_lash", 0.25F)
            .utility((b, t, c) -> this.frenzy(true))
            .tick((b, level, target, t) -> {
               this.commitYaw(t, 2, 8);
               if (t == 2) {
                  Vfx.telegraphLine(level, this.position(), this.facing(), 5.5, 2.8, 6, P);
               }

               if (t <= 7 && t % 2 == 1) {
                  Vfx.gather(level, this.front(0.2, 5.2), P, 1.5, 5);
               }
            })
            .hit((b, level, target, i) -> this.verdict(level, 5.5, 28.0F))
      );
      out.add(
         BossAction.of("rending_feint", 1.7, 1.0)
            .range(0.0, 5.5)
            .cooldown(140)
            .weight(0)
            .tags("melee guard_break")
            .advance(0.7, 0.95, 0.14)
            .combo("weeping_lash", 0.25F)
            .tick((b, level, target, t) -> {
               this.commitYaw(t, 2, 20);
               if (t == 2) {
                  Vfx.telegraphLine(level, this.position(), this.facing(), 6.0, 2.8, 18, P);
               }

               if (t >= 6 && t <= 16 && t % 4 == 2) {
                  Vfx.embers(level, this.front(0.2, 5.2), HEX.glow(), 3, 0.6);
               }
            })
            .hit((b, level, target, i) -> this.verdict(level, 6.0, 30.0F))
      );
      out.add(
         BossAction.of("hag_grasp", 2.4, 0.7, 1.75)
            .range(2.5, 9.0)
            .cooldown(240)
            .phase(1)
            .weight(8)
            .tags("melee punish")
            .lockFacing()
            .utility(
               (b, t, c) -> (c.targetBlocking ? 2.0F : 1.0F) * (c.targetHealth < 0.4F ? 1.5F : 1.0F) * (c.targetHealing ? 1.5F : 1.0F) * this.frenzy(true)
            )
            .start((b, level, target, i) -> {
               this.grabbed = null;
               if (target != null) {
                  this.faceTo(this.predict(target, 14.0));
               }

               Vfx.telegraphLine(level, this.front(1.0, 0.0), this.facing(), 9.0, 1.8, 14, P);
            })
            .tick((b, level, target, t) -> {
               if (t == 15) {
                  Vfx.telegraphCircle(level, onGround(level, this.front(3.0, 0.0)), 2.5, 20, P);
               }

               if (t >= 15 && t <= 34) {
                  this.holdGrab(level, t);
               }
            })
            .hit((b, level, target, i) -> {
               if (i == 0) {
                  this.grasp(level);
               } else {
                  this.graspSlam(level);
               }
            })
            .end((b, level, target, i) -> this.releaseGrab(level, i == -2))
      );
      out.add(
         BossAction.of("rootbound_stomp", 1.4, 0.75)
            .range(0.0, 5.0)
            .cooldown(110)
            .weight(9)
            .tags("melee aoe")
            .combo("weeping_lash", 0.35F)
            .combo("willow_lash", 0.2F)
            .utility((b, t, c) -> this.frenzy(true))
            .tick((b, level, target, t) -> {
               if (t == 4) {
                  Vfx.telegraphCircle(level, onGround(level, this.position()), 5.5, 11, P);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3 c = onGround(level, this.position());

               for (LivingEntity e : this.ringHit(level, c, 0.0, 5.5)) {
                  double d = flatDist(c, e.position());
                  if (this.smite(level, e, this.hit(e, 32.0F) * (float)(0.4 + 0.6 * (1.0 - d / 5.5)), this.away(c, e).scale(1.4), 0.35)) {
                     this.afflict(e, MobEffects.SLOWNESS, 30, 3);
                     this.rot(level, e, 1);
                  }
               }

               Vfx.heavyImpact(level, c, P, 1.2);
               Vfx.groundCrack(level, c, 5.5, P);
               BossFx.ring(level, c.add(0.0, 0.2, 0.0), 4.5, 40, ROOTS, 2);
            })
      );
      out.add(
         BossAction.of("weeping_lash", 1.3, 0.55)
            .range(0.0, 7.0)
            .cooldown(90)
            .weight(10)
            .tags("melee behind")
            .lockFacing()
            .when((b, t) -> this.ctx().targetBehind)
            .tick((b, level, target, t) -> {
               if (t == 3) {
                  Vfx.telegraphCone(level, this.position(), this.getYRot() + 180.0F, 75.0F, 6.5, 8, P);
               }
            })
            .hit((b, level, target, i) -> {
               float yaw = this.getYRot() + 180.0F;

               for (LivingEntity e : this.arc(level, yaw, 6.5, 75.0F)) {
                  if (this.deal(level, e, 20.0F, this.away(this.position(), e).scale(1.6), 0.35)) {
                     this.afflict(e, MobEffects.POISON, 60, 0);
                     this.rot(level, e, 1);
                  }

                  Vfx.impact(level, mid(e), P, 0.5);
               }

               Vfx.slashArc(level, this.side(0.0, -0.6, 3.2), yaw, 10.0F, 0.0F, 6.5, P, 220.0F);
               Vfx.debris(level, this.side(0.0, -2.0, 2.5), Vfx.Debris.LEAF, P.glow(), 20, 0.5);
               Vfx.streaks(level, this.side(0.0, -1.0, 2.5), dirOf(yaw), P, 10);
            })
      );
   }

   private void verdict(ServerLevel level, double reach, float base) {
      Vec3 fwd = this.facing();

      for (LivingEntity e : this.line(level, this.position(), fwd, reach, 1.4)) {
         this.guardBreak(level, e, 100);
         if (this.deal(level, e, base, fwd, 0.1)) {
            this.afflict(e, MobEffects.SLOWNESS, 30, 1);
            this.rot(level, e, 1);
         }
      }

      Vec3 g = onGround(level, this.front(reach - 2.0, 0.0));
      Vfx.slashArc(level, this.front(0.3, 3.2), this.getYRot(), 0.0F, 90.0F, reach - 0.5, P, 160.0F);
      Vfx.heavyImpact(level, g, P, 1.0);
      Vfx.groundCrack(level, g, 2.5, P);
   }

   private void huntingMoves(List<BossAction> out) {
      out.add(
         BossAction.of("drowned_lunge", 1.6, 0.9)
            .range(7.0, 16.0)
            .cooldown(140)
            .weight(10)
            .tags("gap_closer melee")
            .lockFacing()
            .advance(0.35, 0.9, 0.7)
            .combo("twin_rake", 0.35F)
            .utility((b, t, c) -> (c.kitingTicks > 40 ? 1.5F : 1.0F) * this.frenzy(true))
            .start((b, level, target, i) -> {
               this.trampled.clear();
               if (target != null) {
                  this.faceTo(this.predict(target, 18.0));
               }

               this.lungeFrom = this.position();
               Vfx.telegraphLine(level, this.position(), this.facing(), Mth.clamp(this.ctx().hDist + 2.0, 8.0, 14.0), 2.6, 18, P);
            })
            .tick((b, level, target, t) -> {
               if (t >= 7 && t <= 17) {
                  this.trample(level, t);
               }

               if (t >= 7 && t <= 18 && target != null && flatDist(this.position(), target.position()) < this.getBbWidth() * 0.5 + 1.4) {
                  this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
               }
            })
            .hit((b, level, target, i) -> {
               float yaw = this.getYRot();
               Vec3 fwd = this.facing();

               for (LivingEntity e : this.arc(level, yaw, 5.0, 70.0F)) {
                  if (this.deal(level, e, 24.0F, fwd.scale(1.2), 0.3)) {
                     this.rot(level, e, 1);
                  }

                  Vfx.impact(level, mid(e), P, 0.6);
               }

               Vfx.claw(level, this.front(0.5, 2.2), yaw, 20.0F, 4.5, P);
               Vfx.heavyImpact(level, onGround(level, this.front(2.8, 0.0)), P, 0.8);
               Vfx.dashTrail(level, this.lungeFrom.add(0.0, 1.2, 0.0), this.position().add(0.0, 1.2, 0.0), P);
            })
      );
      out.add(
         BossAction.of("mire_dive", 2.6, 0.8, 2.0)
            .range(7.0, 28.0)
            .cooldown(300)
            .phase(1)
            .weight(7)
            .tags("mobility gap_closer aoe")
            .lockFacing()
            .combo("willow_lash", 0.4F)
            .utility((b, t, c) -> c.kitingTicks <= 60 && !c.targetUnreachable ? 1.0F : 1.8F)
            .tick((b, level, target, t) -> {
               if (t == 2) {
                  Vfx.groundCrack(level, onGround(level, this.position()), 3.0, P);
               }

               if (t >= 2 && t <= 15 && t % 3 == 2) {
                  level.sendParticles(MUD, this.getX(), this.getY() + 0.3, this.getZ(), 16, 1.2, 0.2, 1.2, 0.12);
                  Vfx.ring(level, this.position(), P.glow(), 2.0 + t * 0.1, 8);
               }

               if (t == 12) {
                  this.submerged = true;
               }

               if (t >= 17 && t <= 39 && t % 4 == 1) {
                  Vec3 e = this.diveAt;
                  Vfx.smoke(level, e, P.smoke(), 3, 1.2, 1.0);
                  level.sendParticles(ParticleTypes.BUBBLE_POP, e.x, e.y + 0.2, e.z, 12, 1.0, 0.1, 1.0, 0.02);
               }

               if (t == 36) {
                  this.submerged = false;
               }
            })
            .hit((b, level, target, i) -> {
               if (i == 0) {
                  this.diveUnder(level, target);
               } else {
                  this.diveErupt(level);
               }
            })
            .end((b, level, target, i) -> this.submerged = false)
      );
      out.add(
         BossAction.of("hag_pounce", 2.2, 0.6, 1.5)
            .range(8.0, 14.0)
            .cooldown(220)
            .phase(1)
            .weight(8)
            .tags("gap_closer aoe")
            .lockFacing()
            .utility((b, t, c) -> c.targetUnreachable && c.heightDiff > 2.0 ? 1.5F : 1.0F)
            .start((b, level, target, i) -> {
               this.pouncing = false;
               if (target != null) {
                  this.faceTo(this.predict(target, 30.0));
               }
            })
            .hit((b, level, target, i) -> {
               if (i == 0 && target != null) {
                  Vec3 l = onGround(level, this.predict(target, 18.0));
                  int n = Mth.clamp((int)Math.round(12.0 + flatDist(this.position(), l) * 0.6), 14, 26);
                  Vfx.telegraphCircle(level, l, 4.0, n, P);
                  Vec3 from = this.position();
                  this.leapTo(l, Math.max(5.0, l.y - this.getY() + 4.0));
                  this.pouncing = true;
                  Vfx.dashTrail(level, from, from.add(0.0, 2.0, 0.0), P);
                  Vfx.streaks(level, from, UP, P, 8);
               }
            })
            .end((b, level, target, i) -> {
               if (i == -2) {
                  this.pouncing = false;
               }
            })
      );
   }

   private void trample(ServerLevel level, int t) {
      Vec3 right = dirOf(this.getYRot() + 90.0F);

      for (LivingEntity e : this.near(level, this.getBoundingBox().inflate(1.0, 0.5, 1.0))) {
         if (!(flatDist(this.position(), e.position()) > 1.8 + e.getBbWidth() * 0.5) && this.trampled.add(e.getId())) {
            Vec3 rel = e.position().subtract(this.position());
            Vec3 aside = rel.x * right.x + rel.z * right.z >= 0.0 ? right : right.scale(-1.0);
            this.deal(level, e, 10.0F, aside.scale(0.9), 0.3);
         }
      }

      if (t % 2 == 1) {
         Vfx.dashTrail(level, this.position().subtract(this.facing().scale(1.6)).add(0.0, 1.2, 0.0), this.position().add(0.0, 1.2, 0.0), P);
         level.sendParticles(MUD, this.getX(), this.getY() + 0.2, this.getZ(), 10, 1.0, 0.1, 1.0, 0.1);
      }
   }

   private void diveUnder(ServerLevel level, @Nullable LivingEntity target) {
      if (target == null) {
         this.submerged = false;
         this.cancelMove(level, 10);
      } else {
         Vec3 ahead = this.predict(target, 24.0);
         Vec3 look = target.getLookAngle().horizontal();
         if (look.lengthSqr() < 1.0E-4) {
            look = flat(this.position(), target.position());
         }

         Vec3 e = onGround(level, ahead.subtract(look.normalize().scale(2.5)));
         if (!BossFx.safeGround(level, e, 3.0)) {
            e = onGround(level, ahead);
         }

         if (!this.blinkTo(e)) {
            this.submerged = false;
            this.cancelMove(level, 10);
         } else {
            this.faceTo(target.position());
            this.diveAt = onGround(level, this.position());
            Vfx.telegraphCircle(level, this.diveAt, 3.5, 24, P);
         }
      }
   }

   private void diveErupt(ServerLevel level) {
      this.submerged = false;
      Vec3 c = this.diveAt;

      for (LivingEntity e : this.ringHit(level, c, 0.0, 3.5)) {
         if (this.deal(level, e, 30.0F, this.away(c, e), 0.9)) {
            this.afflict(e, MobEffects.POISON, 60, 1);
            this.rot(level, e, 1);
         }
      }

      Vfx.heavyImpact(level, c, P, 1.5);
      Vfx.pillar(level, c, TOX, 6.0, 1.4);
      Vfx.groundCrack(level, c, 3.5, P);
      Vfx.debris(level, c.add(0.0, 1.0, 0.0), Vfx.Debris.LEAF, P.glow(), 20, 0.5);
   }

   @Override
   protected void onLand(ServerLevel level) {
      if (this.pouncing) {
         this.pouncing = false;
         Vec3 c = this.position();

         for (LivingEntity e : this.ringHit(level, c, 0.0, 4.0)) {
            double d = flatDist(c, e.position());
            if (this.smite(level, e, this.hit(e, 36.0F) * (float)(0.5 + 0.5 * (1.0 - d / 4.0)), this.away(c, e).scale(1.5), 0.4)) {
               this.afflict(e, MobEffects.SLOWNESS, 40, 1);
               this.rot(level, e, 1);
            }
         }

         Vfx.heavyImpact(level, c, P, 1.6);
         Vfx.groundCrack(level, c, 4.0, P);
         Vfx.debris(level, c.add(0.0, 1.0, 0.0), Vfx.Debris.LEAF, P.glow(), 20, 0.5);
      }
   }

   private void rangedMoves(List<BossAction> out) {
      out.add(
         BossAction.of("bile_spit", 1.3, 0.6)
            .range(6.0, 26.0)
            .cooldown(60)
            .weight(11)
            .tags("ranged")
            .utility((b, t, c) -> (c.kitingTicks > 40 ? 1.6F : 1.0F) * this.frenzy(false))
            .tick((b, level, target, t) -> {
               if (t >= 4 && t <= 11 && t % 2 == 0) {
                  Vfx.gather(level, this.mouth(), TOX, 1.4, 5);
               }
            })
            .hit((b, level, target, i) -> {
               if (target != null) {
                  this.spit(level, target);
               }
            })
      );
      out.add(
         BossAction.of("retch_barrage", 1.8, 0.55, 0.9, 1.25)
            .range(5.0, 26.0)
            .cooldown(120)
            .weight(8)
            .tags("ranged punish")
            .utility((b, t, c) -> (c.targetAiming ? 1.5F : 1.0F) * (c.kitingTicks > 40 ? 1.6F : 1.0F) * this.frenzy(false))
            .start((b, level, target, i) -> Arrays.fill(this.retchAims, null))
            .tick((b, level, target, t) -> {
               if (target != null && (t == 6 || t == 13 || t == 20)) {
                  this.retchAim(level, target, (t - 6) / 7);
               }
            })
            .hit((b, level, target, i) -> this.retch(level, target, i))
      );
      out.add(
         BossAction.of("witchfire_wisps", 1.6, 0.8)
            .range(0.0, 28.0)
            .cooldown(160)
            .weight(5)
            .tags("ranged anti_air")
            .start((b, level, target, i) -> Vfx.chargeUp(level, this.front(0.2, 5.4), WISP, 16))
            .hit((b, level, target, i) -> this.witchfire(level, target))
      );
      out.add(
         BossAction.of("hex_of_the_crone", 1.8, 0.9)
            .range(4.0, 24.0)
            .cooldown(320)
            .weight(6)
            .tags("ranged punish")
            .utility((b, t, c) -> (c.dist > 10.0 ? 1.5F : 1.0F) * this.frenzy(false))
            .start((b, level, target, i) -> {
               Vfx.chargeUp(level, this.front(2.4, 3.2), HEX, 18);
               if (target != null) {
                  Vfx.runeCircle(level, onGround(level, target.position()), 1.8, HEX, 18, 0.06F, Vfx.Rune.GLYPH);
               }
            })
            .hit((b, level, target, i) -> {
               if (target != null) {
                  Vec3 from = this.side(-0.9, 4.2, 2.3);
                  BogrotBoss.Glob g = new BogrotBoss.Glob(2, from, target.getEyePosition().subtract(from).normalize().scale(0.8), 50);
                  g.target = target;
                  g.homing = 0.18;
                  g.speed = 0.8;
                  g.radius = 1.2;
                  this.addGlob(g);
                  Vfx.flare(level, from, HEX.glow(), 1.8, 6);
               }
            })
      );
   }

   private void spit(ServerLevel level, LivingEntity target) {
      int n = Mth.clamp((int)Math.round(flatDist(this.position(), target.position()) * 1.1), 14, 26);
      int count = this.phase() == 0 ? 1 : (this.phase() == 1 ? 3 : 5);
      Vec3 from = this.mouth();
      Vec3 aim = this.predict(target, n);
      Vec3 dir = flat(this.position(), aim);
      Vec3 perp = new Vec3(-dir.z, 0.0, dir.x);

      for (int k = 0; k < count; k++) {
         Vec3 l = onGround(level, aim.add(perp.scale((k - (count - 1) / 2.0) * 2.8)));
         Vec3 v = l.subtract(from).scale(1.0 / n).add(0.0, 0.025 * (n + 1), 0.0);
         BogrotBoss.Glob g = new BogrotBoss.Glob(0, from, v, n);
         g.gravity = 0.05;
         g.landing = l;
         g.radius = 2.4;
         this.addGlob(g);
         Vfx.telegraphCircle(level, l, 2.4, n, TOX);
      }

      Vfx.flare(level, from, TOX.glow(), 2.0, 6);
      Vfx.sparks(level, from, TOX.core(), 8, 0.3, 0.18);
   }

   private void retchAim(ServerLevel level, LivingEntity target, int k) {
      double dist = Math.sqrt(this.distanceToSqr(target));
      Vec3 a = this.predict(target, dist / 1.5).add(0.0, 1.0, 0.0);
      Vec3 m = this.mouth();
      Vec3 d = a.subtract(m).yRot(-RETCH_YAW[k] * (float) (Math.PI / 180.0));
      this.retchAims[k] = m.add(d);
      Vfx.telegraphLine(level, onGround(level, this.position()), this.retchAims[k].subtract(this.position()), d.length(), 0.9, 5, TOX);
   }

   private void retch(ServerLevel level, @Nullable LivingEntity target, int k) {
      Vec3 aim = k >= 0 && k < 3 ? this.retchAims[k] : null;
      if (aim == null) {
         if (target == null) {
            return;
         }

         aim = target.getEyePosition();
      }

      Vec3 m = this.mouth();
      Vec3 dir = aim.subtract(m).normalize();
      BogrotBoss.Glob g = new BogrotBoss.Glob(4, m, dir.scale(1.5), 40);
      g.radius = 0.7;
      this.addGlob(g);
      BossSounds.play(level, m, "bolt.spore.launch", 1.4F, 1.0F);
      Vfx.flare(level, m, TOX.glow(), 1.6, 5);
      Vfx.streaks(level, m, dir, TOX, 5);
   }

   private void witchfire(ServerLevel level, @Nullable LivingEntity target) {
      int count = 3 + Math.min(2, this.phase());
      Vec3 from = this.side(-0.65, 2.5, 4.9);
      List<ServerPlayer> players = this.engaged();

      for (int k = 0; k < count; k++) {
         LivingEntity to = target;
         if (players.size() > 1) {
            to = (LivingEntity)players.get(Math.floorMod(this.wispRobin++, players.size()));
         }

         if (to != null) {
            Vec3 spread = dirOf(this.getYRot() + k * 360.0F / count).scale(0.3);
            BogrotBoss.Glob g = new BogrotBoss.Glob(1, from, spread.add(0.0, 0.3, 0.0), 80);
            g.target = to;
            g.homing = 0.12;
            g.speed = 0.55;
            g.radius = 1.0;
            this.addGlob(g);
         }
      }

      Vfx.flare(level, from, WISP.glow(), 2.2, 8);
      Vfx.sparks(level, from, WISP.core(), 10, 0.3, 0.2);
   }

   private void areaMoves(List<BossAction> out) {
      out.add(
         BossAction.of("mires_hunger", 2.0, 0.6, 1.5)
            .range(6.0, 16.0)
            .cooldown(260)
            .weight(7)
            .tags("aoe gap_closer")
            .combo("rootbound_stomp", 0.55F)
            .combo("twin_rake", 0.2F)
            .start((b, level, target, i) -> Vfx.runeCircle(level, onGround(level, this.position()), 14.0, P, 40, -0.05F, Vfx.Rune.GLYPH))
            .tick((b, level, target, t) -> {
               if (t >= 12 && t <= 29) {
                  List<LivingEntity> pulled = this.pull(level, 14.0, 0.16, t, P);
                  if (t % 5 == 0) {
                     for (LivingEntity e : pulled) {
                        this.afflict(e, MobEffects.SLOWNESS, 10, 0);
                     }
                  }

                  if (t % 4 == 0) {
                     Vfx.ring(level, this.position(), P.glow(), 14.0 - (t - 12) * 0.55, 8);
                     level.sendParticles(MUD, this.getX(), this.getY() + 0.3, this.getZ(), 20, 4.0, 0.1, 4.0, 0.1);
                  }
               }
            })
            .hit((b, level, target, i) -> {
               Vec3 c = onGround(level, this.position());
               if (i == 0) {
                  Vfx.telegraphCircle(level, c, 4.5, 18, P);
                  Vfx.impact(level, this.front(2.5, 0.0), P, 0.8);
               } else {
                  for (LivingEntity e : this.ringHit(level, c, 0.0, 4.5)) {
                     if (this.deal(level, e, 12.0F, Vec3.ZERO, 0.0)) {
                        this.afflict(e, MobEffects.SLOWNESS, 40, 2);
                        this.rot(level, e, 1);
                     }
                  }

                  Vfx.impact(level, c, P, 1.2);
                  Vfx.groundCrack(level, c, 4.5, P);
               }
            })
      );
      out.add(
         BossAction.of("festering_ripples", 2.8, 1.2, 1.8, 2.4)
            .range(0.0, 12.0)
            .cooldown(280)
            .phase(1)
            .weight(7)
            .tags("aoe")
            .anyTarget()
            .superArmor()
            .tick((b, level, target, t) -> {
               if (t == 4) {
                  this.rippleMarks(level);
               }
            })
            .hit((b, level, target, i) -> this.ripple(level, i))
      );
      out.add(
         BossAction.of("marshfire_belch", 2.6, 1.0, 1.9).range(0.0, 11.0).cooldown(220).phase(1).weight(8).tags("ranged aoe").tick((b, level, target, t) -> {
            if (t <= 19 && t % 2 == 1) {
               Vfx.gather(level, this.mouth(), TOX, 3.0, 8);
            }

            this.commitYaw(t, 8, 52);
         }).hit((b, level, target, i) -> {
            if (i == 0) {
               this.belchGas(level);
            } else {
               this.belchFire(level);
            }
         })
      );
   }

   private void rippleMarks(ServerLevel level) {
      Vec3 c = onGround(level, this.position());
      this.rippleCenter = c;
      this.rippleMid.clear();
      this.rippleOuter.clear();
      Vfx.telegraphCircle(level, c, 4.0, 20, TOX);

      for (int k = 0; k < 8; k++) {
         double a = k * Math.PI / 4.0;
         Vec3 s = onGround(level, c.add(Math.cos(a) * 6.0, 0.0, Math.sin(a) * 6.0));
         this.rippleMid.add(s);
         Vfx.telegraphCircle(level, s, 2.0, 32, TOX);
      }

      Vfx.ring(level, c, TOX.glow(), 8.0, 32);

      for (int k = 0; k < 12; k++) {
         double a = k * Math.PI / 6.0;
         Vec3 s = onGround(level, c.add(Math.cos(a) * 10.0, 0.0, Math.sin(a) * 10.0));
         this.rippleOuter.add(s);
         Vfx.telegraphCircle(level, s, 2.0, 44, TOX);
      }

      Vfx.ring(level, c, TOX.glow(), 12.0, 44);
   }

   private void ripple(ServerLevel level, int i) {
      double r0 = i * 4;
      double r1 = r0 + 4.0;
      Vec3 c = this.rippleCenter;

      for (LivingEntity e : this.ringHit(level, c, r0, r1)) {
         if (this.deal(level, e, 24.0F, Vec3.ZERO, 0.6)) {
            this.afflict(e, MobEffects.POISON, 60, 0);
            this.rot(level, e, 1);
         }
      }

      if (i == 0) {
         Vfx.heavyImpact(level, c, TOX, 1.0);
         Vfx.groundCrack(level, c, 4.0, TOX);
      } else {
         for (Vec3 s : i == 1 ? this.rippleMid : this.rippleOuter) {
            Vfx.pillar(level, s, TOX, 3.0, 0.8);
         }

         Vfx.shockwave(level, c, TOX, r1);
      }
   }

   private void belchGas(ServerLevel level) {
      float yaw = this.getYRot();

      for (LivingEntity e : this.arc(level, yaw, 11.0, 40.0F)) {
         this.afflict(e, MobEffects.NAUSEA, 60, 0);
         this.afflict(e, MobEffects.POISON, 40, 0);
      }

      Vec3 m = this.mouth();
      Vec3 f = this.facing();

      for (int k = 0; k < 6; k++) {
         Vfx.smoke(level, m.add(f.scale(1.5 + k * 1.6)), TOX.smoke(), 4, 0.8 + k * 0.25, 2.0);
      }

      Vfx.telegraphCone(level, this.position(), yaw, 40.0F, 11.0, 18, WISP);
   }

   private void belchFire(ServerLevel level) {
      float yaw = this.getYRot();

      for (LivingEntity e : this.arc(level, yaw, 11.0, 40.0F)) {
         if (this.deal(level, e, 34.0F, this.away(this.position(), e).scale(0.8), 0.4)) {
            e.igniteForSeconds(4.0F);
            this.rot(level, e, 1);
         }
      }

      for (int k = 0; k < 5; k++) {
         Vec3 at = onGround(level, this.front(2 + k * 2, 0.0)).add(0.0, 0.6, 0.0);
         if (k == 0) {
            Vfx.explosion(level, at, WISP, 2.0);
         } else {
            this.schedule(k, () -> Vfx.explosion(level, at, WISP, 2.0));
         }
      }

      Vfx.shake(level, this.position(), 20.0, 0.8, 10);
   }

   private void utilityMoves(List<BossAction> out) {
      out.add(
         BossAction.of("willow_shroud", 1.8, 0.9)
            .range(0.0, 40.0)
            .cooldown(700)
            .weight(5)
            .tags("defensive")
            .anyTarget()
            .utility((b, t, c) -> this.warded() ? 0.0F : (c.surroundCount >= 2 ? 1.5F : 1.0F))
            .tick((b, level, target, t) -> {
               if (t >= 2 && t <= 17 && t % 3 == 2) {
                  Vfx.debris(level, this.position().add(0.0, 3.0, 0.0), Vfx.Debris.LEAF, P.glow(), 6, 0.25);
               }
            })
            .hit((b, level, target, i) -> {
               this.shield(this.getMaxHealth() * 0.1F, 200);
               Vfx.shieldBubble(level, this.position().add(0.0, 2.2, 0.0), 3.2, P, 200);
               this.wasWarded = true;
            })
      );
      out.add(
         BossAction.of("call_of_the_drowned", 2.6, 1.5)
            .range(0.0, 40.0)
            .cooldown(600)
            .phase(1)
            .weight(6)
            .tags("summon")
            .anyTarget()
            .utility((b, t, c) -> this.thrallCount() >= 4 ? 0.0F : 1.0F)
            .start((b, level, target, i) -> {
               Vfx.runeCircle(level, onGround(level, this.position()), 6.0, P, 30, 0.08F, Vfx.Rune.GLYPH);
               Vfx.vortex(level, this.position(), 4.0, 5.0, P, 30);
            })
            .hit((b, level, target, i) -> this.callDrowned(level))
      );
   }

   private void callDrowned(ServerLevel level) {
      this.minionCheck = -100;
      this.countMinions(level);
      int want = Math.min(1 + this.phase(), 4 - this.thralls);
      if (want > 0) {
         BossFx.summonMinions(level, this, RealmMobs.type("bog_thrall"), want, 6.0, null, m -> {});
      }

      if (this.leeches < 6) {
         BossFx.summonMinions(level, this, RealmMobs.type("bog_leech"), this.phase() >= 2 ? 3 : 2, 4.0, null, m -> {});
      }

      Vfx.nova(level, this.position().add(0.0, 1.0, 0.0), P, 6.0);
      if (this.phase() >= 2) {
         for (ServerPlayer pl : this.nearest(2)) {
            Vec3 s = onGround(level, pl.position());
            Vfx.telegraphCircle(level, s, 1.8, 20, P);
            this.schedule(20, () -> this.erupt(level, s, pl));
         }
      }
   }

   private void erupt(ServerLevel level, Vec3 s, ServerPlayer pl) {
      if (this.isAlive()) {
         RealmMob m = (RealmMob)RealmMobs.type("bog_thrall").create(level, EntitySpawnReason.MOB_SUMMONED);
         if (m != null) {
            m.snapTo(s.x, s.y, s.z, this.random.nextFloat() * 360.0F, 0.0F);
            if (pl.isAlive()) {
               m.setTarget(pl);
            }

            level.addFreshEntity(m);
         }

         for (LivingEntity e : this.ringHit(level, s, 0.0, 1.8)) {
            if (this.deal(level, e, 16.0F, Vec3.ZERO, 0.7)) {
               this.rot(level, e, 1);
            }
         }

         Vfx.pillar(level, s, P, 2.5, 0.8);
         level.sendParticles(MUD, s.x, s.y + 0.4, s.z, 30, 0.6, 0.5, 0.6, 0.15);
      }
   }

   private void vineMoves(List<BossAction> out) {
      out.add(
         BossAction.of("thornlash_flurry", 2.2, 0.6, 0.9, 1.2, 1.5)
            .range(3.0, 10.0)
            .cooldown(110)
            .phase(2)
            .weight(11)
            .tags("melee")
            .advance(0.5, 1.5, 0.06)
            .combo("hag_grasp", 0.3F)
            .combo("rootbound_stomp", 0.25F)
            .utility((b, t, c) -> this.frenzy(true))
            .tick((b, level, target, t) -> {
               if (t >= 6 && t <= 24 && t % 6 == 0) {
                  int k = t / 6 - 1;
                  float base = target != null ? yawTo(this.position(), this.predict(target, 6.0)) : this.getYRot();
                  Vec3 dir = dirOf(base + LASH_YAW[k]);
                  this.lashDirs[k] = dir;
                  Vfx.telegraphLine(level, onGround(level, this.position()), dir, 10.0, 1.8, 6, P);
               }
            })
            .hit((b, level, target, i) -> this.thornLash(level, i))
      );
      out.add(
         BossAction.of("strangling_vines", 1.8, 1.0).range(0.0, 30.0).cooldown(180).phase(2).weight(5).tags("ranged anti_air").tick((b, level, target, t) -> {
            if (t == 4) {
               Vec3 a = target != null ? this.predict(target, 16.0) : this.front(6.0, 0.0);
               Vec3 g = BossFx.ground(level, a.x, a.y, a.z);
               this.strangleAt = g != null ? g : a.add(0.0, -8.0, 0.0);
               Vfx.telegraphCircle(level, this.strangleAt, 2.2, 16, P);
            }
         }).hit((b, level, target, i) -> this.strangle(level))
      );
   }

   private void thornLash(ServerLevel level, int k) {
      Vec3 dir = k >= 0 && k < 4 && this.lashDirs[k] != null ? this.lashDirs[k] : this.facing();
      Vec3 feet = this.position();

      for (LivingEntity e : this.line(level, feet, dir, 10.0, 0.9)) {
         if (!(e.getY() > feet.y + 3.0)) {
            e.setInvulnerableTime(0);
            if (this.deal(level, e, 16.0F, dir.scale(0.6), 0.2)) {
               this.afflict(e, MobEffects.POISON, 40, 0);
               this.rot(level, e, 1);
            }

            Vfx.impact(level, mid(e), TOX, 0.5);
         }
      }

      Vec3 slam = onGround(level, feet.add(dir.scale(4.5)).add(0.0, 1.0, 0.0));
      Vec3 end = onGround(level, feet.add(dir.scale(10.0)).add(0.0, 1.0, 0.0));
      Vec3 start = onGround(level, feet.add(dir.scale(1.6)).add(0.0, 1.0, 0.0));
      Vfx.beam(level, start.add(0.0, 0.7, 0.0), end.add(0.0, 0.6, 0.0), TOX, 0.35, 4);
      Vfx.impact(level, slam.add(0.0, 0.2, 0.0), P, 0.6);
      Vfx.streaks(level, slam.add(0.0, 0.5, 0.0), dir, P, 10);
      Vfx.debris(level, slam.add(0.0, 0.3, 0.0), Vfx.Debris.LEAF, P.glow(), 8, 0.3);
   }

   private void strangle(ServerLevel level) {
      Vec3 g = this.strangleAt;

      for (LivingEntity e : this.column(level, g, 2.2, 16.0)) {
         e.stopFallFlying();
         e.setDeltaMovement(0.0, -2.2, 0.0);
         e.syncVelocity = true;
         if (this.deal(level, e, 26.0F, Vec3.ZERO, 0.0)) {
            this.afflict(e, MobEffects.SLOWNESS, 60, 2);
            this.rot(level, e, 2);
         }

         Vfx.debris(level, mid(e), Vfx.Debris.LEAF, P.glow(), 10, 0.4);
      }

      Vfx.pillar(level, g, P, 16.0, 1.2);
      Vfx.streaks(level, g, UP, P, 12);
   }

   private void ultimates(List<BossAction> out) {
      out.add(
         BossAction.of("black_tide", 5.0, 1.8, 2.4, 3.0, 3.8)
            .range(0.0, 24.0)
            .cooldown(900)
            .phase(2)
            .weight(6)
            .tags("aoe ultimate")
            .anyTarget()
            .superArmor()
            .telegraph(0.0, 3.8)
            .start((b, level, target, i) -> {
               Vfx.chargeUp(level, this.front(0.5, 3.0), P, 36);
               Vfx.runeCircle(level, onGround(level, this.position()), 16.0, P, 100, 0.03F, Vfx.Rune.GLYPH);
            })
            .tick((b, level, target, t) -> {
               if (t == 56) {
                  this.geyserMarks(level);
               }
            })
            .hit((b, level, target, i) -> {
               if (i < 3) {
                  Vec3 c = onGround(level, this.position());
                  BossFx.wave(level, this, c, 16.0, 20, 26.0F, MUD, 1.2, true);
                  Vfx.groundCrack(level, c, 4.0, P);
                  if (i == 0) {
                     Vfx.radialFlash(level, c, 24.0, P.glow(), 0.35F);
                  }
               } else {
                  this.geyserBurst(level);
               }
            })
      );
      out.add(
         BossAction.of("rot_mothers_embrace", 5.0, 3.2, 3.9)
            .range(0.0, 14.0)
            .cooldown(1000)
            .phase(3)
            .weight(6)
            .tags("aoe ultimate")
            .anyTarget()
            .superArmor()
            .telegraph(0.0, 3.9)
            .start((b, level, target, i) -> {
               Vec3 c = onGround(level, this.position());
               Vfx.telegraphCircle(level, c, 4.5, 64, Vfx.Palette.BLOOD);
               Vfx.runeCircle(level, c, 18.0, HEX, 64, -0.06F, Vfx.Rune.GLYPH);
            })
            .tick((b, level, target, t) -> {
               if (t == 16) {
                  Vfx.vortex(level, this.position(), 6.0, 5.0, TOX, 48);
               }

               if (t >= 16 && t <= 60) {
                  this.pull(level, 18.0, 0.07 + 0.08 * (t - 16) / 44.0, t, TOX);
               }

               if (t < 64 && t % 4 == 0) {
                  Vfx.gather(level, this.maw(), TOX, 4.0, 8);
               }
            })
            .hit((b, level, target, i) -> {
               if (i == 0) {
                  this.chomp(level);
               } else {
                  this.exhale(level);
               }
            })
      );
      out.add(
         BossAction.of("the_last_drowning", 6.5, 1.6, 2.4, 3.2, 4.0, 5.6)
            .range(0.0, 64.0)
            .cooldown(0)
            .phase(3)
            .weight(0)
            .tags("aoe ultimate")
            .anyTarget()
            .superArmor()
            .telegraph(0.0, 5.6)
            .when((b, t) -> false)
            .start((b, level, target, i) -> {
               for (List<Vec3> h : this.hands) {
                  h.clear();
               }

               Vfx.runeCircle(level, onGround(level, this.position()), 20.0, P, 130, 0.02F, Vfx.Rune.GLYPH);
               Vfx.vortex(level, this.position(), 14.0, 3.0, P, 130);
               Vfx.flash(level, this.position(), 48.0, TOX.glow(), 0.35F);
            })
            .tick((b, level, target, t) -> {
               if (t >= 16 && t <= 64 && t % 16 == 0) {
                  this.handMarks(level, t / 16 - 1);
               }

               if (t == 68) {
                  Vfx.telegraphCircle(level, onGround(level, this.position()), 13.0, 44, TOX);
                  Vfx.chargeUp(level, this.maw(), TOX, 44);
               }
            })
            .hit((b, level, target, i) -> {
               if (i < 4) {
                  this.drownedHands(level, i);
               } else {
                  this.lastDrowning(level);
               }
            })
            .end((b, level, target, i) -> this.exhausted(level))
      );
   }

   private void geyserMarks(ServerLevel level) {
      this.geysers.clear();

      for (ServerPlayer pl : this.nearest(3)) {
         this.geysers.add(onGround(level, this.predict(pl, 20.0)));
      }

      while (this.geysers.size() < 6) {
         this.geysers.add(BossFx.around(level, this, this.position(), 4.0, 14.0));
      }

      for (Vec3 s : this.geysers) {
         Vfx.telegraphCircle(level, s, 2.8, 20, TOX);
      }
   }

   private void geyserBurst(ServerLevel level) {
      for (Vec3 s : this.geysers) {
         for (LivingEntity e : this.ringHit(level, s, 0.0, 2.8)) {
            if (this.deal(level, e, 40.0F, Vec3.ZERO, 1.0)) {
               this.afflict(e, MobEffects.POISON, 80, 1);
               this.rot(level, e, 1);
            }
         }

         Vfx.pillar(level, s, TOX, 9.0, 1.6);
         Vfx.heavyImpact(level, s, P, 1.2);
         this.pool(level, s, 2.6, 160, 1);
      }

      this.geysers.clear();
   }

   private void chomp(ServerLevel level) {
      Vec3 c = onGround(level, this.position());
      int fed = 0;

      for (LivingEntity e : this.ringHit(level, c, 0.0, 4.5)) {
         if (this.deal(level, e, 60.0F, Vec3.ZERO, 0.0)) {
            this.afflict(e, MobEffects.WITHER, 80, 1);
            this.rot(level, e, 2);
            fed++;
         }
      }

      if (fed > 0) {
         this.heal(this.getMaxHealth() * 0.015F * Math.min(3, fed));
         Vfx.impactFrame(level, this.position(), 20.0, TOX.glow(), 0.8F);
      }

      Vfx.heavyImpact(level, this.maw(), Vfx.Palette.BLOOD, 2.0);
      Vfx.telegraphCircle(level, c, 10.0, 14, TOX);
   }

   private void exhale(ServerLevel level) {
      Vec3 c = onGround(level, this.position());

      for (LivingEntity e : this.ringHit(level, c, 0.0, 10.0)) {
         double d = flatDist(c, e.position());
         if (this.smite(level, e, this.hit(e, 38.0F) * (float)(0.4 + 0.6 * (1.0 - d / 10.0)), this.away(c, e).scale(1.6), 0.4)) {
            this.afflict(e, MobEffects.POISON, 100, 1);
            this.afflict(e, MobEffects.NAUSEA, 80, 0);
         }
      }

      Vfx.nova(level, this.position().add(0.0, 2.0, 0.0), TOX, 10.0);
      Vfx.radialFlash(level, this.position(), 30.0, TOX.glow(), 0.5F);
   }

   private void handMarks(ServerLevel level, int k) {
      List<Vec3> spots = this.hands.get(k);
      spots.clear();

      for (ServerPlayer pl : this.nearest(4)) {
         Vec3 s = onGround(level, this.predict(pl, 16.0));
         spots.add(s);
         Vfx.telegraphCircle(level, s, 2.6, 16, P);
      }
   }

   private void drownedHands(ServerLevel level, int k) {
      for (Vec3 s : this.hands.get(k)) {
         for (LivingEntity e : this.ringHit(level, s, 0.0, 2.6)) {
            if (this.deal(level, e, 30.0F, flat(e.position(), this.position()).scale(0.4), 0.6)) {
               this.afflict(e, MobEffects.SLOWNESS, 30, 2);
               this.rot(level, e, 1);
            }
         }

         Vfx.pillar(level, s, P, 4.0, 1.0);
         level.sendParticles(MUD, s.x, s.y + 0.3, s.z, 24, 0.8, 0.3, 0.8, 0.15);
         Vfx.claw(level, s.add(0.0, 1.0, 0.0), this.random.nextFloat() * 360.0F, -60.0F, 2.0, P);
      }
   }

   private void lastDrowning(ServerLevel level) {
      Vec3 c = onGround(level, this.position());

      for (LivingEntity e : this.ringHit(level, c, 0.0, 13.0)) {
         double d = flatDist(c, e.position());
         if (this.smite(level, e, this.hit(e, 70.0F) * (float)(0.4 + 0.6 * (1.0 - d / 13.0)), this.away(c, e).scale(2.0), 0.5)) {
            this.afflict(e, MobEffects.WITHER, 100, 1);
         }
      }

      Vfx.impactFrame(level, this.position(), 30.0, TOX.glow(), 1.0F);
      Vfx.heavyImpact(level, c, P, 3.0);
      Vfx.nova(level, this.position().add(0.0, 1.0, 0.0), TOX, 13.0);
      Vfx.groundCrack(level, c, 13.0, P);
   }

   private void exhausted(ServerLevel level) {
      if (this.isAlive()) {
         this.buff(100, 1.35F, 1.0F);
         this.setCooldown("rot_mothers_embrace", Math.max(this.cooldownLeft("rot_mothers_embrace"), 300));
         Vfx.smoke(level, this.position(), P.smoke(), 12, 1.5, 2.0);
         Vfx.glint(level, this.maw(), TOX.core(), 2.0);
      }
   }

   @Override
   protected void customServerAiStep(ServerLevel world) {
      super.customServerAiStep(world);
      if (this.isAlive() && !this.inIntro()) {
         BossFx.current(this);
         this.tickGlobs(world);
         this.tickPools(world);
         this.tickRot(world);
         this.tickGrab(world);
         boolean ward = this.warded();
         if (this.wasWarded && !ward) {
            this.shroudBurst(world);
         }

         this.wasWarded = ward;
         this.mend(world);
         this.hazards(world);
         this.lookFx(world);
         if (this.getTarget() == null) {
            if (++this.lonely == 600) {
               this.clearFight(world);
            }
         } else {
            this.lonely = 0;
         }
      }
   }

   private void shroudBurst(ServerLevel level) {
      Vec3 c = this.position();

      for (LivingEntity e : this.ringHit(level, c, 0.0, 5.0)) {
         this.deal(level, e, 14.0F, this.away(c, e).scale(1.6), 0.4);
      }

      Vfx.nova(level, c.add(0.0, 2.0, 0.0), P, 5.0);
      Vfx.debris(level, c.add(0.0, 2.5, 0.0), Vfx.Debris.LEAF, P.glow(), 24, 0.5);
   }

   private void hazards(ServerLevel level) {
      if (this.getTarget() != null) {
         BossAction cur = this.currentMove();
         if (cur == null || !cur.anim().startsWith("phase_")) {
            float step = this.enraged() ? 1.67F : 1.0F;
            int p = this.phase();
            if (p >= 1 && (this.bloomTimer -= step) <= 0.0F) {
               this.bloomTimer = p == 1 ? 300.0F : (p == 2 ? 240.0F : 180.0F);
               this.rotBlooms(level);
            }

            if (p >= 2 && (this.tideTimer -= step) <= 0.0F) {
               this.tideTimer = 400.0F;
               this.blackWater(level);
            }

            if (p >= 3 && (this.wispTimer -= step) <= 0.0F) {
               this.wispTimer = 160.0F;
               this.marshWisps(level);
            }
         }
      }
   }

   private void rotBlooms(ServerLevel level) {
      for (ServerPlayer pl : this.nearest(3)) {
         Vec3 s = onGround(level, this.predict(pl, 20.0));
         Vfx.telegraphCircle(level, s, 2.5, 30, TOX);
         this.schedule(30, () -> {
            if (this.isAlive()) {
               for (LivingEntity e : this.ringHit(level, s, 0.0, 2.5)) {
                  if (this.deal(level, e, 18.0F, Vec3.ZERO, 0.4)) {
                     this.afflict(e, MobEffects.POISON, 60, 1);
                     this.rot(level, e, 1);
                  }
               }

               Vfx.explosion(level, s, TOX, 2.5);
               Vfx.debris(level, s.add(0.0, 1.0, 0.0), Vfx.Debris.LEAF, TOX.glow(), 14, 0.4);
               this.pool(level, s, 2.0, 100, 0);
            }
         });
      }
   }

   private void blackWater(ServerLevel level) {
      for (int k = 0; k < 3; k++) {
         Vec3 s = BossFx.around(level, this, this.position(), 5.0, 12.0);
         Vfx.telegraphCircle(level, s, 3.5, 30, P);
         this.schedule(30, () -> {
            if (this.isAlive()) {
               this.pool(level, s, 3.5, 240, 1);
            }
         });
      }
   }

   private void marshWisps(ServerLevel level) {
      List<ServerPlayer> near = this.nearest(2);
      if (!near.isEmpty()) {
         for (int k = 0; k < 2; k++) {
            ServerPlayer pl = near.get(Math.min(k, near.size() - 1));
            Vec3 from = this.lantern(k == 0);
            BogrotBoss.Glob g = new BogrotBoss.Glob(3, from, pl.getEyePosition().subtract(from).normalize().scale(0.16), 200);
            g.target = pl;
            g.homing = 0.06;
            g.speed = 0.16;
            g.radius = 1.2;
            this.addGlob(g);
            Vfx.flare(level, from, WISP.glow(), 1.4, 8);
         }
      }
   }

   private void lookFx(ServerLevel level) {
      int f = (Integer)this.entityData.get(FORM);
      if (f >= 1 && this.tickCount % 10 == 0) {
         Vfx.embers(level, this.side(0.0, -0.4, 3.4), TOX.glow(), 2, 0.8);
      }

      if (f >= 2 && this.tickCount % 14 == 0) {
         Vfx.smoke(level, this.maw(), TOX.smoke(), 1, 0.3, 0.8);
      }

      if (f >= 3) {
         if (this.tickCount % 6 == 0) {
            Vfx.flare(level, this.lantern(this.tickCount % 12 < 6), WISP.glow(), 0.9, 6);
         }

         if (this.tickCount % 40 == 0) {
            Vfx.auraRing(level, this.position(), WISP);
         }
      }
   }

   private void clearFight(ServerLevel level) {
      this.releaseGrab(level, false);
      this.globs.clear();
      this.pools.clear();
      this.rotStacks.clear();
      this.hexedUntil.clear();
      this.submerged = false;
      this.pouncing = false;
   }

   @Override
   public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
      if (this.submerged && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }

      float before = this.getHealth() + this.shield();
      boolean wardUp = this.warded();
      if (!super.hurtServer(world, source, amount)) {
         return false;
      }

      if (this.grabbed != null) {
         this.grabDamage = this.grabDamage + Math.max(0.0F, before - this.getHealth() - this.shield());
         if (this.grabDamage >= this.getMaxHealth() * 0.05F) {
            this.releaseGrab(world, true);
         }
      }

      if (wardUp
         && !source.is(DamageTypes.THORNS)
         && source.getEntity() instanceof LivingEntity att
         && source.getDirectEntity() == att
         && att != this
         && att.isAlive()
         && att.distanceToSqr(this) < 36.0) {
         att.hurtServer(world, this.damageSources().thorns(this), this.dmg(4.0F));
         this.afflict(att, MobEffects.POISON, 40, 0);
         Vfx.sparks(world, att.getEyePosition(), P.glow(), 6, 0.3, 0.18);
      }

      return true;
   }

   @Override
   public void die(DamageSource damageSource) {
      super.die(damageSource);
      if (this.level() instanceof ServerLevel level) {
         this.clearFight(level);
      }
   }

   @Override
   protected void onPhaseChange(ServerLevel level, int phase) {
      this.releaseGrab(level, true);
      this.submerged = false;
      this.pouncing = false;
   }

   @Override
   protected void phaseBurst(ServerLevel level, int phase) {
      this.entityData.set(FORM, Math.min(3, phase));
      Vec3 c = onGround(level, this.position());

      for (LivingEntity e : this.ringHit(level, c, 0.0, 7.0)) {
         this.deal(level, e, 12.0F, this.away(c, e).scale(1.6), 0.5);
      }

      BossFx.knockback(level, this, c, 9.0, 1.2);
      Vfx.nova(level, this.position().add(0.0, 2.0, 0.0), TOX, 9.0);
      Vfx.heavyImpact(level, c, P, 1.5);
      Vfx.shake(level, c, 40.0, 1.0, 20);
      if (phase == 1) {
         Vec3 hump = this.position().add(0.0, 3.4, 0.0);
         Vfx.shards(level, hump, 5127726, 24, 0.6);
         Vfx.debris(level, hump, Vfx.Debris.LEAF, P.glow(), 24, 0.5);
         BossFx.burst(level, hump, ROOTS, 60, 1.2, 0.3);

         for (int k = 0; k < 4; k++) {
            Vec3 s = BossFx.around(level, this, this.position(), 4.0, 9.0);
            Vfx.telegraphCircle(level, s, 2.2, 20, TOX);
            this.schedule(20, () -> {
               if (this.isAlive()) {
                  this.pool(level, s, 2.2, 160, 0);
               }
            });
         }
      } else if (phase == 2) {
         Vfx.explosion(level, this.maw(), TOX, 3.0);
         Vfx.streaks(level, this.vineRoot(), UP, P, 16);
         Vfx.groundCrack(level, c, 10.0, P);
         this.blackWater(level);
         BossFx.summonMinions(level, this, RealmMobs.type("bog_thrall"), 2, 6.0, null, m -> {});
         this.tideTimer = 100.0F;
      } else if (phase >= 3) {
         Vfx.pillar(level, this.position(), WISP, 10.0, 1.4);
         Vfx.runeCircle(level, c, 12.0, HEX, 60, 0.05F, Vfx.Rune.GLYPH);
         Vfx.radialFlash(level, this.position(), 40.0, WISP.glow(), 0.6F);
         Vfx.glint(level, this.maw(), TOX.core(), 2.5);
         this.wispTimer = 60.0F;
      }
   }

   @Override
   protected void onStagger(ServerLevel level) {
      this.releaseGrab(level, true);
      int f = (Integer)this.entityData.get(FORM);
      if (f >= 1) {
         Vec3 sacs = this.side(0.0, -0.4, 3.4);
         Vfx.debris(level, sacs, Vfx.Debris.LEAF, TOX.glow(), 16, 0.4);
         Vfx.smoke(level, sacs, TOX.smoke(), 6, 1.0, 1.4);
      }

      if (f >= 2) {
         Vfx.glint(level, this.maw(), TOX.core(), 2.0);
      }
   }

   @Override
   protected void onEnrage(ServerLevel level) {
      Vfx.auraRing(level, this.position(), WISP, 3.5);
      Vfx.nova(level, this.position().add(0.0, 2.0, 0.0), WISP, 6.0);
      Vfx.flare(level, this.eyes(), WISP.glow(), 3.0, 12);
   }

   @Override
   protected void desperation(ServerLevel level) {
      BossAction cur = this.currentMove();
      if (cur != null && cur.anim().startsWith("phase_")) {
         this.schedule(Math.max(1, cur.length() - this.actionTick() + 1), () -> this.drown(level));
      } else {
         this.drown(level);
      }
   }

   private void drown(ServerLevel level) {
      if (this.isAlive()) {
         this.forceAction(level, "the_last_drowning");
         Vfx.impactFrame(level, this.position(), 40.0, TOX.glow(), 0.7F);
         this.voice(level, "enrage", 4.0F);
      }
   }

   @Override
   protected void onIntroTick(ServerLevel level, int ticksLeft) {
      int t = this.introTicks() - ticksLeft;
      if (t == 1) {
         Vec3 c = onGround(level, this.position());
         Vfx.runeCircle(level, c, 8.0, HEX, 140, 0.03F, Vfx.Rune.GLYPH);
         Vfx.groundCrack(level, c, 5.0, P);
      }

      if (t % 3 == 0) {
         level.sendParticles(MUD, this.getX(), this.getY() + 0.3, this.getZ(), 10, 1.25, 0.2, 1.25, 0.08);
         level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.3, this.getZ(), 20, 1.25, 0.2, 1.25, 0.2);
         level.sendParticles(ParticleTypes.BUBBLE_POP, this.getX(), this.getY() + 0.2, this.getZ(), 8, 1.25, 0.1, 1.25, 0.02);
      }

      if (t == 16) {
         Vfx.debris(level, this.position().add(0.0, 0.6, 0.0), Vfx.Debris.LEAF, P.glow(), 16, 0.4);
         Vfx.smoke(level, this.position(), P.smoke(), 6, 1.6, 1.6);
      }

      if (t == 24 || t == 44) {
         Vec3 hand = this.side(t == 24 ? 1.4 : -1.4, 1.2, 0.3);
         Vfx.impact(level, hand, P, 0.8);
         level.sendParticles(MUD, hand.x, hand.y, hand.z, 20, 0.4, 0.2, 0.4, 0.15);
      }

      if (t >= 44 && t <= 80 && t % 6 == 2) {
         Vfx.smoke(level, this.position(), P.smoke(), 6, 2.5, 2.0);
      }

      if (t == 92) {
         Vfx.flare(level, this.eyes(), P.glow(), 2.4, 10);
         Vfx.glint(level, this.eyes(), P.core(), 1.5);
      }

      if (t == 104) {
         Vfx.nova(level, this.position().add(0.0, 2.0, 0.0), P, 10.0);
         BossFx.knockback(level, this, this.position(), 8.0, 1.2);
         Vfx.radialFlash(level, this.position(), 30.0, P.glow(), 0.5F);
         Vfx.shake(level, this.position(), 30.0, 0.9, 20);
         Vfx.wisps(level, this.position().add(0.0, 3.0, 0.0), TOX, 8, 3.0, 40);
      }
   }

   @Override
   protected void onDeathTick(ServerLevel level, int tick) {
      if (tick <= 24) {
         if (tick % 3 == 0) {
            Vfx.sparks(level, this.maw(), TOX.core(), 6, 0.3, 0.2);
         }

         if (tick % 6 == 0) {
            Vfx.glint(level, this.maw(), TOX.core(), 1.2);
         }
      }

      if (tick >= 24 && tick <= 48 && tick % 4 == 0) {
         Vfx.smoke(level, this.vineRoot(), 2763290, 3, 1.0, 1.2);
      }

      if (tick == 48) {
         for (boolean left : new boolean[]{true, false}) {
            Vfx.flare(level, this.lantern(left), WISP.glow(), 1.6, 8);
            Vfx.embers(level, this.lantern(left), WISP.core(), 6, 0.5);
         }
      }

      if (tick == 66) {
         Vfx.flare(level, this.eyes(), P.glow(), 1.2, 6);
      }

      if (tick >= 84 && tick <= 104 && tick % 5 == 4) {
         Vfx.debris(level, this.position().add(0.0, 3.0, 0.0), Vfx.Debris.LEAF, P.glow(), 10, 0.3);
         Vfx.smoke(level, this.position().add(0.0, 2.5, 0.0), P.smoke(), 3, 1.2, 1.4);
      }

      if (tick >= 104 && tick <= 146 && tick % 3 == 2) {
         level.sendParticles(MUD, this.getX(), this.getY() + 0.3, this.getZ(), 10, 1.2, 0.2, 1.2, 0.08);
         level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.3, this.getZ(), 16, 1.2, 0.2, 1.2, 0.2);
      }

      if (tick == 120) {
         Vfx.groundCrack(level, onGround(level, this.position()), 4.0, P);
      }

      if (tick == 144) {
         Vfx.pillar(level, this.position(), TOX, 8.0, 1.4);
         Vfx.nova(level, this.position(), TOX, 6.0);
      }
   }

   @Override
   protected SoundEvent roarSound() {
      return SOUNDS.roar();
   }

   protected SoundEvent getAmbientSound() {
      return SOUNDS.ambient();
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return SOUNDS.hurt();
   }

   protected SoundEvent getDeathSound() {
      return SOUNDS.death();
   }

   private static final class Glob {
      final int kind;
      Vec3 pos;
      Vec3 vel;
      final int life;
      double gravity;
      double homing;
      double speed;
      double radius = 1.0;
      int age;
      int born;
      @Nullable LivingEntity target;
      @Nullable Vec3 landing;

      Glob(int kind, Vec3 pos, Vec3 vel, int life) {
         this.kind = kind;
         this.pos = pos;
         this.vel = vel;
         this.life = life;
      }
   }

   private record Pool(Vec3 at, double r, int kind, int end) {
   }
}

package com.vexira.mysticalterrain.boss;

import com.vexira.mysticalterrain.boss.entity.WardenShade;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.boss.vfx.VfxOptions;
import com.vexira.mysticalterrain.boss.vfx.VfxParticles;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BossBolt extends Projectile {
   private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(BossBolt.class, EntityDataSerializers.INT);
   private float damage = 6.0F;
   private double aoe;
   private @Nullable MobEffectInstance effect;
   private double gravity;
   private double homing;
   private @Nullable LivingEntity homingTarget;
   private int life = 120;
   private boolean setsFire;
   private boolean rehit;

   public BossBolt(EntityType<? extends BossBolt> type, Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   public static BossBolt shoot(ServerLevel level, LivingEntity owner, BossBolt.Style style, Vec3 from, Vec3 velocity, float damage) {
      BossBolt bolt = new BossBolt(BossEntities.BOSS_BOLT, level);
      bolt.setOwner(owner);
      bolt.entityData.set(STYLE, style.ordinal());
      bolt.damage = damage;
      bolt.setPos(from.x, from.y, from.z);
      bolt.setDeltaMovement(velocity);
      level.addFreshEntity(bolt);
      BossSounds.play(level, from, "bolt." + style.name().toLowerCase() + ".launch", 1.4F, 0.9F + level.getRandom().nextFloat() * 0.2F);
      return bolt;
   }

   public BossBolt aoe(double radius) {
      this.aoe = radius;
      return this;
   }

   public BossBolt effect(MobEffectInstance effect) {
      this.effect = effect;
      return this;
   }

   public BossBolt gravity(double gravity) {
      this.gravity = gravity;
      return this;
   }

   public BossBolt homing(@Nullable LivingEntity target, double strength) {
      this.homingTarget = target;
      this.homing = strength;
      return this;
   }

   public BossBolt life(int ticks) {
      this.life = ticks;
      return this;
   }

   public BossBolt fire() {
      this.setsFire = true;
      return this;
   }

   public BossBolt rehit() {
      this.rehit = true;
      return this;
   }

   public BossBolt.Style style() {
      return BossBolt.Style.values()[this.entityData.get(STYLE)];
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(STYLE, 0);
   }

   public void tick() {
      super.tick();
      Vec3 v = this.getDeltaMovement();
      if (this.level().isClientSide()) {
         this.trail(v);
      } else {
         if (--this.life <= 0) {
            this.impact((ServerLevel)this.level(), this.position());
            return;
         }

         if (this.homingTarget != null && this.homingTarget.isAlive()) {
            Vec3 want = this.homingTarget.getEyePosition().subtract(this.position()).normalize().scale(v.length());
            v = v.lerp(want, this.homing);
         }

         v = v.add(0.0, -this.gravity, 0.0);
         this.setDeltaMovement(v);
         HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
         if (hit.getType() != Type.MISS) {
            this.hitTargetOrDeflectSelf(hit);
            return;
         }
      }

      this.setPos(this.getX() + v.x, this.getY() + v.y, this.getZ() + v.z);
   }

   protected boolean canHitEntity(Entity entity) {
      return this.getOwner() instanceof Player
         ? super.canHitEntity(entity) && !(entity instanceof Player) && !(entity instanceof BossBolt)
         : super.canHitEntity(entity) && !(entity instanceof MysticalBoss) && !(entity instanceof BossBolt) && !(entity instanceof WardenShade);
   }

   protected void onHitEntity(EntityHitResult entityHitResult) {
      if (this.level() instanceof ServerLevel level && this.aoe <= 0.0 && entityHitResult.getEntity() instanceof LivingEntity victim) {
         Entity owner = this.getOwner();
         if (this.rehit) {
            victim.setInvulnerableTime(0);
         }

         victim.hurtServer(level, this.damageSources().indirectMagic(this, owner), this.damage);
         if (this.effect != null) {
            victim.addEffect(new MobEffectInstance(this.effect), owner);
         }

         if (this.setsFire) {
            victim.igniteForSeconds(4.0F);
         }
      }

      if (this.level() instanceof ServerLevel level) {
         this.impact(level, entityHitResult.getLocation());
      }
   }

   protected void onHitBlock(BlockHitResult blockHitResult) {
      super.onHitBlock(blockHitResult);
      if (this.level() instanceof ServerLevel level) {
         this.impact(level, blockHitResult.getLocation());
         if (this.setsFire && level.getBlockState(blockHitResult.getBlockPos().above()).isAir()) {
            level.setBlockAndUpdate(blockHitResult.getBlockPos().above(), Blocks.FIRE.defaultBlockState());
         }
      }
   }

   private void impact(ServerLevel level, Vec3 at) {
      if (this.aoe > 0.0 && this.getOwner() instanceof LivingEntity owner) {
         for (LivingEntity e : BossFx.enemies(level, owner, at, this.aoe)) {
            if (this.rehit) {
               e.setInvulnerableTime(0);
            }

            e.hurtServer(level, this.damageSources().indirectMagic(this, owner), this.damage);
            if (this.effect != null) {
               e.addEffect(new MobEffectInstance(this.effect), owner);
            }

            if (this.setsFire) {
               e.igniteForSeconds(4.0F);
            }
         }
      }

      BossBolt.Style s = this.style();
      Vfx.Palette pal = palette(s);
      if (this.aoe > 0.0) {
         Vfx.explosion(level, at, pal, Math.max(1.2, this.aoe));
      } else {
         Vfx.impact(level, at, pal, 0.6);
      }

      level.sendParticles(burstParticle(s), at.x, at.y, at.z, 24 + (int)(this.aoe * 10.0), 0.3 + this.aoe * 0.3, 0.3, 0.3 + this.aoe * 0.3, 0.08);
      if (s == BossBolt.Style.METEOR || s == BossBolt.Style.STAR) {
         level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
      }

      SoundEvent custom = BossSounds.event("bolt." + s.name().toLowerCase() + ".impact");
      level.playSound(null, at.x, at.y, at.z, custom != null ? custom : impactSound(s), SoundSource.HOSTILE, 1.6F, 0.9F + this.random.nextFloat() * 0.2F);
      this.discard();
   }

   public static Vfx.Palette palette(BossBolt.Style s) {
      return switch (s) {
         case THORN -> Vfx.Palette.FAE;
         case SHADOW -> Vfx.Palette.SHADOW;
         case RUNE -> Vfx.Palette.ARCANE;
         case STAR -> Vfx.Palette.STAR;
         case ICE -> Vfx.Palette.FROST;
         case METEOR -> Vfx.Palette.FIRE;
         case SPORE -> Vfx.Palette.SPORE;
         case SAND -> Vfx.Palette.SAND;
         default -> Vfx.Palette.VOID;
      };
   }

   private void glowTrail(BossBolt.Style s, Vec3 v) {
      Vfx.Palette pal = palette(s);
      ParticleType<VfxOptions> flare = VfxParticles.type(VfxParticles.Kind.FLARE);
      ParticleType<VfxOptions> spark = VfxParticles.type(VfxParticles.Kind.SPARK);
      float size = 0.9F + (float)this.aoe * 0.25F;
      this.level().addParticle(new VfxOptions(flare, pal.glow(), size * 1.8F, 5, 0.0F), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      this.level().addParticle(new VfxOptions(flare, pal.core(), size, 3, 0.0F), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);

      for (int i = 1; i <= 3; i++) {
         double k = i / 4.0;
         this.level()
            .addParticle(
               new VfxOptions(flare, pal.glow(), size * (1.4F - i * 0.3F), 6 + i * 2, 0.0F),
               this.getX() - v.x * k,
               this.getY() - v.y * k,
               this.getZ() - v.z * k,
               0.0,
               0.0,
               0.0
            );
      }

      if (this.random.nextInt(2) == 0) {
         this.level()
            .addParticle(
               new VfxOptions(spark, pal.core(), 0.25F, 10, 0.0F),
               this.getX(),
               this.getY(),
               this.getZ(),
               -v.x * 0.3 + this.random.nextGaussian() * 0.04,
               -v.y * 0.3 + this.random.nextGaussian() * 0.04,
               -v.z * 0.3 + this.random.nextGaussian() * 0.04
            );
      }
   }

   private void trail(Vec3 v) {
      BossBolt.Style s = this.style();
      this.glowTrail(s, v);

      for (int i = 0; i < 3; i++) {
         double f = i / 3.0;
         this.level()
            .addParticle(
               trailParticle(s),
               this.getX() - v.x * f,
               this.getY() - v.y * f,
               this.getZ() - v.z * f,
               (this.random.nextDouble() - 0.5) * 0.02,
               (this.random.nextDouble() - 0.5) * 0.02,
               (this.random.nextDouble() - 0.5) * 0.02
            );
      }

      if (this.random.nextInt(3) == 0) {
         this.level().addParticle(accentParticle(s), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      }
   }

   private static ParticleOptions trailParticle(BossBolt.Style s) {
      return (ParticleOptions)(switch (s) {
         case THORN -> new DustParticleOptions(7327850, 1.2F);
         case SHADOW -> ParticleTypes.SQUID_INK;
         case RUNE -> ParticleTypes.ENCHANT;
         case STAR -> ParticleTypes.END_ROD;
         case ICE -> ParticleTypes.SNOWFLAKE;
         case METEOR -> ParticleTypes.FLAME;
         case SPORE -> ParticleTypes.SPORE_BLOSSOM_AIR;
         case SAND -> new DustParticleOptions(14729338, 1.4F);
         case VOID -> ParticleTypes.PORTAL;
      });
   }

   private static ParticleOptions accentParticle(BossBolt.Style s) {
      return (ParticleOptions)(switch (s) {
         case THORN -> ParticleTypes.CHERRY_LEAVES;
         case SHADOW -> ParticleTypes.SOUL;
         case RUNE -> ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, -10428161);
         case STAR -> ParticleTypes.FIREWORK;
         case ICE -> ParticleTypes.ITEM_SNOWBALL;
         case METEOR -> ParticleTypes.LARGE_SMOKE;
         case SPORE -> ParticleTypes.WARPED_SPORE;
         case SAND -> ParticleTypes.WHITE_ASH;
         case VOID -> ParticleTypes.REVERSE_PORTAL;
      });
   }

   private static ParticleOptions burstParticle(BossBolt.Style s) {
      return (ParticleOptions)(switch (s) {
         case THORN -> ParticleTypes.CHERRY_LEAVES;
         case SHADOW -> ParticleTypes.SCULK_SOUL;
         case RUNE -> ParticleTypes.ENCHANTED_HIT;
         case STAR -> ParticleTypes.FIREWORK;
         case ICE -> ParticleTypes.SNOWFLAKE;
         case METEOR -> ParticleTypes.LAVA;
         case SPORE -> ParticleTypes.SPORE_BLOSSOM_AIR;
         case SAND -> ParticleTypes.WHITE_ASH;
         case VOID -> PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F);
      });
   }

   private static SoundEvent impactSound(BossBolt.Style s) {
      return switch (s) {
         case SHADOW -> (SoundEvent)SoundEvents.SOUL_ESCAPE.value();
         case RUNE, STAR -> SoundEvents.AMETHYST_BLOCK_BREAK;
         case ICE -> SoundEvents.GLASS_BREAK;
         case METEOR -> (SoundEvent)SoundEvents.GENERIC_EXPLODE.value();
         default -> SoundEvents.GRASS_BREAK;
      };
   }

   public boolean shouldBeSaved() {
      return false;
   }

   public boolean isPickable() {
      return false;
   }

   public enum Style {
      THORN,
      SHADOW,
      RUNE,
      STAR,
      ICE,
      METEOR,
      SPORE,
      SAND,
      VOID;
   }
}

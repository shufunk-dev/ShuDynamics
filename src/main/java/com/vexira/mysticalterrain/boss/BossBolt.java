package com.vexira.mysticalterrain.boss;

import com.vexira.mysticalterrain.boss.entity.WardenShade;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.boss.vfx.VfxOptions;
import com.vexira.mysticalterrain.boss.vfx.VfxParticles;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.data.DataTracker.Builder;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.DragonBreathParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public class BossBolt extends ProjectileEntity {
   private static final TrackedData<Integer> STYLE = DataTracker.registerData(BossBolt.class, TrackedDataHandlerRegistry.INTEGER);
   private float damage = 6.0F;
   private double aoe;
   private @Nullable StatusEffectInstance effect;
   private double gravity;
   private double homing;
   private @Nullable LivingEntity homingTarget;
   private int life = 120;
   private boolean setsFire;
   private boolean rehit;

   public BossBolt(EntityType<? extends BossBolt> type, World level) {
      super(type, level);
      this.noClip = true;
   }

   public static BossBolt shoot(ServerWorld level, LivingEntity owner, BossBolt.Style style, Vec3d from, Vec3d velocity, float damage) {
      BossBolt bolt = new BossBolt(BossEntities.BOSS_BOLT, level);
      bolt.setOwner(owner);
      bolt.dataTracker.set(STYLE, style.ordinal());
      bolt.damage = damage;
      bolt.setPosition(from.x, from.y, from.z);
      bolt.setVelocity(velocity);
      level.spawnEntity(bolt);
      BossSounds.play(level, from, "bolt." + style.name().toLowerCase() + ".launch", 1.4F, 0.9F + level.getRandom().nextFloat() * 0.2F);
      return bolt;
   }

   public BossBolt aoe(double radius) {
      this.aoe = radius;
      return this;
   }

   public BossBolt effect(StatusEffectInstance effect) {
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
      return BossBolt.Style.values()[this.dataTracker.get(STYLE)];
   }

   protected void initDataTracker(Builder builder) {
      builder.add(STYLE, 0);
   }

   public void tick() {
      super.tick();
      Vec3d v = this.getVelocity();
      if (this.getEntityWorld().isClient()) {
         this.trail(v);
      } else {
         if (--this.life <= 0) {
            this.impact((ServerWorld)this.getEntityWorld(), this.getEntityPos());
            return;
         }

         if (this.homingTarget != null && this.homingTarget.isAlive()) {
            Vec3d want = this.homingTarget.getEyePos().subtract(this.getEntityPos()).normalize().multiply(v.length());
            v = v.lerp(want, this.homing);
         }

         v = v.add(0.0, -this.gravity, 0.0);
         this.setVelocity(v);
         HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
         if (hit.getType() != Type.MISS) {
            this.hitOrDeflect(hit);
            return;
         }
      }

      this.setPosition(this.getX() + v.x, this.getY() + v.y, this.getZ() + v.z);
   }

   protected boolean canHit(Entity entity) {
      return this.getOwner() instanceof PlayerEntity
         ? super.canHit(entity) && !(entity instanceof PlayerEntity) && !(entity instanceof BossBolt)
         : super.canHit(entity) && !(entity instanceof MysticalBoss) && !(entity instanceof BossBolt) && !(entity instanceof WardenShade);
   }

   protected void onEntityHit(EntityHitResult entityHitResult) {
      if (this.getEntityWorld() instanceof ServerWorld level && this.aoe <= 0.0 && entityHitResult.getEntity() instanceof LivingEntity victim) {
         Entity owner = this.getOwner();
         if (this.rehit) {
            victim.timeUntilRegen = 0;
         }

         victim.damage(level, this.getDamageSources().indirectMagic(this, owner), this.damage);
         if (this.effect != null) {
            victim.addStatusEffect(new StatusEffectInstance(this.effect), owner);
         }

         if (this.setsFire) {
            victim.setOnFireFor(4.0F);
         }
      }

      if (this.getEntityWorld() instanceof ServerWorld level) {
         this.impact(level, entityHitResult.getPos());
      }
   }

   protected void onBlockHit(BlockHitResult blockHitResult) {
      super.onBlockHit(blockHitResult);
      if (this.getEntityWorld() instanceof ServerWorld level) {
         this.impact(level, blockHitResult.getPos());
         if (this.setsFire && level.getBlockState(blockHitResult.getBlockPos().up()).isAir()) {
            level.setBlockState(blockHitResult.getBlockPos().up(), Blocks.FIRE.getDefaultState());
         }
      }
   }

   private void impact(ServerWorld level, Vec3d at) {
      if (this.aoe > 0.0 && this.getOwner() instanceof LivingEntity owner) {
         for (LivingEntity e : BossFx.enemies(level, owner, at, this.aoe)) {
            if (this.rehit) {
               e.timeUntilRegen = 0;
            }

            e.damage(level, this.getDamageSources().indirectMagic(this, owner), this.damage);
            if (this.effect != null) {
               e.addStatusEffect(new StatusEffectInstance(this.effect), owner);
            }

            if (this.setsFire) {
               e.setOnFireFor(4.0F);
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

      level.spawnParticles(burstParticle(s), at.x, at.y, at.z, 24 + (int)(this.aoe * 10.0), 0.3 + this.aoe * 0.3, 0.3, 0.3 + this.aoe * 0.3, 0.08);
      if (s == BossBolt.Style.METEOR || s == BossBolt.Style.STAR) {
         level.spawnParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
      }

      SoundEvent custom = BossSounds.event("bolt." + s.name().toLowerCase() + ".impact");
      level.playSound(null, at.x, at.y, at.z, custom != null ? custom : impactSound(s), SoundCategory.HOSTILE, 1.6F, 0.9F + this.random.nextFloat() * 0.2F);
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

   private void glowTrail(BossBolt.Style s, Vec3d v) {
      Vfx.Palette pal = palette(s);
      ParticleType<VfxOptions> flare = VfxParticles.type(VfxParticles.Kind.FLARE);
      ParticleType<VfxOptions> spark = VfxParticles.type(VfxParticles.Kind.SPARK);
      float size = 0.9F + (float)this.aoe * 0.25F;
      this.getEntityWorld().addParticleClient(new VfxOptions(flare, pal.glow(), size * 1.8F, 5, 0.0F), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      this.getEntityWorld().addParticleClient(new VfxOptions(flare, pal.core(), size, 3, 0.0F), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);

      for (int i = 1; i <= 3; i++) {
         double k = i / 4.0;
         this.getEntityWorld()
            .addParticleClient(
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
         this.getEntityWorld()
            .addParticleClient(
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

   private void trail(Vec3d v) {
      BossBolt.Style s = this.style();
      this.glowTrail(s, v);

      for (int i = 0; i < 3; i++) {
         double f = i / 3.0;
         this.getEntityWorld()
            .addParticleClient(
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
         this.getEntityWorld().addParticleClient(accentParticle(s), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      }
   }

   private static ParticleEffect trailParticle(BossBolt.Style s) {
      return (ParticleEffect)(switch (s) {
         case THORN -> new DustParticleEffect(7327850, 1.2F);
         case SHADOW -> ParticleTypes.SQUID_INK;
         case RUNE -> ParticleTypes.ENCHANT;
         case STAR -> ParticleTypes.END_ROD;
         case ICE -> ParticleTypes.SNOWFLAKE;
         case METEOR -> ParticleTypes.FLAME;
         case SPORE -> ParticleTypes.SPORE_BLOSSOM_AIR;
         case SAND -> new DustParticleEffect(14729338, 1.4F);
         case VOID -> ParticleTypes.PORTAL;
      });
   }

   private static ParticleEffect accentParticle(BossBolt.Style s) {
      return (ParticleEffect)(switch (s) {
         case THORN -> ParticleTypes.CHERRY_LEAVES;
         case SHADOW -> ParticleTypes.SOUL;
         case RUNE -> TintedParticleEffect.create(ParticleTypes.ENTITY_EFFECT, -10428161);
         case STAR -> ParticleTypes.FIREWORK;
         case ICE -> ParticleTypes.ITEM_SNOWBALL;
         case METEOR -> ParticleTypes.LARGE_SMOKE;
         case SPORE -> ParticleTypes.WARPED_SPORE;
         case SAND -> ParticleTypes.WHITE_ASH;
         case VOID -> ParticleTypes.REVERSE_PORTAL;
      });
   }

   private static ParticleEffect burstParticle(BossBolt.Style s) {
      return (ParticleEffect)(switch (s) {
         case THORN -> ParticleTypes.CHERRY_LEAVES;
         case SHADOW -> ParticleTypes.SCULK_SOUL;
         case RUNE -> ParticleTypes.ENCHANTED_HIT;
         case STAR -> ParticleTypes.FIREWORK;
         case ICE -> ParticleTypes.SNOWFLAKE;
         case METEOR -> ParticleTypes.LAVA;
         case SPORE -> ParticleTypes.SPORE_BLOSSOM_AIR;
         case SAND -> ParticleTypes.WHITE_ASH;
         case VOID -> DragonBreathParticleEffect.of(ParticleTypes.DRAGON_BREATH, 1.0F);
      });
   }

   private static SoundEvent impactSound(BossBolt.Style s) {
      return switch (s) {
         case SHADOW -> (SoundEvent)SoundEvents.PARTICLE_SOUL_ESCAPE.value();
         case RUNE, STAR -> SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK;
         case ICE -> SoundEvents.BLOCK_GLASS_BREAK;
         case METEOR -> (SoundEvent)SoundEvents.ENTITY_GENERIC_EXPLODE.value();
         default -> SoundEvents.BLOCK_GRASS_BREAK;
      };
   }

   public boolean shouldSave() {
      return false;
   }

   public boolean canHit() {
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

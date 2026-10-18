package com.vexira.mysticalterrain.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.vexira.mysticalterrain.boss.dragon.DragonAccess;
import com.vexira.mysticalterrain.boss.dragon.DragonRework;
import com.vexira.mysticalterrain.boss.dragon.DragonReworkHolder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin extends Mob implements DragonReworkHolder, DragonAccess {
   @Unique
   private static final EntityDataAccessor<Integer> MYSTICALTERRAIN_STATE = SynchedEntityData.defineId(EnderDragon.class, EntityDataSerializers.INT);
   @Unique
   private DragonRework mysticalterrain$rework;

   protected EnderDragonMixin(EntityType<? extends Mob> type, Level level) {
      super(type, level);
   }

   @Shadow
   protected abstract void reallyHurt(ServerLevel var1, DamageSource var2, float var3);

   @Inject(method = "defineSynchedData", at = @At("TAIL"))
   private void mysticalterrain$defineState(Builder builder, CallbackInfo ci) {
      builder.define(MYSTICALTERRAIN_STATE, 0);
   }

   @Override
   public DragonRework mysticalterrain$rework() {
      if (this.mysticalterrain$rework == null) {
         this.mysticalterrain$rework = new DragonRework((EnderDragon)(Object)this);
      }

      return this.mysticalterrain$rework;
   }

   @Override
   public int mysticalterrain$state() {
      return (Integer)this.entityData.get(MYSTICALTERRAIN_STATE);
   }

   @Override
   public void mysticalterrain$setState(int state) {
      this.entityData.set(MYSTICALTERRAIN_STATE, state);
   }

   @Override
   public void mysticalterrain$reallyHurt(ServerLevel level, DamageSource source, float amount) {
      this.reallyHurt(level, source, amount);
   }

   @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;getFlyTargetLocation()Lnet/minecraft/world/phys/Vec3;"))
   private Vec3 mysticalterrain$holdCourse(DragonPhaseInstance phase, Operation<Vec3> original) {
      return !this.level().isClientSide() && this.mysticalterrain$rework().steering() ? null : (Vec3)original.call(new Object[]{phase});
   }

   @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;doServerTick(Lnet/minecraft/server/level/ServerLevel;)V"))
   private void mysticalterrain$holdPhase(DragonPhaseInstance phase, ServerLevel level, Operation<Void> original) {
      if (!this.mysticalterrain$rework().holdsPhase()) {
         original.call(new Object[]{phase, level});
      }
   }

   @WrapOperation(
      method = "aiStep",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;knockBack(Lnet/minecraft/server/level/ServerLevel;Ljava/util/List;)V"),
      require = 0
   )
   private void mysticalterrain$spareKnockBack(EnderDragon self, ServerLevel level, List<Entity> hit, Operation<Void> original) {
      original.call(new Object[]{self, level, this.mysticalterrain$rework().spare(hit)});
   }

   @WrapOperation(
      method = "aiStep",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;hurt(Lnet/minecraft/server/level/ServerLevel;Ljava/util/List;)V"),
      require = 0
   )
   private void mysticalterrain$spareBite(EnderDragon self, ServerLevel level, List<Entity> hit, Operation<Void> original) {
      original.call(new Object[]{self, level, this.mysticalterrain$rework().spare(hit)});
   }

   @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
   private void mysticalterrain$customDeath(CallbackInfo ci) {
      ci.cancel();
      if (this.level() instanceof ServerLevel server) {
         this.mysticalterrain$rework().customDeath(server);
      } else {
         ((EnderDragon)(Object)this).dragonDeathTime++;
         InterpolationHandler interpolation = this.getInterpolation();
         if (interpolation != null) {
            interpolation.interpolate();
         }
      }
   }

   @WrapOperation(
      method = "aiStep",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"),
      require = 0
   )
   private void mysticalterrain$noVanillaDeathParticles(
      Level level, ParticleOptions p, double x, double y, double z, double dx, double dy, double dz, Operation<Void> original
   ) {
      if (!((EnderDragon)(Object)this).isDeadOrDying()) {
         original.call(new Object[]{level, p, x, y, z, dx, dy, dz});
      }
   }

   @Inject(method = "aiStep", at = @At("HEAD"))
   private void mysticalterrain$followNoAi(CallbackInfo ci) {
      if (this.level().isClientSide() && this.isNoAi() && !this.isDeadOrDying()) {
         InterpolationHandler interpolation = this.getInterpolation();
         if (interpolation != null) {
            interpolation.interpolate();
         }
      }
   }

   @Inject(method = "aiStep", at = @At("TAIL"))
   private void mysticalterrain$tickRework(CallbackInfo ci) {
      if (this.level() instanceof ServerLevel server && !this.isNoAi()) {
         this.mysticalterrain$rework().tick(server);
      }
   }

   @Inject(method = "hurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/boss/enderdragon/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
   private void mysticalterrain$hurt(ServerLevel level, EnderDragonPart part, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
      cir.setReturnValue(this.mysticalterrain$rework().hurt(level, part, source, amount));
   }

   @Inject(method = "onCrystalDestroyed", at = @At("HEAD"))
   private void mysticalterrain$crystalDestroyed(ServerLevel level, EndCrystal crystal, BlockPos pos, DamageSource source, CallbackInfo ci) {
      this.mysticalterrain$rework().onCrystalDestroyed(level, crystal, source);
   }

   @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
   private void mysticalterrain$save(ValueOutput output, CallbackInfo ci) {
      this.mysticalterrain$rework().save(output);
   }

   @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
   private void mysticalterrain$load(ValueInput input, CallbackInfo ci) {
      this.mysticalterrain$rework().load(input);
   }
}

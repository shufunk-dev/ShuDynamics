package com.vexira.mysticalterrain.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.vexira.mysticalterrain.boss.dragon.DragonAccess;
import com.vexira.mysticalterrain.boss.dragon.DragonRework;
import com.vexira.mysticalterrain.boss.dragon.DragonReworkHolder;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.PositionInterpolator;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.boss.dragon.phase.Phase;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.data.DataTracker.Builder;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragonEntity.class)
public abstract class EnderDragonMixin extends MobEntity implements DragonReworkHolder, DragonAccess {
   @Unique
   private static final TrackedData<Integer> MYSTICALTERRAIN_STATE = DataTracker.registerData(EnderDragonEntity.class, TrackedDataHandlerRegistry.INTEGER);
   @Unique
   private DragonRework mysticalterrain$rework;

   protected EnderDragonMixin(EntityType<? extends MobEntity> type, World level) {
      super(type, level);
   }

   @Shadow
   protected abstract void method_6819(ServerWorld var1, DamageSource var2, float var3);

   @Inject(method = "method_5693", at = @At("TAIL"))
   private void mysticalterrain$defineState(Builder builder, CallbackInfo ci) {
      builder.add(MYSTICALTERRAIN_STATE, 0);
   }

   @Override
   public DragonRework mysticalterrain$rework() {
      if (this.mysticalterrain$rework == null) {
         this.mysticalterrain$rework = new DragonRework((EnderDragonEntity)(Object)this);
      }

      return this.mysticalterrain$rework;
   }

   @Override
   public int mysticalterrain$state() {
      return (Integer)this.dataTracker.get(MYSTICALTERRAIN_STATE);
   }

   @Override
   public void mysticalterrain$setState(int state) {
      this.dataTracker.set(MYSTICALTERRAIN_STATE, state);
   }

   @Override
   public void mysticalterrain$reallyHurt(ServerWorld level, DamageSource source, float amount) {
      this.method_6819(level, source, amount);
   }

   @WrapOperation(method = "method_6007", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1521;method_6851()Lnet/minecraft/class_243;"))
   private Vec3d mysticalterrain$holdCourse(Phase phase, Operation<Vec3d> original) {
      return !this.getEntityWorld().isClient() && this.mysticalterrain$rework().steering() ? null : (Vec3d)original.call(new Object[]{phase});
   }

   @WrapOperation(method = "method_6007", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1521;method_6855(Lnet/minecraft/class_3218;)V"))
   private void mysticalterrain$holdPhase(Phase phase, ServerWorld level, Operation<Void> original) {
      if (!this.mysticalterrain$rework().holdsPhase()) {
         original.call(new Object[]{phase, level});
      }
   }

   @WrapOperation(
      method = "method_6007",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1510;method_6825(Lnet/minecraft/class_3218;Ljava/util/List;)V")
   )
   private void mysticalterrain$spareKnockBack(EnderDragonEntity self, ServerWorld level, List<Entity> hit, Operation<Void> original) {
      original.call(new Object[]{self, level, this.mysticalterrain$rework().spare(hit)});
   }

   @WrapOperation(
      method = "method_6007",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1510;method_6827(Lnet/minecraft/class_3218;Ljava/util/List;)V")
   )
   private void mysticalterrain$spareBite(EnderDragonEntity self, ServerWorld level, List<Entity> hit, Operation<Void> original) {
      original.call(new Object[]{self, level, this.mysticalterrain$rework().spare(hit)});
   }

   @Inject(method = "method_6108", at = @At("HEAD"), cancellable = true)
   private void mysticalterrain$customDeath(CallbackInfo ci) {
      ci.cancel();
      if (this.getEntityWorld() instanceof ServerWorld server) {
         this.mysticalterrain$rework().customDeath(server);
      } else {
         ((EnderDragonEntity)(Object)this).ticksSinceDeath++;
         PositionInterpolator interpolation = this.getInterpolator();
         if (interpolation != null) {
            interpolation.tick();
         }
      }
   }

   @WrapOperation(
      method = "method_6007",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1937;method_8406(Lnet/minecraft/class_2394;DDDDDD)V"),
      require = 0
   )
   private void mysticalterrain$noVanillaDeathParticles(
      World level, ParticleEffect p, double x, double y, double z, double dx, double dy, double dz, Operation<Void> original
   ) {
      if (!((EnderDragonEntity)(Object)this).isDead()) {
         original.call(new Object[]{level, p, x, y, z, dx, dy, dz});
      }
   }

   @Inject(method = "method_6007", at = @At("HEAD"))
   private void mysticalterrain$followNoAi(CallbackInfo ci) {
      if (this.getEntityWorld().isClient() && this.isAiDisabled() && !this.isDead()) {
         PositionInterpolator interpolation = this.getInterpolator();
         if (interpolation != null) {
            interpolation.tick();
         }
      }
   }

   @Inject(method = "method_6007", at = @At("TAIL"))
   private void mysticalterrain$tickRework(CallbackInfo ci) {
      if (this.getEntityWorld() instanceof ServerWorld server && !this.isAiDisabled()) {
         this.mysticalterrain$rework().tick(server);
      }
   }

   @Inject(method = "method_6816(Lnet/minecraft/class_3218;Lnet/minecraft/class_1508;Lnet/minecraft/class_1282;F)Z", at = @At("HEAD"), cancellable = true)
   private void mysticalterrain$hurt(ServerWorld level, EnderDragonPart part, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
      cir.setReturnValue(this.mysticalterrain$rework().hurt(level, part, source, amount));
   }

   @Inject(method = "method_6828", at = @At("HEAD"))
   private void mysticalterrain$crystalDestroyed(ServerWorld level, EndCrystalEntity crystal, BlockPos pos, DamageSource source, CallbackInfo ci) {
      this.mysticalterrain$rework().onCrystalDestroyed(level, crystal, source);
   }

   @Inject(method = "method_5652", at = @At("TAIL"))
   private void mysticalterrain$save(WriteView output, CallbackInfo ci) {
      this.mysticalterrain$rework().save(output);
   }

   @Inject(method = "method_5749", at = @At("TAIL"))
   private void mysticalterrain$load(ReadView input, CallbackInfo ci) {
      this.mysticalterrain$rework().load(input);
   }
}

package com.vexira.mysticalterrain.boss;

import java.util.List;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public class RealmMob extends MysticalBoss {
   private static final float[] NO_PHASES = new float[0];

   public RealmMob(EntityType<? extends HostileEntity> type, World level) {
      super(type, level, RealmMobs.idOf(type), Color.WHITE);
   }

   private RealmMobs.Kind kind() {
      return RealmMobs.kind(this.bossId());
   }

   @Override
   public boolean isBoss() {
      return false;
   }

   @Override
   protected void defineActions(List<BossAction> out) {
      this.kind().actions().accept(out);
   }

   @Override
   protected float[] phaseThresholds() {
      return NO_PHASES;
   }

   @Override
   protected boolean hovers() {
      return this.kind().flies();
   }

   @Override
   protected double hoverHeight() {
      return this.kind().hover();
   }

   @Override
   protected double preferredDistance() {
      return this.kind().distance();
   }

   @Override
   protected double targetRange() {
      return 24.0;
   }

   @Override
   public int introTicks() {
      return 1;
   }

   @Override
   public int deathTicks() {
      return this.kind().deathTicks();
   }

   @Override
   protected String moveAnim() {
      LivingEntity target = this.getTarget();
      return this.kind().runs() && target != null && this.squaredDistanceTo(target) > 25.0 ? "run" : "walk";
   }

   @Override
   protected void onDeathTick(ServerWorld level, int tick) {
      if (tick == this.deathTicks() - 1) {
         level.spawnParticles(
            this.kind().deathParticle(),
            this.getX(),
            this.getY() + this.getHeight() / 2.0F,
            this.getZ(),
            16,
            this.getWidth() / 2.0F,
            this.getHeight() / 3.0F,
            this.getWidth() / 2.0F,
            0.02
         );
      }
   }

   @Override
   protected SoundEvent roarSound() {
      return this.kind().sounds().ambient();
   }

   protected @Nullable SoundEvent getAmbientSound() {
      return this.kind().sounds().ambient();
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return this.kind().sounds().hurt();
   }

   protected SoundEvent getDeathSound() {
      return this.kind().sounds().death();
   }
}

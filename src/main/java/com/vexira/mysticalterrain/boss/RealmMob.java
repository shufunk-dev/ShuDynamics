package com.vexira.mysticalterrain.boss;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class RealmMob extends MysticalBoss {
   private static final float[] NO_PHASES = new float[0];

   public RealmMob(EntityType<? extends Monster> type, Level level) {
      super(type, level, RealmMobs.idOf(type), BossBarColor.WHITE);
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
      return this.kind().runs() && target != null && this.distanceToSqr(target) > 25.0 ? "run" : "walk";
   }

   @Override
   protected void onDeathTick(ServerLevel level, int tick) {
      if (tick == this.deathTicks() - 1) {
         level.sendParticles(
            this.kind().deathParticle(),
            this.getX(),
            this.getY() + this.getBbHeight() / 2.0F,
            this.getZ(),
            16,
            this.getBbWidth() / 2.0F,
            this.getBbHeight() / 3.0F,
            this.getBbWidth() / 2.0F,
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

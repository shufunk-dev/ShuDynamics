package com.vexira.mysticalterrain.client.boss;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import org.jspecify.annotations.Nullable;
import com.geckolib.animatable.GeoReplacedEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.util.GeckoLibUtil;

@Environment(EnvType.CLIENT)
public class DragonAnimatable implements GeoReplacedEntity {
   private static final String PREFIX = "animation.ender_dragon.";
   private static final DataTicket<Integer> SEEN_SERIAL = DataTicket.create("mysticalterrain_dragon_seen_serial", Integer.class);
   private static final int LOOP_BLEND = 7;
   private static final int ONE_SHOT_BLEND = 2;
   private static final RawAnimation FLY = loop("fly");
   private static final RawAnimation FLY_FAST = loop("fly_fast");
   private static final RawAnimation CLIMB = loop("climb");
   private static final RawAnimation GLIDE = loop("glide");
   private static final RawAnimation BANK_LEFT = loop("bank_left");
   private static final RawAnimation BANK_RIGHT = loop("bank_right");
   private static final RawAnimation HOVER = loop("hover");
   private static final RawAnimation TAKEOFF = then("takeoff", "fly");
   private static final RawAnimation LAND = hold("land");
   private static final RawAnimation SCAN = loop("perch_scan");
   private static final RawAnimation SUMMON = hold("summon");
   private static final RawAnimation DEATH = hold("death");
   private static final RawAnimation PHASE_1 = then("phase_1", "hover");
   private static final RawAnimation PHASE_2 = then("phase_2", "hover");
   private static final RawAnimation PHASE_3 = then("phase_3", "hover");
   private static final RawAnimation CRASH = then("crash", "stunned");
   private static final RawAnimation STAGGER = then("stagger", "stunned");
   private static final RawAnimation STUNNED = loop("stunned");
   private static final RawAnimation RECOVER = then("recover", "perch");
   private static final RawAnimation FIREBALL = then("fireball", "fly");
   private static final RawAnimation STRAFE_BREATH = then("strafe_breath", "fly");
   private static final RawAnimation RAKE_DIVE = then("rake_dive", "fly");
   private static final RawAnimation ROAR = then("roar", "fly");
   private static final RawAnimation TALON_RAPTURE = then("talon_rapture", "fly");
   private static final RawAnimation WING_WARD = then("wing_ward", "hover");
   private static final RawAnimation VOID_BEAM = then("void_beam", "hover");
   private static final RawAnimation GRAVITY_WELL = then("gravity_well", "hover");
   private static final RawAnimation METEOR_RAIN = then("meteor_rain", "hover");
   private static final RawAnimation OBLIVION_NOVA = then("oblivion_nova", "hover");
   private static final RawAnimation CRYSTAL_RECALL = loop("crystal_recall");
   private static final RawAnimation SHATTERDIVE = then("shatterdive", "dive");
   private static final RawAnimation DIVE = loop("dive");
   private static final RawAnimation SLAM = then("slam", "perch");
   private static final RawAnimation BREATH = loop("breath");
   private static final RawAnimation PERCH_ROAR = then("perch_roar", "perch");
   private static final RawAnimation BREATH_SWEEP = then("breath_sweep", "perch");
   private static final RawAnimation TAIL_SWEEP = then("tail_sweep", "perch");
   private static final RawAnimation RENDING_BITE = then("rending_bite", "perch");
   private static final RawAnimation VOID_FISSURE = then("void_fissure", "perch");
   private static final RawAnimation WING_STORM = then("wing_storm", "perch");
   private static final RawAnimation POUNCE = then("pounce", "perch");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   private static RawAnimation loop(String clip) {
      return RawAnimation.begin().thenLoop("animation.ender_dragon." + clip);
   }

   private static RawAnimation hold(String clip) {
      return RawAnimation.begin().thenPlayAndHold("animation.ender_dragon." + clip);
   }

   private static RawAnimation then(String clip, String next) {
      return RawAnimation.begin().thenPlay("animation.ender_dragon." + clip).thenLoop("animation.ender_dragon." + next);
   }

   public EntityType<?> getReplacingEntityType() {
      return EntityTypes.ENDER_DRAGON;
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("main", 7, this::animate));
   }

   private PlayState animate(AnimationTest<DragonAnimatable> test) {
      if ((Boolean)test.getDataOrDefault(DragonGeoRenderer.DYING, false)) {
         test.controller().setTransitionTicks(2);
         return test.setAndContinue(DEATH);
      }

      int state = (Integer)test.getDataOrDefault(DragonGeoRenderer.STATE, 0);
      boolean loopish = state == 0 || state == 30 || state == 31 || state == 3 || state == 4 || state == 8 || state == 7;
      test.controller().setTransitionTicks(loopish ? 7 : 2);
      RawAnimation anim = byState(state);
      if (anim == null) {
         anim = byPhase(test);
      }

      int serial = (Integer)test.getDataOrDefault(DragonGeoRenderer.SERIAL, 0);
      Integer seen = (Integer)test.manager().getAnimatableData(SEEN_SERIAL);
      if (seen != null && seen != serial && state != 0 && test.isCurrentAnimation(anim)) {
         test.controller().reset();
      }

      test.manager().setAnimatableData(SEEN_SERIAL, serial);
      return test.setAndContinue(anim);
   }

   private static @Nullable RawAnimation byState(int state) {
      return switch (state) {
         case 1 -> SUMMON;
         case 2 -> BREATH_SWEEP;
         case 3 -> CRYSTAL_RECALL;
         case 4 -> CLIMB;
         case 5 -> SLAM;
         case 6 -> ROAR;
         case 7 -> STUNNED;
         case 8 -> DIVE;
         case 9 -> FIREBALL;
         case 10 -> STRAFE_BREATH;
         case 11 -> RAKE_DIVE;
         case 12 -> TALON_RAPTURE;
         case 13 -> WING_WARD;
         case 14 -> VOID_BEAM;
         case 15 -> GRAVITY_WELL;
         case 16 -> METEOR_RAIN;
         case 17 -> SHATTERDIVE;
         case 18 -> OBLIVION_NOVA;
         case 19 -> TAIL_SWEEP;
         case 20 -> RENDING_BITE;
         case 21 -> VOID_FISSURE;
         case 22 -> WING_STORM;
         case 23 -> POUNCE;
         case 24 -> CRASH;
         case 25 -> STAGGER;
         case 26 -> RECOVER;
         case 27 -> PHASE_1;
         case 28 -> PHASE_2;
         case 29 -> PHASE_3;
         case 30 -> FLY_FAST;
         case 31 -> FLY;
         default -> null;
      };
   }

   private static RawAnimation byPhase(AnimationTest<DragonAnimatable> test) {
      int phase = (Integer)test.getDataOrDefault(DragonGeoRenderer.PHASE, EnderDragonPhase.HOLDING_PATTERN.getId());
      if (phase == EnderDragonPhase.SITTING_FLAMING.getId()) {
         return BREATH;
      } else if (phase == EnderDragonPhase.SITTING_ATTACKING.getId()) {
         return PERCH_ROAR;
      } else if (phase == EnderDragonPhase.SITTING_SCANNING.getId()) {
         return SCAN;
      } else if (phase == EnderDragonPhase.HOVERING.getId()) {
         return HOVER;
      } else if (phase == EnderDragonPhase.TAKEOFF.getId()) {
         return TAKEOFF;
      } else if (phase == EnderDragonPhase.LANDING.getId()) {
         return LAND;
      } else if (phase == EnderDragonPhase.LANDING_APPROACH.getId()) {
         return GLIDE;
      } else if (phase == EnderDragonPhase.CHARGING_PLAYER.getId()) {
         return FLY_FAST;
      } else {
         float turn = (Float)test.getDataOrDefault(DragonGeoRenderer.TURN, 0.0F);
         float climb = (Float)test.getDataOrDefault(DragonGeoRenderer.CLIMB, 0.0F);
         if (turn > 12.0F) {
            return BANK_RIGHT;
         } else if (turn < -12.0F) {
            return BANK_LEFT;
         } else if (climb > 1.2F) {
            return CLIMB;
         } else {
            return climb < -1.2F ? GLIDE : FLY;
         }
      }
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}

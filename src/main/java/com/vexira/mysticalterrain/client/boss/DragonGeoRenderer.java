package com.vexira.mysticalterrain.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.dragon.DragonPose;
import com.vexira.mysticalterrain.boss.dragon.DragonReworkHolder;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import org.jspecify.annotations.Nullable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

@Environment(EnvType.CLIENT)
public class DragonGeoRenderer extends GeoReplacedEntityRenderer<DragonAnimatable, EnderDragon, LivingEntityRenderState> {
   public static final DataTicket<Integer> STATE = DataTicket.create("mysticalterrain_dragon_state", Integer.class);
   public static final DataTicket<Integer> SERIAL = DataTicket.create("mysticalterrain_dragon_serial", Integer.class);
   public static final DataTicket<Integer> FIGHT_PHASE = DataTicket.create("mysticalterrain_dragon_fight_phase", Integer.class);
   public static final DataTicket<Boolean> DYING = DataTicket.create("mysticalterrain_dragon_dying", Boolean.class);
   public static final DataTicket<Boolean> SITTING = DataTicket.create("mysticalterrain_dragon_sitting", Boolean.class);
   public static final DataTicket<Boolean> FLAMING = DataTicket.create("mysticalterrain_dragon_flaming", Boolean.class);
   public static final DataTicket<Integer> PHASE = DataTicket.create("mysticalterrain_dragon_phase", Integer.class);
   public static final DataTicket<Float> TURN = DataTicket.create("mysticalterrain_dragon_turn", Float.class);
   public static final DataTicket<Float> CLIMB = DataTicket.create("mysticalterrain_dragon_climb", Float.class);
   private static final DataTicket<float[]> POSE = DataTicket.create("mysticalterrain_dragon_pose", float[].class);
   private static final float EASE = 0.35F;
   private static final Map<EnderDragon, float[]> DRAWN = new WeakHashMap<>();

   public DragonGeoRenderer(Context context) {
      super(context, new DefaultedEntityGeoModel(MysticalTerrain.id("ender_dragon")), new DragonAnimatable());
      this.withRenderLayer(new AutoGlowingGeoLayer(this));
      this.withScale(0.9F);
      this.shadowRadius = 0.0F;
   }

   protected boolean affectedByCulling(EnderDragon dragon) {
      return false;
   }

   public void addRenderData(DragonAnimatable animatable, @Nullable EnderDragon dragon, LivingEntityRenderState state, float partialTick) {
      if (dragon != null) {
         DragonPhaseInstance phase = dragon.getPhaseManager().getCurrentPhase();
         EnderDragonPhase<?> type = phase.getPhase();
         int raw = ((DragonReworkHolder)dragon).mysticalterrain$state();
         int rework = raw & 0xFF;
         state.addGeckolibData(STATE, rework);
         state.addGeckolibData(SERIAL, raw >>> 16 & 0xFF);
         state.addGeckolibData(FIGHT_PHASE, raw >>> 8 & 3);
         state.addGeckolibData(DYING, type == EnderDragonPhase.DYING || dragon.dragonDeathTime > 0);
         state.addGeckolibData(SITTING, phase.isSitting());
         state.addGeckolibData(FLAMING, type == EnderDragonPhase.SITTING_FLAMING);
         state.addGeckolibData(PHASE, type.getId());
         state.addGeckolibData(TURN, Mth.wrapDegrees(dragon.flightHistory.get(0).yRot() - dragon.flightHistory.get(6).yRot()));
         state.addGeckolibData(CLIMB, (float)(dragon.flightHistory.get(0).y() - dragon.flightHistory.get(6).y()));
         state.lightCoords = LightCoordsUtil.pack(
            Math.max(LightCoordsUtil.block(state.lightCoords), 13),
            Math.max(LightCoordsUtil.sky(state.lightCoords), 13)
         );
         state.addGeckolibData(POSE, ease(dragon, DragonPose.pose(dragon, partialTick), dragon.tickCount + partialTick));
      }
   }

   private static float[] ease(EnderDragon dragon, float[] pose, float now) {
      float[] drawn = DRAWN.get(dragon);
      if (drawn == null) {
         drawn = new float[40];
         System.arraycopy(pose, 0, drawn, 0, 39);
         drawn[39] = now;
         DRAWN.put(dragon, drawn);
         return pose;
      } else {
         float dt = now - drawn[39];
         drawn[39] = now;
         float k = !(dt < 0.0F) && !(dt > 20.0F) ? 1.0F - (float)Math.exp(-dt * 0.35F) : 1.0F;
         follow(pose, drawn, 1, 1, k);
         follow(pose, drawn, 8, 5, k);
         follow(pose, drawn, 27, 12, k);
         return pose;
      }
   }

   private static void follow(float[] pose, float[] drawn, int from, int count, float k) {
      for (int i = from; i < from + count; i++) {
         drawn[i] += (pose[i] - drawn[i]) * k;
         pose[i] = drawn[i];
      }
   }

   protected void applyRotations(RenderPassInfo<LivingEntityRenderState> info, PoseStack poseStack, float nativeScale) {
      float[] pose = (float[])info.getGeckolibData(POSE);
      if (pose == null) {
         super.applyRotations(info, poseStack, nativeScale);
      } else {
         poseStack.rotateDegrees(Axis.YP, -pose[0]);
         poseStack.rotateDegrees(Axis.XP, pose[1]);
         poseStack.translate(0.0F, 0.0F, 1.0F);
      }
   }

   protected float getDeathMaxRotation(GeoRenderState state) {
      return 0.0F;
   }

   public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots snapshots) {
      boolean flare = (Integer)info.getOrDefaultGeckolibData(FIGHT_PHASE, 0) >= 2;
      snapshots.ifPresent("core_flare", b -> b.skipRender(!flare));
      float[] pose = (float[])info.getGeckolibData(POSE);
      if (pose != null) {
         snapshots.ifPresent("body", b -> b.setRotZ(b.getRotZ() + pose[2] * (float) (Math.PI / 180.0)));
         float prevYaw = 0.0F;
         float prevPitch = 0.0F;

         for (int n = 0; n < 5; n++) {
            float yaw = pose[3 + n];
            float pitch = pose[8 + n];
            bend(snapshots, "neck" + (n + 1), yaw - prevYaw, pitch - prevPitch);
            prevYaw = yaw;
            prevPitch = pitch;
         }

         bend(snapshots, "head", pose[13] - prevYaw, pose[14] - prevPitch);
         prevYaw = 0.0F;
         prevPitch = 0.0F;

         for (int q = 0; q < 12; q++) {
            float yaw = pose[15 + q];
            float pitch = -pose[27 + q];
            bend(snapshots, "tail" + (q + 1), yaw - prevYaw, pitch - prevPitch);
            prevYaw = yaw;
            prevPitch = pitch;
         }
      }
   }

   private static void bend(BoneSnapshots snapshots, String bone, float yawDeg, float pitchDeg) {
      snapshots.ifPresent(bone, b -> {
         b.setRotY(b.getRotY() - yawDeg * (float) (Math.PI / 180.0));
         b.setRotX(b.getRotX() + pitchDeg * (float) (Math.PI / 180.0));
      });
   }
}

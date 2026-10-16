package com.vexira.mysticalterrain.client.boss;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.dragon.DragonPose;
import com.vexira.mysticalterrain.boss.dragon.DragonReworkHolder;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.Phase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import software.bernie.geckolib.renderer.base.BoneSnapshots;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.RenderPassInfo;
import software.bernie.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

@Environment(EnvType.CLIENT)
public class DragonGeoRenderer extends GeoReplacedEntityRenderer<DragonAnimatable, EnderDragonEntity, LivingEntityRenderState> {
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
   private static final Map<EnderDragonEntity, float[]> DRAWN = new WeakHashMap<>();

   public DragonGeoRenderer(Context context) {
      super(context, new DefaultedEntityGeoModel(MysticalTerrain.id("ender_dragon")), new DragonAnimatable());
      this.withRenderLayer(new AutoGlowingGeoLayer(this));
      this.withScale(0.9F);
      this.shadowRadius = 0.0F;
   }

   protected boolean affectedByCulling(EnderDragonEntity dragon) {
      return false;
   }

   public void addRenderData(DragonAnimatable animatable, @Nullable EnderDragonEntity dragon, LivingEntityRenderState state, float partialTick) {
      if (dragon != null) {
         Phase phase = dragon.getPhaseManager().getCurrent();
         PhaseType<?> type = phase.getType();
         int raw = ((DragonReworkHolder)dragon).mysticalterrain$state();
         int rework = raw & 0xFF;
         state.addGeckolibData(STATE, rework);
         state.addGeckolibData(SERIAL, raw >>> 16 & 0xFF);
         state.addGeckolibData(FIGHT_PHASE, raw >>> 8 & 3);
         state.addGeckolibData(DYING, type == PhaseType.DYING || dragon.ticksSinceDeath > 0);
         state.addGeckolibData(SITTING, phase.isSittingOrHovering());
         state.addGeckolibData(FLAMING, type == PhaseType.SITTING_FLAMING);
         state.addGeckolibData(PHASE, type.getTypeId());
         state.addGeckolibData(TURN, MathHelper.wrapDegrees(dragon.frameTracker.getFrame(0).yRot() - dragon.frameTracker.getFrame(6).yRot()));
         state.addGeckolibData(CLIMB, (float)(dragon.frameTracker.getFrame(0).y() - dragon.frameTracker.getFrame(6).y()));
         state.light = LightmapTextureManager.pack(
            Math.max(LightmapTextureManager.getBlockLightCoordinates(state.light), 13),
            Math.max(LightmapTextureManager.getSkyLightCoordinates(state.light), 13)
         );
         state.addGeckolibData(POSE, ease(dragon, DragonPose.pose(dragon, partialTick), dragon.age + partialTick));
      }
   }

   private static float[] ease(EnderDragonEntity dragon, float[] pose, float now) {
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

   protected void applyRotations(RenderPassInfo<LivingEntityRenderState> info, MatrixStack poseStack, float nativeScale) {
      float[] pose = (float[])info.getGeckolibData(POSE);
      if (pose == null) {
         super.applyRotations(info, poseStack, nativeScale);
      } else {
         poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-pose[0]));
         poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose[1]));
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

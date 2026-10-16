package com.vexira.mysticalterrain.boss.dragon;

import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFrameTracker;
import net.minecraft.entity.boss.dragon.EnderDragonFrameTracker.Frame;
import net.minecraft.entity.boss.dragon.phase.Phase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap.Type;

public final class DragonPose {
   public static final int NECK = 5;
   public static final int TAIL = 12;
   public static final int BODY_YAW = 0;
   public static final int BODY_PITCH = 1;
   public static final int BODY_ROLL = 2;
   public static final int NECK_YAW = 3;
   public static final int NECK_PITCH = 8;
   public static final int HEAD_YAW = 13;
   public static final int HEAD_PITCH = 14;
   public static final int TAIL_YAW = 15;
   public static final int TAIL_PITCH = 27;
   public static final int SIZE = 39;
   private static final float MAX_BODY_PITCH = 45.0F;
   private static final float MAX_SEGMENT_PITCH = 50.0F;
   private static final double PX = 0.05625;
   private static final double SEGMENT = 0.675;
   private static final double HEAD_AHEAD = 4.3875;
   private static final double CLEARANCE = 0.6;
   private static final double[] JAW_FLY = new double[]{5.16, 1.74, 0.0};
   private static final double[] JAW_SPIT = new double[]{5.17, 2.4, 0.0};
   private static final double[] JAW_STRAFE = new double[]{4.3, -0.5, 0.0};
   private static final double[] JAW_HOVER = new double[]{4.87, 2.27, 0.0};
   private static final double[] JAW_LAND = new double[]{4.7, 3.4, 0.0};
   private static final double[] JAW_PERCH = new double[]{4.75, 2.1, 0.0};
   private static final double[] JAW_GROUNDED = new double[]{5.05, 0.9, 1.0};
   private static final double[] LOW_FLY = new double[]{5.0, 0.9};
   private static final double[] LOW_STRAFE = new double[]{4.25, -0.6};
   private static final double[] LOW_RAKE = new double[]{4.2, -1.15};
   private static final double[] LOW_CINEMATIC = new double[]{3.0, -1.5};

   private DragonPose() {
   }

   private static int state(EnderDragonEntity dragon) {
      return ((DragonReworkHolder)dragon).mysticalterrain$state() & 0xFF;
   }

   public static boolean clipAttitude(int state) {
      return switch (state) {
         case 5, 7, 8, 12, 17, 23, 24, 25, 26 -> true;
         default -> false;
      };
   }

   public static float[] pose(EnderDragonEntity dragon, float partialTick) {
      Phase phase = dragon.getPhaseManager().getCurrent();
      PhaseType<?> type = phase.getType();
      int state = state(dragon);
      boolean dying = dragon.isDead();
      boolean flat = phase.isSittingOrHovering() || clipAttitude(state);
      boolean landing = type == PhaseType.LANDING || type == PhaseType.TAKEOFF;
      float[] out = bends(dragon.frameTracker, dying ? 0.0F : partialTick, flat, landing);
      if (!flat) {
         guard(dragon, out, dragon.getLerpedPos(partialTick), low(state, dying));
      }

      return out;
   }

   public static Vec3d mouth(EnderDragonEntity dragon, float partialTick) {
      float[] out = pose(dragon, partialTick);
      double[] jaw = jaw(dragon, state(dragon));
      return world(out, dragon.getLerpedPos(partialTick), jaw[0], jaw[1], jaw[2], 1.0F);
   }

   private static double[] jaw(EnderDragonEntity dragon, int state) {
      PhaseType<?> p = dragon.getPhaseManager().getCurrent().getType();

      return switch (state) {
         case 7, 24, 25, 26 -> JAW_GROUNDED;
         default -> p == PhaseType.LANDING || p == PhaseType.TAKEOFF
            ? JAW_LAND
            : (
               p == PhaseType.HOVER
                  ? JAW_HOVER
                  : (p != PhaseType.SITTING_SCANNING && p != PhaseType.SITTING_ATTACKING && p != PhaseType.SITTING_FLAMING ? JAW_FLY : JAW_PERCH)
            );
         case 9 -> JAW_SPIT;
         case 10 -> JAW_STRAFE;
      };
   }

   private static double[] low(int state, boolean dying) {
      if (dying) {
         return LOW_CINEMATIC;
      }

      return switch (state) {
         case 1, 18, 27, 28, 29 -> LOW_CINEMATIC;
         case 10 -> LOW_STRAFE;
         case 11 -> LOW_RAKE;
         default -> LOW_FLY;
      };
   }

   private static float[] bends(EnderDragonFrameTracker history, float pt, boolean flat, boolean landing) {
      float[] out = new float[39];
      Frame h5 = history.getLerpedFrame(5, pt);
      Frame h6 = history.getLerpedFrame(6, pt);
      Frame h10 = history.getLerpedFrame(10, pt);
      out[0] = history.getLerpedFrame(7, pt).yRot();
      out[1] = flat ? 0.0F : MathHelper.clamp((float)(h5.y() - h10.y()) * 10.0F, -45.0F, 45.0F);
      out[2] = -MathHelper.wrapDegrees(h5.yRot() - h10.yRot()) * 1.5F;

      for (int n = 0; n < 5; n++) {
         Frame s = history.getLerpedFrame(5 - n, pt);
         out[3 + n] = MathHelper.wrapDegrees(s.yRot() - h6.yRot()) * 1.5F;
         out[8 + n] = !flat && !landing ? MathHelper.clamp((float)(s.y() - h6.y()) * 1.5F * 5.0F, -50.0F, 50.0F) : 0.0F;
      }

      out[13] = MathHelper.wrapDegrees(history.getLerpedFrame(0, pt).yRot() - h6.yRot());
      out[14] = 0.0F;
      Frame h11 = history.getLerpedFrame(11, pt);

      for (int q = 0; q < 12; q++) {
         Frame s = history.getLerpedFrame(12 + q, pt);
         out[15 + q] = MathHelper.wrapDegrees(s.yRot() - h11.yRot()) * 1.5F;
         out[27 + q] = flat ? 0.0F : MathHelper.clamp((float)(s.y() - h11.y()) * 1.5F * 5.0F, -50.0F, 50.0F);
      }

      return out;
   }

   private static void guard(EnderDragonEntity dragon, float[] out, Vec3d pos, double[] low) {
      if (droops(out)) {
         for (int i = 4; i > 0; i--) {
            float s = i / 4.0F;
            Vec3d p = world(out, pos, low[0], low[1], 0.0, s);
            int ground = dragon.getEntityWorld().getTopY(Type.MOTION_BLOCKING, MathHelper.floor(p.x), MathHelper.floor(p.z));
            if (ground > pos.y + 1.0 || p.y >= ground + 0.6) {
               droop(out, s);
               return;
            }
         }

         droop(out, 0.0F);
      }
   }

   private static boolean droops(float[] out) {
      if (out[1] < 0.0F) {
         return true;
      }

      for (int n = 0; n < 5; n++) {
         if (out[8 + n] < 0.0F) {
            return true;
         }
      }

      return false;
   }

   private static void droop(float[] out, float s) {
      out[1] = nose(out[1], s);

      for (int n = 0; n < 5; n++) {
         out[8 + n] = nose(out[8 + n], s);
      }
   }

   private static float nose(float pitch, float s) {
      return pitch < 0.0F ? pitch * s : pitch;
   }

   private static Vec3d world(float[] out, Vec3d pos, double ahead, double up, double right, float s) {
      double a = ahead;
      double u = up;
      double r = right;

      for (int n = 0; n < 5; n++) {
         double p = Math.toRadians(nose(out[8 + n], s));
         double y = Math.toRadians(out[3 + n]);
         a += 0.675 * (Math.cos(p) * Math.cos(y) - 1.0);
         u += 0.675 * Math.sin(p);
         r += 0.675 * Math.cos(p) * Math.sin(y);
      }

      double h = Math.toRadians(out[13]);
      double d = ahead - 4.3875;
      a += d * (Math.cos(h) - 1.0);
      r += d * Math.sin(h);
      double b = Math.toRadians(nose(out[1], s));
      double fa = a * Math.cos(b) - u * Math.sin(b);
      double fu = a * Math.sin(b) + u * Math.cos(b);
      Vec3d f = DragonFx.forward(out[0]);
      Vec3d rt = DragonFx.right(out[0]);
      return pos.add(f.x * fa + rt.x * r, fu, f.z * fa + rt.z * r);
   }
}

package com.vexira.mysticalterrain.boss.dragon;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory.Sample;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

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

   private static int state(EnderDragon dragon) {
      return ((DragonReworkHolder)dragon).mysticalterrain$state() & 0xFF;
   }

   public static boolean clipAttitude(int state) {
      return switch (state) {
         case 5, 7, 8, 12, 17, 23, 24, 25, 26 -> true;
         default -> false;
      };
   }

   public static float[] pose(EnderDragon dragon, float partialTick) {
      DragonPhaseInstance phase = dragon.getPhaseManager().getCurrentPhase();
      EnderDragonPhase<?> type = phase.getPhase();
      int state = state(dragon);
      boolean dying = dragon.isDeadOrDying();
      boolean flat = phase.isSitting() || clipAttitude(state);
      boolean landing = type == EnderDragonPhase.LANDING || type == EnderDragonPhase.TAKEOFF;
      float[] out = bends(dragon.flightHistory, dying ? 0.0F : partialTick, flat, landing);
      if (!flat) {
         guard(dragon, out, dragon.getPosition(partialTick), low(state, dying));
      }

      return out;
   }

   public static Vec3 mouth(EnderDragon dragon, float partialTick) {
      float[] out = pose(dragon, partialTick);
      double[] jaw = jaw(dragon, state(dragon));
      return world(out, dragon.getPosition(partialTick), jaw[0], jaw[1], jaw[2], 1.0F);
   }

   private static double[] jaw(EnderDragon dragon, int state) {
      EnderDragonPhase<?> p = dragon.getPhaseManager().getCurrentPhase().getPhase();

      return switch (state) {
         case 7, 24, 25, 26 -> JAW_GROUNDED;
         default -> p == EnderDragonPhase.LANDING || p == EnderDragonPhase.TAKEOFF
            ? JAW_LAND
            : (
               p == EnderDragonPhase.HOVERING
                  ? JAW_HOVER
                  : (p != EnderDragonPhase.SITTING_SCANNING && p != EnderDragonPhase.SITTING_ATTACKING && p != EnderDragonPhase.SITTING_FLAMING ? JAW_FLY : JAW_PERCH)
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

   private static float[] bends(DragonFlightHistory history, float pt, boolean flat, boolean landing) {
      float[] out = new float[39];
      Sample h5 = history.get(5, pt);
      Sample h6 = history.get(6, pt);
      Sample h10 = history.get(10, pt);
      out[0] = history.get(7, pt).yRot();
      out[1] = flat ? 0.0F : Mth.clamp((float)(h5.y() - h10.y()) * 10.0F, -45.0F, 45.0F);
      out[2] = -Mth.wrapDegrees(h5.yRot() - h10.yRot()) * 1.5F;

      for (int n = 0; n < 5; n++) {
         Sample s = history.get(5 - n, pt);
         out[3 + n] = Mth.wrapDegrees(s.yRot() - h6.yRot()) * 1.5F;
         out[8 + n] = !flat && !landing ? Mth.clamp((float)(s.y() - h6.y()) * 1.5F * 5.0F, -50.0F, 50.0F) : 0.0F;
      }

      out[13] = Mth.wrapDegrees(history.get(0, pt).yRot() - h6.yRot());
      out[14] = 0.0F;
      Sample h11 = history.get(11, pt);

      for (int q = 0; q < 12; q++) {
         Sample s = history.get(12 + q, pt);
         out[15 + q] = Mth.wrapDegrees(s.yRot() - h11.yRot()) * 1.5F;
         out[27 + q] = flat ? 0.0F : Mth.clamp((float)(s.y() - h11.y()) * 1.5F * 5.0F, -50.0F, 50.0F);
      }

      return out;
   }

   private static void guard(EnderDragon dragon, float[] out, Vec3 pos, double[] low) {
      if (droops(out)) {
         for (int i = 4; i > 0; i--) {
            float s = i / 4.0F;
            Vec3 p = world(out, pos, low[0], low[1], 0.0, s);
            int ground = dragon.level().getHeight(Types.MOTION_BLOCKING, Mth.floor(p.x), Mth.floor(p.z));
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

   private static Vec3 world(float[] out, Vec3 pos, double ahead, double up, double right, float s) {
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
      Vec3 f = DragonFx.forward(out[0]);
      Vec3 rt = DragonFx.right(out[0]);
      return pos.add(f.x * fa + rt.x * r, fu, f.z * fa + rt.z * r);
   }
}

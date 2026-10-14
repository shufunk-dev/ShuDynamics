package com.vexira.mysticalterrain.tree;

public final class Noise {
   private Noise() {
   }

   static long mix(long z) {
      z = (z ^ z >>> 30) * -4658895280553007687L;
      z = (z ^ z >>> 27) * -7723592293110705685L;
      return z ^ z >>> 31;
   }

   public static long hash(long seed, int x, int y, int z) {
      return mix(seed ^ mix(x * -7046029254386353131L ^ mix(y * -4417276706812531889L ^ mix(z * 1609587929392839161L))));
   }

   public static double unit(long seed, int x, int y, int z) {
      return (hash(seed, x, y, z) >>> 11) * 1.110223E-16F;
   }

   public static double smooth(long seed, double x, double y, double z) {
      int ix = (int)Math.floor(x);
      int iy = (int)Math.floor(y);
      int iz = (int)Math.floor(z);
      double fx = fade(x - ix);
      double fy = fade(y - iy);
      double fz = fade(z - iz);
      double x00 = lerp(fx, unit(seed, ix, iy, iz), unit(seed, ix + 1, iy, iz));
      double x10 = lerp(fx, unit(seed, ix, iy + 1, iz), unit(seed, ix + 1, iy + 1, iz));
      double x01 = lerp(fx, unit(seed, ix, iy, iz + 1), unit(seed, ix + 1, iy, iz + 1));
      double x11 = lerp(fx, unit(seed, ix, iy + 1, iz + 1), unit(seed, ix + 1, iy + 1, iz + 1));
      return lerp(fz, lerp(fy, x00, x10), lerp(fy, x01, x11)) * 2.0 - 1.0;
   }

   private static double fade(double t) {
      return t * t * (3.0 - 2.0 * t);
   }

   private static double lerp(double t, double a, double b) {
      return a + (b - a) * t;
   }
}

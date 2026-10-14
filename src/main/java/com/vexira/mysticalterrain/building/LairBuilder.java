package com.vexira.mysticalterrain.building;

import java.util.SplittableRandom;

final class LairBuilder {
   private static final int[] DX = new int[]{0, -1, 0, 1};
   private static final int[] DZ = new int[]{1, 0, -1, 0};
   private final Blueprint plan = new Blueprint();
   private final SplittableRandom random;

   private LairBuilder(long seed) {
      this.random = new SplittableRandom(seed);
   }

   static Blueprint build(BuildingParams params) {
      LairBuilder b = new LairBuilder(params.seed());
      switch (params.kind()) {
         case LAIR_FORGE:
            b.forge();
            break;
         case LAIR_GLACIER:
            b.glacier();
            break;
         case LAIR_PALACE:
            b.palace();
            break;
         case LAIR_NEST:
            b.nest();
            break;
         case LAIR_CATHEDRAL:
            b.cathedral();
            break;
         case LAIR_SHRINE:
            b.sunkenShrine();
            break;
         case LAIR_PYRAMID:
            b.pyramid();
            break;
         case LAIR_SPORECAP:
            b.sporecap();
            break;
         case LAIR_OBSERVATORY:
            b.observatory();
            break;
         case LAIR_SPIRE:
            b.spire();
            break;
         default:
            throw new IllegalArgumentException("not a lair: " + params.kind());
      }

      return b.plan.finish();
   }

   private void forge() {
      this.arena(16, 12);

      for (int y = -4; y <= -2; y++) {
         this.disc(0, 0, 17.4, y, Part.FOUNDATION);
      }

      this.ringWall(16.4, 0, 6, Part.WALL);
      this.crenels(16, 7);
      this.disc(0, 0, 3.4, -1, Part.FLOOR);
      this.annulus(3.5, 5.4, -1, Part.LAVA);
      this.annulus(3.5, 5.4, -2, Part.FOUNDATION);

      for (int k = 0; k < 4; k++) {
         this.bridge(k, 3, 6);
      }

      for (int k = 0; k < 8; k++) {
         double a = k * Math.PI / 4.0 + (Math.PI / 8);
         int x = (int)Math.round(Math.cos(a) * 11.0);
         int z = (int)Math.round(Math.sin(a) * 11.0);
         this.pillar(x, z, 0, 7, Part.TRIM);
         this.put(x, 8, z, Part.LIGHT);
      }

      this.gate(0, 16, 3, 5);
      this.altar(0, 0);
      this.chests(13, 2, true);
   }

   private void glacier() {
      this.arena(17, 12);

      for (int y = -4; y <= -2; y++) {
         this.disc(0, 0, 18.4, y, Part.FOUNDATION);
      }

      this.annulus(3.5, 7.4, -1, Part.WATER);
      this.annulus(3.5, 7.4, -2, Part.WATER);
      this.annulus(3.5, 7.4, -3, Part.FOUNDATION);
      this.disc(0, 0, 3.4, -1, Part.FLOOR);

      for (int k = 0; k < 18; k++) {
         double a = k * Math.PI * 2.0 / 18.0;
         int x = (int)Math.round(Math.cos(a) * 17.0);
         int z = (int)Math.round(Math.sin(a) * 17.0);
         if (Math.abs(x) > 2 || z <= 0) {
            this.pillar(x, z, 0, 5 + this.random.nextInt(7), Part.WALL);
         }
      }

      int[][] bells = new int[][]{{0, -9}, {9, 0}, {0, 9}, {-9, 0}};

      for (int n = 0; n < 4; n++) {
         this.put(bells[n][0], -1, bells[n][1], Part.FLOOR);
         this.put(bells[n][0], 0, bells[n][1], Part.FIXTURE, n, false);
      }

      this.bridge(2, 3, 8);
      this.altar(0, 0);
      this.chests(14, 2, true);
   }

   private void palace() {
      this.arena(15, 12);

      for (int y = -3; y <= -1; y++) {
         this.disc(0, 0, 15.4 - (y + 3) * 0.5, y, y == -1 ? Part.FLOOR : Part.FOUNDATION);
      }

      for (int k = 0; k < 12; k++) {
         double a = k * Math.PI / 6.0;
         int x = (int)Math.round(Math.cos(a) * 12.0);
         int z = (int)Math.round(Math.sin(a) * 12.0);
         this.pillar(x, z, 0, 8, Part.TRIM);
         this.put(x, 9, z, Part.WALL);
      }

      this.ringWall(12.4, 9, 9, Part.WALL);
      this.annulus(5.0, 6.4, 0, Part.PLANT);
      this.annulus(8.5, 9.4, -1, Part.PATH);

      for (int k = 0; k < 6; k++) {
         double a = k * Math.PI / 3.0;
         this.put((int)Math.round(Math.cos(a) * 7.0), 0, (int)Math.round(Math.sin(a) * 7.0), Part.LIGHT);
      }

      this.altar(0, 0);
      this.chests(10, 2, true);
   }

   private void nest() {
      for (int y = -5; y <= 10; y++) {
         this.disc(0, 0, 15.4, y, Part.AIR);
      }

      for (int r = 0; r <= 15; r++) {
         int depth = r < 11 ? -4 : -4 + (r - 10);
         this.annulus(r - 0.5, r + 0.5, depth, r < 11 ? Part.FLOOR : Part.FOUNDATION);

         for (int y = -7; y < depth; y++) {
            this.annulus(r - 0.5, r + 0.5, y, Part.FOUNDATION);
         }
      }

      this.annulus(3.0, 4.4, -3, Part.PLANT);

      for (int k = 0; k < 5; k++) {
         double a = k * Math.PI * 2.0 / 5.0;
         this.rib(a, 12.0);
      }

      this.altar(0, -3, 0);
      this.chests(9, 2, true, -3);
   }

   private void cathedral() {
      int hw = 9;
      int len = 16;
      this.fill(-hw - 1, -3, -len - 1, hw + 1, -1, len + 1, Part.FOUNDATION);
      this.fill(-hw + 1, -1, -len + 1, hw - 1, -1, len - 1, Part.FLOOR);
      this.fill(-hw + 1, 0, -len + 1, hw - 1, 13, len - 1, Part.AIR);
      this.walls(-hw, -len, hw, len, 0, 12);

      for (int z = -len + 3; z <= len - 3; z += 4) {
         for (int side = -1; side <= 1; side += 2) {
            this.fill(side * hw, 3, z, side * hw, 8, z + 1, Part.WINDOW);
            this.pillar(side * (hw - 1), z - 1, 0, 11, Part.TRIM);
         }
      }

      this.pitchedRoof(-hw, -len, hw, len, 13);
      this.fill(-2, 0, len, 2, 5, len, Part.AIR);
      this.fill(-4, -1, -len + 1, 4, 0, -len + 5, Part.FLOOR);
      this.fill(-1, -1, -len + 6, 1, -1, len - 1, Part.PATH);

      for (int i = 0; i < 3; i++) {
         int z = -len + 9 + i * 7;
         this.put(-4, 0, z, Part.FIXTURE);
         this.put(4, 0, z, Part.FIXTURE);
      }

      for (int z = -len + 4; z <= len - 4; z += 6) {
         this.put(0, 12, z, Part.HANGING_LIGHT);
      }

      this.altar(0, 1, -len + 3);
      this.chests(0, 2, true, 1, -len + 2);
   }

   private void sunkenShrine() {
      this.arena(15, 10);

      for (int y = -4; y <= -2; y++) {
         this.disc(0, 0, 16.4, y, Part.FOUNDATION);
      }

      this.annulus(2.5, 8.4, -1, Part.WATER);
      this.annulus(2.5, 8.4, -2, Part.WATER);
      this.disc(0, 0, 2.4, -1, Part.FLOOR);
      this.annulus(8.5, 15.4, -1, Part.FLOOR);

      for (int k = 0; k < 16; k++) {
         double a = k * Math.PI / 8.0 + this.random.nextDouble() * 0.2;
         int x = (int)Math.round(Math.cos(a) * 13.5);
         int z = (int)Math.round(Math.sin(a) * 13.5);
         int h = this.random.nextInt(6);
         this.pillar(x, z, 0, h, Part.WALL);
         if (h > 2) {
            this.put(x, h + 1, z, Part.PLANT);
         }
      }

      for (int k = 0; k < 10; k++) {
         double a = this.random.nextDouble() * Math.PI * 2.0;
         double r = 9.5 + this.random.nextDouble() * 4.0;
         this.put((int)Math.round(Math.cos(a) * r), 0, (int)Math.round(Math.sin(a) * r), Part.PLANT);
      }

      this.bridge(2, 3, 8);
      this.altar(0, 0);
      this.chests(12, 2, true);
   }

   private void pyramid() {
      int half = 17;

      for (int y = -1; y <= half; y++) {
         int s = half - y;
         this.fill(-s, y, -s, s, y, s, y % 3 == 2 ? Part.TRIM : Part.WALL);
      }

      this.fill(-half - 1, -3, -half - 1, half + 1, -2, half + 1, Part.FOUNDATION);
      this.fill(-11, -1, -11, 11, -1, 11, Part.FLOOR);
      this.fill(-11, 0, -11, 11, 8, 11, Part.AIR);
      this.fill(-2, 0, 11, 2, 4, half + 1, Part.AIR);
      this.fill(-2, -1, 11, 2, -1, half + 1, Part.PATH);

      for (int x = -8; x <= 8; x += 8) {
         for (int z = -8; z <= 8; z += 8) {
            if (x != 0 || z != 0) {
               this.pillar(x, z, 0, 8, Part.TRIM);
            }
         }
      }

      for (int k = 0; k < 4; k++) {
         this.put(DX[k] * 10, 1, DZ[k] * 10, Part.LIGHT);
         this.put(DX[k] * 10, 0, DZ[k] * 10, Part.TRIM);
      }

      this.altar(0, 0);
      this.chests(9, 2, true);
   }

   private void sporecap() {
      this.arena(15, 12);

      for (int y = -3; y <= -1; y++) {
         this.disc(0, 0, 16.4, y, y == -1 ? Part.FLOOR : Part.FOUNDATION);
      }

      this.ringWall(15.4, 0, 12, Part.WALL);

      for (int y = 13; y <= 15; y++) {
         this.disc(0, 0, 19.4 - (y - 13) * 3, y, Part.ROOF);
      }

      for (int k = 0; k < 4; k++) {
         this.fill(DX[k] * 15 - 1, 0, DZ[k] * 15 - 1, DX[k] * 15 + 1, 4, DZ[k] * 15 + 1, Part.AIR);
      }

      for (int k = 0; k < 12; k++) {
         double a = k * Math.PI / 6.0;
         this.put((int)Math.round(Math.cos(a) * 9.0), 12, (int)Math.round(Math.sin(a) * 9.0), Part.HANGING_LIGHT);
         this.put((int)Math.round(Math.cos(a + 0.2) * 12.0), 0, (int)Math.round(Math.sin(a + 0.2) * 12.0), Part.PLANT);
      }

      this.altar(0, 0);
      this.chests(12, 2, true);
   }

   private void observatory() {
      this.arena(15, 14);

      for (int y = -3; y <= -1; y++) {
         this.disc(0, 0, 15.4, y, y == -1 ? Part.FLOOR : Part.FOUNDATION);
      }

      this.annulus(6.5, 7.4, -1, Part.PATH);
      this.annulus(11.5, 12.4, -1, Part.PATH);

      for (int k = 0; k < 5; k++) {
         double a = k * Math.PI * 2.0 / 5.0 - (Math.PI / 2);
         int x = (int)Math.round(Math.cos(a) * 9.0);
         int z = (int)Math.round(Math.sin(a) * 9.0);
         this.put(x, 0, z, Part.TRIM);
         this.put(x, 1, z, Part.FIXTURE);
         this.put(x, 2, z, Part.TRIM);
      }

      for (int k = 0; k < 8; k++) {
         double a = k * Math.PI / 4.0;
         this.rib(a, 14.5);
      }

      this.ringWall(15.4, 0, 1, Part.WALL);

      for (int k = 0; k < 4; k++) {
         this.fill(DX[k] * 15 - 1, 0, DZ[k] * 15 - 1, DX[k] * 15 + 1, 1, DZ[k] * 15 + 1, Part.AIR);
      }

      this.altar(0, 0);
      this.chests(13, 2, true);
   }

   private void spire() {
      this.arena(15, 20);

      for (int y = -3; y <= -1; y++) {
         this.disc(0, 0, 15.4, y, y == -1 ? Part.FLOOR : Part.FOUNDATION);
      }

      for (int k = 0; k < 5; k++) {
         double a = k * Math.PI * 2.0 / 5.0 - (Math.PI / 2);
         double b = a + (Math.PI * 4.0 / 5.0);
         this.line(Math.cos(a) * 11.0, Math.sin(a) * 11.0, Math.cos(b) * 11.0, Math.sin(b) * 11.0, -1, Part.PATH);
      }

      for (int k = 0; k < 10; k++) {
         double a = k * Math.PI / 5.0;
         int x = (int)Math.round(Math.cos(a) * 13.0);
         int z = (int)Math.round(Math.sin(a) * 13.0);
         int h = k % 2 == 0 ? 10 : 6;
         this.pillar(x, z, 0, h, Part.WALL);
         this.put(x, h + 1, z, Part.LIGHT);
      }

      this.altar(0, 0);
      this.chests(10, 2, true);
   }

   private void arena(int r, int h) {
      for (int y = 0; y <= h; y++) {
         this.disc(0, 0, r + 0.4, y, Part.AIR);
      }
   }

   private void altar(int x, int z) {
      this.altar(x, 0, z);
   }

   private void altar(int x, int y, int z) {
      this.fill(x - 1, y - 1, z - 1, x + 1, y - 1, z + 1, Part.TRIM);
      this.put(x, y, z, Part.ALTAR);
   }

   private void chests(int r, int count, boolean treasure) {
      this.chests(r, count, treasure, 0);
   }

   private void chests(int r, int count, boolean treasure, int y) {
      for (int k = 0; k < count; k++) {
         double a = Math.PI * (0.75 + k * 0.5);
         int x = (int)Math.round(Math.cos(a) * r);
         int z = (int)Math.round(Math.sin(a) * r);
         this.plan.chest(x, y, z, k % 4, treasure);
      }
   }

   private void chests(int x, int count, boolean treasure, int y, int z) {
      for (int k = 0; k < count; k++) {
         this.plan.chest(x - 3 + k * 6, y, z, 0, treasure);
      }
   }

   private void gate(int x, int z, int halfWidth, int h) {
      this.fill(x - halfWidth, 0, z - 1, x + halfWidth, h, z + 1, Part.AIR);
   }

   private void bridge(int k, int from, int to) {
      for (int d = from; d <= to; d++) {
         for (int w = -1; w <= 1; w++) {
            int x = DX[k] * d + (DX[k] == 0 ? w : 0);
            int z = DZ[k] * d + (DZ[k] == 0 ? w : 0);
            this.put(x, -1, z, Part.PATH);
         }
      }
   }

   private void rib(double angle, double r) {
      for (int i = 0; i <= 12; i++) {
         double t = i / 12.0;
         double rr = r * (1.0 - 0.45 * t);
         int y = (int)Math.round(Math.sin(t * Math.PI * 0.6) * 11.0);
         this.put((int)Math.round(Math.cos(angle) * rr), y, (int)Math.round(Math.sin(angle) * rr), Part.TRIM);
      }
   }

   private void pillar(int x, int z, int y0, int y1, Part part) {
      for (int y = y0; y <= y1; y++) {
         this.put(x, y, z, part);
      }
   }

   private void walls(int x0, int z0, int x1, int z1, int y0, int y1) {
      for (int y = y0; y <= y1; y++) {
         for (int x = x0; x <= x1; x++) {
            this.put(x, y, z0, Part.WALL);
            this.put(x, y, z1, Part.WALL);
         }

         for (int z = z0; z <= z1; z++) {
            this.put(x0, y, z, Part.WALL);
            this.put(x1, y, z, Part.WALL);
         }
      }
   }

   private void pitchedRoof(int x0, int z0, int x1, int z1, int y) {
      int half = (x1 - x0) / 2 + 1;

      for (int i = 0; i <= half; i++) {
         this.fill(x0 - 1 + i, y + i, z0 - 1, x1 + 1 - i, y + i, z1 + 1, Part.ROOF);
         if (i > 0) {
            this.fill(x0 + i, y + i - 1, z0, x1 - i, y + i - 1, z1, Part.AIR);
         }
      }
   }

   private void crenels(int r, int y) {
      for (int k = 0; k < 48; k += 2) {
         double a = k * Math.PI * 2.0 / 48.0;
         this.put((int)Math.round(Math.cos(a) * r), y, (int)Math.round(Math.sin(a) * r), Part.SLAB);
      }
   }

   private void ringWall(double r, int y0, int y1, Part part) {
      for (int y = y0; y <= y1; y++) {
         this.annulus(r - 1.0, r, y, part);
      }
   }

   private void annulus(double r0, double r1, int y, Part part) {
      int n = (int)Math.ceil(r1);

      for (int x = -n; x <= n; x++) {
         for (int z = -n; z <= n; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d >= r0 && d <= r1) {
               this.put(x, y, z, part);
            }
         }
      }
   }

   private void disc(int cx, int cz, double r, int y, Part part) {
      int n = (int)Math.ceil(r);

      for (int x = -n; x <= n; x++) {
         for (int z = -n; z <= n; z++) {
            if (x * x + z * z <= r * r) {
               this.put(cx + x, y, cz + z, part);
            }
         }
      }
   }

   private void line(double x0, double z0, double x1, double z1, int y, Part part) {
      int steps = (int)Math.ceil(Math.hypot(x1 - x0, z1 - z0) * 2.0);

      for (int i = 0; i <= steps; i++) {
         double t = (double)i / steps;
         this.put((int)Math.round(x0 + (x1 - x0) * t), y, (int)Math.round(z0 + (z1 - z0) * t), part);
      }
   }

   private void fill(int x0, int y0, int z0, int x1, int y1, int z1, Part part) {
      for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
         for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
               this.put(x, y, z, part);
            }
         }
      }
   }

   private void put(int x, int y, int z, Part part) {
      this.plan.set(x, y, z, part, 0, false);
   }

   private void put(int x, int y, int z, Part part, int facing, boolean top) {
      this.plan.set(x, y, z, part, facing, top);
   }
}

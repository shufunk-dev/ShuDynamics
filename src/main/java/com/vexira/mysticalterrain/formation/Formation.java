package com.vexira.mysticalterrain.formation;

import java.util.Arrays;
import java.util.List;

public final class Formation {
   private final List<Solid> solids;
   private final List<int[]> chests;
   private final long seed;
   private final int[] bounds = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};

   Formation(List<Solid> solids, List<int[]> chests, long seed) {
      this.solids = solids;
      this.chests = chests;
      this.seed = seed;

      for (Solid solid : solids) {
         double[] b = solid.bounds();

         for (int i = 0; i < 3; i++) {
            this.bounds[i] = Math.min(this.bounds[i], (int)Math.floor(b[i]));
            this.bounds[i + 3] = Math.max(this.bounds[i + 3], (int)Math.ceil(b[i + 3]));
         }
      }

      for (int[] chest : chests) {
         for (int i = 0; i < 3; i++) {
            this.bounds[i] = Math.min(this.bounds[i], chest[i]);
            this.bounds[i + 3] = Math.max(this.bounds[i + 3], chest[i]);
         }
      }

      if (this.bounds[0] > this.bounds[3]) {
         Arrays.fill(this.bounds, 0);
      }
   }

   public int minX() {
      return this.bounds[0];
   }

   public int minY() {
      return this.bounds[1];
   }

   public int minZ() {
      return this.bounds[2];
   }

   public int maxX() {
      return this.bounds[3];
   }

   public int maxY() {
      return this.bounds[4];
   }

   public int maxZ() {
      return this.bounds[5];
   }

   public int reach() {
      return Math.max(Math.max(-this.bounds[0], this.bounds[3]), Math.max(-this.bounds[2], this.bounds[5]));
   }

   public List<int[]> chests() {
      return this.chests;
   }

   public void rasterize(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Sink sink) {
      for (Solid solid : this.solids) {
         double[] b = solid.bounds();
         int x0 = Math.max(minX, (int)Math.floor(b[0]));
         int x1 = Math.min(maxX, (int)Math.ceil(b[3]));
         int y0 = Math.max(minY, (int)Math.floor(b[1]));
         int y1 = Math.min(maxY, (int)Math.ceil(b[4]));
         int z0 = Math.max(minZ, (int)Math.floor(b[2]));
         int z1 = Math.min(maxZ, (int)Math.ceil(b[5]));
         int axis = solid.axis();
         boolean force = solid.force();

         for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
               for (int z = z0; z <= z1; z++) {
                  Material material = solid.at(x, y, z, this.seed);
                  if (material != null) {
                     sink.place(x, y, z, material, axis, force);
                  }
               }
            }
         }
      }
   }
}

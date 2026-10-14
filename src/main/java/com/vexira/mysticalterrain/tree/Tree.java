package com.vexira.mysticalterrain.tree;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public final class Tree {
   private final List<Shape> wood;
   private final List<Shape> veins;
   private final List<Shape> foliage;
   private final long seed;
   private final double foot;
   private final int[] bounds = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};

   Tree(List<Shape> wood, List<Shape> veins, List<Shape> foliage, long seed, double foot) {
      this.wood = wood;
      this.veins = veins;
      this.foliage = foliage;
      this.seed = seed;
      this.foot = foot;

      for (List<Shape> shapes : List.of(wood, veins, foliage)) {
         for (Shape shape : shapes) {
            double[] b = shape.bounds();

            for (int i = 0; i < 3; i++) {
               this.bounds[i] = Math.min(this.bounds[i], (int)Math.floor(b[i]));
               this.bounds[i + 3] = Math.max(this.bounds[i + 3], (int)Math.ceil(b[i + 3]));
            }
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

   public double foot() {
      return this.foot;
   }

   public int reach() {
      return Math.max(Math.max(-this.bounds[0], this.bounds[3]), Math.max(-this.bounds[2], this.bounds[5]));
   }

   public void rasterize(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Sink sink) {
      minX = Math.max(minX, this.bounds[0]);
      minY = Math.max(minY, this.bounds[1]);
      minZ = Math.max(minZ, this.bounds[2]);
      maxX = Math.min(maxX, this.bounds[3]);
      maxY = Math.min(maxY, this.bounds[4]);
      maxZ = Math.min(maxZ, this.bounds[5]);
      if (minX <= maxX && minY <= maxY && minZ <= maxZ) {
         this.draw(this.wood, minX, minY, minZ, maxX, maxY, maxZ, sink, null);
         this.draw(this.veins, minX, minY, minZ, maxX, maxY, maxZ, sink, null);
         BitSet done = new BitSet((maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1));
         this.draw(this.foliage, minX, minY, minZ, maxX, maxY, maxZ, sink, done);
      }
   }

   private void draw(List<Shape> shapes, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Sink sink, BitSet done) {
      int sizeY = maxY - minY + 1;
      int sizeZ = maxZ - minZ + 1;
      double[] span = new double[2];

      for (Shape shape : shapes) {
         double[] b = shape.bounds();
         int x0 = Math.max(minX, (int)Math.floor(b[0]));
         int x1 = Math.min(maxX, (int)Math.ceil(b[3]));
         int y0 = Math.max(minY, (int)Math.floor(b[1]));
         int y1 = Math.min(maxY, (int)Math.ceil(b[4]));
         int z0 = Math.max(minZ, (int)Math.floor(b[2]));
         int z1 = Math.min(maxZ, (int)Math.ceil(b[5]));
         int axis = shape.axis();

         for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
               if (shape.row(x, y, span)) {
                  int from = Math.max(z0, (int)Math.ceil(span[0]));
                  int to = Math.min(z1, (int)Math.floor(span[1]));

                  for (int z = from; z <= to; z++) {
                     int index = done == null ? 0 : ((x - minX) * sizeY + (y - minY)) * sizeZ + (z - minZ);
                     if ((done == null || !done.get(index)) && shape.contains(x, y, z, this.seed)) {
                        if (done != null) {
                           done.set(index);
                        }

                        sink.place(x, y, z, shape.part(), axis);
                     }
                  }
               }
            }
         }
      }
   }
}

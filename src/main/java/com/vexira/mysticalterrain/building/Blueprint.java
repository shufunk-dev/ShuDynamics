package com.vexira.mysticalterrain.building;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Blueprint {
   public static final int SOUTH = 0;
   public static final int WEST = 1;
   public static final int NORTH = 2;
   public static final int EAST = 3;
   private final Map<Long, Integer> cells = new HashMap<>();
   private final List<int[]> chests = new ArrayList<>();
   private final List<int[]> spawners = new ArrayList<>();
   private final int[] bounds = new int[]{0, 0, 0, 0, 0, 0};

   private static long key(int x, int y, int z) {
      return (long)(x + 1024) << 22 | (long)(y + 1024) << 11 | z + 1024;
   }

   void set(int x, int y, int z, Part part, int facing, boolean top) {
      this.cells.put(key(x, y, z), part.ordinal() | facing << 5 | (top ? 128 : 0));
   }

   void remove(int x, int y, int z) {
      this.cells.remove(key(x, y, z));
   }

   public Part get(int x, int y, int z) {
      Integer v = this.cells.get(key(x, y, z));
      return v == null ? null : Part.of(v & 31);
   }

   void chest(int x, int y, int z, int facing, boolean treasure) {
      this.set(x, y, z, Part.AIR, 0, false);
      this.chests.add(new int[]{x, y, z, facing, treasure ? 1 : 0});
   }

   void spawner(int x, int y, int z) {
      this.set(x, y, z, Part.AIR, 0, false);
      this.spawners.add(new int[]{x, y, z});
   }

   public List<int[]> chests() {
      return this.chests;
   }

   public List<int[]> spawners() {
      return this.spawners;
   }

   Blueprint finish() {
      int[] b = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};

      for (long k : this.cells.keySet()) {
         int x = (int)(k >> 22) - 1024;
         int y = (int)(k >> 11 & 2047L) - 1024;
         int z = (int)(k & 2047L) - 1024;
         b[0] = Math.min(b[0], x);
         b[1] = Math.min(b[1], y);
         b[2] = Math.min(b[2], z);
         b[3] = Math.max(b[3], x);
         b[4] = Math.max(b[4], y);
         b[5] = Math.max(b[5], z);
      }

      if (b[0] <= b[3]) {
         System.arraycopy(b, 0, this.bounds, 0, 6);
      }

      return this;
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

   public void forEach(Blueprint.Visitor visitor) {
      this.cells
         .forEach(
            (k, v) -> visitor.cell((int)(k >> 22) - 1024, (int)(k >> 11 & 2047L) - 1024, (int)(k & 2047L) - 1024, Part.of(v & 31), v >> 5 & 3, (v & 128) != 0)
         );
   }

   @FunctionalInterface
   public interface Visitor {
      void cell(int var1, int var2, int var3, Part var4, int var5, boolean var6);
   }
}

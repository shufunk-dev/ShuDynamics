package com.vexira.mysticalterrain.building;

public enum Part {
   FOUNDATION,
   FLOOR,
   WALL,
   TRIM,
   ROOF,
   ROOF_STAIRS,
   ROOF_SLAB,
   STAIRS,
   SLAB,
   WINDOW,
   BARS,
   LADDER,
   PATH,
   LIGHT,
   HANGING_LIGHT,
   SHELF,
   STORAGE,
   WORKSTATION,
   CARPET,
   PLANT,
   COBWEB,
   WATER,
   AIR,
   LAVA,
   ALTAR,
   FIXTURE;

   private static final Part[] VALUES = values();

   static Part of(int ordinal) {
      return VALUES[ordinal];
   }

   public boolean solid() {
      return switch (this) {
         case FOUNDATION, FLOOR, WALL, TRIM, ROOF, WINDOW, BARS, PATH, ALTAR -> true;
         default -> false;
      };
   }
}

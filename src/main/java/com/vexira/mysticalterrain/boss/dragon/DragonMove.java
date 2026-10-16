package com.vexira.mysticalterrain.boss.dragon;

import org.jspecify.annotations.Nullable;

public enum DragonMove {
   FIREBALL("fireball", 9, DragonMove.Where.VANILLA, 20, -1, -1, 0, off(), 4),
   STRAFE_BREATH("strafe_breath", 10, DragonMove.Where.AIR, 60, 0, 16, 10, cd(30, 22, 16, 16), 16, 24, 32, 40, 48),
   RAKE_DIVE("rake_dive", 11, DragonMove.Where.AIR, 40, 4, 20, 12, cd(16, 12, 9, 9), 20),
   ROAR("roar", 6, DragonMove.Where.AIR, 48, 0, 12, 4, cd(20, 15, 12, 12), 12),
   TALON_RAPTURE("talon_rapture", 12, DragonMove.Where.AIR, 80, 2, 20, 8, cd(-1, 35, 28, 22), 20, 64),
   WING_WARD("wing_ward", 13, DragonMove.Where.HOVER, 50, 0, 8, 3, cd(30, 30, 25, 20), 8),
   VOID_BEAM("void_beam", 14, DragonMove.Where.HOVER, 70, 0, 30, 8, cd(-1, 24, 18, 14), 30, 38, 46, 54, 62),
   GRAVITY_WELL("gravity_well", 15, DragonMove.Where.HOVER, 80, 10, 32, 2, cd(-1, 30, 24, 20), 32, 44, 56, 68),
   METEOR_RAIN("meteor_rain", 16, DragonMove.Where.HOVER, 100, 0, 30, 6, cd(-1, 45, 36, 30), 30, 40, 50, 60, 70, 80),
   CRYSTAL_RECALL("crystal_recall", 3, DragonMove.Where.HOVER, 140, 0, 140, 5, cd(-1, 60, 50, -1)),
   SHATTERDIVE("shatterdive", 17, DragonMove.Where.HOVER, 50, 0, 50, 6, cd(-1, -1, 60, 40), 48),
   DIVE("dive", 8, DragonMove.Where.CHAIN, 80, 0, 80, 0, off()),
   SLAM("slam", 5, DragonMove.Where.CHAIN, 40, -1, -1, 0, off(), 2),
   OBLIVION_NOVA("oblivion_nova", 18, DragonMove.Where.HOVER, 120, 10, 96, 8, cd(-1, -1, -1, 50), 96),
   BREATH("breath", 0, DragonMove.Where.VANILLA, 200, 0, 10, 0, off(), 10),
   PERCH_ROAR("perch_roar", 0, DragonMove.Where.VANILLA, 40, 0, 15, 0, off(), 15),
   BREATH_SWEEP("breath_sweep", 2, DragonMove.Where.PERCH, 60, 0, 16, 10, cd(24, 18, 14, 12), 16, 24, 32, 40, 48),
   TAIL_SWEEP("tail_sweep", 19, DragonMove.Where.PERCH, 44, 0, 21, 2, cd(14, 11, 9, 8), 21),
   RENDING_BITE("rending_bite", 20, DragonMove.Where.PERCH, 32, 2, 21, 14, cd(8, 6, 5, 4), 10, 21),
   VOID_FISSURE("void_fissure", 21, DragonMove.Where.PERCH, 40, 14, 28, 8, cd(-1, 16, 12, 10), 14, 28),
   WING_STORM("wing_storm", 22, DragonMove.Where.PERCH, 52, 0, 18, 7, cd(-1, 22, 16, 14), 18, 30, 42),
   POUNCE("pounce", 23, DragonMove.Where.PERCH, 44, 6, 22, 8, cd(-1, -1, 20, 15), 22);

   private static final DragonMove[] ALL = values();
   public final String anim;
   public final int state;
   public final DragonMove.Where where;
   public final int length;
   public final int teleFrom;
   public final int teleTo;
   private final int weight;
   private final int[] cooldown;
   private final int[] hits;

   DragonMove(String anim, int state, DragonMove.Where where, int length, int teleFrom, int teleTo, int weight, int[] cooldown, int... hits) {
      this.anim = anim;
      this.state = state;
      this.where = where;
      this.length = length;
      this.teleFrom = teleFrom;
      this.teleTo = teleTo;
      this.weight = weight;
      this.cooldown = cooldown;
      this.hits = hits;
   }

   private static int[] cd(int one, int two, int three, int four) {
      return new int[]{one, two, three, four};
   }

   private static int[] off() {
      return new int[]{-1, -1, -1, -1};
   }

   public boolean available(int phase) {
      return this.cooldown[Math.max(0, Math.min(3, phase))] >= 0;
   }

   public int cooldownTicks(int phase) {
      int s = this.cooldown[Math.max(0, Math.min(3, phase))];
      return s < 0 ? -1 : s * 20;
   }

   public int weight(int phase) {
      if (this == METEOR_RAIN) {
         return phase >= 3 ? 10 : (phase == 2 ? 8 : 6);
      } else {
         return this.weight;
      }
   }

   public boolean hitAt(int tick) {
      for (int h : this.hits) {
         if (h == tick) {
            return true;
         }
      }

      return false;
   }

   public boolean telegraphing(int tick) {
      return this.teleFrom >= 0 && tick >= this.teleFrom && tick < this.teleTo;
   }

   public boolean superArmor() {
      return this == METEOR_RAIN || this == SHATTERDIVE || this == DIVE || this == SLAM || this == OBLIVION_NOVA;
   }

   public boolean pickable() {
      return this.where == DragonMove.Where.AIR || this.where == DragonMove.Where.HOVER || this.where == DragonMove.Where.PERCH;
   }

   public static @Nullable DragonMove byAnim(String anim) {
      for (DragonMove m : ALL) {
         if (m.anim.equals(anim)) {
            return m;
         }
      }

      return null;
   }

   public static @Nullable DragonMove byState(int state) {
      if (state == 0) {
         return null;
      }

      for (DragonMove m : ALL) {
         if (m.state == state) {
            return m;
         }
      }

      return null;
   }

   public enum Where {
      AIR,
      HOVER,
      PERCH,
      CHAIN,
      VANILLA;
   }
}

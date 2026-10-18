package com.vexira.mysticalterrain.gear;

public final class Active {
   private final int cooldown;
   private final Active.Cast cast;
   private int cost;
   private float scale = 1.5F;

   private Active(int cooldown, Active.Cast cast) {
      this.cooldown = cooldown;
      this.cast = cast;
   }

   public static Active of(int cooldown, Active.Cast cast) {
      return new Active(cooldown, cast);
   }

   public Active empowered(int cost, float scale) {
      this.cost = Math.max(0, Math.min(100, cost));
      this.scale = scale;
      return this;
   }

   public Active empowered(int cost) {
      return this.empowered(cost, 1.5F);
   }

   public int cooldown() {
      return this.cooldown;
   }

   public Active.Cast cast() {
      return this.cast;
   }

   public int cost() {
      return this.cost;
   }

   public float empowerScale() {
      return this.scale;
   }

   @FunctionalInterface
   public interface Cast {
      boolean fire(GearUse var1);
   }
}

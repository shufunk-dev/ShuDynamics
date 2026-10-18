package com.vexira.mysticalterrain.tree;

public record V3(double x, double y, double z) {
   public static final V3 ZERO = new V3(0.0, 0.0, 0.0);
   public static final V3 UP = new V3(0.0, 1.0, 0.0);

   public static V3 of(double azimuth, double elevation) {
      double h = StrictMath.cos(elevation);
      return new V3(StrictMath.cos(azimuth) * h, StrictMath.sin(elevation), StrictMath.sin(azimuth) * h);
   }

   public V3 add(V3 o) {
      return new V3(this.x + o.x, this.y + o.y, this.z + o.z);
   }

   public V3 add(double dx, double dy, double dz) {
      return new V3(this.x + dx, this.y + dy, this.z + dz);
   }

   public V3 sub(V3 o) {
      return new V3(this.x - o.x, this.y - o.y, this.z - o.z);
   }

   public V3 mul(double s) {
      return new V3(this.x * s, this.y * s, this.z * s);
   }

   public double dot(V3 o) {
      return this.x * o.x + this.y * o.y + this.z * o.z;
   }

   public V3 cross(V3 o) {
      return new V3(this.y * o.z - this.z * o.y, this.z * o.x - this.x * o.z, this.x * o.y - this.y * o.x);
   }

   public double length() {
      return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
   }

   public double horizontal() {
      return Math.sqrt(this.x * this.x + this.z * this.z);
   }

   public double azimuth() {
      return StrictMath.atan2(this.z, this.x);
   }

   public V3 normalize() {
      double l = this.length();
      return l < 1.0E-9 ? UP : this.mul(1.0 / l);
   }

   public V3 lerp(V3 o, double t) {
      return new V3(this.x + (o.x - this.x) * t, this.y + (o.y - this.y) * t, this.z + (o.z - this.z) * t);
   }
}

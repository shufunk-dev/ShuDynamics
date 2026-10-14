package com.vexira.mysticalterrain.tree;

sealed interface Shape permits Shape.Tube, Shape.Blob, Shape.Skirt, Shape.Cap, Shape.Canopy {
   Part part();

   double[] bounds();

   boolean contains(int var1, int var2, int var3, long var4);

   Shape scaled(double var1, double var3);

   default int axis() {
      return 1;
   }

   default boolean row(int x, int y, double[] span) {
      double[] b = this.bounds();
      span[0] = b[2];
      span[1] = b[5];
      return true;
   }

   private static V3 scale(V3 v, double h, double vert) {
      return new V3(v.x() * h, v.y() * vert, v.z() * h);
   }

   record Blob(V3 c, double rx, double ry, double rz, double lump, double holes, double floor, Part part) implements Shape {
      @Override
      public double[] bounds() {
         double s = 1.0 + this.lump;
         return new double[]{
            this.c.x() - this.rx * s - 1.0,
            this.c.y() - this.ry * Math.min(s, this.floor) - 1.0,
            this.c.z() - this.rz * s - 1.0,
            this.c.x() + this.rx * s + 1.0,
            this.c.y() + this.ry * s + 1.0,
            this.c.z() + this.rz * s + 1.0
         };
      }

      @Override
      public boolean contains(int x, int y, int z, long seed) {
         if (y - this.c.y() < -this.ry * this.floor) {
            return false;
         }

         double dx = (x - this.c.x()) / this.rx;
         double dy = (y - this.c.y()) / this.ry;
         double dz = (z - this.c.z()) / this.rz;
         double d2 = dx * dx + dy * dy + dz * dz;
         if (d2 > (1.0 + Math.abs(this.lump)) * (1.0 + Math.abs(this.lump))) {
            return false;
         }

         double limit = 1.0 + this.lump * Noise.smooth(seed + 17L, x / 3.5, y / 3.0, z / 3.5);
         return d2 > limit * limit ? false : this.holes <= 0.0 || d2 < 0.6 * limit * limit || Noise.unit(seed + 31L, x, y, z) >= this.holes;
      }

      @Override
      public boolean row(int x, int y, double[] span) {
         if (y - this.c.y() < -this.ry * this.floor) {
            return false;
         }

         double l = 1.0 + Math.abs(this.lump);
         double dx = (x - this.c.x()) / this.rx;
         double dy = (y - this.c.y()) / this.ry;
         double rest = l * l - dx * dx - dy * dy;
         if (rest < 0.0) {
            return false;
         }

         double w = Math.abs(this.rz) * Math.sqrt(rest) + 1.0E-6;
         span[0] = this.c.z() - w;
         span[1] = this.c.z() + w;
         return true;
      }

      @Override
      public Shape scaled(double horizontal, double vertical) {
         return new Shape.Blob(
            Shape.scale(this.c, horizontal, vertical),
            this.rx * horizontal,
            this.ry * vertical,
            this.rz * horizontal,
            this.lump,
            this.holes,
            this.floor,
            this.part
         );
      }
   }

   final class Canopy implements Shape {
      private final double[][] lobes;
      private final double thick;
      private final double drip;
      private final double lump;
      private final double holes;
      private final long seed;
      private final int minX;
      private final int minZ;
      private final int sizeZ;
      private final float[] top;
      private final float[] bottom;
      private final float[] low;
      private final double[] bounds;

      Canopy(double[][] lobes, double thick, double drip, double lump, double holes, long seed) {
         this.lobes = lobes;
         this.thick = thick;
         this.drip = drip;
         this.lump = lump;
         this.holes = holes;
         this.seed = seed;
         double x0 = Double.MAX_VALUE;
         double x1 = -Double.MAX_VALUE;
         double z0 = Double.MAX_VALUE;
         double z1 = -Double.MAX_VALUE;

         for (double[] l : lobes) {
            x0 = Math.min(x0, l[0] - l[3]);
            x1 = Math.max(x1, l[0] + l[3]);
            z0 = Math.min(z0, l[2] - l[3]);
            z1 = Math.max(z1, l[2] + l[3]);
         }

         this.minX = (int)Math.floor(x0);
         this.minZ = (int)Math.floor(z0);
         int sizeX = (int)Math.ceil(x1) - this.minX + 1;
         this.sizeZ = (int)Math.ceil(z1) - this.minZ + 1;
         this.top = new float[sizeX * this.sizeZ];
         this.bottom = new float[this.top.length];
         this.low = new float[this.top.length];
         double y0 = Double.MAX_VALUE;
         double y1 = -Double.MAX_VALUE;

         for (int i = 0; i < sizeX; i++) {
            for (int k = 0; k < this.sizeZ; k++) {
               int index = i * this.sizeZ + k;
               if (!this.column(this.minX + i, this.minZ + k, index)) {
                  this.top[index] = Float.NaN;
               } else {
                  y0 = Math.min(y0, this.low[index]);
                  y1 = Math.max(y1, this.top[index]);
               }
            }
         }

         this.bounds = y0 > y1
            ? new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
            : new double[]{this.minX, Math.floor(y0), this.minZ, this.minX + sizeX - 1, Math.ceil(y1), this.minZ + this.sizeZ - 1};
      }

      private boolean column(int x, int z, int index) {
         double best = -Double.MAX_VALUE;
         double fmax = 0.0;
         double height = 0.0;

         for (double[] l : this.lobes) {
            double dx = x - l[0];
            double dz = z - l[2];
            double f = 1.0 - (dx * dx + dz * dz) / (l[3] * l[3]);
            if (f > 0.0) {
               double y = l[1] + l[4] * Math.sqrt(f);
               if (y > best) {
                  best = y;
                  height = l[4];
               }

               fmax = Math.max(fmax, f);
            }
         }

         if (fmax <= 0.02 + 0.025 * (Noise.smooth(this.seed + 2L, x / 5.0, 0.0, z / 5.0) + 1.0)) {
            return false;
         }

         double edge = Math.sqrt(fmax);
         best += height
            * this.lump
            * 0.35
            * edge
            * (0.65 * Noise.smooth(this.seed, x / 7.0, 0.0, z / 7.0) + 0.35 * Noise.smooth(this.seed + 1L, x / 3.0, 0.0, z / 3.0));
         double under = best - (this.thick * (0.2 + 0.8 * edge) + 1.0);
         double fine = Noise.unit(this.seed + 41L, x, 0, z);
         double coarse = Noise.unit(this.seed + 43L, Math.floorDiv(x, 2), 0, Math.floorDiv(z, 2));
         double hang = fmax < 0.3 ? this.drip * Math.max(Math.pow(fine, 5.0), 0.75 * Math.pow(coarse, 3.0)) : this.drip * 0.3 * Math.pow(fine, 6.0);
         this.top[index] = (float)best;
         this.bottom[index] = (float)under;
         this.low[index] = (float)(under - Math.floor(hang));
         return true;
      }

      boolean holds(double x, double y, double z) {
         int i = (int)Math.floor(x) - this.minX;
         int k = (int)Math.floor(z) - this.minZ;
         if (i >= 0 && k >= 0 && k < this.sizeZ && i * this.sizeZ + k < this.top.length) {
            int index = i * this.sizeZ + k;
            return !Float.isNaN(this.top[index]) && y <= this.top[index] && y >= this.bottom[index];
         } else {
            return false;
         }
      }

      @Override
      public Part part() {
         return Part.LEAVES;
      }

      @Override
      public double[] bounds() {
         return this.bounds;
      }

      @Override
      public boolean contains(int x, int y, int z, long ignored) {
         int i = x - this.minX;
         int k = z - this.minZ;
         if (i >= 0 && k >= 0 && k < this.sizeZ && i * this.sizeZ + k < this.top.length) {
            int index = i * this.sizeZ + k;
            float t = this.top[index];
            if (Float.isNaN(t) || y > t || y < this.low[index]) {
               return false;
            } else if (!(y < this.bottom[index]) && !(this.holes <= 0.0)) {
               boolean surface = t - y < 1.5 || y - this.bottom[index] < 1.5;
               return !surface || Noise.unit(this.seed + 31L, x, y, z) >= this.holes;
            } else {
               return true;
            }
         } else {
            return false;
         }
      }

      @Override
      public Shape scaled(double horizontal, double vertical) {
         double[][] scaled = new double[this.lobes.length][];

         for (int i = 0; i < this.lobes.length; i++) {
            double[] l = this.lobes[i];
            scaled[i] = new double[]{l[0] * horizontal, l[1] * vertical, l[2] * horizontal, l[3] * horizontal, l[4] * vertical};
         }

         return new Shape.Canopy(scaled, this.thick * vertical, this.drip * vertical, this.lump, this.holes, this.seed);
      }
   }

   record Cap(V3 c, double r, double height, double thickness, double rim, double holes) implements Shape {
      @Override
      public Part part() {
         return Part.LEAVES;
      }

      @Override
      public double[] bounds() {
         double s = this.r * 1.06 + 1.0;
         return new double[]{
            this.c.x() - s, this.c.y() - this.rim - this.thickness - 1.0, this.c.z() - s, this.c.x() + s, this.c.y() + this.height + 1.0, this.c.z() + s
         };
      }

      @Override
      public boolean contains(int x, int y, int z, long seed) {
         double hx = x - this.c.x();
         double hz = z - this.c.z();
         double u = Math.sqrt(hx * hx + hz * hz) / this.r;
         if (u > 1.06) {
            return false;
         }

         u += 0.06 * Noise.smooth(seed + 3L, x / 3.0, this.c.y(), z / 3.0);
         if (u > 1.0) {
            return false;
         }

         double top = this.c.y() + this.height * Math.sqrt(Math.max(0.0, 1.0 - u * u));
         double bottom = top - this.thickness;
         if (u > 0.8) {
            bottom -= this.rim * (u - 0.8) / 0.2;
         }

         return !(y > top + 0.5) && !(y < bottom - 0.5) ? this.holes <= 0.0 || Noise.unit(seed + 7L, x, y, z) >= this.holes : false;
      }

      @Override
      public boolean row(int x, int y, double[] span) {
         double hx = x - this.c.x();
         double reach = Math.abs(this.r) * 1.06;
         double rest = reach * reach - hx * hx;
         if (rest < 0.0) {
            return false;
         }

         double w = Math.sqrt(rest) + 1.0E-6;
         span[0] = this.c.z() - w;
         span[1] = this.c.z() + w;
         return true;
      }

      @Override
      public Shape scaled(double horizontal, double vertical) {
         return new Shape.Cap(
            Shape.scale(this.c, horizontal, vertical), this.r * horizontal, this.height * vertical, this.thickness * vertical, this.rim * vertical, this.holes
         );
      }
   }

   record Skirt(V3 c, double r, double thickness, double droop, double holes) implements Shape {
      @Override
      public Part part() {
         return Part.LEAVES;
      }

      @Override
      public double[] bounds() {
         double s = this.r * 1.25 + 1.0;
         return new double[]{
            this.c.x() - s,
            this.c.y() - this.droop * 1.5 - this.thickness - 1.0,
            this.c.z() - s,
            this.c.x() + s,
            this.c.y() + this.thickness + 1.0,
            this.c.z() + s
         };
      }

      @Override
      public boolean contains(int x, int y, int z, long seed) {
         double hx = x - this.c.x();
         double hz = z - this.c.z();
         double rho = Math.sqrt(hx * hx + hz * hz);
         if (rho > Math.abs(this.r) * 1.22) {
            return false;
         }

         double limit = this.r * (1.0 + 0.22 * Noise.smooth(seed + 5L, x / 2.5, this.c.y() / 3.0, z / 2.5));
         if (rho > limit) {
            return false;
         }

         double u = rho / Math.max(1.0, this.r);
         double mid = this.c.y() - this.droop * u * u;
         return Math.abs(y - mid) > this.thickness * (1.0 - 0.45 * u) / 2.0 + 0.3
            ? false
            : this.holes <= 0.0 || u < 0.7 || Noise.unit(seed + 13L, x, y, z) >= this.holes;
      }

      @Override
      public boolean row(int x, int y, double[] span) {
         double hx = x - this.c.x();
         double reach = Math.abs(this.r) * 1.22;
         double rest = reach * reach - hx * hx;
         if (rest < 0.0) {
            return false;
         }

         double w = Math.sqrt(rest) + 1.0E-6;
         span[0] = this.c.z() - w;
         span[1] = this.c.z() + w;
         return true;
      }

      @Override
      public Shape scaled(double horizontal, double vertical) {
         return new Shape.Skirt(Shape.scale(this.c, horizontal, vertical), this.r * horizontal, this.thickness * vertical, this.droop * vertical, this.holes);
      }
   }

   record Tube(V3 a, V3 b, double ra, double rb, Part part, double bark) implements Shape {
      @Override
      public double[] bounds() {
         double r = Math.max(this.ra, this.rb) * (1.0 + this.bark) + 1.0;
         return new double[]{
            Math.min(this.a.x(), this.b.x()) - r,
            Math.min(this.a.y(), this.b.y()) - r,
            Math.min(this.a.z(), this.b.z()) - r,
            Math.max(this.a.x(), this.b.x()) + r,
            Math.max(this.a.y(), this.b.y()) + r,
            Math.max(this.a.z(), this.b.z()) + r
         };
      }

      @Override
      public boolean contains(int x, int y, int z, long seed) {
         double abx = this.b.x() - this.a.x();
         double aby = this.b.y() - this.a.y();
         double abz = this.b.z() - this.a.z();
         double len2 = abx * abx + aby * aby + abz * abz;
         double t = len2 < 1.0E-9 ? 0.0 : ((x - this.a.x()) * abx + (y - this.a.y()) * aby + (z - this.a.z()) * abz) / len2;
         t = Math.max(0.0, Math.min(1.0, t));
         double dx = x - this.a.x() - abx * t;
         double dy = y - this.a.y() - aby * t;
         double dz = z - this.a.z() - abz * t;
         double r = this.ra + (this.rb - this.ra) * t;
         double d2 = dx * dx + dy * dy + dz * dz;
         if (this.bark > 0.0) {
            double outer = r * (1.0 + this.bark);
            double inner = this.bark < 1.0 ? r * (1.0 - this.bark) : 0.0;
            if (d2 > outer * outer + 0.15) {
               return false;
            }

            if (d2 <= inner * inner + 0.15) {
               return true;
            }

            r *= 1.0 + this.bark * Noise.smooth(seed, x / 2.5, y / 5.0, z / 2.5);
         }

         return d2 <= r * r + 0.15;
      }

      @Override
      public boolean row(int x, int y, double[] span) {
         double reach = Math.max(this.ra, this.rb) * (1.0 + Math.max(this.bark, 0.0));
         double reach2 = reach * reach + 0.15 + 1.0E-6;
         double abx = this.b.x() - this.a.x();
         double aby = this.b.y() - this.a.y();
         double abz = this.b.z() - this.a.z();
         double px = x - this.a.x();
         double py = y - this.a.y();
         double qa = abx * abx + aby * aby;
         double qb = -2.0 * (px * abx + py * aby);
         double qc = px * px + py * py - reach2;
         double t0 = 0.0;
         double t1 = 1.0;
         if (qa < 1.0E-9) {
            if (qc > 0.0) {
               return false;
            }
         } else {
            double disc = qb * qb - 4.0 * qa * qc;
            if (disc < 0.0) {
               return false;
            }

            double root = Math.sqrt(disc);
            t0 = Math.max(0.0, (-qb - root) / (2.0 * qa) - 1.0E-6);
            t1 = Math.min(1.0, (-qb + root) / (2.0 * qa) + 1.0E-6);
            if (t0 > t1) {
               return false;
            }
         }

         double za = this.a.z() + abz * t0;
         double zb = this.a.z() + abz * t1;
         double r = Math.sqrt(reach2);
         span[0] = Math.min(za, zb) - r;
         span[1] = Math.max(za, zb) + r;
         return true;
      }

      @Override
      public int axis() {
         double x = Math.abs(this.b.x() - this.a.x());
         double y = Math.abs(this.b.y() - this.a.y());
         double z = Math.abs(this.b.z() - this.a.z());
         return y >= x && y >= z ? 1 : (x >= z ? 0 : 2);
      }

      @Override
      public Shape scaled(double horizontal, double vertical) {
         double r = Math.sqrt(Math.min(1.0, Math.min(horizontal, vertical)));
         return new Shape.Tube(
            Shape.scale(this.a, horizontal, vertical), Shape.scale(this.b, horizontal, vertical), this.ra * r, this.rb * r, this.part, this.bark
         );
      }
   }
}

package com.vexira.mysticalterrain.formation;

import com.vexira.mysticalterrain.tree.Noise;
import com.vexira.mysticalterrain.tree.V3;

sealed interface Solid permits Solid.Prism, Solid.Tube, Solid.Ellipsoid, Solid.Ring, Solid.Box {
   double TAU = Math.PI * 2;
   V3 X = new V3(1.0, 0.0, 0.0);

   double[] bounds();

   Material at(int var1, int var2, int var3, long var4);

   boolean force();

   default int axis() {
      return 1;
   }

   private static int dominant(V3 d) {
      double x = Math.abs(d.x());
      double y = Math.abs(d.y());
      double z = Math.abs(d.z());
      return y >= x && y >= z ? 1 : (x >= z ? 0 : 2);
   }

   private static double[] span(V3 a, V3 b, double pad) {
      return new double[]{
         Math.min(a.x(), b.x()) - pad,
         Math.min(a.y(), b.y()) - pad,
         Math.min(a.z(), b.z()) - pad,
         Math.max(a.x(), b.x()) + pad,
         Math.max(a.y(), b.y()) + pad,
         Math.max(a.z(), b.z()) + pad
      };
   }

   record Box(int x0, int y0, int z0, int x1, int y1, int z1, double decay, Material material, boolean force) implements Solid {
      @Override
      public double[] bounds() {
         return new double[]{
            Math.min(this.x0, this.x1),
            Math.min(this.y0, this.y1),
            Math.min(this.z0, this.z1),
            Math.max(this.x0, this.x1),
            Math.max(this.y0, this.y1),
            Math.max(this.z0, this.z1)
         };
      }

      @Override
      public Material at(int x, int y, int z, long seed) {
         if (x >= Math.min(this.x0, this.x1)
            && x <= Math.max(this.x0, this.x1)
            && y >= Math.min(this.y0, this.y1)
            && y <= Math.max(this.y0, this.y1)
            && z >= Math.min(this.z0, this.z1)
            && z <= Math.max(this.z0, this.z1)) {
            return this.decay > 0.0 && Noise.unit(seed + 31L, x, y, z) < this.decay ? null : this.material;
         } else {
            return null;
         }
      }
   }

   record Ellipsoid(
      V3 c, double rx, double ry, double rz, double lump, double shell, Material material, Material inside, double maxY, Material cap, boolean force
   ) implements Solid {
      @Override
      public double[] bounds() {
         double s = 1.0 + this.lump;
         return new double[]{
            this.c.x() - this.rx * s - 1.0,
            this.c.y() - this.ry * s - 1.0,
            this.c.z() - this.rz * s - 1.0,
            this.c.x() + this.rx * s + 1.0,
            Math.min(this.c.y() + this.ry * s + 1.0, this.maxY),
            this.c.z() + this.rz * s + 1.0
         };
      }

      @Override
      public Material at(int x, int y, int z, long seed) {
         if (y > this.maxY) {
            return null;
         } else {
            double dx = (x - this.c.x()) / this.rx;
            double dy = (y - this.c.y()) / this.ry;
            double dz = (z - this.c.z()) / this.rz;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double limit = 1.0 + this.lump * Noise.smooth(seed + 17L, x / 3.5, y / 3.0, z / 3.5);
            if (d > limit) {
               return null;
            } else if (this.cap != null && y > this.maxY - 1.0) {
               return this.cap;
            } else if (this.shell <= 0.0) {
               return this.material;
            } else {
               return (limit - d) * Math.min(this.rx, Math.min(this.ry, this.rz)) < this.shell ? this.material : this.inside;
            }
         }
      }
   }

   final class Prism implements Solid {
      private final V3 a;
      private final V3 dir;
      private final V3 u;
      private final V3 v;
      private final double length;
      private final double r;
      private final int sides;
      private final double twist;
      private final double tip;
      private final double shell;
      private final Material core;
      private final Material body;
      private final Material skin;
      private final Material point;
      private final boolean force;

      Prism(V3 a, V3 b, double r, int sides, double twist, double tip, double shell, Material core, Material body, Material skin, Material point, boolean force) {
         this.a = a;
         this.dir = b.sub(a).normalize();
         this.u = this.dir.cross(Math.abs(this.dir.y()) > 0.9 ? X : V3.UP).normalize();
         this.v = this.dir.cross(this.u);
         this.length = b.sub(a).length();
         this.r = r;
         this.sides = sides;
         this.twist = twist;
         this.tip = tip;
         this.shell = shell;
         this.core = core;
         this.body = body;
         this.skin = skin;
         this.point = point;
         this.force = force;
      }

      @Override
      public double[] bounds() {
         return Solid.span(this.a, this.a.add(this.dir.mul(this.length + this.tip)), this.r + 1.0);
      }

      @Override
      public Material at(int x, int y, int z, long seed) {
         V3 p = new V3(x - this.a.x(), y - this.a.y(), z - this.a.z());
         double t = p.dot(this.dir);
         if (!(t < -0.3) && !(t > this.length + this.tip)) {
            V3 q = p.sub(this.dir.mul(t));
            double rho = q.length();
            double radius = t <= this.length ? this.r : this.r * (1.0 - (t - this.length) / this.tip);
            if (rho > radius + 0.6) {
               return null;
            } else {
               double sector = (Math.PI * 2) / this.sides;
               double angle = ((StrictMath.atan2(q.dot(this.v), q.dot(this.u)) - this.twist) % sector + sector) % sector - sector / 2.0;
               double edge = radius * Math.cos(Math.PI / this.sides) / Math.cos(angle);
               if (rho > edge + 0.3) {
                  return null;
               } else if (t > this.length) {
                  return this.point;
               } else if (edge - rho < this.shell) {
                  return this.skin;
               } else {
                  return rho < 0.9 ? this.core : this.body;
               }
            }
         } else {
            return null;
         }
      }

      @Override
      public boolean force() {
         return this.force;
      }

      @Override
      public int axis() {
         return Solid.dominant(this.dir);
      }
   }

   record Ring(V3 c, double outer, double inner, double height, double ragged, Material material, boolean force) implements Solid {
      @Override
      public double[] bounds() {
         return new double[]{
            this.c.x() - this.outer - 1.0,
            this.c.y(),
            this.c.z() - this.outer - 1.0,
            this.c.x() + this.outer + 1.0,
            this.c.y() + this.height + 1.0,
            this.c.z() + this.outer + 1.0
         };
      }

      @Override
      public Material at(int x, int y, int z, long seed) {
         double dy = y - this.c.y();
         if (dy < 0.0) {
            return null;
         } else {
            double hx = x - this.c.x();
            double hz = z - this.c.z();
            double rho = Math.sqrt(hx * hx + hz * hz);
            if (!(rho > this.outer + 0.3) && !(rho < this.inner - 0.3)) {
               double top = this.height * (1.0 - this.ragged * (0.5 + 0.5 * Noise.smooth(seed + 23L, x / 3.0, 0.0, z / 3.0)));
               return dy <= top ? this.material : null;
            } else {
               return null;
            }
         }
      }
   }

   record Tube(V3 a, V3 b, double ra, double rb, Material material, boolean force) implements Solid {
      @Override
      public double[] bounds() {
         return Solid.span(this.a, this.b, Math.max(this.ra, this.rb) + 1.0);
      }

      @Override
      public Material at(int x, int y, int z, long seed) {
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
         return dx * dx + dy * dy + dz * dz <= r * r + 0.15 ? this.material : null;
      }

      @Override
      public int axis() {
         return Solid.dominant(this.b.sub(this.a));
      }
   }
}

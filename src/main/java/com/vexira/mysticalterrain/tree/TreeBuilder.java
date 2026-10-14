package com.vexira.mysticalterrain.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;

public final class TreeBuilder {
   private static final double TAU = Math.PI * 2;
   private static final double GOLDEN = 2.39996;
   private final TreeParams p;
   private final SplittableRandom random;
   private final double holes;
   private final List<Shape> wood = new ArrayList<>();
   private final List<Shape> veins = new ArrayList<>();
   private final List<Shape> foliage = new ArrayList<>();
   private final List<TreeBuilder.Curve> roots = new ArrayList<>();
   private TreeBuilder.Curve trunk;
   private double foot;

   private TreeBuilder(TreeParams p) {
      this.p = p;
      this.random = new SplittableRandom(p.seed());
      this.holes = Math.max(0.0, Math.min(0.8, 1.0 - p.density()));
   }

   public static Tree build(TreeParams params) {
      TreeBuilder b = new TreeBuilder(params);
      switch (params.style()) {
         case BROADLEAF:
            b.broadleaf();
            break;
         case BLOSSOM:
            b.blossom();
            break;
         case WEEPING:
            b.weeping();
            break;
         case BONSAI:
            b.bonsai();
            break;
         case ELDER:
            b.elder();
            break;
         case CONIFER:
            b.conifer();
            break;
         case SEQUOIA:
            b.sequoia();
            break;
         case UMBRELLA:
            b.umbrella();
            break;
         case COLUMN:
            b.column();
            break;
         case BAOBAB:
            b.baobab();
            break;
         case KAPOK:
            b.kapok();
            break;
         case BANYAN:
            b.banyan();
            break;
         case MANGROVE:
            b.mangrove();
            break;
         case PALM:
            b.palm();
            break;
         case SNAG:
            b.snag();
            break;
         case MUSHROOM:
            b.mushroom();
            break;
         case SPIRAL:
            b.spiral();
            break;
         case CRYSTAL:
            b.crystal();
            break;
         case CLUMP:
            b.clump();
            break;
         case CANOPY:
            b.canopy();
            break;
         case LAYERED:
            b.layered();
      }

      if (params.style() != TreeStyle.PALM) {
         b.veins(params.veins());
      }

      return b.fit();
   }

   private void broadleaf() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      double fork = 0.4 + this.p.shape() * 0.2;
      TreeBuilder.Curve t = this.trunk(0.0, h * fork, this.drift(h * fork, this.p.lean(), this.p.gnarl(), false), taper(r, r * 0.75, 0.6));
      this.roots(this.count(this.p.roots(), 0, 0), r, 1.0);
      int limbs = this.count(this.p.branches(), 3, 5);
      double rise = h * (1.0 - fork);
      double az0 = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < limbs; i++) {
         double az = az0 + i * (Math.PI * 2) / limbs + this.range(-0.3, 0.3);
         double out = c * this.range(0.55, 0.75);
         double up = rise * this.range(0.45, 0.7);
         double el = StrictMath.atan2(up, out);
         double len = StrictMath.hypot(out, up);
         TreeBuilder.Curve limb = this.grow(
            this.limb(t.at(this.range(0.85, 1.0)), az, el, len, r * 0.6, thin(r * 0.3), 0.2, 0.3 + this.p.gnarl() * 0.4), Part.BRANCH, 0.08
         );
         this.cloud(limb.end(), c * this.range(0.3, 0.4), 0.65);
         int subs = 1 + this.random.nextInt(2);

         for (int j = 0; j < subs; j++) {
            TreeBuilder.Curve sub = this.grow(
               this.limb(
                  limb.at(this.range(0.45, 0.8)),
                  az + this.range(-0.9, 0.9),
                  el * this.range(0.4, 0.9),
                  len * this.range(0.35, 0.55),
                  thin(r * 0.3),
                  thin(r * 0.15),
                  0.3,
                  0.4
               ),
               Part.BRANCH,
               0.0
            );
            this.cloud(sub.end(), c * this.range(0.25, 0.32), 0.65);
         }
      }

      V3 top = t.end();
      this.cloud(new V3(top.x(), h * 0.9, top.z()), c * 0.5, 0.5);
   }

   private void blossom() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      double fork = 0.35 + this.p.shape() * 0.15;
      TreeBuilder.Curve t = this.trunk(0.0, h * fork, this.drift(h * fork, this.p.lean() + 0.15, Math.max(this.p.gnarl(), 0.5), false), taper(r, r * 0.8, 0.8));
      this.roots(this.count(this.p.roots(), 4, 6), r, 1.2);
      double cluster = Math.min(c * 0.36, 5.0 + c * 0.14);
      int limbs = this.count(this.p.branches(), 4, 6);
      double rise = h * (1.0 - fork);
      double az0 = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < limbs; i++) {
         double az = az0 + i * (Math.PI * 2) / limbs + this.range(-0.35, 0.35);
         double out = c * this.range(0.55, 0.8);
         double up = rise * this.range(0.45, 0.7);
         double el = StrictMath.atan2(up, out) * 0.8;
         double len = StrictMath.hypot(out, up) * 1.05;
         TreeBuilder.Curve limb = this.grow(
            this.limb(t.at(this.range(0.8, 1.0)), az, el, len, r * 0.65, thin(r * 0.3), 0.6, 0.35 + this.p.gnarl() * 0.3), Part.BRANCH, 0.1
         );
         this.blossomCluster(limb.end(), cluster * this.range(1.0, 1.2));
         this.cloud(limb.at(0.7).add(0.0, cluster * 0.5, 0.0), cluster * 0.8, 0.6);
         int subs = 2 + this.random.nextInt(2) + (int)(c / 15.0);

         for (int j = 0; j < subs; j++) {
            TreeBuilder.Curve sub = this.grow(
               this.limb(
                  limb.at(this.range(0.35, 0.8)),
                  az + this.range(-0.9, 0.9),
                  el * this.range(0.4, 0.9),
                  len * this.range(0.3, 0.5),
                  thin(r * 0.3),
                  thin(r * 0.15),
                  0.4,
                  0.4
               ),
               Part.BRANCH,
               0.0
            );
            this.blossomCluster(sub.end(), cluster * this.range(0.7, 0.9));
         }
      }

      V3 top = t.end();
      int ring = Math.max(3, (int)(Math.PI * c / (cluster * 1.3)));

      for (int i = 0; i < ring; i++) {
         double a = az0 + (i + 0.5) * (Math.PI * 2) / ring;
         this.cloud(new V3(top.x() + StrictMath.cos(a) * c * 0.5, h * this.range(0.75, 0.85), top.z() + StrictMath.sin(a) * c * 0.5), cluster * 0.9, 0.5);
      }

      this.cloud(new V3(top.x(), h * 0.85, top.z()), cluster * 1.3, 0.45);
   }

   private void blossomCluster(V3 at, double size) {
      this.cloud(at, size, 0.6);
      if (this.random.nextDouble() < 0.5) {
         double a = this.random.nextDouble() * (Math.PI * 2);
         V3 lobe = at.add(StrictMath.cos(a) * size * 0.3, -size * this.range(0.55, 0.8), StrictMath.sin(a) * size * 0.3);
         this.foliage.add(new Shape.Blob(lobe, size * 0.45, size * 0.55, size * 0.45, 0.3, this.holes, 1.0, Part.LEAVES));
      }
   }

   private void weeping() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      double fork = 0.45 + this.p.shape() * 0.15;
      TreeBuilder.Curve t = this.trunk(0.0, h * fork, this.drift(h * fork, this.p.lean(), this.p.gnarl(), false), taper(r, r * 0.75, 0.7));
      this.roots(this.count(this.p.roots(), 0, 3), r, 1.0);
      int limbs = this.count(this.p.branches(), 4, 7);
      double hang = h * (0.25 + 0.5 * this.p.droop());
      double rise = h * (1.0 - fork);
      double az0 = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < limbs; i++) {
         double az = az0 + i * (Math.PI * 2) / limbs + this.range(-0.3, 0.3);
         double out = c * this.range(0.55, 0.8);
         double up = rise * this.range(0.6, 0.9);
         TreeBuilder.Curve limb = this.grow(
            this.limb(
               t.at(this.range(0.75, 1.0)), az, Math.min(1.3, StrictMath.atan2(up, out) * 1.25), StrictMath.hypot(out, up), r * 0.55, thin(r * 0.2), -1.1, 0.3
            ),
            Part.BRANCH,
            0.06
         );

         for (double s : new double[]{0.45, 0.72, 1.0}) {
            V3 at = limb.at(s);
            double br = c * this.range(0.18, 0.26);
            this.foliage.add(new Shape.Blob(at, br, br * 0.6, br, 0.3, this.holes, 1.0, Part.LEAVES));
            this.strands(at, br, br * 0.6, (int)(br * 1.5) + 2, hang);
         }
      }

      V3 top = new V3(t.end().x(), h * 0.95, t.end().z());
      double cr = c * 0.4;
      this.cloud(top, cr, 0.6);
      this.strands(top, cr, cr * 0.6, (int)(cr * 2.0) + 3, hang);
   }

   private void bonsai() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      TreeBuilder.Curve t = this.trunk(0.0, h, this.drift(h, Math.max(this.p.lean(), 0.4), this.p.gnarl(), true), taper(r, thin(r * 0.35), 0.8));
      this.roots(this.count(this.p.roots(), 3, 5), r, 1.0);
      int n = this.count(this.p.branches(), 4, 7);
      double az = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < n; i++) {
         double s = 0.35 + 0.6 * (i + this.random.nextDouble()) / n;
         az += 2.39996 + this.range(-0.3, 0.3);
         double len = c * this.range(0.5, 0.9) * (1.1 - s * 0.4);
         TreeBuilder.Curve b = this.grow(
            this.limb(t.at(s), az, this.range(-0.15, 0.3), len, thin(t.radius(s) * 0.55), 0.55, 0.35, 0.35 + this.p.gnarl() * 0.3), Part.BRANCH, 0.05
         );
         double pr = c * this.range(0.3, 0.42);
         this.cushion(b.end(), pr);
         if (len > 8.0 && this.random.nextDouble() < 0.5) {
            this.cushion(b.at(0.6).add(0.0, 1.0, 0.0), pr * 0.6);
         }
      }

      this.cushion(t.end().add(0.0, 1.0, 0.0), c * 0.3);
   }

   private void cushion(V3 at, double size) {
      double thick = Math.max(2.0, size * 0.4);
      this.pad(at, size, thick);
      if (this.p.droop() > 0.0) {
         this.strands(at, size, thick * 0.4, (int)(size * 2.0) + 2, this.p.height() * this.p.droop() * 0.5);
      }
   }

   private void elder() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      TreeBuilder.Curve t = this.trunk(0.0, h, this.drift(h, this.p.lean(), Math.max(0.6, this.p.gnarl()), false), taper(r, thin(r * 0.45), 0.9));
      this.roots(this.count(this.p.roots(), 7, 11), r, 1.5);
      int n = this.count(this.p.branches(), 5, 8);
      double az = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < n; i++) {
         double s = 0.3 + 0.62 * (i + this.random.nextDouble()) / n;
         az += 2.39996 + this.range(-0.25, 0.25);
         double len = c * this.range(0.65, 1.0) * (1.15 - s * 0.45);
         TreeBuilder.Curve b = this.grow(
            this.limb(t.at(s), az, this.range(0.05, 0.45) + s * 0.3, len, thin(t.radius(s) * 0.5), 0.6, 0.25, 0.6), Part.BRANCH, 0.1
         );
         if (this.random.nextDouble() < 0.8) {
            double pr = c * this.range(0.28, 0.38);
            this.pad(b.end(), pr, Math.max(1.5, pr * 0.3));
            if (len > 10.0 && this.random.nextDouble() < 0.6) {
               TreeBuilder.Curve sub = this.grow(this.limb(b.at(0.55), az + this.range(-0.9, 0.9), 0.35, len * 0.4, 0.8, 0.55, 0.3, 0.5), Part.BRANCH, 0.0);
               this.pad(sub.end(), pr * 0.6, Math.max(1.5, pr * 0.25));
            }
         } else {
            this.grow(this.limb(b.end(), az + this.range(-0.5, 0.5), 0.6, len * 0.35, 0.6, 0.5, 0.2, 0.6), Part.BRANCH, 0.0);
         }
      }

      double top = c * 0.33;
      this.pad(t.end().add(0.0, 1.0, 0.0), top, Math.max(1.5, top * 0.3));
   }

   private void conifer() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      TreeBuilder.Curve t = this.trunk(0.0, h, this.drift(h, this.p.lean() * 0.3, this.p.gnarl() * 0.4, false), taper(r, thin(r * 0.25), 0.5));
      this.roots(this.count(this.p.roots(), 0, 0), r, 1.0);
      double bare = 0.1 + this.p.shape() * 0.3;
      double step = 2.0 + h / 45.0;
      double y = h * bare;
      double span = h - y;

      for (int layer = 0; y < h - 1.0; layer++) {
         double u = (y - h * bare) / span;
         double rr = c * StrictMath.pow(1.0 - u, 0.9) * (layer % 2 == 0 ? 1.0 : 0.85) + 1.2;
         V3 center = t.at(y / h);
         this.foliage.add(new Shape.Skirt(center, rr, 1.4 + step * 0.4, rr * (0.25 + this.p.droop() * 0.3), this.holes));
         if (rr > 3.0) {
            int arms = 4 + this.random.nextInt(3);
            double a0 = this.random.nextDouble() * (Math.PI * 2);

            for (int k = 0; k < arms; k++) {
               this.grow(this.limb(center.add(0.0, -0.5, 0.0), a0 + k * (Math.PI * 2) / arms, -0.25, rr * 0.7, 0.6, 0.5, -0.2, 0.1), Part.BRANCH, 0.0);
            }
         }

         y += step * this.range(0.85, 1.15);
      }

      V3 top = t.end();
      this.foliage.add(new Shape.Tube(top.add(0.0, -2.0, 0.0), top.add(0.0, 2.0 + h / 25.0, 0.0), 1.3, 0.5, Part.LEAVES, 0.0));
   }

   private void sequoia() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      TreeBuilder.Curve t = this.trunk(0.0, h, this.drift(h, this.p.lean() * 0.3, this.p.gnarl() * 0.5, false), taper(r, thin(r * 0.35), 0.9));
      this.roots(this.count(this.p.roots(), 5, 8), r, 1.1);
      double bare = 0.4 + this.p.shape() * 0.2;
      int n = this.count(this.p.branches(), Math.max(8, (int)(h * (1.0 - bare) / 2.2)), Math.max(8, (int)(h * (1.0 - bare) / 2.2)));
      double az = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < n; i++) {
         double u = (i + this.random.nextDouble()) / n;
         double s = bare + (0.96 - bare) * u;
         double profile = StrictMath.pow(1.0 - u, 0.7) * (0.6 + 0.4 * Math.min(1.0, u * 4.0));
         az += 2.39996;
         V3 start = t.at(s);
         double len = c * profile * this.range(0.7, 1.0);
         V3 end = start;
         if (len > 2.0) {
            end = this.grow(this.limb(start, az, this.range(-0.2, 0.25), len, Math.max(0.6, t.radius(s) * 0.3), 0.55, 0.3, 0.2), Part.BRANCH, 0.0).end();
         }

         this.cloud(end, Math.max(2.2, c * 0.22 * profile + 1.5), 0.7);
      }

      this.cloud(t.end(), c * 0.28 + 1.5, 0.8);
   }

   private void umbrella() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      double fork = 0.4 + this.p.shape() * 0.4;
      TreeBuilder.Curve t = this.trunk(0.0, h * fork, this.drift(h * fork, this.p.lean(), this.p.gnarl(), false), taper(r, r * 0.8, 0.5));
      this.roots(this.count(this.p.roots(), 0, 3), r, 1.0);
      int forks = this.count(this.p.branches(), 3, 5);
      int depth = h > 45.0 ? 3 : 2;
      double az = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < forks; i++) {
         this.split(t.end(), az + i * (Math.PI * 2) / forks + this.range(-0.4, 0.4), depth, h * (1.0 - fork), c * 0.8, r * 0.7);
      }
   }

   private void split(V3 from, double az, int depth, double rise, double spread, double radius) {
      double out = spread * this.range(0.4, 0.6);
      double up = rise * this.range(0.45, 0.65);
      TreeBuilder.Curve b = this.grow(
         this.limb(from, az, StrictMath.atan2(up, out), StrictMath.hypot(out, up), thin(radius), thin(radius * 0.6), 0.1, 0.25), Part.BRANCH, 0.05
      );
      if (depth > 1) {
         for (int i = 0; i < 2; i++) {
            this.split(b.end(), az + (i - 0.5) * this.range(0.7, 1.2), depth - 1, Math.max(2.0, rise - up), Math.max(2.0, spread - out), radius * 0.6);
         }
      } else {
         double pr = this.p.crown() * this.range(0.3, 0.4);
         this.pad(b.end(), pr, Math.max(1.5, pr * 0.25));
      }
   }

   private void column() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      TreeBuilder.Curve t = this.trunk(0.0, h, this.drift(h, this.p.lean() * 0.3, this.p.gnarl() * 0.3, false), taper(r, thin(r * 0.4), 0.4));
      double bare = 0.06 + this.p.shape() * 0.15;

      for (double y = h * bare; y <= h + 1.0; y += 2.2) {
         double u = (y - h * bare) / (h * (1.0 - bare) + 1.0);
         double w = c * StrictMath.pow(StrictMath.sin(Math.PI * Math.min(1.0, u * 0.9 + 0.08)), 0.7) + 0.8;
         this.foliage.add(new Shape.Blob(t.at(Math.min(1.0, y / h)), w, 2.6, w, 0.25, this.holes, 1.0, Part.LEAVES));
      }

      V3 top = t.end();
      this.foliage.add(new Shape.Tube(top, top.add(0.0, 3.0, 0.0), 1.0, 0.5, Part.LEAVES, 0.0));
   }

   private void baobab() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      double bulge = 0.35 + this.p.shape() * 0.3;
      TreeBuilder.Curve t = this.trunk(
         0.0,
         h,
         this.drift(h, this.p.lean() * 0.3, this.p.gnarl() * 0.4, false),
         s -> r * (1.0 + bulge * StrictMath.sin(Math.PI * Math.min(1.0, s * 1.1))) * (1.0 - 0.45 * s)
            + r * 0.4 * StrictMath.pow(Math.max(0.0, 1.0 - s / 0.1), 2.0)
      );
      this.roots(this.count(this.p.roots(), 0, 4), r, 0.8);
      int n = this.count(this.p.branches(), 6, 10);
      V3 top = t.end();
      double az0 = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < n; i++) {
         double az = az0 + i * (Math.PI * 2) / n + this.range(-0.25, 0.25);
         V3 start = top.add(StrictMath.cos(az) * r * 0.3, this.range(-2.0, 0.0), StrictMath.sin(az) * r * 0.3);
         TreeBuilder.Curve b = this.grow(
            this.limb(start, az, this.range(0.25, 0.8), c * this.range(0.5, 0.9), thin(r * 0.32), 0.6, 0.2, 0.3), Part.BRANCH, 0.05
         );

         for (int j = 0; j < 2; j++) {
            TreeBuilder.Curve twig = this.grow(this.limb(b.end(), az + this.range(-0.6, 0.6), 0.5, c * 0.25, 0.6, 0.5, 0.2, 0.3), Part.BRANCH, 0.0);
            this.cloud(twig.end(), this.range(1.8, 2.8) + c * 0.08, 0.6);
         }
      }
   }

   private void kapok() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      TreeBuilder.Curve t = this.trunk(0.0, h, this.drift(h, this.p.lean() * 0.3, this.p.gnarl() * 0.4, false), taper(r, r * 0.6, 0.3));
      this.buttresses(this.count(this.p.roots(), 4, 6), r, h * this.range(0.06, 0.1));
      int n = this.count(this.p.branches(), 4, 6) + (int)(c / 12.0);
      double start = 0.7 + this.p.shape() * 0.1;
      double az = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < n; i++) {
         double s = start + (0.97 - start) * i / n;
         az += 2.39996;
         double len = c * this.range(0.6, 0.85);
         TreeBuilder.Curve b = this.grow(this.limb(t.at(s), az, this.range(0.15, 0.5), len, thin(r * 0.4), 0.8, 0.35, 0.3), Part.BRANCH, 0.06);
         double pr = c * this.range(0.3, 0.4);
         this.pad(b.end(), pr, Math.max(2.0, c * 0.1));
         TreeBuilder.Curve sub = this.grow(
            this.limb(b.at(0.6), az + (this.random.nextBoolean() ? 0.7 : -0.7), 0.5, len * 0.4, 0.8, 0.55, 0.3, 0.3), Part.BRANCH, 0.0
         );
         this.cloud(sub.end(), c * 0.2, 0.6);
      }

      this.pad(t.end().add(0.0, 1.0, 0.0), c * 0.35, Math.max(2.0, c * 0.1));
   }

   private void buttresses(int count, double r, double height) {
      height = Math.min(height, 3.0 + r * 1.2);
      double az0 = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < count; i++) {
         double az = az0 + i * (Math.PI * 2) / count + this.range(-0.3, 0.3);
         double out = r * this.range(1.2, 1.9) + 1.5;
         double top = height * this.range(0.65, 1.0);
         double swerve = this.range(-0.35, 0.35);

         for (int k = 0; k <= (int)top; k++) {
            double reach = out * StrictMath.pow(1.0 - k / (top + 1.0), 2.2);
            if (reach < 0.8) {
               break;
            }

            double thick = 1.1 - 0.5 * k / (top + 1.0);
            V3 a = V3.of(az, 0.0).mul(r * 0.6).add(0.0, k, 0.0);
            V3 m = V3.of(az + swerve * 0.5, 0.0).mul(r * 0.8 + reach * 0.5).add(0.0, k - 0.3, 0.0);
            V3 b = V3.of(az + swerve, 0.0).mul(r * 0.8 + reach).add(0.0, k - 0.8, 0.0);
            this.wood.add(new Shape.Tube(a, m, thick, thick * 0.85, Part.ROOT, 0.0));
            this.wood.add(new Shape.Tube(m, b, thick * 0.85, 0.55, Part.ROOT, 0.0));
         }
      }
   }

   private void banyan() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      double fork = 0.5 + this.p.shape() * 0.15;
      TreeBuilder.Curve t = this.trunk(0.0, h * fork, this.drift(h * fork, this.p.lean(), this.p.gnarl(), false), taper(r, r * 0.8, 0.8));
      this.roots(this.count(this.p.roots(), 4, 7), r, 1.0);
      int n = this.count(this.p.branches(), 6, 9);
      double az0 = this.random.nextDouble() * (Math.PI * 2);
      List<V3> props = new ArrayList<>();

      for (int i = 0; i < n; i++) {
         double az = az0 + i * (Math.PI * 2) / n + this.range(-0.25, 0.25);
         TreeBuilder.Curve b = this.grow(
            this.limb(t.at(this.range(0.8, 1.0)), az, this.range(0.05, 0.3), c * this.range(0.75, 0.95), thin(r * 0.45), 0.8, 0.25, 0.25), Part.BRANCH, 0.06
         );
         int drops = 1 + this.random.nextInt(2);

         for (int k = 0; k < drops; k++) {
            V3 at = b.at(this.range(0.4, 0.9));
            if (!(Math.hypot(at.x(), at.z()) < r * 1.8 + 2.0) && !crowds(props, at, 5.0)) {
               props.add(at);
               double rr = this.range(0.75, 1.1) * Math.max(1.0, r * 0.3);
               V3 foot = new V3(at.x() + this.range(-0.8, 0.8), -8.0, at.z() + this.range(-0.8, 0.8));
               this.wood.add(new Shape.Tube(at, foot, rr * 0.8, rr * 1.3, Part.TRUNK, 0.1));
            }
         }

         for (int k = this.random.nextInt(3); k > 0; k--) {
            V3 at = b.at(this.range(0.3, 0.95));
            if (!crowds(props, at, 3.0)) {
               props.add(at);
               this.wood.add(new Shape.Tube(at, at.add(this.range(-0.5, 0.5), -this.range(3.0, 7.0), this.range(-0.5, 0.5)), 0.55, 0.5, Part.BRANCH, 0.0));
            }
         }

         this.cloud(b.end(), c * this.range(0.25, 0.33), 0.45);
         this.cloud(b.at(0.55).add(0.0, 2.0, 0.0), c * 0.25, 0.45);
      }

      V3 top = t.end();
      this.cloud(new V3(top.x(), h, top.z()), c * 0.38, 0.4);
   }

   private static boolean crowds(List<V3> points, V3 at, double gap) {
      for (V3 q : points) {
         if (Math.hypot(q.x() - at.x(), q.z() - at.z()) < gap) {
            return true;
         }
      }

      return false;
   }

   private void mangrove() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      double lift = h * this.range(0.18, 0.28);
      TreeBuilder.Curve t = this.trunk(lift, h - lift, this.drift(h - lift, this.p.lean(), this.p.gnarl(), false), taper(r, r * 0.6, 0.2));
      int n = this.count(this.p.roots(), 7, 11);
      double az0 = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < n; i++) {
         double az = az0 + i * (Math.PI * 2) / n + this.range(-0.3, 0.3);
         V3 top = t.at(this.range(0.0, 0.12)).add(StrictMath.cos(az) * r * 0.6, 0.0, StrictMath.sin(az) * r * 0.6);
         double dist = r * 1.5 + lift * this.range(0.8, 1.3);
         V3 foot = new V3(StrictMath.cos(az) * dist, -2.0, StrictMath.sin(az) * dist);
         V3 mid = top.lerp(foot, 0.5).add(StrictMath.cos(az) * dist * 0.25, lift * 0.35, StrictMath.sin(az) * dist * 0.25);
         TreeBuilder.Curve root = new TreeBuilder.Curve();

         for (int j = 0; j <= 8; j++) {
            double s = j / 8.0;
            root.add(top.lerp(mid, s).lerp(mid.lerp(foot, s), s), thin(r * (0.5 - 0.15 * s)));
         }

         this.grow(root, Part.ROOT, 0.05);
      }

      int limbs = this.count(this.p.branches(), 4, 6);

      for (int i = 0; i < limbs; i++) {
         double az = az0 + i * (Math.PI * 2) / limbs + this.range(-0.3, 0.3);
         TreeBuilder.Curve b = this.grow(
            this.limb(t.at(this.range(0.5, 0.9)), az, this.range(0.4, 0.8), c * this.range(0.5, 0.75), thin(r * 0.45), 0.6, 0.2, 0.3), Part.BRANCH, 0.05
         );
         this.cloud(b.end(), c * 0.36, 0.7);
      }

      this.cloud(t.end(), c * 0.42, 0.7);
   }

   private void palm() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      TreeBuilder.Curve t = this.trunk(0.0, h, this.drift(h, Math.max(0.15, this.p.lean()), this.p.gnarl() * 0.3, false), taper(r, r * 0.75, 0.35));
      V3 top = t.end();
      int n = this.count(this.p.branches(), 12, 16);
      double lift = 0.25 + this.p.shape() * 0.6;
      double width = 1.5 + c * 0.12;
      double rib = Math.max(0.5, c * 0.045);
      double az0 = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < n; i++) {
         double az = az0 + i * 2.39996 + this.range(-0.15, 0.15);
         boolean young = i >= n - 3;
         double length = c * (young ? this.range(0.35, 0.5) : this.range(0.85, 1.1));
         double rise = young ? this.range(1.0, 1.3) : lift + this.range(-0.2, 0.3) + i % 3 * 0.12;
         TreeBuilder.Curve frond = this.limb(
            top.add(0.0, young ? 0.5 : this.range(-0.5, 0.5), 0.0), az, rise, length, 0.5, 0.5, young ? -0.2 : -(1.2 + this.p.droop()), 0.05
         );
         V3 side = V3.of(az + (Math.PI / 2), 0.0);
         int points = frond.points.size();

         for (int j = 0; j + 1 < points; j++) {
            V3 a = frond.points.get(j);
            this.foliage.add(new Shape.Tube(a, frond.points.get(j + 1), rib, rib, Part.LEAVES, 0.0));
            double s = (double)j / (points - 1);
            double w = width * (young ? 0.5 : 1.0) * (1.0 - s * 0.7) * StrictMath.sin(Math.PI * Math.min(1.0, s * 1.3 + 0.1));
            if (j > 0 && w > 0.6) {
               this.foliage.add(new Shape.Tube(a, a.add(side.mul(w)).add(0.0, -w * 0.6, 0.0), rib, 0.45, Part.LEAVES, 0.0));
               this.foliage.add(new Shape.Tube(a, a.add(side.mul(-w)).add(0.0, -w * 0.6, 0.0), rib, 0.45, Part.LEAVES, 0.0));
            }
         }
      }

      this.foliage.add(new Shape.Blob(top, 1.6, 1.3, 1.6, 0.2, 0.0, 1.0, Part.LEAVES));
      int fruit = 4 + this.random.nextInt(4);

      for (int i = 0; i < fruit; i++) {
         double a = this.random.nextDouble() * (Math.PI * 2);
         this.foliage
            .add(
               new Shape.Blob(
                  top.add(StrictMath.cos(a) * (r + 0.5), this.range(-2.5, -1.0), StrictMath.sin(a) * (r + 0.5)), 0.8, 0.8, 0.8, 0.0, 0.0, 1.0, Part.FRUIT
               )
            );
      }

      if (this.p.veins() > 0) {
         for (double y = 2 + this.random.nextInt(2); y < h - 2.0; y += 2 + this.random.nextInt(2)) {
            V3 at = t.at(y / h);
            double rr = t.radius(y / h) + 0.3;
            this.veins.add(new Shape.Tube(at, at.add(0.0, 0.4, 0.0), rr, rr, Part.VEIN, 0.0));
         }
      }
   }

   private void snag() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      TreeBuilder.Curve t = this.trunk(0.0, h, this.drift(h, this.p.lean(), Math.max(0.5, this.p.gnarl()), false), taper(r, thin(r * 0.3), 0.7));
      this.roots(this.count(this.p.roots(), 3, 5), r, 0.9);
      int n = this.count(this.p.branches(), 3, 7);
      double az = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < n; i++) {
         double s = this.range(0.3, 0.92);
         az += 2.39996 + this.range(-0.4, 0.4);
         double len = t.radius(s) + c * this.range(0.6, 1.0) * (1.1 - s * 0.4);
         TreeBuilder.Curve b = this.grow(this.limb(t.at(s), az, this.range(0.1, 0.7), len, thin(t.radius(s) * 0.45), 0.55, 0.3, 0.6), Part.BRANCH, 0.05);
         if (this.random.nextDouble() < 0.5) {
            this.grow(this.limb(b.at(0.6), az + (this.random.nextBoolean() ? 0.8 : -0.8), 0.6, len * 0.35, 0.6, 0.5, 0.2, 0.5), Part.BRANCH, 0.0);
         }

         this.foliage.add(new Shape.Blob(b.end(), this.range(1.2, 2.0), this.range(1.0, 1.5), this.range(1.2, 2.0), 0.3, this.holes, 1.0, Part.LEAVES));
      }
   }

   private void mushroom() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      TreeBuilder.Curve t = this.trunk(0.0, h, this.drift(h, this.p.lean(), this.p.gnarl(), false), taper(r, r * 0.8, 0.6));
      V3 top = t.end();
      double tall = 0.25 + this.p.shape() * 0.7;
      double capHeight = c * tall;
      this.foliage.add(new Shape.Cap(top.add(0.0, -capHeight * 0.3, 0.0), c, capHeight, Math.max(1.5, c * 0.12), c * 0.25 * this.p.droop(), this.holes * 0.3));
      int n = this.count(this.p.branches(), 0, 2);

      for (int i = 0; i < n; i++) {
         double az = this.random.nextDouble() * (Math.PI * 2);
         TreeBuilder.Curve stem = this.grow(this.limb(t.at(this.range(0.35, 0.7)), az, 0.7, c * 0.35, thin(r * 0.4), 0.6, 0.3, 0.2), Part.BRANCH, 0.0);
         double cr = c * this.range(0.3, 0.45);
         this.foliage
            .add(new Shape.Cap(stem.end().add(0.0, -cr * 0.1, 0.0), cr, cr * tall, Math.max(1.2, cr * 0.15), cr * 0.2 * this.p.droop(), this.holes * 0.3));
      }
   }

   private void spiral() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      double coil = r * 1.5 + 2.0 + this.p.shape() * 4.0;
      double turns = 1.2 + this.p.shape() * 1.5;
      double phase = this.random.nextDouble() * (Math.PI * 2);
      TreeBuilder.Curve t = this.trunk(0.0, h, sx -> {
         double k = coil * Math.min(1.0, sx * 5.0);
         return new V3(StrictMath.cos(phase + (Math.PI * 2) * turns * sx) * k, 0.0, StrictMath.sin(phase + (Math.PI * 2) * turns * sx) * k);
      }, taper(r, r * 0.6, 0.3));
      this.roots(this.count(this.p.roots(), 3, 5), r, 1.0);
      int n = this.count(this.p.branches(), 5, 9);

      for (int i = 0; i < n; i++) {
         double s = 0.3 + 0.65 * (i + 0.5) / n;
         V3 at = t.at(s);
         TreeBuilder.Curve b = this.grow(
            this.limb(
               at,
               at.azimuth() + this.range(-0.3, 0.3),
               this.range(0.1, 0.5),
               c * this.range(0.5, 0.8) * (1.1 - s * 0.4),
               thin(t.radius(s) * 0.5),
               0.55,
               0.6,
               0.2
            ),
            Part.BRANCH,
            0.05
         );
         this.cloud(b.end(), c * this.range(0.2, 0.28), 0.7);
      }

      this.cloud(t.end(), c * 0.35, 0.7);
   }

   private void crystal() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      int kinks = 3 + this.random.nextInt(3);
      V3[] offsets = new V3[kinks + 1];
      offsets[0] = V3.ZERO;

      for (int i = 1; i <= kinks; i++) {
         double a = this.random.nextDouble() * (Math.PI * 2);
         double d = (this.p.lean() * h * 0.25 + 1.0) * this.range(0.3, 1.0);
         offsets[i] = offsets[i - 1].add(StrictMath.cos(a) * d, 0.0, StrictMath.sin(a) * d).mul(0.7);
      }

      TreeBuilder.Curve t = this.trunk(0.0, h, sx -> {
         double f = sx * kinks;
         int ix = Math.min(kinks - 1, (int)f);
         return offsets[ix].lerp(offsets[ix + 1], f - ix);
      }, taper(r, thin(r * 0.4), 0.4));
      this.roots(this.count(this.p.roots(), 3, 4), r, 0.8);
      int n = this.count(this.p.branches(), 3, 6) + (int)(c / 8.0);
      double az = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < n; i++) {
         double s = this.range(0.35, 0.9);
         az += 2.39996 + this.range(-0.3, 0.3);
         double el = this.range(0.3, 0.8);
         double len = c * this.range(0.45, 0.7);
         V3 start = t.at(s);
         V3 bend = start.add(V3.of(az, el).mul(len * 0.55));
         V3 end = bend.add(V3.of(az + this.range(-0.5, 0.5), el + this.range(-0.2, 0.4)).mul(len * 0.45));
         double br = thin(t.radius(s) * 0.45);
         this.wood.add(new Shape.Tube(start, bend, br, br * 0.8, Part.BRANCH, 0.0));
         this.wood.add(new Shape.Tube(bend, end, br * 0.8, 0.55, Part.BRANCH, 0.0));
         this.crystals(end, c * this.range(0.3, 0.45));
      }

      this.crystals(t.end(), c * 0.55);
   }

   private void crystals(V3 at, double size) {
      double core = Math.max(1.0, size * 0.3);
      this.foliage.add(new Shape.Blob(at, core, core * 0.85, core, 0.1, 0.0, 1.0, Part.LEAVES));
      int spikes = 7 + this.random.nextInt(6) + (int)(size / 3.0);

      for (int i = 0; i < spikes; i++) {
         V3 dir = V3.of(this.random.nextDouble() * (Math.PI * 2), this.range(-0.3, 1.4));
         double len = Math.max(2.5, size * this.range(0.6, 1.2));
         this.foliage.add(new Shape.Tube(at, at.add(dir.mul(len)), Math.max(0.8, size * this.range(0.12, 0.2)), 0.3, Part.LEAVES, 0.0));
      }
   }

   private void clump() {
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      int n = Math.max(1, this.count(this.p.branches(), 3, 5));
      boolean braided = this.p.shape() > 0.5;
      double phase = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < n; i++) {
         double az = phase + i * (Math.PI * 2) / n + this.range(-0.3, 0.3);
         double off = braided ? r * 1.3 + 0.8 : r * this.range(1.5, 3.0) + 0.8;
         double hh = h * this.range(0.75, 1.0);
         double rr = r * this.range(0.6, 1.0);
         double lean = this.p.lean() * hh * 0.3;
         DoubleFunction<V3> path = braided
            ? s -> new V3(
               StrictMath.cos(az + (Math.PI * 8.0 / 5.0) * s) * off * (1.0 - s * 0.5),
               0.0,
               StrictMath.sin(az + (Math.PI * 8.0 / 5.0) * s) * off * (1.0 - s * 0.5)
            )
            : s -> new V3(StrictMath.cos(az) * (off + lean * StrictMath.pow(s, 1.3)), 0.0, StrictMath.sin(az) * (off + lean * StrictMath.pow(s, 1.3)));
         TreeBuilder.Curve stem = this.trunk(0.0, hh, path, taper(rr, thin(rr * 0.5), 0.3));
         if (!braided) {
            double w = c * this.range(0.3, 0.4);
            this.foliage.add(new Shape.Blob(stem.at(0.8), w, hh * 0.22, w, 0.3, this.holes, 1.0, Part.LEAVES));
            this.cloud(stem.end(), w * 0.6, 0.8);
         }
      }

      if (braided) {
         this.cloud(new V3(0.0, h * 0.95, 0.0), c * 0.5, 0.7);
      }
   }

   private void canopy() {
      Growth g = this.p.growth();
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      double rise = c * (0.75 - 0.4 * g.flat());
      double rim = h - rise;
      double lean = this.random.nextDouble() * (Math.PI * 2);
      TreeBuilder.Curve t = this.trunk(0.0, rim + rise * 0.15, this.bent(rim, lean, g), taper(r, r * 0.6, 0.45 + 0.45 * g.buttress()));
      this.footing(g, r);
      V3 top = t.end();
      int n = Math.max(1, g.lobes());
      List<double[]> lobes = new ArrayList<>();
      lobes.add(new double[]{top.x(), rim, top.z(), c * (0.5 + 0.2 * (1.0 - g.spread())), rise});

      for (int i = 0; i < n; i++) {
         double az = lean + i * (Math.PI * 2) / n + this.range(-0.35, 0.35);
         double pull = 1.0 + g.lopside() * StrictMath.cos(az - lean);
         double d = c * g.spread() * 0.5 * pull;
         lobes.add(
            new double[]{
               top.x() + StrictMath.cos(az) * d,
               rim + rise * g.tiers() * this.range(-0.3, 0.35),
               top.z() + StrictMath.sin(az) * d,
               c * this.range(0.35, 0.5) * (0.8 + 0.3 * pull),
               rise * this.range(0.55, 0.95)
            }
         );
      }

      int florets = 2 + this.random.nextInt(3);

      for (int i = 0; i < florets; i++) {
         double az = this.random.nextDouble() * (Math.PI * 2);
         double d = c * this.range(0.1, 0.35);
         lobes.add(
            new double[]{
               top.x() + StrictMath.cos(az) * d,
               rim + rise * this.range(0.35, 0.6),
               top.z() + StrictMath.sin(az) * d,
               c * this.range(0.25, 0.38),
               rise * this.range(0.45, 0.7)
            }
         );
      }

      fitLobes(lobes, top, c);
      Shape.Canopy shell = new Shape.Canopy(
         lobes.toArray(double[][]::new), rise * (0.9 - 0.6 * g.hollow()) + 2.0, rise * g.drip() * 1.2, 0.6, this.holes, this.p.seed()
      );
      this.foliage.add(shell);
      double[] b = shell.bounds();
      double step = Math.max(1.6, Math.min(3.2, c / 9.0));
      int target = (int)Math.max(250.0, Math.min(4000.0, c * c * 2.0));
      List<V3> points = new ArrayList<>();

      for (int tries = 0; points.size() < target && tries < target * 30; tries++) {
         double x = this.range(b[0], b[3]);
         double y = this.range(b[1], b[4]);
         double z = this.range(b[2], b[5]);
         if (shell.holds(x, y, z)) {
            points.add(new V3(x, y, z));
         }
      }

      Colonizer grower = new Colonizer(step * 5.0, step * 1.4, step, new V3(0.0, g.tropism() * 0.5, 0.0), g.jitter(), this.random);

      for (double s = 0.7; s <= 1.001; s += 0.1) {
         grower.seed(t.at(s));
      }

      this.leaders(grower, t, g, lean, rise);
      grower.grow(points, (int)(c * 4.0 / step) + 40, 12000);
      this.branches(grower, r * 0.55, step, false, g);
   }

   private void layered() {
      Growth g = this.p.growth();
      double h = this.p.height();
      double r = this.p.radius();
      double c = this.p.crown();
      double lean = this.random.nextDouble() * (Math.PI * 2);
      double stem = h * 0.88;
      TreeBuilder.Curve t = this.trunk(0.0, stem, this.bent(stem, lean, g), taper(r, thin(r * 0.35), 0.45 + 0.45 * g.buttress()));
      this.footing(g, r);
      int n = Math.max(3, (int)Math.round(g.lobes() * Math.max(1.6, c / 12.0)));
      List<double[]> pads = new ArrayList<>();

      for (int i = 0; i < n; i++) {
         double u = n == 1 ? 1.0 : (double)i / (n - 1);
         double level = g.sprout() + (0.97 - g.sprout()) * StrictMath.pow(u, 1.0 - 0.5 * g.tiers());
         double az = i == n - 1 ? lean : lean + (i % 2 == 0 ? 1 : -1) * this.range(0.2, Math.PI * (1.0 - 0.55 * g.lopside()));
         double pull = (1.0 + g.lopside() * StrictMath.cos(az - lean)) / (1.0 + g.lopside());
         double pr = Math.min(c * 0.5, Math.max(3.5, c * this.range(0.3, 0.45) * (1.0 - 0.3 * u) * Math.min(1.0, 22.0 / c + 0.35)));
         double d = i == n - 1 ? c * 0.1 : (c - pr) * g.spread() * (1.0 - 0.5 * u) * this.range(0.7, 1.0) * pull;
         double pt = Math.max(1.8, pr * (0.38 - 0.2 * g.flat()));
         V3 at = t.at(Math.min(1.0, level * h / stem)).add(V3.of(az, 0.0).mul(d)).add(0.0, d * Math.min(0.0, g.tropism()) * 0.6 + this.range(-1.0, 2.0), 0.0);
         pads.add(new double[]{at.x(), at.y(), at.z(), pr, pt});
      }

      fitLobes(pads, t.end(), c);
      double step = Math.max(1.5, Math.min(3.0, c / 10.0));
      List<V3> points = new ArrayList<>();

      for (double[] pad : pads) {
         int want = (int)Math.max(24.0, Math.min(700.0, pad[3] * pad[3] * pad[4] * 0.9));
         int tries = 0;

         for (int got = 0; got < want && tries < want * 8; tries++) {
            double dx = this.range(-1.0, 1.0);
            double dy = this.range(-1.0, 1.0);
            double dz = this.range(-1.0, 1.0);
            if (dx * dx + dy * dy + dz * dz <= 1.0) {
               points.add(new V3(pad[0] + dx * pad[3], pad[1] + dy * pad[4], pad[2] + dz * pad[3]));
               got++;
            }
         }

         this.foliage
            .add(
               new Shape.Blob(
                  new V3(pad[0], pad[1] + pad[4] * 0.3, pad[2]),
                  pad[3] * 0.85,
                  Math.max(1.3, pad[4] * 0.6),
                  pad[3] * 0.85,
                  0.5,
                  Math.max(this.holes, 0.2),
                  0.3,
                  Part.LEAVES
               )
            );
      }

      Colonizer grower = new Colonizer(step * 6.0, step * 1.3, step, new V3(0.0, g.tropism() * 0.4, 0.0), g.jitter(), this.random);

      for (double s = Math.max(0.15, g.sprout() * 0.8); s <= 1.001; s += step / stem) {
         grower.seed(t.at(s));
      }

      this.leaders(grower, t, g, lean, h * 0.3);
      grower.grow(points, (int)(c * 4.0 / step) + 40, 12000);
      this.branches(grower, r * 0.5, step, true, g);
   }

   private void leaders(Colonizer grower, TreeBuilder.Curve t, Growth g, double lean, double length) {
      for (int i = 1; i < g.forks(); i++) {
         double s = Math.max(0.3, g.split());
         double az = lean + Math.PI + (i - (g.forks() - 1) / 2.0) * this.range(1.0, 1.8);
         double rr = t.radius(s) * 0.75;
         TreeBuilder.Curve leader = this.grow(
            this.limb(t.at(s), az, this.range(0.75, 1.15), length * this.range(0.6, 0.9), rr, thin(rr * 0.5), 0.2, 0.25),
            Part.TRUNK,
            0.1 + this.p.gnarl() * 0.1
         );

         for (int k = 1; k < leader.points.size(); k++) {
            grower.seed(leader.points.get(k));
         }
      }
   }

   private void branches(Colonizer grower, double thickest, double step, boolean flatClumps, Growth g) {
      double[] pipe = grower.pipes(2.4);
      double widest = 1.0;

      for (int i = 0; i < grower.size; i++) {
         if (!grower.seed[i] && grower.parent[i] >= 0 && grower.seed[grower.parent[i]]) {
            widest = Math.max(widest, pipe[i]);
         }
      }

      double q = widest > 1.01 ? Math.log(Math.max(1.0, thickest / 0.5)) / Math.log(widest) : 0.0;
      double[] radius = new double[grower.size];

      for (int i = 0; i < grower.size; i++) {
         radius[i] = Math.min(thickest, 0.5 * Math.pow(pipe[i], q));
      }

      boolean[] tips = grower.tips();
      double clump = flatClumps ? Math.max(2.0, step * g.twig() * 1.3) : Math.max(1.6, step * g.twig() * 1.1);
      double lace = flatClumps ? Math.max(this.holes, 0.15) : this.holes;

      for (int i = 0; i < grower.size; i++) {
         int from = grower.parent[i];
         if (!grower.seed[i] && from >= 0) {
            double ra = grower.seed[from] ? radius[i] * 1.3 : Math.min(radius[from], radius[i] * 1.3);
            this.wood
               .add(new Shape.Tube(grower.at(from), grower.at(i), Math.max(0.5, ra), Math.max(0.5, radius[i]), Part.BRANCH, radius[i] > 1.5 ? 0.08 : 0.0));
            if (tips[i]) {
               double size = clump * this.range(0.8, 1.2);
               if (flatClumps) {
                  this.foliage.add(new Shape.Blob(grower.at(i).add(0.0, 0.6, 0.0), size, Math.max(1.1, size * 0.42), size, 0.35, lace, 0.35, Part.LEAVES));
               } else {
                  this.foliage.add(new Shape.Blob(grower.at(i), size, size * 0.8, size, 0.3, lace, 1.0, Part.LEAVES));
               }
            }
         }
      }
   }

   private void footing(Growth g, double r) {
      if (g.buttress() > 0.55) {
         this.buttresses(4 + (int)(g.buttress() * 2.5), r * 0.8, this.p.height() * (0.03 + 0.04 * g.buttress()));
      }

      this.roots(this.count(this.p.roots(), 5, 8), r * 0.7, 1.0 + g.buttress() * 0.4);
   }

   private static void fitLobes(List<double[]> lobes, V3 centre, double c) {
      double far = 0.0;

      for (double[] l : lobes) {
         far = Math.max(far, Math.hypot(l[0] - centre.x(), l[2] - centre.z()) + l[3]);
      }

      if (!(far <= c)) {
         double k = c / far;

         for (double[] l : lobes) {
            l[0] = centre.x() + (l[0] - centre.x()) * k;
            l[2] = centre.z() + (l[2] - centre.z()) * k;
            l[3] *= k;
         }
      }
   }

   private DoubleFunction<V3> bent(double height, double lean, Growth g) {
      V3 toward = V3.of(lean, 0.0);
      V3 side = V3.of(lean + (Math.PI / 2), 0.0);
      double swing = height * g.swing() * 0.22;
      double bends = g.bends();
      double drift = height * this.p.lean() * 0.3;
      double wobble = this.p.gnarl() * (1.0 + height * 0.03);
      long s = this.random.nextLong();
      return t -> {
         double k = Math.min(1.0, t * 3.0);
         double across = swing * StrictMath.sin(Math.PI * bends * t) * k;
         double along = swing * 0.5 * (1.0 - StrictMath.cos(Math.PI * bends * t)) + drift * t * t;
         return side.mul(across)
            .add(toward.mul(along))
            .add(wobble * k * Noise.smooth(s, t * 3.0, 0.0, 0.0), 0.0, wobble * k * Noise.smooth(s + 1L, t * 3.0, 0.0, 0.0));
      };
   }

   private TreeBuilder.Curve trunk(double base, double height, DoubleFunction<V3> drift, DoubleUnaryOperator radius) {
      TreeBuilder.Curve c = new TreeBuilder.Curve();
      int n = Math.max(6, (int)Math.ceil(height / 3.0));

      for (int i = 0; i <= n; i++) {
         double t = (double)i / n;
         c.add(drift.apply(t).add(0.0, base + t * height, 0.0), radius.applyAsDouble(t));
      }

      V3 foot = c.points.get(0);
      double r0 = c.radii.get(0);
      if (base <= 0.0) {
         this.wood.add(new Shape.Tube(foot.add(0.0, -(2.0 + r0 * 1.5), 0.0), foot, r0, r0, Part.TRUNK, 0.12));
         this.foot = Math.max(this.foot, Math.hypot(foot.x(), foot.z()) + r0);
      }

      this.grow(c, Part.TRUNK, 0.12 + this.p.gnarl() * 0.15);
      this.trunk = c;
      return c;
   }

   private DoubleFunction<V3> drift(double height, double lean, double gnarl, boolean sinuous) {
      double az = this.random.nextDouble() * (Math.PI * 2);
      double side = az + (Math.PI / 2);
      double phase = this.random.nextDouble() * (Math.PI * 2);
      long s = this.random.nextLong();
      double amount = lean * height * (sinuous ? 0.32 : 0.4);
      double wobble = gnarl * (1.2 + height * 0.04);
      return t -> {
         double along = sinuous ? amount * StrictMath.sin(4.241150082346221 * t) : amount * StrictMath.pow(t, 1.6);
         double across = sinuous ? amount * 0.45 * StrictMath.sin(7.225663103256523 * t + phase) - amount * 0.45 * StrictMath.sin(phase) : 0.0;
         double k = Math.min(1.0, t * 4.0) * wobble;
         double wx = k * Noise.smooth(s, t * 3.5, 0.0, 0.0);
         double wz = k * Noise.smooth(s + 1L, t * 3.5, 0.0, 0.0);
         return new V3(StrictMath.cos(az) * along + StrictMath.cos(side) * across + wx, 0.0, StrictMath.sin(az) * along + StrictMath.sin(side) * across + wz);
      };
   }

   private static DoubleUnaryOperator taper(double base, double top, double flare) {
      double f = flare * Math.min(1.0, Math.max(0.0, (base - 0.5) / 2.5));
      return t -> base + (top - base) * t + base * f * StrictMath.pow(Math.max(0.0, 1.0 - t / 0.14), 2.0);
   }

   private TreeBuilder.Curve limb(V3 start, double azimuth, double elevation, double length, double r0, double r1, double bend, double wiggle) {
      TreeBuilder.Curve c = new TreeBuilder.Curve();
      int n = Math.max(2, (int)Math.ceil(length / 2.5));
      V3 d = V3.of(azimuth, elevation);
      V3 pos = start;
      double step = length / n;
      c.add(pos, r0);

      for (int i = 1; i <= n; i++) {
         d = d.add(
               wiggle * (this.random.nextDouble() - 0.5), bend / n + wiggle * 0.5 * (this.random.nextDouble() - 0.5), wiggle * (this.random.nextDouble() - 0.5)
            )
            .normalize();
         pos = pos.add(d.mul(step));
         c.add(pos, r0 + (r1 - r0) * i / n);
      }

      return c;
   }

   private void roots(int count, double r, double spread) {
      for (int i = 0; i < count; i++) {
         double az = i * (Math.PI * 2) / count + this.range(-0.35, 0.35);
         double length = r * this.range(1.5, 2.4) * spread + this.p.height() * 0.01;
         double y0 = this.range(0.2, 0.8) + r * 0.3;
         double depth = this.range(1.2, 2.5) + r * 0.2;
         double phase = this.random.nextDouble() * (Math.PI * 2);
         TreeBuilder.Curve c = new TreeBuilder.Curve();
         int n = Math.max(4, (int)Math.ceil(length / 1.5));

         for (int j = 0; j <= n; j++) {
            double s = (double)j / n;
            double rho = r * 0.35 + length * s;
            double a = az + 0.2 * StrictMath.sin(s * Math.PI * 1.3 + phase);
            double y = y0 * StrictMath.pow(1.0 - s, 2.2) - 0.35 - depth * s * s * s;
            c.add(new V3(StrictMath.cos(a) * rho, y, StrictMath.sin(a) * rho), thin(r * 0.42 * (1.0 - s * 0.8)));
         }

         this.grow(c, Part.ROOT, 0.1);
         this.roots.add(c);
      }
   }

   private void veins(int count) {
      if (this.trunk != null) {
         for (int i = 0; i < count; i++) {
            double az = this.random.nextDouble() * (Math.PI * 2);
            double end = this.range(0.35, 0.85);
            TreeBuilder.Curve c = new TreeBuilder.Curve();

            for (int j = 0; j <= 24; j++) {
               double t = end * j / 24.0;
               az += this.range(-0.18, 0.18);
               double r = this.trunk.radius(t) - 0.2;
               c.add(this.trunk.at(t).add(StrictMath.cos(az) * r, 0.0, StrictMath.sin(az) * r), 0.6);
            }

            addTubes(this.veins, c, Part.VEIN, 0.0);
            if (i % 2 == 0 && i / 2 < this.roots.size()) {
               TreeBuilder.Curve root = this.roots.get(i / 2);
               TreeBuilder.Curve along = new TreeBuilder.Curve();

               for (int j = 0; j <= 10; j++) {
                  double s = 0.8 * j / 10.0;
                  along.add(root.at(s).add(0.0, root.radius(s) - 0.2, 0.0), 0.6);
               }

               addTubes(this.veins, along, Part.VEIN, 0.0);
            }
         }
      }
   }

   private void cloud(V3 c, double r, double flat) {
      r = Math.max(1.5, r);
      this.foliage.add(new Shape.Blob(c, r, r * flat, r, 0.3, this.holes, 1.0, Part.LEAVES));
      int lobes = 2 + (int)(r / 2.5);

      for (int i = 0; i < lobes; i++) {
         double a = this.random.nextDouble() * (Math.PI * 2);
         double d = r * this.range(0.35, 0.75);
         double rr = r * this.range(0.45, 0.7);
         V3 o = c.add(StrictMath.cos(a) * d, r * flat * this.range(-0.35, 0.35), StrictMath.sin(a) * d);
         this.foliage.add(new Shape.Blob(o, rr, rr * flat, rr, 0.3, this.holes, 1.0, Part.LEAVES));
      }
   }

   private void pad(V3 c, double r, double thick) {
      this.foliage.add(new Shape.Blob(c, r, thick, r, 0.35, this.holes, 0.45, Part.LEAVES));
      if (r > 3.0 && this.random.nextDouble() < 0.6) {
         double a = this.random.nextDouble() * (Math.PI * 2);
         this.foliage
            .add(
               new Shape.Blob(
                  c.add(StrictMath.cos(a) * r * 0.2, thick * 0.8, StrictMath.sin(a) * r * 0.2),
                  r * 0.55,
                  thick * 0.8,
                  r * 0.55,
                  0.3,
                  this.holes,
                  0.4,
                  Part.LEAVES
               )
            );
      }
   }

   private void strands(V3 c, double r, double below, int count, double length) {
      for (int i = 0; i < count; i++) {
         double a = this.random.nextDouble() * (Math.PI * 2);
         double u = Math.sqrt(this.random.nextDouble()) * 0.95;
         double x = Math.round(c.x() + StrictMath.cos(a) * u * r);
         double z = Math.round(c.z() + StrictMath.sin(a) * u * r);
         double top = Math.floor(c.y() - below * Math.sqrt(1.0 - u * u) * 0.7);
         double len = Math.min(length * this.range(0.35, 1.0), top - 1.5);
         if (len >= 1.0) {
            this.foliage.add(new Shape.Tube(new V3(x, top, z), new V3(x, top - Math.floor(len), z), 0.45, 0.45, Part.LEAVES, 0.0));
         }
      }
   }

   private TreeBuilder.Curve grow(TreeBuilder.Curve c, Part part, double bark) {
      addTubes(this.wood, c, part, bark);
      return c;
   }

   private static void addTubes(List<Shape> into, TreeBuilder.Curve c, Part part, double bark) {
      for (int i = 0; i + 1 < c.points.size(); i++) {
         into.add(new Shape.Tube(c.points.get(i), c.points.get(i + 1), c.radii.get(i), c.radii.get(i + 1), part, bark));
      }
   }

   private Tree fit() {
      double h = 1.0;
      double v = 1.0;
      Tree tree = new Tree(this.wood, this.veins, this.foliage, this.p.seed(), this.foot);

      for (int i = 0; i < 12 && (tree.reach() > this.p.reach() || tree.maxY() > this.p.ceiling()); i++) {
         if (tree.reach() > this.p.reach()) {
            h *= (this.p.reach() - 1.0) / tree.reach();
         }

         if (tree.maxY() > this.p.ceiling()) {
            v *= Math.max(0.05, (this.p.ceiling() - 1.0) / tree.maxY());
         }

         tree = new Tree(scale(this.wood, h, v), scale(this.veins, h, v), scale(this.foliage, h, v), this.p.seed(), this.foot * h);
      }

      return tree;
   }

   private static List<Shape> scale(List<Shape> shapes, double h, double v) {
      return shapes.stream().map(s -> s.scaled(h, v)).toList();
   }

   private int count(int value, int lo, int hi) {
      return value >= 0 ? value : lo + this.random.nextInt(hi - lo + 1);
   }

   private double range(double lo, double hi) {
      return lo + this.random.nextDouble() * (hi - lo);
   }

   private static double thin(double r) {
      return Math.max(0.55, r);
   }

   private static final class Curve {
      final List<V3> points = new ArrayList<>();
      final List<Double> radii = new ArrayList<>();

      void add(V3 point, double radius) {
         this.points.add(point);
         this.radii.add(radius);
      }

      V3 at(double t) {
         double f = Math.max(0.0, Math.min(1.0, t)) * (this.points.size() - 1);
         int i = Math.min((int)f, this.points.size() - 2);
         return this.points.get(i).lerp(this.points.get(i + 1), f - i);
      }

      double radius(double t) {
         double f = Math.max(0.0, Math.min(1.0, t)) * (this.radii.size() - 1);
         int i = Math.min((int)f, this.radii.size() - 2);
         return this.radii.get(i) + (this.radii.get(i + 1) - this.radii.get(i)) * (f - i);
      }

      V3 end() {
         return this.points.get(this.points.size() - 1);
      }
   }
}

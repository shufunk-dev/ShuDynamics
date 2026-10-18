package com.vexira.mysticalterrain.formation;

import com.vexira.mysticalterrain.tree.V3;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

public final class FormationBuilder {
   private static final double TAU = Math.PI * 2;
   private static final double INF = Double.POSITIVE_INFINITY;
   private static final double SQUARE = -Math.PI / 4;
   private final FormationParams p;
   private final SplittableRandom random;
   private final List<Solid> solids = new ArrayList<>();
   private final List<int[]> chests = new ArrayList<>();

   private FormationBuilder(FormationParams p) {
      this.p = p;
      this.random = new SplittableRandom(p.seed());
   }

   public static Formation build(FormationParams params) {
      FormationBuilder b = new FormationBuilder(params);
      switch (params.kind()) {
         case SPIRES:
            b.spires();
            break;
         case SHARDS:
            b.shards();
            break;
         case MONOLITHS:
            b.monoliths();
            break;
         case FOSSIL:
            b.fossil();
            break;
         case CRATER:
            b.crater();
            break;
         case RUINS:
            b.ruins();
            break;
         case SHRINE:
            b.shrine();
            break;
         case GATE:
            b.gate();
            break;
         case ISLAND:
            b.island();
            break;
         case GEODE:
            b.geode();
            break;
         case STAR:
            b.star();
            break;
         case REEF:
            b.reef();
      }

      return new Formation(b.solids, b.chests, params.seed());
   }

   private void spires() {
      double h = this.p.size();
      double r = Math.max(1.3, h * 0.075) * this.p.girth();
      this.solids.add(new Solid.Ellipsoid(new V3(0.0, -r * 0.6, 0.0), r * 2.6, r * 1.4, r * 2.6, 0.35, 0.0, Material.BASE, null, r * 0.5, null, true));
      int n = this.count(4, 7);
      double az0 = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < n; i++) {
         boolean main = i == 0;
         double hi = main ? h : h * this.range(0.3, 0.8);
         double ri = main ? r : Math.max(1.0, r * Math.pow(hi / h, 0.8) * this.range(0.8, 1.1));
         double az = main ? az0 : az0 + i * (Math.PI * 2) / (n - 1) + this.range(-0.4, 0.4);
         double tilt = this.p.lean() * (main ? this.range(0.0, 0.25) : this.range(0.3, 0.8));
         double off = main ? 0.0 : r * this.range(0.8, 1.8);
         V3 start = new V3(Math.cos(az) * off, -ri * 1.5, Math.sin(az) * off);
         this.crystal(start, tilted(az, tilt), hi * 0.8 + ri * 1.5, ri, hi * 0.2 + ri);
      }

      int small = 3 + this.random.nextInt(4) + (int)(h / 15.0);

      for (int i = 0; i < small; i++) {
         double az = this.random.nextDouble() * (Math.PI * 2);
         double dist = r * this.range(1.8, 3.2);
         double hs = this.range(3.0, 4.0 + h * 0.12);
         this.crystal(
            new V3(Math.cos(az) * dist, -1.0, Math.sin(az) * dist), tilted(az, this.range(0.3, 0.9)), hs * 0.7, Math.max(0.7, hs * 0.12), hs * 0.3 + 1.0
         );
      }
   }

   private void shards() {
      double len = this.p.size();
      int n = this.count(2, 5);

      for (int i = 0; i < n; i++) {
         double l = len * this.range(0.5, 1.0);
         double r = Math.max(1.1, l * 0.14) * this.p.girth();
         double az = this.random.nextDouble() * (Math.PI * 2);
         double spot = this.random.nextDouble() * (Math.PI * 2);
         double off = len * 0.3 * this.random.nextDouble();
         V3 start = new V3(Math.cos(spot) * off, -r * 0.7, Math.sin(spot) * off);
         double tip = this.random.nextDouble() < 0.3 ? 0.0 : r * 1.8;
         this.solids
            .add(
               new Solid.Prism(
                  start,
                  start.add(tilted(az, this.range(1.05, 1.45)).mul(l)),
                  r,
                  this.p.sides(),
                  this.random.nextDouble() * (Math.PI * 2),
                  tip,
                  1.0,
                  Material.LIGHT,
                  Material.BODY,
                  Material.SHELL,
                  Material.TIP,
                  true
               )
            );
      }

      int upright = 2 + this.random.nextInt(4);

      for (int i = 0; i < upright; i++) {
         double az = this.random.nextDouble() * (Math.PI * 2);
         double dist = len * this.range(0.2, 0.6);
         double hs = this.range(2.0, 3.0 + len * 0.2);
         this.crystal(
            new V3(Math.cos(az) * dist, -1.0, Math.sin(az) * dist), tilted(az, this.range(0.1, 0.6)), hs * 0.7, Math.max(0.7, hs * 0.13), hs * 0.3 + 1.0
         );
      }

      this.scatter(Material.BASE, 3 + this.random.nextInt(5), len * 0.2, len * 0.8, 0.7, 1.6);
   }

   private void monoliths() {
      double ring = this.p.size();
      int n = this.count(5, 8);
      double h = ring * this.range(1.4, 2.0);
      double w = Math.max(1.0, ring * 0.12) * this.p.girth();
      this.solids.add(new Solid.Ellipsoid(new V3(0.0, -1.0, 0.0), ring + 3.0, 1.6, ring + 3.0, 0.15, 0.0, Material.BASE, null, -1.0, null, true));
      this.solids.add(new Solid.Ellipsoid(new V3(0.0, -1.0, 0.0), 3.5, 1.5, 3.5, 0.0, 0.0, Material.ACCENT, null, -1.0, null, true));
      this.solids.add(new Solid.Box(-1, 0, -1, 1, 0, 1, 0.0, Material.BODY, true));
      this.chests.add(new int[]{0, 1, 0});
      double az0 = this.random.nextDouble() * (Math.PI * 2);

      for (int i = 0; i < n; i++) {
         double az = az0 + i * (Math.PI * 2) / n + this.range(-0.15, 0.15);
         V3 foot = new V3(Math.cos(az) * ring, 0.0, Math.sin(az) * ring);
         double hi = h * this.range(0.75, 1.0);
         if (this.random.nextDouble() < 0.2) {
            V3 start = foot.add(0.0, w * 0.6, 0.0);
            V3 outward = new V3(Math.cos(az), 0.0, Math.sin(az));
            this.solids
               .add(
                  new Solid.Prism(
                     start,
                     start.add(outward.mul(hi * 0.8)),
                     w,
                     4,
                     -Math.PI / 4,
                     w * 2.0,
                     0.8,
                     Material.BODY,
                     Material.BODY,
                     Material.SHELL,
                     Material.TIP,
                     true
                  )
               );
         } else {
            this.solids
               .add(
                  new Solid.Prism(
                     foot.add(0.0, -3.0, 0.0),
                     foot.add(0.0, hi, 0.0),
                     w,
                     4,
                     -Math.PI / 4,
                     w * 2.0,
                     0.8,
                     Material.BODY,
                     Material.BODY,
                     Material.SHELL,
                     Material.TIP,
                     true
                  )
               );
         }
      }
   }

   private void fossil() {
      double len = this.p.size();
      double s = Math.max(1.4, len * 0.07) * this.p.girth();
      double heading = this.random.nextDouble() * (Math.PI * 2);
      V3 fwd = new V3(Math.cos(heading), 0.0, Math.sin(heading));
      V3 side = new V3(-fwd.z(), 0.0, fwd.x());
      double sway = this.range(-1.0, 1.0) * len * 0.08;
      double phase = this.random.nextDouble() * (Math.PI * 2);
      int bones = Math.max(6, (int)(len / (s * 1.6)));
      List<V3> spine = new ArrayList<>();

      for (int i = 0; i <= bones; i++) {
         double t = (double)i / bones;
         double lateral = Math.sin(t * Math.PI * 0.9 + phase) * sway;
         double up = -s * 0.3 + Math.sin(t * Math.PI) * s * 1.6;
         spine.add(fwd.mul((t - 0.5) * len).add(side.mul(lateral)).add(0.0, up, 0.0));
      }

      for (int i = 0; i <= bones; i++) {
         double taper = Math.min(1.0, 0.5 + i / (bones * 0.5));
         V3 c = spine.get(i);
         this.solids
            .add(
               new Solid.Ellipsoid(
                  c,
                  s * taper,
                  s * 0.9 * taper,
                  s * taper,
                  0.15,
                  0.0,
                  i % 3 == 0 ? Material.ACCENT : Material.BODY,
                  null,
                  Double.POSITIVE_INFINITY,
                  null,
                  false
               )
            );
         if (i > 0) {
            this.solids.add(new Solid.Tube(spine.get(i - 1), c, s * 0.45 * taper, s * 0.45 * taper, Material.BODY, false));
         }
      }

      for (int i = (int)(bones * 0.3); i <= (int)(bones * 0.75); i++) {
         double t = (double)i / bones;
         double rr = s * this.range(3.2, 4.2) * (1.0 - Math.abs(t - 0.52) * 1.2);
         V3 centre = spine.get(i).add(0.0, -rr * 0.35, 0.0);

         for (int sign = -1; sign <= 1; sign += 2) {
            V3 last = null;

            for (int k = 0; k <= 6; k++) {
               double phi = Math.PI * (0.15 + 0.8 * k / 6.0);
               V3 pt = centre.add(side.mul(sign * rr * Math.sin(phi) * 1.1)).add(0.0, rr * Math.cos(phi), 0.0);
               if (last != null) {
                  this.solids.add(new Solid.Tube(last, pt, s * 0.32, s * 0.26, Material.BODY, false));
               }

               last = pt;
            }
         }
      }

      V3 head = spine.get(bones).add(fwd.mul(s * 2.2)).add(0.0, s * 0.4, 0.0);
      this.solids.add(new Solid.Ellipsoid(head, s * 2.4, s * 1.7, s * 1.9, 0.12, 1.3, Material.BODY, null, Double.POSITIVE_INFINITY, null, false));
      this.solids
         .add(
            new Solid.Tube(
               head.add(fwd.mul(-s)).add(0.0, -s * 0.9, 0.0), head.add(fwd.mul(s * 3.2)).add(0.0, -s * 1.4, 0.0), s * 0.5, s * 0.35, Material.BODY, false
            )
         );

      for (int sign = -1; sign <= 1; sign += 2) {
         V3 eye = head.add(fwd.mul(s * 0.9)).add(side.mul(sign * s * 1.3)).add(0.0, s * 0.5, 0.0);
         this.solids.add(new Solid.Ellipsoid(eye, s * 0.6, s * 0.55, s * 0.6, 0.0, 0.0, Material.AIR, null, Double.POSITIVE_INFINITY, null, true));
         V3 root = head.add(fwd.mul(-s * 0.8)).add(side.mul(sign * s * 1.2)).add(0.0, s * 1.2, 0.0);
         V3 bend = root.add(fwd.mul(-s * 1.8)).add(side.mul(sign * s * 1.2)).add(0.0, s * 1.6, 0.0);
         V3 end = bend.add(fwd.mul(-s * 2.2)).add(side.mul(sign * s * 0.3)).add(0.0, s * 0.2, 0.0);
         this.solids.add(new Solid.Tube(root, bend, s * 0.5, s * 0.35, Material.ACCENT, false));
         this.solids.add(new Solid.Tube(bend, end, s * 0.35, 0.5, Material.ACCENT, false));
      }
   }

   private void crater() {
      double r = this.p.size();
      double depth = r * this.range(0.35, 0.5);
      double lift = r * 0.3;
      V3 c = new V3(0.0, lift, 0.0);
      this.solids.add(new Solid.Ellipsoid(c, r, lift + depth, r, 0.12, 0.0, Material.AIR, null, Double.POSITIVE_INFINITY, null, true));
      this.solids.add(new Solid.Ellipsoid(c, r + 1.5, lift + depth + 1.5, r + 1.5, 0.12, 2.0, Material.BASE, null, -1.0, null, true));
      double mr = Math.max(1.5, r * 0.22) * this.p.girth();
      V3 rock = new V3(this.range(-0.2, 0.2) * r, -depth + mr * 0.5, this.range(-0.2, 0.2) * r);
      this.solids.add(new Solid.Ellipsoid(rock, mr, mr * 0.85, mr, 0.35, 0.0, Material.BODY, null, Double.POSITIVE_INFINITY, null, true));
      this.scatter(Material.BASE, 8 + (int)r, r * 0.9, r * 1.3, r * 0.06 + 0.8, r * 0.14 + 0.8);
      this.scatter(Material.BODY, 2 + this.random.nextInt(4), r * 1.3, r * 2.0, 0.8, 1.4);
   }

   private void ruins() {
      int s = this.p.size();
      this.solids.add(new Solid.Box(-s, -1, -s, s, -1, s, 0.3, Material.BASE, true));
      int towers = this.count(1, 3);

      for (int i = 0; i < towers; i++) {
         double az = this.random.nextDouble() * (Math.PI * 2);
         double d = i == 0 ? 0.0 : s * this.range(0.4, 0.7);
         int cx = (int)Math.round(Math.cos(az) * d);
         int cz = (int)Math.round(Math.sin(az) * d);
         V3 c = new V3(cx, -1.0, cz);
         double tr = this.range(2.5, 4.5) * this.p.girth();
         this.solids.add(new Solid.Ring(c, tr, tr - 1.3, s * this.range(0.8, 1.6), 0.55, Material.BODY, true));
         this.solids.add(new Solid.Ellipsoid(c, tr - 0.6, 0.6, tr - 0.6, 0.0, 0.0, Material.BASE, null, -1.0, null, true));
         int door = (int)Math.round(tr);
         this.solids.add(new Solid.Box(cx + door - 2, 0, cz - 1, cx + door + 1, 2, cz + 1, 0.0, Material.AIR, true));
         if (i == 0) {
            this.chests.add(new int[]{cx, 0, cz});
         }
      }

      int columns = 4 + this.random.nextInt(5);
      double heading = this.random.nextDouble() * (Math.PI * 2);
      double cos = Math.cos(heading);
      double sin = Math.sin(heading);

      for (int i = 0; i < columns; i++) {
         double along = (i - (columns - 1) / 2.0) * 4.0;
         int x = (int)Math.round(cos * along + sin * s * 0.6);
         int z = (int)Math.round(sin * along - cos * s * 0.6);
         int top = this.random.nextDouble() < 0.35 ? 1 + this.random.nextInt(3) : (int)(s * this.range(0.5, 0.9)) + 3;
         this.solids.add(new Solid.Box(x, -1, z, x + 1, top, z + 1, 0.0, Material.ACCENT, true));
         if (top > 4) {
            this.solids.add(new Solid.Box(x, top + 1, z, x, top + 1, z, 0.0, Material.LIGHT, false));
         }
      }

      int fx = (int)Math.round(-sin * s * 0.5);
      int fz = (int)Math.round(cos * s * 0.5);
      int fallen = (int)(s * this.range(0.5, 0.8)) + 3;
      if (this.random.nextBoolean()) {
         this.solids.add(new Solid.Box(fx, 0, fz, fx + fallen, 1, fz + 1, 0.0, Material.ACCENT, false));
      } else {
         this.solids.add(new Solid.Box(fx, 0, fz, fx + 1, 1, fz + fallen, 0.0, Material.ACCENT, false));
      }
   }

   private void shrine() {
      double s = this.p.size();
      this.solids
         .add(
            new Solid.Prism(
               new V3(0.0, -4.0, 0.0),
               new V3(0.0, -1.0, 0.0),
               s + 3.0,
               8,
               Math.PI / 8,
               0.0,
               0.0,
               Material.BASE,
               Material.BASE,
               Material.BASE,
               Material.BASE,
               true
            )
         );
      this.solids
         .add(
            new Solid.Prism(
               new V3(0.0, -4.0, 0.0),
               new V3(0.0, 0.0, 0.0),
               s + 1.5,
               8,
               Math.PI / 8,
               0.0,
               0.0,
               Material.BASE,
               Material.BASE,
               Material.BASE,
               Material.BASE,
               true
            )
         );
      this.solids
         .add(
            new Solid.Prism(
               new V3(0.0, -1.0, 0.0),
               new V3(0.0, 0.0, 0.0),
               s * 0.45,
               8,
               Math.PI / 8,
               0.0,
               0.0,
               Material.ACCENT,
               Material.ACCENT,
               Material.ACCENT,
               Material.ACCENT,
               true
            )
         );
      int pillars = this.count(6, 8);
      double width = s > 7.0 ? 1.2 : 0.8;

      for (int i = 0; i < pillars; i++) {
         double az = (i + 0.5) * (Math.PI * 2) / pillars;
         int x = (int)Math.round(Math.cos(az) * (s - 0.5));
         int z = (int)Math.round(Math.sin(az) * (s - 0.5));
         int top = (int)(s * this.range(0.9, 1.2)) + 3;
         this.solids
            .add(
               new Solid.Prism(
                  new V3(x, 1.0, z), new V3(x, top, z), width, 4, -Math.PI / 4, 0.0, 0.0, Material.BODY, Material.BODY, Material.BODY, Material.BODY, true
               )
            );
         this.solids
            .add(
               new Solid.Prism(
                  new V3(x, top + 1, z),
                  new V3(x, top + 2, z),
                  0.9,
                  6,
                  this.random.nextDouble() * (Math.PI * 2),
                  2.5,
                  1.0,
                  Material.TIP,
                  Material.TIP,
                  Material.TIP,
                  Material.TIP,
                  false
               )
            );
      }

      this.solids.add(new Solid.Box(-1, 1, -1, 1, 1, 1, 0.0, Material.ACCENT, true));
      this.chests.add(new int[]{0, 2, 0});
      double y = 6.0 + s * 0.3;
      double twist = this.random.nextDouble() * (Math.PI * 2);
      this.solids
         .add(
            new Solid.Prism(
               new V3(0.0, y, 0.0), new V3(0.0, y + s * 0.5, 0.0), 1.3, 6, twist, 3.0, 1.0, Material.LIGHT, Material.SHELL, Material.SHELL, Material.TIP, false
            )
         );
      this.solids
         .add(
            new Solid.Prism(
               new V3(0.0, y + 0.5, 0.0),
               new V3(0.0, y - 0.5, 0.0),
               1.3,
               6,
               twist,
               3.0,
               1.0,
               Material.LIGHT,
               Material.SHELL,
               Material.SHELL,
               Material.TIP,
               false
            )
         );
   }

   private void gate() {
      double half = this.p.size();
      double h = half * this.range(1.5, 2.1);
      double w = Math.max(1.0, half * 0.2) * this.p.girth();
      boolean alongZ = this.random.nextBoolean();
      int ends = (int)half + 3;
      this.solids
         .add(
            alongZ
               ? new Solid.Box(-3, -1, -ends, 3, -1, ends, 0.15, Material.BASE, true)
               : new Solid.Box(-ends, -1, -3, ends, -1, 3, 0.15, Material.BASE, true)
         );

      for (int sign = -1; sign <= 1; sign += 2) {
         this.solids
            .add(
               new Solid.Prism(
                  across(sign * half, -3.0, alongZ),
                  across(sign * half, h, alongZ),
                  w + 0.4,
                  4,
                  -Math.PI / 4,
                  0.0,
                  1.0,
                  Material.BODY,
                  Material.BODY,
                  Material.SHELL,
                  Material.SHELL,
                  true
               )
            );
      }

      V3 last = null;

      for (int k = 0; k <= 12; k++) {
         double phi = Math.PI * k / 12.0;
         V3 pt = across(half * Math.cos(phi), h + half * 0.8 * Math.sin(phi), alongZ);
         if (last != null) {
            this.solids.add(new Solid.Tube(last, pt, w, w, Material.BODY, true));
         }

         last = pt;
      }

      double crown = h + half * 0.8;
      this.solids
         .add(new Solid.Ellipsoid(new V3(0.0, crown, 0.0), w * 1.4, w * 1.4, w * 1.4, 0.1, 0.0, Material.ACCENT, null, Double.POSITIVE_INFINITY, null, true));
      int hang = (int)Math.floor(crown - w * 1.4) - 1;
      this.solids.add(new Solid.Box(0, hang + 1, 0, 0, (int)Math.floor(crown), 0, 0.0, Material.ACCENT, true));
      this.solids.add(new Solid.Box(0, hang, 0, 0, hang, 0, 0.0, Material.LIGHT, false));
      this.scatter(Material.BASE, 3 + this.random.nextInt(4), half * 0.5, half * 1.5, 0.7, 1.5);
   }

   private void island() {
      double r = this.p.size();
      double t = r * this.range(0.9, 1.4);
      this.solids.add(new Solid.Ellipsoid(V3.ZERO, r, t, r * this.range(0.8, 1.2), 0.35, 0.0, Material.BODY, null, 0.0, Material.SHELL, false));
      int drips = 3 + this.random.nextInt(5);

      for (int i = 0; i < drips; i++) {
         double az = this.random.nextDouble() * (Math.PI * 2);
         double d = r * 0.55 * this.random.nextDouble();
         V3 top = new V3(Math.cos(az) * d, -t * 0.45, Math.sin(az) * d);
         V3 end = top.add(0.0, -this.range(2.0, 2.0 + r * 0.8), 0.0);
         this.solids.add(new Solid.Tube(top, end, this.range(1.0, 1.8), 0.4, Material.BODY, false));
         if (this.random.nextBoolean()) {
            this.solids.add(new Solid.Ellipsoid(end, 0.8, 0.8, 0.8, 0.0, 0.0, Material.LIGHT, null, Double.POSITIVE_INFINITY, null, false));
         }
      }

      int crystals = this.random.nextInt(4);

      for (int i = 0; i < crystals; i++) {
         double az = this.random.nextDouble() * (Math.PI * 2);
         double d = r * 0.6 * this.random.nextDouble();
         double hs = this.range(2.0, 5.0);
         this.crystal(new V3(Math.cos(az) * d, 0.0, Math.sin(az) * d), tilted(az, this.range(0.0, 0.5)), hs * 0.7, 0.8, hs * 0.3 + 1.0);
      }
   }

   private void geode() {
      double r = this.p.size();
      this.solids.add(new Solid.Ellipsoid(V3.ZERO, r, r * 0.92, r, 0.1, 1.2, Material.BASE, null, Double.POSITIVE_INFINITY, null, true));
      this.solids.add(new Solid.Ellipsoid(V3.ZERO, r - 1.2, (r - 1.2) * 0.92, r - 1.2, 0.1, 1.0, Material.SHELL, null, Double.POSITIVE_INFINITY, null, true));
      this.solids
         .add(new Solid.Ellipsoid(V3.ZERO, r - 2.2, (r - 2.2) * 0.92, r - 2.2, 0.1, 1.0, Material.BODY, Material.AIR, Double.POSITIVE_INFINITY, null, true));
      V3 out = V3.of(this.random.nextDouble() * (Math.PI * 2), this.range(-0.2, 0.6)).mul(r * 1.3);
      this.solids.add(new Solid.Tube(out, out.mul(0.25), r * 0.45, r * 0.3, Material.AIR, true));
      if (r >= 5.0) {
         double twist = this.random.nextDouble() * (Math.PI * 2);
         this.solids
            .add(
               new Solid.Prism(
                  new V3(0.0, -0.5, 0.0), new V3(0.0, 0.5, 0.0), 0.9, 6, twist, r * 0.35, 1.0, Material.TIP, Material.TIP, Material.TIP, Material.TIP, false
               )
            );
         this.solids
            .add(
               new Solid.Prism(
                  new V3(0.0, 0.5, 0.0), new V3(0.0, -0.5, 0.0), 0.9, 6, twist, r * 0.35, 1.0, Material.TIP, Material.TIP, Material.TIP, Material.TIP, false
               )
            );
      }
   }

   private void star() {
      double r = this.p.size() * this.p.girth();
      this.solids.add(new Solid.Ellipsoid(V3.ZERO, r, r, r, 0.2, 0.0, Material.LIGHT, null, Double.POSITIVE_INFINITY, null, false));
      this.solids.add(new Solid.Ellipsoid(V3.ZERO, r + 1.6, r + 1.6, r + 1.6, 0.2, 1.0, Material.SHELL, null, Double.POSITIVE_INFINITY, null, false));
      int rays = 4 + this.random.nextInt(4);

      for (int i = 0; i < rays; i++) {
         V3 dir = V3.of(this.random.nextDouble() * (Math.PI * 2), this.range(-1.2, 1.2));
         this.solids.add(new Solid.Tube(dir.mul(r), dir.mul(r * this.range(2.2, 3.6) + 2.0), 0.9, 0.3, Material.TIP, false));
      }

      if (this.random.nextBoolean()) {
         V3 normal = V3.of(this.random.nextDouble() * (Math.PI * 2), this.range(0.6, 1.4));
         V3 u = normal.cross(Math.abs(normal.y()) > 0.9 ? new V3(1.0, 0.0, 0.0) : V3.UP).normalize();
         V3 v = normal.cross(u);
         double orbit = r * 2.6 + 1.0;
         int beads = (int)(orbit * 3.0);

         for (int k = 0; k < beads; k++) {
            double a = (Math.PI * 2) * k / beads;
            this.solids
               .add(
                  new Solid.Ellipsoid(
                     u.mul(Math.cos(a) * orbit).add(v.mul(Math.sin(a) * orbit)),
                     0.7,
                     0.7,
                     0.7,
                     0.0,
                     0.0,
                     Material.ACCENT,
                     null,
                     Double.POSITIVE_INFINITY,
                     null,
                     false
                  )
               );
         }
      }

      int moons = this.random.nextInt(4);

      for (int i = 0; i < moons; i++) {
         double mr = this.range(0.8, 1.4);
         this.solids
            .add(
               new Solid.Ellipsoid(
                  V3.of(this.random.nextDouble() * (Math.PI * 2), this.range(-1.0, 1.0)).mul(r * this.range(3.0, 5.0) + 2.0),
                  mr,
                  mr,
                  mr,
                  0.2,
                  0.0,
                  Material.LIGHT,
                  null,
                  Double.POSITIVE_INFINITY,
                  null,
                  false
               )
            );
      }
   }

   private void reef() {
      double h = this.p.size();
      this.solids
         .add(new Solid.Ellipsoid(new V3(0.0, -1.0, 0.0), h * 0.35 + 1.0, h * 0.2 + 1.0, h * 0.35 + 1.0, 0.3, 0.0, Material.BASE, null, 0.0, null, true));
      int stalks = this.count(3, 6);

      for (int i = 0; i < stalks; i++) {
         double az = this.random.nextDouble() * (Math.PI * 2);
         V3 start = new V3(Math.cos(az) * h * 0.12, -1.0, Math.sin(az) * h * 0.12);
         this.branch(start, V3.of(az, this.range(1.0, 1.4)), h * this.range(0.35, 0.55), Math.max(0.8, h * 0.06) * this.p.girth(), 0);
      }
   }

   private void branch(V3 start, V3 dir, double len, double r, int depth) {
      V3 end = start.add(dir.mul(len));
      this.solids.add(new Solid.Tube(start, end, r, r * 0.75, Material.BODY, true));
      if (depth < 2 && len > 3.0) {
         int kids = this.random.nextDouble() < 0.3 ? 3 : 2;
         double elevation = Math.asin(Math.max(-1.0, Math.min(1.0, dir.y())));

         for (int k = 0; k < kids; k++) {
            V3 child = V3.of(dir.azimuth() + this.range(-1.2, 1.2), Math.max(0.3, Math.min(1.5, elevation + this.range(-0.3, 0.25))));
            this.branch(end, child, len * this.range(0.55, 0.75), Math.max(0.6, r * 0.7), depth + 1);
         }
      } else {
         double bulb = this.range(1.1, 2.1);
         this.solids
            .add(
               new Solid.Ellipsoid(
                  end, bulb, bulb, bulb, 0.2, 0.0, this.random.nextBoolean() ? Material.LIGHT : Material.ACCENT, null, Double.POSITIVE_INFINITY, null, false
               )
            );
      }
   }

   private void crystal(V3 start, V3 dir, double length, double r, double tip) {
      this.solids
         .add(
            new Solid.Prism(
               start,
               start.add(dir.mul(length)),
               r,
               this.p.sides(),
               this.random.nextDouble() * (Math.PI * 2),
               tip,
               1.0,
               Material.LIGHT,
               Material.BODY,
               Material.SHELL,
               Material.TIP,
               true
            )
         );
   }

   private void scatter(Material material, int n, double near, double far, double small, double big) {
      for (int i = 0; i < n; i++) {
         double az = this.random.nextDouble() * (Math.PI * 2);
         double d = this.range(near, far);
         double size = this.range(small, big);
         this.solids
            .add(
               new Solid.Ellipsoid(
                  new V3(Math.cos(az) * d, this.range(-0.5, 0.5), Math.sin(az) * d),
                  size * 1.3,
                  size,
                  size * 1.3,
                  0.3,
                  0.0,
                  material,
                  null,
                  Double.POSITIVE_INFINITY,
                  null,
                  false
               )
            );
      }
   }

   private static V3 tilted(double az, double tilt) {
      return new V3(Math.cos(az) * Math.sin(tilt), Math.cos(tilt), Math.sin(az) * Math.sin(tilt));
   }

   private static V3 across(double offset, double y, boolean alongZ) {
      return alongZ ? new V3(0.0, y, offset) : new V3(offset, y, 0.0);
   }

   private int count(int lo, int hi) {
      return this.p.count() >= 0 ? this.p.count() : lo + this.random.nextInt(hi - lo + 1);
   }

   private double range(double lo, double hi) {
      return lo + this.random.nextDouble() * (hi - lo);
   }
}

package com.vexira.mysticalterrain.client.vfx;

import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.boss.vfx.VfxCastPayload;
import com.vexira.mysticalterrain.boss.vfx.VfxOptions;
import com.vexira.mysticalterrain.boss.vfx.VfxParticles;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
final class VfxCasts {
   private static final int WHITE = 16777215;
   private final VfxCastPayload p;
   private final Random r;
   private final int core;
   private final int glow;
   private final int smoke;
   private final int debris;
   private final float density;

   private VfxCasts(VfxCastPayload p) {
      this.p = p;
      this.r = Random.create(p.seed());
      this.core = p.core() & 16777215;
      this.glow = p.glow() & 16777215;
      this.smoke = p.smoke() & 16777215;
      this.debris = p.debris();
      this.density = VfxClient.density();
   }

   static void play(VfxCastPayload p) {
      if (MinecraftClient.getInstance().world != null && !p.points().isEmpty()) {
         new VfxCasts(p).run();
      }
   }

   private void run() {
      Vec3d a = this.p.point(0);
      switch (this.p.effect()) {
         case SPARKS:
            this.sparks(a, this.core, (int)this.arg(0), this.arg(1), this.arg(2));
            break;
         case EMBERS:
            this.embers(a, this.core, (int)this.arg(0), this.arg(1));
            break;
         case SMOKE:
            this.smoke(a, this.core, (int)this.arg(0), this.arg(1), this.arg(2));
            break;
         case SHARDS:
            this.shards(a, this.core, (int)this.arg(0), this.arg(1));
            break;
         case DEBRIS:
            this.debris(a, this.core, (int)this.arg(0), this.arg(1), (int)this.arg(2));
            break;
         case GLINT:
            this.glint(a, this.core, this.size(0));
            break;
         case IMPACT:
            this.impact(a, MathHelper.clamp(this.arg(0), 0.1F, 4.0F));
            break;
         case SHOCKWAVE:
            this.shockwave(a, this.size(0));
            break;
         case BEAM:
            this.beam(a, this.p.point(1), this.size(0), (int)this.arg(1));
            break;
         case GATHER:
            this.gather(a, this.size(0), (int)this.arg(1));
            break;
         case PILLAR:
            this.pillar(a, this.size(0), this.size(1));
            break;
         case EXPLOSION:
            this.explosion(a, this.size(0));
            break;
         case AURA:
            this.aura(a, this.size(0));
            break;
         case TRAIL:
            flare(a, this.glow, this.size(0) * 1.8, 8);
            flare(a, this.core, this.size(0), 5);
            break;
         case SWING:
            this.slashArc(a.add(0.0, 0.9, 0.0), this.arg(0), 0.0F, 0.0F, this.size(1), 160.0F);
            break;
         case TELEGRAPH_CIRCLE:
            this.telegraphCircle(a, this.size(0), this.ticks(1));
            break;
         case TELEGRAPH_LINE:
            this.telegraphLine(a, this.vec(1), this.size(0), this.size(1), this.ticks(2));
            break;
         case TELEGRAPH_CONE:
            this.telegraphCone(a, this.arg(0), MathHelper.clamp(this.arg(1), 5.0F, 180.0F), this.size(2), this.ticks(3));
            break;
         case LIGHTNING:
            this.lightning(a, this.p.point(1), MathHelper.clamp((int)this.arg(0), 0, 6), true);
            break;
         case CHAIN_LIGHTNING:
            for (int i = 0; i + 1 < this.p.points().size(); i++) {
               this.lightning(this.p.point(i), this.p.point(i + 1), 1, false);
               flare(this.p.point(i + 1), this.glow, 1.6, 6);
            }
            break;
         case VORTEX:
            this.vortex(a, this.size(0), this.size(1), this.ticks(2));
            break;
         case NOVA:
            this.nova(a, this.size(0));
            break;
         case RUNE_CIRCLE:
            this.runeCircle(a, this.size(0), this.ticks(1), this.arg(2), MathHelper.clamp((int)this.arg(3), 0, 3));
            break;
         case CHARGE_UP:
            this.chargeUp(a, this.ticks(0));
            break;
         case METEOR_TRAIL:
            this.meteorTrail(a, this.vec(1));
            break;
         case GROUND_CRACK:
            this.groundCrack(a, this.size(0));
            break;
         case HEAVY_IMPACT:
            this.heavyImpact(a, MathHelper.clamp(this.arg(0), 0.2F, 4.0F));
            break;
         case SHIELD_BUBBLE:
            this.shieldBubble(a, this.size(0), this.ticks(1));
            break;
         case WARD:
            this.ward(a, this.size(0), this.ticks(1), MathHelper.clamp((int)this.arg(2), 0, 3));
            break;
         case DASH_TRAIL:
            this.dashTrail(a, this.p.point(1));
            break;
         case AURA_RING:
            this.auraRing(a, this.size(0) > 0.0 ? this.size(0) : 1.2);
            break;
         case WISPS:
            this.wisps(a, (int)this.arg(0), this.size(1) > 0.0 ? this.size(1) : 1.6, Math.max(10, this.ticks(2)));
            break;
         case SLASH_ARC:
            this.slashArc(a, this.arg(0), this.arg(1), this.arg(2), this.size(3), this.arg(4));
            break;
         case CLAW:
            this.claw(a, this.arg(0), this.arg(1), this.size(2));
            break;
         case STREAKS:
            this.streaks(a, this.vec(1), (int)this.arg(0));
      }
   }

   private float arg(int i) {
      float v = this.p.arg(i);
      return Float.isFinite(v) ? v : 0.0F;
   }

   private double size(int i) {
      return MathHelper.clamp(this.arg(i), 0.0F, 96.0F);
   }

   private int ticks(int i) {
      return MathHelper.clamp((int)this.arg(i), 2, 1200);
   }

   private Vec3d vec(int i) {
      return this.p.point(i).subtract(this.p.point(0));
   }

   private int n(double count, int cap) {
      return count <= 0.0 ? 0 : Math.min(cap, Math.max(1, (int)Math.round(count * this.density)));
   }

   private static VfxOptions.Builder of(VfxParticles.Kind kind, int color, double size, int life) {
      return VfxOptions.of(kind, color, size, life);
   }

   private static void add(VfxOptions.Builder b, Vec3d at) {
      VfxClient.spawn(b.build(), at.x, at.y, at.z, 0.0, 0.0, 0.0);
   }

   private static void add(VfxOptions.Builder b, Vec3d at, Vec3d v) {
      VfxClient.spawn(b.build(), at.x, at.y, at.z, v.x, v.y, v.z);
   }

   private static void flare(Vec3d at, int color, double size, int life) {
      add(of(VfxParticles.Kind.FLARE, color, size, life), at);
   }

   private static int dim(int c, float k) {
      int rr = MathHelper.clamp((int)((c >> 16 & 0xFF) * k), 0, 255);
      int gg = MathHelper.clamp((int)((c >> 8 & 0xFF) * k), 0, 255);
      int bb = MathHelper.clamp((int)((c & 0xFF) * k), 0, 255);
      return rr << 16 | gg << 8 | bb;
   }

   private static Vec3d forward(float yawDeg) {
      float y = yawDeg * (float) (Math.PI / 180.0);
      return new Vec3d(-MathHelper.sin(y), 0.0, MathHelper.cos(y));
   }

   private Vec3d unit() {
      Vec3d v = new Vec3d(this.r.nextGaussian(), this.r.nextGaussian(), this.r.nextGaussian());
      return v.lengthSquared() < 1.0E-6 ? new Vec3d(0.0, 1.0, 0.0) : v.normalize();
   }

   private Vec3d perp(Vec3d d) {
      Vec3d dn = d.lengthSquared() < 1.0E-8 ? new Vec3d(0.0, 1.0, 0.0) : d.normalize();

      for (int i = 0; i < 4; i++) {
         Vec3d u = this.unit();
         Vec3d q = u.subtract(dn.multiply(u.dotProduct(dn)));
         if (q.lengthSquared() > 1.0E-4) {
            return q.normalize();
         }
      }

      return Math.abs(dn.y) < 0.9 ? dn.crossProduct(new Vec3d(0.0, 1.0, 0.0)).normalize() : dn.crossProduct(new Vec3d(1.0, 0.0, 0.0)).normalize();
   }

   private int bound(int n) {
      return n <= 0 ? 0 : this.r.nextInt(n);
   }

   private int debrisColor() {
      Vfx.Debris[] all = Vfx.Debris.values();
      if (this.debris >= 0 && this.debris < all.length) {
         return switch (all[this.debris]) {
            case FEATHER, SNOW -> this.core;
            case PETAL, LEAF -> this.glow;
            case ASH -> this.smoke;
            case BONE -> 15129800;
         };
      } else {
         return this.smoke;
      }
   }

   private static void screen(Vec3d at, double radius, double shake, int ticks, int color, float flash, int style) {
      VfxClient.screenAt(at, radius, shake, ticks, color, flash, style);
   }

   private void sparks(Vec3d at, int color, int count, double speed, double size) {
      int m = this.n(count, 40);

      for (int i = 0; i < m; i++) {
         Vec3d d = new Vec3d(this.r.nextGaussian(), Math.abs(this.r.nextGaussian()) * 0.9 + 0.2, this.r.nextGaussian())
            .normalize()
            .multiply(speed * (0.5 + this.r.nextDouble()));
         add(
            of(VfxParticles.Kind.BEAM, color, size * (0.8 + this.r.nextDouble() * 0.5), 10 + this.r.nextInt(12))
               .variant(1)
               .streak(2.4F)
               .gravity(0.55F)
               .drag(0.9F)
               .collide()
               .curve(2),
            at,
            d
         );
      }
   }

   private void embers(Vec3d at, int color, int count, double spread) {
      int m = this.n(count, 40);

      for (int i = 0; i < m; i++) {
         Vec3d q = at.add(this.r.nextGaussian() * spread, this.r.nextDouble() * spread, this.r.nextGaussian() * spread);
         add(
            of(VfxParticles.Kind.EMBER, color, 0.12 + this.r.nextDouble() * 0.12, 30 + this.r.nextInt(30)).curl((this.r.nextFloat() - 0.5F) * 0.1F),
            q,
            new Vec3d(this.r.nextGaussian() * 0.01, 0.02 + this.r.nextDouble() * 0.03, this.r.nextGaussian() * 0.01)
         );
      }
   }

   private void smoke(Vec3d at, int color, int count, double spread, double size) {
      int m = this.n(count, 24);

      for (int i = 0; i < m; i++) {
         Vec3d q = at.add(this.r.nextGaussian() * spread, this.r.nextDouble() * spread * 0.5, this.r.nextGaussian() * spread);
         add(
            of(VfxParticles.Kind.SMOKE, color, size * (0.7 + this.r.nextDouble() * 0.6), 30 + this.r.nextInt(25)),
            q,
            new Vec3d(this.r.nextGaussian() * 0.02, 0.02 + this.r.nextDouble() * 0.02, this.r.nextGaussian() * 0.02)
         );
      }
   }

   private void shards(Vec3d at, int color, int count, double speed) {
      int m = this.n(count, 30);

      for (int i = 0; i < m; i++) {
         Vec3d d = new Vec3d(this.r.nextGaussian(), 0.6 + this.r.nextDouble(), this.r.nextGaussian())
            .normalize()
            .multiply(speed * (0.6 + this.r.nextDouble() * 0.8));
         add(of(VfxParticles.Kind.SHARD, color, 0.18 + this.r.nextDouble() * 0.2, 25 + this.r.nextInt(20)).collide(), at, d);
      }
   }

   private void debris(Vec3d at, int color, int count, double speed, int variant) {
      int v = MathHelper.clamp(variant, 0, Vfx.Debris.values().length - 1);
      boolean light = v != Vfx.Debris.BONE.ordinal();
      int m = this.n(count, 30);

      for (int i = 0; i < m; i++) {
         Vec3d d = new Vec3d(this.r.nextGaussian(), 0.8 + this.r.nextDouble(), this.r.nextGaussian()).normalize().multiply(speed * (0.5 + this.r.nextDouble()));
         add(
            of(VfxParticles.Kind.DEBRIS, color, (light ? 0.32 : 0.26) * (0.7 + this.r.nextDouble() * 0.6), 40 + this.r.nextInt(30))
               .variant(v)
               .gravity(light ? 0.12F : 1.0F)
               .drag(light ? 0.9F : 0.96F)
               .collide()
               .spin((this.r.nextFloat() - 0.5F) * 0.3F),
            at,
            d
         );
      }
   }

   private void glint(Vec3d at, int color, double size) {
      add(of(VfxParticles.Kind.GLINT, color, size, 8).variant(0).roll(this.r.nextFloat() * 90.0F).spin(0.05F), at);
      add(of(VfxParticles.Kind.GLINT, color, size * 0.6, 6).variant(1).roll(45.0F + this.r.nextFloat() * 20.0F), at);
   }

   private void impact(Vec3d at, double power) {
      Vec3d c = at.add(0.0, 0.4, 0.0);
      flare(c, this.core, 1.6 * power, 6);
      flare(c, this.glow, 3.2 * power, 10);
      add(of(VfxParticles.Kind.GLINT, this.core, 2.4 * power, 7).variant(0).roll(this.r.nextFloat() * 90.0F), c);
      Vec3d g = at.add(0.0, 0.08, 0.0);
      add(of(VfxParticles.Kind.SHOCK, this.glow, 4.4 * power, 11).variant(0), g);
      add(of(VfxParticles.Kind.RING, this.core, 2.4 * power, 7), g);
      this.sparks(c, this.core, (int)(10.0 * power), 0.45 * power, 0.22);
      this.smoke(at, this.smoke, (int)(4.0 * power), 0.8 * power, 1.4 * power);
      if (power >= 0.8) {
         screen(at, 18.0 + 8.0 * power, Math.min(1.5, 0.35 * power), (int)(8.0 + 4.0 * power), 0, 0.0F, 0);
      }
   }

   private void shockwave(Vec3d at, double radius) {
      Vec3d g = at.add(0.0, 0.08, 0.0);
      add(of(VfxParticles.Kind.SHOCK, this.glow, radius * 2.0, 14).variant(0), g);
      add(of(VfxParticles.Kind.RING, this.core, radius * 1.2, 10), g);
      flare(at.add(0.0, 0.3, 0.0), this.core, radius * 0.6, 6);
      if (radius >= 2.0) {
         add(of(VfxParticles.Kind.CRACK, dim(this.glow, 0.8F), radius * 1.1, 40).flat(), at.add(0.0, 0.03, 0.0));
      }

      int m = this.n(radius * 4.0, 24);

      for (int i = 0; i < m; i++) {
         double a = i * Math.PI * 2.0 / m;
         Vec3d e = at.add(Math.cos(a) * radius * 0.4, 0.2, Math.sin(a) * radius * 0.4);
         add(of(VfxParticles.Kind.SMOKE, this.smoke, 1.0 + this.r.nextDouble(), 24), e, new Vec3d(Math.cos(a) * 0.12, 0.03, Math.sin(a) * 0.12));
      }

      this.shards(at, this.smoke, (int)Math.min(20.0, radius * 2.0), 0.5);
      screen(at, radius * 4.0, Math.min(1.4, 0.18 * radius), 12, 0, 0.0F, 0);
   }

   private void beam(Vec3d from, Vec3d to, double width, int life) {
      Vec3d d = to.subtract(from);
      boolean held = life > 0;
      int fade = held ? 3 : 1;
      add(of(VfxParticles.Kind.BEAM, this.core, width, held ? life : 3).variant(0).span().fade(fade), from, d);
      add(of(VfxParticles.Kind.BEAM, dim(this.glow, 0.75F), width * 2.6, held ? life : 4).variant(2).span().fade(fade), from, d);
      add(of(VfxParticles.Kind.FLARE, this.core, width * 3.0, held ? life : 6).fade(held ? 3 : 0), to);
      flare(from, this.glow, width * 2.2, 5);
      this.sparks(to, this.glow, 3, 0.25, 0.18);
   }

   private void gather(Vec3d at, double radius, int count) {
      int m = this.n(count, 24);

      for (int i = 0; i < m; i++) {
         double a = this.r.nextDouble() * Math.PI * 2.0;
         double h = (this.r.nextDouble() - 0.3) * radius;
         Vec3d from = at.add(Math.cos(a) * radius, h, Math.sin(a) * radius);
         int life = 10 + this.r.nextInt(4);
         add(
            of(VfxParticles.Kind.BEAM, this.r.nextBoolean() ? this.core : this.glow, 0.16, life).variant(1).streak(2.5F).drag(1.0F).gravity(0.0F).fade(3),
            from,
            at.subtract(from).multiply(1.0 / life)
         );
      }

      flare(at, this.glow, radius * 0.5, 5);
   }

   private void pillar(Vec3d at, double height, double width) {
      Vec3d up = new Vec3d(0.0, height, 0.0);
      add(of(VfxParticles.Kind.BEAM, this.core, width, 9).variant(0).span().curve(6).fade(3), at, up);
      add(of(VfxParticles.Kind.BEAM, this.glow, width * 2.4, 12).variant(2).span().curve(6).fade(3), at, up);
      flare(at.add(0.0, 0.3, 0.0), this.glow, width * 2.6, 10);
      flare(at.add(up), this.core, width * 1.4, 8);
      add(of(VfxParticles.Kind.SHOCK, this.glow, width * 3.6, 12).variant(1), at.add(0.0, 0.08, 0.0));
      this.embers(at.add(0.0, height * 0.3, 0.0), this.core, 10, width);
   }

   private void explosion(Vec3d at, double radius) {
      flare(at, 16777215, radius * 1.2, 4);
      flare(at, this.core, radius * 2.2, 8);
      flare(at, this.glow, radius * 3.4, 14);
      add(of(VfxParticles.Kind.GLINT, this.core, radius * 3.0, 8).variant(1).roll(this.r.nextFloat() * 90.0F), at);
      add(of(VfxParticles.Kind.SHOCK, this.glow, radius * 3.2, 16).variant(0), at.add(0.0, 0.08, 0.0));
      add(of(VfxParticles.Kind.SHOCK, dim(this.glow, 0.8F), radius * 2.6, 10).variant(1).face().roll(0.0F), at);
      add(of(VfxParticles.Kind.RING, this.core, radius * 2.0, 10), at.add(0.0, 0.08, 0.0));
      this.sparks(at, this.core, (int)(radius * 10.0), 0.6 * radius, 0.25);
      this.smoke(at, this.smoke, (int)(radius * 5.0), radius, radius * 1.3);
      this.shards(at, this.smoke, (int)(radius * 3.0), 0.6);
      if (this.debris >= 0) {
         this.debris(at, this.debrisColor(), (int)(radius * 2.0), 0.5, this.debris);
      }

      screen(at, radius * 6.0, Math.min(1.6, 0.3 * radius), 14, this.glow, (float)Math.min(0.45, 0.08 * radius), 1);
   }

   private void aura(Vec3d at, double radius) {
      add(of(VfxParticles.Kind.RING, this.glow, radius * 2.0, 20), at.add(0.0, 0.08, 0.0));
      this.embers(at, this.core, (int)(radius * 3.0), radius * 0.6);
   }

   private void telegraphCircle(Vec3d center, double radius, int ticks) {
      double size = radius * 2.0 / 0.955;
      Vec3d g = center.add(0.0, 0.06, 0.0);
      add(of(VfxParticles.Kind.TELEGRAPH, this.glow, size, ticks).variant(0).flat().spin(0.01F).fade(5), g);
      add(of(VfxParticles.Kind.TELEGRAPH, dim(this.glow, 0.7F), size, ticks).variant(1).flat().curve(7).fade(5), g.add(0.0, 0.01, 0.0));
      add(of(VfxParticles.Kind.RING, this.core, radius * 2.2, 7).flat().delay(ticks), g);
   }

   private void telegraphLine(Vec3d from, Vec3d dir, double length, double width, int ticks) {
      Vec3d flat = new Vec3d(dir.x, 0.0, dir.z);
      Vec3d d = (flat.lengthSquared() < 1.0E-6 ? new Vec3d(0.0, 0.0, 1.0) : flat.normalize()).multiply(Math.max(0.5, length));
      Vec3d g = from.add(0.0, 0.06, 0.0);
      add(of(VfxParticles.Kind.TELEGRAPH, this.glow, width / 0.88, ticks).variant(2).lie().fade(5), g, d);
      add(of(VfxParticles.Kind.BEAM, dim(this.glow, 0.6F), width * 0.8, ticks).variant(0).lie().curve(7).fade(5), g.add(0.0, 0.01, 0.0), d);
      add(of(VfxParticles.Kind.BEAM, this.core, width * 0.5, 6).variant(0).lie().delay(ticks), g, d);
   }

   private void telegraphCone(Vec3d origin, float yaw, float halfAngle, double range, int ticks) {
      int wedges = MathHelper.clamp((int)Math.ceil(2.0F * halfAngle / 30.0F - 0.01), 1, 12);
      float step = 2.0F * halfAngle / wedges;
      double size = range * 2.0 / 0.955;
      Vec3d g = origin.add(0.0, 0.06, 0.0);

      for (int i = 0; i < wedges; i++) {
         float a = yaw - halfAngle + (i + 0.5F) * step;
         add(of(VfxParticles.Kind.TELEGRAPH, this.glow, size, ticks).variant(3).flat(a).fade(5), g);
         add(of(VfxParticles.Kind.TELEGRAPH, dim(this.glow, 0.55F), size, ticks).variant(3).flat(a).curve(7).fade(5), g.add(0.0, 0.01, 0.0));
      }

      if (halfAngle < 175.0F) {
         for (int s = -1; s <= 1; s += 2) {
            Vec3d edge = forward(yaw + s * halfAngle).multiply(range);
            add(of(VfxParticles.Kind.TELEGRAPH, this.glow, 0.3, ticks).variant(2).lie().fade(5), g, edge);
         }
      }
   }

   private List<Vec3d> jag(Vec3d from, Vec3d to, int depth, double amp) {
      List<Vec3d> pts = new ArrayList<>(List.of(from, to));
      double a = from.distanceTo(to) * amp;

      for (int d = 0; d < depth; d++) {
         List<Vec3d> next = new ArrayList<>();

         for (int i = 0; i + 1 < pts.size(); i++) {
            Vec3d p0 = pts.get(i);
            Vec3d p1 = pts.get(i + 1);
            next.add(p0);
            next.add(p0.add(p1).multiply(0.5).add(this.perp(p1.subtract(p0)).multiply(this.r.nextGaussian() * a)));
         }

         next.add(pts.get(pts.size() - 1));
         pts = next;
         a *= 0.55;
      }

      return pts;
   }

   private void lightning(Vec3d from, Vec3d to, int branches, boolean full) {
      double len = from.distanceTo(to);
      if (!(len < 0.1)) {
         int depth = !full ? 2 : (len < 4.0 ? 2 : (len < 12.0 ? 3 : 4));
         List<Vec3d> path = this.jag(from, to, depth, 0.18);

         for (int i = 0; i + 1 < path.size(); i++) {
            Vec3d a = path.get(i);
            Vec3d d = path.get(i + 1).subtract(a);
            add(of(VfxParticles.Kind.BOLT, this.core, 0.8, 7).span().fade(4), a, d);
            add(of(VfxParticles.Kind.BEAM, this.glow, 2.4, 8).variant(0).span().fade(4), a, d);
         }

         int forks = full ? this.n(branches, 6) : Math.min(branches, 1);

         for (int k = 0; k < forks && path.size() > 2; k++) {
            int i = 1 + this.r.nextInt(path.size() - 2);
            Vec3d s = path.get(i);
            Vec3d seg = path.get(i + 1).subtract(s);
            Vec3d dir = seg.normalize().add(this.perp(seg).multiply(0.9)).normalize();
            List<Vec3d> fork = this.jag(s, s.add(dir.multiply(len * 0.25 * (0.5 + this.r.nextDouble()))), 2, 0.25);

            for (int j = 0; j + 1 < fork.size(); j++) {
               add(of(VfxParticles.Kind.BOLT, this.core, 0.5, 5).span().fade(4), fork.get(j), fork.get(j + 1).subtract(fork.get(j)));
            }
         }

         if (full) {
            flare(from, this.glow, 1.4, 5);
            flare(to, this.core, 2.2, 6);
            add(of(VfxParticles.Kind.GLINT, this.core, 2.6, 5).variant(0).roll(this.r.nextFloat() * 90.0F), to);
            this.sparks(to, this.core, 6, 0.3, 0.2);
         }
      }
   }

   private void vortex(Vec3d center, double radius, double height, int life) {
      int m = this.n(30.0, 36);
      float w = 0.22F;

      for (int i = 0; i < m; i++) {
         int delay = (int)((float)i / m * Math.max(0, life - 24));
         int pl = Math.max(8, Math.min(life - delay, 22 + this.r.nextInt(10)));
         double h = this.r.nextDouble() * height * 0.6;
         double rad = radius * Math.min(1.1, 0.35 + 0.65 * (h / Math.max(0.1, height) + 0.4 * this.r.nextDouble()));
         double a = this.r.nextDouble() * Math.PI * 2.0;
         Vec3d pos = center.add(Math.cos(a) * rad, h, Math.sin(a) * rad);
         Vec3d v = new Vec3d(-Math.sin(a) * rad * w, (height - h) / pl * 0.8, Math.cos(a) * rad * w);
         if (i % 4 == 3) {
            add(of(VfxParticles.Kind.SMOKE, this.smoke, 1.0 + this.r.nextDouble() * 0.8, pl).curl(w).drag(1.0F).gravity(0.0F).delay(delay), pos, v);
         } else {
            add(
               of(VfxParticles.Kind.BEAM, this.r.nextFloat() < 0.35F ? this.core : this.glow, 0.14 + this.r.nextDouble() * 0.08, pl)
                  .variant(1)
                  .streak(3.0F)
                  .curl(w)
                  .drag(1.0F)
                  .gravity(0.0F)
                  .fade(3)
                  .delay(delay),
               pos,
               v
            );
         }
      }

      Vec3d g = center.add(0.0, 0.08, 0.0);
      add(of(VfxParticles.Kind.RING, this.glow, radius * 2.0, life).flat().spin(0.12F).curve(5).fade(3), g);
      add(of(VfxParticles.Kind.SHOCK, dim(this.glow, 0.7F), radius * 2.2, Math.min(life, 20)).variant(1), g);
   }

   private void nova(Vec3d center, double radius) {
      flare(center, 16777215, radius * 0.8, 5);
      flare(center, this.core, radius * 1.6, 9);
      flare(center, this.glow, radius * 2.6, 14);
      add(of(VfxParticles.Kind.SHOCK, this.glow, radius * 2.0, 12).variant(0).face().roll(0.0F).curve(6), center);
      add(of(VfxParticles.Kind.SHOCK, dim(this.glow, 0.8F), radius * 2.2, 14).variant(1).flat(), center);
      add(of(VfxParticles.Kind.GLINT, this.core, radius * 1.5, 8).variant(1).roll(this.r.nextFloat() * 45.0F), center);
      int m = this.n(20.0, 24);

      for (int i = 0; i < m; i++) {
         add(
            of(VfxParticles.Kind.BEAM, this.r.nextBoolean() ? this.core : this.glow, 0.2, 10).variant(1).streak(3.0F).drag(0.88F).gravity(0.0F).curve(2),
            center,
            this.unit().multiply(radius / 7.0)
         );
      }

      screen(center, radius * 3.0, 0.4, 8, this.glow, 0.25F, 1);
   }

   private void runeCircle(Vec3d center, double radius, int life, float spin, int rune) {
      Vec3d g = center.add(0.0, 0.05, 0.0);
      add(of(VfxParticles.Kind.RUNE, this.glow, radius * 2.0, life).variant(rune).flat().spin(spin), g);
      add(of(VfxParticles.Kind.RUNE, dim(this.core, 0.55F), radius * 1.1, life).variant((rune + 2) % 4).flat().spin(-spin * 1.6F), g.add(0.0, 0.01, 0.0));
      add(of(VfxParticles.Kind.RING, this.glow, radius * 2.25, life).flat().curve(5).fade(3), g);
      int m = this.n(6.0, 10);

      for (int i = 0; i < m; i++) {
         double a = this.r.nextDouble() * Math.PI * 2.0;
         add(
            of(VfxParticles.Kind.GLINT, this.core, 0.3, 20 + this.r.nextInt(10)).variant(0).delay(this.bound(life - 20)),
            g.add(Math.cos(a) * radius, 0.1, Math.sin(a) * radius),
            new Vec3d(0.0, 0.04, 0.0)
         );
      }
   }

   private void chargeUp(Vec3d at, int ticks) {
      int m = this.n(20.0, 24);

      for (int i = 0; i < m; i++) {
         int delay = Math.max(0, (int)((float)(i * (ticks - 8)) / m));
         Vec3d from = at.add(this.unit().multiply(1.3 + this.r.nextDouble() * 1.2));
         add(
            of(VfxParticles.Kind.BEAM, i % 2 == 0 ? this.core : this.glow, 0.12, 8).variant(1).streak(2.5F).drag(1.0F).gravity(0.0F).fade(3).delay(delay),
            from,
            at.subtract(from).multiply(0.125)
         );
      }

      add(of(VfxParticles.Kind.FLARE, this.glow, 2.0, ticks).curve(1).fade(5), at);
      add(of(VfxParticles.Kind.FLARE, this.core, 1.0, ticks).curve(1).fade(5), at);

      for (int k = 0; k < 3; k++) {
         double a = k * Math.PI * 2.0 / 3.0;
         double rad = 1.1;
         float w = 0.3F;
         Vec3d pos = at.add(Math.cos(a) * rad, (this.r.nextDouble() - 0.5) * 0.4, Math.sin(a) * rad);
         Vec3d v = new Vec3d(-Math.sin(a) * rad * w, 0.0, Math.cos(a) * rad * w);

         for (int t = 0; t < 3; t++) {
            add(
               of(VfxParticles.Kind.WISP, dim(this.glow, 1.0F - t * 0.3F), 0.4 * (1.0 - t * 0.25), ticks)
                  .variant(0)
                  .curl(w)
                  .drag(0.97F)
                  .gravity(0.0F)
                  .fade(3)
                  .delay(t * 2),
               pos,
               v
            );
         }
      }

      add(of(VfxParticles.Kind.FLARE, this.core, 2.8, 6).delay(ticks), at);
      add(of(VfxParticles.Kind.SHOCK, this.glow, 3.2, 8).variant(1).face().roll(0.0F).delay(ticks), at);
   }

   private void meteorTrail(Vec3d at, Vec3d v) {
      if (v.lengthSquared() < 1.0E-6) {
         flare(at, this.glow, 1.8, 6);
      } else {
         add(of(VfxParticles.Kind.BEAM, dim(this.glow, 0.8F), 1.3, 6).variant(1).streak(3.5F).drag(0.6F).gravity(0.0F), at, v);
         add(of(VfxParticles.Kind.BEAM, this.core, 0.55, 4).variant(1).streak(3.0F).drag(0.6F).gravity(0.0F), at, v);
         flare(at, this.glow, 2.0, 3);
         int i = 0;

         for (int m = this.n(2.0, 3); i < m; i++) {
            add(
               of(VfxParticles.Kind.SMOKE, this.smoke, 1.2 + this.r.nextDouble() * 0.8, 28 + this.r.nextInt(10)),
               at.subtract(v.multiply(0.5)).add(this.r.nextGaussian() * 0.2, this.r.nextGaussian() * 0.2, this.r.nextGaussian() * 0.2),
               new Vec3d(this.r.nextGaussian() * 0.02, 0.03, this.r.nextGaussian() * 0.02)
            );
         }

         i = 0;

         for (int m = this.n(2.0, 3); i < m; i++) {
            add(
               of(VfxParticles.Kind.EMBER, this.core, 0.15, 20 + this.r.nextInt(10)),
               at,
               v.multiply(-0.1).add(this.r.nextGaussian() * 0.03, this.r.nextGaussian() * 0.03, this.r.nextGaussian() * 0.03)
            );
         }
      }
   }

   private void groundCrack(Vec3d center, double radius) {
      Vec3d g = center.add(0.0, 0.04, 0.0);
      add(of(VfxParticles.Kind.SCORCH, dim(this.smoke, 0.7F), radius * 2.3, 90).flat(), g);
      add(of(VfxParticles.Kind.CRACK, this.glow, radius * 2.0, 60).flat(), g.add(0.0, 0.01, 0.0));
      add(of(VfxParticles.Kind.CRACK, this.core, radius * 1.1, 26).flat().fade(1), g.add(0.0, 0.02, 0.0));
      int i = 0;

      for (int m = this.n(6.0, 10); i < m; i++) {
         double a = this.r.nextDouble() * Math.PI * 2.0;
         double d = Math.sqrt(this.r.nextDouble()) * radius * 0.8;
         add(of(VfxParticles.Kind.EMBER, this.core, 0.14, 30 + this.r.nextInt(20)), g.add(Math.cos(a) * d, 0.05, Math.sin(a) * d), new Vec3d(0.0, 0.03, 0.0));
      }

      this.smoke(g, this.smoke, 4, radius * 0.5, 1.2);
   }

   private void heavyImpact(Vec3d at, double power) {
      Vec3d c = at.add(0.0, 0.5, 0.0);
      flare(c, 16777215, 1.4 * power, 4);
      flare(c, this.core, 2.4 * power, 7);
      flare(c, this.glow, 4.6 * power, 12);
      add(of(VfxParticles.Kind.GLINT, this.core, 3.4 * power, 7).variant(0).roll(this.r.nextFloat() * 90.0F), c);
      Vec3d g = at.add(0.0, 0.08, 0.0);
      add(of(VfxParticles.Kind.SHOCK, this.glow, 6.4 * power, 15).variant(0), g);
      add(of(VfxParticles.Kind.SHOCK, this.core, 3.6 * power, 9).variant(1), g.add(0.0, 0.01, 0.0));
      this.groundCrack(at, 1.7 * power);
      if (this.debris >= 0) {
         this.debris(c, this.debrisColor(), (int)(6.0 * power), 0.45 + 0.1 * power, this.debris);
      }

      this.shards(at.add(0.0, 0.3, 0.0), this.smoke, (int)(4.0 * power), 0.55);
      int m = this.n(6.0 * power, 14);

      for (int i = 0; i < m; i++) {
         double a = i * Math.PI * 2.0 / m;
         add(
            of(VfxParticles.Kind.SMOKE, this.smoke, 1.2 * power * (0.8 + this.r.nextDouble() * 0.4), 30),
            at.add(Math.cos(a) * 0.6 * power, 0.25, Math.sin(a) * 0.6 * power),
            new Vec3d(Math.cos(a) * 0.14, 0.02, Math.sin(a) * 0.14)
         );
      }

      this.sparks(c, this.core, (int)(10.0 * power), 0.5 * power, 0.24);
      screen(at, 20.0 + 10.0 * power, Math.min(2.0, 0.5 * power), (int)(10.0 + 4.0 * power), 0, 0.0F, 0);
      if (power >= 1.5) {
         screen(at, 10.0 + 5.0 * power, 0.0, 0, this.glow, (float)Math.min(0.4, 0.14 * power), 2);
      } else if (power >= 1.0) {
         screen(at, 12.0 + 6.0 * power, 0.0, 0, this.glow, 0.15F, 1);
      }
   }

   private void shieldBubble(Vec3d at, double radius, int life) {
      add(of(VfxParticles.Kind.RING, this.glow, radius * 2.0 / 0.86, life).face().roll(0.0F).curve(5).fade(3), at);
      flare(at, dim(this.glow, 0.5F), radius * 1.6, 8);
      int[][] rows = new int[][]{{-15, 8}, {20, 8}, {52, 5}, {85, 1}};
      int k = 0;

      for (int row = 0; row < rows.length; row++) {
         int count = rows[row][1];

         for (int j = 0; j < count; j++) {
            if (!(this.density < 0.5F) || k++ % 2 != 1) {
               float az = (j + row % 2 * 0.5F) * 360.0F / count;
               float pitch = -rows[row][0];
               Vec3d dir = Vec3d.fromPolar(pitch, az);
               add(
                  of(VfxParticles.Kind.HEX, dim(this.glow, 0.8F), radius * 0.85, Math.max(4, life - row * 2))
                     .variant(0)
                     .facing(az, pitch)
                     .roll(0.0F)
                     .curve(4)
                     .fade(3)
                     .delay(row * 2),
                  at.add(dir.multiply(radius))
               );
            }
         }
      }
   }

   private void ward(Vec3d at, double radius, int life, int rune) {
      Vec3d g = at.add(0.0, 0.05, 0.0);
      add(of(VfxParticles.Kind.RUNE, this.glow, radius * 2.0, life).variant(rune).flat().spin(0.02F), g);
      add(of(VfxParticles.Kind.RING, this.glow, radius * 2.2, life).flat().curve(5).fade(3), g);

      for (int j = 0; j < 8; j++) {
         float az = j * 45.0F;
         Vec3d pos = at.add(forward(az).multiply(radius)).add(0.0, radius * 0.45 + 0.3, 0.0);
         add(
            of(VfxParticles.Kind.HEX, dim(this.glow, 0.65F), radius * 0.8, Math.max(4, life - j))
               .variant(0)
               .facing(az, 0.0F)
               .roll(0.0F)
               .curve(4)
               .fade(3)
               .delay(j),
            pos
         );
      }

      add(of(VfxParticles.Kind.BEAM, dim(this.glow, 0.3F), radius * 1.9, life).variant(0).span().fade(3), g, new Vec3d(0.0, radius * 1.3, 0.0));
   }

   private void dashTrail(Vec3d from, Vec3d to) {
      Vec3d d = to.subtract(from);
      double len = d.length();
      if (!(len < 0.05)) {
         add(of(VfxParticles.Kind.BEAM, this.glow, 1.1, 9).variant(1).span(), from, d);
         add(of(VfxParticles.Kind.BEAM, this.core, 0.35, 6).variant(1).span(), from, d);
         int i = 0;

         for (int m = this.n(6.0, 8); i < m; i++) {
            Vec3d s = from.add(d.multiply(this.r.nextDouble() * 0.3)).add(this.perp(d).multiply(0.3 + this.r.nextDouble() * 0.6));
            add(
               of(VfxParticles.Kind.BEAM, this.r.nextBoolean() ? this.core : this.glow, 0.08, 5 + this.r.nextInt(3)).variant(1).span().delay(this.r.nextInt(2)),
               s,
               d.multiply(0.5 + this.r.nextDouble() * 0.4)
            );
         }

         for (int k = 1; k <= 3; k++) {
            flare(from.add(d.multiply(k / 4.0)), dim(this.glow, 0.8F - k * 0.15F), 1.5, 4 + k * 2);
         }
      }
   }

   private void auraRing(Vec3d at, double radius) {
      Vec3d g = at.add(0.0, 0.06, 0.0);
      add(of(VfxParticles.Kind.RING, this.glow, radius * 2.0, 24).flat().curve(5).spin(0.08F).fade(3), g);
      add(of(VfxParticles.Kind.SHOCK, dim(this.glow, 0.6F), radius * 2.4, 16).variant(1), g);
      add(of(VfxParticles.Kind.BEAM, dim(this.glow, 0.3F), radius * 1.8, 20).variant(0).span().fade(3), g, new Vec3d(0.0, 2.2, 0.0));
      int i = 0;

      for (int m = this.n(8.0, 10); i < m; i++) {
         double a = this.r.nextDouble() * Math.PI * 2.0;
         add(
            of(VfxParticles.Kind.GLINT, this.core, 0.3, 18 + this.r.nextInt(10)).variant(0).spin(0.1F).delay(this.r.nextInt(6)),
            g.add(Math.cos(a) * radius, 0.1, Math.sin(a) * radius),
            new Vec3d(0.0, 0.05, 0.0)
         );
      }
   }

   private void wisps(Vec3d center, int count, double radius, int life) {
      int m = this.n(count, 12);

      for (int i = 0; i < m; i++) {
         double a = i * Math.PI * 2.0 / m + this.r.nextDouble() * 0.5;
         double rad = radius * (0.6 + 0.4 * this.r.nextDouble());
         float w = (this.r.nextBoolean() ? 1 : -1) * (0.08F + this.r.nextFloat() * 0.06F);
         Vec3d pos = center.add(Math.cos(a) * rad, (this.r.nextDouble() - 0.5) * 1.2, Math.sin(a) * rad);
         Vec3d v = new Vec3d(-Math.sin(a) * rad * w, (this.r.nextDouble() - 0.5) * 0.02, Math.cos(a) * rad * w);
         int pl = life + this.r.nextInt(20);
         double size = 0.55 + this.r.nextDouble() * 0.25;

         for (int t = 0; t < 3; t++) {
            add(
               of(VfxParticles.Kind.WISP, dim(this.glow, 1.0F - t * 0.3F), size * (1.0 - t * 0.25), pl)
                  .variant(0)
                  .curl(w)
                  .drag(1.0F)
                  .gravity(0.0F)
                  .delay(t * 2),
               pos,
               v
            );
         }
      }
   }

   private void slashArc(Vec3d o, float yaw, float pitch, float roll, double reach, float arc) {
      if (!(reach <= 0.05)) {
         boolean mirror = arc < 0.0F;
         float span = Math.abs(arc);
         int variant;
         float rim;
         if (span <= 140.0F) {
            variant = 1;
            rim = 0.8F;
         } else if (span <= 230.0F) {
            variant = reach >= 3.5 ? 2 : 0;
            rim = reach >= 3.5 ? 0.76F : 0.74F;
         } else {
            variant = 3;
            rim = 0.82F;
         }

         double size = reach * 2.0 / rim;
         float follow = (mirror ? -1 : 1) * 0.06F;
         add(of(VfxParticles.Kind.SLASH, this.glow, size, 7).variant(variant).plane(yaw, pitch, roll).mirror(mirror).spin(follow), o);
         add(of(VfxParticles.Kind.SLASH, this.core, size * 0.97, 5).variant(variant).plane(yaw, pitch, roll).mirror(mirror).spin(follow), o);
         Vec3d f = Vec3d.fromPolar(pitch, yaw);
         float yr = yaw * (float) (Math.PI / 180.0);
         Vec3d right = new Vec3d(-MathHelper.cos(yr), 0.0, -MathHelper.sin(yr));
         Vec3d up = right.crossProduct(f);
         double rr = Math.toRadians(roll);
         Vec3d side = right.multiply(Math.cos(rr)).add(up.multiply(Math.sin(rr)));
         double h = Math.toRadians(Math.min(span, 300.0F) / 2.0F);
         double sign = mirror ? 1.0 : -1.0;
         Vec3d tipDir = f.multiply(Math.cos(h)).add(side.multiply(sign * Math.sin(h)));
         Vec3d tangent = f.multiply(-Math.sin(h)).add(side.multiply(sign * Math.cos(h)));
         Vec3d tip = o.add(tipDir.multiply(reach));
         int i = 0;

         for (int m = this.n(4.0, 6); i < m; i++) {
            Vec3d v = tangent.multiply(0.3 + this.r.nextDouble() * 0.15)
               .add(tipDir.multiply(0.08))
               .add(this.r.nextGaussian() * 0.04, this.r.nextGaussian() * 0.04, this.r.nextGaussian() * 0.04);
            add(of(VfxParticles.Kind.BEAM, this.core, 0.16, 7 + this.r.nextInt(4)).variant(1).streak(2.4F).gravity(0.4F).drag(0.88F).curve(2), tip, v);
         }
      }
   }

   private void claw(Vec3d o, float yaw, float pitch, double reach) {
      boolean mirror = this.r.nextBoolean();
      float roll = mirror ? 50.0F : -50.0F;
      double size = reach * 2.0 / 0.86;
      float follow = (mirror ? -1 : 1) * 0.05F;
      add(of(VfxParticles.Kind.SLASH, this.glow, size, 7).variant(4).plane(yaw, pitch, roll).mirror(mirror).spin(follow), o);
      add(of(VfxParticles.Kind.SLASH, this.core, size * 0.97, 5).variant(4).plane(yaw, pitch, roll).mirror(mirror).spin(follow), o);
      this.sparks(o.add(Vec3d.fromPolar(pitch, yaw).multiply(reach)), this.core, 4, 0.25, 0.18);
   }

   private void streaks(Vec3d at, Vec3d dir, int count) {
      Vec3d dn = dir.lengthSquared() < 1.0E-6 ? new Vec3d(0.0, 0.0, 1.0) : dir.normalize();
      int i = 0;

      for (int m = this.n(count, 16); i < m; i++) {
         double len = 1.5 + this.r.nextDouble() * 2.5;
         Vec3d s = at.add(this.perp(dn).multiply(0.3 + this.r.nextDouble() * 1.2)).add(dn.multiply((this.r.nextDouble() - 0.5) * 2.0 - len * 0.5));
         add(
            of(VfxParticles.Kind.BEAM, this.r.nextBoolean() ? this.core : this.glow, 0.06 + this.r.nextDouble() * 0.06, 4 + this.r.nextInt(3))
               .variant(1)
               .span()
               .delay(this.r.nextInt(2)),
            s,
            dn.multiply(len)
         );
      }
   }
}

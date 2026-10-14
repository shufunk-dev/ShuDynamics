package com.vexira.mysticalterrain.building;

import com.vexira.mysticalterrain.tree.Noise;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

public final class BuildingBuilder {
   private static final int STORY = 4;
   private static final int[] DX = new int[]{0, -1, 0, 1};
   private static final int[] DZ = new int[]{1, 0, -1, 0};
   private final BuildingParams p;
   private final SplittableRandom random;
   private final Blueprint plan = new Blueprint();

   private BuildingBuilder(BuildingParams p) {
      this.p = p;
      this.random = new SplittableRandom(p.seed());
   }

   public static Blueprint build(BuildingParams params) {
      if (params.kind().lair()) {
         return LairBuilder.build(params);
      }

      BuildingBuilder b = new BuildingBuilder(params);
      switch (params.kind()) {
         case TOWER:
            b.tower();
            break;
         case KEEP:
            b.keep();
            break;
         case TEMPLE:
            b.temple();
            break;
         case ZIGGURAT:
            b.ziggurat();
            break;
         case CRYPT:
            b.crypt();
            break;
         case HAMLET:
            b.hamlet();
            break;
         case SHRINE:
            b.shrine();
            break;
         case WATCHTOWER:
            b.watchtower();
            break;
         case OBSERVATORY:
            b.observatory();
            break;
         case LIBRARY:
            b.library();
            break;
         case FORGE:
            b.forge();
            break;
         case ARENA:
            b.arena();
            break;
         case MAZE:
            b.maze();
            break;
         case OUTPOST:
            b.outpost();
            break;
         case VAULT:
            b.vault();
            break;
         case GRAVEYARD:
            b.graveyard();
            break;
         case LIGHTHOUSE:
            b.lighthouse();
            break;
         default:
            throw new IllegalStateException(params.kind() + " is built by LairBuilder");
      }

      b.ruin();
      return b.plan.finish();
   }

   private void tower() {
      int s = clamp(this.p.size(), 2, 6);
      int floors = clamp(this.p.floors(), 2, 7);
      int top = floors * 4;
      boolean round = this.random.nextBoolean();
      if (round) {
         this.disc(0, 0, s + 1.4, -1, Part.FOUNDATION);

         for (int y = 0; y <= top; y++) {
            this.ring(0, 0, s + 0.5, y, y % 4 == 0 && y > 0 ? Part.FLOOR : null);
         }
      } else {
         this.fill(-s - 1, -1, -s - 1, s + 1, -1, s + 1, Part.FOUNDATION);

         for (int k = 0; k < floors; k++) {
            this.box(-s, -s, s, s, k * 4, 3);
         }

         this.fill(-s, top, -s, s, top, s, Part.FLOOR);
      }

      for (int k = 0; k < floors; k++) {
         int y = k * 4;
         this.sideWindows(s, y + 2, round);
         this.put(0, y + 4 - 1, 0, Part.HANGING_LIGHT);
      }

      int face = this.random.nextInt(4);
      this.door(face, s, 0, 1, 3);
      int back = (face + 2) % 4;
      this.ladder(DX[back] * (s - 1), DZ[back] * (s - 1), 1, top, face);
      if (floors >= 3) {
         this.cellar(-s + 1, -s + 1, s - 1, s - 1, -5, DX[back] * (s - 1), DZ[back] * (s - 1), face);
      }

      this.chest(DX[(face + 1) % 4] * (s - 1), top - 4 + 1, DZ[(face + 1) % 4] * (s - 1), (face + 3) % 4, true);
      if (round) {
         this.cone(0, 0, s + 1.5, top + 1);
      } else if (this.random.nextBoolean()) {
         this.battlements(-s, -s, s, s, top);
      } else {
         this.hipRoof(-s, -s, s, s, top + 1);
      }
   }

   private void keep() {
      int s = clamp(this.p.size(), 10, 18);
      int wall = 6;
      this.fill(-s - 1, -1, -s - 1, s + 1, -1, s + 1, Part.FOUNDATION);
      this.paving(-s + 1, -s + 1, s - 1, s - 1, 0);
      this.walls(-s, -s, s, s, 0, wall, Part.WALL, Part.WALL);
      this.fill(-s + 1, wall, -s + 1, s - 1, wall, -s + 1, Part.FLOOR);
      this.fill(-s + 1, wall, s - 1, s - 1, wall, s - 1, Part.FLOOR);
      this.fill(-s + 1, wall, -s + 1, -s + 1, wall, s - 1, Part.FLOOR);
      this.fill(s - 1, wall, -s + 1, s - 1, wall, s - 1, Part.FLOOR);
      this.crenels(-s, -s, s, s, wall + 1);

      for (int cx = -1; cx <= 1; cx += 2) {
         for (int cz = -1; cz <= 1; cz += 2) {
            int x = cx * s;
            int z = cz * s;
            this.fill(x - 2, 0, z - 2, x + 2, wall + 3, z + 2, Part.WALL);
            this.fill(x - 1, 1, z - 1, x + 1, wall + 3, z + 1, Part.AIR);
            this.fill(x - 1, wall + 3, z - 1, x + 1, wall + 3, z + 1, Part.FLOOR);
            this.crenels(x - 2, z - 2, x + 2, z + 2, wall + 4);
            this.ladder(x + cx, z, 1, wall + 3, cx > 0 ? 1 : 3);
            this.fill(x - cx * 2, 1, z, x - cx * 2, 2, z, Part.AIR);
            this.put(x, wall + 2, z, Part.HANGING_LIGHT);
         }
      }

      this.fill(-2, 0, s, 2, 4, s, Part.AIR);
      this.arch(-2, 2, s, 4, true);
      this.fill(-3, 0, s - 1, 3, 5, s + 1, Part.TRIM);
      this.fill(-2, 0, s - 1, 2, 4, s + 1, Part.AIR);
      int k = Math.max(4, s / 2 - 1);
      int floors = 2 + this.random.nextInt(2);

      for (int f = 0; f < floors; f++) {
         this.box(-k, -k, k, k, f * 4, 3);
         this.sideWindows(k, f * 4 + 2, false);
         this.put(0, f * 4 + 4 - 1, 0, Part.HANGING_LIGHT);
      }

      this.battlements(-k, -k, k, k, floors * 4);
      this.door(0, k, 0, 1, 3);
      this.ladder(0, -k + 1, 1, floors * 4, 0);

      for (int f = 0; f < floors; f++) {
         this.furnish(-k + 1, -k + 1, k - 1, k - 1, f * 4 + 1, f == floors - 1 ? Part.SHELF : Part.STORAGE);
      }

      this.chest(k - 1, 1, k - 1, 1, false);
      this.chest(-k + 1, (floors - 1) * 4 + 1, k - 1, 3, true);
      this.cellar(-k + 1, -k + 1, k - 1, k - 1, -5, 0, -k + 1, 0);

      for (int i = 0; i < 4; i++) {
         this.put(DX[i] * (s - 3), 0, DZ[i] * (s - 3) + (i == 0 ? -2 : 0), Part.TRIM);
         this.put(DX[i] * (s - 3), 1, DZ[i] * (s - 3) + (i == 0 ? -2 : 0), Part.LIGHT);
      }
   }

   private void temple() {
      int s = clamp(this.p.size(), 4, 9);
      int len = s + s / 2 + 2;
      int h = s + 3;

      for (int t = 0; t < 3; t++) {
         this.fill(-s - 3 + t, t - 3, -len - 3 + t, s + 3 - t, t - 3, len + 3 - t, Part.FOUNDATION);
      }

      this.fill(-s - 1, -1, -len - 1, s + 1, -1, len + 1, Part.FLOOR);

      for (int t = 0; t < 3; t++) {
         for (int x = -2; x <= 2; x++) {
            this.put(x, t - 3, len + 3 - t, Part.STAIRS, 2, false);
         }
      }

      for (int x = -s; x <= s; x += 2) {
         this.column(x, -len, h);
         this.column(x, len, h);
      }

      for (int z = -len + 2; z <= len - 2; z += 2) {
         this.column(-s, z, h);
         this.column(s, z, h);
      }

      int ix = s - 2;
      int iz = len - 2;
      this.walls(-ix, -iz, ix, iz, 0, h - 1, Part.WALL, Part.TRIM);
      this.fill(-ix + 1, 0, -iz + 1, ix - 1, h - 1, iz - 1, Part.AIR);
      this.fill(-ix + 1, -1, -iz + 1, ix - 1, -1, iz - 1, Part.FLOOR);
      this.fill(-1, 0, iz, 1, 3, iz, Part.AIR);

      for (int z = -iz + 2; z <= iz - 2; z += 3) {
         this.put(-ix, 2, z, Part.WINDOW);
         this.put(ix, 2, z, Part.WINDOW);
      }

      this.fill(-s - 1, h, -len - 1, s + 1, h, len + 1, Part.FLOOR);
      this.gable(-s - 1, -len - 1, s + 1, len + 1, h + 1, true);
      this.fill(-1, 0, -iz + 1, 1, 0, -iz + 2, Part.TRIM);
      this.put(-1, 1, -iz + 1, Part.LIGHT);
      this.put(1, 1, -iz + 1, Part.LIGHT);
      this.chest(0, 1, -iz + 1, 0, true);

      for (int z = -iz + 3; z <= iz - 1; z++) {
         this.put(0, 0, z, Part.CARPET);
      }

      for (int z = -iz + 3; z <= iz - 2; z += 3) {
         this.put(-ix + 1, h - 1, z, Part.HANGING_LIGHT);
         this.put(ix - 1, h - 1, z, Part.HANGING_LIGHT);
      }

      this.cellar(-2, -iz + 3, 2, -iz + 7, -5, 0, -iz + 3, 0);
   }

   private void ziggurat() {
      int s = clamp(this.p.size(), 7, 16);
      int tiers = s / 2;

      for (int t = 0; t < tiers; t++) {
         int r = s - t * 2;
         this.fill(-r, t * 2 - 1, -r, r, t * 2, r, Part.WALL);
         this.walls(-r, -r, r, r, t * 2 + 1, t * 2 + 1, Part.TRIM, Part.TRIM);
         this.fill(-r + 1, t * 2 + 1, -r + 1, r - 1, t * 2 + 1, r - 1, Part.AIR);
      }

      int topY = tiers * 2 - 1;

      for (int y = 0; y <= topY; y++) {
         int z = s - y + 1;

         for (int x = -1; x <= 1; x++) {
            this.fill(x, -1, z, x, y - 1, z, Part.WALL);
            this.put(x, y, z, Part.STAIRS, 2, false);
            this.fill(x, y + 1, z, x, y + 3, z, Part.AIR);
         }
      }

      int r = s - (tiers - 1) * 2;
      int sh = Math.max(2, r - 1);
      this.box(-sh, -sh, sh, sh, topY, 3);
      this.door(0, sh, topY, 1, 3);
      this.hipRoof(-sh, -sh, sh, sh, topY + 4);
      this.fill(-sh + 1, topY + 4, -sh + 1, sh - 1, topY + 4, sh - 1, Part.FLOOR);
      this.put(0, topY + 1, 0, Part.LIGHT);
      this.chest(0, topY + 1, -sh + 1, 0, false);
      int c = Math.max(2, s / 3);
      this.fill(-1, 0, -s, 1, 2, 0, Part.AIR);
      this.fill(-c, 0, -c, c, 3, c, Part.AIR);
      this.fill(-c - 1, -1, -c - 1, c + 1, -1, c + 1, Part.FLOOR);

      for (int i = 0; i < 4; i++) {
         if (i != 2) {
            this.put(DX[i] * c, 0, DZ[i] * c, Part.TRIM);
         }
      }

      this.put(0, 0, c - 1, Part.TRIM);
      this.chest(0, 1, c - 1, 2, true);
      this.chest(-c + 1, 0, 0, 3, false);
      this.spawner(c - 1, 0, 0);
      this.put(-c, 3, -c, Part.HANGING_LIGHT);
      this.put(c, 3, c, Part.HANGING_LIGHT);

      for (int z = -3; z > -s; z -= 3) {
         this.put(0, 2, z, Part.HANGING_LIGHT);
      }
   }

   private void crypt() {
      int s = clamp(this.p.size(), 4, 9);
      this.fill(-3, -1, -3, 3, -1, 3, Part.FOUNDATION);
      this.walls(-3, -3, 3, 3, 0, 4, Part.WALL, Part.TRIM);
      this.fill(-2, 0, -2, 2, 4, 2, Part.AIR);
      this.gable(-3, -3, 3, 3, 5, true);
      this.fill(-1, 0, 3, 1, 2, 3, Part.AIR);
      this.arch(-1, 1, 3, 2, true);
      int depth = 9;

      for (int i = 0; i < depth; i++) {
         int y = -1 - i;
         int z = 1 - i;

         for (int x = -1; x <= 1; x++) {
            this.put(x, y, z, Part.STAIRS, 0, false);
            this.fill(x, y + 1, z, x, Math.min(y + 4, 4), z, Part.AIR);
         }

         this.fill(-2, y, z, -2, Math.min(y + 4, -1), z, Part.WALL);
         this.fill(2, y, z, 2, Math.min(y + 4, -1), z, Part.WALL);
         if (y + 5 < 0) {
            this.fill(-1, y + 5, z, 1, y + 5, z, Part.WALL);
         }

         this.fill(-1, y - 1, z, 1, y - 1, z, Part.FOUNDATION);
      }

      int floor = -depth - 1;
      int zEnd = 1 - depth;
      int len = s * 2 + 4;
      int z0 = zEnd - len;
      int z1 = zEnd;
      this.fill(-s - 1, floor - 1, z0 - 1, s + 1, floor + 6, z1, Part.WALL);
      this.fill(-s, floor, z0, s, floor + 4, z1 - 1, Part.AIR);
      this.fill(-s, floor - 1, z0, s, floor - 1, z1 - 1, Part.FLOOR);
      this.fill(-1, floor, z1, 1, floor + 3, z1, Part.AIR);

      for (int z = z0 + 2; z <= z1 - 3; z += 3) {
         for (int side = -1; side <= 1; side += 2) {
            int x = side * s;
            this.fill(x, floor, z, x, floor + 4, z, Part.TRIM);
            int nx = x - side;
            this.put(nx, floor, z + 1, Part.SLAB, 0, true);
         }
      }

      for (int z = z0 + 1; z <= z1 - 2; z += 4) {
         this.put(0, floor + 4, z, Part.HANGING_LIGHT);
      }

      for (int i = 0; i < 2; i++) {
         int x = i == 0 ? -s : s;
         this.put(x, floor, z0 + 1, Part.COBWEB);
         this.put(x, floor + 3, z1 - 2, Part.COBWEB);
      }

      this.spawner(0, floor, (z0 + z1) / 2);
      this.fill(-1, floor, z0, 1, floor, z0, Part.TRIM);
      this.chest(0, floor + 1, z0, 0, true);
      this.chest(-s, floor, z1 - 2, 3, false);
   }

   private void hamlet() {
      int houses = clamp(this.p.floors(), 2, 5);
      int spread = Math.max(clamp(this.p.size(), 8, 18), 9 + houses * 2);
      this.fill(-1, -1, -1, 1, -1, 1, Part.FOUNDATION);
      this.walls(-1, -1, 1, 1, 0, 0, Part.TRIM, Part.TRIM);
      this.put(0, -1, 0, Part.WATER);
      this.put(0, 0, 0, Part.WATER);

      for (int x = -1; x <= 1; x += 2) {
         for (int z = -1; z <= 1; z += 2) {
            this.fill(x, 1, z, x, 2, z, Part.BARS);
         }
      }

      this.fill(-1, 3, -1, 1, 3, 1, Part.ROOF_SLAB);
      double az0 = this.random.nextDouble() * Math.PI * 2.0;

      for (int i = 0; i < houses; i++) {
         double az = az0 + i * Math.PI * 2.0 / houses + this.range(-0.3, 0.3);
         int hx = (int)Math.round(Math.cos(az) * spread);
         int hz = (int)Math.round(Math.sin(az) * spread);
         int facing = Math.abs(hx) > Math.abs(hz) ? (hx > 0 ? 1 : 3) : (hz > 0 ? 2 : 0);
         int[] step = this.cottage(hx, hz, facing, i == 0);
         this.path(0, 0, step[0], step[1]);
      }
   }

   private int[] cottage(int cx, int cz, int facing, boolean big) {
      int w = big ? 3 : 2 + this.random.nextInt(2);
      int d = w + 1 + this.random.nextInt(2);
      boolean alongX = facing == 3 || facing == 1;
      int hw = alongX ? d : w;
      int hd = alongX ? w : d;
      int x0 = cx - hw;
      int x1 = cx + hw;
      int z0 = cz - hd;
      int z1 = cz + hd;
      this.fill(x0 - 1, -1, z0 - 1, x1 + 1, -1, z1 + 1, Part.FOUNDATION);
      this.box(x0, z0, x1, z1, 0, 3);
      this.fill(x0, 4, z0, x1, 4, z1, Part.FLOOR);

      for (int x : new int[]{x0, x1}) {
         for (int z : new int[]{z0, z1}) {
            this.fill(x, 0, z, x, 4, z, Part.TRIM);
         }
      }

      for (int x = x0 + 2; x <= x1 - 2; x += 2) {
         this.put(x, 2, z0, Part.WINDOW);
         this.put(x, 2, z1, Part.WINDOW);
      }

      for (int z = z0 + 2; z <= z1 - 2; z += 2) {
         this.put(x0, 2, z, Part.WINDOW);
         this.put(x1, 2, z, Part.WINDOW);
      }

      int dx = DX[facing];
      int dz = DZ[facing];
      int doorX = dx == 0 ? cx : (dx < 0 ? x0 : x1);
      int doorZ = dz == 0 ? cz : (dz < 0 ? z0 : z1);
      this.fill(doorX, 1, doorZ, doorX, 2, doorZ, Part.AIR);
      this.put(doorX + dx, -1, doorZ + dz, Part.PATH);
      this.gable(x0 - 1, z0 - 1, x1 + 1, z1 + 1, 5, x1 - x0 < z1 - z0);
      int ix = dx == 0 ? cx + 1 : (dx < 0 ? x1 - 1 : x0 + 1);
      int iz = dz == 0 ? cz + 1 : (dz < 0 ? z1 - 1 : z0 + 1);
      this.chest(ix, 1, iz, facing, false);
      this.put(x0 + 1, 1, z0 + 1, Part.WORKSTATION);
      this.put(x1 - 1, 1, z1 - 1, Part.STORAGE);
      this.put(cx, 3, cz, Part.HANGING_LIGHT);
      return new int[]{doorX + dx * 2, doorZ + dz * 2};
   }

   private void shrine() {
      int s = clamp(this.p.size(), 2, 4);
      this.fill(-s - 1, -1, -s - 1, s + 1, -1, s + 1, Part.FOUNDATION);
      this.fill(-s, 0, -s, s, 0, s, Part.FLOOR);

      for (int x = -s; x <= s; x += 2 * s) {
         for (int z = -s; z <= s; z += 2 * s) {
            this.fill(x, 1, z, x, s + 2, z, Part.TRIM);
         }
      }

      this.fill(-s, s + 3, -s, s, s + 3, s, Part.FLOOR);
      this.hipRoof(-s, -s, s, s, s + 4);
      this.put(0, 1, 0, Part.TRIM);
      this.put(0, 2, 0, Part.LIGHT);
      this.chest(0, 1, -1, 0, false);
      this.put(-1, 1, 1, Part.PLANT);
      this.put(1, 1, 1, Part.PLANT);

      for (int i = 0; i < 4; i++) {
         this.put(DX[i] * (s + 1), 0, DZ[i] * (s + 1), Part.SLAB);
      }
   }

   private void watchtower() {
      int h = 8 + clamp(this.p.size(), 0, 8);

      for (int x = -2; x <= 2; x += 4) {
         for (int z = -2; z <= 2; z += 4) {
            this.fill(x, -1, z, x, h + 3, z, Part.TRIM);
         }
      }

      for (int y = 3; y < h; y += 4) {
         this.fill(-2, y, -1, -2, y, 1, Part.BARS);
         this.fill(2, y, -1, 2, y, 1, Part.BARS);
      }

      this.fill(-3, h, -3, 3, h, 3, Part.FLOOR);
      this.walls(-3, -3, 3, 3, h + 1, h + 1, Part.BARS, Part.BARS);
      this.hipRoof(-3, -3, 3, 3, h + 4);
      this.fill(-3, h + 4, -3, 3, h + 4, 3, Part.FLOOR);
      this.fill(0, 0, -1, 0, h, -1, Part.WALL);
      this.ladder(0, 0, 0, h, 0);
      this.put(0, h + 3, 0, Part.HANGING_LIGHT);
      this.chest(2, h + 1, 1, 1, false);
      this.put(-2, h + 1, 1, Part.STORAGE);
   }

   private void observatory() {
      int s = clamp(this.p.size(), 4, 7);
      int floors = clamp(this.p.floors(), 2, 4);
      int top = floors * 4;
      this.disc(0, 0, s + 1.4, -1, Part.FOUNDATION);

      for (int y = 0; y <= top; y++) {
         this.ring(0, 0, s + 0.5, y, y % 4 == 0 && y > 0 ? Part.FLOOR : null);
      }

      for (int k = 0; k < floors; k++) {
         this.sideWindows(s, k * 4 + 2, true);
         this.furnishRound(s, k * 4 + 1, k == 0 ? Part.SHELF : Part.STORAGE);
         this.put(0, k * 4 + 4 - 1, 0, Part.HANGING_LIGHT);
      }

      this.door(0, s, 0, 1, 3);
      this.ladder(0, -s + 1, 1, top, 0);

      for (int y = 0; y <= s; y++) {
         double r = Math.sqrt(Math.max(0.0, (s + 0.5) * (s + 0.5) - y * y));

         for (int x = -s - 1; x <= s + 1; x++) {
            for (int z = -s - 1; z <= s + 1; z++) {
               double d = Math.sqrt(x * x + z * z);
               if (d <= r + 0.5) {
                  this.put(x, top + 1 + y, z, d > r - 0.7 ? (y % 3 == 2 ? Part.TRIM : Part.WINDOW) : Part.AIR);
               }
            }
         }
      }

      this.disc(0, 0, 1.6, top + s + 2, Part.WINDOW);

      for (int i = 0; i < 4; i++) {
         this.put(i - 1, top + 1 + i, 0, Part.BARS);
      }

      this.put(-2, top + 1, 0, Part.TRIM);
      this.chest(0, top + 1, -s + 2, 0, true);
      this.put(2, top + 1, 2, Part.LIGHT);
   }

   private void library() {
      int s = clamp(this.p.size(), 5, 9);
      int len = s + 3;
      this.fill(-s - 1, -1, -len - 1, s + 1, -1, len + 1, Part.FOUNDATION);
      this.box(-s, -len, s, len, 0, 7);
      this.fill(-s, 8, -len, s, 8, len, Part.FLOOR);

      for (int x = -s; x <= s; x += 2 * s) {
         for (int z = -len; z <= len; z += 2 * len) {
            this.fill(x, 0, z, x, 8, z, Part.TRIM);
         }
      }

      for (int z = -len + 2; z <= len - 2; z += 3) {
         this.fill(-s, 2, z, -s, 5, z, Part.WINDOW);
         this.fill(s, 2, z, s, 5, z, Part.WINDOW);
      }

      for (int z = -len + 1; z <= len - 1; z++) {
         if ((z + len) % 3 != 2) {
            this.fill(-s + 1, 1, z, -s + 1, 3, z, Part.SHELF);
            this.fill(s - 1, 1, z, s - 1, 3, z, Part.SHELF);
         }
      }

      this.fill(-s + 1, 4, -len + 1, -s + 2, 4, len - 1, Part.FLOOR);
      this.fill(s - 2, 4, -len + 1, s - 1, 4, len - 1, Part.FLOOR);
      this.fill(-s + 2, 5, -len + 1, -s + 2, 5, len - 1, Part.BARS);
      this.fill(s - 2, 5, -len + 1, s - 2, 5, len - 1, Part.BARS);

      for (int z = -len + 1; z <= len - 1; z += 2) {
         this.fill(-s + 1, 5, z, -s + 1, 6, z, Part.SHELF);
         this.fill(s - 1, 5, z, s - 1, 6, z, Part.SHELF);
      }

      this.ladder(-s + 3, -len + 1, 1, 4, 0);
      this.put(-s + 2, 5, -len + 1, Part.AIR);

      for (int z = -len + 1; z <= len - 1; z++) {
         this.put(0, 1, z, Part.CARPET);
      }

      for (int z = -len + 3; z <= len - 3; z += 4) {
         this.fill(-1, 1, z, 1, 1, z, Part.WORKSTATION);
         this.put(0, 7, z, Part.HANGING_LIGHT);
      }

      this.door(0, len, 0, 1, 3);
      this.gable(-s - 1, -len - 1, s + 1, len + 1, 9, true);
      this.chest(0, 1, -len + 1, 0, true);
      this.chest(-s + 1, 5, len - 1, 3, false);
   }

   private void forge() {
      int s = clamp(this.p.size(), 4, 7);
      int d = s - 1;
      this.fill(-s - 1, -1, -d - 1, s + 1, -1, d + 1, Part.FOUNDATION);
      this.fill(-s, 0, -d, s, 0, d, Part.FLOOR);
      this.walls(-s, -d, s, d, 1, 4, Part.WALL, Part.TRIM);
      this.fill(-s + 1, 1, -d + 1, s - 1, 4, d - 1, Part.AIR);
      this.fill(-s + 1, 1, d, s - 1, 3, d, Part.AIR);

      for (int x = -s; x <= s; x += 3) {
         this.fill(x, 1, d, x, 4, d, Part.TRIM);
      }

      for (int x = -s - 1; x <= s + 1; x++) {
         for (int z = -d - 1; z <= d + 1; z++) {
            int k = d + 1 - z;
            this.put(x, 5 + k / 2, z, Part.ROOF_SLAB, 0, k % 2 == 1);
         }
      }

      this.fill(-s, 1, -d - 2, -s + 2, 9 + s / 2, -d, Part.TRIM);
      this.fill(-s + 1, 1, -d - 1, -s + 1, 9 + s / 2, -d - 1, Part.AIR);
      this.fill(-s + 1, 1, -d, -s + 1, 2, -d, Part.AIR);
      this.put(-s + 1, 1, -d - 1, Part.LIGHT);

      for (int x = -s + 3; x <= s - 1; x += 2) {
         this.put(x, 1, -d + 1, Part.WORKSTATION);
      }

      this.put(s - 1, 1, 0, Part.WORKSTATION);
      this.put(s - 1, 1, d - 1, Part.STORAGE);
      this.put(s - 1, 2, d - 1, Part.STORAGE);
      this.put(s - 2, 1, d - 1, Part.STORAGE);
      this.put(0, 4, 0, Part.HANGING_LIGHT);
      this.chest(-s + 1, 1, d - 1, 3, false);
      this.chest(0, 1, -d + 1, 0, false);
   }

   private void arena() {
      int r = clamp(this.p.size(), 9, 18);
      int tiers = 4;

      for (int x = -r - tiers - 2; x <= r + tiers + 2; x++) {
         for (int z = -r - tiers - 2; z <= r + tiers + 2; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d <= r) {
               this.put(x, -1, z, Part.PATH);
               this.fill(x, 0, z, x, 3, z, Part.AIR);
            } else if (d <= r + tiers + 1.5) {
               int t = Math.min(tiers, (int)(d - r));
               this.fill(x, -1, z, x, t, z, t == 0 ? Part.TRIM : Part.FOUNDATION);
               this.fill(x, t + 1, z, x, t + 3, z, Part.AIR);
               if (t >= 1 && t < tiers) {
                  this.put(x, t, z, Part.SLAB, 0, true);
               }
            } else if (d <= r + tiers + 2.5) {
               this.fill(x, -1, z, x, tiers + 3, z, Part.WALL);
               if ((x + z) % 2 == 0) {
                  this.put(x, tiers + 4, z, Part.TRIM);
               }
            }
         }
      }

      for (int i = 0; i < 4; i++) {
         for (int k = r; k <= r + tiers + 3; k++) {
            for (int w = -1; w <= 1; w++) {
               int x = DX[i] * k + DZ[i] * w;
               int z = DZ[i] * k + DX[i] * w;
               this.put(x, -1, z, Part.PATH);
               this.fill(x, 0, z, x, 2, z, Part.AIR);
            }
         }

         this.put(DX[i] * (r - 1) + DZ[i] * 2, 0, DZ[i] * (r - 1) + DX[i] * 2, Part.LIGHT);
      }

      for (int i = 0; i < 4; i++) {
         double az = (Math.PI / 4) + i * Math.PI / 2.0;
         this.spawner((int)Math.round(Math.cos(az) * (r - 1)), 0, (int)Math.round(Math.sin(az) * (r - 1)));
      }

      this.fill(-1, 0, -1, 1, 0, 1, Part.TRIM);
      this.chest(0, 1, 0, 0, true);
   }

   private void maze() {
      int cells = clamp(this.p.size(), 5, 12) | 1;
      int half = cells;
      int h = 3;
      this.fill(-half - 1, -1, -half - 1, half + 1, -1, half + 1, Part.FOUNDATION);
      this.fill(-half, 0, -half, half, h, half, Part.WALL);
      boolean[][] seen = new boolean[cells][cells];
      List<int[]> ends = new ArrayList<>();
      List<int[]> stack = new ArrayList<>();
      int c = cells / 2;
      stack.add(new int[]{c, c});
      seen[c][c] = true;
      this.carveCell(c, c, half, h);
      boolean pushed = false;

      while (!stack.isEmpty()) {
         int[] cur = stack.get(stack.size() - 1);
         int[] order = new int[]{0, 1, 2, 3};

         for (int i = 3; i > 0; i--) {
            int j = this.random.nextInt(i + 1);
            int t = order[i];
            order[i] = order[j];
            order[j] = t;
         }

         boolean moved = false;

         for (int dir : order) {
            int nx = cur[0] + DX[dir];
            int nz = cur[1] + DZ[dir];
            if (nx >= 0 && nz >= 0 && nx < cells && nz < cells && !seen[nx][nz]) {
               seen[nx][nz] = true;
               int ax = cur[0] * 2 - half + 1;
               int az = cur[1] * 2 - half + 1;
               this.fill(ax + DX[dir], 0, az + DZ[dir], ax + DX[dir], h, az + DZ[dir], Part.AIR);
               this.carveCell(nx, nz, half, h);
               stack.add(new int[]{nx, nz});
               moved = true;
               break;
            }
         }

         if (!moved) {
            int[] done = stack.remove(stack.size() - 1);
            if (pushed) {
               ends.add(done);
            }
         }

         pushed = moved;
      }

      this.fill(-1, 0, half, 1, h, half, Part.AIR);

      for (int x = -half; x <= half; x++) {
         for (int z = -half; z <= half; z++) {
            if (this.plan.get(x, h, z) == Part.WALL) {
               this.put(x, h + 1, z, Part.TRIM);
            }
         }
      }

      int cx = c * 2 - half + 1;
      int cz = c * 2 - half + 1;
      this.chest(cx, 0, cz, 0, true);
      this.put(cx, h, cz, Part.HANGING_LIGHT);
      this.put(cx, h + 1, cz, Part.TRIM);
      int spawners = 0;

      for (int[] e : ends) {
         if (spawners >= 3) {
            break;
         }

         if (this.random.nextDouble() < 0.4) {
            this.spawner(e[0] * 2 - half + 1, 0, e[1] * 2 - half + 1);
            spawners++;
         }
      }
   }

   private void carveCell(int cx, int cz, int half, int h) {
      int x = cx * 2 - half + 1;
      int z = cz * 2 - half + 1;
      this.fill(x, 0, z, x, h, z, Part.AIR);
      this.put(x, -1, z, Part.PATH);
   }

   private void outpost() {
      int s = clamp(this.p.size(), 7, 13);

      for (int x = -s; x <= s; x++) {
         for (int z = -s; z <= s; z++) {
            if (Math.max(Math.abs(x), Math.abs(z)) == s) {
               int h = 3 + (int)(Noise.unit(this.p.seed(), x, 0, z) * 2.0);
               this.fill(x, -1, z, x, h, z, Part.TRIM);
               this.put(x, h + 1, z, Part.BARS);
            } else {
               this.put(x, -1, z, x * x + z * z < 4 ? Part.PATH : Part.FOUNDATION);
            }
         }
      }

      this.fill(-1, 0, s, 1, 5, s, Part.AIR);
      this.put(0, 0, 0, Part.LIGHT);
      int tents = 2 + this.random.nextInt(3);

      for (int i = 0; i < tents; i++) {
         double az = i * Math.PI * 2.0 / tents + this.range(-0.3, 0.3) + (Math.PI / 4);
         int tx = (int)Math.round(Math.cos(az) * s * 0.55);
         int tz = (int)Math.round(Math.sin(az) * s * 0.55);
         this.tent(tx, tz, this.random.nextBoolean());
      }

      this.fill(s - 4, 0, -s + 1, s - 1, 5, -s + 4, Part.AIR);

      for (int x = s - 4; x <= s - 1; x += 3) {
         for (int z = -s + 1; z <= -s + 4; z += 3) {
            this.fill(x, 0, z, x, 5, z, Part.TRIM);
         }
      }

      this.fill(s - 4, 5, -s + 1, s - 1, 5, -s + 4, Part.FLOOR);
      this.ladder(s - 1, -s + 5, 0, 5, 0);
      this.chest(s - 3, 6, -s + 2, 0, false);
      this.put(-s + 1, 0, -s + 1, Part.STORAGE);
      this.put(-s + 2, 0, -s + 1, Part.STORAGE);
      this.put(-s + 1, 1, -s + 1, Part.STORAGE);
      this.chest(-s + 1, 0, -s + 2, 3, true);
   }

   private void tent(int cx, int cz, boolean alongZ) {
      int hw = 2;
      int hl = 3;

      for (int l = -hl; l <= hl; l++) {
         for (int w = -hw; w <= hw; w++) {
            int x = alongZ ? cx + w : cx + l;
            int z = alongZ ? cz + l : cz + w;
            int y = hw - Math.abs(w);
            if (Math.abs(l) == hl) {
               this.fill(x, 0, z, x, y, z, Math.abs(w) == 0 ? Part.AIR : Part.ROOF);
               this.put(x, y + 1, z, Part.ROOF);
            } else {
               this.fill(x, 0, z, x, y, z, Part.AIR);
               int f = alongZ ? (w < 0 ? 3 : 1) : (w < 0 ? 0 : 2);
               this.put(x, y + 1, z, w == 0 ? Part.ROOF : Part.ROOF_STAIRS, f, false);
            }
         }
      }

      this.put(cx, 0, cz, Part.CARPET);
      this.put(alongZ ? cx - 1 : cx, 0, alongZ ? cz : cz - 1, Part.STORAGE);
   }

   private void vault() {
      int s = clamp(this.p.size(), 4, 8);
      int h = 5;
      this.fill(-s - 2, -3, -s - 2, s + 2, h + 1, s + 2, Part.WALL);
      this.fill(-s, -2, -s, s, h - 1, s, Part.AIR);
      this.fill(-s, -3, -s, s, -3, s, Part.FLOOR);

      for (int x = -s - 2; x <= s + 2; x += 2 * s + 4) {
         for (int z = -s - 2; z <= s + 2; z += 2 * s + 4) {
            this.fill(x, -3, z, x, h + 2, z, Part.TRIM);
         }
      }

      this.battlements(-s - 2, -s - 2, s + 2, s + 2, h + 1);

      for (int x = -s + 1; x <= s - 1; x += 2) {
         this.put(x, -2, -s + 1, Part.TRIM);
         this.chest(x, -1, -s + 1, 0, x % 4 == 0);
      }

      this.spawner(-s + 1, -2, s - 1);
      this.spawner(s - 1, -2, s - 1);
      this.put(0, h - 1, 0, Part.HANGING_LIGHT);
      this.put(0, h - 1, -s + 2, Part.HANGING_LIGHT);
      this.fill(-1, -2, -1, 1, -2, 1, Part.CARPET);
   }

   private void graveyard() {
      int s = clamp(this.p.size(), 6, 12);

      for (int x = -s; x <= s; x++) {
         for (int z = -s; z <= s; z++) {
            this.put(x, -1, z, (x + z * 3) % 7 == 0 ? Part.PATH : Part.FOUNDATION);
            if (Math.max(Math.abs(x), Math.abs(z)) == s) {
               this.put(x, 0, z, (x + z) % 4 == 0 ? Part.TRIM : Part.BARS);
            }
         }
      }

      this.fill(-1, 0, s, 1, 1, s, Part.AIR);

      for (int z = -s + 3; z <= s - 3; z += 3) {
         for (int x = -s + 2; x <= s - 2; x += 3) {
            if (Math.abs(x) > 1) {
               this.put(x, 0, z, Part.TRIM);
               if (this.random.nextDouble() < 0.6) {
                  this.put(x, 1, z, Part.SLAB);
               }

               this.put(x, -1, z + 1, Part.PATH);
            }
         }
      }

      for (int z = -s + 2; z <= s - 2; z += 5) {
         this.put(0, 0, z, Part.TRIM);
         this.put(0, 1, z, Part.LIGHT);
      }

      this.fill(-3, 0, -s - 6, 3, 0, -s - 1, Part.FLOOR);
      this.walls(-3, -s - 6, 3, -s - 1, 1, 4, Part.WALL, Part.TRIM);
      this.fill(-2, 1, -s - 5, 2, 4, -s - 2, Part.AIR);
      this.fill(-1, 1, -s - 1, 1, 2, -s - 1, Part.AIR);
      this.fill(-1, 1, -s, 1, 2, -s, Part.AIR);
      this.gable(-3, -s - 6, 3, -s - 1, 5, true);
      this.chest(0, 1, -s - 5, 0, true);
      this.put(-2, 1, -s - 5, Part.LIGHT);
      this.fill(-2, -5, -s - 5, 2, -2, -s - 2, Part.AIR);
      this.walls(-3, -s - 6, 3, -s - 1, -5, -1, Part.WALL, Part.WALL);
      this.fill(-2, -6, -s - 5, 2, -6, -s - 2, Part.FLOOR);
      this.spawner(0, -5, -s - 4);
      this.ladder(2, -s - 2, -5, 0, 2);
      this.chest(-2, -5, -s - 2, 3, false);
   }

   private void lighthouse() {
      int s = clamp(this.p.size(), 3, 5);
      int h = 16 + this.p.floors() * 3;
      this.disc(0, 0, s + 2.5, -1, Part.FOUNDATION);

      for (int y = 0; y <= h; y++) {
         double r = s + 0.5 - y * 0.08;
         boolean floor = y % 4 == 0 && y > 0;
         this.ring(0, 0, r, y, floor ? Part.FLOOR : null);
         if (y / 3 % 2 == 0) {
            this.ringOnly(0, 0, r, y, Part.TRIM);
         }
      }

      double top = s + 0.5 - h * 0.08;
      this.disc(0, 0, top + 1.5, h + 1, Part.FLOOR);
      this.ringOnly(0, 0, top + 1.5, h + 2, Part.BARS);
      this.disc(0, 0, Math.max(1.5, top - 0.5), h + 2, Part.AIR);
      this.fill(-1, h + 2, -1, 1, h + 4, 1, Part.WINDOW);
      this.put(0, h + 3, 0, Part.LIGHT);
      this.fill(-1, h + 5, -1, 1, h + 5, 1, Part.ROOF);
      this.put(0, h + 6, 0, Part.TRIM);
      this.door(0, s, 0, 1, 3);
      this.fill(0, 1, 0, 0, h + 1, 0, Part.TRIM);
      this.ladder(0, 0, 1, h + 1, 0);

      for (int y = 3; y < h; y += 5) {
         this.put(0, y, (int)Math.round(s - y * 0.08), Part.WINDOW);
      }

      this.put(0, h + 1, 0, Part.TRIM);
      this.cottage(s + 6, 0, 1, false);
      this.chest(1, 1, -1, 3, false);
   }

   private void column(int x, int z, int h) {
      this.put(x, 0, z, Part.TRIM);
      this.fill(x, 1, z, x, h - 2, z, Part.WALL);
      this.put(x, h - 1, z, Part.TRIM);
   }

   private void cellar(int x0, int z0, int x1, int z1, int y, int lx, int lz, int facing) {
      this.fill(x0 - 1, y - 1, z0 - 1, x1 + 1, y + 4, z1 + 1, Part.WALL);
      this.fill(x0, y, z0, x1, y + 4 - 2, z1, Part.AIR);
      this.fill(x0, y - 1, z0, x1, y - 1, z1, Part.FLOOR);
      this.ladder(lx, lz, y, 0, facing);
      this.spawner((x0 + x1) / 2, y, (z0 + z1) / 2 + (lz == z0 ? 1 : 0));
      this.chest(x0 == lx ? x1 : x0, y, z1 == lz ? z0 : z1, facing, true);
      this.put(x1, y + 4 - 2, z0, Part.HANGING_LIGHT);
      this.put(x0, y, z0 == lz ? z1 : z0, Part.COBWEB);
   }

   private void furnish(int x0, int z0, int x1, int z1, int y, Part kind) {
      for (int x = x0 + 1; x <= x1 - 1; x++) {
         if (this.random.nextDouble() < 0.5) {
            this.put(x, y, z0, kind);
         }
      }

      for (int z = z0 + 1; z <= z1 - 2; z++) {
         if (this.random.nextDouble() < 0.35) {
            this.put(x0, y, z, kind);
         }

         if (this.random.nextDouble() < 0.35) {
            this.put(x1, y, z, kind);
         }
      }
   }

   private void furnishRound(int s, int y, Part kind) {
      for (int i = 0; i < 8; i++) {
         double az = i * Math.PI / 4.0 + 0.3;
         if (this.random.nextDouble() < 0.6) {
            this.put((int)Math.round(Math.cos(az) * (s - 1)), y, (int)Math.round(Math.sin(az) * (s - 1)), kind);
         }
      }
   }

   private void paving(int x0, int z0, int x1, int z1, int y) {
      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            this.put(x, y - 1, z, Noise.unit(this.p.seed() + 3L, x, 0, z) < 0.25 ? Part.FOUNDATION : Part.PATH);
         }
      }
   }

   private void path(int x0, int z0, int x1, int z1) {
      int n = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));

      for (int i = 0; i <= n; i++) {
         int x = x0 + Math.round((float)(x1 - x0) * i / Math.max(1, n));
         int z = z0 + Math.round((float)(z1 - z0) * i / Math.max(1, n));
         if (this.plan.get(x, -1, z) == null) {
            this.put(x, -1, z, Part.PATH);
         }
      }
   }

   private void box(int x0, int z0, int x1, int z1, int y, int h) {
      this.fill(x0, y, z0, x1, y, z1, Part.FLOOR);
      this.walls(x0, z0, x1, z1, y + 1, y + h, Part.WALL, Part.TRIM);
      this.fill(x0 + 1, y + 1, z0 + 1, x1 - 1, y + h, z1 - 1, Part.AIR);
   }

   private void walls(int x0, int z0, int x1, int z1, int y0, int y1, Part wall, Part corner) {
      for (int y = y0; y <= y1; y++) {
         for (int x = x0; x <= x1; x++) {
            this.put(x, y, z0, x != x0 && x != x1 ? wall : corner);
            this.put(x, y, z1, x != x0 && x != x1 ? wall : corner);
         }

         for (int z = z0 + 1; z < z1; z++) {
            this.put(x0, y, z, wall);
            this.put(x1, y, z, wall);
         }
      }
   }

   private void door(int side, int s, int y, int halfWidth, int h) {
      for (int w = -halfWidth + 1; w <= halfWidth - 1; w++) {
         int x = DX[side] * s + DZ[side] * w;
         int z = DZ[side] * s + DX[side] * w;
         this.fill(x, y + 1, z, x, y + h - 1, z, Part.AIR);
         this.put(x + DX[side], y - 1, z + DZ[side], Part.PATH);
      }
   }

   private void sideWindows(int s, int y, boolean round) {
      for (int i = 0; i < 4; i++) {
         for (int w = -1; w <= 1; w += 2) {
            int off = s >= 4 ? w * 2 : (w < 0 ? 0 : 99);
            if (off != 99) {
               int x = DX[i] * s + DZ[i] * off;
               int z = DZ[i] * s + DX[i] * off;
               if (round) {
                  double d = Math.sqrt(x * x + z * z);
                  x = (int)Math.round(x * (s + 0.3) / d);
                  z = (int)Math.round(z * (s + 0.3) / d);
               }

               this.put(x, y, z, Part.WINDOW);
            }
         }
      }
   }

   private void ladder(int x, int z, int y0, int y1, int facing) {
      for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
         this.put(x, y, z, Part.LADDER, facing, false);
      }
   }

   private void arch(int x0, int x1, int z, int y, boolean alongX) {
      for (int x = x0 - 1; x <= x1 + 1; x++) {
         this.put(alongX ? x : z, y, alongX ? z : x, Part.TRIM);
      }
   }

   private void crenels(int x0, int z0, int x1, int z1, int y) {
      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            if (edge && (x + z & 1) == 0) {
               this.put(x, y, z, Part.TRIM);
            }
         }
      }
   }

   private void battlements(int x0, int z0, int x1, int z1, int y) {
      this.fill(x0, y, z0, x1, y, z1, Part.FLOOR);
      this.walls(x0, z0, x1, z1, y + 1, y + 1, Part.WALL, Part.TRIM);
      this.crenels(x0, z0, x1, z1, y + 2);
   }

   private void gable(int x0, int z0, int x1, int z1, int y, boolean alongZ) {
      int a0 = alongZ ? x0 : z0;
      int a1 = alongZ ? x1 : z1;
      int b0 = alongZ ? z0 : x0;
      int b1 = alongZ ? z1 : x1;

      for (int i = 0; a0 + i <= a1 - i; i++) {
         int lo = a0 + i;
         int hi = a1 - i;

         for (int b = b0 - 1; b <= b1 + 1; b++) {
            if (lo == hi) {
               this.set2(alongZ, lo, y + i, b, Part.ROOF, 0, false);
            } else {
               this.set2(alongZ, lo, y + i, b, Part.ROOF_STAIRS, alongZ ? 3 : 0, false);
               this.set2(alongZ, hi, y + i, b, Part.ROOF_STAIRS, alongZ ? 1 : 2, false);
               if (b == b0 || b == b1) {
                  for (int a = lo + 1; a < hi; a++) {
                     this.set2(alongZ, a, y + i, b, Part.WALL, 0, false);
                  }
               }
            }
         }
      }
   }

   private void set2(boolean alongZ, int a, int y, int b, Part part, int facing, boolean top) {
      this.put(alongZ ? a : b, y, alongZ ? b : a, part, facing, top);
   }

   private void hipRoof(int x0, int z0, int x1, int z1, int y) {
      int i = 0;

      while (true) {
         int a0 = x0 - 1 + i;
         int a1 = x1 + 1 - i;
         int b0 = z0 - 1 + i;
         int b1 = z1 + 1 - i;
         if (a0 > a1 || b0 > b1) {
            break;
         }

         if (a0 == a1 || b0 == b1) {
            this.fill(a0, y + i, b0, a1, y + i, b1, Part.ROOF);
            break;
         }

         for (int x = a0; x <= a1; x++) {
            this.put(x, y + i, b0, Part.ROOF_STAIRS, 0, false);
            this.put(x, y + i, b1, Part.ROOF_STAIRS, 2, false);
         }

         for (int z = b0 + 1; z < b1; z++) {
            this.put(a0, y + i, z, Part.ROOF_STAIRS, 3, false);
            this.put(a1, y + i, z, Part.ROOF_STAIRS, 1, false);
         }

         i++;
      }
   }

   private void cone(int cx, int cz, double r, int y) {
      for (int i = 0; r - i * 0.7 > 0.3; i++) {
         double rr = r - i * 0.7;
         int reach = (int)Math.ceil(rr);

         for (int x = -reach; x <= reach; x++) {
            for (int z = -reach; z <= reach; z++) {
               double d = Math.sqrt(x * x + z * z);
               if (d <= rr) {
                  this.put(cx + x, y + i, cz + z, d > rr - 1.1 ? Part.ROOF : Part.AIR);
               }
            }
         }
      }
   }

   private void ring(int cx, int cz, double r, int y, Part inside) {
      int reach = (int)Math.ceil(r + 1.0);

      for (int x = -reach; x <= reach; x++) {
         for (int z = -reach; z <= reach; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d <= r + 0.4 && d > r - 0.8) {
               this.put(cx + x, y, cz + z, Part.WALL);
            } else if (d <= r - 0.8) {
               this.put(cx + x, y, cz + z, inside != null ? inside : (y == 0 ? Part.FLOOR : Part.AIR));
            }
         }
      }
   }

   private void ringOnly(int cx, int cz, double r, int y, Part part) {
      int reach = (int)Math.ceil(r + 1.0);

      for (int x = -reach; x <= reach; x++) {
         for (int z = -reach; z <= reach; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d <= r + 0.4 && d > r - 0.8) {
               this.put(cx + x, y, cz + z, part);
            }
         }
      }
   }

   private void disc(int cx, int cz, double r, int y, Part part) {
      int reach = (int)Math.ceil(r);

      for (int x = -reach; x <= reach; x++) {
         for (int z = -reach; z <= reach; z++) {
            if (x * x + z * z <= r * r) {
               this.put(cx + x, y, cz + z, part);
            }
         }
      }
   }

   private void chest(int x, int y, int z, int facing, boolean treasure) {
      this.plan.chest(x, y, z, facing, treasure);
   }

   private void spawner(int x, int y, int z) {
      this.plan.spawner(x, y, z);
   }

   private void ruin() {
      double d = this.p.decay();
      if (!(d <= 0.0)) {
         int[] top = new int[]{0};
         this.plan.forEach((x, y, z, part, facing, up) -> top[0] = Math.max(top[0], y));
         int height = Math.max(4, top[0]);
         long seed = this.p.seed();
         List<int[]> gone = new ArrayList<>();
         List<int[]> webs = new ArrayList<>();
         this.plan.forEach((x, y, z, part, facing, up) -> {
            if (y > 0) {
               double n = (Noise.smooth(seed, x / 6.0, 0.0, z / 6.0) + 1.0) / 2.0;
               double cut = height * (1.0 - d * (0.25 + 0.75 * n)) + 1.0;
               if (y > cut) {
                  gone.add(new int[]{x, y, z});
               } else if (part != Part.AIR && Noise.unit(seed + 1L, x, y, z) < d * 0.08) {
                  gone.add(new int[]{x, y, z});
               } else if (part == Part.AIR && Noise.unit(seed + 2L, x, y, z) < d * 0.015) {
                  webs.add(new int[]{x, y, z});
               }
            }
         });

         for (int[] g : gone) {
            this.plan.remove(g[0], g[1], g[2]);
         }

         for (int[] w : webs) {
            this.put(w[0], w[1], w[2], Part.COBWEB);
         }

         this.plan.chests().removeIf(c -> this.plan.get(c[0], c[1], c[2]) == null || c[1] > 0 && this.plan.get(c[0], c[1] - 1, c[2]) == null);
         this.plan.spawners().removeIf(c -> this.plan.get(c[0], c[1], c[2]) == null);
         List<int[]> growth = new ArrayList<>();
         this.plan.forEach((x, y, z, part, facing, up) -> {
            if (part.solid() && y >= 0 && this.plan.get(x, y + 1, z) == null && Noise.unit(seed + 4L, x, y, z) < d * 0.35) {
               growth.add(new int[]{x, y + 1, z});
            }
         });

         for (int[] g : growth) {
            this.put(g[0], g[1], g[2], Part.PLANT);
         }
      }
   }

   private void fill(int x0, int y0, int z0, int x1, int y1, int z1, Part part) {
      for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
         for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
               this.put(x, y, z, part);
            }
         }
      }
   }

   private void put(int x, int y, int z, Part part) {
      this.plan.set(x, y, z, part, 0, false);
   }

   private void put(int x, int y, int z, Part part, int facing, boolean top) {
      this.plan.set(x, y, z, part, facing, top);
   }

   private double range(double lo, double hi) {
      return lo + this.random.nextDouble() * (hi - lo);
   }

   private static int clamp(int v, int lo, int hi) {
      return Math.max(lo, Math.min(hi, v));
   }
}

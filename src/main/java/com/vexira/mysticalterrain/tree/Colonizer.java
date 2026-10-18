package com.vexira.mysticalterrain.tree;

import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

final class Colonizer {
   private final double influence;
   private final double kill;
   private final double step;
   private final double jitter;
   private final V3 tropism;
   private final SplittableRandom random;
   double[] x = new double[256];
   double[] y = new double[256];
   double[] z = new double[256];
   int[] parent = new int[256];
   boolean[] seed = new boolean[256];
   int size;

   Colonizer(double influence, double kill, double step, V3 tropism, double jitter, SplittableRandom random) {
      this.influence = influence;
      this.kill = kill;
      this.step = step;
      this.tropism = tropism;
      this.jitter = jitter;
      this.random = random;
   }

   int seed(V3 p) {
      int i = this.add(p.x(), p.y(), p.z(), -1);
      this.seed[i] = true;
      return i;
   }

   V3 at(int i) {
      return new V3(this.x[i], this.y[i], this.z[i]);
   }

   void grow(List<V3> attractors, int iterations, int limit) {
      int count = attractors.size();
      double[] ax = new double[count];
      double[] ay = new double[count];
      double[] az = new double[count];
      Colonizer.Grid near = new Colonizer.Grid(count);

      for (int i = 0; i < count; i++) {
         V3 a = attractors.get(i);
         ax[i] = a.x();
         ay[i] = a.y();
         az[i] = a.z();
         near.add(key(this.cell(ax[i]), this.cell(ay[i]), this.cell(az[i])), i);
      }

      int alive = count;
      boolean[] dead = new boolean[count];
      int[] nearest = new int[count];
      double[] nearestD = new double[count];
      Arrays.fill(nearest, -1);
      double reach = this.influence * this.influence;
      int seen = 0;

      for (int round = 0; round < iterations && alive > 0 && this.size < limit; round++) {
         for (int n = seen; n < this.size; n++) {
            int cx = this.cell(this.x[n]);
            int cy = this.cell(this.y[n]);
            int cz = this.cell(this.z[n]);

            for (int i = -1; i <= 1; i++) {
               for (int j = -1; j <= 1; j++) {
                  for (int k = -1; k <= 1; k++) {
                     for (int a = near.first(key(cx + i, cy + j, cz + k)); a >= 0; a = near.next[a]) {
                        if (!dead[a]) {
                           double vx = ax[a] - this.x[n];
                           double vy = ay[a] - this.y[n];
                           double vz = az[a] - this.z[n];
                           double d = vx * vx + vy * vy + vz * vz;
                           if (d < reach && (nearest[a] < 0 || d < nearestD[a] || d == nearestD[a] && this.sooner(n, nearest[a], ax[a], ay[a], az[a]))) {
                              nearest[a] = n;
                              nearestD[a] = d;
                           }
                        }
                     }
                  }
               }
            }
         }

         seen = this.size;
         double[] dx = new double[this.size];
         double[] dy = new double[this.size];
         double[] dz = new double[this.size];
         int[] pulls = new int[this.size];

         for (int a = 0; a < count; a++) {
            if (!dead[a]) {
               int best = nearest[a];
               if (best >= 0) {
                  double vx = ax[a] - this.x[best];
                  double vy = ay[a] - this.y[best];
                  double vz = az[a] - this.z[best];
                  double d = Math.sqrt(vx * vx + vy * vy + vz * vz);
                  if (d < this.kill) {
                     dead[a] = true;
                     alive--;
                  } else {
                     dx[best] += vx / d;
                     dy[best] += vy / d;
                     dz[best] += vz / d;
                     pulls[best]++;
                  }
               }
            }
         }

         int grown = 0;
         int before = this.size;

         for (int i = 0; i < before && this.size < limit; i++) {
            if (pulls[i] != 0) {
               double nx = dx[i] / pulls[i] + this.tropism.x() + this.jitter * (this.random.nextDouble() - 0.5);
               double ny = dy[i] / pulls[i] + this.tropism.y() + this.jitter * (this.random.nextDouble() - 0.5);
               double nz = dz[i] / pulls[i] + this.tropism.z() + this.jitter * (this.random.nextDouble() - 0.5);
               double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
               if (!(len < 1.0E-6)) {
                  this.add(this.x[i] + nx / len * this.step, this.y[i] + ny / len * this.step, this.z[i] + nz / len * this.step, i);
                  grown++;
               }
            }
         }

         if (grown == 0) {
            break;
         }
      }
   }

   private boolean sooner(int n, int m, double px, double py, double pz) {
      int cn = this.order(n, px, py, pz);
      int cm = this.order(m, px, py, pz);
      return cn != cm ? cn < cm : n < m;
   }

   private int order(int n, double px, double py, double pz) {
      return (this.cell(this.x[n]) - this.cell(px) + 1) * 9 + (this.cell(this.y[n]) - this.cell(py) + 1) * 3 + this.cell(this.z[n]) - this.cell(pz) + 1;
   }

   double[] pipes(double exponent) {
      double[] acc = new double[this.size];
      double[] out = new double[this.size];

      for (int i = this.size - 1; i >= 0; i--) {
         out[i] = acc[i] == 0.0 ? 1.0 : Math.pow(acc[i], 1.0 / exponent);
         if (this.parent[i] >= 0 && !this.seed[i]) {
            acc[this.parent[i]] = acc[this.parent[i]] + Math.pow(out[i], exponent);
         }
      }

      return out;
   }

   boolean[] tips() {
      boolean[] tip = new boolean[this.size];
      Arrays.fill(tip, true);

      for (int i = 0; i < this.size; i++) {
         if (this.parent[i] >= 0) {
            tip[this.parent[i]] = false;
         }

         if (this.seed[i]) {
            tip[i] = false;
         }
      }

      return tip;
   }

   private int add(double px, double py, double pz, int from) {
      if (this.size == this.x.length) {
         int n = this.size * 2;
         this.x = Arrays.copyOf(this.x, n);
         this.y = Arrays.copyOf(this.y, n);
         this.z = Arrays.copyOf(this.z, n);
         this.parent = Arrays.copyOf(this.parent, n);
         this.seed = Arrays.copyOf(this.seed, n);
      }

      this.x[this.size] = px;
      this.y[this.size] = py;
      this.z[this.size] = pz;
      this.parent[this.size] = from;
      return this.size++;
   }

   private int cell(double v) {
      return (int)Math.floor(v / this.influence);
   }

   private static long key(int i, int j, int k) {
      return (i & 2097151L) << 42 | (j & 2097151L) << 21 | k & 2097151L;
   }

   private static final class Grid {
      private static final long EMPTY = Long.MIN_VALUE;
      private long[] keys = new long[64];
      private int[] first = new int[64];
      private int[] last = new int[64];
      private int used;
      final int[] next;

      Grid(int capacity) {
         this.next = new int[capacity];
         Arrays.fill(this.keys, Long.MIN_VALUE);
      }

      void add(long key, int index) {
         this.next[index] = -1;
         int slot = this.find(key);
         if (this.keys[slot] == Long.MIN_VALUE) {
            this.keys[slot] = key;
            this.first[slot] = index;
            this.last[slot] = index;
            if (++this.used * 2 > this.keys.length) {
               this.rehash();
            }
         } else {
            this.next[this.last[slot]] = index;
            this.last[slot] = index;
         }
      }

      int first(long key) {
         int slot = this.find(key);
         return this.keys[slot] == Long.MIN_VALUE ? -1 : this.first[slot];
      }

      private int find(long key) {
         int mask = this.keys.length - 1;
         int slot = (int)Noise.mix(key) & mask;

         while (this.keys[slot] != Long.MIN_VALUE && this.keys[slot] != key) {
            slot = slot + 1 & mask;
         }

         return slot;
      }

      private void rehash() {
         long[] oldKeys = this.keys;
         int[] oldFirst = this.first;
         int[] oldLast = this.last;
         this.keys = new long[oldKeys.length * 2];
         this.first = new int[this.keys.length];
         this.last = new int[this.keys.length];
         Arrays.fill(this.keys, Long.MIN_VALUE);

         for (int i = 0; i < oldKeys.length; i++) {
            if (oldKeys[i] != Long.MIN_VALUE) {
               int slot = this.find(oldKeys[i]);
               this.keys[slot] = oldKeys[i];
               this.first[slot] = oldFirst[i];
               this.last[slot] = oldLast[i];
            }
         }
      }
   }
}

package com.vexira.mysticalterrain.boss.ai;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public final class ThreatTable {
   private static final float DECAY = 0.997F;
   private final Map<UUID, Float> threat = new HashMap<>();

   public void add(Entity player, float amount) {
      if (amount > 0.0F) {
         this.threat.merge(player.getUUID(), amount, Float::sum);
      }
   }

   public float get(Entity player) {
      return this.threat.getOrDefault(player.getUUID(), 0.0F);
   }

   public void clear() {
      this.threat.clear();
   }

   public Map<UUID, Float> view() {
      return Collections.unmodifiableMap(this.threat);
   }

   public void tick(LivingEntity boss, List<ServerPlayer> engaged) {
      this.threat.replaceAll((k, v) -> v * 0.997F);
      this.threat.values().removeIf(v -> v < 0.05F);

      for (ServerPlayer p : engaged) {
         double d = p.distanceTo(boss) - boss.getBbWidth() * 0.5;
         if (d < 8.0) {
            this.threat.merge(p.getUUID(), (float)(8.0 - Math.max(0.0, d)) * 0.01F, Float::sum);
         }
      }
   }

   public @Nullable ServerPlayer pick(List<ServerPlayer> engaged, @Nullable LivingEntity current, LivingEntity boss) {
      ServerPlayer best = null;
      float bestThreat = 0.0F;

      for (ServerPlayer p : engaged) {
         float t = this.get(p);
         if (best == null || t > bestThreat) {
            best = p;
            bestThreat = t;
         }
      }

      if (best == null) {
         return null;
      }

      if (bestThreat < 0.5F) {
         ServerPlayer nearest = null;
         double nearestD = Double.MAX_VALUE;

         for (ServerPlayer p : engaged) {
            double d = p.distanceToSqr(boss);
            if (d < nearestD) {
               nearest = p;
               nearestD = d;
            }
         }

         return current instanceof ServerPlayer cp && engaged.contains(cp) ? cp : nearest;
      } else {
         return current instanceof ServerPlayer cp && cp != best && engaged.contains(cp) && this.get(cp) * 1.15F + 3.0F >= bestThreat ? cp : best;
      }
   }
}

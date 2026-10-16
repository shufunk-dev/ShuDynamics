package com.vexira.mysticalterrain.boss.ai;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jspecify.annotations.Nullable;

public final class ThreatTable {
   private static final float DECAY = 0.997F;
   private final Map<UUID, Float> threat = new HashMap<>();

   public void add(Entity player, float amount) {
      if (amount > 0.0F) {
         this.threat.merge(player.getUuid(), amount, Float::sum);
      }
   }

   public float get(Entity player) {
      return this.threat.getOrDefault(player.getUuid(), 0.0F);
   }

   public void clear() {
      this.threat.clear();
   }

   public Map<UUID, Float> view() {
      return Collections.unmodifiableMap(this.threat);
   }

   public void tick(LivingEntity boss, List<ServerPlayerEntity> engaged) {
      this.threat.replaceAll((k, v) -> v * 0.997F);
      this.threat.values().removeIf(v -> v < 0.05F);

      for (ServerPlayerEntity p : engaged) {
         double d = p.distanceTo(boss) - boss.getWidth() * 0.5;
         if (d < 8.0) {
            this.threat.merge(p.getUuid(), (float)(8.0 - Math.max(0.0, d)) * 0.01F, Float::sum);
         }
      }
   }

   public @Nullable ServerPlayerEntity pick(List<ServerPlayerEntity> engaged, @Nullable LivingEntity current, LivingEntity boss) {
      ServerPlayerEntity best = null;
      float bestThreat = 0.0F;

      for (ServerPlayerEntity p : engaged) {
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
         ServerPlayerEntity nearest = null;
         double nearestD = Double.MAX_VALUE;

         for (ServerPlayerEntity p : engaged) {
            double d = p.squaredDistanceTo(boss);
            if (d < nearestD) {
               nearest = p;
               nearestD = d;
            }
         }

         return current instanceof ServerPlayerEntity cp && engaged.contains(cp) ? cp : nearest;
      } else {
         return current instanceof ServerPlayerEntity cp && cp != best && engaged.contains(cp) && this.get(cp) * 1.15F + 3.0F >= bestThreat ? cp : best;
      }
   }
}

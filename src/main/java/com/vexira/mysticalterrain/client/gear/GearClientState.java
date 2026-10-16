package com.vexira.mysticalterrain.client.gear;

import com.vexira.mysticalterrain.gear.GearSyncPayload;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class GearClientState {
   static final int FLASH = 8;
   private static final Map<String, long[]> COOLDOWNS = new HashMap<>();
   private static final Map<String, Float> RESONANCE = new HashMap<>();
   private static long ticks;

   private GearClientState() {
   }

   static void tick(MinecraftClient client) {
      if (client.world != null && !client.isPaused()) {
         ticks++;
      }
   }

   static void apply(GearSyncPayload payload) {
      for (long[] cd : COOLDOWNS.values()) {
         cd[0] = Math.min(cd[0], ticks);
      }

      for (GearSyncPayload.Cooldown cd : payload.cooldowns()) {
         COOLDOWNS.put(cd.key(), new long[]{ticks + cd.remaining(), Math.max(1, cd.total())});
      }

      RESONANCE.clear();
      RESONANCE.putAll(payload.resonance());
   }

   static void clear() {
      COOLDOWNS.clear();
      RESONANCE.clear();
   }

   public static int remaining(String key) {
      long[] cd = COOLDOWNS.get(key);
      return cd == null ? 0 : (int)Math.max(0L, cd[0] - ticks);
   }

   public static float fraction(String key, float partial) {
      long[] cd = COOLDOWNS.get(key);
      if (cd == null) {
         return 0.0F;
      }

      float left = (float)(cd[0] - ticks) - partial;
      return left <= 0.0F ? 0.0F : Math.min(1.0F, left / (float)cd[1]);
   }

   public static float flash(String key, float partial) {
      long[] cd = COOLDOWNS.get(key);
      if (cd == null) {
         return 0.0F;
      }

      float since = (float)ticks + partial - (float)cd[0];
      return !(since < 0.0F) && !(since > 8.0F) ? 1.0F - since / 8.0F : 0.0F;
   }

   public static float resonance(String weapon) {
      return RESONANCE.getOrDefault(weapon, 0.0F);
   }
}

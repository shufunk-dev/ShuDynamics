package com.vexira.mysticalterrain.gear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public final class GearState {
   public static final int MAX_RESONANCE = 100;
   private static final int DECAY_DELAY = 200;
   private static final int DECAY_EVERY = 10;
   private static final Map<UUID, GearState> ALL = new HashMap<>();
   private static int now;
   private final Map<String, int[]> cooldowns = new HashMap<>();
   private final Map<String, Float> resonance = new HashMap<>();
   private int lastGain;
   private boolean dirty;
   private @Nullable String charging;
   private long chargeId;
   private int chargeLevel;

   private GearState() {
   }

   public static GearState of(Player player) {
      return ALL.computeIfAbsent(player.getUUID(), id -> new GearState());
   }

   static @Nullable GearState peek(Player player) {
      return ALL.get(player.getUUID());
   }

   static void forget(Player player) {
      ALL.remove(player.getUUID());
   }

   static void clear() {
      ALL.clear();
      now = 0;
   }

   static void time(int tick) {
      now = tick;
   }

   public static int now() {
      return now;
   }

   public int remaining(String key) {
      int[] cd = this.cooldowns.get(key);
      return cd == null ? 0 : Math.max(0, cd[0] - now);
   }

   public boolean ready(String key) {
      return this.remaining(key) <= 0;
   }

   public void cooldown(String key, int ticks) {
      if (ticks > 0) {
         this.cooldowns.put(key, new int[]{now + ticks, ticks});
         this.dirty = true;
      }
   }

   public void clearCooldowns() {
      this.cooldowns.clear();
      this.dirty = true;
   }

   public float resonance(String weapon) {
      return this.resonance.getOrDefault(weapon, 0.0F);
   }

   public void setResonance(String weapon, float value) {
      float v = Math.max(0.0F, Math.min(100.0F, value));
      float old = this.resonance(weapon);
      if (v > old) {
         this.lastGain = now;
      }

      this.resonance.put(weapon, v);
      if ((int)old != (int)v) {
         this.dirty = true;
      }
   }

   public void addResonance(String weapon, float amount) {
      this.setResonance(weapon, this.resonance(weapon) + amount);
   }

   public boolean spendResonance(String weapon, float amount) {
      if (this.resonance(weapon) < amount) {
         return false;
      }

      this.setResonance(weapon, this.resonance(weapon) - amount);
      this.dirty = true;
      return true;
   }

   void beginCharge(String weapon, long animId) {
      this.charging = weapon;
      this.chargeId = animId;
      this.chargeLevel = 0;
   }

   void endCharge() {
      this.charging = null;
      this.chargeLevel = 0;
   }

   @Nullable String charging() {
      return this.charging;
   }

   int chargeLevel() {
      return this.chargeLevel;
   }

   void chargeLevel(int level) {
      this.chargeLevel = level;
   }

   void tick(ServerPlayer player) {
      if (this.charging != null && (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof MysticWeapon w && w.gearId().equals(this.charging)))
         )
       {
         MysticWeapon weapon = MysticGear.WEAPONS.get(this.charging);
         if (weapon != null) {
            weapon.stopTriggeredAnim(player, this.chargeId, "main", "charge");
         }

         this.endCharge();
      }

      if (!this.resonance.isEmpty() && now - this.lastGain > 200 && now % 10 == 0) {
         for (Iterator<Entry<String, Float>> it = this.resonance.entrySet().iterator(); it.hasNext(); this.dirty = true) {
            Entry<String, Float> e = it.next();
            float v = e.getValue() - 1.0F;
            if (v <= 0.0F) {
               it.remove();
            } else {
               e.setValue(v);
            }
         }
      }

      this.cooldowns.values().removeIf(cd -> cd[0] <= now - 40);
      if (this.dirty) {
         this.dirty = false;
         if (ServerPlayNetworking.canSend(player, GearSyncPayload.TYPE)) {
            ServerPlayNetworking.send(player, this.snapshot());
         }
      }
   }

   void markDirty() {
      this.dirty = true;
   }

   GearSyncPayload snapshot() {
      List<GearSyncPayload.Cooldown> cds = new ArrayList<>();
      this.cooldowns.forEach((key, cd) -> {
         if (cd[0] > now) {
            cds.add(new GearSyncPayload.Cooldown(key, cd[0] - now, cd[1]));
         }
      });
      return new GearSyncPayload(cds, new HashMap<>(this.resonance));
   }
}

package com.vexira.mysticalterrain.boss;

import com.vexira.mysticalterrain.MysticalTerrain;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record BossSounds(SoundEvent roar, SoundEvent ambient, SoundEvent hurt, SoundEvent death) {
   public static final String[] BOSSES = new String[]{
      "pyraxis", "hrimthane", "lirael", "grothmaw", "hollow_warden", "bogrot", "sekhmar", "mycelord", "archon", "astrael"
   };
   private static final Map<String, BossSounds> ALL = new HashMap<>();
   private static final Map<String, SoundEvent> EVENTS = new HashMap<>();

   static void register() {
      for (String id : BOSSES) {
         ALL.put(id, new BossSounds(sound(id, "roar"), sound(id, "ambient"), sound(id, "hurt"), sound(id, "death")));
      }

      try (InputStream in = BossSounds.class.getResourceAsStream("/mysticalterrain_sounds.txt")) {
         if (in != null) {
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
               String key = line.trim();
               if (!key.isEmpty()) {
                  Identifier id = MysticalTerrain.id(key);
                  SoundEvent e = BuiltInRegistries.SOUND_EVENT
                     .getOptional(id)
                     .orElseGet(() -> (SoundEvent)Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id)));
                  EVENTS.put(key, e);
               }
            }
         }
      } catch (IOException e) {
         throw new IllegalStateException("Could not read mysticalterrain_sounds.txt", e);
      }
   }

   public static @Nullable SoundEvent event(String key) {
      return EVENTS.get(key);
   }

   public static void play(ServerLevel level, Vec3 at, String key, float volume, float pitch) {
      SoundEvent e = EVENTS.get(key);
      if (e != null) {
         level.playSound(null, at.x, at.y, at.z, e, SoundSource.HOSTILE, volume, pitch);
      }
   }

   public static BossSounds of(String id) {
      BossSounds sounds = ALL.get(id);
      if (sounds == null) {
         throw new IllegalStateException("Boss sounds for " + id + " were not registered");
      } else {
         return sounds;
      }
   }

   private static SoundEvent sound(String boss, String kind) {
      Identifier id = MysticalTerrain.id("boss." + boss + "." + kind);
      return BuiltInRegistries.SOUND_EVENT.getOptional(id).orElseGet(() -> (SoundEvent)Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id)));
   }
}

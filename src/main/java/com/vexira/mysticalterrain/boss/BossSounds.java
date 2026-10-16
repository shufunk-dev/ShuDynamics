package com.vexira.mysticalterrain.boss;

import com.vexira.mysticalterrain.MysticalTerrain;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
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
                  SoundEvent e = Registries.SOUND_EVENT
                     .getOptionalValue(id)
                     .orElseGet(() -> (SoundEvent)Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id)));
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

   public static void play(ServerWorld level, Vec3d at, String key, float volume, float pitch) {
      SoundEvent e = EVENTS.get(key);
      if (e != null) {
         level.playSound(null, at.x, at.y, at.z, e, SoundCategory.HOSTILE, volume, pitch);
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
      return Registries.SOUND_EVENT.getOptionalValue(id).orElseGet(() -> (SoundEvent)Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id)));
   }
}

package com.vexira.mysticalterrain.client.mixin;

import com.vexira.mysticalterrain.MysticalTerrain;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(SoundManager.class)
public abstract class DragonSoundMixin {
   private static final Map<String, String> DRAGON = Map.of(
      "entity.ender_dragon.ambient",
      "boss.ender_dragon.ambient",
      "entity.ender_dragon.growl",
      "boss.ender_dragon.roar",
      "entity.ender_dragon.flap",
      "boss.ender_dragon.flap",
      "entity.ender_dragon.hurt",
      "boss.ender_dragon.hurt",
      "entity.ender_dragon.death",
      "boss.ender_dragon.death",
      "entity.ender_dragon.shoot",
      "boss.ender_dragon.fireball.start"
   );
   private static final RandomSource RANDOM = RandomSource.create();

   @ModifyVariable(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"), argsOnly = true)
   private SoundInstance mysticalterrain$dragonVoice(SoundInstance sound) {
      return swap(sound);
   }

   @ModifyVariable(method = "playDelayed(Lnet/minecraft/client/resources/sounds/SoundInstance;I)V", at = @At("HEAD"), argsOnly = true)
   private SoundInstance mysticalterrain$dragonVoiceDelayed(SoundInstance sound) {
      return swap(sound);
   }

   private static SoundInstance swap(SoundInstance sound) {
      Identifier id = sound.getIdentifier();
      if (id != null && id.getNamespace().equals("minecraft")) {
         String custom = DRAGON.get(id.getPath());
         return (SoundInstance)(custom == null
            ? sound
            : new SimpleSoundInstance(
               MysticalTerrain.id(custom),
               sound.getSource(),
               5.0F,
               0.95F + RANDOM.nextFloat() * 0.1F,
               RANDOM,
               false,
               0,
               sound.getAttenuation(),
               sound.getX(),
               sound.getY(),
               sound.getZ(),
               sound.isRelative()
            ));
      } else {
         return sound;
      }
   }
}

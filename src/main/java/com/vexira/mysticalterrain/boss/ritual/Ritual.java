package com.vexira.mysticalterrain.boss.ritual;

import com.vexira.mysticalterrain.boss.BossEntities;
import com.vexira.mysticalterrain.boss.MysticalBoss;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;

public enum Ritual {
   PYRAXIS("pyraxis", ParticleTypes.FLAME, ParticleTypes.LAVA),
   HRIMTHANE("hrimthane", ParticleTypes.SNOWFLAKE, ParticleTypes.END_ROD),
   LIRAEL("lirael", ParticleTypes.CHERRY_LEAVES, ParticleTypes.END_ROD),
   GROTHMAW("grothmaw", ParticleTypes.HAPPY_VILLAGER, ParticleTypes.CAMPFIRE_COSY_SMOKE),
   HOLLOW_WARDEN("hollow_warden", ParticleTypes.SOUL, ParticleTypes.LARGE_SMOKE),
   BOGROT("bogrot", ParticleTypes.SPLASH, ParticleTypes.SNEEZE),
   SEKHMAR("sekhmar", ParticleTypes.WHITE_ASH, ParticleTypes.SOUL_FIRE_FLAME),
   MYCELORD("mycelord", ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.WARPED_SPORE),
   ARCHON("archon", ParticleTypes.ENCHANT, ParticleTypes.END_ROD),
   ASTRAEL("astrael", ParticleTypes.END_ROD, ParticleTypes.FIREWORK);

   private final String id;
   private final ParticleOptions particle;
   private final ParticleOptions accent;

   Ritual(String id, ParticleOptions particle, ParticleOptions accent) {
      this.id = id;
      this.particle = particle;
      this.accent = accent;
   }

   public String id() {
      return this.id;
   }

   public ParticleOptions particle() {
      return this.particle;
   }

   public ParticleOptions accent() {
      return this.accent;
   }

   public EntityType<? extends MysticalBoss> boss() {
      return BossEntities.BOSSES.get(this.id);
   }

   public int steps() {
      return switch (this) {
         case HRIMTHANE, SEKHMAR -> 4;
         case LIRAEL -> 3;
         default -> 1;
         case HOLLOW_WARDEN -> 6;
         case ARCHON -> 5;
      };
   }
}

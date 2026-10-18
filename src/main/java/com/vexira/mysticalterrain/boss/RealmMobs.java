package com.vexira.mysticalterrain.boss;

import com.vexira.mysticalterrain.MysticalTerrain;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public final class RealmMobs {
   private static final Map<String, RealmMobs.Kind> KINDS = new LinkedHashMap<>();
   private static final Map<EntityType<?>, String> IDS = new HashMap<>();
   public static final Map<String, EntityType<RealmMob>> TYPES = new LinkedHashMap<>();
   private static final RealmMobs.Sounds FIERY = new RealmMobs.Sounds(
      SoundEvents.BLAZE_AMBIENT, SoundEvents.BLAZE_HURT, SoundEvents.BLAZE_DEATH
   );
   private static final RealmMobs.Sounds ROCKY = new RealmMobs.Sounds(
      SoundEvents.RAVAGER_AMBIENT, SoundEvents.IRON_GOLEM_HURT, SoundEvents.IRON_GOLEM_DEATH
   );
   private static final RealmMobs.Sounds ICY = new RealmMobs.Sounds(
      SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundEvents.GLASS_BREAK, SoundEvents.GLASS_BREAK
   );
   private static final RealmMobs.Sounds WOLFISH = new RealmMobs.Sounds(
      SoundEvents.POLAR_BEAR_WARNING, SoundEvents.POLAR_BEAR_HURT, SoundEvents.POLAR_BEAR_DEATH
   );
   private static final RealmMobs.Sounds FAIRY = new RealmMobs.Sounds(
      SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundEvents.ALLAY_HURT, SoundEvents.ALLAY_DEATH
   );
   private static final RealmMobs.Sounds KNIGHTLY = new RealmMobs.Sounds(
      SoundEvents.VINDICATOR_AMBIENT, SoundEvents.VINDICATOR_HURT, SoundEvents.VINDICATOR_DEATH
   );
   private static final RealmMobs.Sounds REPTILE = new RealmMobs.Sounds(
      SoundEvents.SNIFFER_IDLE, SoundEvents.SNIFFER_HURT, SoundEvents.SNIFFER_DEATH
   );
   private static final RealmMobs.Sounds GHOSTLY = new RealmMobs.Sounds(
      SoundEvents.VEX_AMBIENT, SoundEvents.VEX_HURT, SoundEvents.VEX_DEATH
   );
   private static final RealmMobs.Sounds DROWNED = new RealmMobs.Sounds(
      SoundEvents.DROWNED_AMBIENT, SoundEvents.DROWNED_HURT, SoundEvents.DROWNED_DEATH
   );
   private static final RealmMobs.Sounds BUGGY = new RealmMobs.Sounds(
      SoundEvents.SILVERFISH_AMBIENT, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH
   );
   private static final RealmMobs.Sounds HUSKY = new RealmMobs.Sounds(
      SoundEvents.HUSK_AMBIENT, SoundEvents.HUSK_HURT, SoundEvents.HUSK_DEATH
   );
   private static final RealmMobs.Sounds FUNGAL = new RealmMobs.Sounds(
      SoundEvents.SLIME_SQUISH_SMALL, SoundEvents.SLIME_HURT_SMALL, SoundEvents.SLIME_DEATH_SMALL
   );
   private static final RealmMobs.Sounds ARCANE = new RealmMobs.Sounds(
      SoundEvents.BEACON_AMBIENT, SoundEvents.IRON_GOLEM_HURT, SoundEvents.BEACON_DEACTIVATE
   );
   private static final RealmMobs.Sounds STARRY = new RealmMobs.Sounds(
      SoundEvents.AMETHYST_BLOCK_CHIME, SoundEvents.AMETHYST_BLOCK_HIT, SoundEvents.AMETHYST_BLOCK_BREAK
   );

   private RealmMobs() {
   }

   private static void kind(
      String id,
      String boss,
      float width,
      float height,
      double health,
      double damage,
      double speed,
      double armor,
      boolean flies,
      boolean runs,
      double distance,
      double hover,
      int deathTicks,
      ParticleOptions deathParticle,
      RealmMobs.Sounds sounds,
      int spawnWeight,
      int groupMax,
      Consumer<List<BossAction>> actions
   ) {
      KINDS.put(
         id,
         new RealmMobs.Kind(
            id,
            boss,
            width,
            height,
            health,
            damage,
            speed,
            armor,
            flies,
            runs,
            distance,
            hover,
            deathTicks,
            deathParticle,
            sounds,
            spawnWeight,
            groupMax,
            actions
         )
      );
   }

   public static RealmMobs.Kind kind(String id) {
      return KINDS.get(id);
   }

   public static String idOf(EntityType<?> type) {
      return IDS.get(type);
   }

   public static EntityType<RealmMob> type(String id) {
      return TYPES.get(id);
   }

   private static Vec3 front(MysticalBoss b, double d) {
      return b.position().add(Vec3.directionFromRotation(0.0F, b.getYRot()).scale(d));
   }

   private static BossBolt shoot(ServerLevel level, MysticalBoss b, LivingEntity target, BossBolt.Style style, double speed, float damage) {
      if (target == null) {
         return null;
      }

      Vec3 from = b.position().add(0.0, b.getBbHeight() * 0.7, 0.0);
      return BossBolt.shoot(level, b, style, from, target.getEyePosition().subtract(from).normalize().scale(speed), damage);
   }

   private static void fangRing(ServerLevel level, MysticalBoss b, double r, int count) {
      for (int k = 0; k < count; k++) {
         double a = k * Math.PI * 2.0 / count;
         Vec3 at = BossFx.ground(level, b.getX() + Math.cos(a) * r, b.getY(), b.getZ() + Math.sin(a) * r);
         if (at != null) {
            level.addFreshEntity(new EvokerFangs(level, at.x, at.y, at.z, (float)a, 2, b));
         }
      }
   }

   static void register() {
      for (RealmMobs.Kind k : KINDS.values()) {
         ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, MysticalTerrain.id(k.id()));
         Builder<RealmMob> builder = Builder.of(RealmMob::new, MobCategory.MONSTER).sized(k.width(), k.height()).clientTrackingRange(10);
         if (k.boss().equals("pyraxis")) {
            builder = builder.fireImmune();
         }

         EntityType<RealmMob> type = (EntityType<RealmMob>)Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
         TYPES.put(k.id(), type);
         IDS.put(type, k.id());
         FabricDefaultAttributeRegistry.register(
            type,
            Monster.createMonsterAttributes()
               .add(Attributes.MAX_HEALTH, k.health())
               .add(Attributes.ATTACK_DAMAGE, k.damage())
               .add(Attributes.MOVEMENT_SPEED, k.speed())
               .add(Attributes.ARMOR, k.armor())
               .add(Attributes.FOLLOW_RANGE, 32.0)
               .add(Attributes.FLYING_SPEED, k.speed())
         );
         SpawnPlacements.register(
            type, k.flies() ? SpawnPlacementTypes.NO_RESTRICTIONS : SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules
         );
         TagKey<Biome> lairRealm = TagKey.create(Registries.BIOME, MysticalTerrain.id("has_structure/building/lair/" + k.boss()));
         TagKey<Biome> mobRealm = TagKey.create(Registries.BIOME, MysticalTerrain.id("realm_mob_biomes/" + k.boss()));
         BiomeModifications.addSpawn(BiomeSelectors.tag(lairRealm).or(BiomeSelectors.tag(mobRealm)), MobCategory.MONSTER, type, k.spawnWeight(), 1, k.groupMax());
         ResourceKey<Item> eggKey = ResourceKey.create(Registries.ITEM, MysticalTerrain.id(k.id() + "_spawn_egg"));
         MysticalBosses.addToTab((Item)Registry.register(BuiltInRegistries.ITEM, eggKey, new SpawnEggItem(new Properties().setId(eggKey).spawnEgg(type))));
      }
   }

   static {
      kind("cinder_imp", "pyraxis", 0.7F, 1.3F, 20.0, 4.0, 0.3, 2.0, true, false, 6.0, 2.5, 30, ParticleTypes.FLAME, FIERY, 30, 3, out -> {
         out.add(BossAction.of("claw", 0.8, 0.35).range(0.0, 2.8).cooldown(10).hit((b, l, t, i) -> {
            BossFx.melee(l, b, 2.8, 90.0F, 5.0F, 0.4);
            BossFx.cone(l, b, 2.8, 90.0F, e -> e.igniteForSeconds(2.0F));
         }));
         out.add(BossAction.of("fireball", 1.0, 0.5).range(3.0, 18.0).cooldown(50).hit((b, l, t, i) -> shoot(l, b, t, BossBolt.Style.METEOR, 0.8, 5.0F)));
      });
      kind("magma_brute", "pyraxis", 1.4F, 2.6F, 60.0, 9.0, 0.22, 8.0, false, false, 2.5, 0.0, 40, ParticleTypes.LAVA, ROCKY, 20, 1, out -> {
         out.add(BossAction.of("punch", 1.2, 0.55).range(0.0, 3.4).cooldown(14).hit((b, l, t, i) -> {
            BossFx.melee(l, b, 3.4, 70.0F, 9.0F, 1.0);
            BossFx.cone(l, b, 3.4, 70.0F, e -> e.igniteForSeconds(3.0F));
         }));
         out.add(BossAction.of("slam", 1.8, 0.9).range(0.0, 4.0).cooldown(100).hit((b, l, t, i) -> {
            BossFx.shockwave(l, b, front(b, 1.5), 4.0, 10.0F, 1.2);
            BossFx.burst(l, front(b, 1.5), ParticleTypes.LAVA, 12, 1.0, 0.2);
         }));
      });
      kind("frost_wisp", "hrimthane", 0.8F, 1.4F, 18.0, 4.0, 0.28, 2.0, true, false, 7.0, 2.5, 30, ParticleTypes.SNOWFLAKE, ICY, 30, 3, out -> {
         out.add(BossAction.of("shard", 1.0, 0.5).range(0.0, 18.0).cooldown(30).hit((b, l, t, i) -> {
            BossBolt bolt = shoot(l, b, t, BossBolt.Style.ICE, 0.8, 5.0F);
            if (bolt != null) {
               bolt.effect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
            }
         }));
         out.add(BossAction.of("nova", 1.6, 0.8).range(0.0, 5.0).cooldown(120).hit((b, l, t, i) -> {
            BossFx.hurt(l, b, b.position(), 5.0, 6.0F, new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));

            for (LivingEntity e : BossFx.victims(l, b, b.position(), 5.0)) {
               e.setTicksFrozen(e.getTicksRequiredToFreeze() + 80);
            }

            BossFx.ring(l, b.position().add(0.0, 0.5, 0.0), 4.0, 30, ParticleTypes.SNOWFLAKE, 3);
         }));
      });
      kind("rime_stalker", "hrimthane", 1.2F, 1.2F, 30.0, 7.0, 0.34, 3.0, false, true, 2.0, 0.0, 30, ParticleTypes.SNOWFLAKE, WOLFISH, 40, 4, out -> {
         out.add(BossAction.of("bite", 0.8, 0.35).range(0.0, 2.6).cooldown(10).hit((b, l, t, i) -> {
            BossFx.melee(l, b, 2.6, 60.0F, 7.0F, 0.5);
            BossFx.cone(l, b, 2.6, 60.0F, e -> e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), b));
         }));
         out.add(BossAction.of("howl", 1.6, 0.8).range(0.0, 20.0).cooldown(400).anyTarget().hit((b, l, t, i) -> {
            for (RealmMob pack : l.getEntitiesOfClass(RealmMob.class, b.getBoundingBox().inflate(16.0), m -> m.bossId().equals("rime_stalker"))) {
               pack.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1));
               pack.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 0));
            }

            l.playSound(null, b.getX(), b.getY(), b.getZ(), SoundEvents.GOAT_SCREAMING_RAM_IMPACT, SoundSource.HOSTILE, 3.0F, 0.6F);
         }));
      });
      kind(
         "thorn_sprite",
         "lirael",
         0.5F,
         0.8F,
         12.0,
         3.0,
         0.34,
         0.0,
         true,
         false,
         1.5,
         1.2,
         20,
         ParticleTypes.CHERRY_LEAVES,
         FAIRY,
         30,
         4,
         out -> out.add(BossAction.of("dart", 0.8, 0.4).range(0.0, 2.4).cooldown(12).hit((b, l, t, i) -> {
            BossFx.melee(l, b, 2.4, 120.0F, 3.0F, 0.2);
            BossFx.cone(l, b, 2.4, 120.0F, e -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0), b));
         }))
      );
      kind("briar_knight", "lirael", 0.8F, 2.2F, 50.0, 8.0, 0.26, 10.0, false, false, 2.2, 0.0, 40, ParticleTypes.CHERRY_LEAVES, KNIGHTLY, 20, 2, out -> {
         out.add(BossAction.of("slash", 1.2, 0.55).range(0.0, 3.0).cooldown(12).hit((b, l, t, i) -> BossFx.melee(l, b, 3.0, 80.0F, 8.0F, 0.7)));
         out.add(BossAction.of("thorn_guard", 1.6, 0.6).range(0.0, 6.0).cooldown(160).hit((b, l, t, i) -> {
            b.buff(60, 0.4F, 1.0F);
            fangRing(l, b, 2.2, 8);
         }));
      });
      kind("primal_raptor", "grothmaw", 0.9F, 1.6F, 26.0, 6.0, 0.36, 3.0, false, true, 2.0, 0.0, 30, ParticleTypes.CRIT, REPTILE, 40, 4, out -> {
         out.add(BossAction.of("bite", 0.8, 0.35).range(0.0, 2.6).cooldown(8).hit((b, l, t, i) -> BossFx.melee(l, b, 2.6, 60.0F, 6.0F, 0.4)));
         out.add(BossAction.of("pounce", 1.2, 0.6).range(4.0, 10.0).cooldown(100).lockFacing().tick((b, l, t, k) -> {
            if (k == 6 && t != null) {
               Vec3 d = t.position().subtract(b.position());
               b.setDeltaMovement(d.x * 0.16, 0.55, d.z * 0.16);
            }
         }).hit((b, l, t, i) -> BossFx.melee(l, b, 2.6, 120.0F, 8.0F, 0.8)));
      });
      kind(
         "shellback",
         "grothmaw",
         1.6F,
         1.4F,
         70.0,
         9.0,
         0.18,
         14.0,
         false,
         false,
         2.5,
         0.0,
         40,
         ParticleTypes.POOF,
         REPTILE,
         20,
         2,
         out -> {
            out.add(BossAction.of("bite", 1.0, 0.45).range(0.0, 3.0).cooldown(14).hit((b, l, t, i) -> BossFx.melee(l, b, 3.0, 60.0F, 9.0F, 0.8)));
            out.add(
               BossAction.of("roll", 2.0, 1.0).range(4.0, 14.0).cooldown(160).lockFacing().start((b, l, t, i) -> b.buff(40, 0.3F, 1.0F)).tick((b, l, t, k) -> {
                  if (k >= 6 && k < 20 && t != null) {
                     Vec3 d = t.position().subtract(b.position());
                     Vec3 v = new Vec3(d.x, 0.0, d.z).normalize().scale(0.6);
                     b.setDeltaMovement(v.x, b.getDeltaMovement().y, v.z);
                  }
               }).hit((b, l, t, i) -> BossFx.shockwave(l, b, b.position(), 2.8, 10.0F, 1.6))
            );
         }
      );
      kind("shade_wraith", "hollow_warden", 0.8F, 2.4F, 30.0, 6.0, 0.3, 4.0, true, false, 2.5, 0.5, 36, ParticleTypes.LARGE_SMOKE, GHOSTLY, 30, 2, out -> {
         out.add(BossAction.of("claw", 1.0, 0.45).range(0.0, 3.0).cooldown(14).hit((b, l, t, i) -> {
            BossFx.melee(l, b, 3.0, 90.0F, 6.0F, 0.4);
            BossFx.cone(l, b, 3.0, 90.0F, e -> e.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0), b));
         }));
         out.add(BossAction.of("phase_strike", 1.4, 0.8).range(3.0, 14.0).cooldown(120).hit((b, l, t, i) -> {
            if (t != null) {
               Vec3 behind = t.position().add(Vec3.directionFromRotation(0.0F, t.getYRot()).scale(-2.0));
               BossFx.burst(l, b.position().add(0.0, 1.0, 0.0), ParticleTypes.LARGE_SMOKE, 16, 0.4, 0.02);
               b.teleportTo(behind.x, behind.y + 0.3, behind.z);
               b.faceTarget(t, 360.0F);
               BossFx.melee(l, b, 3.0, 120.0F, 8.0F, 0.4);
               t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), b);
            }
         }));
      });
      kind("gloom_hound", "hollow_warden", 1.1F, 1.2F, 32.0, 7.0, 0.34, 3.0, false, true, 2.0, 0.0, 30, ParticleTypes.SMOKE, WOLFISH, 40, 3, out -> {
         out.add(BossAction.of("bite", 0.8, 0.35).range(0.0, 2.6).cooldown(10).hit((b, l, t, i) -> BossFx.melee(l, b, 2.6, 60.0F, 7.0F, 0.5)));
         out.add(BossAction.of("shadow_leap", 1.2, 0.7).range(4.0, 12.0).cooldown(120).lockFacing().tick((b, l, t, k) -> {
            if (k == 6 && t != null) {
               Vec3 d = t.position().subtract(b.position());
               b.setDeltaMovement(d.x * 0.14, 0.5, d.z * 0.14);
               BossFx.burst(l, b.position(), ParticleTypes.SQUID_INK, 10, 0.4, 0.02);
            }
         }).hit((b, l, t, i) -> {
            BossFx.melee(l, b, 2.8, 120.0F, 8.0F, 0.5);
            BossFx.cone(l, b, 2.8, 120.0F, e -> e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0), b));
         }));
      });
      kind("bog_thrall", "bogrot", 0.7F, 2.0F, 34.0, 7.0, 0.24, 4.0, false, false, 2.0, 0.0, 40, ParticleTypes.SPLASH, DROWNED, 40, 3, out -> {
         out.add(BossAction.of("attack", 1.2, 0.55).range(0.0, 2.8).cooldown(12).hit((b, l, t, i) -> BossFx.melee(l, b, 2.8, 70.0F, 7.0F, 0.5)));
         out.add(BossAction.of("retch", 1.6, 0.8).range(0.0, 6.0).cooldown(140).hit((b, l, t, i) -> {
            BossFx.cone(l, b, 6.0, 45.0F, e -> {
               e.hurtServer(l, b.damageSources().mobAttack(b), 3.0F);
               e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), b);
               e.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0), b);
            });
            Vec3 m = b.position().add(Vec3.directionFromRotation(0.0F, b.getYRot()).scale(1.0)).add(0.0, 1.6, 0.0);
            Vec3 dir = Vec3.directionFromRotation(20.0F, b.getYRot());
            l.sendParticles(ParticleTypes.SNEEZE, m.x, m.y, m.z, 0, dir.x, dir.y, dir.z, 0.6);
            l.sendParticles(ParticleTypes.SNEEZE, m.x + dir.x * 2.0, m.y + dir.y * 2.0, m.z + dir.z * 2.0, 20, 0.6, 0.3, 0.6, 0.02);
         }));
      });
      kind(
         "bog_leech",
         "bogrot",
         0.6F,
         0.4F,
         8.0,
         3.0,
         0.26,
         0.0,
         false,
         false,
         1.2,
         0.0,
         20,
         ParticleTypes.SPLASH,
         BUGGY,
         30,
         4,
         out -> out.add(BossAction.of("latch", 0.8, 0.35).range(0.0, 1.8).cooldown(10).hit((b, l, t, i) -> BossFx.cone(l, b, 1.8, 120.0F, e -> {
            if (e.hurtServer(l, b.damageSources().mobAttack(b), 3.0F)) {
               b.heal(3.0F);
               e.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 0), b);
            }
         })))
      );
      kind(
         "scarab",
         "sekhmar",
         0.7F,
         0.5F,
         10.0,
         3.0,
         0.32,
         4.0,
         false,
         false,
         1.2,
         0.0,
         20,
         ParticleTypes.WHITE_ASH,
         BUGGY,
         30,
         5,
         out -> out.add(BossAction.of("bite", 0.6, 0.25).range(0.0, 1.8).cooldown(8).hit((b, l, t, i) -> BossFx.melee(l, b, 1.8, 120.0F, 3.0F, 0.2)))
      );
      kind("tomb_guardian", "sekhmar", 0.8F, 2.2F, 45.0, 8.0, 0.25, 8.0, false, false, 2.2, 0.0, 40, ParticleTypes.WHITE_ASH, HUSKY, 30, 2, out -> {
         out.add(BossAction.of("slash", 1.2, 0.55).range(0.0, 3.0).cooldown(12).hit((b, l, t, i) -> BossFx.melee(l, b, 3.0, 80.0F, 8.0F, 0.6)));
         out.add(BossAction.of("bandage", 1.4, 0.7).range(3.0, 14.0).cooldown(140).hit((b, l, t, i) -> {
            BossBolt bolt = shoot(l, b, t, BossBolt.Style.SAND, 0.8, 4.0F);
            if (bolt != null) {
               bolt.homing(t, 0.08).effect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 5));
            }
         }));
      });
      kind(
         "sporeling",
         "mycelord",
         0.6F,
         0.9F,
         14.0,
         3.0,
         0.26,
         0.0,
         false,
         false,
         1.8,
         0.0,
         24,
         ParticleTypes.SPORE_BLOSSOM_AIR,
         FUNGAL,
         40,
         4,
         out -> out.add(
            BossAction.of("puff", 1.2, 0.6)
               .range(0.0, 3.5)
               .cooldown(60)
               .hit(
                  (b, l, t, i) -> BossFx.zone(
                     l, b, b.position(), 2.4, 60, ParticleTypes.SPORE_BLOSSOM_AIR, 1.0F, new MobEffectInstance(MobEffects.POISON, 60, 0)
                  )
               )
         )
      );
      kind("myconid_shaman", "mycelord", 0.8F, 2.0F, 36.0, 6.0, 0.24, 4.0, false, false, 6.0, 0.0, 40, ParticleTypes.SPORE_BLOSSOM_AIR, FUNGAL, 20, 1, out -> {
         out.add(BossAction.of("staff_hit", 1.2, 0.55).range(0.0, 3.0).cooldown(14).hit((b, l, t, i) -> BossFx.melee(l, b, 3.0, 70.0F, 6.0F, 0.6)));
         out.add(BossAction.of("spore_cast", 1.8, 0.9).range(3.0, 18.0).cooldown(90).hit((b, l, t, i) -> {
            BossBolt bolt = shoot(l, b, t, BossBolt.Style.SPORE, 0.6, 6.0F);
            if (bolt != null) {
               bolt.aoe(2.0).effect(new MobEffectInstance(MobEffects.POISON, 80, 1));
            }
         }));
         out.add(BossAction.of("heal", 1.6, 0.8).range(0.0, 30.0).cooldown(200).anyTarget().hit((b, l, t, i) -> {
            for (MysticalBoss ally : l.getEntitiesOfClass(MysticalBoss.class, b.getBoundingBox().inflate(10.0))) {
               ally.heal(8.0F);
               l.sendParticles(ParticleTypes.HAPPY_VILLAGER, ally.getX(), ally.getY() + ally.getBbHeight(), ally.getZ(), 8, 0.4, 0.4, 0.4, 0.0);
            }
         }));
      });
      kind("rune_golem", "archon", 1.3F, 2.8F, 70.0, 11.0, 0.2, 12.0, false, false, 2.6, 0.0, 40, ParticleTypes.ENCHANT, ARCANE, 20, 1, out -> {
         out.add(BossAction.of("punch", 1.2, 0.55).range(0.0, 3.4).cooldown(14).hit((b, l, t, i) -> BossFx.melee(l, b, 3.4, 70.0F, 11.0F, 1.2)));
         out.add(BossAction.of("rune_slam", 1.8, 0.9).range(0.0, 5.0).cooldown(120).hit((b, l, t, i) -> {
            BossFx.shockwave(l, b, b.position(), 5.0, 10.0F, 1.3);
            BossFx.ring(l, b.position().add(0.0, 0.2, 0.0), 3.0, 24, ParticleTypes.ENCHANTED_HIT, 2);
         }));
      });
      kind("arcane_eye", "archon", 1.0F, 1.0F, 24.0, 3.0, 0.26, 4.0, true, false, 9.0, 3.0, 30, ParticleTypes.END_ROD, ARCANE, 20, 2, out -> {
         out.add(BossAction.of("beam", 2.0, 0.6, 0.9, 1.2, 1.5).range(3.0, 18.0).cooldown(100).tick((b, l, t, k) -> {
            if (t != null && k >= 10 && k <= 32 && b.hasLineOfSight(t)) {
               BossFx.line(l, b.position().add(0.0, 0.5, 0.0), t.getEyePosition(), ParticleTypes.END_ROD, 0.6);
            }
         }).hit((b, l, t, i) -> {
            if (t != null && b.hasLineOfSight(t)) {
               t.hurtServer(l, b.damageSources().indirectMagic(b, b), 3.0F);
            }
         }));
         out.add(BossAction.of("blink", 1.0, 0.6).range(0.0, 5.0).cooldown(100).anyTarget().hit((b, l, t, i) -> {
            Vec3 at = BossFx.around(l, b, b.position(), 6.0, 10.0);
            BossFx.burst(l, b.position(), ParticleTypes.REVERSE_PORTAL, 20, 0.4, 0.1);
            b.teleportTo(at.x, at.y + 3.0, at.z);
         }));
      });
      kind("starling", "astrael", 0.6F, 0.6F, 16.0, 4.0, 0.32, 0.0, true, false, 5.0, 2.0, 24, ParticleTypes.FIREWORK, STARRY, 30, 4, out -> {
         out.add(BossAction.of("zap", 0.8, 0.4).range(0.0, 16.0).cooldown(24).hit((b, l, t, i) -> shoot(l, b, t, BossBolt.Style.STAR, 0.9, 4.0F)));
         out.add(BossAction.of("burst", 1.2, 0.6).range(0.0, 3.5).cooldown(80).hit((b, l, t, i) -> {
            BossFx.hurt(l, b, b.position(), 3.0, 5.0F, new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            BossFx.effect(l, b, b.position(), 3.0, new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));
            BossFx.burst(l, b.position(), ParticleTypes.FIREWORK, 30, 0.3, 0.2);
         }));
      });
      kind("astral_sentinel", "astrael", 0.8F, 2.4F, 55.0, 9.0, 0.26, 10.0, false, false, 2.4, 0.0, 40, ParticleTypes.END_ROD, KNIGHTLY, 20, 1, out -> {
         out.add(BossAction.of("spear", 1.2, 0.55).range(0.0, 3.6).cooldown(14).hit((b, l, t, i) -> BossFx.melee(l, b, 3.6, 60.0F, 9.0F, 0.8)));
         out.add(BossAction.of("smite", 1.8, 0.9).range(0.0, 14.0).cooldown(140).hit((b, l, t, i) -> {
            if (t != null) {
               BossFx.telegraph(l, b, t.position(), 1.8, 16, ParticleTypes.END_ROD, p -> {
                  BossFx.pillar(l, p, 10.0, ParticleTypes.END_ROD, 2);
                  BossFx.hurt(l, b, p, 1.8, 10.0F, null);
               });
            }
         }));
      });
   }

   public record Kind(
      String id,
      String boss,
      float width,
      float height,
      double health,
      double damage,
      double speed,
      double armor,
      boolean flies,
      boolean runs,
      double distance,
      double hover,
      int deathTicks,
      ParticleOptions deathParticle,
      RealmMobs.Sounds sounds,
      int spawnWeight,
      int groupMax,
      Consumer<List<BossAction>> actions
   ) {
   }

   public record Sounds(SoundEvent ambient, SoundEvent hurt, SoundEvent death) {
   }
}

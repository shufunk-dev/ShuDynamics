package com.vexira.mysticalterrain.boss;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.entity.ArchonBoss;
import com.vexira.mysticalterrain.boss.entity.AstraelBoss;
import com.vexira.mysticalterrain.boss.entity.BogrotBoss;
import com.vexira.mysticalterrain.boss.entity.GrothmawBoss;
import com.vexira.mysticalterrain.boss.entity.HollowWardenBoss;
import com.vexira.mysticalterrain.boss.entity.HrimthaneBoss;
import com.vexira.mysticalterrain.boss.entity.LiraelBoss;
import com.vexira.mysticalterrain.boss.entity.MycelordBoss;
import com.vexira.mysticalterrain.boss.entity.PyraxisBoss;
import com.vexira.mysticalterrain.boss.entity.SekhmarBoss;
import com.vexira.mysticalterrain.boss.entity.WardenShade;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.EntityType.EntityFactory;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.SpawnEggItem;

public final class BossEntities {
   public static final Map<String, EntityType<? extends MysticalBoss>> BOSSES = new LinkedHashMap<>();
   public static final EntityType<PyraxisBoss> PYRAXIS = boss("pyraxis", PyraxisBoss::new, 2.4F, 5.0F, true);
   public static final EntityType<HrimthaneBoss> HRIMTHANE = boss("hrimthane", HrimthaneBoss::new, 3.0F, 4.0F, false);
   public static final EntityType<LiraelBoss> LIRAEL = boss("lirael", LiraelBoss::new, 1.2F, 3.4F, false);
   public static final EntityType<GrothmawBoss> GROTHMAW = boss("grothmaw", GrothmawBoss::new, 3.2F, 4.5F, false);
   public static final EntityType<HollowWardenBoss> HOLLOW_WARDEN = boss("hollow_warden", HollowWardenBoss::new, 1.4F, 4.2F, false);
   public static final EntityType<BogrotBoss> BOGROT = boss("bogrot", BogrotBoss::new, 2.6F, 4.8F, false);
   public static final EntityType<SekhmarBoss> SEKHMAR = boss("sekhmar", SekhmarBoss::new, 2.4F, 4.0F, false);
   public static final EntityType<MycelordBoss> MYCELORD = boss("mycelord", MycelordBoss::new, 3.0F, 6.0F, false);
   public static final EntityType<ArchonBoss> ARCHON = boss("archon", ArchonBoss::new, 2.2F, 5.0F, false);
   public static final EntityType<AstraelBoss> ASTRAEL = boss("astrael", AstraelBoss::new, 1.2F, 3.6F, false);
   public static final EntityType<WardenShade> WARDEN_SHADE = register(
      "warden_shade", Builder.of(WardenShade::new, MobCategory.MONSTER).sized(1.0F, 3.6F).clientTrackingRange(10)
   );
   public static final EntityType<BossBolt> BOSS_BOLT = register(
      "boss_bolt", Builder.of(BossBolt::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(2)
   );

   private BossEntities() {
   }

   private static <T extends MysticalBoss> EntityType<T> boss(String id, EntityFactory<T> factory, float width, float height, boolean fireImmune) {
      Builder<T> builder = Builder.of(factory, MobCategory.MONSTER).sized(width, height).clientTrackingRange(16);
      if (fireImmune) {
         builder = builder.fireImmune();
      }

      EntityType<T> type = register(id, builder);
      BOSSES.put(id, type);
      return type;
   }

   private static <T extends Entity> EntityType<T> register(String id, Builder<T> builder) {
      ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, MysticalTerrain.id(id));
      return (EntityType<T>)Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
   }

   static void register() {
      FabricDefaultAttributeRegistry.register(PYRAXIS, PyraxisBoss.attributes());
      FabricDefaultAttributeRegistry.register(HRIMTHANE, HrimthaneBoss.attributes());
      FabricDefaultAttributeRegistry.register(LIRAEL, LiraelBoss.attributes());
      FabricDefaultAttributeRegistry.register(GROTHMAW, GrothmawBoss.attributes());
      FabricDefaultAttributeRegistry.register(HOLLOW_WARDEN, HollowWardenBoss.attributes());
      FabricDefaultAttributeRegistry.register(BOGROT, BogrotBoss.attributes());
      FabricDefaultAttributeRegistry.register(SEKHMAR, SekhmarBoss.attributes());
      FabricDefaultAttributeRegistry.register(MYCELORD, MycelordBoss.attributes());
      FabricDefaultAttributeRegistry.register(ARCHON, ArchonBoss.attributes());
      FabricDefaultAttributeRegistry.register(ASTRAEL, AstraelBoss.attributes());
      FabricDefaultAttributeRegistry.register(WARDEN_SHADE, WardenShade.createAttributes());

      for (Entry<String, EntityType<? extends MysticalBoss>> e : BOSSES.entrySet()) {
         ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MysticalTerrain.id(e.getKey() + "_spawn_egg"));
         Item egg = (Item)Registry.register(BuiltInRegistries.ITEM, key, new SpawnEggItem(new Properties().setId(key).spawnEgg(e.getValue())));
         MysticalBosses.addToTab(egg);
      }
   }
}

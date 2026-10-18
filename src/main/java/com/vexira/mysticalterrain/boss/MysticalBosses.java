package com.vexira.mysticalterrain.boss;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.dragon.DragonRitual;
import com.vexira.mysticalterrain.boss.dragon.VoidPedestalBlock;
import com.vexira.mysticalterrain.boss.ritual.FrostBellBlock;
import com.vexira.mysticalterrain.boss.ritual.GloomBrazierBlock;
import com.vexira.mysticalterrain.boss.ritual.Ritual;
import com.vexira.mysticalterrain.boss.ritual.RitualAltarBlock;
import com.vexira.mysticalterrain.boss.ritual.RitualAltarBlockEntity;
import com.vexira.mysticalterrain.boss.ritual.RunePillarBlock;
import com.vexira.mysticalterrain.boss.vfx.VfxParticles;
import com.vexira.mysticalterrain.gear.MysticGear;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndLevelTick;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class MysticalBosses {
   private static final List<Item> TAB = new ArrayList<>();
   public static final SoundEvent DRAGON_ROAR = sound("boss.dragon.roar");
   public static final SoundEvent DRAGON_SLAM = sound("boss.dragon.slam");
   public static final SoundEvent DRAGON_CHANNEL = sound("boss.dragon.channel");
   public static final SoundEvent DRAGON_SUMMON = sound("boss.dragon.summon");
   public static final SoundEvent RITUAL_ACTIVATE = sound("boss.ritual.activate");
   public static final Item EYE_OF_THE_VOID = item("eye_of_the_void", Item::new, new Properties().rarity(Rarity.EPIC).stacksTo(16));
   public static final Block VOID_PEDESTAL = block(
      "void_pedestal",
      VoidPedestalBlock::new,
      net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_BLACK)
         .strength(-1.0F, 3600000.0F)
         .noLootTable()
         .sound(SoundType.STONE)
         .pushReaction(PushReaction.IMMOVEABLE)
         .lightLevel(state -> state.getValue(VoidPedestalBlock.EYE) ? 10 : 3)
   );
   public static final Item CINDER_HEART = item("cinder_heart", Item::new, new Properties().rarity(Rarity.RARE).fireResistant());
   public static final Item PRIMAL_EGG = item("primal_egg", Item::new, new Properties().rarity(Rarity.RARE).stacksTo(1));
   public static final Item DROWNED_IDOL = item("drowned_idol", Item::new, new Properties().rarity(Rarity.RARE));
   public static final Item[] CANOPIC_JARS = new Item[]{
      item("jackal_canopic_jar", Item::new, new Properties().rarity(Rarity.RARE).stacksTo(1)),
      item("falcon_canopic_jar", Item::new, new Properties().rarity(Rarity.RARE).stacksTo(1)),
      item("baboon_canopic_jar", Item::new, new Properties().rarity(Rarity.RARE).stacksTo(1)),
      item("human_canopic_jar", Item::new, new Properties().rarity(Rarity.RARE).stacksTo(1))
   };
   public static final Item SPORE_HEART = item("spore_heart", Item::new, new Properties().rarity(Rarity.RARE));
   public static final Item STARLIGHT_VIAL = item("starlight_vial", Item::new, new Properties().rarity(Rarity.EPIC).stacksTo(16));
   public static final Map<Ritual, Block> ALTARS = new EnumMap<>(Ritual.class);
   public static final Block FROST_BELL;
   public static final Block GLOOM_BRAZIER;
   public static final Block RUNE_PILLAR;
   public static final BlockEntityType<RitualAltarBlockEntity> ALTAR_ENTITY;
   public static final Map<String, Item> ESSENCES;

   private MysticalBosses() {
   }

   private static net.minecraft.world.level.block.state.BlockBehaviour.Properties fixture(MapColor color) {
      return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
         .mapColor(color)
         .strength(-1.0F, 3600000.0F)
         .noLootTable()
         .pushReaction(PushReaction.IMMOVEABLE)
         .noOcclusion();
   }

   public static int jarIndex(ItemStack stack) {
      for (int i = 0; i < CANOPIC_JARS.length; i++) {
         if (stack.is(CANOPIC_JARS[i])) {
            return i;
         }
      }

      return -1;
   }

   public static void addToTab(Item item) {
      TAB.add(item);
   }

   public static void register() {
      VfxParticles.register();
      BossSounds.register();
      BossEntities.register();
      RealmMobs.register();
      MysticGear.register();
      BossCommand.register();
      Registry.register(
         BuiltInRegistries.CREATIVE_MODE_TAB,
         MysticalTerrain.id("bosses"),
         FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mysticalterrain.bosses"))
            .icon(() -> new ItemStack(EYE_OF_THE_VOID))
            .displayItems((params, entries) -> TAB.forEach(entries::accept))
            .build()
      );
      ServerTickEvents.END_LEVEL_TICK.register((EndLevelTick)level -> {
         if (level.dimension() == Level.END) {
            DragonRitual.tick(level);
         }
      });
   }

   private static SoundEvent sound(String name) {
      Identifier id = MysticalTerrain.id(name);
      return (SoundEvent)Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
   }

   static <T extends Item> T item(String name, Function<Properties, T> factory, Properties properties) {
      ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MysticalTerrain.id(name));
      T item = (T)Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
      TAB.add(item);
      return item;
   }

   static <T extends Block> T block(
      String name, Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, T> factory, net.minecraft.world.level.block.state.BlockBehaviour.Properties properties
   ) {
      ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, MysticalTerrain.id(name));
      T block = (T)Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
      ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, MysticalTerrain.id(name));
      TAB.add((Item)Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Properties().setId(itemKey).useBlockDescriptionPrefix())));
      return block;
   }

   static {
      for (Ritual ritual : Ritual.values()) {
         ALTARS.put(
            ritual,
            block(
               ritual.id() + "_altar",
               p -> new RitualAltarBlock(ritual, p),
               fixture(MapColor.COLOR_BLACK).lightLevel(state -> 4 + (Integer)state.getValue(RitualAltarBlock.STAGE) * 2)
            )
         );
      }

      FROST_BELL = block("frost_bell", FrostBellBlock::new, fixture(MapColor.ICE).sound(SoundType.GLASS).lightLevel(state -> 6));
      GLOOM_BRAZIER = block(
         "gloom_brazier",
         GloomBrazierBlock::new,
         fixture(MapColor.COLOR_PURPLE).sound(SoundType.LANTERN).lightLevel(state -> state.getValue(GloomBrazierBlock.LIT) ? 12 : 0)
      );
      RUNE_PILLAR = block(
         "rune_pillar",
         RunePillarBlock::new,
         fixture(MapColor.COLOR_CYAN).sound(SoundType.DEEPSLATE_BRICKS).lightLevel(state -> state.getValue(RunePillarBlock.CHARGED) ? 13 : 3)
      );
      ALTAR_ENTITY = (BlockEntityType<RitualAltarBlockEntity>)Registry.register(
         BuiltInRegistries.BLOCK_ENTITY_TYPE,
         MysticalTerrain.id("ritual_altar"),
         FabricBlockEntityTypeBuilder.create(RitualAltarBlockEntity::new, ALTARS.values().toArray(Block[]::new)).build()
      );
      ESSENCES = new LinkedHashMap<>();

      for (String id : BossSounds.BOSSES) {
         ESSENCES.put(id, item(id + "_essence", Item::new, new Properties().rarity(Rarity.EPIC).fireResistant()));
      }
   }
}

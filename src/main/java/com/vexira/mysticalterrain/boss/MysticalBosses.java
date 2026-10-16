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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndWorldTick;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.Settings;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;

public final class MysticalBosses {
   private static final List<Item> TAB = new ArrayList<>();
   public static final SoundEvent DRAGON_ROAR = sound("boss.dragon.roar");
   public static final SoundEvent DRAGON_SLAM = sound("boss.dragon.slam");
   public static final SoundEvent DRAGON_CHANNEL = sound("boss.dragon.channel");
   public static final SoundEvent DRAGON_SUMMON = sound("boss.dragon.summon");
   public static final SoundEvent RITUAL_ACTIVATE = sound("boss.ritual.activate");
   public static final Item EYE_OF_THE_VOID = item("eye_of_the_void", Item::new, new Settings().rarity(Rarity.EPIC).maxCount(16));
   public static final Block VOID_PEDESTAL = block(
      "void_pedestal",
      VoidPedestalBlock::new,
      net.minecraft.block.AbstractBlock.Settings.create()
         .mapColor(MapColor.BLACK)
         .strength(-1.0F, 3600000.0F)
         .dropsNothing()
         .sounds(BlockSoundGroup.STONE)
         .pistonBehavior(PistonBehavior.BLOCK)
         .luminance(state -> state.get(VoidPedestalBlock.EYE) ? 10 : 3)
   );
   public static final Item CINDER_HEART = item("cinder_heart", Item::new, new Settings().rarity(Rarity.RARE).fireproof());
   public static final Item PRIMAL_EGG = item("primal_egg", Item::new, new Settings().rarity(Rarity.RARE).maxCount(1));
   public static final Item DROWNED_IDOL = item("drowned_idol", Item::new, new Settings().rarity(Rarity.RARE));
   public static final Item[] CANOPIC_JARS = new Item[]{
      item("jackal_canopic_jar", Item::new, new Settings().rarity(Rarity.RARE).maxCount(1)),
      item("falcon_canopic_jar", Item::new, new Settings().rarity(Rarity.RARE).maxCount(1)),
      item("baboon_canopic_jar", Item::new, new Settings().rarity(Rarity.RARE).maxCount(1)),
      item("human_canopic_jar", Item::new, new Settings().rarity(Rarity.RARE).maxCount(1))
   };
   public static final Item SPORE_HEART = item("spore_heart", Item::new, new Settings().rarity(Rarity.RARE));
   public static final Item STARLIGHT_VIAL = item("starlight_vial", Item::new, new Settings().rarity(Rarity.EPIC).maxCount(16));
   public static final Map<Ritual, Block> ALTARS = new EnumMap<>(Ritual.class);
   public static final Block FROST_BELL;
   public static final Block GLOOM_BRAZIER;
   public static final Block RUNE_PILLAR;
   public static final BlockEntityType<RitualAltarBlockEntity> ALTAR_ENTITY;
   public static final Map<String, Item> ESSENCES;

   private MysticalBosses() {
   }

   private static net.minecraft.block.AbstractBlock.Settings fixture(MapColor color) {
      return net.minecraft.block.AbstractBlock.Settings.create()
         .mapColor(color)
         .strength(-1.0F, 3600000.0F)
         .dropsNothing()
         .pistonBehavior(PistonBehavior.BLOCK)
         .nonOpaque();
   }

   public static int jarIndex(ItemStack stack) {
      for (int i = 0; i < CANOPIC_JARS.length; i++) {
         if (stack.isOf(CANOPIC_JARS[i])) {
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
         Registries.ITEM_GROUP,
         MysticalTerrain.id("bosses"),
         FabricItemGroup.builder()
            .displayName(Text.translatable("itemGroup.mysticalterrain.bosses"))
            .icon(() -> new ItemStack(EYE_OF_THE_VOID))
            .entries((params, entries) -> TAB.forEach(entries::add))
            .build()
      );
      ServerTickEvents.END_WORLD_TICK.register((EndWorldTick)level -> {
         if (level.getRegistryKey() == World.END) {
            DragonRitual.tick(level);
         }
      });
   }

   private static SoundEvent sound(String name) {
      Identifier id = MysticalTerrain.id(name);
      return (SoundEvent)Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
   }

   static <T extends Item> T item(String name, Function<Settings, T> factory, Settings properties) {
      RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, MysticalTerrain.id(name));
      T item = (T)Registry.register(Registries.ITEM, key, factory.apply(properties.registryKey(key)));
      TAB.add(item);
      return item;
   }

   static <T extends Block> T block(
      String name, Function<net.minecraft.block.AbstractBlock.Settings, T> factory, net.minecraft.block.AbstractBlock.Settings properties
   ) {
      RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, MysticalTerrain.id(name));
      T block = (T)Registry.register(Registries.BLOCK, key, factory.apply(properties.registryKey(key)));
      RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, MysticalTerrain.id(name));
      TAB.add((Item)Registry.register(Registries.ITEM, itemKey, new BlockItem(block, new Settings().registryKey(itemKey).useBlockPrefixedTranslationKey())));
      return block;
   }

   static {
      for (Ritual ritual : Ritual.values()) {
         ALTARS.put(
            ritual,
            block(
               ritual.id() + "_altar",
               p -> new RitualAltarBlock(ritual, p),
               fixture(MapColor.BLACK).luminance(state -> 4 + (Integer)state.get(RitualAltarBlock.STAGE) * 2)
            )
         );
      }

      FROST_BELL = block("frost_bell", FrostBellBlock::new, fixture(MapColor.PALE_PURPLE).sounds(BlockSoundGroup.GLASS).luminance(state -> 6));
      GLOOM_BRAZIER = block(
         "gloom_brazier",
         GloomBrazierBlock::new,
         fixture(MapColor.PURPLE).sounds(BlockSoundGroup.LANTERN).luminance(state -> state.get(GloomBrazierBlock.LIT) ? 12 : 0)
      );
      RUNE_PILLAR = block(
         "rune_pillar",
         RunePillarBlock::new,
         fixture(MapColor.CYAN).sounds(BlockSoundGroup.DEEPSLATE_BRICKS).luminance(state -> state.get(RunePillarBlock.CHARGED) ? 13 : 3)
      );
      ALTAR_ENTITY = (BlockEntityType<RitualAltarBlockEntity>)Registry.register(
         Registries.BLOCK_ENTITY_TYPE,
         MysticalTerrain.id("ritual_altar"),
         FabricBlockEntityTypeBuilder.create(RitualAltarBlockEntity::new, ALTARS.values().toArray(Block[]::new)).build()
      );
      ESSENCES = new LinkedHashMap<>();

      for (String id : BossSounds.BOSSES) {
         ESSENCES.put(id, item(id + "_essence", Item::new, new Settings().rarity(Rarity.EPIC).fireproof()));
      }
   }
}

package com.vexira.mysticalterrain.block;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.block.*;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class MysticalBlocks {
   public static final String MOD_ID = "mysticalterrain";
   private static final List<Item> TAB = new ArrayList<>();
   public static final List<WoodSet> WOOD = new ArrayList<>();
   public static final List<Block> LEAVES = new ArrayList<>();
   public static final List<Block> PLANTS = new ArrayList<>();
   private static final List<Block> STONES = new ArrayList<>();
   private static final List<Block> GROUND = new ArrayList<>();

   public static final WoodSet MYSTWOOD = wood("mystwood", MapColor.PURPLE, MapColor.MAGENTA, BlockSoundGroup.WOOD, true);
   public static final Block MYSTWOOD_LEAVES = leaves("mystwood_leaves", MapColor.DARK_GREEN, BlockSoundGroup.GRASS, 0);
   public static final Block STARBLOOM_BLOSSOMS = leaves("starbloom_blossoms", MapColor.PALE_YELLOW, BlockSoundGroup.CHERRY_LEAVES, 0);
   public static final Block RUNESTONE = stone("runestone", MapColor.STONE_GRAY, BlockSoundGroup.STONE);
   public static final Block RUNESTONE_BRICKS = bricks("runestone_bricks", MapColor.STONE_GRAY, BlockSoundGroup.STONE);

   public static final WoodSet MAPLE = wood("maple", MapColor.BROWN, MapColor.ORANGE, BlockSoundGroup.WOOD, true);
   public static final Block MAPLE_LEAVES = leaves("maple_leaves", MapColor.RED, BlockSoundGroup.GRASS, 0);
   public static final Block GOLDEN_MAPLE_LEAVES = leaves("golden_maple_leaves", MapColor.GOLD, BlockSoundGroup.GRASS, 0);
   public static final Block RUSSET_GRANITE = stone("russet_granite", MapColor.DIRT_BROWN, BlockSoundGroup.STONE);
   public static final Block RUSSET_GRANITE_BRICKS = bricks("russet_granite_bricks", MapColor.DIRT_BROWN, BlockSoundGroup.STONE);
   public static final Block LEAF_MOLD = soil("leaf_mold", Blocks.ROOTED_DIRT, MapColor.OAK_TAN);
   public static final Block GOLDENROD = flower("goldenrod", StatusEffects.HASTE, 0.35F, 0);
   public static final Block AUTUMN_FERN = plant("autumn_fern", Blocks.FERN, 0);

   public static final WoodSet RIMEWOOD = wood("rimewood", MapColor.LIGHT_BLUE_GRAY, MapColor.WHITE, BlockSoundGroup.WOOD, true);
   public static final Block RIMEWOOD_LEAVES = leaves("rimewood_leaves", MapColor.PALE_PURPLE, BlockSoundGroup.GRASS, 0);
   public static final Block RIMESTONE = stone("rimestone", MapColor.BLUE, BlockSoundGroup.DEEPSLATE);
   public static final Block RIMESTONE_BRICKS = bricks("rimestone_bricks", MapColor.BLUE, BlockSoundGroup.DEEPSLATE);
   public static final Block PERMAFROST = soil("permafrost", Blocks.DIRT, MapColor.LIGHT_BLUE);
   public static final Block FROST_FERN = plant("frost_fern", Blocks.FERN, 0);
   public static final Block SNOWBELL = flower("snowbell", StatusEffects.SPEED, 8.0F, 0);

   public static final WoodSet CHARWOOD = wood("charwood", MapColor.BLACK, MapColor.BLACK, BlockSoundGroup.NETHER_WOOD, false);
   public static final Block EMBER_LEAVES = leaves("ember_leaves", MapColor.ORANGE, BlockSoundGroup.GRASS, 4);
   public static final Block ASHSTONE = stone("ashstone", MapColor.DEEPSLATE_GRAY, BlockSoundGroup.BASALT);
   public static final Block ASHSTONE_BRICKS = bricks("ashstone_bricks", MapColor.DEEPSLATE_GRAY, BlockSoundGroup.POLISHED_DEEPSLATE);
   public static final Block ASH_SOIL = soil("ash_soil", Blocks.DIRT, MapColor.GRAY);
   public static final Block FIRE_LILY = flower("fire_lily", StatusEffects.FIRE_RESISTANCE, 4.0F, 6);
   public static final Block ASH_GRASS = plant("ash_grass", Blocks.SHORT_GRASS, 0);

   public static final WoodSet SILVERBARK = wood("silverbark", MapColor.TERRACOTTA_WHITE, MapColor.WHITE, BlockSoundGroup.CHERRY_WOOD, true);
   public static final Block SILVERBARK_LEAVES = leaves("silverbark_leaves", MapColor.WHITE_GRAY, BlockSoundGroup.AZALEA_LEAVES, 0);
   public static final Block WISTERIA_BLOSSOMS = leaves("wisteria_blossoms", MapColor.PALE_PURPLE, BlockSoundGroup.CHERRY_LEAVES, 0);
   public static final Block MOONSTONE = stone("moonstone", MapColor.OFF_WHITE, BlockSoundGroup.DEEPSLATE);
   public static final Block MOONSTONE_BRICKS = bricks("moonstone_bricks", MapColor.OFF_WHITE, BlockSoundGroup.DEEPSLATE);
   public static final Block FAE_MOSS = soil("fae_moss", Blocks.MOSS_BLOCK, MapColor.TEAL);
   public static final Block MOONPETAL = flower("moonpetal", StatusEffects.NIGHT_VISION, 5.0F, 0);
   public static final Block GLIMMERCAP = plant("glimmercap", Blocks.BROWN_MUSHROOM, 5);

   public static final WoodSet GINKGO = wood("ginkgo", MapColor.TERRACOTTA_YELLOW, MapColor.ORANGE, BlockSoundGroup.WOOD, true);
   public static final Block GINKGO_LEAVES = leaves("ginkgo_leaves", MapColor.YELLOW, BlockSoundGroup.GRASS, 0);
   public static final Block FOSSIL_STONE = stone("fossil_stone", MapColor.TERRACOTTA_YELLOW, BlockSoundGroup.STONE);
   public static final Block FOSSIL_STONE_BRICKS = bricks("fossil_stone_bricks", MapColor.TERRACOTTA_YELLOW, BlockSoundGroup.STONE);
   public static final Block PRIMAL_MOSS = soil("primal_moss", Blocks.MOSS_BLOCK, MapColor.LIME);
   public static final Block HORSETAIL = plant("horsetail", Blocks.SHORT_GRASS, 0);
   public static final Block ROYAL_FERN = plant("royal_fern", Blocks.FERN, 0);

   public static final WoodSet GLOAMWOOD = wood("gloamwood", MapColor.DARK_CRIMSON, MapColor.BLACK, BlockSoundGroup.WOOD, true);
   public static final Block GLOAMWOOD_LEAVES = leaves("gloamwood_leaves", MapColor.DARK_AQUA, BlockSoundGroup.GRASS, 0);
   public static final Block GLOOMSTONE = stone("gloomstone", MapColor.DARK_CRIMSON, BlockSoundGroup.NETHER_BRICKS);
   public static final Block GLOOMSTONE_BRICKS = bricks("gloomstone_bricks", MapColor.DARK_CRIMSON, BlockSoundGroup.NETHER_BRICKS);
   public static final Block GLOAM_MOSS = soil("gloam_moss", Blocks.MOSS_BLOCK, MapColor.DARK_AQUA);
   public static final Block NIGHTSHADE = flower("nightshade", StatusEffects.BLINDNESS, 9.0F, 0);
   public static final Block DUSKBELL = flower("duskbell", StatusEffects.INVISIBILITY, 8.0F, 0);

   public static final WoodSet PALM = wood("palm", MapColor.TERRACOTTA_YELLOW, MapColor.PALE_YELLOW, BlockSoundGroup.BAMBOO_WOOD, true);
   public static final Block PALM_FRONDS = leaves("palm_fronds", MapColor.EMERALD_GREEN, BlockSoundGroup.GRASS, 0);
   public static final Block MANGO_LEAVES = leaves("mango_leaves", MapColor.EMERALD_GREEN, BlockSoundGroup.GRASS, 0);
   public static final Block HIBISCUS = flower("hibiscus", StatusEffects.STRENGTH, 8.0F, 0);
   public static final Block BIRD_OF_PARADISE = flower("bird_of_paradise", StatusEffects.JUMP_BOOST, 5.0F, 0);

   public static final WoodSet CYPRESS = wood("cypress", MapColor.SPRUCE_BROWN, MapColor.ORANGE, BlockSoundGroup.WOOD, true);
   public static final Block CYPRESS_LEAVES = leaves("cypress_leaves", MapColor.DARK_GREEN, BlockSoundGroup.GRASS, 0);
   public static final Block MIRESTONE = stone("mirestone", MapColor.DARK_GREEN, BlockSoundGroup.DEEPSLATE);
   public static final Block MIRESTONE_BRICKS = bricks("mirestone_bricks", MapColor.DARK_GREEN, BlockSoundGroup.POLISHED_DEEPSLATE);
   public static final Block PEAT = soil("peat", Blocks.MUD, MapColor.BROWN);
   public static final Block CATTAIL = plant("cattail", Blocks.SHORT_GRASS, 0);
   public static final Block MARSH_GRASS = plant("marsh_grass", Blocks.SHORT_GRASS, 0);

   public static final WoodSet OLIVE = wood("olive", MapColor.TERRACOTTA_YELLOW, MapColor.GRAY, BlockSoundGroup.WOOD, true);
   public static final Block OLIVE_LEAVES = leaves("olive_leaves", MapColor.DARK_GREEN, BlockSoundGroup.GRASS, 0);
   public static final Block SANDROCK = stone("sandrock", MapColor.PALE_YELLOW, BlockSoundGroup.STONE);
   public static final Block SANDROCK_BRICKS = bricks("sandrock_bricks", MapColor.PALE_YELLOW, BlockSoundGroup.STONE);
   public static final Block SUNBAKED_CLAY = soil("sunbaked_clay", Blocks.CLAY, MapColor.PALE_YELLOW);
   public static final Block DESERT_SAGE = flower("desert_sage", StatusEffects.WATER_BREATHING, 6.0F, 0);
   public static final Block DUNE_GRASS = plant("dune_grass", Blocks.SHORT_GRASS, 0);

   public static final WoodSet SPOREWOOD = wood("sporewood", MapColor.WHITE, MapColor.WHITE, BlockSoundGroup.NETHER_WOOD, false);
   public static final Block SPORECAP_BLOCK = cap("sporecap_block", MapColor.PURPLE, 0);
   public static final Block GLOWCAP_BLOCK = cap("glowcap_block", MapColor.BRIGHT_TEAL, 12);
   public static final Block SPORE_SOIL = soil("spore_soil", Blocks.ROOTED_DIRT, MapColor.PURPLE);
   public static final Block INKCAP = plant("inkcap", Blocks.BROWN_MUSHROOM, 0);
   public static final Block VIOLET_PUFFBALL = plant("violet_puffball", Blocks.RED_MUSHROOM, 0);

   public static final WoodSet STARBARK = wood("starbark", MapColor.TERRACOTTA_WHITE, MapColor.BLACK, BlockSoundGroup.WOOD, true);
   public static final Block STARBARK_LEAVES = leaves("starbark_leaves", MapColor.TERRACOTTA_WHITE, BlockSoundGroup.AZALEA_LEAVES, 0);
   public static final Block STARSTONE = stone("starstone", MapColor.DARK_AQUA, BlockSoundGroup.NETHER_BRICKS);
   public static final Block STARSTONE_BRICKS = bricks("starstone_bricks", MapColor.DARK_AQUA, BlockSoundGroup.NETHER_BRICKS);
   public static final Block STARFLOWER = flower("starflower", StatusEffects.GLOWING, 10.0F, 7);
   public static final Block VOIDBLOOM = flower("voidbloom", StatusEffects.SLOWNESS, 4.0F, 4);


   private MysticalBlocks() {}

   public static void register() {
      FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();

      for (WoodSet set : WOOD) {
         StrippableBlockRegistry.register(set.log(), set.strippedLog());
         StrippableBlockRegistry.register(set.wood(), set.strippedWood());
         if (set.flammable()) {
            set.logs().forEach(block -> flammable.add(block, 5, 5));
            set.building().forEach(block -> flammable.add(block, 5, 20));
         }
      }

      LEAVES.stream().filter(block -> block != EMBER_LEAVES).forEach(block -> flammable.add(block, 30, 60));
      PLANTS.stream().filter(block -> block != FIRE_LILY && block != ASH_GRASS).forEach(block -> flammable.add(block, 60, 100));

      RegistryKey<ItemGroup> itemGroupKey = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "blocks"));
      Registry.register(
         Registries.ITEM_GROUP,
         itemGroupKey,
         FabricItemGroup.builder()
            .displayName(Text.translatable("itemGroup.mysticalterrain.blocks"))
            .icon(() -> new ItemStack(MAPLE_LEAVES))
            .entries((parameters, output) -> TAB.forEach(output::add))
            .build()
      );

      ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
         for (WoodSet set : WOOD) {
            set.logs().forEach(entries::add);
            set.building().forEach(entries::add);
            entries.add(set.door());
            entries.add(set.trapdoor());
         }
         STONES.forEach(entries::add);
      });

      ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(entries -> {
         WOOD.forEach(set -> entries.add(set.log()));
         LEAVES.forEach(entries::add);
         GROUND.forEach(entries::add);
         PLANTS.forEach(entries::add);
      });
   }

   private static WoodSet wood(String name, MapColor inside, MapColor bark, BlockSoundGroup sound, boolean flammable) {
      Block log = register(name + "_log", PillarBlock::new, log(inside, bark, sound));
      Block wood = register(name + "_wood", PillarBlock::new, log(bark, bark, sound));
      Block strippedLog = register("stripped_" + name + "_log", PillarBlock::new, log(inside, inside, sound));
      Block strippedWood = register("stripped_" + name + "_wood", PillarBlock::new, log(inside, inside, sound));
      Block planks = register(name + "_planks", Block::new, copy(Blocks.OAK_PLANKS, inside, sound));
      Block stairs = register(name + "_stairs", p -> new StairsBlock(planks.getDefaultState(), p), copy(Blocks.OAK_STAIRS, inside, sound));
      Block slab = register(name + "_slab", SlabBlock::new, copy(Blocks.OAK_SLAB, inside, sound));
      Block fence = register(name + "_fence", FenceBlock::new, copy(Blocks.OAK_FENCE, inside, sound));
      Block gate = register(name + "_fence_gate", p -> new FenceGateBlock(WoodType.OAK, p), copy(Blocks.OAK_FENCE_GATE, inside, sound));
      Block door = register(name + "_door", p -> new DoorBlock(BlockSetType.OAK, p), copy(Blocks.OAK_DOOR, inside, sound), (b, s) -> new TallBlockItem(b, s));
      Block trapdoor = register(name + "_trapdoor", p -> new TrapdoorBlock(BlockSetType.OAK, p), copy(Blocks.OAK_TRAPDOOR, inside, sound));
      WoodSet set = new WoodSet(log, wood, strippedLog, strippedWood, planks, stairs, slab, fence, gate, door, trapdoor, flammable);
      WOOD.add(set);
      return set;
   }

   private static Block leaves(String name, MapColor color, BlockSoundGroup sound, int light) {
      Block block = register(name, MysticalLeavesBlock::new, copy(Blocks.OAK_LEAVES, color, sound).luminance(state -> light));
      LEAVES.add(block);
      return block;
   }

   private static Block cap(String name, MapColor color, int light) {
      Block block = register(name, Block::new, AbstractBlock.Settings.copy(Blocks.BROWN_MUSHROOM_BLOCK).mapColor(color).luminance(state -> light));
      GROUND.add(block);
      return block;
   }

   private static Block stone(String name, MapColor color, BlockSoundGroup sound) {
      Block block = register(name, Block::new, copy(Blocks.STONE, color, sound));
      STONES.add(block);
      return block;
   }

   private static Block bricks(String name, MapColor color, BlockSoundGroup sound) {
      Block block = register(name, Block::new, copy(Blocks.STONE_BRICKS, color, sound));
      STONES.add(block);
      return block;
   }

   private static Block soil(String name, Block like, MapColor color) {
      Block block = register(name, Block::new, AbstractBlock.Settings.copy(like).mapColor(color));
      GROUND.add(block);
      return block;
   }

   private static Block flower(String name, RegistryEntry<StatusEffect> effect, float seconds, int light) {
      Block block = register(name, p -> new FlowerBlock(effect, seconds, p), AbstractBlock.Settings.copy(Blocks.DANDELION).luminance(state -> light));
      PLANTS.add(block);
      return block;
   }

   private static Block plant(String name, Block like, int light) {
      Block block = register(name, MysticalPlantBlock::new, AbstractBlock.Settings.copy(like).luminance(state -> light));
      PLANTS.add(block);
      return block;
   }

   private static AbstractBlock.Settings log(MapColor top, MapColor side, BlockSoundGroup sound) {
      return AbstractBlock.Settings.copy(Blocks.OAK_LOG)
         .mapColor(state -> state.get(PillarBlock.AXIS) == net.minecraft.util.math.Direction.Axis.Y ? top : side)
         .sounds(sound);
   }

   private static AbstractBlock.Settings copy(Block base, MapColor color, BlockSoundGroup sound) {
      return AbstractBlock.Settings.copy(base).mapColor(color).sounds(sound);
   }

   private static <T extends Block> T register(String name, Function<AbstractBlock.Settings, T> factory, AbstractBlock.Settings properties) {
      return register(name, factory, properties, BlockItem::new);
   }

   private static <T extends Block> T register(
      String name, Function<AbstractBlock.Settings, T> factory, AbstractBlock.Settings properties, BiFunction<Block, Item.Settings, Item> item
   ) {
      RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, name));
      T block = Registry.register(Registries.BLOCK, key, factory.apply(properties.registryKey(key)));
      RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, name));
      Item blockItem = Registry.register(Registries.ITEM, itemKey, item.apply(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey()));
      TAB.add(blockItem);
      return block;
   }

}

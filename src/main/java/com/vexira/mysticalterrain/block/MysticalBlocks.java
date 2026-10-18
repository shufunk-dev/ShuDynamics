package com.vexira.mysticalterrain.block;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
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

   public static final WoodSet MYSTWOOD = wood("mystwood", MapColor.COLOR_PURPLE, MapColor.COLOR_MAGENTA, SoundType.WOOD, true);
   public static final Block MYSTWOOD_LEAVES = leaves("mystwood_leaves", MapColor.PLANT, SoundType.GRASS, 0);
   public static final Block STARBLOOM_BLOSSOMS = leaves("starbloom_blossoms", MapColor.SAND, SoundType.CHERRY_LEAVES, 0);
   public static final Block RUNESTONE = stone("runestone", MapColor.STONE, SoundType.STONE);
   public static final Block RUNESTONE_BRICKS = bricks("runestone_bricks", MapColor.STONE, SoundType.STONE);

   public static final WoodSet MAPLE = wood("maple", MapColor.COLOR_BROWN, MapColor.COLOR_ORANGE, SoundType.WOOD, true);
   public static final Block MAPLE_LEAVES = leaves("maple_leaves", MapColor.COLOR_RED, SoundType.GRASS, 0);
   public static final Block GOLDEN_MAPLE_LEAVES = leaves("golden_maple_leaves", MapColor.GOLD, SoundType.GRASS, 0);
   public static final Block RUSSET_GRANITE = stone("russet_granite", MapColor.DIRT, SoundType.STONE);
   public static final Block RUSSET_GRANITE_BRICKS = bricks("russet_granite_bricks", MapColor.DIRT, SoundType.STONE);
   public static final Block LEAF_MOLD = soil("leaf_mold", Blocks.ROOTED_DIRT, MapColor.WOOD);
   public static final Block GOLDENROD = flower("goldenrod", MobEffects.HASTE, 0.35F, 0);
   public static final Block AUTUMN_FERN = plant("autumn_fern", Blocks.FERN, 0);

   public static final WoodSet RIMEWOOD = wood("rimewood", MapColor.CLAY, MapColor.SNOW, SoundType.WOOD, true);
   public static final Block RIMEWOOD_LEAVES = leaves("rimewood_leaves", MapColor.ICE, SoundType.GRASS, 0);
   public static final Block RIMESTONE = stone("rimestone", MapColor.COLOR_BLUE, SoundType.DEEPSLATE);
   public static final Block RIMESTONE_BRICKS = bricks("rimestone_bricks", MapColor.COLOR_BLUE, SoundType.DEEPSLATE);
   public static final Block PERMAFROST = soil("permafrost", Blocks.DIRT, MapColor.COLOR_LIGHT_BLUE);
   public static final Block FROST_FERN = plant("frost_fern", Blocks.FERN, 0);
   public static final Block SNOWBELL = flower("snowbell", MobEffects.SPEED, 8.0F, 0);

   public static final WoodSet CHARWOOD = wood("charwood", MapColor.COLOR_BLACK, MapColor.COLOR_BLACK, SoundType.NETHER_WOOD, false);
   public static final Block EMBER_LEAVES = leaves("ember_leaves", MapColor.COLOR_ORANGE, SoundType.GRASS, 4);
   public static final Block ASHSTONE = stone("ashstone", MapColor.DEEPSLATE, SoundType.BASALT);
   public static final Block ASHSTONE_BRICKS = bricks("ashstone_bricks", MapColor.DEEPSLATE, SoundType.POLISHED_DEEPSLATE);
   public static final Block ASH_SOIL = soil("ash_soil", Blocks.DIRT, MapColor.COLOR_GRAY);
   public static final Block FIRE_LILY = flower("fire_lily", MobEffects.FIRE_RESISTANCE, 4.0F, 6);
   public static final Block ASH_GRASS = plant("ash_grass", Blocks.SHORT_GRASS, 0);

   public static final WoodSet SILVERBARK = wood("silverbark", MapColor.TERRACOTTA_WHITE, MapColor.SNOW, SoundType.CHERRY_WOOD, true);
   public static final Block SILVERBARK_LEAVES = leaves("silverbark_leaves", MapColor.WOOL, SoundType.AZALEA_LEAVES, 0);
   public static final Block WISTERIA_BLOSSOMS = leaves("wisteria_blossoms", MapColor.ICE, SoundType.CHERRY_LEAVES, 0);
   public static final Block MOONSTONE = stone("moonstone", MapColor.QUARTZ, SoundType.DEEPSLATE);
   public static final Block MOONSTONE_BRICKS = bricks("moonstone_bricks", MapColor.QUARTZ, SoundType.DEEPSLATE);
   public static final Block FAE_MOSS = soil("fae_moss", Blocks.MOSS_BLOCK, MapColor.WARPED_NYLIUM);
   public static final Block MOONPETAL = flower("moonpetal", MobEffects.NIGHT_VISION, 5.0F, 0);
   public static final Block GLIMMERCAP = plant("glimmercap", Blocks.BROWN_MUSHROOM, 5);

   public static final WoodSet GINKGO = wood("ginkgo", MapColor.TERRACOTTA_YELLOW, MapColor.COLOR_ORANGE, SoundType.WOOD, true);
   public static final Block GINKGO_LEAVES = leaves("ginkgo_leaves", MapColor.COLOR_YELLOW, SoundType.GRASS, 0);
   public static final Block FOSSIL_STONE = stone("fossil_stone", MapColor.TERRACOTTA_YELLOW, SoundType.STONE);
   public static final Block FOSSIL_STONE_BRICKS = bricks("fossil_stone_bricks", MapColor.TERRACOTTA_YELLOW, SoundType.STONE);
   public static final Block PRIMAL_MOSS = soil("primal_moss", Blocks.MOSS_BLOCK, MapColor.COLOR_LIGHT_GREEN);
   public static final Block HORSETAIL = plant("horsetail", Blocks.SHORT_GRASS, 0);
   public static final Block ROYAL_FERN = plant("royal_fern", Blocks.FERN, 0);

   public static final WoodSet GLOAMWOOD = wood("gloamwood", MapColor.CRIMSON_HYPHAE, MapColor.COLOR_BLACK, SoundType.WOOD, true);
   public static final Block GLOAMWOOD_LEAVES = leaves("gloamwood_leaves", MapColor.WARPED_STEM, SoundType.GRASS, 0);
   public static final Block GLOOMSTONE = stone("gloomstone", MapColor.CRIMSON_HYPHAE, SoundType.NETHER_BRICKS);
   public static final Block GLOOMSTONE_BRICKS = bricks("gloomstone_bricks", MapColor.CRIMSON_HYPHAE, SoundType.NETHER_BRICKS);
   public static final Block GLOAM_MOSS = soil("gloam_moss", Blocks.MOSS_BLOCK, MapColor.WARPED_STEM);
   public static final Block NIGHTSHADE = flower("nightshade", MobEffects.BLINDNESS, 9.0F, 0);
   public static final Block DUSKBELL = flower("duskbell", MobEffects.INVISIBILITY, 8.0F, 0);

   public static final WoodSet PALM = wood("palm", MapColor.TERRACOTTA_YELLOW, MapColor.SAND, SoundType.BAMBOO_WOOD, true);
   public static final Block PALM_FRONDS = leaves("palm_fronds", MapColor.EMERALD, SoundType.GRASS, 0);
   public static final Block MANGO_LEAVES = leaves("mango_leaves", MapColor.EMERALD, SoundType.GRASS, 0);
   public static final Block HIBISCUS = flower("hibiscus", MobEffects.STRENGTH, 8.0F, 0);
   public static final Block BIRD_OF_PARADISE = flower("bird_of_paradise", MobEffects.JUMP_BOOST, 5.0F, 0);

   public static final WoodSet CYPRESS = wood("cypress", MapColor.PODZOL, MapColor.COLOR_ORANGE, SoundType.WOOD, true);
   public static final Block CYPRESS_LEAVES = leaves("cypress_leaves", MapColor.PLANT, SoundType.GRASS, 0);
   public static final Block MIRESTONE = stone("mirestone", MapColor.PLANT, SoundType.DEEPSLATE);
   public static final Block MIRESTONE_BRICKS = bricks("mirestone_bricks", MapColor.PLANT, SoundType.POLISHED_DEEPSLATE);
   public static final Block PEAT = soil("peat", Blocks.MUD, MapColor.COLOR_BROWN);
   public static final Block CATTAIL = plant("cattail", Blocks.SHORT_GRASS, 0);
   public static final Block MARSH_GRASS = plant("marsh_grass", Blocks.SHORT_GRASS, 0);

   public static final WoodSet OLIVE = wood("olive", MapColor.TERRACOTTA_YELLOW, MapColor.COLOR_GRAY, SoundType.WOOD, true);
   public static final Block OLIVE_LEAVES = leaves("olive_leaves", MapColor.PLANT, SoundType.GRASS, 0);
   public static final Block SANDROCK = stone("sandrock", MapColor.SAND, SoundType.STONE);
   public static final Block SANDROCK_BRICKS = bricks("sandrock_bricks", MapColor.SAND, SoundType.STONE);
   public static final Block SUNBAKED_CLAY = soil("sunbaked_clay", Blocks.CLAY, MapColor.SAND);
   public static final Block DESERT_SAGE = flower("desert_sage", MobEffects.WATER_BREATHING, 6.0F, 0);
   public static final Block DUNE_GRASS = plant("dune_grass", Blocks.SHORT_GRASS, 0);

   public static final WoodSet SPOREWOOD = wood("sporewood", MapColor.SNOW, MapColor.SNOW, SoundType.NETHER_WOOD, false);
   public static final Block SPORECAP_BLOCK = cap("sporecap_block", MapColor.COLOR_PURPLE, 0);
   public static final Block GLOWCAP_BLOCK = cap("glowcap_block", MapColor.WARPED_WART_BLOCK, 12);
   public static final Block SPORE_SOIL = soil("spore_soil", Blocks.ROOTED_DIRT, MapColor.COLOR_PURPLE);
   public static final Block INKCAP = plant("inkcap", Blocks.BROWN_MUSHROOM, 0);
   public static final Block VIOLET_PUFFBALL = plant("violet_puffball", Blocks.RED_MUSHROOM, 0);

   public static final WoodSet STARBARK = wood("starbark", MapColor.TERRACOTTA_WHITE, MapColor.COLOR_BLACK, SoundType.WOOD, true);
   public static final Block STARBARK_LEAVES = leaves("starbark_leaves", MapColor.TERRACOTTA_WHITE, SoundType.AZALEA_LEAVES, 0);
   public static final Block STARSTONE = stone("starstone", MapColor.WARPED_STEM, SoundType.NETHER_BRICKS);
   public static final Block STARSTONE_BRICKS = bricks("starstone_bricks", MapColor.WARPED_STEM, SoundType.NETHER_BRICKS);
   public static final Block STARFLOWER = flower("starflower", MobEffects.GLOWING, 10.0F, 7);
   public static final Block VOIDBLOOM = flower("voidbloom", MobEffects.SLOWNESS, 4.0F, 4);


   private MysticalBlocks() {}

   public static void register() {
      FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();

      for (WoodSet set : WOOD) {
         if (set.flammable()) {
            set.logs().forEach(block -> flammable.add(block, 5, 5));
            set.building().forEach(block -> flammable.add(block, 5, 20));
         }
      }

      LEAVES.stream().filter(block -> block != EMBER_LEAVES).forEach(block -> flammable.add(block, 30, 60));
      PLANTS.stream().filter(block -> block != FIRE_LILY && block != ASH_GRASS).forEach(block -> flammable.add(block, 60, 100));

      ResourceKey<CreativeModeTab> itemGroupKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MOD_ID, "blocks"));
      Registry.register(
         BuiltInRegistries.CREATIVE_MODE_TAB,
         itemGroupKey,
         FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mysticalterrain.blocks"))
            .icon(() -> new ItemStack(MAPLE_LEAVES))
            .displayItems((parameters, output) -> TAB.forEach(output::accept))
            .build()
      );

      CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
         for (WoodSet set : WOOD) {
            set.logs().forEach(entries::accept);
            set.building().forEach(entries::accept);
            entries.accept(set.door());
            entries.accept(set.trapdoor());
         }
         STONES.forEach(entries::accept);
      });

      CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
         WOOD.forEach(set -> entries.accept(set.log()));
         LEAVES.forEach(entries::accept);
         GROUND.forEach(entries::accept);
         PLANTS.forEach(entries::accept);
      });
   }

   private static WoodSet wood(String name, MapColor inside, MapColor bark, SoundType sound, boolean flammable) {
      Block log = register(name + "_log", RotatedPillarBlock::new, log(inside, bark, sound));
      Block wood = register(name + "_wood", RotatedPillarBlock::new, log(bark, bark, sound));
      Block strippedLog = register("stripped_" + name + "_log", RotatedPillarBlock::new, log(inside, inside, sound));
      Block strippedWood = register("stripped_" + name + "_wood", RotatedPillarBlock::new, log(inside, inside, sound));
      Block planks = register(name + "_planks", Block::new, copy(Blocks.OAK_PLANKS, inside, sound));
      Block stairs = register(name + "_stairs", p -> new StairBlock(planks.defaultBlockState(), p), copy(Blocks.OAK_STAIRS, inside, sound));
      Block slab = register(name + "_slab", SlabBlock::new, copy(Blocks.OAK_SLAB, inside, sound));
      Block fence = register(name + "_fence", FenceBlock::new, copy(Blocks.OAK_FENCE, inside, sound));
      Block gate = register(name + "_fence_gate", p -> new FenceGateBlock(WoodType.OAK, p), copy(Blocks.OAK_FENCE_GATE, inside, sound));
      Block door = register(name + "_door", p -> new DoorBlock(BlockSetType.OAK, p), copy(Blocks.OAK_DOOR, inside, sound), (b, s) -> new DoubleHighBlockItem(b, s));
      Block trapdoor = register(name + "_trapdoor", p -> new TrapDoorBlock(BlockSetType.OAK, p), copy(Blocks.OAK_TRAPDOOR, inside, sound));
      WoodSet set = new WoodSet(log, wood, strippedLog, strippedWood, planks, stairs, slab, fence, gate, door, trapdoor, flammable);
      WOOD.add(set);
      return set;
   }

   private static Block leaves(String name, MapColor color, SoundType sound, int light) {
      Block block = register(name, MysticalLeavesBlock::new, copy(Blocks.OAK_LEAVES, color, sound).lightLevel(state -> light));
      LEAVES.add(block);
      return block;
   }

   private static Block cap(String name, MapColor color, int light) {
      Block block = register(name, Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM_BLOCK).mapColor(color).lightLevel(state -> light));
      GROUND.add(block);
      return block;
   }

   private static Block stone(String name, MapColor color, SoundType sound) {
      Block block = register(name, Block::new, copy(Blocks.STONE, color, sound));
      STONES.add(block);
      return block;
   }

   private static Block bricks(String name, MapColor color, SoundType sound) {
      Block block = register(name, Block::new, copy(Blocks.STONE_BRICKS, color, sound));
      STONES.add(block);
      return block;
   }

   private static Block soil(String name, Block like, MapColor color) {
      Block block = register(name, Block::new, BlockBehaviour.Properties.ofFullCopy(like).mapColor(color));
      GROUND.add(block);
      return block;
   }

   private static Block flower(String name, Holder<MobEffect> effect, float seconds, int light) {
      Block block = register(name, p -> new FlowerBlock(effect, seconds, p), BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION).lightLevel(state -> light));
      PLANTS.add(block);
      return block;
   }

   private static Block plant(String name, Block like, int light) {
      Block block = register(name, MysticalPlantBlock::new, BlockBehaviour.Properties.ofFullCopy(like).lightLevel(state -> light));
      PLANTS.add(block);
      return block;
   }

   private static BlockBehaviour.Properties log(MapColor top, MapColor side, SoundType sound) {
      return BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)
         .mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == net.minecraft.core.Direction.Axis.Y ? top : side)
         .sound(sound);
   }

   private static BlockBehaviour.Properties copy(Block base, MapColor color, SoundType sound) {
      return BlockBehaviour.Properties.ofFullCopy(base).mapColor(color).sound(sound);
   }

   private static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties properties) {
      return register(name, factory, properties, BlockItem::new);
   }

   private static <T extends Block> T register(
      String name, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties properties, BiFunction<Block, Item.Properties, Item> item
   ) {
      ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, name));
      T block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
      ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, name));
      Item blockItem = Registry.register(BuiltInRegistries.ITEM, itemKey, item.apply(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
      TAB.add(blockItem);
      return block;
   }

}

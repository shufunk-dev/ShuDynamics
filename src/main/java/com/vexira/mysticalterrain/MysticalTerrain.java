package com.vexira.mysticalterrain;

import com.vexira.mysticalterrain.biome.MysticalTrees;
import com.vexira.mysticalterrain.block.MysticalBlocks;
import com.vexira.mysticalterrain.feature.*;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MysticalTerrain implements ModInitializer {
   public static final String MOD_ID = "mysticalterrain";
   public static final Logger LOGGER = LoggerFactory.getLogger("mysticalterrain");

   @Override
   public void onInitialize() {
      init();
   }

   public static void init() {
      // 1. Features
      Registry.register(BuiltInRegistries.FEATURE_TYPE, id("colossal_tree"), ColossalTreeFeature.CODEC);
      Registry.register(BuiltInRegistries.FEATURE_TYPE, id("spire"), SpireFeature.CODEC);
      Registry.register(BuiltInRegistries.FEATURE_TYPE, id("boulder"), BoulderFeature.CODEC);
      Registry.register(BuiltInRegistries.FEATURE_TYPE, id("grand_tree"), GrandTreeFeature.CODEC);
      Registry.register(BuiltInRegistries.FEATURE_TYPE, id("ground_cover"), GroundCoverFeature.CODEC);
      Registry.register(BuiltInRegistries.FEATURE_TYPE, id("formation"), FormationFeature.CODEC);
      Registry.register(BuiltInRegistries.FEATURE_TYPE, id("building"), BuildingFeature.CODEC);

      // 2. Placement Modifiers
      Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, id("grand_tree_count"), GrandTreeCount.CODEC);

      // 3. Structure Types
      Registry.register(BuiltInRegistries.STRUCTURE_TYPE, id("grand_tree"), GrandTreeStructure.TYPE);
      Registry.register(BuiltInRegistries.STRUCTURE_TYPE, id("formation"), FormationStructure.TYPE);
      Registry.register(BuiltInRegistries.STRUCTURE_TYPE, id("building"), BuildingStructure.TYPE);

      // 4. Structure Pieces
      Registry.register(BuiltInRegistries.STRUCTURE_PIECE, id("grand_tree"), GrandTreePiece.TYPE);
      Registry.register(BuiltInRegistries.STRUCTURE_PIECE, id("formation"), FormationPiece.TYPE);
      Registry.register(BuiltInRegistries.STRUCTURE_PIECE, id("building"), BuildingPiece.TYPE);

      // 5. Blocks, Bosses, Gear & Biomes
      MysticalBlocks.register();
      com.vexira.mysticalterrain.boss.MysticalBosses.register();
      com.vexira.mysticalterrain.boss.net.BossNet.register();
      com.vexira.mysticalterrain.gear.GearSystem.init();
      MysticalTrees.register();

      LOGGER.info("Mystical Terrain (Community Patch Edition) features, structures, bosses, weapons, armor & blocksets successfully initialized!");
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
   }
}

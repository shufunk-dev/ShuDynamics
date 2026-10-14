package com.vexira.mysticalterrain;

import com.vexira.mysticalterrain.biome.MysticalTrees;
import com.vexira.mysticalterrain.block.MysticalBlocks;
import com.vexira.mysticalterrain.feature.*;
import net.enchantedwood.world.gen.mystical.feature.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MysticalTerrain {
   public static final String MOD_ID = "mysticalterrain";
   public static final Logger LOGGER = LoggerFactory.getLogger("mysticalterrain");

   public static void init() {
      // 1. Features
      Registry.register(Registries.FEATURE, id("colossal_tree"), new ColossalTreeFeature(ColossalTreeConfiguration.CODEC));
      Registry.register(Registries.FEATURE, id("spire"), new SpireFeature(SpireConfiguration.CODEC));
      Registry.register(Registries.FEATURE, id("boulder"), new BoulderFeature(BoulderConfiguration.CODEC));
      Registry.register(Registries.FEATURE, id("grand_tree"), new GrandTreeFeature(GrandTreeConfiguration.CODEC));
      Registry.register(Registries.FEATURE, id("ground_cover"), new GroundCoverFeature(GroundCoverConfiguration.CODEC));
      Registry.register(Registries.FEATURE, id("formation"), new FormationFeature(FormationConfiguration.CODEC));
      Registry.register(Registries.FEATURE, id("building"), new BuildingFeature(BuildingConfiguration.CODEC));

      // 2. Placement Modifiers
      Registry.register(Registries.PLACEMENT_MODIFIER_TYPE, id("grand_tree_count"), GrandTreeCount.TYPE);

      // 3. Structure Types
      Registry.register(Registries.STRUCTURE_TYPE, id("grand_tree"), GrandTreeStructure.TYPE);
      Registry.register(Registries.STRUCTURE_TYPE, id("formation"), FormationStructure.TYPE);
      Registry.register(Registries.STRUCTURE_TYPE, id("building"), BuildingStructure.TYPE);

      // 4. Structure Pieces
      Registry.register(Registries.STRUCTURE_PIECE, id("grand_tree"), GrandTreePiece.TYPE);
      Registry.register(Registries.STRUCTURE_PIECE, id("formation"), FormationPiece.TYPE);
      Registry.register(Registries.STRUCTURE_PIECE, id("building"), BuildingPiece.TYPE);

      // 5. Blocks & Biomes
      MysticalBlocks.register();
      MysticalTrees.register();

      LOGGER.info("Mystical Terrain features, structures & blocksets successfully integrated for The Convergence!");
   }

   public static Identifier id(String path) {
      return Identifier.of(MOD_ID, path);
   }
}

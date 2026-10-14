package com.vexira.mysticalterrain.feature;

import net.enchantedwood.world.gen.mystical.feature.GrandTreeConfiguration;
import net.enchantedwood.world.gen.mystical.feature.TreePlacer;
import net.enchantedwood.world.gen.mystical.tree.Growth;
import net.enchantedwood.world.gen.mystical.tree.Tree;
import net.enchantedwood.world.gen.mystical.tree.TreeBuilder;
import net.enchantedwood.world.gen.mystical.tree.TreeParams;
import net.enchantedwood.world.gen.mystical.tree.TreeStyle;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;

public class GrandTreePiece extends StructurePiece {
   public static final StructurePieceType TYPE = GrandTreePiece::new;
   private final RegistryKey<ConfiguredFeature<?, ?>> key;
   private final GrandTreeConfiguration config;
   private final BlockPos base;
   private final TreeParams params;
   private volatile Tree tree;

   public GrandTreePiece(RegistryKey<ConfiguredFeature<?, ?>> key, GrandTreeConfiguration config, BlockPos base, TreeParams params) {
      this(key, config, base, params, TreeBuilder.build(params));
   }

   private GrandTreePiece(RegistryKey<ConfiguredFeature<?, ?>> key, GrandTreeConfiguration config, BlockPos base, TreeParams params, Tree tree) {
      super(
         TYPE,
         0,
         new BlockBox(
            base.getX() + tree.minX(),
            base.getY() + tree.minY(),
            base.getZ() + tree.minZ(),
            base.getX() + tree.maxX(),
            base.getY() + tree.maxY(),
            base.getZ() + tree.maxZ()
         )
      );
      this.key = key;
      this.config = config;
      this.base = base;
      this.params = params;
      this.tree = tree;
   }

   public GrandTreePiece(StructureContext context, NbtCompound tag) {
      super(TYPE, tag);
      this.key = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(tag.getString("tree", "")));
      this.config = context.registryManager()
         .getOrThrow(RegistryKeys.CONFIGURED_FEATURE)
         .getOptional(this.key)
         .map(RegistryEntry::value)
         .map(ConfiguredFeature::config)
         .filter(GrandTreeConfiguration.class::isInstance)
         .map(GrandTreeConfiguration.class::cast)
         .orElse(null);
      this.base = new BlockPos(tag.getInt("x", 0), tag.getInt("y", 0), tag.getInt("z", 0));
      this.params = new TreeParams(
         TreeStyle.valueOf(tag.getString("style", "BROADLEAF")),
         tag.getInt("height", 20),
         tag.getDouble("radius", 2.0),
         tag.getDouble("crown", 8.0),
         tag.getDouble("lean", 0.0),
         tag.getDouble("gnarl", 0.0),
         tag.getInt("roots", -1),
         tag.getInt("branches", -1),
         tag.getDouble("droop", 0.0),
         tag.getDouble("shape", 0.5),
         tag.getDouble("density", 0.85),
         tag.getInt("veins", 0),
         tag.getInt("reach", 14),
         tag.getInt("ceiling", 100),
         tag.getLong("seed", 0L),
         this.config == null ? Growth.DEFAULT : this.config.growth()
      );
   }

   @Override
   protected void writeNbt(StructureContext context, NbtCompound tag) {
      tag.putString("tree", this.key.getValue().toString());
      tag.putInt("x", this.base.getX());
      tag.putInt("y", this.base.getY());
      tag.putInt("z", this.base.getZ());
      tag.putString("style", this.params.style().name());
      tag.putInt("height", this.params.height());
      tag.putDouble("radius", this.params.radius());
      tag.putDouble("crown", this.params.crown());
      tag.putDouble("lean", this.params.lean());
      tag.putDouble("gnarl", this.params.gnarl());
      tag.putInt("roots", this.params.roots());
      tag.putInt("branches", this.params.branches());
      tag.putDouble("droop", this.params.droop());
      tag.putDouble("shape", this.params.shape());
      tag.putDouble("density", this.params.density());
      tag.putInt("veins", this.params.veins());
      tag.putInt("reach", this.params.reach());
      tag.putInt("ceiling", this.params.ceiling());
      tag.putLong("seed", this.params.seed());
   }

   @Override
   public void generate(
      StructureWorldAccess level, StructureAccessor structures, ChunkGenerator generator, Random random, BlockBox box, ChunkPos chunk, BlockPos pivot
   ) {
      if (this.config != null) {
         if (this.tree == null) {
            this.tree = TreeBuilder.build(this.params);
         }

         TreePlacer.place(level, this.config, this.tree, this.base, this.params.seed(), box);
      }
   }
}

package com.vexira.mysticalterrain.feature;

import com.vexira.mysticalterrain.tree.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public class GrandTreePiece extends StructurePiece {
   public static final StructurePieceType TYPE = GrandTreePiece::new;
   private final ResourceKey<Feature> key;
   private final GrandTreeConfiguration config;
   private final BlockPos base;
   private final TreeParams params;
   private volatile Tree tree;

   public GrandTreePiece(ResourceKey<Feature> key, GrandTreeConfiguration config, BlockPos base, TreeParams params) {
      this(key, config, base, params, TreeBuilder.build(params));
   }

   private GrandTreePiece(ResourceKey<Feature> key, GrandTreeConfiguration config, BlockPos base, TreeParams params, Tree tree) {
      super(
         TYPE,
         0,
         new BoundingBox(
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

   public GrandTreePiece(StructurePieceSerializationContext context, CompoundTag tag) {
      super(TYPE, tag);
      this.key = ResourceKey.create(Registries.FEATURE, Identifier.parse(tag.getStringOr("tree", "")));
      this.config = context.registryAccess()
         .lookupOrThrow(Registries.FEATURE)
         .get(this.key)
         .map(Holder::value)
         .filter(GrandTreeFeature.class::isInstance)
         .map(GrandTreeFeature.class::cast)
         .map(GrandTreeFeature::config)
         .orElse(null);
      this.base = new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0));
      this.params = new TreeParams(
         TreeStyle.valueOf(tag.getStringOr("style", "BROADLEAF")),
         tag.getIntOr("height", 20),
         tag.getDoubleOr("radius", 2.0),
         tag.getDoubleOr("crown", 8.0),
         tag.getDoubleOr("lean", 0.0),
         tag.getDoubleOr("gnarl", 0.0),
         tag.getIntOr("roots", -1),
         tag.getIntOr("branches", -1),
         tag.getDoubleOr("droop", 0.0),
         tag.getDoubleOr("shape", 0.5),
         tag.getDoubleOr("density", 0.85),
         tag.getIntOr("veins", 0),
         tag.getIntOr("reach", 14),
         tag.getIntOr("ceiling", 100),
         tag.getLongOr("seed", 0L),
         this.config == null ? Growth.DEFAULT : this.config.growth()
      );
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
      tag.putString("tree", this.key.identifier().toString());
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
   public void postProcess(
      WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunk, BlockPos pivot
   ) {
      if (this.config != null) {
         if (this.tree == null) {
            this.tree = TreeBuilder.build(this.params);
         }

         TreePlacer.place(level, this.config, this.tree, this.base, this.params.seed(), box);
      }
   }
}

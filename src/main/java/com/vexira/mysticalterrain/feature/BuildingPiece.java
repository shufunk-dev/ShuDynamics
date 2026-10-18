package com.vexira.mysticalterrain.feature;

import com.vexira.mysticalterrain.building.Blueprint;
import com.vexira.mysticalterrain.building.BuildingBuilder;
import com.vexira.mysticalterrain.building.BuildingKind;
import com.vexira.mysticalterrain.building.BuildingParams;
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

public class BuildingPiece extends StructurePiece {
   public static final StructurePieceType TYPE = BuildingPiece::new;
   private final ResourceKey<Feature> key;
   private final BuildingConfiguration config;
   private final BlockPos base;
   private final BuildingParams params;
   private volatile Blueprint plan;

   public BuildingPiece(ResourceKey<Feature> key, BuildingConfiguration config, BlockPos base, BuildingParams params) {
      this(key, config, base, params, BuildingBuilder.build(params));
   }

   public BuildingPiece(ResourceKey<Feature> key, BuildingConfiguration config, BlockPos base, BuildingParams params, Blueprint plan) {
      super(
         TYPE,
         0,
         new BoundingBox(
            base.getX() + plan.minX(),
            base.getY() + Math.min(Math.max(plan.minY(), 0), plan.maxY()),
            base.getZ() + plan.minZ(),
            base.getX() + plan.maxX(),
            base.getY() + plan.maxY(),
            base.getZ() + plan.maxZ()
         )
      );
      this.key = key;
      this.config = config;
      this.base = base;
      this.params = params;
      this.plan = plan;
   }

   public BuildingPiece(StructurePieceSerializationContext context, CompoundTag tag) {
      super(TYPE, tag);
      this.key = ResourceKey.create(Registries.FEATURE, Identifier.parse(tag.getStringOr("building", "")));
      this.config = context.registryAccess()
         .lookupOrThrow(Registries.FEATURE)
         .get(this.key)
         .map(Holder::value)
         .filter(BuildingFeature.class::isInstance)
         .map(BuildingFeature.class::cast)
         .map(BuildingFeature::config)
         .orElse(null);
      this.base = new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0));

      BuildingKind kind;
      try {
         kind = BuildingKind.valueOf(tag.getStringOr("kind", "SHRINE"));
      } catch (IllegalArgumentException e) {
         kind = BuildingKind.SHRINE;
      }

      this.params = new BuildingParams(
         kind,
         tag.getIntOr("size", 4),
         tag.getIntOr("floors", 2),
         tag.getDoubleOr("decay", 0.0),
         tag.getLongOr("seed", 0L)
      );
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
      tag.putString("building", this.key.identifier().toString());
      tag.putInt("x", this.base.getX());
      tag.putInt("y", this.base.getY());
      tag.putInt("z", this.base.getZ());
      tag.putString("kind", this.params.kind().name());
      tag.putInt("size", this.params.size());
      tag.putInt("floors", this.params.floors());
      tag.putDouble("decay", this.params.decay());
      tag.putLong("seed", this.params.seed());
   }

   @Override
   public void postProcess(
      WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunk, BlockPos pivot
   ) {
      if (this.config != null) {
         if (this.plan == null) {
            this.plan = BuildingBuilder.build(this.params);
         }

         BuildingPlacer.place(level, this.config, this.plan, this.base, this.params.seed(), box);
      }
   }
}

package com.vexira.mysticalterrain.feature;

import com.vexira.mysticalterrain.formation.Formation;
import com.vexira.mysticalterrain.formation.FormationBuilder;
import com.vexira.mysticalterrain.formation.FormationKind;
import com.vexira.mysticalterrain.formation.FormationParams;
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

public class FormationPiece extends StructurePiece {
   public static final StructurePieceType TYPE = FormationPiece::new;
   private final ResourceKey<Feature> key;
   private final FormationConfiguration config;
   private final BlockPos base;
   private final FormationParams params;
   private volatile Formation formation;

   public FormationPiece(ResourceKey<Feature> key, FormationConfiguration config, BlockPos base, FormationParams params) {
      this(key, config, base, params, FormationBuilder.build(params));
   }

   private FormationPiece(ResourceKey<Feature> key, FormationConfiguration config, BlockPos base, FormationParams params, Formation formation) {
      super(
         TYPE,
         0,
         new BoundingBox(
            base.getX() + formation.minX(),
            base.getY() + formation.minY(),
            base.getZ() + formation.minZ(),
            base.getX() + formation.maxX(),
            base.getY() + formation.maxY(),
            base.getZ() + formation.maxZ()
         )
      );
      this.key = key;
      this.config = config;
      this.base = base;
      this.params = params;
      this.formation = formation;
   }

   public FormationPiece(StructurePieceSerializationContext context, CompoundTag tag) {
      super(TYPE, tag);
      this.key = ResourceKey.create(Registries.FEATURE, Identifier.parse(tag.getStringOr("formation", "")));
      this.config = context.registryAccess()
         .lookupOrThrow(Registries.FEATURE)
         .get(this.key)
         .map(Holder::value)
         .filter(FormationFeature.class::isInstance)
         .map(FormationFeature.class::cast)
         .map(FormationFeature::config)
         .orElse(null);
      this.base = new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0));

      FormationKind kind;
      try {
         kind = FormationKind.valueOf(tag.getStringOr("kind", "SPIRES"));
      } catch (IllegalArgumentException e) {
         kind = FormationKind.SPIRES;
      }

      this.params = new FormationParams(
         kind,
         tag.getIntOr("size", 8),
         tag.getIntOr("count", -1),
         tag.getDoubleOr("lean", 0.5),
         tag.getDoubleOr("girth", 1.0),
         tag.getIntOr("sides", 6),
         tag.getLongOr("seed", 0L)
      );
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
      tag.putString("formation", this.key.identifier().toString());
      tag.putInt("x", this.base.getX());
      tag.putInt("y", this.base.getY());
      tag.putInt("z", this.base.getZ());
      tag.putString("kind", this.params.kind().name());
      tag.putInt("size", this.params.size());
      tag.putInt("count", this.params.count());
      tag.putDouble("lean", this.params.lean());
      tag.putDouble("girth", this.params.girth());
      tag.putInt("sides", this.params.sides());
      tag.putLong("seed", this.params.seed());
   }

   @Override
   public void postProcess(
      WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunk, BlockPos pivot
   ) {
      if (this.config != null) {
         if (this.formation == null) {
            this.formation = FormationBuilder.build(this.params);
         }

         FormationPlacer.place(level, this.config, this.formation, this.base, this.params.seed(), box);
      }
   }
}

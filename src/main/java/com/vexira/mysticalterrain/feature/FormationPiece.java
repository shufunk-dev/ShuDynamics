package com.vexira.mysticalterrain.feature;

import com.vexira.mysticalterrain.formation.Formation;
import com.vexira.mysticalterrain.formation.FormationBuilder;
import com.vexira.mysticalterrain.formation.FormationKind;
import com.vexira.mysticalterrain.formation.FormationParams;
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

public class FormationPiece extends StructurePiece {
   public static final StructurePieceType TYPE = FormationPiece::new;
   private final RegistryKey<ConfiguredFeature<?, ?>> key;
   private final FormationConfiguration config;
   private final BlockPos base;
   private final FormationParams params;
   private volatile Formation formation;

   public FormationPiece(RegistryKey<ConfiguredFeature<?, ?>> key, FormationConfiguration config, BlockPos base, FormationParams params) {
      this(key, config, base, params, FormationBuilder.build(params));
   }

   private FormationPiece(RegistryKey<ConfiguredFeature<?, ?>> key, FormationConfiguration config, BlockPos base, FormationParams params, Formation formation) {
      super(
         TYPE,
         0,
         new BlockBox(
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

   public FormationPiece(StructureContext context, NbtCompound tag) {
      super(TYPE, tag);
      this.key = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(tag.getString("formation", "")));
      this.config = context.registryManager()
         .getOrThrow(RegistryKeys.CONFIGURED_FEATURE)
         .getOptional(this.key)
         .map(RegistryEntry::value)
         .map(ConfiguredFeature::config)
         .filter(FormationConfiguration.class::isInstance)
         .map(FormationConfiguration.class::cast)
         .orElse(null);
      this.base = new BlockPos(tag.getInt("x", 0), tag.getInt("y", 0), tag.getInt("z", 0));

      FormationKind kind;
      try {
         kind = FormationKind.valueOf(tag.getString("kind", "SPIRES"));
      } catch (IllegalArgumentException e) {
         kind = FormationKind.SPIRES;
      }

      this.params = new FormationParams(
         kind,
         tag.getInt("size", 8),
         tag.getInt("count", -1),
         tag.getDouble("lean", 0.5),
         tag.getDouble("girth", 1.0),
         tag.getInt("sides", 6),
         tag.getLong("seed", 0L)
      );
   }

   @Override
   protected void writeNbt(StructureContext context, NbtCompound tag) {
      tag.putString("formation", this.key.getValue().toString());
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
   public void generate(
      StructureWorldAccess level, StructureAccessor structures, ChunkGenerator generator, Random random, BlockBox box, ChunkPos chunk, BlockPos pivot
   ) {
      if (this.config != null) {
         if (this.formation == null) {
            this.formation = FormationBuilder.build(this.params);
         }

         FormationPlacer.place(level, this.config, this.formation, this.base, this.params.seed(), box);
      }
   }
}

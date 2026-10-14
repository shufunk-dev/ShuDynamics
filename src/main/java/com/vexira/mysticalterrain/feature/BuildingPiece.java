package com.vexira.mysticalterrain.feature;

import com.vexira.mysticalterrain.building.Blueprint;
import com.vexira.mysticalterrain.building.BuildingBuilder;
import com.vexira.mysticalterrain.building.BuildingKind;
import com.vexira.mysticalterrain.building.BuildingParams;
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

public class BuildingPiece extends StructurePiece {
   public static final StructurePieceType TYPE = BuildingPiece::new;
   private final RegistryKey<ConfiguredFeature<?, ?>> key;
   private final BuildingConfiguration config;
   private final BlockPos base;
   private final BuildingParams params;
   private volatile Blueprint plan;

   public BuildingPiece(RegistryKey<ConfiguredFeature<?, ?>> key, BuildingConfiguration config, BlockPos base, BuildingParams params) {
      this(key, config, base, params, BuildingBuilder.build(params));
   }

   private BuildingPiece(RegistryKey<ConfiguredFeature<?, ?>> key, BuildingConfiguration config, BlockPos base, BuildingParams params, Blueprint plan) {
      super(
         TYPE,
         0,
         new BlockBox(
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

   public BuildingPiece(StructureContext context, NbtCompound tag) {
      super(TYPE, tag);
      this.key = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(tag.getString("building", "")));
      this.config = context.registryManager()
         .getOrThrow(RegistryKeys.CONFIGURED_FEATURE)
         .getOptional(this.key)
         .map(RegistryEntry::value)
         .map(ConfiguredFeature::config)
         .filter(BuildingConfiguration.class::isInstance)
         .map(BuildingConfiguration.class::cast)
         .orElse(null);
      this.base = new BlockPos(tag.getInt("x", 0), tag.getInt("y", 0), tag.getInt("z", 0));

      BuildingKind kind;
      try {
         kind = BuildingKind.valueOf(tag.getString("kind", "SHRINE"));
      } catch (IllegalArgumentException e) {
         kind = BuildingKind.SHRINE;
      }

      this.params = new BuildingParams(
         kind,
         tag.getInt("size", 4),
         tag.getInt("floors", 2),
         tag.getDouble("decay", 0.0),
         tag.getLong("seed", 0L)
      );
   }

   @Override
   protected void writeNbt(StructureContext context, NbtCompound tag) {
      tag.putString("building", this.key.getValue().toString());
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
   public void generate(
      StructureWorldAccess level, StructureAccessor structures, ChunkGenerator generator, Random random, BlockBox box, ChunkPos chunk, BlockPos pivot
   ) {
      if (this.config != null) {
         if (this.plan == null) {
            this.plan = BuildingBuilder.build(this.params);
         }

         BuildingPlacer.place(level, this.config, this.plan, this.base, this.params.seed(), box);
      }
   }
}

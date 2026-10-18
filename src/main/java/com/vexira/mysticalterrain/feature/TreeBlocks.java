package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record TreeBlocks(
    BlockStateProvider trunk,
    Optional<BlockStateProvider> branch,
    Optional<BlockStateProvider> root,
    Optional<BlockStateProvider> leaves,
    Optional<BlockStateProvider> vein,
    Optional<BlockStateProvider> fruit,
    float fruitChance,
    Optional<BlockStateProvider> hanging,
    float hangingChance,
    IntProvider hangingLength,
    Optional<BlockStateProvider> cover,
    float coverChance
) {
    public static final Codec<TreeBlocks> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            BlockStateProvider.DIRECT_CODEC.fieldOf("trunk").forGetter(TreeBlocks::trunk),
            BlockStateProvider.DIRECT_CODEC.optionalFieldOf("branch").forGetter(TreeBlocks::branch),
            BlockStateProvider.DIRECT_CODEC.optionalFieldOf("root").forGetter(TreeBlocks::root),
            BlockStateProvider.DIRECT_CODEC.optionalFieldOf("leaves").forGetter(TreeBlocks::leaves),
            BlockStateProvider.DIRECT_CODEC.optionalFieldOf("vein").forGetter(TreeBlocks::vein),
            BlockStateProvider.DIRECT_CODEC.optionalFieldOf("fruit").forGetter(TreeBlocks::fruit),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("fruit_chance", 0.03F).forGetter(TreeBlocks::fruitChance),
            BlockStateProvider.DIRECT_CODEC.optionalFieldOf("hanging").forGetter(TreeBlocks::hanging),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("hanging_chance", 0.08F).forGetter(TreeBlocks::hangingChance),
            IntProviders.codec(1, 32).optionalFieldOf("hanging_length", UniformInt.of(1, 4)).forGetter(TreeBlocks::hangingLength),
            BlockStateProvider.DIRECT_CODEC.optionalFieldOf("cover").forGetter(TreeBlocks::cover),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("cover_chance", 0.25F).forGetter(TreeBlocks::coverChance)
        ).apply(instance, TreeBlocks::new)
    );

    public BlockStateProvider branchOrTrunk() {
        return this.branch.orElse(this.trunk);
    }

    public BlockStateProvider rootOrTrunk() {
        return this.root.orElse(this.trunk);
    }
}

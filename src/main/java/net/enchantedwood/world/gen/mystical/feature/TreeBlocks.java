package net.enchantedwood.world.gen.mystical.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.intprovider.UniformIntProvider;

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
            BlockStateProvider.TYPE_CODEC.fieldOf("trunk").forGetter(TreeBlocks::trunk),
            BlockStateProvider.TYPE_CODEC.optionalFieldOf("branch").forGetter(TreeBlocks::branch),
            BlockStateProvider.TYPE_CODEC.optionalFieldOf("root").forGetter(TreeBlocks::root),
            BlockStateProvider.TYPE_CODEC.optionalFieldOf("leaves").forGetter(TreeBlocks::leaves),
            BlockStateProvider.TYPE_CODEC.optionalFieldOf("vein").forGetter(TreeBlocks::vein),
            BlockStateProvider.TYPE_CODEC.optionalFieldOf("fruit").forGetter(TreeBlocks::fruit),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("fruit_chance", 0.03F).forGetter(TreeBlocks::fruitChance),
            BlockStateProvider.TYPE_CODEC.optionalFieldOf("hanging").forGetter(TreeBlocks::hanging),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("hanging_chance", 0.08F).forGetter(TreeBlocks::hangingChance),
            IntProvider.createValidatingCodec(1, 32).optionalFieldOf("hanging_length", UniformIntProvider.create(1, 4)).forGetter(TreeBlocks::hangingLength),
            BlockStateProvider.TYPE_CODEC.optionalFieldOf("cover").forGetter(TreeBlocks::cover),
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

package net.enchantedwood.world;

import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.grower.TreeGrower;

public class ModSaplingGenerators {
    public static final TreeGrower RUBBER = new TreeGrower(
            "rubber",
            WeightedList.of(ModWorldGeneration.RUBBER_TREE_KEY),
            WeightedList.of(),
            WeightedList.of(),
            ModWorldGeneration.RUBBER_TREE_KEY
    );

    public static final TreeGrower AVOCADO = new TreeGrower(
            "avocado",
            WeightedList.of(ModWorldGeneration.AVOCADO_TREE_KEY),
            WeightedList.of(),
            WeightedList.of(),
            ModWorldGeneration.AVOCADO_TREE_KEY
    );

    public static final TreeGrower STARFRUIT = new TreeGrower(
            "starfruit",
            WeightedList.of(ModWorldGeneration.STARFRUIT_TREE_KEY),
            WeightedList.of(),
            WeightedList.of(),
            ModWorldGeneration.STARFRUIT_TREE_KEY
    );
}

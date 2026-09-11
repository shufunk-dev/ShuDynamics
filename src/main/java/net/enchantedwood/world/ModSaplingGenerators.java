package net.enchantedwood.world;

import net.minecraft.block.SaplingGenerator;
import java.util.Optional;

public class ModSaplingGenerators {
    public static final SaplingGenerator RUBBER = new SaplingGenerator(
            "rubber",
            Optional.empty(),
            Optional.of(ModWorldGeneration.RUBBER_TREE_KEY),
            Optional.empty()
    );

    public static final SaplingGenerator AVOCADO = new SaplingGenerator(
            "avocado",
            Optional.empty(),
            Optional.of(ModWorldGeneration.AVOCADO_TREE_KEY),
            Optional.empty()
    );

    public static final SaplingGenerator STARFRUIT = new SaplingGenerator(
            "starfruit",
            Optional.empty(),
            Optional.of(ModWorldGeneration.STARFRUIT_TREE_KEY),
            Optional.empty()
    );
}

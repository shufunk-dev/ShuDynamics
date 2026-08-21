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
}

package net.enchantedwood.world.gen.mystical.tree;

public record TreeParams(
    TreeStyle style,
    int height,
    double radius,
    double crown,
    double lean,
    double gnarl,
    int roots,
    int branches,
    double droop,
    double shape,
    double density,
    int veins,
    int reach,
    int ceiling,
    long seed,
    Growth growth
) {
}

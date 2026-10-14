package net.enchantedwood.world.gen.mystical.tree;

@FunctionalInterface
public interface Sink {
    void place(int x, int y, int z, Part part, int axis);
}

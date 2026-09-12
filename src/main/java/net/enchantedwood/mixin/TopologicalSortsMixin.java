package net.enchantedwood.mixin;

import net.minecraft.util.TopologicalSorts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(TopologicalSorts.class)
public class TopologicalSortsMixin {
    /**
     * Prevents fatal worldgen freezes/crashes caused by feature dependency cycles.
     * In dimensions combining diverse biomes (such as The Convergence combining Overworld cave
     * biomes, Nether biomes, and custom biomes), Mojang's PlacedFeatureIndexer runs a strict
     * topological sort that throws an IllegalStateException if any cycle is detected.
     * When visiting already contains 'now', returning false breaks the cycle cleanly without
     * omitting any features or crashing the chunk generator thread.
     */
    @Inject(method = "sort", at = @At("HEAD"), cancellable = true)
    private static <T> void enchantedwood$breakWorldgenFeatureCycle(
            Map<T, Set<T>> successors,
            Set<T> visited,
            Set<T> visiting,
            Consumer<T> reversedOrderConsumer,
            T now,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (visiting.contains(now)) {
            cir.setReturnValue(false);
        }
    }
}

package net.enchantedwood.mixin;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.JigsawStructure;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(JigsawStructure.class)
public class JigsawStructureMixin {

    @Shadow @Final private RegistryEntry<StructurePool> startPool;

    @ModifyVariable(
            method = "getStructurePosition",
            at = @At("STORE"),
            ordinal = 0
    )
    private int enchantedwood$adjustBastionHeightForConvergence(int y, Structure.Context context) {
        // Detect open-surface dimension like The Convergence (min_y = -64).
        // In the Nether (min_y = 0), vanilla start_height: 33 remains completely untouched.
        if (context.chunkGenerator().getMinimumY() == -64 && this.startPool != null && this.startPool.getKey().isPresent()) {
            Identifier poolId = this.startPool.getKey().get().getValue();
            if (poolId.getPath().startsWith("bastion/")) {
                ChunkPos chunkPos = context.chunkPos();
                int surfaceY = context.chunkGenerator().getHeightOnGround(
                        chunkPos.getCenterX(),
                        chunkPos.getCenterZ(),
                        Heightmap.Type.WORLD_SURFACE_WG,
                        context.world(),
                        context.noiseConfig()
                );
                // Embed the bottom rampart 4 blocks into the surface terrain for a solid foundation
                return Math.max(surfaceY - 4, 64);
            }
        }
        return y;
    }
}

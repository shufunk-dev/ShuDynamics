package net.enchantedwood.mixin;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(JigsawStructure.class)
public class JigsawStructureMixin {

    @Shadow @Final private Holder<StructureTemplatePool> startPool;

    @ModifyVariable(
            method = "getStructurePosition",
            at = @At("STORE"),
            ordinal = 0
    )
    private int enchantedwood$adjustBastionHeightForConvergence(int y, Structure.GenerationContext context) {
        // Detect open-surface dimension like The Convergence (min_y = -64).
        // In the Nether (min_y = 0), vanilla start_height: 33 remains completely untouched.
        if (context.chunkGenerator().getMinY() == -64 && this.startPool != null && this.startPool.unwrapKey().isPresent()) {
            Identifier poolId = this.startPool.unwrapKey().get().identifier();
            if (poolId.getPath().startsWith("bastion/")) {
                ChunkPos chunkPos = context.chunkPos();
                int surfaceY = context.chunkGenerator().getFirstFreeHeight(
                        chunkPos.getMiddleBlockX(),
                        chunkPos.getMiddleBlockZ(),
                        Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(),
                        context.randomState()
                );
                // Embed the bottom rampart 4 blocks into the surface terrain for a solid foundation
                return Math.max(surfaceY - 4, 64);
            }
        }
        return y;
    }
}

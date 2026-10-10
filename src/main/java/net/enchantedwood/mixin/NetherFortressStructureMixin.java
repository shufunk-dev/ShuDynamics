package net.enchantedwood.mixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetherFortressStructure.class)
public class NetherFortressStructureMixin {

    @Inject(method = "generatePieces", at = @At("TAIL"))
    private static void enchantedwood$adjustFortressHeightForConvergence(
            StructurePiecesBuilder collector,
            Structure.GenerationContext context,
            CallbackInfo ci
    ) {
        // Detect open-surface dimension like The Convergence (min_y = -64).
        // In the Nether (min_y = 0), vanilla shiftInto(48, 70) remains completely untouched.
        if (context.chunkGenerator().getMinY() == -64) {
            ChunkPos chunkPos = context.chunkPos();
            int surfaceY = context.chunkGenerator().getFirstFreeHeight(
                    chunkPos.getMiddleBlockX(),
                    chunkPos.getMiddleBlockZ(),
                    Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(),
                    context.randomState()
            );

            BoundingBox box = collector.getBoundingBox();
            int currentMinY = box.minY();
            // Nether Fortress bridges automatically generate pillar foundations (fillDownwards)
            // anchoring deep into the terrain. Anchoring the bottom walkway level right at the
            // surface ensures the fortress stands proudly across the landscape under the sky.
            int targetY = Math.max(surfaceY - 2, 64);
            collector.offsetPiecesVertically(targetY - currentMinY);
        }
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(
            method = "getStructurePosition",
            at = @At("STORE"),
            ordinal = 0
    )
    private net.minecraft.core.BlockPos enchantedwood$adjustLocatePosForConvergence(
            net.minecraft.core.BlockPos pos,
            Structure.GenerationContext context
    ) {
        // Ensure /locate structure minecraft:fortress reports the surface height instead of vanilla Y=64
        if (context.chunkGenerator().getMinY() == -64) {
            ChunkPos chunkPos = context.chunkPos();
            int surfaceY = context.chunkGenerator().getFirstFreeHeight(
                    chunkPos.getMiddleBlockX(),
                    chunkPos.getMiddleBlockZ(),
                    Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(),
                    context.randomState()
            );
            return new net.minecraft.core.BlockPos(pos.getX(), Math.max(surfaceY - 2, 64), pos.getZ());
        }
        return pos;
    }
}

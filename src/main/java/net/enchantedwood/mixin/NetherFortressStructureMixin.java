package net.enchantedwood.mixin;

import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.NetherFortressStructure;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetherFortressStructure.class)
public class NetherFortressStructureMixin {

    @Inject(method = "addPieces", at = @At("TAIL"))
    private static void enchantedwood$adjustFortressHeightForConvergence(
            StructurePiecesCollector collector,
            Structure.Context context,
            CallbackInfo ci
    ) {
        // Detect open-surface dimension like The Convergence (min_y = -64).
        // In the Nether (min_y = 0), vanilla shiftInto(48, 70) remains completely untouched.
        if (context.chunkGenerator().getMinimumY() == -64) {
            ChunkPos chunkPos = context.chunkPos();
            int surfaceY = context.chunkGenerator().getHeightOnGround(
                    chunkPos.getCenterX(),
                    chunkPos.getCenterZ(),
                    Heightmap.Type.WORLD_SURFACE_WG,
                    context.world(),
                    context.noiseConfig()
            );

            BlockBox box = collector.getBoundingBox();
            int currentMinY = box.getMinY();
            // Nether Fortress bridges automatically generate pillar foundations (fillDownwards)
            // anchoring deep into the terrain. Anchoring the bottom walkway level right at the
            // surface ensures the fortress stands proudly across the landscape under the sky.
            int targetY = Math.max(surfaceY - 2, 64);
            collector.shift(targetY - currentMinY);
        }
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(
            method = "getStructurePosition",
            at = @At("STORE"),
            ordinal = 0
    )
    private net.minecraft.util.math.BlockPos enchantedwood$adjustLocatePosForConvergence(
            net.minecraft.util.math.BlockPos pos,
            Structure.Context context
    ) {
        // Ensure /locate structure minecraft:fortress reports the surface height instead of vanilla Y=64
        if (context.chunkGenerator().getMinimumY() == -64) {
            ChunkPos chunkPos = context.chunkPos();
            int surfaceY = context.chunkGenerator().getHeightOnGround(
                    chunkPos.getCenterX(),
                    chunkPos.getCenterZ(),
                    Heightmap.Type.WORLD_SURFACE_WG,
                    context.world(),
                    context.noiseConfig()
            );
            return new net.minecraft.util.math.BlockPos(pos.getX(), Math.max(surfaceY - 2, 64), pos.getZ());
        }
        return pos;
    }
}

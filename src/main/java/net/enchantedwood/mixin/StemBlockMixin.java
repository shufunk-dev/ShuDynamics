package net.enchantedwood.mixin;

import net.enchantedwood.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.StemBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StemBlock.class)
public class StemBlockMixin {
    @Inject(method = "canPlantOnTop", at = @At("HEAD"), cancellable = true)
    private void allowVolcanicSoilStem(BlockState floor, BlockView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (floor.isOf(ModBlocks.VOLCANIC_SOIL)) {
            cir.setReturnValue(true);
        }
    }
}

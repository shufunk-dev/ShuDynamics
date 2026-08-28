package net.enchantedwood.mixin;

import net.enchantedwood.block.ModBlocks;
import net.minecraft.block.AttachedStemBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AttachedStemBlock.class)
public class AttachedStemBlockMixin {
    @Inject(method = "canPlantOnTop", at = @At("HEAD"), cancellable = true)
    private void allowVolcanicSoilAttached(BlockState floor, BlockView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (floor.isOf(ModBlocks.VOLCANIC_SOIL)) {
            cir.setReturnValue(true);
        }
    }
}

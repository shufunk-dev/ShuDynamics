package net.enchantedwood.client.renderer;

import net.enchantedwood.fluid.MoltenMetal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class TitaniumTankRenderState extends BlockEntityRenderState {
    public boolean isFormed = false;
    public int lavaAmount = 0;
    public MoltenMetal currentFluid = MoltenMetal.NONE;
    public BlockPos minPos = null;
}

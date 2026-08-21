package net.enchantedwood.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.util.math.Direction;
import net.enchantedwood.block.custom.GearTier;

@Environment(EnvType.CLIENT)
public class EnchantedChestRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public GearTier gearTier = GearTier.NONE;
    public float lidProgress = 0.0f;
}

package net.enchantedwood.block.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AsphaltSlabBlock extends SlabBlock {
    public AsphaltSlabBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient()) {
            if (entity instanceof LivingEntity living) {
                // Give a subtle continuous speed boost when running on asphalt roads
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20, 0, false, false, true));
            }
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient() && player != null && !player.isCreative()) {
            ItemStack tool = player.getMainHandStack();
            if (AsphaltBlock.isIronOrBetterPickaxe(tool)) {
                int slabCount = state.get(TYPE) == SlabType.DOUBLE ? 2 : 1;
                if (AsphaltBlock.hasSilkTouch(tool) || world.random.nextBoolean()) {
                    // 50% chance: Drop Asphalt Slab intact
                    dropStack(world, pos, new ItemStack(this, slabCount));
                } else {
                    // 50% chance: Reverts into Mineral Tar
                    dropStack(world, pos, new ItemStack(ModItems.MINERAL_TAR, slabCount));
                }
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}

package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class DormantRiftBlock extends Block {
    public static final MapCodec<DormantRiftBlock> CODEC = createCodec(DormantRiftBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;

    protected static final VoxelShape X_SHAPE = Block.createCuboidShape(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
    protected static final VoxelShape Z_SHAPE = Block.createCuboidShape(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);

    public DormantRiftBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(AXIS) == Direction.Axis.Z ? Z_SHAPE : X_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.BLOCKS, 1.0f, 1.6f);
        if (!world.isClient()) {
            player.sendMessage(
                    Text.literal("§c⚠️ Resonance Locked: §7The 6 Keystones are fully stabilized, but the gateway requires §ev2.0 dimensional alignment §7to breach the barrier."),
                    false
            );
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, EntityCollisionHandler handler, boolean isInside) {
        if (entity instanceof PlayerEntity player) {
            // Apply zero-gravity float while standing inside the rift
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 40, 0, false, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 20, 0, false, false, false));

            if (player.age % 40 == 0) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.AMBIENT, 0.5f, 1.8f);
                player.sendMessage(
                        Text.literal("§5✦ §dThe rift vibrates with unknown spatial coordinates... You hear faint winds from an alien world on the other side, but the threshold is not yet open."),
                        true
                );
            }
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, WorldView world, net.minecraft.world.tick.ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        // If the frame around this rift breaks, collapse the rift
        BlockPos below = pos.down();
        BlockPos above = pos.up();
        boolean hasSupport = world.getBlockState(below).isOf(this) || !world.isAir(below);
        boolean hasRoof = world.getBlockState(above).isOf(this) || !world.isAir(above);
        if (!hasSupport || !hasRoof) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(80) == 0) {
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 0.5f, 1.9f);
        }

        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + random.nextDouble();
        double z = pos.getZ() + random.nextDouble();
        world.addParticleClient(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0.05, 0);
        if (random.nextBoolean()) {
            world.addParticleClient(ParticleTypes.END_ROD, x, y, z, 0, 0.02, 0);
        }
    }
}

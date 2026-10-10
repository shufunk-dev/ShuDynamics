package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.LaserQuarryBlockEntity;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class LaserQuarryBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public LaserQuarryBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaserQuarryBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.LASER_QUARRY_BLOCK_ENTITY) {
            if (world instanceof ServerLevel serverWorld) {
                return (w, pos, st, blockEntity) -> LaserQuarryBlockEntity.tick(serverWorld, pos, st, (LaserQuarryBlockEntity) blockEntity);
            } else {
                return (w, pos, st, blockEntity) -> LaserQuarryBlockEntity.clientTick(w, pos, st, (LaserQuarryBlockEntity) blockEntity);
            }
        }
        return null;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, net.minecraft.util.RandomSource random) {
        double d = pos.getX() + 0.5;
        double e = pos.getY() + 0.5;
        double f = pos.getZ() + 0.5;

        if (state.getValue(LIT)) {
            // Concentrated scanning core particles on the machine
            world.addParticle(ParticleTypes.ELECTRIC_SPARK, d, e + 0.5, f, 0.0, 0.1, 0.0);
            world.addParticle(ParticleTypes.PORTAL, d + (random.nextDouble() - 0.5) * 0.4, e + 0.8, f + (random.nextDouble() - 0.5) * 0.4, 0.0, -0.2, 0.0);
        } else {
            // Standby indicator when paused/idle
            world.addParticle(ParticleTypes.ELECTRIC_SPARK, d, e + 0.8, f, 0.0, 0.02, 0.0);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.getMainHandItem().getItem() instanceof net.enchantedwood.item.custom.WrenchItem ||
            player.getOffhandItem().getItem() instanceof net.enchantedwood.item.custom.WrenchItem ||
            ((player.getMainHandItem().getItem() instanceof net.enchantedwood.item.custom.WirelessStorageCrystalItem ||
              player.getOffhandItem().getItem() instanceof net.enchantedwood.item.custom.WirelessStorageCrystalItem) && player.isShiftKeyDown())) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof MenuProvider factory) {
                player.openMenu(factory);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide()) {
            net.minecraft.world.item.component.CustomData nbtComponent = itemStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (nbtComponent != null) {
                net.minecraft.nbt.CompoundTag nbt = nbtComponent.copyTag();
                if (nbt.contains("boundX")) {
                    int bx = nbt.getInt("boundX").orElse(0);
                    int by = nbt.getInt("boundY").orElse(0);
                    int bz = nbt.getInt("boundZ").orElse(0);
                    String bDim = nbt.getString("boundDimension").orElse("minecraft:overworld");
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof LaserQuarryBlockEntity quarry) {
                        quarry.bindNetwork(new BlockPos(bx, by, bz), bDim);
                        if (placer instanceof Player player) {
                            String dimName = bDim.contains("mining_dimension") ? "Mining Dimension" :
                                    bDim.contains("nether") ? "Nether" :
                                    bDim.contains("end") ? "The End" : "Overworld";
                            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("§6[Quarry] §a✨ Auto-connected to Base Network at (" + bx + ", " + by + ", " + bz + ") in " + dimName + "!"));
                        }
                    }
                }
            }
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof LaserQuarryBlockEntity quarry) {
                quarry.resetScanCoordinates(state);
                if (world instanceof ServerLevel serverWorld) {
                    quarry.updateChunkLoading(serverWorld);
                }
            }
            if (placer instanceof Player player) {
                net.minecraft.world.level.ChunkPos chunk = net.minecraft.world.level.ChunkPos.containing(pos);
                int relX = pos.getX() - (chunk.x() * 16);
                int relZ = pos.getZ() - (chunk.z() * 16);
                Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6[Quarry] §e⚡ Laser Boundary Active! §7(Facing " + facing.getSerializedName().toUpperCase() + " • §bCorner Layout§7)"));
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof LaserQuarryBlockEntity quarryEntity) {
            quarryEntity.releaseChunkTickets(world);
            Containers.dropContents(world, pos, quarryEntity);
        }
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }
}

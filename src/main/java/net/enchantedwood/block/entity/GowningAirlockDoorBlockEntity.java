package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.List;

public class GowningAirlockDoorBlockEntity extends BlockEntity {
    private int autoCloseTimer = 0;
    private int scanCooldown = 0;

    public GowningAirlockDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GOWNING_AIRLOCK_DOOR_BE, pos, state);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, GowningAirlockDoorBlockEntity entity) {
        boolean isOpen = state.get(DoorBlock.OPEN);

        if (!isOpen) {
            entity.scanCooldown++;
            if (entity.scanCooldown >= 10) {
                entity.scanCooldown = 0;
                entity.scanExterior(world, pos, state);
            }
        } else {
            // Door is open - handle auto-close timer
            if (entity.autoCloseTimer > 0) {
                entity.autoCloseTimer--;
            } else {
                // Check if any entity is currently in the doorway
                Box doorway = new Box(pos).stretch(0, 1.0, 0);
                List<PlayerEntity> insideDoorway = world.getEntitiesByClass(PlayerEntity.class, doorway, p -> true);

                if (insideDoorway.isEmpty()) {
                    entity.setDoorOpenState(world, pos, state, false);
                    world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 1.0f, 1.1f);
                    entity.scanCooldown = -20; // 1-second cooldown after closing before scanning again
                } else {
                    entity.autoCloseTimer = 20;
                }
            }
        }
    }

    private void scanExterior(ServerWorld world, BlockPos pos, BlockState state) {
        Direction facing = state.get(DoorBlock.FACING);
        BlockPos inFront = pos.offset(facing);

        // Exact 1-block doorway column in front of the door with zero bleed behind the door or into side walls
        double minX = inFront.getX();
        double maxX = inFront.getX() + 1.0;
        double minY = pos.getY();
        double maxY = pos.getY() + 2.0;
        double minZ = inFront.getZ();
        double maxZ = inFront.getZ() + 1.0;

        if (facing == Direction.NORTH) {
            minZ -= 0.25;
        } else if (facing == Direction.SOUTH) {
            maxZ += 0.25;
        } else if (facing == Direction.WEST) {
            minX -= 0.25;
        } else if (facing == Direction.EAST) {
            maxX += 0.25;
        }

        Box scanBox = new Box(minX, minY, minZ, maxX, maxY, maxZ);

        List<ServerPlayerEntity> players = world.getEntitiesByClass(ServerPlayerEntity.class, scanBox, p -> !p.isSpectator());

        if (!players.isEmpty()) {
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
            setDoorOpenState(world, pos, state, true);
            this.autoCloseTimer = 70; // 3.5 seconds
            players.getFirst().sendMessage(Text.literal("§a✦ Gowning Room Airlock: Welcome Personnel ✦"), true);
        }
    }

    public void openForExit() {
        if (world != null && !world.isClient()) {
            BlockState state = getCachedState();
            setDoorOpenState((ServerWorld) world, pos, state, true);
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
            this.autoCloseTimer = 70;
        }
    }

    private void setDoorOpenState(ServerWorld world, BlockPos pos, BlockState state, boolean open) {
        world.setBlockState(pos, state.with(DoorBlock.OPEN, open), 3);
        BlockPos upperPos = pos.up();
        BlockState upperState = world.getBlockState(upperPos);
        if (upperState.isOf(state.getBlock())) {
            world.setBlockState(upperPos, upperState.with(DoorBlock.OPEN, open), 3);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.autoCloseTimer = view.getInt("AutoCloseTimer", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("AutoCloseTimer", this.autoCloseTimer);
    }
}

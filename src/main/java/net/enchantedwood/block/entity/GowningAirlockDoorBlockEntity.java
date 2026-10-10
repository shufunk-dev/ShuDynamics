package net.enchantedwood.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class GowningAirlockDoorBlockEntity extends BlockEntity {
    private int autoCloseTimer = 0;
    private int scanCooldown = 0;

    public GowningAirlockDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GOWNING_AIRLOCK_DOOR_BE, pos, state);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, GowningAirlockDoorBlockEntity entity) {
        boolean isOpen = state.getValue(DoorBlock.OPEN);

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
                AABB doorway = new AABB(pos).expandTowards(0, 1.0, 0);
                List<Player> insideDoorway = world.getEntitiesOfClass(Player.class, doorway, p -> true);

                if (insideDoorway.isEmpty()) {
                    entity.setDoorOpenState(world, pos, state, false);
                    world.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 1.1f);
                    entity.scanCooldown = -20; // 1-second cooldown after closing before scanning again
                } else {
                    entity.autoCloseTimer = 20;
                }
            }
        }
    }

    private void scanExterior(ServerLevel world, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(DoorBlock.FACING);
        BlockPos inFront = pos.relative(facing);

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

        AABB scanBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        List<ServerPlayer> players = world.getEntitiesOfClass(ServerPlayer.class, scanBox, p -> !p.isSpectator());

        if (!players.isEmpty()) {
            world.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
            setDoorOpenState(world, pos, state, true);
            this.autoCloseTimer = 70; // 3.5 seconds
            players.getFirst().sendOverlayMessage(Component.literal("§a✦ Gowning Room Airlock: Welcome Personnel ✦"));
        }
    }

    public void openForExit() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            setDoorOpenState((ServerLevel) level, worldPosition, state, true);
            level.playSound(null, worldPosition, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
            this.autoCloseTimer = 70;
        }
    }

    private void setDoorOpenState(ServerLevel world, BlockPos pos, BlockState state, boolean open) {
        world.setBlock(pos, state.setValue(DoorBlock.OPEN, open), 3);
        BlockPos upperPos = pos.above();
        BlockState upperState = world.getBlockState(upperPos);
        if (upperState.is(state.getBlock())) {
            world.setBlock(upperPos, upperState.setValue(DoorBlock.OPEN, open), 3);
        }
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.autoCloseTimer = view.getIntOr("AutoCloseTimer", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putInt("AutoCloseTimer", this.autoCloseTimer);
    }
}

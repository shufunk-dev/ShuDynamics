package net.enchantedwood.block.entity;

import net.enchantedwood.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import java.util.List;

public class DecontaminationAirlockDoorBlockEntity extends BlockEntity {
    private int autoCloseTimer = 0;
    private int scanCooldown = 0;
    private boolean requireSuit = true;

    public DecontaminationAirlockDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECONTAMINATION_AIRLOCK_DOOR_BE, pos, state);
    }

    public void toggleSecurityMode(Player player) {
        this.requireSuit = !this.requireSuit;
        setChanged();
        if (level != null) {
            float pitch = this.requireSuit ? 1.4f : 0.9f;
            level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0f, pitch);
            if (this.requireSuit) {
                player.sendOverlayMessage(Component.literal("§b✦ Airlock Mode: §eCleanroom Decontamination §7(Full Bunny Suit Required)"));
            } else {
                player.sendOverlayMessage(Component.literal("§a✦ Airlock Mode: §6Gowning Anteroom §7(Automatic Proximity Entry - No Suit Required)"));
            }
        }
    }

    public static boolean isWearingFullCleanroomSuit(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CLEANROOM_HOOD) &&
               player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.CLEANROOM_SMOCK) &&
               player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.CLEANROOM_TROUSERS) &&
               player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.CLEANROOM_BOOTIES);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, DecontaminationAirlockDoorBlockEntity entity) {
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
                    // Extend timer slightly until doorway is clear
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

        for (ServerPlayer player : players) {
            // 1. Gowning Anteroom Mode: opens for any personnel entering the gowning room
            if (!this.requireSuit) {
                world.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                setDoorOpenState(world, pos, state, true);
                this.autoCloseTimer = 70;
                player.sendOverlayMessage(Component.literal("§a✦ Anteroom Airlock: Personnel Detected ✦"));
                return;
            }

            // 2. Cleanroom Decontamination Mode: requires full Bunny Suit
            if (player.isCreative() || isWearingFullCleanroomSuit(player)) {
                // Decontamination steam spray sequence
                world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                        16, 0.25, 0.4, 0.25, 0.03);
                world.sendParticles(ParticleTypes.CLOUD,
                        pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        8, 0.3, 0.3, 0.3, 0.02);

                world.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.6f);
                world.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 0.9f);

                setDoorOpenState(world, pos, state, true);
                this.autoCloseTimer = 70; // 3.5 seconds
                player.sendOverlayMessage(Component.literal("§a✦ Decontamination Cycle Complete: Airflow Nominal ✦"));
                return;
            } else {
                // Particulate contamination rejection warning
                world.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0f, 0.5f);
                world.sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                        4, 0.1, 0.1, 0.1, 0.01);
                player.sendOverlayMessage(Component.literal("§c✖ Access Denied: Particulate Hazard! Cleanroom Bunny Suit Required ✖"));
            }
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
        this.requireSuit = view.getBooleanOr("RequireSuit", true);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putInt("AutoCloseTimer", this.autoCloseTimer);
        view.putBoolean("RequireSuit", this.requireSuit);
    }
}

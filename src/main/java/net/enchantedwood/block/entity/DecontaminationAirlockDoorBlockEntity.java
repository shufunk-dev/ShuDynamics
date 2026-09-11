package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
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
import net.enchantedwood.item.ModItems;

import java.util.List;

public class DecontaminationAirlockDoorBlockEntity extends BlockEntity {
    private int autoCloseTimer = 0;
    private int scanCooldown = 0;
    private boolean requireSuit = true;

    public DecontaminationAirlockDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECONTAMINATION_AIRLOCK_DOOR_BE, pos, state);
    }

    public void toggleSecurityMode(PlayerEntity player) {
        this.requireSuit = !this.requireSuit;
        markDirty();
        if (world != null) {
            float pitch = this.requireSuit ? 1.4f : 0.9f;
            world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.BLOCKS, 1.0f, pitch);
            if (this.requireSuit) {
                player.sendMessage(Text.literal("§b✦ Airlock Mode: §eCleanroom Decontamination §7(Full Bunny Suit Required)"), true);
            } else {
                player.sendMessage(Text.literal("§a✦ Airlock Mode: §6Gowning Anteroom §7(Automatic Proximity Entry - No Suit Required)"), true);
            }
        }
    }

    public static boolean isWearingFullCleanroomSuit(PlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.CLEANROOM_HOOD) &&
               player.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.CLEANROOM_SMOCK) &&
               player.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.CLEANROOM_TROUSERS) &&
               player.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.CLEANROOM_BOOTIES);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, DecontaminationAirlockDoorBlockEntity entity) {
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
                    // Extend timer slightly until doorway is clear
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

        for (ServerPlayerEntity player : players) {
            // 1. Gowning Anteroom Mode: opens for any personnel entering the gowning room
            if (!this.requireSuit) {
                world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
                setDoorOpenState(world, pos, state, true);
                this.autoCloseTimer = 70;
                player.sendMessage(Text.literal("§a✦ Anteroom Airlock: Personnel Detected ✦"), true);
                return;
            }

            // 2. Cleanroom Decontamination Mode: requires full Bunny Suit
            if (player.isCreative() || isWearingFullCleanroomSuit(player)) {
                // Decontamination steam spray sequence
                world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                        16, 0.25, 0.4, 0.25, 0.03);
                world.spawnParticles(ParticleTypes.CLOUD,
                        pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        8, 0.3, 0.3, 0.3, 0.02);

                world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.8f, 1.6f);
                world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1.0f, 0.9f);

                setDoorOpenState(world, pos, state, true);
                this.autoCloseTimer = 70; // 3.5 seconds
                player.sendMessage(Text.literal("§a✦ Decontamination Cycle Complete: Airflow Nominal ✦"), true);
                return;
            } else {
                // Particulate contamination rejection warning
                world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 1.0f, 0.5f);
                world.spawnParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                        4, 0.1, 0.1, 0.1, 0.01);
                player.sendMessage(Text.literal("§c✖ Access Denied: Particulate Hazard! Cleanroom Bunny Suit Required ✖"), true);
            }
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
        this.requireSuit = view.getBoolean("RequireSuit", true);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("AutoCloseTimer", this.autoCloseTimer);
        view.putBoolean("RequireSuit", this.requireSuit);
    }
}

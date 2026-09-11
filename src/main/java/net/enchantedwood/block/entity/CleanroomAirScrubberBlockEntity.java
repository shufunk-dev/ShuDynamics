package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.TintedGlassBlock;
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.enchantedwood.block.custom.*;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CleanroomAirScrubberBlockEntity extends BlockEntity implements EnergyProvider {
    public static final int CAPACITY = 250_000;
    public static final int MAX_RECEIVE = 5_000;
    public static final int ENERGY_DRAW = 20; // 20 FE/t
    public static final int MAX_VOLUME = 2_500; // max interior air volume

    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);
    private boolean isSealed = false;
    private final Set<BlockPos> interiorPositions = new HashSet<>();
    private final Set<BlockPos> interiorMachines = new HashSet<>();
    private BlockPos lastLeakPos = null;
    private int scanTimer = 0;

    public CleanroomAirScrubberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CLEANROOM_AIR_SCRUBBER_BE, pos, state);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, CleanroomAirScrubberBlockEntity entity) {
        entity.scanTimer++;

        // Draw energy to maintain positive pressure
        boolean hasEnergy = entity.energyStorage.getEnergy() >= ENERGY_DRAW;
        if (hasEnergy) {
            entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
        }

        // Periodic flood-fill boundary scan every 40 ticks (2 seconds)
        if (entity.scanTimer >= 40) {
            entity.scanTimer = 0;
            entity.scanRoom(world, pos);

            boolean sterile = entity.isSealed && hasEnergy;
            if (state.get(CleanroomAirScrubberBlock.STERILE) != sterile) {
                world.setBlockState(pos, state.with(CleanroomAirScrubberBlock.STERILE, sterile));
            }

            if (sterile) {
                CleanroomManager.registerCleanroom(world, pos, entity.interiorPositions);
                // Particle visual cue at scrubber face
                Direction facing = state.get(CleanroomAirScrubberBlock.FACING);
                BlockPos facePos = pos.offset(facing);
                world.spawnParticles(ParticleTypes.CLOUD,
                        facePos.getX() + 0.5, facePos.getY() + 0.5, facePos.getZ() + 0.5,
                        2, 0.2, 0.2, 0.2, 0.01);
            } else {
                CleanroomManager.unregisterCleanroom(world, pos);
            }

            entity.markDirty();
        }

        // Wireless Cleanroom Induction Power Bus:
        // When sterile & powered, broadcast external FE from the Scrubber directly to any machine inside the cleanroom!
        boolean sterile = entity.isSealed && hasEnergy;
        if (sterile && entity.energyStorage.getEnergy() > ENERGY_DRAW && !entity.interiorMachines.isEmpty()) {
            int available = entity.energyStorage.getEnergy() - ENERGY_DRAW;
            for (BlockPos mPos : entity.interiorMachines) {
                if (available <= 0) break;
                BlockEntity be = world.getBlockEntity(mPos);
                if (be instanceof EnergyProvider provider) {
                    EnergyStorage targetStorage = provider.getEnergyStorage(null);
                    if (targetStorage != null && targetStorage.getEnergy() < targetStorage.getMaxEnergy()) {
                        int needed = targetStorage.getMaxEnergy() - targetStorage.getEnergy();
                        int toTransfer = Math.min(needed, Math.min(available, 1000)); // up to 1,000 FE/t per machine
                        if (toTransfer > 0) {
                            int accepted = targetStorage.insertEnergy(toTransfer, false);
                            entity.energyStorage.extractEnergy(accepted, false);
                            available -= accepted;
                        }
                    }
                }
            }
        }
    }

    /**
     * Breadth-First-Search (BFS) flood-fill to verify airtight room perimeter.
     * Bounded at MAX_VOLUME (2,500 blocks).
     */
    public void scanRoom(ServerWorld world, BlockPos scrubberPos) {
        Direction facing = world.getBlockState(scrubberPos).get(CleanroomAirScrubberBlock.FACING);
        BlockPos start = scrubberPos.offset(facing);

        if (!world.isAir(start) && !isCleanroomInteriorBlock(world, start, world.getBlockState(start))) {
            // Front face is blocked by an unsealed block
            isSealed = false;
            lastLeakPos = start;
            interiorPositions.clear();
            interiorMachines.clear();
            return;
        }

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);

        interiorPositions.clear();
        interiorMachines.clear();
        lastLeakPos = null;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            interiorPositions.add(current);

            BlockEntity currentBe = world.getBlockEntity(current);
            if (currentBe instanceof EnergyProvider && currentBe != this) {
                interiorMachines.add(current);
            }

            if (interiorPositions.size() > MAX_VOLUME) {
                // Room is too large or unbounded
                isSealed = false;
                lastLeakPos = current;
                interiorPositions.clear();
                interiorMachines.clear();
                return;
            }

            if (current.getY() >= world.getTopYInclusive() - 5 || current.getY() <= world.getBottomY() + 5) {
                isSealed = false;
                lastLeakPos = current;
                interiorPositions.clear();
                interiorMachines.clear();
                return;
            }

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.offset(dir);
                if (neighbor.equals(scrubberPos)) {
                    continue; // The scrubber itself is a sealed boundary
                }

                BlockState neighborState = world.getBlockState(neighbor);

                // 1. Check if neighbor is an interior machine with EnergyProvider
                BlockEntity nBe = world.getBlockEntity(neighbor);
                if (nBe instanceof EnergyProvider && nBe != this && !(neighborState.getBlock() instanceof DoorBlock)) {
                    if (visited.add(neighbor)) {
                        interiorPositions.add(neighbor);
                        interiorMachines.add(neighbor);
                        queue.add(neighbor);
                    }
                    continue;
                }

                // 2. Check if neighbor is an airtight boundary wall
                if (isAirtightBoundary(world, neighbor, neighborState)) {
                    continue;
                }

                // 3. Check if neighbor is air
                if (world.isAir(neighbor)) {
                    if (visited.add(neighbor)) {
                        queue.add(neighbor);
                    }
                } else if (isCleanroomInteriorBlock(world, neighbor, neighborState)) {
                    // Interior furnishing or non-energy equipment
                    if (visited.add(neighbor)) {
                        interiorPositions.add(neighbor);
                        queue.add(neighbor);
                    }
                } else {
                    // Non-airtight leak block (dirt, cobblestone, outside ground, open void, etc.)
                    isSealed = false;
                    lastLeakPos = neighbor;
                    interiorPositions.clear();
                    interiorMachines.clear();
                    return;
                }
            }
        }

        isSealed = true;
    }

    private boolean isAirtightBoundary(ServerWorld world, BlockPos pos, BlockState state) {
        var block = state.getBlock();

        // Any BlockEntity that is NOT a door, scrubber, or medical cabinet is interior equipment, NOT a boundary wall!
        BlockEntity be = world.getBlockEntity(pos);
        if (be != null && !(block instanceof CleanroomAirScrubberBlock) && !(block instanceof DoorBlock) && !(block instanceof SterileMedicalCabinetBlock)) {
            return false;
        }

        if (block instanceof CleanroomCasingBlock ||
            block instanceof CleanroomFilterCasingBlock ||
            block instanceof CleanroomAirScrubberBlock ||
            block instanceof DecontaminationAirlockDoorBlock ||
            block instanceof GowningAirlockDoorBlock ||
            block instanceof SterileCleanroomLampBlock ||
            block instanceof SterileMedicalCabinetBlock ||
            block instanceof ReinforcedTankGlassBlock ||
            block == net.enchantedwood.block.ModBlocks.AEROGEL_GLASS ||
            block instanceof TransparentBlock ||
            block instanceof TintedGlassBlock) {
            return true;
        }

        if (state.isIn(BlockTags.IMPERMEABLE)) {
            return true;
        }

        // Full solid cubes (e.g. steel blocks, titanium casings, smooth stone, concrete)
        return state.isOpaqueFullCube();
    }

    private boolean isCleanroomInteriorBlock(ServerWorld world, BlockPos pos, BlockState state) {
        var block = state.getBlock();

        // Doors and the scrubber are boundaries, not interior furnishings
        if (block instanceof DoorBlock || block instanceof CleanroomAirScrubberBlock) {
            return false;
        }

        // Any block entity inside the cleanroom (machines, synthesizers, looms, chests, cabinets)
        if (world.getBlockEntity(pos) != null) {
            return true;
        }

        // Lighting, redstone, buttons, levers, display panels, carpets
        if (block instanceof SterileCleanroomLampBlock ||
            block instanceof net.minecraft.block.TorchBlock ||
            block instanceof net.minecraft.block.WallTorchBlock ||
            block instanceof net.minecraft.block.LanternBlock ||
            block instanceof net.minecraft.block.EndRodBlock ||
            block instanceof net.minecraft.block.RedstoneWireBlock ||
            block instanceof net.minecraft.block.LeverBlock ||
            block instanceof net.minecraft.block.ButtonBlock ||
            block instanceof net.minecraft.block.LightBlock ||
            block instanceof net.minecraft.block.CarpetBlock) {
            return true;
        }
        return false;
    }

    public void reportStatusTo(PlayerEntity player) {
        if (world == null || world.isClient()) return;

        boolean hasPower = energyStorage.getEnergy() >= ENERGY_DRAW;

        if (isSealed && hasPower) {
            player.sendMessage(Text.literal("§a========================================"), false);
            player.sendMessage(Text.literal("§a✦ CLEANROOM AIR SCRUBBER: 100% STERILE ✦"), false);
            player.sendMessage(Text.literal("§7• Hermetic Enclosure: §aVERIFIED (No Leaks)"), false);
            player.sendMessage(Text.literal("§7• Positive Air Pressure: §aACTIVE"), false);
            player.sendMessage(Text.literal("§7• Sterile Interior Volume: §f" + interiorPositions.size() + " blocks"), false);
            player.sendMessage(Text.literal("§e• Cleanroom Energy Buffer: §6" + String.format("%,d / %,d FE", energyStorage.getEnergy(), CAPACITY)), false);
            player.sendMessage(Text.literal("§d• Wireless Power Grid: §aACTIVE §7(" + interiorMachines.size() + " Machines Powered wirelessly)"), false);
            player.sendMessage(Text.literal("§b✦ Synthesizer output upgraded to GRADE-A PURE!"), false);
            player.sendMessage(Text.literal("§a========================================"), false);
        } else if (!hasPower) {
            player.sendMessage(Text.literal("§e========================================"), false);
            player.sendMessage(Text.literal("§e⚠ CLEANROOM AIR SCRUBBER: OFFLINE (NO POWER) ⚠"), false);
            player.sendMessage(Text.literal("§7• Connect FE power cables (Requires 20 FE/t)."), false);
            player.sendMessage(Text.literal("§7• Current Energy: §c" + String.format("%,d / %,d FE", energyStorage.getEnergy(), CAPACITY)), false);
            player.sendMessage(Text.literal("§e========================================"), false);
        } else {
            player.sendMessage(Text.literal("§c========================================"), false);
            player.sendMessage(Text.literal("§c✖ CLEANROOM AIR SCRUBBER: SEAL BREACH DETECTED ✖"), false);
            player.sendMessage(Text.literal("§7• Status: §cUNPRESSURIZED (Atmosphere Leaking)"), false);
            if (lastLeakPos != null) {
                player.sendMessage(Text.literal("§7• Breach Coordinate: §e[" + lastLeakPos.getX() + ", " + lastLeakPos.getY() + ", " + lastLeakPos.getZ() + "]"), false);
                player.sendMessage(Text.literal("§8  Seal all open holes with Cleanroom Casings, Glass, or an Airlock Door."), false);
            } else {
                player.sendMessage(Text.literal("§7• Place scrubber with at least 1 adjacent interior air block."), false);
            }
            player.sendMessage(Text.literal("§c========================================"), false);
        }
    }

    public void onRemoved() {
        if (world != null) {
            CleanroomManager.unregisterZone(world.getRegistryKey(), pos);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.energyStorage.readData(view);
        this.isSealed = view.getBoolean("IsSealed", false);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        this.energyStorage.writeData(view);
        view.putBoolean("IsSealed", this.isSealed);
    }

    @Override
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }
}

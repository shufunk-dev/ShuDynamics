package net.enchantedwood.block.custom;

import net.enchantedwood.entity.ModEntities;
import net.enchantedwood.entity.custom.ResonanceColossusEntity;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ResonanceAltarBlock extends Block {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public ResonanceAltarBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return dismissActiveBosses(state, world, pos, player);
        }
        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return dismissActiveBosses(state, world, pos, player);
        }

        boolean isTier1 = stack.is(ModItems.CORE_OF_AWAKENING);
        boolean isTier2 = stack.is(ModItems.CORRUPTED_CORE_OF_CATACLYSM);
        boolean isTier3 = stack.is(ModItems.PRIMORDIAL_RIFT_KEYSTONE);

        if (!isTier1 && !isTier2 && !isTier3) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Safety Protocol 1: Dimension Lock (Only active inside The Convergence dimension)
        if (world.dimension() != ModDimensions.CONVERGENCE_WORLD_KEY) {
            player.sendSystemMessage(Component.literal("§cThe Resonance Altar requires the unstable dimensional frequencies of The Convergence to perform summoning rituals."));
            world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.8f);
            return InteractionResult.FAIL;
        }

        // Check if already active & verify if any tier of boss is active nearby
        net.minecraft.world.phys.AABB checkArea = new net.minecraft.world.phys.AABB(pos).inflate(128.0);
        java.util.List<ResonanceColossusEntity> t1 = world.getEntitiesOfClass(ResonanceColossusEntity.class, checkArea, net.minecraft.world.entity.LivingEntity::isAlive);
        java.util.List<net.enchantedwood.entity.custom.AscendantColossusEntity> t2 = world.getEntitiesOfClass(net.enchantedwood.entity.custom.AscendantColossusEntity.class, checkArea, net.minecraft.world.entity.LivingEntity::isAlive);
        java.util.List<net.enchantedwood.entity.custom.PrimordialCataclysmEntity> t3 = world.getEntitiesOfClass(net.enchantedwood.entity.custom.PrimordialCataclysmEntity.class, checkArea, net.minecraft.world.entity.LivingEntity::isAlive);

        boolean anyBossActive = !t1.isEmpty() || !t2.isEmpty() || !t3.isEmpty();

        if (state.getValue(ACTIVE)) {
            if (anyBossActive) {
                player.sendOverlayMessage(Component.literal("§eA Boss encounter is currently active on the battlefield!"));
                return InteractionResult.FAIL;
            } else {
                world.setBlockAndUpdate(pos, state.setValue(ACTIVE, false));
            }
        } else if (anyBossActive) {
            player.sendSystemMessage(Component.literal("§eA Boss encounter is currently active on the battlefield!"));
            return InteractionResult.FAIL;
        }

        if (world instanceof ServerLevel serverWorld) {
            if (!player.isCreative()) {
                stack.shrink(1);
            }

            // Set Altar to active
            serverWorld.setBlockAndUpdate(pos, state.setValue(ACTIVE, true));

            if (isTier1) {
                // Tier 1: Resonance Colossus
                serverWorld.sendParticles(ParticleTypes.SONIC_BOOM, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.0);
                serverWorld.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 40, 1.0, 1.0, 1.0, 0.15);
                serverWorld.playSound(null, pos, SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.5f, 0.7f);
                serverWorld.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.5f, 0.8f);

                ResonanceColossusEntity colossus = ModEntities.RESONANCE_COLOSSUS.create(serverWorld, net.minecraft.world.entity.EntitySpawnReason.EVENT);
                if (colossus != null) {
                    colossus.snapTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0f, 0.0f);
                    colossus.setAltarPos(pos);
                    serverWorld.addFreshEntity(colossus);
                }

                for (Player p : serverWorld.players()) {
                    p.sendOverlayMessage(Component.literal("§5✦ The ground shakes as The Resonance Colossus (Tier 1) awakens at the Dais! ✦"));
                }
            } else if (isTier2) {
                // Tier 2: Ascendant Colossus
                serverWorld.sendParticles(ParticleTypes.SONIC_BOOM, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.0);
                serverWorld.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 60, 1.2, 1.2, 1.2, 0.2);
                serverWorld.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0f, 0.8f);
                serverWorld.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.0f, 0.6f);

                net.enchantedwood.entity.custom.AscendantColossusEntity ascendant = ModEntities.ASCENDANT_COLOSSUS.create(serverWorld, net.minecraft.world.entity.EntitySpawnReason.EVENT);
                if (ascendant != null) {
                    ascendant.snapTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0f, 0.0f);
                    ascendant.setAltarPos(pos);
                    serverWorld.addFreshEntity(ascendant);
                }

                for (Player p : serverWorld.players()) {
                    p.sendSystemMessage(Component.literal("§4✦ Crimson lightning rends the sky as The Ascendant Colossus (Tier 2) descends upon the Dais! ✦"));
                }
            } else {
                // Tier 3: Primordial Cataclysm
                serverWorld.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 100, 2.0, 2.0, 2.0, 0.3);
                serverWorld.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 80, 1.5, 1.5, 1.5, 0.1);
                serverWorld.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.HOSTILE, 2.5f, 0.4f);
                serverWorld.playSound(null, pos, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.5f, 0.5f);

                net.enchantedwood.entity.custom.PrimordialCataclysmEntity cataclysm = ModEntities.PRIMORDIAL_CATACLYSM.create(serverWorld, net.minecraft.world.entity.EntitySpawnReason.EVENT);
                if (cataclysm != null) {
                    cataclysm.snapTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0f, 0.0f);
                    cataclysm.setAltarPos(pos);
                    serverWorld.addFreshEntity(cataclysm);
                }

                for (Player p : serverWorld.players()) {
                    p.sendSystemMessage(Component.literal("§d✦ The fabric of reality fractures! The Primordial Cataclysm (Tier 3 Mythic Boss) has arrived! ✦"));
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    private InteractionResult dismissActiveBosses(BlockState state, Level world, BlockPos pos, Player player) {
        if (!world.isClientSide() && world instanceof ServerLevel sw) {
            net.minecraft.world.phys.AABB checkArea = new net.minecraft.world.phys.AABB(pos).inflate(128.0);
            var t1 = sw.getEntitiesOfClass(ResonanceColossusEntity.class, checkArea, net.minecraft.world.entity.LivingEntity::isAlive);
            var t2 = sw.getEntitiesOfClass(net.enchantedwood.entity.custom.AscendantColossusEntity.class, checkArea, net.minecraft.world.entity.LivingEntity::isAlive);
            var t3 = sw.getEntitiesOfClass(net.enchantedwood.entity.custom.PrimordialCataclysmEntity.class, checkArea, net.minecraft.world.entity.LivingEntity::isAlive);
            int removed = 0;
            for (var b : t1) { b.discard(); removed++; }
            for (var b : t2) { b.discard(); removed++; }
            for (var b : t3) { b.discard(); removed++; }
            sw.setBlockAndUpdate(pos, state.setValue(ACTIVE, false));
            if (removed > 0) {
                player.sendSystemMessage(Component.literal("§e✦ Resonance Altar: Dismissed " + removed + " active boss encounter(s) and reset arena! ✦"));
                sw.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.5f, 1.0f);
            } else {
                player.sendOverlayMessage(Component.literal("§7✦ Resonance Altar: Reset to idle (no active bosses in arena). ✦"));
            }
        }
        return InteractionResult.SUCCESS;
    }
}

package net.enchantedwood.block.custom;

import net.enchantedwood.entity.ModEntities;
import net.enchantedwood.entity.custom.ResonanceColossusEntity;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.world.dimension.ModDimensions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ResonanceAltarBlock extends Block {

    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    public ResonanceAltarBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(ACTIVE, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        boolean isTier1 = stack.isOf(ModItems.CORE_OF_AWAKENING);
        boolean isTier2 = stack.isOf(ModItems.CORRUPTED_CORE_OF_CATACLYSM);
        boolean isTier3 = stack.isOf(ModItems.PRIMORDIAL_RIFT_KEYSTONE);

        if (!isTier1 && !isTier2 && !isTier3) {
            return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
        }

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        // Safety Protocol 1: Dimension Lock (Only active inside The Convergence dimension)
        if (world.getRegistryKey() != ModDimensions.CONVERGENCE_WORLD_KEY) {
            player.sendMessage(Text.literal("§cThe Resonance Altar requires the unstable dimensional frequencies of The Convergence to perform summoning rituals."), true);
            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 0.8f);
            return ActionResult.FAIL;
        }

        // Check if already active & verify if any tier of boss is active nearby
        net.minecraft.util.math.Box checkArea = new net.minecraft.util.math.Box(pos).expand(128.0);
        java.util.List<ResonanceColossusEntity> t1 = world.getEntitiesByClass(ResonanceColossusEntity.class, checkArea, net.minecraft.entity.LivingEntity::isAlive);
        java.util.List<net.enchantedwood.entity.custom.AscendantColossusEntity> t2 = world.getEntitiesByClass(net.enchantedwood.entity.custom.AscendantColossusEntity.class, checkArea, net.minecraft.entity.LivingEntity::isAlive);
        java.util.List<net.enchantedwood.entity.custom.PrimordialCataclysmEntity> t3 = world.getEntitiesByClass(net.enchantedwood.entity.custom.PrimordialCataclysmEntity.class, checkArea, net.minecraft.entity.LivingEntity::isAlive);

        boolean anyBossActive = !t1.isEmpty() || !t2.isEmpty() || !t3.isEmpty();

        if (state.get(ACTIVE)) {
            if (anyBossActive) {
                player.sendMessage(Text.literal("§eA Boss encounter is currently active on the battlefield!"), true);
                return ActionResult.FAIL;
            } else {
                world.setBlockState(pos, state.with(ACTIVE, false));
            }
        } else if (anyBossActive) {
            player.sendMessage(Text.literal("§eA Boss encounter is currently active on the battlefield!"), true);
            return ActionResult.FAIL;
        }

        if (world instanceof ServerWorld serverWorld) {
            if (!player.isCreative()) {
                stack.decrement(1);
            }

            // Set Altar to active
            serverWorld.setBlockState(pos, state.with(ACTIVE, true));

            if (isTier1) {
                // Tier 1: Resonance Colossus
                serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.0);
                serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 40, 1.0, 1.0, 1.0, 0.15);
                serverWorld.playSound(null, pos, SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 1.5f, 0.7f);
                serverWorld.playSound(null, pos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 1.5f, 0.8f);

                ResonanceColossusEntity colossus = ModEntities.RESONANCE_COLOSSUS.create(serverWorld, net.minecraft.entity.SpawnReason.EVENT);
                if (colossus != null) {
                    colossus.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0f, 0.0f);
                    colossus.setAltarPos(pos);
                    serverWorld.spawnEntity(colossus);
                }

                for (PlayerEntity p : serverWorld.getPlayers()) {
                    p.sendMessage(Text.literal("§5✦ The ground shakes as The Resonance Colossus (Tier 1) awakens at the Dais! ✦"), false);
                }
            } else if (isTier2) {
                // Tier 2: Ascendant Colossus
                serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.0);
                serverWorld.spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 60, 1.2, 1.2, 1.2, 0.2);
                serverWorld.playSound(null, pos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 2.0f, 0.8f);
                serverWorld.playSound(null, pos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 2.0f, 0.6f);

                net.enchantedwood.entity.custom.AscendantColossusEntity ascendant = ModEntities.ASCENDANT_COLOSSUS.create(serverWorld, net.minecraft.entity.SpawnReason.EVENT);
                if (ascendant != null) {
                    ascendant.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0f, 0.0f);
                    ascendant.setAltarPos(pos);
                    serverWorld.spawnEntity(ascendant);
                }

                for (PlayerEntity p : serverWorld.getPlayers()) {
                    p.sendMessage(Text.literal("§4✦ Crimson lightning rends the sky as The Ascendant Colossus (Tier 2) descends upon the Dais! ✦"), false);
                }
            } else {
                // Tier 3: Primordial Cataclysm
                serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 100, 2.0, 2.0, 2.0, 0.3);
                serverWorld.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 80, 1.5, 1.5, 1.5, 0.1);
                serverWorld.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.HOSTILE, 2.5f, 0.4f);
                serverWorld.playSound(null, pos, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 2.5f, 0.5f);

                net.enchantedwood.entity.custom.PrimordialCataclysmEntity cataclysm = ModEntities.PRIMORDIAL_CATACLYSM.create(serverWorld, net.minecraft.entity.SpawnReason.EVENT);
                if (cataclysm != null) {
                    cataclysm.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0f, 0.0f);
                    cataclysm.setAltarPos(pos);
                    serverWorld.spawnEntity(cataclysm);
                }

                for (PlayerEntity p : serverWorld.getPlayers()) {
                    p.sendMessage(Text.literal("§d✦ The fabric of reality fractures! The Primordial Cataclysm (Tier 3 Mythic Boss) has arrived! ✦"), false);
                }
            }
        }

        return ActionResult.SUCCESS;
    }
}

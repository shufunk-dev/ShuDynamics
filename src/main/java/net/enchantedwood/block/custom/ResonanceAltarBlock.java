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
        if (!stack.isOf(ModItems.CORE_OF_AWAKENING)) {
            return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
        }

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        // Safety Protocol 1: Dimension Lock (Only active inside The Convergence dimension)
        if (world.getRegistryKey() != ModDimensions.CONVERGENCE_WORLD_KEY) {
            player.sendMessage(Text.literal("§cThe Resonance Altar requires the unstable dimensional frequencies of The Convergence to awaken the Colossus."), true);
            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 0.8f);
            return ActionResult.FAIL;
        }

        // Check if already active
        if (state.get(ACTIVE)) {
            // Failsafe: check if a living Resonance Colossus actually exists nearby
            net.minecraft.util.math.Box checkArea = new net.minecraft.util.math.Box(pos).expand(128.0);
            java.util.List<ResonanceColossusEntity> activeBosses = world.getEntitiesByClass(ResonanceColossusEntity.class, checkArea, net.minecraft.entity.LivingEntity::isAlive);
            if (activeBosses.isEmpty()) {
                // Boss is not present: auto-reset altar to allow re-awakening!
                world.setBlockState(pos, state.with(ACTIVE, false));
            } else {
                player.sendMessage(Text.literal("§eThe Resonance Colossus is currently active on the battlefield!"), true);
                return ActionResult.FAIL;
            }
        }

        if (world instanceof ServerWorld serverWorld) {
            // Consume Core of Awakening unless creative
            if (!player.isCreative()) {
                stack.decrement(1);
            }

            // Set Altar to active
            serverWorld.setBlockState(pos, state.with(ACTIVE, true));

            // Dramatic Awakening FX
            serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.0);
            serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 40, 1.0, 1.0, 1.0, 0.15);
            serverWorld.playSound(null, pos, SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 1.5f, 0.7f);
            serverWorld.playSound(null, pos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 1.5f, 0.8f);

            // Spawn The Resonance Colossus
            ResonanceColossusEntity colossus = ModEntities.RESONANCE_COLOSSUS.create(serverWorld, net.minecraft.entity.SpawnReason.EVENT);
            if (colossus != null) {
                colossus.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0f, 0.0f);
                colossus.setAltarPos(pos);
                serverWorld.spawnEntity(colossus);
            }

            for (PlayerEntity p : serverWorld.getPlayers()) {
                p.sendMessage(Text.literal("§5✦ The ground shakes as The Resonance Colossus awakens at the Dais! ✦"), false);
            }
        }

        return ActionResult.SUCCESS;
    }
}

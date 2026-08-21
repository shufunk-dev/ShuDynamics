package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.enchantedwood.block.custom.EnchantedLampBlock;

import java.util.List;

public class EnchantedLampBlockEntity extends BlockEntity {
    private static final double WARD_RADIUS = 32.0;
    private int tickCounter = 0;

    public EnchantedLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANTED_LAMP_BLOCK_ENTITY, pos, state);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, EnchantedLampBlockEntity entity) {
        if (!state.get(EnchantedLampBlock.LIT)) {
            return;
        }

        // Check every 10 ticks (0.5s) for hostile mobs in sanctuary aura
        if (++entity.tickCounter % 10 == 0) {
            Box sanctuaryBox = new Box(pos).expand(WARD_RADIUS);
            List<HostileEntity> monsters = world.getEntitiesByClass(HostileEntity.class, sanctuaryBox,
                    mob -> mob.isAlive() && mob.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= (WARD_RADIUS * WARD_RADIUS));

            for (HostileEntity mob : monsters) {
                // Dissolve / repel hostile monsters in sanctuary light
                world.spawnParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 0.5, mob.getZ(), 10, 0.2, 0.5, 0.2, 0.05);
                world.spawnParticles(ParticleTypes.ENCHANT, mob.getX(), mob.getY() + 1.0, mob.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
                mob.discard(); // Safely removes hostile monster from sanctuary
            }
        }
    }
}

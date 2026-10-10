package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.EnchantedLampBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.List;

public class EnchantedLampBlockEntity extends BlockEntity {
    private static final double WARD_RADIUS = 32.0;
    private int tickCounter = 0;

    public EnchantedLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANTED_LAMP_BLOCK_ENTITY, pos, state);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, EnchantedLampBlockEntity entity) {
        if (!state.getValue(EnchantedLampBlock.LIT)) {
            return;
        }

        // Check every 10 ticks (0.5s) for hostile mobs in sanctuary aura
        if (++entity.tickCounter % 10 == 0) {
            AABB sanctuaryBox = new AABB(pos).inflate(WARD_RADIUS);
            List<Monster> monsters = world.getEntitiesOfClass(Monster.class, sanctuaryBox,
                    mob -> mob.isAlive() && mob.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= (WARD_RADIUS * WARD_RADIUS));

            for (Monster mob : monsters) {
                // Dissolve / repel hostile monsters in sanctuary light
                world.sendParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 0.5, mob.getZ(), 10, 0.2, 0.5, 0.2, 0.05);
                world.sendParticles(ParticleTypes.ENCHANT, mob.getX(), mob.getY() + 1.0, mob.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
                mob.discard(); // Safely removes hostile monster from sanctuary
            }
        }
    }
}

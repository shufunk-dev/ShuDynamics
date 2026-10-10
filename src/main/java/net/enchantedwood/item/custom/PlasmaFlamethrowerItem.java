package net.enchantedwood.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class PlasmaFlamethrowerItem extends Item {
    public PlasmaFlamethrowerItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (!world.isClientSide()) {
            ServerLevel serverWorld = (ServerLevel) world;
            Vec3 eyePos = user.getEyePosition();
            Vec3 lookVec = user.getViewVector(1.0f);
            double range = 12.0;

            // Project continuous plasma stream particles
            for (double d = 0.8; d <= range; d += 0.5) {
                Vec3 point = eyePos.add(lookVec.scale(d));
                double spread = 0.05 * (d / 2.0);
                serverWorld.sendParticles(
                        ParticleTypes.FLAME,
                        point.x, point.y - 0.15, point.z,
                        4, spread, spread, spread, 0.03
                );
                serverWorld.sendParticles(
                        ParticleTypes.SMOKE,
                        point.x, point.y - 0.15, point.z,
                        1, spread, spread, spread, 0.01
                );
                if (d > 6.0 && d % 1.0 == 0) {
                    serverWorld.sendParticles(
                            ParticleTypes.LAVA,
                            point.x, point.y - 0.15, point.z,
                            1, spread, spread, spread, 0.01
                    );
                }
            }

            // Damage and ignite mobs along the cone
            Vec3 targetEnd = eyePos.add(lookVec.scale(range));
            AABB coneBox = new AABB(eyePos, targetEnd).inflate(1.5);
            List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, coneBox, e -> e != user && e.isAlive());

            for (LivingEntity target : targets) {
                Vec3 toTarget = target.getEyePosition().subtract(eyePos).normalize();
                double dot = lookVec.dot(toTarget);
                if (dot > 0.70) { // inside 45-degree frontal cone
                    target.igniteForSeconds(8.0f);
                    target.hurtServer(serverWorld, world.damageSources().onFire(), 7.0f);
                    target.knockback(0.4, -lookVec.x, -lookVec.z, world.damageSources().onFire(), 0.0f);
                }
            }

            // Block hit igniting
            BlockHitResult hit = world.clip(new ClipContext(
                    eyePos, targetEnd,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    user
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                net.minecraft.core.BlockPos firePos = hit.getBlockPos().relative(hit.getDirection());
                if (world.getBlockState(firePos).isAir()) {
                    world.setBlockAndUpdate(firePos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                }
            }

            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8f, 1.2f);
            stack.hurtAndBreak(1, user, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }

        user.getCooldowns().addCooldown(stack, 6);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§6✦ High-Energy Plasma Projector"));
        textConsumer.accept(Component.literal("§7Right-click to unleash a 12-block streaming beam of superheated plasma."));
        textConsumer.accept(Component.literal("§c✦ Ignites targets for 8s and pierces through mobs."));
        textConsumer.accept(Component.literal("§8Durability: 850 Uses"));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}

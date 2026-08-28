package net.enchantedwood.item.custom;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Consumer;

public class PlasmaFlamethrowerItem extends Item {
    public PlasmaFlamethrowerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient()) {
            ServerWorld serverWorld = (ServerWorld) world;
            Vec3d eyePos = user.getEyePos();
            Vec3d lookVec = user.getRotationVec(1.0f);
            double range = 12.0;

            // Project continuous plasma stream particles
            for (double d = 0.8; d <= range; d += 0.5) {
                Vec3d point = eyePos.add(lookVec.multiply(d));
                double spread = 0.05 * (d / 2.0);
                serverWorld.spawnParticles(
                        ParticleTypes.FLAME,
                        point.x, point.y - 0.15, point.z,
                        4, spread, spread, spread, 0.03
                );
                serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,
                        point.x, point.y - 0.15, point.z,
                        1, spread, spread, spread, 0.01
                );
                if (d > 6.0 && d % 1.0 == 0) {
                    serverWorld.spawnParticles(
                            ParticleTypes.LAVA,
                            point.x, point.y - 0.15, point.z,
                            1, spread, spread, spread, 0.01
                    );
                }
            }

            // Damage and ignite mobs along the cone
            Vec3d targetEnd = eyePos.add(lookVec.multiply(range));
            Box coneBox = new Box(eyePos, targetEnd).expand(1.5);
            List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, coneBox, e -> e != user && e.isAlive());

            for (LivingEntity target : targets) {
                Vec3d toTarget = target.getEyePos().subtract(eyePos).normalize();
                double dot = lookVec.dotProduct(toTarget);
                if (dot > 0.70) { // inside 45-degree frontal cone
                    target.setOnFireFor(8.0f);
                    target.damage(serverWorld, world.getDamageSources().onFire(), 7.0f);
                    target.takeKnockback(0.4, -lookVec.x, -lookVec.z);
                }
            }

            // Block hit igniting
            BlockHitResult hit = world.raycast(new RaycastContext(
                    eyePos, targetEnd,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    user
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                net.minecraft.util.math.BlockPos firePos = hit.getBlockPos().offset(hit.getSide());
                if (world.getBlockState(firePos).isAir()) {
                    world.setBlockState(firePos, net.minecraft.block.Blocks.FIRE.getDefaultState());
                }
            }

            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.8f, 1.2f);
            stack.damage(1, user, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }

        user.getItemCooldownManager().set(stack, 6);
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§6✦ High-Energy Plasma Projector"));
        textConsumer.accept(Text.literal("§7Right-click to unleash a 12-block streaming beam of superheated plasma."));
        textConsumer.accept(Text.literal("§c✦ Ignites targets for 8s and pierces through mobs."));
        textConsumer.accept(Text.literal("§8Durability: 850 Uses"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}

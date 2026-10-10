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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AscendantCleaverItem extends Item {

    public AscendantCleaverItem(Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (user.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        user.getCooldowns().addCooldown(stack, 140); // 7 second cooldown (faster than tier 1)

        if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
            Vec3 origin = user.position();
            Vec3 look = user.getViewVector(1.0f);

            // Ground Shockwave forward 20 blocks
            for (int i = 1; i <= 20; i++) {
                Vec3 point = origin.add(look.scale(i));
                serverWorld.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y + 0.5, point.z, 1, 0, 0, 0, 0);
                serverWorld.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y + 0.5, point.z, 12, 0.5, 0.5, 0.5, 0.1);
                serverWorld.sendParticles(ParticleTypes.FLAME, point.x, point.y + 0.5, point.z, 8, 0.4, 0.4, 0.4, 0.05);

                AABB hitBox = new AABB(point.x - 2.0, point.y - 1.0, point.z - 2.0, point.x + 2.0, point.y + 2.5, point.z + 2.0);
                List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != user && e.isAlive());

                for (LivingEntity target : targets) {
                    target.hurtServer(serverWorld, serverWorld.damageSources().playerAttack(user), 24.0f);
                    target.push(0, 0.9, 0);
                    target.needsSync = true;
                }
            }

            serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.8f, 0.8f);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§4✦ Ascendant World-Cleaver ✦"));
        textConsumer.accept(Component.literal("§7Forged by infusing the Resonance Cleaver with Primordial Catalysts."));
        textConsumer.accept(Component.literal("§e✦ Right-Click: §cCascading Sonic Fissure"));
        textConsumer.accept(Component.literal("§8 • Tears open a 20-block kinetic trench in targeted direction"));
        textConsumer.accept(Component.literal("§8 • Deals 24 True Damage to all caught victims and launches them"));
        textConsumer.accept(Component.literal("§8 • Cooldown: 7.0 seconds"));
        textConsumer.accept(Component.literal("§b✦ Legendary God-Tier Weapon."));
    }
}

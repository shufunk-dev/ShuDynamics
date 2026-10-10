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

public class SingularityStaffItem extends Item {

    public SingularityStaffItem(Properties settings) {
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

        user.getCooldowns().addCooldown(stack, 240); // 12 second cooldown

        if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
            Vec3 look = user.getViewVector(1.0f);
            Vec3 targetCenter = user.getEyePosition().add(look.scale(12.0));

            serverWorld.sendParticles(ParticleTypes.PORTAL, targetCenter.x, targetCenter.y, targetCenter.z, 150, 2.0, 2.0, 2.0, 0.2);
            serverWorld.sendParticles(ParticleTypes.REVERSE_PORTAL, targetCenter.x, targetCenter.y, targetCenter.z, 100, 1.5, 1.5, 1.5, 0.1);
            serverWorld.sendParticles(ParticleTypes.SONIC_BOOM, targetCenter.x, targetCenter.y, targetCenter.z, 2, 0.0, 0.0, 0.0, 0.0);

            serverWorld.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 2.0f, 0.5f);

            AABB pullBox = new AABB(targetCenter.x - 8, targetCenter.y - 8, targetCenter.z - 8, targetCenter.x + 8, targetCenter.y + 8, targetCenter.z + 8);
            List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class, pullBox, e -> e != user && e.isAlive());

            for (LivingEntity target : targets) {
                Vec3 pull = targetCenter.subtract(target.position()).normalize().scale(1.4);
                target.setDeltaMovement(pull.x, 0.5, pull.z);
                target.needsSync = true;
                target.hurtServer(serverWorld, serverWorld.damageSources().magic(), 12.0f);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§5✦ Relic of the Resonance Colossus ✦"));
        textConsumer.accept(Component.literal("§7High-tech gravitational staff channeling a localized black hole."));
        textConsumer.accept(Component.literal("§e✦ Right-Click: §dSingularity Vortex"));
        textConsumer.accept(Component.literal("§8 • Creates a gravity well 12 blocks ahead pulling all enemies inward"));
        textConsumer.accept(Component.literal("§8 • Crushes targets for 12 magic/kinetic damage"));
        textConsumer.accept(Component.literal("§8 • Cooldown: 12s"));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}

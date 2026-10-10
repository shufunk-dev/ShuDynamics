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

public class VoidSingularityNexusItem extends Item {

    public VoidSingularityNexusItem(Properties settings) {
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

        user.getCooldowns().addCooldown(stack, 180); // 9 second cooldown

        if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
            Vec3 look = user.getViewVector(1.0f);
            Vec3 targetCenter = user.getEyePosition().add(look.scale(16.0));

            serverWorld.sendParticles(ParticleTypes.PORTAL, targetCenter.x, targetCenter.y, targetCenter.z, 200, 3.0, 3.0, 3.0, 0.3);
            serverWorld.sendParticles(ParticleTypes.REVERSE_PORTAL, targetCenter.x, targetCenter.y, targetCenter.z, 150, 2.5, 2.5, 2.5, 0.2);
            serverWorld.sendParticles(ParticleTypes.SONIC_BOOM, targetCenter.x, targetCenter.y, targetCenter.z, 3, 0.0, 0.0, 0.0, 0.0);

            serverWorld.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 2.5f, 0.4f);

            // Drag all entities within 16 blocks
            AABB pullBox = new AABB(targetCenter.x - 16, targetCenter.y - 16, targetCenter.z - 16, targetCenter.x + 16, targetCenter.y + 16, targetCenter.z + 16);
            List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class, pullBox, e -> e != user && e.isAlive());

            for (LivingEntity target : targets) {
                Vec3 pull = targetCenter.subtract(target.position()).normalize().scale(2.2);
                target.setDeltaMovement(pull.x, 0.6, pull.z);
                target.needsSync = true;
                target.hurtServer(serverWorld, serverWorld.damageSources().magic(), 25.0f);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§5✦ Void Singularity Nexus ✦"));
        textConsumer.accept(Component.literal("§7Forged by infusing the Singularity Staff with Primordial Catalysts."));
        textConsumer.accept(Component.literal("§e✦ Right-Click: §5Micro-Black Hole Implosion"));
        textConsumer.accept(Component.literal("§8 • Rips open a gravitational singularity 16 blocks forward"));
        textConsumer.accept(Component.literal("§8 • Drags all entities within 16 blocks inward and implodes for 25 Damage"));
        textConsumer.accept(Component.literal("§8 • Cooldown: 9.0 seconds"));
        textConsumer.accept(Component.literal("§b✦ Planetary Swarm Annihilator."));
    }
}

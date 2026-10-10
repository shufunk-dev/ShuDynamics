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

public class ResonanceCleaverItem extends Item {

    public ResonanceCleaverItem(Properties settings) {
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

        user.getCooldowns().addCooldown(stack, 200); // 10 second cooldown

        if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
            Vec3 origin = user.position();
            Vec3 look = user.getViewVector(1.0f);

            // Ground Shockwave forward 10 blocks
            for (int i = 1; i <= 10; i++) {
                Vec3 point = origin.add(look.scale(i));
                serverWorld.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y + 0.5, point.z, 1, 0, 0, 0, 0);
                serverWorld.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y + 0.5, point.z, 8, 0.4, 0.4, 0.4, 0.05);

                AABB hitBox = new AABB(point.x - 1.5, point.y - 1.0, point.z - 1.5, point.x + 1.5, point.y + 2.0, point.z + 1.5);
                List<LivingEntity> targets = serverWorld.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != user && e.isAlive());

                for (LivingEntity target : targets) {
                    target.hurtServer(serverWorld, serverWorld.damageSources().playerAttack(user), 14.0f);
                    target.push(0, 0.65, 0);
                    target.needsSync = true;
                }
            }

            serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f, 1.2f);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§5✦ Relic of the Resonance Colossus ✦"));
        textConsumer.accept(Component.literal("§7Massive two-handed greatblade infused with dimensional shockwaves."));
        textConsumer.accept(Component.literal("§e✦ Right-Click: §bCataclysmic Ground Slam"));
        textConsumer.accept(Component.literal("§8 • Unleashes an expanding shockwave forward 10 blocks"));
        textConsumer.accept(Component.literal("§8 • Deals 14 damage and launches targets airborne"));
        textConsumer.accept(Component.literal("§8 • Cooldown: 10s"));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}

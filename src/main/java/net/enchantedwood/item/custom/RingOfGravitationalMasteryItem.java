package net.enchantedwood.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class RingOfGravitationalMasteryItem extends Item {

    public RingOfGravitationalMasteryItem(Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public net.minecraft.world.InteractionResult use(Level world, Player user, net.minecraft.world.InteractionHand hand) {
        if (!world.isClientSide()) {
            user.getAbilities().mayfly = true;
            user.onUpdateAbilities();
            world.playSound(null, user.getX(), user.getY(), user.getZ(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 1.4f);
            user.sendOverlayMessage(Component.literal("§d✦ Gravitational Mastery Engaged: Double-tap Jump (Space) to fly!"));
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        if (entity instanceof ServerPlayer player) {
            if (!player.isCreative() && !player.isSpectator()) {
                if (!player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }

                // Flight particles
                if (player.getAbilities().flying && world.getRandom().nextInt(3) == 0) {
                    world.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.2, player.getZ(), 2, 0.2, 0.1, 0.2, 0.02);
                    world.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.1, player.getZ(), 3, 0.2, 0.1, 0.2, 0.05);
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§d✦ Ring of Gravitational Mastery ✦"));
        textConsumer.accept(Component.literal("§7Boundless relic harvested from the core of The Primordial Cataclysm."));
        textConsumer.accept(Component.literal("§e✦ Passive: §bTrue Creative Flight in Survival"));
        textConsumer.accept(Component.literal("§8 • Nullifies all gravity forces & completely eliminates fall damage"));
        textConsumer.accept(Component.literal("§8 • Active while anywhere in player inventory"));
        textConsumer.accept(Component.literal("§5✦ Definitive proof of mastering ShuDynamics."));
    }
}

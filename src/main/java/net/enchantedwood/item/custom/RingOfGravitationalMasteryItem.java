package net.enchantedwood.item.custom;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Consumer;

public class RingOfGravitationalMasteryItem extends Item {

    public RingOfGravitationalMasteryItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @org.jetbrains.annotations.Nullable net.minecraft.entity.EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        if (entity instanceof ServerPlayerEntity player) {
            if (!player.isCreative() && !player.isSpectator()) {
                if (!player.getAbilities().allowFlying) {
                    player.getAbilities().allowFlying = true;
                    player.sendAbilitiesUpdate();
                }

                // Flight particles
                if (player.getAbilities().flying && world.getRandom().nextInt(3) == 0) {
                    world.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.2, player.getZ(), 2, 0.2, 0.1, 0.2, 0.02);
                    world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.1, player.getZ(), 3, 0.2, 0.1, 0.2, 0.05);
                }
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§d✦ Ring of Gravitational Mastery ✦"));
        textConsumer.accept(Text.literal("§7Boundless relic harvested from the core of The Primordial Cataclysm."));
        textConsumer.accept(Text.literal("§e✦ Passive: §bTrue Creative Flight in Survival"));
        textConsumer.accept(Text.literal("§8 • Nullifies all gravity forces & completely eliminates fall damage"));
        textConsumer.accept(Text.literal("§8 • Active while anywhere in player inventory"));
        textConsumer.accept(Text.literal("§5✦ Definitive proof of mastering ShuDynamics."));
    }
}

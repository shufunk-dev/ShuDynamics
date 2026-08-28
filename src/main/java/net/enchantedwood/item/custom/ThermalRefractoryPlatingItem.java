package net.enchantedwood.item.custom;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ThermalRefractoryPlatingItem extends Item {
    public ThermalRefractoryPlatingItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof PlayerEntity player) {
            // Apply infinite Fire Resistance while carried anywhere in inventory
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
            player.extinguish();

            // Lava buoyancy & mobility
            if (player.isInLava()) {
                player.setVelocity(player.getVelocity().x * 1.15, Math.max(player.getVelocity().y, 0.1), player.getVelocity().z * 1.15);
                player.fallDistance = 0.0f;
                if (world.getTime() % 10 == 0) {
                    world.spawnParticles(
                            ParticleTypes.FLAME,
                            player.getX(), player.getY() + 0.1, player.getZ(),
                            2, 0.2, 0.0, 0.2, 0.01
                    );
                }
            }
        }
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§6✦ Nether Thermal Refractory Matrix"));
        textConsumer.accept(Text.literal("§7Grants §ePermanent Fire Resistance §7& §cExtinguish §7while in inventory."));
        textConsumer.accept(Text.literal("§b✦ Lava Buoyancy: §7Enables swift mobility across molten lava."));
        textConsumer.accept(Text.literal("§8100% Fireproof & Lava-Proof module."));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}

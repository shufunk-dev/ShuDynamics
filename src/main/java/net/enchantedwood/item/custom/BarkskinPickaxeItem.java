package net.enchantedwood.item.custom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

public class BarkskinPickaxeItem extends Item {
    public BarkskinPickaxeItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof LivingEntity livingEntity && slot == EquipmentSlot.MAINHAND) {
            if (entity.getBlockPos().getY() < 50) {
                livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 40, 1, false, false, true));
            }
        }
        super.inventoryTick(stack, world, entity, slot);
    }
}



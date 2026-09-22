package net.enchantedwood.mixin;

import net.enchantedwood.item.custom.ModularPowerArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow public abstract Item getItem();
    @Shadow public abstract boolean isDamageable();
    @Shadow public abstract int getMaxDamage();
    @Shadow public abstract void setDamage(int damage);
    @Shadow public abstract int getDamage();
    @Shadow public abstract Text getName();

    @Inject(method = "onDurabilityChange", at = @At("HEAD"), cancellable = true)
    private void enforceEmergencyChassisLock(int damage, ServerPlayerEntity player, Consumer<Item> breakCallback, CallbackInfo ci) {
        if (this.getItem() instanceof ModularPowerArmorItem && this.isDamageable()) {
            int maxDmg = this.getMaxDamage();
            if (damage >= maxDmg) {
                // Lock at maxDamage - 1 (1 HP remaining)
                int lockedDamage = Math.max(0, maxDmg - 1);
                boolean wasAlreadyLocked = this.getDamage() >= lockedDamage;
                this.setDamage(lockedDamage);

                if (player != null && !wasAlreadyLocked) {
                    player.sendMessage(
                            Text.literal("§c§l[EMERGENCY CHASSIS LOCK] §e" + this.getName().getString() + " §7integrity critical! Modules entered safety shutdown."),
                            true
                    );
                    if (player.getEntityWorld() != null) {
                        player.getEntityWorld().playSound(
                                null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.2f, 0.5f
                        );
                        player.getEntityWorld().playSound(
                                null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 1.0f, 1.8f
                        );
                    }
                }
                // Cancel standard durability change so shouldBreak() and decrement(1) are never reached!
                ci.cancel();
            }
        }
    }
}

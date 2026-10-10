package net.enchantedwood.mixin;

import net.enchantedwood.item.custom.ModularPowerArmorItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow public abstract Item getItem();
    @Shadow public abstract boolean isDamageableItem();
    @Shadow public abstract int getMaxDamage();
    @Shadow public abstract void setDamageValue(int damage);
    @Shadow public abstract int getDamageValue();
    @Shadow public abstract Component getHoverName();

    @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true)
    private void enforceEmergencyChassisLock(int damage, ServerPlayer player, Consumer<ItemStack> breakCallback, CallbackInfo ci) {
        if (this.getItem() instanceof ModularPowerArmorItem && this.isDamageableItem()) {
            int maxDmg = this.getMaxDamage();
            if (damage >= maxDmg) {
                // Lock at maxDamage - 1 (1 HP remaining)
                int lockedDamage = Math.max(0, maxDmg - 1);
                boolean wasAlreadyLocked = this.getDamageValue() >= lockedDamage;
                this.setDamageValue(lockedDamage);

                if (player != null && !wasAlreadyLocked) {
                    player.sendOverlayMessage(
                            Component.literal("§c§l[EMERGENCY CHASSIS LOCK] §e" + this.getHoverName().getString() + " §7integrity critical! Modules entered safety shutdown."));
                    if (player.level() != null) {
                        player.level().playSound(
                                null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2f, 0.5f
                        );
                        player.level().playSound(
                                null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 1.0f, 1.8f
                        );
                    }
                }
                // Cancel standard durability change so shouldBreak() and decrement(1) are never reached!
                ci.cancel();
            }
        }
    }
}

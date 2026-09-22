package net.enchantedwood.mixin;

import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClampedEntityAttribute.class)
public abstract class ClampedEntityAttributeMixin extends EntityAttribute {

    protected ClampedEntityAttributeMixin(String translationKey, double fallback) {
        super(translationKey, fallback);
    }

    @Inject(method = "clamp", at = @At("HEAD"), cancellable = true)
    private void unclampMaxHealth(double value, CallbackInfoReturnable<Double> cir) {
        if (this.getTranslationKey().contains("max_health")) {
            cir.setReturnValue(Math.max(1.0, value));
        }
    }

    @Inject(method = "getMaxValue", at = @At("HEAD"), cancellable = true)
    private void uncapGetMaxValue(CallbackInfoReturnable<Double> cir) {
        if (this.getTranslationKey().contains("max_health")) {
            cir.setReturnValue(100000.0);
        }
    }
}

package com.vexira.mysticalterrain.mixin;

import com.vexira.mysticalterrain.boss.ritual.RitualFixtures;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
   @Inject(method = "tick", at = @At("TAIL"))
   private void mysticalterrain$offering(CallbackInfo ci) {
      RitualFixtures.itemTick((ItemEntity)(Object)this);
   }
}

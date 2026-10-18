package com.vexira.mysticalterrain.mixin;

import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EnderDragonFight.class)
public interface EnderDragonFightInvoker {
   @Invoker("updateCrystalCount")
   void mysticalterrain$updateCrystalCount();
}

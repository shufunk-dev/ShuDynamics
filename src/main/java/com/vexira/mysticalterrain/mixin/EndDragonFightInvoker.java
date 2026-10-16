package com.vexira.mysticalterrain.mixin;

import net.minecraft.entity.boss.dragon.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EnderDragonFight.class)
public interface EndDragonFightInvoker {
   @Invoker("countAliveCrystals")
   void mysticalterrain$updateCrystalCount();
}

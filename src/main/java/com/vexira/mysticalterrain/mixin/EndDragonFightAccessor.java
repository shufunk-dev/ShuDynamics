package com.vexira.mysticalterrain.mixin;

import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EnderDragonFight.class)
public interface EndDragonFightAccessor {
   @Invoker("method_12523")
   EnderDragonEntity mysticalterrain$createNewDragon();

   @Invoker("method_12518")
   void mysticalterrain$spawnExitPortal(boolean var1);

   @Accessor("field_13115")
   boolean mysticalterrain$dragonKilled();

   @Accessor("field_13115")
   void mysticalterrain$setDragonKilled(boolean var1);

   @Accessor("field_13117")
   BlockPos mysticalterrain$portalLocation();

   @Accessor("field_44877")
   BlockPos mysticalterrain$origin();

   @Accessor("field_13119")
   ServerBossBar mysticalterrain$dragonEvent();
}

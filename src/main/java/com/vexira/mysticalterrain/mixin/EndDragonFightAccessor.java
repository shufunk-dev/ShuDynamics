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
   @Invoker("createDragon")
   EnderDragonEntity mysticalterrain$createNewDragon();

   @Invoker("generateEndPortal")
   void mysticalterrain$spawnExitPortal(boolean var1);

   @Accessor("dragonKilled")
   boolean mysticalterrain$dragonKilled();

   @Accessor("dragonKilled")
   void mysticalterrain$setDragonKilled(boolean var1);

   @Accessor("exitPortalLocation")
   BlockPos mysticalterrain$portalLocation();

   @Accessor("origin")
   BlockPos mysticalterrain$origin();

   @Accessor("bossBar")
   ServerBossBar mysticalterrain$dragonEvent();
}

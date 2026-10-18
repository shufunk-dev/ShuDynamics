package com.vexira.mysticalterrain.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EnderDragonFight.class)
public interface EnderDragonFightAccessor {
   @Invoker("createNewDragon")
   EnderDragon mysticalterrain$createNewDragon();

   @Invoker("spawnExitPortal")
   void mysticalterrain$spawnExitPortal(boolean var1);

   @Accessor("dragonKilled")
   boolean mysticalterrain$dragonKilled();

   @Accessor("dragonKilled")
   void mysticalterrain$setDragonKilled(boolean var1);

   @Accessor("exitPortalLocation")
   BlockPos mysticalterrain$portalLocation();

   @Accessor("origin")
   BlockPos mysticalterrain$origin();

   @Accessor("dragonEvent")
   ServerBossEvent mysticalterrain$dragonEvent();
}

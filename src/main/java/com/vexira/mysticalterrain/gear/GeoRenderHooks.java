package com.vexira.mysticalterrain.gear;

import java.util.function.Function;
import net.minecraft.item.Item;

public final class GeoRenderHooks {
   public static Function<Item, Object> weapons = item -> null;
   public static Function<Item, Object> armor = item -> null;

   private GeoRenderHooks() {
   }
}

package com.vexira.mysticalterrain.gear;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public final class ClientView {
   public static ToIntFunction<String> cooldown = key -> 0;
   public static Supplier<@Nullable Player> player = () -> null;

   private ClientView() {
   }
}

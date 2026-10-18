package com.vexira.mysticalterrain.client.boss;

import com.vexira.mysticalterrain.boss.net.BossStatePayload;
import com.vexira.mysticalterrain.client.boss.bar.BarAnim;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class BossBarState {
   private static final Map<UUID, BarAnim> BARS = new HashMap<>();
   private static int ticks;

   private BossBarState() {
   }

   public static void init() {
      ClientPlayNetworking.registerGlobalReceiver(BossStatePayload.TYPE, (payload, context) -> bar(payload.bar()).accept(payload, now()));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         if (!client.isPaused()) {
            ticks++;
         }
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> client.execute(BossBarState::clear));
   }

   public static float now() {
      return ticks + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
   }

   public static BarAnim bar(UUID id) {
      BarAnim anim = BARS.get(id);
      if (anim == null) {
         anim = new BarAnim();
         BARS.put(id, anim);
      }

      return anim;
   }

   public static Set<UUID> ids() {
      return Set.copyOf(BARS.keySet());
   }

   public static @Nullable BossStatePayload state(UUID id) {
      BarAnim anim = BARS.get(id);
      return anim != null ? anim.state : null;
   }

   public static void retain(Set<UUID> live) {
      if (!BARS.isEmpty() && (BARS.size() > live.size() || !live.containsAll(BARS.keySet()))) {
         BARS.keySet().retainAll(live);
      }
   }

   public static void forget(UUID id) {
      BARS.remove(id);
   }

   public static void clear() {
      BARS.clear();
   }
}

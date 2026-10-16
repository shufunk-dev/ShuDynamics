package com.vexira.mysticalterrain.boss.net;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jspecify.annotations.Nullable;

public final class BossNet {
   private BossNet() {
   }

   public static void register() {
      PayloadTypeRegistry.playS2C().register(BossStatePayload.TYPE, BossStatePayload.STREAM_CODEC);
   }

   public static void send(ServerPlayerEntity player, BossStatePayload payload) {
      if (ServerPlayNetworking.canSend(player, BossStatePayload.TYPE)) {
         ServerPlayNetworking.send(player, payload);
      }
   }

   public static void sendAll(ServerBossBar bar, BossStatePayload payload, @Nullable UUID marked) {
      for (ServerPlayerEntity p : bar.getPlayers()) {
         send(p, p.getUuid().equals(marked) ? payload.withFlags(payload.flags() | 8) : payload);
      }
   }

   public static final class Sync {
      private @Nullable BossStatePayload last;
      private @Nullable UUID lastMarked;
      private int lastTick = -1073741824;
      private final Set<UUID> seen = new HashSet<>();

      public void tick(int now, ServerBossBar bar, BossStatePayload state, @Nullable UUID marked) {
         Collection<ServerPlayerEntity> players = bar.getPlayers();
         if (players.isEmpty()) {
            this.seen.clear();
            this.last = null;
         } else {
            boolean joined = false;

            for (ServerPlayerEntity p : players) {
               if (!this.seen.contains(p.getUuid())) {
                  joined = true;
                  break;
               }
            }

            int since = now - this.lastTick;
            if (since < 0) {
               since = Integer.MAX_VALUE;
            }

            boolean changed = !state.equals(this.last) || !Objects.equals(marked, this.lastMarked);
            if (joined || changed && since >= 2 || since >= 20) {
               this.seen.clear();

               for (ServerPlayerEntity p : players) {
                  this.seen.add(p.getUuid());
               }

               BossNet.sendAll(bar, state, marked);
               this.last = state;
               this.lastMarked = marked;
               this.lastTick = now;
            }
         }
      }

      public void reset() {
         this.last = null;
         this.seen.clear();
      }
   }
}

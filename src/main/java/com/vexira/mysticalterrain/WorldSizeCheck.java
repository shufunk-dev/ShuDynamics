package com.vexira.mysticalterrain;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents.Configure;

public record WorldSizeCheck() implements CustomPacketPayload {
   public static final Type<WorldSizeCheck> TYPE = new Type(MysticalTerrain.id("world_size"));
   private static final StreamCodec<FriendlyByteBuf, WorldSizeCheck> CODEC = StreamCodec.unit(new WorldSizeCheck());

   public Type<WorldSizeCheck> type() {
      return TYPE;
   }

   static void register() {
      PayloadTypeRegistry.clientboundConfiguration().register(TYPE, CODEC);
      PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
      ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {});
      ServerConfigurationConnectionEvents.CONFIGURE.register((Configure)(handler, server) -> {
         if (!ServerConfigurationNetworking.canSend(handler, TYPE)) {
            handler.disconnect(Component.literal("This server uses Mystical Terrain's bigger world. Install Mystical Terrain to join."));
         }
      });
   }
}

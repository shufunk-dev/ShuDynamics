package com.vexira.mysticalterrain;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents.Configure;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.text.Text;

public record WorldSizeCheck() implements CustomPayload {
   public static final Id<WorldSizeCheck> TYPE = new Id(MysticalTerrain.id("world_size"));
   private static final PacketCodec<PacketByteBuf, WorldSizeCheck> CODEC = PacketCodec.unit(new WorldSizeCheck());

   public Id<WorldSizeCheck> getId() {
      return TYPE;
   }

   static void register() {
      PayloadTypeRegistry.configurationS2C().register(TYPE, CODEC);
      PayloadTypeRegistry.playC2S().register(TYPE, CODEC);
      ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {});
      ServerConfigurationConnectionEvents.CONFIGURE.register((Configure)(handler, server) -> {
         if (!ServerConfigurationNetworking.canSend(handler, TYPE)) {
            handler.disconnect(Text.literal("This server uses Mystical Terrain's bigger world. Install Mystical Terrain to join."));
         }
      });
   }
}

package com.vexira.mysticalterrain.gear;

import com.vexira.mysticalterrain.MysticalTerrain;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;

public record SetAbilityPayload() implements CustomPayload {
   public static final SetAbilityPayload INSTANCE = new SetAbilityPayload();
   public static final Id<SetAbilityPayload> TYPE = new Id(MysticalTerrain.id("gear_set_ability"));
   public static final PacketCodec<ByteBuf, SetAbilityPayload> CODEC = PacketCodec.unit(INSTANCE);

   public Id<? extends CustomPayload> getId() {
      return TYPE;
   }
}

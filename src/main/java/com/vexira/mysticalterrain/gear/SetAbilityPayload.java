package com.vexira.mysticalterrain.gear;

import com.vexira.mysticalterrain.MysticalTerrain;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetAbilityPayload() implements CustomPacketPayload {
   public static final SetAbilityPayload INSTANCE = new SetAbilityPayload();
   public static final Type<SetAbilityPayload> TYPE = new Type(MysticalTerrain.id("gear_set_ability"));
   public static final StreamCodec<ByteBuf, SetAbilityPayload> CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}

package com.vexira.mysticalterrain.boss.vfx;

import com.vexira.mysticalterrain.MysticalTerrain;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FxPayload(float shake, int shakeTicks, int flashColor, float flash, int style) implements CustomPacketPayload {
   public static final int FLAT = 0;
   public static final int RADIAL = 1;
   public static final int IMPACT_FRAME = 2;
   public static final Type<FxPayload> TYPE = new Type(MysticalTerrain.id("fx"));
   public static final StreamCodec<ByteBuf, FxPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT,
      FxPayload::shake,
      ByteBufCodecs.VAR_INT,
      FxPayload::shakeTicks,
      ByteBufCodecs.INT,
      FxPayload::flashColor,
      ByteBufCodecs.FLOAT,
      FxPayload::flash,
      ByteBufCodecs.VAR_INT,
      FxPayload::style,
      FxPayload::new
   );

   public FxPayload(float shake, int shakeTicks, int flashColor, float flash) {
      this(shake, shakeTicks, flashColor, flash, 0);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}

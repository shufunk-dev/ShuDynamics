package com.vexira.mysticalterrain.boss.vfx;

import com.vexira.mysticalterrain.MysticalTerrain;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;

public record FxPayload(float shake, int shakeTicks, int flashColor, float flash, int style) implements CustomPayload {
   public static final int FLAT = 0;
   public static final int RADIAL = 1;
   public static final int IMPACT_FRAME = 2;
   public static final Id<FxPayload> TYPE = new Id(MysticalTerrain.id("fx"));
   public static final PacketCodec<ByteBuf, FxPayload> CODEC = PacketCodec.tuple(
      PacketCodecs.FLOAT,
      FxPayload::shake,
      PacketCodecs.VAR_INT,
      FxPayload::shakeTicks,
      PacketCodecs.INTEGER,
      FxPayload::flashColor,
      PacketCodecs.FLOAT,
      FxPayload::flash,
      PacketCodecs.VAR_INT,
      FxPayload::style,
      FxPayload::new
   );

   public FxPayload(float shake, int shakeTicks, int flashColor, float flash) {
      this(shake, shakeTicks, flashColor, flash, 0);
   }

   public Id<? extends CustomPayload> getId() {
      return TYPE;
   }
}

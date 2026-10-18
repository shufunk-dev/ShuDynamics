package com.vexira.mysticalterrain.gear;

import com.vexira.mysticalterrain.MysticalTerrain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record GearSyncPayload(List<GearSyncPayload.Cooldown> cooldowns, Map<String, Float> resonance) implements CustomPacketPayload {
   public static final Type<GearSyncPayload> TYPE = new Type(MysticalTerrain.id("gear_sync"));
   public static final StreamCodec<FriendlyByteBuf, GearSyncPayload> CODEC = CustomPacketPayload.codec(GearSyncPayload::write, GearSyncPayload::read);

   private void write(FriendlyByteBuf buf) {
      buf.writeVarInt(this.cooldowns.size());

      for (GearSyncPayload.Cooldown cd : this.cooldowns) {
         buf.writeUtf(cd.key());
         buf.writeVarInt(cd.remaining());
         buf.writeVarInt(cd.total());
      }

      buf.writeVarInt(this.resonance.size());
      this.resonance.forEach((weapon, value) -> {
         buf.writeUtf(weapon);
         buf.writeFloat(value);
      });
   }

   private static GearSyncPayload read(FriendlyByteBuf buf) {
      int n = buf.readVarInt();
      List<GearSyncPayload.Cooldown> cds = new ArrayList<>(n);

      for (int i = 0; i < n; i++) {
         cds.add(new GearSyncPayload.Cooldown(buf.readUtf(), buf.readVarInt(), buf.readVarInt()));
      }

      int m = buf.readVarInt();
      Map<String, Float> res = new HashMap<>();

      for (int i = 0; i < m; i++) {
         res.put(buf.readUtf(), buf.readFloat());
      }

      return new GearSyncPayload(cds, res);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record Cooldown(String key, int remaining, int total) {
   }
}

package com.vexira.mysticalterrain.gear;

import com.vexira.mysticalterrain.MysticalTerrain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;

public record GearSyncPayload(List<GearSyncPayload.Cooldown> cooldowns, Map<String, Float> resonance) implements CustomPayload {
   public static final Id<GearSyncPayload> TYPE = new Id(MysticalTerrain.id("gear_sync"));
   public static final PacketCodec<PacketByteBuf, GearSyncPayload> CODEC = CustomPayload.codecOf(GearSyncPayload::write, GearSyncPayload::read);

   private void write(PacketByteBuf buf) {
      buf.writeVarInt(this.cooldowns.size());

      for (GearSyncPayload.Cooldown cd : this.cooldowns) {
         buf.writeString(cd.key());
         buf.writeVarInt(cd.remaining());
         buf.writeVarInt(cd.total());
      }

      buf.writeVarInt(this.resonance.size());
      this.resonance.forEach((weapon, value) -> {
         buf.writeString(weapon);
         buf.writeFloat(value);
      });
   }

   private static GearSyncPayload read(PacketByteBuf buf) {
      int n = buf.readVarInt();
      List<GearSyncPayload.Cooldown> cds = new ArrayList<>(n);

      for (int i = 0; i < n; i++) {
         cds.add(new GearSyncPayload.Cooldown(buf.readString(), buf.readVarInt(), buf.readVarInt()));
      }

      int m = buf.readVarInt();
      Map<String, Float> res = new HashMap<>();

      for (int i = 0; i < m; i++) {
         res.put(buf.readString(), buf.readFloat());
      }

      return new GearSyncPayload(cds, res);
   }

   public Id<? extends CustomPayload> getId() {
      return TYPE;
   }

   public record Cooldown(String key, int remaining, int total) {
   }
}

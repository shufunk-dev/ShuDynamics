package com.vexira.mysticalterrain.boss.net;

import com.vexira.mysticalterrain.MysticalTerrain;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.util.Uuids;

public record BossStatePayload(
   UUID bar,
   String boss,
   int level,
   float hpScale,
   float health,
   float maxHealth,
   int phase,
   List<Float> thresholds,
   float shield,
   float shieldMax,
   float poise,
   boolean staggered,
   String cast,
   int castTick,
   int castLength,
   int flags
) implements CustomPayload {
   public static final Id<BossStatePayload> TYPE = new Id(MysticalTerrain.id("boss_state"));
   public static final PacketCodec<ByteBuf, BossStatePayload> STREAM_CODEC = PacketCodec.of(BossStatePayload::write, BossStatePayload::read);
   public static final int FLAG_ENRAGED = 1;
   public static final int FLAG_INVULNERABLE = 2;
   public static final int FLAG_BUFFED = 4;
   public static final int FLAG_MARKED = 8;
   public static final int FLAG_TELEGRAPH = 16;
   private static final int MAX_THRESHOLDS = 16;

   public BossStatePayload {
      thresholds = List.copyOf(thresholds);
   }

   public static BossStatePayload simple(UUID bar, String boss, int level, float health, float maxHealth, int phase, List<Float> thresholds, int flags) {
      return new BossStatePayload(bar, boss, level, 1.0F, health, maxHealth, phase, thresholds, 0.0F, 0.0F, -1.0F, false, "", 0, 0, flags);
   }

   public static BossStatePayload simple(
      UUID bar, String boss, int level, float health, float maxHealth, int phase, List<Float> thresholds, String cast, int castTick, int castLength, int flags
   ) {
      return new BossStatePayload(bar, boss, level, 1.0F, health, maxHealth, phase, thresholds, 0.0F, 0.0F, -1.0F, false, cast, castTick, castLength, flags);
   }

   public static List<Float> thresholds(float... fractions) {
      List<Float> out = new ArrayList<>(fractions.length);

      for (float f : fractions) {
         out.add(f);
      }

      return out;
   }

   public BossStatePayload withFlags(int newFlags) {
      return new BossStatePayload(
         this.bar,
         this.boss,
         this.level,
         this.hpScale,
         this.health,
         this.maxHealth,
         this.phase,
         this.thresholds,
         this.shield,
         this.shieldMax,
         this.poise,
         this.staggered,
         this.cast,
         this.castTick,
         this.castLength,
         newFlags
      );
   }

   public boolean has(int flag) {
      return (this.flags & flag) != 0;
   }

   private void write(ByteBuf buf) {
      Uuids.PACKET_CODEC.encode(buf, this.bar);
      PacketCodecs.STRING.encode(buf, this.boss);
      PacketCodecs.VAR_INT.encode(buf, this.level);
      buf.writeFloat(this.hpScale);
      buf.writeFloat(this.health);
      buf.writeFloat(this.maxHealth);
      PacketCodecs.VAR_INT.encode(buf, this.phase);
      int n = Math.min(16, this.thresholds.size());
      PacketCodecs.VAR_INT.encode(buf, n);

      for (int i = 0; i < n; i++) {
         buf.writeFloat(this.thresholds.get(i));
      }

      buf.writeFloat(this.shield);
      buf.writeFloat(this.shieldMax);
      buf.writeFloat(this.poise);
      buf.writeBoolean(this.staggered);
      PacketCodecs.STRING.encode(buf, this.cast);
      PacketCodecs.VAR_INT.encode(buf, this.castTick);
      PacketCodecs.VAR_INT.encode(buf, this.castLength);
      PacketCodecs.VAR_INT.encode(buf, this.flags);
   }

   private static BossStatePayload read(ByteBuf buf) {
      UUID bar = (UUID)Uuids.PACKET_CODEC.decode(buf);
      String boss = (String)PacketCodecs.STRING.decode(buf);
      int level = (Integer)PacketCodecs.VAR_INT.decode(buf);
      float hpScale = buf.readFloat();
      float health = buf.readFloat();
      float maxHealth = buf.readFloat();
      int phase = (Integer)PacketCodecs.VAR_INT.decode(buf);
      int n = Math.min(16, Math.max(0, (Integer)PacketCodecs.VAR_INT.decode(buf)));
      List<Float> thresholds = new ArrayList<>(n);

      for (int i = 0; i < n; i++) {
         thresholds.add(buf.readFloat());
      }

      float shield = buf.readFloat();
      float shieldMax = buf.readFloat();
      float poise = buf.readFloat();
      boolean staggered = buf.readBoolean();
      String cast = (String)PacketCodecs.STRING.decode(buf);
      int castTick = (Integer)PacketCodecs.VAR_INT.decode(buf);
      int castLength = (Integer)PacketCodecs.VAR_INT.decode(buf);
      int flags = (Integer)PacketCodecs.VAR_INT.decode(buf);
      return new BossStatePayload(
         bar, boss, level, hpScale, health, maxHealth, phase, thresholds, shield, shieldMax, poise, staggered, cast, castTick, castLength, flags
      );
   }

   public Id<? extends CustomPayload> getId() {
      return TYPE;
   }
}

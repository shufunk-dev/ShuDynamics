package com.vexira.mysticalterrain.boss.net;

import com.vexira.mysticalterrain.MysticalTerrain;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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
) implements CustomPacketPayload {
   public static final Type<BossStatePayload> TYPE = new Type(MysticalTerrain.id("boss_state"));
   public static final StreamCodec<ByteBuf, BossStatePayload> STREAM_CODEC = StreamCodec.ofMember(BossStatePayload::write, BossStatePayload::read);
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
      UUIDUtil.STREAM_CODEC.encode(buf, this.bar);
      ByteBufCodecs.STRING_UTF8.encode(buf, this.boss);
      ByteBufCodecs.VAR_INT.encode(buf, this.level);
      buf.writeFloat(this.hpScale);
      buf.writeFloat(this.health);
      buf.writeFloat(this.maxHealth);
      ByteBufCodecs.VAR_INT.encode(buf, this.phase);
      int n = Math.min(16, this.thresholds.size());
      ByteBufCodecs.VAR_INT.encode(buf, n);

      for (int i = 0; i < n; i++) {
         buf.writeFloat(this.thresholds.get(i));
      }

      buf.writeFloat(this.shield);
      buf.writeFloat(this.shieldMax);
      buf.writeFloat(this.poise);
      buf.writeBoolean(this.staggered);
      ByteBufCodecs.STRING_UTF8.encode(buf, this.cast);
      ByteBufCodecs.VAR_INT.encode(buf, this.castTick);
      ByteBufCodecs.VAR_INT.encode(buf, this.castLength);
      ByteBufCodecs.VAR_INT.encode(buf, this.flags);
   }

   private static BossStatePayload read(ByteBuf buf) {
      UUID bar = (UUID)UUIDUtil.STREAM_CODEC.decode(buf);
      String boss = (String)ByteBufCodecs.STRING_UTF8.decode(buf);
      int level = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
      float hpScale = buf.readFloat();
      float health = buf.readFloat();
      float maxHealth = buf.readFloat();
      int phase = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
      int n = Math.min(16, Math.max(0, (Integer)ByteBufCodecs.VAR_INT.decode(buf)));
      List<Float> thresholds = new ArrayList<>(n);

      for (int i = 0; i < n; i++) {
         thresholds.add(buf.readFloat());
      }

      float shield = buf.readFloat();
      float shieldMax = buf.readFloat();
      float poise = buf.readFloat();
      boolean staggered = buf.readBoolean();
      String cast = (String)ByteBufCodecs.STRING_UTF8.decode(buf);
      int castTick = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
      int castLength = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
      int flags = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
      return new BossStatePayload(
         bar, boss, level, hpScale, health, maxHealth, phase, thresholds, shield, shieldMax, poise, staggered, cast, castTick, castLength, flags
      );
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}

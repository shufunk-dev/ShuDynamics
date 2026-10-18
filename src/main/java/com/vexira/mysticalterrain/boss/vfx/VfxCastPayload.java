package com.vexira.mysticalterrain.boss.vfx;

import com.vexira.mysticalterrain.MysticalTerrain;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record VfxCastPayload(VfxCastPayload.Effect effect, List<Vec3> points, float[] args, int core, int glow, int smoke, int debris, int seed)
   implements CustomPacketPayload {
   public static final int MAX_POINTS = 32;
   public static final int MAX_ARGS = 16;
   public static final Type<VfxCastPayload> TYPE = new Type(MysticalTerrain.id("vfx_cast"));
   public static final StreamCodec<ByteBuf, VfxCastPayload> CODEC = StreamCodec.of(VfxCastPayload::write, VfxCastPayload::read);

   public Vec3 point(int i) {
      return i < this.points.size() ? this.points.get(i) : (this.points.isEmpty() ? Vec3.ZERO : this.points.get(0));
   }

   public float arg(int i) {
      return i < this.args.length ? this.args[i] : 0.0F;
   }

   private static void write(ByteBuf buf, VfxCastPayload p) {
      ByteBufCodecs.VAR_INT.encode(buf, p.effect.ordinal());
      int n = Math.min(32, p.points.size());
      ByteBufCodecs.VAR_INT.encode(buf, n);
      Vec3 o = n > 0 ? p.points.get(0) : Vec3.ZERO;

      for (int i = 0; i < n; i++) {
         Vec3 v = p.points.get(i);
         if (i == 0) {
            buf.writeDouble(v.x);
            buf.writeDouble(v.y);
            buf.writeDouble(v.z);
         } else {
            buf.writeFloat((float)(v.x - o.x));
            buf.writeFloat((float)(v.y - o.y));
            buf.writeFloat((float)(v.z - o.z));
         }
      }

      int a = Math.min(16, p.args.length);
      ByteBufCodecs.VAR_INT.encode(buf, a);

      for (int i = 0; i < a; i++) {
         buf.writeFloat(p.args[i]);
      }

      buf.writeInt(p.core);
      buf.writeInt(p.glow);
      buf.writeInt(p.smoke);
      buf.writeByte(p.debris);
      buf.writeInt(p.seed);
   }

   private static VfxCastPayload read(ByteBuf buf) {
      VfxCastPayload.Effect[] all = VfxCastPayload.Effect.values();
      int e = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
      VfxCastPayload.Effect effect = all[Math.floorMod(e, all.length)];
      int n = Math.min(32, (Integer)ByteBufCodecs.VAR_INT.decode(buf));
      List<Vec3> points = new ArrayList<>(n);
      Vec3 o = Vec3.ZERO;

      for (int i = 0; i < n; i++) {
         if (i == 0) {
            o = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            points.add(o);
         } else {
            points.add(o.add(buf.readFloat(), buf.readFloat(), buf.readFloat()));
         }
      }

      int a = Math.min(16, (Integer)ByteBufCodecs.VAR_INT.decode(buf));
      float[] args = new float[a];

      for (int i = 0; i < a; i++) {
         args[i] = buf.readFloat();
      }

      return new VfxCastPayload(effect, points, args, buf.readInt(), buf.readInt(), buf.readInt(), buf.readByte(), buf.readInt());
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public enum Effect {
      SPARKS,
      EMBERS,
      SMOKE,
      SHARDS,
      IMPACT,
      SHOCKWAVE,
      BEAM,
      GATHER,
      PILLAR,
      EXPLOSION,
      AURA,
      TRAIL,
      SWING,
      TELEGRAPH_CIRCLE,
      TELEGRAPH_LINE,
      TELEGRAPH_CONE,
      LIGHTNING,
      CHAIN_LIGHTNING,
      VORTEX,
      NOVA,
      RUNE_CIRCLE,
      CHARGE_UP,
      METEOR_TRAIL,
      GROUND_CRACK,
      HEAVY_IMPACT,
      SHIELD_BUBBLE,
      WARD,
      DASH_TRAIL,
      AURA_RING,
      WISPS,
      SLASH_ARC,
      CLAW,
      DEBRIS,
      GLINT,
      STREAKS;
   }
}

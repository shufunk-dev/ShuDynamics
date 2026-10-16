package com.vexira.mysticalterrain.boss.vfx;

import com.vexira.mysticalterrain.MysticalTerrain;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.util.math.Vec3d;

public record VfxCastPayload(VfxCastPayload.Effect effect, List<Vec3d> points, float[] args, int core, int glow, int smoke, int debris, int seed)
   implements CustomPayload {
   public static final int MAX_POINTS = 32;
   public static final int MAX_ARGS = 16;
   public static final Id<VfxCastPayload> TYPE = new Id(MysticalTerrain.id("vfx_cast"));
   public static final PacketCodec<ByteBuf, VfxCastPayload> CODEC = PacketCodec.ofStatic(VfxCastPayload::write, VfxCastPayload::read);

   public Vec3d point(int i) {
      return i < this.points.size() ? this.points.get(i) : (this.points.isEmpty() ? Vec3d.ZERO : this.points.get(0));
   }

   public float arg(int i) {
      return i < this.args.length ? this.args[i] : 0.0F;
   }

   private static void write(ByteBuf buf, VfxCastPayload p) {
      PacketCodecs.VAR_INT.encode(buf, p.effect.ordinal());
      int n = Math.min(32, p.points.size());
      PacketCodecs.VAR_INT.encode(buf, n);
      Vec3d o = n > 0 ? p.points.get(0) : Vec3d.ZERO;

      for (int i = 0; i < n; i++) {
         Vec3d v = p.points.get(i);
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
      PacketCodecs.VAR_INT.encode(buf, a);

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
      int e = (Integer)PacketCodecs.VAR_INT.decode(buf);
      VfxCastPayload.Effect effect = all[Math.floorMod(e, all.length)];
      int n = Math.min(32, (Integer)PacketCodecs.VAR_INT.decode(buf));
      List<Vec3d> points = new ArrayList<>(n);
      Vec3d o = Vec3d.ZERO;

      for (int i = 0; i < n; i++) {
         if (i == 0) {
            o = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
            points.add(o);
         } else {
            points.add(o.add(buf.readFloat(), buf.readFloat(), buf.readFloat()));
         }
      }

      int a = Math.min(16, (Integer)PacketCodecs.VAR_INT.decode(buf));
      float[] args = new float[a];

      for (int i = 0; i < a; i++) {
         args[i] = buf.readFloat();
      }

      return new VfxCastPayload(effect, points, args, buf.readInt(), buf.readInt(), buf.readInt(), buf.readByte(), buf.readInt());
   }

   public Id<? extends CustomPayload> getId() {
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

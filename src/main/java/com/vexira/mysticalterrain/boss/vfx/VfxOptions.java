package com.vexira.mysticalterrain.boss.vfx;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record VfxOptions(
   ParticleType<VfxOptions> type,
   int color,
   float scale,
   int life,
   float yaw,
   float pitch,
   float roll,
   int color2,
   int variant,
   int flags,
   float spin,
   float gravity,
   float drag,
   float stretch,
   float curl,
   int delay
) implements ParticleOptions {
   public static final int AUTO = 0;
   public static final int FACE = 1;
   public static final int FLAT = 2;
   public static final int FACING = 3;
   public static final int PLANE = 4;
   public static final int STREAK = 5;
   public static final int SPAN = 6;
   public static final int UPRIGHT = 7;
   public static final int CURVE_KIND = 0;
   public static final int GROW = 1;
   public static final int SHRINK = 2;
   public static final int PULSE = 3;
   public static final int POP = 4;
   public static final int CONST = 5;
   public static final int EXPAND = 6;
   public static final int LINEAR = 7;
   public static final int FADE_KIND = 0;
   public static final int FADE_OUT = 1;
   public static final int FADE_LATE = 2;
   public static final int FADE_IN_OUT = 3;
   public static final int FADE_FLICKER = 4;
   public static final int FADE_HOLD = 5;
   public static final int FADE_NONE = 6;
   public static final int COLLIDE = 512;
   public static final int MIRROR = 1024;
   public static final int LIE = 2048;

   public VfxOptions(ParticleType<VfxOptions> type, int color, float scale, int life, float yaw) {
      this(type, color, scale, life, yaw, 0.0F, Float.NaN, -1, -1, 0, 0.0F, Float.NaN, Float.NaN, 0.0F, 0.0F, 0);
   }

   public int mode() {
      return this.flags & 7;
   }

   public int curve() {
      return this.flags >> 3 & 7;
   }

   public int fade() {
      return this.flags >> 6 & 7;
   }

   public boolean has(int flag) {
      return (this.flags & flag) != 0;
   }

   public static VfxOptions.Builder of(VfxParticles.Kind kind, int color, double size, int life) {
      return new VfxOptions.Builder(VfxParticles.type(kind), color, (float)size, life);
   }

   public static MapCodec<VfxOptions> codec(ParticleType<VfxOptions> type) {
      return RecordCodecBuilder.mapCodec(
         i -> i.group(
               Codec.INT.fieldOf("color").forGetter(VfxOptions::color),
               Codec.FLOAT.fieldOf("scale").forGetter(VfxOptions::scale),
               Codec.INT.fieldOf("life").forGetter(VfxOptions::life),
               Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(VfxOptions::yaw),
               Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(VfxOptions::pitch),
               Codec.FLOAT.optionalFieldOf("roll", Float.NaN).forGetter(VfxOptions::roll),
               Codec.INT.optionalFieldOf("color2", -1).forGetter(VfxOptions::color2),
               Codec.INT.optionalFieldOf("variant", -1).forGetter(VfxOptions::variant),
               Codec.INT.optionalFieldOf("flags", 0).forGetter(VfxOptions::flags),
               Codec.FLOAT.optionalFieldOf("spin", 0.0F).forGetter(VfxOptions::spin),
               Codec.FLOAT.optionalFieldOf("gravity", Float.NaN).forGetter(VfxOptions::gravity),
               Codec.FLOAT.optionalFieldOf("drag", Float.NaN).forGetter(VfxOptions::drag),
               Codec.FLOAT.optionalFieldOf("stretch", 0.0F).forGetter(VfxOptions::stretch),
               Codec.FLOAT.optionalFieldOf("curl", 0.0F).forGetter(VfxOptions::curl),
               Codec.INT.optionalFieldOf("delay", 0).forGetter(VfxOptions::delay)
            )
            .apply(i, (c, s, l, y, p, r, c2, v, f, sp, g, d, st, cu, de) -> new VfxOptions(type, c, s, l, y, p, r, c2, v, f, sp, g, d, st, cu, de))
      );
   }

   public static StreamCodec<ByteBuf, VfxOptions> streamCodec(ParticleType<VfxOptions> type) {
      return StreamCodec.of((buf, o) -> o.write(buf), buf -> read(type, buf));
   }

   private void write(ByteBuf buf) {
      buf.writeInt(this.color);
      buf.writeFloat(this.scale);
      ByteBufCodecs.VAR_INT.encode(buf, this.life);
      buf.writeFloat(this.yaw);
      int extra = (this.pitch != 0.0F ? 1 : 0)
         | (!Float.isNaN(this.roll) ? 2 : 0)
         | (this.color2 != -1 ? 4 : 0)
         | (this.variant >= 0 ? 8 : 0)
         | (this.spin != 0.0F ? 16 : 0)
         | (!Float.isNaN(this.gravity) ? 32 : 0)
         | (!Float.isNaN(this.drag) ? 64 : 0)
         | (this.stretch != 0.0F ? 128 : 0)
         | (this.curl != 0.0F ? 256 : 0)
         | (this.delay != 0 ? 512 : 0);
      ByteBufCodecs.VAR_INT.encode(buf, this.flags);
      ByteBufCodecs.VAR_INT.encode(buf, extra);
      if ((extra & 1) != 0) {
         buf.writeFloat(this.pitch);
      }

      if ((extra & 2) != 0) {
         buf.writeFloat(this.roll);
      }

      if ((extra & 4) != 0) {
         buf.writeInt(this.color2);
      }

      if ((extra & 8) != 0) {
         ByteBufCodecs.VAR_INT.encode(buf, this.variant);
      }

      if ((extra & 16) != 0) {
         buf.writeFloat(this.spin);
      }

      if ((extra & 32) != 0) {
         buf.writeFloat(this.gravity);
      }

      if ((extra & 64) != 0) {
         buf.writeFloat(this.drag);
      }

      if ((extra & 128) != 0) {
         buf.writeFloat(this.stretch);
      }

      if ((extra & 256) != 0) {
         buf.writeFloat(this.curl);
      }

      if ((extra & 512) != 0) {
         ByteBufCodecs.VAR_INT.encode(buf, this.delay);
      }
   }

   private static VfxOptions read(ParticleType<VfxOptions> type, ByteBuf buf) {
      int color = buf.readInt();
      float scale = buf.readFloat();
      int life = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
      float yaw = buf.readFloat();
      int flags = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
      int extra = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
      float pitch = (extra & 1) != 0 ? buf.readFloat() : 0.0F;
      float roll = (extra & 2) != 0 ? buf.readFloat() : Float.NaN;
      int color2 = (extra & 4) != 0 ? buf.readInt() : -1;
      int variant = (extra & 8) != 0 ? (Integer)ByteBufCodecs.VAR_INT.decode(buf) : -1;
      float spin = (extra & 16) != 0 ? buf.readFloat() : 0.0F;
      float gravity = (extra & 32) != 0 ? buf.readFloat() : Float.NaN;
      float drag = (extra & 64) != 0 ? buf.readFloat() : Float.NaN;
      float stretch = (extra & 128) != 0 ? buf.readFloat() : 0.0F;
      float curl = (extra & 256) != 0 ? buf.readFloat() : 0.0F;
      int delay = (extra & 512) != 0 ? (Integer)ByteBufCodecs.VAR_INT.decode(buf) : 0;
      return new VfxOptions(type, color, scale, life, yaw, pitch, roll, color2, variant, flags, spin, gravity, drag, stretch, curl, delay);
   }

   public ParticleType<VfxOptions> getType() {
      return this.type;
   }

   public static final class Builder {
      private final ParticleType<VfxOptions> type;
      private final int color;
      private final float scale;
      private final int life;
      private float yaw;
      private float pitch;
      private float roll = Float.NaN;
      private int color2 = -1;
      private int variant = -1;
      private int flags;
      private float spin;
      private float gravity = Float.NaN;
      private float drag = Float.NaN;
      private float stretch;
      private float curl;
      private int delay;

      private Builder(ParticleType<VfxOptions> type, int color, float scale, int life) {
         this.type = type;
         this.color = color;
         this.scale = scale;
         this.life = life;
      }

      public VfxOptions.Builder mode(int mode) {
         this.flags = this.flags & -8 | mode & 7;
         return this;
      }

      public VfxOptions.Builder face() {
         return this.mode(1);
      }

      public VfxOptions.Builder flat() {
         return this.mode(2);
      }

      public VfxOptions.Builder flat(float yaw) {
         this.yaw = yaw;
         this.roll = 0.0F;
         return this.mode(2);
      }

      public VfxOptions.Builder facing(float yaw, float pitch) {
         this.yaw = yaw;
         this.pitch = pitch;
         return this.mode(3);
      }

      public VfxOptions.Builder plane(float yaw, float pitch, float roll) {
         this.yaw = yaw;
         this.pitch = pitch;
         this.roll = roll;
         return this.mode(4);
      }

      public VfxOptions.Builder streak(float stretch) {
         this.stretch = stretch;
         return this.mode(5);
      }

      public VfxOptions.Builder span() {
         return this.mode(6);
      }

      public VfxOptions.Builder lie() {
         this.flags |= 2048;
         return this.mode(6);
      }

      public VfxOptions.Builder upright() {
         return this.mode(7);
      }

      public VfxOptions.Builder curve(int curve) {
         this.flags = this.flags & -57 | (curve & 7) << 3;
         return this;
      }

      public VfxOptions.Builder fade(int fade) {
         this.flags = this.flags & -449 | (fade & 7) << 6;
         return this;
      }

      public VfxOptions.Builder yaw(float yaw) {
         this.yaw = yaw;
         return this;
      }

      public VfxOptions.Builder pitch(float pitch) {
         this.pitch = pitch;
         return this;
      }

      public VfxOptions.Builder roll(float degrees) {
         this.roll = degrees;
         return this;
      }

      public VfxOptions.Builder to(int color2) {
         this.color2 = color2 & 16777215;
         return this;
      }

      public VfxOptions.Builder variant(int variant) {
         this.variant = variant;
         return this;
      }

      public VfxOptions.Builder spin(float radPerTick) {
         this.spin = radPerTick;
         return this;
      }

      public VfxOptions.Builder gravity(float gravity) {
         this.gravity = gravity;
         return this;
      }

      public VfxOptions.Builder drag(float keep) {
         this.drag = keep;
         return this;
      }

      public VfxOptions.Builder curl(float radPerTick) {
         this.curl = radPerTick;
         return this;
      }

      public VfxOptions.Builder delay(int ticks) {
         this.delay = Math.max(0, ticks);
         return this;
      }

      public VfxOptions.Builder collide() {
         this.flags |= 512;
         return this;
      }

      public VfxOptions.Builder mirror(boolean mirror) {
         this.flags = mirror ? this.flags | 1024 : this.flags & -1025;
         return this;
      }

      public VfxOptions build() {
         return new VfxOptions(
            this.type,
            this.color & 16777215,
            this.scale,
            this.life,
            this.yaw,
            this.pitch,
            this.roll,
            this.color2,
            this.variant,
            this.flags,
            this.spin,
            this.gravity,
            this.drag,
            this.stretch,
            this.curl,
            this.delay
         );
      }
   }
}

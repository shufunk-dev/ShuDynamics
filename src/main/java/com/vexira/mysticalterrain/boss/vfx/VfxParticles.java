package com.vexira.mysticalterrain.boss.vfx;

import com.vexira.mysticalterrain.MysticalTerrain;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jspecify.annotations.Nullable;

public final class VfxParticles {
   public static final int SLASH_CLASSIC = 0;
   public static final int SLASH_THIN = 1;
   public static final int SLASH_HEAVY = 2;
   public static final int SLASH_SPIN = 3;
   public static final int SLASH_CLAW = 4;
   public static final int FLARE_CROSS = 0;
   public static final int FLARE_STAR = 1;
   public static final int GLINT_STAR = 0;
   public static final int GLINT_SPARKLE = 1;
   public static final int SHOCK_THICK = 0;
   public static final int SHOCK_RIPPLE = 1;
   public static final int WISP_ORB = 0;
   public static final int WISP_FLAME = 1;
   public static final int HEX_PANEL = 0;
   public static final int HEX_COMB = 1;
   public static final int BEAM_SMOOTH = 0;
   public static final int BEAM_STREAK = 1;
   public static final int BEAM_ENERGY = 2;
   public static final int TELE_RIM = 0;
   public static final int TELE_DISC = 1;
   public static final int TELE_BAR = 2;
   public static final int TELE_WEDGE = 3;
   public static final Map<VfxParticles.Kind, ParticleType<VfxOptions>> TYPES = new LinkedHashMap<>();

   private VfxParticles() {
   }

   public static void register() {
      for (VfxParticles.Kind k : VfxParticles.Kind.values()) {
         ParticleType<VfxOptions> type = FabricParticleTypes.complex(true, VfxOptions::codec, VfxOptions::streamCodec);
         TYPES.put(k, (ParticleType<VfxOptions>)Registry.register(BuiltInRegistries.PARTICLE_TYPE, MysticalTerrain.id("vfx_" + k.name().toLowerCase()), type));
      }

      PayloadTypeRegistry.clientboundPlay().register(FxPayload.TYPE, FxPayload.CODEC);
      PayloadTypeRegistry.clientboundPlay().register(VfxCastPayload.TYPE, VfxCastPayload.CODEC);
   }

   public static ParticleType<VfxOptions> type(VfxParticles.Kind kind) {
      return TYPES.get(kind);
   }

   public static VfxParticles.@Nullable Kind kind(ParticleType<?> type) {
      for (Entry<VfxParticles.Kind, ParticleType<VfxOptions>> e : TYPES.entrySet()) {
         if (e.getValue() == type) {
            return e.getKey();
         }
      }

      return null;
   }

   public enum Kind {
      RING(true, 1),
      FLARE(true, 2),
      SPARK(true, 1),
      EMBER(true, 1),
      SMOKE(false, 4),
      SLASH(true, 5),
      SHARD(false, 4),
      RUNE(true, 4),
      GLINT(true, 2),
      BOLT(true, 3),
      SHOCK(true, 2),
      WISP(true, 2),
      CRACK(true, 2),
      SCORCH(false, 2),
      DEBRIS(false, 6),
      HEX(true, 2),
      BEAM(true, 3),
      TELEGRAPH(true, 4);

      public final boolean glow;
      public final int variants;

      Kind(boolean glow, int variants) {
         this.glow = glow;
         this.variants = variants;
      }
   }
}

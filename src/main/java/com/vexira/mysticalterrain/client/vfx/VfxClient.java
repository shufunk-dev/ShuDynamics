package com.vexira.mysticalterrain.client.vfx;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.vfx.FxPayload;
import com.vexira.mysticalterrain.boss.vfx.VfxCastPayload;
import com.vexira.mysticalterrain.boss.vfx.VfxOptions;
import com.vexira.mysticalterrain.boss.vfx.VfxParticles;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleGroupRegistry;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class VfxClient {
   private static final Identifier VIGNETTE = MysticalTerrain.id("textures/gui/vfx/vignette.png");
   private static final Identifier LINES = MysticalTerrain.id("textures/gui/vfx/impact_lines.png");
   private static final int FRAME_TICKS = 4;
   private static final Map<VfxParticles.Kind, SpriteSet> SPRITES = new EnumMap<>(VfxParticles.Kind.class);
   private static float shake;
   private static int shakeTicks;
   private static int shakeTotal;
   private static int flashColor;
   private static float flash;
   private static int flashStyle;
   private static int frameTicks;
   private static float shakeYaw;
   private static float shakePitch;
   private static float shakeRoll;
   private static final RandomSource RANDOM = RandomSource.create();

   private VfxClient() {
   }

   public static void register() {
      for (Entry<VfxParticles.Kind, ParticleType<VfxOptions>> e : VfxParticles.TYPES.entrySet()) {
         VfxParticles.Kind kind = e.getKey();
         ParticleProviderRegistry.getInstance().register(e.getValue(), sprites -> {
            SPRITES.put(kind, sprites);
            return (o, level, x, y, z, dx, dy, dz, random) -> allow(kind, random) ? new VfxParticle(level, x, y, z, dx, dy, dz, o, kind, sprites) : null;
         });
      }

      ParticleGroupRegistry.register(VfxParticle.GROUP, VfxParticleGroup::new);
      ParticleGroupRegistry.registerOrdering(ParticleRenderType.SINGLE_QUADS, VfxParticle.GROUP);
      ClientPlayNetworking.registerGlobalReceiver(FxPayload.TYPE, (payload, context) -> apply(payload));
      ClientPlayNetworking.registerGlobalReceiver(VfxCastPayload.TYPE, (payload, context) -> VfxCasts.play(payload));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         if (shakeTicks > 0) {
            shakeTicks--;
         }

         if (frameTicks > 0) {
            frameTicks--;
         }

         flash = Math.max(0.0F, flash - (flashStyle == 2 ? 0.12F : 0.06F));
      });
      HudElementRegistry.addLast(MysticalTerrain.id("impact_flash"), (graphics, tick) -> drawScreen(graphics));
   }

   private static boolean allow(VfxParticles.Kind kind, RandomSource random) {
      return kind != VfxParticles.Kind.SPARK
            && kind != VfxParticles.Kind.EMBER
            && kind != VfxParticles.Kind.SMOKE
            && kind != VfxParticles.Kind.SHARD
            && kind != VfxParticles.Kind.DEBRIS
         ? true
         : random.nextFloat() < density();
   }

   public static float density() {
      return switch ((ParticleStatus)Minecraft.getInstance().options.particles().get()) {
         case ALL -> 1.0F;
         case DECREASED -> 0.5F;
         case MINIMAL -> 0.2F;
         default -> throw new MatchException(null, null);
      };
   }

   public static @Nullable VfxParticle spawn(VfxOptions o, double x, double y, double z, double dx, double dy, double dz) {
      Minecraft mc = Minecraft.getInstance();
      VfxParticles.Kind kind = VfxParticles.kind(o.getType());
      SpriteSet sprites = kind == null ? null : SPRITES.get(kind);
      if (mc.level != null && sprites != null) {
         VfxParticle p = new VfxParticle(mc.level, x, y, z, dx, dy, dz, o, kind, sprites);
         mc.particleEngine.add(p);
         return p;
      } else {
         return null;
      }
   }

   public static void apply(FxPayload payload) {
      if (payload.shake() > 0.0F && payload.shake() >= shake * ((float)shakeTicks / Math.max(1, shakeTotal))) {
         shake = Math.min(2.0F, payload.shake());
         shakeTicks = shakeTotal = Math.max(1, payload.shakeTicks());
      }

      if (payload.flash() > 0.0F && payload.flash() >= flash) {
         flash = Math.min(0.8F, payload.flash());
         flashColor = payload.flashColor();
         flashStyle = payload.style();
         if (payload.style() == 2) {
            frameTicks = 4;
         }
      }
   }

   public static void screenAt(Vec3 at, double radius, double strength, int ticks, int color, float flashStrength, int style) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !(radius <= 0.0)) {
         double d = mc.player.position().distanceTo(at);
         if (!(d > radius)) {
            float falloff = (float)Mth.clamp(1.0 - d / radius, 0.15, 1.0);
            apply(new FxPayload((float)strength * falloff, ticks, color, flashStrength * falloff, style));
         }
      }
   }

   private static void drawScreen(GuiGraphicsExtractor graphics) {
      int w = graphics.guiWidth();
      int h = graphics.guiHeight();
      int rgb = flashColor & 16777215;
      if (flash > 0.01F) {
         switch (flashStyle) {
            case 1:
               graphics.blit(
                  RenderPipelines.GUI_TEXTURED, VIGNETTE, 0, 0, 0.0F, 0.0F, w, h, 256, 256, 256, 256, ARGB.color(Math.min(1.0F, flash * 1.6F), rgb)
               );
               graphics.fill(0, 0, w, h, ARGB.color(flash * 0.25F, rgb));
               break;
            case 2:
               graphics.fill(0, 0, w, h, ARGB.color(flash * 0.7F, rgb));
               break;
            default:
               graphics.fill(0, 0, w, h, ARGB.color(flash, rgb));
         }
      }

      if (frameTicks > 0) {
         float k = frameTicks / 4.0F;
         float grow = 1.0F + 0.3F * (1.0F - k);
         int cw = (int)(w * grow);
         int ch = (int)(h * grow);
         graphics.blit(RenderPipelines.GUI_TEXTURED, VIGNETTE, 0, 0, 0.0F, 0.0F, w, h, 256, 256, 256, 256, ARGB.color(0.85F * k, 0));
         graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            LINES,
            (w - cw) / 2,
            (h - ch) / 2,
            0.0F,
            0.0F,
            cw,
            ch,
            256,
            256,
            256,
            256,
            ARGB.color(Math.min(1.0F, 1.2F * k), 16777215)
         );
      }
   }

   public static float[] cameraOffset(float partial) {
      if (shakeTicks > 0 && !(shake <= 0.0F)) {
         float k = shake * (shakeTicks - partial) / Math.max(1, shakeTotal);
         shakeYaw = Mth.lerp(0.6F, shakeYaw, (RANDOM.nextFloat() - 0.5F) * 2.0F * k * 2.2F);
         shakePitch = Mth.lerp(0.6F, shakePitch, (RANDOM.nextFloat() - 0.5F) * 2.0F * k * 1.6F);
         shakeRoll = Mth.lerp(0.5F, shakeRoll, (RANDOM.nextFloat() - 0.5F) * 2.0F * k * 1.2F);
         return new float[]{shakeYaw, shakePitch, shakeRoll};
      } else {
         return null;
      }
   }
}

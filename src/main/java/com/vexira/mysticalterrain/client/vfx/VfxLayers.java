package com.vexira.mysticalterrain.client.vfx;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.pipeline.RenderPipeline.UniformDescription;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vexira.mysticalterrain.MysticalTerrain;
import java.lang.reflect.Method;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.particle.BillboardParticle.RenderType;

@Environment(EnvType.CLIENT)
public final class VfxLayers {
   private static RenderType additive;
   private static RenderType soft;
   private static boolean additiveFailed;
   private static boolean softFailed;
   private static Method shaderPackInUse;
   private static Object irisApi;
   private static boolean irisChecked;

   private VfxLayers() {
   }

   private static boolean shaderPack() {
      if (!irisChecked) {
         irisChecked = true;

         try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            irisApi = api.getMethod("getInstance").invoke(null);
            shaderPackInUse = api.getMethod("isShaderPackInUse");
         } catch (Throwable ignored) {
            irisApi = null;
         }
      }

      if (irisApi == null) {
         return false;
      }

      try {
         return (Boolean)shaderPackInUse.invoke(irisApi);
      } catch (Throwable e) {
         return true;
      }
   }

   public static boolean additiveUsable() {
      return !additiveFailed && !Boolean.getBoolean("mysticalterrain.vfx.soft") && !shaderPack() ? additive() != RenderType.PARTICLE_ATLAS_TRANSLUCENT : false;
   }

   public static boolean softUsable() {
      return !softFailed && !shaderPack() && soft() != RenderType.PARTICLE_ATLAS_TRANSLUCENT;
   }

   public static RenderType additive() {
      if (additive == null && !additiveFailed) {
         try {
            additive = layer("vfx_additive", BlendFunction.ADDITIVE);
         } catch (Throwable e) {
            MysticalTerrain.LOGGER.warn("Additive boss effects unavailable, using translucent", e);
            additiveFailed = true;
         }
      }

      return additiveFailed ? RenderType.PARTICLE_ATLAS_TRANSLUCENT : additive;
   }

   public static RenderType soft() {
      if (soft == null && !softFailed) {
         try {
            soft = layer("vfx_soft", BlendFunction.TRANSLUCENT);
         } catch (Throwable e) {
            MysticalTerrain.LOGGER.warn("Soft boss smoke unavailable, using translucent", e);
            softFailed = true;
         }
      }

      return softFailed ? RenderType.PARTICLE_ATLAS_TRANSLUCENT : soft;
   }

   static int order(RenderType layer) {
      if (additive != null && additive.equals(layer)) {
         return 2;
      } else {
         return soft != null && soft.equals(layer) ? 1 : 0;
      }
   }

   private static RenderType layer(String name, BlendFunction blend) {
      RenderPipeline base = RenderPipelines.TRANSLUCENT_PARTICLE;
      Builder b = RenderPipeline.builder(new Snippet[0])
         .withLocation(MysticalTerrain.id("pipeline/" + name))
         .withVertexShader(base.getVertexShader())
         .withFragmentShader(MysticalTerrain.id("core/" + name))
         .withVertexFormat(base.getVertexFormat(), base.getVertexFormatMode())
         .withDepthTestFunction(base.getDepthTestFunction())
         .withCull(false)
         .withBlend(blend)
         .withDepthWrite(false);

      for (String s : base.getSamplers()) {
         b.withSampler(s);
      }

      for (UniformDescription u : base.getUniforms()) {
         if (u.textureFormat() != null) {
            b.withUniform(u.name(), u.type(), u.textureFormat());
         } else {
            b.withUniform(u.name(), u.type());
         }
      }

      base.getShaderDefines().values().forEach((k, v) -> {
         try {
            b.withShaderDefine(k, Integer.parseInt(v));
         } catch (NumberFormatException ex) {
            b.withShaderDefine(k, Float.parseFloat(v));
         }
      });
      base.getShaderDefines().flags().forEach(b::withShaderDefine);
      RenderPipeline pipeline = b.build();
      if (!RenderSystem.getDevice().precompilePipeline(pipeline).isValid()) {
         throw new IllegalStateException("the " + name + " shader didn't compile");
      } else {
         return new RenderType(true, RenderType.PARTICLE_ATLAS_TRANSLUCENT.textureAtlasLocation(), pipeline);
      }
   }
}

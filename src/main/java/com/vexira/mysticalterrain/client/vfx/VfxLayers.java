package com.vexira.mysticalterrain.client.vfx;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Snippet;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.vexira.mysticalterrain.MysticalTerrain;
import java.lang.reflect.Method;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.client.renderer.RenderPipelines;

@Environment(EnvType.CLIENT)
public final class VfxLayers {
   private static Layer additive;
   private static Layer soft;
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
      return !additiveFailed && !Boolean.getBoolean("mysticalterrain.vfx.soft") && !shaderPack() ? additive() != Layer.TRANSLUCENT : false;
   }

   public static boolean softUsable() {
      return !softFailed && !shaderPack() && soft() != Layer.TRANSLUCENT;
   }

   public static Layer additive() {
      if (additive == null && !additiveFailed) {
         try {
            additive = layer("vfx_additive", BlendFunction.ADDITIVE);
         } catch (Throwable e) {
            MysticalTerrain.LOGGER.warn("Additive boss effects unavailable, using translucent", e);
            additiveFailed = true;
         }
      }

      return additiveFailed ? Layer.TRANSLUCENT : additive;
   }

   public static Layer soft() {
      if (soft == null && !softFailed) {
         try {
            soft = layer("vfx_soft", BlendFunction.TRANSLUCENT);
         } catch (Throwable e) {
            MysticalTerrain.LOGGER.warn("Soft boss smoke unavailable, using translucent", e);
            softFailed = true;
         }
      }

      return softFailed ? Layer.TRANSLUCENT : soft;
   }

   static int order(Layer layer) {
      if (additive != null && additive.equals(layer)) {
         return 2;
      } else {
         return soft != null && soft.equals(layer) ? 1 : 0;
      }
   }

   private static Layer layer(String name, BlendFunction blend) {
      RenderPipeline base = RenderPipelines.TRANSLUCENT_PARTICLE;
      Builder b = RenderPipeline.builder(new Snippet[0])
         .withLocation(MysticalTerrain.id("pipeline/" + name))
         .withVertexShader(base.getShaders().get(ShaderType.VERTEX))
         .withFragmentShader(MysticalTerrain.id("core/" + name))
         .withDepthStencilState(base.getDepthStencilState())
         .withPolygonMode(base.getPolygonMode())
         .withPrimitiveTopology(base.getPrimitiveTopology())
         .withCull(false)
         .withColorTargetState(new ColorTargetState(blend));

      for (int i = 0; i < base.getVertexFormatBindings().size(); i++) {
         b.withVertexBinding(i, base.getVertexFormatBinding(i));
      }

      for (var layout : base.getBindGroupLayouts()) {
         b.withBindGroupLayout(layout);
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
      return new Layer(true, Layer.TRANSLUCENT.textureAtlasLocation(), pipeline);
   }
}

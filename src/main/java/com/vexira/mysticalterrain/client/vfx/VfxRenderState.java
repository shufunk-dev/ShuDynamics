package com.vexira.mysticalterrain.client.vfx;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.BillboardParticleSubmittable;
import net.minecraft.client.particle.BillboardParticle.RenderType;
import net.minecraft.client.particle.BillboardParticleSubmittable.Buffers;
import net.minecraft.client.particle.BillboardParticleSubmittable.Layer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.LayeredCustomCommandRenderer.VerticesCache;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
final class VfxRenderState extends BillboardParticleSubmittable {
   private final Quaternionf rot = new Quaternionf();
   private final Vector3f pos = new Vector3f();

   protected void drawFace(
      VertexConsumer vertexConsumer,
      float x,
      float y,
      float z,
      float rotationX,
      float rotationY,
      float rotationZ,
      float rotationW,
      float size,
      float minU,
      float maxU,
      float minV,
      float maxV,
      int color,
      int brightness
   ) {
      float n = rotationX * rotationX + rotationY * rotationY + rotationZ * rotationZ + rotationW * rotationW;
      float s = (float)Math.sqrt(Math.max(1.0E-8F, n));
      this.rot.set(rotationX / s, rotationY / s, rotationZ / s, rotationW / s);
      float sx = size * n;
      this.vertex(vertexConsumer, x, y, z, sx, -size, maxU, maxV, color, brightness);
      this.vertex(vertexConsumer, x, y, z, sx, size, maxU, minV, color, brightness);
      this.vertex(vertexConsumer, x, y, z, -sx, size, minU, minV, color, brightness);
      this.vertex(vertexConsumer, x, y, z, -sx, -size, minU, maxV, color, brightness);
   }

   private void vertex(VertexConsumer out, float x, float y, float z, float lx, float ly, float u, float v, int color, int light) {
      this.pos.set(lx, ly, 0.0F).rotate(this.rot).add(x, y, z);
      out.vertex(this.pos.x(), this.pos.y(), this.pos.z()).texture(u, v).color(color).light(light);
   }

   public void render(Buffers buffers, VerticesCache cache, RenderPass pass, TextureManager manager, boolean translucent) {
      List<Entry<RenderType, Layer>> layers = new ArrayList<>(buffers.layers().entrySet());
      if (layers.stream().allMatch(ex -> VfxLayers.order((RenderType)ex.getKey()) == 0)) {
         super.render(buffers, cache, pass, manager, translucent);
      } else {
         layers.sort(Comparator.comparingInt(ex -> VfxLayers.order((RenderType)ex.getKey())));
         ShapeIndexBuffer indices = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
         pass.setVertexBuffer(0, cache.get());
         pass.setIndexBuffer(indices.getIndexBuffer(buffers.indexCount()), indices.getIndexType());
         pass.setUniform("DynamicTransforms", buffers.dynamicTransforms());

         for (Entry<RenderType, Layer> e : layers) {
            if (translucent == e.getKey().translucent()) {
               pass.setPipeline(e.getKey().pipeline());
               AbstractTexture texture = manager.getTexture(e.getKey().textureAtlasLocation());
               pass.bindTexture("Sampler0", texture.getGlTextureView(), texture.getSampler());
               pass.drawIndexed(e.getValue().vertexOffset(), 0, e.getValue().indexCount(), 1);
            }
         }
      }
   }
}

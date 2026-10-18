package com.vexira.mysticalterrain.client.vfx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
final class VfxRenderState extends QuadParticleRenderState {
   private final Quaternionf rot = new Quaternionf();
   private final Vector3f pos = new Vector3f();

   @Override
   protected void renderRotatedQuad(
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
      out.addVertex(this.pos.x(), this.pos.y(), this.pos.z()).setUv(u, v).setColor(color).setLight(light);
   }
}

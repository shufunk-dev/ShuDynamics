package com.vexira.mysticalterrain.client.vfx;

import com.vexira.mysticalterrain.MysticalTerrain;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.Submittable;

@Environment(EnvType.CLIENT)
final class VfxParticleGroup extends ParticleRenderer<VfxParticle> {
   private final VfxRenderState state = new VfxRenderState();
   private boolean warned;

   VfxParticleGroup(ParticleManager engine) {
      super(engine);
   }

   public Submittable render(Frustum frustum, Camera camera, float tickProgress) {
      for (VfxParticle p : this.particles) {
         if (p.visible(frustum)) {
            try {
               p.render(this.state, camera, tickProgress);
            } catch (RuntimeException e) {
               p.markDead();
               if (!this.warned) {
                  this.warned = true;
                  MysticalTerrain.LOGGER.warn("Dropped a broken effect particle", e);
               }
            }
         }
      }

      return this.state;
   }
}

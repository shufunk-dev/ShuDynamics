package com.vexira.mysticalterrain.client.vfx;

import com.vexira.mysticalterrain.MysticalTerrain;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;

@Environment(EnvType.CLIENT)
final class VfxParticleGroup extends ParticleGroup<VfxParticle> {
   private final VfxRenderState state = new VfxRenderState();
   private boolean warned;

   VfxParticleGroup(ParticleEngine engine) {
      super(engine);
   }

   public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float tickProgress) {
      for (VfxParticle p : this.particles) {
         if (p.visible(frustum)) {
            try {
               p.extract(this.state, camera, tickProgress);
            } catch (RuntimeException e) {
               p.remove();
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

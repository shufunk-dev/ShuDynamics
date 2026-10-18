package com.vexira.mysticalterrain.client.boss;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.MysticalBoss;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

@Environment(EnvType.CLIENT)
public class BossRenderer<T extends MysticalBoss> extends GeoEntityRenderer<T, LivingEntityRenderState> {
   public BossRenderer(Context context, String id, float shadow) {
      super(context, new DefaultedEntityGeoModel(MysticalTerrain.id(id)));
      this.withRenderLayer(new AutoGlowingGeoLayer(this));
      this.shadowRadius = shadow;
   }

   protected float getDeathMaxRotation(GeoRenderState state) {
      return 0.0F;
   }

   public int getPackedOverlay(T animatable, @Nullable Void relatedObject, float u, float partialTick) {
      return !animatable.isBoss()
         ? super.getPackedOverlay(animatable, relatedObject, u, partialTick)
         : OverlayTexture.pack(OverlayTexture.u(u), OverlayTexture.v(animatable.hurtTime > 0));
   }

   protected AABB getBoundingBoxForCulling(T entity) {
      double r = entity.renderReach();
      return r <= 0.0 ? entity.getBoundingBox() : entity.getBoundingBox().inflate(r, r * 0.5, r);
   }
}

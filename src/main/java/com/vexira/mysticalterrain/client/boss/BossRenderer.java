package com.vexira.mysticalterrain.client.boss;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.MysticalBoss;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.math.Box;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

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
         : OverlayTexture.packUv(OverlayTexture.getU(u), OverlayTexture.getV(animatable.hurtTime > 0));
   }

   protected Box getBoundingBoxForCulling(T entity) {
      double r = entity.renderReach();
      return r <= 0.0 ? entity.getBoundingBox() : entity.getBoundingBox().expand(r, r * 0.5, r);
   }
}

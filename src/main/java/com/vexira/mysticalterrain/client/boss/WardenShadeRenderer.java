package com.vexira.mysticalterrain.client.boss;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.entity.WardenShade;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

@Environment(EnvType.CLIENT)
public class WardenShadeRenderer extends GeoEntityRenderer<WardenShade, LivingEntityRenderState> {
   public WardenShadeRenderer(Context context) {
      super(context, new DefaultedEntityGeoModel(MysticalTerrain.id("hollow_warden")));
      this.withScale(0.85F);
   }

   public @Nullable RenderType getRenderType(LivingEntityRenderState state, Identifier texture) {
      return RenderTypes.entityTranslucent(texture);
   }

   public int getRenderColor(WardenShade animatable, @Nullable Void relatedObject, float partialTick) {
      return ARGB.color(140, 70, 60, 110);
   }
}

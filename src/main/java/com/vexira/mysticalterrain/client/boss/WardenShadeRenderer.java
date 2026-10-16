package com.vexira.mysticalterrain.client.boss;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.entity.WardenShade;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Environment(EnvType.CLIENT)
public class WardenShadeRenderer extends GeoEntityRenderer<WardenShade, LivingEntityRenderState> {
   public WardenShadeRenderer(Context context) {
      super(context, new DefaultedEntityGeoModel(MysticalTerrain.id("hollow_warden")));
      this.withScale(0.85F);
   }

   public @Nullable RenderLayer getRenderType(LivingEntityRenderState state, Identifier texture) {
      return RenderLayers.entityTranslucent(texture);
   }

   public int getRenderColor(WardenShade animatable, @Nullable Void relatedObject, float partialTick) {
      return ColorHelper.getArgb(140, 70, 60, 110);
   }
}

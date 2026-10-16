package com.vexira.mysticalterrain.client.boss;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.gear.GeoRenderHooks;
import com.vexira.mysticalterrain.gear.MysticArmor;
import com.vexira.mysticalterrain.gear.MysticWeapon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.model.DefaultedGeoModel;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

@Environment(EnvType.CLIENT)
public final class GearRenderers {
   private GearRenderers() {
   }

   private static <R extends BipedEntityRenderState & GeoRenderState> GeoArmorRenderer<MysticArmor, R> armorRenderer(String model) {
      GeoArmorRenderer<MysticArmor, R> renderer = new GeoArmorRenderer(new DefaultedGeoModel<MysticArmor>(MysticalTerrain.id(model)) {
         protected String subtype() {
            return "armor";
         }
      });
      renderer.withRenderLayer(new AutoGlowingGeoLayer(renderer));
      return renderer;
   }

   public static void install() {
      GeoRenderHooks.weapons = item -> new GeoRenderProvider() {
         private GeoItemRenderer<MysticWeapon> renderer;

         public GeoItemRenderer<?> getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new GeoItemRenderer(new DefaultedItemGeoModel(MysticalTerrain.id(((MysticWeapon)item).gearId())));
               this.renderer.withRenderLayer(new AutoGlowingGeoLayer(this.renderer));
            }

            return this.renderer;
         }
      };
      GeoRenderHooks.armor = item -> new GeoRenderProvider() {
         private GeoArmorRenderer<MysticArmor, ?> renderer;

         public @Nullable GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot slot) {
            if (this.renderer == null) {
               this.renderer = GearRenderers.armorRenderer(((MysticArmor)item).model());
            }

            return this.renderer;
         }
      };
   }
}

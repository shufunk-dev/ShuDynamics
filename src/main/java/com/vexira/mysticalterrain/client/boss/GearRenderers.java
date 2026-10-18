package com.vexira.mysticalterrain.client.boss;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.gear.GeoRenderHooks;
import com.vexira.mysticalterrain.gear.MysticArmor;
import com.vexira.mysticalterrain.gear.MysticWeapon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.model.DefaultedGeoModel;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

@Environment(EnvType.CLIENT)
public final class GearRenderers {
   private GearRenderers() {
   }

   private static <R extends HumanoidRenderState & GeoRenderState> GeoArmorRenderer<MysticArmor, R> armorRenderer(String model) {
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

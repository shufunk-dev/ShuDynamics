package com.vexira.mysticalterrain.gear;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

public class MysticArmor extends Item implements GeoItem {
   private static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private final String set;
   private final String model;
   private final EquipmentSlot slot;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private Object renderProvider;

   public MysticArmor(Properties properties, String set, String model, EquipmentSlot slot) {
      super(properties);
      this.set = set;
      this.model = model;
      this.slot = slot;
   }

   public String model() {
      return this.model;
   }

   public String set() {
      return this.set;
   }

   public EquipmentSlot slot() {
      return this.slot;
   }

   public static Map<String, Integer> worn(Player player) {
      Map<String, Integer> counts = new LinkedHashMap<>(2);

      for (EquipmentSlot s : SLOTS) {
         if (player.getItemBySlot(s).getItem() instanceof MysticArmor a) {
            counts.merge(a.set, 1, Integer::sum);
         }
      }

      return counts;
   }

   public static int pieces(Player player, String set) {
      int n = 0;

      for (EquipmentSlot s : SLOTS) {
         if (player.getItemBySlot(s).getItem() instanceof MysticArmor a && a.set.equals(set)) {
            n++;
         }
      }

      return n;
   }

   public static boolean wearsFullSet(Player player, String set) {
      return pieces(player, set) == 4;
   }

   public static @Nullable String fullSet(Player player) {
      return player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof MysticArmor chest && wearsFullSet(player, chest.set) ? chest.set : null;
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
      Player viewer = ClientView.player.get();
      GearTooltips.armor(this.set, viewer == null ? 0 : pieces(viewer, this.set), textConsumer);
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("idle", 0, test -> test.setAndContinue(RawAnimation.begin().thenLoop("animation." + this.model + ".idle"))));
   }

   public Object getRenderProvider() {
      if (this.renderProvider == null) {
         this.renderProvider = GeoRenderHooks.armor.apply(this);
      }

      return this.renderProvider;
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}

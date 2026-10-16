package com.vexira.mysticalterrain.gear;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MysticArmor extends Item implements GeoItem {
   private static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private final String set;
   private final String model;
   private final EquipmentSlot slot;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private Object renderProvider;

   public MysticArmor(Settings properties, String set, String model, EquipmentSlot slot) {
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

   public static Map<String, Integer> worn(PlayerEntity player) {
      Map<String, Integer> counts = new LinkedHashMap<>(2);

      for (EquipmentSlot s : SLOTS) {
         if (player.getEquippedStack(s).getItem() instanceof MysticArmor a) {
            counts.merge(a.set, 1, Integer::sum);
         }
      }

      return counts;
   }

   public static int pieces(PlayerEntity player, String set) {
      int n = 0;

      for (EquipmentSlot s : SLOTS) {
         if (player.getEquippedStack(s).getItem() instanceof MysticArmor a && a.set.equals(set)) {
            n++;
         }
      }

      return n;
   }

   public static boolean wearsFullSet(PlayerEntity player, String set) {
      return pieces(player, set) == 4;
   }

   public static @Nullable String fullSet(PlayerEntity player) {
      return player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof MysticArmor chest && wearsFullSet(player, chest.set) ? chest.set : null;
   }

   public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
      PlayerEntity viewer = ClientView.player.get();
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

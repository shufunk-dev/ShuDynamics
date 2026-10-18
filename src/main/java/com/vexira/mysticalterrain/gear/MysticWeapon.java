package com.vexira.mysticalterrain.gear;

import com.vexira.mysticalterrain.boss.vfx.Vfx;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.SingletonGeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

public class MysticWeapon extends Item implements GeoItem {
   private static final int HOLD = 72000;
   private final WeaponKit kit;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private Object renderProvider;

   public MysticWeapon(Properties properties, WeaponKit kit) {
      super(properties);
      this.kit = kit;
      SingletonGeoAnimatable.registerSyncedAnimatable(this);
   }

   public WeaponKit kit() {
      return this.kit;
   }

   public String gearId() {
      return this.kit.id();
   }

   private static boolean ready(Level level, Player player, String key) {
      return level.isClientSide() ? ClientView.cooldown.applyAsInt(key) <= 0 : GearState.of(player).ready(key);
   }

   public InteractionResult use(Level world, Player user, InteractionHand hand) {
      ItemStack stack = user.getItemInHand(hand);
      if (user.isSecondaryUseActive() && this.kit.alternate() != null) {
         return this.instant(world, user, hand, stack, GearUse.Slot.ALTERNATE);
      }

      if (this.kit.charge() != null && this.kit.active(GearUse.Slot.CHARGED) != null && ready(world, user, this.kit.key(GearUse.Slot.CHARGED))) {
         user.startUsingItem(hand);
         if (world instanceof ServerLevel server) {
            long id = GeoItem.getOrAssignId(stack, server);
            GearState.of(user).beginCharge(this.kit.id(), id);
            this.triggerAnim(user, id, "main", "charge");
         }

         return InteractionResult.CONSUME;
      } else {
         return this.instant(world, user, hand, stack, GearUse.Slot.PRIMARY);
      }
   }

   private InteractionResult instant(Level level, Player player, InteractionHand hand, ItemStack stack, GearUse.Slot slot) {
      if (this.kit.active(slot) == null) {
         return InteractionResult.PASS;
      } else if (!ready(level, player, this.kit.key(slot))) {
         return InteractionResult.FAIL;
      } else if (level instanceof ServerLevel server && !this.fire(server, player, hand, stack, slot, 0)) {
         return InteractionResult.FAIL;
      } else {
         player.swing(hand, SwingAnimation.DEFAULT, false);
         return InteractionResult.SUCCESS;
      }
   }

   private boolean fire(ServerLevel level, Player player, InteractionHand hand, ItemStack stack, GearUse.Slot slot, int charge) {
      Active active = this.kit.active(slot);
      if (active == null) {
         return false;
      }

      GearState state = GearState.of(player);
      boolean empowered = active.cost() > 0 && state.resonance(this.kit.id()) >= active.cost();
      float power = this.kit.power(slot, charge) * (empowered ? active.empowerScale() : 1.0F);
      Charge spec = this.kit.charge();
      int maxCharge = slot == GearUse.Slot.CHARGED && spec != null ? spec.levels() : 0;
      GearUse use = new GearUse(level, player, stack, hand, this.kit.arsenal(), this.kit, slot, charge, maxCharge, empowered, power);
      if (!active.cast().fire(use)) {
         return false;
      }

      if (empowered) {
         state.spendResonance(this.kit.id(), active.cost());
         GearKit.empowerFlourish(level, player, this.kit.arsenal().palette());
      }

      state.cooldown(this.kit.key(slot), active.cooldown());
      GearKit.sound(level, player, 1.3F, 0.95F + level.getRandom().nextFloat() * 0.1F, this.kit.sounds(slot));
      this.triggerAnim(player, GeoItem.getOrAssignId(stack, level), "main", slot == GearUse.Slot.ALTERNATE ? "alt" : "use");
      stack.hurtAndBreak(1, player, hand);
      return true;
   }

   public int getUseDuration(ItemStack stack, LivingEntity user) {
      return this.kit.charge() != null ? 72000 : 0;
   }

   public ItemUseAnimation getUseAnimation(ItemStack stack) {
      Charge charge = this.kit.charge();
      return charge != null ? charge.pose() : ItemUseAnimation.NONE;
   }

   public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
      Charge charge = this.kit.charge();
      if (charge != null && world instanceof ServerLevel server && user instanceof Player player) {
         GearState state = GearState.of(player);
         int held = 72000 - remainingUseTicks;
         int reached = charge.level(held);
         boolean max = reached >= charge.levels();
         Vfx.Palette pal = this.kit.arsenal().palette();
         if (reached > state.chargeLevel()) {
            state.chargeLevel(reached);
            if (charge.reached() != null) {
               charge.reached().reached(server, player, this.kit, reached, max);
            } else {
               GearKit.chargeLevel(server, player, pal, reached, max);
            }

            GearKit.sound(server, player, max ? 1.1F : 0.7F, 0.8F + 0.15F * reached, "weapon." + this.kit.id() + ".charge", "weapon." + this.kit.id() + ".use");
         } else if (held % 4 == 0) {
            Vec3 tip = GearKit.tip(player);
            Vfx.sparks(server, tip, pal.glow(), 1 + reached, 0.08, 0.1);
            if (max && held % 12 == 0) {
               Vfx.flare(server, tip, pal.core(), 1.2, 5);
            }
         }
      }
   }

   public boolean releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
      Charge charge = this.kit.charge();
      if (charge != null && user instanceof Player player) {
         int reached = charge.level(72000 - remainingUseTicks);
         GearUse.Slot slot = reached > 0 ? GearUse.Slot.CHARGED : GearUse.Slot.PRIMARY;
         InteractionHand hand = player.getUsedItemHand();
         if (world instanceof ServerLevel server) {
            GearState state = GearState.of(player);
            this.stopTriggeredAnim(player, GeoItem.getOrAssignId(stack, server), "main", "charge");
            state.endCharge();
            if (this.kit.active(slot) != null && state.ready(this.kit.key(slot)) && this.fire(server, player, hand, stack, slot, reached)) {
               player.swing(hand, SwingAnimation.DEFAULT, false);
               return true;
            } else {
               return false;
            }
         } else {
            if (this.kit.active(slot) != null && ready(world, player, this.kit.key(slot))) {
               player.swing(hand, SwingAnimation.DEFAULT, false);
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      super.hurtEnemy(stack, target, attacker);
      if (attacker.level() instanceof ServerLevel level) {
         this.kit.passive().hit(level, attacker, target, stack);
         if (attacker instanceof Player player && this.kit.usesResonance()) {
            GearState.of(player).addResonance(this.kit.id(), this.kit.resonancePerHit());
         }
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
      GearTooltips.weapon(this.kit, textConsumer);
   }

   public void registerControllers(ControllerRegistrar controllers) {
      String anim = "animation." + this.kit.id() + ".";
      controllers.add(new AnimationController("idle", 0, test -> test.setAndContinue(RawAnimation.begin().thenLoop(anim + "idle"))));
      controllers.add(
         new AnimationController("main", 0, test -> PlayState.STOP)
            .triggerableAnim("use", RawAnimation.begin().thenPlay(anim + "use"))
            .triggerableAnim("alt", RawAnimation.begin().thenPlay(anim + "alt"))
            .triggerableAnim("charge", RawAnimation.begin().thenLoop(anim + "charge"))
      );
   }

   public Object getRenderProvider() {
      if (this.renderProvider == null) {
         this.renderProvider = GeoRenderHooks.weapons.apply(this);
      }

      return this.renderProvider;
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}

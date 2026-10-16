package com.vexira.mysticalterrain.gear;

import com.vexira.mysticalterrain.boss.BossBolt;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.gear.arsenal.Arsenal;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

public final class GearUse {
   private final ServerWorld level;
   private final PlayerEntity player;
   private final ItemStack stack;
   private final @Nullable Hand hand;
   private final Arsenal arsenal;
   private final @Nullable WeaponKit weapon;
   private final GearUse.Slot slot;
   private final int charge;
   private final int maxCharge;
   private final boolean empowered;
   private final float power;

   GearUse(
      ServerWorld level,
      PlayerEntity player,
      ItemStack stack,
      @Nullable Hand hand,
      Arsenal arsenal,
      @Nullable WeaponKit weapon,
      GearUse.Slot slot,
      int charge,
      int maxCharge,
      boolean empowered,
      float power
   ) {
      this.level = level;
      this.player = player;
      this.stack = stack;
      this.hand = hand;
      this.arsenal = arsenal;
      this.weapon = weapon;
      this.slot = slot;
      this.charge = charge;
      this.maxCharge = maxCharge;
      this.empowered = empowered;
      this.power = power;
   }

   public ServerWorld level() {
      return this.level;
   }

   public PlayerEntity player() {
      return this.player;
   }

   public ItemStack stack() {
      return this.stack;
   }

   public @Nullable Hand hand() {
      return this.hand;
   }

   public Arsenal arsenal() {
      return this.arsenal;
   }

   public @Nullable WeaponKit weapon() {
      return this.weapon;
   }

   public GearUse.Slot slot() {
      return this.slot;
   }

   public Vfx.Palette palette() {
      return this.arsenal.palette();
   }

   public int charge() {
      return this.charge;
   }

   public int maxCharge() {
      return this.maxCharge;
   }

   public boolean fullyCharged() {
      return this.maxCharge > 0 && this.charge >= this.maxCharge;
   }

   public boolean empowered() {
      return this.empowered;
   }

   public float power() {
      return this.power;
   }

   public float scaled(float base) {
      return base * this.power;
   }

   public GearState state() {
      return GearState.of(this.player);
   }

   public void strike(LivingEntity e, float damage) {
      GearKit.strike(this.level, this.player, e, damage * this.power);
   }

   public void magic(LivingEntity e, float damage) {
      GearKit.magic(this.level, this.player, e, damage * this.power);
   }

   public BossBolt bolt(BossBolt.Style style, double speed, float damage) {
      return GearKit.bolt(this.level, this.player, style, speed, damage * this.power);
   }

   public List<LivingEntity> enemies(Vec3d at, double radius) {
      return GearKit.enemies(this.level, this.player, at, radius);
   }

   public void resonate(float amount) {
      if (this.weapon != null) {
         this.state().addResonance(this.weapon.id(), amount);
      }
   }

   public enum Slot {
      PRIMARY("primary", "use"),
      ALTERNATE("alternate", "alt"),
      CHARGED("charge", "release"),
      SET("set", "active");

      private final String key;
      private final String sound;

      Slot(String key, String sound) {
         this.key = key;
         this.sound = sound;
      }

      public String key() {
         return this.key;
      }

      public String sound() {
         return this.sound;
      }
   }
}

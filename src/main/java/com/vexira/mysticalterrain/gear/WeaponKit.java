package com.vexira.mysticalterrain.gear;

import com.vexira.mysticalterrain.gear.arsenal.Arsenal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.jspecify.annotations.Nullable;

public final class WeaponKit {
   public static final WeaponKit.OnHit NONE = (level, attacker, target, stack) -> {};
   private final Arsenal arsenal;
   private final String id;
   private final float damage;
   private final float speed;
   private boolean axe;
   private @Nullable Active primary;
   private @Nullable Active alternate;
   private @Nullable Charge charge;
   private WeaponKit.OnHit passive = NONE;
   private float resonancePerHit = 10.0F;

   public WeaponKit(Arsenal arsenal, String id, float damage, float speed) {
      this.arsenal = arsenal;
      this.id = id;
      this.damage = damage;
      this.speed = speed;
   }

   public WeaponKit axe() {
      this.axe = true;
      return this;
   }

   public WeaponKit primary(int cooldown, Active.Cast cast) {
      return this.primary(Active.of(cooldown, cast));
   }

   public WeaponKit primary(Active active) {
      this.primary = active;
      return this;
   }

   public WeaponKit alternate(int cooldown, Active.Cast cast) {
      return this.alternate(Active.of(cooldown, cast));
   }

   public WeaponKit alternate(Active active) {
      this.alternate = active;
      return this;
   }

   public WeaponKit charge(Charge charge) {
      this.charge = charge;
      return this;
   }

   public WeaponKit passive(WeaponKit.OnHit onHit) {
      this.passive = onHit;
      return this;
   }

   public WeaponKit resonancePerHit(float amount) {
      this.resonancePerHit = amount;
      return this;
   }

   public Arsenal arsenal() {
      return this.arsenal;
   }

   public String id() {
      return this.id;
   }

   public float damage() {
      return this.damage;
   }

   public float speed() {
      return this.speed;
   }

   public boolean isAxe() {
      return this.axe;
   }

   public @Nullable Active primary() {
      return this.primary;
   }

   public @Nullable Active alternate() {
      return this.alternate;
   }

   public @Nullable Charge charge() {
      return this.charge;
   }

   public WeaponKit.OnHit passive() {
      return this.passive;
   }

   public float resonancePerHit() {
      return this.resonancePerHit;
   }

   public boolean chargeIsSeparate() {
      return this.charge != null && this.charge.active() != null;
   }

   public @Nullable Active active(GearUse.Slot slot) {
      return switch (slot) {
         case PRIMARY -> this.primary;
         case ALTERNATE -> this.alternate;
         case CHARGED -> this.chargeIsSeparate() ? this.charge.active() : this.primary;
         case SET -> null;
      };
   }

   public String key(GearUse.Slot slot) {
      return slot == GearUse.Slot.CHARGED && !this.chargeIsSeparate() ? this.id + "." + GearUse.Slot.PRIMARY.key() : this.id + "." + slot.key();
   }

   public float power(GearUse.Slot slot, int charge) {
      return slot == GearUse.Slot.CHARGED && this.charge != null && !this.chargeIsSeparate() ? 1.0F + this.charge.perLevel() * charge : 1.0F;
   }

   public boolean usesResonance() {
      return this.primary != null && this.primary.cost() > 0
         || this.alternate != null && this.alternate.cost() > 0
         || this.chargeIsSeparate() && this.charge.active().cost() > 0;
   }

   public String[] sounds(GearUse.Slot slot) {
      String use = "weapon." + this.id + ".use";
      return slot == GearUse.Slot.PRIMARY ? new String[]{use} : new String[]{"weapon." + this.id + "." + slot.sound(), use};
   }

   @FunctionalInterface
   public interface OnHit {
      void hit(ServerWorld var1, LivingEntity var2, LivingEntity var3, ItemStack var4);
   }
}

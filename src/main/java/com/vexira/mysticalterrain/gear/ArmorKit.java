package com.vexira.mysticalterrain.gear;

import com.vexira.mysticalterrain.gear.arsenal.Arsenal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public final class ArmorKit {
   private final Arsenal arsenal;
   private double knockback = 0.1;
   private double toughness = 3.5;
   private ArmorKit.@Nullable Tick twoPiece;
   private ArmorKit.@Nullable Tick fourPiece;
   private ArmorKit.@Nullable Hurt hurt;
   private ArmorKit.@Nullable Guard damage;
   private ArmorKit.@Nullable Guard death;
   private @Nullable Active active;
   private int activePieces = 4;

   public ArmorKit(Arsenal arsenal) {
      this.arsenal = arsenal;
   }

   public ArmorKit knockback(double perPiece) {
      this.knockback = perPiece;
      return this;
   }

   public ArmorKit toughness(double perPiece) {
      this.toughness = perPiece;
      return this;
   }

   public ArmorKit twoPiece(ArmorKit.Tick tick) {
      this.twoPiece = tick;
      return this;
   }

   public ArmorKit fourPiece(ArmorKit.Tick tick) {
      this.fourPiece = tick;
      return this;
   }

   public ArmorKit onHurt(ArmorKit.Hurt hurt) {
      this.hurt = hurt;
      return this;
   }

   public ArmorKit allowDamage(ArmorKit.Guard guard) {
      this.damage = guard;
      return this;
   }

   public ArmorKit allowDeath(ArmorKit.Guard guard) {
      this.death = guard;
      return this;
   }

   public ArmorKit active(int cooldown, Active.Cast cast) {
      return this.active(Active.of(cooldown, cast));
   }

   public ArmorKit active(Active active) {
      this.active = active;
      return this;
   }

   public ArmorKit activePieces(int pieces) {
      this.activePieces = Math.max(1, Math.min(4, pieces));
      return this;
   }

   public Arsenal arsenal() {
      return this.arsenal;
   }

   public double knockback() {
      return this.knockback;
   }

   public double toughness() {
      return this.toughness;
   }

   public boolean hasTwoPiece() {
      return this.twoPiece != null;
   }

   public boolean hasFourPiece() {
      return this.fourPiece != null;
   }

   public @Nullable Active active() {
      return this.active;
   }

   public int activePieces() {
      return this.activePieces;
   }

   public String key() {
      return "set." + this.arsenal.boss();
   }

   public String[] sounds() {
      String first = this.arsenal.weaponKits().isEmpty() ? "" : "weapon." + this.arsenal.weaponKits().get(0).id() + ".use";
      return new String[]{"armor." + this.arsenal.armorModel() + ".ability", first};
   }

   void tick(ServerLevel level, Player player, int pieces) {
      if (pieces >= 2 && this.twoPiece != null) {
         this.twoPiece.tick(level, player, pieces);
      }

      if (pieces >= 4 && this.fourPiece != null) {
         this.fourPiece.tick(level, player, pieces);
      }
   }

   void hurt(ServerLevel level, Player player, @Nullable LivingEntity attacker, DamageSource source, float taken, int pieces) {
      if (this.hurt != null) {
         this.hurt.hurt(level, player, attacker, source, taken, pieces);
      }
   }

   boolean allowDamage(ServerLevel level, Player player, DamageSource source, float amount, int pieces) {
      return this.damage == null || this.damage.allow(level, player, source, amount, pieces);
   }

   boolean allowDeath(ServerLevel level, Player player, DamageSource source, float amount, int pieces) {
      return this.death == null || this.death.allow(level, player, source, amount, pieces);
   }

   @FunctionalInterface
   public interface Guard {
      boolean allow(ServerLevel var1, Player var2, DamageSource var3, float var4, int var5);
   }

   @FunctionalInterface
   public interface Hurt {
      void hurt(ServerLevel var1, Player var2, @Nullable LivingEntity var3, DamageSource var4, float var5, int var6);
   }

   @FunctionalInterface
   public interface Tick {
      void tick(ServerLevel var1, Player var2, int var3);
   }
}

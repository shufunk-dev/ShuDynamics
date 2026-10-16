package com.vexira.mysticalterrain.gear;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.consume.UseAction;
import net.minecraft.server.world.ServerWorld;
import org.jspecify.annotations.Nullable;

public final class Charge {
   private final int levels;
   private final int ticksPerLevel;
   private float perLevel = 0.4F;
   private @Nullable Active active;
   private UseAction pose = UseAction.TRIDENT;
   private Charge.@Nullable Reached reached;

   private Charge(int levels, int ticksPerLevel) {
      this.levels = Math.max(1, levels);
      this.ticksPerLevel = Math.max(1, ticksPerLevel);
   }

   public static Charge of(int levels, int ticksPerLevel) {
      return new Charge(levels, ticksPerLevel);
   }

   public Charge perLevel(float bonus) {
      this.perLevel = bonus;
      return this;
   }

   public Charge cast(int cooldown, Active.Cast cast) {
      return this.cast(Active.of(cooldown, cast));
   }

   public Charge cast(Active active) {
      this.active = active;
      return this;
   }

   public Charge pose(UseAction pose) {
      this.pose = pose;
      return this;
   }

   public Charge onLevel(Charge.Reached reached) {
      this.reached = reached;
      return this;
   }

   public int levels() {
      return this.levels;
   }

   public int ticksPerLevel() {
      return this.ticksPerLevel;
   }

   public float perLevel() {
      return this.perLevel;
   }

   public @Nullable Active active() {
      return this.active;
   }

   public UseAction pose() {
      return this.pose;
   }

   public Charge.@Nullable Reached reached() {
      return this.reached;
   }

   public int level(float held) {
      return Math.min(this.levels, (int)(held / this.ticksPerLevel));
   }

   public int maxTicks() {
      return this.levels * this.ticksPerLevel;
   }

   @FunctionalInterface
   public interface Reached {
      void reached(ServerWorld var1, PlayerEntity var2, WeaponKit var3, int var4, boolean var5);
   }
}

package com.vexira.mysticalterrain.boss;

import com.vexira.mysticalterrain.boss.ai.BossContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public final class BossAction {
   final String anim;
   final int length;
   final int[] hits;
   double minRange = 0.0;
   double maxRange = 5.0;
   int cooldown = 100;
   int minPhase = 0;
   int maxPhase = Integer.MAX_VALUE;
   int weight = 10;
   boolean track = true;
   boolean needsTarget = true;
   boolean superArmor;
   int telegraphFrom = -1;
   int telegraphTo = -1;
   final Set<BossAction.Tag> tags = EnumSet.noneOf(BossAction.Tag.class);
   final List<BossAction.Combo> combos = new ArrayList<>();
   final List<BossAction.Advance> advances = new ArrayList<>();
   BossAction.@Nullable Hit onHit;
   BossAction.@Nullable Tick onTick;
   BossAction.@Nullable Hit onStart;
   BossAction.@Nullable Hit onEnd;
   BossAction.@Nullable Condition condition;
   BossAction.@Nullable Utility utility;

   private BossAction(String anim, double seconds, double... hitSeconds) {
      this.anim = anim;
      this.length = (int)Math.round(seconds * 20.0);
      this.hits = new int[hitSeconds.length];

      for (int i = 0; i < hitSeconds.length; i++) {
         this.hits[i] = (int)Math.round(hitSeconds[i] * 20.0);
      }
   }

   public static BossAction of(String anim, double seconds, double... hitSeconds) {
      return new BossAction(anim, seconds, hitSeconds);
   }

   public BossAction range(double min, double max) {
      this.minRange = min;
      this.maxRange = max;
      return this;
   }

   public BossAction cooldown(int ticks) {
      this.cooldown = ticks;
      return this;
   }

   public BossAction phase(int minPhase) {
      this.minPhase = minPhase;
      return this;
   }

   public BossAction maxPhase(int maxPhase) {
      this.maxPhase = maxPhase;
      return this;
   }

   public BossAction weight(int weight) {
      this.weight = weight;
      return this;
   }

   public BossAction lockFacing() {
      this.track = false;
      return this;
   }

   public BossAction anyTarget() {
      this.needsTarget = false;
      return this;
   }

   public BossAction when(BossAction.Condition condition) {
      this.condition = condition;
      return this;
   }

   public BossAction hit(BossAction.Hit hit) {
      this.onHit = hit;
      return this;
   }

   public BossAction tick(BossAction.Tick tick) {
      this.onTick = tick;
      return this;
   }

   public BossAction start(BossAction.Hit start) {
      this.onStart = start;
      return this;
   }

   public BossAction tags(BossAction.Tag... tags) {
      Collections.addAll(this.tags, tags);
      return this;
   }

   public BossAction tags(String spaceSeparated) {
      for (String s : spaceSeparated.trim().split("\\s+")) {
         if (!s.isEmpty()) {
            this.tags.add(BossAction.Tag.valueOf(s.toUpperCase(Locale.ROOT)));
         }
      }

      return this;
   }

   public BossAction combo(String nextAnim, float chance) {
      this.combos.add(new BossAction.Combo(nextAnim, chance));
      return this;
   }

   public BossAction superArmor() {
      this.superArmor = true;
      return this;
   }

   public BossAction utility(BossAction.Utility utility) {
      this.utility = utility;
      return this;
   }

   public BossAction end(BossAction.Hit end) {
      this.onEnd = end;
      return this;
   }

   public BossAction advance(double fromSec, double toSec, double blocksPerTick) {
      this.advances.add(new BossAction.Advance((int)Math.round(fromSec * 20.0), (int)Math.round(toSec * 20.0), blocksPerTick));
      return this;
   }

   public BossAction telegraph(double fromSec, double toSec) {
      this.telegraphFrom = (int)Math.round(fromSec * 20.0);
      this.telegraphTo = (int)Math.round(toSec * 20.0);
      return this;
   }

   public String anim() {
      return this.anim;
   }

   public int length() {
      return this.length;
   }

   public int[] hits() {
      return (int[])this.hits.clone();
   }

   public double minRange() {
      return this.minRange;
   }

   public double maxRange() {
      return this.maxRange;
   }

   public int cooldown() {
      return this.cooldown;
   }

   public int minPhase() {
      return this.minPhase;
   }

   public int maxPhase() {
      return this.maxPhase;
   }

   public int weight() {
      return this.weight;
   }

   public boolean tracks() {
      return this.track;
   }

   public boolean needsTarget() {
      return this.needsTarget;
   }

   public boolean isSuperArmor() {
      return this.superArmor;
   }

   public Set<BossAction.Tag> tagSet() {
      return Collections.unmodifiableSet(this.tags);
   }

   public boolean has(BossAction.Tag tag) {
      return this.tags.contains(tag);
   }

   public List<BossAction.Combo> combos() {
      return Collections.unmodifiableList(this.combos);
   }

   public List<BossAction.Advance> advances() {
      return Collections.unmodifiableList(this.advances);
   }

   public int telegraphFrom() {
      return this.telegraphFrom;
   }

   public int telegraphTo() {
      return this.telegraphTo;
   }

   public boolean telegraphing(int tick) {
      return this.telegraphFrom >= 0 && tick >= this.telegraphFrom && tick <= this.telegraphTo;
   }

   public record Advance(int from, int to, double speed) {
   }

   public record Combo(String anim, float chance) {
   }

   @FunctionalInterface
   public interface Condition {
      boolean test(MysticalBoss var1, @Nullable LivingEntity var2);
   }

   @FunctionalInterface
   public interface Hit {
      void hit(MysticalBoss var1, ServerLevel var2, @Nullable LivingEntity var3, int var4);
   }

   public enum Tag {
      MELEE,
      RANGED,
      AOE,
      GAP_CLOSER,
      ANTI_AIR,
      GUARD_BREAK,
      PUNISH,
      DEFENSIVE,
      SUMMON,
      ULTIMATE,
      MOBILITY,
      BEHIND;
   }

   @FunctionalInterface
   public interface Tick {
      void tick(MysticalBoss var1, ServerLevel var2, @Nullable LivingEntity var3, int var4);
   }

   @FunctionalInterface
   public interface Utility {
      float score(MysticalBoss var1, @Nullable LivingEntity var2, BossContext var3);
   }
}

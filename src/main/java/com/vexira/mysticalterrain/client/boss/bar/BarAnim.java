package com.vexira.mysticalterrain.client.boss.bar;

import com.vexira.mysticalterrain.boss.net.BossStatePayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class BarAnim {
   public static final int POP_LIFE = 28;
   public static final int POP_FADE = 6;
   private static final int MAX_POPS = 6;
   public @Nullable BossStatePayload state;
   public float stateAt;
   public float bornAt = Float.NaN;
   private float lastFrame;
   public float shown = -1.0F;
   public float target;
   public float chip;
   public float chipAt = -1000.0F;
   public float healAt = -1000.0F;
   public int fillPhase = -1;
   public int prevFillPhase;
   public float fillPhaseAt = -1000.0F;
   public int bannerPhase;
   public float bannerAt = -1000.0F;
   public float castAlpha;
   public float castProgress;
   public String castAnim = "";
   public BarText.@Nullable Line castName;
   private int castGen = -1;
   public float poiseShown = -1.0F;
   public float staggerAt = -1000.0F;
   public float wardAt = -1000.0F;
   public final BarAnim.Pop[] pops = new BarAnim.Pop[6];
   private int lastPop = -1;
   private int seed = -1640531527;
   private int hpValue = Integer.MIN_VALUE;
   private int hpMax = Integer.MIN_VALUE;
   private BarText.@Nullable Line hpLine;
   private int pctKey = -1;
   private BarText.@Nullable Line pctLine;
   private int wardValue = Integer.MIN_VALUE;
   private BarText.@Nullable Line wardLine;
   private int levelValue = Integer.MIN_VALUE;
   private int levelGen = -1;
   private BarText.@Nullable Line levelLine;
   private @Nullable Text nameSrc;
   private int nameAvail = -1;
   private int nameGen = -1;
   private BarText.@Nullable Line nameLine;
   private @Nullable Text widthSrc;
   private int widthGen = -1;
   private int nameFull;
   private BarText.@Nullable Line titleSrc;
   private int titleAvail = -1;
   private BarText.@Nullable Line titleLine;

   public BarAnim() {
      for (int i = 0; i < 6; i++) {
         this.pops[i] = new BarAnim.Pop();
      }
   }

   public void accept(BossStatePayload p, float now) {
      BossStatePayload old = this.state;
      this.state = p;
      this.stateAt = now;
      if (old != null && old.boss().equals(p.boss())) {
         float maxHp = p.maxHealth() * p.hpScale();
         float lost = (old.health() - p.health()) * p.hpScale();
         if (lost > 0.05F) {
            this.hit(lost, p.maxHealth() > 0.0F ? p.health() / p.maxHealth() : 0.0F, maxHp, now, false);
         } else if (p.health() > old.health() + 0.01F) {
            this.healAt = now;
         }

         float warded = (old.shield() - p.shield()) * p.hpScale();
         if (warded > 0.05F && p.shield() > 0.0F) {
            this.hit(warded, p.maxHealth() > 0.0F ? p.health() / p.maxHealth() : 0.0F, maxHp, now, true);
         }

         if (p.phase() > old.phase()) {
            this.bannerPhase = p.phase();
            this.bannerAt = now;
         }

         if (p.staggered() && !old.staggered()) {
            this.staggerAt = now;
         }

         if (p.shield() > 0.0F && old.shield() <= 0.0F) {
            this.wardAt = now;
         }
      }
   }

   private void hit(float amount, float frac, float maxHp, float now, boolean ward) {
      BarAnim.Pop last = this.lastPop >= 0 ? this.pops[this.lastPop] : null;
      if (last != null && last.alive(now) && last.ward == ward && now - last.at < 8.0F && now - last.bornAt < 18.0F) {
         last.amount += amount;
         last.at = now;
         last.frac = frac;
         last.big = last.big | last.amount >= maxHp * 0.04F;
         last.line = BarText.of("-" + BarText.group(Math.max(1, Math.round(last.amount))));
      } else {
         int i = (this.lastPop + 1) % 6;
         BarAnim.Pop pop = this.pops[i];

         for (BarAnim.Pop o : this.pops) {
            if (o != pop && o.alive(now) && o.bornAt < now) {
               o.fadeAt = Math.min(o.fadeAt, now);
            }
         }

         pop.bornAt = now;
         pop.at = now;
         pop.fadeAt = Float.MAX_VALUE;
         pop.amount = amount;
         pop.frac = frac;
         pop.dx = (this.random() - 0.5F) * 28.0F;
         pop.ward = ward;
         pop.big = amount >= maxHp * 0.04F;
         pop.line = BarText.of("-" + BarText.group(Math.max(1, Math.round(amount))));
         this.lastPop = i;
      }
   }

   public int newest() {
      return this.lastPop;
   }

   private float random() {
      int x = this.seed;
      x ^= x << 13;
      x ^= x >>> 17;
      x ^= x << 5;
      this.seed = x;
      return (x >>> 8) / 1.6777216E7F;
   }

   public void frame(float now, float progress, int phase, BarStyle style) {
      if (Float.isNaN(this.bornAt)) {
         this.bornAt = now;
      }

      float dt = this.shown < 0.0F ? 0.0F : MathHelper.clamp(now - this.lastFrame, 0.0F, 10.0F);
      this.lastFrame = now;
      this.target = progress;
      if (this.shown < 0.0F) {
         this.shown = progress;
         this.chip = progress;
         this.fillPhase = phase;
         this.prevFillPhase = phase;
      } else {
         if (progress < this.shown) {
            this.chip = Math.max(this.chip, this.shown);
            this.chipAt = now;
            this.shown = progress;
         } else if (progress > this.shown) {
            if (now - this.healAt < 30.0F) {
               this.shown = Math.min(progress, this.shown + dt * Math.max(0.004F, (progress - this.shown) * 0.12F));
            } else {
               this.shown = progress;
            }
         }

         if (this.chip > this.shown && now - this.chipAt > 14.0F) {
            this.chip = Math.max(this.shown, this.chip - dt * (0.004F + (this.chip - this.shown) * 0.1F));
         }

         if (this.chip < this.shown) {
            this.chip = this.shown;
         }
      }

      if (phase != this.fillPhase) {
         this.prevFillPhase = this.fillPhase;
         this.fillPhase = phase;
         this.fillPhaseAt = now;
      }

      BossStatePayload st = this.state;
      boolean casting = st != null && !st.cast().isEmpty() && st.castLength() > 0;
      if (casting) {
         int gen = BarText.generation();
         if (!st.cast().equals(this.castAnim) || this.castName == null || gen != this.castGen) {
            this.castAnim = st.cast();
            this.castName = BarText.move(style, this.castAnim);
            this.castGen = gen;
         }

         this.castProgress = MathHelper.clamp((st.castTick() + (now - this.stateAt)) / st.castLength(), 0.0F, 1.0F);
      }

      this.castAlpha = approach(this.castAlpha, casting ? 1.0F : 0.0F, dt * (casting ? 0.25F : 0.1F));
      if (st != null && st.poise() >= 0.0F) {
         this.poiseShown = this.poiseShown < 0.0F ? st.poise() : approach(this.poiseShown, st.poise(), dt * 0.06F);
      } else {
         this.poiseShown = -1.0F;
      }
   }

   private static float approach(float from, float to, float step) {
      return from < to ? Math.min(to, from + step) : Math.max(to, from - step);
   }

   public float open(float now) {
      float t = MathHelper.clamp((now - this.bornAt) / 15.0F, 0.0F, 1.0F);
      return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
   }

   public BarText.Line hp(int hp, int max) {
      if (this.hpLine == null || hp != this.hpValue || max != this.hpMax) {
         this.hpValue = hp;
         this.hpMax = max;
         this.hpLine = BarText.of(BarText.group(hp) + " / " + BarText.group(max));
      }

      return this.hpLine;
   }

   public BarText.Line percent(float fraction) {
      int key = Math.round(fraction * 1000.0F);
      if (this.pctLine == null || key != this.pctKey) {
         this.pctKey = key;
         this.pctLine = BarText.of(BarText.percent(fraction));
      }

      return this.pctLine;
   }

   public BarText.Line ward(int value) {
      if (this.wardLine == null || value != this.wardValue) {
         this.wardValue = value;
         this.wardLine = BarText.of(BarText.group(value));
      }

      return this.wardLine;
   }

   public BarText.Line level(int level) {
      int gen = BarText.generation();
      if (this.levelLine == null || level != this.levelValue || gen != this.levelGen) {
         this.levelValue = level;
         this.levelGen = gen;
         this.levelLine = BarText.level(level);
      }

      return this.levelLine;
   }

   public int nameWidth(Text name) {
      int gen = BarText.generation();
      if (name != this.widthSrc || gen != this.widthGen) {
         this.widthSrc = name;
         this.widthGen = gen;
         this.nameFull = MinecraftClient.getInstance().textRenderer.getWidth(name);
      }

      return this.nameFull;
   }

   public BarText.Line name(Text name, int avail) {
      int gen = BarText.generation();
      if (this.nameLine == null || name != this.nameSrc || avail != this.nameAvail || gen != this.nameGen) {
         this.nameSrc = name;
         this.nameAvail = avail;
         this.nameGen = gen;
         this.nameLine = BarText.fit(name, avail);
      }

      return this.nameLine;
   }

   public BarText.@Nullable Line title(BarStyle style, int avail) {
      BarText.Line full = BarText.title(style);
      if (full != null && full.width() > avail) {
         if (this.titleLine == null || full != this.titleSrc || avail != this.titleAvail) {
            this.titleSrc = full;
            this.titleAvail = avail;
            this.titleLine = BarText.fit(style.titleText, avail);
         }

         return this.titleLine;
      } else {
         return full;
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class Pop {
      public float bornAt = -1000.0F;
      public float at = -1000.0F;
      public float fadeAt = Float.MAX_VALUE;
      public float amount;
      public float frac;
      public float dx;
      public boolean big;
      public boolean ward;
      public BarText.@Nullable Line line;

      public boolean alive(float now) {
         return this.line != null && now - this.bornAt < 28.0F && now - this.fadeAt < 6.0F;
      }
   }
}

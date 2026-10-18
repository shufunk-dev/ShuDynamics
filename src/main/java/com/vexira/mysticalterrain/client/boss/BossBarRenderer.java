package com.vexira.mysticalterrain.client.boss;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.net.BossStatePayload;
import com.vexira.mysticalterrain.client.boss.bar.BarAnim;
import com.vexira.mysticalterrain.client.boss.bar.BarStyle;
import com.vexira.mysticalterrain.client.boss.bar.BarText;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class BossBarRenderer {
   private static final Identifier ATLAS = MysticalTerrain.id("textures/gui/boss_bar/atlas.png");
   private static final int TEX_W = 512;
   private static final int TEX_H = 256;
   private static final int FULL_W = 512;
   private static final int FULL_H = 118;
   private static final int FRAME_Y = 36;
   private static final int TRACK_X = 80;
   private static final int TRACK_Y = 56;
   private static final int TRACK_W = 400;
   private static final int TRACK_H = 20;
   private static final int EMBLEM_X = 2;
   private static final int EMBLEM_Y = 26;
   private static final int BADGE_X = 6;
   private static final int BADGE_Y = 94;
   private static final int HEAD_X = 96;
   private static final int HEAD_RIGHT = 468;
   private static final int TITLE_Y = 28;
   private static final int CHIP_Y = 24;
   private static final int TITLE_ROOM = 80;
   private static final int[] CHIP_LABEL_DROP = new int[]{0, 2, 1};
   private static final int POISE_X = 132;
   private static final int POISE_Y = 86;
   private static final int CAST_X = 112;
   private static final int COMPACT_W = 416;
   private static final int COMPACT_H = 50;
   private static final int C_FRAME_X = 16;
   private static final int C_FRAME_Y = 14;
   private static final int C_TRACK_X = 36;
   private static final int C_TRACK_Y = 24;
   private static final int C_TRACK_W = 352;
   private static final int C_TRACK_H = 12;
   private static final int GAP = 4;
   private static final int FLAG_ENRAGED = 1;
   private static final int FLAG_INVULNERABLE = 2;
   private static final int FLAG_BUFFED = 4;
   private static final int FLAG_MARKED = 8;
   private static final int FLAG_TELEGRAPH = 16;
   private static final int ENRAGED = 16732208;
   private static final int MARKED = 16748608;
   private static final int INVULNERABLE = 11059416;
   private static final int BUFFED = 7397504;
   private static final int HEAL = 7405456;
   private static final int WARD = 10479359;
   private static final int POISE = 16756800;
   private static final int LOW = 16724000;
   private static final int WARNING = 16760880;
   private static final int NUMBER = 16774360;
   private static final int NUMBER_BIG = 16754744;
   private static final int ICON_ENRAGED = 0;
   private static final int ICON_TARGET = 1;
   private static final int ICON_INVULN = 2;
   private static final int ICON_BUFF = 3;
   private static final int ICON_WARNING = 4;
   private static final int ICON_WARD = 6;

   private BossBarRenderer() {
   }

   public static boolean isStyled(LerpingBossEvent event) {
      return BossBarState.state(event.getId()) != null || keyStyle(event) != null;
   }

   private static @Nullable BarStyle keyStyle(LerpingBossEvent event) {
      return event.getName().getContents() instanceof TranslatableContents t ? BarStyle.byKey(t.getKey()) : null;
   }

   private static BarStyle style(LerpingBossEvent event, @Nullable BossStatePayload st) {
      if (st != null) {
         return BarStyle.byId(st.boss());
      }

      BarStyle style = keyStyle(event);
      return style != null ? style : BarStyle.GENERIC;
   }

   private static int thresholdCount(@Nullable BossStatePayload st, BarStyle style) {
      return st != null ? st.thresholds().size() : style.phases.length;
   }

   private static float threshold(@Nullable BossStatePayload st, BarStyle style, int i) {
      return st != null ? st.thresholds().get(i) : style.phases[i];
   }

   private static int phase(@Nullable BossStatePayload st, BarStyle style, float progress) {
      if (st != null) {
         return Math.max(0, st.phase());
      }

      int phase = 0;

      for (float t : style.phases) {
         if (progress <= t) {
            phase++;
         }
      }

      return phase;
   }

   public static int draw(GuiGraphicsExtractor g, LerpingBossEvent event, int y, boolean compact) {
      Minecraft mc = Minecraft.getInstance();
      BarAnim a = BossBarState.bar(event.getId());
      BossStatePayload st = a.state;
      BarStyle style = style(event, st);
      float now = BossBarState.now();
      float progress = Mth.clamp(event.getProgress(), 0.0F, 1.0F);
      a.frame(now, progress, phase(st, style, progress), style);
      int w = compact ? 416 : 512;
      int h = compact ? 50 : fullHeight(a);
      float s = 0.5F * Math.min(1.0F, (g.guiWidth() - 8) / (w * 0.5F));
      int x = Math.round((g.guiWidth() - w * s) / 2.0F);
      Matrix3x2fStack pose = g.pose();
      pose.pushMatrix();
      pose.translate(x, y);
      pose.scale(s, s);
      float open = a.open(now);
      int half = Math.round((w / 2 + 24) * open);
      if (open < 1.0F) {
         g.enableScissor(w / 2 - half, -40, w / 2 + half, h + 60);
      }

      if (compact) {
         drawCompact(g, mc.font, event, a, st, style, now);
      } else {
         drawFull(g, mc.font, event, a, st, style, now, h);
      }

      if (open < 1.0F) {
         g.disableScissor();
         int top = compact ? 18 : 42;
         int bottom = compact ? 42 : 86;
         int seam = ARGB.color(1.0F - open, style.mono ? -1 : ARGB.srgbLerp(0.5F, style.accent, -1));
         g.fill(w / 2 - half - 3, top, w / 2 - half + 1, bottom, seam);
         g.fill(w / 2 + half - 1, top, w / 2 + half + 3, bottom, seam);
      }

      pose.popMatrix();
      return Math.round((h + 4) * s);
   }

   private static int fullHeight(BarAnim a) {
      int castBottom = (a.poiseShown >= 0.0F ? 102 : 88) + 30;
      return Math.round(118.0F + Math.max(0, castBottom - 118) * a.castAlpha);
   }

   private static void drawFull(
      GuiGraphicsExtractor g, Font font, LerpingBossEvent event, BarAnim a, @Nullable BossStatePayload st, BarStyle style, float now, int h
   ) {
      int flags = st != null ? st.flags() : 0;
      stretch(g, ATLAS, -24, -8, 560, h + 16, 336, 88, 64, 32, -1);
      sprite(g, style.sheet, 0, 36, 0, 0, 512, 64, -1);
      track(g, a, st, style, now, 80, 56, 400, 20, false);
      trackText(g, font, a, st, style);
      if ((flags & 1) != 0) {
         float p = 0.55F + 0.45F * Mth.sin(now * 0.3F);
         stretch(g, ATLAS, -20, 4, 124, 124, 80, 0, 48, 48, ARGB.color(0.85F * p, style.tint(16732208)));
      }

      sprite(g, style.sheet, 2, 26, 416, 64, 80, 80, -1);
      sprite(g, style.sheet, 6, 94, 440, 144, 72, 22, -1);
      BarText.Line level = a.level(st != null ? st.level() : 0);
      text(g, font, level, 42 - level.width(), 98, style.tint(15260848), 1.0F, true);
      header(g, font, event, a, st, style, now, flags);
      poise(g, font, a, st, style, now);
      cast(g, font, a, style, now, flags);
      banner(g, font, a, style, now);
      pops(g, font, a, style, now);
   }

   private static void header(
      GuiGraphicsExtractor g, Font font, LerpingBossEvent event, BarAnim a, @Nullable BossStatePayload st, BarStyle style, float now, int flags
   ) {
      int count = thresholdCount(st, style) + 1;
      int pipsLeft = 468;
      int phase = Math.max(0, a.fillPhase);
      if (count > 1) {
         int n = Math.min(count, 6);
         pipsLeft = 468 - n * 22 + 4;
         int current = Math.min(phase, n - 1);

         for (int i = 0; i < n; i++) {
            int px = pipsLeft + i * 22;
            if (i == current) {
               float p = 0.6F + 0.4F * Mth.sin(now * 0.2F);
               stretch(g, ATLAS, px - 9, -5, 36, 36, 80, 0, 48, 48, ARGB.color(0.8F * p, style.accent));
            }

            sprite(g, style.sheet, px, 4, i <= phase ? 440 : 458, 168, 18, 18, -1);
         }
      }

      int chipX = chips(g, font, style, now, flags);
      BarText.Line title = a.title(style, (chipX - 96 - 8) / 2);
      float ns = crisp(1.5F);
      if (ns > 1.0F && a.nameWidth(event.getName()) * ns > (pipsLeft - 96 - 8) / 2.0F) {
         ns = 1.0F;
      }

      int avail = (int)((pipsLeft - 96 - 8) / (2.0F * ns));
      BarText.Line name = a.name(event.getName(), avail);
      int nameH = Math.round(18.0F * ns);
      int nameY = title != null ? Math.max(0, 26 - nameH) : Math.max(0, (46 - nameH) / 2);
      text(g, font, name, 96, nameY & -2, style.nameColor, ns, true);
      if (title != null) {
         text(g, font, title, 96, 28, style.titleColor, 1.0F, true);
      }
   }

   private static int chips(GuiGraphicsExtractor g, Font font, BarStyle style, float now, int flags) {
      BarText.Line[] labels = new BarText.Line[3];
      if ((flags & 2) != 0) {
         labels[0] = BarText.ui("gui.mysticalterrain.bossbar.invulnerable", "INVULNERABLE");
      }

      if ((flags & 8) != 0) {
         labels[1] = BarText.ui("gui.mysticalterrain.bossbar.marked", "TARGETED");
      }

      if ((flags & 1) != 0) {
         labels[2] = BarText.ui("gui.mysticalterrain.bossbar.enraged", "ENRAGED");
      }

      boolean buffed = (flags & 4) != 0;
      int room = 284;
      int dropped = 0;

      while (dropped < CHIP_LABEL_DROP.length && chipRow(labels, dropped, buffed) > room) {
         dropped++;
      }

      int x = 468;
      if (labels[0] != null) {
         x = chip(g, font, x, 2, chipLabel(labels, 0, dropped), style.tint(11059416), 1.0F);
      }

      if (labels[1] != null) {
         float p = 0.65F + 0.35F * Mth.sin(now * 0.5F);
         x = chip(g, font, x, 1, chipLabel(labels, 1, dropped), style.tint(16748608), p);
      }

      if (labels[2] != null) {
         x = chip(g, font, x, 0, chipLabel(labels, 2, dropped), style.tint(16732208), 1.0F);
      }

      if (buffed) {
         x -= 20;
         sprite(g, ATLAS, x, 26, 176, 0, 16, 16, style.tint(7397504));
         x -= 4;
      }

      return x;
   }

   private static BarText.@Nullable Line chipLabel(BarText.@Nullable Line[] labels, int i, int dropped) {
      for (int k = 0; k < dropped; k++) {
         if (CHIP_LABEL_DROP[k] == i) {
            return null;
         }
      }

      return labels[i];
   }

   private static int chipRow(BarText.@Nullable Line[] labels, int dropped, boolean buffed) {
      int w = buffed ? 24 : 0;

      for (int i = 0; i < labels.length; i++) {
         if (labels[i] != null) {
            w += chipWidth(chipLabel(labels, i, dropped)) + 4;
         }
      }

      return w;
   }

   private static int chipWidth(BarText.@Nullable Line label) {
      return label != null ? 26 + label.width() * 2 + 6 : 28;
   }

   private static int chip(GuiGraphicsExtractor g, Font font, int right, int icon, BarText.@Nullable Line label, int color, float alpha) {
      int w = chipWidth(label);
      int x = right - w;
      int plate = ARGB.color(alpha, color);
      sprite(g, ATLAS, x, 24, 240, 0, 6, 20, plate);
      stretch(g, ATLAS, x + 6, 24, w - 12, 20, 246, 0, 4, 20, plate);
      sprite(g, ATLAS, x + w - 6, 24, 250, 0, 6, 20, plate);
      sprite(g, ATLAS, label != null ? x + 5 : x + 6, 26, 128 + icon * 16, 0, 16, 16, plate);
      if (label != null) {
         text(g, font, label, x + 24, 26, ARGB.color(alpha, ARGB.srgbLerp(0.6F, color, -1)), 1.0F, true);
      }

      return x - 4;
   }

   private static void trackText(GuiGraphicsExtractor g, Font font, BarAnim a, @Nullable BossStatePayload st, BarStyle style) {
      int y = 58;
      BarText.Line pct = a.percent(a.target);
      outlined(g, font, pct, 472 - pct.width() * 2, y, -1, -805306368, 1.0F);
      if (st != null && !(st.maxHealth() <= 0.0F)) {
         float scale = st.hpScale() > 0.0F ? st.hpScale() : 1.0F;
         int max = Math.round(st.maxHealth() * scale);
         BarText.Line hp = a.hp(Math.round(a.target * st.maxHealth() * scale), max);
         outlined(g, font, hp, 280 - hp.width(), y, -1, -805306368, 1.0F);
         if (st.shield() > 0.0F) {
            int ward = style.tint(10479359);
            BarText.Line value = a.ward(Math.round(st.shield() * scale));
            sprite(g, ATLAS, 84, 58, 224, 0, 16, 16, ward);
            outlined(g, font, value, 102, y, ARGB.srgbLerp(0.5F, ward, -1), -805306368, 1.0F);
         }
      }
   }

   private static void poise(GuiGraphicsExtractor g, Font font, BarAnim a, @Nullable BossStatePayload st, BarStyle style, float now) {
      if (!(a.poiseShown < 0.0F) && st != null) {
         sprite(g, ATLAS, 132, 86, 0, 68, 296, 12, -1);
         boolean broken = st.staggered();
         int color = style.tint(16756800);
         if (broken && (int)(now / 3.0F) % 2 == 0) {
            color = -1;
         }

         int pw = Math.round(Mth.clamp(a.poiseShown, 0.0F, 1.0F) * 288.0F);
         if (pw > 0) {
            sprite(g, ATLAS, 136, 89, 0, 80, pw, 6, color);
         }

         float flash = 1.0F - (now - a.staggerAt) / 12.0F;
         if (flash > 0.0F) {
            g.fill(134, 87, 426, 97, ARGB.white(0.85F * flash));
         }

         if (broken) {
            BarText.Line label = BarText.ui("gui.mysticalterrain.bossbar.staggered", "STAGGERED");
            float s = 1.0F + 0.5F * Math.max(0.0F, 1.0F - (now - a.staggerAt) / 6.0F);
            int cx = 280;
            int cy = 92;
            outlined(g, font, label, cx - Math.round(label.width() * s), cy - Math.round(8.0F * s), style.tint(16769696), -536870912, s);
         }
      }
   }

   private static void cast(GuiGraphicsExtractor g, Font font, BarAnim a, BarStyle style, float now, int flags) {
      BarText.Line name = a.castName;
      float al = a.castAlpha;
      if (!(al <= 0.01F) && name != null) {
         int y = a.poiseShown >= 0.0F ? 102 : 88;
         sprite(g, ATLAS, 112, y, 0, 88, 336, 28, ARGB.white(al));
         int fw = Math.round(a.castProgress * 300.0F);
         int tint = style.mono ? -1 : ARGB.srgbLerp(0.35F, style.accent, -1);
         if (fw > 0) {
            sprite(g, ATLAS, 140, y + 6, 0, 116, fw, 16, ARGB.color(al, tint));
            if (fw < 300) {
               g.fill(140 + Math.max(0, fw - 2), y + 6, 140 + fw, y + 22, ARGB.white(0.9F * al));
            }
         }

         boolean telegraph = (flags & 16) != 0;
         if (telegraph) {
            float p = 0.5F + 0.5F * Mth.sin(now * 1.2F);
            g.fill(140, y + 6, 440, y + 22, ARGB.color(0.3F * p * al, style.tint(16724000)));
            stretch(g, ATLAS, 106, y - 6, 40, 40, 80, 0, 48, 48, ARGB.color(0.8F * al, style.tint(16760880)));
         }

         float blink = telegraph ? 1.0F : 0.6F + 0.4F * Mth.sin(now * 0.6F);
         sprite(g, ATLAS, 118, y + 6, 192, 0, 16, 16, ARGB.color(al * blink, style.tint(16760880)));
         outlined(g, font, name, 290 - name.width(), y + 4, ARGB.white(al), ARGB.color(0.85F * al, 0), 1.0F);
      }
   }

   private static void banner(GuiGraphicsExtractor g, Font font, BarAnim a, BarStyle style, float now) {
      float t = now - a.bannerAt;
      if (!(t < 0.0F) && !(t > 50.0F)) {
         float alpha;
         float off;
         if (t < 8.0F) {
            float k = ease(t / 8.0F);
            alpha = k;
            off = (1.0F - k) * -140.0F;
         } else if (t < 40.0F) {
            alpha = 1.0F;
            off = (t - 8.0F) * 0.25F;
         } else {
            float k = (t - 40.0F) / 10.0F;
            alpha = 1.0F - k;
            off = 8.0F + k * k * 140.0F;
         }

         int cx = 280 + Math.round(off);
         int cy = 66;
         sprite(g, ATLAS, cx - 160, cy - 18, 0, 132, 320, 36, ARGB.color(alpha, style.accent));
         BarText.Line line = BarText.banner(a.bannerPhase);
         float s = crisp(1.5F);
         int glow = ARGB.color(0.9F * alpha, ARGB.scaleRGB(style.accent, 0.35F));
         outlined(g, font, line, cx - Math.round(line.width() * s), cy - Math.round(7.0F * s) & -2, ARGB.white(alpha), glow, s);
      }
   }

   private static float bannerAlpha(BarAnim a, float now) {
      float t = now - a.bannerAt;
      if (!(t < 0.0F) && !(t > 50.0F)) {
         return t < 8.0F ? ease(t / 8.0F) : (t < 40.0F ? 1.0F : 1.0F - (t - 40.0F) / 10.0F);
      } else {
         return 0.0F;
      }
   }

   private static void pops(GuiGraphicsExtractor g, Font font, BarAnim a, BarStyle style, float now) {
      float big = crisp(1.5F);
      float under = 1.0F - bannerAlpha(a, now);
      if (!(under <= 0.01F)) {
         int n = a.pops.length;
         BarText.Line pct = a.percent(a.target);
         int right = 472 - pct.width() * 2 - 6;

         for (int k = 1; k <= n; k++) {
            BarAnim.Pop p = a.pops[(a.newest() + k) % n];
            BarText.Line line = p.line;
            if (line != null && p.alive(now)) {
               float age = now - p.bornAt;
               float life = age / 28.0F;
               float rise = 1.0F - (1.0F - life) * (1.0F - life);
               float punch = Math.max(0.0F, 1.0F - (now - p.at) / 5.0F);
               float s = (p.big ? big : 1.0F) * (1.0F + 0.3F * punch);
               float alpha = (life > 0.7F ? (1.0F - life) / 0.3F : 1.0F) * under;
               if (now > p.fadeAt) {
                  alpha *= Math.max(0.0F, 1.0F - (now - p.fadeAt) / 6.0F);
               }

               int color = style.tint(p.ward ? 10479359 : (p.big ? 16754744 : 16774360));
               if (punch > 0.0F) {
                  color = ARGB.srgbLerp(punch * 0.8F, color, -1);
               }

               int halfW = Math.round(line.width() * s) + 4;
               int x = 80 + Math.round(p.frac * 400.0F + p.dx + age * 0.6F);
               int lo = 80 + halfW;
               x = Math.clamp(x, lo, Math.max(lo, right - halfW));
               int y = 66 - Math.round(rise * 6.0F);
               int th = Math.round(16.0F * s);
               plate(g, x - halfW - 2, y - th / 2 - 3, 2 * halfW + 4, th + 5, ARGB.color(0.82F * alpha, 2760218));
               outlined(
                  g,
                  font,
                  line,
                  x - Math.round(line.width() * s),
                  y - Math.round(8.0F * s),
                  ARGB.color(alpha, color),
                  ARGB.color(0.9F * alpha, 0),
                  s
               );
            }
         }
      }
   }

   private static void drawCompact(GuiGraphicsExtractor g, Font font, LerpingBossEvent event, BarAnim a, @Nullable BossStatePayload st, BarStyle style, float now) {
      int flags = st != null ? st.flags() : 0;
      stretch(g, ATLAS, -16, -6, 448, 58, 336, 88, 64, 32, -1);
      sprite(g, style.sheet, 16, 14, 0, 144, 400, 32, -1);
      track(g, a, st, style, now, 36, 24, 352, 12, true);
      if ((flags & 1) != 0) {
         float p = 0.55F + 0.45F * Mth.sin(now * 0.3F);
         stretch(g, ATLAS, -12, -2, 64, 64, 80, 0, 48, 48, ARGB.color(0.85F * p, style.tint(16732208)));
      }

      sprite(g, style.sheet, 0, 10, 400, 144, 40, 40, -1);
      int right = 404;
      int count = thresholdCount(st, style) + 1;
      int phase = Math.max(0, a.fillPhase);
      if (count > 1) {
         int n = Math.min(count, 6);

         for (int i = n - 1; i >= 0; i--) {
            right -= 14;
            stretch(g, style.sheet, right, 3, 12, 12, i <= phase ? 440 : 458, 168, 18, 18, -1);
         }

         right -= 4;
      }

      BarText.Line pct = a.percent(a.target);
      right -= pct.width() * 2;
      text(g, font, pct, right, 0, -1513240, 1.0F, true);
      right -= 4;
      right = icon(g, right, flags, 2, 2, style.tint(11059416));
      right = icon(g, right, flags, 8, 1, style.tint(16748608));
      right = icon(g, right, flags, 1, 0, style.tint(16732208));
      right = icon(g, right, flags, 4, 3, style.tint(7397504));
      BarText.Line name = a.name(event.getName(), (right - 44 - 6) / 2);
      text(g, font, name, 44, 0, style.nameColor, 1.0F, true);
   }

   private static int icon(GuiGraphicsExtractor g, int right, int flags, int flag, int icon, int color) {
      if ((flags & flag) == 0) {
         return right;
      }

      sprite(g, ATLAS, right - 16, 1, 128 + icon * 16, 0, 16, 16, color);
      return right - 18;
   }

   private static void track(
      GuiGraphicsExtractor g, BarAnim a, @Nullable BossStatePayload st, BarStyle style, float now, int tx, int ty, int tw, int th, boolean compact
   ) {
      int fw = Math.round(Mth.clamp(a.shown, 0.0F, 1.0F) * tw);
      int phase = Math.clamp(a.fillPhase, 0, 3);
      int prev = Math.clamp(a.prevFillPhase, 0, 3);
      float mix = Mth.clamp((now - a.fillPhaseAt) / 16.0F, 0.0F, 1.0F);
      if (fw > 0) {
         if (mix < 1.0F && prev != phase) {
            fillStrip(g, style.sheet, tx, ty, fw, th, tw, prev, -1);
            fillStrip(g, style.sheet, tx, ty, fw, th, tw, phase, ARGB.white(mix));
         } else {
            fillStrip(g, style.sheet, tx, ty, fw, th, tw, phase, -1);
         }
      }

      int target = Math.round(Mth.clamp(a.target, 0.0F, 1.0F) * tw);
      if (target > fw) {
         float p = 0.7F + 0.3F * Mth.sin(now * 0.8F);
         g.fill(tx + fw, ty, tx + target, ty + th, ARGB.color(0.75F * p, style.tint(7405456)));
         g.fill(tx + fw, ty, tx + target, ty + th / 3, ARGB.white(0.35F * p));
      }

      int cw = Math.round(Mth.clamp(a.chip, 0.0F, 1.0F) * tw);
      if (cw > fw) {
         float age = now - a.chipAt;
         int col = age < 3.0F ? -1 : ARGB.srgbLerp(Mth.clamp((age - 3.0F) / 8.0F, 0.0F, 1.0F), -1, style.chip);
         g.fill(tx + fw, ty, tx + cw, ty + th, ARGB.color(0.92F, col));
         g.fill(tx + fw, ty + th - th / 4, tx + cw, ty + th, 939524096);
      }

      float cycle = now % 100.0F / 45.0F;
      if (cycle < 1.0F && fw > 16) {
         int sx = Math.round(-64.0F + cycle * (fw + 64));
         int l = Math.max(sx, 0);
         int r = Math.min(sx + 64, fw);
         if (r > l) {
            stretch(g, ATLAS, tx + l, ty, r - l, th, l - sx, 0, r - l, 20, ARGB.white(0.85F));
         }
      }

      if (st != null && st.shield() > 0.0F && st.shieldMax() > 0.0F) {
         int ward = style.tint(10479359);
         int ww = Math.round(Mth.clamp(st.shield() / st.shieldMax(), 0.0F, 1.0F) * tw);
         int scroll = 31 - Math.floorMod((int)(now * 0.8F), 32);
         if (ww > 0) {
            stretch(g, ATLAS, tx, ty, ww, th, scroll, 48, ww, 20, ARGB.color(0.55F, ward));
            g.fill(tx + Math.max(0, ww - 2), ty, tx + ww, ty + th, ARGB.white(0.9F));
         }

         float f = 1.0F - (now - a.wardAt) / 10.0F;
         if (f > 0.0F) {
            g.fill(tx, ty, tx + tw, ty + th, ARGB.color(0.6F * f, ward));
         }
      }

      int flags = st != null ? st.flags() : 0;
      if ((flags & 2) != 0) {
         g.fill(tx, ty, tx + tw, ty + th, ARGB.color(0.45F, style.tint(11056320)));
      }

      if (a.shown > 0.0F && a.shown < 0.2F) {
         float p = 0.5F + 0.5F * Mth.sin(now * 0.45F);
         if (fw > 0) {
            g.fill(tx, ty, tx + fw, ty + th, ARGB.white(0.22F * p));
         }

         int low = ARGB.color(0.75F * p, style.tint(16724000));
         g.fill(tx - 3, ty - 3, tx + tw + 3, ty - 1, low);
         g.fill(tx - 3, ty + th + 1, tx + tw + 3, ty + th + 3, low);
         g.fill(tx - 3, ty - 1, tx - 1, ty + th + 1, low);
         g.fill(tx + tw + 1, ty - 1, tx + tw + 3, ty + th + 1, low);
      }

      stretch(g, ATLAS, tx, ty, tw, th, 64, 0, 4, 20, -1);
      float flash = 1.0F - (now - a.bannerAt) / 12.0F;
      if (flash > 0.0F) {
         g.fill(tx, ty, tx + tw, ty + th, ARGB.white(0.7F * flash));
      }

      int n = thresholdCount(st, style);
      int lit = style.mono ? -1 : ARGB.srgbLerp(0.5F, style.accent, -1);

      for (int i = 0; i < n; i++) {
         float t = threshold(st, style, i);
         if (!(t <= 0.0F) && !(t >= 1.0F)) {
            int nx = tx + Math.round(t * tw);
            int col = a.target <= t ? -9803158 : lit;
            if (compact) {
               stretch(g, ATLAS, nx - 3, ty - 3, 6, th + 6, 72, 0, 8, 28, col);
            } else {
               sprite(g, ATLAS, nx - 4, ty - 4, 72, 0, 8, 28, col);
            }
         }
      }
   }

   private static void fillStrip(GuiGraphicsExtractor g, Identifier sheet, int x, int y, int w, int h, int trackW, int phase, int color) {
      int uw = trackW == 400 ? w : Math.max(1, Math.round(w * 400.0F / trackW));
      stretch(g, sheet, x, y, w, h, 0, 64 + phase * 20, uw, 20, color);
   }

   private static void sprite(GuiGraphicsExtractor g, Identifier tex, int x, int y, int u, int v, int w, int h, int color) {
      if (w > 0 && h > 0) {
         g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, w, h, 512, 256, color);
      }
   }

   private static void plate(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
      stretch(g, ATLAS, x, y, 6, h, 240, 0, 6, 20, color);
      stretch(g, ATLAS, x + 6, y, w - 12, h, 246, 0, 4, 20, color);
      stretch(g, ATLAS, x + w - 6, y, 6, h, 250, 0, 6, 20, color);
   }

   private static void stretch(GuiGraphicsExtractor g, Identifier tex, int x, int y, int w, int h, int u, int v, int uw, int vh, int color) {
      if (w > 0 && h > 0) {
         g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, uw, vh, 512, 256, color);
      }
   }

   private static void text(GuiGraphicsExtractor g, Font font, BarText.Line line, int x, int y, int color, float scale, boolean shadow) {
      Matrix3x2fStack pose = g.pose();
      pose.pushMatrix();
      pose.translate(x, y);
      pose.scale(2.0F * scale, 2.0F * scale);
      g.text(font, line.text(), 0, 0, color, shadow);
      pose.popMatrix();
   }

   private static void outlined(GuiGraphicsExtractor g, Font font, BarText.Line line, int x, int y, int color, int outline, float scale) {
      Matrix3x2fStack pose = g.pose();
      pose.pushMatrix();
      pose.translate(x, y);
      pose.scale(2.0F * scale, 2.0F * scale);
      g.text(font, line.text(), -1, 0, outline, false);
      g.text(font, line.text(), 1, 0, outline, false);
      g.text(font, line.text(), 0, -1, outline, false);
      g.text(font, line.text(), 0, 1, outline, false);
      g.text(font, line.text(), 0, 0, color, false);
      pose.popMatrix();
   }

   private static float crisp(float target) {
      int gs = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
      return (float)Math.max(gs, (int)Math.floor(target * gs)) / gs;
   }

   private static float ease(float t) {
      float k = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
      return 1.0F - k * k * k;
   }
}

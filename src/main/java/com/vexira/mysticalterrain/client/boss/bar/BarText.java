package com.vexira.mysticalterrain.client.boss.bar;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class BarText {
   private static final String[] ROMAN = new String[]{"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
   private static final Map<String, BarText.Line> UI = new HashMap<>();
   private static final BarText.Line[] BANNERS = new BarText.Line[ROMAN.length];
   private static @Nullable Language language;
   private static int generation;

   private BarText() {
   }

   public static int generation() {
      Language now = Language.getInstance();
      if (now != language) {
         language = now;
         generation++;
         UI.clear();
         Arrays.fill(BANNERS, null);
      }

      return generation;
   }

   private static TextRenderer font() {
      return MinecraftClient.getInstance().textRenderer;
   }

   public static BarText.Line of(String text) {
      return new BarText.Line(OrderedText.styledForwardsVisitedString(text, Style.EMPTY), font().getWidth(text));
   }

   public static BarText.Line of(Text text) {
      OrderedText seq = text.asOrderedText();
      return new BarText.Line(seq, font().getWidth(seq));
   }

   public static BarText.Line fit(StringVisitable text, int width) {
      TextRenderer font = font();
      int w = font.getWidth(text);
      if (w <= width) {
         OrderedText seq = Language.getInstance().reorder(text);
         return new BarText.Line(seq, w);
      } else {
         StringVisitable cut = font.trimToWidth(text, Math.max(0, width - font.getWidth("...")));
         OrderedText seq = Language.getInstance().reorder(StringVisitable.concat(new StringVisitable[]{cut, StringVisitable.plain("...")}));
         return new BarText.Line(seq, font.getWidth(seq));
      }
   }

   public static BarText.Line ui(String key, String fallback) {
      generation();
      BarText.Line line = UI.get(key);
      if (line == null) {
         line = of(Text.translatableWithFallback(key, fallback));
         UI.put(key, line);
      }

      return line;
   }

   public static BarText.Line banner(int phase) {
      generation();
      int i = Math.max(0, Math.min(ROMAN.length - 1, phase));
      if (BANNERS[i] == null) {
         BANNERS[i] = of(Text.translatableWithFallback("gui.mysticalterrain.bossbar.phase_banner", "PHASE %s", new Object[]{ROMAN[i]}));
      }

      return BANNERS[i];
   }

   public static BarText.Line level(int level) {
      String value = level > 0 ? Integer.toString(level) : "??";
      return of(Text.translatableWithFallback("gui.mysticalterrain.bossbar.level", "Lv. %s", new Object[]{value}));
   }

   public static BarText.@Nullable Line title(BarStyle style) {
      sync(style);
      return style.title;
   }

   public static BarText.Line move(BarStyle style, String anim) {
      sync(style);
      BarText.Line line = style.moves.get(anim);
      if (line == null) {
         String key = style.movePrefix + anim;
         Text name = Language.getInstance().hasTranslation(key) ? Text.translatable(key) : Text.literal(pretty(anim));
         line = of(Text.translatableWithFallback("gui.mysticalterrain.bossbar.casting", "%s", new Object[]{name}));
         style.moves.put(anim, line);
      }

      return line;
   }

   private static void sync(BarStyle style) {
      int gen = generation();
      if (style.textGen != gen) {
         style.textGen = gen;
         style.moves.clear();
         style.titleText = Text.translatable(style.titleKey);
         style.title = Language.getInstance().hasTranslation(style.titleKey) ? of(style.titleText) : null;
      }
   }

   public static String pretty(String anim) {
      StringBuilder out = new StringBuilder(anim.length());
      boolean upper = true;

      for (int i = 0; i < anim.length(); i++) {
         char c = anim.charAt(i);
         if (c != '_' && c != '.' && c != '-') {
            if (Character.isDigit(c) && i > 0 && !Character.isDigit(anim.charAt(i - 1)) && out.charAt(out.length() - 1) != ' ') {
               out.append(' ').append(c);
               upper = false;
            } else {
               out.append(upper ? Character.toUpperCase(c) : c);
               upper = false;
            }
         } else {
            out.append(' ');
            upper = true;
         }
      }

      return out.toString();
   }

   public static String group(long value) {
      String digits = Long.toString(Math.abs(value));
      StringBuilder out = new StringBuilder(digits.length() + digits.length() / 3 + 1);
      if (value < 0L) {
         out.append('-');
      }

      int lead = digits.length() % 3;

      for (int i = 0; i < digits.length(); i++) {
         if (i > 0 && (i - lead) % 3 == 0) {
            out.append(',');
         }

         out.append(digits.charAt(i));
      }

      return out.toString();
   }

   public static String percent(float fraction) {
      float pct = Math.max(0.0F, fraction * 100.0F);
      if (!(pct >= 10.0F) && pct != 0.0F) {
         int tenths = Math.round(pct * 10.0F);
         return tenths / 10 + "." + tenths % 10 + "%";
      } else {
         return Math.round(pct) + "%";
      }
   }

   @Environment(EnvType.CLIENT)
   public record Line(OrderedText text, int width) {
   }
}

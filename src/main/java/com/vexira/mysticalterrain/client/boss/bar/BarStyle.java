package com.vexira.mysticalterrain.client.boss.bar;

import com.vexira.mysticalterrain.MysticalTerrain;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class BarStyle {
   private static final Map<String, BarStyle> BY_ID = new HashMap<>();
   private static final Map<String, BarStyle> BY_KEY = new HashMap<>();
   public final String id;
   public final Identifier sheet;
   public final int nameColor;
   public final int titleColor;
   public final int accent;
   public final int chip;
   public final boolean mono;
   public final float[] phases;
   final String titleKey;
   final String movePrefix;
   int textGen = -1;
   BarText.@Nullable Line title;
   Text titleText = Text.empty();
   final Map<String, BarText.Line> moves = new HashMap<>();
   public static final BarStyle GENERIC = new BarStyle("generic", 16748672, 16732240, 16769232, false, 0.66F, 0.33F);

   private BarStyle(String id, int nameColor, int accent, int chip, boolean mono, float... phases) {
      this.id = id;
      this.sheet = MysticalTerrain.id("textures/gui/boss_bar/" + id + ".png");
      this.nameColor = 0xFF000000 | nameColor;
      int muted = ColorHelper.scaleRgb(ColorHelper.lerp(0.55F, 0xFF000000 | nameColor, -7435642), 0.95F);
      this.titleColor = mono ? ColorHelper.grayscale(muted) : muted;
      this.accent = 0xFF000000 | accent;
      this.chip = 0xFF000000 | chip;
      this.mono = mono;
      this.phases = phases;
      this.titleKey = "boss.mysticalterrain." + id + ".title";
      this.movePrefix = "boss.mysticalterrain." + id + ".move.";
   }

   private static void add(String id, int nameColor, int accent, int chip, float... phases) {
      add(id, "entity.mysticalterrain." + id, nameColor, accent, chip, false, phases);
   }

   private static void add(String id, String key, int nameColor, int accent, int chip, boolean mono, float... phases) {
      BarStyle style = new BarStyle(id, nameColor, accent, chip, mono, phases);
      BY_ID.put(id, style);
      BY_KEY.put(key, style);
   }

   public static BarStyle byId(String id) {
      BarStyle style = BY_ID.get(id);
      return style != null ? style : GENERIC;
   }

   public static @Nullable BarStyle byKey(String translationKey) {
      return BY_KEY.get(translationKey);
   }

   public int tint(int rgb) {
      int c = 0xFF000000 | rgb;
      if (!this.mono) {
         return c;
      }

      int grey = ColorHelper.getRed(ColorHelper.grayscale(c));
      grey = Math.min(255, 90 + grey);
      return ColorHelper.getArgb(255, grey, grey, grey);
   }

   static {
      add("ender_dragon", "entity.minecraft.ender_dragon", 15921906, 16777215, 16777215, true, 0.7F, 0.4F, 0.15F);
      add("pyraxis", 16756832, 16747056, 16769184, 0.75F, 0.5F, 0.25F);
      add("hrimthane", 13168895, 8378623, 15268607, 0.75F, 0.5F, 0.25F);
      add("lirael", 16759016, 16742600, 16769268, 0.75F, 0.5F, 0.25F);
      add("grothmaw", 15786144, 15769648, 16773312, 0.75F, 0.5F, 0.25F);
      add("hollow_warden", 13160688, 11071743, 15266047, 0.7F, 0.45F, 0.2F);
      add("bogrot", 12120208, 9895760, 15400896, 0.75F, 0.5F, 0.25F);
      add("sekhmar", 16769162, 16760896, 16774336, 0.75F, 0.5F, 0.25F);
      add("mycelord", 9502688, 5308380, 14745592, 0.75F, 0.5F, 0.25F);
      add("archon", 9498879, 5302015, 14744319, 0.75F, 0.5F, 0.25F);
      add("astrael", 16774344, 16770720, 16777200, 0.75F, 0.5F, 0.25F);
   }
}

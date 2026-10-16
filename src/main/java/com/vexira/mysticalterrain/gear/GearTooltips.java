package com.vexira.mysticalterrain.gear;

import com.vexira.mysticalterrain.gear.arsenal.Arsenal;
import com.vexira.mysticalterrain.gear.arsenal.Arsenals;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;

final class GearTooltips {
   private static final String UI = "tooltip.mysticalterrain.gear.";

   private GearTooltips() {
   }

   private static boolean has(String key) {
      return Language.getInstance().hasTranslation(key);
   }

   private static String seconds(int ticks) {
      return ticks % 20 == 0 ? Integer.toString(ticks / 20) : String.format(Locale.ROOT, "%.1f", ticks / 20.0);
   }

   private static MutableText head(String label, Text name, int cooldown) {
      MutableText line = Text.empty();
      line.append(Text.translatable("tooltip.mysticalterrain.gear." + label).formatted(Formatting.DARK_GRAY));
      line.append(" ");
      line.append(name.copy().formatted(Formatting.GOLD));
      if (cooldown > 0) {
         line.append(
            Text.literal("  ")
               .append(Text.translatable("tooltip.mysticalterrain.gear.cooldown", new Object[]{seconds(cooldown)}))
               .formatted(Formatting.DARK_GRAY)
         );
      }

      return line;
   }

   private static void desc(Consumer<Text> out, String key, Formatting color) {
      if (has(key)) {
         out.accept(Text.literal("  ").append(Text.translatable(key)).formatted(color));
      }
   }

   private static void ability(Consumer<Text> out, String base, String slot, Active active) {
      String key = base + "." + slot;
      if (has(key)) {
         out.accept(head(slot, Text.translatable(key), active.cooldown()));
         desc(out, key + ".desc", Formatting.GRAY);
         if (active.cost() > 0) {
            MutableText line = Text.literal("  ").append(Text.translatable("tooltip.mysticalterrain.gear.empowered", new Object[]{active.cost()}));
            if (has(key + ".empowered")) {
               line.append(": ").append(Text.translatable(key + ".empowered"));
            }

            out.accept(line.formatted(Formatting.DARK_AQUA));
         }
      }
   }

   static void weapon(WeaponKit kit, Consumer<Text> out) {
      String base = "gear.mysticalterrain." + kit.id();
      if (!has(base + ".primary") && has("item.mysticalterrain." + kit.id() + ".ability")) {
         legacy(kit, out);
      } else {
         if (kit.primary() != null) {
            ability(out, base, "primary", kit.primary());
         }

         if (kit.alternate() != null) {
            ability(out, base, "alternate", kit.alternate());
         }

         Charge charge = kit.charge();
         if (charge != null) {
            Active own = charge.active();
            Text name = has(base + ".charge") ? Text.translatable(base + ".charge") : Text.translatable(base + ".primary");
            out.accept(
               head("charge", name, own != null ? own.cooldown() : 0)
                  .append(
                     Text.literal("  ")
                        .append(Text.translatable("tooltip.mysticalterrain.gear.levels", new Object[]{charge.levels()}))
                        .formatted(Formatting.DARK_GRAY)
                  )
            );
            desc(out, base + ".charge.desc", Formatting.GRAY);
            if (own != null && own.cost() > 0) {
               out.accept(
                  Text.literal("  ")
                     .append(Text.translatable("tooltip.mysticalterrain.gear.empowered", new Object[]{own.cost()}))
                     .formatted(Formatting.DARK_AQUA)
               );
            }
         }

         if (kit.passive() != WeaponKit.NONE && has(base + ".passive")) {
            out.accept(head("passive", Text.translatable(base + ".passive"), 0));
            desc(out, base + ".passive.desc", Formatting.GRAY);
         }

         if (kit.usesResonance()) {
            out.accept(Text.translatable("tooltip.mysticalterrain.gear.resonance").formatted(Formatting.DARK_AQUA));
         }
      }
   }

   private static void legacy(WeaponKit kit, Consumer<Text> out) {
      out.accept(Text.translatable("item.mysticalterrain." + kit.id() + ".ability").formatted(Formatting.GOLD));
      String hit = "item.mysticalterrain." + kit.id() + ".on_hit";
      if (has(hit)) {
         out.accept(Text.translatable(hit).formatted(Formatting.GRAY));
      }

      if (kit.primary() != null) {
         out.accept(Text.translatable("tooltip.mysticalterrain.cooldown", new Object[]{seconds(kit.primary().cooldown())}).formatted(Formatting.DARK_GRAY));
      }
   }

   static void armor(String boss, int pieces, Consumer<Text> out) {
      String base = "gear.mysticalterrain.set." + boss;
      Arsenal arsenal = Arsenals.of(boss);
      if (!has(base)) {
         if (has("armor.mysticalterrain." + boss + ".bonus")) {
            out.accept(Text.translatable("tooltip.mysticalterrain.set_bonus").formatted(Formatting.GOLD));
            out.accept(Text.translatable("armor.mysticalterrain." + boss + ".bonus").formatted(Formatting.GRAY));
         }
      } else {
         out.accept(Text.translatable("tooltip.mysticalterrain.gear.set", new Object[]{Text.translatable(base), pieces}).formatted(Formatting.GOLD));
         tier(out, base + ".two_piece", "two_piece", pieces >= 2);
         tier(out, base + ".four_piece", "four_piece", pieces >= 4);
         ArmorKit kit = arsenal == null ? null : arsenal.armorKit();
         if (kit != null && kit.active() != null && has(base + ".active")) {
            boolean on = pieces >= kit.activePieces();
            MutableText line = Text.empty();
            line.append(
               Text.translatable("tooltip.mysticalterrain.gear.set_active", new Object[]{Text.keybind("key.mysticalterrain.set_ability")})
                  .formatted(Formatting.DARK_GRAY)
            );
            line.append(" ");
            line.append(Text.translatable(base + ".active").formatted(on ? Formatting.GOLD : Formatting.GRAY));
            line.append(
               Text.literal("  ")
                  .append(Text.translatable("tooltip.mysticalterrain.gear.cooldown", new Object[]{seconds(kit.active().cooldown())}))
                  .formatted(Formatting.DARK_GRAY)
            );
            out.accept(line);
            desc(out, base + ".active.desc", on ? Formatting.GRAY : Formatting.DARK_GRAY);
         }
      }
   }

   private static void tier(Consumer<Text> out, String key, String label, boolean on) {
      if (has(key)) {
         out.accept(
            Text.translatable("tooltip.mysticalterrain.gear." + label)
               .formatted(on ? Formatting.YELLOW : Formatting.DARK_GRAY)
               .append(" ")
               .append(Text.translatable(key).formatted(on ? Formatting.GRAY : Formatting.DARK_GRAY))
         );
      }
   }
}

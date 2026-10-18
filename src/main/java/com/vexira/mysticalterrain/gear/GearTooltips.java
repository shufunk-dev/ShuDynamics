package com.vexira.mysticalterrain.gear;

import com.vexira.mysticalterrain.gear.arsenal.Arsenal;
import com.vexira.mysticalterrain.gear.arsenal.Arsenals;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class GearTooltips {
   private static final String UI = "tooltip.mysticalterrain.gear.";

   private GearTooltips() {
   }

   private static boolean has(String key) {
      return Language.getInstance().has(key);
   }

   private static String seconds(int ticks) {
      return ticks % 20 == 0 ? Integer.toString(ticks / 20) : String.format(Locale.ROOT, "%.1f", ticks / 20.0);
   }

   private static MutableComponent head(String label, Component name, int cooldown) {
      MutableComponent line = Component.empty();
      line.append(Component.translatable("tooltip.mysticalterrain.gear." + label).withStyle(ChatFormatting.DARK_GRAY));
      line.append(" ");
      line.append(name.copy().withStyle(ChatFormatting.GOLD));
      if (cooldown > 0) {
         line.append(
            Component.literal("  ")
               .append(Component.translatable("tooltip.mysticalterrain.gear.cooldown", new Object[]{seconds(cooldown)}))
               .withStyle(ChatFormatting.DARK_GRAY)
         );
      }

      return line;
   }

   private static void desc(Consumer<Component> out, String key, ChatFormatting color) {
      if (has(key)) {
         out.accept(Component.literal("  ").append(Component.translatable(key)).withStyle(color));
      }
   }

   private static void ability(Consumer<Component> out, String base, String slot, Active active) {
      String key = base + "." + slot;
      if (has(key)) {
         out.accept(head(slot, Component.translatable(key), active.cooldown()));
         desc(out, key + ".desc", ChatFormatting.GRAY);
         if (active.cost() > 0) {
            MutableComponent line = Component.literal("  ").append(Component.translatable("tooltip.mysticalterrain.gear.empowered", new Object[]{active.cost()}));
            if (has(key + ".empowered")) {
               line.append(": ").append(Component.translatable(key + ".empowered"));
            }

            out.accept(line.withStyle(ChatFormatting.DARK_AQUA));
         }
      }
   }

   static void weapon(WeaponKit kit, Consumer<Component> out) {
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
            Component name = has(base + ".charge") ? Component.translatable(base + ".charge") : Component.translatable(base + ".primary");
            out.accept(
               head("charge", name, own != null ? own.cooldown() : 0)
                  .append(
                     Component.literal("  ")
                        .append(Component.translatable("tooltip.mysticalterrain.gear.levels", new Object[]{charge.levels()}))
                        .withStyle(ChatFormatting.DARK_GRAY)
                  )
            );
            desc(out, base + ".charge.desc", ChatFormatting.GRAY);
            if (own != null && own.cost() > 0) {
               out.accept(
                  Component.literal("  ")
                     .append(Component.translatable("tooltip.mysticalterrain.gear.empowered", new Object[]{own.cost()}))
                     .withStyle(ChatFormatting.DARK_AQUA)
               );
            }
         }

         if (kit.passive() != WeaponKit.NONE && has(base + ".passive")) {
            out.accept(head("passive", Component.translatable(base + ".passive"), 0));
            desc(out, base + ".passive.desc", ChatFormatting.GRAY);
         }

         if (kit.usesResonance()) {
            out.accept(Component.translatable("tooltip.mysticalterrain.gear.resonance").withStyle(ChatFormatting.DARK_AQUA));
         }
      }
   }

   private static void legacy(WeaponKit kit, Consumer<Component> out) {
      out.accept(Component.translatable("item.mysticalterrain." + kit.id() + ".ability").withStyle(ChatFormatting.GOLD));
      String hit = "item.mysticalterrain." + kit.id() + ".on_hit";
      if (has(hit)) {
         out.accept(Component.translatable(hit).withStyle(ChatFormatting.GRAY));
      }

      if (kit.primary() != null) {
         out.accept(Component.translatable("tooltip.mysticalterrain.cooldown", new Object[]{seconds(kit.primary().cooldown())}).withStyle(ChatFormatting.DARK_GRAY));
      }
   }

   static void armor(String boss, int pieces, Consumer<Component> out) {
      String base = "gear.mysticalterrain.set." + boss;
      Arsenal arsenal = Arsenals.of(boss);
      if (!has(base)) {
         if (has("armor.mysticalterrain." + boss + ".bonus")) {
            out.accept(Component.translatable("tooltip.mysticalterrain.set_bonus").withStyle(ChatFormatting.GOLD));
            out.accept(Component.translatable("armor.mysticalterrain." + boss + ".bonus").withStyle(ChatFormatting.GRAY));
         }
      } else {
         out.accept(Component.translatable("tooltip.mysticalterrain.gear.set", new Object[]{Component.translatable(base), pieces}).withStyle(ChatFormatting.GOLD));
         tier(out, base + ".two_piece", "two_piece", pieces >= 2);
         tier(out, base + ".four_piece", "four_piece", pieces >= 4);
         ArmorKit kit = arsenal == null ? null : arsenal.armorKit();
         if (kit != null && kit.active() != null && has(base + ".active")) {
            boolean on = pieces >= kit.activePieces();
            MutableComponent line = Component.empty();
            line.append(
               Component.translatable("tooltip.mysticalterrain.gear.set_active", new Object[]{Component.keybind("key.mysticalterrain.set_ability")})
                  .withStyle(ChatFormatting.DARK_GRAY)
            );
            line.append(" ");
            line.append(Component.translatable(base + ".active").withStyle(on ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            line.append(
               Component.literal("  ")
                  .append(Component.translatable("tooltip.mysticalterrain.gear.cooldown", new Object[]{seconds(kit.active().cooldown())}))
                  .withStyle(ChatFormatting.DARK_GRAY)
            );
            out.accept(line);
            desc(out, base + ".active.desc", on ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY);
         }
      }
   }

   private static void tier(Consumer<Component> out, String key, String label, boolean on) {
      if (has(key)) {
         out.accept(
            Component.translatable("tooltip.mysticalterrain.gear." + label)
               .withStyle(on ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY)
               .append(" ")
               .append(Component.translatable(key).withStyle(on ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY))
         );
      }
   }
}

package com.vexira.mysticalterrain.client.gear;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.gear.Active;
import com.vexira.mysticalterrain.gear.ArmorKit;
import com.vexira.mysticalterrain.gear.Charge;
import com.vexira.mysticalterrain.gear.GearUse;
import com.vexira.mysticalterrain.gear.MysticArmor;
import com.vexira.mysticalterrain.gear.MysticWeapon;
import com.vexira.mysticalterrain.gear.WeaponKit;
import com.vexira.mysticalterrain.gear.arsenal.Arsenal;
import com.vexira.mysticalterrain.gear.arsenal.Arsenals;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
final class GearHud implements HudElement {
   private static final int SLOT = 18;
   private static final int GAP = 2;
   private static final Identifier GENERIC = MysticalTerrain.id("gear/generic");
   private final KeyBinding setKey;

   GearHud(KeyBinding setKey) {
      this.setKey = setKey;
   }

   static @Nullable WeaponKit held(PlayerEntity p) {
      for (ItemStack stack : new ItemStack[]{p.getMainHandStack(), p.getOffHandStack()}) {
         if (stack.getItem() instanceof MysticWeapon w) {
            return w.kit();
         }
      }

      return null;
   }

   static @Nullable ArmorKit activeSet(PlayerEntity p) {
      for (Entry<String, Integer> e : MysticArmor.worn(p).entrySet()) {
         Arsenal a = Arsenals.of(e.getKey());
         if (a != null && a.armorKit().active() != null && e.getValue() >= a.armorKit().activePieces()) {
            return a.armorKit();
         }
      }

      return null;
   }

   private static Identifier icon(String name) {
      return MysticalTerrain.id("gear/" + name);
   }

   private static int opaque(int rgb) {
      return 0xFF000000 | rgb & 16777215;
   }

   private static int alpha(int a, int rgb) {
      return MathHelper.clamp(a, 0, 255) << 24 | rgb & 16777215;
   }

   public void render(DrawContext g, RenderTickCounter delta) {
      MinecraftClient mc = MinecraftClient.getInstance();
      ClientPlayerEntity p = mc.player;
      if (p != null && !mc.options.hudHidden && !p.isSpectator()) {
         float partial = delta.getTickProgress(false);
         WeaponKit kit = held(p);
         List<GearHud.Widget> widgets = new ArrayList<>(4);
         if (kit != null) {
            Vfx.Palette pal = kit.arsenal().palette();
            float res = GearClientState.resonance(kit.id());
            if (kit.primary() != null) {
               widgets.add(new GearHud.Widget(icon(kit.id() + "_primary"), kit.key(GearUse.Slot.PRIMARY), pal, kit.primary(), res, null));
            }

            if (kit.alternate() != null) {
               widgets.add(new GearHud.Widget(icon(kit.id() + "_alternate"), kit.key(GearUse.Slot.ALTERNATE), pal, kit.alternate(), res, null));
            }

            if (kit.chargeIsSeparate()) {
               widgets.add(new GearHud.Widget(icon(kit.id() + "_charge"), kit.key(GearUse.Slot.CHARGED), pal, kit.active(GearUse.Slot.CHARGED), res, null));
            }
         }

         ArmorKit set = activeSet(p);
         if (set != null) {
            String label = this.setKey.getBoundKeyLocalizedText().getString();
            widgets.add(
               new GearHud.Widget(
                  icon("set_" + set.arsenal().boss()), set.key(), set.arsenal().palette(), set.active(), 0.0F, label.length() <= 2 ? label : null
               )
            );
         }

         if (!widgets.isEmpty()) {
            int w = widgets.size() * 18 + (widgets.size() - 1) * 2;
            int cx = g.getScaledWindowWidth() / 2;
            int pad = 6 + (mc.options.getAttackIndicator().getValue() == AttackIndicator.HOTBAR ? 22 : 0);
            int x0 = p.getMainArm() == Arm.RIGHT ? cx + 91 + pad : cx - 91 - pad - w;
            int y0 = g.getScaledWindowHeight() - 18 - 2;
            float time = p.age + partial;
            SpriteAtlasTexture atlas = mc.getAtlasManager().getAtlasTexture(Atlases.GUI);

            for (int i = 0; i < widgets.size(); i++) {
               this.widget(g, mc.textRenderer, atlas, x0 + i * 20, y0, widgets.get(i), partial, time);
            }

            int top = y0;
            if (kit != null && kit.usesResonance()) {
               top -= 5;
               resonance(g, x0, top, w, kit, time);
            }

            if (kit != null && kit.charge() != null && p.isUsingItem() && p.getActiveItem().getItem() instanceof MysticWeapon used && used.kit() == kit) {
               top -= 7;
               charge(g, x0, top, w, kit.charge(), p.getItemUseTime(partial), kit.arsenal().palette(), time);
            }
         }
      }
   }

   private void widget(DrawContext g, TextRenderer font, SpriteAtlasTexture atlas, int x, int y, GearHud.Widget w, float partial, float time) {
      float cooling = GearClientState.fraction(w.key(), partial);
      int remaining = GearClientState.remaining(w.key());
      float flash = GearClientState.flash(w.key(), partial);
      boolean ready = cooling <= 0.0F;
      g.fill(x, y, x + 18, y + 18, -1072952302);
      int border = ready ? opaque(w.palette().glow()) : -13355970;
      if (ready && w.empowerReady()) {
         float k = 0.5F + 0.5F * MathHelper.sin(time * 0.35F);
         border = opaque(lerp(k, w.palette().glow(), w.palette().core()));
      }

      g.drawStrokedRectangle(x, y, 18, 18, border);
      g.fill(x + 1, y + 1, x + 18 - 1, y + 2, 687865855);
      Sprite sprite = atlas.getSprite(w.icon());
      if (sprite == atlas.getMissingSprite()) {
         sprite = atlas.getSprite(GENERIC);
      }

      g.drawSpriteStretched(RenderPipelines.GUI_TEXTURED, sprite, x + 1, y + 1, 16, 16, ready ? -1 : -8618876);
      if (!ready) {
         sweep(g, x + 1, y + 1, 16, 16, cooling, -1476395008);
         if (remaining >= 20) {
            String s = Integer.toString((remaining + 19) / 20);
            g.drawText(font, s, x + (18 - font.getWidth(s)) / 2 + 1, y + 5, -855310, true);
         }
      }

      if (flash > 0.0F) {
         g.fill(x + 1, y + 1, x + 18 - 1, y + 18 - 1, alpha((int)(flash * 120.0F), 16777215));
         g.drawStrokedRectangle(x, y, 18, 18, alpha((int)(flash * 255.0F), 16777215));
      }

      if (w.label() != null && (ready || remaining < 20)) {
         g.getMatrices().pushMatrix();
         g.getMatrices().translate(x + 18 - 1.5F - font.getWidth(w.label()) * 0.5F, y + 18 - 5.5F);
         g.getMatrices().scale(0.5F, 0.5F);
         g.drawText(font, w.label(), 0, 0, -1513240, true);
         g.getMatrices().popMatrix();
      }
   }

   private static void sweep(DrawContext g, int x, int y, int w, int h, float fraction, int color) {
      if (fraction >= 0.999F) {
         g.fill(x, y, x + w, y + h, color);
      } else {
         double start = (1.0F - fraction) * Math.PI * 2.0;
         double cx = w / 2.0;
         double cy = h / 2.0;

         for (int row = 0; row < h; row++) {
            int run = -1;

            for (int col = 0; col <= w; col++) {
               boolean dark = false;
               if (col < w) {
                  double a = Math.atan2(col + 0.5 - cx, cy - (row + 0.5));
                  dark = (a < 0.0 ? a + (Math.PI * 2) : a) >= start;
               }

               if (dark && run < 0) {
                  run = col;
               } else if (!dark && run >= 0) {
                  g.fill(x + run, y + row, x + col, y + row + 1, color);
                  run = -1;
               }
            }
         }
      }
   }

   private static void resonance(DrawContext g, int x, int y, int w, WeaponKit kit, float time) {
      Vfx.Palette pal = kit.arsenal().palette();
      float value = GearClientState.resonance(kit.id());
      g.fill(x - 1, y - 1, x + w + 1, y + 4, -1073741824);
      g.fill(x, y, x + w, y + 3, -15066588);
      int filled = Math.round(w * value / 100.0F);
      if (filled > 0) {
         g.fill(x, y, x + filled, y + 3, opaque(pal.glow()));
         g.fill(x, y, x + filled, y + 1, opaque(pal.core()));
      }

      for (GearUse.Slot slot : new GearUse.Slot[]{GearUse.Slot.PRIMARY, GearUse.Slot.ALTERNATE, GearUse.Slot.CHARGED}) {
         Active a = kit.active(slot);
         if (a != null && a.cost() > 0 && a.cost() < 100) {
            int tx = x + Math.round(w * a.cost() / 100.0F);
            g.fill(tx, y, tx + 1, y + 3, value >= a.cost() ? -1 : -10855834);
         }
      }

      if (value >= 100.0F) {
         g.fill(x, y, x + w, y + 3, alpha((int)(60.0F + 50.0F * MathHelper.sin(time * 0.3F)), 16777215));
      }
   }

   private static void charge(DrawContext g, int x, int y, int w, Charge charge, float held, Vfx.Palette pal, float time) {
      int levels = charge.levels();
      float progress = Math.min(levels, held / charge.ticksPerLevel());
      int seg = Math.max(2, (w - (levels - 1)) / levels);
      int span = levels * seg + levels - 1;
      int sx0 = x + (w - span) / 2;
      g.fill(sx0 - 1, y - 1, sx0 + span + 1, y + 5, -1073741824);

      for (int i = 0; i < levels; i++) {
         int sx = sx0 + i * (seg + 1);
         g.fill(sx, y, sx + seg, y + 4, -15066588);
         float f = MathHelper.clamp(progress - i, 0.0F, 1.0F);
         if (f >= 1.0F) {
            g.fill(sx, y, sx + seg, y + 4, opaque(pal.core()));
            g.fill(sx, y + 3, sx + seg, y + 4, opaque(pal.glow()));
         } else if (f > 0.0F) {
            g.fill(sx, y, sx + Math.max(1, Math.round(seg * f)), y + 4, alpha(200, pal.glow()));
         }
      }

      if (progress >= levels) {
         g.fill(sx0, y, sx0 + span, y + 4, alpha((int)(70.0F + 60.0F * MathHelper.sin(time * 0.6F)), 16777215));
      }
   }

   private static int lerp(float k, int a, int b) {
      int r = (int)MathHelper.lerp(k, a >> 16 & 0xFF, b >> 16 & 0xFF);
      int gr = (int)MathHelper.lerp(k, a >> 8 & 0xFF, b >> 8 & 0xFF);
      int bl = (int)MathHelper.lerp(k, a & 0xFF, b & 0xFF);
      return r << 16 | gr << 8 | bl;
   }

   @Environment(EnvType.CLIENT)
   private record Widget(Identifier icon, String key, Vfx.Palette palette, @Nullable Active active, float resonance, @Nullable String label) {
      boolean empowerReady() {
         return this.active != null && this.active.cost() > 0 && this.resonance >= this.active.cost();
      }
   }
}

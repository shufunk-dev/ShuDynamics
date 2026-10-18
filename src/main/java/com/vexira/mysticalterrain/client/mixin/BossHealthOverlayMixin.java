package com.vexira.mysticalterrain.client.mixin;

import com.vexira.mysticalterrain.client.boss.BossBarRenderer;
import com.vexira.mysticalterrain.client.boss.BossBarState;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   @Final
   Map<UUID, LerpingBossEvent> events;

   @Shadow
   private void extractBar(GuiGraphicsExtractor graphics, int x, int y, BossEvent event) {
      throw new AssertionError();
   }

   @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
   private void mysticalterrain$extractRenderState(GuiGraphicsExtractor graphics, CallbackInfo ci) {
      BossBarState.retain(this.events.keySet());
      boolean styled = false;

      for (LerpingBossEvent event : this.events.values()) {
         if (BossBarRenderer.isStyled(event)) {
            styled = true;
            break;
         }
      }

      if (styled) {
         ci.cancel();
         int y = 4;
         boolean first = true;

         for (LerpingBossEvent event : this.events.values()) {
            if (BossBarRenderer.isStyled(event)) {
               y += BossBarRenderer.draw(graphics, event, y, !first);
               first = false;
            } else {
               int x = graphics.guiWidth() / 2 - 91;
               Component name = event.getName();
               graphics.text(
                  this.minecraft.font, name, graphics.guiWidth() / 2 - this.minecraft.font.width(name) / 2, y, -1
               );
               this.extractBar(graphics, x, y + 9, event);
               y += 19;
            }

            if (y >= graphics.guiHeight() / 3) {
               break;
            }
         }
      }
   }

   @Inject(method = "reset", at = @At("HEAD"))
   private void mysticalterrain$reset(CallbackInfo ci) {
      BossBarState.clear();
   }
}

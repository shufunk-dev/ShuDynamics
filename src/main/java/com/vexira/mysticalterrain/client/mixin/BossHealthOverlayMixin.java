package com.vexira.mysticalterrain.client.mixin;

import com.vexira.mysticalterrain.client.boss.BossBarRenderer;
import com.vexira.mysticalterrain.client.boss.BossBarState;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(BossBarHud.class)
public abstract class BossHealthOverlayMixin {
   @Shadow
   @Final
   private MinecraftClient client;
   @Shadow
   @Final
   Map<UUID, ClientBossBar> bossBars;

   @Shadow
   private void renderBossBar(DrawContext graphics, int x, int y, BossBar event) {
      throw new AssertionError();
   }

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void mysticalterrain$render(DrawContext graphics, CallbackInfo ci) {
      BossBarState.retain(this.bossBars.keySet());
      boolean styled = false;

      for (ClientBossBar event : this.bossBars.values()) {
         if (BossBarRenderer.isStyled(event)) {
            styled = true;
            break;
         }
      }

      if (styled) {
         ci.cancel();
         int y = 4;
         boolean first = true;

         for (ClientBossBar event : this.bossBars.values()) {
            if (BossBarRenderer.isStyled(event)) {
               y += BossBarRenderer.draw(graphics, event, y, !first);
               first = false;
            } else {
               int x = graphics.getScaledWindowWidth() / 2 - 91;
               Text name = event.getName();
               graphics.drawTextWithShadow(
                  this.client.textRenderer, name, graphics.getScaledWindowWidth() / 2 - this.client.textRenderer.getWidth(name) / 2, y, -1
               );
               this.renderBossBar(graphics, x, y + 9, event);
               y += 19;
            }

            if (y >= graphics.getScaledWindowHeight() / 3) {
               break;
            }
         }
      }
   }

   @Inject(method = "clear", at = @At("HEAD"))
   private void mysticalterrain$reset(CallbackInfo ci) {
      BossBarState.clear();
   }
}

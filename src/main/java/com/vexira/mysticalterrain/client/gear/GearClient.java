package com.vexira.mysticalterrain.client.gear;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.gear.ArmorKit;
import com.vexira.mysticalterrain.gear.ClientView;
import com.vexira.mysticalterrain.gear.GearSyncPayload;
import com.vexira.mysticalterrain.gear.SetAbilityPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class GearClient {
   private static boolean initialized;

   private GearClient() {
   }

   public static void init() {
      if (!initialized) {
         initialized = true;
         KeyMapping set = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mysticalterrain.set_ability", 86, category()));
         ClientPlayNetworking.registerGlobalReceiver(GearSyncPayload.TYPE, (payload, context) -> GearClientState.apply(payload));
         ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> GearClientState.clear());
         ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
            GearClientState.tick(client);

            while (set.consumeClick()) {
               pressSet(client);
            }
         });
         ClientView.cooldown = GearClientState::remaining;
         ClientView.player = () -> Minecraft.getInstance().player;
         HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, MysticalTerrain.id("gear_abilities"), new GearHud(set));
      }
   }

   private static Category category() {
      KeyMapping toggle = KeyMapping.get("key.mysticalterrain.toggle_16x");
      return toggle != null ? toggle.getCategory() : new Category(MysticalTerrain.id("controls"));
   }

   private static void pressSet(Minecraft client) {
      LocalPlayer p = client.player;
      if (p != null && ClientPlayNetworking.canSend(SetAbilityPayload.TYPE)) {
         ArmorKit set = GearHud.activeSet(p);
         if (set == null) {
            client.gui.hud.setOverlayMessage(Component.translatable("message.mysticalterrain.no_set_ability"), false);
         } else {
            if (GearClientState.remaining(set.key()) <= 0) {
               ClientPlayNetworking.send(SetAbilityPayload.INSTANCE);
            }
         }
      }
   }
}

package com.vexira.mysticalterrain.client.gear;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.gear.ArmorKit;
import com.vexira.mysticalterrain.gear.ClientView;
import com.vexira.mysticalterrain.gear.GearSyncPayload;
import com.vexira.mysticalterrain.gear.SetAbilityPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class GearClient {
   private static boolean initialized;

   private GearClient() {
   }

   public static void init() {
      if (!initialized) {
         initialized = true;
         KeyBinding set = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.mysticalterrain.set_ability", Type.KEYSYM, 86, category()));
         ClientPlayNetworking.registerGlobalReceiver(GearSyncPayload.TYPE, (payload, context) -> GearClientState.apply(payload));
         ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> GearClientState.clear());
         ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
            GearClientState.tick(client);

            while (set.wasPressed()) {
               pressSet(client);
            }
         });
         ClientView.cooldown = GearClientState::remaining;
         ClientView.player = () -> MinecraftClient.getInstance().player;
         HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, MysticalTerrain.id("gear_abilities"), new GearHud(set));
      }
   }

   private static Category category() {
      KeyBinding toggle = KeyBinding.byId("key.mysticalterrain.toggle_16x");
      return toggle != null ? toggle.getCategory() : new Category(MysticalTerrain.id("controls"));
   }

   private static void pressSet(MinecraftClient client) {
      ClientPlayerEntity p = client.player;
      if (p != null && ClientPlayNetworking.canSend(SetAbilityPayload.TYPE)) {
         ArmorKit set = GearHud.activeSet(p);
         if (set == null) {
            client.inGameHud.setOverlayMessage(Text.translatable("message.mysticalterrain.no_set_ability"), false);
         } else {
            if (GearClientState.remaining(set.key()) <= 0) {
               ClientPlayNetworking.send(SetAbilityPayload.INSTANCE);
            }
         }
      }
   }
}

package com.vexira.mysticalterrain.client;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.WorldSizeCheck;
import com.vexira.mysticalterrain.block.MysticalBlocks;
import com.vexira.mysticalterrain.block.WoodSet;
import com.vexira.mysticalterrain.boss.BossEntities;
import com.vexira.mysticalterrain.boss.MysticalBosses;
import com.vexira.mysticalterrain.boss.RealmMobs;
import com.vexira.mysticalterrain.client.boss.BossBarState;
import com.vexira.mysticalterrain.client.boss.BossRenderer;
import com.vexira.mysticalterrain.client.boss.DragonGeoRenderer;
import com.vexira.mysticalterrain.client.boss.GearRenderers;
import com.vexira.mysticalterrain.client.boss.WardenShadeRenderer;
import com.vexira.mysticalterrain.client.gear.GearClient;
import com.vexira.mysticalterrain.client.vfx.VfxClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;

@Environment(EnvType.CLIENT)
public class MysticalTerrainClient implements ClientModInitializer {
   private static final Identifier SIXTEEN = MysticalTerrain.id("16x");

   public void onInitializeClient() {
      VfxClient.register();
      BossBarState.init();
      ResourceLoader.registerBuiltinPack(
         SIXTEEN,
         FabricLoader.getInstance().getModContainer("mysticalterrain")
            .or(() -> FabricLoader.getInstance().getModContainer("enchantedwood"))
            .orElseThrow(),
         Component.translatable("pack.mysticalterrain.16x"),
         PackActivationType.NORMAL
      );
      KeyMapping toggle = KeyMappingHelper.registerKeyMapping(
         new KeyMapping("key.mysticalterrain.toggle_16x", 296, Category.register(MysticalTerrain.id("controls")))
      );
      GearClient.init();
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         while (toggle.consumeClick()) {
            toggleSixteen(client);
         }
      });

      EntityRenderers.register(EntityTypes.ENDER_DRAGON, DragonGeoRenderer::new);
      GearRenderers.install();
      BossEntities.BOSSES.forEach((id, type) -> EntityRenderers.register(type, ctx -> new BossRenderer(ctx, id, 1.2F)));
      EntityRenderers.register(BossEntities.WARDEN_SHADE, WardenShadeRenderer::new);
      RealmMobs.TYPES.forEach((id, type) -> EntityRenderers.register(type, ctx -> new BossRenderer(ctx, id, RealmMobs.kind(id).width() * 0.5F)));
      EntityRenderers.register(BossEntities.BOSS_BOLT, NoopRenderer::new);
   }

   private static void toggleSixteen(Minecraft client) {
      PackRepository packs = client.getResourcePackRepository();
      String id = SIXTEEN.toString();
      boolean on = !packs.getSelectedIds().contains(id);
      if (on ? packs.addPack(id) : packs.removePack(id)) {
         client.options.updateResourcePacks(packs);
         client.gui.hud.setOverlayMessage(Component.translatable(on ? "message.mysticalterrain.16x_on" : "message.mysticalterrain.16x_off"), false);
      }
   }
}

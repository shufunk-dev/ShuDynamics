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
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactories;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.entity.EntityType;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class MysticalTerrainClient implements ClientModInitializer {
   private static final Identifier SIXTEEN = MysticalTerrain.id("16x");

   public void onInitializeClient() {
      VfxClient.register();
      BossBarState.init();
      ResourceLoader.registerBuiltinPack(
         SIXTEEN,
         (ModContainer)FabricLoader.getInstance().getModContainer("mysticalterrain").or(() -> FabricLoader.getInstance().getModContainer("enchantedwood")).orElseThrow(),
         Text.translatable("pack.mysticalterrain.16x"),
         PackActivationType.NORMAL
      );
      KeyBinding toggle = KeyBindingHelper.registerKeyBinding(
         new KeyBinding("key.mysticalterrain.toggle_16x", Type.KEYSYM, 296, Category.create(MysticalTerrain.id("controls")))
      );
      GearClient.init();
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         while (toggle.wasPressed()) {
            toggleSixteen(client);
         }
      });

      for (WoodSet set : MysticalBlocks.WOOD) {
         BlockRenderLayerMap.putBlocks(BlockRenderLayer.CUTOUT, new Block[]{set.door(), set.trapdoor()});
      }

      MysticalBlocks.PLANTS.forEach(block -> BlockRenderLayerMap.putBlock(block, BlockRenderLayer.CUTOUT));
      EntityRendererFactories.register(EntityType.ENDER_DRAGON, DragonGeoRenderer::new);
      GearRenderers.install();
      BossEntities.BOSSES.forEach((id, type) -> EntityRendererFactories.register(type, ctx -> new BossRenderer(ctx, id, 1.2F)));
      EntityRendererFactories.register(BossEntities.WARDEN_SHADE, WardenShadeRenderer::new);
      RealmMobs.TYPES.forEach((id, type) -> EntityRendererFactories.register(type, ctx -> new BossRenderer(ctx, id, RealmMobs.kind(id).width() * 0.5F)));
      EntityRendererFactories.register(BossEntities.BOSS_BOLT, EmptyEntityRenderer::new);
      BlockRenderLayerMap.putBlocks(BlockRenderLayer.CUTOUT, MysticalBosses.ALTARS.values().toArray(Block[]::new));
      BlockRenderLayerMap.putBlocks(BlockRenderLayer.CUTOUT, new Block[]{MysticalBosses.FROST_BELL, MysticalBosses.GLOOM_BRAZIER, MysticalBosses.VOID_PEDESTAL});
   }

   private static void toggleSixteen(MinecraftClient client) {
      ResourcePackManager packs = client.getResourcePackManager();
      String id = SIXTEEN.toString();
      boolean on = !packs.getEnabledIds().contains(id);
      if (on ? packs.enable(id) : packs.disable(id)) {
         client.options.refreshResourcePacks(packs);
         client.inGameHud.setOverlayMessage(Text.translatable(on ? "message.mysticalterrain.16x_on" : "message.mysticalterrain.16x_off"), false);
      }
   }
}

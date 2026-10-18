package com.vexira.mysticalterrain.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(EnderDragonRenderer.class)
public interface EnderDragonRendererInvoker {
   @Invoker("submitRays")
   static void mysticalterrain$submitRays(PoseStack poseStack, float progress, SubmitNodeCollector collector, RenderType type) {
      throw new AssertionError();
   }
}

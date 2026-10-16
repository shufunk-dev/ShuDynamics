package com.vexira.mysticalterrain.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(EnderDragonEntityRenderer.class)
public interface EnderDragonRendererInvoker {
   @Invoker("renderDeathAnimation")
   static void mysticalterrain$submitRays(MatrixStack poseStack, float progress, OrderedRenderCommandQueue collector, RenderLayer type) {
      throw new AssertionError();
   }
}

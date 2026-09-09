package net.enchantedwood.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;

@Environment(EnvType.CLIENT)
public class ResonanceColossusRenderState extends LivingEntityRenderState {
    public float limbSwing;
    public float limbSwingAmount;
    public float ageInTicks;
}

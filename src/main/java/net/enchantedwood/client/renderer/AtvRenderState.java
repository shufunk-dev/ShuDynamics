package net.enchantedwood.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;

@Environment(EnvType.CLIENT)
public class AtvRenderState extends EntityRenderState {
    public float pitch;
    public float yaw;
    public float wheelRotation;
    public int lightmapCoordinates;
    public ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay;
}

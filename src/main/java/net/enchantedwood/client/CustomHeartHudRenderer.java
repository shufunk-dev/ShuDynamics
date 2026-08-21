package net.enchantedwood.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

@Environment(EnvType.CLIENT)
public class CustomHeartHudRenderer {

    private static final Identifier ENCHANTED_FULL = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/sprites/hud/heart/enchanted_full.png");
    private static final Identifier ENCHANTED_HALF = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/sprites/hud/heart/enchanted_half.png");

    private static final Identifier IRON_FULL = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/sprites/hud/heart/iron_full.png");
    private static final Identifier IRON_HALF = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/sprites/hud/heart/iron_half.png");

    private static final Identifier GOLD_FULL = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/sprites/hud/heart/gold_full.png");
    private static final Identifier GOLD_HALF = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/sprites/hud/heart/gold_half.png");

    private static final Identifier DIAMOND_FULL = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/sprites/hud/heart/diamond_full.png");
    private static final Identifier DIAMOND_HALF = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/sprites/hud/heart/diamond_half.png");

    private static final Identifier NETHERITE_FULL = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/sprites/hud/heart/netherite_full.png");
    private static final Identifier NETHERITE_HALF = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/sprites/hud/heart/netherite_half.png");

    public static void register() {
        HudRenderCallback.EVENT.register(CustomHeartHudRenderer::onRenderHud);
    }

    private static void onRenderHud(DrawContext context, RenderTickCounter renderTickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || player.isSpectator() || player.isCreative()) return;

        float maxHealth = player.getMaxHealth();
        if (maxHealth <= 20.0f) return; // No equipped Heart Locket

        // Determine tier textures strictly by EQUIPPED max health expansion
        Identifier fullTexture;
        Identifier halfTexture;

        if (maxHealth >= 39.5f) { // Netherite Tier (+20 HP)
            fullTexture = NETHERITE_FULL;
            halfTexture = NETHERITE_HALF;
        } else if (maxHealth >= 33.5f) { // Diamond Tier (+14 HP)
            fullTexture = DIAMOND_FULL;
            halfTexture = DIAMOND_HALF;
        } else if (maxHealth >= 29.5f) { // Gold Tier (+10 HP)
            fullTexture = GOLD_FULL;
            halfTexture = GOLD_HALF;
        } else if (maxHealth >= 25.5f) { // Iron Tier (+6 HP)
            fullTexture = IRON_FULL;
            halfTexture = IRON_HALF;
        } else { // Enchanted Tier (+2 HP)
            fullTexture = ENCHANTED_FULL;
            halfTexture = ENCHANTED_HALF;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        int left = width / 2 - 91;
        int top = height - 39;

        float currentHealth = player.getHealth();
        int extraHearts = (int) Math.ceil((maxHealth - 20.0f) / 2.0f);

        // Render Tier-Colored Extra Hearts dynamically across all upper rows (Row 2, Row 3, etc.)
        for (int i = 0; i < extraHearts; i++) {
            int heartIdx = 10 + i;
            int row = i / 10;
            int hx = left + (i % 10) * 8;
            int hy = top - 10 - (row * 10);

            float heartHp = (heartIdx + 1) * 2.0f;
            float prevHp = heartIdx * 2.0f;

            if (currentHealth >= heartHp) {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, fullTexture, hx, hy, 0.0f, 0.0f, 9, 9, 9, 9);
            } else if (currentHealth > prevHp) {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, halfTexture, hx, hy, 0.0f, 0.0f, 9, 9, 9, 9);
            }
        }
    }
}

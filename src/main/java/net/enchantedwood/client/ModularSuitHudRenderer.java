package net.enchantedwood.client;

import net.enchantedwood.item.custom.ModularPowerArmorItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class ModularSuitHudRenderer {
    public static boolean hudVisible = true;

    public static void register() {
        HudRenderCallback.EVENT.register(ModularSuitHudRenderer::onRenderHud);
    }

    public static boolean isWearingFullModularSet(PlayerEntity player) {
        ItemStack head = player.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack legs = player.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack feet = player.getEquippedStack(EquipmentSlot.FEET);

        return head.getItem() instanceof ModularPowerArmorItem
                && chest.getItem() instanceof ModularPowerArmorItem
                && legs.getItem() instanceof ModularPowerArmorItem
                && feet.getItem() instanceof ModularPowerArmorItem;
    }

    private static void onRenderHud(DrawContext context, RenderTickCounter renderTickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || !hudVisible) return;
        PlayerEntity player = client.player;
        if (player == null || player.isSpectator()) return;

        // Only display HUD when wearing a full set of modular power armor
        if (!isWearingFullModularSet(player)) return;

        ItemStack head = player.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack legs = player.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);

        int hudX = 6;
        int hudY = 6;
        int hudW = 124;
        int hudH = 58;

        // Calculate total suit power
        int totalEnergy = ModularPowerArmorItem.getStoredEnergy(head)
                + ModularPowerArmorItem.getStoredEnergy(chest)
                + ModularPowerArmorItem.getStoredEnergy(legs)
                + ModularPowerArmorItem.getStoredEnergy(boots);

        int totalMaxEnergy = ModularPowerArmorItem.getMaxEnergy(head)
                + ModularPowerArmorItem.getMaxEnergy(chest)
                + ModularPowerArmorItem.getMaxEnergy(legs)
                + ModularPowerArmorItem.getMaxEnergy(boots);

        int totalPct = totalMaxEnergy > 0 ? (int) Math.round((double) totalEnergy * 100.0 / totalMaxEnergy) : 0;

        // Draw translucent futuristic HUD background frame
        context.fill(hudX, hudY, hudX + hudW, hudY + hudH, 0xAA0A0F16);
        // Subtle cybernetic cyan borders
        context.fill(hudX, hudY, hudX + hudW, hudY + 1, 0x6600E5FF);
        context.fill(hudX, hudY + hudH - 1, hudX + hudW, hudY + hudH, 0x6600E5FF);
        context.fill(hudX, hudY, hudX + 1, hudY + hudH, 0x6600E5FF);
        context.fill(hudX + hudW - 1, hudY, hudX + hudW, hudY + hudH, 0x6600E5FF);

        // Cybernetic corner markers
        context.fill(hudX, hudY, hudX + 3, hudY + 2, 0xFF00E5FF);
        context.fill(hudX + hudW - 3, hudY, hudX + hudW, hudY + 2, 0xFF00E5FF);
        context.fill(hudX, hudY + hudH - 2, hudX + 3, hudY + hudH, 0xFF00E5FF);
        context.fill(hudX + hudW - 3, hudY + hudH - 2, hudX + hudW, hudY + hudH, 0xFF00E5FF);

        // Header Title
        context.drawText(client.textRenderer, Text.literal("⚡ SUIT STATUS"), hudX + 5, hudY + 4, 0x00E5FF, false);
        String totalPctStr = totalMaxEnergy > 0 ? totalPct + "%" : "OFFLINE";
        int totalColor = totalMaxEnergy > 0 ? (totalPct > 50 ? 0x00E5FF : (totalPct > 20 ? 0xFFD700 : 0xFF4444)) : 0x777777;
        context.drawText(client.textRenderer, Text.literal(totalPctStr), hudX + hudW - 5 - client.textRenderer.getWidth(totalPctStr), hudY + 4, totalColor, false);

        // Subtle divider
        context.fill(hudX + 4, hudY + 13, hudX + hudW - 4, hudY + 14, 0x3300E5FF);

        // 4 Piece Rows: Head, Chest, Legs, Boots
        drawPieceRow(context, client, player, head, "HEAD", hudX + 4, hudY + 16, EquipmentSlot.HEAD);
        drawPieceRow(context, client, player, chest, "CHEST", hudX + 4, hudY + 26, EquipmentSlot.CHEST);
        drawPieceRow(context, client, player, legs, "LEGS", hudX + 4, hudY + 36, EquipmentSlot.LEGS);
        drawPieceRow(context, client, player, boots, "BOOTS", hudX + 4, hudY + 46, EquipmentSlot.FEET);
    }

    private static void drawPieceRow(DrawContext context, MinecraftClient client, PlayerEntity player, ItemStack piece, String label, int rx, int ry, EquipmentSlot slot) {
        // Label
        context.drawText(client.textRenderer, Text.literal(label), rx, ry, 0xAAAAAA, false);

        int max = ModularPowerArmorItem.getMaxEnergy(piece);
        int energy = ModularPowerArmorItem.getStoredEnergy(piece);

        if (max <= 0) {
            context.drawText(client.textRenderer, Text.literal("§8NO BAT"), rx + 32, ry, 0x666666, false);
        } else {
            float pct = Math.max(0.0f, Math.min(1.0f, (float) energy / max));
            int bx = rx + 32;
            int by = ry + 2;
            int bw = 40;
            int bh = 4;

            // Bar recess
            context.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF1B232E);
            context.fill(bx, by, bx + bw, by + bh, 0xFF111822);

            int fillW = Math.round(pct * bw);
            int barColor = pct > 0.5f ? 0xFF00E5FF : (pct > 0.2f ? 0xFFFFD700 : 0xFFFF3333);
            if (fillW > 0) {
                context.fill(bx, by, bx + fillW, by + bh, barColor);
            }

            // Percentage Text
            int pInt = Math.round(pct * 100.0f);
            String pStr = pInt + "%";
            context.drawText(client.textRenderer, Text.literal(pStr), rx + 75, ry, barColor, false);
        }

        // Active Status Tag
        String tag = null;
        int tagColor = 0xFFFFFF;

        if (slot == EquipmentSlot.HEAD) {
            if (player.hasStatusEffect(StatusEffects.NIGHT_VISION) && ModularPowerArmorItem.hasModule(piece, "enchantedwood:night_vision_module")) {
                tag = "NVG";
                tagColor = 0x00FF66;
            }
        } else if (slot == EquipmentSlot.CHEST) {
            if (player.getAbilities().flying && ModularPowerArmorItem.hasModule(piece, "enchantedwood:ion_repulsor_module")) {
                tag = "ION";
                tagColor = 0x00E5FF;
            } else if (player.getAbilities().flying && ModularPowerArmorItem.hasModule(piece, "enchantedwood:hydrogen_thruster_module")) {
                tag = "JET";
                tagColor = 0xFFAA00;
            }
        }

        if (tag == null && piece.isDamaged() && ModularPowerArmorItem.hasModule(piece, "enchantedwood:nanite_repair_matrix")) {
            long lastDmg = net.enchantedwood.event.PlayerHealthHandler.getLastDamageTime(player.getUuid());
            if (System.currentTimeMillis() - lastDmg >= 10_000L) {
                tag = "REP";
                tagColor = 0x55FF55;
            }
        }

        if (tag != null) {
            context.drawText(client.textRenderer, Text.literal(tag), rx + 98, ry, tagColor, false);
        }
    }
}

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
        int hudW = 186;
        int hudH = 60;

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

        int totalArmorMax = head.getMaxDamage() + chest.getMaxDamage() + legs.getMaxDamage() + boots.getMaxDamage();
        int totalArmorDmg = head.getDamage() + chest.getDamage() + legs.getDamage() + boots.getDamage();
        int armorPct = totalArmorMax > 0 ? Math.max(0, Math.min(100, (int) Math.round((double) (totalArmorMax - totalArmorDmg) * 100.0 / totalArmorMax))) : 100;

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
        String headerRight = String.format("⚡ %d%%  🛡 %d%%", totalPct, armorPct);
        int rightColor = totalPct > 50 ? 0x00E5FF : (totalPct > 20 ? 0xFFFFD700 : 0xFFFF4444);
        context.drawText(client.textRenderer, Text.literal(headerRight), hudX + hudW - 5 - client.textRenderer.getWidth(headerRight), hudY + 4, rightColor, false);

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

        // --- 1. Battery Power Section ---
        int max = ModularPowerArmorItem.getMaxEnergy(piece);
        int energy = ModularPowerArmorItem.getStoredEnergy(piece);

        context.drawText(client.textRenderer, Text.literal("⚡"), rx + 28, ry, 0x00E5FF, false);

        if (max <= 0) {
            context.drawText(client.textRenderer, Text.literal("NO BAT"), rx + 36, ry, 0x666666, false);
        } else {
            float pct = Math.max(0.0f, Math.min(1.0f, (float) energy / max));
            int bx = rx + 36;
            int by = ry + 2;
            int bw = 24;
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
            context.drawText(client.textRenderer, Text.literal(pStr), rx + 62, ry, barColor, false);
        }

        // --- 2. Armor Durability Section ---
        context.drawText(client.textRenderer, Text.literal("🛡"), rx + 86, ry, 0x55FF55, false);

        int maxDmg = piece.getMaxDamage();
        int dmg = piece.getDamage();
        float durPct = maxDmg > 0 ? Math.max(0.0f, Math.min(1.0f, (float) (maxDmg - dmg) / maxDmg)) : 1.0f;

        int dbx = rx + 95;
        int dby = ry + 2;
        int dbw = 24;
        int dbh = 4;

        // Durability Bar recess
        context.fill(dbx - 1, dby - 1, dbx + dbw + 1, dby + dbh + 1, 0xFF1B232E);
        context.fill(dbx, dby, dbx + dbw, dby + dbh, 0xFF111822);

        int dFillW = Math.round(durPct * dbw);
        int dColor = durPct > 0.6f ? 0xFF55FF55 : (durPct > 0.25f ? 0xFFFFD700 : 0xFFFF3333);
        if (dFillW > 0) {
            context.fill(dbx, dby, dbx + dFillW, dby + dbh, dColor);
        }

        int dInt = Math.round(durPct * 100.0f);
        String dStr = dInt + "%";
        context.drawText(client.textRenderer, Text.literal(dStr), rx + 121, ry, dColor, false);

        // --- 3. Active Status Tag ---
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
            } else {
                tag = "WAIT";
                tagColor = 0xAAAAAA;
            }
        }

        if (tag != null) {
            context.drawText(client.textRenderer, Text.literal(tag), rx + 148, ry, tagColor, false);
        }
    }
}

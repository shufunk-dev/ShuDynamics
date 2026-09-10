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
import net.minecraft.util.Identifier;

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

        // Dynamic Header Title & Hazard Alert
        boolean inAcid = isClientInAcidHazard(player) || player.hasStatusEffect(StatusEffects.POISON) || player.hasStatusEffect(StatusEffects.WITHER);
        boolean inHeat = isClientInThermalHazard(player);
        boolean inAtmosphere = isClientInAtmosphericHazard(player);

        String titleText = "⚡ SUIT STATUS";
        int titleColor = 0x00E5FF;
        if (inAcid && inHeat) {
            titleText = "⚡ HAZARD SHIELD";
            titleColor = 0xFF55FF;
        } else if (inAcid) {
            titleText = "⚡ ACID DEFENSE";
            titleColor = 0x55FF55;
        } else if (inHeat) {
            titleText = "⚡ HEAT SHIELD";
            titleColor = 0xFFAA00;
        } else if (inAtmosphere) {
            titleText = "⚡ LIFE SUPPORT";
            titleColor = 0x55FFFF;
        }

        context.drawText(client.textRenderer, Text.literal(titleText), hudX + 5, hudY + 4, titleColor, false);
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

    private static boolean isClientInAcidHazard(PlayerEntity player) {
        if (player.getEntityWorld() == null) return false;
        var pos = player.getBlockPos();
        var biomeKey = player.getEntityWorld().getBiome(pos).getKey();
        boolean isCaustic = biomeKey.isPresent() && biomeKey.get().getValue().equals(Identifier.of("enchantedwood", "caustic_mire"));
        if (!isCaustic) return false;
        boolean inWater = player.isTouchingWater() || player.isSubmergedInWater();
        boolean inRain = player.getEntityWorld().isRaining() && player.getEntityWorld().isSkyVisible(pos);
        return inWater || inRain;
    }

    private static boolean isClientInThermalHazard(PlayerEntity player) {
        if (player.getEntityWorld() == null) return false;
        if (player.isInLava() || player.isOnFire()) return true;
        var pos = player.getBlockPos();
        var biomeKey = player.getEntityWorld().getBiome(pos).getKey();
        boolean isCaldera = biomeKey.isPresent() && biomeKey.get().getValue().equals(Identifier.of("enchantedwood", "scorched_caldera"));
        return isCaldera && player.getY() <= 25;
    }

    private static boolean isClientInAtmosphericHazard(PlayerEntity player) {
        if (player.getEntityWorld() == null) return false;
        var pos = player.getBlockPos();
        var biomeKey = player.getEntityWorld().getBiome(pos).getKey();
        boolean isAnoxic = biomeKey.isPresent() && biomeKey.get().getValue().equals(Identifier.of("enchantedwood", "anoxic_barrens"));
        if (isAnoxic) return true;
        boolean isHighAltitude = player.getY() >= 180 && player.getEntityWorld().isSkyVisible(pos);
        boolean isAnoxicCave = player.getY() <= 35 && !player.getEntityWorld().isSkyVisible(pos) && player.getEntityWorld().getLightLevel(pos) <= 7;
        return isHighAltitude || isAnoxicCave;
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

        int pieceEnergy = ModularPowerArmorItem.getStoredEnergy(piece);

        // Environmental Hazard Plating Overrides (High Priority)
        if (ModularPowerArmorItem.hasModule(piece, "enchantedwood:acid_proof_plating")) {
            boolean inAcidHazard = isClientInAcidHazard(player);
            boolean hasToxin = player.hasStatusEffect(StatusEffects.POISON) || player.hasStatusEffect(StatusEffects.WITHER) || player.hasStatusEffect(StatusEffects.NAUSEA);
            if (inAcidHazard || hasToxin) {
                if (pieceEnergy > 0) {
                    tag = "ACID";
                    tagColor = 0x55FF55;
                } else {
                    tag = "DEP!";
                    tagColor = (player.getEntityWorld() != null && player.getEntityWorld().getTime() % 10 < 5) ? 0xFF2222 : 0x880000;
                }
            } else {
                tag = "SHLD";
                tagColor = 0x33AA88;
            }
        } else if (ModularPowerArmorItem.hasModule(piece, "enchantedwood:thermal_refractory_plating")) {
            boolean inThermalHazard = isClientInThermalHazard(player);
            if (inThermalHazard) {
                if (pieceEnergy > 0) {
                    tag = "HEAT";
                    tagColor = 0xFFAA00;
                } else {
                    tag = "DEP!";
                    tagColor = (player.getEntityWorld() != null && player.getEntityWorld().getTime() % 10 < 5) ? 0xFF2222 : 0x880000;
                }
            } else {
                tag = "THERM";
                tagColor = 0xAA7733;
            }
        }

        // Standard piece modules
        if (tag == null) {
            if (slot == EquipmentSlot.HEAD) {
                if (isClientInAtmosphericHazard(player)) {
                    if (pieceEnergy > 0) {
                        tag = "O2";
                        tagColor = 0x55FFFF;
                    } else {
                        tag = "DEP!";
                        tagColor = (player.getEntityWorld() != null && player.getEntityWorld().getTime() % 10 < 5) ? 0xFF2222 : 0x880000;
                    }
                } else if (player.hasStatusEffect(StatusEffects.NIGHT_VISION) && ModularPowerArmorItem.hasModule(piece, "enchantedwood:night_vision_module")) {
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
            } else if (slot == EquipmentSlot.FEET) {
                if (!player.isOnGround() && ModularPowerArmorItem.hasModule(piece, "enchantedwood:high_jump_module")) {
                    tag = "JUMP";
                    tagColor = 0x00E5FF;
                } else if (ModularPowerArmorItem.hasModule(piece, "enchantedwood:step_assist_module") && player.getVelocity().horizontalLengthSquared() > 0.005) {
                    tag = "STEP";
                    tagColor = 0xAAAAAA;
                }
            } else if (slot == EquipmentSlot.LEGS) {
                if (ModularPowerArmorItem.hasModule(piece, "enchantedwood:speed_servo_module") && (player.isSprinting() || player.getVelocity().horizontalLengthSquared() > 0.005)) {
                    tag = "SPD";
                    tagColor = 0x00E5FF;
                }
            }
        }

        // Nanite Auto-Repair across suit
        if (tag == null && piece.isDamaged()) {
            boolean suitHasNanites = isWearingFullModularSet(player) && (
                    ModularPowerArmorItem.hasModule(player.getEquippedStack(EquipmentSlot.HEAD), "enchantedwood:nanite_repair_matrix") ||
                    ModularPowerArmorItem.hasModule(player.getEquippedStack(EquipmentSlot.CHEST), "enchantedwood:nanite_repair_matrix") ||
                    ModularPowerArmorItem.hasModule(player.getEquippedStack(EquipmentSlot.LEGS), "enchantedwood:nanite_repair_matrix") ||
                    ModularPowerArmorItem.hasModule(player.getEquippedStack(EquipmentSlot.FEET), "enchantedwood:nanite_repair_matrix")
            );
            if (suitHasNanites) {
                long lastDmg = net.enchantedwood.event.PlayerHealthHandler.getLastDamageTime(player.getUuid());
                if (System.currentTimeMillis() - lastDmg >= 10_000L) {
                    tag = "REP";
                    tagColor = 0x55FF55;
                } else {
                    tag = "WAIT";
                    tagColor = 0xAAAAAA;
                }
            }
        }

        if (tag != null) {
            context.drawText(client.textRenderer, Text.literal(tag), rx + 148, ry, tagColor, false);
        }
    }
}

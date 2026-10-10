package net.enchantedwood.client;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.item.custom.ModularPowerArmorItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class ModularSuitHudRenderer {
    public static boolean hudVisible = true;

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "suit_hud"), ModularSuitHudRenderer::onRenderHud);
    }

    public static boolean isWearingFullModularSet(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);

        return head.getItem() instanceof ModularPowerArmorItem
                && chest.getItem() instanceof ModularPowerArmorItem
                && legs.getItem() instanceof ModularPowerArmorItem
                && feet.getItem() instanceof ModularPowerArmorItem;
    }

    public static boolean isWearingAnyModularPiece(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ModularPowerArmorItem
                || player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModularPowerArmorItem
                || player.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof ModularPowerArmorItem
                || player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof ModularPowerArmorItem;
    }

    private static void onRenderHud(GuiGraphicsExtractor context, DeltaTracker renderTickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui.hud.isHidden() || !hudVisible) return;
        Player player = client.player;
        if (player == null || player.isSpectator()) return;

        boolean inSanctuary = isClientInsideSanctuary(player);
        boolean inAcid = !inSanctuary && (isClientInAcidHazard(player) || player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.WITHER));
        boolean inHeat = !inSanctuary && isClientInThermalHazard(player);
        boolean inAtmosphere = !inSanctuary && isClientInAtmosphericHazard(player);

        String titleText;
        int titleColor;
        if (inSanctuary) {
            titleText = "[SAFE] SANCTUARY";
            titleColor = 0xFF55FF55; // Bright green safe zone
        } else if (inAcid && inHeat) {
            titleText = "[!] MULTI-HAZARD";
            titleColor = 0xFFFF3333; // Red alert
        } else if (inAcid) {
            titleText = "[!] ACID HAZARD";
            titleColor = 0xFFFF55FF; // Magenta acid alert
        } else if (inHeat) {
            titleText = "[!] THERMAL HEAT";
            titleColor = 0xFFFFAA00; // Orange heat alert
        } else if (inAtmosphere) {
            titleText = "[!] HYPOXIA";
            titleColor = 0xFF55FFFF; // Cyan hypoxia alert
        } else {
            titleText = "[SAFE] NORMAL";
            titleColor = 0xFF00E5FF; // Sky cyan nominal clear
        }

        boolean hasSuitPiece = isWearingAnyModularPiece(player);

        // If the player is NOT wearing any modular suit armor:
        // Render a compact, clean Environmental Scanner badge in the top-left corner!
        if (!hasSuitPiece) {
            int textW = client.font.width(titleText);
            int badgeW = textW + 16;
            int badgeH = 15;
            int bx = 6;
            int by = 6;
            context.fill(bx, by, bx + badgeW, by + badgeH, 0xDD0A0F16);
            context.fill(bx, by, bx + badgeW, by + 1, titleColor);
            context.fill(bx, by + badgeH - 1, bx + badgeW, by + badgeH, titleColor);
            context.fill(bx, by, bx + 1, by + badgeH, titleColor);
            context.fill(bx + badgeW - 1, by, bx + badgeW, by + badgeH, titleColor);
            context.text(client.font, Component.literal(titleText), bx + 8, by + 4, titleColor, true);
            return;
        }

        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        int hudX = 6;
        int hudY = 6;
        int hudW = 224;
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
        int totalArmorDmg = head.getDamageValue() + chest.getDamageValue() + legs.getDamageValue() + boots.getDamageValue();
        int armorPct = totalArmorMax > 0 ? Math.max(0, Math.min(100, (int) Math.round((double) (totalArmorMax - totalArmorDmg) * 100.0 / totalArmorMax))) : 100;

        // Calculate O2 / Life Support metrics
        int air = player.getAirSupply();
        int maxAir = player.getMaxAirSupply();
        int airPct = maxAir > 0 ? Math.max(0, Math.min(100, (int) Math.round((double) air * 100.0 / maxAir))) : 100;
        int headEnergy = ModularPowerArmorItem.getStoredEnergy(head);

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

        context.text(client.font, Component.literal(titleText), hudX + 5, hudY + 4, titleColor, true);

        // Header Badges: Battery, Armor Durability, and Life Support (O2)
        String pwrStr = "⚡ " + totalPct + "%";
        int pwrColor = totalPct > 50 ? 0xFF00E5FF : (totalPct > 20 ? 0xFFFFD700 : 0xFFFF4444);

        String armStr = "🛡 " + armorPct + "%";
        int armColor = armorPct > 60 ? 0xFF55FF55 : (armorPct > 25 ? 0xFFFFD700 : 0xFFFF3333);

        String o2Str;
        int o2Color;
        if (headEnergy <= 0 && (airPct < 100 || inAtmosphere)) {
            o2Str = "O2 DEP!";
            o2Color = (player.level() != null && player.level().getGameTime() % 10 < 5) ? 0xFFFF2222 : 0xFF880000;
        } else if (inAtmosphere) {
            o2Str = "O2 " + airPct + "%";
            o2Color = 0xFF55FFFF; // Active life support bright cyan
        } else {
            o2Str = "O2 " + airPct + "%";
            o2Color = airPct > 75 ? 0xFF00E5FF : (airPct > 35 ? 0xFFFFD700 : 0xFFFF3333);
        }

        int o2W = client.font.width(o2Str);
        int armW = client.font.width(armStr);
        int pwrW = client.font.width(pwrStr);
        int badgeGap = 6;

        int curX = hudX + hudW - 5;
        curX -= o2W;
        context.text(client.font, Component.literal(o2Str), curX, hudY + 4, o2Color, true);

        curX -= (armW + badgeGap);
        context.text(client.font, Component.literal(armStr), curX, hudY + 4, armColor, true);

        curX -= (pwrW + badgeGap);
        context.text(client.font, Component.literal(pwrStr), curX, hudY + 4, pwrColor, true);

        // Subtle divider
        context.fill(hudX + 4, hudY + 13, hudX + hudW - 4, hudY + 14, 0x3300E5FF);

        // 4 Piece Rows: Head, Chest, Legs, Boots
        drawPieceRow(context, client, player, head, "HEAD", hudX + 4, hudY + 16, EquipmentSlot.HEAD);
        drawPieceRow(context, client, player, chest, "CHEST", hudX + 4, hudY + 26, EquipmentSlot.CHEST);
        drawPieceRow(context, client, player, legs, "LEGS", hudX + 4, hudY + 36, EquipmentSlot.LEGS);
        drawPieceRow(context, client, player, boots, "BOOTS", hudX + 4, hudY + 46, EquipmentSlot.FEET);
    }

    private static boolean isClientInsideSanctuary(Player player) {
        if (player.level() == null) return false;
        var pos = player.blockPosition();
        var biomeKey = player.level().getBiome(pos).unwrapKey();
        if (biomeKey.isPresent() && biomeKey.get().identifier().equals(Identifier.fromNamespaceAndPath("enchantedwood", "riftwood_haven"))) {
            return true;
        }
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-8, -4, -8), pos.offset(8, 4, 8))) {
            var state = player.level().getBlockState(check);
            if (state.is(net.enchantedwood.block.ModBlocks.DORMANT_RIFT) || state.is(net.enchantedwood.block.ModBlocks.ATMOSPHERIC_ANCHOR)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isClientInAcidHazard(Player player) {
        if (player.level() == null) return false;
        var pos = player.blockPosition();
        var biomeKey = player.level().getBiome(pos).unwrapKey();
        boolean isCaustic = biomeKey.isPresent() && biomeKey.get().identifier().equals(Identifier.fromNamespaceAndPath("enchantedwood", "caustic_mire"));
        if (!isCaustic) return false;
        boolean inWater = player.isInWater() || player.isUnderWater();
        boolean inRain = player.level().isRaining() && player.level().canSeeSky(pos);
        return inWater || inRain;
    }

    private static boolean isClientInThermalHazard(Player player) {
        if (player.level() == null) return false;
        if (player.isInLava() || player.isOnFire()) return true;
        var pos = player.blockPosition();
        var biomeKey = player.level().getBiome(pos).unwrapKey();
        boolean isCaldera = biomeKey.isPresent() && biomeKey.get().identifier().equals(Identifier.fromNamespaceAndPath("enchantedwood", "scorched_caldera"));
        boolean isDeepCaldera = isCaldera && player.getY() <= 25;
        boolean nearHeatSource = isClientNearThermalSource(player, pos);
        return isDeepCaldera || nearHeatSource;
    }

    private static boolean isClientNearThermalSource(Player player, BlockPos pos) {
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            var state = player.level().getBlockState(check);
            if (state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) || state.is(net.minecraft.world.level.block.Blocks.LAVA)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isClientInAtmosphericHazard(Player player) {
        if (player.level() == null) return false;
        var pos = player.blockPosition();
        var biomeKey = player.level().getBiome(pos).unwrapKey();
        boolean isAnoxic = biomeKey.isPresent() && biomeKey.get().identifier().equals(Identifier.fromNamespaceAndPath("enchantedwood", "anoxic_barrens"));
        if (isAnoxic) return true;
        boolean isHighAltitude = player.getY() >= 180 && player.level().canSeeSky(pos);
        boolean isAnoxicCave = player.getY() <= 35 && !player.level().canSeeSky(pos);
        return isHighAltitude || isAnoxicCave;
    }

    private static void drawPieceRow(GuiGraphicsExtractor context, Minecraft client, Player player, ItemStack piece, String label, int rx, int ry, EquipmentSlot slot) {
        // Label
        context.text(client.font, Component.literal(label), rx, ry, 0xAAAAAA, false);

        if (!(piece.getItem() instanceof ModularPowerArmorItem)) {
            context.text(client.font, Component.literal("---"), rx + 28, ry, 0x555555, false);
            return;
        }

        // --- 1. Battery Power Section ---
        int max = ModularPowerArmorItem.getMaxEnergy(piece);
        int energy = ModularPowerArmorItem.getStoredEnergy(piece);

        context.text(client.font, Component.literal("⚡"), rx + 28, ry, 0x00E5FF, false);

        if (max <= 0) {
            context.text(client.font, Component.literal("NO BAT"), rx + 36, ry, 0x666666, false);
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
            context.text(client.font, Component.literal(pStr), rx + 62, ry, barColor, false);
        }

        // --- 2. Armor Durability Section ---
        context.text(client.font, Component.literal("🛡"), rx + 86, ry, 0x55FF55, false);

        int maxDmg = piece.getMaxDamage();
        int dmg = piece.getDamageValue();
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
        context.text(client.font, Component.literal(dStr), rx + 121, ry, dColor, false);

        // --- 3. Active Status Tag ---
        String tag = null;
        int tagColor = 0xFFFFFF;

        int pieceEnergy = ModularPowerArmorItem.getStoredEnergy(piece);

        // Environmental Hazard Plating Overrides (High Priority)
        if (ModularPowerArmorItem.hasModule(piece, "enchantedwood:acid_proof_plating")) {
            boolean inAcidHazard = isClientInAcidHazard(player);
            boolean hasToxin = player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.WITHER) || player.hasEffect(MobEffects.NAUSEA);
            if (inAcidHazard || hasToxin) {
                if (pieceEnergy > 0) {
                    tag = "ACID";
                    tagColor = 0x55FF55;
                } else {
                    tag = "DEP!";
                    tagColor = (player.level() != null && player.level().getGameTime() % 10 < 5) ? 0xFF2222 : 0x880000;
                }
            } else if (slot != EquipmentSlot.HEAD || !isClientInAtmosphericHazard(player)) {
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
                    tagColor = (player.level() != null && player.level().getGameTime() % 10 < 5) ? 0xFF2222 : 0x880000;
                }
            } else if (slot != EquipmentSlot.HEAD || !isClientInAtmosphericHazard(player)) {
                tag = "THERM";
                tagColor = 0xAA7733;
            }
        }

        // Standard piece modules
        if (tag == null) {
            if (slot == EquipmentSlot.HEAD) {
                if (isClientInAtmosphericHazard(player)) {
                    if (pieceEnergy > 0) {
                        tag = "O2:ACT";
                        tagColor = 0x55FFFF;
                    } else {
                        tag = "DEP!";
                        tagColor = (player.level() != null && player.level().getGameTime() % 10 < 5) ? 0xFF2222 : 0x880000;
                    }
                } else if (player.hasEffect(MobEffects.NIGHT_VISION) && ModularPowerArmorItem.hasModule(piece, "enchantedwood:night_vision_module")) {
                    tag = "NVG";
                    tagColor = 0x00FF66;
                } else if (pieceEnergy > 0) {
                    tag = "O2:OK";
                    tagColor = 0x00E5FF;
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
                if (!player.onGround() && ModularPowerArmorItem.hasModule(piece, "enchantedwood:high_jump_module")) {
                    tag = "JUMP";
                    tagColor = 0x00E5FF;
                } else if (ModularPowerArmorItem.hasModule(piece, "enchantedwood:step_assist_module") && player.getDeltaMovement().horizontalDistanceSqr() > 0.005) {
                    tag = "STEP";
                    tagColor = 0xAAAAAA;
                }
            } else if (slot == EquipmentSlot.LEGS) {
                if (ModularPowerArmorItem.hasModule(piece, "enchantedwood:speed_servo_module") && (player.isSprinting() || player.getDeltaMovement().horizontalDistanceSqr() > 0.005)) {
                    tag = "SPD";
                    tagColor = 0x00E5FF;
                }
            }
        }

        // Nanite Auto-Repair across suit
        if (tag == null && piece.isDamaged()) {
            boolean suitHasNanites = isWearingFullModularSet(player) && (
                    ModularPowerArmorItem.hasModule(player.getItemBySlot(EquipmentSlot.HEAD), "enchantedwood:nanite_repair_matrix") ||
                    ModularPowerArmorItem.hasModule(player.getItemBySlot(EquipmentSlot.CHEST), "enchantedwood:nanite_repair_matrix") ||
                    ModularPowerArmorItem.hasModule(player.getItemBySlot(EquipmentSlot.LEGS), "enchantedwood:nanite_repair_matrix") ||
                    ModularPowerArmorItem.hasModule(player.getItemBySlot(EquipmentSlot.FEET), "enchantedwood:nanite_repair_matrix")
            );
            if (suitHasNanites) {
                long lastDmg = net.enchantedwood.event.PlayerHealthHandler.getLastDamageTime(player.getUUID());
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
            context.text(client.font, Component.literal(tag), rx + 158, ry, tagColor, false);
        }
    }
}

package net.enchantedwood.item.custom;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ModularPowerArmorItem extends Item {
    private final EquipmentType equipmentType;

    public ModularPowerArmorItem(EquipmentType equipmentType, Settings settings) {
        super(settings);
        this.equipmentType = equipmentType;
    }

    public EquipmentType getEquipmentType() {
        return this.equipmentType;
    }


    public static NbtCompound getCustomData(ItemStack stack) {
        if (stack.contains(DataComponentTypes.CUSTOM_DATA)) {
            NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (comp != null) {
                return comp.copyNbt();
            }
        }
        return new NbtCompound();
    }

    public static void setCustomData(ItemStack stack, NbtCompound nbt) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static int getStoredEnergy(ItemStack stack) {
        return getCustomData(stack).getInt("Energy", 0);
    }

    public static int getMaxEnergy(ItemStack stack) {
        return getCustomData(stack).getInt("MaxEnergy", 0);
    }

    public static void setStoredEnergy(ItemStack stack, int energy) {
        NbtCompound nbt = getCustomData(stack);
        int max = nbt.getInt("MaxEnergy", 0);
        nbt.putInt("Energy", Math.max(0, Math.min(max > 0 ? max : Integer.MAX_VALUE, energy)));
        setCustomData(stack, nbt);
    }

    public static void setMaxEnergy(ItemStack stack, int maxEnergy) {
        NbtCompound nbt = getCustomData(stack);
        nbt.putInt("MaxEnergy", Math.max(0, maxEnergy));
        int energy = nbt.getInt("Energy", 0);
        if (energy > maxEnergy) {
            nbt.putInt("Energy", maxEnergy);
        }
        setCustomData(stack, nbt);
    }

    public static String getInstalledBatteryId(ItemStack stack) {
        return getCustomData(stack).getString("BatteryId", "");
    }

    public static void setInstalledBatteryId(ItemStack stack, String id) {
        NbtCompound nbt = getCustomData(stack);
        if (id == null || id.isEmpty()) {
            nbt.remove("BatteryId");
        } else {
            nbt.putString("BatteryId", id);
        }
        setCustomData(stack, nbt);
    }

    public static String getInstalledChipId(ItemStack stack) {
        return getCustomData(stack).getString("ChipId", "");
    }

    public static void setInstalledChipId(ItemStack stack, String id) {
        NbtCompound nbt = getCustomData(stack);
        if (id == null || id.isEmpty()) {
            nbt.remove("ChipId");
        } else {
            nbt.putString("ChipId", id);
        }
        setCustomData(stack, nbt);
    }

    public static String getInstalledModuleId(ItemStack stack, int slotIndex) {
        return getCustomData(stack).getString("Module" + slotIndex, "");
    }

    public static void setInstalledModuleId(ItemStack stack, int slotIndex, String id) {
        NbtCompound nbt = getCustomData(stack);
        String key = "Module" + slotIndex;
        if (id == null || id.isEmpty()) {
            nbt.remove(key);
        } else {
            nbt.putString(key, id);
        }
        setCustomData(stack, nbt);
    }

    public static boolean hasModule(ItemStack stack, String moduleId) {
        NbtCompound nbt = getCustomData(stack);
        return moduleId.equals(nbt.getString("Module0", "")) || moduleId.equals(nbt.getString("Module1", ""));
    }

    public static int extractEnergy(ItemStack stack, int amount) {
        int stored = getStoredEnergy(stack);
        int toExtract = Math.min(stored, amount);
        if (toExtract > 0) {
            setStoredEnergy(stack, stored - toExtract);
        }
        return toExtract;
    }

    public static int insertEnergy(ItemStack stack, int amount) {
        int stored = getStoredEnergy(stack);
        int max = getMaxEnergy(stack);
        int toInsert = Math.min(max - stored, amount);
        if (toInsert > 0) {
            setStoredEnergy(stack, stored + toInsert);
        }
        return toInsert;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return getMaxEnergy(stack) > 0;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        int max = getMaxEnergy(stack);
        if (max <= 0) return 0;
        int energy = getStoredEnergy(stack);
        return Math.round((float) energy * 13.0f / (float) max);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        int max = getMaxEnergy(stack);
        if (max <= 0) return 0xFFFFFF;
        float f = Math.max(0.0f, (float) getStoredEnergy(stack) / (float) max);
        return MathHelper.hsvToRgb(f / 3.0f, 1.0f, 1.0f);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§6⚡ Modular Power Suit Chassis"));
        textConsumer.accept(Text.literal("§7Reinforced titanium exoskeleton engineered for deep-dimension anomalies."));

        int max = getMaxEnergy(stack);
        int energy = getStoredEnergy(stack);
        if (max > 0) {
            textConsumer.accept(Text.literal(String.format("§eEnergy: §f%,d / %,d FE", energy, max)));
        } else {
            textConsumer.accept(Text.literal("§8• No Battery Installed (Slot in Suit Panel [V] or Powered Anvil)"));
        }

        String batteryId = getInstalledBatteryId(stack);
        if (!batteryId.isEmpty()) {
            Item item = Registries.ITEM.get(Identifier.tryParse(batteryId));
            textConsumer.accept(Text.literal("§e🔋 Battery: §f" + item.getName().getString()));
        }

        String chipId = getInstalledChipId(stack);
        if (!chipId.isEmpty()) {
            Item item = Registries.ITEM.get(Identifier.tryParse(chipId));
            textConsumer.accept(Text.literal("§b💻 Logic Core: §f" + item.getName().getString()));
        }

        String mod0 = getInstalledModuleId(stack, 0);
        String mod1 = getInstalledModuleId(stack, 1);
        if (!mod0.isEmpty()) {
            Item item = Registries.ITEM.get(Identifier.tryParse(mod0));
            textConsumer.accept(Text.literal("§a⚙️ Module A: §f" + item.getName().getString()));
        }
        if (!mod1.isEmpty()) {
            Item item = Registries.ITEM.get(Identifier.tryParse(mod1));
            textConsumer.accept(Text.literal("§a⚙️ Module B: §f" + item.getName().getString()));
        }

        textConsumer.accept(Text.literal("§8[Press 'V' to open Suit Access Panel]"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        if (slot != null && slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && entity instanceof PlayerEntity player) {
            // Check Nanite Auto-Repair
            if (hasModule(stack, "enchantedwood:nanite_repair_matrix") && stack.getDamage() > 0) {
                long lastDamage = net.enchantedwood.event.PlayerHealthHandler.getLastDamageTime(player.getUuid());
                if (System.currentTimeMillis() - lastDamage >= 10_000L) {
                    // Out of combat for >= 10s: repair 1 durability every 40 ticks (2 seconds)
                    if (world.getTime() % 40 == 0) {
                        int currentEnergy = getStoredEnergy(stack);
                        if (currentEnergy >= 100) {
                            setStoredEnergy(stack, currentEnergy - 100);
                            stack.setDamage(Math.max(0, stack.getDamage() - 1));
                            world.spawnParticles(
                                    ParticleTypes.ELECTRIC_SPARK,
                                    player.getX(), player.getY() + 1.0, player.getZ(),
                                    2, 0.2, 0.3, 0.2, 0.05
                            );
                        }
                    }
                }
            }

            // Check Adaptive Night Vision HUD (Helmet Module)
            if (slot == EquipmentSlot.HEAD && hasModule(stack, "enchantedwood:night_vision_module")) {
                int lightLevel = world.getLightLevel(player.getBlockPos());
                if (lightLevel <= 6) {
                    int storedEnergy = getStoredEnergy(stack);
                    if (storedEnergy >= 2) {
                        setStoredEnergy(stack, storedEnergy - 2);
                        boolean hadNightVision = player.hasStatusEffect(StatusEffects.NIGHT_VISION);
                        // Refresh with 240 ticks (12 seconds) so vanilla low-duration flashing never triggers
                        player.addStatusEffect(new StatusEffectInstance(
                                StatusEffects.NIGHT_VISION,
                                240,
                                0,
                                false,
                                false,
                                true
                        ));
                        if (!hadNightVision) {
                            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE,
                                    SoundCategory.PLAYERS,
                                    0.4f, 1.8f);
                        }
                    } else {
                        // Out of power: shut off HUD
                        StatusEffectInstance currentEffect = player.getStatusEffect(StatusEffects.NIGHT_VISION);
                        if (currentEffect != null && currentEffect.getDuration() <= 260) {
                            player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                        }
                    }
                } else if (lightLevel >= 9) {
                    // Bright daylight / well-lit base: disengage optical HUD to conserve energy
                    StatusEffectInstance currentEffect = player.getStatusEffect(StatusEffects.NIGHT_VISION);
                    if (currentEffect != null && currentEffect.getDuration() <= 260) {
                        player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,
                                SoundCategory.PLAYERS,
                                0.3f, 2.0f);
                    }
                }
            }
        }
        super.inventoryTick(stack, world, entity, slot);
    }
}

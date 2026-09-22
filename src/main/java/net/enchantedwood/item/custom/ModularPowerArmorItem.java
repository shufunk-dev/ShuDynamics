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
        if ("enchantedwood:infinite_dimensional_matrix".equals(getInstalledBatteryId(stack))) {
            return 10_000_000;
        }
        return getCustomData(stack).getInt("Energy", 0);
    }

    public static int getMaxEnergy(ItemStack stack) {
        if ("enchantedwood:infinite_dimensional_matrix".equals(getInstalledBatteryId(stack))) {
            return 10_000_000;
        }
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

    public static boolean isChassisLocked(ItemStack stack) {
        return stack.getItem() instanceof ModularPowerArmorItem && stack.isDamageable() && stack.getDamage() >= stack.getMaxDamage() - 1;
    }

    public static boolean hasModule(ItemStack stack, String moduleId) {
        // If chassis is locked down at 1 HP, active modules are offline to protect systems,
        // EXCEPT Nanite Auto-Repair which remains active to reboot the chassis!
        if (isChassisLocked(stack) && !"enchantedwood:nanite_repair_matrix".equals(moduleId)) {
            return false;
        }
        NbtCompound nbt = getCustomData(stack);
        return moduleId.equals(nbt.getString("Module0", "")) || moduleId.equals(nbt.getString("Module1", ""));
    }

    public static int extractEnergy(ItemStack stack, int amount) {
        if ("enchantedwood:infinite_dimensional_matrix".equals(getInstalledBatteryId(stack))) {
            return amount;
        }
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
        if ("enchantedwood:infinite_dimensional_matrix".equals(getInstalledBatteryId(stack))) {
            return false;
        }
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
        if (isChassisLocked(stack)) {
            textConsumer.accept(Text.literal("§c⚠ EMERGENCY CHASSIS LOCK ⚠"));
            textConsumer.accept(Text.literal("§cIntegrity critical (1 HP)! Safety shutdown active."));
            textConsumer.accept(Text.literal("§eRepair in Anvil or allow Nanites to reboot chassis."));
        }

        textConsumer.accept(Text.literal("§6⚡ Modular Power Suit Chassis"));
        textConsumer.accept(Text.literal("§7Reinforced titanium exoskeleton engineered for deep-dimension anomalies."));

        int max = getMaxEnergy(stack);
        int energy = getStoredEnergy(stack);
        boolean isInfinite = "enchantedwood:infinite_dimensional_matrix".equals(getInstalledBatteryId(stack));
        if (isInfinite) {
            textConsumer.accept(Text.literal("§eEnergy: §a∞ Infinite FE"));
        } else if (max > 0) {
            textConsumer.accept(Text.literal(String.format("§eEnergy: §f%,d / %,d FE", energy, max)));
        } else {
            textConsumer.accept(Text.literal("§8• No Battery Installed (Slot in Suit Panel [V] or Powered Anvil)"));
        }

        String batteryId = getInstalledBatteryId(stack);
        if (!batteryId.isEmpty()) {
            if (isInfinite) {
                textConsumer.accept(Text.literal("§d🔋 Battery: §b✦ Infinite Dimensional Matrix ✦ §a(Limitless Energy)"));
            } else {
                Item item = Registries.ITEM.get(Identifier.tryParse(batteryId));
                textConsumer.accept(Text.literal("§e🔋 Battery: §f" + item.getName().getString()));
            }
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
}

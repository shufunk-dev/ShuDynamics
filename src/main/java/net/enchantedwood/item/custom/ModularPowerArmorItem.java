package net.enchantedwood.item.custom;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;

public class ModularPowerArmorItem extends Item {
    private final ArmorType equipmentType;

    public ModularPowerArmorItem(ArmorType equipmentType, Properties settings) {
        super(settings);
        this.equipmentType = equipmentType;
    }

    public ArmorType getEquipmentType() {
        return this.equipmentType;
    }


    public static CompoundTag getCustomData(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
            if (comp != null) {
                return comp.copyTag();
            }
        }
        return new CompoundTag();
    }

    public static void setCustomData(ItemStack stack, CompoundTag nbt) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    public static int getStoredEnergy(ItemStack stack) {
        if ("enchantedwood:infinite_dimensional_matrix".equals(getInstalledBatteryId(stack))) {
            return 10_000_000;
        }
        return getCustomData(stack).getIntOr("Energy", 0);
    }

    public static int getMaxEnergy(ItemStack stack) {
        if ("enchantedwood:infinite_dimensional_matrix".equals(getInstalledBatteryId(stack))) {
            return 10_000_000;
        }
        return getCustomData(stack).getIntOr("MaxEnergy", 0);
    }

    public static void setStoredEnergy(ItemStack stack, int energy) {
        CompoundTag nbt = getCustomData(stack);
        int max = nbt.getIntOr("MaxEnergy", 0);
        nbt.putInt("Energy", Math.max(0, Math.min(max > 0 ? max : Integer.MAX_VALUE, energy)));
        setCustomData(stack, nbt);
    }

    public static void setMaxEnergy(ItemStack stack, int maxEnergy) {
        CompoundTag nbt = getCustomData(stack);
        nbt.putInt("MaxEnergy", Math.max(0, maxEnergy));
        int energy = nbt.getIntOr("Energy", 0);
        if (energy > maxEnergy) {
            nbt.putInt("Energy", maxEnergy);
        }
        setCustomData(stack, nbt);
    }

    public static String getInstalledBatteryId(ItemStack stack) {
        return getCustomData(stack).getStringOr("BatteryId", "");
    }

    public static void setInstalledBatteryId(ItemStack stack, String id) {
        CompoundTag nbt = getCustomData(stack);
        if (id == null || id.isEmpty()) {
            nbt.remove("BatteryId");
        } else {
            nbt.putString("BatteryId", id);
        }
        setCustomData(stack, nbt);
    }

    public static String getInstalledChipId(ItemStack stack) {
        return getCustomData(stack).getStringOr("ChipId", "");
    }

    public static void setInstalledChipId(ItemStack stack, String id) {
        CompoundTag nbt = getCustomData(stack);
        if (id == null || id.isEmpty()) {
            nbt.remove("ChipId");
        } else {
            nbt.putString("ChipId", id);
        }
        setCustomData(stack, nbt);
    }

    public static String getInstalledModuleId(ItemStack stack, int slotIndex) {
        return getCustomData(stack).getStringOr("Module" + slotIndex, "");
    }

    public static void setInstalledModuleId(ItemStack stack, int slotIndex, String id) {
        CompoundTag nbt = getCustomData(stack);
        String key = "Module" + slotIndex;
        if (id == null || id.isEmpty()) {
            nbt.remove(key);
        } else {
            nbt.putString(key, id);
        }
        setCustomData(stack, nbt);
    }

    public static boolean isChassisLocked(ItemStack stack) {
        return stack.getItem() instanceof ModularPowerArmorItem && stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    public static boolean hasModule(ItemStack stack, String moduleId) {
        // If chassis is locked down at 1 HP, active modules are offline to protect systems,
        // EXCEPT Nanite Auto-Repair which remains active to reboot the chassis!
        if (isChassisLocked(stack) && !"enchantedwood:nanite_repair_matrix".equals(moduleId)) {
            return false;
        }
        CompoundTag nbt = getCustomData(stack);
        return moduleId.equals(nbt.getStringOr("Module0", "")) || moduleId.equals(nbt.getStringOr("Module1", ""));
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
    public boolean isBarVisible(ItemStack stack) {
        if ("enchantedwood:infinite_dimensional_matrix".equals(getInstalledBatteryId(stack))) {
            return false;
        }
        return getMaxEnergy(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int max = getMaxEnergy(stack);
        if (max <= 0) return 0;
        int energy = getStoredEnergy(stack);
        return Math.round((float) energy * 13.0f / (float) max);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int max = getMaxEnergy(stack);
        if (max <= 0) return 0xFFFFFF;
        float f = Math.max(0.0f, (float) getStoredEnergy(stack) / (float) max);
        return Mth.hsvToRgb(f / 3.0f, 1.0f, 1.0f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        if (isChassisLocked(stack)) {
            textConsumer.accept(Component.literal("§c⚠ EMERGENCY CHASSIS LOCK ⚠"));
            textConsumer.accept(Component.literal("§cIntegrity critical (1 HP)! Safety shutdown active."));
            textConsumer.accept(Component.literal("§eRepair in Anvil or allow Nanites to reboot chassis."));
        }

        textConsumer.accept(Component.literal("§6⚡ Modular Power Suit Chassis"));
        textConsumer.accept(Component.literal("§7Reinforced titanium exoskeleton engineered for deep-dimension anomalies."));

        int max = getMaxEnergy(stack);
        int energy = getStoredEnergy(stack);
        boolean isInfinite = "enchantedwood:infinite_dimensional_matrix".equals(getInstalledBatteryId(stack));
        if (isInfinite) {
            textConsumer.accept(Component.literal("§eEnergy: §a∞ Infinite FE"));
        } else if (max > 0) {
            textConsumer.accept(Component.literal(String.format("§eEnergy: §f%,d / %,d FE", energy, max)));
        } else {
            textConsumer.accept(Component.literal("§8• No Battery Installed (Slot in Suit Panel [V] or Powered Anvil)"));
        }

        String batteryId = getInstalledBatteryId(stack);
        if (!batteryId.isEmpty()) {
            if (isInfinite) {
                textConsumer.accept(Component.literal("§d🔋 Battery: §b✦ Infinite Dimensional Matrix ✦ §a(Limitless Energy)"));
            } else {
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(batteryId));
                textConsumer.accept(Component.literal("§e🔋 Battery: §f" + item.getName(item.getDefaultInstance()).getString()));
            }
        }

        String chipId = getInstalledChipId(stack);
        if (!chipId.isEmpty()) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(chipId));
            textConsumer.accept(Component.literal("§b💻 Logic Core: §f" + item.getName(item.getDefaultInstance()).getString()));
        }

        String mod0 = getInstalledModuleId(stack, 0);
        String mod1 = getInstalledModuleId(stack, 1);
        if (!mod0.isEmpty()) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(mod0));
            textConsumer.accept(Component.literal("§a⚙️ Module A: §f" + item.getName(item.getDefaultInstance()).getString()));
        }
        if (!mod1.isEmpty()) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(mod1));
            textConsumer.accept(Component.literal("§a⚙️ Module B: §f" + item.getName(item.getDefaultInstance()).getString()));
        }

        textConsumer.accept(Component.literal("§8[Press 'V' to open Suit Access Panel]"));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}

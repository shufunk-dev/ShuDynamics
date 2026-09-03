package net.enchantedwood.item;

import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.tag.ModTags;

import java.util.Map;

public class ModArmorMaterials {
    // Tier 1: Enchanted Wood (15 total defense)
    public static final ArmorMaterial ENCHANTED_WOOD = new ArmorMaterial(
            15,
            Map.of(
                    EquipmentType.HELMET, 2,
                    EquipmentType.CHESTPLATE, 6,
                    EquipmentType.LEGGINGS, 5,
                    EquipmentType.BOOTS, 2
            ),
            15,
            SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
            1.0f,
            0.0f,
            ModTags.Items.REPAIRS_ENCHANTED_WOOD,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_wood"))
    );

    // Tier 2: Enchanted Cobblestone (19 total defense)
    public static final ArmorMaterial ENCHANTED_COBBLESTONE = new ArmorMaterial(
            25,
            Map.of(
                    EquipmentType.HELMET, 3,
                    EquipmentType.CHESTPLATE, 7,
                    EquipmentType.LEGGINGS, 6,
                    EquipmentType.BOOTS, 3
            ),
            20,
            SoundEvents.ITEM_ARMOR_EQUIP_CHAIN,
            2.0f,
            0.0f,
            ModTags.Items.REPAIRS_ENCHANTED_COBBLESTONE,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_cobblestone"))
    );

    // Bronze (17 total defense)
    public static final ArmorMaterial BRONZE = new ArmorMaterial(
            20,
            Map.of(
                    EquipmentType.HELMET, 3,
                    EquipmentType.CHESTPLATE, 6,
                    EquipmentType.LEGGINGS, 5,
                    EquipmentType.BOOTS, 3
            ),
            20,
            SoundEvents.ITEM_ARMOR_EQUIP_IRON,
            1.0f,
            0.0f,
            ModTags.Items.REPAIRS_BRONZE,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(EnchantedWoodMod.MOD_ID, "bronze"))
    );

    // Tin (11 total defense)
    public static final ArmorMaterial TIN = new ArmorMaterial(
            15,
            Map.of(
                    EquipmentType.HELMET, 2,
                    EquipmentType.CHESTPLATE, 5,
                    EquipmentType.LEGGINGS, 4,
                    EquipmentType.BOOTS, 2
            ),
            12,
            SoundEvents.ITEM_ARMOR_EQUIP_IRON,
            0.0f,
            0.0f,
            ModTags.Items.REPAIRS_TIN,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(EnchantedWoodMod.MOD_ID, "tin"))
    );

    // Titanium (20 total defense)
    public static final ArmorMaterial TITANIUM = new ArmorMaterial(
            35,
            Map.of(
                    EquipmentType.HELMET, 3,
                    EquipmentType.CHESTPLATE, 8,
                    EquipmentType.LEGGINGS, 6,
                    EquipmentType.BOOTS, 3
            ),
            18,
            SoundEvents.ITEM_ARMOR_EQUIP_IRON,
            2.0f,
            0.0f,
            ModTags.Items.REPAIRS_TITANIUM,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(EnchantedWoodMod.MOD_ID, "titanium"))
    );

    // Aluminum (15 total defense)
    public static final ArmorMaterial ALUMINUM = new ArmorMaterial(
            18,
            Map.of(
                    EquipmentType.HELMET, 2,
                    EquipmentType.CHESTPLATE, 6,
                    EquipmentType.LEGGINGS, 5,
                    EquipmentType.BOOTS, 2
            ),
            16,
            SoundEvents.ITEM_ARMOR_EQUIP_IRON,
            0.5f,
            0.0f,
            ModTags.Items.REPAIRS_ALUMINUM,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(EnchantedWoodMod.MOD_ID, "aluminum"))
    );

    // Steel (20 total defense)
    public static final ArmorMaterial STEEL = new ArmorMaterial(
            28,
            Map.of(
                    EquipmentType.HELMET, 3,
                    EquipmentType.CHESTPLATE, 8,
                    EquipmentType.LEGGINGS, 6,
                    EquipmentType.BOOTS, 3
            ),
            15,
            SoundEvents.ITEM_ARMOR_EQUIP_IRON,
            1.5f,
            0.1f,
            ModTags.Items.REPAIRS_STEEL,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(EnchantedWoodMod.MOD_ID, "steel"))
    );

    // Tier 3: Enchanted Diamond (23 total defense)
    public static final ArmorMaterial ENCHANTED_DIAMOND = new ArmorMaterial(
            50,
            Map.of(
                    EquipmentType.HELMET, 4,
                    EquipmentType.CHESTPLATE, 8,
                    EquipmentType.LEGGINGS, 7,
                    EquipmentType.BOOTS, 4
            ),
            30,
            SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
            3.0f,
            0.1f,
            ModTags.Items.REPAIRS_ENCHANTED_DIAMOND,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_diamond"))
    );

    // Tier 4: Enchanted Netherite (26 total defense)
    public static final ArmorMaterial ENCHANTED_NETHERITE = new ArmorMaterial(
            75,
            Map.of(
                    EquipmentType.HELMET, 4,
                    EquipmentType.CHESTPLATE, 10,
                    EquipmentType.LEGGINGS, 8,
                    EquipmentType.BOOTS, 4
            ),
            35,
            SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
            4.0f,
            0.3f,
            ModTags.Items.REPAIRS_ENCHANTED_NETHERITE,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_netherite"))
    );

    // Scuba / Diving Suit (15 total defense, specialized water resistance)
    public static final ArmorMaterial SCUBA = new ArmorMaterial(
            22,
            Map.of(
                    EquipmentType.HELMET, 2,
                    EquipmentType.CHESTPLATE, 6,
                    EquipmentType.LEGGINGS, 5,
                    EquipmentType.BOOTS, 2
            ),
            14,
            SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
            0.5f,
            0.0f,
            ModTags.Items.REPAIRS_SCUBA,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(EnchantedWoodMod.MOD_ID, "scuba"))
    );

    // Cobalt (21 total defense, lightweight & high speed)
    public static final ArmorMaterial COBALT = new ArmorMaterial(
            32,
            Map.of(
                    EquipmentType.HELMET, 3,
                    EquipmentType.CHESTPLATE, 8,
                    EquipmentType.LEGGINGS, 7,
                    EquipmentType.BOOTS, 3
            ),
            18,
            SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
            2.0f,
            0.0f,
            ModTags.Items.REPAIRS_COBALT,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(EnchantedWoodMod.MOD_ID, "cobalt"))
    );

    // Ardite (22 total defense, high durability & knockback resistance)
    public static final ArmorMaterial ARDITE = new ArmorMaterial(
            45,
            Map.of(
                    EquipmentType.HELMET, 3,
                    EquipmentType.CHESTPLATE, 8,
                    EquipmentType.LEGGINGS, 8,
                    EquipmentType.BOOTS, 3
            ),
            16,
            SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
            2.5f,
            0.2f,
            ModTags.Items.REPAIRS_ARDITE,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(EnchantedWoodMod.MOD_ID, "ardite"))
    );

    // Manyullyn (25 total defense, fireproof supreme Nether armor)
    public static final ArmorMaterial MANYULLYN = new ArmorMaterial(
            60,
            Map.of(
                    EquipmentType.HELMET, 4,
                    EquipmentType.CHESTPLATE, 9,
                    EquipmentType.LEGGINGS, 8,
                    EquipmentType.BOOTS, 4
            ),
            24,
            SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
            3.5f,
            0.25f,
            ModTags.Items.REPAIRS_MANYULLYN,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(EnchantedWoodMod.MOD_ID, "manyullyn"))
    );

    // Tungsten (Heavy Refractory Armor, 20 total defense + 3.0 toughness + 0.1 knockback resistance)
    public static final ArmorMaterial TUNGSTEN = new ArmorMaterial(
            38,
            Map.of(
                    EquipmentType.HELMET, 3,
                    EquipmentType.CHESTPLATE, 8,
                    EquipmentType.LEGGINGS, 6,
                    EquipmentType.BOOTS, 3
            ),
            18,
            SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
            3.0f,
            0.1f,
            ModTags.Items.REPAIRS_TUNGSTEN,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(EnchantedWoodMod.MOD_ID, "tungsten"))
    );
}

package net.enchantedwood.item;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.tag.ModTags;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import java.util.Map;

public class ModArmorMaterials {
    // Tier 1: Enchanted Wood (15 total defense)
    public static final ArmorMaterial ENCHANTED_WOOD = new ArmorMaterial(
            15,
            Map.of(
                    ArmorType.HELMET, 2,
                    ArmorType.CHESTPLATE, 6,
                    ArmorType.LEGGINGS, 5,
                    ArmorType.BOOTS, 2
            ),
            15,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            1.0f,
            0.0f,
            ModTags.Items.REPAIRS_ENCHANTED_WOOD,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "enchanted_wood"))
    );

    // Tier 2: Enchanted Cobblestone (19 total defense)
    public static final ArmorMaterial ENCHANTED_COBBLESTONE = new ArmorMaterial(
            25,
            Map.of(
                    ArmorType.HELMET, 3,
                    ArmorType.CHESTPLATE, 7,
                    ArmorType.LEGGINGS, 6,
                    ArmorType.BOOTS, 3
            ),
            20,
            SoundEvents.ARMOR_EQUIP_CHAIN,
            2.0f,
            0.0f,
            ModTags.Items.REPAIRS_ENCHANTED_COBBLESTONE,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "enchanted_cobblestone"))
    );

    // Bronze (17 total defense)
    public static final ArmorMaterial BRONZE = new ArmorMaterial(
            20,
            Map.of(
                    ArmorType.HELMET, 3,
                    ArmorType.CHESTPLATE, 6,
                    ArmorType.LEGGINGS, 5,
                    ArmorType.BOOTS, 3
            ),
            20,
            SoundEvents.ARMOR_EQUIP_IRON,
            1.0f,
            0.0f,
            ModTags.Items.REPAIRS_BRONZE,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "bronze"))
    );

    // Tin (11 total defense)
    public static final ArmorMaterial TIN = new ArmorMaterial(
            15,
            Map.of(
                    ArmorType.HELMET, 2,
                    ArmorType.CHESTPLATE, 5,
                    ArmorType.LEGGINGS, 4,
                    ArmorType.BOOTS, 2
            ),
            12,
            SoundEvents.ARMOR_EQUIP_IRON,
            0.0f,
            0.0f,
            ModTags.Items.REPAIRS_TIN,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "tin"))
    );

    // Titanium (20 total defense)
    public static final ArmorMaterial TITANIUM = new ArmorMaterial(
            35,
            Map.of(
                    ArmorType.HELMET, 3,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.LEGGINGS, 6,
                    ArmorType.BOOTS, 3
            ),
            18,
            SoundEvents.ARMOR_EQUIP_IRON,
            2.0f,
            0.0f,
            ModTags.Items.REPAIRS_TITANIUM,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "titanium"))
    );

    // Aluminum (15 total defense)
    public static final ArmorMaterial ALUMINUM = new ArmorMaterial(
            18,
            Map.of(
                    ArmorType.HELMET, 2,
                    ArmorType.CHESTPLATE, 6,
                    ArmorType.LEGGINGS, 5,
                    ArmorType.BOOTS, 2
            ),
            16,
            SoundEvents.ARMOR_EQUIP_IRON,
            0.5f,
            0.0f,
            ModTags.Items.REPAIRS_ALUMINUM,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "aluminum"))
    );

    // Steel (20 total defense)
    public static final ArmorMaterial STEEL = new ArmorMaterial(
            28,
            Map.of(
                    ArmorType.HELMET, 3,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.LEGGINGS, 6,
                    ArmorType.BOOTS, 3
            ),
            15,
            SoundEvents.ARMOR_EQUIP_IRON,
            1.5f,
            0.1f,
            ModTags.Items.REPAIRS_STEEL,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "steel"))
    );

    // Tier 3: Enchanted Diamond (23 total defense)
    public static final ArmorMaterial ENCHANTED_DIAMOND = new ArmorMaterial(
            50,
            Map.of(
                    ArmorType.HELMET, 4,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.LEGGINGS, 7,
                    ArmorType.BOOTS, 4
            ),
            30,
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            3.0f,
            0.1f,
            ModTags.Items.REPAIRS_ENCHANTED_DIAMOND,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "enchanted_diamond"))
    );

    // Tier 4: Enchanted Netherite (26 total defense)
    public static final ArmorMaterial ENCHANTED_NETHERITE = new ArmorMaterial(
            75,
            Map.of(
                    ArmorType.HELMET, 4,
                    ArmorType.CHESTPLATE, 10,
                    ArmorType.LEGGINGS, 8,
                    ArmorType.BOOTS, 4
            ),
            35,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            4.0f,
            0.3f,
            ModTags.Items.REPAIRS_ENCHANTED_NETHERITE,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "enchanted_netherite"))
    );

    // Scuba / Diving Suit (15 total defense, specialized water resistance)
    public static final ArmorMaterial SCUBA = new ArmorMaterial(
            22,
            Map.of(
                    ArmorType.HELMET, 2,
                    ArmorType.CHESTPLATE, 6,
                    ArmorType.LEGGINGS, 5,
                    ArmorType.BOOTS, 2
            ),
            14,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            0.5f,
            0.0f,
            ModTags.Items.REPAIRS_SCUBA,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "scuba"))
    );

    // Cobalt (21 total defense, lightweight & high speed)
    public static final ArmorMaterial COBALT = new ArmorMaterial(
            32,
            Map.of(
                    ArmorType.HELMET, 3,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.LEGGINGS, 7,
                    ArmorType.BOOTS, 3
            ),
            18,
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            2.0f,
            0.0f,
            ModTags.Items.REPAIRS_COBALT,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "cobalt"))
    );

    // Ardite (22 total defense, high durability & knockback resistance)
    public static final ArmorMaterial ARDITE = new ArmorMaterial(
            45,
            Map.of(
                    ArmorType.HELMET, 3,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.LEGGINGS, 8,
                    ArmorType.BOOTS, 3
            ),
            16,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            2.5f,
            0.2f,
            ModTags.Items.REPAIRS_ARDITE,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "ardite"))
    );

    // Manyullyn (25 total defense, fireproof supreme Nether armor)
    public static final ArmorMaterial MANYULLYN = new ArmorMaterial(
            60,
            Map.of(
                    ArmorType.HELMET, 4,
                    ArmorType.CHESTPLATE, 9,
                    ArmorType.LEGGINGS, 8,
                    ArmorType.BOOTS, 4
            ),
            24,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.5f,
            0.25f,
            ModTags.Items.REPAIRS_MANYULLYN,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "manyullyn"))
    );

    // Tungsten (Heavy Refractory Armor, 20 total defense + 3.0 toughness + 0.1 knockback resistance)
    public static final ArmorMaterial TUNGSTEN = new ArmorMaterial(
            38,
            Map.of(
                    ArmorType.HELMET, 3,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.LEGGINGS, 6,
                    ArmorType.BOOTS, 3
            ),
            18,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.0f,
            0.1f,
            ModTags.Items.REPAIRS_TUNGSTEN,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "tungsten"))
    );

    // Modular Power Suit Chassis (High durability, 24 total defense, 3.0 toughness, knockback resistance)
    public static final ArmorMaterial MODULAR_POWER = new ArmorMaterial(
            50,
            Map.of(
                    ArmorType.HELMET, 4,
                    ArmorType.CHESTPLATE, 9,
                    ArmorType.LEGGINGS, 7,
                    ArmorType.BOOTS, 4
            ),
            20,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.0f,
            0.15f,
            ModTags.Items.REPAIRS_MODULAR_POWER,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "modular_power"))
    );

    // Cleanroom Bunny Suit (Sterile, anti-static, zero particulate emission)
    public static final ArmorMaterial CLEANROOM_SUIT = new ArmorMaterial(
            20,
            Map.of(
                    ArmorType.HELMET, 1,
                    ArmorType.CHESTPLATE, 3,
                    ArmorType.LEGGINGS, 2,
                    ArmorType.BOOTS, 1
            ),
            15,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            0.0f,
            0.0f,
            ModTags.Items.REPAIRS_SCUBA,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "cleanroom_suit"))
    );
}

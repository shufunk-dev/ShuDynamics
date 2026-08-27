package net.enchantedwood.item;

import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.BlockTags;
import net.enchantedwood.tag.ModTags;

public class ModMaterials {
    public static final ToolMaterial ENCHANTED_WOOD = new ToolMaterial(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL,
            500,
            7.0f,
            3.0f,
            22,
            ModTags.Items.REPAIRS_ENCHANTED_WOOD
    );

    public static final ToolMaterial ENCHANTED_COBBLESTONE = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL,
            1500,
            14.0f,
            7.5f,
            30,
            ModTags.Items.REPAIRS_ENCHANTED_COBBLESTONE
    );

    public static final ToolMaterial BRONZE = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL,
            380,
            12.0f,
            2.0f,
            20,
            ModTags.Items.REPAIRS_BRONZE
    );

    public static final ToolMaterial COPPER = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL,
            200,
            5.0f,
            1.5f,
            14,
            ModTags.Items.REPAIRS_COPPER
    );

    public static final ToolMaterial TIN = new ToolMaterial(
            BlockTags.INCORRECT_FOR_STONE_TOOL,
            160,
            4.5f,
            1.0f,
            12,
            ModTags.Items.REPAIRS_TIN
    );

    public static final ToolMaterial TITANIUM = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            1800,
            9.0f,
            4.0f,
            18,
            ModTags.Items.REPAIRS_TITANIUM
    );

    public static final ToolMaterial ALUMINUM = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL,
            450,
            8.0f,
            2.5f,
            16,
            ModTags.Items.REPAIRS_ALUMINUM
    );

    public static final ToolMaterial STEEL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            1200,
            10.0f,
            3.5f,
            18,
            ModTags.Items.REPAIRS_STEEL
    );

    public static final ToolMaterial ENCHANTED_DIAMOND = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            4000,
            18.0f,
            4.0f,
            30,
            ModTags.Items.REPAIRS_ENCHANTED_DIAMOND
    );

    public static final ToolMaterial ENCHANTED_NETHERITE = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            10000,
            25.0f,
            6.0f,
            35,
            ModTags.Items.REPAIRS_ENCHANTED_NETHERITE
    );

    public static final ToolMaterial COBALT = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            1400,
            14.0f,
            3.5f,
            18,
            ModTags.Items.REPAIRS_COBALT
    );

    public static final ToolMaterial ARDITE = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            2400,
            9.0f,
            4.0f,
            16,
            ModTags.Items.REPAIRS_ARDITE
    );

    public static final ToolMaterial MANYULLYN = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            3500,
            18.0f,
            5.5f,
            24,
            ModTags.Items.REPAIRS_MANYULLYN
    );
}

package net.enchantedwood.tag;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

public class ModTags {
    public static class Items {
        public static final TagKey<Item> REPAIRS_ENCHANTED_WOOD = of("repairs_enchanted_wood");
        public static final TagKey<Item> REPAIRS_ENCHANTED_COBBLESTONE = of("repairs_enchanted_cobblestone");
        public static final TagKey<Item> REPAIRS_BRONZE = of("repairs_bronze");
        public static final TagKey<Item> REPAIRS_COPPER = of("repairs_copper");
        public static final TagKey<Item> REPAIRS_TIN = of("repairs_tin");
        public static final TagKey<Item> REPAIRS_TITANIUM = of("repairs_titanium");
        public static final TagKey<Item> REPAIRS_ALUMINUM = of("repairs_aluminum");
        public static final TagKey<Item> REPAIRS_STEEL = of("repairs_steel");
        public static final TagKey<Item> REPAIRS_ENCHANTED_DIAMOND = of("repairs_enchanted_diamond");
        public static final TagKey<Item> REPAIRS_ENCHANTED_NETHERITE = of("repairs_enchanted_netherite");

        private static TagKey<Item> of(String id) {
            return TagKey.of(RegistryKeys.ITEM, Identifier.of(EnchantedWoodMod.MOD_ID, id));
        }
    }
}

package net.enchantedwood.tag;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

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
        public static final TagKey<Item> REPAIRS_SCUBA = of("repairs_scuba");
        public static final TagKey<Item> REPAIRS_COBALT = of("repairs_cobalt");
        public static final TagKey<Item> REPAIRS_ARDITE = of("repairs_ardite");
        public static final TagKey<Item> REPAIRS_MANYULLYN = of("repairs_manyullyn");
        public static final TagKey<Item> REPAIRS_TUNGSTEN = of("repairs_tungsten");
        public static final TagKey<Item> REPAIRS_MODULAR_POWER = of("repairs_modular_power");

        private static TagKey<Item> of(String id) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, id));
        }
    }
}

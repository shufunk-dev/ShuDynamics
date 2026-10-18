package com.vexira.mysticalterrain.gear;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.MysticalBosses;
import com.vexira.mysticalterrain.gear.arsenal.Arsenal;
import com.vexira.mysticalterrain.gear.arsenal.Arsenals;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents.Modify;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import org.jspecify.annotations.Nullable;

public final class MysticGear {
   public static final Item VOID_ESSENCE = register("ender_dragon_essence", new Properties().rarity(Rarity.EPIC).fireResistant());
   public static final Map<String, MysticWeapon> WEAPONS = new LinkedHashMap<>();
   public static final Map<String, List<MysticArmor>> ARMOR = new LinkedHashMap<>();

   private MysticGear() {
   }

   public static @Nullable String armorModel(String boss) {
      Arsenal arsenal = Arsenals.of(boss);
      return arsenal == null ? null : arsenal.armorModel();
   }

   public static String prefix(String armorModel) {
      return armorModel.endsWith("_armor") ? armorModel.substring(0, armorModel.length() - "_armor".length()) : armorModel;
   }

   private static Item register(String name, Properties props) {
      ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MysticalTerrain.id(name));
      Item item = (Item)Registry.register(BuiltInRegistries.ITEM, key, new Item(props.setId(key)));
      MysticalBosses.addToTab(item);
      return item;
   }

   private static TagKey<Item> repairs(String boss) {
      return TagKey.create(Registries.ITEM, MysticalTerrain.id("repairs/" + boss));
   }

   public static void register() {
      GearSystem.init();

      for (Arsenal arsenal : Arsenals.all()) {
         for (WeaponKit kit : arsenal.weaponKits()) {
            ToolMaterial material = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2400, 9.0F, 0.0F, 18, repairs(arsenal.boss()));
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MysticalTerrain.id(kit.id()));
            Properties props = new Properties().setId(key).rarity(Rarity.EPIC).fireResistant();
            props = kit.isAxe() ? props.axe(material, kit.damage(), kit.speed()) : props.sword(material, kit.damage(), kit.speed());
            MysticWeapon weapon = (MysticWeapon)Registry.register(BuiltInRegistries.ITEM, key, new MysticWeapon(props, kit));
            WEAPONS.put(kit.id(), weapon);
            MysticalBosses.addToTab(weapon);
         }
      }

      EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
      String[] names = new String[]{"helmet", "chestplate", "leggings", "boots"};
      int[] defense = new int[]{3, 8, 6, 3};
      int[] durability = new int[]{481, 592, 555, 518};

      for (Arsenal arsenal : Arsenals.all()) {
         String boss = arsenal.boss();
         String model = arsenal.armorModel();
         ArmorKit set = arsenal.armorKit();
         List<MysticArmor> pieces = new ArrayList<>();

         for (int i = 0; i < 4; i++) {
            String id = prefix(model) + "_" + names[i];
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MysticalTerrain.id(id));
            EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slots[i]);
            Identifier mod = MysticalTerrain.id("armor." + names[i]);
            ItemAttributeModifiers attributes = ItemAttributeModifiers.builder()
               .add(Attributes.ARMOR, new AttributeModifier(mod, defense[i], Operation.ADD_VALUE), group)
               .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(mod, set.toughness(), Operation.ADD_VALUE), group)
               .add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(mod, set.knockback(), Operation.ADD_VALUE), group)
               .build();
            Properties props = new Properties()
               .setId(key)
               .rarity(Rarity.EPIC)
               .fireResistant()
               .durability(durability[i])
               .repairable(repairs(boss))
               .enchantable(18)
               .attributes(attributes)
               .component(
                  DataComponents.EQUIPPABLE,
                  Equippable.builder(slots[i])
                     .setEquipSound(SoundEvents.ARMOR_EQUIP_NETHERITE)
                     .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, MysticalTerrain.id(model)))
                     .build()
               );
            MysticArmor piece = (MysticArmor)Registry.register(BuiltInRegistries.ITEM, key, new MysticArmor(props, boss, model, slots[i]));
            pieces.add(piece);
            MysticalBosses.addToTab(piece);
         }

         ARMOR.put(boss, pieces);
      }

      LootTableEvents.MODIFY
         .register(
            (Modify)(keyx, table, source, registries) -> {
               if (keyx.identifier().equals(Identifier.withDefaultNamespace("entities/ender_dragon"))) {
                  table.withPool(
                     LootPool.lootPool()
                        .add(LootItem.lootTableItem(VOID_ESSENCE).apply(SetItemCountFunction.setCount(ContextIntProviders.between(8, 12))))
                  );
               }
            }
         );
   }
}

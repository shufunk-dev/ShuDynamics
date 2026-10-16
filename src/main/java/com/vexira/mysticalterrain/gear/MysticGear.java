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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import org.jspecify.annotations.Nullable;

public final class MysticGear {
   public static final Item VOID_ESSENCE = register("ender_dragon_essence", new Settings().rarity(Rarity.EPIC).fireproof());
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

   private static Item register(String name, Settings props) {
      RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, MysticalTerrain.id(name));
      Item item = (Item)Registry.register(Registries.ITEM, key, new Item(props.registryKey(key)));
      MysticalBosses.addToTab(item);
      return item;
   }

   private static TagKey<Item> repairs(String boss) {
      return TagKey.of(RegistryKeys.ITEM, MysticalTerrain.id("repairs/" + boss));
   }

   public static void register() {
      GearSystem.init();

      for (Arsenal arsenal : Arsenals.all()) {
         for (WeaponKit kit : arsenal.weaponKits()) {
            ToolMaterial material = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2400, 9.0F, 0.0F, 18, repairs(arsenal.boss()));
            RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, MysticalTerrain.id(kit.id()));
            Settings props = new Settings().registryKey(key).rarity(Rarity.EPIC).fireproof();
            props = kit.isAxe() ? props.axe(material, kit.damage(), kit.speed()) : props.sword(material, kit.damage(), kit.speed());
            MysticWeapon weapon = (MysticWeapon)Registry.register(Registries.ITEM, key, new MysticWeapon(props, kit));
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
            RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, MysticalTerrain.id(id));
            AttributeModifierSlot group = AttributeModifierSlot.forEquipmentSlot(slots[i]);
            Identifier mod = MysticalTerrain.id("armor." + names[i]);
            AttributeModifiersComponent attributes = AttributeModifiersComponent.builder()
               .add(EntityAttributes.ARMOR, new EntityAttributeModifier(mod, defense[i], Operation.ADD_VALUE), group)
               .add(EntityAttributes.ARMOR_TOUGHNESS, new EntityAttributeModifier(mod, set.toughness(), Operation.ADD_VALUE), group)
               .add(EntityAttributes.KNOCKBACK_RESISTANCE, new EntityAttributeModifier(mod, set.knockback(), Operation.ADD_VALUE), group)
               .build();
            Settings props = new Settings()
               .registryKey(key)
               .rarity(Rarity.EPIC)
               .fireproof()
               .maxDamage(durability[i])
               .repairable(repairs(boss))
               .enchantable(18)
               .attributeModifiers(attributes)
               .component(
                  DataComponentTypes.EQUIPPABLE,
                  EquippableComponent.builder(slots[i])
                     .equipSound(SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE)
                     .model(RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, MysticalTerrain.id(model)))
                     .build()
               );
            MysticArmor piece = (MysticArmor)Registry.register(Registries.ITEM, key, new MysticArmor(props, boss, model, slots[i]));
            pieces.add(piece);
            MysticalBosses.addToTab(piece);
         }

         ARMOR.put(boss, pieces);
      }

      LootTableEvents.MODIFY
         .register(
            (Modify)(keyx, table, source, registries) -> {
               if (keyx.getValue().equals(Identifier.ofVanilla("entities/ender_dragon"))) {
                  table.pool(
                     LootPool.builder()
                        .with(ItemEntry.builder(VOID_ESSENCE).apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(8.0F, 12.0F))))
                  );
               }
            }
         );
   }
}

package com.vexira.mysticalterrain.gear.arsenal;

import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.gear.ArmorKit;
import com.vexira.mysticalterrain.gear.WeaponKit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Arsenal {
   private final String boss;
   private final String armorModel;
   private final Vfx.Palette palette;
   private final List<WeaponKit> weapons = new ArrayList<>();
   private final ArmorKit armor;

   protected Arsenal(String boss, String armorModel, Vfx.Palette palette) {
      this.boss = boss;
      this.armorModel = armorModel;
      this.palette = palette;
      this.armor = new ArmorKit(this);
   }

   protected abstract void weapons();

   protected abstract void armor(ArmorKit var1);

   protected final WeaponKit weapon(String id, float damage, float speed) {
      WeaponKit kit = new WeaponKit(this, id, damage, speed);
      this.weapons.add(kit);
      return kit;
   }

   final void build() {
      this.weapons();
      this.armor(this.armor);
   }

   public String boss() {
      return this.boss;
   }

   public String armorModel() {
      return this.armorModel;
   }

   public Vfx.Palette palette() {
      return this.palette;
   }

   public List<WeaponKit> weaponKits() {
      return Collections.unmodifiableList(this.weapons);
   }

   public ArmorKit armorKit() {
      return this.armor;
   }
}

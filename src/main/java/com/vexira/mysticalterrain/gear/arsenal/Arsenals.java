package com.vexira.mysticalterrain.gear.arsenal;

import com.vexira.mysticalterrain.gear.WeaponKit;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class Arsenals {
   private static final Map<String, Arsenal> BY_BOSS = new LinkedHashMap<>();
   private static final Map<String, WeaponKit> WEAPONS = new LinkedHashMap<>();

   private Arsenals() {
   }

   private static void add(Arsenal arsenal) {
      arsenal.build();
      BY_BOSS.put(arsenal.boss(), arsenal);

      for (WeaponKit kit : arsenal.weaponKits()) {
         if (WEAPONS.put(kit.id(), kit) != null) {
            throw new IllegalStateException("Weapon " + kit.id() + " is declared twice");
         }
      }
   }

   public static Collection<Arsenal> all() {
      return Collections.unmodifiableCollection(BY_BOSS.values());
   }

   public static @Nullable Arsenal of(String boss) {
      return BY_BOSS.get(boss);
   }

   public static @Nullable WeaponKit weapon(String id) {
      return WEAPONS.get(id);
   }

   static {
      add(new EnderDragonArsenal());
      add(new PyraxisArsenal());
      add(new HrimthaneArsenal());
      add(new LiraelArsenal());
      add(new GrothmawArsenal());
      add(new HollowWardenArsenal());
      add(new BogrotArsenal());
      add(new SekhmarArsenal());
      add(new MycelordArsenal());
      add(new ArchonArsenal());
      add(new AstraelArsenal());
   }
}

package net.enchantedwood.sound;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

public class ModSounds {
    public static final SoundEvent CONTROLLER_HUM = registerSound("block.storage_controller.hum");

    private static SoundEvent registerSound(String name) {
        Identifier id = Identifier.of(EnchantedWoodMod.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerModSounds() {
        EnchantedWoodMod.LOGGER.info("Registering Custom Sounds for " + EnchantedWoodMod.MOD_ID);
    }
}

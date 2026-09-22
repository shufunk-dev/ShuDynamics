package net.enchantedwood.sound;

import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

public class ModSounds {
    public static final SoundEvent CONTROLLER_HUM = registerSound("block.storage_controller.hum");

    // Music Discs Sound Events
    public static final SoundEvent MUSIC_DISC_CONVERGENCE = registerSound("music.music_disc_convergence");
    public static final SoundEvent MUSIC_DISC_COLOSSUS = registerSound("music.music_disc_colossus");
    public static final SoundEvent MUSIC_DISC_OVERDRIVE = registerSound("music.music_disc_overdrive");
    public static final SoundEvent MUSIC_DISC_CLEANROOM = registerSound("music.music_disc_cleanroom");
    public static final SoundEvent MUSIC_DISC_AUTOCRAFT = registerSound("music.music_disc_autocraft");
    public static final SoundEvent MUSIC_DISC_HAVEN_BLOOM = registerSound("music.music_disc_haven_bloom");
    public static final SoundEvent MUSIC_DISC_CRUCIBLE = registerSound("music.music_disc_crucible");
    public static final SoundEvent MUSIC_DISC_STRATOSPHERE = registerSound("music.music_disc_stratosphere");
    public static final SoundEvent MUSIC_DISC_ABYSSAL = registerSound("music.music_disc_abyssal");
    public static final SoundEvent MUSIC_DISC_ANOXIC = registerSound("music.music_disc_anoxic");

    // Jukebox Song Registry Keys
    public static final RegistryKey<JukeboxSong> CONVERGENCE_SONG = ofJukeboxSong("music_disc_convergence");
    public static final RegistryKey<JukeboxSong> COLOSSUS_SONG = ofJukeboxSong("music_disc_colossus");
    public static final RegistryKey<JukeboxSong> OVERDRIVE_SONG = ofJukeboxSong("music_disc_overdrive");
    public static final RegistryKey<JukeboxSong> CLEANROOM_SONG = ofJukeboxSong("music_disc_cleanroom");
    public static final RegistryKey<JukeboxSong> AUTOCRAFT_SONG = ofJukeboxSong("music_disc_autocraft");
    public static final RegistryKey<JukeboxSong> HAVEN_BLOOM_SONG = ofJukeboxSong("music_disc_haven_bloom");
    public static final RegistryKey<JukeboxSong> CRUCIBLE_SONG = ofJukeboxSong("music_disc_crucible");
    public static final RegistryKey<JukeboxSong> STRATOSPHERE_SONG = ofJukeboxSong("music_disc_stratosphere");
    public static final RegistryKey<JukeboxSong> ABYSSAL_SONG = ofJukeboxSong("music_disc_abyssal");
    public static final RegistryKey<JukeboxSong> ANOXIC_SONG = ofJukeboxSong("music_disc_anoxic");

    private static RegistryKey<JukeboxSong> ofJukeboxSong(String name) {
        return RegistryKey.of(RegistryKeys.JUKEBOX_SONG, Identifier.of(EnchantedWoodMod.MOD_ID, name));
    }

    private static SoundEvent registerSound(String name) {
        Identifier id = Identifier.of(EnchantedWoodMod.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerModSounds() {
        EnchantedWoodMod.LOGGER.info("Registering Custom Sounds for " + EnchantedWoodMod.MOD_ID);
    }
}


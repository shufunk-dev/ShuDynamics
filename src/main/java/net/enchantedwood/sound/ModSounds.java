package net.enchantedwood.sound;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;

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
    public static final ResourceKey<JukeboxSong> CONVERGENCE_SONG = ofJukeboxSong("music_disc_convergence");
    public static final ResourceKey<JukeboxSong> COLOSSUS_SONG = ofJukeboxSong("music_disc_colossus");
    public static final ResourceKey<JukeboxSong> OVERDRIVE_SONG = ofJukeboxSong("music_disc_overdrive");
    public static final ResourceKey<JukeboxSong> CLEANROOM_SONG = ofJukeboxSong("music_disc_cleanroom");
    public static final ResourceKey<JukeboxSong> AUTOCRAFT_SONG = ofJukeboxSong("music_disc_autocraft");
    public static final ResourceKey<JukeboxSong> HAVEN_BLOOM_SONG = ofJukeboxSong("music_disc_haven_bloom");
    public static final ResourceKey<JukeboxSong> CRUCIBLE_SONG = ofJukeboxSong("music_disc_crucible");
    public static final ResourceKey<JukeboxSong> STRATOSPHERE_SONG = ofJukeboxSong("music_disc_stratosphere");
    public static final ResourceKey<JukeboxSong> ABYSSAL_SONG = ofJukeboxSong("music_disc_abyssal");
    public static final ResourceKey<JukeboxSong> ANOXIC_SONG = ofJukeboxSong("music_disc_anoxic");

    private static ResourceKey<JukeboxSong> ofJukeboxSong(String name) {
        return ResourceKey.create(Registries.JUKEBOX_SONG, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, name));
    }

    private static SoundEvent registerSound(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void registerModSounds() {
        EnchantedWoodMod.LOGGER.info("Registering Custom Sounds for " + EnchantedWoodMod.MOD_ID);
    }
}


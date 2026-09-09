package net.enchantedwood.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

public class ModStatusEffects {
    public static final RegistryEntry.Reference<StatusEffect> ACID_PROTECTION = register("acid_protection", new AcidProtectionStatusEffect());
    public static final RegistryEntry.Reference<StatusEffect> THERMAL_PROTECTION = register("thermal_protection", new ThermalProtectionStatusEffect());
    public static final RegistryEntry.Reference<StatusEffect> ATMOSPHERIC_PROTECTION = register("atmospheric_protection", new AtmosphericProtectionStatusEffect());

    // Combat & Survivalist Boss Effects
    public static final RegistryEntry.Reference<StatusEffect> VAMPIRIC_VITALITY = register("vampiric_vitality", new VampiricVitalityStatusEffect());
    public static final RegistryEntry.Reference<StatusEffect> ADRENALINE_RUSH = register("adrenaline_rush", new AdrenalineRushStatusEffect());
    public static final RegistryEntry.Reference<StatusEffect> KINETIC_DAMPENING = register("kinetic_dampening", new KineticDampeningStatusEffect());
    public static final RegistryEntry.Reference<StatusEffect> CELESTIAL_LEAP = register("celestial_leap", new CelestialLeapStatusEffect());
    public static final RegistryEntry.Reference<StatusEffect> METABOLIC_SATURATION = register("metabolic_saturation", new MetabolicSaturationStatusEffect());

    private static RegistryEntry.Reference<StatusEffect> register(String name, StatusEffect effect) {
        return Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(EnchantedWoodMod.MOD_ID, name), effect);
    }

    public static void registerModEffects() {
        EnchantedWoodMod.LOGGER.info("Registering Custom Status Effects for " + EnchantedWoodMod.MOD_ID);
    }
}

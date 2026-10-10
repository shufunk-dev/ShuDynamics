package net.enchantedwood.effect;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

public class ModStatusEffects {
    public static final Holder.Reference<MobEffect> ACID_PROTECTION = register("acid_protection", new AcidProtectionStatusEffect());
    public static final Holder.Reference<MobEffect> THERMAL_PROTECTION = register("thermal_protection", new ThermalProtectionStatusEffect());
    public static final Holder.Reference<MobEffect> ATMOSPHERIC_PROTECTION = register("atmospheric_protection", new AtmosphericProtectionStatusEffect());

    // Combat & Survivalist Boss Effects
    public static final Holder.Reference<MobEffect> VAMPIRIC_VITALITY = register("vampiric_vitality", new VampiricVitalityStatusEffect());
    public static final Holder.Reference<MobEffect> ADRENALINE_RUSH = register("adrenaline_rush", new AdrenalineRushStatusEffect());
    public static final Holder.Reference<MobEffect> KINETIC_DAMPENING = register("kinetic_dampening", new KineticDampeningStatusEffect());
    public static final Holder.Reference<MobEffect> CELESTIAL_LEAP = register("celestial_leap", new CelestialLeapStatusEffect());
    public static final Holder.Reference<MobEffect> METABOLIC_SATURATION = register("metabolic_saturation", new MetabolicSaturationStatusEffect());

    private static Holder.Reference<MobEffect> register(String name, MobEffect effect) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, name), effect);
    }

    public static void registerModEffects() {
        EnchantedWoodMod.LOGGER.info("Registering Custom Status Effects for " + EnchantedWoodMod.MOD_ID);
    }
}

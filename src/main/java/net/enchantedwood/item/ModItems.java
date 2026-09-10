package net.enchantedwood.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.*;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.item.custom.BarkskinPickaxeItem;
import net.enchantedwood.item.custom.EnchantedArmorItem;
import net.enchantedwood.item.custom.EnchantedCobblestoneArmorItem;
import net.enchantedwood.item.custom.EnchantedCobblestonePickaxeItem;
import net.enchantedwood.item.custom.EnchantedCobblestoneSwordItem;
import net.enchantedwood.item.custom.BroadAxeItem;
import net.enchantedwood.item.custom.HammerItem;
import net.enchantedwood.item.custom.LivingwoodSwordItem;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.item.custom.EnchantedRedstoneItem;
import net.enchantedwood.item.custom.GearItem;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.ConsumableComponents;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import java.util.function.Function;

public class ModItems {

    // Materials
    public static final Item INFUSED_HEARTWOOD = registerItem("infused_heartwood", Item::new);
    public static final Item ENCHANTED_DUST = registerItem("enchanted_dust", Item::new);
    public static final Item ENCHANTED_WOOD = registerItem("enchanted_wood", Item::new);
    public static final Item ENCHANTED_COAL = registerItem("enchanted_coal", Item::new);
    public static final Item ENCHANTED_REDSTONE = registerItem("enchanted_redstone", EnchantedRedstoneItem::new);
    public static final Item ENCHANTED_EMERALD = registerItem("enchanted_emerald", net.enchantedwood.item.custom.EnchantedEmeraldItem::new);
    public static final Item ENCHANTED_CAPE = registerItem("enchanted_cape", settings -> new net.enchantedwood.item.custom.EnchantedCapeItem(settings.maxCount(1)));
    public static final Item RESIN = registerItem("resin", Item::new);
    public static final Item RUBBER = registerItem("rubber", Item::new);

    // Agriculture & Crops
    public static final Item CORN = registerItem("corn", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodComponent.Builder().nutrition(3).saturationModifier(0.6f).build()),
            Text.literal("§7Can be §eRoasted §7on fire for food, or distilled in a"),
            Text.literal("§eFuel Refinery §7(§62 Corn + Empty Canister§7) into §aBiofuel§7.")
    ));
    public static final Item ROASTED_CORN = registerItem("roasted_corn", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodComponent.Builder().nutrition(7).saturationModifier(0.8f).build()),
            Text.literal("§aDelicious roasted sweet corn. Restores 7 food points.")
    ));
    public static final Item CORN_SEEDS = registerItem("corn_seeds", settings -> new net.enchantedwood.item.custom.TooltipBlockItem(
            net.enchantedwood.block.ModBlocks.CORN_CROP,
            settings,
            Text.literal("§7Plant on tilled farmland to grow 8-stage Sweet Corn."),
            Text.literal("§8Obtained by breaking wild grass or crafting with Enchanted Dust.")
    ));

    // Convergence Cuisine & Ingredients
    public static final Item RICE_SEEDS = registerItem("rice_seeds", settings -> new net.enchantedwood.item.custom.TooltipBlockItem(
            net.enchantedwood.block.ModBlocks.RICE_CROP,
            settings,
            Text.literal("§7Plant on tilled farmland or Volcanic Soil to cultivate Rice."),
            Text.literal("§8Native crop of §dThe Convergence§8. Essential ingredient for Sushi.")
    ));
    public static final Item RICE = registerItem("rice", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Grown grain from The Convergence."),
            Text.literal("§8Cook in a Smoker/Furnace or combine with water to prepare §fSushi Rice§8.")
    ));
    public static final Item SUSHI_RICE = registerItem("sushi_rice", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodComponent.Builder().nutrition(3).saturationModifier(0.4f).build()),
            Text.literal("§fSeasoned Steamed Sushi Rice."),
            Text.literal("§8The essential base for rolling authentic Sushi.")
    ));
    public static final Item CUCUMBER_SEEDS = registerItem("cucumber_seeds", settings -> new net.enchantedwood.item.custom.TooltipBlockItem(
            net.enchantedwood.block.ModBlocks.CUCUMBER_CROP,
            settings,
            Text.literal("§7Plant on tilled farmland or Volcanic Soil to grow Crisp Cucumbers."),
            Text.literal("§8Native crop of §dThe Convergence§8.")
    ));
    public static final Item CUCUMBER = registerItem("cucumber", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodComponent.Builder().nutrition(3).saturationModifier(0.5f).build()),
            Text.literal("§aFresh, crisp green cucumber."),
            Text.literal("§8Restores 3 food points. Key ingredient in vegetarian and California rolls.")
    ));
    public static final Item AVOCADO = registerItem("avocado", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodComponent.Builder().nutrition(4).saturationModifier(0.7f).build()),
            Text.literal("§2Creamy, nutrient-rich avocado fruit."),
            Text.literal("§8Harvested from Avocado Trees in §dThe Convergence§8.")
    ));
    public static final Item NORI_SHEET = registerItem("nori_sheet", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodComponent.Builder().nutrition(1).saturationModifier(0.3f).build()),
            Text.literal("§8Thin roasted seaweed sheet pressed from Dried Kelp."),
            Text.literal("§8Used to wrap sushi rolls.")
    ));

    // Sushi Rolls
    public static final Item SALMON_ROLL = registerItem("salmon_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodComponent.Builder().nutrition(7).saturationModifier(0.8f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 20 * 20, 0)))
                            .build()
            ),
            Text.literal("§6Fresh Pacific Salmon wrapped in Nori and seasoned Sushi Rice."),
            Text.literal("§bGrants Dolphin's Grace I (20s). Restores 7 hunger points.")
    ));

    public static final Item COD_ROLL = registerItem("cod_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodComponent.Builder().nutrition(6).saturationModifier(0.7f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.HASTE, 20 * 30, 0)))
                            .build()
            ),
            Text.literal("§eTender Cod Roll wrapped in crispy Nori."),
            Text.literal("§eGrants Haste I (30s). Restores 6 hunger points.")
    ));

    public static final Item AVOCADO_CUCUMBER_ROLL = registerItem("avocado_cucumber_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodComponent.Builder().nutrition(6).saturationModifier(0.8f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 35, 0)))
                            .build()
            ),
            Text.literal("§aRefreshing Vegetarian Maki with creamy avocado & cucumber."),
            Text.literal("§aGrants Speed I (35s). Restores 6 hunger points.")
    ));

    public static final Item CALIFORNIA_ROLL = registerItem("california_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodComponent.Builder().nutrition(9).saturationModifier(0.9f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 8, 0)))
                            .build()
            ),
            Text.literal("§dClassic California Roll with fish, avocado, and cucumber."),
            Text.literal("§dGrants Regeneration I (8s). Restores 9 hunger points.")
    ));

    public static final Item GARDEN_ROLL = registerItem("garden_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodComponent.Builder().nutrition(8).saturationModifier(0.8f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 20 * 45, 0)))
                            .build()
            ),
            Text.literal("§6Garden Delight Roll made with sweet carrots, cucumber & avocado."),
            Text.literal("§9Grants Night Vision (45s). Restores 8 hunger points.")
    ));

    public static final Item MASTER_RAINBOW_ROLL = registerItem("master_rainbow_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodComponent.Builder().nutrition(12).saturationModifier(1.0f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(java.util.List.of(
                                    new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 12, 1),
                                    new StatusEffectInstance(StatusEffects.SPEED, 20 * 45, 1),
                                    new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 20 * 45, 0)
                            )))
                            .build()
            ),
            Text.literal("§5✦ Master Rainbow Sushi Platter ✦"),
            Text.literal("§7The ultimate culinary synthesis: Salmon, Cod, Avocado, Cucumber, and Carrots!"),
            Text.literal("§dGrants Regeneration II, Speed II, and Dolphin's Grace.")
    ));

    // Protective Survival Foods (Organic Hypospray Alternatives)
    public static final Item ALKALINE_DETOX_ROLL = registerItem("alkaline_detox_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodComponent.Builder().nutrition(8).saturationModifier(0.9f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(java.util.List.of(
                                    new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.ACID_PROTECTION, 20 * 300, 0),
                                    new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 120, 1)
                            )))
                            .build()
            ),
            Text.literal("§a✦ Alkaline Acid-Shield Roll ✦"),
            Text.literal("§7Infused with crisp cucumber & creamy alkalizing avocado."),
            Text.literal("§e✦ Buff: §aAcid Protection §f(5:00)"),
            Text.literal("§8 • 100% Immunity to Poison, Wither & Acid damage"),
            Text.literal("§8 • Continuous negative status effect cleansing"),
            Text.literal("§e✦ Buff: §6Absorption II §f(2:00) §8(+4 Golden Hearts)"),
            Text.literal("§b✦ Organic alternative to chemical Hyposprays.")
    ));

    public static final Item VOLCANIC_DRAGON_ROLL = registerItem("volcanic_dragon_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodComponent.Builder().nutrition(8).saturationModifier(0.9f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(java.util.List.of(
                                    new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION, 20 * 300, 0),
                                    new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20 * 300, 0)
                            )))
                            .build()
            ),
            Text.literal("§6✦ Volcanic Dragon Roll ✦"),
            Text.literal("§7Spicy magma-infused thermal sushi roll."),
            Text.literal("§e✦ Buff: §6Thermal Protection §f(5:00)"),
            Text.literal("§8 • 100% Immunity to Fire, Lava, Magma & Freezing"),
            Text.literal("§8 • Grants Molten Lava Buoyancy & Auto-Extinguish"),
            Text.literal("§e✦ Buff: §cFire Resistance §f(5:00)"),
            Text.literal("§b✦ Organic alternative to chemical Hyposprays.")
    ));

    public static final Item HIGH_ALTITUDE_KELP_ROLL = registerItem("high_altitude_kelp_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodComponent.Builder().nutrition(8).saturationModifier(0.9f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(java.util.List.of(
                                    new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.ATMOSPHERIC_PROTECTION, 20 * 300, 0),
                                    new StatusEffectInstance(StatusEffects.WATER_BREATHING, 20 * 300, 0)
                            )))
                            .build()
            ),
            Text.literal("§b✦ High-Altitude Kelp Roll ✦"),
            Text.literal("§7Deep-sea kelp pressed with oxygen-dense mountain vegetables."),
            Text.literal("§e✦ Buff: §bAtmospheric Protection §f(5:00)"),
            Text.literal("§8 • Infinite Oxygen: Immunity to Drowning & Suffocation"),
            Text.literal("§8 • Shields against high-altitude vacuum collapse"),
            Text.literal("§e✦ Buff: §9Water Breathing §f(5:00)"),
            Text.literal("§b✦ Organic alternative to chemical Hyposprays.")
    ));

    public static final Item SURVIVALIST_BENTO = registerItem("survivalist_bento", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(16).food(
                    new FoodComponent.Builder().nutrition(14).saturationModifier(1.0f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(java.util.List.of(
                                    new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.ACID_PROTECTION, 20 * 480, 0),
                                    new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION, 20 * 480, 0),
                                    new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.ATMOSPHERIC_PROTECTION, 20 * 480, 0),
                                    new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20 * 480, 0),
                                    new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 30, 1),
                                    new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 180, 2)
                            )))
                            .build()
            ),
            Text.literal("§5✦ Master Survivalist Bento Box ✦"),
            Text.literal("§dThe pinnacle of environmental hazard culinary defense."),
            Text.literal("§e✦ Active Tri-Shield Protection §f(8:00):"),
            Text.literal("§a  ✔ Acid Protection §8(Poison, Wither & Corrosive Immunity)"),
            Text.literal("§6  ✔ Thermal Protection §8(Fire, Lava & Freeze Immunity)"),
            Text.literal("§b  ✔ Atmospheric Protection §8(Infinite Air & Vacuum Immunity)"),
            Text.literal("§e✦ Buffs: §dRegeneration II §f(0:30) §7+ §eAbsorption III §f(3:00)"),
            Text.literal("§f✦ The ultimate organic alternative to all chemical Hyposprays!")
    ));

    // Convergence Exotic Ingredients & Boss Combat Culinary Dishes
    public static final Item WASABI_ROOT = registerItem("wasabi_root", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodComponent.Builder().nutrition(2).saturationModifier(0.3f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.HASTE, 20 * 10, 0)))
                            .build()
            ),
            Text.literal("§aConvergence Wasabi Root"),
            Text.literal("§7Pungent wild rhizome harvested along convergence riverbanks."),
            Text.literal("§8Essential spice for crafting Fresh Wasabi Nigiri.")
    ));

    public static final Item DRAGON_FRUIT = registerItem("dragon_fruit", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodComponent.Builder().nutrition(4).saturationModifier(0.6f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 5, 0)))
                            .build()
            ),
            Text.literal("§dExotic Dragon Fruit"),
            Text.literal("§7Vibrant magenta fruit rich in cellular life-essence."),
            Text.literal("§8Essential ingredient in Dragon Fruit Pitaya Bowls.")
    ));

    public static final Item STARFRUIT = registerItem("starfruit", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodComponent.Builder().nutrition(4).saturationModifier(0.6f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 20 * 10, 0)))
                            .build()
            ),
            Text.literal("§eResonance Starfruit"),
            Text.literal("§7Luminescent star-shaped fruit charged with anti-gravitational energy."),
            Text.literal("§8Essential ingredient in Resonance Starfruit Tarts.")
    ));

    public static final Item PITAYA_BOWL = registerItem("pitaya_bowl", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(16).food(
                    new FoodComponent.Builder().nutrition(10).saturationModifier(1.0f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(java.util.List.of(
                                    new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.VAMPIRIC_VITALITY, 20 * 300, 0),
                                    new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 300, 0),
                                    new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 20, 1)
                            )))
                            .build()
            ),
            Text.literal("§d✦ Dragon Fruit Pitaya Bowl ✦"),
            Text.literal("§7Thick, nutrient-dense smoothie bowl crowned with dragon fruit & avocado."),
            Text.literal("§e✦ Buff: §cVampiric Vitality §f(5:00)"),
            Text.literal("§8 • 15% Melee Lifesteal: heals attacker for 15% of physical damage dealt"),
            Text.literal("§8 • Critical organic sustain during drawn-out boss encounters"),
            Text.literal("§e✦ Buffs: §cStrength I §f(5:00) §7+ §dRegeneration II §f(0:20)"),
            Text.literal("§b✦ Organic combat meal for warrior builds.")
    ));

    public static final Item WASABI_NIGIRI = registerItem("wasabi_nigiri", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodComponent.Builder().nutrition(8).saturationModifier(0.9f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(java.util.List.of(
                                    new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.ADRENALINE_RUSH, 20 * 300, 0),
                                    new StatusEffectInstance(StatusEffects.HASTE, 20 * 300, 1)
                            )))
                            .build()
            ),
            Text.literal("§a✦ Fresh Wasabi Nigiri ✦"),
            Text.literal("§7Premium sushi rice topped with freshly grated convergence wasabi root."),
            Text.literal("§e✦ Buff: §aAdrenaline Rush §f(5:00)"),
            Text.literal("§8 • +25% Attack Speed & +20% Movement Speed"),
            Text.literal("§8 • Cleanses & immunizes against Slowness, Mining Fatigue & Weakness"),
            Text.literal("§e✦ Buff: §eHaste II §f(5:00)"),
            Text.literal("§b✦ High-velocity rush for aggressive combat.")
    ));

    public static final Item GOLDEN_HONEY_MOCHI = registerItem("golden_honey_mochi", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodComponent.Builder().nutrition(8).saturationModifier(1.0f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(java.util.List.of(
                                    new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING, 20 * 300, 0),
                                    new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 300, 0),
                                    new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 180, 1)
                            )))
                            .build()
            ),
            Text.literal("§6✦ Golden Honey Mochi ✦"),
            Text.literal("§7Chewy pounded rice mochi infused with sweet honey & golden dust."),
            Text.literal("§e✦ Buff: §6Kinetic Dampening §f(5:00)"),
            Text.literal("§8 • 100% Knockback Resistance & Impact Shock Absorption"),
            Text.literal("§8 • Eliminates boss slam recoil and zero kinetic crash damage"),
            Text.literal("§e✦ Buffs: §9Resistance I §f(5:00) §7+ §eAbsorption II §f(3:00)"),
            Text.literal("§b✦ Indispensable frontline defense against boss slams.")
    ));

    public static final Item STARFRUIT_TART = registerItem("starfruit_tart", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodComponent.Builder().nutrition(8).saturationModifier(0.9f).build(),
                    ConsumableComponents.food()
                            .consumeEffect(new ApplyEffectsConsumeEffect(java.util.List.of(
                                    new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.CELESTIAL_LEAP, 20 * 300, 0),
                                    new StatusEffectInstance(StatusEffects.SPEED, 20 * 300, 0)
                            )))
                            .build()
            ),
            Text.literal("§e✦ Resonance Starfruit Tart ✦"),
            Text.literal("§7Baked celestial tart glazed with crystalline starfruit essence."),
            Text.literal("§e✦ Buff: §eCelestial Leap §f(5:00)"),
            Text.literal("§8 • 3-Block High Jump & Featherweight Slow-Fall Gliding"),
            Text.literal("§8 • Complete immunity to all fall damage"),
            Text.literal("§8 • Perfect for leaping over boss ground slams & shockwaves"),
            Text.literal("§e✦ Buff: §bSpeed I §f(5:00)"),
            Text.literal("§b✦ Aerial tactical mobility in boss arenas.")
    ));

    // Convergence Mob Variant Spawn Eggs
    public static final Item CONVERGENCE_ZOMBIE_SPAWN_EGG = registerItem("convergence_zombie_spawn_egg",
            settings -> new net.enchantedwood.item.custom.ConvergenceSpawnEggItem(net.enchantedwood.entity.ModEntities.CONVERGENCE_ZOMBIE, settings));

    public static final Item CONVERGENCE_SKELETON_SPAWN_EGG = registerItem("convergence_skeleton_spawn_egg",
            settings -> new net.enchantedwood.item.custom.ConvergenceSpawnEggItem(net.enchantedwood.entity.ModEntities.CONVERGENCE_SKELETON, settings));

    public static final Item CONVERGENCE_CREEPER_SPAWN_EGG = registerItem("convergence_creeper_spawn_egg",
            settings -> new net.enchantedwood.item.custom.ConvergenceSpawnEggItem(net.enchantedwood.entity.ModEntities.CONVERGENCE_CREEPER, settings));

    public static final Item CONVERGENCE_SPIDER_SPAWN_EGG = registerItem("convergence_spider_spawn_egg",
            settings -> new net.enchantedwood.item.custom.ConvergenceSpawnEggItem(net.enchantedwood.entity.ModEntities.CONVERGENCE_SPIDER, settings));

    // The Resonance Colossus: Summoning Key & Boss Relics
    public static final Item CORE_OF_AWAKENING = registerItem("core_of_awakening", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(16),
            Text.literal("§5✦ Core of Awakening ✦"),
            Text.literal("§dCrystalline relic charged with primordial resonance energy."),
            Text.literal("§7Right-click on the §eResonance Altar §7in §dThe Convergence §7to awaken"),
            Text.literal("§7or revive §5The Resonance Colossus§7."),
            Text.literal("§8 • Protected by Arena Leash & Retreat Protocol")
    ));

    public static final Item RESONANCE_CLEAVER = registerItem("resonance_cleaver",
            settings -> new net.enchantedwood.item.custom.ResonanceCleaverItem(settings.sword(ModMaterials.MANYULLYN, 6.0f, -3.0f).maxDamage(2500)));

    public static final Item SINGULARITY_STAFF = registerItem("singularity_staff",
            settings -> new net.enchantedwood.item.custom.SingularityStaffItem(settings.maxDamage(1500)));

    public static final Item ETERNAL_BENTO_BOX = registerItem("eternal_bento_box",
            settings -> new net.enchantedwood.item.custom.EternalBentoBoxItem(settings.maxCount(1)));

    // Advanced Medical Technology: Hypospray, Essences & Cartridges
    public static final Item HYPOSPRAY = registerItem("hypospray",
            settings -> new net.enchantedwood.item.custom.HyposprayItem(settings.maxCount(1)));

    public static final Item EMPTY_CARTRIDGE = registerItem("empty_cartridge", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(64),
            Text.literal("§9Empty Hypospray Cartridge"),
            Text.literal("§7Sterile titanium & reinforced glass ampoule."),
            Text.literal("§8Used in the §eChemical Synthesizer §8to compound medical inoculants.")
    ));

    // Centrifuged Chemical Essences
    public static final Item ALKALINE_BASE_EXTRACT = registerItem("alkaline_base_extract", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(64),
            Text.literal("§aAlkaline Base Extract"),
            Text.literal("§7Concentrated alkalizing solution extracted in the §eIndustrial Centrifuge§7."),
            Text.literal("§8Synthesizes Acid-Neutralizing Cartridges.")
    ));

    public static final Item CRYO_THERMAL_EXTRACT = registerItem("cryo_thermal_extract", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(64),
            Text.literal("§6Cryo-Thermal Extract"),
            Text.literal("§7Endothermic compound separated from volcanic matter in the §eIndustrial Centrifuge§7."),
            Text.literal("§8Synthesizes Endothermic Heat-Buffer Cartridges.")
    ));

    public static final Item OXYGENATED_EXTRACT = registerItem("oxygenated_extract", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(64),
            Text.literal("§bOxygenated Extract"),
            Text.literal("§7Purified oxygen-binding hemoglobin essence from the §eIndustrial Centrifuge§7."),
            Text.literal("§8Synthesizes Hyper-Oxygenation Cartridges.")
    ));

    public static final Item CELLULAR_NANITE_EXTRACT = registerItem("cellular_nanite_extract", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(64),
            Text.literal("§dCellular Nanite Extract"),
            Text.literal("§7Bio-regenerative stem essence extracted from rare flora in the §eIndustrial Centrifuge§7."),
            Text.literal("§8Synthesizes Nanite Trauma Inoculants.")
    ));

    public static final Item ADRENAL_ESSENCE = registerItem("adrenal_essence", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(64),
            Text.literal("§eAdrenal Essence"),
            Text.literal("§7Hyper-metabolic stimulant concentrate from the §eIndustrial Centrifuge§7."),
            Text.literal("§8Synthesizes Adrenaline Combat Stims.")
    ));

    // Medical Hypospray Cartridges
    public static final Item ACID_NEUTRALIZING_CARTRIDGE = registerItem("acid_neutralizing_cartridge",
            settings -> new net.enchantedwood.item.custom.HyposprayCartridgeItem(settings.maxCount(16), net.enchantedwood.item.custom.HyposprayCartridgeItem.Type.ACID_NEUTRALIZING));

    public static final Item HEAT_BUFFER_CARTRIDGE = registerItem("heat_buffer_cartridge",
            settings -> new net.enchantedwood.item.custom.HyposprayCartridgeItem(settings.maxCount(16), net.enchantedwood.item.custom.HyposprayCartridgeItem.Type.HEAT_BUFFER));

    public static final Item HYPER_OXYGENATION_CARTRIDGE = registerItem("hyper_oxygenation_cartridge",
            settings -> new net.enchantedwood.item.custom.HyposprayCartridgeItem(settings.maxCount(16), net.enchantedwood.item.custom.HyposprayCartridgeItem.Type.HYPER_OXYGENATION));

    public static final Item NANITE_TRAUMA_CARTRIDGE = registerItem("nanite_trauma_cartridge",
            settings -> new net.enchantedwood.item.custom.HyposprayCartridgeItem(settings.maxCount(16), net.enchantedwood.item.custom.HyposprayCartridgeItem.Type.NANITE_TRAUMA));

    public static final Item ADRENALINE_STIM_CARTRIDGE = registerItem("adrenaline_stim_cartridge",
            settings -> new net.enchantedwood.item.custom.HyposprayCartridgeItem(settings.maxCount(16), net.enchantedwood.item.custom.HyposprayCartridgeItem.Type.ADRENALINE_STIM));

    // Petrochemicals & Fuels
    public static final Item CRUDE_OIL_SLUDGE = registerItem("crude_oil_sludge", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Distill in a §eFuel Refinery §7with an §fEmpty Gas Canister§7."),
            Text.literal("§8Outputs: §6Gasoline Canister §8+ §8Mineral Tar §8byproduct.")
    ));
    public static final Item MINERAL_TAR = registerItem("mineral_tar", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Petrochemical byproduct used to synthesize §8Asphalt Blocks§7."),
            Text.literal("§8Craft with Cobblestone/Deepslate + Gravel.")
    ));
    public static final Item BIOFUEL_CANISTER = registerItem("biofuel_canister", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(16),
            Text.literal("§aEco-Friendly Ethanol Fuel §7for §eATV Engines§7."),
            Text.literal("§8Synthesized in Fuel Refinery from Corn, Wheat, or Potatoes.")
    ));
    public static final Item GASOLINE_CANISTER = registerItem("gasoline_canister", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(16),
            Text.literal("§6Refined Hydrocarbon Fuel §7for §eATV Engines§7."),
            Text.literal("§8Combine with 2 Corn in Fuel Refinery for §dHigh-Octane Racing Fuel§8.")
    ));
    public static final Item HIGH_OCTANE_FUEL_CANISTER = registerItem("high_octane_fuel_canister", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(16),
            Text.literal("§dPremium Racing Fuel §7providing maximum acceleration & top speed."),
            Text.literal("§8Required for Titanium Twin-Turbo ATV Engines.")
    ));

    // Highway & Road Transition Clay Molds
    public static final Item UNFIRED_CONCRETE_CURB = registerItem("unfired_concrete_curb", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Unfired clay mold for concrete curbs."),
            Text.literal("§8Smelt in a Furnace to fire into a finished Concrete Curb.")
    ));
    public static final Item UNFIRED_ROAD_TRANSITION_RAMP = registerItem("unfired_road_transition_ramp", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Unfired sloped clay mold for road transition ramps."),
            Text.literal("§8Smelt in a Furnace to fire into a finished Road Transition Ramp.")
    ));

    // Modular All-Terrain Vehicle (ATV) & Components
    public static final Item ATV_ITEM = registerItem("atv", settings -> new net.enchantedwood.item.custom.AtvItem(settings.maxCount(1)));
    public static final Item ATV_SEAT = registerItem("atv_seat", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Core component for assembling an All-Terrain Vehicle."),
            Text.literal("§8Craft with Leather + Black Wool + Iron Ingot.")
    ));
    public static final Item RUBBER_TIRE = registerItem("rubber_tire", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Standard vulcanized rubber tire with balanced all-terrain grip."),
            Text.literal("§8Craft with 4 Rubber around 1 Iron Ingot.")
    ));
    public static final Item STEEL_RIM_TIRE = registerItem("steel_rim_tire", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Reinforced steel rim tire with improved highway stability."),
            Text.literal("§8Craft with Rubber Tire + Steel Ingot.")
    ));
    public static final Item TITANIUM_STUDDED_TIRE = registerItem("titanium_studded_tire", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§bStudded ice-grip tire for maximum traction on snow & ice."),
            Text.literal("§8Craft with Rubber Tire + Titanium Ingot.")
    ));
    public static final Item COPPER_ATV_ENGINE = registerItem("copper_atv_engine", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§eStarter Engine §8(~25 km/h) §7• Fuel: Biofuel / Gasoline"),
            Text.literal("§8Craft with Copper Ingots, Piston, Copper Gear, and Redstone.")
    ));
    public static final Item ALUMINUM_ATV_ENGINE = registerItem("aluminum_atv_engine", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§bAgile V4 Engine §8(~40 km/h) §7• Fuel: Biofuel / Gasoline"),
            Text.literal("§8Craft with Aluminum Ingots, Piston, Aluminum Gear, and Redstone.")
    ));
    public static final Item STEEL_ATV_ENGINE = registerItem("steel_atv_engine", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7High-Torque V8 Engine §8(~55 km/h) §7• Fuel: Gasoline / High-Octane"),
            Text.literal("§8Craft with Steel Ingots, Piston, Steel Gear, and Enchanted Redstone.")
    ));
    public static final Item TITANIUM_ATV_ENGINE = registerItem("titanium_atv_engine", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§dTwin-Turbo Nitro Engine §8(~80 km/h) §7• Fuel: High-Octane Racing Fuel"),
            Text.literal("§8Craft with Titanium Ingots, Steel ATV Engine, and Titanium Gear.")
    ));
    public static final Item ALUMINUM_SUSPENSION = registerItem("aluminum_suspension", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§bSport suspension providing §e0.8-block step-up §b& agile responsive handling."),
            Text.literal("§8Craft with Aluminum Ingots + Iron Bars.")
    ));
    public static final Item STEEL_SUSPENSION = registerItem("steel_suspension", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Heavy suspension providing §e1.0-block step-up §7& fall absorption."),
            Text.literal("§8Craft with Steel Ingots + Iron Bars.")
    ));
    public static final Item TITANIUM_SUSPENSION = registerItem("titanium_suspension", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Heavy-duty suspension providing §e1.5-block step-up §7& full fall negation."),
            Text.literal("§8Craft with Titanium Ingots + Iron Bars.")
    ));
    public static final Item ALUMINUM_ATV_CHASSIS = registerItem("aluminum_atv_chassis", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§bLightweight racing chassis frame for agile handling."),
            Text.literal("§8Craft with 7 Aluminum Ingots in an H-shape.")
    ));
    public static final Item STEEL_ATV_CHASSIS = registerItem("steel_atv_chassis", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Reinforced steel chassis offering balanced structural durability."),
            Text.literal("§8Craft with 7 Steel Ingots in an H-shape.")
    ));
    public static final Item TITANIUM_ATV_CHASSIS = registerItem("titanium_atv_chassis", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§dHeavy hazard-shielded chassis built for dimensional exploration."),
            Text.literal("§8Craft with 7 Titanium Ingots in an H-shape.")
    ));
    public static final Item SEALED_HAZARD_CANOPY = registerItem("sealed_hazard_canopy", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(1).fireproof(),
            Text.literal("§b✦ Pressurized Environmental Cockpit Canopy"),
            Text.literal("§7Reinforced composite canopy with hermetic seals and air scrubbers:"),
            Text.literal("§f• 100% Protection from Acid Rain & Corrosive Waters"),
            Text.literal("§f• 100% Protection from Volcanic Heat & Caldera Scalds"),
            Text.literal("§f• 100% Protection from Atmospheric Hypoxia & Anoxic Caves"),
            Text.literal("§8Install into ATV trunk or cargo slot to seal the cabin.")
    ));
    public static final Item SMALL_CARGO_TRUNK = registerItem("small_cargo_trunk", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Adds §e9 inventory slots §7to the ATV rear cargo rack."),
            Text.literal("§8Craft with Iron Ingots around a Chest.")
    ));
    public static final Item MEDIUM_CARGO_TRUNK = registerItem("medium_cargo_trunk", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Adds §e18 inventory slots §7to the ATV rear cargo rack."),
            Text.literal("§8Craft with Steel Ingots around a Small Cargo Trunk.")
    ));
    public static final Item LARGE_CARGO_TRUNK = registerItem("large_cargo_trunk", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Adds §e27 inventory slots §7to the ATV rear cargo rack."),
            Text.literal("§8Craft with Titanium Ingots around a Medium Cargo Trunk.")
    ));

    // ATV Mining Drill Bits (Replaceable Modules)
    public static final Item IRON_DRILL_BIT = registerItem("iron_drill_bit", settings -> new net.enchantedwood.item.custom.DrillBitItem(net.enchantedwood.item.custom.DrillBitItem.DrillTier.IRON, settings));
    public static final Item STEEL_DRILL_BIT = registerItem("steel_drill_bit", settings -> new net.enchantedwood.item.custom.DrillBitItem(net.enchantedwood.item.custom.DrillBitItem.DrillTier.STEEL, settings));
    public static final Item DIAMOND_DRILL_BIT = registerItem("diamond_drill_bit", settings -> new net.enchantedwood.item.custom.DrillBitItem(net.enchantedwood.item.custom.DrillBitItem.DrillTier.DIAMOND, settings));
    public static final Item TITANIUM_DRILL_BIT = registerItem("titanium_drill_bit", settings -> new net.enchantedwood.item.custom.DrillBitItem(net.enchantedwood.item.custom.DrillBitItem.DrillTier.TITANIUM, settings));
    public static final Item NETHERITE_DRILL_BIT = registerItem("netherite_drill_bit", settings -> new net.enchantedwood.item.custom.DrillBitItem(net.enchantedwood.item.custom.DrillBitItem.DrillTier.NETHERITE, settings.fireproof()));

    // ATV Lumberjack Tree Harvesters (Replaceable Modules)
    public static final Item IRON_TREE_SAW = registerItem("iron_tree_saw", settings -> new net.enchantedwood.item.custom.TreeSawItem(net.enchantedwood.item.custom.TreeSawItem.SawTier.IRON, settings));
    public static final Item STEEL_TREE_SAW = registerItem("steel_tree_saw", settings -> new net.enchantedwood.item.custom.TreeSawItem(net.enchantedwood.item.custom.TreeSawItem.SawTier.STEEL, settings));
    public static final Item DIAMOND_TREE_SAW = registerItem("diamond_tree_saw", settings -> new net.enchantedwood.item.custom.TreeSawItem(net.enchantedwood.item.custom.TreeSawItem.SawTier.DIAMOND, settings));
    public static final Item TITANIUM_TREE_SAW = registerItem("titanium_tree_saw", settings -> new net.enchantedwood.item.custom.TreeSawItem(net.enchantedwood.item.custom.TreeSawItem.SawTier.TITANIUM, settings));
    public static final Item NETHERITE_TREE_SAW = registerItem("netherite_tree_saw", settings -> new net.enchantedwood.item.custom.TreeSawItem(net.enchantedwood.item.custom.TreeSawItem.SawTier.NETHERITE, settings.fireproof()));

    // ATV Agricultural Crop Harvesters (Replaceable Modules)
    public static final Item IRON_CROP_HARVESTER = registerItem("iron_crop_harvester", settings -> new net.enchantedwood.item.custom.CropHarvesterItem(net.enchantedwood.item.custom.CropHarvesterItem.HarvesterTier.IRON, settings));
    public static final Item STEEL_CROP_HARVESTER = registerItem("steel_crop_harvester", settings -> new net.enchantedwood.item.custom.CropHarvesterItem(net.enchantedwood.item.custom.CropHarvesterItem.HarvesterTier.STEEL, settings));
    public static final Item DIAMOND_CROP_HARVESTER = registerItem("diamond_crop_harvester", settings -> new net.enchantedwood.item.custom.CropHarvesterItem(net.enchantedwood.item.custom.CropHarvesterItem.HarvesterTier.DIAMOND, settings));
    public static final Item TITANIUM_CROP_HARVESTER = registerItem("titanium_crop_harvester", settings -> new net.enchantedwood.item.custom.CropHarvesterItem(net.enchantedwood.item.custom.CropHarvesterItem.HarvesterTier.TITANIUM, settings));
    public static final Item NETHERITE_CROP_HARVESTER = registerItem("netherite_crop_harvester", settings -> new net.enchantedwood.item.custom.CropHarvesterItem(net.enchantedwood.item.custom.CropHarvesterItem.HarvesterTier.NETHERITE, settings.fireproof()));

    // ATV Automotive Headlights (Core Required Part)
    public static final Item HALOGEN_HEADLIGHTS = registerItem("halogen_headlights", settings -> new net.enchantedwood.item.custom.HeadlightsItem(net.enchantedwood.item.custom.HeadlightsItem.LightTier.HALOGEN, settings));
    public static final Item LED_FLOODLIGHTS = registerItem("led_floodlights", settings -> new net.enchantedwood.item.custom.HeadlightsItem(net.enchantedwood.item.custom.HeadlightsItem.LightTier.LED, settings));
    public static final Item XENON_HIGH_BEAMS = registerItem("xenon_high_beams", settings -> new net.enchantedwood.item.custom.HeadlightsItem(net.enchantedwood.item.custom.HeadlightsItem.LightTier.XENON, settings));

    // Base Gears
    public static final Item IRON_GEAR = registerItem("iron_gear", settings -> new GearItem(GearTier.IRON, false, settings));
    public static final Item COPPER_GEAR = registerItem("copper_gear", settings -> new GearItem(GearTier.COPPER, false, settings));
    public static final Item BRONZE_GEAR = registerItem("bronze_gear", settings -> new GearItem(GearTier.BRONZE, false, settings));
    public static final Item GOLD_GEAR = registerItem("gold_gear", settings -> new GearItem(GearTier.GOLD, false, settings));
    public static final Item TITANIUM_GEAR = registerItem("titanium_gear", settings -> new GearItem(GearTier.TITANIUM, false, settings));
    public static final Item DIAMOND_GEAR = registerItem("diamond_gear", settings -> new GearItem(GearTier.DIAMOND, false, settings));
    public static final Item NETHERITE_GEAR = registerItem("netherite_gear", settings -> new GearItem(GearTier.NETHERITE, false, settings));

    // Enchanted Gears
    public static final Item ENCHANTED_IRON_GEAR = registerItem("enchanted_iron_gear", settings -> new GearItem(GearTier.ENCHANTED_IRON, true, settings));
    public static final Item ENCHANTED_COPPER_GEAR = registerItem("enchanted_copper_gear", settings -> new GearItem(GearTier.COPPER, true, settings));
    public static final Item ENCHANTED_BRONZE_GEAR = registerItem("enchanted_bronze_gear", settings -> new GearItem(GearTier.BRONZE, true, settings));
    public static final Item ENCHANTED_GOLD_GEAR = registerItem("enchanted_gold_gear", settings -> new GearItem(GearTier.GOLD, true, settings));
    public static final Item ENCHANTED_TITANIUM_GEAR = registerItem("enchanted_titanium_gear", settings -> new GearItem(GearTier.TITANIUM, true, settings));
    public static final Item ENCHANTED_DIAMOND_GEAR = registerItem("enchanted_diamond_gear", settings -> new GearItem(GearTier.DIAMOND, true, settings));
    public static final Item ENCHANTED_NETHERITE_GEAR = registerItem("enchanted_netherite_gear", settings -> new GearItem(GearTier.NETHERITE, true, settings));

    // Raw Ores & Materials
    public static final Item COPPER_BUCKET = registerItem("copper_bucket", settings -> new net.enchantedwood.item.custom.CopperBucketItem(net.minecraft.fluid.Fluids.EMPTY, settings.maxCount(16)));
    public static final Item COPPER_WATER_BUCKET = registerItem("copper_water_bucket", settings -> new net.enchantedwood.item.custom.CopperBucketItem(net.minecraft.fluid.Fluids.WATER, settings.maxCount(1).recipeRemainder(COPPER_BUCKET)));
    public static final Item COPPER_LAVA_BUCKET = registerItem("copper_lava_bucket", settings -> new net.enchantedwood.item.custom.CopperBucketItem(net.minecraft.fluid.Fluids.LAVA, settings.maxCount(1).recipeRemainder(COPPER_BUCKET)));
    public static final Item ENCHANTED_LAVA_BUCKET = registerItem("enchanted_lava_bucket", settings -> new net.enchantedwood.item.custom.CopperBucketItem(net.minecraft.fluid.Fluids.LAVA, settings.maxCount(1).recipeRemainder(Items.BUCKET)));
    public static final Item ENCHANTED_COPPER_LAVA_BUCKET = registerItem("enchanted_copper_lava_bucket", settings -> new net.enchantedwood.item.custom.CopperBucketItem(net.minecraft.fluid.Fluids.LAVA, settings.maxCount(1).recipeRemainder(COPPER_BUCKET)));
    public static final Item COPPER_NUGGET = registerItem("copper_nugget", Item::new);
    public static final Item RAW_TIN = registerItem("raw_tin", Item::new);
    public static final Item TIN_INGOT = registerItem("tin_ingot", Item::new);
    public static final Item TIN_NUGGET = registerItem("tin_nugget", Item::new);
    public static final Item BRONZE_INGOT = registerItem("bronze_ingot", Item::new);
    public static final Item BRONZE_NUGGET = registerItem("bronze_nugget", Item::new);
    public static final Item RAW_TITANIUM = registerItem("raw_titanium", Item::new);
    public static final Item TITANIUM_INGOT = registerItem("titanium_ingot", Item::new);
    public static final Item TITANIUM_NUGGET = registerItem("titanium_nugget", Item::new);
    public static final Item TITANIUM_ROLLER = registerItem("titanium_roller", Item::new);
    public static final Item ENCHANTED_DIAMOND = registerItem("enchanted_diamond", Item::new);
    public static final Item ENCHANTED_NETHERITE_INGOT = registerItem("enchanted_netherite_ingot",
            settings -> new Item(settings.fireproof()) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });

    // Enchanted Health Upgrades (Heart Lockets)
    public static final Item ENCHANTED_HEART = registerItem("enchanted_heart", settings -> new net.enchantedwood.item.custom.EnchantedHeartItem(2.0f, settings.maxCount(1)));
    public static final Item IRON_ENCHANTED_HEART = registerItem("iron_enchanted_heart", settings -> new net.enchantedwood.item.custom.EnchantedHeartItem(6.0f, settings.maxCount(1)));
    public static final Item GOLD_ENCHANTED_HEART = registerItem("gold_enchanted_heart", settings -> new net.enchantedwood.item.custom.EnchantedHeartItem(10.0f, settings.maxCount(1)));
    public static final Item DIAMOND_ENCHANTED_HEART = registerItem("diamond_enchanted_heart", settings -> new net.enchantedwood.item.custom.EnchantedHeartItem(14.0f, settings.maxCount(1)));
    public static final Item NETHERITE_ENCHANTED_HEART = registerItem("netherite_enchanted_heart", settings -> new net.enchantedwood.item.custom.EnchantedHeartItem(20.0f, settings.maxCount(1).fireproof()));

    // Storage Crystals & Wireless Access
    public static final Item STORAGE_CRYSTAL_1K = registerItem("storage_crystal_1k", settings -> new net.enchantedwood.item.custom.StorageCrystalItem(1000, settings.maxCount(1)));
    public static final Item STORAGE_CRYSTAL_4K = registerItem("storage_crystal_4k", settings -> new net.enchantedwood.item.custom.StorageCrystalItem(4000, settings.maxCount(1)));
    public static final Item STORAGE_CRYSTAL_16K = registerItem("storage_crystal_16k", settings -> new net.enchantedwood.item.custom.StorageCrystalItem(16000, settings.maxCount(1)));
    public static final Item STORAGE_CRYSTAL_64K = registerItem("storage_crystal_64k", settings -> new net.enchantedwood.item.custom.StorageCrystalItem(64000, settings.maxCount(1)));
    public static final Item WIRELESS_STORAGE_CRYSTAL = registerItem("wireless_storage_crystal", settings -> new net.enchantedwood.item.custom.WirelessStorageCrystalItem(settings.maxCount(1)));
    public static final Item CHUNK_LOADER_MODULE = registerItem("chunk_loader_module", settings -> new Item(settings.maxCount(1)));
    public static final Item INTERDIMENSIONAL_CARD = registerItem("interdimensional_card", settings -> new Item(settings.maxCount(1)));

    // Ore Dusts
    public static final Item IRON_DUST = registerItem("iron_dust", Item::new);
    public static final Item COPPER_DUST = registerItem("copper_dust", Item::new);
    public static final Item TIN_DUST = registerItem("tin_dust", Item::new);
    public static final Item BRONZE_DUST = registerItem("bronze_dust", Item::new);
    public static final Item TITANIUM_DUST = registerItem("titanium_dust", Item::new);
    public static final Item BAUXITE_DUST = registerItem("bauxite_dust", Item::new);
    public static final Item GOLD_DUST = registerItem("gold_dust", Item::new);
    public static final Item DIAMOND_DUST = registerItem("diamond_dust", Item::new);
    public static final Item NETHERITE_DUST = registerItem("netherite_dust", Item::new);
    public static final Item EMERALD_DUST = registerItem("emerald_dust", Item::new);
    public static final Item COAL_DUST = registerItem("coal_dust", Item::new);
    public static final Item QUARTZ_DUST = registerItem("quartz_dust", Item::new);

    // Microelectronics & Modular Power Suit Components (ShuDynamics 2.0)
    public static final Item SILICON = registerItem("silicon", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7High-purity semiconductor ingot smelted from Quartz Dust."),
            Text.literal("§8Press in a Hydraulic Press to manufacture Silicon Wafers.")
    ));
    public static final Item SILICON_WAFER = registerItem("silicon_wafer", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Ultra-thin polished semiconductor substrate."),
            Text.literal("§8Foundation for printing Modular Power Suit micro-circuits.")
    ));
    public static final Item BASIC_COMPUTER_CHIP = registerItem("basic_computer_chip", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§eTier 1 Micro-Controller"),
            Text.literal("§7Essential logic board for Power Suit chassis & battery power routing."),
            Text.literal("§8Controls internal FE distribution across modular suit pieces.")
    ));
    public static final Item ADVANCED_COMPUTER_CHIP = registerItem("advanced_computer_chip", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§bTier 2 Environmental Processor"),
            Text.literal("§7High-frequency logic processor for active suit anomaly protection:"),
            Text.literal("§f• Atmospheric filtration (acid rain, vacuum / no air)"),
            Text.literal("§f• Thermal regulation (extreme heat & extreme cold biomes)")
    ));
    public static final Item QUANTUM_COMPUTER_CHIP = registerItem("quantum_computer_chip", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.fireproof(),
            Text.literal("§dTier 3 Quantum Core"),
            Text.literal("§7Dimensional computing unit engineered to neutralize severe anomalies:"),
            Text.literal("§f• Spatial distortion dampening & gravity stabilization"),
            Text.literal("§8Required for elite deep-dimension Power Suit modules.")
    ));

    // Modular Power Suit (ShuDynamics 2.0)
    public static final Item MODULAR_POWER_HELMET = registerItem("modular_power_helmet", settings ->
            new net.enchantedwood.item.custom.ModularPowerArmorItem(EquipmentType.HELMET, settings.armor(ModArmorMaterials.MODULAR_POWER, EquipmentType.HELMET).fireproof()));
    public static final Item MODULAR_POWER_CHESTPLATE = registerItem("modular_power_chestplate", settings ->
            new net.enchantedwood.item.custom.ModularPowerArmorItem(EquipmentType.CHESTPLATE, settings.armor(ModArmorMaterials.MODULAR_POWER, EquipmentType.CHESTPLATE).fireproof()));
    public static final Item MODULAR_POWER_LEGGINGS = registerItem("modular_power_leggings", settings ->
            new net.enchantedwood.item.custom.ModularPowerArmorItem(EquipmentType.LEGGINGS, settings.armor(ModArmorMaterials.MODULAR_POWER, EquipmentType.LEGGINGS).fireproof()));
    public static final Item MODULAR_POWER_BOOTS = registerItem("modular_power_boots", settings ->
            new net.enchantedwood.item.custom.ModularPowerArmorItem(EquipmentType.BOOTS, settings.armor(ModArmorMaterials.MODULAR_POWER, EquipmentType.BOOTS).fireproof()));
    public static final Item NANITE_REPAIR_MATRIX = registerItem("nanite_repair_matrix",
            net.enchantedwood.item.custom.NaniteRepairMatrixItem::new);
    public static final Item HYDROGEN_THRUSTER_MODULE = registerItem("hydrogen_thruster_module", settings ->
            new net.enchantedwood.item.custom.SuitModuleItem(settings, "Hydrogen Thrusters", java.util.List.of(
                    Text.literal("§b• Chemical Rocket Flight Mode §8(Spacebar to fly)"),
                    Text.literal("§7• Refuels automatically from Hydrogen Canisters in inventory"),
                    Text.literal("§e• Safe Glide / Fall Dampening included")
            )));
    public static final Item ION_REPULSOR_MODULE = registerItem("ion_repulsor_module", settings ->
            new net.enchantedwood.item.custom.SuitModuleItem(settings, "Quantum Ion Repulsors", java.util.List.of(
                    Text.literal("§d• Iron Man Repulsor Flight §8(Spacebar to fly / hover)"),
                    Text.literal("§e• Consumes 25 FE / tick (500 FE/s) directly from Battery"),
                    Text.literal("§a• 100% Electric — Zero Fuel Canisters Required!"),
                    Text.literal("§7• Complete Fall Damage Negation")
            )));
    public static final Item NIGHT_VISION_MODULE = registerItem("night_vision_module", settings ->
            new net.enchantedwood.item.custom.SuitModuleItem(settings, "Adaptive Night Vision HUD", java.util.List.of(
                    Text.literal("§b• Optical HUD Night Vision"),
                    Text.literal("§7• Automatically engages when ambient light ≤ 6"),
                    Text.literal("§7• Powers down in illuminated areas (≥ 9 light)"),
                    Text.literal("§e• Energy Cost: §f2 FE / tick §7(only while active in dark)"),
                    Text.literal("§8• Compatible with: §fModular Power Helmet")
            )));
    public static final Item SPEED_SERVO_MODULE = registerItem("speed_servo_module", settings ->
            new net.enchantedwood.item.custom.SuitModuleItem(settings, "Speed Servos", java.util.List.of(
                    Text.literal("§b• Overclocks leg hydraulics for high-speed sprint locomotion"),
                    Text.literal("§f• Grants §bSpeed II §fwhile moving"),
                    Text.literal("§e• Energy Cost: §f2 FE / tick §7while sprinting/moving"),
                    Text.literal("§8• Compatible with: §fModular Power Leggings")
            )));
    public static final Item STEP_ASSIST_MODULE = registerItem("step_assist_module", settings ->
            new net.enchantedwood.item.custom.SuitModuleItem(settings, "Hydraulic Step-Assist", java.util.List.of(
                    Text.literal("§b• Pneumatic servos allow seamless 1.0-block auto-stepping"),
                    Text.literal("§f• Walk up full blocks smoothly without jumping"),
                    Text.literal("§e• Energy Cost: §f1 FE / sec §7while walking"),
                    Text.literal("§8• Compatible with: §fModular Power Boots")
            )));
    public static final Item HIGH_JUMP_MODULE = registerItem("high_jump_module", settings ->
            new net.enchantedwood.item.custom.SuitModuleItem(settings, "High-Jump Actuators", java.util.List.of(
                    Text.literal("§b• Neo-Titanium pneumatic coils propel user upwards"),
                    Text.literal("§f• Grants §bJump Boost II §f(jump over 2.5 blocks)"),
                    Text.literal("§a• 100% Fall Damage Negation §7upon landing"),
                    Text.literal("§e• Energy Cost: §f1 FE / tick §7(while jumping or falling)"),
                    Text.literal("§8• Compatible with: §fModular Power Boots")
            )));
    public static final Item ACID_PROOF_PLATING = registerItem("acid_proof_plating",
            settings -> new net.enchantedwood.item.custom.AcidProofPlatingItem(settings.maxCount(1).fireproof()));

    // Phase 2: Metallurgy & Gas Items
    public static final Item RAW_BAUXITE = registerItem("raw_bauxite", Item::new);
    public static final Item ALUMINUM_INGOT = registerItem("aluminum_ingot", Item::new);
    public static final Item ALUMINUM_NUGGET = registerItem("aluminum_nugget", Item::new);
    public static final Item ALUMINUM_GEAR = registerItem("aluminum_gear", settings -> new GearItem(GearTier.ALUMINUM, false, settings));
    public static final Item ENCHANTED_ALUMINUM_GEAR = registerItem("enchanted_aluminum_gear", settings -> new GearItem(GearTier.ALUMINUM, true, settings));
    public static final Item EMPTY_GAS_CANISTER = registerItem("empty_gas_canister", settings -> new Item(settings.maxCount(16)));
    public static final Item OXYGEN_CANISTER = registerItem("oxygen_canister", settings -> new Item(settings.maxCount(16)));
    public static final Item HYDROGEN_CANISTER = registerItem("hydrogen_canister", settings -> new net.enchantedwood.item.custom.HydrogenCanisterItem(settings.maxCount(16)));
    public static final Item HYDROGEN_JETPACK = registerItem("hydrogen_jetpack", settings -> new net.enchantedwood.item.custom.HydrogenJetpackItem(settings.armor(ModArmorMaterials.ALUMINUM, EquipmentType.CHESTPLATE)));
    public static final Item OXY_HYDROGEN_TORCH = registerItem("oxy_hydrogen_torch", settings -> new net.enchantedwood.item.custom.OxyHydrogenTorchItem(settings));

    // Phase 3: Coke Coal & Steel Metallurgy
    public static final Item COKE_COAL = registerItem("coke_coal", Item::new);
    public static final Item STEEL_INGOT = registerItem("steel_ingot", Item::new);
    public static final Item STEEL_NUGGET = registerItem("steel_nugget", Item::new);
    public static final Item STEEL_DUST = registerItem("steel_dust", Item::new);
    public static final Item STEEL_GEAR = registerItem("steel_gear", settings -> new GearItem(GearTier.STEEL, false, settings));
    public static final Item ENCHANTED_STEEL_GEAR = registerItem("enchanted_steel_gear", settings -> new GearItem(GearTier.STEEL, true, settings));

    // Nether Metallurgy: Tungsten, Cobalt, Ardite & Manyullyn
    public static final Item RAW_TUNGSTEN = registerItem("raw_tungsten", Item::new);
    public static final Item TUNGSTEN_INGOT = registerItem("tungsten_ingot", Item::new);
    public static final Item TUNGSTEN_NUGGET = registerItem("tungsten_nugget", Item::new);
    public static final Item TUNGSTEN_DUST = registerItem("tungsten_dust", Item::new);
    public static final Item TUNGSTEN_PLATE = registerItem("tungsten_plate", Item::new);
    public static final Item TUNGSTEN_CARBIDE_INGOT = registerItem("tungsten_carbide_ingot", settings -> new Item(settings.fireproof()));

    public static final Item RAW_COBALT = registerItem("raw_cobalt", Item::new);
    public static final Item COBALT_INGOT = registerItem("cobalt_ingot", Item::new);
    public static final Item COBALT_NUGGET = registerItem("cobalt_nugget", Item::new);
    public static final Item COBALT_DUST = registerItem("cobalt_dust", Item::new);
    public static final Item COBALT_PLATE = registerItem("cobalt_plate", Item::new);

    public static final Item RAW_ARDITE = registerItem("raw_ardite", Item::new);
    public static final Item ARDITE_INGOT = registerItem("ardite_ingot", Item::new);
    public static final Item ARDITE_NUGGET = registerItem("ardite_nugget", Item::new);
    public static final Item ARDITE_DUST = registerItem("ardite_dust", Item::new);
    public static final Item ARDITE_PLATE = registerItem("ardite_plate", Item::new);

    public static final Item MANYULLYN_INGOT = registerItem("manyullyn_ingot", settings -> new Item(settings.fireproof()));
    public static final Item MANYULLYN_NUGGET = registerItem("manyullyn_nugget", settings -> new Item(settings.fireproof()));
    public static final Item MANYULLYN_DUST = registerItem("manyullyn_dust", settings -> new Item(settings.fireproof()));
    public static final Item MANYULLYN_PLATE = registerItem("manyullyn_plate", settings -> new Item(settings.fireproof()));

    // Volcanic Minerals & Byproducts
    public static final Item SULFUR_DUST = registerItem("sulfur_dust", Item::new);
    public static final Item VOLCANIC_ASH = registerItem("volcanic_ash", Item::new);
    public static final Item VOLCANIC_FERTILIZER = registerItem("volcanic_fertilizer", net.enchantedwood.item.custom.VolcanicFertilizerItem::new);
    public static final Item FIRE_CRYSTAL = registerItem("fire_crystal", settings -> new Item(settings.fireproof()));

    // Convergence Minerals, Crystals & Ores
    public static final Item RAW_FLUORITE = registerItem("raw_fluorite", Item::new);
    public static final Item FLUORITE_CRYSTAL = registerItem("fluorite_crystal", Item::new);

    public static final Item RAW_ZIRCONIA = registerItem("raw_zirconia", Item::new);
    public static final Item ZIRCONIA_NODULE = registerItem("zirconia_nodule", settings -> new Item(settings.fireproof()));

    public static final Item RAW_TANTALUM = registerItem("raw_tantalum", Item::new);
    public static final Item TANTALUM_INGOT = registerItem("tantalum_ingot", Item::new);
    public static final Item TANTALUM_DUST = registerItem("tantalum_dust", Item::new);

    public static final Item RAW_HAFNIUM = registerItem("raw_hafnium", Item::new);
    public static final Item HAFNIUM_INGOT = registerItem("hafnium_ingot", settings -> new Item(settings.fireproof()));
    public static final Item HAFNIUM_DUST = registerItem("hafnium_dust", settings -> new Item(settings.fireproof()));

    public static final Item RAW_NEODYMIUM = registerItem("raw_neodymium", Item::new);
    public static final Item NEODYMIUM_MAGNET = registerItem("neodymium_magnet", Item::new);
    public static final Item NEODYMIUM_DUST = registerItem("neodymium_dust", Item::new);

    public static final Item RAW_AEROGEL = registerItem("raw_aerogel", Item::new);
    public static final Item AEROGEL_SHARD = registerItem("aerogel_shard", Item::new);

    // Convergence Superalloys
    public static final Item TAN_TI_INGOT = registerItem("tan_ti_ingot", Item::new);
    public static final Item HAFNIUM_TUNGSTEN_CARBIDE_INGOT = registerItem("hafnium_tungsten_carbide_ingot", settings -> new Item(settings.fireproof()));
    public static final Item NEO_TITANIUM_INGOT = registerItem("neo_titanium_ingot", Item::new);

    // Steel Tools & Weapons
    public static final Item STEEL_SWORD = registerItem("steel_sword", settings -> new Item(settings.sword(ModMaterials.STEEL, 3.5f, -2.4f)));
    public static final Item STEEL_PICKAXE = registerItem("steel_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.STEEL, 1.5f, -2.8f)));
    public static final Item STEEL_AXE = registerItem("steel_axe", settings -> new Item(settings.axe(ModMaterials.STEEL, 6.5f, -3.0f)));
    public static final Item STEEL_SHOVEL = registerItem("steel_shovel", settings -> new Item(settings.shovel(ModMaterials.STEEL, 2.0f, -3.0f)));
    public static final Item STEEL_HOE = registerItem("steel_hoe", settings -> new Item(settings.hoe(ModMaterials.STEEL, -2.0f, -1.0f)));
    public static final Item STEEL_HAMMER = registerItem("steel_hammer", settings -> new HammerItem(settings.pickaxe(ModMaterials.STEEL, 5.0f, -3.0f)));
    public static final Item STEEL_BROAD_AXE = registerItem("steel_broad_axe", settings -> new BroadAxeItem(settings.axe(ModMaterials.STEEL, 7.5f, -3.1f)));

    // Steel Armor
    public static final Item STEEL_HELMET = registerItem("steel_helmet", settings -> new Item(settings.armor(ModArmorMaterials.STEEL, EquipmentType.HELMET)));
    public static final Item STEEL_CHESTPLATE = registerItem("steel_chestplate", settings -> new Item(settings.armor(ModArmorMaterials.STEEL, EquipmentType.CHESTPLATE)));
    public static final Item STEEL_LEGGINGS = registerItem("steel_leggings", settings -> new Item(settings.armor(ModArmorMaterials.STEEL, EquipmentType.LEGGINGS)));
    public static final Item STEEL_BOOTS = registerItem("steel_boots", settings -> new Item(settings.armor(ModArmorMaterials.STEEL, EquipmentType.BOOTS)));

    // Cobalt Tools & Weapons (High attack speed & velocity)
    public static final Item COBALT_SWORD = registerItem("cobalt_sword", settings -> new Item(settings.sword(ModMaterials.COBALT, 3.5f, -2.2f)));
    public static final Item COBALT_PICKAXE = registerItem("cobalt_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.COBALT, 1.5f, -2.6f)));
    public static final Item COBALT_AXE = registerItem("cobalt_axe", settings -> new Item(settings.axe(ModMaterials.COBALT, 6.0f, -2.8f)));
    public static final Item COBALT_SHOVEL = registerItem("cobalt_shovel", settings -> new Item(settings.shovel(ModMaterials.COBALT, 1.5f, -2.8f)));
    public static final Item COBALT_HOE = registerItem("cobalt_hoe", settings -> new Item(settings.hoe(ModMaterials.COBALT, -2.0f, 0.0f)));
    public static final Item COBALT_HAMMER = registerItem("cobalt_hammer", settings -> new HammerItem(settings.pickaxe(ModMaterials.COBALT, 4.5f, -2.8f)));
    public static final Item COBALT_BROAD_AXE = registerItem("cobalt_broad_axe", settings -> new BroadAxeItem(settings.axe(ModMaterials.COBALT, 7.0f, -2.9f)));

    // Cobalt Armor (Lightweight agility)
    public static final Item COBALT_HELMET = registerItem("cobalt_helmet", settings -> new Item(settings.armor(ModArmorMaterials.COBALT, EquipmentType.HELMET)));
    public static final Item COBALT_CHESTPLATE = registerItem("cobalt_chestplate", settings -> new Item(settings.armor(ModArmorMaterials.COBALT, EquipmentType.CHESTPLATE)));
    public static final Item COBALT_LEGGINGS = registerItem("cobalt_leggings", settings -> new Item(settings.armor(ModArmorMaterials.COBALT, EquipmentType.LEGGINGS)));
    public static final Item COBALT_BOOTS = registerItem("cobalt_boots", settings -> new Item(settings.armor(ModArmorMaterials.COBALT, EquipmentType.BOOTS)));

    // Ardite Tools & Weapons (Heavy durability & stonebound)
    public static final Item ARDITE_SWORD = registerItem("ardite_sword", settings -> new Item(settings.sword(ModMaterials.ARDITE, 4.0f, -2.5f)));
    public static final Item ARDITE_PICKAXE = registerItem("ardite_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.ARDITE, 2.0f, -2.9f)));
    public static final Item ARDITE_AXE = registerItem("ardite_axe", settings -> new Item(settings.axe(ModMaterials.ARDITE, 7.0f, -3.1f)));
    public static final Item ARDITE_SHOVEL = registerItem("ardite_shovel", settings -> new Item(settings.shovel(ModMaterials.ARDITE, 2.0f, -3.0f)));
    public static final Item ARDITE_HOE = registerItem("ardite_hoe", settings -> new Item(settings.hoe(ModMaterials.ARDITE, -1.0f, -1.0f)));
    public static final Item ARDITE_HAMMER = registerItem("ardite_hammer", settings -> new HammerItem(settings.pickaxe(ModMaterials.ARDITE, 5.5f, -3.1f)));
    public static final Item ARDITE_BROAD_AXE = registerItem("ardite_broad_axe", settings -> new BroadAxeItem(settings.axe(ModMaterials.ARDITE, 8.0f, -3.2f)));

    // Ardite Armor (Heavy fortitude)
    public static final Item ARDITE_HELMET = registerItem("ardite_helmet", settings -> new Item(settings.armor(ModArmorMaterials.ARDITE, EquipmentType.HELMET)));
    public static final Item ARDITE_CHESTPLATE = registerItem("ardite_chestplate", settings -> new Item(settings.armor(ModArmorMaterials.ARDITE, EquipmentType.CHESTPLATE)));
    public static final Item ARDITE_LEGGINGS = registerItem("ardite_leggings", settings -> new Item(settings.armor(ModArmorMaterials.ARDITE, EquipmentType.LEGGINGS)));
    public static final Item ARDITE_BOOTS = registerItem("ardite_boots", settings -> new Item(settings.armor(ModArmorMaterials.ARDITE, EquipmentType.BOOTS)));

    // Manyullyn Tools & Weapons (Supreme Nether Masterwork - Fireproof)
    public static final Item MANYULLYN_SWORD = registerItem("manyullyn_sword", settings -> new Item(settings.sword(ModMaterials.MANYULLYN, 6.0f, -2.4f).fireproof()));
    public static final Item MANYULLYN_PICKAXE = registerItem("manyullyn_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.MANYULLYN, 2.5f, -2.7f).fireproof()));
    public static final Item MANYULLYN_AXE = registerItem("manyullyn_axe", settings -> new Item(settings.axe(ModMaterials.MANYULLYN, 8.0f, -2.9f).fireproof()));
    public static final Item MANYULLYN_SHOVEL = registerItem("manyullyn_shovel", settings -> new Item(settings.shovel(ModMaterials.MANYULLYN, 2.5f, -2.9f).fireproof()));
    public static final Item MANYULLYN_HOE = registerItem("manyullyn_hoe", settings -> new Item(settings.hoe(ModMaterials.MANYULLYN, -1.0f, 0.0f).fireproof()));
    public static final Item MANYULLYN_HAMMER = registerItem("manyullyn_hammer", settings -> new HammerItem(settings.pickaxe(ModMaterials.MANYULLYN, 7.0f, -2.9f).fireproof()));
    public static final Item MANYULLYN_BROAD_AXE = registerItem("manyullyn_broad_axe", settings -> new BroadAxeItem(settings.axe(ModMaterials.MANYULLYN, 9.0f, -3.0f).fireproof()));

    // Manyullyn Armor (Supreme Nether Masterwork - Fireproof)
    public static final Item MANYULLYN_HELMET = registerItem("manyullyn_helmet", settings -> new Item(settings.armor(ModArmorMaterials.MANYULLYN, EquipmentType.HELMET).fireproof()));
    public static final Item MANYULLYN_CHESTPLATE = registerItem("manyullyn_chestplate", settings -> new Item(settings.armor(ModArmorMaterials.MANYULLYN, EquipmentType.CHESTPLATE).fireproof()));
    public static final Item MANYULLYN_LEGGINGS = registerItem("manyullyn_leggings", settings -> new Item(settings.armor(ModArmorMaterials.MANYULLYN, EquipmentType.LEGGINGS).fireproof()));
    public static final Item MANYULLYN_BOOTS = registerItem("manyullyn_boots", settings -> new Item(settings.armor(ModArmorMaterials.MANYULLYN, EquipmentType.BOOTS).fireproof()));

    // Tungsten Tools & Weapons (Heavy Refractory)
    public static final Item TUNGSTEN_SWORD = registerItem("tungsten_sword", settings -> new Item(settings.sword(ModMaterials.TUNGSTEN, 4.5f, -2.4f)));
    public static final Item TUNGSTEN_PICKAXE = registerItem("tungsten_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.TUNGSTEN, 2.0f, -2.8f)));
    public static final Item TUNGSTEN_AXE = registerItem("tungsten_axe", settings -> new Item(settings.axe(ModMaterials.TUNGSTEN, 7.5f, -3.0f)));
    public static final Item TUNGSTEN_SHOVEL = registerItem("tungsten_shovel", settings -> new Item(settings.shovel(ModMaterials.TUNGSTEN, 2.0f, -3.0f)));
    public static final Item TUNGSTEN_HOE = registerItem("tungsten_hoe", settings -> new Item(settings.hoe(ModMaterials.TUNGSTEN, -1.5f, -0.5f)));
    public static final Item TUNGSTEN_HAMMER = registerItem("tungsten_hammer", settings -> new HammerItem(settings.pickaxe(ModMaterials.TUNGSTEN, 6.0f, -3.0f)));
    public static final Item TUNGSTEN_BROAD_AXE = registerItem("tungsten_broad_axe", settings -> new BroadAxeItem(settings.axe(ModMaterials.TUNGSTEN, 8.0f, -3.1f)));

    // Tungsten Armor (High Toughness & Knockback Resistance)
    public static final Item TUNGSTEN_HELMET = registerItem("tungsten_helmet", settings -> new Item(settings.armor(ModArmorMaterials.TUNGSTEN, EquipmentType.HELMET)));
    public static final Item TUNGSTEN_CHESTPLATE = registerItem("tungsten_chestplate", settings -> new Item(settings.armor(ModArmorMaterials.TUNGSTEN, EquipmentType.CHESTPLATE)));
    public static final Item TUNGSTEN_LEGGINGS = registerItem("tungsten_leggings", settings -> new Item(settings.armor(ModArmorMaterials.TUNGSTEN, EquipmentType.LEGGINGS)));
    public static final Item TUNGSTEN_BOOTS = registerItem("tungsten_boots", settings -> new Item(settings.armor(ModArmorMaterials.TUNGSTEN, EquipmentType.BOOTS)));

    // Aluminum Tools & Weapons
    public static final Item ALUMINUM_SWORD = registerItem("aluminum_sword", settings -> new Item(settings.sword(ModMaterials.ALUMINUM, 3.0f, -2.4f)));
    public static final Item ALUMINUM_PICKAXE = registerItem("aluminum_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.ALUMINUM, 1.0f, -2.8f)));
    public static final Item ALUMINUM_AXE = registerItem("aluminum_axe", settings -> new Item(settings.axe(ModMaterials.ALUMINUM, 6.0f, -3.1f)));
    public static final Item ALUMINUM_SHOVEL = registerItem("aluminum_shovel", settings -> new Item(settings.shovel(ModMaterials.ALUMINUM, 1.5f, -3.0f)));
    public static final Item ALUMINUM_HOE = registerItem("aluminum_hoe", settings -> new Item(settings.hoe(ModMaterials.ALUMINUM, -2.0f, -1.0f)));
    public static final Item ALUMINUM_HAMMER = registerItem("aluminum_hammer", settings -> new HammerItem(settings.pickaxe(ModMaterials.ALUMINUM, 4.0f, -3.0f)));
    public static final Item ALUMINUM_BROAD_AXE = registerItem("aluminum_broad_axe", settings -> new BroadAxeItem(settings.axe(ModMaterials.ALUMINUM, 6.5f, -3.1f)));

    // Aluminum Armor
    public static final Item ALUMINUM_HELMET = registerItem("aluminum_helmet", settings -> new Item(settings.armor(ModArmorMaterials.ALUMINUM, EquipmentType.HELMET)));
    public static final Item ALUMINUM_CHESTPLATE = registerItem("aluminum_chestplate", settings -> new Item(settings.armor(ModArmorMaterials.ALUMINUM, EquipmentType.CHESTPLATE)));
    public static final Item ALUMINUM_LEGGINGS = registerItem("aluminum_leggings", settings -> new Item(settings.armor(ModArmorMaterials.ALUMINUM, EquipmentType.LEGGINGS)));
    public static final Item ALUMINUM_BOOTS = registerItem("aluminum_boots", settings -> new Item(settings.armor(ModArmorMaterials.ALUMINUM, EquipmentType.BOOTS)));

    // Bronze Tools & Weapons
    public static final Item BRONZE_SWORD = registerItem("bronze_sword", settings -> new Item(settings.sword(ModMaterials.BRONZE, 3.0f, -2.4f)));
    public static final Item BRONZE_PICKAXE = registerItem("bronze_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.BRONZE, 1.0f, -2.8f)));
    public static final Item BRONZE_AXE = registerItem("bronze_axe", settings -> new Item(settings.axe(ModMaterials.BRONZE, 6.0f, -3.1f)));
    public static final Item BRONZE_SHOVEL = registerItem("bronze_shovel", settings -> new Item(settings.shovel(ModMaterials.BRONZE, 1.5f, -3.0f)));
    public static final Item BRONZE_HOE = registerItem("bronze_hoe", settings -> new Item(settings.hoe(ModMaterials.BRONZE, -2.0f, -1.0f)));

    // Bronze Armor
    public static final Item BRONZE_HELMET = registerItem("bronze_helmet", settings -> new Item(settings.armor(ModArmorMaterials.BRONZE, EquipmentType.HELMET)));
    public static final Item BRONZE_CHESTPLATE = registerItem("bronze_chestplate", settings -> new Item(settings.armor(ModArmorMaterials.BRONZE, EquipmentType.CHESTPLATE)));
    public static final Item BRONZE_LEGGINGS = registerItem("bronze_leggings", settings -> new Item(settings.armor(ModArmorMaterials.BRONZE, EquipmentType.LEGGINGS)));
    public static final Item BRONZE_BOOTS = registerItem("bronze_boots", settings -> new Item(settings.armor(ModArmorMaterials.BRONZE, EquipmentType.BOOTS)));

    // Tin Tools & Weapons
    public static final Item TIN_SWORD = registerItem("tin_sword", settings -> new Item(settings.sword(ModMaterials.TIN, 2.5f, -2.4f)));
    public static final Item TIN_PICKAXE = registerItem("tin_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.TIN, 1.0f, -2.8f)));
    public static final Item TIN_AXE = registerItem("tin_axe", settings -> new Item(settings.axe(ModMaterials.TIN, 5.5f, -3.2f)));
    public static final Item TIN_SHOVEL = registerItem("tin_shovel", settings -> new Item(settings.shovel(ModMaterials.TIN, 1.0f, -3.0f)));
    public static final Item TIN_HOE = registerItem("tin_hoe", settings -> new Item(settings.hoe(ModMaterials.TIN, -2.0f, -1.0f)));

    // Tin Armor
    public static final Item TIN_HELMET = registerItem("tin_helmet", settings -> new Item(settings.armor(ModArmorMaterials.TIN, EquipmentType.HELMET)));
    public static final Item TIN_CHESTPLATE = registerItem("tin_chestplate", settings -> new Item(settings.armor(ModArmorMaterials.TIN, EquipmentType.CHESTPLATE)));
    public static final Item TIN_LEGGINGS = registerItem("tin_leggings", settings -> new Item(settings.armor(ModArmorMaterials.TIN, EquipmentType.LEGGINGS)));
    public static final Item TIN_BOOTS = registerItem("tin_boots", settings -> new Item(settings.armor(ModArmorMaterials.TIN, EquipmentType.BOOTS)));

    // Titanium Tools & Weapons
    public static final Item TITANIUM_SWORD = registerItem("titanium_sword", settings -> new Item(settings.sword(ModMaterials.TITANIUM, 4.0f, -2.4f)));
    public static final Item TITANIUM_PICKAXE = registerItem("titanium_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.TITANIUM, 2.0f, -2.8f)));
    public static final Item TITANIUM_AXE = registerItem("titanium_axe", settings -> new Item(settings.axe(ModMaterials.TITANIUM, 7.0f, -3.0f)));
    public static final Item TITANIUM_SHOVEL = registerItem("titanium_shovel", settings -> new Item(settings.shovel(ModMaterials.TITANIUM, 2.5f, -3.0f)));
    public static final Item TITANIUM_HOE = registerItem("titanium_hoe", settings -> new Item(settings.hoe(ModMaterials.TITANIUM, -1.0f, 0.0f)));

    // Titanium Armor
    public static final Item TITANIUM_HELMET = registerItem("titanium_helmet", settings -> new Item(settings.armor(ModArmorMaterials.TITANIUM, EquipmentType.HELMET)));
    public static final Item TITANIUM_CHESTPLATE = registerItem("titanium_chestplate", settings -> new Item(settings.armor(ModArmorMaterials.TITANIUM, EquipmentType.CHESTPLATE)));
    public static final Item TITANIUM_LEGGINGS = registerItem("titanium_leggings", settings -> new Item(settings.armor(ModArmorMaterials.TITANIUM, EquipmentType.LEGGINGS)));
    public static final Item TITANIUM_BOOTS = registerItem("titanium_boots", settings -> new Item(settings.armor(ModArmorMaterials.TITANIUM, EquipmentType.BOOTS)));



    // Tools & Weapons
    public static final Item LIVINGWOOD_SWORD = registerItem("livingwood_sword",
            settings -> new LivingwoodSwordItem(settings.sword(ModMaterials.ENCHANTED_WOOD, 3.0f, -2.4f)));

    public static final Item BARKSKIN_PICKAXE = registerItem("barkskin_pickaxe",
            settings -> new BarkskinPickaxeItem(settings.pickaxe(ModMaterials.ENCHANTED_WOOD, 1.0f, -2.8f)));

    public static final Item IRONWOOD_AXE = registerItem("ironwood_axe",
            settings -> new AxeItem(ModMaterials.ENCHANTED_WOOD, 6.0f, -3.0f, settings) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });

    public static final Item VERDANT_SHOVEL = registerItem("verdant_shovel",
            settings -> new ShovelItem(ModMaterials.ENCHANTED_WOOD, 1.5f, -3.0f, settings) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });

    public static final Item WOODEN_SHEARS = registerItem("wooden_shears", settings -> new net.enchantedwood.item.custom.WoodenShearsItem(settings.maxDamage(30)));

    public static final Item ELDERWOOD_HOE = registerItem("elderwood_hoe",
            settings -> new net.enchantedwood.item.custom.AutoHarvestHoeItem(ModMaterials.ENCHANTED_WOOD, 0.0f, -1.0f, settings));

    // Armor Set
    public static final Item ENCHANTED_WOOD_HELMET = registerItem("enchanted_wood_helmet",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_WOOD, EquipmentType.HELMET)));

    public static final Item ENCHANTED_WOOD_CHESTPLATE = registerItem("enchanted_wood_chestplate",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_WOOD, EquipmentType.CHESTPLATE)));

    public static final Item ENCHANTED_WOOD_LEGGINGS = registerItem("enchanted_wood_leggings",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_WOOD, EquipmentType.LEGGINGS)));

    public static final Item ENCHANTED_WOOD_BOOTS = registerItem("enchanted_wood_boots",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_WOOD, EquipmentType.BOOTS)));

    // Enchanted Cobblestone Tools & Weapons
    public static final Item ENCHANTED_COBBLESTONE_SWORD = registerItem("enchanted_cobblestone_sword",
            settings -> new EnchantedCobblestoneSwordItem(settings.sword(ModMaterials.ENCHANTED_COBBLESTONE, 3.0f, -2.4f)));

    public static final Item ENCHANTED_COBBLESTONE_PICKAXE = registerItem("enchanted_cobblestone_pickaxe",
            settings -> new EnchantedCobblestonePickaxeItem(settings.pickaxe(ModMaterials.ENCHANTED_COBBLESTONE, 1.0f, -2.8f)));

    public static final Item ENCHANTED_COBBLESTONE_AXE = registerItem("enchanted_cobblestone_axe",
            settings -> new AxeItem(ModMaterials.ENCHANTED_COBBLESTONE, 6.0f, -3.1f, settings) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });

    public static final Item ENCHANTED_COBBLESTONE_SHOVEL = registerItem("enchanted_cobblestone_shovel",
            settings -> new ShovelItem(ModMaterials.ENCHANTED_COBBLESTONE, 1.5f, -3.0f, settings) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });

    public static final Item ENCHANTED_COBBLESTONE_HOE = registerItem("enchanted_cobblestone_hoe",
            settings -> new net.enchantedwood.item.custom.AutoHarvestHoeItem(ModMaterials.ENCHANTED_COBBLESTONE, -1.0f, -1.0f, settings));

    // Base 3x3 Mining Sledgehammers
    public static final Item WOODEN_HAMMER = registerItem("wooden_hammer",
            settings -> new HammerItem(settings.pickaxe(ToolMaterial.WOOD, 2.0f, -3.2f)));
    public static final Item STONE_HAMMER = registerItem("stone_hammer",
            settings -> new HammerItem(settings.pickaxe(ToolMaterial.STONE, 3.0f, -3.2f)));
    public static final Item COPPER_HAMMER = registerItem("copper_hammer",
            settings -> new HammerItem(settings.pickaxe(ModMaterials.COPPER, 3.5f, -3.1f)));
    public static final Item IRON_HAMMER = registerItem("iron_hammer",
            settings -> new HammerItem(settings.pickaxe(ToolMaterial.IRON, 4.0f, -3.0f)));
    public static final Item BRONZE_HAMMER = registerItem("bronze_hammer",
            settings -> new HammerItem(settings.pickaxe(ModMaterials.BRONZE, 4.5f, -3.0f)));
    public static final Item GOLDEN_HAMMER = registerItem("golden_hammer",
            settings -> new HammerItem(settings.pickaxe(ToolMaterial.GOLD, 2.0f, -2.8f)));
    public static final Item DIAMOND_HAMMER = registerItem("diamond_hammer",
            settings -> new HammerItem(settings.pickaxe(ToolMaterial.DIAMOND, 5.0f, -3.0f)));
    public static final Item NETHERITE_HAMMER = registerItem("netherite_hammer",
            settings -> new HammerItem(settings.pickaxe(ToolMaterial.NETHERITE, 6.0f, -2.8f).fireproof()));
    public static final Item TITANIUM_HAMMER = registerItem("titanium_hammer",
            settings -> new HammerItem(settings.pickaxe(ModMaterials.TITANIUM, 5.5f, -2.9f)));

    // Base Tree-Felling Broad Axes
    public static final Item WOODEN_BROAD_AXE = registerItem("wooden_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ToolMaterial.WOOD, 4.0f, -3.3f)));
    public static final Item STONE_BROAD_AXE = registerItem("stone_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ToolMaterial.STONE, 5.0f, -3.3f)));
    public static final Item COPPER_BROAD_AXE = registerItem("copper_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ModMaterials.COPPER, 5.5f, -3.2f)));
    public static final Item IRON_BROAD_AXE = registerItem("iron_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ToolMaterial.IRON, 6.0f, -3.1f)));
    public static final Item BRONZE_BROAD_AXE = registerItem("bronze_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ModMaterials.BRONZE, 6.5f, -3.1f)));
    public static final Item GOLDEN_BROAD_AXE = registerItem("golden_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ToolMaterial.GOLD, 4.0f, -2.9f)));
    public static final Item DIAMOND_BROAD_AXE = registerItem("diamond_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ToolMaterial.DIAMOND, 7.0f, -3.0f)));
    public static final Item NETHERITE_BROAD_AXE = registerItem("netherite_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ToolMaterial.NETHERITE, 8.0f, -2.9f).fireproof()));
    public static final Item TITANIUM_BROAD_AXE = registerItem("titanium_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ModMaterials.TITANIUM, 7.5f, -3.0f)));

    public static final Item ENCHANTED_COBBLESTONE_HAMMER = registerItem("enchanted_cobblestone_hammer",
            settings -> new HammerItem(settings.pickaxe(ModMaterials.ENCHANTED_COBBLESTONE, 5.0f, -3.2f)) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });
    public static final Item ENCHANTED_COBBLESTONE_BROAD_AXE = registerItem("enchanted_cobblestone_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ModMaterials.ENCHANTED_COBBLESTONE, 7.0f, -3.2f)) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });

    // Enchanted Cobblestone Armor Set
    public static final Item ENCHANTED_COBBLESTONE_HELMET = registerItem("enchanted_cobblestone_helmet",
            settings -> new EnchantedCobblestoneArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_COBBLESTONE, EquipmentType.HELMET)));

    public static final Item ENCHANTED_COBBLESTONE_CHESTPLATE = registerItem("enchanted_cobblestone_chestplate",
            settings -> new EnchantedCobblestoneArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_COBBLESTONE, EquipmentType.CHESTPLATE)));

    public static final Item ENCHANTED_COBBLESTONE_LEGGINGS = registerItem("enchanted_cobblestone_leggings",
            settings -> new EnchantedCobblestoneArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_COBBLESTONE, EquipmentType.LEGGINGS)));

    public static final Item ENCHANTED_COBBLESTONE_BOOTS = registerItem("enchanted_cobblestone_boots",
            settings -> new EnchantedCobblestoneArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_COBBLESTONE, EquipmentType.BOOTS)));

    // Enchanted Diamond Equipment
    public static final Item ENCHANTED_DIAMOND_HAMMER = registerItem("enchanted_diamond_hammer",
            settings -> new HammerItem(settings.pickaxe(ModMaterials.ENCHANTED_DIAMOND, 6.0f, -3.0f)) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });
    public static final Item ENCHANTED_DIAMOND_BROAD_AXE = registerItem("enchanted_diamond_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ModMaterials.ENCHANTED_DIAMOND, 8.0f, -3.0f)) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });

    public static final Item ENCHANTED_DIAMOND_HELMET = registerItem("enchanted_diamond_helmet",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_DIAMOND, EquipmentType.HELMET)));
    public static final Item ENCHANTED_DIAMOND_CHESTPLATE = registerItem("enchanted_diamond_chestplate",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_DIAMOND, EquipmentType.CHESTPLATE)));
    public static final Item ENCHANTED_DIAMOND_LEGGINGS = registerItem("enchanted_diamond_leggings",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_DIAMOND, EquipmentType.LEGGINGS)));
    public static final Item ENCHANTED_DIAMOND_BOOTS = registerItem("enchanted_diamond_boots",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_DIAMOND, EquipmentType.BOOTS)));

    // Enchanted Netherite Equipment (Fireproof!)
    public static final Item ENCHANTED_NETHERITE_HAMMER = registerItem("enchanted_netherite_hammer",
            settings -> new HammerItem(settings.pickaxe(ModMaterials.ENCHANTED_NETHERITE, 8.0f, -2.8f).fireproof()) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });
    public static final Item ENCHANTED_NETHERITE_BROAD_AXE = registerItem("enchanted_netherite_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ModMaterials.ENCHANTED_NETHERITE, 10.0f, -2.8f).fireproof()) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });

    public static final Item ENCHANTED_NETHERITE_HELMET = registerItem("enchanted_netherite_helmet",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_NETHERITE, EquipmentType.HELMET).fireproof()));
    public static final Item ENCHANTED_NETHERITE_CHESTPLATE = registerItem("enchanted_netherite_chestplate",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_NETHERITE, EquipmentType.CHESTPLATE).fireproof()));
    public static final Item ENCHANTED_NETHERITE_LEGGINGS = registerItem("enchanted_netherite_leggings",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_NETHERITE, EquipmentType.LEGGINGS).fireproof()));
    public static final Item ENCHANTED_NETHERITE_BOOTS = registerItem("enchanted_netherite_boots",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_NETHERITE, EquipmentType.BOOTS).fireproof()));

    // Enchanted Chest Items
    public static final Item COPPER_ENCHANTED_CHEST = registerItem("copper_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.COPPER, settings));
    public static final Item BRONZE_ENCHANTED_CHEST = registerItem("bronze_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.BRONZE, settings));
    public static final Item ENCHANTED_IRON_ENCHANTED_CHEST = registerItem("enchanted_iron_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.ENCHANTED_IRON, settings));
    public static final Item GOLD_ENCHANTED_CHEST = registerItem("gold_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.GOLD, settings));
    public static final Item DIAMOND_ENCHANTED_CHEST = registerItem("diamond_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.DIAMOND, settings));
    public static final Item NETHERITE_ENCHANTED_CHEST = registerItem("netherite_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.NETHERITE, settings.fireproof()));

    // Scuba & Underwater Diving Equipment (v1.2.0)
    public static final Item SNORKEL = registerItem("snorkel",
            settings -> new net.enchantedwood.item.custom.SnorkelItem(EquipmentType.HELMET.getEquipmentSlot(), settings.armor(ModArmorMaterials.SCUBA, EquipmentType.HELMET)));
    public static final Item DIVING_MASK = registerItem("diving_mask",
            settings -> new net.enchantedwood.item.custom.ScubaArmorItem(EquipmentType.HELMET.getEquipmentSlot(), settings.armor(ModArmorMaterials.SCUBA, EquipmentType.HELMET)));
    public static final Item SCUBA_CHESTPLATE = registerItem("scuba_chestplate",
            settings -> new net.enchantedwood.item.custom.ScubaArmorItem(EquipmentType.CHESTPLATE.getEquipmentSlot(), settings.armor(ModArmorMaterials.SCUBA, EquipmentType.CHESTPLATE)));
    public static final Item WETSUIT_LEGGINGS = registerItem("wetsuit_leggings",
            settings -> new net.enchantedwood.item.custom.ScubaArmorItem(EquipmentType.LEGGINGS.getEquipmentSlot(), settings.armor(ModArmorMaterials.SCUBA, EquipmentType.LEGGINGS)));
    public static final Item DIVING_FLIPPERS = registerItem("diving_flippers",
            settings -> new net.enchantedwood.item.custom.ScubaArmorItem(EquipmentType.BOOTS.getEquipmentSlot(), settings.armor(ModArmorMaterials.SCUBA, EquipmentType.BOOTS)));
    public static final Item MYSTERY_KEYSTONE = registerItem("mystery_keystone", net.enchantedwood.item.custom.MysteryKeystoneItem::new);

    // Portable Handheld Battery Packs (v1.3.0)
    public static final Item COPPER_BATTERY_PACK = registerItem("copper_battery_pack",
            settings -> new net.enchantedwood.item.custom.BatteryItem(settings, 10_000, 200, 200));
    public static final Item ALUMINUM_BATTERY_PACK = registerItem("aluminum_battery_pack",
            settings -> new net.enchantedwood.item.custom.BatteryItem(settings, 50_000, 500, 500));
    public static final Item STEEL_BATTERY_PACK = registerItem("steel_battery_pack",
            settings -> new net.enchantedwood.item.custom.BatteryItem(settings, 250_000, 2_000, 2_000));
    public static final Item TUNGSTEN_BATTERY_PACK = registerItem("tungsten_battery_pack",
            settings -> new net.enchantedwood.item.custom.BatteryItem(settings, 1_250_000, 10_000, 10_000));

    // Industrial Logistics Tools
    public static final Item WRENCH = registerItem("wrench",
            settings -> new net.enchantedwood.item.custom.WrenchItem(settings));

    // Nether Metallurgy & High-Temp Technology Suite (v1.4.0 Finalization)
    public static final Item BASALT_FLUX_CATALYST = registerItem("basalt_flux_catalyst",
            settings -> new net.enchantedwood.item.custom.TooltipItem(settings,
                    Text.literal("§6✦ Basalt Pyrometallurgical Flux"),
                    Text.literal("§7Doubles smelting yields and grants +50% speed in Blast Furnaces and Foundries.")));

    public static final Item THERMAL_REFRACTORY_PLATING = registerItem("thermal_refractory_plating",
            settings -> new net.enchantedwood.item.custom.ThermalRefractoryPlatingItem(settings.maxCount(1).fireproof()));

    public static final Item BLAZE_OVERCLOCK_CORE = registerItem("blaze_overclock_core",
            settings -> new GearItem(GearTier.BLAZE_OVERCLOCK, true, settings.maxCount(16).fireproof()));

    public static final Item INFERNAL_HAMMER = registerItem("infernal_hammer",
            settings -> new net.enchantedwood.item.custom.InfernalHammerItem(settings.pickaxe(ModMaterials.MANYULLYN, 7.0f, -2.9f).fireproof()));

    public static final Item PLASMA_FLAMETHROWER = registerItem("plasma_flamethrower",
            settings -> new net.enchantedwood.item.custom.PlasmaFlamethrowerItem(settings.maxDamage(850).fireproof()));

    // Quarry Upgrade Cores
    public static final Item RANGE_UPGRADE_T1 = registerItem("range_upgrade_t1",
            settings -> new net.enchantedwood.item.custom.TooltipItem(settings.maxCount(16),
                    Text.literal("§b✦ Tier 1 Quarry Range Core"),
                    Text.literal("§7Expands Laser Quarry scanning perimeter to §e3x3 Chunks §7(48x48 blocks).")));

    public static final Item RANGE_UPGRADE_T2 = registerItem("range_upgrade_t2",
            settings -> new net.enchantedwood.item.custom.TooltipItem(settings.maxCount(16),
                    Text.literal("§d✦ Tier 2 Quarry Range Core"),
                    Text.literal("§7Expands Laser Quarry scanning perimeter to §e5x5 Chunks §7(80x80 blocks).")));

    public static final Item FORTUNE_CORE = registerItem("fortune_core",
            settings -> new net.enchantedwood.item.custom.TooltipItem(settings.maxCount(16),
                    Text.literal("§6✦ Quarry Fortune Core"),
                    Text.literal("§7Applies §eFortune III §7ore multiplication to extracted ores.")));

    public static final Item SILK_TOUCH_CORE = registerItem("silk_touch_core",
            settings -> new net.enchantedwood.item.custom.TooltipItem(settings.maxCount(16),
                    Text.literal("§a✦ Quarry Silk Touch Core"),
                    Text.literal("§7Applies §eSilk Touch §7to harvest raw ore blocks intact.")));

    // Creative Tab
    public static final RegistryKey<ItemGroup> ENCHANTED_WOOD_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_wood_group"));
    public static final ItemGroup ENCHANTED_WOOD_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(LIVINGWOOD_SWORD))
            .displayName(Text.translatable("itemGroup.enchantedwood.enchanted_wood_group"))
            .entries((displayContext, entries) -> {
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_CHEST);
                entries.add(COPPER_ENCHANTED_CHEST);
                entries.add(BRONZE_ENCHANTED_CHEST);
                entries.add(ENCHANTED_IRON_ENCHANTED_CHEST);
                entries.add(GOLD_ENCHANTED_CHEST);
                entries.add(DIAMOND_ENCHANTED_CHEST);
                entries.add(NETHERITE_ENCHANTED_CHEST);
                entries.add(INFUSED_HEARTWOOD);


                entries.add(ENCHANTED_DUST);
                entries.add(ENCHANTED_WOOD);
                entries.add(ENCHANTED_COAL);
                entries.add(ENCHANTED_REDSTONE);
                entries.add(ENCHANTED_EMERALD);
                entries.add(RAW_TIN);
                entries.add(net.enchantedwood.block.ModBlocks.TIN_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.DEEPSLATE_TIN_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.RAW_TIN_BLOCK);
                entries.add(net.enchantedwood.block.ModBlocks.TIN_BLOCK);
                entries.add(TIN_INGOT);
                entries.add(TIN_NUGGET);
                entries.add(BRONZE_INGOT);
                entries.add(BRONZE_NUGGET);
                entries.add(net.enchantedwood.block.ModBlocks.BRONZE_BLOCK);
                entries.add(IRON_GEAR);
                entries.add(ENCHANTED_IRON_GEAR);
                entries.add(COPPER_GEAR);
                entries.add(ENCHANTED_COPPER_GEAR);
                entries.add(BRONZE_GEAR);
                entries.add(ENCHANTED_BRONZE_GEAR);
                entries.add(GOLD_GEAR);
                entries.add(ENCHANTED_GOLD_GEAR);
                entries.add(TITANIUM_GEAR);
                entries.add(ENCHANTED_TITANIUM_GEAR);
                entries.add(DIAMOND_GEAR);
                entries.add(ENCHANTED_DIAMOND_GEAR);
                entries.add(NETHERITE_GEAR);
                entries.add(ENCHANTED_NETHERITE_GEAR);
                entries.add(IRON_DUST);
                entries.add(COPPER_DUST);
                entries.add(TIN_DUST);
                entries.add(BRONZE_DUST);
                entries.add(GOLD_DUST);
                entries.add(DIAMOND_DUST);
                entries.add(NETHERITE_DUST);
                entries.add(RAW_TITANIUM);
                entries.add(TITANIUM_INGOT);
                entries.add(TITANIUM_NUGGET);
                entries.add(TITANIUM_DUST);
                entries.add(TITANIUM_ROLLER);
                entries.add(net.enchantedwood.block.ModBlocks.TITANIUM_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.DEEPSLATE_TITANIUM_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.RAW_TITANIUM_BLOCK);
                entries.add(net.enchantedwood.block.ModBlocks.TITANIUM_BLOCK);
                entries.add(EMERALD_DUST);
                entries.add(COAL_DUST);
                entries.add(QUARTZ_DUST);
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_COBBLESTONE);
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_FURNACE);
                entries.add(net.enchantedwood.block.ModBlocks.CRUSHER);
                entries.add(net.enchantedwood.block.ModBlocks.DUST_SMELTER);
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_COAL_BLOCK);
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_LAVA_GENERATOR);
                entries.add(net.enchantedwood.block.ModBlocks.COPPER_GENERATOR);
                entries.add(net.enchantedwood.block.ModBlocks.COPPER_BATTERY);
                entries.add(COPPER_BATTERY_PACK);
                entries.add(net.enchantedwood.block.ModBlocks.COPPER_CABLE);

                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_STORAGE_CONTROLLER);
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_DRIVE_BAY);
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_STORAGE_TERMINAL);
                entries.add(net.enchantedwood.block.ModBlocks.DIGITAL_CONVERTER);
                entries.add(net.enchantedwood.block.ModBlocks.SUPER_COMPUTER);

                // Microelectronics & Computer Chips (ShuDynamics 2.0)
                entries.add(SILICON);
                entries.add(SILICON_WAFER);
                entries.add(BASIC_COMPUTER_CHIP);
                entries.add(ADVANCED_COMPUTER_CHIP);
                entries.add(QUANTUM_COMPUTER_CHIP);
                entries.add(net.enchantedwood.block.ModBlocks.POWERED_ANVIL);

                entries.add(net.enchantedwood.block.ModBlocks.LASER_QUARRY);
                entries.add(RANGE_UPGRADE_T1);
                entries.add(RANGE_UPGRADE_T2);
                entries.add(FORTUNE_CORE);
                entries.add(SILK_TOUCH_CORE);
                entries.add(STORAGE_CRYSTAL_1K);
                entries.add(STORAGE_CRYSTAL_4K);
                entries.add(STORAGE_CRYSTAL_16K);
                entries.add(STORAGE_CRYSTAL_64K);
                entries.add(WIRELESS_STORAGE_CRYSTAL);
                entries.add(BRONZE_SWORD);
                entries.add(BRONZE_PICKAXE);
                entries.add(BRONZE_AXE);
                entries.add(BRONZE_SHOVEL);
                entries.add(BRONZE_HOE);
                entries.add(BRONZE_HELMET);
                entries.add(BRONZE_CHESTPLATE);
                entries.add(BRONZE_LEGGINGS);
                entries.add(BRONZE_BOOTS);

                entries.add(TIN_SWORD);
                entries.add(TIN_PICKAXE);
                entries.add(TIN_AXE);
                entries.add(TIN_SHOVEL);
                entries.add(TIN_HOE);
                entries.add(TIN_HELMET);
                entries.add(TIN_CHESTPLATE);
                entries.add(TIN_LEGGINGS);
                entries.add(TIN_BOOTS);

                entries.add(TITANIUM_SWORD);
                entries.add(TITANIUM_PICKAXE);
                entries.add(TITANIUM_AXE);
                entries.add(TITANIUM_SHOVEL);
                entries.add(TITANIUM_HOE);
                entries.add(TITANIUM_HELMET);
                entries.add(TITANIUM_CHESTPLATE);
                entries.add(TITANIUM_LEGGINGS);
                entries.add(TITANIUM_BOOTS);

                entries.add(LIVINGWOOD_SWORD);
                entries.add(BARKSKIN_PICKAXE);
                entries.add(IRONWOOD_AXE);
                entries.add(VERDANT_SHOVEL);
                entries.add(ELDERWOOD_HOE);
                entries.add(WOODEN_SHEARS);
                entries.add(ENCHANTED_WOOD_HELMET);
                entries.add(ENCHANTED_WOOD_CHESTPLATE);
                entries.add(ENCHANTED_WOOD_LEGGINGS);
                entries.add(ENCHANTED_WOOD_BOOTS);
                entries.add(ENCHANTED_DIAMOND);
                entries.add(ENCHANTED_NETHERITE_INGOT);
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_NETHERITE_BLOCK);
                entries.add(ENCHANTED_COBBLESTONE_SWORD);
                entries.add(ENCHANTED_COBBLESTONE_PICKAXE);
                entries.add(ENCHANTED_COBBLESTONE_AXE);
                entries.add(ENCHANTED_COBBLESTONE_SHOVEL);
                entries.add(ENCHANTED_COBBLESTONE_HOE);
                entries.add(WOODEN_HAMMER);
                entries.add(STONE_HAMMER);
                entries.add(COPPER_HAMMER);
                entries.add(IRON_HAMMER);
                entries.add(BRONZE_HAMMER);
                entries.add(GOLDEN_HAMMER);
                entries.add(DIAMOND_HAMMER);
                entries.add(NETHERITE_HAMMER);
                entries.add(TITANIUM_HAMMER);
                entries.add(WOODEN_BROAD_AXE);
                entries.add(STONE_BROAD_AXE);
                entries.add(COPPER_BROAD_AXE);
                entries.add(IRON_BROAD_AXE);
                entries.add(BRONZE_BROAD_AXE);
                entries.add(GOLDEN_BROAD_AXE);
                entries.add(DIAMOND_BROAD_AXE);
                entries.add(NETHERITE_BROAD_AXE);
                entries.add(TITANIUM_BROAD_AXE);
                entries.add(ENCHANTED_COBBLESTONE_HAMMER);
                entries.add(ENCHANTED_COBBLESTONE_BROAD_AXE);
                entries.add(ENCHANTED_COBBLESTONE_HELMET);
                entries.add(ENCHANTED_COBBLESTONE_CHESTPLATE);
                entries.add(ENCHANTED_COBBLESTONE_LEGGINGS);
                entries.add(ENCHANTED_COBBLESTONE_BOOTS);
                entries.add(ENCHANTED_DIAMOND_HAMMER);
                entries.add(ENCHANTED_DIAMOND_BROAD_AXE);
                entries.add(ENCHANTED_DIAMOND_HELMET);
                entries.add(ENCHANTED_DIAMOND_CHESTPLATE);
                entries.add(ENCHANTED_DIAMOND_LEGGINGS);
                entries.add(ENCHANTED_DIAMOND_BOOTS);
                entries.add(ENCHANTED_NETHERITE_HAMMER);
                entries.add(ENCHANTED_NETHERITE_BROAD_AXE);
                entries.add(ENCHANTED_NETHERITE_HELMET);
                entries.add(ENCHANTED_NETHERITE_CHESTPLATE);
                entries.add(ENCHANTED_NETHERITE_LEGGINGS);
                entries.add(ENCHANTED_NETHERITE_BOOTS);
                entries.add(ENCHANTED_CAPE);
                entries.add(ENCHANTED_HEART);
                entries.add(IRON_ENCHANTED_HEART);
                entries.add(GOLD_ENCHANTED_HEART);
                entries.add(DIAMOND_ENCHANTED_HEART);
                entries.add(NETHERITE_ENCHANTED_HEART);
                entries.add(COPPER_BUCKET);
                entries.add(COPPER_WATER_BUCKET);
                entries.add(COPPER_LAVA_BUCKET);
                entries.add(ENCHANTED_LAVA_BUCKET);
                entries.add(ENCHANTED_COPPER_LAVA_BUCKET);

                // Energy & Phase 2 Metallurgy
                entries.add(net.enchantedwood.block.ModBlocks.GAS_PIPE);
                entries.add(net.enchantedwood.block.ModBlocks.HYDROGEN_PIPE);
                entries.add(net.enchantedwood.block.ModBlocks.OXYGEN_GENERATOR);
                entries.add(net.enchantedwood.block.ModBlocks.ALUMINUM_REFINER);
                entries.add(net.enchantedwood.block.ModBlocks.ALUMINUM_GENERATOR);
                entries.add(net.enchantedwood.block.ModBlocks.ALUMINUM_BATTERY);
                entries.add(ALUMINUM_BATTERY_PACK);
                entries.add(net.enchantedwood.block.ModBlocks.ALUMINUM_CABLE);

                entries.add(net.enchantedwood.block.ModBlocks.BAUXITE_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.DEEPSLATE_BAUXITE_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.RAW_BAUXITE_BLOCK);
                entries.add(net.enchantedwood.block.ModBlocks.ALUMINUM_BLOCK);

                entries.add(RAW_BAUXITE);
                entries.add(BAUXITE_DUST);
                entries.add(ALUMINUM_INGOT);
                entries.add(ALUMINUM_NUGGET);
                entries.add(ALUMINUM_GEAR);
                entries.add(EMPTY_GAS_CANISTER);
                entries.add(OXYGEN_CANISTER);
                entries.add(HYDROGEN_CANISTER);
                entries.add(HYDROGEN_JETPACK);
                entries.add(OXY_HYDROGEN_TORCH);

                entries.add(ENCHANTED_ALUMINUM_GEAR);
                entries.add(ALUMINUM_SWORD);
                entries.add(ALUMINUM_PICKAXE);
                entries.add(ALUMINUM_AXE);
                entries.add(ALUMINUM_SHOVEL);
                entries.add(ALUMINUM_HOE);
                entries.add(ALUMINUM_HAMMER);
                entries.add(ALUMINUM_BROAD_AXE);
                entries.add(ALUMINUM_HELMET);
                entries.add(ALUMINUM_CHESTPLATE);
                entries.add(ALUMINUM_LEGGINGS);
                entries.add(ALUMINUM_BOOTS);

                // Phase 3: Coke Coal & Steel Grid
                entries.add(net.enchantedwood.block.ModBlocks.COKE_OVEN);
                entries.add(net.enchantedwood.block.ModBlocks.STEEL_BLAST_FURNACE);
                entries.add(net.enchantedwood.block.ModBlocks.STEEL_GENERATOR);
                entries.add(net.enchantedwood.block.ModBlocks.STEEL_BATTERY);
                entries.add(STEEL_BATTERY_PACK);
                entries.add(net.enchantedwood.block.ModBlocks.STEEL_CABLE);
                entries.add(net.enchantedwood.block.ModBlocks.STEEL_BLOCK);

                entries.add(COKE_COAL);
                entries.add(net.enchantedwood.block.ModBlocks.COKE_COAL_BLOCK);
                entries.add(STEEL_INGOT);
                entries.add(STEEL_NUGGET);
                entries.add(STEEL_DUST);
                entries.add(STEEL_GEAR);
                entries.add(ENCHANTED_STEEL_GEAR);

                entries.add(STEEL_SWORD);
                entries.add(STEEL_PICKAXE);
                entries.add(STEEL_AXE);
                entries.add(STEEL_SHOVEL);
                entries.add(STEEL_HOE);
                entries.add(STEEL_HAMMER);
                entries.add(STEEL_BROAD_AXE);
                entries.add(STEEL_HELMET);
                entries.add(STEEL_CHESTPLATE);
                entries.add(STEEL_LEGGINGS);
                entries.add(STEEL_BOOTS);

                // Rubber Tree & Polymers
                entries.add(net.enchantedwood.block.ModBlocks.RUBBER_LOG);
                entries.add(net.enchantedwood.block.ModBlocks.RUBBER_WOOD);
                entries.add(net.enchantedwood.block.ModBlocks.STRIPPED_RUBBER_LOG);
                entries.add(net.enchantedwood.block.ModBlocks.STRIPPED_RUBBER_WOOD);
                entries.add(net.enchantedwood.block.ModBlocks.RUBBER_PLANKS);
                entries.add(net.enchantedwood.block.ModBlocks.RUBBER_LEAVES);
                entries.add(net.enchantedwood.block.ModBlocks.RUBBER_SAPLING);
                entries.add(RESIN);
                entries.add(RUBBER);

                // Storage Network Upgrades
                entries.add(CHUNK_LOADER_MODULE);
                entries.add(INTERDIMENSIONAL_CARD);

                // Scuba & Underwater Diving Equipment (v1.2.0)
                entries.add(SNORKEL);
                entries.add(DIVING_MASK);
                entries.add(SCUBA_CHESTPLATE);
                entries.add(WETSUIT_LEGGINGS);
                entries.add(DIVING_FLIPPERS);

                // Anomaly Keystones
                entries.add(net.enchantedwood.block.ModBlocks.ATMOSPHERIC_ANCHOR);
                entries.add(net.enchantedwood.block.ModBlocks.KINETIC_ANCHOR);
                entries.add(net.enchantedwood.block.ModBlocks.THERMAL_ANCHOR);
                entries.add(net.enchantedwood.block.ModBlocks.METALLURGICAL_ANCHOR);
                entries.add(net.enchantedwood.block.ModBlocks.PLASMA_ANCHOR);
                entries.add(net.enchantedwood.block.ModBlocks.DIMENSIONAL_SINGULARITY);
                entries.add(MYSTERY_KEYSTONE);

                // Nether Metallurgy & Materials
                entries.add(RAW_TUNGSTEN);
                entries.add(TUNGSTEN_INGOT);
                entries.add(TUNGSTEN_NUGGET);
                entries.add(TUNGSTEN_DUST);
                entries.add(TUNGSTEN_PLATE);
                entries.add(TUNGSTEN_CARBIDE_INGOT);
                entries.add(TUNGSTEN_SWORD);
                entries.add(TUNGSTEN_PICKAXE);
                entries.add(TUNGSTEN_AXE);
                entries.add(TUNGSTEN_SHOVEL);
                entries.add(TUNGSTEN_HOE);
                entries.add(TUNGSTEN_HAMMER);
                entries.add(TUNGSTEN_BROAD_AXE);
                entries.add(TUNGSTEN_HELMET);
                entries.add(TUNGSTEN_CHESTPLATE);
                entries.add(TUNGSTEN_LEGGINGS);
                entries.add(TUNGSTEN_BOOTS);
                entries.add(net.enchantedwood.block.ModBlocks.NETHER_TUNGSTEN_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.DEEPSLATE_TUNGSTEN_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.RAW_TUNGSTEN_BLOCK);
                entries.add(net.enchantedwood.block.ModBlocks.TUNGSTEN_BLOCK);

                entries.add(RAW_COBALT);
                entries.add(COBALT_INGOT);
                entries.add(COBALT_NUGGET);
                entries.add(COBALT_DUST);
                entries.add(COBALT_PLATE);
                entries.add(COBALT_SWORD);
                entries.add(COBALT_PICKAXE);
                entries.add(COBALT_AXE);
                entries.add(COBALT_SHOVEL);
                entries.add(COBALT_HOE);
                entries.add(COBALT_HAMMER);
                entries.add(COBALT_BROAD_AXE);
                entries.add(COBALT_HELMET);
                entries.add(COBALT_CHESTPLATE);
                entries.add(COBALT_LEGGINGS);
                entries.add(COBALT_BOOTS);
                entries.add(net.enchantedwood.block.ModBlocks.COBALT_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.RAW_COBALT_BLOCK);
                entries.add(net.enchantedwood.block.ModBlocks.COBALT_BLOCK);

                entries.add(RAW_ARDITE);
                entries.add(ARDITE_INGOT);
                entries.add(ARDITE_NUGGET);
                entries.add(ARDITE_DUST);
                entries.add(ARDITE_PLATE);
                entries.add(ARDITE_SWORD);
                entries.add(ARDITE_PICKAXE);
                entries.add(ARDITE_AXE);
                entries.add(ARDITE_SHOVEL);
                entries.add(ARDITE_HOE);
                entries.add(ARDITE_HAMMER);
                entries.add(ARDITE_BROAD_AXE);
                entries.add(ARDITE_HELMET);
                entries.add(ARDITE_CHESTPLATE);
                entries.add(ARDITE_LEGGINGS);
                entries.add(ARDITE_BOOTS);
                entries.add(net.enchantedwood.block.ModBlocks.ARDITE_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.RAW_ARDITE_BLOCK);
                entries.add(net.enchantedwood.block.ModBlocks.ARDITE_BLOCK);

                entries.add(MANYULLYN_INGOT);
                entries.add(MANYULLYN_NUGGET);
                entries.add(MANYULLYN_DUST);
                entries.add(MANYULLYN_PLATE);
                entries.add(MANYULLYN_SWORD);
                entries.add(MANYULLYN_PICKAXE);
                entries.add(MANYULLYN_AXE);
                entries.add(MANYULLYN_SHOVEL);
                entries.add(MANYULLYN_HOE);
                entries.add(MANYULLYN_HAMMER);
                entries.add(MANYULLYN_BROAD_AXE);
                entries.add(MANYULLYN_HELMET);
                entries.add(MANYULLYN_CHESTPLATE);
                entries.add(MANYULLYN_LEGGINGS);
                entries.add(MANYULLYN_BOOTS);
                entries.add(net.enchantedwood.block.ModBlocks.MANYULLYN_BLOCK);

                entries.add(SULFUR_DUST);
                entries.add(VOLCANIC_ASH);
                entries.add(VOLCANIC_FERTILIZER);
                entries.add(FIRE_CRYSTAL);

                // Nether Factory & Tier 4 Power Infrastructure
                entries.add(net.enchantedwood.block.ModBlocks.TUNGSTEN_BATTERY);
                entries.add(TUNGSTEN_BATTERY_PACK);
                entries.add(net.enchantedwood.block.ModBlocks.TUNGSTEN_CABLE);
                entries.add(net.enchantedwood.block.ModBlocks.GEOTHERMAL_GENERATOR);
                entries.add(net.enchantedwood.block.ModBlocks.ALLOY_FOUNDRY);
                entries.add(net.enchantedwood.block.ModBlocks.HYDRAULIC_PRESS);
                entries.add(net.enchantedwood.block.ModBlocks.ITEM_SALVAGER);
                entries.add(net.enchantedwood.block.ModBlocks.MAGMA_CRUCIBLE);
                entries.add(net.enchantedwood.block.ModBlocks.LAVA_PUMP);
                entries.add(net.enchantedwood.block.ModBlocks.CRUSHER_MK2);
                entries.add(net.enchantedwood.block.ModBlocks.DUST_SMELTER_MK2);
                entries.add(net.enchantedwood.block.ModBlocks.SOIL_INFUSER);

                // 5x5 Multiblock Titanium Lava Reservoir & Titanium Lava Pipes
                entries.add(net.enchantedwood.block.ModBlocks.TITANIUM_LAVA_PIPE);
                entries.add(net.enchantedwood.block.ModBlocks.TITANIUM_TANK_CASING);
                entries.add(net.enchantedwood.block.ModBlocks.REINFORCED_TANK_GLASS);
                entries.add(net.enchantedwood.block.ModBlocks.TITANIUM_TANK_INBOUND_PORT);

                // Universal Item Logistics System
                entries.add(net.enchantedwood.block.ModBlocks.ITEM_PIPE);
                entries.add(net.enchantedwood.block.ModBlocks.ITEM_EXTRACTOR);
                entries.add(net.enchantedwood.block.ModBlocks.ITEM_INSERTER);
                entries.add(WRENCH);

                // Volcanic Agronomy & Pozzolanic Architecture
                entries.add(net.enchantedwood.block.ModBlocks.VOLCANIC_SOIL);
                entries.add(net.enchantedwood.block.ModBlocks.POZZOLANIC_ASPHALT);
                entries.add(net.enchantedwood.block.ModBlocks.VOLCANIC_BRICKS);
                entries.add(net.enchantedwood.block.ModBlocks.VOLCANIC_BRICK_STAIRS);
                entries.add(net.enchantedwood.block.ModBlocks.VOLCANIC_BRICK_SLAB);

                // Agriculture & Biofuels
                entries.add(CORN);
                entries.add(ROASTED_CORN);
                entries.add(CORN_SEEDS);

                // Convergence Cuisine & Ingredients
                entries.add(RICE);
                entries.add(RICE_SEEDS);
                entries.add(SUSHI_RICE);
                entries.add(CUCUMBER);
                entries.add(CUCUMBER_SEEDS);
                entries.add(AVOCADO);
                entries.add(net.enchantedwood.block.ModBlocks.AVOCADO_SAPLING);
                entries.add(net.enchantedwood.block.ModBlocks.AVOCADO_LOG);
                entries.add(net.enchantedwood.block.ModBlocks.AVOCADO_WOOD);
                entries.add(net.enchantedwood.block.ModBlocks.AVOCADO_LEAVES);
                entries.add(net.enchantedwood.block.ModBlocks.WILD_RICE);
                entries.add(net.enchantedwood.block.ModBlocks.WILD_CUCUMBER);
                entries.add(NORI_SHEET);

                // Gourmet Sushi Rolls
                entries.add(SALMON_ROLL);
                entries.add(COD_ROLL);
                entries.add(AVOCADO_CUCUMBER_ROLL);
                entries.add(CALIFORNIA_ROLL);
                entries.add(GARDEN_ROLL);
                entries.add(MASTER_RAINBOW_ROLL);

                // Protective Hazard-Shield Foods (Hypospray Alternatives)
                entries.add(ALKALINE_DETOX_ROLL);
                entries.add(VOLCANIC_DRAGON_ROLL);
                entries.add(HIGH_ALTITUDE_KELP_ROLL);
                entries.add(SURVIVALIST_BENTO);

                // Petrochemicals & Fuels
                entries.add(net.enchantedwood.block.ModBlocks.OIL_SAND);
                entries.add(CRUDE_OIL_SLUDGE);
                entries.add(MINERAL_TAR);
                entries.add(BIOFUEL_CANISTER);
                entries.add(GASOLINE_CANISTER);
                entries.add(HIGH_OCTANE_FUEL_CANISTER);
                entries.add(net.enchantedwood.block.ModBlocks.FUEL_REFINERY);
                entries.add(net.enchantedwood.block.ModBlocks.ROAD_PAVER);
                entries.add(net.enchantedwood.block.ModBlocks.ASPHALT_BLOCK);
                entries.add(net.enchantedwood.block.ModBlocks.ASPHALT_SLAB);
                entries.add(net.enchantedwood.block.ModBlocks.CONCRETE_CURB);
                entries.add(net.enchantedwood.block.ModBlocks.ROAD_TRANSITION_RAMP);
                entries.add(UNFIRED_CONCRETE_CURB);
                entries.add(UNFIRED_ROAD_TRANSITION_RAMP);

                // Modular All-Terrain Vehicles (ATV) & Upgrades
                entries.add(net.enchantedwood.block.ModBlocks.VEHICLE_FABRICATOR);
                entries.add(ATV_ITEM);
                entries.add(ATV_SEAT);
                entries.add(RUBBER_TIRE);
                entries.add(STEEL_RIM_TIRE);
                entries.add(TITANIUM_STUDDED_TIRE);
                entries.add(COPPER_ATV_ENGINE);
                entries.add(ALUMINUM_ATV_ENGINE);
                entries.add(STEEL_ATV_ENGINE);
                entries.add(TITANIUM_ATV_ENGINE);
                entries.add(ALUMINUM_SUSPENSION);
                entries.add(STEEL_SUSPENSION);
                entries.add(TITANIUM_SUSPENSION);
                entries.add(ALUMINUM_ATV_CHASSIS);
                entries.add(STEEL_ATV_CHASSIS);
                entries.add(TITANIUM_ATV_CHASSIS);
                entries.add(SEALED_HAZARD_CANOPY);
                entries.add(SMALL_CARGO_TRUNK);
                entries.add(MEDIUM_CARGO_TRUNK);
                entries.add(LARGE_CARGO_TRUNK);
                entries.add(IRON_DRILL_BIT);
                entries.add(STEEL_DRILL_BIT);
                entries.add(DIAMOND_DRILL_BIT);
                entries.add(TITANIUM_DRILL_BIT);
                entries.add(NETHERITE_DRILL_BIT);

                entries.add(IRON_TREE_SAW);
                entries.add(STEEL_TREE_SAW);
                entries.add(DIAMOND_TREE_SAW);
                entries.add(TITANIUM_TREE_SAW);
                entries.add(NETHERITE_TREE_SAW);

                entries.add(IRON_CROP_HARVESTER);
                entries.add(STEEL_CROP_HARVESTER);
                entries.add(DIAMOND_CROP_HARVESTER);
                entries.add(TITANIUM_CROP_HARVESTER);
                entries.add(NETHERITE_CROP_HARVESTER);

                entries.add(HALOGEN_HEADLIGHTS);
                entries.add(LED_FLOODLIGHTS);
                entries.add(XENON_HIGH_BEAMS);

                // Enchanted Lighting
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_LAMP);

                // Nether Metallurgy & Heavy Technology Suite (v1.4.0 Finalization)
                entries.add(net.enchantedwood.block.ModBlocks.BASALT_CABLE);
                entries.add(net.enchantedwood.block.ModBlocks.REINFORCED_OBSIDIAN);
                entries.add(net.enchantedwood.block.ModBlocks.VOLCANIC_GLASS);
                entries.add(BASALT_FLUX_CATALYST);
                entries.add(THERMAL_REFRACTORY_PLATING);
                entries.add(BLAZE_OVERCLOCK_CORE);
                entries.add(INFERNAL_HAMMER);
                entries.add(PLASMA_FLAMETHROWER);

                // Boss Combat Culinary Dishes & Convergence Botanicals
                entries.add(WASABI_ROOT);
                entries.add(DRAGON_FRUIT);
                entries.add(STARFRUIT);

                entries.add(net.enchantedwood.block.ModBlocks.WILD_WASABI);
                entries.add(net.enchantedwood.block.ModBlocks.WILD_DRAGON_FRUIT);
                entries.add(net.enchantedwood.block.ModBlocks.STARFRUIT_LEAVES);

                entries.add(PITAYA_BOWL);
                entries.add(WASABI_NIGIRI);
                entries.add(GOLDEN_HONEY_MOCHI);
                entries.add(STARFRUIT_TART);

                // Convergence Mob Variant Spawn Eggs
                entries.add(CONVERGENCE_ZOMBIE_SPAWN_EGG);
                entries.add(CONVERGENCE_SKELETON_SPAWN_EGG);
                entries.add(CONVERGENCE_CREEPER_SPAWN_EGG);
                entries.add(CONVERGENCE_SPIDER_SPAWN_EGG);

                // The Resonance Colossus: Altar, Awakening Core & Relics
                entries.add(net.enchantedwood.block.ModBlocks.RESONANCE_ALTAR);
                entries.add(CORE_OF_AWAKENING);
                entries.add(RESONANCE_CLEAVER);
                entries.add(SINGULARITY_STAFF);
                entries.add(ETERNAL_BENTO_BOX);

                // Advanced Medical Laboratory Suite
                entries.add(net.enchantedwood.block.ModBlocks.INDUSTRIAL_CENTRIFUGE);
                entries.add(net.enchantedwood.block.ModBlocks.CHEMICAL_SYNTHESIZER);
                entries.add(HYPOSPRAY);
                entries.add(EMPTY_CARTRIDGE);
                entries.add(ALKALINE_BASE_EXTRACT);
                entries.add(CRYO_THERMAL_EXTRACT);
                entries.add(OXYGENATED_EXTRACT);
                entries.add(CELLULAR_NANITE_EXTRACT);
                entries.add(ADRENAL_ESSENCE);
                entries.add(ACID_NEUTRALIZING_CARTRIDGE);
                entries.add(HEAT_BUFFER_CARTRIDGE);
                entries.add(HYPER_OXYGENATION_CARTRIDGE);
                entries.add(NANITE_TRAUMA_CARTRIDGE);
                entries.add(ADRENALINE_STIM_CARTRIDGE);

                // Modular Power Suit & Exosuit Modules
                entries.add(MODULAR_POWER_HELMET);
                entries.add(MODULAR_POWER_CHESTPLATE);
                entries.add(MODULAR_POWER_LEGGINGS);
                entries.add(MODULAR_POWER_BOOTS);
                entries.add(NANITE_REPAIR_MATRIX);
                entries.add(HYDROGEN_THRUSTER_MODULE);
                entries.add(ION_REPULSOR_MODULE);
                entries.add(NIGHT_VISION_MODULE);
                entries.add(SPEED_SERVO_MODULE);
                entries.add(STEP_ASSIST_MODULE);
                entries.add(HIGH_JUMP_MODULE);
                entries.add(ACID_PROOF_PLATING);

                // Convergence Cave Ores, Minerals & Superalloys
                entries.add(net.enchantedwood.block.ModBlocks.FLUORITE_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.DEEPSLATE_FLUORITE_ORE);
                entries.add(RAW_FLUORITE);
                entries.add(FLUORITE_CRYSTAL);

                entries.add(net.enchantedwood.block.ModBlocks.ZIRCONIA_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.DEEPSLATE_ZIRCONIA_ORE);
                entries.add(RAW_ZIRCONIA);
                entries.add(ZIRCONIA_NODULE);

                entries.add(net.enchantedwood.block.ModBlocks.TANTALUM_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.DEEPSLATE_TANTALUM_ORE);
                entries.add(RAW_TANTALUM);
                entries.add(TANTALUM_INGOT);
                entries.add(TANTALUM_DUST);

                entries.add(net.enchantedwood.block.ModBlocks.HAFNIUM_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.DEEPSLATE_HAFNIUM_ORE);
                entries.add(RAW_HAFNIUM);
                entries.add(HAFNIUM_INGOT);
                entries.add(HAFNIUM_DUST);

                entries.add(net.enchantedwood.block.ModBlocks.NEODYMIUM_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.DEEPSLATE_NEODYMIUM_ORE);
                entries.add(RAW_NEODYMIUM);
                entries.add(NEODYMIUM_MAGNET);
                entries.add(NEODYMIUM_DUST);

                entries.add(net.enchantedwood.block.ModBlocks.AEROGEL_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.DEEPSLATE_AEROGEL_ORE);
                entries.add(RAW_AEROGEL);
                entries.add(AEROGEL_SHARD);
                entries.add(net.enchantedwood.block.ModBlocks.AEROGEL_GLASS);

                entries.add(TAN_TI_INGOT);
                entries.add(HAFNIUM_TUNGSTEN_CARBIDE_INGOT);
                entries.add(NEO_TITANIUM_INGOT);
            })
            .build();

    private static <T extends Item> T registerItem(String name, Function<Item.Settings, T> itemFactory) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(EnchantedWoodMod.MOD_ID, name));
        T item = itemFactory.apply(new Item.Settings().registryKey(key));
        return Registry.register(Registries.ITEM, key, item);
    }

    public static void registerModItems() {
        EnchantedWoodMod.LOGGER.info("Registering Enchanted Wood Items for " + EnchantedWoodMod.MOD_ID);
        Registry.register(Registries.ITEM_GROUP, ENCHANTED_WOOD_GROUP_KEY, ENCHANTED_WOOD_GROUP);
    }
}


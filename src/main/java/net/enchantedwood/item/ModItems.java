package net.enchantedwood.item;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorType;
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
import net.enchantedwood.sound.ModSounds;
import java.util.function.Function;

public class ModItems {

    // Materials
    public static final Item INFUSED_HEARTWOOD = registerItem("infused_heartwood", Item::new);
    public static final Item ENCHANTED_DUST = registerItem("enchanted_dust", Item::new);
    public static final Item ENCHANTED_WOOD = registerItem("enchanted_wood", Item::new);
    public static final Item ENCHANTED_COAL = registerItem("enchanted_coal", Item::new);
    public static final Item ENCHANTED_REDSTONE = registerItem("enchanted_redstone", EnchantedRedstoneItem::new);
    public static final Item ENCHANTED_EMERALD = registerItem("enchanted_emerald", net.enchantedwood.item.custom.EnchantedEmeraldItem::new);
    public static final Item ENCHANTED_CAPE = registerItem("enchanted_cape", settings -> new net.enchantedwood.item.custom.EnchantedCapeItem(settings.stacksTo(1)));
    public static final Item RESIN = registerItem("resin", Item::new);
    public static final Item RUBBER = registerItem("rubber", Item::new);

    // Agriculture & Crops
    public static final Item CORN = registerItem("corn", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.6f).build()),
            Component.literal("§7Can be §eRoasted §7on fire for food, or distilled in a"),
            Component.literal("§eFuel Refinery §7(§62 Corn + Empty Canister§7) into §aBiofuel§7.")
    ));
    public static final Item ROASTED_CORN = registerItem("roasted_corn", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.8f).build()),
            Component.literal("§aDelicious roasted sweet corn. Restores 7 food points.")
    ));
    public static final Item CORN_SEEDS = registerItem("corn_seeds", settings -> new net.enchantedwood.item.custom.TooltipBlockItem(
            net.enchantedwood.block.ModBlocks.CORN_CROP,
            settings,
            Component.literal("§7Plant on tilled farmland to grow 8-stage Sweet Corn."),
            Component.literal("§8Obtained by breaking wild grass or crafting with Enchanted Dust.")
    ));

    // Convergence Cuisine & Ingredients
    public static final Item RICE_SEEDS = registerItem("rice_seeds", settings -> new net.enchantedwood.item.custom.TooltipBlockItem(
            net.enchantedwood.block.ModBlocks.RICE_CROP,
            settings,
            Component.literal("§7Plant on tilled farmland or Volcanic Soil to cultivate Rice."),
            Component.literal("§8Native crop of §dThe Convergence§8. Essential ingredient for Sushi.")
    ));
    public static final Item RICE = registerItem("rice", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Grown grain from The Convergence."),
            Component.literal("§8Cook in a Smoker/Furnace or combine with water to prepare §fSushi Rice§8.")
    ));
    public static final Item SUSHI_RICE = registerItem("sushi_rice", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4f).build()),
            Component.literal("§fSeasoned Steamed Sushi Rice."),
            Component.literal("§8The essential base for rolling authentic Sushi.")
    ));
    public static final Item CUCUMBER_SEEDS = registerItem("cucumber_seeds", settings -> new net.enchantedwood.item.custom.TooltipBlockItem(
            net.enchantedwood.block.ModBlocks.CUCUMBER_CROP,
            settings,
            Component.literal("§7Plant on tilled farmland or Volcanic Soil to grow Crisp Cucumbers."),
            Component.literal("§8Native crop of §dThe Convergence§8.")
    ));
    public static final Item CUCUMBER = registerItem("cucumber", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.5f).build()),
            Component.literal("§aFresh, crisp green cucumber."),
            Component.literal("§8Restores 3 food points. Key ingredient in vegetarian and California rolls.")
    ));
    public static final Item AVOCADO = registerItem("avocado", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.7f).build()),
            Component.literal("§2Creamy, nutrient-rich avocado fruit."),
            Component.literal("§8Harvested from Avocado Trees in §dThe Convergence§8.")
    ));
    public static final Item NORI_SHEET = registerItem("nori_sheet", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.3f).build()),
            Component.literal("§8Thin roasted seaweed sheet pressed from Dried Kelp."),
            Component.literal("§8Used to wrap sushi rolls.")
    ));

    // Sushi Rolls
    public static final Item SALMON_ROLL = registerItem("salmon_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(7).saturationModifier(0.8f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 20, 0)))
                            .build()
            ),
            Component.literal("§6Fresh Pacific Salmon wrapped in Nori and seasoned Sushi Rice."),
            Component.literal("§bGrants Dolphin's Grace I (20s). Restores 7 hunger points.")
    ));

    public static final Item COD_ROLL = registerItem("cod_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(6).saturationModifier(0.7f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HASTE, 20 * 30, 0)))
                            .build()
            ),
            Component.literal("§eTender Cod Roll wrapped in crispy Nori."),
            Component.literal("§eGrants Haste I (30s). Restores 6 hunger points.")
    ));

    public static final Item AVOCADO_CUCUMBER_ROLL = registerItem("avocado_cucumber_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(6).saturationModifier(0.8f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 35, 0)))
                            .build()
            ),
            Component.literal("§aRefreshing Vegetarian Maki with creamy avocado & cucumber."),
            Component.literal("§aGrants Speed I (35s). Restores 6 hunger points.")
    ));

    public static final Item CALIFORNIA_ROLL = registerItem("california_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(9).saturationModifier(0.9f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 0)))
                            .build()
            ),
            Component.literal("§dClassic California Roll with fish, avocado, and cucumber."),
            Component.literal("§dGrants Regeneration I (8s). Restores 9 hunger points.")
    ));

    public static final Item GARDEN_ROLL = registerItem("garden_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(8).saturationModifier(0.8f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 45, 0)))
                            .build()
            ),
            Component.literal("§6Garden Delight Roll made with sweet carrots, cucumber & avocado."),
            Component.literal("§9Grants Night Vision (45s). Restores 8 hunger points.")
    ));

    public static final Item MASTER_RAINBOW_ROLL = registerItem("master_rainbow_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(12).saturationModifier(1.0f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(java.util.List.of(
                                    new MobEffectInstance(MobEffects.REGENERATION, 20 * 12, 1),
                                    new MobEffectInstance(MobEffects.SPEED, 20 * 45, 1),
                                    new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 45, 0)
                            )))
                            .build()
            ),
            Component.literal("§5✦ Master Rainbow Sushi Platter ✦"),
            Component.literal("§7The ultimate culinary synthesis: Salmon, Cod, Avocado, Cucumber, and Carrots!"),
            Component.literal("§dGrants Regeneration II, Speed II, and Dolphin's Grace.")
    ));

    // Protective Survival Foods (Organic Hypospray Alternatives)
    public static final Item ALKALINE_DETOX_ROLL = registerItem("alkaline_detox_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(8).saturationModifier(0.9f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(java.util.List.of(
                                    new MobEffectInstance(net.enchantedwood.effect.ModStatusEffects.ACID_PROTECTION, 20 * 300, 0),
                                    new MobEffectInstance(MobEffects.ABSORPTION, 20 * 120, 1)
                            )))
                            .build()
            ),
            Component.literal("§a✦ Alkaline Acid-Shield Roll ✦"),
            Component.literal("§7Infused with crisp cucumber & creamy alkalizing avocado."),
            Component.literal("§e✦ Buff: §aAcid Protection §f(5:00)"),
            Component.literal("§8 • 100% Immunity to Poison, Wither & Acid damage"),
            Component.literal("§8 • Continuous negative status effect cleansing"),
            Component.literal("§e✦ Buff: §6Absorption II §f(2:00) §8(+4 Golden Hearts)"),
            Component.literal("§b✦ Organic alternative to chemical Hyposprays.")
    ));

    public static final Item VOLCANIC_DRAGON_ROLL = registerItem("volcanic_dragon_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(8).saturationModifier(0.9f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(java.util.List.of(
                                    new MobEffectInstance(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION, 20 * 300, 0),
                                    new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 300, 0)
                            )))
                            .build()
            ),
            Component.literal("§6✦ Volcanic Dragon Roll ✦"),
            Component.literal("§7Spicy magma-infused thermal sushi roll."),
            Component.literal("§e✦ Buff: §6Thermal Protection §f(5:00)"),
            Component.literal("§8 • 100% Immunity to Fire, Lava, Magma & Freezing"),
            Component.literal("§8 • Grants Molten Lava Buoyancy & Auto-Extinguish"),
            Component.literal("§e✦ Buff: §cFire Resistance §f(5:00)"),
            Component.literal("§b✦ Organic alternative to chemical Hyposprays.")
    ));

    public static final Item HIGH_ALTITUDE_KELP_ROLL = registerItem("high_altitude_kelp_roll", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(8).saturationModifier(0.9f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(java.util.List.of(
                                    new MobEffectInstance(net.enchantedwood.effect.ModStatusEffects.ATMOSPHERIC_PROTECTION, 20 * 300, 0),
                                    new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 300, 0)
                            )))
                            .build()
            ),
            Component.literal("§b✦ High-Altitude Kelp Roll ✦"),
            Component.literal("§7Deep-sea kelp pressed with oxygen-dense mountain vegetables."),
            Component.literal("§e✦ Buff: §bAtmospheric Protection §f(5:00)"),
            Component.literal("§8 • Infinite Oxygen: Immunity to Drowning & Suffocation"),
            Component.literal("§8 • Shields against high-altitude vacuum collapse"),
            Component.literal("§e✦ Buff: §9Water Breathing §f(5:00)"),
            Component.literal("§b✦ Organic alternative to chemical Hyposprays.")
    ));

    public static final Item SURVIVALIST_BENTO = registerItem("survivalist_bento", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(16).food(
                    new FoodProperties.Builder().nutrition(14).saturationModifier(1.0f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(java.util.List.of(
                                    new MobEffectInstance(net.enchantedwood.effect.ModStatusEffects.ACID_PROTECTION, 20 * 480, 0),
                                    new MobEffectInstance(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION, 20 * 480, 0),
                                    new MobEffectInstance(net.enchantedwood.effect.ModStatusEffects.ATMOSPHERIC_PROTECTION, 20 * 480, 0),
                                    new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 480, 0),
                                    new MobEffectInstance(MobEffects.REGENERATION, 20 * 30, 1),
                                    new MobEffectInstance(MobEffects.ABSORPTION, 20 * 180, 2)
                            )))
                            .build()
            ),
            Component.literal("§5✦ Master Survivalist Bento Box ✦"),
            Component.literal("§dThe pinnacle of environmental hazard culinary defense."),
            Component.literal("§e✦ Active Tri-Shield Protection §f(8:00):"),
            Component.literal("§a  ✔ Acid Protection §8(Poison, Wither & Corrosive Immunity)"),
            Component.literal("§6  ✔ Thermal Protection §8(Fire, Lava & Freeze Immunity)"),
            Component.literal("§b  ✔ Atmospheric Protection §8(Infinite Air & Vacuum Immunity)"),
            Component.literal("§e✦ Buffs: §dRegeneration II §f(0:30) §7+ §eAbsorption III §f(3:00)"),
            Component.literal("§f✦ The ultimate organic alternative to all chemical Hyposprays!")
    ));

    // Convergence Exotic Ingredients & Boss Combat Culinary Dishes
    public static final Item WASABI_ROOT = registerItem("wasabi_root", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HASTE, 20 * 10, 0)))
                            .build()
            ),
            Component.literal("§aConvergence Wasabi Root"),
            Component.literal("§7Pungent wild rhizome harvested along convergence riverbanks."),
            Component.literal("§8Essential spice for crafting Fresh Wasabi Nigiri.")
    ));

    public static final Item DRAGON_FRUIT = registerItem("dragon_fruit", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 5, 0)))
                            .build()
            ),
            Component.literal("§dExotic Dragon Fruit"),
            Component.literal("§7Vibrant magenta fruit rich in cellular life-essence."),
            Component.literal("§8Essential ingredient in Dragon Fruit Pitaya Bowls.")
    ));

    public static final Item STARFRUIT = registerItem("starfruit", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20 * 10, 0)))
                            .build()
            ),
            Component.literal("§eResonance Starfruit"),
            Component.literal("§7Luminescent star-shaped fruit charged with anti-gravitational energy."),
            Component.literal("§8Essential ingredient in Resonance Starfruit Tarts.")
    ));

    public static final Item PITAYA_BOWL = registerItem("pitaya_bowl", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(16).food(
                    new FoodProperties.Builder().nutrition(10).saturationModifier(1.0f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(java.util.List.of(
                                    new MobEffectInstance(net.enchantedwood.effect.ModStatusEffects.VAMPIRIC_VITALITY, 20 * 300, 0),
                                    new MobEffectInstance(MobEffects.STRENGTH, 20 * 300, 0),
                                    new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 1)
                            )))
                            .build()
            ),
            Component.literal("§d✦ Dragon Fruit Pitaya Bowl ✦"),
            Component.literal("§7Thick, nutrient-dense smoothie bowl crowned with dragon fruit & avocado."),
            Component.literal("§e✦ Buff: §cVampiric Vitality §f(5:00)"),
            Component.literal("§8 • 15% Melee Lifesteal: heals attacker for 15% of physical damage dealt"),
            Component.literal("§8 • Critical organic sustain during drawn-out boss encounters"),
            Component.literal("§e✦ Buffs: §cStrength I §f(5:00) §7+ §dRegeneration II §f(0:20)"),
            Component.literal("§b✦ Organic combat meal for warrior builds.")
    ));

    public static final Item WASABI_NIGIRI = registerItem("wasabi_nigiri", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(8).saturationModifier(0.9f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(java.util.List.of(
                                    new MobEffectInstance(net.enchantedwood.effect.ModStatusEffects.ADRENALINE_RUSH, 20 * 300, 0),
                                    new MobEffectInstance(MobEffects.HASTE, 20 * 300, 1)
                            )))
                            .build()
            ),
            Component.literal("§a✦ Fresh Wasabi Nigiri ✦"),
            Component.literal("§7Premium sushi rice topped with freshly grated convergence wasabi root."),
            Component.literal("§e✦ Buff: §aAdrenaline Rush §f(5:00)"),
            Component.literal("§8 • +25% Attack Speed & +20% Movement Speed"),
            Component.literal("§8 • Cleanses & immunizes against Slowness, Mining Fatigue & Weakness"),
            Component.literal("§e✦ Buff: §eHaste II §f(5:00)"),
            Component.literal("§b✦ High-velocity rush for aggressive combat.")
    ));

    public static final Item GOLDEN_HONEY_MOCHI = registerItem("golden_honey_mochi", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(8).saturationModifier(1.0f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(java.util.List.of(
                                    new MobEffectInstance(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING, 20 * 300, 0),
                                    new MobEffectInstance(MobEffects.RESISTANCE, 20 * 300, 0),
                                    new MobEffectInstance(MobEffects.ABSORPTION, 20 * 180, 1)
                            )))
                            .build()
            ),
            Component.literal("§6✦ Golden Honey Mochi ✦"),
            Component.literal("§7Chewy pounded rice mochi infused with sweet honey & golden dust."),
            Component.literal("§e✦ Buff: §6Kinetic Dampening §f(5:00)"),
            Component.literal("§8 • 100% Knockback Resistance & Impact Shock Absorption"),
            Component.literal("§8 • Eliminates boss slam recoil and zero kinetic crash damage"),
            Component.literal("§e✦ Buffs: §9Resistance I §f(5:00) §7+ §eAbsorption II §f(3:00)"),
            Component.literal("§b✦ Indispensable frontline defense against boss slams.")
    ));

    public static final Item STARFRUIT_TART = registerItem("starfruit_tart", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(8).saturationModifier(0.9f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(java.util.List.of(
                                    new MobEffectInstance(net.enchantedwood.effect.ModStatusEffects.CELESTIAL_LEAP, 20 * 300, 0),
                                    new MobEffectInstance(MobEffects.SPEED, 20 * 300, 0)
                            )))
                            .build()
            ),
            Component.literal("§e✦ Resonance Starfruit Tart ✦"),
            Component.literal("§7Baked celestial tart glazed with crystalline starfruit essence."),
            Component.literal("§e✦ Buff: §eCelestial Leap §f(5:00)"),
            Component.literal("§8 • 3-Block High Jump & Featherweight Slow-Fall Gliding"),
            Component.literal("§8 • Complete immunity to all fall damage"),
            Component.literal("§8 • Perfect for leaping over boss ground slams & shockwaves"),
            Component.literal("§e✦ Buff: §bSpeed I §f(5:00)"),
            Component.literal("§b✦ Aerial tactical mobility in boss arenas.")
    ));

    // ==========================================
    // Culinary Expansion: Seeds & Produce
    // ==========================================
    public static final Item TOMATO_SEEDS = registerItem("tomato_seeds", settings -> new net.enchantedwood.item.custom.TooltipBlockItem(
            net.enchantedwood.block.ModBlocks.TOMATO_CROP,
            settings,
            Component.literal("§7Plant on tilled farmland to cultivate ripe juicy tomatoes."),
            Component.literal("§8Essential for rich tomato sauces and pizza bases.")
    ));
    public static final Item TOMATO = registerItem("tomato", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4f).build()),
            Component.literal("§cPlump, juicy garden tomato."),
            Component.literal("§8Can be crushed into savory tomato sauce or sliced for burgers and salads.")
    ));

    public static final Item ONION_SEEDS = registerItem("onion_seeds", settings -> new net.enchantedwood.item.custom.TooltipBlockItem(
            net.enchantedwood.block.ModBlocks.ONION_CROP,
            settings,
            Component.literal("§7Plant on tilled farmland to cultivate pungent onions."),
            Component.literal("§8Aromatic seasoning for pizzas, burgers, tacos, and salads.")
    ));
    public static final Item ONION = registerItem("onion", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()),
            Component.literal("§eCrisp, pungent garden onion."),
            Component.literal("§8Adds savory depth to artisan dishes and street food.")
    ));

    public static final Item LETTUCE_SEEDS = registerItem("lettuce_seeds", settings -> new net.enchantedwood.item.custom.TooltipBlockItem(
            net.enchantedwood.block.ModBlocks.LETTUCE_CROP,
            settings,
            Component.literal("§7Plant on tilled farmland to cultivate fresh leafy lettuce."),
            Component.literal("§8Crisp leafy greens for burgers, tacos, and garden salads.")
    ));
    public static final Item LETTUCE = registerItem("lettuce", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4f).build()),
            Component.literal("§aCrisp leafy green lettuce."),
            Component.literal("§8Vital ingredient for burgers, tacos, and refreshing salads.")
    ));

    public static final Item SOYBEAN_SEEDS = registerItem("soybean_seeds", settings -> new net.enchantedwood.item.custom.TooltipBlockItem(
            net.enchantedwood.block.ModBlocks.SOYBEAN_CROP,
            settings,
            Component.literal("§7Plant on tilled farmland to cultivate versatile soybeans."),
            Component.literal("§8High-protein legume processed into soy milk, artisan cheese, and tofu.")
    ));
    public static final Item SOYBEANS = registerItem("soybeans", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()),
            Component.literal("§eNutritious golden soybeans."),
            Component.literal("§8Can be pressed into soy milk, curdled into cheese, or made into tofu.")
    ));

    public static final Item CHILI_PEPPER_SEEDS = registerItem("chili_pepper_seeds", settings -> new net.enchantedwood.item.custom.TooltipBlockItem(
            net.enchantedwood.block.ModBlocks.CHILI_PEPPER_CROP,
            settings,
            Component.literal("§7Plant on tilled farmland to cultivate fiery chili peppers."),
            Component.literal("§8Adds spicy heat to supreme pizzas and street tacos.")
    ));
    public static final Item CHILI_PEPPER = registerItem("chili_pepper", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(2).saturationModifier(0.4f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 15, 0)))
                            .build()
            ),
            Component.literal("§cFiery red chili pepper with a spicy kick."),
            Component.literal("§eGrants brief Speed I (15s). Key ingredient for Supreme Pizza & Tacos.")
    ));

    public static final Item STRAWBERRY = registerItem("strawberry", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.5f).build()),
            Component.literal("§cSweet, fragrant summer strawberry."),
            Component.literal("§8Harvested from strawberry bushes. Churned into gourmet ice cream.")
    ));

    public static final Item BLUEBERRY = registerItem("blueberry", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4f).build()),
            Component.literal("§9Rich antioxidant wild blueberry."),
            Component.literal("§8Harvested from blueberry bushes. Churned into vibrant ice cream.")
    ));

    // ==========================================
    // Culinary Expansion: Processed Ingredients
    // ==========================================
    public static final Item SALT = registerItem("salt", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§fRefined mineral salt crystals."),
            Component.literal("§8Boiled from sea water buckets or centrifuged from Calcite & Dripstone."),
            Component.literal("§7Essential seasoning for doughs, patties, cheese, and ice cream coolant.")
    ));

    public static final Item WHEAT_FLOUR = registerItem("wheat_flour", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§fFinely milled whole wheat flour."),
            Component.literal("§8Ground in a Crusher or mortar. The foundation for breads and doughs.")
    ));

    public static final Item PIZZA_DOUGH = registerItem("pizza_dough", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§eHand-tossed artisan pizza dough."),
            Component.literal("§8Kneaded with flour, salt, and water. Base for brick oven pizzas.")
    ));

    public static final Item BURGER_BUN = registerItem("burger_bun", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.5f).build()),
            Component.literal("§6Golden toasted brioche sesame bun."),
            Component.literal("§8Baked in the Brick Oven or furnace.")
    ));

    public static final Item TACO_SHELL = registerItem("taco_shell", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4f).build()),
            Component.literal("§eCrisp stone-ground corn tortilla shell."),
            Component.literal("§8Prepared from corn kernels and baked crisp.")
    ));

    public static final Item TOMATO_SAUCE = registerItem("tomato_sauce", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4f).build()),
            Component.literal("§cRich slow-simmered marinara sauce."),
            Component.literal("§8Cooked from ripe tomatoes, salt, and aromatic herbs.")
    ));

    public static final Item SOY_MILK = registerItem("soy_milk", settings -> new net.enchantedwood.item.custom.BottleFoodItem(
            settings.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4f).build()),
            Component.literal("§fSilky smooth plant-based soy milk in a glass bottle."),
            Component.literal("§aClears all active status effects when consumed like cow's milk."),
            Component.literal("§8Used to churn ice cream and ferment artisan cheese.")
    ));

    public static final Item CHEESE_SLICE = registerItem("cheese_slice", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).build()),
            Component.literal("§eRich, creamy aged artisan cheese slice."),
            Component.literal("§8Meltable topping for pizzas and juicy burgers.")
    ));

    public static final Item TOFU = registerItem("tofu", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).build()),
            Component.literal("§fTender pressed soybean curd."),
            Component.literal("§8Nutritious plant protein suitable for savory vegan delicacies.")
    ));

    public static final Item RAW_BURGER_PATTY = registerItem("raw_burger_patty", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§cSeasoned minced ground beef patty."),
            Component.literal("§8Sear in the Brick Oven or furnace to unlock juicy savory goodness.")
    ));

    public static final Item COOKED_BURGER_PATTY = registerItem("cooked_burger_patty", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8f).build()),
            Component.literal("§6Flame-broiled savory beef burger patty."),
            Component.literal("§8Ready to be assembled into hearty cheeseburgers.")
    ));

    public static final Item PREPARED_ANCHOVIES = registerItem("prepared_anchovies", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.5f).build()),
            Component.literal("§7Salt-cured savory ocean anchovy fillets."),
            Component.literal("§8Intense umami topping for traditional artisanal pizza.")
    ));

    // ==========================================
    // Culinary Expansion: Artisan Brick Oven Pizzas
    // ==========================================
    public static final Item RAW_MARGHERITA_PIZZA = registerItem("raw_margherita_pizza", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§eUnbaked Margherita Pizza with tomato sauce and mozzarella."),
            Component.literal("§7Bake in the §6Brick Oven §7for an authentic crispy crust.")
    ));
    public static final Item MARGHERITA_PIZZA = registerItem("margherita_pizza", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(10).saturationModifier(0.8f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SATURATION, 20 * 5, 0)))
                            .build()
            ),
            Component.literal("§6✦ Brick Oven Margherita Pizza ✦"),
            Component.literal("§7Classic Neapolitan pizza with bubbling cheese and sweet basil tomato sauce."),
            Component.literal("§aRestores 10 hunger points with lasting Saturation.")
    ));

    public static final Item RAW_MEAT_LOVERS_PIZZA = registerItem("raw_meat_lovers_pizza", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§cUnbaked Meat Lovers Pizza piled high with steak, pork, and patty."),
            Component.literal("§7Bake in the §6Brick Oven §7to render the savory meats.")
    ));
    public static final Item MEAT_LOVERS_PIZZA = registerItem("meat_lovers_pizza", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(14).saturationModifier(0.9f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 30, 0)))
                            .build()
            ),
            Component.literal("§c✦ Carnivore's Brick Oven Meat Lovers Pizza ✦"),
            Component.literal("§7Loaded with steak, roasted pork, and seasoned beef over bubbling cheese."),
            Component.literal("§cGrants Strength I (30s). Restores 14 hunger points.")
    ));

    public static final Item RAW_ANCHOVY_ONION_PIZZA = registerItem("raw_anchovy_onion_pizza", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Unbaked Coastal Pizza with cured anchovies and sweet onions."),
            Component.literal("§7Bake in the §6Brick Oven §7for seaside umami excellence.")
    ));
    public static final Item ANCHOVY_ONION_PIZZA = registerItem("anchovy_onion_pizza", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(11).saturationModifier(0.8f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 45, 0)))
                            .build()
            ),
            Component.literal("§3✦ Mediterranean Anchovy & Onion Pizza ✦"),
            Component.literal("§7Deep ocean umami balanced by caramelized sweet onions and tangy tomato sauce."),
            Component.literal("§bGrants Water Breathing (45s). Restores 11 hunger points.")
    ));

    public static final Item RAW_SUPREME_PIZZA = registerItem("raw_supreme_pizza", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§6Unbaked Supreme Pizza loaded with all toppings."),
            Component.literal("§7Bake in the §6Brick Oven §7for the ultimate master feast.")
    ));
    public static final Item SUPREME_PIZZA = registerItem("supreme_pizza", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(16).saturationModifier(1.0f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 15, 0)))
                            .build()
            ),
            Component.literal("§6✦ Grand Master Supreme Pizza ✦"),
            Component.literal("§7The pinnacle of brick oven baking: cheese, steak, onions, and spicy chilies."),
            Component.literal("§dGrants Regeneration I (15s) and full Saturation. Restores 16 hunger points.")
    ));

    // ==========================================
    // Culinary Expansion: Gourmet Street Foods
    // ==========================================
    public static final Item CLASSIC_CHEESEBURGER = registerItem("classic_cheeseburger", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(10).saturationModifier(0.8f).build()),
            Component.literal("§6Classic All-American Cheeseburger"),
            Component.literal("§7Flame-broiled beef patty, melted cheese, crisp lettuce, tomato, and onion in a brioche bun."),
            Component.literal("§aHearty meal restoring 10 hunger points.")
    ));

    public static final Item DELUXE_BACON_BURGER = registerItem("deluxe_bacon_burger", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(13).saturationModifier(0.9f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 20, 0)))
                            .build()
            ),
            Component.literal("§6✦ Deluxe Smoked Bacon Cheeseburger ✦"),
            Component.literal("§7Stacked double patty with crispy smoked bacon, double cheese, and smoky sauce."),
            Component.literal("§9Grants Resistance I (20s). Restores 13 hunger points.")
    ));

    public static final Item BEEF_TACO = registerItem("beef_taco", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.7f).build()),
            Component.literal("§eCrispy Street Beef Taco"),
            Component.literal("§7Seasoned ground beef, shredded cheese, lettuce, and diced onion in a crunchy corn shell."),
            Component.literal("§aRestores 8 hunger points.")
    ));

    public static final Item FISH_TACO = registerItem("fish_taco", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(8).saturationModifier(0.7f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 20, 0)))
                            .build()
            ),
            Component.literal("§bBaja Fresh Fish Taco"),
            Component.literal("§7Tender grilled fish fillet with cilantro lime dressing, lettuce, and chili peppers."),
            Component.literal("§bGrants Dolphin's Grace (20s). Restores 8 hunger points.")
    ));

    // ==========================================
    // Culinary Expansion: Fresh Farm Salads
    // ==========================================
    public static final Item GARDEN_SALAD = registerItem("garden_salad", settings -> new net.enchantedwood.item.custom.BowlFoodItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(6).saturationModifier(0.6f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 30, 0)))
                            .build()
            ),
            Component.literal("§aFresh Farmer's Garden Salad"),
            Component.literal("§7Tossed crisp lettuce, sweet tomatoes, sliced onions, and cucumbers in an artisan wooden bowl."),
            Component.literal("§aGrants Speed I (30s). Returns empty bowl.")
    ));

    public static final Item BERRY_MEDLEY_SALAD = registerItem("berry_medley_salad", settings -> new net.enchantedwood.item.custom.BowlFoodItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(7).saturationModifier(0.7f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 0)))
                            .build()
            ),
            Component.literal("§d✦ Wild Berry Medley Salad ✦"),
            Component.literal("§7Tossed strawberries, blueberries, sweet berries, and greens glazed with wildflower honey."),
            Component.literal("§dGrants Regeneration I (10s). Returns empty bowl.")
    ));

    // ==========================================
    // Culinary Expansion: Churned Gourmet Ice Creams
    // ==========================================
    public static final Item VANILLA_ICE_CREAM = registerItem("vanilla_ice_cream", settings -> new net.enchantedwood.item.custom.BowlFoodItem(
            settings.food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.5f).build()),
            Component.literal("§fCreamy Classic Vanilla Ice Cream"),
            Component.literal("§7Slow-churned with milk, ice, and sweet sugar in an ice cream bowl."),
            Component.literal("§eClears harmful ailments and refreshes the spirit.")
    ));

    public static final Item STRAWBERRY_ICE_CREAM = registerItem("strawberry_ice_cream", settings -> new net.enchantedwood.item.custom.BowlFoodItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(7).saturationModifier(0.6f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 45, 0)))
                            .build()
            ),
            Component.literal("§cSweet Strawberry Swirl Ice Cream"),
            Component.literal("§7Churned with ripe strawberries and rich cream."),
            Component.literal("§cGrants Speed I (45s).")
    ));

    public static final Item BLUEBERRY_ICE_CREAM = registerItem("blueberry_ice_cream", settings -> new net.enchantedwood.item.custom.BowlFoodItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(7).saturationModifier(0.6f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 45, 0)))
                            .build()
            ),
            Component.literal("§9Antioxidant Blueberry Ice Cream"),
            Component.literal("§7Velvety blueberry custard churned to frozen perfection."),
            Component.literal("§9Grants Night Vision (45s).")
    ));

    public static final Item CHOCOLATE_ICE_CREAM = registerItem("chocolate_ice_cream", settings -> new net.enchantedwood.item.custom.BowlFoodItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(8).saturationModifier(0.6f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HASTE, 20 * 45, 0)))
                            .build()
            ),
            Component.literal("§6Decadent Dutch Chocolate Ice Cream"),
            Component.literal("§7Rich cocoa beans churned with sweet milk into dark chocolate decadence."),
            Component.literal("§eGrants Haste I (45s).")
    ));

    public static final Item SWEET_BERRY_ICE_CREAM = registerItem("sweet_berry_ice_cream", settings -> new net.enchantedwood.item.custom.BowlFoodItem(
            settings.food(
                    new FoodProperties.Builder().nutrition(7).saturationModifier(0.6f).build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20 * 45, 0)))
                            .build()
            ),
            Component.literal("§cWild Sweet Berry Gelato"),
            Component.literal("§7Tart and sweet wild berries churned with whole milk."),
            Component.literal("§aGrants Jump Boost I (45s).")
    ));

    public static final Item ICE_CUBES = registerItem("ice_cubes",
            settings -> new net.enchantedwood.item.custom.TooltipItem(settings,
                    Component.literal("§bPure Ice Cubes"),
                    Component.literal("§7Cryogenically frozen crystalline ice cubes. Perfect for rapid chilling and refreshing drinks.")));

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
            settings.stacksTo(16),
            Component.literal("§5✦ Core of Awakening ✦"),
            Component.literal("§dCrystalline relic charged with primordial resonance energy."),
            Component.literal("§7Right-click on the §eResonance Altar §7in §dThe Convergence §7to awaken"),
            Component.literal("§7or revive §5The Resonance Colossus§7."),
            Component.literal("§8 • Protected by Arena Leash & Retreat Protocol")
    ));

    public static final Item RESONANCE_CLEAVER = registerItem("resonance_cleaver",
            settings -> new net.enchantedwood.item.custom.ResonanceCleaverItem(settings.sword(ModMaterials.MANYULLYN, 6.0f, -3.0f).durability(2500)));

    public static final Item SINGULARITY_STAFF = registerItem("singularity_staff",
            settings -> new net.enchantedwood.item.custom.SingularityStaffItem(settings.durability(1500)));

    public static final Item ETERNAL_BENTO_BOX = registerItem("eternal_bento_box",
            settings -> new net.enchantedwood.item.custom.EternalBentoBoxItem(settings.stacksTo(1)));

    // Blank Vinyl Disc & Music Discs
    public static final Item BLANK_VINYL_DISC = registerItem("blank_vinyl_disc", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(64),
            Component.literal("§7Unrecorded vinyl disc pressed from rubber & resin."),
            Component.literal("§8Used in the §eHarmonic Record Press §8to cut custom music discs.")
    ));

    public static final Item MUSIC_DISC_CONVERGENCE = registerItem("music_disc_convergence",
            settings -> new Item(settings.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModSounds.CONVERGENCE_SONG)));
    public static final Item MUSIC_DISC_COLOSSUS = registerItem("music_disc_colossus",
            settings -> new Item(settings.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModSounds.COLOSSUS_SONG)));
    public static final Item MUSIC_DISC_OVERDRIVE = registerItem("music_disc_overdrive",
            settings -> new Item(settings.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModSounds.OVERDRIVE_SONG)));
    public static final Item MUSIC_DISC_CLEANROOM = registerItem("music_disc_cleanroom",
            settings -> new Item(settings.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModSounds.CLEANROOM_SONG)));
    public static final Item MUSIC_DISC_AUTOCRAFT = registerItem("music_disc_autocraft",
            settings -> new Item(settings.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModSounds.AUTOCRAFT_SONG)));
    public static final Item MUSIC_DISC_HAVEN_BLOOM = registerItem("music_disc_haven_bloom",
            settings -> new Item(settings.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModSounds.HAVEN_BLOOM_SONG)));
    public static final Item MUSIC_DISC_CRUCIBLE = registerItem("music_disc_crucible",
            settings -> new Item(settings.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModSounds.CRUCIBLE_SONG)));
    public static final Item MUSIC_DISC_STRATOSPHERE = registerItem("music_disc_stratosphere",
            settings -> new Item(settings.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModSounds.STRATOSPHERE_SONG)));
    public static final Item MUSIC_DISC_ABYSSAL = registerItem("music_disc_abyssal",
            settings -> new Item(settings.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModSounds.ABYSSAL_SONG)));
    public static final Item MUSIC_DISC_ANOXIC = registerItem("music_disc_anoxic",
            settings -> new Item(settings.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModSounds.ANOXIC_SONG)));

    // Tier 2 & Tier 3 Boss Summoning Cores, Catalysts & Ascendant Relics
    public static final Item CORRUPTED_CORE_OF_CATACLYSM = registerItem("corrupted_core_of_cataclysm", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(16),
            Component.literal("§4✦ Corrupted Core of Cataclysm ✦"),
            Component.literal("§cVolatile celestial core pulsating with cataclysmic instability."),
            Component.literal("§7Right-click on the §eResonance Altar §7in §dThe Convergence §7to awaken"),
            Component.literal("§4The Ascendant Colossus §7(Tier 2 Boss)."),
            Component.literal("§8 • Demands active protection buffs and tactical precision")
    ));

    public static final Item PRIMORDIAL_RIFT_KEYSTONE = registerItem("primordial_rift_keystone", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(16),
            Component.literal("§d✦ Primordial Rift Keystone ✦"),
            Component.literal("§5Mythic singularity key vibrating with universe-rending energy."),
            Component.literal("§7Right-click on the §eResonance Altar §7in §dThe Convergence §7to summon"),
            Component.literal("§dThe Primordial Cataclysm §7(Tier 3 Mythic Final Boss)."),
            Component.literal("§8 • The ultimate test of solo conquest in ShuDynamics")
    ));

    public static final Item PRIMORDIAL_CATALYST = registerItem("primordial_catalyst", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(64),
            Component.literal("§6Primordial Catalyst"),
            Component.literal("§7Concentrated cosmic catalyst dropped by The Ascendant Colossus."),
            Component.literal("§8Used to upgrade Base Relics into their Ascendant God-Tier forms.")
    ));

    public static final Item SINGULARITY_HEART = registerItem("singularity_heart", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(16),
            Component.literal("§4Singularity Heart"),
            Component.literal("§7The pulsing gravitational core of The Ascendant Colossus."),
            Component.literal("§8Essential key for forging the Primordial Rift Keystone.")
    ));

    public static final Item ASCENDANT_CLEAVER = registerItem("ascendant_cleaver",
            settings -> new net.enchantedwood.item.custom.AscendantCleaverItem(settings.sword(ModMaterials.MANYULLYN, 10.0f, -2.8f).durability(5000)));

    public static final Item VOID_SINGULARITY_NEXUS = registerItem("void_singularity_nexus",
            settings -> new net.enchantedwood.item.custom.VoidSingularityNexusItem(settings.durability(3000)));

    public static final Item OMEGA_BENTO_BOX = registerItem("omega_bento_box",
            settings -> new net.enchantedwood.item.custom.OmegaBentoBoxItem(settings.stacksTo(1)));

    public static final Item RING_OF_GRAVITATIONAL_MASTERY = registerItem("ring_of_gravitational_mastery",
            settings -> new net.enchantedwood.item.custom.RingOfGravitationalMasteryItem(settings.stacksTo(1)));

    public static final Item INFINITE_DIMENSIONAL_MATRIX = registerItem("infinite_dimensional_matrix",
            net.enchantedwood.item.custom.InfiniteDimensionalMatrixItem::new);

    // Advanced Medical Technology: Hypospray, Essences & Cartridges
    public static final Item HYPOSPRAY = registerItem("hypospray",
            settings -> new net.enchantedwood.item.custom.HyposprayItem(settings.stacksTo(1)));

    public static final Item EMPTY_CARTRIDGE = registerItem("empty_cartridge", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(64),
            Component.literal("§9Empty Hypospray Cartridge"),
            Component.literal("§7Sterile quartz-reinforced glass ampoule with tin cap."),
            Component.literal("§8Crafted with §fGlass Pane + Tin Ingot + Quartz§8."),
            Component.literal("§8Used in the §eChemical Synthesizer §8to compound medical inoculants.")
    ));

    // Centrifuged Chemical Essences
    public static final Item ALKALINE_BASE_EXTRACT = registerItem("alkaline_base_extract", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(64),
            Component.literal("§aAlkaline Base Extract"),
            Component.literal("§7Concentrated alkalizing solution extracted in the §eIndustrial Centrifuge§7."),
            Component.literal("§8Synthesizes Acid-Neutralizing Cartridges.")
    ));

    public static final Item CRYO_THERMAL_EXTRACT = registerItem("cryo_thermal_extract", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(64),
            Component.literal("§6Cryo-Thermal Extract"),
            Component.literal("§7Endothermic compound separated from volcanic matter in the §eIndustrial Centrifuge§7."),
            Component.literal("§8Synthesizes Endothermic Heat-Buffer Cartridges.")
    ));

    public static final Item OXYGENATED_EXTRACT = registerItem("oxygenated_extract", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(64),
            Component.literal("§bOxygenated Extract"),
            Component.literal("§7Purified oxygen-binding hemoglobin essence from the §eIndustrial Centrifuge§7."),
            Component.literal("§8Synthesizes Hyper-Oxygenation Cartridges.")
    ));

    public static final Item CELLULAR_NANITE_EXTRACT = registerItem("cellular_nanite_extract", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(64),
            Component.literal("§dCellular Nanite Extract"),
            Component.literal("§7Bio-regenerative stem essence extracted from rare flora in the §eIndustrial Centrifuge§7."),
            Component.literal("§8Synthesizes Nanite Trauma Inoculants.")
    ));

    public static final Item ADRENAL_ESSENCE = registerItem("adrenal_essence", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(64),
            Component.literal("§eAdrenal Essence"),
            Component.literal("§7Hyper-metabolic stimulant concentrate from the §eIndustrial Centrifuge§7."),
            Component.literal("§8Synthesizes Adrenaline Combat Stims.")
    ));

    // Medical Hypospray Cartridges
    public static final Item ACID_NEUTRALIZING_CARTRIDGE = registerItem("acid_neutralizing_cartridge",
            settings -> new net.enchantedwood.item.custom.HyposprayCartridgeItem(settings.stacksTo(16), net.enchantedwood.item.custom.HyposprayCartridgeItem.Type.ACID_NEUTRALIZING));

    public static final Item HEAT_BUFFER_CARTRIDGE = registerItem("heat_buffer_cartridge",
            settings -> new net.enchantedwood.item.custom.HyposprayCartridgeItem(settings.stacksTo(16), net.enchantedwood.item.custom.HyposprayCartridgeItem.Type.HEAT_BUFFER));

    public static final Item HYPER_OXYGENATION_CARTRIDGE = registerItem("hyper_oxygenation_cartridge",
            settings -> new net.enchantedwood.item.custom.HyposprayCartridgeItem(settings.stacksTo(16), net.enchantedwood.item.custom.HyposprayCartridgeItem.Type.HYPER_OXYGENATION));

    public static final Item NANITE_TRAUMA_CARTRIDGE = registerItem("nanite_trauma_cartridge",
            settings -> new net.enchantedwood.item.custom.HyposprayCartridgeItem(settings.stacksTo(16), net.enchantedwood.item.custom.HyposprayCartridgeItem.Type.NANITE_TRAUMA));

    public static final Item ADRENALINE_STIM_CARTRIDGE = registerItem("adrenaline_stim_cartridge",
            settings -> new net.enchantedwood.item.custom.HyposprayCartridgeItem(settings.stacksTo(16), net.enchantedwood.item.custom.HyposprayCartridgeItem.Type.ADRENALINE_STIM));

    // Cleanroom Industrial Suite & Bunny Suit
    public static final Item STERILE_POLYMER_FABRIC = registerItem("sterile_polymer_fabric", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(64),
            Component.literal("§fSterile Polymer Fabric"),
            Component.literal("§7Non-linting, anti-static electrostatic microfiber."),
            Component.literal("§8Tailored in the Polymer Loom for cleanroom bunny suits.")
    ));

    public static final Item CLEANROOM_HOOD = registerItem("cleanroom_hood", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.humanoidArmor(ModArmorMaterials.CLEANROOM_SUIT, ArmorType.HELMET),
            Component.literal("§fCleanroom Sanitary Hood"),
            Component.literal("§7Zero-shedding particulate-barrier head covering."),
            Component.literal("§bPart of the Cleanroom Bunny Suit.")
    ));

    public static final Item CLEANROOM_SMOCK = registerItem("cleanroom_smock", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.humanoidArmor(ModArmorMaterials.CLEANROOM_SUIT, ArmorType.CHESTPLATE),
            Component.literal("§fCleanroom Sanitary Smock"),
            Component.literal("§7Sealed-cuff anti-static torso smock."),
            Component.literal("§bPart of the Cleanroom Bunny Suit.")
    ));

    public static final Item CLEANROOM_TROUSERS = registerItem("cleanroom_trousers", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.humanoidArmor(ModArmorMaterials.CLEANROOM_SUIT, ArmorType.LEGGINGS),
            Component.literal("§fCleanroom Sanitary Trousers"),
            Component.literal("§7Lint-free electro-dissipative sterile trousers."),
            Component.literal("§bPart of the Cleanroom Bunny Suit.")
    ));

    public static final Item CLEANROOM_BOOTIES = registerItem("cleanroom_booties", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.humanoidArmor(ModArmorMaterials.CLEANROOM_SUIT, ArmorType.BOOTS),
            Component.literal("§fCleanroom Sanitary Booties"),
            Component.literal("§7Anti-slip ESD non-marking floor boot covers."),
            Component.literal("§bPart of the Cleanroom Bunny Suit.")
    ));

    // Petrochemicals & Fuels
    public static final Item CRUDE_OIL_SLUDGE = registerItem("crude_oil_sludge", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Distill in a §eFuel Refinery §7with an §fEmpty Gas Canister§7."),
            Component.literal("§8Outputs: §6Gasoline Canister §8+ §8Mineral Tar §8byproduct.")
    ));
    public static final Item MINERAL_TAR = registerItem("mineral_tar", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Petrochemical byproduct used to synthesize §8Asphalt Blocks§7."),
            Component.literal("§8Craft with Cobblestone/Deepslate + Gravel.")
    ));
    public static final Item BIOFUEL_CANISTER = registerItem("biofuel_canister", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(16),
            Component.literal("§aEco-Friendly Ethanol Fuel §7for §eATV Engines§7."),
            Component.literal("§8Synthesized in Fuel Refinery from Corn, Wheat, or Potatoes.")
    ));
    public static final Item GASOLINE_CANISTER = registerItem("gasoline_canister", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(16),
            Component.literal("§6Refined Hydrocarbon Fuel §7for §eATV Engines§7."),
            Component.literal("§8Combine with 2 Corn in Fuel Refinery for §dHigh-Octane Racing Fuel§8.")
    ));
    public static final Item HIGH_OCTANE_FUEL_CANISTER = registerItem("high_octane_fuel_canister", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(16),
            Component.literal("§dPremium Racing Fuel §7providing maximum acceleration & top speed."),
            Component.literal("§8Required for Titanium Twin-Turbo ATV Engines.")
    ));

    // Highway & Road Transition Clay Molds
    public static final Item UNFIRED_CONCRETE_CURB = registerItem("unfired_concrete_curb", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Unfired clay mold for concrete curbs."),
            Component.literal("§8Smelt in a Furnace to fire into a finished Concrete Curb.")
    ));
    public static final Item UNFIRED_ROAD_TRANSITION_RAMP = registerItem("unfired_road_transition_ramp", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Unfired sloped clay mold for road transition ramps."),
            Component.literal("§8Smelt in a Furnace to fire into a finished Road Transition Ramp.")
    ));

    // Modular All-Terrain Vehicle (ATV) & Components
    public static final Item ATV_ITEM = registerItem("atv", settings -> new net.enchantedwood.item.custom.AtvItem(settings.stacksTo(1)));
    public static final Item ATV_SEAT = registerItem("atv_seat", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Core component for assembling an All-Terrain Vehicle."),
            Component.literal("§8Craft with Leather + Black Wool + Iron Ingot.")
    ));
    public static final Item RUBBER_TIRE = registerItem("rubber_tire", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Standard vulcanized rubber tire with balanced all-terrain grip."),
            Component.literal("§8Craft with 4 Rubber around 1 Iron Ingot.")
    ));
    public static final Item STEEL_RIM_TIRE = registerItem("steel_rim_tire", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Reinforced steel rim tire with improved highway stability."),
            Component.literal("§8Craft with Rubber Tire + Steel Ingot.")
    ));
    public static final Item TITANIUM_STUDDED_TIRE = registerItem("titanium_studded_tire", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§bStudded ice-grip tire for maximum traction on snow & ice."),
            Component.literal("§8Craft with Rubber Tire + Titanium Ingot.")
    ));
    public static final Item COPPER_ATV_ENGINE = registerItem("copper_atv_engine", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§eStarter Engine §8(~25 km/h) §7• Fuel: Biofuel / Gasoline"),
            Component.literal("§8Craft with Copper Ingots, Piston, Copper Gear, and Redstone.")
    ));
    public static final Item ALUMINUM_ATV_ENGINE = registerItem("aluminum_atv_engine", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§bAgile V4 Engine §8(~40 km/h) §7• Fuel: Biofuel / Gasoline"),
            Component.literal("§8Craft with Aluminum Ingots, Piston, Aluminum Gear, and Redstone.")
    ));
    public static final Item STEEL_ATV_ENGINE = registerItem("steel_atv_engine", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7High-Torque V8 Engine §8(~55 km/h) §7• Fuel: Gasoline / High-Octane"),
            Component.literal("§8Craft with Steel Ingots, Piston, Steel Gear, and Enchanted Redstone.")
    ));
    public static final Item TITANIUM_ATV_ENGINE = registerItem("titanium_atv_engine", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§dTwin-Turbo Nitro Engine §8(~80 km/h) §7• Fuel: High-Octane Racing Fuel"),
            Component.literal("§8Craft with Titanium Ingots, Steel ATV Engine, and Titanium Gear.")
    ));
    public static final Item ALUMINUM_SUSPENSION = registerItem("aluminum_suspension", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§bSport suspension providing §e0.8-block step-up §b& agile responsive handling."),
            Component.literal("§8Craft with Aluminum Ingots + Iron Bars.")
    ));
    public static final Item STEEL_SUSPENSION = registerItem("steel_suspension", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Heavy suspension providing §e1.0-block step-up §7& fall absorption."),
            Component.literal("§8Craft with Steel Ingots + Iron Bars.")
    ));
    public static final Item TITANIUM_SUSPENSION = registerItem("titanium_suspension", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Heavy-duty suspension providing §e1.5-block step-up §7& full fall negation."),
            Component.literal("§8Craft with Titanium Ingots + Iron Bars.")
    ));
    public static final Item ALUMINUM_ATV_CHASSIS = registerItem("aluminum_atv_chassis", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§bLightweight racing chassis frame for agile handling."),
            Component.literal("§8Craft with 7 Aluminum Ingots in an H-shape.")
    ));
    public static final Item STEEL_ATV_CHASSIS = registerItem("steel_atv_chassis", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Reinforced steel chassis offering balanced structural durability."),
            Component.literal("§8Craft with 7 Steel Ingots in an H-shape.")
    ));
    public static final Item TITANIUM_ATV_CHASSIS = registerItem("titanium_atv_chassis", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§dHeavy hazard-shielded chassis built for dimensional exploration."),
            Component.literal("§8Craft with 7 Titanium Ingots in an H-shape.")
    ));
    public static final Item SEALED_HAZARD_CANOPY = registerItem("sealed_hazard_canopy", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.stacksTo(1).fireResistant(),
            Component.literal("§b✦ Pressurized Environmental Cockpit Canopy"),
            Component.literal("§7Reinforced composite canopy with hermetic seals and air scrubbers:"),
            Component.literal("§f• 100% Protection from Acid Rain & Corrosive Waters"),
            Component.literal("§f• 100% Protection from Volcanic Heat & Caldera Scalds"),
            Component.literal("§f• 100% Protection from Atmospheric Hypoxia & Anoxic Caves"),
            Component.literal("§8Install into ATV trunk or cargo slot to seal the cabin.")
    ));
    public static final Item SMALL_CARGO_TRUNK = registerItem("small_cargo_trunk", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Adds §e9 inventory slots §7to the ATV rear cargo rack."),
            Component.literal("§8Craft with Iron Ingots around a Chest.")
    ));
    public static final Item MEDIUM_CARGO_TRUNK = registerItem("medium_cargo_trunk", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Adds §e18 inventory slots §7to the ATV rear cargo rack."),
            Component.literal("§8Craft with Steel Ingots around a Small Cargo Trunk.")
    ));
    public static final Item LARGE_CARGO_TRUNK = registerItem("large_cargo_trunk", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Adds §e27 inventory slots §7to the ATV rear cargo rack."),
            Component.literal("§8Craft with Titanium Ingots around a Medium Cargo Trunk.")
    ));

    // ATV Mining Drill Bits (Replaceable Modules)
    public static final Item IRON_DRILL_BIT = registerItem("iron_drill_bit", settings -> new net.enchantedwood.item.custom.DrillBitItem(net.enchantedwood.item.custom.DrillBitItem.DrillTier.IRON, settings));
    public static final Item STEEL_DRILL_BIT = registerItem("steel_drill_bit", settings -> new net.enchantedwood.item.custom.DrillBitItem(net.enchantedwood.item.custom.DrillBitItem.DrillTier.STEEL, settings));
    public static final Item DIAMOND_DRILL_BIT = registerItem("diamond_drill_bit", settings -> new net.enchantedwood.item.custom.DrillBitItem(net.enchantedwood.item.custom.DrillBitItem.DrillTier.DIAMOND, settings));
    public static final Item TITANIUM_DRILL_BIT = registerItem("titanium_drill_bit", settings -> new net.enchantedwood.item.custom.DrillBitItem(net.enchantedwood.item.custom.DrillBitItem.DrillTier.TITANIUM, settings));
    public static final Item NETHERITE_DRILL_BIT = registerItem("netherite_drill_bit", settings -> new net.enchantedwood.item.custom.DrillBitItem(net.enchantedwood.item.custom.DrillBitItem.DrillTier.NETHERITE, settings.fireResistant()));

    // ATV Lumberjack Tree Harvesters (Replaceable Modules)
    public static final Item IRON_TREE_SAW = registerItem("iron_tree_saw", settings -> new net.enchantedwood.item.custom.TreeSawItem(net.enchantedwood.item.custom.TreeSawItem.SawTier.IRON, settings));
    public static final Item STEEL_TREE_SAW = registerItem("steel_tree_saw", settings -> new net.enchantedwood.item.custom.TreeSawItem(net.enchantedwood.item.custom.TreeSawItem.SawTier.STEEL, settings));
    public static final Item DIAMOND_TREE_SAW = registerItem("diamond_tree_saw", settings -> new net.enchantedwood.item.custom.TreeSawItem(net.enchantedwood.item.custom.TreeSawItem.SawTier.DIAMOND, settings));
    public static final Item TITANIUM_TREE_SAW = registerItem("titanium_tree_saw", settings -> new net.enchantedwood.item.custom.TreeSawItem(net.enchantedwood.item.custom.TreeSawItem.SawTier.TITANIUM, settings));
    public static final Item NETHERITE_TREE_SAW = registerItem("netherite_tree_saw", settings -> new net.enchantedwood.item.custom.TreeSawItem(net.enchantedwood.item.custom.TreeSawItem.SawTier.NETHERITE, settings.fireResistant()));

    // ATV Agricultural Crop Harvesters (Replaceable Modules)
    public static final Item IRON_CROP_HARVESTER = registerItem("iron_crop_harvester", settings -> new net.enchantedwood.item.custom.CropHarvesterItem(net.enchantedwood.item.custom.CropHarvesterItem.HarvesterTier.IRON, settings));
    public static final Item STEEL_CROP_HARVESTER = registerItem("steel_crop_harvester", settings -> new net.enchantedwood.item.custom.CropHarvesterItem(net.enchantedwood.item.custom.CropHarvesterItem.HarvesterTier.STEEL, settings));
    public static final Item DIAMOND_CROP_HARVESTER = registerItem("diamond_crop_harvester", settings -> new net.enchantedwood.item.custom.CropHarvesterItem(net.enchantedwood.item.custom.CropHarvesterItem.HarvesterTier.DIAMOND, settings));
    public static final Item TITANIUM_CROP_HARVESTER = registerItem("titanium_crop_harvester", settings -> new net.enchantedwood.item.custom.CropHarvesterItem(net.enchantedwood.item.custom.CropHarvesterItem.HarvesterTier.TITANIUM, settings));
    public static final Item NETHERITE_CROP_HARVESTER = registerItem("netherite_crop_harvester", settings -> new net.enchantedwood.item.custom.CropHarvesterItem(net.enchantedwood.item.custom.CropHarvesterItem.HarvesterTier.NETHERITE, settings.fireResistant()));

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
    public static final Item COPPER_BUCKET = registerItem("copper_bucket", settings -> new net.enchantedwood.item.custom.CopperBucketItem(net.minecraft.world.level.material.Fluids.EMPTY, settings.stacksTo(16)));
    public static final Item COPPER_WATER_BUCKET = registerItem("copper_water_bucket", settings -> new net.enchantedwood.item.custom.CopperBucketItem(net.minecraft.world.level.material.Fluids.WATER, settings.stacksTo(1).craftRemainder(COPPER_BUCKET)));
    public static final Item COPPER_LAVA_BUCKET = registerItem("copper_lava_bucket", settings -> new net.enchantedwood.item.custom.CopperBucketItem(net.minecraft.world.level.material.Fluids.LAVA, settings.stacksTo(1).craftRemainder(COPPER_BUCKET)));
    public static final Item ENCHANTED_LAVA_BUCKET = registerItem("enchanted_lava_bucket", settings -> new net.enchantedwood.item.custom.CopperBucketItem(net.minecraft.world.level.material.Fluids.LAVA, settings.stacksTo(1).craftRemainder(Items.BUCKET)));
    public static final Item ENCHANTED_COPPER_LAVA_BUCKET = registerItem("enchanted_copper_lava_bucket", settings -> new net.enchantedwood.item.custom.CopperBucketItem(net.minecraft.world.level.material.Fluids.LAVA, settings.stacksTo(1).craftRemainder(COPPER_BUCKET)));
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
            settings -> new Item(settings.fireResistant()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
            });

    // Enchanted Health Upgrades (Heart Lockets)
    public static final Item ENCHANTED_HEART = registerItem("enchanted_heart", settings -> new net.enchantedwood.item.custom.EnchantedHeartItem(2.0f, settings.stacksTo(1)));
    public static final Item IRON_ENCHANTED_HEART = registerItem("iron_enchanted_heart", settings -> new net.enchantedwood.item.custom.EnchantedHeartItem(6.0f, settings.stacksTo(1)));
    public static final Item GOLD_ENCHANTED_HEART = registerItem("gold_enchanted_heart", settings -> new net.enchantedwood.item.custom.EnchantedHeartItem(10.0f, settings.stacksTo(1)));
    public static final Item DIAMOND_ENCHANTED_HEART = registerItem("diamond_enchanted_heart", settings -> new net.enchantedwood.item.custom.EnchantedHeartItem(14.0f, settings.stacksTo(1)));
    public static final Item NETHERITE_ENCHANTED_HEART = registerItem("netherite_enchanted_heart", settings -> new net.enchantedwood.item.custom.EnchantedHeartItem(20.0f, settings.stacksTo(1).fireResistant()));

    // Storage Crystals & Wireless Access
    public static final Item STORAGE_CRYSTAL_1K = registerItem("storage_crystal_1k", settings -> new net.enchantedwood.item.custom.StorageCrystalItem(1000, settings.stacksTo(1)));
    public static final Item STORAGE_CRYSTAL_4K = registerItem("storage_crystal_4k", settings -> new net.enchantedwood.item.custom.StorageCrystalItem(4000, settings.stacksTo(1)));
    public static final Item STORAGE_CRYSTAL_16K = registerItem("storage_crystal_16k", settings -> new net.enchantedwood.item.custom.StorageCrystalItem(16000, settings.stacksTo(1)));
    public static final Item STORAGE_CRYSTAL_64K = registerItem("storage_crystal_64k", settings -> new net.enchantedwood.item.custom.StorageCrystalItem(64000, settings.stacksTo(1)));
    public static final Item WIRELESS_STORAGE_CRYSTAL = registerItem("wireless_storage_crystal", settings -> new net.enchantedwood.item.custom.WirelessStorageCrystalItem(settings.stacksTo(1)));
    public static final Item CHUNK_LOADER_MODULE = registerItem("chunk_loader_module", settings -> new Item(settings.stacksTo(1)));
    public static final Item INTERDIMENSIONAL_CARD = registerItem("interdimensional_card", settings -> new Item(settings.stacksTo(1)));

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
            Component.literal("§7High-purity semiconductor ingot smelted from Quartz Dust."),
            Component.literal("§8Press in a Hydraulic Press to manufacture Silicon Wafers.")
    ));
    public static final Item SILICON_WAFER = registerItem("silicon_wafer", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§7Ultra-thin polished semiconductor substrate."),
            Component.literal("§8Foundation for printing Modular Power Suit micro-circuits.")
    ));
    public static final Item BASIC_COMPUTER_CHIP = registerItem("basic_computer_chip", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§eTier 1 Micro-Controller"),
            Component.literal("§7Essential logic board for Power Suit chassis & battery power routing."),
            Component.literal("§8Controls internal FE distribution across modular suit pieces.")
    ));
    public static final Item ADVANCED_COMPUTER_CHIP = registerItem("advanced_computer_chip", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Component.literal("§bTier 2 Environmental Processor"),
            Component.literal("§7High-frequency logic processor for active suit anomaly protection:"),
            Component.literal("§f• Atmospheric filtration (acid rain, vacuum / no air)"),
            Component.literal("§f• Thermal regulation (extreme heat & extreme cold biomes)")
    ));
    public static final Item QUANTUM_COMPUTER_CHIP = registerItem("quantum_computer_chip", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.fireResistant(),
            Component.literal("§dTier 3 Quantum Core"),
            Component.literal("§7Dimensional computing unit engineered to neutralize severe anomalies:"),
            Component.literal("§f• Spatial distortion dampening & gravity stabilization"),
            Component.literal("§8Required for elite deep-dimension Power Suit modules.")
    ));
    public static final Item METALLURGY_CONTROLLER_CHIP = registerItem("metallurgy_controller_chip", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.fireResistant(),
            Component.literal("§6⚡ Industrial Metallurgy Logic Chip"),
            Component.literal("§7Installs into the Induction Smelter to unlock thermal alloying:"),
            Component.literal("§f• Enables high-precision stoichiometric synthesis of Bronze, Steel & Manyullyn"),
            Component.literal("§8Fabricated in the Circuit Fabricator.")
    ));

    // Modular Power Suit (ShuDynamics 2.0)
    public static final Item MODULAR_POWER_HELMET = registerItem("modular_power_helmet", settings ->
            new net.enchantedwood.item.custom.ModularPowerArmorItem(ArmorType.HELMET, settings.humanoidArmor(ModArmorMaterials.MODULAR_POWER, ArmorType.HELMET).fireResistant()));
    public static final Item MODULAR_POWER_CHESTPLATE = registerItem("modular_power_chestplate", settings ->
            new net.enchantedwood.item.custom.ModularPowerArmorItem(ArmorType.CHESTPLATE, settings.humanoidArmor(ModArmorMaterials.MODULAR_POWER, ArmorType.CHESTPLATE).fireResistant()));
    public static final Item MODULAR_POWER_LEGGINGS = registerItem("modular_power_leggings", settings ->
            new net.enchantedwood.item.custom.ModularPowerArmorItem(ArmorType.LEGGINGS, settings.humanoidArmor(ModArmorMaterials.MODULAR_POWER, ArmorType.LEGGINGS).fireResistant()));
    public static final Item MODULAR_POWER_BOOTS = registerItem("modular_power_boots", settings ->
            new net.enchantedwood.item.custom.ModularPowerArmorItem(ArmorType.BOOTS, settings.humanoidArmor(ModArmorMaterials.MODULAR_POWER, ArmorType.BOOTS).fireResistant()));
    public static final Item NANITE_REPAIR_MATRIX = registerItem("nanite_repair_matrix",
            net.enchantedwood.item.custom.NaniteRepairMatrixItem::new);
    public static final Item HYDROGEN_THRUSTER_MODULE = registerItem("hydrogen_thruster_module", settings ->
            new net.enchantedwood.item.custom.SuitModuleItem(settings, "Hydrogen Thrusters", java.util.List.of(
                    Component.literal("§b• Chemical Rocket Flight Mode §8(Spacebar to fly)"),
                    Component.literal("§7• Refuels automatically from Hydrogen Canisters in inventory"),
                    Component.literal("§e• Safe Glide / Fall Dampening included")
            )));
    public static final Item ION_REPULSOR_MODULE = registerItem("ion_repulsor_module", settings ->
            new net.enchantedwood.item.custom.SuitModuleItem(settings, "Quantum Ion Repulsors", java.util.List.of(
                    Component.literal("§d• Iron Man Repulsor Flight §8(Spacebar to fly / hover)"),
                    Component.literal("§e• Consumes 25 FE / tick (500 FE/s) directly from Battery"),
                    Component.literal("§a• 100% Electric — Zero Fuel Canisters Required!"),
                    Component.literal("§7• Complete Fall Damage Negation")
            )));
    public static final Item NIGHT_VISION_MODULE = registerItem("night_vision_module", settings ->
            new net.enchantedwood.item.custom.SuitModuleItem(settings, "Adaptive Night Vision HUD", java.util.List.of(
                    Component.literal("§b• Optical HUD Night Vision"),
                    Component.literal("§7• Automatically engages when ambient light ≤ 6"),
                    Component.literal("§7• Powers down in illuminated areas (≥ 9 light)"),
                    Component.literal("§e• Energy Cost: §f2 FE / tick §7(only while active in dark)"),
                    Component.literal("§8• Compatible with: §fModular Power Helmet")
            )));
    public static final Item SPEED_SERVO_MODULE = registerItem("speed_servo_module", settings ->
            new net.enchantedwood.item.custom.SuitModuleItem(settings, "Speed Servos", java.util.List.of(
                    Component.literal("§b• Overclocks leg hydraulics for high-speed sprint locomotion"),
                    Component.literal("§f• Grants §bSpeed II §fwhile moving"),
                    Component.literal("§e• Energy Cost: §f2 FE / tick §7while sprinting/moving"),
                    Component.literal("§8• Compatible with: §fModular Power Leggings")
            )));
    public static final Item STEP_ASSIST_MODULE = registerItem("step_assist_module", settings ->
            new net.enchantedwood.item.custom.SuitModuleItem(settings, "Hydraulic Step-Assist", java.util.List.of(
                    Component.literal("§b• Pneumatic servos allow seamless 1.0-block auto-stepping"),
                    Component.literal("§f• Walk up full blocks smoothly without jumping"),
                    Component.literal("§e• Energy Cost: §f1 FE / sec §7while walking"),
                    Component.literal("§8• Compatible with: §fModular Power Boots")
            )));
    public static final Item HIGH_JUMP_MODULE = registerItem("high_jump_module", settings ->
            new net.enchantedwood.item.custom.SuitModuleItem(settings, "High-Jump Actuators", java.util.List.of(
                    Component.literal("§b• Neo-Titanium pneumatic coils propel user upwards"),
                    Component.literal("§f• Grants §bJump Boost II §f(jump over 2.5 blocks)"),
                    Component.literal("§a• 100% Fall Damage Negation §7upon landing"),
                    Component.literal("§e• Energy Cost: §f1 FE / tick §7(while jumping or falling)"),
                    Component.literal("§8• Compatible with: §fModular Power Boots")
            )));
    public static final Item ACID_PROOF_PLATING = registerItem("acid_proof_plating",
            settings -> new net.enchantedwood.item.custom.AcidProofPlatingItem(settings.stacksTo(1).fireResistant()));

    // Phase 2: Metallurgy & Gas Items
    public static final Item RAW_BAUXITE = registerItem("raw_bauxite", Item::new);
    public static final Item ALUMINUM_INGOT = registerItem("aluminum_ingot", Item::new);
    public static final Item ALUMINUM_NUGGET = registerItem("aluminum_nugget", Item::new);
    public static final Item ALUMINUM_GEAR = registerItem("aluminum_gear", settings -> new GearItem(GearTier.ALUMINUM, false, settings));
    public static final Item ENCHANTED_ALUMINUM_GEAR = registerItem("enchanted_aluminum_gear", settings -> new GearItem(GearTier.ALUMINUM, true, settings));
    public static final Item EMPTY_GAS_CANISTER = registerItem("empty_gas_canister", settings -> new Item(settings.stacksTo(16)));
    public static final Item OXYGEN_CANISTER = registerItem("oxygen_canister", settings -> new Item(settings.stacksTo(16)));
    public static final Item HYDROGEN_CANISTER = registerItem("hydrogen_canister", settings -> new net.enchantedwood.item.custom.HydrogenCanisterItem(settings.stacksTo(16)));
    public static final Item HYDROGEN_JETPACK = registerItem("hydrogen_jetpack", settings -> new net.enchantedwood.item.custom.HydrogenJetpackItem(settings.humanoidArmor(ModArmorMaterials.ALUMINUM, ArmorType.CHESTPLATE)));
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
    public static final Item TUNGSTEN_CARBIDE_INGOT = registerItem("tungsten_carbide_ingot", settings -> new Item(settings.fireResistant()));

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

    public static final Item MANYULLYN_INGOT = registerItem("manyullyn_ingot", settings -> new Item(settings.fireResistant()));
    public static final Item MANYULLYN_NUGGET = registerItem("manyullyn_nugget", settings -> new Item(settings.fireResistant()));
    public static final Item MANYULLYN_DUST = registerItem("manyullyn_dust", settings -> new Item(settings.fireResistant()));
    public static final Item MANYULLYN_PLATE = registerItem("manyullyn_plate", settings -> new Item(settings.fireResistant()));

    // Volcanic Minerals & Byproducts
    public static final Item SULFUR_DUST = registerItem("sulfur_dust", Item::new);
    public static final Item VOLCANIC_ASH = registerItem("volcanic_ash", Item::new);
    public static final Item VOLCANIC_FERTILIZER = registerItem("volcanic_fertilizer", net.enchantedwood.item.custom.VolcanicFertilizerItem::new);
    public static final Item FIRE_CRYSTAL = registerItem("fire_crystal", settings -> new Item(settings.fireResistant()));

    // Convergence Minerals, Crystals & Ores
    public static final Item RAW_FLUORITE = registerItem("raw_fluorite", Item::new);
    public static final Item FLUORITE_CRYSTAL = registerItem("fluorite_crystal", Item::new);

    public static final Item RAW_ZIRCONIA = registerItem("raw_zirconia", Item::new);
    public static final Item ZIRCONIA_NODULE = registerItem("zirconia_nodule", settings -> new Item(settings.fireResistant()));

    public static final Item RAW_TANTALUM = registerItem("raw_tantalum", Item::new);
    public static final Item TANTALUM_INGOT = registerItem("tantalum_ingot", Item::new);
    public static final Item TANTALUM_DUST = registerItem("tantalum_dust", Item::new);

    public static final Item RAW_HAFNIUM = registerItem("raw_hafnium", Item::new);
    public static final Item HAFNIUM_INGOT = registerItem("hafnium_ingot", settings -> new Item(settings.fireResistant()));
    public static final Item HAFNIUM_DUST = registerItem("hafnium_dust", settings -> new Item(settings.fireResistant()));

    public static final Item RAW_NEODYMIUM = registerItem("raw_neodymium", Item::new);
    public static final Item NEODYMIUM_MAGNET = registerItem("neodymium_magnet", Item::new);
    public static final Item NEODYMIUM_DUST = registerItem("neodymium_dust", Item::new);

    public static final Item RAW_AEROGEL = registerItem("raw_aerogel", Item::new);
    public static final Item AEROGEL_SHARD = registerItem("aerogel_shard", Item::new);

    // Convergence Superalloys
    public static final Item TAN_TI_INGOT = registerItem("tan_ti_ingot", Item::new);
    public static final Item HAFNIUM_TUNGSTEN_CARBIDE_INGOT = registerItem("hafnium_tungsten_carbide_ingot", settings -> new Item(settings.fireResistant()));
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
    public static final Item STEEL_HELMET = registerItem("steel_helmet", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.STEEL, ArmorType.HELMET)));
    public static final Item STEEL_CHESTPLATE = registerItem("steel_chestplate", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.STEEL, ArmorType.CHESTPLATE)));
    public static final Item STEEL_LEGGINGS = registerItem("steel_leggings", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.STEEL, ArmorType.LEGGINGS)));
    public static final Item STEEL_BOOTS = registerItem("steel_boots", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.STEEL, ArmorType.BOOTS)));

    // Cobalt Tools & Weapons (High attack speed & velocity)
    public static final Item COBALT_SWORD = registerItem("cobalt_sword", settings -> new Item(settings.sword(ModMaterials.COBALT, 3.5f, -2.2f)));
    public static final Item COBALT_PICKAXE = registerItem("cobalt_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.COBALT, 1.5f, -2.6f)));
    public static final Item COBALT_AXE = registerItem("cobalt_axe", settings -> new Item(settings.axe(ModMaterials.COBALT, 6.0f, -2.8f)));
    public static final Item COBALT_SHOVEL = registerItem("cobalt_shovel", settings -> new Item(settings.shovel(ModMaterials.COBALT, 1.5f, -2.8f)));
    public static final Item COBALT_HOE = registerItem("cobalt_hoe", settings -> new Item(settings.hoe(ModMaterials.COBALT, -2.0f, 0.0f)));
    public static final Item COBALT_HAMMER = registerItem("cobalt_hammer", settings -> new HammerItem(settings.pickaxe(ModMaterials.COBALT, 4.5f, -2.8f)));
    public static final Item COBALT_BROAD_AXE = registerItem("cobalt_broad_axe", settings -> new BroadAxeItem(settings.axe(ModMaterials.COBALT, 7.0f, -2.9f)));

    // Cobalt Armor (Lightweight agility)
    public static final Item COBALT_HELMET = registerItem("cobalt_helmet", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.COBALT, ArmorType.HELMET)));
    public static final Item COBALT_CHESTPLATE = registerItem("cobalt_chestplate", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.COBALT, ArmorType.CHESTPLATE)));
    public static final Item COBALT_LEGGINGS = registerItem("cobalt_leggings", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.COBALT, ArmorType.LEGGINGS)));
    public static final Item COBALT_BOOTS = registerItem("cobalt_boots", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.COBALT, ArmorType.BOOTS)));

    // Ardite Tools & Weapons (Heavy durability & stonebound)
    public static final Item ARDITE_SWORD = registerItem("ardite_sword", settings -> new Item(settings.sword(ModMaterials.ARDITE, 4.0f, -2.5f)));
    public static final Item ARDITE_PICKAXE = registerItem("ardite_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.ARDITE, 2.0f, -2.9f)));
    public static final Item ARDITE_AXE = registerItem("ardite_axe", settings -> new Item(settings.axe(ModMaterials.ARDITE, 7.0f, -3.1f)));
    public static final Item ARDITE_SHOVEL = registerItem("ardite_shovel", settings -> new Item(settings.shovel(ModMaterials.ARDITE, 2.0f, -3.0f)));
    public static final Item ARDITE_HOE = registerItem("ardite_hoe", settings -> new Item(settings.hoe(ModMaterials.ARDITE, -1.0f, -1.0f)));
    public static final Item ARDITE_HAMMER = registerItem("ardite_hammer", settings -> new HammerItem(settings.pickaxe(ModMaterials.ARDITE, 5.5f, -3.1f)));
    public static final Item ARDITE_BROAD_AXE = registerItem("ardite_broad_axe", settings -> new BroadAxeItem(settings.axe(ModMaterials.ARDITE, 8.0f, -3.2f)));

    // Ardite Armor (Heavy fortitude)
    public static final Item ARDITE_HELMET = registerItem("ardite_helmet", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.ARDITE, ArmorType.HELMET)));
    public static final Item ARDITE_CHESTPLATE = registerItem("ardite_chestplate", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.ARDITE, ArmorType.CHESTPLATE)));
    public static final Item ARDITE_LEGGINGS = registerItem("ardite_leggings", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.ARDITE, ArmorType.LEGGINGS)));
    public static final Item ARDITE_BOOTS = registerItem("ardite_boots", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.ARDITE, ArmorType.BOOTS)));

    // Manyullyn Tools & Weapons (Supreme Nether Masterwork - Fireproof)
    public static final Item MANYULLYN_SWORD = registerItem("manyullyn_sword", settings -> new Item(settings.sword(ModMaterials.MANYULLYN, 6.0f, -2.4f).fireResistant()));
    public static final Item MANYULLYN_PICKAXE = registerItem("manyullyn_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.MANYULLYN, 2.5f, -2.7f).fireResistant()));
    public static final Item MANYULLYN_AXE = registerItem("manyullyn_axe", settings -> new Item(settings.axe(ModMaterials.MANYULLYN, 8.0f, -2.9f).fireResistant()));
    public static final Item MANYULLYN_SHOVEL = registerItem("manyullyn_shovel", settings -> new Item(settings.shovel(ModMaterials.MANYULLYN, 2.5f, -2.9f).fireResistant()));
    public static final Item MANYULLYN_HOE = registerItem("manyullyn_hoe", settings -> new Item(settings.hoe(ModMaterials.MANYULLYN, -1.0f, 0.0f).fireResistant()));
    public static final Item MANYULLYN_HAMMER = registerItem("manyullyn_hammer", settings -> new HammerItem(settings.pickaxe(ModMaterials.MANYULLYN, 7.0f, -2.9f).fireResistant()));
    public static final Item MANYULLYN_BROAD_AXE = registerItem("manyullyn_broad_axe", settings -> new BroadAxeItem(settings.axe(ModMaterials.MANYULLYN, 9.0f, -3.0f).fireResistant()));

    // Manyullyn Armor (Supreme Nether Masterwork - Fireproof)
    public static final Item MANYULLYN_HELMET = registerItem("manyullyn_helmet", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.MANYULLYN, ArmorType.HELMET).fireResistant()));
    public static final Item MANYULLYN_CHESTPLATE = registerItem("manyullyn_chestplate", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.MANYULLYN, ArmorType.CHESTPLATE).fireResistant()));
    public static final Item MANYULLYN_LEGGINGS = registerItem("manyullyn_leggings", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.MANYULLYN, ArmorType.LEGGINGS).fireResistant()));
    public static final Item MANYULLYN_BOOTS = registerItem("manyullyn_boots", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.MANYULLYN, ArmorType.BOOTS).fireResistant()));

    // Tungsten Tools & Weapons (Heavy Refractory)
    public static final Item TUNGSTEN_SWORD = registerItem("tungsten_sword", settings -> new Item(settings.sword(ModMaterials.TUNGSTEN, 4.5f, -2.4f)));
    public static final Item TUNGSTEN_PICKAXE = registerItem("tungsten_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.TUNGSTEN, 2.0f, -2.8f)));
    public static final Item TUNGSTEN_AXE = registerItem("tungsten_axe", settings -> new Item(settings.axe(ModMaterials.TUNGSTEN, 7.5f, -3.0f)));
    public static final Item TUNGSTEN_SHOVEL = registerItem("tungsten_shovel", settings -> new Item(settings.shovel(ModMaterials.TUNGSTEN, 2.0f, -3.0f)));
    public static final Item TUNGSTEN_HOE = registerItem("tungsten_hoe", settings -> new Item(settings.hoe(ModMaterials.TUNGSTEN, -1.5f, -0.5f)));
    public static final Item TUNGSTEN_HAMMER = registerItem("tungsten_hammer", settings -> new HammerItem(settings.pickaxe(ModMaterials.TUNGSTEN, 6.0f, -3.0f)));
    public static final Item TUNGSTEN_BROAD_AXE = registerItem("tungsten_broad_axe", settings -> new BroadAxeItem(settings.axe(ModMaterials.TUNGSTEN, 8.0f, -3.1f)));

    // Tungsten Armor (High Toughness & Knockback Resistance)
    public static final Item TUNGSTEN_HELMET = registerItem("tungsten_helmet", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.TUNGSTEN, ArmorType.HELMET)));
    public static final Item TUNGSTEN_CHESTPLATE = registerItem("tungsten_chestplate", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.TUNGSTEN, ArmorType.CHESTPLATE)));
    public static final Item TUNGSTEN_LEGGINGS = registerItem("tungsten_leggings", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.TUNGSTEN, ArmorType.LEGGINGS)));
    public static final Item TUNGSTEN_BOOTS = registerItem("tungsten_boots", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.TUNGSTEN, ArmorType.BOOTS)));

    // Aluminum Tools & Weapons
    public static final Item ALUMINUM_SWORD = registerItem("aluminum_sword", settings -> new Item(settings.sword(ModMaterials.ALUMINUM, 3.0f, -2.4f)));
    public static final Item ALUMINUM_PICKAXE = registerItem("aluminum_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.ALUMINUM, 1.0f, -2.8f)));
    public static final Item ALUMINUM_AXE = registerItem("aluminum_axe", settings -> new Item(settings.axe(ModMaterials.ALUMINUM, 6.0f, -3.1f)));
    public static final Item ALUMINUM_SHOVEL = registerItem("aluminum_shovel", settings -> new Item(settings.shovel(ModMaterials.ALUMINUM, 1.5f, -3.0f)));
    public static final Item ALUMINUM_HOE = registerItem("aluminum_hoe", settings -> new Item(settings.hoe(ModMaterials.ALUMINUM, -2.0f, -1.0f)));
    public static final Item ALUMINUM_HAMMER = registerItem("aluminum_hammer", settings -> new HammerItem(settings.pickaxe(ModMaterials.ALUMINUM, 4.0f, -3.0f)));
    public static final Item ALUMINUM_BROAD_AXE = registerItem("aluminum_broad_axe", settings -> new BroadAxeItem(settings.axe(ModMaterials.ALUMINUM, 6.5f, -3.1f)));

    // Aluminum Armor
    public static final Item ALUMINUM_HELMET = registerItem("aluminum_helmet", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.ALUMINUM, ArmorType.HELMET)));
    public static final Item ALUMINUM_CHESTPLATE = registerItem("aluminum_chestplate", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.ALUMINUM, ArmorType.CHESTPLATE)));
    public static final Item ALUMINUM_LEGGINGS = registerItem("aluminum_leggings", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.ALUMINUM, ArmorType.LEGGINGS)));
    public static final Item ALUMINUM_BOOTS = registerItem("aluminum_boots", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.ALUMINUM, ArmorType.BOOTS)));

    // Bronze Tools & Weapons
    public static final Item BRONZE_SWORD = registerItem("bronze_sword", settings -> new Item(settings.sword(ModMaterials.BRONZE, 3.0f, -2.4f)));
    public static final Item BRONZE_PICKAXE = registerItem("bronze_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.BRONZE, 1.0f, -2.8f)));
    public static final Item BRONZE_AXE = registerItem("bronze_axe", settings -> new Item(settings.axe(ModMaterials.BRONZE, 6.0f, -3.1f)));
    public static final Item BRONZE_SHOVEL = registerItem("bronze_shovel", settings -> new Item(settings.shovel(ModMaterials.BRONZE, 1.5f, -3.0f)));
    public static final Item BRONZE_HOE = registerItem("bronze_hoe", settings -> new Item(settings.hoe(ModMaterials.BRONZE, -2.0f, -1.0f)));

    // Bronze Armor
    public static final Item BRONZE_HELMET = registerItem("bronze_helmet", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.BRONZE, ArmorType.HELMET)));
    public static final Item BRONZE_CHESTPLATE = registerItem("bronze_chestplate", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.BRONZE, ArmorType.CHESTPLATE)));
    public static final Item BRONZE_LEGGINGS = registerItem("bronze_leggings", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.BRONZE, ArmorType.LEGGINGS)));
    public static final Item BRONZE_BOOTS = registerItem("bronze_boots", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.BRONZE, ArmorType.BOOTS)));

    // Tin Tools & Weapons
    public static final Item TIN_SWORD = registerItem("tin_sword", settings -> new Item(settings.sword(ModMaterials.TIN, 2.5f, -2.4f)));
    public static final Item TIN_PICKAXE = registerItem("tin_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.TIN, 1.0f, -2.8f)));
    public static final Item TIN_AXE = registerItem("tin_axe", settings -> new Item(settings.axe(ModMaterials.TIN, 5.5f, -3.2f)));
    public static final Item TIN_SHOVEL = registerItem("tin_shovel", settings -> new Item(settings.shovel(ModMaterials.TIN, 1.0f, -3.0f)));
    public static final Item TIN_HOE = registerItem("tin_hoe", settings -> new Item(settings.hoe(ModMaterials.TIN, -2.0f, -1.0f)));

    // Tin Armor
    public static final Item TIN_HELMET = registerItem("tin_helmet", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.TIN, ArmorType.HELMET)));
    public static final Item TIN_CHESTPLATE = registerItem("tin_chestplate", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.TIN, ArmorType.CHESTPLATE)));
    public static final Item TIN_LEGGINGS = registerItem("tin_leggings", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.TIN, ArmorType.LEGGINGS)));
    public static final Item TIN_BOOTS = registerItem("tin_boots", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.TIN, ArmorType.BOOTS)));

    // Titanium Tools & Weapons
    public static final Item TITANIUM_SWORD = registerItem("titanium_sword", settings -> new Item(settings.sword(ModMaterials.TITANIUM, 4.0f, -2.4f)));
    public static final Item TITANIUM_PICKAXE = registerItem("titanium_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.TITANIUM, 2.0f, -2.8f)));
    public static final Item TITANIUM_AXE = registerItem("titanium_axe", settings -> new Item(settings.axe(ModMaterials.TITANIUM, 7.0f, -3.0f)));
    public static final Item TITANIUM_SHOVEL = registerItem("titanium_shovel", settings -> new Item(settings.shovel(ModMaterials.TITANIUM, 2.5f, -3.0f)));
    public static final Item TITANIUM_HOE = registerItem("titanium_hoe", settings -> new Item(settings.hoe(ModMaterials.TITANIUM, -1.0f, 0.0f)));

    // Titanium Armor
    public static final Item TITANIUM_HELMET = registerItem("titanium_helmet", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.TITANIUM, ArmorType.HELMET)));
    public static final Item TITANIUM_CHESTPLATE = registerItem("titanium_chestplate", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.TITANIUM, ArmorType.CHESTPLATE)));
    public static final Item TITANIUM_LEGGINGS = registerItem("titanium_leggings", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.TITANIUM, ArmorType.LEGGINGS)));
    public static final Item TITANIUM_BOOTS = registerItem("titanium_boots", settings -> new Item(settings.humanoidArmor(ModArmorMaterials.TITANIUM, ArmorType.BOOTS)));



    // Tools & Weapons
    public static final Item LIVINGWOOD_SWORD = registerItem("livingwood_sword",
            settings -> new LivingwoodSwordItem(settings.sword(ModMaterials.ENCHANTED_WOOD, 3.0f, -2.4f)));

    public static final Item BARKSKIN_PICKAXE = registerItem("barkskin_pickaxe",
            settings -> new BarkskinPickaxeItem(settings.pickaxe(ModMaterials.ENCHANTED_WOOD, 1.0f, -2.8f)));

    public static final Item IRONWOOD_AXE = registerItem("ironwood_axe",
            settings -> new Item(settings.axe(ModMaterials.ENCHANTED_WOOD, 6.0f, -3.0f).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    public static final Item VERDANT_SHOVEL = registerItem("verdant_shovel",
            settings -> new Item(settings.shovel(ModMaterials.ENCHANTED_WOOD, 1.5f, -3.0f).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    public static final Item WOODEN_SHEARS = registerItem("wooden_shears", settings -> new net.enchantedwood.item.custom.WoodenShearsItem(settings.durability(30)));

    public static final Item ELDERWOOD_HOE = registerItem("elderwood_hoe",
            settings -> new net.enchantedwood.item.custom.AutoHarvestHoeItem(settings.hoe(ModMaterials.ENCHANTED_WOOD, 0.0f, -1.0f)));

    // Armor Set
    public static final Item ENCHANTED_WOOD_HELMET = registerItem("enchanted_wood_helmet",
            settings -> new EnchantedArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_WOOD, ArmorType.HELMET)));

    public static final Item ENCHANTED_WOOD_CHESTPLATE = registerItem("enchanted_wood_chestplate",
            settings -> new EnchantedArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_WOOD, ArmorType.CHESTPLATE)));

    public static final Item ENCHANTED_WOOD_LEGGINGS = registerItem("enchanted_wood_leggings",
            settings -> new EnchantedArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_WOOD, ArmorType.LEGGINGS)));

    public static final Item ENCHANTED_WOOD_BOOTS = registerItem("enchanted_wood_boots",
            settings -> new EnchantedArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_WOOD, ArmorType.BOOTS)));

    // Enchanted Cobblestone Tools & Weapons
    public static final Item ENCHANTED_COBBLESTONE_SWORD = registerItem("enchanted_cobblestone_sword",
            settings -> new EnchantedCobblestoneSwordItem(settings.sword(ModMaterials.ENCHANTED_COBBLESTONE, 3.0f, -2.4f)));

    public static final Item ENCHANTED_COBBLESTONE_PICKAXE = registerItem("enchanted_cobblestone_pickaxe",
            settings -> new EnchantedCobblestonePickaxeItem(settings.pickaxe(ModMaterials.ENCHANTED_COBBLESTONE, 1.0f, -2.8f)));

    public static final Item ENCHANTED_COBBLESTONE_AXE = registerItem("enchanted_cobblestone_axe",
            settings -> new Item(settings.axe(ModMaterials.ENCHANTED_COBBLESTONE, 6.0f, -3.1f)) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
            });

    public static final Item ENCHANTED_COBBLESTONE_SHOVEL = registerItem("enchanted_cobblestone_shovel",
            settings -> new Item(settings.shovel(ModMaterials.ENCHANTED_COBBLESTONE, 1.5f, -3.0f)) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
            });

    public static final Item ENCHANTED_COBBLESTONE_HOE = registerItem("enchanted_cobblestone_hoe",
            settings -> new net.enchantedwood.item.custom.AutoHarvestHoeItem(settings.hoe(ModMaterials.ENCHANTED_COBBLESTONE, -1.0f, -1.0f)));

    // Base 3x3 Mining Sledgehammers
    public static final Item WOODEN_HAMMER = registerItem("wooden_hammer",
            settings -> new HammerItem(settings.pickaxe(ToolMaterial.WOOD, 2.0f, -3.2f)));
    public static final Item STONE_HAMMER = registerItem("stone_hammer",
            settings -> new HammerItem(settings.pickaxe(ToolMaterial.STONE, 3.0f, -3.2f)) {
                @Override
                public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay displayComponent, java.util.function.Consumer<net.minecraft.network.chat.Component> textConsumer, net.minecraft.world.item.TooltipFlag type) {
                    super.appendHoverText(stack, context, displayComponent, textConsumer, type);
                    textConsumer.accept(net.minecraft.network.chat.Component.literal("§8Craftable with Cobblestone, Andesite, Granite,"));
                    textConsumer.accept(net.minecraft.network.chat.Component.literal("§8Diorite, Basalt, Deepslate, or Netherrack."));
                }
            });
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
            settings -> new HammerItem(settings.pickaxe(ToolMaterial.NETHERITE, 6.0f, -2.8f).fireResistant()));
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
            settings -> new BroadAxeItem(settings.axe(ToolMaterial.NETHERITE, 8.0f, -2.9f).fireResistant()));
    public static final Item TITANIUM_BROAD_AXE = registerItem("titanium_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ModMaterials.TITANIUM, 7.5f, -3.0f)));

    public static final Item ENCHANTED_COBBLESTONE_HAMMER = registerItem("enchanted_cobblestone_hammer",
            settings -> new HammerItem(settings.pickaxe(ModMaterials.ENCHANTED_COBBLESTONE, 5.0f, -3.2f)) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
            });
    public static final Item ENCHANTED_COBBLESTONE_BROAD_AXE = registerItem("enchanted_cobblestone_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ModMaterials.ENCHANTED_COBBLESTONE, 7.0f, -3.2f)) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
            });

    // Enchanted Cobblestone Armor Set
    public static final Item ENCHANTED_COBBLESTONE_HELMET = registerItem("enchanted_cobblestone_helmet",
            settings -> new EnchantedCobblestoneArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_COBBLESTONE, ArmorType.HELMET)));

    public static final Item ENCHANTED_COBBLESTONE_CHESTPLATE = registerItem("enchanted_cobblestone_chestplate",
            settings -> new EnchantedCobblestoneArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_COBBLESTONE, ArmorType.CHESTPLATE)));

    public static final Item ENCHANTED_COBBLESTONE_LEGGINGS = registerItem("enchanted_cobblestone_leggings",
            settings -> new EnchantedCobblestoneArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_COBBLESTONE, ArmorType.LEGGINGS)));

    public static final Item ENCHANTED_COBBLESTONE_BOOTS = registerItem("enchanted_cobblestone_boots",
            settings -> new EnchantedCobblestoneArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_COBBLESTONE, ArmorType.BOOTS)));

    // Enchanted Diamond Equipment
    public static final Item ENCHANTED_DIAMOND_HAMMER = registerItem("enchanted_diamond_hammer",
            settings -> new HammerItem(settings.pickaxe(ModMaterials.ENCHANTED_DIAMOND, 6.0f, -3.0f)) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
            });
    public static final Item ENCHANTED_DIAMOND_BROAD_AXE = registerItem("enchanted_diamond_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ModMaterials.ENCHANTED_DIAMOND, 8.0f, -3.0f)) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
            });

    public static final Item ENCHANTED_DIAMOND_HELMET = registerItem("enchanted_diamond_helmet",
            settings -> new EnchantedArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_DIAMOND, ArmorType.HELMET)));
    public static final Item ENCHANTED_DIAMOND_CHESTPLATE = registerItem("enchanted_diamond_chestplate",
            settings -> new EnchantedArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_DIAMOND, ArmorType.CHESTPLATE)));
    public static final Item ENCHANTED_DIAMOND_LEGGINGS = registerItem("enchanted_diamond_leggings",
            settings -> new EnchantedArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_DIAMOND, ArmorType.LEGGINGS)));
    public static final Item ENCHANTED_DIAMOND_BOOTS = registerItem("enchanted_diamond_boots",
            settings -> new EnchantedArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_DIAMOND, ArmorType.BOOTS)));

    // Enchanted Netherite Equipment (Fireproof!)
    public static final Item ENCHANTED_NETHERITE_HAMMER = registerItem("enchanted_netherite_hammer",
            settings -> new HammerItem(settings.pickaxe(ModMaterials.ENCHANTED_NETHERITE, 8.0f, -2.8f).fireResistant()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
            });
    public static final Item ENCHANTED_NETHERITE_BROAD_AXE = registerItem("enchanted_netherite_broad_axe",
            settings -> new BroadAxeItem(settings.axe(ModMaterials.ENCHANTED_NETHERITE, 10.0f, -2.8f).fireResistant()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
            });

    public static final Item ENCHANTED_NETHERITE_HELMET = registerItem("enchanted_netherite_helmet",
            settings -> new EnchantedArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_NETHERITE, ArmorType.HELMET).fireResistant()));
    public static final Item ENCHANTED_NETHERITE_CHESTPLATE = registerItem("enchanted_netherite_chestplate",
            settings -> new EnchantedArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_NETHERITE, ArmorType.CHESTPLATE).fireResistant()));
    public static final Item ENCHANTED_NETHERITE_LEGGINGS = registerItem("enchanted_netherite_leggings",
            settings -> new EnchantedArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_NETHERITE, ArmorType.LEGGINGS).fireResistant()));
    public static final Item ENCHANTED_NETHERITE_BOOTS = registerItem("enchanted_netherite_boots",
            settings -> new EnchantedArmorItem(settings.humanoidArmor(ModArmorMaterials.ENCHANTED_NETHERITE, ArmorType.BOOTS).fireResistant()));

    // Enchanted Chest Items
    public static final Item COPPER_ENCHANTED_CHEST = registerItem("copper_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.COPPER, settings));
    public static final Item BRONZE_ENCHANTED_CHEST = registerItem("bronze_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.BRONZE, settings));
    public static final Item ENCHANTED_IRON_ENCHANTED_CHEST = registerItem("enchanted_iron_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.ENCHANTED_IRON, settings));
    public static final Item GOLD_ENCHANTED_CHEST = registerItem("gold_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.GOLD, settings));
    public static final Item DIAMOND_ENCHANTED_CHEST = registerItem("diamond_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.DIAMOND, settings));
    public static final Item NETHERITE_ENCHANTED_CHEST = registerItem("netherite_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.NETHERITE, settings.fireResistant()));

    // Scuba & Underwater Diving Equipment (v1.2.0)
    public static final Item SNORKEL = registerItem("snorkel",
            settings -> new net.enchantedwood.item.custom.SnorkelItem(ArmorType.HELMET.getSlot(), settings.humanoidArmor(ModArmorMaterials.SCUBA, ArmorType.HELMET)));
    public static final Item DIVING_MASK = registerItem("diving_mask",
            settings -> new net.enchantedwood.item.custom.ScubaArmorItem(ArmorType.HELMET.getSlot(), settings.humanoidArmor(ModArmorMaterials.SCUBA, ArmorType.HELMET)));
    public static final Item SCUBA_CHESTPLATE = registerItem("scuba_chestplate",
            settings -> new net.enchantedwood.item.custom.ScubaArmorItem(ArmorType.CHESTPLATE.getSlot(), settings.humanoidArmor(ModArmorMaterials.SCUBA, ArmorType.CHESTPLATE)));
    public static final Item WETSUIT_LEGGINGS = registerItem("wetsuit_leggings",
            settings -> new net.enchantedwood.item.custom.ScubaArmorItem(ArmorType.LEGGINGS.getSlot(), settings.humanoidArmor(ModArmorMaterials.SCUBA, ArmorType.LEGGINGS)));
    public static final Item DIVING_FLIPPERS = registerItem("diving_flippers",
            settings -> new net.enchantedwood.item.custom.ScubaArmorItem(ArmorType.BOOTS.getSlot(), settings.humanoidArmor(ModArmorMaterials.SCUBA, ArmorType.BOOTS)));
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
                    Component.literal("§6✦ Basalt Pyrometallurgical Flux"),
                    Component.literal("§7Doubles smelting yields and grants +50% speed in Blast Furnaces and Foundries.")));

    public static final Item THERMAL_REFRACTORY_PLATING = registerItem("thermal_refractory_plating",
            settings -> new net.enchantedwood.item.custom.ThermalRefractoryPlatingItem(settings.stacksTo(1).fireResistant()));

    public static final Item BLAZE_OVERCLOCK_CORE = registerItem("blaze_overclock_core",
            settings -> new GearItem(GearTier.BLAZE_OVERCLOCK, true, settings.stacksTo(16).fireResistant()));

    public static final Item INFERNAL_HAMMER = registerItem("infernal_hammer",
            settings -> new net.enchantedwood.item.custom.InfernalHammerItem(settings.pickaxe(ModMaterials.MANYULLYN, 7.0f, -2.9f).fireResistant()));

    public static final Item PLASMA_FLAMETHROWER = registerItem("plasma_flamethrower",
            settings -> new net.enchantedwood.item.custom.PlasmaFlamethrowerItem(settings.durability(850).fireResistant()));

    // Quarry Upgrade Cores
    public static final Item RANGE_UPGRADE_T1 = registerItem("range_upgrade_t1",
            settings -> new net.enchantedwood.item.custom.TooltipItem(settings.stacksTo(16),
                    Component.literal("§b✦ Tier 1 Quarry Range Core"),
                    Component.literal("§7Expands Laser Quarry scanning perimeter to §e3x3 Chunks §7(48x48 blocks).")));

    public static final Item RANGE_UPGRADE_T2 = registerItem("range_upgrade_t2",
            settings -> new net.enchantedwood.item.custom.TooltipItem(settings.stacksTo(16),
                    Component.literal("§d✦ Tier 2 Quarry Range Core"),
                    Component.literal("§7Expands Laser Quarry scanning perimeter to §e5x5 Chunks §7(80x80 blocks).")));

    public static final Item FORTUNE_CORE = registerItem("fortune_core",
            settings -> new net.enchantedwood.item.custom.TooltipItem(settings.stacksTo(16),
                    Component.literal("§6✦ Quarry Fortune Core"),
                    Component.literal("§7Applies §eFortune III §7ore multiplication to extracted ores.")));

    public static final Item SILK_TOUCH_CORE = registerItem("silk_touch_core",
            settings -> new net.enchantedwood.item.custom.TooltipItem(settings.stacksTo(16),
                    Component.literal("§a✦ Quarry Silk Touch Core"),
                    Component.literal("§7Applies §eSilk Touch §7to harvest raw ore blocks intact.")));

    // Creative Tab
    public static final ResourceKey<CreativeModeTab> ENCHANTED_WOOD_GROUP_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "enchanted_wood_group"));
    public static final CreativeModeTab ENCHANTED_WOOD_GROUP = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(LIVINGWOOD_SWORD))
            .title(Component.translatable("itemGroup.enchantedwood.enchanted_wood_group"))
            .displayItems((displayContext, entries) -> {
                entries.accept(net.enchantedwood.block.ModBlocks.ENCHANTED_CHEST);
                entries.accept(COPPER_ENCHANTED_CHEST);
                entries.accept(BRONZE_ENCHANTED_CHEST);
                entries.accept(ENCHANTED_IRON_ENCHANTED_CHEST);
                entries.accept(GOLD_ENCHANTED_CHEST);
                entries.accept(DIAMOND_ENCHANTED_CHEST);
                entries.accept(NETHERITE_ENCHANTED_CHEST);
                entries.accept(INFUSED_HEARTWOOD);


                entries.accept(ENCHANTED_DUST);
                entries.accept(ENCHANTED_WOOD);
                entries.accept(ENCHANTED_COAL);
                entries.accept(ENCHANTED_REDSTONE);
                entries.accept(ENCHANTED_EMERALD);
                entries.accept(RAW_TIN);
                entries.accept(net.enchantedwood.block.ModBlocks.TIN_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.DEEPSLATE_TIN_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.RAW_TIN_BLOCK);
                entries.accept(net.enchantedwood.block.ModBlocks.TIN_BLOCK);
                entries.accept(TIN_INGOT);
                entries.accept(TIN_NUGGET);
                entries.accept(BRONZE_INGOT);
                entries.accept(BRONZE_NUGGET);
                entries.accept(net.enchantedwood.block.ModBlocks.BRONZE_BLOCK);
                entries.accept(IRON_GEAR);
                entries.accept(ENCHANTED_IRON_GEAR);
                entries.accept(COPPER_GEAR);
                entries.accept(ENCHANTED_COPPER_GEAR);
                entries.accept(BRONZE_GEAR);
                entries.accept(ENCHANTED_BRONZE_GEAR);
                entries.accept(GOLD_GEAR);
                entries.accept(ENCHANTED_GOLD_GEAR);
                entries.accept(TITANIUM_GEAR);
                entries.accept(ENCHANTED_TITANIUM_GEAR);
                entries.accept(DIAMOND_GEAR);
                entries.accept(ENCHANTED_DIAMOND_GEAR);
                entries.accept(NETHERITE_GEAR);
                entries.accept(ENCHANTED_NETHERITE_GEAR);
                entries.accept(IRON_DUST);
                entries.accept(COPPER_DUST);
                entries.accept(TIN_DUST);
                entries.accept(BRONZE_DUST);
                entries.accept(GOLD_DUST);
                entries.accept(DIAMOND_DUST);
                entries.accept(NETHERITE_DUST);
                entries.accept(RAW_TITANIUM);
                entries.accept(TITANIUM_INGOT);
                entries.accept(TITANIUM_NUGGET);
                entries.accept(TITANIUM_DUST);
                entries.accept(TITANIUM_ROLLER);
                entries.accept(net.enchantedwood.block.ModBlocks.TITANIUM_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.DEEPSLATE_TITANIUM_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.RAW_TITANIUM_BLOCK);
                entries.accept(net.enchantedwood.block.ModBlocks.TITANIUM_BLOCK);
                entries.accept(EMERALD_DUST);
                entries.accept(COAL_DUST);
                entries.accept(QUARTZ_DUST);
                entries.accept(net.enchantedwood.block.ModBlocks.ENCHANTED_COBBLESTONE);
                entries.accept(net.enchantedwood.block.ModBlocks.ENCHANTED_FURNACE);
                entries.accept(net.enchantedwood.block.ModBlocks.CRUSHER);
                entries.accept(net.enchantedwood.block.ModBlocks.DUST_SMELTER);
                entries.accept(net.enchantedwood.block.ModBlocks.ENCHANTED_COAL_BLOCK);
                entries.accept(net.enchantedwood.block.ModBlocks.ENCHANTED_LAVA_GENERATOR);
                entries.accept(net.enchantedwood.block.ModBlocks.COPPER_GENERATOR);
                entries.accept(net.enchantedwood.block.ModBlocks.COPPER_BATTERY);
                entries.accept(COPPER_BATTERY_PACK);
                entries.accept(net.enchantedwood.block.ModBlocks.COPPER_CABLE);

                entries.accept(net.enchantedwood.block.ModBlocks.ENCHANTED_STORAGE_CONTROLLER);
                entries.accept(net.enchantedwood.block.ModBlocks.ENCHANTED_DRIVE_BAY);
                entries.accept(net.enchantedwood.block.ModBlocks.ENCHANTED_STORAGE_TERMINAL);
                entries.accept(net.enchantedwood.block.ModBlocks.DIGITAL_CONVERTER);
                entries.accept(net.enchantedwood.block.ModBlocks.SUPER_COMPUTER);

                // Microelectronics & Industrial Metallurgy (ShuDynamics 2.0)
                entries.accept(SILICON);
                entries.accept(SILICON_WAFER);
                entries.accept(BASIC_COMPUTER_CHIP);
                entries.accept(ADVANCED_COMPUTER_CHIP);
                entries.accept(QUANTUM_COMPUTER_CHIP);
                entries.accept(METALLURGY_CONTROLLER_CHIP);
                entries.accept(net.enchantedwood.block.ModBlocks.CIRCUIT_FABRICATOR);
                entries.accept(net.enchantedwood.block.ModBlocks.INDUCTION_SMELTER);
                entries.accept(net.enchantedwood.block.ModBlocks.CASTING_PORT);
                entries.accept(net.enchantedwood.block.ModBlocks.POWERED_ANVIL);

                entries.accept(net.enchantedwood.block.ModBlocks.LASER_QUARRY);
                entries.accept(RANGE_UPGRADE_T1);
                entries.accept(RANGE_UPGRADE_T2);
                entries.accept(FORTUNE_CORE);
                entries.accept(SILK_TOUCH_CORE);
                entries.accept(STORAGE_CRYSTAL_1K);
                entries.accept(STORAGE_CRYSTAL_4K);
                entries.accept(STORAGE_CRYSTAL_16K);
                entries.accept(STORAGE_CRYSTAL_64K);
                entries.accept(WIRELESS_STORAGE_CRYSTAL);
                entries.accept(BRONZE_SWORD);
                entries.accept(BRONZE_PICKAXE);
                entries.accept(BRONZE_AXE);
                entries.accept(BRONZE_SHOVEL);
                entries.accept(BRONZE_HOE);
                entries.accept(BRONZE_HELMET);
                entries.accept(BRONZE_CHESTPLATE);
                entries.accept(BRONZE_LEGGINGS);
                entries.accept(BRONZE_BOOTS);

                entries.accept(TIN_SWORD);
                entries.accept(TIN_PICKAXE);
                entries.accept(TIN_AXE);
                entries.accept(TIN_SHOVEL);
                entries.accept(TIN_HOE);
                entries.accept(TIN_HELMET);
                entries.accept(TIN_CHESTPLATE);
                entries.accept(TIN_LEGGINGS);
                entries.accept(TIN_BOOTS);

                entries.accept(TITANIUM_SWORD);
                entries.accept(TITANIUM_PICKAXE);
                entries.accept(TITANIUM_AXE);
                entries.accept(TITANIUM_SHOVEL);
                entries.accept(TITANIUM_HOE);
                entries.accept(TITANIUM_HELMET);
                entries.accept(TITANIUM_CHESTPLATE);
                entries.accept(TITANIUM_LEGGINGS);
                entries.accept(TITANIUM_BOOTS);

                entries.accept(LIVINGWOOD_SWORD);
                entries.accept(BARKSKIN_PICKAXE);
                entries.accept(IRONWOOD_AXE);
                entries.accept(VERDANT_SHOVEL);
                entries.accept(ELDERWOOD_HOE);
                entries.accept(WOODEN_SHEARS);
                entries.accept(ENCHANTED_WOOD_HELMET);
                entries.accept(ENCHANTED_WOOD_CHESTPLATE);
                entries.accept(ENCHANTED_WOOD_LEGGINGS);
                entries.accept(ENCHANTED_WOOD_BOOTS);
                entries.accept(ENCHANTED_DIAMOND);
                entries.accept(ENCHANTED_NETHERITE_INGOT);
                entries.accept(net.enchantedwood.block.ModBlocks.ENCHANTED_NETHERITE_BLOCK);
                entries.accept(ENCHANTED_COBBLESTONE_SWORD);
                entries.accept(ENCHANTED_COBBLESTONE_PICKAXE);
                entries.accept(ENCHANTED_COBBLESTONE_AXE);
                entries.accept(ENCHANTED_COBBLESTONE_SHOVEL);
                entries.accept(ENCHANTED_COBBLESTONE_HOE);
                entries.accept(WOODEN_HAMMER);
                entries.accept(STONE_HAMMER);
                entries.accept(COPPER_HAMMER);
                entries.accept(IRON_HAMMER);
                entries.accept(BRONZE_HAMMER);
                entries.accept(GOLDEN_HAMMER);
                entries.accept(DIAMOND_HAMMER);
                entries.accept(NETHERITE_HAMMER);
                entries.accept(TITANIUM_HAMMER);
                entries.accept(WOODEN_BROAD_AXE);
                entries.accept(STONE_BROAD_AXE);
                entries.accept(COPPER_BROAD_AXE);
                entries.accept(IRON_BROAD_AXE);
                entries.accept(BRONZE_BROAD_AXE);
                entries.accept(GOLDEN_BROAD_AXE);
                entries.accept(DIAMOND_BROAD_AXE);
                entries.accept(NETHERITE_BROAD_AXE);
                entries.accept(TITANIUM_BROAD_AXE);
                entries.accept(ENCHANTED_COBBLESTONE_HAMMER);
                entries.accept(ENCHANTED_COBBLESTONE_BROAD_AXE);
                entries.accept(ENCHANTED_COBBLESTONE_HELMET);
                entries.accept(ENCHANTED_COBBLESTONE_CHESTPLATE);
                entries.accept(ENCHANTED_COBBLESTONE_LEGGINGS);
                entries.accept(ENCHANTED_COBBLESTONE_BOOTS);
                entries.accept(ENCHANTED_DIAMOND_HAMMER);
                entries.accept(ENCHANTED_DIAMOND_BROAD_AXE);
                entries.accept(ENCHANTED_DIAMOND_HELMET);
                entries.accept(ENCHANTED_DIAMOND_CHESTPLATE);
                entries.accept(ENCHANTED_DIAMOND_LEGGINGS);
                entries.accept(ENCHANTED_DIAMOND_BOOTS);
                entries.accept(ENCHANTED_NETHERITE_HAMMER);
                entries.accept(ENCHANTED_NETHERITE_BROAD_AXE);
                entries.accept(ENCHANTED_NETHERITE_HELMET);
                entries.accept(ENCHANTED_NETHERITE_CHESTPLATE);
                entries.accept(ENCHANTED_NETHERITE_LEGGINGS);
                entries.accept(ENCHANTED_NETHERITE_BOOTS);
                entries.accept(ENCHANTED_CAPE);
                entries.accept(ENCHANTED_HEART);
                entries.accept(IRON_ENCHANTED_HEART);
                entries.accept(GOLD_ENCHANTED_HEART);
                entries.accept(DIAMOND_ENCHANTED_HEART);
                entries.accept(NETHERITE_ENCHANTED_HEART);
                entries.accept(COPPER_BUCKET);
                entries.accept(COPPER_WATER_BUCKET);
                entries.accept(COPPER_LAVA_BUCKET);
                entries.accept(ENCHANTED_LAVA_BUCKET);
                entries.accept(ENCHANTED_COPPER_LAVA_BUCKET);

                // Energy & Phase 2 Metallurgy
                entries.accept(net.enchantedwood.block.ModBlocks.GAS_PIPE);
                entries.accept(net.enchantedwood.block.ModBlocks.HYDROGEN_PIPE);
                entries.accept(net.enchantedwood.block.ModBlocks.OXYGEN_GENERATOR);
                entries.accept(net.enchantedwood.block.ModBlocks.ALUMINUM_REFINER);
                entries.accept(net.enchantedwood.block.ModBlocks.ALUMINUM_GENERATOR);
                entries.accept(net.enchantedwood.block.ModBlocks.ALUMINUM_BATTERY);
                entries.accept(ALUMINUM_BATTERY_PACK);
                entries.accept(net.enchantedwood.block.ModBlocks.ALUMINUM_CABLE);

                entries.accept(net.enchantedwood.block.ModBlocks.BAUXITE_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.DEEPSLATE_BAUXITE_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.RAW_BAUXITE_BLOCK);
                entries.accept(net.enchantedwood.block.ModBlocks.ALUMINUM_BLOCK);

                entries.accept(RAW_BAUXITE);
                entries.accept(BAUXITE_DUST);
                entries.accept(ALUMINUM_INGOT);
                entries.accept(ALUMINUM_NUGGET);
                entries.accept(ALUMINUM_GEAR);
                entries.accept(EMPTY_GAS_CANISTER);
                entries.accept(OXYGEN_CANISTER);
                entries.accept(HYDROGEN_CANISTER);
                entries.accept(HYDROGEN_JETPACK);
                entries.accept(OXY_HYDROGEN_TORCH);

                entries.accept(ENCHANTED_ALUMINUM_GEAR);
                entries.accept(ALUMINUM_SWORD);
                entries.accept(ALUMINUM_PICKAXE);
                entries.accept(ALUMINUM_AXE);
                entries.accept(ALUMINUM_SHOVEL);
                entries.accept(ALUMINUM_HOE);
                entries.accept(ALUMINUM_HAMMER);
                entries.accept(ALUMINUM_BROAD_AXE);
                entries.accept(ALUMINUM_HELMET);
                entries.accept(ALUMINUM_CHESTPLATE);
                entries.accept(ALUMINUM_LEGGINGS);
                entries.accept(ALUMINUM_BOOTS);

                // Phase 3: Coke Coal & Steel Grid
                entries.accept(net.enchantedwood.block.ModBlocks.COKE_OVEN);
                entries.accept(net.enchantedwood.block.ModBlocks.STEEL_BLAST_FURNACE);
                entries.accept(net.enchantedwood.block.ModBlocks.STEEL_GENERATOR);
                entries.accept(net.enchantedwood.block.ModBlocks.STEEL_BATTERY);
                entries.accept(STEEL_BATTERY_PACK);
                entries.accept(net.enchantedwood.block.ModBlocks.STEEL_CABLE);
                entries.accept(net.enchantedwood.block.ModBlocks.STEEL_BLOCK);

                entries.accept(COKE_COAL);
                entries.accept(net.enchantedwood.block.ModBlocks.COKE_COAL_BLOCK);
                entries.accept(STEEL_INGOT);
                entries.accept(STEEL_NUGGET);
                entries.accept(STEEL_DUST);
                entries.accept(STEEL_GEAR);
                entries.accept(ENCHANTED_STEEL_GEAR);

                entries.accept(STEEL_SWORD);
                entries.accept(STEEL_PICKAXE);
                entries.accept(STEEL_AXE);
                entries.accept(STEEL_SHOVEL);
                entries.accept(STEEL_HOE);
                entries.accept(STEEL_HAMMER);
                entries.accept(STEEL_BROAD_AXE);
                entries.accept(STEEL_HELMET);
                entries.accept(STEEL_CHESTPLATE);
                entries.accept(STEEL_LEGGINGS);
                entries.accept(STEEL_BOOTS);

                // Rubber Tree & Polymers
                entries.accept(net.enchantedwood.block.ModBlocks.RUBBER_LOG);
                entries.accept(net.enchantedwood.block.ModBlocks.RUBBER_WOOD);
                entries.accept(net.enchantedwood.block.ModBlocks.STRIPPED_RUBBER_LOG);
                entries.accept(net.enchantedwood.block.ModBlocks.STRIPPED_RUBBER_WOOD);
                entries.accept(net.enchantedwood.block.ModBlocks.RUBBER_PLANKS);
                entries.accept(net.enchantedwood.block.ModBlocks.RUBBER_LEAVES);
                entries.accept(net.enchantedwood.block.ModBlocks.RUBBER_SAPLING);
                entries.accept(RESIN);
                entries.accept(RUBBER);

                // Storage Network Upgrades
                entries.accept(CHUNK_LOADER_MODULE);
                entries.accept(INTERDIMENSIONAL_CARD);

                // Scuba & Underwater Diving Equipment (v1.2.0)
                entries.accept(SNORKEL);
                entries.accept(DIVING_MASK);
                entries.accept(SCUBA_CHESTPLATE);
                entries.accept(WETSUIT_LEGGINGS);
                entries.accept(DIVING_FLIPPERS);

                // Anomaly Keystones
                entries.accept(net.enchantedwood.block.ModBlocks.ATMOSPHERIC_ANCHOR);
                entries.accept(net.enchantedwood.block.ModBlocks.KINETIC_ANCHOR);
                entries.accept(net.enchantedwood.block.ModBlocks.THERMAL_ANCHOR);
                entries.accept(net.enchantedwood.block.ModBlocks.METALLURGICAL_ANCHOR);
                entries.accept(net.enchantedwood.block.ModBlocks.PLASMA_ANCHOR);
                entries.accept(net.enchantedwood.block.ModBlocks.DIMENSIONAL_SINGULARITY);
                entries.accept(MYSTERY_KEYSTONE);

                // Nether Metallurgy & Materials
                entries.accept(RAW_TUNGSTEN);
                entries.accept(TUNGSTEN_INGOT);
                entries.accept(TUNGSTEN_NUGGET);
                entries.accept(TUNGSTEN_DUST);
                entries.accept(TUNGSTEN_PLATE);
                entries.accept(TUNGSTEN_CARBIDE_INGOT);
                entries.accept(TUNGSTEN_SWORD);
                entries.accept(TUNGSTEN_PICKAXE);
                entries.accept(TUNGSTEN_AXE);
                entries.accept(TUNGSTEN_SHOVEL);
                entries.accept(TUNGSTEN_HOE);
                entries.accept(TUNGSTEN_HAMMER);
                entries.accept(TUNGSTEN_BROAD_AXE);
                entries.accept(TUNGSTEN_HELMET);
                entries.accept(TUNGSTEN_CHESTPLATE);
                entries.accept(TUNGSTEN_LEGGINGS);
                entries.accept(TUNGSTEN_BOOTS);
                entries.accept(net.enchantedwood.block.ModBlocks.NETHER_IRON_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.NETHER_COAL_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.NETHER_COPPER_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.NETHER_TIN_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.NETHER_REDSTONE_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.NETHER_LAPIS_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.NETHER_DIAMOND_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.NETHER_TUNGSTEN_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.DEEPSLATE_TUNGSTEN_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.RAW_TUNGSTEN_BLOCK);
                entries.accept(net.enchantedwood.block.ModBlocks.TUNGSTEN_BLOCK);

                entries.accept(RAW_COBALT);
                entries.accept(COBALT_INGOT);
                entries.accept(COBALT_NUGGET);
                entries.accept(COBALT_DUST);
                entries.accept(COBALT_PLATE);
                entries.accept(COBALT_SWORD);
                entries.accept(COBALT_PICKAXE);
                entries.accept(COBALT_AXE);
                entries.accept(COBALT_SHOVEL);
                entries.accept(COBALT_HOE);
                entries.accept(COBALT_HAMMER);
                entries.accept(COBALT_BROAD_AXE);
                entries.accept(COBALT_HELMET);
                entries.accept(COBALT_CHESTPLATE);
                entries.accept(COBALT_LEGGINGS);
                entries.accept(COBALT_BOOTS);
                entries.accept(net.enchantedwood.block.ModBlocks.COBALT_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.RAW_COBALT_BLOCK);
                entries.accept(net.enchantedwood.block.ModBlocks.COBALT_BLOCK);

                entries.accept(RAW_ARDITE);
                entries.accept(ARDITE_INGOT);
                entries.accept(ARDITE_NUGGET);
                entries.accept(ARDITE_DUST);
                entries.accept(ARDITE_PLATE);
                entries.accept(ARDITE_SWORD);
                entries.accept(ARDITE_PICKAXE);
                entries.accept(ARDITE_AXE);
                entries.accept(ARDITE_SHOVEL);
                entries.accept(ARDITE_HOE);
                entries.accept(ARDITE_HAMMER);
                entries.accept(ARDITE_BROAD_AXE);
                entries.accept(ARDITE_HELMET);
                entries.accept(ARDITE_CHESTPLATE);
                entries.accept(ARDITE_LEGGINGS);
                entries.accept(ARDITE_BOOTS);
                entries.accept(net.enchantedwood.block.ModBlocks.ARDITE_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.RAW_ARDITE_BLOCK);
                entries.accept(net.enchantedwood.block.ModBlocks.ARDITE_BLOCK);

                entries.accept(MANYULLYN_INGOT);
                entries.accept(MANYULLYN_NUGGET);
                entries.accept(MANYULLYN_DUST);
                entries.accept(MANYULLYN_PLATE);
                entries.accept(MANYULLYN_SWORD);
                entries.accept(MANYULLYN_PICKAXE);
                entries.accept(MANYULLYN_AXE);
                entries.accept(MANYULLYN_SHOVEL);
                entries.accept(MANYULLYN_HOE);
                entries.accept(MANYULLYN_HAMMER);
                entries.accept(MANYULLYN_BROAD_AXE);
                entries.accept(MANYULLYN_HELMET);
                entries.accept(MANYULLYN_CHESTPLATE);
                entries.accept(MANYULLYN_LEGGINGS);
                entries.accept(MANYULLYN_BOOTS);
                entries.accept(net.enchantedwood.block.ModBlocks.MANYULLYN_BLOCK);

                entries.accept(SULFUR_DUST);
                entries.accept(VOLCANIC_ASH);
                entries.accept(VOLCANIC_FERTILIZER);
                entries.accept(FIRE_CRYSTAL);

                // Nether Factory & Tier 4 Power Infrastructure
                entries.accept(net.enchantedwood.block.ModBlocks.TUNGSTEN_BATTERY);
                entries.accept(TUNGSTEN_BATTERY_PACK);
                entries.accept(net.enchantedwood.block.ModBlocks.TUNGSTEN_CABLE);
                entries.accept(net.enchantedwood.block.ModBlocks.GEOTHERMAL_GENERATOR);
                entries.accept(net.enchantedwood.block.ModBlocks.ALLOY_FOUNDRY);
                entries.accept(net.enchantedwood.block.ModBlocks.HYDRAULIC_PRESS);
                entries.accept(net.enchantedwood.block.ModBlocks.ITEM_SALVAGER);
                entries.accept(net.enchantedwood.block.ModBlocks.MAGMA_CRUCIBLE);
                entries.accept(net.enchantedwood.block.ModBlocks.LAVA_PUMP);
                entries.accept(net.enchantedwood.block.ModBlocks.WATER_PUMP);
                entries.accept(net.enchantedwood.block.ModBlocks.CRYO_FREEZER);
                entries.accept(net.enchantedwood.block.ModBlocks.CRUSHER_MK2);
                entries.accept(net.enchantedwood.block.ModBlocks.DUST_SMELTER_MK2);
                entries.accept(net.enchantedwood.block.ModBlocks.SOIL_INFUSER);

                // 5x5 Multiblock Titanium Lava Reservoir & Titanium Lava Pipes
                entries.accept(net.enchantedwood.block.ModBlocks.TITANIUM_LAVA_PIPE);
                entries.accept(net.enchantedwood.block.ModBlocks.TITANIUM_TANK_CASING);
                entries.accept(net.enchantedwood.block.ModBlocks.REINFORCED_TANK_GLASS);
                entries.accept(net.enchantedwood.block.ModBlocks.TITANIUM_TANK_INBOUND_PORT);

                // Universal Item Logistics System
                entries.accept(net.enchantedwood.block.ModBlocks.WATER_PIPE);
                entries.accept(net.enchantedwood.block.ModBlocks.ITEM_PIPE);
                entries.accept(net.enchantedwood.block.ModBlocks.ITEM_EXTRACTOR);
                entries.accept(net.enchantedwood.block.ModBlocks.ITEM_INSERTER);
                entries.accept(WRENCH);

                // Volcanic Agronomy & Pozzolanic Architecture
                entries.accept(net.enchantedwood.block.ModBlocks.VOLCANIC_SOIL);
                entries.accept(net.enchantedwood.block.ModBlocks.POZZOLANIC_ASPHALT);
                entries.accept(net.enchantedwood.block.ModBlocks.VOLCANIC_BRICKS);
                entries.accept(net.enchantedwood.block.ModBlocks.VOLCANIC_BRICK_STAIRS);
                entries.accept(net.enchantedwood.block.ModBlocks.VOLCANIC_BRICK_SLAB);

                // Agriculture & Biofuels
                entries.accept(CORN);
                entries.accept(ROASTED_CORN);
                entries.accept(CORN_SEEDS);

                // Convergence Cuisine & Ingredients
                entries.accept(RICE);
                entries.accept(RICE_SEEDS);
                entries.accept(SUSHI_RICE);
                entries.accept(CUCUMBER);
                entries.accept(CUCUMBER_SEEDS);
                entries.accept(AVOCADO);
                entries.accept(net.enchantedwood.block.ModBlocks.AVOCADO_SAPLING);
                entries.accept(net.enchantedwood.block.ModBlocks.AVOCADO_LOG);
                entries.accept(net.enchantedwood.block.ModBlocks.AVOCADO_WOOD);
                entries.accept(net.enchantedwood.block.ModBlocks.AVOCADO_LEAVES);
                entries.accept(net.enchantedwood.block.ModBlocks.WILD_RICE);
                entries.accept(net.enchantedwood.block.ModBlocks.WILD_CUCUMBER);
                entries.accept(NORI_SHEET);

                // Gourmet Sushi Rolls
                entries.accept(SALMON_ROLL);
                entries.accept(COD_ROLL);
                entries.accept(AVOCADO_CUCUMBER_ROLL);
                entries.accept(CALIFORNIA_ROLL);
                entries.accept(GARDEN_ROLL);
                entries.accept(MASTER_RAINBOW_ROLL);

                // Protective Hazard-Shield Foods (Hypospray Alternatives)
                entries.accept(ALKALINE_DETOX_ROLL);
                entries.accept(VOLCANIC_DRAGON_ROLL);
                entries.accept(HIGH_ALTITUDE_KELP_ROLL);
                entries.accept(SURVIVALIST_BENTO);

                // Petrochemicals & Fuels
                entries.accept(net.enchantedwood.block.ModBlocks.OIL_SAND);
                entries.accept(CRUDE_OIL_SLUDGE);
                entries.accept(MINERAL_TAR);
                entries.accept(BIOFUEL_CANISTER);
                entries.accept(GASOLINE_CANISTER);
                entries.accept(HIGH_OCTANE_FUEL_CANISTER);
                entries.accept(net.enchantedwood.block.ModBlocks.FUEL_REFINERY);
                entries.accept(net.enchantedwood.block.ModBlocks.ROAD_PAVER);
                entries.accept(net.enchantedwood.block.ModBlocks.ROAD_PAVER_MK2);
                entries.accept(net.enchantedwood.block.ModBlocks.ASPHALT_BLOCK);
                entries.accept(net.enchantedwood.block.ModBlocks.ASPHALT_SLAB);
                entries.accept(net.enchantedwood.block.ModBlocks.CONCRETE_CURB);
                entries.accept(net.enchantedwood.block.ModBlocks.ROAD_TRANSITION_RAMP);
                entries.accept(net.enchantedwood.block.ModBlocks.ASPHALT_TRANSITION_RAMP);
                entries.accept(net.enchantedwood.block.ModBlocks.ASPHALT_RAMP);
                entries.accept(net.enchantedwood.block.ModBlocks.CONCRETE_CURB_RAMP);
                entries.accept(UNFIRED_CONCRETE_CURB);
                entries.accept(UNFIRED_ROAD_TRANSITION_RAMP);

                // Modular All-Terrain Vehicles (ATV) & Upgrades
                entries.accept(net.enchantedwood.block.ModBlocks.VEHICLE_FABRICATOR);
                entries.accept(ATV_ITEM);
                entries.accept(ATV_SEAT);
                entries.accept(RUBBER_TIRE);
                entries.accept(STEEL_RIM_TIRE);
                entries.accept(TITANIUM_STUDDED_TIRE);
                entries.accept(COPPER_ATV_ENGINE);
                entries.accept(ALUMINUM_ATV_ENGINE);
                entries.accept(STEEL_ATV_ENGINE);
                entries.accept(TITANIUM_ATV_ENGINE);
                entries.accept(ALUMINUM_SUSPENSION);
                entries.accept(STEEL_SUSPENSION);
                entries.accept(TITANIUM_SUSPENSION);
                entries.accept(ALUMINUM_ATV_CHASSIS);
                entries.accept(STEEL_ATV_CHASSIS);
                entries.accept(TITANIUM_ATV_CHASSIS);
                entries.accept(SEALED_HAZARD_CANOPY);
                entries.accept(SMALL_CARGO_TRUNK);
                entries.accept(MEDIUM_CARGO_TRUNK);
                entries.accept(LARGE_CARGO_TRUNK);
                entries.accept(IRON_DRILL_BIT);
                entries.accept(STEEL_DRILL_BIT);
                entries.accept(DIAMOND_DRILL_BIT);
                entries.accept(TITANIUM_DRILL_BIT);
                entries.accept(NETHERITE_DRILL_BIT);

                entries.accept(IRON_TREE_SAW);
                entries.accept(STEEL_TREE_SAW);
                entries.accept(DIAMOND_TREE_SAW);
                entries.accept(TITANIUM_TREE_SAW);
                entries.accept(NETHERITE_TREE_SAW);

                entries.accept(IRON_CROP_HARVESTER);
                entries.accept(STEEL_CROP_HARVESTER);
                entries.accept(DIAMOND_CROP_HARVESTER);
                entries.accept(TITANIUM_CROP_HARVESTER);
                entries.accept(NETHERITE_CROP_HARVESTER);

                entries.accept(HALOGEN_HEADLIGHTS);
                entries.accept(LED_FLOODLIGHTS);
                entries.accept(XENON_HIGH_BEAMS);

                // Enchanted Lighting
                entries.accept(net.enchantedwood.block.ModBlocks.ENCHANTED_LAMP);

                // Nether Metallurgy & Heavy Technology Suite (v1.4.0 Finalization)
                entries.accept(net.enchantedwood.block.ModBlocks.BASALT_CABLE);
                entries.accept(net.enchantedwood.block.ModBlocks.REINFORCED_OBSIDIAN);
                entries.accept(net.enchantedwood.block.ModBlocks.VOLCANIC_GLASS);
                entries.accept(BASALT_FLUX_CATALYST);
                entries.accept(THERMAL_REFRACTORY_PLATING);
                entries.accept(BLAZE_OVERCLOCK_CORE);
                entries.accept(INFERNAL_HAMMER);
                entries.accept(PLASMA_FLAMETHROWER);

                // Boss Combat Culinary Dishes & Convergence Botanicals
                entries.accept(WASABI_ROOT);
                entries.accept(DRAGON_FRUIT);
                entries.accept(STARFRUIT);

                entries.accept(net.enchantedwood.block.ModBlocks.WILD_WASABI);
                entries.accept(net.enchantedwood.block.ModBlocks.WILD_DRAGON_FRUIT);
                entries.accept(net.enchantedwood.block.ModBlocks.STARFRUIT_SAPLING);
                entries.accept(net.enchantedwood.block.ModBlocks.STARFRUIT_LOG);
                entries.accept(net.enchantedwood.block.ModBlocks.STARFRUIT_WOOD);
                entries.accept(net.enchantedwood.block.ModBlocks.STRIPPED_STARFRUIT_LOG);
                entries.accept(net.enchantedwood.block.ModBlocks.STRIPPED_STARFRUIT_WOOD);
                entries.accept(net.enchantedwood.block.ModBlocks.STARFRUIT_PLANKS);
                entries.accept(net.enchantedwood.block.ModBlocks.STARFRUIT_LEAVES);

                entries.accept(PITAYA_BOWL);
                entries.accept(WASABI_NIGIRI);
                entries.accept(GOLDEN_HONEY_MOCHI);
                entries.accept(STARFRUIT_TART);

                // Convergence Mob Variant Spawn Eggs
                entries.accept(CONVERGENCE_ZOMBIE_SPAWN_EGG);
                entries.accept(CONVERGENCE_SKELETON_SPAWN_EGG);
                entries.accept(CONVERGENCE_CREEPER_SPAWN_EGG);
                entries.accept(CONVERGENCE_SPIDER_SPAWN_EGG);

                // The Resonance Colossus: Altar, Awakening Core & Relics
                entries.accept(net.enchantedwood.block.ModBlocks.RESONANCE_ALTAR);
                entries.accept(CORE_OF_AWAKENING);
                entries.accept(RESONANCE_CLEAVER);
                entries.accept(SINGULARITY_STAFF);
                entries.accept(ETERNAL_BENTO_BOX);
                entries.accept(CORRUPTED_CORE_OF_CATACLYSM);
                entries.accept(PRIMORDIAL_RIFT_KEYSTONE);
                entries.accept(PRIMORDIAL_CATALYST);
                entries.accept(SINGULARITY_HEART);
                entries.accept(ASCENDANT_CLEAVER);
                entries.accept(VOID_SINGULARITY_NEXUS);
                entries.accept(OMEGA_BENTO_BOX);
                entries.accept(RING_OF_GRAVITATIONAL_MASTERY);
                entries.accept(INFINITE_DIMENSIONAL_MATRIX);
                entries.accept(net.enchantedwood.block.ModBlocks.TROPHY_OF_OMNIPOTENCE);

                // Advanced Medical Laboratory Suite
                entries.accept(net.enchantedwood.block.ModBlocks.INDUSTRIAL_CENTRIFUGE);
                entries.accept(net.enchantedwood.block.ModBlocks.CHEMICAL_SYNTHESIZER);
                entries.accept(HYPOSPRAY);
                entries.accept(EMPTY_CARTRIDGE);
                entries.accept(ALKALINE_BASE_EXTRACT);
                entries.accept(CRYO_THERMAL_EXTRACT);
                entries.accept(OXYGENATED_EXTRACT);
                entries.accept(CELLULAR_NANITE_EXTRACT);
                entries.accept(ADRENAL_ESSENCE);
                entries.accept(ACID_NEUTRALIZING_CARTRIDGE);
                entries.accept(HEAT_BUFFER_CARTRIDGE);
                entries.accept(HYPER_OXYGENATION_CARTRIDGE);
                entries.accept(NANITE_TRAUMA_CARTRIDGE);
                entries.accept(ADRENALINE_STIM_CARTRIDGE);

                // Cleanroom Industrial Suite & Bunny Suit
                entries.accept(net.enchantedwood.block.ModBlocks.POLYMER_LOOM);
                entries.accept(STERILE_POLYMER_FABRIC);
                entries.accept(CLEANROOM_HOOD);
                entries.accept(CLEANROOM_SMOCK);
                entries.accept(CLEANROOM_TROUSERS);
                entries.accept(CLEANROOM_BOOTIES);
                entries.accept(net.enchantedwood.block.ModBlocks.CLEANROOM_CASING);
                entries.accept(net.enchantedwood.block.ModBlocks.CLEANROOM_FILTER_CASING);
                entries.accept(net.enchantedwood.block.ModBlocks.CLEANROOM_AIR_SCRUBBER);
                entries.accept(net.enchantedwood.block.ModBlocks.GOWNING_AIRLOCK_DOOR);
                entries.accept(net.enchantedwood.block.ModBlocks.DECONTAMINATION_AIRLOCK_DOOR);
                entries.accept(net.enchantedwood.block.ModBlocks.STERILE_CLEANROOM_LAMP);
                entries.accept(net.enchantedwood.block.ModBlocks.STERILE_MEDICAL_CABINET);

                // Modular Power Suit & Exosuit Modules
                entries.accept(MODULAR_POWER_HELMET);
                entries.accept(MODULAR_POWER_CHESTPLATE);
                entries.accept(MODULAR_POWER_LEGGINGS);
                entries.accept(MODULAR_POWER_BOOTS);
                entries.accept(NANITE_REPAIR_MATRIX);
                entries.accept(HYDROGEN_THRUSTER_MODULE);
                entries.accept(ION_REPULSOR_MODULE);
                entries.accept(NIGHT_VISION_MODULE);
                entries.accept(SPEED_SERVO_MODULE);
                entries.accept(STEP_ASSIST_MODULE);
                entries.accept(HIGH_JUMP_MODULE);
                entries.accept(ACID_PROOF_PLATING);

                // Convergence Cave Ores, Minerals & Superalloys
                entries.accept(net.enchantedwood.block.ModBlocks.FLUORITE_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.DEEPSLATE_FLUORITE_ORE);
                entries.accept(RAW_FLUORITE);
                entries.accept(FLUORITE_CRYSTAL);

                entries.accept(net.enchantedwood.block.ModBlocks.ZIRCONIA_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.DEEPSLATE_ZIRCONIA_ORE);
                entries.accept(RAW_ZIRCONIA);
                entries.accept(ZIRCONIA_NODULE);

                entries.accept(net.enchantedwood.block.ModBlocks.TANTALUM_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.DEEPSLATE_TANTALUM_ORE);
                entries.accept(RAW_TANTALUM);
                entries.accept(TANTALUM_INGOT);
                entries.accept(TANTALUM_DUST);

                entries.accept(net.enchantedwood.block.ModBlocks.HAFNIUM_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.DEEPSLATE_HAFNIUM_ORE);
                entries.accept(RAW_HAFNIUM);
                entries.accept(HAFNIUM_INGOT);
                entries.accept(HAFNIUM_DUST);

                entries.accept(net.enchantedwood.block.ModBlocks.NEODYMIUM_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.DEEPSLATE_NEODYMIUM_ORE);
                entries.accept(RAW_NEODYMIUM);
                entries.accept(NEODYMIUM_MAGNET);
                entries.accept(NEODYMIUM_DUST);

                entries.accept(net.enchantedwood.block.ModBlocks.AEROGEL_ORE);
                entries.accept(net.enchantedwood.block.ModBlocks.DEEPSLATE_AEROGEL_ORE);
                entries.accept(RAW_AEROGEL);
                entries.accept(AEROGEL_SHARD);
                entries.accept(net.enchantedwood.block.ModBlocks.AEROGEL_GLASS);

                entries.accept(TAN_TI_INGOT);
                entries.accept(HAFNIUM_TUNGSTEN_CARBIDE_INGOT);
                entries.accept(NEO_TITANIUM_INGOT);

                // Record Press & Music Discs
                entries.accept(net.enchantedwood.block.ModBlocks.HARMONIC_RECORD_PRESS);
                entries.accept(BLANK_VINYL_DISC);
                entries.accept(MUSIC_DISC_CONVERGENCE);
                entries.accept(MUSIC_DISC_COLOSSUS);
                entries.accept(MUSIC_DISC_OVERDRIVE);
                entries.accept(MUSIC_DISC_CLEANROOM);
                entries.accept(MUSIC_DISC_AUTOCRAFT);
                entries.accept(MUSIC_DISC_HAVEN_BLOOM);
                entries.accept(MUSIC_DISC_CRUCIBLE);
                entries.accept(MUSIC_DISC_STRATOSPHERE);
                entries.accept(MUSIC_DISC_ABYSSAL);
                entries.accept(MUSIC_DISC_ANOXIC);

                // Culinary Expansion: Appliances, Crops & Foods
                entries.accept(net.enchantedwood.block.ModBlocks.BRICK_OVEN);
                entries.accept(net.enchantedwood.block.ModBlocks.ICE_CREAM_MACHINE);
                entries.accept(TOMATO_SEEDS);
                entries.accept(TOMATO);
                entries.accept(ONION_SEEDS);
                entries.accept(ONION);
                entries.accept(LETTUCE_SEEDS);
                entries.accept(LETTUCE);
                entries.accept(SOYBEAN_SEEDS);
                entries.accept(SOYBEANS);
                entries.accept(CHILI_PEPPER_SEEDS);
                entries.accept(CHILI_PEPPER);
                entries.accept(STRAWBERRY);
                entries.accept(BLUEBERRY);
                entries.accept(net.enchantedwood.block.ModBlocks.STRAWBERRY_BUSH);
                entries.accept(net.enchantedwood.block.ModBlocks.BLUEBERRY_BUSH);

                entries.accept(SALT);
                entries.accept(WHEAT_FLOUR);
                entries.accept(PIZZA_DOUGH);
                entries.accept(BURGER_BUN);
                entries.accept(TACO_SHELL);
                entries.accept(TOMATO_SAUCE);
                entries.accept(SOY_MILK);
                entries.accept(CHEESE_SLICE);
                entries.accept(TOFU);
                entries.accept(RAW_BURGER_PATTY);
                entries.accept(COOKED_BURGER_PATTY);
                entries.accept(PREPARED_ANCHOVIES);

                entries.accept(RAW_MARGHERITA_PIZZA);
                entries.accept(MARGHERITA_PIZZA);
                entries.accept(RAW_MEAT_LOVERS_PIZZA);
                entries.accept(MEAT_LOVERS_PIZZA);
                entries.accept(RAW_ANCHOVY_ONION_PIZZA);
                entries.accept(ANCHOVY_ONION_PIZZA);
                entries.accept(RAW_SUPREME_PIZZA);
                entries.accept(SUPREME_PIZZA);

                entries.accept(CLASSIC_CHEESEBURGER);
                entries.accept(DELUXE_BACON_BURGER);
                entries.accept(BEEF_TACO);
                entries.accept(FISH_TACO);

                entries.accept(GARDEN_SALAD);
                entries.accept(BERRY_MEDLEY_SALAD);

                entries.accept(VANILLA_ICE_CREAM);
                entries.accept(STRAWBERRY_ICE_CREAM);
                entries.accept(BLUEBERRY_ICE_CREAM);
                entries.accept(CHOCOLATE_ICE_CREAM);
                entries.accept(SWEET_BERRY_ICE_CREAM);
                entries.accept(ICE_CUBES);
            })
            .build();


    private static <T extends Item> T registerItem(String name, Function<Item.Properties, T> itemFactory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, name));
        T item = itemFactory.apply(new Item.Properties().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void registerModItems() {
        EnchantedWoodMod.LOGGER.info("Registering Enchanted Wood Items for " + EnchantedWoodMod.MOD_ID);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ENCHANTED_WOOD_GROUP_KEY, ENCHANTED_WOOD_GROUP);
    }
}


package net.enchantedwood.compat.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.WidgetHolder;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.item.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class EnchantedWoodEmiPlugin implements EmiPlugin {

    // GUI Textures for authentic machine screen integration
    public static final Identifier CRUSHER_GUI = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/crusher_gui.png");
    public static final Identifier ALLOY_FOUNDRY_GUI = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/alloy_foundry_gui.png");
    public static final Identifier CENTRIFUGE_GUI = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/industrial_centrifuge_gui.png");
    public static final Identifier COKE_OVEN_GUI = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/coke_oven_gui.png");
    public static final Identifier CHEMICAL_SYNTHESIZER_GUI = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/chemical_synthesizer_gui.png");
    public static final Identifier POLYMER_LOOM_GUI = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/polymer_loom_gui.png");
    public static final Identifier CIRCUIT_FABRICATOR_GUI = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/circuit_fabricator_gui.png");
    public static final Identifier INDUCTION_SMELTER_GUI = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/induction_smelter_gui.png");
    public static final Identifier CASTING_PORT_GUI = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/casting_port_gui.png");
    public static final Identifier HYDRAULIC_PRESS_GUI = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/hydraulic_press_gui.png");

    // Categories
    public static final EmiRecipeCategory CIRCUIT_FABRICATOR = new EmiRecipeCategory(
            Identifier.of(EnchantedWoodMod.MOD_ID, "circuit_fabricator"),
            EmiStack.of(ModBlocks.CIRCUIT_FABRICATOR)
    );

    public static final EmiRecipeCategory INDUCTION_SMELTER = new EmiRecipeCategory(
            Identifier.of(EnchantedWoodMod.MOD_ID, "induction_smelter"),
            EmiStack.of(ModBlocks.INDUCTION_SMELTER)
    );

    public static final EmiRecipeCategory CASTING_PORT = new EmiRecipeCategory(
            Identifier.of(EnchantedWoodMod.MOD_ID, "casting_port"),
            EmiStack.of(ModBlocks.CASTING_PORT)
    );

    public static final EmiRecipeCategory ALLOY_FOUNDRY = new EmiRecipeCategory(
            Identifier.of(EnchantedWoodMod.MOD_ID, "alloy_foundry"),
            EmiStack.of(ModBlocks.ALLOY_FOUNDRY)
    );

    public static final EmiRecipeCategory CRUSHER = new EmiRecipeCategory(
            Identifier.of(EnchantedWoodMod.MOD_ID, "crusher"),
            EmiStack.of(ModBlocks.CRUSHER)
    );

    public static final EmiRecipeCategory DUST_SMELTER = new EmiRecipeCategory(
            Identifier.of(EnchantedWoodMod.MOD_ID, "dust_smelter"),
            EmiStack.of(ModBlocks.DUST_SMELTER)
    );

    public static final EmiRecipeCategory STEEL_BLAST_FURNACE = new EmiRecipeCategory(
            Identifier.of(EnchantedWoodMod.MOD_ID, "steel_blast_furnace"),
            EmiStack.of(ModBlocks.STEEL_BLAST_FURNACE)
    );

    public static final EmiRecipeCategory SOIL_INFUSER = new EmiRecipeCategory(
            Identifier.of(EnchantedWoodMod.MOD_ID, "soil_infuser"),
            EmiStack.of(ModBlocks.SOIL_INFUSER)
    );

    public static final EmiRecipeCategory COKE_OVEN = new EmiRecipeCategory(
            Identifier.of(EnchantedWoodMod.MOD_ID, "coke_oven"),
            EmiStack.of(ModBlocks.COKE_OVEN)
    );

    public static final EmiRecipeCategory FUEL_REFINERY = new EmiRecipeCategory(
            Identifier.of(EnchantedWoodMod.MOD_ID, "fuel_refinery"),
            EmiStack.of(ModBlocks.FUEL_REFINERY)
    );

    public static final EmiRecipeCategory MAGMA_CRUCIBLE = new EmiRecipeCategory(
            Identifier.of(EnchantedWoodMod.MOD_ID, "magma_crucible"),
            EmiStack.of(ModBlocks.MAGMA_CRUCIBLE)
    );

    public static final EmiRecipeCategory HYDRAULIC_PRESS = new EmiRecipeCategory(
            Identifier.of(EnchantedWoodMod.MOD_ID, "hydraulic_press"),
            EmiStack.of(ModBlocks.HYDRAULIC_PRESS)
    );

    public static final EmiRecipeCategory INDUSTRIAL_CENTRIFUGE = new EmiRecipeCategory(
            Identifier.of(EnchantedWoodMod.MOD_ID, "industrial_centrifuge"),
            EmiStack.of(ModBlocks.INDUSTRIAL_CENTRIFUGE)
    );

    public static final EmiRecipeCategory CHEMICAL_SYNTHESIZER = new EmiRecipeCategory(
            Identifier.of(EnchantedWoodMod.MOD_ID, "chemical_synthesizer"),
            EmiStack.of(ModBlocks.CHEMICAL_SYNTHESIZER)
    );

    public static final EmiRecipeCategory POLYMER_LOOM = new EmiRecipeCategory(
            Identifier.of(EnchantedWoodMod.MOD_ID, "polymer_loom"),
            EmiStack.of(ModBlocks.POLYMER_LOOM)
    );

    @Override
    public void register(EmiRegistry registry) {
        // Register Categories
        registry.addCategory(ALLOY_FOUNDRY);
        registry.addCategory(CRUSHER);
        registry.addCategory(DUST_SMELTER);
        registry.addCategory(STEEL_BLAST_FURNACE);
        registry.addCategory(SOIL_INFUSER);
        registry.addCategory(COKE_OVEN);
        registry.addCategory(FUEL_REFINERY);
        registry.addCategory(MAGMA_CRUCIBLE);
        registry.addCategory(HYDRAULIC_PRESS);
        registry.addCategory(INDUSTRIAL_CENTRIFUGE);
        registry.addCategory(CHEMICAL_SYNTHESIZER);
        registry.addCategory(POLYMER_LOOM);
        registry.addCategory(CIRCUIT_FABRICATOR);
        registry.addCategory(INDUCTION_SMELTER);
        registry.addCategory(CASTING_PORT);

        // Register Workstations (Catalysts)
        registry.addWorkstation(CIRCUIT_FABRICATOR, EmiStack.of(ModBlocks.CIRCUIT_FABRICATOR));
        registry.addWorkstation(INDUCTION_SMELTER, EmiStack.of(ModBlocks.INDUCTION_SMELTER));
        registry.addWorkstation(CASTING_PORT, EmiStack.of(ModBlocks.CASTING_PORT));
        registry.addWorkstation(ALLOY_FOUNDRY, EmiStack.of(ModBlocks.ALLOY_FOUNDRY));
        registry.addWorkstation(CRUSHER, EmiStack.of(ModBlocks.CRUSHER));
        registry.addWorkstation(CRUSHER, EmiStack.of(ModBlocks.CRUSHER_MK2));
        registry.addWorkstation(DUST_SMELTER, EmiStack.of(ModBlocks.DUST_SMELTER));
        registry.addWorkstation(DUST_SMELTER, EmiStack.of(ModBlocks.DUST_SMELTER_MK2));
        registry.addWorkstation(STEEL_BLAST_FURNACE, EmiStack.of(ModBlocks.STEEL_BLAST_FURNACE));
        registry.addWorkstation(SOIL_INFUSER, EmiStack.of(ModBlocks.SOIL_INFUSER));
        registry.addWorkstation(COKE_OVEN, EmiStack.of(ModBlocks.COKE_OVEN));
        registry.addWorkstation(FUEL_REFINERY, EmiStack.of(ModBlocks.FUEL_REFINERY));
        registry.addWorkstation(MAGMA_CRUCIBLE, EmiStack.of(ModBlocks.MAGMA_CRUCIBLE));
        registry.addWorkstation(HYDRAULIC_PRESS, EmiStack.of(ModBlocks.HYDRAULIC_PRESS));
        registry.addWorkstation(INDUSTRIAL_CENTRIFUGE, EmiStack.of(ModBlocks.INDUSTRIAL_CENTRIFUGE));
        registry.addWorkstation(CHEMICAL_SYNTHESIZER, EmiStack.of(ModBlocks.CHEMICAL_SYNTHESIZER));
        registry.addWorkstation(POLYMER_LOOM, EmiStack.of(ModBlocks.POLYMER_LOOM));
        registry.addWorkstation(dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.CRAFTING, EmiStack.of(ModBlocks.SUPER_COMPUTER));
        registry.addWorkstation(CIRCUIT_FABRICATOR, EmiStack.of(ModBlocks.SUPER_COMPUTER));
        registry.addWorkstation(HYDRAULIC_PRESS, EmiStack.of(ModBlocks.SUPER_COMPUTER));
        registry.addWorkstation(dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.SMELTING, EmiStack.of(ModBlocks.SUPER_COMPUTER));

        // Exclusion area for Induction Smelter Mixing Bay side panel
        registry.addExclusionArea(net.enchantedwood.screen.InductionSmelterScreen.class, (screen, consumer) -> {
            if (screen.getScreenHandler().hasChip()) {
                int x = (screen.width - 176) / 2 + 176;
                int y = (screen.height - 166) / 2 + 16;
                consumer.accept(new Bounds(x, y, 66, 126));
            }
        });

        // Recipe auto-transfer handler for Super Computer 3x3 Ghost Pattern Matrix
        registry.addRecipeHandler(net.enchantedwood.screen.ModScreenHandlers.SUPER_COMPUTER_SCREEN_HANDLER, new dev.emi.emi.api.recipe.handler.EmiRecipeHandler<net.enchantedwood.screen.SuperComputerScreenHandler>() {
            @Override
            public dev.emi.emi.api.recipe.EmiPlayerInventory getInventory(net.minecraft.client.gui.screen.ingame.HandledScreen<net.enchantedwood.screen.SuperComputerScreenHandler> screen) {
                List<EmiStack> stacks = new ArrayList<>();
                for (int i = 15; i < screen.getScreenHandler().slots.size(); i++) {
                    ItemStack s = screen.getScreenHandler().getSlot(i).getStack();
                    if (!s.isEmpty()) {
                        stacks.add(EmiStack.of(s));
                    }
                }
                return new dev.emi.emi.api.recipe.EmiPlayerInventory(stacks);
            }

            @Override
            public boolean supportsRecipe(EmiRecipe recipe) {
                return recipe.getCategory() == dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.CRAFTING
                        || recipe.getCategory() == CIRCUIT_FABRICATOR
                        || recipe.getCategory() == HYDRAULIC_PRESS
                        || recipe.getCategory() == dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.SMELTING;
            }

            @Override
            public boolean canCraft(EmiRecipe recipe, dev.emi.emi.api.recipe.handler.EmiCraftContext<net.enchantedwood.screen.SuperComputerScreenHandler> context) {
                return true; // Always allow transferring into the ghost blueprint matrix!
            }

            @Override
            public boolean craft(EmiRecipe recipe, dev.emi.emi.api.recipe.handler.EmiCraftContext<net.enchantedwood.screen.SuperComputerScreenHandler> context) {
                List<ItemStack> pattern = new ArrayList<>(9);
                List<EmiIngredient> inputs = recipe.getInputs();
                for (int i = 0; i < 9; i++) {
                    if (i < inputs.size() && !inputs.get(i).isEmpty()) {
                        List<EmiStack> stacks = inputs.get(i).getEmiStacks();
                        if (!stacks.isEmpty()) {
                            pattern.add(stacks.get(0).getItemStack());
                        } else {
                            pattern.add(ItemStack.EMPTY);
                        }
                    } else {
                        pattern.add(ItemStack.EMPTY);
                    }
                }

                // Send ghost pattern packet to server to populate 3x3 matrix
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new net.enchantedwood.network.SetSuperComputerRecipePayload(pattern));
                return true;
            }
        });

        // Recipe auto-transfer handler for physical Circuit Fabricator GUI
        registry.addRecipeHandler(net.enchantedwood.screen.ModScreenHandlers.CIRCUIT_FABRICATOR_SCREEN_HANDLER, new dev.emi.emi.api.recipe.handler.StandardRecipeHandler<net.enchantedwood.screen.CircuitFabricatorScreenHandler>() {
            @Override
            public List<net.minecraft.screen.slot.Slot> getInputSources(net.enchantedwood.screen.CircuitFabricatorScreenHandler handler) {
                List<net.minecraft.screen.slot.Slot> list = new ArrayList<>();
                for (int i = 6; i < handler.slots.size(); i++) {
                    list.add(handler.getSlot(i));
                }
                return list;
            }

            @Override
            public List<net.minecraft.screen.slot.Slot> getCraftingSlots(net.enchantedwood.screen.CircuitFabricatorScreenHandler handler) {
                return List.of(handler.getSlot(0), handler.getSlot(1), handler.getSlot(2), handler.getSlot(3));
            }

            @Override
            public @org.jetbrains.annotations.Nullable net.minecraft.screen.slot.Slot getOutputSlot(net.enchantedwood.screen.CircuitFabricatorScreenHandler handler) {
                return handler.getSlot(4);
            }

            @Override
            public boolean supportsRecipe(EmiRecipe recipe) {
                return recipe.getCategory() == CIRCUIT_FABRICATOR;
            }
        });

        // Recipe auto-transfer handler for physical Hydraulic Press GUI
        registry.addRecipeHandler(net.enchantedwood.screen.ModScreenHandlers.HYDRAULIC_PRESS_SCREEN_HANDLER, new dev.emi.emi.api.recipe.handler.StandardRecipeHandler<net.enchantedwood.screen.HydraulicPressScreenHandler>() {
            @Override
            public List<net.minecraft.screen.slot.Slot> getInputSources(net.enchantedwood.screen.HydraulicPressScreenHandler handler) {
                List<net.minecraft.screen.slot.Slot> list = new ArrayList<>();
                for (int i = 3; i < handler.slots.size(); i++) {
                    list.add(handler.getSlot(i));
                }
                return list;
            }

            @Override
            public List<net.minecraft.screen.slot.Slot> getCraftingSlots(net.enchantedwood.screen.HydraulicPressScreenHandler handler) {
                return List.of(handler.getSlot(0));
            }

            @Override
            public @org.jetbrains.annotations.Nullable net.minecraft.screen.slot.Slot getOutputSlot(net.enchantedwood.screen.HydraulicPressScreenHandler handler) {
                return handler.getSlot(2);
            }

            @Override
            public boolean supportsRecipe(EmiRecipe recipe) {
                return recipe.getCategory() == HYDRAULIC_PRESS;
            }
        });

        // 1. Alloy Foundry Recipes
        registerAlloyRecipes(registry);

        // 2. Crusher Recipes
        registerCrusherRecipes(registry);

        // 3. Dust Smelter Recipes
        registerDustSmelterRecipes(registry);

        // 4. Steel Blast Furnace Recipes
        registerBlastFurnaceRecipes(registry);

        // 5. Soil Infuser Recipes
        registerSoilInfuserRecipes(registry);

        // 6. Coke Oven Recipes
        registerCokeOvenRecipes(registry);

        // 7. Fuel Refinery Recipes
        registerRefineryRecipes(registry);

        // 8. Magma Crucible Recipes
        registerCrucibleRecipes(registry);

        // 9. Hydraulic Plate Press Recipes
        registerHydraulicPressRecipes(registry);

        // 10. Industrial Centrifuge Recipes
        registerCentrifugeRecipes(registry);

        // 11. Chemical Synthesizer Recipes
        registerSynthesizerRecipes(registry);

        // 12. Circuit Fabricator Recipes
        registerCircuitFabricatorRecipes(registry);

        // 13. Induction Smelter Recipes
        registerInductionSmelterRecipes(registry);

        // 14. Casting Port Recipes
        registerCastingPortRecipes(registry);
    }

    private static void registerHydraulicPressRecipes(EmiRegistry registry) {
        int idx = 0;
        addPress(registry, idx++, EmiStack.of(ModItems.TUNGSTEN_INGOT), EmiStack.of(ModItems.TUNGSTEN_PLATE), "Refractory Plate");
        addPress(registry, idx++, EmiStack.of(ModItems.COBALT_INGOT), EmiStack.of(ModItems.COBALT_PLATE), "Agile Cobalt Plate");
        addPress(registry, idx++, EmiStack.of(ModItems.ARDITE_INGOT), EmiStack.of(ModItems.ARDITE_PLATE), "Dense Ardite Plate");
        addPress(registry, idx++, EmiStack.of(ModItems.MANYULLYN_INGOT), EmiStack.of(ModItems.MANYULLYN_PLATE), "Nether Alloy Plate");
        addPress(registry, idx++, EmiStack.of(ModBlocks.TUNGSTEN_BLOCK), EmiStack.of(ModItems.TUNGSTEN_PLATE, 9), "Bulk Stamping");
        addPress(registry, idx++, EmiStack.of(ModBlocks.COBALT_BLOCK), EmiStack.of(ModItems.COBALT_PLATE, 9), "Bulk Stamping");
        addPress(registry, idx++, EmiStack.of(ModBlocks.ARDITE_BLOCK), EmiStack.of(ModItems.ARDITE_PLATE, 9), "Bulk Stamping");
        addPress(registry, idx++, EmiStack.of(ModBlocks.MANYULLYN_BLOCK), EmiStack.of(ModItems.MANYULLYN_PLATE, 9), "Bulk Stamping");
        addPress(registry, idx++, EmiStack.of(ModItems.SILICON), EmiStack.of(ModItems.SILICON_WAFER, 2), "Semiconductor Wafer Slicing");
    }

    private static void addPress(EmiRegistry registry, int id, EmiStack input, EmiStack output, String note) {
        registry.addRecipe(new SimpleOneInputRecipe(HYDRAULIC_PRESS, Identifier.of(EnchantedWoodMod.MOD_ID, "press_" + id),
                EmiIngredient.of(List.of(input)), output, note));
    }

    private static void registerAlloyRecipes(EmiRegistry registry) {
        int idx = 0;
        // Cobalt + Ardite -> 2x Manyullyn
        registry.addRecipe(new SimpleTwoInputRecipe(ALLOY_FOUNDRY, Identifier.of(EnchantedWoodMod.MOD_ID, "alloy_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.COBALT_INGOT), EmiStack.of(ModItems.COBALT_DUST))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.ARDITE_INGOT), EmiStack.of(ModItems.ARDITE_DUST))),
                EmiStack.of(ModItems.MANYULLYN_INGOT, 2), "Nether Master Alloy"));

        // Iron + Basalt Flux Catalyst -> 2x Steel
        registry.addRecipe(new SimpleTwoInputRecipe(ALLOY_FOUNDRY, Identifier.of(EnchantedWoodMod.MOD_ID, "alloy_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.IRON_INGOT), EmiStack.of(ModItems.IRON_DUST))),
                EmiStack.of(ModItems.BASALT_FLUX_CATALYST),
                EmiStack.of(ModItems.STEEL_INGOT, 2), "Catalytic Steel Fluxing"));

        // Tungsten + Coal Dust -> Tungsten Carbide
        registry.addRecipe(new SimpleTwoInputRecipe(ALLOY_FOUNDRY, Identifier.of(EnchantedWoodMod.MOD_ID, "alloy_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.TUNGSTEN_INGOT), EmiStack.of(ModItems.TUNGSTEN_DUST))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.COAL_DUST), EmiStack.of(ModItems.COKE_COAL))),
                EmiStack.of(ModItems.TUNGSTEN_CARBIDE_INGOT), "Refractory Sintering"));

        // Copper + Tin -> 2x Bronze
        registry.addRecipe(new SimpleTwoInputRecipe(ALLOY_FOUNDRY, Identifier.of(EnchantedWoodMod.MOD_ID, "alloy_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.COPPER_INGOT), EmiStack.of(ModItems.COPPER_DUST))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.TIN_INGOT), EmiStack.of(ModItems.TIN_DUST))),
                EmiStack.of(ModItems.BRONZE_INGOT, 2), "Classical Bronze Smelting"));

        // Gold + Enchanted Dust -> 2x Enchanted Gold
        registry.addRecipe(new SimpleTwoInputRecipe(ALLOY_FOUNDRY, Identifier.of(EnchantedWoodMod.MOD_ID, "alloy_" + idx++),
                EmiStack.of(Items.GOLD_INGOT),
                EmiStack.of(ModItems.ENCHANTED_DUST),
                EmiStack.of(ModItems.ENCHANTED_EMERALD), "Arcane Transmutation"));

        // Obsidian + Titanium -> 2x Reinforced Obsidian
        registry.addRecipe(new SimpleTwoInputRecipe(ALLOY_FOUNDRY, Identifier.of(EnchantedWoodMod.MOD_ID, "alloy_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.OBSIDIAN), EmiStack.of(Items.CRYING_OBSIDIAN))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.TITANIUM_INGOT), EmiStack.of(ModItems.TITANIUM_DUST))),
                EmiStack.of(ModBlocks.REINFORCED_OBSIDIAN, 2), "Blast Containment Shell"));

        // Glass + Fire Crystal -> 2x Volcanic Glass
        registry.addRecipe(new SimpleTwoInputRecipe(ALLOY_FOUNDRY, Identifier.of(EnchantedWoodMod.MOD_ID, "alloy_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.GLASS), EmiStack.of(Items.GLASS_PANE))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.FIRE_CRYSTAL), EmiStack.of(ModItems.VOLCANIC_ASH), EmiStack.of(ModItems.SULFUR_DUST))),
                EmiStack.of(ModBlocks.VOLCANIC_GLASS, 2), "High-Temperature Silicate"));

        // Convergence Superalloys & Advanced Composites
        registry.addRecipe(new SimpleTwoInputRecipe(ALLOY_FOUNDRY, Identifier.of(EnchantedWoodMod.MOD_ID, "alloy_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.TANTALUM_INGOT), EmiStack.of(ModItems.TANTALUM_DUST), EmiStack.of(ModItems.RAW_TANTALUM))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.TITANIUM_INGOT), EmiStack.of(ModItems.TITANIUM_DUST))),
                EmiStack.of(ModItems.TAN_TI_INGOT, 2), "Corrosion-Proof Superalloy"));

        registry.addRecipe(new SimpleTwoInputRecipe(ALLOY_FOUNDRY, Identifier.of(EnchantedWoodMod.MOD_ID, "alloy_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.HAFNIUM_INGOT), EmiStack.of(ModItems.HAFNIUM_DUST), EmiStack.of(ModItems.RAW_HAFNIUM))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.TUNGSTEN_INGOT), EmiStack.of(ModItems.TUNGSTEN_DUST))),
                EmiStack.of(ModItems.HAFNIUM_TUNGSTEN_CARBIDE_INGOT, 2), "Refractory Thermal Carbide"));

        registry.addRecipe(new SimpleTwoInputRecipe(ALLOY_FOUNDRY, Identifier.of(EnchantedWoodMod.MOD_ID, "alloy_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.NEODYMIUM_MAGNET), EmiStack.of(ModItems.NEODYMIUM_DUST), EmiStack.of(ModItems.RAW_NEODYMIUM))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.TITANIUM_INGOT), EmiStack.of(ModItems.TITANIUM_DUST))),
                EmiStack.of(ModItems.NEO_TITANIUM_INGOT, 2), "High-Elasticity Spring-Steel"));

        registry.addRecipe(new SimpleTwoInputRecipe(ALLOY_FOUNDRY, Identifier.of(EnchantedWoodMod.MOD_ID, "alloy_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.AEROGEL_SHARD), EmiStack.of(ModItems.RAW_AEROGEL))),
                EmiIngredient.of(List.of(EmiStack.of(Items.GLASS), EmiStack.of(ModBlocks.VOLCANIC_GLASS))),
                EmiStack.of(ModBlocks.AEROGEL_GLASS, 2), "Thermal Vacuum Insulation"));
    }

    private static void registerCrusherRecipes(EmiRegistry registry) {
        int idx = 0;
        addCrush(registry, idx++, List.of(EmiStack.of(Items.RAW_IRON), EmiStack.of(Items.IRON_ORE), EmiStack.of(Items.DEEPSLATE_IRON_ORE), EmiStack.of(ModBlocks.NETHER_IRON_ORE)), EmiStack.of(ModItems.IRON_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.RAW_COPPER), EmiStack.of(Items.COPPER_ORE), EmiStack.of(Items.DEEPSLATE_COPPER_ORE), EmiStack.of(ModBlocks.NETHER_COPPER_ORE)), EmiStack.of(ModItems.COPPER_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.RAW_GOLD), EmiStack.of(Items.GOLD_ORE), EmiStack.of(Items.DEEPSLATE_GOLD_ORE), EmiStack.of(Items.NETHER_GOLD_ORE)), EmiStack.of(ModItems.GOLD_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.RAW_BAUXITE), EmiStack.of(ModBlocks.BAUXITE_ORE), EmiStack.of(ModBlocks.DEEPSLATE_BAUXITE_ORE)), EmiStack.of(ModItems.BAUXITE_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.RAW_TIN), EmiStack.of(ModBlocks.TIN_ORE), EmiStack.of(ModBlocks.DEEPSLATE_TIN_ORE), EmiStack.of(ModBlocks.NETHER_TIN_ORE)), EmiStack.of(ModItems.TIN_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.RAW_TITANIUM), EmiStack.of(ModBlocks.TITANIUM_ORE), EmiStack.of(ModBlocks.DEEPSLATE_TITANIUM_ORE)), EmiStack.of(ModItems.TITANIUM_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.RAW_COBALT), EmiStack.of(ModBlocks.COBALT_ORE)), EmiStack.of(ModItems.COBALT_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.RAW_ARDITE), EmiStack.of(ModBlocks.ARDITE_ORE)), EmiStack.of(ModItems.ARDITE_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.RAW_TUNGSTEN), EmiStack.of(ModBlocks.NETHER_TUNGSTEN_ORE), EmiStack.of(ModBlocks.DEEPSLATE_TUNGSTEN_ORE)), EmiStack.of(ModItems.TUNGSTEN_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.RAW_TANTALUM), EmiStack.of(ModBlocks.TANTALUM_ORE), EmiStack.of(ModBlocks.DEEPSLATE_TANTALUM_ORE)), EmiStack.of(ModItems.TANTALUM_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.RAW_HAFNIUM), EmiStack.of(ModBlocks.HAFNIUM_ORE), EmiStack.of(ModBlocks.DEEPSLATE_HAFNIUM_ORE)), EmiStack.of(ModItems.HAFNIUM_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.RAW_NEODYMIUM), EmiStack.of(ModBlocks.NEODYMIUM_ORE), EmiStack.of(ModBlocks.DEEPSLATE_NEODYMIUM_ORE)), EmiStack.of(ModItems.NEODYMIUM_DUST, 2), "Up to 8× with Blaze Core");

        addCrush(registry, idx++, List.of(EmiStack.of(Items.IRON_INGOT)), EmiStack.of(ModItems.IRON_DUST, 1), "Ingot Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.COPPER_INGOT)), EmiStack.of(ModItems.COPPER_DUST, 1), "Ingot Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.GOLD_INGOT)), EmiStack.of(ModItems.GOLD_DUST, 1), "Ingot Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.ALUMINUM_INGOT)), EmiStack.of(ModItems.BAUXITE_DUST, 1), "Ingot Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.TIN_INGOT)), EmiStack.of(ModItems.TIN_DUST, 1), "Ingot Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.TITANIUM_INGOT)), EmiStack.of(ModItems.TITANIUM_DUST, 1), "Ingot Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.COBALT_INGOT)), EmiStack.of(ModItems.COBALT_DUST, 1), "Ingot Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.ARDITE_INGOT)), EmiStack.of(ModItems.ARDITE_DUST, 1), "Ingot Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.MANYULLYN_INGOT)), EmiStack.of(ModItems.MANYULLYN_DUST, 1), "Ingot Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.STEEL_INGOT)), EmiStack.of(ModItems.STEEL_DUST, 1), "Ingot Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.BRONZE_INGOT)), EmiStack.of(ModItems.BRONZE_DUST, 1), "Ingot Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.TUNGSTEN_INGOT)), EmiStack.of(ModItems.TUNGSTEN_DUST, 1), "Ingot Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.TANTALUM_INGOT)), EmiStack.of(ModItems.TANTALUM_DUST, 1), "Ingot Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.HAFNIUM_INGOT)), EmiStack.of(ModItems.HAFNIUM_DUST, 1), "Ingot Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.NEODYMIUM_MAGNET)), EmiStack.of(ModItems.NEODYMIUM_DUST, 1), "Magnet Pulverization");

        addCrush(registry, idx++, List.of(EmiStack.of(Items.COAL), EmiStack.of(Items.CHARCOAL), EmiStack.of(ModItems.COKE_COAL)), EmiStack.of(ModItems.COAL_DUST, 2), "Carbon Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.WHEAT)), EmiStack.of(Items.BREAD, 1), "Flour & Bread Milling");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.SUGAR_CANE)), EmiStack.of(Items.SUGAR, 2), "Sugar Milling");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.BONE)), EmiStack.of(Items.BONE_MEAL, 4), "Bone Crushing");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.BLAZE_ROD)), EmiStack.of(Items.BLAZE_POWDER, 4), "Blaze Crushing (+Sulfur in MK2)");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.MAGMA_BLOCK)), EmiStack.of(Items.MAGMA_CREAM, 4), "Magma Crushing (40% Sulfur / 40% Blaze in MK2)");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.COBBLESTONE), EmiStack.of(Items.STONE)), EmiStack.of(Items.GRAVEL, 1), "Milling");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.GRAVEL)), EmiStack.of(Items.SAND, 1), "Milling");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.BASALT), EmiStack.of(Items.SMOOTH_BASALT)), EmiStack.of(ModItems.VOLCANIC_ASH, 2), "Ash Extraction");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.QUARTZ), EmiStack.of(Items.QUARTZ_BLOCK)), EmiStack.of(ModItems.QUARTZ_DUST, 2), "Quartz Pulverization (+Redstone in MK2)");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.ANCIENT_DEBRIS), EmiStack.of(Items.NETHERITE_SCRAP), EmiStack.of(Items.NETHERITE_INGOT)), EmiStack.of(ModItems.NETHERITE_DUST, 2), "Ancient Debris Pulverization (+Gold in MK2)");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.DIAMOND_ORE), EmiStack.of(Items.DEEPSLATE_DIAMOND_ORE)), EmiStack.of(ModItems.DIAMOND_DUST, 2), "Up to 8× with Blaze Core (+Emerald in MK2)");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.DIAMOND)), EmiStack.of(ModItems.DIAMOND_DUST, 1), "Diamond Gem Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.DIAMOND_BLOCK)), EmiStack.of(ModItems.DIAMOND_DUST, 9), "Bulk Diamond Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.EMERALD_ORE), EmiStack.of(Items.DEEPSLATE_EMERALD_ORE)), EmiStack.of(ModItems.EMERALD_DUST, 2), "Up to 8× with Blaze Core (+Diamond in MK2)");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.EMERALD)), EmiStack.of(ModItems.EMERALD_DUST, 1), "Emerald Gem Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.EMERALD_BLOCK)), EmiStack.of(ModItems.EMERALD_DUST, 9), "Bulk Emerald Pulverization");
    }

    private static void addCrush(EmiRegistry registry, int id, List<EmiStack> inputs, EmiStack output, String note) {
        registry.addRecipe(new SimpleOneInputRecipe(CRUSHER, Identifier.of(EnchantedWoodMod.MOD_ID, "crush_" + id),
                EmiIngredient.of(inputs), output, note));
    }

    private static void registerDustSmelterRecipes(EmiRegistry registry) {
        int idx = 0;
        addSmelt(registry, idx++, EmiStack.of(ModItems.IRON_DUST), EmiStack.of(Items.IRON_INGOT));
        addSmelt(registry, idx++, EmiStack.of(ModItems.COPPER_DUST), EmiStack.of(Items.COPPER_INGOT));
        addSmelt(registry, idx++, EmiStack.of(ModItems.GOLD_DUST), EmiStack.of(Items.GOLD_INGOT));
        addSmelt(registry, idx++, EmiStack.of(ModItems.BAUXITE_DUST), EmiStack.of(ModItems.ALUMINUM_INGOT));
        addSmelt(registry, idx++, EmiStack.of(ModItems.TIN_DUST), EmiStack.of(ModItems.TIN_INGOT));
        addSmelt(registry, idx++, EmiStack.of(ModItems.TITANIUM_DUST), EmiStack.of(ModItems.TITANIUM_INGOT));
        addSmelt(registry, idx++, EmiStack.of(ModItems.COBALT_DUST), EmiStack.of(ModItems.COBALT_INGOT));
        addSmelt(registry, idx++, EmiStack.of(ModItems.ARDITE_DUST), EmiStack.of(ModItems.ARDITE_INGOT));
        addSmelt(registry, idx++, EmiStack.of(ModItems.MANYULLYN_DUST), EmiStack.of(ModItems.MANYULLYN_INGOT));
        addSmelt(registry, idx++, EmiStack.of(ModItems.STEEL_DUST), EmiStack.of(ModItems.STEEL_INGOT));
        addSmelt(registry, idx++, EmiStack.of(ModItems.BRONZE_DUST), EmiStack.of(ModItems.BRONZE_INGOT));
        addSmelt(registry, idx++, EmiStack.of(ModItems.TUNGSTEN_DUST), EmiStack.of(ModItems.TUNGSTEN_INGOT));
        addSmelt(registry, idx++, EmiStack.of(ModItems.TANTALUM_DUST), EmiStack.of(ModItems.TANTALUM_INGOT));
        addSmelt(registry, idx++, EmiStack.of(ModItems.HAFNIUM_DUST), EmiStack.of(ModItems.HAFNIUM_INGOT));
        addSmelt(registry, idx++, EmiStack.of(ModItems.NEODYMIUM_DUST), EmiStack.of(ModItems.NEODYMIUM_MAGNET));
        addSmelt(registry, idx++, EmiStack.of(ModItems.NETHERITE_DUST), EmiStack.of(Items.NETHERITE_INGOT));
        addSmelt(registry, idx++, EmiStack.of(ModItems.QUARTZ_DUST), EmiStack.of(ModItems.SILICON));
        addSmelt(registry, idx++, EmiStack.of(Items.SAND), EmiStack.of(Items.GLASS));
        addSmelt(registry, idx++, EmiStack.of(Items.RED_SAND), EmiStack.of(Items.GLASS));
        addSmelt(registry, idx++, EmiStack.of(Items.CLAY_BALL), EmiStack.of(Items.BRICK));
    }

    private static void addSmelt(EmiRegistry registry, int id, EmiStack input, EmiStack output) {
        registry.addRecipe(new SimpleOneInputRecipe(DUST_SMELTER, Identifier.of(EnchantedWoodMod.MOD_ID, "smelt_" + id),
                input, output, "Induction Smelting"));
    }

    private static void registerBlastFurnaceRecipes(EmiRegistry registry) {
        int idx = 0;
        // Coke Coal mode
        registry.addRecipe(new SimpleTwoInputRecipe(STEEL_BLAST_FURNACE, Identifier.of(EnchantedWoodMod.MOD_ID, "blast_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.IRON_INGOT), EmiStack.of(ModItems.IRON_DUST))),
                EmiStack.of(ModItems.COKE_COAL),
                EmiStack.of(ModItems.STEEL_INGOT), "Traditional Coke Smelting"));

        // Basalt Flux Catalyst mode
        registry.addRecipe(new SimpleTwoInputRecipe(STEEL_BLAST_FURNACE, Identifier.of(EnchantedWoodMod.MOD_ID, "blast_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.IRON_INGOT), EmiStack.of(ModItems.IRON_DUST))),
                EmiStack.of(ModItems.BASALT_FLUX_CATALYST),
                EmiStack.of(ModItems.STEEL_INGOT), "⚡ Basalt Flux Catalyzed (2× Speed)"));

        // Green Hydrogen mode
        registry.addRecipe(new SimpleTwoInputRecipe(STEEL_BLAST_FURNACE, Identifier.of(EnchantedWoodMod.MOD_ID, "blast_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.IRON_INGOT), EmiStack.of(ModItems.IRON_DUST))),
                EmiStack.of(ModItems.HYDROGEN_CANISTER),
                EmiStack.of(ModItems.STEEL_INGOT), "🌿 Green Hydrogen Reduction (Zero Carbon)"));
    }

    private static void registerSoilInfuserRecipes(EmiRegistry registry) {
        int idx = 0;
        List<EmiStack> soils = List.of(EmiStack.of(Items.DIRT), EmiStack.of(Items.COARSE_DIRT), EmiStack.of(Items.MUD), EmiStack.of(Items.PODZOL));

        registry.addRecipe(new SimpleTwoInputRecipe(SOIL_INFUSER, Identifier.of(EnchantedWoodMod.MOD_ID, "infuse_" + idx++),
                EmiIngredient.of(soils), EmiStack.of(ModItems.VOLCANIC_ASH),
                EmiStack.of(ModBlocks.VOLCANIC_SOIL, 2), "Standard Ash Infusion (2× Yield)"));

        registry.addRecipe(new SimpleTwoInputRecipe(SOIL_INFUSER, Identifier.of(EnchantedWoodMod.MOD_ID, "infuse_" + idx++),
                EmiIngredient.of(soils), EmiStack.of(ModItems.SULFUR_DUST),
                EmiStack.of(ModBlocks.VOLCANIC_SOIL, 3), "Sulfur Boosted Infusion (3× Yield)"));

        registry.addRecipe(new SimpleTwoInputRecipe(SOIL_INFUSER, Identifier.of(EnchantedWoodMod.MOD_ID, "infuse_" + idx++),
                EmiIngredient.of(soils), EmiStack.of(ModItems.BASALT_FLUX_CATALYST),
                EmiStack.of(ModBlocks.VOLCANIC_SOIL, 4), "⚡ Basalt Flux Maximum Infusion (4× Yield)"));
    }

    private static void registerCokeOvenRecipes(EmiRegistry registry) {
        int idx = 0;
        registry.addRecipe(new CokeOvenRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "coke_" + idx++),
                EmiStack.of(Items.COAL), EmiStack.of(ModItems.COKE_COAL), EmiStack.of(ModItems.MINERAL_TAR), "Pyrolysis Carbonization"));
        registry.addRecipe(new CokeOvenRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "coke_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.OAK_LOG), EmiStack.of(Items.BIRCH_LOG), EmiStack.of(Items.SPRUCE_LOG), EmiStack.of(Items.DARK_OAK_LOG))),
                EmiStack.of(Items.CHARCOAL), EmiStack.of(ModItems.MINERAL_TAR), "Wood Pyrolysis"));
    }

    private static void registerRefineryRecipes(EmiRegistry registry) {
        int idx = 0;
        registry.addRecipe(new SimpleTwoInputRecipe(FUEL_REFINERY, Identifier.of(EnchantedWoodMod.MOD_ID, "refine_" + idx++),
                EmiStack.of(ModItems.CRUDE_OIL_SLUDGE), EmiStack.of(ModItems.EMPTY_GAS_CANISTER),
                EmiStack.of(ModItems.GASOLINE_CANISTER), "Petrochemical Distillation (+ Mineral Tar)"));

        registry.addRecipe(new SimpleTwoInputRecipe(FUEL_REFINERY, Identifier.of(EnchantedWoodMod.MOD_ID, "refine_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.CORN), EmiStack.of(Items.WHEAT), EmiStack.of(Items.POTATO))),
                EmiStack.of(ModItems.EMPTY_GAS_CANISTER),
                EmiStack.of(ModItems.BIOFUEL_CANISTER), "Biofuel Fermentation"));

        registry.addRecipe(new SimpleTwoInputRecipe(FUEL_REFINERY, Identifier.of(EnchantedWoodMod.MOD_ID, "refine_" + idx++),
                EmiStack.of(ModItems.GASOLINE_CANISTER), EmiStack.of(ModItems.CORN, 2),
                EmiStack.of(ModItems.HIGH_OCTANE_FUEL_CANISTER), "Ethanol Alkylation (High-Octane)"));
    }

    private static void registerCrucibleRecipes(EmiRegistry registry) {
        int idx = 0;
        registry.addRecipe(new SimpleOneInputRecipe(MAGMA_CRUCIBLE, Identifier.of(EnchantedWoodMod.MOD_ID, "melt_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.COBBLESTONE, 4), EmiStack.of(Items.STONE, 4))),
                EmiStack.of(Items.LAVA_BUCKET), "Stone Magma Liquefaction"));

        registry.addRecipe(new SimpleOneInputRecipe(MAGMA_CRUCIBLE, Identifier.of(EnchantedWoodMod.MOD_ID, "melt_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.BASALT, 2), EmiStack.of(Items.BLACKSTONE, 2))),
                EmiStack.of(Items.LAVA_BUCKET), "Basalt Magma Liquefaction"));

        registry.addRecipe(new SimpleOneInputRecipe(MAGMA_CRUCIBLE, Identifier.of(EnchantedWoodMod.MOD_ID, "melt_" + idx++),
                EmiStack.of(Items.OBSIDIAN, 1),
                EmiStack.of(Items.LAVA_BUCKET), "Obsidian Magma Liquefaction"));
    }

    // Recipe Implementations
    public static class SimpleTwoInputRecipe implements EmiRecipe {
        private final EmiRecipeCategory category;
        private final Identifier id;
        private final EmiIngredient inputA;
        private final EmiIngredient inputB;
        private final EmiStack output;
        private final String note;

        public SimpleTwoInputRecipe(EmiRecipeCategory category, Identifier id, EmiIngredient inputA, EmiIngredient inputB, EmiStack output, String note) {
            this.category = category;
            this.id = id;
            this.inputA = inputA;
            this.inputB = inputB;
            this.output = output;
            this.note = note;
        }

        @Override public EmiRecipeCategory getCategory() { return category; }
        @Override public Identifier getId() { return id; }
        @Override public List<EmiIngredient> getInputs() { return List.of(inputA, inputB); }
        @Override public List<EmiStack> getOutputs() { return List.of(output); }
        @Override public int getDisplayWidth() { return 130; }
        @Override public int getDisplayHeight() { return 56; }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            // Machine Background panel (Alloy Foundry)
            widgets.addTexture(ALLOY_FOUNDRY_GUI, 0, 0, 130, 56, 15, 17);
            // Energy Storage Bar (illuminated)
            widgets.addTexture(ALLOY_FOUNDRY_GUI, 3, 3, 16, 50, 192, 0);
            // Animated smelting arrow
            widgets.addAnimatedTexture(ALLOY_FOUNDRY_GUI, 71, 17, 24, 17, 176, 14, 2000, true, false, false);
            // Slots aligned to authentic GUI
            widgets.addSlot(inputA, 28, 17).drawBack(false);
            widgets.addSlot(inputB, 48, 17).drawBack(false);
            widgets.addSlot(output, 104, 17).recipeContext(this).drawBack(false);
            if (note != null && !note.isEmpty()) {
                widgets.addText(Text.literal("§8" + note), 26, 43, 0x555555, false);
            }
        }
    }

    public static class SimpleOneInputRecipe implements EmiRecipe {
        private final EmiRecipeCategory category;
        private final Identifier id;
        private final EmiIngredient input;
        private final EmiStack output;
        private final String note;

        public SimpleOneInputRecipe(EmiRecipeCategory category, Identifier id, EmiIngredient input, EmiStack output, String note) {
            this.category = category;
            this.id = id;
            this.input = input;
            this.output = output;
            this.note = note;
        }

        @Override public EmiRecipeCategory getCategory() { return category; }
        @Override public Identifier getId() { return id; }
        @Override public List<EmiIngredient> getInputs() { return List.of(input); }
        @Override public List<EmiStack> getOutputs() { return List.of(output); }
        @Override public int getDisplayWidth() { return 126; }
        @Override public int getDisplayHeight() { return 56; }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            Identifier gui = (category == HYDRAULIC_PRESS) ? HYDRAULIC_PRESS_GUI : CRUSHER_GUI;
            // Machine Background panel
            widgets.addTexture(gui, 0, 0, 126, 56, 46, 17);
            // Energy bar (illuminated)
            widgets.addTexture(gui, 106, 3, 14, 52, 190, 0);
            // Animated progress arrow
            widgets.addAnimatedTexture(gui, 33, 17, 24, 17, 176, 14, 2000, true, false, false);
            // Slots aligned to GUI
            widgets.addSlot(input, 9, 17).drawBack(false);
            widgets.addSlot(output, 69, 17).recipeContext(this).drawBack(false);
            if (note != null && !note.isEmpty()) {
                widgets.addText(Text.literal("§8" + note), 8, 43, 0x555555, false);
            }
        }
    }

    public static class CokeOvenRecipe implements EmiRecipe {
        private final Identifier id;
        private final EmiIngredient input;
        private final EmiStack output;
        private final EmiStack byproduct;
        private final String note;

        public CokeOvenRecipe(Identifier id, EmiIngredient input, EmiStack output, EmiStack byproduct, String note) {
            this.id = id;
            this.input = input;
            this.output = output;
            this.byproduct = byproduct;
            this.note = note;
        }

        @Override public EmiRecipeCategory getCategory() { return COKE_OVEN; }
        @Override public Identifier getId() { return id; }
        @Override public List<EmiIngredient> getInputs() { return List.of(input); }
        @Override public List<EmiStack> getOutputs() { return List.of(output, byproduct); }
        @Override public int getDisplayWidth() { return 126; }
        @Override public int getDisplayHeight() { return 56; }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            // Machine Background panel
            widgets.addTexture(COKE_OVEN_GUI, 0, 0, 126, 56, 46, 17);
            // Animated cook progress arrow
            widgets.addAnimatedTexture(COKE_OVEN_GUI, 33, 17, 24, 17, 176, 14, 2500, true, false, false);
            // Slots aligned to GUI
            widgets.addSlot(input, 9, 17).drawBack(false);
            widgets.addSlot(output, 69, 17).recipeContext(this).drawBack(false);
            widgets.addSlot(byproduct, 95, 17).recipeContext(this);
            if (note != null && !note.isEmpty()) {
                widgets.addText(Text.literal("§8" + note), 8, 43, 0x555555, false);
            }
        }
    }

    private static void registerCentrifugeRecipes(EmiRegistry registry) {
        int idx = 0;
        // 1. Slime Ball -> Alkaline Base Extract + Sulfur Dust
        registry.addRecipe(new CentrifugeEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "centrifuge_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.SLIME_BALL))),
                EmiStack.of(ModItems.ALKALINE_BASE_EXTRACT),
                EmiStack.of(ModItems.SULFUR_DUST),
                "Alkaline Neutralizer Separation"));

        // 2. Magma Cream / Crimson Fungus -> Cryo-Thermal Extract + Volcanic Ash
        registry.addRecipe(new CentrifugeEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "centrifuge_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.MAGMA_CREAM), EmiStack.of(Items.CRIMSON_FUNGUS))),
                EmiStack.of(ModItems.CRYO_THERMAL_EXTRACT),
                EmiStack.of(ModItems.VOLCANIC_ASH),
                "Endothermic Extract Separation"));

        // 3. Kelp / Seagrass / Cucumber -> Oxygenated Extract + Bone Meal
        registry.addRecipe(new CentrifugeEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "centrifuge_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.KELP), EmiStack.of(Items.SEAGRASS), EmiStack.of(ModItems.CUCUMBER))),
                EmiStack.of(ModItems.OXYGENATED_EXTRACT),
                EmiStack.of(Items.BONE_MEAL),
                "Oxygenated Hemoglobin Separation"));

        // 4. Dragon Fruit / Nether Wart -> Cellular Nanite Extract + Sugar
        registry.addRecipe(new CentrifugeEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "centrifuge_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.DRAGON_FRUIT), EmiStack.of(Items.NETHER_WART))),
                EmiStack.of(ModItems.CELLULAR_NANITE_EXTRACT),
                EmiStack.of(Items.SUGAR),
                "Cellular Nanite Matrix Separation"));

        // 5. Glow Berries / Wasabi Root -> Adrenal Essence + Glowstone Dust
        registry.addRecipe(new CentrifugeEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "centrifuge_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.GLOW_BERRIES), EmiStack.of(ModItems.WASABI_ROOT))),
                EmiStack.of(ModItems.ADRENAL_ESSENCE),
                EmiStack.of(Items.GLOWSTONE_DUST),
                "Adrenal Neuro-Stimulant Separation"));
    }

    private static void registerSynthesizerRecipes(EmiRegistry registry) {
        int idx = 0;
        // 0. Sterile Empty Cartridge Assembly
        registry.addRecipe(new SynthesizerEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "synth_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.GLASS_PANE), EmiStack.of(Items.GLASS))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.TIN_INGOT), EmiStack.of(ModItems.TITANIUM_INGOT))),
                EmiIngredient.of(List.of(EmiStack.of(Items.QUARTZ), EmiStack.of(Items.REDSTONE))),
                EmiStack.of(ModItems.EMPTY_CARTRIDGE, 4),
                "Sterile Hermetic Ampoule Assembly"));

        // 1. Acid-Neutralizing Cartridge
        registry.addRecipe(new SynthesizerEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "synth_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.EMPTY_CARTRIDGE))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.ALKALINE_BASE_EXTRACT))),
                EmiIngredient.of(List.of(EmiStack.of(Items.REDSTONE), EmiStack.of(Items.GLOWSTONE_DUST), EmiStack.of(ModItems.SULFUR_DUST))),
                EmiStack.of(ModItems.ACID_NEUTRALIZING_CARTRIDGE),
                "Acid & Environmental Toxin Immunity (6m)"));

        // 2. Endothermic Heat-Buffer Cartridge
        registry.addRecipe(new SynthesizerEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "synth_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.EMPTY_CARTRIDGE))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.CRYO_THERMAL_EXTRACT))),
                EmiIngredient.of(List.of(EmiStack.of(Items.BLAZE_POWDER), EmiStack.of(ModItems.FIRE_CRYSTAL), EmiStack.of(Items.MAGMA_CREAM), EmiStack.of(ModItems.VOLCANIC_ASH))),
                EmiStack.of(ModItems.HEAT_BUFFER_CARTRIDGE),
                "Lava & Caldera Thermal Protection (6m)"));

        // 3. Hyper-Oxygenation Cartridge
        registry.addRecipe(new SynthesizerEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "synth_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.EMPTY_CARTRIDGE))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.OXYGENATED_EXTRACT))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.TITANIUM_INGOT), EmiStack.of(ModItems.ALUMINUM_INGOT), EmiStack.of(Items.IRON_INGOT), EmiStack.of(Items.BONE_MEAL))),
                EmiStack.of(ModItems.HYPER_OXYGENATION_CARTRIDGE),
                "Atmospheric & Vacuum Breathing (6m)"));

        // 4. Nanite Trauma Cartridge
        registry.addRecipe(new SynthesizerEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "synth_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.EMPTY_CARTRIDGE))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.CELLULAR_NANITE_EXTRACT))),
                EmiIngredient.of(List.of(EmiStack.of(Items.GOLDEN_APPLE), EmiStack.of(ModItems.TITANIUM_INGOT), EmiStack.of(Items.GHAST_TEAR), EmiStack.of(Items.BONE_MEAL))),
                EmiStack.of(ModItems.NANITE_TRAUMA_CARTRIDGE),
                "Critical Trauma: +8 HP & Regen II & Cleansing"));

        // 5. Adrenaline Combat Stim Cartridge
        registry.addRecipe(new SynthesizerEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "synth_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.EMPTY_CARTRIDGE))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.ADRENAL_ESSENCE))),
                EmiIngredient.of(List.of(EmiStack.of(Items.SUGAR), EmiStack.of(Items.GLOWSTONE_DUST), EmiStack.of(Items.QUARTZ))),
                EmiStack.of(ModItems.ADRENALINE_STIM_CARTRIDGE),
                "Combat Stim: Speed II, Haste II, Resistance I (3m)"));

        // Grade-A Pure Cleanroom Hypospray Synthesis Recipes
        registry.addRecipe(new SynthesizerEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "synth_pure_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.EMPTY_CARTRIDGE))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.ALKALINE_BASE_EXTRACT))),
                EmiIngredient.of(List.of(EmiStack.of(Items.REDSTONE), EmiStack.of(Items.GLOWSTONE_DUST), EmiStack.of(ModItems.SULFUR_DUST))),
                EmiStack.of(createPureCartridge(ModItems.ACID_NEUTRALIZING_CARTRIDGE)),
                "✦ Cleanroom Only: 12m Duration + Saturation Buff"));

        registry.addRecipe(new SynthesizerEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "synth_pure_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.EMPTY_CARTRIDGE))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.CRYO_THERMAL_EXTRACT))),
                EmiIngredient.of(List.of(EmiStack.of(Items.BLAZE_POWDER), EmiStack.of(ModItems.FIRE_CRYSTAL), EmiStack.of(Items.MAGMA_CREAM))),
                EmiStack.of(createPureCartridge(ModItems.HEAT_BUFFER_CARTRIDGE)),
                "✦ Cleanroom Only: 12m Duration + Fire Resistance"));

        registry.addRecipe(new SynthesizerEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "synth_pure_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.EMPTY_CARTRIDGE))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.OXYGENATED_EXTRACT))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.TITANIUM_INGOT), EmiStack.of(ModItems.ALUMINUM_INGOT), EmiStack.of(Items.IRON_INGOT))),
                EmiStack.of(createPureCartridge(ModItems.HYPER_OXYGENATION_CARTRIDGE)),
                "✦ Cleanroom Only: 12m Duration + Dolphin's Grace"));

        registry.addRecipe(new SynthesizerEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "synth_pure_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.EMPTY_CARTRIDGE))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.CELLULAR_NANITE_EXTRACT))),
                EmiIngredient.of(List.of(EmiStack.of(Items.GOLDEN_APPLE), EmiStack.of(ModItems.TITANIUM_INGOT), EmiStack.of(Items.GHAST_TEAR))),
                EmiStack.of(createPureCartridge(ModItems.NANITE_TRAUMA_CARTRIDGE)),
                "✦ Cleanroom Only: +20 HP, Regen III, Absorption II (4 hearts)"));

        registry.addRecipe(new SynthesizerEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "synth_pure_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.EMPTY_CARTRIDGE))),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.ADRENAL_ESSENCE))),
                EmiIngredient.of(List.of(EmiStack.of(Items.SUGAR), EmiStack.of(Items.GLOWSTONE_DUST), EmiStack.of(Items.QUARTZ))),
                EmiStack.of(createPureCartridge(ModItems.ADRENALINE_STIM_CARTRIDGE)),
                "✦ Cleanroom Only: Speed III, Haste II, Resistance II, Strength (6m)"));

        // Polymer Loom Recipes
        int loomIdx = 0;
        registry.addRecipe(new PolymerLoomEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "loom_" + loomIdx++),
                EmiStack.of(ModItems.RUBBER),
                EmiStack.of(Items.STRING),
                EmiStack.of(ModItems.SILICON),
                EmiStack.of(ModItems.STERILE_POLYMER_FABRIC, 4),
                "ESD Microfiber Textile Weaving"));

        registry.addRecipe(new PolymerLoomEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "loom_" + loomIdx++),
                EmiStack.of(ModItems.STERILE_POLYMER_FABRIC),
                EmiStack.of(Items.GLASS_PANE),
                EmiStack.of(ModItems.TIN_INGOT),
                EmiStack.of(ModItems.CLEANROOM_HOOD),
                "Zero-Shedding Cleanroom Sanitary Hood"));

        registry.addRecipe(new PolymerLoomEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "loom_" + loomIdx++),
                EmiStack.of(ModItems.STERILE_POLYMER_FABRIC),
                EmiStack.of(ModItems.TITANIUM_INGOT),
                EmiStack.of(ModItems.STEEL_INGOT),
                EmiStack.of(ModItems.CLEANROOM_SMOCK),
                "Sealed-Cuff Anti-Static Torso Smock"));

        registry.addRecipe(new PolymerLoomEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "loom_" + loomIdx++),
                EmiStack.of(ModItems.STERILE_POLYMER_FABRIC),
                EmiStack.of(ModItems.ALUMINUM_INGOT),
                EmiStack.of(ModItems.RUBBER),
                EmiStack.of(ModItems.CLEANROOM_TROUSERS),
                "Lint-Free Electro-Dissipative Trousers"));

        registry.addRecipe(new PolymerLoomEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "loom_" + loomIdx++),
                EmiStack.of(ModItems.STERILE_POLYMER_FABRIC),
                EmiStack.of(ModItems.RUBBER),
                EmiStack.of(ModItems.TITANIUM_INGOT),
                EmiStack.of(ModItems.CLEANROOM_BOOTIES),
                "Anti-Slip ESD Floor Boot Covers"));
    }

    private static ItemStack createPureCartridge(net.minecraft.item.Item item) {
        ItemStack stack = new ItemStack(item);
        net.enchantedwood.item.custom.HyposprayCartridgeItem.setPure(stack, true);
        return stack;
    }

    public static class CentrifugeEmiRecipe implements EmiRecipe {
        private final Identifier id;
        private final EmiIngredient input;
        private final EmiStack output;
        private final EmiStack byproduct;
        private final String note;

        public CentrifugeEmiRecipe(Identifier id, EmiIngredient input, EmiStack output, EmiStack byproduct, String note) {
            this.id = id;
            this.input = input;
            this.output = output;
            this.byproduct = byproduct;
            this.note = note;
        }

        @Override public EmiRecipeCategory getCategory() { return INDUSTRIAL_CENTRIFUGE; }
        @Override public Identifier getId() { return id; }
        @Override public List<EmiIngredient> getInputs() { return List.of(input); }
        @Override public List<EmiStack> getOutputs() { return List.of(output, byproduct); }
        @Override public int getDisplayWidth() { return 142; }
        @Override public int getDisplayHeight() { return 56; }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            // Authentic Machine Background panel (Industrial Centrifuge)
            widgets.addTexture(CENTRIFUGE_GUI, 0, 0, 142, 56, 15, 17);
            // Energy Storage Bar (illuminated)
            widgets.addTexture(CENTRIFUGE_GUI, 3, 3, 16, 50, 192, 0);
            // Animated centrifugal separation arrow
            widgets.addAnimatedTexture(CENTRIFUGE_GUI, 57, 17, 24, 17, 176, 14, 2500, true, false, false);
            // Slots aligned to authentic machine frame
            widgets.addSlot(input, 32, 17).drawBack(false);
            widgets.addSlot(output, 90, 17).recipeContext(this).drawBack(false);
            widgets.addSlot(byproduct, 118, 17).recipeContext(this).drawBack(false);
            if (note != null && !note.isEmpty()) {
                widgets.addText(Text.literal("§8" + note), 28, 43, 0x555555, false);
            }
        }
    }

    public static class SynthesizerEmiRecipe implements EmiRecipe {
        private final Identifier id;
        private final EmiIngredient cartridge;
        private final EmiIngredient essence;
        private final EmiIngredient catalyst;
        private final EmiStack output;
        private final String note;

        public SynthesizerEmiRecipe(Identifier id, EmiIngredient cartridge, EmiIngredient essence, EmiIngredient catalyst, EmiStack output, String note) {
            this.id = id;
            this.cartridge = cartridge;
            this.essence = essence;
            this.catalyst = catalyst;
            this.output = output;
            this.note = note;
        }

        @Override public EmiRecipeCategory getCategory() { return CHEMICAL_SYNTHESIZER; }
        @Override public Identifier getId() { return id; }
        @Override public List<EmiIngredient> getInputs() { return List.of(cartridge, essence, catalyst); }
        @Override public List<EmiStack> getOutputs() { return List.of(output); }
        @Override public int getDisplayWidth() { return 142; }
        @Override public int getDisplayHeight() { return 64; }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            // Authentic Chemical Synthesizer machine background panel (crop from u=15, v=14)
            widgets.addTexture(CHEMICAL_SYNTHESIZER_GUI, 0, 0, 142, 64, 15, 14);
            // Energy Storage Bar (illuminated at x=18-15=3, y=20-14=6)
            widgets.addTexture(CHEMICAL_SYNTHESIZER_GUI, 3, 6, 16, 50, 192, 0);
            // Animated compounding arrow: x=76-15=61, y=35-14=21
            widgets.addAnimatedTexture(CHEMICAL_SYNTHESIZER_GUI, 61, 21, 24, 17, 176, 14, 3000, true, false, false);
            // 3 stacked input chambers (align with beveled slots on texture: x=44-15=29)
            widgets.addSlot(cartridge, 29, 3).drawBack(false);
            widgets.addSlot(essence, 29, 21).drawBack(false);
            widgets.addSlot(catalyst, 29, 39).drawBack(false);
            // Output slot (align with large 26x26 output box on texture: item at x=120-15=105, y=35-14=21)
            widgets.addSlot(output, 105, 21).recipeContext(this).drawBack(false);
            if (note != null && !note.isEmpty()) {
                widgets.addText(Text.literal("§8" + note), 26, 56, 0x555555, false);
            }
        }
    }

    public static class PolymerLoomEmiRecipe implements EmiRecipe {
        private final Identifier id;
        private final EmiIngredient polymer;
        private final EmiIngredient fiber;
        private final EmiIngredient additive;
        private final EmiStack output;
        private final String note;

        public PolymerLoomEmiRecipe(Identifier id, EmiIngredient polymer, EmiIngredient fiber, EmiIngredient additive, EmiStack output, String note) {
            this.id = id;
            this.polymer = polymer;
            this.fiber = fiber;
            this.additive = additive;
            this.output = output;
            this.note = note;
        }

        @Override public EmiRecipeCategory getCategory() { return POLYMER_LOOM; }
        @Override public Identifier getId() { return id; }
        @Override public List<EmiIngredient> getInputs() { return List.of(polymer, fiber, additive); }
        @Override public List<EmiStack> getOutputs() { return List.of(output); }
        @Override public int getDisplayWidth() { return 142; }
        @Override public int getDisplayHeight() { return 64; }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            // Authentic Polymer Loom machine background panel
            widgets.addTexture(POLYMER_LOOM_GUI, 0, 0, 142, 64, 15, 14);
            // Energy Storage Bar (illuminated at x=3, y=6)
            widgets.addTexture(POLYMER_LOOM_GUI, 3, 6, 16, 50, 192, 0);
            // Animated weaving arrow at x=61, y=21
            widgets.addAnimatedTexture(POLYMER_LOOM_GUI, 61, 21, 24, 17, 176, 14, 3000, true, false, false);
            // 3 stacked input chambers
            widgets.addSlot(polymer, 29, 3).drawBack(false);
            widgets.addSlot(fiber, 29, 21).drawBack(false);
            widgets.addSlot(additive, 29, 39).drawBack(false);
            // Output slot
            widgets.addSlot(output, 105, 21).recipeContext(this).drawBack(false);
            if (note != null && !note.isEmpty()) {
                widgets.addText(Text.literal("§8" + note), 26, 56, 0x555555, false);
            }
        }
    }

    public static class CircuitFabricatorEmiRecipe implements EmiRecipe {
        private final Identifier id;
        private final EmiIngredient substrate;
        private final EmiIngredient comp1;
        private final EmiIngredient comp2;
        private final EmiIngredient comp3;
        private final EmiStack output;
        private final String note;

        public CircuitFabricatorEmiRecipe(Identifier id, EmiIngredient substrate, EmiIngredient comp1, EmiIngredient comp2, EmiIngredient comp3, EmiStack output, String note) {
            this.id = id;
            this.substrate = substrate;
            this.comp1 = comp1;
            this.comp2 = comp2;
            this.comp3 = comp3;
            this.output = output;
            this.note = note;
        }

        @Override public EmiRecipeCategory getCategory() { return CIRCUIT_FABRICATOR; }
        @Override public Identifier getId() { return id; }
        @Override public List<EmiIngredient> getInputs() { return List.of(substrate, comp1, comp2, comp3); }
        @Override public List<EmiStack> getOutputs() { return List.of(output); }
        @Override public int getDisplayWidth() { return 142; }
        @Override public int getDisplayHeight() { return 64; }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            // Authentic Circuit Fabricator background panel
            widgets.addTexture(CIRCUIT_FABRICATOR_GUI, 0, 0, 142, 64, 8, 12);
            // Energy Storage Bar (illuminated)
            widgets.addTexture(CIRCUIT_FABRICATOR_GUI, 2, 6, 12, 50, 176, 0);
            // Animated fabrication laser
            widgets.addAnimatedTexture(CIRCUIT_FABRICATOR_GUI, 80, 22, 24, 17, 188, 0, 2500, true, false, false);
            // Substrate slot at x=34-8=26, y=35-12=23
            widgets.addSlot(substrate, 26, 23).drawBack(false);
            // 3 component slots at x=64-8=56, y=17-12=5, 23, 41
            widgets.addSlot(comp1, 56, 5).drawBack(false);
            widgets.addSlot(comp2, 56, 23).drawBack(false);
            widgets.addSlot(comp3, 56, 41).drawBack(false);
            // Output slot at x=124-8=116, y=35-12=23
            widgets.addSlot(output, 116, 23).recipeContext(this).drawBack(false);
            if (note != null && !note.isEmpty()) {
                widgets.addText(Text.literal("§8" + note), 24, 56, 0x555555, false);
            }
        }
    }

    private static void registerCircuitFabricatorRecipes(EmiRegistry registry) {
        int idx = 0;
        // 1. Basic Computer Chip
        registry.addRecipe(new CircuitFabricatorEmiRecipe(
                Identifier.of(EnchantedWoodMod.MOD_ID, "circuit_fab_" + idx++),
                EmiStack.of(ModItems.SILICON_WAFER),
                EmiStack.of(Items.COPPER_INGOT),
                EmiStack.of(Items.GOLD_NUGGET),
                EmiStack.of(Items.REDSTONE),
                EmiStack.of(ModItems.BASIC_COMPUTER_CHIP),
                "Tier 1 Micro-Controller"
        ));
        // 2. Advanced Computer Chip
        registry.addRecipe(new CircuitFabricatorEmiRecipe(
                Identifier.of(EnchantedWoodMod.MOD_ID, "circuit_fab_" + idx++),
                EmiStack.of(ModItems.BASIC_COMPUTER_CHIP),
                EmiStack.of(Items.DIAMOND),
                EmiStack.of(Items.GLOWSTONE_DUST),
                EmiStack.of(Items.LAPIS_LAZULI),
                EmiStack.of(ModItems.ADVANCED_COMPUTER_CHIP),
                "Tier 2 Environmental Processor"
        ));
        // 3. Quantum Computer Chip
        registry.addRecipe(new CircuitFabricatorEmiRecipe(
                Identifier.of(EnchantedWoodMod.MOD_ID, "circuit_fab_" + idx++),
                EmiStack.of(ModItems.ADVANCED_COMPUTER_CHIP),
                EmiStack.of(ModItems.NETHERITE_DUST),
                EmiStack.of(Items.BLAZE_POWDER),
                EmiStack.of(ModItems.ENCHANTED_DUST),
                EmiStack.of(ModItems.QUANTUM_COMPUTER_CHIP),
                "Tier 3 Quantum Core"
        ));
        // 4. Metallurgy Controller Chip
        registry.addRecipe(new CircuitFabricatorEmiRecipe(
                Identifier.of(EnchantedWoodMod.MOD_ID, "circuit_fab_" + idx++),
                EmiStack.of(ModItems.SILICON_WAFER),
                EmiStack.of(Items.GOLD_INGOT),
                EmiStack.of(Items.REDSTONE),
                EmiStack.of(ModItems.ZIRCONIA_NODULE),
                EmiStack.of(ModItems.METALLURGY_CONTROLLER_CHIP),
                "Industrial Alloying Logic Module"
        ));
    }

    public static class InductionSmelterEmiRecipe implements EmiRecipe {
        private final Identifier id;
        private final EmiIngredient input;
        private final EmiStack outputStack;
        private final String fluidYield;
        private final String note;

        public InductionSmelterEmiRecipe(Identifier id, EmiIngredient input, EmiStack outputStack, String fluidYield, String note) {
            this.id = id;
            this.input = input;
            this.outputStack = outputStack;
            this.fluidYield = fluidYield;
            this.note = note;
        }

        @Override public EmiRecipeCategory getCategory() { return INDUCTION_SMELTER; }
        @Override public Identifier getId() { return id; }
        @Override public List<EmiIngredient> getInputs() { return List.of(input); }
        @Override public List<EmiStack> getOutputs() { return List.of(outputStack); }
        @Override public int getDisplayWidth() { return 142; }
        @Override public int getDisplayHeight() { return 64; }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(INDUCTION_SMELTER_GUI, 0, 0, 142, 64, 15, 14);
            // Energy bar (illuminated)
            widgets.addTexture(INDUCTION_SMELTER_GUI, 10, 4, 12, 50, 188, 0);
            // Flame animation
            widgets.addAnimatedTexture(INDUCTION_SMELTER_GUI, 61, 28, 24, 17, 200, 0, 2000, true, false, false);
            // Input slot
            widgets.addSlot(input, 36, 12).drawBack(false);
            // Output stack / fluid proxy
            widgets.addSlot(outputStack, 95, 20).recipeContext(this).drawBack(false);
            widgets.addText(Text.literal("§6" + fluidYield), 36, 42, 0xFFAA00, false);
            if (note != null && !note.isEmpty()) {
                widgets.addText(Text.literal("§8" + note), 20, 54, 0x555555, false);
            }
        }
    }

    private static void registerInductionSmelterRecipes(EmiRegistry registry) {
        int idx = 0;
        // Armor & Loot Reclaim
        registry.addRecipe(new InductionSmelterEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "smelt_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.IRON_CHESTPLATE))), EmiStack.of(Items.IRON_INGOT, 8), "720 mB Molten Iron", "100% Dungeon Gear Reclaim"));
        registry.addRecipe(new InductionSmelterEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "smelt_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.GOLDEN_CHESTPLATE))), EmiStack.of(Items.GOLD_INGOT, 8), "720 mB Molten Gold", "100% Dungeon Gear Reclaim"));
        registry.addRecipe(new InductionSmelterEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "smelt_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.IRON_HORSE_ARMOR), EmiStack.of(Items.GOLDEN_HORSE_ARMOR))), EmiStack.of(Items.IRON_INGOT, 7), "630 mB Metal", "Horse Armor Reclaim"));
        registry.addRecipe(new InductionSmelterEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "smelt_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.ANVIL), EmiStack.of(Items.CHIPPED_ANVIL), EmiStack.of(Items.DAMAGED_ANVIL))), EmiStack.of(Items.IRON_BLOCK, 3), "2,790 mB Iron", "Full 31 Ingot Reclaim"));
        registry.addRecipe(new InductionSmelterEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "smelt_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.IRON_SWORD), EmiStack.of(Items.GOLDEN_SWORD))), EmiStack.of(Items.IRON_INGOT, 2), "180 mB Metal", "Weapon Melting"));
        // Primary Ingots
        registry.addRecipe(new InductionSmelterEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "smelt_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.IRON_INGOT))), EmiStack.of(Items.IRON_INGOT), "90 mB Molten Iron", "Standard Ingot Smelt"));
        registry.addRecipe(new InductionSmelterEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "smelt_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.GOLD_INGOT))), EmiStack.of(Items.GOLD_INGOT), "90 mB Molten Gold", "Standard Ingot Smelt"));
        registry.addRecipe(new InductionSmelterEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "smelt_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(Items.COPPER_INGOT))), EmiStack.of(Items.COPPER_INGOT), "90 mB Molten Copper", "Standard Ingot Smelt"));
        registry.addRecipe(new InductionSmelterEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "smelt_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.TIN_INGOT))), EmiStack.of(ModItems.TIN_INGOT), "90 mB Molten Tin", "Standard Ingot Smelt"));
        registry.addRecipe(new InductionSmelterEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "smelt_" + idx++),
                EmiIngredient.of(List.of(EmiStack.of(ModItems.TITANIUM_INGOT))), EmiStack.of(ModItems.TITANIUM_INGOT), "90 mB Molten Titanium", "Standard Ingot Smelt"));
    }

    public static class CastingPortEmiRecipe implements EmiRecipe {
        private final Identifier id;
        private final EmiIngredient fluidInput;
        private final EmiStack output;
        private final String cost;

        public CastingPortEmiRecipe(Identifier id, EmiIngredient fluidInput, EmiStack output, String cost) {
            this.id = id;
            this.fluidInput = fluidInput;
            this.output = output;
            this.cost = cost;
        }

        @Override public EmiRecipeCategory getCategory() { return CASTING_PORT; }
        @Override public Identifier getId() { return id; }
        @Override public List<EmiIngredient> getInputs() { return List.of(fluidInput); }
        @Override public List<EmiStack> getOutputs() { return List.of(output); }
        @Override public int getDisplayWidth() { return 142; }
        @Override public int getDisplayHeight() { return 56; }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(CASTING_PORT_GUI, 0, 0, 142, 56, 25, 14);
            // Solidify progress
            widgets.addAnimatedTexture(CASTING_PORT_GUI, 47, 20, 24, 17, 176, 0, 1000, true, false, false);
            widgets.addSlot(fluidInput, 11, 20).drawBack(false);
            widgets.addSlot(output, 90, 20).recipeContext(this).drawBack(false);
            widgets.addText(Text.literal("§b" + cost), 40, 42, 0x00BCD4, false);
        }
    }

    private static void registerCastingPortRecipes(EmiRegistry registry) {
        int idx = 0;
        // Nuggets (10 mB)
        registry.addRecipe(new CastingPortEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "cast_" + idx++),
                EmiStack.of(Items.IRON_INGOT), EmiStack.of(Items.IRON_NUGGET), "10 mB Molten Iron -> Nugget"));
        registry.addRecipe(new CastingPortEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "cast_" + idx++),
                EmiStack.of(Items.GOLD_INGOT), EmiStack.of(Items.GOLD_NUGGET), "10 mB Molten Gold -> Nugget"));
        registry.addRecipe(new CastingPortEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "cast_" + idx++),
                EmiStack.of(Items.COPPER_INGOT), EmiStack.of(ModItems.COPPER_NUGGET), "10 mB Molten Copper -> Nugget"));
        // Ingots (90 mB)
        registry.addRecipe(new CastingPortEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "cast_" + idx++),
                EmiStack.of(Items.IRON_NUGGET), EmiStack.of(Items.IRON_INGOT), "90 mB Molten Iron -> Ingot"));
        registry.addRecipe(new CastingPortEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "cast_" + idx++),
                EmiStack.of(Items.GOLD_NUGGET), EmiStack.of(Items.GOLD_INGOT), "90 mB Molten Gold -> Ingot"));
        registry.addRecipe(new CastingPortEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "cast_" + idx++),
                EmiStack.of(ModItems.COPPER_NUGGET), EmiStack.of(Items.COPPER_INGOT), "90 mB Molten Copper -> Ingot"));
        registry.addRecipe(new CastingPortEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "cast_" + idx++),
                EmiStack.of(ModItems.TIN_NUGGET), EmiStack.of(ModItems.TIN_INGOT), "90 mB Molten Tin -> Ingot"));
        registry.addRecipe(new CastingPortEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "cast_" + idx++),
                EmiStack.of(ModItems.BRONZE_NUGGET), EmiStack.of(ModItems.BRONZE_INGOT), "90 mB Molten Bronze -> Ingot"));
        registry.addRecipe(new CastingPortEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "cast_" + idx++),
                EmiStack.of(ModItems.STEEL_NUGGET), EmiStack.of(ModItems.STEEL_INGOT), "90 mB Molten Steel -> Ingot"));
        registry.addRecipe(new CastingPortEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "cast_" + idx++),
                EmiStack.of(ModItems.TITANIUM_NUGGET), EmiStack.of(ModItems.TITANIUM_INGOT), "90 mB Molten Titanium -> Ingot"));
        // Blocks (810 mB)
        registry.addRecipe(new CastingPortEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "cast_" + idx++),
                EmiStack.of(Items.IRON_INGOT), EmiStack.of(Items.IRON_BLOCK), "810 mB Molten Iron -> Block"));
        registry.addRecipe(new CastingPortEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "cast_" + idx++),
                EmiStack.of(Items.GOLD_INGOT), EmiStack.of(Items.GOLD_BLOCK), "810 mB Molten Gold -> Block"));
        registry.addRecipe(new CastingPortEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "cast_" + idx++),
                EmiStack.of(Items.COPPER_INGOT), EmiStack.of(Items.COPPER_BLOCK), "810 mB Molten Copper -> Block"));
        registry.addRecipe(new CastingPortEmiRecipe(Identifier.of(EnchantedWoodMod.MOD_ID, "cast_" + idx++),
                EmiStack.of(ModItems.BRONZE_INGOT), EmiStack.of(ModBlocks.BRONZE_BLOCK), "810 mB Molten Bronze -> Block"));
    }
}


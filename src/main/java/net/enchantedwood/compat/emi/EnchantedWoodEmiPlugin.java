package net.enchantedwood.compat.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
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

    // Categories
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

        // Register Workstations (Catalysts)
        registry.addWorkstation(ALLOY_FOUNDRY, EmiStack.of(ModBlocks.ALLOY_FOUNDRY));
        registry.addWorkstation(CRUSHER, EmiStack.of(ModBlocks.CRUSHER));
        registry.addWorkstation(CRUSHER, EmiStack.of(ModBlocks.CRUSHER_MK2));
        registry.addWorkstation(DUST_SMELTER, EmiStack.of(ModBlocks.DUST_SMELTER));
        registry.addWorkstation(STEEL_BLAST_FURNACE, EmiStack.of(ModBlocks.STEEL_BLAST_FURNACE));
        registry.addWorkstation(SOIL_INFUSER, EmiStack.of(ModBlocks.SOIL_INFUSER));
        registry.addWorkstation(COKE_OVEN, EmiStack.of(ModBlocks.COKE_OVEN));
        registry.addWorkstation(FUEL_REFINERY, EmiStack.of(ModBlocks.FUEL_REFINERY));
        registry.addWorkstation(MAGMA_CRUCIBLE, EmiStack.of(ModBlocks.MAGMA_CRUCIBLE));
        registry.addWorkstation(HYDRAULIC_PRESS, EmiStack.of(ModBlocks.HYDRAULIC_PRESS));
        registry.addWorkstation(dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.CRAFTING, EmiStack.of(ModBlocks.SUPER_COMPUTER));

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
                return recipe.getCategory() == dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.CRAFTING;
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
    }

    private static void registerCrusherRecipes(EmiRegistry registry) {
        int idx = 0;
        addCrush(registry, idx++, List.of(EmiStack.of(Items.RAW_IRON), EmiStack.of(Items.IRON_ORE), EmiStack.of(Items.DEEPSLATE_IRON_ORE)), EmiStack.of(ModItems.IRON_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.RAW_COPPER), EmiStack.of(Items.COPPER_ORE), EmiStack.of(Items.DEEPSLATE_COPPER_ORE)), EmiStack.of(ModItems.COPPER_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.RAW_GOLD), EmiStack.of(Items.GOLD_ORE), EmiStack.of(Items.DEEPSLATE_GOLD_ORE), EmiStack.of(Items.NETHER_GOLD_ORE)), EmiStack.of(ModItems.GOLD_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.RAW_BAUXITE), EmiStack.of(ModBlocks.BAUXITE_ORE), EmiStack.of(ModBlocks.DEEPSLATE_BAUXITE_ORE)), EmiStack.of(ModItems.BAUXITE_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.RAW_TIN), EmiStack.of(ModBlocks.TIN_ORE), EmiStack.of(ModBlocks.DEEPSLATE_TIN_ORE)), EmiStack.of(ModItems.TIN_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.RAW_TITANIUM), EmiStack.of(ModBlocks.TITANIUM_ORE), EmiStack.of(ModBlocks.DEEPSLATE_TITANIUM_ORE)), EmiStack.of(ModItems.TITANIUM_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.RAW_COBALT), EmiStack.of(ModBlocks.COBALT_ORE)), EmiStack.of(ModItems.COBALT_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.RAW_ARDITE), EmiStack.of(ModBlocks.ARDITE_ORE)), EmiStack.of(ModItems.ARDITE_DUST, 2), "Up to 8× with Blaze Core");
        addCrush(registry, idx++, List.of(EmiStack.of(ModItems.RAW_TUNGSTEN), EmiStack.of(ModBlocks.NETHER_TUNGSTEN_ORE), EmiStack.of(ModBlocks.DEEPSLATE_TUNGSTEN_ORE)), EmiStack.of(ModItems.TUNGSTEN_DUST, 2), "Up to 8× with Blaze Core");

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

        addCrush(registry, idx++, List.of(EmiStack.of(Items.COAL), EmiStack.of(Items.CHARCOAL), EmiStack.of(ModItems.COKE_COAL)), EmiStack.of(ModItems.COAL_DUST, 2), "Carbon Pulverization");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.WHEAT)), EmiStack.of(Items.BREAD, 1), "Flour & Bread Milling");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.SUGAR_CANE)), EmiStack.of(Items.SUGAR, 2), "Sugar Milling");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.BONE)), EmiStack.of(Items.BONE_MEAL, 4), "Bone Crushing");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.BLAZE_ROD)), EmiStack.of(Items.BLAZE_POWDER, 4), "Blaze Crushing");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.COBBLESTONE), EmiStack.of(Items.STONE)), EmiStack.of(Items.GRAVEL, 1), "Milling");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.GRAVEL)), EmiStack.of(Items.SAND, 1), "Milling");
        addCrush(registry, idx++, List.of(EmiStack.of(Items.BASALT), EmiStack.of(Items.SMOOTH_BASALT)), EmiStack.of(ModItems.VOLCANIC_ASH, 2), "Ash Extraction");
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
        @Override public int getDisplayHeight() { return 36; }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addSlot(inputA, 10, 8);
            widgets.addSlot(inputB, 34, 8);
            widgets.addFillingArrow(62, 8, 2000);
            widgets.addSlot(output, 96, 8).recipeContext(this);
            if (note != null && !note.isEmpty()) {
                widgets.addText(Text.literal("§8" + note), 10, 26, 0x555555, false);
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
        @Override public int getDisplayWidth() { return 130; }
        @Override public int getDisplayHeight() { return 36; }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addSlot(input, 18, 8);
            widgets.addFillingArrow(52, 8, 1500);
            widgets.addSlot(output, 88, 8).recipeContext(this);
            if (note != null && !note.isEmpty()) {
                widgets.addText(Text.literal("§8" + note), 10, 26, 0x555555, false);
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
        @Override public int getDisplayWidth() { return 140; }
        @Override public int getDisplayHeight() { return 36; }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addSlot(input, 12, 8);
            widgets.addFillingArrow(42, 8, 3000);
            widgets.addSlot(output, 78, 8).recipeContext(this);
            widgets.addSlot(byproduct, 102, 8).recipeContext(this);
            if (note != null && !note.isEmpty()) {
                widgets.addText(Text.literal("§8" + note), 10, 26, 0x555555, false);
            }
        }
    }
}

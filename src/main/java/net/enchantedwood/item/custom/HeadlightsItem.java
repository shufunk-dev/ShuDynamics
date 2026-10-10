package net.enchantedwood.item.custom;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class HeadlightsItem extends Item {
    public enum LightTier {
        HALOGEN("Halogen Headlights", "§e", 12, "Standard warm beam (12 Light, 8m range)."),
        LED("LED Floodlights", "§b", 15, "Wide-angle floodlight array (15 Light, 16m broad cone)."),
        XENON("Xenon High-Beams", "§d", 15, "Piercing long-range high-beams (15 Light, 32m reach). Outlines night hazards in forward beam.");

        private final String name;
        private final String colorCode;
        private final int lightLevel;
        private final String description;

        LightTier(String name, String colorCode, int lightLevel, String description) {
            this.name = name;
            this.colorCode = colorCode;
            this.lightLevel = lightLevel;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getColorCode() {
            return colorCode;
        }

        public int getLightLevel() {
            return lightLevel;
        }

        public String getDescription() {
            return description;
        }
    }

    private final LightTier tier;

    public HeadlightsItem(LightTier tier, Properties settings) {
        super(settings);
        this.tier = tier;
    }

    public LightTier getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§7ATV Automotive Headlights Module"));
        textConsumer.accept(Component.literal("§eTier: " + tier.getColorCode() + tier.getName()));
        textConsumer.accept(Component.literal("§bLuminance: §fLevel " + tier.getLightLevel()));
        textConsumer.accept(Component.literal("§8" + tier.getDescription()));
        textConsumer.accept(Component.literal("§6Required component for ATV assembly in the Vehicle Fabricator."));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}

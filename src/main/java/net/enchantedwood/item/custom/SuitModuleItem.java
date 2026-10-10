package net.enchantedwood.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class SuitModuleItem extends Item {
    private final String moduleType;
    private final List<Component> descriptions;

    public SuitModuleItem(Properties settings, String moduleType, List<Component> descriptions) {
        super(settings.stacksTo(16).fireResistant());
        this.moduleType = moduleType;
        this.descriptions = descriptions;
    }

    public String getModuleType() {
        return this.moduleType;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§6⚡ Suit Upgrade Module: §f" + this.moduleType));
        for (Component desc : this.descriptions) {
            textConsumer.accept(desc);
        }
        textConsumer.accept(Component.literal("§8Install into Modular Power Suit via Access Panel (V) or Powered Anvil"));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}

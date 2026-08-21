package net.enchantedwood.item.custom;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class StorageCrystalItem extends Item {
    private final int capacity;

    public StorageCrystalItem(int capacity, Settings settings) {
        super(settings);
        this.capacity = capacity;
    }

    public int getCapacity() {
        return this.capacity;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§7Storage Capacity: §e" + String.format("%,d Items", capacity)));
        textConsumer.accept(Text.literal("§a✔ Empty Crystal §7(Ready for Installation / Upgrade)"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}

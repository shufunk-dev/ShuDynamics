package net.enchantedwood.item.custom;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class StorageCrystalItem extends Item {
    private final int capacity;

    public StorageCrystalItem(int capacity, Properties settings) {
        super(settings);
        this.capacity = capacity;
    }

    public int getCapacity() {
        return this.capacity;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§7Storage Capacity: §e" + String.format("%,d Items", capacity)));
        textConsumer.accept(Component.literal("§a✔ Empty Crystal §7(Ready for Installation / Upgrade)"));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}

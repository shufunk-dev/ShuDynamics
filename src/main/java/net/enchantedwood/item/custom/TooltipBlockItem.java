package net.enchantedwood.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class TooltipBlockItem extends BlockItem {
    private final List<Component> tooltips;

    public TooltipBlockItem(Block block, Properties settings, Component... tooltips) {
        super(block, settings);
        this.tooltips = List.of(tooltips);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        net.minecraft.world.item.component.CustomData nbtComponent = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (nbtComponent != null) {
            net.minecraft.nbt.CompoundTag nbt = nbtComponent.copyTag();
            if (nbt.contains("boundX")) {
                int x = nbt.getInt("boundX").orElse(0);
                int y = nbt.getInt("boundY").orElse(0);
                int z = nbt.getInt("boundZ").orElse(0);
                String dim = nbt.getString("boundDimension").orElse("minecraft:overworld");
                String dimName = dim.contains("mining_dimension") ? "Mining Dimension" :
                        dim.contains("nether") ? "Nether" :
                        dim.contains("end") ? "The End" : "Overworld";
                textConsumer.accept(Component.literal("§6✔ Bound Network: §f(" + x + ", " + y + ", " + z + ") in " + dimName));
                textConsumer.accept(Component.literal("§a✨ Automatically reconnects when placed!"));
            }
        }
        for (Component line : tooltips) {
            textConsumer.accept(line);
        }
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}

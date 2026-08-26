package net.enchantedwood.block.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AsphaltBlock extends Block {
    public AsphaltBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient()) {
            if (entity instanceof LivingEntity living) {
                // Give a subtle continuous speed boost when running on asphalt roads
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20, 0, false, false, true));
            }
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    public static boolean isIronOrBetterPickaxe(ItemStack tool) {
        if (tool.isEmpty()) return false;

        boolean isPickaxe = tool.isIn(ItemTags.PICKAXES)
                || tool.getItem() instanceof net.enchantedwood.item.custom.HammerItem
                || Registries.ITEM.getId(tool.getItem()).getPath().contains("pickaxe")
                || Registries.ITEM.getId(tool.getItem()).getPath().contains("hammer");

        if (!isPickaxe) return false;

        String id = Registries.ITEM.getId(tool.getItem()).getPath();
        // Disallow wooden, stone, or golden tiers (only iron, bronze, steel, diamond, netherite, titanium, enchanted)
        if (id.startsWith("wooden_") || id.startsWith("stone_") || id.startsWith("golden_") || id.equals("gold_pickaxe")) {
            return false;
        }
        return true;
    }

    public static boolean hasSilkTouch(ItemStack tool) {
        if (tool.isEmpty()) return false;
        ItemEnchantmentsComponent enchantments = tool.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments != null) {
            for (var entry : enchantments.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(Enchantments.SILK_TOUCH)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient() && player != null && !player.isCreative()) {
            ItemStack tool = player.getMainHandStack();
            if (isIronOrBetterPickaxe(tool)) {
                if (hasSilkTouch(tool) || world.random.nextBoolean()) {
                    // 50% chance: Drop Asphalt Block intact
                    dropStack(world, pos, new ItemStack(this));
                } else {
                    // 50% chance: Reverts into Mineral Tar
                    dropStack(world, pos, new ItemStack(ModItems.MINERAL_TAR));
                }
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}

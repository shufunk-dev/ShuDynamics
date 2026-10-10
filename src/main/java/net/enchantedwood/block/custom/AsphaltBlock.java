package net.enchantedwood.block.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class AsphaltBlock extends Block {
    public AsphaltBlock(Properties settings) {
        super(settings);
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClientSide()) {
            if (entity instanceof LivingEntity living) {
                // Give a subtle continuous speed boost when running on asphalt roads
                living.addEffect(new MobEffectInstance(MobEffects.SPEED, 20, 0, false, false, true));
            }
        }
        super.stepOn(world, pos, state, entity);
    }

    public static boolean isIronOrBetterPickaxe(ItemStack tool) {
        if (tool.isEmpty()) return false;

        boolean isPickaxe = tool.is(ItemTags.PICKAXES)
                || tool.getItem() instanceof net.enchantedwood.item.custom.HammerItem
                || BuiltInRegistries.ITEM.getKey(tool.getItem()).getPath().contains("pickaxe")
                || BuiltInRegistries.ITEM.getKey(tool.getItem()).getPath().contains("hammer");

        if (!isPickaxe) return false;

        String id = BuiltInRegistries.ITEM.getKey(tool.getItem()).getPath();
        // Disallow wooden, stone, or golden tiers (only iron, bronze, steel, diamond, netherite, titanium, enchanted)
        if (id.startsWith("wooden_") || id.startsWith("stone_") || id.startsWith("golden_") || id.equals("gold_pickaxe")) {
            return false;
        }
        return true;
    }

    public static boolean hasSilkTouch(ItemStack tool) {
        if (tool.isEmpty()) return false;
        ItemEnchantments enchantments = tool.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null) {
            for (var entry : enchantments.entrySet()) {
                if (entry.getKey().is(Enchantments.SILK_TOUCH)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide() && player != null && !player.isCreative()) {
            ItemStack tool = player.getMainHandItem();
            if (isIronOrBetterPickaxe(tool)) {
                if (hasSilkTouch(tool) || world.getRandom().nextBoolean()) {
                    // 50% chance: Drop Asphalt Block intact
                    popResource(world, pos, new ItemStack(this));
                } else {
                    // 50% chance: Reverts into Mineral Tar
                    popResource(world, pos, new ItemStack(ModItems.MINERAL_TAR));
                }
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}

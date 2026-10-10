package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class AtmosphericAnchorBlock extends Block {

    public AtmosphericAnchorBlock(Properties settings) {
        super(settings);
    }

    public static boolean isEnchantedPickaxe(ItemStack tool) {
        if (tool.isEmpty()) return false;

        boolean isPickaxe = tool.is(ItemTags.PICKAXES)
                || tool.is(ModItems.ENCHANTED_COBBLESTONE_PICKAXE)
                || tool.getItem() instanceof net.enchantedwood.item.custom.EnchantedCobblestonePickaxeItem
                || tool.getItem() instanceof net.enchantedwood.item.custom.HammerItem
                || BuiltInRegistries.ITEM.getKey(tool.getItem()).getPath().contains("pickaxe")
                || BuiltInRegistries.ITEM.getKey(tool.getItem()).getPath().contains("hammer");

        if (!isPickaxe) return false;

        // 1. Mod's innate Enchanted-tier tools (Enchanted Cobblestone Pickaxe, Enchanted Hammers, etc.)
        String itemPath = BuiltInRegistries.ITEM.getKey(tool.getItem()).getPath();
        if (itemPath.contains("enchanted") || tool.is(ModItems.ENCHANTED_COBBLESTONE_PICKAXE) || tool.is(ModItems.ENCHANTED_COBBLESTONE_HAMMER)) {
            return true;
        }

        // 2. Any pickaxe with active enchantments
        if (tool.isEnchanted()) return true;
        if (tool.get(DataComponents.ENCHANTMENTS) != null && !tool.get(DataComponents.ENCHANTMENTS).isEmpty()) return true;
        if (tool.get(DataComponents.STORED_ENCHANTMENTS) != null && !tool.get(DataComponents.STORED_ENCHANTMENTS).isEmpty()) return true;

        return false;
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide() && player != null && !player.isCreative()) {
            ItemStack tool = player.getMainHandItem();

            if (isEnchantedPickaxe(tool)) {
                popResource(world, pos, new ItemStack(this));
            } else {
                player.sendOverlayMessage(Component.literal("§c⚠ Anomaly Keystone destabilized! An enchanted pickaxe is required to harvest it."));
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0f, 0.8f);
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (random.nextInt(20) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            double y = pos.getY() + 0.9;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            world.addParticle(ParticleTypes.CLOUD, x, y, z, 0.0, 0.04, 0.0);
            if (random.nextInt(40) == 0) {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2f, 1.8f);
            }
        }
    }
}

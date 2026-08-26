package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class AtmosphericAnchorBlock extends Block {
    public static final MapCodec<AtmosphericAnchorBlock> CODEC = createCodec(AtmosphericAnchorBlock::new);

    public AtmosphericAnchorBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    public static boolean isEnchantedPickaxe(ItemStack tool) {
        if (tool.isEmpty()) return false;

        boolean isPickaxe = tool.isIn(ItemTags.PICKAXES)
                || tool.isOf(ModItems.ENCHANTED_COBBLESTONE_PICKAXE)
                || tool.getItem() instanceof net.enchantedwood.item.custom.EnchantedCobblestonePickaxeItem
                || tool.getItem() instanceof net.enchantedwood.item.custom.HammerItem
                || Registries.ITEM.getId(tool.getItem()).getPath().contains("pickaxe")
                || Registries.ITEM.getId(tool.getItem()).getPath().contains("hammer");

        if (!isPickaxe) return false;

        // 1. Mod's innate Enchanted-tier tools (Enchanted Cobblestone Pickaxe, Enchanted Hammers, etc.)
        String itemPath = Registries.ITEM.getId(tool.getItem()).getPath();
        if (itemPath.contains("enchanted") || tool.isOf(ModItems.ENCHANTED_COBBLESTONE_PICKAXE) || tool.isOf(ModItems.ENCHANTED_COBBLESTONE_HAMMER)) {
            return true;
        }

        // 2. Any pickaxe with active enchantments
        if (tool.hasEnchantments()) return true;
        if (tool.get(DataComponentTypes.ENCHANTMENTS) != null && !tool.get(DataComponentTypes.ENCHANTMENTS).isEmpty()) return true;
        if (tool.get(DataComponentTypes.STORED_ENCHANTMENTS) != null && !tool.get(DataComponentTypes.STORED_ENCHANTMENTS).isEmpty()) return true;

        return false;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient() && player != null && !player.isCreative()) {
            ItemStack tool = player.getMainHandStack();

            if (isEnchantedPickaxe(tool)) {
                dropStack(world, pos, new ItemStack(this));
            } else {
                player.sendMessage(Text.literal("§c⚠ Anomaly Keystone destabilized! An enchanted pickaxe is required to harvest it."), true);
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.BLOCKS, 1.0f, 0.8f);
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(20) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            double y = pos.getY() + 0.9;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            world.addParticleClient(ParticleTypes.CLOUD, x, y, z, 0.0, 0.04, 0.0);
            if (random.nextInt(40) == 0) {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.2f, 1.8f);
            }
        }
    }
}

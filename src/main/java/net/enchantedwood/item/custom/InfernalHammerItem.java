package net.enchantedwood.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class InfernalHammerItem extends HammerItem {
    private static final ThreadLocal<Boolean> IS_MINING_AREA = ThreadLocal.withInitial(() -> false);

    public InfernalHammerItem(Properties settings) {
        super(settings);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.igniteForSeconds(8.0f);
        super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§6✦ Nether Thermal Excavator"));
        textConsumer.accept(Component.literal("§7Mines a §e3×3 area §7of stone, ores, and terrain."));
        textConsumer.accept(Component.literal("§c✦ Innate Auto-Smelt: §7Smelts mined ores directly into ingots."));
        textConsumer.accept(Component.literal("§4✦ Fire Aspect: §7Ignites targets on hit & 100% fireproof."));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClientSide() && miner instanceof ServerPlayer player && !IS_MINING_AREA.get()) {
            if (state.getDestroySpeed(world, pos) > 0.0f) {
                IS_MINING_AREA.set(true);
                try {
                    mine3x3AutoSmelt(stack, (ServerLevel) world, pos, player);
                } finally {
                    IS_MINING_AREA.set(false);
                }
            }
        }
        return super.mineBlock(stack, world, state, pos, miner);
    }

    private void mine3x3AutoSmelt(ItemStack stack, ServerLevel world, BlockPos origin, ServerPlayer player) {
        Direction side = getTargetedSide(player, origin);

        int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;

        switch (side.getAxis()) {
            case Y -> {
                minX = -1; maxX = 1;
                minZ = -1; maxZ = 1;
            }
            case X -> {
                minY = -1; maxY = 1;
                minZ = -1; maxZ = 1;
            }
            case Z -> {
                minX = -1; maxX = 1;
                minY = -1; maxY = 1;
            }
        }

        // Smelt drops from the center block too
        smeltNearbyDrops(world, origin);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    BlockPos targetPos = origin.offset(x, y, z);
                    BlockState targetState = world.getBlockState(targetPos);

                    if (canHarvestBlock(targetState, world, targetPos)) {
                        // Spawn flame particles
                        world.sendParticles(ParticleTypes.FLAME, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.02);
                        player.gameMode.destroyBlock(targetPos);
                        smeltNearbyDrops(world, targetPos);

                        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                        if (stack.isEmpty()) return;
                    }
                }
            }
        }

        world.playSound(null, origin, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.8f);
    }

    private void smeltNearbyDrops(ServerLevel world, BlockPos pos) {
        List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(1.5), e -> true);
        for (ItemEntity itemEntity : items) {
            ItemStack drop = itemEntity.getItem();
            ItemStack smelted = getSmeltedResult(drop);
            if (!smelted.isEmpty()) {
                itemEntity.setItem(smelted);
            }
        }
    }

    public static ItemStack getSmeltedResult(ItemStack input) {
        Item item = input.getItem();
        int count = input.getCount();

        if (item == Items.RAW_IRON || item == Items.IRON_ORE || item == Items.DEEPSLATE_IRON_ORE) {
            return new ItemStack(Items.IRON_INGOT, count);
        }
        if (item == Items.RAW_COPPER || item == Items.COPPER_ORE || item == Items.DEEPSLATE_COPPER_ORE) {
            return new ItemStack(Items.COPPER_INGOT, count);
        }
        if (item == Items.RAW_GOLD || item == Items.GOLD_ORE || item == Items.DEEPSLATE_GOLD_ORE || item == Items.NETHER_GOLD_ORE) {
            return new ItemStack(Items.GOLD_INGOT, count);
        }
        if (item == Items.COBBLESTONE || item == Items.COBBLED_DEEPSLATE) {
            return new ItemStack(item == Items.COBBLESTONE ? Items.STONE : Items.DEEPSLATE, count);
        }
        if (item == Items.SAND || item == Items.RED_SAND) {
            return new ItemStack(Items.GLASS, count);
        }
        if (item == Items.ANCIENT_DEBRIS) {
            return new ItemStack(Items.NETHERITE_SCRAP, count);
        }
        if (item == Items.CLAY_BALL) {
            return new ItemStack(Items.BRICK, count);
        }
        if (item == Items.WET_SPONGE) {
            return new ItemStack(Items.SPONGE, count);
        }
        if (item == net.enchantedwood.item.ModItems.RAW_BAUXITE) {
            return new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, count);
        }
        if (item == net.enchantedwood.item.ModItems.RAW_TIN) {
            return new ItemStack(net.enchantedwood.item.ModItems.TIN_INGOT, count);
        }
        if (item == net.enchantedwood.item.ModItems.RAW_TITANIUM) {
            return new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, count);
        }
        return ItemStack.EMPTY;
    }

    private boolean canHarvestBlock(BlockState state, Level world, BlockPos pos) {
        if (state.isAir() || state.getDestroySpeed(world, pos) < 0) return false;
        if (state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock || world.getBlockEntity(pos) != null) return false;

        return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.NEEDS_STONE_TOOL)
                || state.is(BlockTags.NEEDS_IRON_TOOL)
                || state.is(BlockTags.NEEDS_DIAMOND_TOOL)
                || !state.requiresCorrectToolForDrops();
    }

    private Direction getTargetedSide(Player player, BlockPos pos) {
        if (player.getXRot() > 40.0f) {
            return Direction.UP;
        } else if (player.getXRot() < -40.0f) {
            return Direction.DOWN;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 rotation = player.getViewVector(1.0f);
        Vec3 reachVec = eyePos.add(rotation.x * 5.0, rotation.y * 5.0, rotation.z * 5.0);

        BlockHitResult hit = player.level().clip(new ClipContext(
                eyePos,
                reachVec,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getDirection();
        }
        return player.getDirection().getOpposite();
    }
}

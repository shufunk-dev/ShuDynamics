package net.enchantedwood.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Consumer;

public class InfernalHammerItem extends HammerItem {
    private static final ThreadLocal<Boolean> IS_MINING_AREA = ThreadLocal.withInitial(() -> false);

    public InfernalHammerItem(Settings settings) {
        super(settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.setOnFireFor(8.0f);
        super.postHit(stack, target, attacker);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§6✦ Nether Thermal Excavator"));
        textConsumer.accept(Text.literal("§7Mines a §e3×3 area §7of stone, ores, and terrain."));
        textConsumer.accept(Text.literal("§c✦ Innate Auto-Smelt: §7Smelts mined ores directly into ingots."));
        textConsumer.accept(Text.literal("§4✦ Fire Aspect: §7Ignites targets on hit & 100% fireproof."));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient() && miner instanceof ServerPlayerEntity player && !IS_MINING_AREA.get()) {
            if (state.getHardness(world, pos) > 0.0f) {
                IS_MINING_AREA.set(true);
                try {
                    mine3x3AutoSmelt(stack, (ServerWorld) world, pos, player);
                } finally {
                    IS_MINING_AREA.set(false);
                }
            }
        }
        return super.postMine(stack, world, state, pos, miner);
    }

    private void mine3x3AutoSmelt(ItemStack stack, ServerWorld world, BlockPos origin, ServerPlayerEntity player) {
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

                    BlockPos targetPos = origin.add(x, y, z);
                    BlockState targetState = world.getBlockState(targetPos);

                    if (canHarvestBlock(targetState, world, targetPos)) {
                        // Spawn flame particles
                        world.spawnParticles(ParticleTypes.FLAME, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.02);
                        player.interactionManager.tryBreakBlock(targetPos);
                        smeltNearbyDrops(world, targetPos);

                        stack.damage(1, player, EquipmentSlot.MAINHAND);
                        if (stack.isEmpty()) return;
                    }
                }
            }
        }

        world.playSound(null, origin, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5f, 1.8f);
    }

    private void smeltNearbyDrops(ServerWorld world, BlockPos pos) {
        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, new net.minecraft.util.math.Box(pos).expand(1.5), e -> true);
        for (ItemEntity itemEntity : items) {
            ItemStack drop = itemEntity.getStack();
            ItemStack smelted = getSmeltedResult(drop);
            if (!smelted.isEmpty()) {
                itemEntity.setStack(smelted);
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

    private boolean canHarvestBlock(BlockState state, World world, BlockPos pos) {
        if (state.isAir() || state.getHardness(world, pos) < 0) return false;
        if (state.getBlock() instanceof net.minecraft.block.BlockEntityProvider || world.getBlockEntity(pos) != null) return false;

        return state.isIn(BlockTags.PICKAXE_MINEABLE)
                || state.isIn(BlockTags.SHOVEL_MINEABLE)
                || state.isIn(BlockTags.NEEDS_STONE_TOOL)
                || state.isIn(BlockTags.NEEDS_IRON_TOOL)
                || state.isIn(BlockTags.NEEDS_DIAMOND_TOOL)
                || !state.isToolRequired();
    }

    private Direction getTargetedSide(PlayerEntity player, BlockPos pos) {
        if (player.getPitch() > 40.0f) {
            return Direction.UP;
        } else if (player.getPitch() < -40.0f) {
            return Direction.DOWN;
        }

        Vec3d eyePos = player.getEyePos();
        Vec3d rotation = player.getRotationVec(1.0f);
        Vec3d reachVec = eyePos.add(rotation.x * 5.0, rotation.y * 5.0, rotation.z * 5.0);

        BlockHitResult hit = player.getEntityWorld().raycast(new RaycastContext(
                eyePos,
                reachVec,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getSide();
        }
        return player.getHorizontalFacing().getOpposite();
    }
}

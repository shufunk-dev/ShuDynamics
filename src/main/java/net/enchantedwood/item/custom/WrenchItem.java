package net.enchantedwood.item.custom;

import net.enchantedwood.block.entity.DigitalConverterBlockEntity;
import net.enchantedwood.block.entity.EnchantedStorageControllerBlockEntity;
import net.enchantedwood.block.entity.EnchantedStorageTerminalBlockEntity;
import net.enchantedwood.block.entity.LaserQuarryBlockEntity;
import net.enchantedwood.util.Wrenchable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class WrenchItem extends Item {
    public WrenchItem(Settings settings) {
        super(settings.maxCount(1));
    }

    public static Direction getTargetedDirection(BlockPos pos, Direction side, Vec3d hitPos) {
        double dx = hitPos.x - (pos.getX() + 0.5);
        double dy = hitPos.y - (pos.getY() + 0.5);
        double dz = hitPos.z - (pos.getZ() + 0.5);

        double absX = Math.abs(dx);
        double absY = Math.abs(dy);
        double absZ = Math.abs(dz);

        // If clicked on a protruding arm
        if (absX > 0.22 || absY > 0.22 || absZ > 0.22) {
            if (absX > absY && absX > absZ) {
                return dx > 0 ? Direction.EAST : Direction.WEST;
            } else if (absY > absX && absY > absZ) {
                return dy > 0 ? Direction.UP : Direction.DOWN;
            } else {
                return dz > 0 ? Direction.SOUTH : Direction.NORTH;
            }
        }

        return side;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (user.isSneaking()) {
            ItemStack stack = user.getStackInHand(hand);
            NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
            if (nbt.contains("boundX")) {
                if (!world.isClient()) {
                    nbt.remove("boundX");
                    nbt.remove("boundY");
                    nbt.remove("boundZ");
                    nbt.remove("boundDimension");
                    stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
                    world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.8f, 0.8f);
                    user.sendMessage(Text.literal("§6[Wrench] §7Cleared stored network frequency from Wrench."), true);
                }
                return ActionResult.SUCCESS;
            }
        }
        return super.use(world, user, hand);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        Direction side = context.getSide();
        Vec3d hitPos = context.getHitPos();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        ItemStack wrenchStack = context.getStack();

        if (player == null) return ActionResult.PASS;

        Direction targetDir = getTargetedDirection(pos, side, hitPos);

        // 1. Check if block implements Wrenchable (Pipes, Extractors, Inserters)
        if (block instanceof Wrenchable wrenchable) {
            if (player.isSneaking()) {
                return wrenchable.onShiftWrenched(world, pos, player, targetDir);
            } else {
                return wrenchable.onWrenched(world, pos, player, targetDir);
            }
        }

        // 2. Storage Network Linking (Shift + Right-Click)
        if (player.isSneaking()) {
            BlockEntity targetBe = world.getBlockEntity(pos);

            // A. Shift + Right-Click Storage Controller or Terminal -> Store network frequency
            if (targetBe instanceof EnchantedStorageControllerBlockEntity || targetBe instanceof EnchantedStorageTerminalBlockEntity) {
                if (!world.isClient()) {
                    NbtCompound nbt = wrenchStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
                    nbt.putInt("boundX", pos.getX());
                    nbt.putInt("boundY", pos.getY());
                    nbt.putInt("boundZ", pos.getZ());
                    nbt.putString("boundDimension", world.getRegistryKey().getValue().toString());
                    wrenchStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

                    world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.4f);
                    player.sendMessage(Text.literal("§6[Wrench] §a✨ Stored Base Storage Network at (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")!"), true);
                }
                return ActionResult.SUCCESS;
            }

            // B. Shift + Right-Click Laser Quarry -> Link or Unlink
            if (targetBe instanceof LaserQuarryBlockEntity quarry) {
                if (!world.isClient()) {
                    NbtCompound nbt = wrenchStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
                    if (nbt.contains("boundX")) {
                        int bx = nbt.getInt("boundX").orElse(0);
                        int by = nbt.getInt("boundY").orElse(0);
                        int bz = nbt.getInt("boundZ").orElse(0);
                        String bDim = nbt.getString("boundDimension").orElse("minecraft:overworld");

                        quarry.bindNetwork(new BlockPos(bx, by, bz), bDim);
                        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.3f);
                        player.sendMessage(Text.literal("§6[Wrench] §a✨ Laser Quarry linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"), true);
                    } else if (quarry.isBoundToRemote()) {
                        quarry.unbindNetwork();
                        world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 1.0f, 0.8f);
                        player.sendMessage(Text.literal("§6[Wrench] §e⚠️ Unlinked Laser Quarry from remote network (reverted to local search)."), true);
                    } else {
                        // Dismantle if no network stored
                        world.breakBlock(pos, true, player);
                        world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
                        player.sendMessage(Text.literal("§6[Wrench] §eDismantled " + block.getName().getString()), true);
                    }
                }
                return ActionResult.SUCCESS;
            }

            // C. Shift + Right-Click Digital Converter -> Link or Unlink
            if (targetBe instanceof DigitalConverterBlockEntity converter) {
                if (!world.isClient()) {
                    NbtCompound nbt = wrenchStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
                    if (nbt.contains("boundX")) {
                        int bx = nbt.getInt("boundX").orElse(0);
                        int by = nbt.getInt("boundY").orElse(0);
                        int bz = nbt.getInt("boundZ").orElse(0);
                        String bDim = nbt.getString("boundDimension").orElse("minecraft:overworld");

                        converter.bindNetwork(new BlockPos(bx, by, bz), bDim);
                        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.3f);
                        player.sendMessage(Text.literal("§6[Wrench] §a✨ Digital Converter linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"), true);
                    } else if (converter.isBoundToRemote()) {
                        converter.unbindNetwork();
                        world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 1.0f, 0.8f);
                        player.sendMessage(Text.literal("§6[Wrench] §e⚠️ Unlinked Digital Converter from remote network (reverted to local search)."), true);
                    } else {
                        // Dismantle if no network stored
                        world.breakBlock(pos, true, player);
                        world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
                        player.sendMessage(Text.literal("§6[Wrench] §eDismantled " + block.getName().getString()), true);
                    }
                }
                return ActionResult.SUCCESS;
            }

            // 3. General Shift + Right-Click Dismantle on mod blocks
            if (state.getBlock().asItem() != null && state.getHardness(world, pos) >= 0) {
                if (!world.isClient()) {
                    world.breakBlock(pos, true, player);
                    world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    player.sendMessage(Text.literal("§6[Wrench] §eDismantled " + block.getName().getString()), true);
                }
                return ActionResult.SUCCESS;
            }
        }

        // 4. Right-Click Machine Rotation
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            if (!world.isClient()) {
                Direction current = state.get(Properties.HORIZONTAL_FACING);
                Direction next = current.rotateYClockwise();
                world.setBlockState(pos, state.with(Properties.HORIZONTAL_FACING, next), Block.NOTIFY_ALL);
                world.playSound(null, pos, SoundEvents.BLOCK_COPPER_GRATE_PLACE, SoundCategory.BLOCKS, 1.0f, 1.2f);
                player.sendMessage(Text.literal("§6[Wrench] §aRotated " + next.asString().toUpperCase()), true);
            }
            return ActionResult.SUCCESS;
        } else if (state.contains(Properties.FACING)) {
            if (!world.isClient()) {
                Direction current = state.get(Properties.FACING);
                Direction[] all = Direction.values();
                Direction next = all[(current.ordinal() + 1) % all.length];
                world.setBlockState(pos, state.with(Properties.FACING, next), Block.NOTIFY_ALL);
                world.playSound(null, pos, SoundEvents.BLOCK_COPPER_GRATE_PLACE, SoundCategory.BLOCKS, 1.0f, 1.2f);
                player.sendMessage(Text.literal("§6[Wrench] §aFacing " + next.asString().toUpperCase()), true);
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (nbt.contains("boundX")) {
            int x = nbt.getInt("boundX").orElse(0);
            int y = nbt.getInt("boundY").orElse(0);
            int z = nbt.getInt("boundZ").orElse(0);
            String dim = nbt.getString("boundDimension").orElse("minecraft:overworld");
            String dimName = dim.contains("mining_dimension") ? "Mining Dimension" :
                             dim.contains("nether") ? "Nether" :
                             dim.contains("end") ? "The End" : "Overworld";
            textConsumer.accept(Text.literal("§6✔ Stored Network: §f(" + x + ", " + y + ", " + z + ") in " + dimName));
            textConsumer.accept(Text.literal("§7Sneak + Right-Click Quarry or Converter to link"));
            textConsumer.accept(Text.literal("§8Sneak + Right-Click air to clear stored frequency"));
        } else {
            textConsumer.accept(Text.literal("§7Stored Network: §8None"));
            textConsumer.accept(Text.literal("§8Sneak + Right-Click Controller/Terminal to store frequency"));
        }
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}

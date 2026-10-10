package net.enchantedwood.item.custom;

import net.enchantedwood.block.entity.DigitalConverterBlockEntity;
import net.enchantedwood.block.entity.EnchantedStorageControllerBlockEntity;
import net.enchantedwood.block.entity.EnchantedStorageTerminalBlockEntity;
import net.enchantedwood.block.entity.LaserQuarryBlockEntity;
import net.enchantedwood.util.Wrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import java.util.function.Consumer;

public class WrenchItem extends Item {
    public WrenchItem(Properties settings) {
        super(settings.stacksTo(1));
    }

    public static Direction getTargetedDirection(BlockPos pos, Direction side, Vec3 hitPos) {
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
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (user.isShiftKeyDown()) {
            ItemStack stack = user.getItemInHand(hand);
            CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (nbt.contains("boundX")) {
                if (!world.isClientSide()) {
                    nbt.remove("boundX");
                    nbt.remove("boundY");
                    nbt.remove("boundZ");
                    nbt.remove("boundDimension");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                    world.playSound(null, user.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8f, 0.8f);
                    user.sendOverlayMessage(Component.literal("§6[Wrench] §7Cleared stored network frequency from Wrench."));
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(world, user, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        Direction side = context.getClickedFace();
        Vec3 hitPos = context.getClickLocation();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        ItemStack wrenchStack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        Direction targetDir = getTargetedDirection(pos, side, hitPos);

        // 1. Check if block implements Wrenchable (Pipes, Extractors, Inserters)
        if (block instanceof Wrenchable wrenchable) {
            if (player.isShiftKeyDown()) {
                return wrenchable.onShiftWrenched(world, pos, player, targetDir);
            } else {
                return wrenchable.onWrenched(world, pos, player, targetDir);
            }
        }

        // 2. Storage Network Linking (Shift + Right-Click)
        if (player.isShiftKeyDown()) {
            BlockEntity targetBe = world.getBlockEntity(pos);

            // A. Shift + Right-Click Storage Controller or Terminal -> Store network frequency
            if (targetBe instanceof EnchantedStorageControllerBlockEntity || targetBe instanceof EnchantedStorageTerminalBlockEntity) {
                if (!world.isClientSide()) {
                    CompoundTag nbt = wrenchStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    nbt.putInt("boundX", pos.getX());
                    nbt.putInt("boundY", pos.getY());
                    nbt.putInt("boundZ", pos.getZ());
                    nbt.putString("boundDimension", world.dimension().identifier().toString());
                    wrenchStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

                    world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.4f);
                    player.sendOverlayMessage(Component.literal("§6[Wrench] §a✨ Stored Base Storage Network at (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")!"));
                }
                return InteractionResult.SUCCESS;
            }

            // B. Shift + Right-Click Laser Quarry -> Link or Unlink
            if (targetBe instanceof LaserQuarryBlockEntity quarry) {
                if (!world.isClientSide()) {
                    CompoundTag nbt = wrenchStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    if (nbt.contains("boundX")) {
                        int bx = nbt.getInt("boundX").orElse(0);
                        int by = nbt.getInt("boundY").orElse(0);
                        int bz = nbt.getInt("boundZ").orElse(0);
                        String bDim = nbt.getString("boundDimension").orElse("minecraft:overworld");

                        quarry.bindNetwork(new BlockPos(bx, by, bz), bDim);
                        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
                        player.sendOverlayMessage(Component.literal("§6[Wrench] §a✨ Laser Quarry linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"));
                    } else {
                        // Dismantle if no network stored
                        ItemStack dropStack = new ItemStack(block.asItem());
                        if (!player.getInventory().add(dropStack)) {
                            Block.popResource(world, pos, dropStack);
                        }
                        world.destroyBlock(pos, false, player);
                        world.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
                        player.sendOverlayMessage(Component.literal("§6[Wrench] §eDismantled " + block.getName().getString()));
                    }
                }
                return InteractionResult.SUCCESS;
            }

            // C. Shift + Right-Click Digital Converter -> Link or Dismantle
            if (targetBe instanceof DigitalConverterBlockEntity converter) {
                if (!world.isClientSide()) {
                    CompoundTag nbt = wrenchStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    if (nbt.contains("boundX")) {
                        int bx = nbt.getInt("boundX").orElse(0);
                        int by = nbt.getInt("boundY").orElse(0);
                        int bz = nbt.getInt("boundZ").orElse(0);
                        String bDim = nbt.getString("boundDimension").orElse("minecraft:overworld");

                        converter.bindNetwork(new BlockPos(bx, by, bz), bDim);
                        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
                        player.sendOverlayMessage(Component.literal("§6[Wrench] §a✨ Digital Converter linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"));
                    } else {
                        // Dismantle if no network stored
                        ItemStack dropStack = new ItemStack(block.asItem());
                        if (!player.getInventory().add(dropStack)) {
                            Block.popResource(world, pos, dropStack);
                        }
                        world.destroyBlock(pos, false, player);
                        world.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
                        player.sendOverlayMessage(Component.literal("§6[Wrench] §eDismantled " + block.getName().getString()));
                    }
                }
                return InteractionResult.SUCCESS;
            }

            // D. Shift + Right-Click Casting Port -> Link or Dismantle
            if (targetBe instanceof net.enchantedwood.block.entity.CastingPortBlockEntity castingPort) {
                if (!world.isClientSide()) {
                    CompoundTag nbt = wrenchStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    if (nbt.contains("boundX")) {
                        int bx = nbt.getInt("boundX").orElse(0);
                        int by = nbt.getInt("boundY").orElse(0);
                        int bz = nbt.getInt("boundZ").orElse(0);
                        String bDim = nbt.getString("boundDimension").orElse("minecraft:overworld");

                        castingPort.bindNetwork(new BlockPos(bx, by, bz), bDim);
                        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
                        player.sendOverlayMessage(Component.literal("§6[Wrench] §a✨ Casting Port linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"));
                    } else {
                        // Dismantle if no network stored
                        ItemStack dropStack = new ItemStack(block.asItem());
                        if (!player.getInventory().add(dropStack)) {
                            Block.popResource(world, pos, dropStack);
                        }
                        world.destroyBlock(pos, false, player);
                        world.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
                        player.sendOverlayMessage(Component.literal("§6[Wrench] §eDismantled " + block.getName().getString()));
                    }
                }
                return InteractionResult.SUCCESS;
            }

            // E. Shift + Right-Click Hydraulic Press -> Link or Dismantle
            if (targetBe instanceof net.enchantedwood.block.entity.HydraulicPressBlockEntity press) {
                if (!world.isClientSide()) {
                    CompoundTag nbt = wrenchStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    if (nbt.contains("boundX")) {
                        int bx = nbt.getInt("boundX").orElse(0);
                        int by = nbt.getInt("boundY").orElse(0);
                        int bz = nbt.getInt("boundZ").orElse(0);
                        String bDim = nbt.getString("boundDimension").orElse("minecraft:overworld");

                        press.bindNetwork(new BlockPos(bx, by, bz), bDim);
                        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
                        player.sendOverlayMessage(Component.literal("§6[Wrench] §a✨ Hydraulic Press linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"));
                    } else {
                        ItemStack dropStack = new ItemStack(block.asItem());
                        if (!player.getInventory().add(dropStack)) {
                            Block.popResource(world, pos, dropStack);
                        }
                        world.destroyBlock(pos, false, player);
                        world.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
                        player.sendOverlayMessage(Component.literal("§6[Wrench] §eDismantled " + block.getName().getString()));
                    }
                }
                return InteractionResult.SUCCESS;
            }

            // F. Shift + Right-Click Circuit Fabricator -> Link or Dismantle
            if (targetBe instanceof net.enchantedwood.block.entity.CircuitFabricatorBlockEntity fab) {
                if (!world.isClientSide()) {
                    CompoundTag nbt = wrenchStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    if (nbt.contains("boundX")) {
                        int bx = nbt.getInt("boundX").orElse(0);
                        int by = nbt.getInt("boundY").orElse(0);
                        int bz = nbt.getInt("boundZ").orElse(0);
                        String bDim = nbt.getString("boundDimension").orElse("minecraft:overworld");

                        fab.bindNetwork(new BlockPos(bx, by, bz), bDim);
                        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
                        player.sendOverlayMessage(Component.literal("§6[Wrench] §a✨ Circuit Fabricator linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"));
                    } else {
                        ItemStack dropStack = new ItemStack(block.asItem());
                        if (!player.getInventory().add(dropStack)) {
                            Block.popResource(world, pos, dropStack);
                        }
                        world.destroyBlock(pos, false, player);
                        world.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
                        player.sendOverlayMessage(Component.literal("§6[Wrench] §eDismantled " + block.getName().getString()));
                    }
                }
                return InteractionResult.SUCCESS;
            }

            // G. Shift + Right-Click Enchanted Furnace -> Link or Dismantle
            if (targetBe instanceof net.enchantedwood.block.entity.EnchantedFurnaceBlockEntity furnace) {
                if (!world.isClientSide()) {
                    CompoundTag nbt = wrenchStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    if (nbt.contains("boundX")) {
                        int bx = nbt.getInt("boundX").orElse(0);
                        int by = nbt.getInt("boundY").orElse(0);
                        int bz = nbt.getInt("boundZ").orElse(0);
                        String bDim = nbt.getString("boundDimension").orElse("minecraft:overworld");

                        furnace.bindNetwork(new BlockPos(bx, by, bz), bDim);
                        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
                        player.sendOverlayMessage(Component.literal("§6[Wrench] §a✨ Enchanted Furnace linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"));
                    } else {
                        ItemStack dropStack = new ItemStack(block.asItem());
                        if (!player.getInventory().add(dropStack)) {
                            Block.popResource(world, pos, dropStack);
                        }
                        world.destroyBlock(pos, false, player);
                        world.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
                        player.sendOverlayMessage(Component.literal("§6[Wrench] §eDismantled " + block.getName().getString()));
                    }
                }
                return InteractionResult.SUCCESS;
            }

            // H. Shift + Right-Click Super Computer -> Link or Dismantle
            if (targetBe instanceof net.enchantedwood.block.entity.SuperComputerBlockEntity computer) {
                if (!world.isClientSide()) {
                    CompoundTag nbt = wrenchStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    if (nbt.contains("boundX")) {
                        int bx = nbt.getInt("boundX").orElse(0);
                        int by = nbt.getInt("boundY").orElse(0);
                        int bz = nbt.getInt("boundZ").orElse(0);
                        String bDim = nbt.getString("boundDimension").orElse("minecraft:overworld");

                        computer.bindNetwork(new BlockPos(bx, by, bz), bDim);
                        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
                        player.sendOverlayMessage(Component.literal("§6[Wrench] §a✨ Super Computer linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"));
                    } else {
                        ItemStack dropStack = new ItemStack(block.asItem());
                        if (!player.getInventory().add(dropStack)) {
                            Block.popResource(world, pos, dropStack);
                        }
                        world.destroyBlock(pos, false, player);
                        world.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
                        player.sendOverlayMessage(Component.literal("§6[Wrench] §eDismantled " + block.getName().getString()));
                    }
                }
                return InteractionResult.SUCCESS;
            }

            // 3. General Shift + Right-Click Dismantle on mod blocks
            if (state.getBlock().asItem() != null && state.getDestroySpeed(world, pos) >= 0) {
                if (!world.isClientSide()) {
                    ItemStack dropStack = new ItemStack(block.asItem());
                    if (!player.getInventory().add(dropStack)) {
                        Block.popResource(world, pos, dropStack);
                    }
                    world.destroyBlock(pos, false, player);
                    world.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
                    player.sendOverlayMessage(Component.literal("§6[Wrench] §eDismantled " + block.getName().getString()));
                }
                return InteractionResult.SUCCESS;
            }
        }

        // 4. Right-Click Machine Rotation
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            if (!world.isClientSide()) {
                Direction current = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                Direction next = current.getClockWise();
                BlockState nextState = state.setValue(BlockStateProperties.HORIZONTAL_FACING, next);
                world.setBlock(pos, nextState, Block.UPDATE_ALL);
                world.playSound(null, pos, SoundEvents.COPPER_GRATE_PLACE, SoundSource.BLOCKS, 1.0f, 1.2f);

                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof LaserQuarryBlockEntity quarry) {
                    quarry.resetScanCoordinates(nextState);
                    if (world instanceof net.minecraft.server.level.ServerLevel serverWorld) {
                        quarry.updateChunkLoading(serverWorld);
                    }
                    player.sendOverlayMessage(Component.literal("§6[Wrench] §aRotated " + next.getSerializedName().toUpperCase() + " §7(Corner Layout)"));
                } else {
                    player.sendOverlayMessage(Component.literal("§6[Wrench] §aRotated " + next.getSerializedName().toUpperCase()));
                }
            }
            return InteractionResult.SUCCESS;
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            if (!world.isClientSide()) {
                Direction current = state.getValue(BlockStateProperties.FACING);
                Direction[] all = Direction.values();
                Direction next = all[(current.ordinal() + 1) % all.length];
                world.setBlock(pos, state.setValue(BlockStateProperties.FACING, next), Block.UPDATE_ALL);
                world.playSound(null, pos, SoundEvents.COPPER_GRATE_PLACE, SoundSource.BLOCKS, 1.0f, 1.2f);
                player.sendOverlayMessage(Component.literal("§6[Wrench] §aFacing " + next.getSerializedName().toUpperCase()));
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (nbt.contains("boundX")) {
            int x = nbt.getInt("boundX").orElse(0);
            int y = nbt.getInt("boundY").orElse(0);
            int z = nbt.getInt("boundZ").orElse(0);
            String dim = nbt.getString("boundDimension").orElse("minecraft:overworld");
            String dimName = dim.contains("mining_dimension") ? "Mining Dimension" :
                             dim.contains("nether") ? "Nether" :
                             dim.contains("end") ? "The End" : "Overworld";
            textConsumer.accept(Component.literal("§6✔ Stored Network: §f(" + x + ", " + y + ", " + z + ") in " + dimName));
            textConsumer.accept(Component.literal("§7Sneak + Right-Click Quarry or Converter to link"));
            textConsumer.accept(Component.literal("§8Sneak + Right-Click air to clear stored frequency"));
        } else {
            textConsumer.accept(Component.literal("§7Stored Network: §8None"));
            textConsumer.accept(Component.literal("§8Sneak + Right-Click Controller/Terminal to store frequency"));
        }
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}

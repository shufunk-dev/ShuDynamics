package net.enchantedwood.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.enchantedwood.block.custom.EnchantedStorageControllerBlock;
import net.enchantedwood.block.entity.CastingPortBlockEntity;
import net.enchantedwood.block.entity.CircuitFabricatorBlockEntity;
import net.enchantedwood.block.entity.DigitalConverterBlockEntity;
import net.enchantedwood.block.entity.EnchantedFurnaceBlockEntity;
import net.enchantedwood.block.entity.EnchantedStorageControllerBlockEntity;
import net.enchantedwood.block.entity.EnchantedStorageTerminalBlockEntity;
import net.enchantedwood.block.entity.HydraulicPressBlockEntity;
import net.enchantedwood.block.entity.LaserQuarryBlockEntity;
import net.enchantedwood.block.entity.SuperComputerBlockEntity;

import java.util.function.Consumer;

public class WirelessStorageCrystalItem extends Item {
    public WirelessStorageCrystalItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockEntity be = world.getBlockEntity(pos);

        // 1. Bind crystal to Controller or Terminal
        if (be instanceof EnchantedStorageControllerBlockEntity || be instanceof EnchantedStorageTerminalBlockEntity) {
            if (!world.isClientSide() && player != null) {
                ItemStack stack = context.getItemInHand();
                CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                nbt.putInt("boundX", pos.getX());
                nbt.putInt("boundY", pos.getY());
                nbt.putInt("boundZ", pos.getZ());
                nbt.putString("boundDimension", world.dimension().identifier().toString());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

                world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.4f);
                player.sendOverlayMessage(Component.literal("§a✨ Wireless Crystal bound to (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")!"));
            }
            return InteractionResult.SUCCESS;
        }

        // 2. Sneak + Right-Click: Link target machine to network stored on crystal
        if (player != null && player.isShiftKeyDown()) {
            ItemStack stack = context.getItemInHand();
            CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (nbt.contains("boundX")) {
                int bx = nbt.getInt("boundX").orElse(0);
                int by = nbt.getInt("boundY").orElse(0);
                int bz = nbt.getInt("boundZ").orElse(0);
                String bDim = nbt.getString("boundDimension").orElse("minecraft:overworld");
                BlockPos targetNetPos = new BlockPos(bx, by, bz);

                if (be instanceof LaserQuarryBlockEntity quarry) {
                    if (!world.isClientSide()) {
                        quarry.bindNetwork(targetNetPos, bDim);
                        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
                        player.sendOverlayMessage(Component.literal("§6[Wireless Crystal] §a✨ Laser Quarry linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"));
                    }
                    return InteractionResult.SUCCESS;
                } else if (be instanceof DigitalConverterBlockEntity converter) {
                    if (!world.isClientSide()) {
                        converter.bindNetwork(targetNetPos, bDim);
                        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
                        player.sendOverlayMessage(Component.literal("§6[Wireless Crystal] §a✨ Digital Converter linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"));
                    }
                    return InteractionResult.SUCCESS;
                } else if (be instanceof CastingPortBlockEntity castingPort) {
                    if (!world.isClientSide()) {
                        castingPort.bindNetwork(targetNetPos, bDim);
                        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
                        player.sendOverlayMessage(Component.literal("§6[Wireless Crystal] §a✨ Casting Port linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"));
                    }
                    return InteractionResult.SUCCESS;
                } else if (be instanceof HydraulicPressBlockEntity press) {
                    if (!world.isClientSide()) {
                        press.bindNetwork(targetNetPos, bDim);
                        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
                        player.sendOverlayMessage(Component.literal("§6[Wireless Crystal] §a✨ Hydraulic Press linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"));
                    }
                    return InteractionResult.SUCCESS;
                } else if (be instanceof CircuitFabricatorBlockEntity fab) {
                    if (!world.isClientSide()) {
                        fab.bindNetwork(targetNetPos, bDim);
                        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
                        player.sendOverlayMessage(Component.literal("§6[Wireless Crystal] §a✨ Circuit Fabricator linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"));
                    }
                    return InteractionResult.SUCCESS;
                } else if (be instanceof EnchantedFurnaceBlockEntity furnace) {
                    if (!world.isClientSide()) {
                        furnace.bindNetwork(targetNetPos, bDim);
                        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
                        player.sendOverlayMessage(Component.literal("§6[Wireless Crystal] §a✨ Enchanted Furnace linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"));
                    }
                    return InteractionResult.SUCCESS;
                } else if (be instanceof SuperComputerBlockEntity computer) {
                    if (!world.isClientSide()) {
                        computer.bindNetwork(targetNetPos, bDim);
                        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
                        player.sendOverlayMessage(Component.literal("§6[Wireless Crystal] §a✨ Super Computer linked to Base Network at (" + bx + ", " + by + ", " + bz + ")!"));
                    }
                    return InteractionResult.SUCCESS;
                }
            } else if (be instanceof LaserQuarryBlockEntity || be instanceof DigitalConverterBlockEntity ||
                       be instanceof CastingPortBlockEntity || be instanceof HydraulicPressBlockEntity ||
                       be instanceof CircuitFabricatorBlockEntity || be instanceof EnchantedFurnaceBlockEntity ||
                       be instanceof SuperComputerBlockEntity) {
                if (!world.isClientSide()) {
                    player.sendOverlayMessage(Component.literal("§e⚠️ Sneak + Right-Click on an Enchanted Storage Controller or Terminal first to bind this crystal!"));
                }
                return InteractionResult.SUCCESS;
            }
        }

        return super.useOn(context);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (!world.isClientSide()) {
            CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (!nbt.contains("boundX")) {
                user.sendOverlayMessage(Component.literal("§e⚠️ Sneak + Right-Click on an Enchanted Storage Controller or Terminal to bind this Wireless Crystal."));
                return InteractionResult.SUCCESS;
            }

            int x = nbt.getInt("boundX").orElse(0);
            int y = nbt.getInt("boundY").orElse(0);
            int z = nbt.getInt("boundZ").orElse(0);
            String dimStr = nbt.getString("boundDimension").orElse("minecraft:overworld");
            BlockPos targetPos = new BlockPos(x, y, z);

            MinecraftServer server = world.getServer();
            if (server == null) return InteractionResult.SUCCESS;

            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimStr));
            ServerLevel targetWorld = server.getLevel(dimKey);
            if (targetWorld == null) targetWorld = (ServerLevel) world;

            boolean crossDimension = world != targetWorld;

            // Ensure chunk is accessible
            if (!targetWorld.hasChunk(targetPos.getX() >> 4, targetPos.getZ() >> 4)) {
                user.sendOverlayMessage(Component.literal("§c❌ Base chunk is unloaded. Install a Chunk Loader Module in the Storage Controller for infinite range!"));
                return InteractionResult.SUCCESS;
            }

            BlockEntity be = targetWorld.getBlockEntity(targetPos);
            EnchantedStorageTerminalBlockEntity terminal = null;
            EnchantedStorageControllerBlockEntity controller = null;

            if (be instanceof EnchantedStorageTerminalBlockEntity directTerminal) {
                terminal = directTerminal;
                // Search nearby for controller
                BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
                for (int dx = -16; dx <= 16; dx++) {
                    for (int dy = -8; dy <= 8; dy++) {
                        for (int dz = -16; dz <= 16; dz++) {
                            mut.set(targetPos.getX() + dx, targetPos.getY() + dy, targetPos.getZ() + dz);
                            BlockEntity candidate = targetWorld.getBlockEntity(mut);
                            if (candidate instanceof EnchantedStorageControllerBlockEntity c) {
                                controller = c;
                                break;
                            }
                        }
                        if (controller != null) break;
                    }
                    if (controller != null) break;
                }
            } else if (be instanceof EnchantedStorageControllerBlockEntity directController) {
                controller = directController;
                // Search nearby for terminal
                BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
                for (int dx = -16; dx <= 16; dx++) {
                    for (int dy = -8; dy <= 8; dy++) {
                        for (int dz = -16; dz <= 16; dz++) {
                            mut.set(targetPos.getX() + dx, targetPos.getY() + dy, targetPos.getZ() + dz);
                            BlockEntity candidate = targetWorld.getBlockEntity(mut);
                            if (candidate instanceof EnchantedStorageTerminalBlockEntity t) {
                                terminal = t;
                                break;
                            }
                        }
                        if (terminal != null) break;
                    }
                    if (terminal != null) break;
                }
            }

            // Power check
            if (controller != null && !controller.isOnline()) {
                user.sendOverlayMessage(Component.literal("§c❌ Storage Network is offline! (Controller has no power)"));
                return InteractionResult.SUCCESS;
            }

            // Cross-dimension check
            if (crossDimension) {
                if (controller == null || !controller.hasInterdimensionalCard()) {
                    user.sendOverlayMessage(Component.literal("§c❌ Interdimensional access requires an Interdimensional Card installed in the Storage Controller!"));
                    return InteractionResult.SUCCESS;
                }
            }

            if (terminal != null) {
                user.openMenu(terminal);
                world.playSound(null, user.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.2f);
            } else {
                user.sendOverlayMessage(Component.literal("§c❌ No Enchanted Storage Terminal found connected to this Controller."));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (nbt.contains("boundX")) {
            int x = nbt.getInt("boundX").orElse(0);
            int y = nbt.getInt("boundY").orElse(0);
            int z = nbt.getInt("boundZ").orElse(0);
            String dim = nbt.getString("boundDimension").orElse("minecraft:overworld");
            String dimName = dim.contains("nether") ? "Nether" : dim.contains("end") ? "The End" : "Overworld";
            textConsumer.accept(Component.literal("§a✔ Bound: §f(" + x + ", " + y + ", " + z + ") in " + dimName));
            textConsumer.accept(Component.literal("§7Right-Click anywhere to open network storage"));
        } else {
            textConsumer.accept(Component.literal("§7Status: §eUnbound"));
            textConsumer.accept(Component.literal("§8Right-Click a Controller or Terminal to bind"));
        }
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}

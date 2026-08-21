package net.enchantedwood.item.custom;

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
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.enchantedwood.block.custom.EnchantedStorageControllerBlock;
import net.enchantedwood.block.entity.EnchantedStorageControllerBlockEntity;
import net.enchantedwood.block.entity.EnchantedStorageTerminalBlockEntity;

import java.util.function.Consumer;

public class WirelessStorageCrystalItem extends Item {
    public WirelessStorageCrystalItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        BlockEntity be = world.getBlockEntity(pos);

        if (be instanceof EnchantedStorageControllerBlockEntity || be instanceof EnchantedStorageTerminalBlockEntity) {
            if (!world.isClient() && player != null) {
                ItemStack stack = context.getStack();
                NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
                nbt.putInt("boundX", pos.getX());
                nbt.putInt("boundY", pos.getY());
                nbt.putInt("boundZ", pos.getZ());
                nbt.putString("boundDimension", world.getRegistryKey().getValue().toString());
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

                world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.4f);
                player.sendMessage(Text.literal("§a✨ Wireless Crystal bound to (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")!"), true);
            }
            return ActionResult.SUCCESS;
        }
        return super.useOnBlock(context);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient()) {
            NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
            if (!nbt.contains("boundX")) {
                user.sendMessage(Text.literal("§e⚠️ Sneak + Right-Click on an Enchanted Storage Controller or Terminal to bind this Wireless Crystal."), true);
                return ActionResult.SUCCESS;
            }

            int x = nbt.getInt("boundX").orElse(0);
            int y = nbt.getInt("boundY").orElse(0);
            int z = nbt.getInt("boundZ").orElse(0);
            String dimStr = nbt.getString("boundDimension").orElse("minecraft:overworld");
            BlockPos targetPos = new BlockPos(x, y, z);

            MinecraftServer server = world.getServer();
            if (server == null) return ActionResult.SUCCESS;

            RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(dimStr));
            ServerWorld targetWorld = server.getWorld(dimKey);
            if (targetWorld == null) targetWorld = (ServerWorld) world;

            boolean crossDimension = world != targetWorld;

            // Ensure chunk is accessible
            if (!targetWorld.isChunkLoaded(targetPos.getX() >> 4, targetPos.getZ() >> 4)) {
                user.sendMessage(Text.literal("§c❌ Base chunk is unloaded. Install a Chunk Loader Module in the Storage Controller for infinite range!"), true);
                return ActionResult.SUCCESS;
            }

            BlockEntity be = targetWorld.getBlockEntity(targetPos);
            EnchantedStorageTerminalBlockEntity terminal = null;
            EnchantedStorageControllerBlockEntity controller = null;

            if (be instanceof EnchantedStorageTerminalBlockEntity directTerminal) {
                terminal = directTerminal;
                // Search nearby for controller
                BlockPos.Mutable mut = new BlockPos.Mutable();
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
                BlockPos.Mutable mut = new BlockPos.Mutable();
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
                user.sendMessage(Text.literal("§c❌ Storage Network is offline! (Controller has no power)"), true);
                return ActionResult.SUCCESS;
            }

            // Cross-dimension check
            if (crossDimension) {
                if (controller == null || !controller.hasInterdimensionalCard()) {
                    user.sendMessage(Text.literal("§c❌ Interdimensional access requires an Interdimensional Card installed in the Storage Controller!"), true);
                    return ActionResult.SUCCESS;
                }
            }

            if (terminal != null) {
                user.openHandledScreen(terminal);
                world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.8f, 1.2f);
            } else {
                user.sendMessage(Text.literal("§c❌ No Enchanted Storage Terminal found connected to this Controller."), true);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (nbt.contains("boundX")) {
            int x = nbt.getInt("boundX").orElse(0);
            int y = nbt.getInt("boundY").orElse(0);
            int z = nbt.getInt("boundZ").orElse(0);
            String dim = nbt.getString("boundDimension").orElse("minecraft:overworld");
            String dimName = dim.contains("nether") ? "Nether" : dim.contains("end") ? "The End" : "Overworld";
            textConsumer.accept(Text.literal("§a✔ Bound: §f(" + x + ", " + y + ", " + z + ") in " + dimName));
            textConsumer.accept(Text.literal("§7Right-Click anywhere to open network storage"));
        } else {
            textConsumer.accept(Text.literal("§7Status: §eUnbound"));
            textConsumer.accept(Text.literal("§8Right-Click a Controller or Terminal to bind"));
        }
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}

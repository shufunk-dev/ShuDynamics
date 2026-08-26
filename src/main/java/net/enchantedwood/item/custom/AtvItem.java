package net.enchantedwood.item.custom;

import net.enchantedwood.entity.ModEntities;
import net.enchantedwood.entity.custom.AtvEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class AtvItem extends Item {
    public AtvItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        BlockPos pos = context.getBlockPos();
        Direction side = context.getSide();
        BlockPos spawnPos = pos.offset(side);

        AtvEntity atv = new AtvEntity(ModEntities.ATV, world);
        atv.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        atv.setYaw(context.getPlayerYaw());

        ItemStack stack = context.getStack();
        atv.readInventoryFromItem(stack);

        world.spawnEntity(atv);
        stack.decrement(1);

        if (context.getPlayer() != null) {
            triggerAnomaly2Unlock(context.getPlayer(), world);
        }

        return ActionResult.SUCCESS;
    }

    public static void triggerAnomaly2Unlock(PlayerEntity player, World world) {
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer && !world.isClient()) {
            if (serverPlayer.addCommandTag("unlocked_kinetic_anchor")) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sound.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sound.SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, net.minecraft.sound.SoundCategory.PLAYERS, 0.8f, 1.4f);

                player.sendMessage(net.minecraft.text.Text.literal(""), false);
                player.sendMessage(net.minecraft.text.Text.literal("§5✦ §d§l[DIMENSIONAL RESONANCE DETECTED] §5✦"), false);
                player.sendMessage(net.minecraft.text.Text.literal("§fBy mastering terrestrial kinetic propulsion, you have unlocked the blueprint for:"), false);
                player.sendMessage(net.minecraft.text.Text.literal("§b⚙ §e§lAnomaly Keystone #2: §6Kinetic Anchor"), false);
                player.sendMessage(net.minecraft.text.Text.literal("§8(Craft with Titanium Ingot, Gasoline Canister, Rubber, Infused Heartwood, Asphalt & Crying Obsidian)"), false);
                player.sendMessage(net.minecraft.text.Text.literal(""), false);

                player.sendMessage(net.minecraft.text.Text.literal("§a✔ Anomaly Keystone #2 Unlocked: Kinetic Anchor"), true);

                try {
                    serverPlayer.unlockRecipes(java.util.List.of(net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.RECIPE, net.minecraft.util.Identifier.of(net.enchantedwood.EnchantedWoodMod.MOD_ID, "kinetic_anchor"))));
                } catch (Exception ignored) {}
            }
        }
    }
}

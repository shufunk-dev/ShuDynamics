package net.enchantedwood.item.custom;

import net.enchantedwood.entity.ModEntities;
import net.enchantedwood.entity.custom.AtvEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class AtvItem extends Item {
    public AtvItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        BlockPos spawnPos = pos.relative(side);

        AtvEntity atv = new AtvEntity(ModEntities.ATV, world);
        atv.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        atv.setYRot(context.getRotation());

        ItemStack stack = context.getItemInHand();
        atv.readInventoryFromItem(stack);

        world.addFreshEntity(atv);
        stack.shrink(1);

        if (context.getPlayer() != null) {
            triggerAnomaly2Unlock(context.getPlayer(), world);
        }

        return InteractionResult.SUCCESS;
    }

    public static void triggerAnomaly2Unlock(Player player, Level world) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && !world.isClientSide()) {
            if (serverPlayer.addTag("unlocked_kinetic_anchor")) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.4f);

                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§5✦ §d§l[DIMENSIONAL RESONANCE DETECTED] §5✦"));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§fBy mastering terrestrial kinetic propulsion, you have unlocked the blueprint for:"));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§b⚙ §e§lAnomaly Keystone #2: §6Kinetic Anchor"));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§8(Craft with Titanium Ingot, Gasoline Canister, Rubber, Infused Heartwood, Asphalt & Crying Obsidian)"));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));

                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a✔ Anomaly Keystone #2 Unlocked: Kinetic Anchor"));

                try {
                    serverPlayer.awardRecipesByKey(java.util.List.of(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath(net.enchantedwood.EnchantedWoodMod.MOD_ID, "kinetic_anchor"))));
                } catch (Exception ignored) {}
            }
        }
    }
}

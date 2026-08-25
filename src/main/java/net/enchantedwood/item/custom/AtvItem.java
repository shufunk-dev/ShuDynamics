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

        return ActionResult.SUCCESS;
    }
}

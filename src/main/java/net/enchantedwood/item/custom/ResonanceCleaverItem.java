package net.enchantedwood.item.custom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Consumer;

public class ResonanceCleaverItem extends Item {

    public ResonanceCleaverItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (user.getItemCooldownManager().isCoolingDown(stack)) {
            return ActionResult.PASS;
        }

        user.getItemCooldownManager().set(stack, 200); // 10 second cooldown

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            Vec3d origin = user.getEntityPos();
            Vec3d look = user.getRotationVec(1.0f);

            // Ground Shockwave forward 10 blocks
            for (int i = 1; i <= 10; i++) {
                Vec3d point = origin.add(look.multiply(i));
                serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM, point.x, point.y + 0.5, point.z, 1, 0, 0, 0, 0);
                serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y + 0.5, point.z, 8, 0.4, 0.4, 0.4, 0.05);

                Box hitBox = new Box(point.x - 1.5, point.y - 1.0, point.z - 1.5, point.x + 1.5, point.y + 2.0, point.z + 1.5);
                List<LivingEntity> targets = serverWorld.getEntitiesByClass(LivingEntity.class, hitBox, e -> e != user && e.isAlive());

                for (LivingEntity target : targets) {
                    target.damage(serverWorld, serverWorld.getDamageSources().playerAttack(user), 14.0f);
                    target.addVelocity(0, 0.65, 0);
                    target.velocityDirty = true;
                }
            }

            serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.2f, 1.2f);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§5✦ Relic of the Resonance Colossus ✦"));
        textConsumer.accept(Text.literal("§7Massive two-handed greatblade infused with dimensional shockwaves."));
        textConsumer.accept(Text.literal("§e✦ Right-Click: §bCataclysmic Ground Slam"));
        textConsumer.accept(Text.literal("§8 • Unleashes an expanding shockwave forward 10 blocks"));
        textConsumer.accept(Text.literal("§8 • Deals 14 damage and launches targets airborne"));
        textConsumer.accept(Text.literal("§8 • Cooldown: 10s"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}

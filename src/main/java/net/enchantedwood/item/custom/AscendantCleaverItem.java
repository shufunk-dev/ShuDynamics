package net.enchantedwood.item.custom;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
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

public class AscendantCleaverItem extends Item {

    public AscendantCleaverItem(Settings settings) {
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

        user.getItemCooldownManager().set(stack, 140); // 7 second cooldown (faster than tier 1)

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            Vec3d origin = user.getEntityPos();
            Vec3d look = user.getRotationVec(1.0f);

            // Ground Shockwave forward 20 blocks
            for (int i = 1; i <= 20; i++) {
                Vec3d point = origin.add(look.multiply(i));
                serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM, point.x, point.y + 0.5, point.z, 1, 0, 0, 0, 0);
                serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y + 0.5, point.z, 12, 0.5, 0.5, 0.5, 0.1);
                serverWorld.spawnParticles(ParticleTypes.FLAME, point.x, point.y + 0.5, point.z, 8, 0.4, 0.4, 0.4, 0.05);

                Box hitBox = new Box(point.x - 2.0, point.y - 1.0, point.z - 2.0, point.x + 2.0, point.y + 2.5, point.z + 2.0);
                List<LivingEntity> targets = serverWorld.getEntitiesByClass(LivingEntity.class, hitBox, e -> e != user && e.isAlive());

                for (LivingEntity target : targets) {
                    target.damage(serverWorld, serverWorld.getDamageSources().playerAttack(user), 24.0f);
                    target.addVelocity(0, 0.9, 0);
                    target.velocityDirty = true;
                }
            }

            serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.8f, 0.8f);
            return ActionResult.SUCCESS;
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§4✦ Ascendant World-Cleaver ✦"));
        textConsumer.accept(Text.literal("§7Forged by infusing the Resonance Cleaver with Primordial Catalysts."));
        textConsumer.accept(Text.literal("§e✦ Right-Click: §cCascading Sonic Fissure"));
        textConsumer.accept(Text.literal("§8 • Tears open a 20-block kinetic trench in targeted direction"));
        textConsumer.accept(Text.literal("§8 • Deals 24 True Damage to all caught victims and launches them"));
        textConsumer.accept(Text.literal("§8 • Cooldown: 7.0 seconds"));
        textConsumer.accept(Text.literal("§b✦ Legendary God-Tier Weapon."));
    }
}

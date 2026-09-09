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

public class SingularityStaffItem extends Item {

    public SingularityStaffItem(Settings settings) {
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

        user.getItemCooldownManager().set(stack, 240); // 12 second cooldown

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            Vec3d look = user.getRotationVec(1.0f);
            Vec3d targetCenter = user.getEyePos().add(look.multiply(12.0));

            serverWorld.spawnParticles(ParticleTypes.PORTAL, targetCenter.x, targetCenter.y, targetCenter.z, 150, 2.0, 2.0, 2.0, 0.2);
            serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, targetCenter.x, targetCenter.y, targetCenter.z, 100, 1.5, 1.5, 1.5, 0.1);
            serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM, targetCenter.x, targetCenter.y, targetCenter.z, 2, 0.0, 0.0, 0.0, 0.0);

            serverWorld.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 2.0f, 0.5f);

            Box pullBox = new Box(targetCenter.x - 8, targetCenter.y - 8, targetCenter.z - 8, targetCenter.x + 8, targetCenter.y + 8, targetCenter.z + 8);
            List<LivingEntity> targets = serverWorld.getEntitiesByClass(LivingEntity.class, pullBox, e -> e != user && e.isAlive());

            for (LivingEntity target : targets) {
                Vec3d pull = targetCenter.subtract(target.getEntityPos()).normalize().multiply(1.4);
                target.setVelocity(pull.x, 0.5, pull.z);
                target.velocityDirty = true;
                target.damage(serverWorld, serverWorld.getDamageSources().magic(), 12.0f);
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§5✦ Relic of the Resonance Colossus ✦"));
        textConsumer.accept(Text.literal("§7High-tech gravitational staff channeling a localized black hole."));
        textConsumer.accept(Text.literal("§e✦ Right-Click: §dSingularity Vortex"));
        textConsumer.accept(Text.literal("§8 • Creates a gravity well 12 blocks ahead pulling all enemies inward"));
        textConsumer.accept(Text.literal("§8 • Crushes targets for 12 magic/kinetic damage"));
        textConsumer.accept(Text.literal("§8 • Cooldown: 12s"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}

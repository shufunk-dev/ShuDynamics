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

public class VoidSingularityNexusItem extends Item {

    public VoidSingularityNexusItem(Settings settings) {
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

        user.getItemCooldownManager().set(stack, 180); // 9 second cooldown

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            Vec3d look = user.getRotationVec(1.0f);
            Vec3d targetCenter = user.getEyePos().add(look.multiply(16.0));

            serverWorld.spawnParticles(ParticleTypes.PORTAL, targetCenter.x, targetCenter.y, targetCenter.z, 200, 3.0, 3.0, 3.0, 0.3);
            serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, targetCenter.x, targetCenter.y, targetCenter.z, 150, 2.5, 2.5, 2.5, 0.2);
            serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM, targetCenter.x, targetCenter.y, targetCenter.z, 3, 0.0, 0.0, 0.0, 0.0);

            serverWorld.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 2.5f, 0.4f);

            // Drag all entities within 16 blocks
            Box pullBox = new Box(targetCenter.x - 16, targetCenter.y - 16, targetCenter.z - 16, targetCenter.x + 16, targetCenter.y + 16, targetCenter.z + 16);
            List<LivingEntity> targets = serverWorld.getEntitiesByClass(LivingEntity.class, pullBox, e -> e != user && e.isAlive());

            for (LivingEntity target : targets) {
                Vec3d pull = targetCenter.subtract(target.getEntityPos()).normalize().multiply(2.2);
                target.setVelocity(pull.x, 0.6, pull.z);
                target.velocityDirty = true;
                target.damage(serverWorld, serverWorld.getDamageSources().magic(), 25.0f);
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§5✦ Void Singularity Nexus ✦"));
        textConsumer.accept(Text.literal("§7Forged by infusing the Singularity Staff with Primordial Catalysts."));
        textConsumer.accept(Text.literal("§e✦ Right-Click: §5Micro-Black Hole Implosion"));
        textConsumer.accept(Text.literal("§8 • Rips open a gravitational singularity 16 blocks forward"));
        textConsumer.accept(Text.literal("§8 • Drags all entities within 16 blocks inward and implodes for 25 Damage"));
        textConsumer.accept(Text.literal("§8 • Cooldown: 9.0 seconds"));
        textConsumer.accept(Text.literal("§b✦ Planetary Swarm Annihilator."));
    }
}

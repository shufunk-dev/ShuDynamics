package net.enchantedwood.item.custom;

import net.enchantedwood.effect.ModStatusEffects;
import net.enchantedwood.item.ModItems;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
import net.minecraft.world.World;

import java.util.function.Consumer;

public class HyposprayItem extends Item {

    public HyposprayItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        return executeInoculation(world, user, user, hand);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        return executeInoculation(entity.getEntityWorld(), user, entity, hand);
    }

    private ActionResult executeInoculation(World world, PlayerEntity user, LivingEntity target, Hand hand) {
        // Find loaded cartridge: check offhand first, then main inventory
        ItemStack offhand = user.getOffHandStack();
        ItemStack cartridgeStack = ItemStack.EMPTY;
        boolean isOffhand = false;

        if (!offhand.isEmpty() && offhand.getItem() instanceof HyposprayCartridgeItem) {
            cartridgeStack = offhand;
            isOffhand = true;
        } else {
            for (int i = 0; i < user.getInventory().size(); i++) {
                ItemStack candidate = user.getInventory().getStack(i);
                if (!candidate.isEmpty() && candidate.getItem() instanceof HyposprayCartridgeItem) {
                    cartridgeStack = candidate;
                    break;
                }
            }
        }

        if (cartridgeStack.isEmpty() || !(cartridgeStack.getItem() instanceof HyposprayCartridgeItem cartridgeItem)) {
            if (!world.isClient()) {
                user.sendMessage(Text.literal("§e[Hypospray] No medical cartridge loaded! Place an ampoule in your offhand or inventory."), true);
            }
            return ActionResult.FAIL;
        }

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            boolean isOverdose = target.hasStatusEffect(ModStatusEffects.METABOLIC_SATURATION);

            if (isOverdose) {
                // Sickness / Overdose Penalty for injecting within 5s
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 10, 1));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 10, 1));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 10, 1));
                target.damage(serverWorld, serverWorld.getDamageSources().magic(), 4.0f); // 2 hearts rejection damage

                // Biohazard alarm sounds & dark smoke particles
                serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 0.6f, 1.6f);
                serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 0.8f, 0.8f);
                serverWorld.spawnParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.3, 0.4, 0.3, 0.05);

                user.sendMessage(Text.literal("§c⚠ WARNING: Metabolic Inoculant Overload! Chemical sickness induced! ⚠"), true);
                cartridgeItem.applyEffects(target, true); // 50% diminished potency
            } else {
                // Successful clean injection
                // Iconic Star Trek pneumatic jet hiss
                serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 0.9f, 1.8f);
                serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.6f, 1.7f);

                // High-pressure medical mist particles
                serverWorld.spawnParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.2, 0.3, 0.2, 0.03);
                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1.2, target.getZ(), 4, 0.2, 0.2, 0.2, 0.02);

                // Apply 5-second metabolic saturation
                target.addStatusEffect(new StatusEffectInstance(ModStatusEffects.METABOLIC_SATURATION, 100, 0));
                cartridgeItem.applyEffects(target, false);

                user.sendMessage(Text.literal("§a✔ Inoculation administered: " + cartridgeItem.getCartridgeType().title + " §7(Metabolic rest: 5s)"), true);
            }

            // Consume cartridge & refund Empty Cartridge
            cartridgeStack.decrement(1);
            user.giveItemStack(new ItemStack(ModItems.EMPTY_CARTRIDGE));
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§6✦ Starfleet Medical Hypospray ✦"));
        textConsumer.accept(Text.literal("§7High-pressure pneumatic needleless drug delivery device."));
        textConsumer.accept(Text.literal("§e✦ Right-Click: §fInoculate Self"));
        textConsumer.accept(Text.literal("§e✦ Right-Click on Entity: §fInoculate Teammate or Pet"));
        textConsumer.accept(Text.literal("§b✦ Auto-Loading: §7Loads cartridge from offhand, or auto-draws from inventory."));
        textConsumer.accept(Text.literal("§a✔ Recycles: §7Returns an Empty Cartridge after each injection."));
        textConsumer.accept(Text.literal("§c⚠ Medical Notice: §7Wait 5s between injections to avoid Inoculant Sickness."));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}

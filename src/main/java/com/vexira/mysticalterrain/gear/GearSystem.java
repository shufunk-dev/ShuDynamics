package com.vexira.mysticalterrain.gear;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.vexira.mysticalterrain.gear.arsenal.Arsenal;
import com.vexira.mysticalterrain.gear.arsenal.Arsenals;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AfterDamage;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AllowDamage;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AllowDeath;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.StartTick;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Join;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public final class GearSystem {
   private static boolean initialized;

   private GearSystem() {
   }

   public static void init() {
      if (!initialized) {
         initialized = true;
         PayloadTypeRegistry.playS2C().register(GearSyncPayload.TYPE, GearSyncPayload.CODEC);
         PayloadTypeRegistry.playC2S().register(SetAbilityPayload.TYPE, SetAbilityPayload.CODEC);
         ServerPlayNetworking.registerGlobalReceiver(SetAbilityPayload.TYPE, (payload, context) -> castSet(context.player()));
         ServerTickEvents.START_SERVER_TICK.register((StartTick)server -> GearState.time(server.getTicks()));
         ServerTickEvents.END_SERVER_TICK.register(GearSystem::tick);
         ServerPlayConnectionEvents.JOIN.register((Join)(handler, sender, server) -> GearState.of(handler.player).markDirty());
         ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, server) -> GearState.forget(handler.player));
         ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> GearState.clear());
         ServerLivingEntityEvents.ALLOW_DAMAGE.register((AllowDamage)(entity, source, amount) -> {
            if (entity instanceof PlayerEntity p && p.getEntityWorld() instanceof ServerWorld level) {
               for (Entry<String, Integer> e : MysticArmor.worn(p).entrySet()) {
                  Arsenal a = Arsenals.of(e.getKey());
                  if (a != null && e.getValue() >= 2 && !a.armorKit().allowDamage(level, p, source, amount, e.getValue())) {
                     return false;
                  }
               }
            }

            return true;
         });
         ServerLivingEntityEvents.AFTER_DAMAGE.register((AfterDamage)(entity, source, baseAmount, taken, blocked) -> {
            if (entity instanceof PlayerEntity p && p.getEntityWorld() instanceof ServerWorld level) {
               LivingEntity attacker = source.getAttacker() instanceof LivingEntity l && l != p ? l : null;

               for (Entry<String, Integer> e : MysticArmor.worn(p).entrySet()) {
                  Arsenal a = Arsenals.of(e.getKey());
                  if (a != null && e.getValue() >= 2) {
                     a.armorKit().hurt(level, p, attacker, source, taken, e.getValue());
                  }
               }
            }
         });
         ServerLivingEntityEvents.ALLOW_DEATH.register((AllowDeath)(entity, source, amount) -> {
            if (entity instanceof PlayerEntity p && p.getEntityWorld() instanceof ServerWorld level) {
               for (Entry<String, Integer> e : MysticArmor.worn(p).entrySet()) {
                  Arsenal a = Arsenals.of(e.getKey());
                  if (a != null && e.getValue() >= 2 && !a.armorKit().allowDeath(level, p, source, amount, e.getValue())) {
                     return false;
                  }
               }
            }

            return true;
         });
         CommandRegistrationCallback.EVENT
            .register(
               (CommandRegistrationCallback)(dispatcher, registries, environment) -> dispatcher.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("mysticgear")
                           .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK)))
                        .then(CommandManager.literal("reset").executes(ctx -> {
                           GearState.of(((ServerCommandSource)ctx.getSource()).getPlayerOrThrow()).clearCooldowns();
                           ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal("Gear cooldowns reset"), false);
                           return 1;
                        })))
                     .then(CommandManager.literal("resonance").then(CommandManager.argument("value", IntegerArgumentType.integer(0, 100)).executes(ctx -> {
                        ServerPlayerEntity p = ((ServerCommandSource)ctx.getSource()).getPlayerOrThrow();
                        int value = IntegerArgumentType.getInteger(ctx, "value");

                        for (ItemStack held : new ItemStack[]{p.getMainHandStack(), p.getOffHandStack()}) {
                           if (held.getItem() instanceof MysticWeapon w) {
                              GearState.of(p).setResonance(w.gearId(), value);
                           }
                        }

                        ((ServerCommandSource)ctx.getSource()).sendFeedback(() -> Text.literal("Resonance of the held weapon set to " + value), false);
                        return 1;
                     })))
               )
            );
      }
   }

   private static void tick(MinecraftServer server) {
      for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
         if (p.isAlive()) {
            for (Entry<String, Integer> e : MysticArmor.worn(p).entrySet()) {
               Arsenal a = Arsenals.of(e.getKey());
               if (a != null) {
                  a.armorKit().tick(p.getEntityWorld(), p, e.getValue());
               }
            }
         }

         GearState state = GearState.peek(p);
         if (state != null) {
            state.tick(p);
         }
      }
   }

   static void castSet(ServerPlayerEntity p) {
      if (p.isAlive() && !p.isSpectator()) {
         ServerWorld level = p.getEntityWorld();

         for (Entry<String, Integer> e : MysticArmor.worn(p).entrySet()) {
            Arsenal a = Arsenals.of(e.getKey());
            ArmorKit kit = a == null ? null : a.armorKit();
            Active active = kit == null ? null : kit.active();
            if (active != null && e.getValue() >= kit.activePieces()) {
               GearState state = GearState.of(p);
               if (!state.ready(kit.key())) {
                  return;
               }

               GearUse use = new GearUse(level, p, p.getEquippedStack(EquipmentSlot.CHEST), null, a, null, GearUse.Slot.SET, 0, 0, false, 1.0F);
               if (active.cast().fire(use)) {
                  state.cooldown(kit.key(), active.cooldown());
                  GearKit.sound(level, p, 1.4F, 0.95F + level.getRandom().nextFloat() * 0.1F, kit.sounds());
               }

               return;
            }
         }
      }
   }
}

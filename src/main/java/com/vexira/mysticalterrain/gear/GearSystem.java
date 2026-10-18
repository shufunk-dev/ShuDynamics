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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class GearSystem {
   private static boolean initialized;

   private GearSystem() {
   }

   public static void init() {
      if (!initialized) {
         initialized = true;
         PayloadTypeRegistry.clientboundPlay().register(GearSyncPayload.TYPE, GearSyncPayload.CODEC);
         PayloadTypeRegistry.serverboundPlay().register(SetAbilityPayload.TYPE, SetAbilityPayload.CODEC);
         ServerPlayNetworking.registerGlobalReceiver(SetAbilityPayload.TYPE, (payload, context) -> castSet(context.player()));
         ServerTickEvents.START_SERVER_TICK.register((StartTick)server -> GearState.time(server.getTickCount()));
         ServerTickEvents.END_SERVER_TICK.register(GearSystem::tick);
         ServerPlayConnectionEvents.JOIN.register((Join)(handler, sender, server) -> GearState.of(handler.player).markDirty());
         ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, server) -> GearState.forget(handler.player));
         ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> GearState.clear());
         ServerLivingEntityEvents.ALLOW_DAMAGE.register((AllowDamage)(entity, source, amount) -> {
            if (entity instanceof Player p && p.level() instanceof ServerLevel level) {
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
            if (entity instanceof Player p && p.level() instanceof ServerLevel level) {
               LivingEntity attacker = source.getEntity() instanceof LivingEntity l && l != p ? l : null;

               for (Entry<String, Integer> e : MysticArmor.worn(p).entrySet()) {
                  Arsenal a = Arsenals.of(e.getKey());
                  if (a != null && e.getValue() >= 2) {
                     a.armorKit().hurt(level, p, attacker, source, taken, e.getValue());
                  }
               }
            }
         });
         ServerLivingEntityEvents.ALLOW_DEATH.register((AllowDeath)(entity, source, amount) -> {
            if (entity instanceof Player p && p.level() instanceof ServerLevel level) {
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
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("mysticgear")
                           .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                        .then(Commands.literal("reset").executes(ctx -> {
                           GearState.of(((CommandSourceStack)ctx.getSource()).getPlayerOrException()).clearCooldowns();
                           ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Gear cooldowns reset"), false);
                           return 1;
                        })))
                     .then(Commands.literal("resonance").then(Commands.argument("value", IntegerArgumentType.integer(0, 100)).executes(ctx -> {
                        ServerPlayer p = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
                        int value = IntegerArgumentType.getInteger(ctx, "value");

                        for (ItemStack held : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
                           if (held.getItem() instanceof MysticWeapon w) {
                              GearState.of(p).setResonance(w.gearId(), value);
                           }
                        }

                        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Resonance of the held weapon set to " + value), false);
                        return 1;
                     })))
               )
            );
      }
   }

   private static void tick(MinecraftServer server) {
      for (ServerPlayer p : server.getPlayerList().getPlayers()) {
         if (p.isAlive()) {
            for (Entry<String, Integer> e : MysticArmor.worn(p).entrySet()) {
               Arsenal a = Arsenals.of(e.getKey());
               if (a != null) {
                  a.armorKit().tick(p.level(), p, e.getValue());
               }
            }
         }

         GearState state = GearState.peek(p);
         if (state != null) {
            state.tick(p);
         }
      }
   }

   static void castSet(ServerPlayer p) {
      if (p.isAlive() && !p.isSpectator()) {
         ServerLevel level = p.level();

         for (Entry<String, Integer> e : MysticArmor.worn(p).entrySet()) {
            Arsenal a = Arsenals.of(e.getKey());
            ArmorKit kit = a == null ? null : a.armorKit();
            Active active = kit == null ? null : kit.active();
            if (active != null && e.getValue() >= kit.activePieces()) {
               GearState state = GearState.of(p);
               if (!state.ready(kit.key())) {
                  return;
               }

               GearUse use = new GearUse(level, p, p.getItemBySlot(EquipmentSlot.CHEST), null, a, null, GearUse.Slot.SET, 0, 0, false, 1.0F);
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

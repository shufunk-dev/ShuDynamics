package com.vexira.mysticalterrain.boss.ritual;

import com.vexira.mysticalterrain.boss.BossFx;
import com.vexira.mysticalterrain.boss.MysticalBoss;
import com.vexira.mysticalterrain.boss.MysticalBosses;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RitualAltarBlockEntity extends BlockEntity {
   private static final int SPECTACLE = 70;
   private static final int HATCH = 200;
   private static final int GROW = 600;
   private int progress;
   private int jars;
   private int timer;
   private int summon = -1;
   private @Nullable UUID boss;

   public RitualAltarBlockEntity(BlockPos pos, BlockState state) {
      super(MysticalBosses.ALTAR_ENTITY, pos, state);
   }

   public Ritual ritual() {
      return ((RitualAltarBlock)this.getBlockState().getBlock()).ritual();
   }

   private boolean ready() {
      return this.summon < 0 && this.boss == null;
   }

   public @Nullable Component offer(ServerLevel level, Player player, ItemStack stack, int jar) {
      if (!this.ready()) {
         return Component.translatable("ritual.mysticalterrain.busy");
      }

      switch (this.ritual()) {
         case LIRAEL:
            this.step(level, 1);
            break;
         case GROTHMAW:
         case MYCELORD:
            if (this.progress > 0) {
               return Component.translatable("ritual.mysticalterrain.busy");
            }

            this.progress = 1;
            this.timer = 0;
            this.setStage(level, 1);
            break;
         case ASTRAEL:
            this.begin(level);
            break;
         case SEKHMAR:
            if ((this.jars & 1 << jar) != 0) {
               return Component.translatable("ritual.mysticalterrain.sekhmar.placed");
            }

            this.jars |= 1 << jar;
            this.step(level, 1);
            break;
         default:
            return Component.translatable("ritual.mysticalterrain.busy");
      }

      this.setChanged();
      return null;
   }

   public void ringBell(ServerLevel level, int index) {
      if (this.ready() && this.ritual() == Ritual.HRIMTHANE) {
         if (index == this.progress) {
            level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 2.0F, 0.6F + index * 0.2F);
            this.step(level, 1);
         } else {
            level.playSound(null, this.worldPosition, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.5F, 0.6F);
            this.progress = 0;
            this.setStage(level, 0);
         }

         this.setChanged();
      }
   }

   public boolean acceptThrown(ServerLevel level) {
      if (!this.ready()) {
         return false;
      }

      this.begin(level);
      return true;
   }

   private void step(ServerLevel level, int by) {
      this.progress += by;
      this.setStage(level, this.progress);
      BossFx.burst(level, Vec3.atCenterOf(this.worldPosition).add(0.0, 0.8, 0.0), this.ritual().particle(), 20, 0.3, 0.05);
      if (this.progress >= this.ritual().steps()) {
         this.begin(level);
      }
   }

   private void setStage(Level level, int stage) {
      BlockState state = this.getBlockState();
      int s = Math.max(0, Math.min(6, stage));
      if ((Integer)state.getValue(RitualAltarBlock.STAGE) != s) {
         level.setBlock(this.worldPosition, (BlockState)state.setValue(RitualAltarBlock.STAGE, s), 3);
      }
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, RitualAltarBlockEntity altar) {
      if (level instanceof ServerLevel server) {
         altar.tick(server);
      }
   }

   private void tick(ServerLevel level) {
      if (this.summon >= 0) {
         this.tickSpectacle(level);
      } else if (this.boss == null) {
         switch (this.ritual()) {
            case GROTHMAW:
               this.incubate(level, 200, SoundEvents.TURTLE_EGG_CRACK);
               break;
            case MYCELORD:
               this.incubate(level, 600, SoundEvents.ROOTED_DIRT_PLACE);
               break;
            case ASTRAEL:
            case SEKHMAR:
            default:
               break;
            case HOLLOW_WARDEN:
               if (level.getGameTime() % 20L == 0L) {
                  int lit = RitualFixtures.count(level, this.worldPosition, MysticalBosses.GLOOM_BRAZIER, GloomBrazierBlock.LIT);
                  this.setStage(level, lit);
                  if (lit >= 6) {
                     this.begin(level);
                  }
               }
               break;
            case ARCHON:
               if (level.getGameTime() % 20L == 0L) {
                  int charged = RitualFixtures.count(level, this.worldPosition, MysticalBosses.RUNE_PILLAR, RunePillarBlock.CHARGED);
                  this.setStage(level, charged);
                  if (charged >= 5) {
                     this.begin(level);
                  }
               }
         }
      } else {
         if (level.getGameTime() % 40L == 0L) {
            Entity e = level.getEntity(this.boss);
            if (e != null && e.isAlive()) {
               this.timer = 0;
            } else if (e != null || level.isLoaded(this.worldPosition) && ++this.timer > 3) {
               this.reset(level);
            }
         }
      }
   }

   private void incubate(ServerLevel level, int time, SoundEvent sound) {
      if (this.progress > 0) {
         this.timer++;
         int stage = 1 + this.timer * 4 / time;
         this.setStage(level, stage);
         if (this.timer % 40 == 0) {
            level.playSound(null, this.worldPosition, sound, SoundSource.BLOCKS, 1.5F, 0.6F);
            BossFx.burst(level, Vec3.atCenterOf(this.worldPosition).add(0.0, 1.0, 0.0), this.ritual().particle(), 10, 0.4, 0.05);
         }

         if (this.timer >= time) {
            this.begin(level);
         }
      }
   }

   public void begin(ServerLevel level) {
      this.summon = 0;
      this.setStage(level, 6);
      Vec3 c = Vec3.atCenterOf(this.worldPosition);
      level.playSound(null, c.x, c.y, c.z, MysticalBosses.RITUAL_ACTIVATE, SoundSource.HOSTILE, 6.0F, 0.9F);

      for (ServerPlayer p : level.getPlayers(px -> px.distanceToSqr(c) < 4096.0)) {
         p.sendSystemMessage(Component.translatable("ritual.mysticalterrain." + this.ritual().id() + ".begin"));
      }

      this.setChanged();
   }

   private void tickSpectacle(ServerLevel level) {
      this.summon++;
      Vec3 c = Vec3.atCenterOf(this.worldPosition).add(0.0, 0.6, 0.0);
      double rise = this.summon / 70.0;

      for (int k = 0; k < 3; k++) {
         double a = this.summon * 0.3 + k * Math.PI * 2.0 / 3.0;
         double r = 3.5 * (1.0 - rise) + 0.5;
         level.sendParticles(this.ritual().particle(), c.x + Math.cos(a) * r, c.y + rise * 6.0, c.z + Math.sin(a) * r, 3, 0.05, 0.05, 0.05, 0.01);
      }

      level.sendParticles(this.ritual().accent(), c.x, c.y + 1.0, c.z, 2, 0.2, rise * 3.0, 0.2, 0.02);
      if (this.summon % 20 == 0) {
         level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3.0F, 0.5F + (float)rise);
      }

      if (this.summon >= 70) {
         this.spawnBoss(level);
      }
   }

   private void spawnBoss(ServerLevel level) {
      this.summon = -1;
      MysticalBoss boss = (MysticalBoss)this.ritual().boss().create(level, EntitySpawnReason.EVENT);
      if (boss == null) {
         this.reset(level);
      } else {
         BlockPos at = this.worldPosition.above();
         boss.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
         boss.beginIntro(this.worldPosition);
         level.addFreshEntity(boss);
         this.boss = boss.getUUID();
         this.progress = 0;
         this.jars = 0;
         this.timer = 0;
         BossFx.burst(level, Vec3.atCenterOf(at), ParticleTypes.EXPLOSION, 4, 1.5, 0.0);
         this.setStage(level, 0);
         this.setChanged();
      }
   }

   private void reset(ServerLevel level) {
      this.boss = null;
      this.progress = 0;
      this.jars = 0;
      this.timer = 0;
      this.setStage(level, 0);
      RitualFixtures.resetAll(level, this.worldPosition);
      this.setChanged();
   }

   protected void saveAdditional(ValueOutput view) {
      super.saveAdditional(view);
      view.putInt("Progress", this.progress);
      view.putInt("Jars", this.jars);
      view.putInt("Timer", this.timer);
      if (this.boss != null) {
         view.store("Boss", UUIDUtil.CODEC, this.boss);
      }
   }

   protected void loadAdditional(ValueInput view) {
      super.loadAdditional(view);
      this.progress = view.getIntOr("Progress", 0);
      this.jars = view.getIntOr("Jars", 0);
      this.timer = view.getIntOr("Timer", 0);
      this.boss = (UUID)view.read("Boss", UUIDUtil.CODEC).orElse(null);
   }
}

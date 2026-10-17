package com.vexira.mysticalterrain.boss.ritual;

import com.vexira.mysticalterrain.boss.BossFx;
import com.vexira.mysticalterrain.boss.MysticalBoss;
import com.vexira.mysticalterrain.boss.MysticalBosses;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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
      return ((RitualAltarBlock)this.getCachedState().getBlock()).ritual();
   }

   private boolean ready() {
      return this.summon < 0 && this.boss == null;
   }

   public @Nullable Text offer(ServerWorld level, PlayerEntity player, ItemStack stack, int jar) {
      if (!this.ready()) {
         return Text.translatable("ritual.mysticalterrain.busy");
      }

      switch (this.ritual()) {
         case LIRAEL:
            this.step(level, 1);
            break;
         case GROTHMAW:
         case MYCELORD:
            if (this.progress > 0) {
               return Text.translatable("ritual.mysticalterrain.busy");
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
               return Text.translatable("ritual.mysticalterrain.sekhmar.placed");
            }

            this.jars |= 1 << jar;
            this.step(level, 1);
            break;
         default:
            return Text.translatable("ritual.mysticalterrain.busy");
      }

      this.markDirty();
      return null;
   }

   public void ringBell(ServerWorld level, int index) {
      if (this.ready() && this.ritual() == Ritual.HRIMTHANE) {
         if (index == this.progress) {
            level.playSound(null, this.pos, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.BLOCKS, 2.0F, 0.6F + index * 0.2F);
            this.step(level, 1);
         } else {
            level.playSound(null, this.pos, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1.5F, 0.6F);
            this.progress = 0;
            this.setStage(level, 0);
         }

         this.markDirty();
      }
   }

   public boolean acceptThrown(ServerWorld level) {
      if (!this.ready()) {
         return false;
      }

      this.begin(level);
      return true;
   }

   private void step(ServerWorld level, int by) {
      this.progress += by;
      this.setStage(level, this.progress);
      BossFx.burst(level, Vec3d.ofCenter(this.pos).add(0.0, 0.8, 0.0), this.ritual().particle(), 20, 0.3, 0.05);
      if (this.progress >= this.ritual().steps()) {
         this.begin(level);
      }
   }

   private void setStage(World level, int stage) {
      BlockState state = this.getCachedState();
      int s = Math.max(0, Math.min(6, stage));
      if ((Integer)state.get(RitualAltarBlock.STAGE) != s) {
         level.setBlockState(this.pos, (BlockState)state.with(RitualAltarBlock.STAGE, s), 3);
      }
   }

   public static void serverTick(World level, BlockPos pos, BlockState state, RitualAltarBlockEntity altar) {
      if (level instanceof ServerWorld server) {
         altar.tick(server);
      }
   }

   private void tick(ServerWorld level) {
      if (this.summon >= 0) {
         this.tickSpectacle(level);
      } else if (this.boss == null) {
         switch (this.ritual()) {
            case GROTHMAW:
               this.incubate(level, 200, SoundEvents.ENTITY_TURTLE_EGG_CRACK);
               break;
            case MYCELORD:
               this.incubate(level, 600, SoundEvents.BLOCK_ROOTED_DIRT_PLACE);
               break;
            case ASTRAEL:
            case SEKHMAR:
            default:
               break;
            case HOLLOW_WARDEN:
               if (level.getTime() % 20L == 0L) {
                  int lit = RitualFixtures.count(level, this.pos, MysticalBosses.GLOOM_BRAZIER, GloomBrazierBlock.LIT);
                  this.setStage(level, lit);
                  if (lit >= 6) {
                     this.begin(level);
                  }
               }
               break;
            case ARCHON:
               if (level.getTime() % 20L == 0L) {
                  int charged = RitualFixtures.count(level, this.pos, MysticalBosses.RUNE_PILLAR, RunePillarBlock.CHARGED);
                  this.setStage(level, charged);
                  if (charged >= 5) {
                     this.begin(level);
                  }
               }
         }
      } else {
         if (level.getTime() % 40L == 0L) {
            Entity e = level.getEntity(this.boss);
            if (e != null && e.isAlive()) {
               this.timer = 0;
            } else if (e != null || level.isPosLoaded(this.pos) && ++this.timer > 3) {
               this.reset(level);
            }
         }
      }
   }

   private void incubate(ServerWorld level, int time, SoundEvent sound) {
      if (this.progress > 0) {
         this.timer++;
         int stage = 1 + this.timer * 4 / time;
         this.setStage(level, stage);
         if (this.timer % 40 == 0) {
            level.playSound(null, this.pos, sound, SoundCategory.BLOCKS, 1.5F, 0.6F);
            BossFx.burst(level, Vec3d.ofCenter(this.pos).add(0.0, 1.0, 0.0), this.ritual().particle(), 10, 0.4, 0.05);
         }

         if (this.timer >= time) {
            this.begin(level);
         }
      }
   }

   public void begin(ServerWorld level) {
      this.summon = 0;
      this.setStage(level, 6);
      Vec3d c = Vec3d.ofCenter(this.pos);
      level.playSound(null, c.x, c.y, c.z, MysticalBosses.RITUAL_ACTIVATE, SoundCategory.HOSTILE, 6.0F, 0.9F);

      for (ServerPlayerEntity p : level.getPlayers(px -> px.squaredDistanceTo(c) < 4096.0)) {
         p.sendMessage(Text.translatable("ritual.mysticalterrain." + this.ritual().id() + ".begin"), false);
      }

      this.markDirty();
   }

   private void tickSpectacle(ServerWorld level) {
      this.summon++;
      Vec3d c = Vec3d.ofCenter(this.pos).add(0.0, 0.6, 0.0);
      double rise = this.summon / 70.0;

      for (int k = 0; k < 3; k++) {
         double a = this.summon * 0.3 + k * Math.PI * 2.0 / 3.0;
         double r = 3.5 * (1.0 - rise) + 0.5;
         level.spawnParticles(this.ritual().particle(), c.x + Math.cos(a) * r, c.y + rise * 6.0, c.z + Math.sin(a) * r, 3, 0.05, 0.05, 0.05, 0.01);
      }

      level.spawnParticles(this.ritual().accent(), c.x, c.y + 1.0, c.z, 2, 0.2, rise * 3.0, 0.2, 0.02);
      if (this.summon % 20 == 0) {
         level.playSound(null, c.x, c.y, c.z, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 3.0F, 0.5F + (float)rise);
      }

      if (this.summon >= 70) {
         this.spawnBoss(level);
      }
   }

   private void spawnBoss(ServerWorld level) {
      this.summon = -1;
      MysticalBoss boss = (MysticalBoss)this.ritual().boss().create(level, SpawnReason.EVENT);
      if (boss == null) {
         this.reset(level);
      } else {
         BlockPos at = this.pos.up();
         boss.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
         boss.beginIntro(this.pos);
         level.spawnEntity(boss);
         this.boss = boss.getUuid();
         this.progress = 0;
         this.jars = 0;
         this.timer = 0;
         BossFx.burst(level, Vec3d.ofCenter(at), ParticleTypes.EXPLOSION, 4, 1.5, 0.0);
         this.setStage(level, 0);
         this.markDirty();
      }
   }

   private void reset(ServerWorld level) {
      this.boss = null;
      this.progress = 0;
      this.jars = 0;
      this.timer = 0;
      this.setStage(level, 0);
      RitualFixtures.resetAll(level, this.pos);
      this.markDirty();
   }

   protected void writeData(WriteView view) {
      super.writeData(view);
      view.putInt("Progress", this.progress);
      view.putInt("Jars", this.jars);
      view.putInt("Timer", this.timer);
      if (this.boss != null) {
         view.put("Boss", Uuids.INT_STREAM_CODEC, this.boss);
      }
   }

   protected void readData(ReadView view) {
      super.readData(view);
      this.progress = view.getInt("Progress", 0);
      this.jars = view.getInt("Jars", 0);
      this.timer = view.getInt("Timer", 0);
      this.boss = (UUID)view.read("Boss", Uuids.INT_STREAM_CODEC).orElse(null);
   }
}

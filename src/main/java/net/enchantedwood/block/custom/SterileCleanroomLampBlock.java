package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class SterileCleanroomLampBlock extends Block {
    public static final MapCodec<SterileCleanroomLampBlock> CODEC = createCodec(SterileCleanroomLampBlock::new);

    public enum Mode implements StringIdentifiable {
        WHITE("white", 15, "§f✦ Cleanroom Lamp: Daylight LED (Luminance 15)"),
        UV("uv", 11, "§d✦ Cleanroom Lamp: UV-C Germicidal Mode (UV Sterilization)"),
        OFF("off", 0, "§8✦ Cleanroom Lamp: Standby (Off)");

        private final String name;
        public final int luminance;
        public final String statusMessage;

        Mode(String name, int luminance, String statusMessage) {
            this.name = name;
            this.luminance = luminance;
            this.statusMessage = statusMessage;
        }

        @Override
        public String asString() {
            return this.name;
        }

        public Mode next() {
            return switch (this) {
                case WHITE -> UV;
                case UV -> OFF;
                case OFF -> WHITE;
            };
        }
    }

    public static final EnumProperty<Mode> MODE = EnumProperty.of("mode", Mode.class);

    public SterileCleanroomLampBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(MODE, Mode.WHITE));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(MODE, Mode.WHITE);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        Mode current = state.get(MODE);
        Mode next = current.next();
        world.setBlockState(pos, state.with(MODE, next), Block.NOTIFY_ALL);

        float pitch = next == Mode.OFF ? 0.7f : (next == Mode.UV ? 1.4f : 1.1f);
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.6f, pitch);

        if (!world.isClient()) {
            player.sendMessage(Text.literal(next.statusMessage), true);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(MODE) == Mode.UV) {
            // Subtle germicidal ultraviolet sterilization sparkle
            if (random.nextFloat() < 0.25f) {
                double x = pos.getX() + 0.1 + random.nextDouble() * 0.8;
                double y = pos.getY() + 0.1 + random.nextDouble() * 0.8;
                double z = pos.getZ() + 0.1 + random.nextDouble() * 0.8;
                world.addParticleClient(ParticleTypes.END_ROD, x, y, z, 0.0, -0.01, 0.0);
            }
        }
    }
}

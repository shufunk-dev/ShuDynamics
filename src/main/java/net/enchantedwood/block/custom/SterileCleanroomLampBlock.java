package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class SterileCleanroomLampBlock extends Block {

    public enum Mode implements StringRepresentable {
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
        public String getSerializedName() {
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

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    public SterileCleanroomLampBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(MODE, Mode.WHITE));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(MODE, Mode.WHITE);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        Mode current = state.getValue(MODE);
        Mode next = current.next();
        world.setBlock(pos, state.setValue(MODE, next), Block.UPDATE_ALL);

        float pitch = next == Mode.OFF ? 0.7f : (next == Mode.UV ? 1.4f : 1.1f);
        world.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6f, pitch);

        if (!world.isClientSide()) {
            player.sendOverlayMessage(Component.literal(next.statusMessage));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (state.getValue(MODE) == Mode.UV) {
            // Subtle germicidal ultraviolet sterilization sparkle
            if (random.nextFloat() < 0.25f) {
                double x = pos.getX() + 0.1 + random.nextDouble() * 0.8;
                double y = pos.getY() + 0.1 + random.nextDouble() * 0.8;
                double z = pos.getZ() + 0.1 + random.nextDouble() * 0.8;
                world.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, -0.01, 0.0);
            }
        }
    }
}
